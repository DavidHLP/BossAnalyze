#!/usr/bin/env python3
"""Destructive fixtures ONLY in an explicitly named isolated restored schema.
MYSQL_PWD is consumed by docker, never printed. No production migration here.
"""
import argparse
import concurrent.futures
import os
from pathlib import Path
import subprocess
import time

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--container', required=True)
parser.add_argument('--schema', required=True)
parser.add_argument('--user', required=True)
args = parser.parse_args()
if not args.schema.startswith('boss_role_rehearsal_') or not args.schema.replace('_', '').isalnum():
    parser.error('only a newly restored boss_role_rehearsal_* schema is allowed')
if not os.environ.get('MYSQL_PWD'):
    parser.error('MYSQL_PWD must be supplied privately')
command = ['docker', 'exec', '-i', '-e', 'MYSQL_PWD', args.container,
           'mysql', '-u' + args.user, '--default-character-set=utf8mb4', '--batch', '--skip-column-names', args.schema]
migration = Path(__file__).with_name('20261108-role-name-unique.sql').read_text()


def sql(statement, ok=True):
    result = subprocess.run(command, input=statement, text=True, capture_output=True, timeout=20)
    if ok and result.returncode:
        # Do not expose duplicate values or restored business data in errors.
        raise AssertionError('unexpected MySQL failure (details suppressed)')
    return result


def rejects(fixture, cleanup):
    sql(fixture)
    result = sql(migration, ok=False)
    assert result.returncode and 'ERROR 1064' in result.stderr, 'guard must reject before DDL'
    sql(cleanup)


sql(migration)
sql(migration)
print('PASS migration + rerun')
sql('ALTER TABLE role DROP INDEX uk_role_name;')
for definition in ['KEY uk_role_name(role_name)',
                   'UNIQUE KEY uk_role_name(role_name,id)',
                   'UNIQUE KEY uk_role_name(role_name(10))']:
    rejects('ALTER TABLE role ADD ' + definition + ';', 'ALTER TABLE role DROP INDEX uk_role_name;')
print('PASS rejects same-name nonunique/composite/prefix indexes')
rejects("INSERT INTO role(role_name) VALUES ('fixture_duplicate'),('fixture_duplicate');",
        "DELETE FROM role WHERE role_name='fixture_duplicate';")
rejects("INSERT INTO role(role_name) VALUES (' fixture_trim ');",
        "DELETE FROM role WHERE role_name=' fixture_trim ';")
rejects("UPDATE role SET status=0 WHERE BINARY role_name=BINARY 'USER';",
        "UPDATE role SET status=1 WHERE BINARY role_name=BINARY 'USER';")
print('PASS rejects duplicates/whitespace/disabled default USER')
sql('ALTER TABLE role ADD UNIQUE KEY alternate_role_name(role_name);')
sql(migration)
assert sql("SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='role' AND index_name='uk_role_name';").stdout.strip() == '0'
sql('ALTER TABLE role DROP INDEX alternate_role_name;')
sql(migration)
print('PASS recognizes alternate-name full unique index')

# Each race uses separate clients/transactions. The first writer holds its lock
# until performance_schema confirms the second is actually waiting on that lock.
for kind in ['create/create', 'create/rename', 'rename/rename']:
    tag = kind.replace('/', '_')
    target = 'fixture_target_' + tag
    left = 'fixture_left_' + tag
    right = 'fixture_right_' + tag
    sql(f"INSERT INTO role(role_name) VALUES ('{left}'),('{right}');")
    first = (f"INSERT INTO role(role_name) VALUES ('{target}');" if kind != 'rename/rename'
             else f"UPDATE role SET role_name='{target}' WHERE role_name='{left}';")
    second = (f"INSERT INTO role(role_name) VALUES ('{target}');" if kind == 'create/create'
              else f"UPDATE role SET role_name='{target}' WHERE role_name='{right}';")
    client = subprocess.Popen(command[:-1] + ['--unbuffered', args.schema], stdin=subprocess.PIPE,
                              stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    try:
        client.stdin.write('SET innodb_lock_wait_timeout=8; BEGIN; ' + first + " SELECT 'locked';\n")
        client.stdin.flush()
        assert client.stdout.readline().strip() == 'locked'
        with concurrent.futures.ThreadPoolExecutor(max_workers=1) as pool:
            waiting = pool.submit(sql, 'SET innodb_lock_wait_timeout=8; BEGIN; ' + second + ' COMMIT;', False)
            deadline = time.monotonic() + 5
            observed = False
            while time.monotonic() < deadline:
                count = sql('SELECT COUNT(*) FROM performance_schema.data_lock_waits w JOIN performance_schema.data_locks l ON l.ENGINE_LOCK_ID=w.REQUESTING_ENGINE_LOCK_ID AND l.ENGINE=w.ENGINE WHERE l.OBJECT_SCHEMA=DATABASE() AND l.OBJECT_NAME=\'role\';').stdout.strip()
                if int(count) > 0:
                    observed = True
                    break
                time.sleep(0.05)
            client.stdin.write('COMMIT;\n')
            client.stdin.close()
            assert client.wait(timeout=10) == 0
            loser = waiting.result(timeout=12)
            assert observed, 'second transaction must overlap and wait'
            assert loser.returncode and 'ERROR 1062' in loser.stderr
        assert sql(f"SELECT COUNT(*) FROM role WHERE role_name='{target}';").stdout.strip() == '1'
    finally:
        if client.poll() is None:
            client.terminate()
            client.wait(timeout=5)
    print('PASS independent-connection ' + kind + ': lock wait observed, one commit, one 1062')

sql("INSERT INTO role(role_name) VALUES ('fixture_Cafe');")
for equivalent in ['fixture_cafe', 'fixture_Café']:
    result = sql(f"INSERT INTO role(role_name) VALUES ('{equivalent}');", ok=False)
    assert result.returncode and 'ERROR 1062' in result.stderr
# 0900_ai_ci is NO PAD: trailing spaces differ in SQL; the application trims.
result = sql("SELECT COUNT(*) FROM information_schema.collations WHERE collation_name=(SELECT collation_name FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='role' AND column_name='role_name') AND pad_attribute='NO PAD';")
if result.stdout.strip() == '1':
    sql("INSERT INTO role(role_name) VALUES ('fixture_Cafe ');")
    rejects('SELECT 1;', "DELETE FROM role WHERE BINARY role_name=BINARY 'fixture_Cafe ';")
print('PASS collation case/accent and trailing-space preflight semantics')
print('MySQL constraint tests passed; API HTTP/envelope 409 requires separate application validation.')
