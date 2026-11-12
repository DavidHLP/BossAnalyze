#!/usr/bin/env python3
"""Read-only inspection, never a release/rollback executor."""
import argparse
import contextlib
import hashlib
import json
import os
from pathlib import Path
import re
import sqlite3
import stat
import subprocess
import urllib.parse
import urllib.request


BASELINE = "0dcb7b6fb0532e4c652a5d3050b5524d25bc7052"
PORTS = dict(python=8090, crawler=8081, api=8080, analytics=8082, frontend=5020)
METADATA = ("current", "manifest", "session", "pair-state", "config")
CONTAINERS = ("bossanalyze-local-mysql-restored", "bossanalyze-local-redis-restored",
              "bossanalyze-store-20261108", "bossanalyze-mailpit-issue39")
FIELDS = ("pid", "starttime", "exe", "argv", "cwd")
# Changes to these build inputs need a new safety review, not ancestry alone.
BUILD_INPUTS = ("backend", "ai-service", "frontend", "deploy/rustfs")


class Refusal(ValueError):
    """Only static, public-safe explanations may cross the output boundary."""


def require(ok, reason):
    if not ok:
        raise Refusal(reason)


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def canonical(value):
    return json.dumps(value, sort_keys=True, separators=(",", ":")).encode()


def decode(data):
    def pairs(items):
        result = {}
        for key, value in items:
            require(key not in result, "duplicate JSON key")
            result[key] = value
        return result
    def invalid(_):
        raise ValueError("non-finite JSON number")
    return json.loads(data, object_pairs_hook=pairs, parse_constant=invalid)


def absolute(value):
    path = Path(value)
    require(path.is_absolute(), "absolute path required")
    return path


def read_file(path):
    before = path.stat()
    require(stat.S_ISREG(before.st_mode), "regular file required")
    data = path.read_bytes()
    after = path.stat()
    require(all(getattr(before, key) == getattr(after, key)
                for key in ("st_dev", "st_ino", "st_size", "st_mtime_ns", "st_ctime_ns")), "file changed during read")
    return data


def input_digest(path):
    require(not path.is_symlink(), "unsealed symlink input")
    if path.is_file():
        return sha256(read_file(path))
    require(path.is_dir(), "missing target input")
    before = path.stat().st_mtime_ns
    entries = sorted(path.rglob("*"))
    rows = []
    for item in entries:
        require(not item.is_symlink(), "unsealed symlink input")
        relative = item.relative_to(path).as_posix()
        rows.append([relative + "/", None] if item.is_dir()
                    else [relative, sha256(read_file(item))])
    require(path.stat().st_mtime_ns == before and sorted(path.rglob("*")) == entries, "target tree changed during read")
    return sha256(canonical(rows))


def budget_digest(path):
    # A read-only SQLite connection can still create WAL/SHM files. Deserialize
    # a stable, WAL-free byte snapshot into memory instead of opening the ledger.
    wal = Path(str(path) + "-wal")
    require(not wal.exists() or wal.stat().st_size == 0, "budget has WAL; cannot obtain a zero-write snapshot")
    data = read_file(path)
    with contextlib.closing(sqlite3.connect(":memory:")) as connection:
        connection.deserialize(data)
        value = sha256("\n".join(connection.iterdump()).encode())
    require(read_file(path) == data and (not wal.exists() or wal.stat().st_size == 0), "budget changed during read")
    return value


def run(command):
    return subprocess.run(command, check=True, capture_output=True, timeout=10).stdout


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


class Linux:
    """The only host boundary: /proc, ss, docker inspect and loopback GET."""
    def process(self, pid):
        path = Path("/proc", str(pid))
        before = (path / "stat").read_text().rsplit(")", 1)[1].split()[19]
        result = dict(pid=pid, starttime=before, exe=os.readlink(path / "exe"),
                      cwd=os.readlink(path / "cwd"),
                      argv=(path / "cmdline").read_bytes().rstrip(b"\0").decode().split("\0"))
        require((path / "stat").read_text().rsplit(")", 1)[1].split()[19] == before, "PID reused during read")
        return result

    def listeners(self, port):
        rows = run(["ss", "-H", "-ltnp", "sport", "=", ":" + str(port)]).decode().splitlines()
        owners = set()
        for row in rows:
            pids = re.findall(r"\bpid=(\d+)", row)
            require(pids, "listener owner hidden")
            owners.update(map(int, pids))
        return owners

    def containers(self):
        template = ('{"id":{{json .Id}},"pid":{{.State.Pid}},"running":{{json .State.Running}},'
                    '"startedAt":{{json .State.StartedAt}},"ports":{{json .NetworkSettings.Ports}}}')
        return {name: decode(run(["docker", "inspect", "--format", template, name])) for name in CONTAINERS}

    def get(self, port, path):
        opener = urllib.request.build_opener(urllib.request.ProxyHandler({}), NoRedirect())
        with opener.open("http://127.0.0.1:" + str(port) + path, timeout=5) as response:
            require(response.status == 200, "health HTTP status is not 200")
            body = response.read(1024 * 1024 + 1)
            require(len(body) <= 1024 * 1024, "inspection response too large")
            return body

    def health(self, name, port):
        path = "/health/ready" if name == "python" else "/" if name == "frontend" else "/actuator/health"
        body = self.get(port, path)
        if name == "python":
            value = decode(body)
            require(value["status"] == "ready" and value["resumeMatching"]["enabled"] is False and
                    value["qualityGate"]["calibrated"] is False and
                    value["qualityGate"]["automaticWritebackAllowed"] is False, "matching/quality hold absent")
        elif name != "frontend":
            require(decode(body)["status"] == "UP", "component is not UP")
        if name == "api":
            require(decode(self.get(port, "/actuator/health/readiness"))["status"] == "UP", "API readiness is not UP")
        return True

    def analytics_idle(self, pid):
        require(self.listeners(4040) == {pid}, "Spark UI ownership changed")
        base = "/api/v1/applications"
        applications = decode(self.get(4040, base))
        require(isinstance(applications, list) and applications, "Spark applications unknown")
        for application in applications:
            jobs = decode(self.get(4040, base + "/" + urllib.parse.quote(application["id"], safe="") + "/jobs"))
            require(isinstance(jobs, list) and all(job["status"] != "RUNNING" for job in jobs), "Spark job RUNNING")
        require(self.listeners(4040) == {pid}, "Spark UI owner changed during read")
        return True


def source_safe(repository, revision):
    require(isinstance(revision, str) and re.fullmatch(r"[0-9a-f]{40}", revision), "unknown source revision")
    git = ["git", "--no-replace-objects", "-C", str(repository)]
    require(run([*git, "cat-file", "-t", revision]).strip() == b"commit", "source is not a commit")
    try:
        run([*git, "merge-base", "--is-ancestor", BASELINE, revision])
        run([*git, "diff", "--no-ext-diff", "--no-textconv", "--quiet", BASELINE, revision, "--", *BUILD_INPUTS])
    except subprocess.CalledProcessError:
        raise Refusal("target source predates or changes the frozen security/matching-hold build inputs") from None


def record_valid(record, port=None):
    require(type(record["pid"]) is int and record["pid"] > 0, "invalid PID")
    require(isinstance(record["starttime"], str) and record["starttime"].isdigit(), "invalid starttime")
    absolute(record["exe"])
    absolute(record["cwd"])
    require(isinstance(record["argv"], list) and record["argv"] and
            all(isinstance(x, str) and x for x in record["argv"]), "invalid argv")
    if port is not None:
        require(type(record["port"]) is int and record["port"] == port, "component port changed")


def identity(system, record, port):
    record_valid(record, port if port != 27017 else None)
    live = system.process(record["pid"])
    require(all(live[key] == record[key] for key in FIELDS), "process identity changed or absent")
    require(system.listeners(port) == {record["pid"]}, "listener ownership changed or unverifiable")


def target_safe(state, args):
    require(set(state["old"]) == set(PORTS), "incomplete rollback targets")
    repository = absolute(args.repository)
    for revision in {record["sourceSHA"] for record in state["old"].values()}:
        source_safe(repository, revision)
    require(args.target_receipt is not None and args.target_receipt_sha256 is not None,
            "reviewed target receipt and independently pinned SHA-256 required")
    body = read_file(absolute(args.target_receipt))
    require(sha256(body) == args.target_receipt_sha256, "target receipt pin mismatch")
    receipt = decode(body)
    require(receipt["schemaVersion"] == 1 and receipt["safetyBaseline"] == BASELINE and
            set(receipt["targets"]) == set(PORTS), "unknown target safety contract")
    verified = {}
    for name, port in PORTS.items():
        record = state["old"][name]
        record_valid(record, port)
        env = record["env"]
        active_env = state["active"][name]["env"]
        require(isinstance(env, dict) and all(isinstance(k,str) and isinstance(v,str) for k,v in env.items()), "invalid target environment")
        require(all(env.get(k) == active_env.get(k) for k in set(env) | set(active_env)
                    if k.startswith(("AI_", "JEV_", "CMD_"))), "model, policy or budget settings changed")
        require(env.get("AI_QUALITY_APPROVED", "0") != "1", "quality approval enabled")
        for key in ("CRAWLER_AI_SCHEDULING_ENABLED", "CRAWLER_AI_WRITEBACK_ENABLED"):
            require(env.get(key, "false").lower() == "false", "scheduling/writeback enabled")
        proof = receipt["targets"][name]
        require(proof["sourceSHA"] == record["sourceSHA"] and proof["argv"] == record["argv"] and
                proof["cwd"] == record["cwd"] and proof["envSha256"] == sha256(canonical(record["env"])),
                "target launch contract differs from reviewed receipt")
        require(isinstance(proof["inputs"], dict) and proof["inputs"], "target input seals absent")
        paths = []
        for value, expected in proof["inputs"].items():
            path = absolute(value)
            if value not in verified:
                verified[value] = input_digest(path)
            actual = verified[value]
            require(actual == expected, "target artifact or runtime input changed")
            paths.append(path.resolve())
        for value in (record["artifact"], record["exe"], record["cwd"], record["argv"][0]):
            path = absolute(value).resolve(strict=True)
            require(any(path == root or path.is_relative_to(root) for root in paths), "target launch input is not sealed")


def main(argv=None, *, system=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--state", type=Path, required=True)
    parser.add_argument("--metadata", action="append", default=[], metavar="NAME=ABSOLUTE_PATH")
    parser.add_argument("--repository", type=Path)
    parser.add_argument("--target-receipt", type=Path)
    parser.add_argument("--target-receipt-sha256")
    args = parser.parse_args(argv)
    reasons = []
    checked = []

    def check(label, action, reason):
        try:
            action()
            checked.append(label)
            return True
        except Refusal as error:
            reasons.append(dict(check=label, reason=str(error)))
            return False
        except (OSError, ValueError, TypeError, KeyError, AttributeError, StopIteration,
                IndexError, subprocess.SubprocessError, sqlite3.Error):
            reasons.append(dict(check=label, reason=reason))
            return False

    try:
        body = read_file(absolute(args.state))
        state = decode(body)
        require(isinstance(state, dict), "release state must be an object")
    except (OSError, ValueError, TypeError):
        state = None
        reasons.append(dict(check="state", reason="unreadable or invalid release state"))
    if state is not None:
        check("pending", lambda: require("pending" in state and state["pending"] is None, "pending"),
              "unresolved or missing pending launch state")
        check("phase", lambda: require(state["phase"] in ("switching", "awaiting-acceptance", "accepted", "restoring"), "phase"),
              "release phase is not restorable")
        metadata = {}
        def metadata_paths():
            for item in args.metadata:
                key, value = item.split("=", 1)
                require(key in METADATA and key not in metadata, "unknown or duplicate metadata role")
                metadata[key] = absolute(value)
            require(set(metadata) == set(METADATA) == set(state["expectedMetadata"]), "incomplete metadata mapping")
            require(len({p.resolve() for p in metadata.values()}) == len(METADATA), "metadata paths alias")
        mapped = check("metadata-paths", metadata_paths, "all five distinct explicit metadata paths are required")

        def cas():
            for key, path in metadata.items():
                require(read_file(path) == state["expectedMetadata"][key].encode(), "metadata drift")
        if mapped:
            check("metadata-CAS", cas, "metadata drift; never refresh expectedMetadata automatically")
        targets_valid = check("targets", lambda: target_safe(state, args),
                              "rollback target unknown, unsafe, unreviewed or changed; security fixes and matching hold must be preserved")
        complete = check("active-set", lambda: require(set(state["active"]) == set(PORTS), "active set"),
                         "all five active process records are required")
        if complete:
            system = system if system is not None else Linux()
            def protected():
                require(set(state["infrastructure"]) == set(CONTAINERS), "incomplete infrastructure")
                actual = system.containers()
                require(actual == state["infrastructure"] and all(r["running"] is True and r["pid"] > 0 for r in actual.values()), "containers changed")
                identity(system, state["mongodb"], 27017)
                require(budget_digest(absolute(state["budgetPath"])) == state["budgetDigest"], "budget changed")
            check("protected", protected, "protected containers, Mongo identity/listener or shared budget changed/unverifiable")
            for name, port in PORTS.items():
                record = state["active"][name]
                if check(name + "-identity", lambda r=record,p=port: identity(system, r, p),
                         name + " process identity/listener stale, absent or unverifiable"):
                    check(name + "-health", lambda n=name,p=port: require(system.health(n,p), "unhealthy"),
                          name + " is not healthy (Python must also retain the matching/quality hold)")
                    if name == "analytics":
                        check("analytics-idle", lambda: require(system.analytics_idle(record["pid"]), "Spark busy"),
                              "Spark UI ownership or absence of RUNNING jobs is unverifiable")
                    check(name + "-identity-after", lambda r=record,p=port: identity(system, r, p),
                          name + " identity/listener changed during inspection")
            check("protected-after", protected, "protected state changed during inspection or is unverifiable")
        if mapped:
            check("metadata-CAS-after", cas, "metadata drift during inspection; expectedMetadata was not refreshed")
        if targets_valid:
            check("targets-after", lambda: target_safe(state, args), "target evidence changed during inspection")
        check("state-after", lambda: require(read_file(args.state) == body, "state drift"),
              "release state changed during inspection")
    print(json.dumps(dict(status="refused" if reasons else "passed", reasons=reasons,
                          checked=checked, rollbackExecuted=False), ensure_ascii=False))
    return 1 if reasons else 0


if __name__ == "__main__":
    raise SystemExit(main())
