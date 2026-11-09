#!/usr/bin/env python3
"""One command; no paid provider replay, no changes to running deployments."""
import json
import os
from pathlib import Path
import signal
import subprocess
import tempfile
import time
import zipfile

ROOT = Path(__file__).resolve().parents[2]
JAVA = Path(os.environ.get('WORKFLOW_JAVA_HOME', '/home/david/.local/share/mise/installs/java/zulu-17'))
MAVEN = os.environ.get('WORKFLOW_MAVEN', '/home/david/.local/share/mise/installs/maven/3.9.16/apache-maven-3.9.16/bin/mvn')
PYTHON = os.environ.get('WORKFLOW_PYTHON', '/tmp/boss-jev-rereview-evidence/venv/bin/python')
os.umask(0o077)
out = Path(tempfile.mkdtemp(prefix='boss-workflow-20261108-'))
print(json.dumps({'output': str(out)}), flush=True)
env = os.environ.copy()
for name in list(env):
    if name.startswith(('CMD_', 'AI_', 'JEV_')):
        del env[name]
env.update(JAVA_HOME=str(JAVA), PATH=str(JAVA / 'bin') + ':' + env['PATH'],
           PYTHONPATH=str(ROOT / 'ai-service'), WORKFLOW_OUTPUT=str(out),
           PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD='1')
processes = []
records = {}

def stat(pid):
    try:
        # comm can contain spaces; fields after its closing parenthesis are stable.
        fields = Path(f'/proc/{pid}/stat').read_text().rsplit(')', 1)[1].split()
        return int(fields[2]), fields[19], fields[0], int(fields[1])
    except (FileNotFoundError, ProcessLookupError):
        return None

def observe():
    current = {int(entry.name): stat(int(entry.name)) for entry in Path('/proc').iterdir() if entry.name.isdigit()}
    owned = {p.pid for p in processes} | {row['pid'] for row in records.values()
        if current.get(row['pid']) and current[row['pid']][1] == row['starttime']}
    for _ in range(20):
        found = {pid for pid, value in current.items() if value and value[3] in owned}
        if found <= owned:
            break
        owned |= found
    for pid in owned:
        value = current.get(pid)
        if value:
            records[(pid, value[1])] = dict(pid=pid, starttime=value[1], processGroup=value[0], parentPid=value[3])
    (out / 'processes.json').write_text(json.dumps(list(records.values()), indent=2))

def start(name, args):
    log = (out / (name + '.log')).open('w')
    try:
        p = subprocess.Popen(args, cwd=ROOT, env=env, stdout=log, stderr=subprocess.STDOUT, start_new_session=True)
    finally:
        log.close()
    processes.append(p)
    observe()
    return p

def wait(p, timeout):
    deadline = time.monotonic() + timeout
    while p.poll() is None:
        observe()
        if time.monotonic() > deadline:
            raise TimeoutError('owned process deadline')
        time.sleep(0.1)
    if p.returncode:
        raise RuntimeError('owned process failed; inspect private output logs')

def interrupted(signum, frame):
    raise KeyboardInterrupt('fixture interrupted; cleaning owned processes')

signal.signal(signal.SIGTERM, interrupted)
(out / 'source.json').write_text(json.dumps(dict(
    root=str(ROOT), head=subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip(),
    java=str(JAVA), maven=MAVEN, python=PYTHON,
    boss='synthetic fixture', provider='instance-local MockTransport; no external model call')))
status = 'failed'
try:
    wait(start('build', [MAVEN, '-f', 'backend/pom.xml', '-pl', 'crawler', '-am', '-DskipTests', 'package']), 240)
    classes = out / 'java'
    classes.mkdir()
    with zipfile.ZipFile(ROOT / 'backend/crawler/target/spring-crawler-0.0.1-SNAPSHOT.jar') as archive:
        archive.extractall(classes)
    cp = ':'.join(map(str, [classes, classes / 'BOOT-INF/classes', classes / 'BOOT-INF/lib/*']))
    wait(start('compile', [str(JAVA / 'bin/javac'), '-cp', cp, '-d', str(classes), 'tools/workflow-fixture/Workflow.java']), 60)
    provider = start('provider', [PYTHON, str(ROOT / 'tools/workflow-fixture/provider.py')])
    deadline = time.monotonic() + 20
    while not (out / 'python-port.txt').exists():
        if provider.poll() is not None or time.monotonic() > deadline:
            raise RuntimeError('fixture provider did not bind')
        time.sleep(0.1)
    # Bound readiness uses the actual HTTP endpoint, not file creation alone.
    import urllib.request
    for attempt in range(100):
        try:
            with urllib.request.urlopen('http://127.0.0.1:' + (out / 'python-port.txt').read_text() + '/health/ready', timeout=1) as response:
                assert response.status == 200
            break
        except OSError:
            if provider.poll() is not None:
                raise RuntimeError('fixture provider exited')
            time.sleep(0.1)
    else:
        raise RuntimeError('fixture provider readiness deadline')
    wait(start('workflow', [str(JAVA / 'bin/java'), '-cp', cp, 'com.david.hlp.crawler.sample.Workflow']), 180)
    status = 'verified'
finally:
    observe()
    for p in reversed(processes):
        if p.poll() is None:
            p.terminate()
            try:
                p.wait(timeout=10)
            except subprocess.TimeoutExpired:
                p.kill()
                p.wait(timeout=5)
        # Only our fresh process groups; clean any leftover browser descendants.
        try:
            os.killpg(p.pid, signal.SIGTERM)
        except ProcessLookupError:
            pass
    def live_owned():
        live = []
        for row in records.values():
            value = stat(row['pid'])
            if value and value[1] == row['starttime'] and value[2] != 'Z':
                live.append(row)
        return live

    # Chromium may use its own process group; signal only observed PID/starttime pairs.
    for sig in (signal.SIGTERM, signal.SIGKILL):
        for row in live_owned():
            value = stat(row['pid'])
            if value and value[1] == row['starttime']:
                try:
                    os.kill(row['pid'], sig)
                except ProcessLookupError:
                    pass
        deadline = time.monotonic() + 3
        while live_owned() and time.monotonic() < deadline:
            time.sleep(0.1)
    live = live_owned()
    (out / 'cleanup.json').write_text(json.dumps(dict(status=status, liveOwnedProcesses=live,
        retained=['new audit schema', 'private output'], stopped='owned services and browser'), indent=2))
    if live:
        raise RuntimeError('owned processes remain; see cleanup.json')
print(json.dumps(dict(status=status, output=str(out), dto=str(out / 'frontend-dto.json'))))
