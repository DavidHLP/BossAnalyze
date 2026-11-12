"""CLI contract tests; all process/network observations are isolated substitutes."""
import contextlib
import hashlib
import importlib.util
import io
import json
import os
import sys
from pathlib import Path
import tempfile
import subprocess
from types import SimpleNamespace
import unittest
from unittest.mock import patch


SCRIPT = Path(__file__).with_name("preflight.py")
BASELINE = "0dcb7b6fb0532e4c652a5d3050b5524d25bc7052"
PORTS = dict(python=8090, crawler=8081, api=8080, analytics=8082, frontend=5020)
CONTAINERS = ("bossanalyze-local-mysql-restored", "bossanalyze-local-redis-restored",
              "bossanalyze-store-20261108", "bossanalyze-mailpit-issue39")


def load_cli():
    spec = importlib.util.spec_from_file_location("preflight", SCRIPT)
    cli = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(cli)
    return cli


def digest(value):
    return hashlib.sha256(value).hexdigest()


class PreflightTest(unittest.TestCase):
    def fixture(self, folder):
        root = Path(folder)
        target = root / "target"
        target.mkdir()
        artifact = target / "program"
        artifact.write_bytes(b"synthetic sealed artifact, never executable")
        env = {"JEV_MODEL": "typesafe/jev-3.1.0", "AI_QUALITY_APPROVED": "0"}
        records = {name: dict(pid=100 + i, starttime=str(900 + i), exe=str(artifact),
                              argv=[str(artifact)], cwd=str(target), env=env.copy(),
                              port=port, name=name, artifact=str(artifact), sourceSHA=BASELINE)
                   for i, (name, port) in enumerate(PORTS.items())}
        mongo = dict(pid=200, starttime="800", exe="/synthetic/mongo", argv=["mongo"], cwd="/")
        infrastructure = {name: dict(id="container-" + name, pid=300 + i, running=True,
                                     startedAt="2026-11-12T00:00:00Z", ports={})
                          for i, name in enumerate(CONTAINERS)}
        budget = root / "budget.sqlite3"
        import sqlite3
        with contextlib.closing(sqlite3.connect(budget)) as db:
            db.execute("CREATE TABLE usage (tokens INTEGER)")
            db.execute("INSERT INTO usage VALUES (7)")
            db.commit()
            budget_digest = digest("\n".join(db.iterdump()).encode())
        metadata = {}
        for key in ("current", "manifest", "session", "pair-state", "config"):
            path = root / (key + ".json")
            path.write_text('{"fixture":true}\n')
            metadata[key] = path
        state = dict(phase="accepted", pending=None, expectedMetadata={k:p.read_text() for k,p in metadata.items()},
                     active=records, old={k:dict(v) for k,v in records.items()}, mongodb=mongo,
                     infrastructure=infrastructure, budgetPath=str(budget), budgetDigest=budget_digest)
        state_path = root / "state.json"
        state_path.write_text(json.dumps(state))
        tree_digest = digest(json.dumps([["program", digest(artifact.read_bytes())]], separators=(",", ":")).encode())
        receipt = dict(schemaVersion=1, safetyBaseline=BASELINE, targets={
            name: dict(sourceSHA=BASELINE, argv=r["argv"], cwd=r["cwd"],
                       envSha256=digest(json.dumps(env, sort_keys=True, separators=(",", ":")).encode()),
                       inputs={str(target):tree_digest}) for name,r in records.items()})
        receipt_path = root / "reviewed-targets.json"
        receipt_path.write_text(json.dumps(receipt))
        args = ["--state", str(state_path), "--repository", str(SCRIPT.parents[2]),
                "--target-receipt", str(receipt_path), "--target-receipt-sha256", digest(receipt_path.read_bytes())]
        for key,path in metadata.items():
            args.extend(["--metadata", key + "=" + str(path)])
        observed = json.loads(json.dumps([*records.values(), mongo]))
        containers = json.loads(json.dumps(infrastructure))
        host = SimpleNamespace(
            process=lambda pid: next(r for r in observed if r["pid"] == pid),
            listeners=lambda port: {mongo["pid"]} if port == 27017 else {records[next(n for n,p in PORTS.items() if p == port)]["pid"]},
            containers=lambda: containers,
            health=lambda name,port: True,
            analytics_idle=lambda pid: True)
        return SimpleNamespace(root=root, state=state, state_path=state_path, args=args, host=host,
                               metadata=metadata, receipt=receipt, receipt_path=receipt_path)

    def invoke(self, fixture):
        output = io.StringIO()
        with contextlib.redirect_stdout(output):
            code = load_cli().main(fixture.args, system=fixture.host)
        return code, json.loads(output.getvalue())

    def save_state(self, fixture):
        fixture.state_path.write_text(json.dumps(fixture.state))

    def pin_receipt(self, fixture):
        fixture.receipt_path.write_text(json.dumps(fixture.receipt))
        fixture.args[fixture.args.index("--target-receipt-sha256") + 1] = digest(fixture.receipt_path.read_bytes())

    def test_healthy_bound_live_state_and_reviewed_target_pass_without_writes(self):
        with tempfile.TemporaryDirectory() as folder:
            fixture = self.fixture(folder)
            before = {p.relative_to(folder):p.read_bytes() for p in Path(folder).rglob("*") if p.is_file()}
            code, report = self.invoke(fixture)
            self.assertEqual((code, report["status"]), (0, "passed"), report)
            self.assertFalse(report["rollbackExecuted"])
            after = {p.relative_to(folder):p.read_bytes() for p in Path(folder).rglob("*") if p.is_file()}
            self.assertEqual(before, after)

    def test_default_linux_cli_uses_only_read_observations(self):
        with tempfile.TemporaryDirectory() as folder:
            fixture = self.fixture(folder)
            pid = os.getpid()
            proc = Path("/proc") / str(pid)
            process = dict(pid=pid, starttime=(proc/"stat").read_text().rsplit(")", 1)[1].split()[19],
                           exe=os.readlink(proc/"exe"), cwd=os.readlink(proc/"cwd"),
                           argv=(proc/"cmdline").read_bytes().rstrip(b"\0").decode().split("\0"))
            for record in [*fixture.state["active"].values(), fixture.state["mongodb"]]:
                record.update(process)
            fixture.state_path.write_text(json.dumps(fixture.state))
            real_run = subprocess.run
            ready = dict(status="ready", resumeMatching=dict(enabled=False),
                         qualityGate=dict(calibrated=False, automaticWritebackAllowed=False))
            running_jobs = []
            def command(args, **kwargs):
                if args[0] == "git":
                    return real_run(args, **kwargs)
                if args[:2] == ["docker", "inspect"]:
                    return SimpleNamespace(stdout=json.dumps(fixture.state["infrastructure"][args[-1]]).encode())
                self.assertEqual(args[:4], ["ss", "-H", "-ltnp", "sport"])
                return SimpleNamespace(stdout=f'LISTEN 0 128 127.0.0.1:8090 *:* users:(("synthetic",pid={pid},fd=3))\n'.encode())
            def response(request, **kwargs):
                url = request if isinstance(request, str) else request.full_url
                if url.endswith("/health/ready"):
                    body = ready
                elif url.endswith("/applications"):
                    body = [dict(id="app-synthetic")]
                elif url.endswith("/jobs"):
                    body = running_jobs
                else:
                    body = dict(status="UP")
                stream = io.BytesIO(json.dumps(body).encode())
                stream.status = 200
                return stream
            before = {p.relative_to(folder):p.read_bytes() for p in Path(folder).rglob("*") if p.is_file()}
            output = io.StringIO()
            with patch("subprocess.run", side_effect=command), \
                 patch("urllib.request.OpenerDirector.open", side_effect=response), \
                 contextlib.redirect_stdout(output):
                code = load_cli().main(fixture.args)
            self.assertEqual((code, json.loads(output.getvalue())["status"]), (0, "passed"), output.getvalue())
            self.assertEqual(before, {p.relative_to(folder):p.read_bytes() for p in Path(folder).rglob("*") if p.is_file()})
            for gate, mutate in (
                ("python-health", lambda:ready["resumeMatching"].update(enabled=True)),
                ("python-health", lambda:ready["qualityGate"].update(calibrated=True)),
                ("python-health", lambda:ready["qualityGate"].update(automaticWritebackAllowed=True)),
                ("analytics-idle", lambda:running_jobs.append(dict(status="RUNNING"))),
            ):
                with self.subTest(gate=gate):
                    ready["resumeMatching"]["enabled"] = False
                    ready["qualityGate"].update(calibrated=False, automaticWritebackAllowed=False)
                    running_jobs.clear()
                    mutate()
                    output = io.StringIO()
                    with patch("subprocess.run", side_effect=command), \
                         patch("urllib.request.OpenerDirector.open", side_effect=response), \
                         contextlib.redirect_stdout(output):
                        code = load_cli().main(fixture.args)
                    self.assertEqual(code, 1)
                    self.assertIn(gate, [r["check"] for r in json.loads(output.getvalue())["reasons"]])

    def test_pending_is_refused_without_changing_input_or_disclosing_it(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "state.json"
            body = b'{"pending":{"secret":"NEVER-PRINT-THIS"}}'
            path.write_bytes(body)
            cli = load_cli()
            output = io.StringIO()
            with contextlib.redirect_stdout(output):
                code = cli.main(["--state", str(path)])
            self.assertEqual(code, 1)
            self.assertEqual(json.loads(output.getvalue())["status"], "refused")
            self.assertIn("pending", output.getvalue())
            self.assertNotIn("NEVER-PRINT-THIS", output.getvalue())
            self.assertEqual(path.read_bytes(), body)
            self.assertEqual([p.name for p in Path(folder).iterdir()], ["state.json"])

    def test_reviewed_target_cannot_change_model_budget_or_enable_quality(self):
        for key, value in (("JEV_MODEL", "typesafe/jev-9.9.9"), ("AI_DAILY_TOKENS", "999999999"),
                           ("AI_QUALITY_APPROVED", "1")):
            with self.subTest(setting=key), tempfile.TemporaryDirectory() as folder:
                fixture = self.fixture(folder)
                env = dict(fixture.state["old"]["python"]["env"], **{key:value})
                fixture.state["old"]["python"]["env"] = env
                fixture.receipt["targets"]["python"]["envSha256"] = digest(json.dumps(env, sort_keys=True, separators=(",", ":")).encode())
                self.save_state(fixture)
                self.pin_receipt(fixture)
                code, report = self.invoke(fixture)
                self.assertEqual(code, 1, report)
                self.assertIn("targets", [r["check"] for r in report["reasons"]])

    def test_refusal_matrix_never_writes_or_refreshes_cas(self):
        def stale(f, field, value):
            f.state["active"]["python"][field] = value
        def budget_changed(f):
            import sqlite3
            with contextlib.closing(sqlite3.connect(f.state["budgetPath"])) as db:
                db.execute("UPDATE usage SET tokens=8")
                db.commit()
        cases = [
            ("pending", lambda f:f.state.update(pending={"phase":"unknown"}), "pending"),
            ("phase", lambda f:f.state.update(phase="prepared"), "phase"),
            ("CAS", lambda f:f.metadata["current"].write_text('{"drift":true}'), "metadata-CAS"),
            ("PID", lambda f:stale(f,"pid",9999), "python-identity"),
            ("starttime", lambda f:stale(f,"starttime","9999"), "python-identity"),
            ("exe", lambda f:stale(f,"exe","/unowned/program"), "python-identity"),
            ("argv", lambda f:stale(f,"argv",["/unowned/program"]), "python-identity"),
            ("cwd", lambda f:stale(f,"cwd","/unowned"), "python-identity"),
            ("listener", lambda f:setattr(f.host,"listeners",lambda port:{9999}), "python-identity"),
            ("container", lambda f:f.state["infrastructure"][CONTAINERS[0]].update(id="stale"), "protected"),
            ("Mongo", lambda f:f.state["mongodb"].update(starttime="9999"), "protected"),
            ("budget", budget_changed, "protected"),
            ("WAL", lambda f:Path(f.state["budgetPath"]+"-wal").write_bytes(b"uncheckpointed"), "protected"),
            ("health", lambda f:setattr(f.host,"health",lambda name,port:False), "python-health"),
            ("Spark", lambda f:setattr(f.host,"analytics_idle",lambda pid:False), "analytics-idle"),
            ("unknown source", lambda f:f.state["old"]["python"].update(sourceSHA="unknown"), "targets"),
            ("unsafe source", lambda f:f.state["old"]["python"].update(sourceSHA="6aa3b079ffa712a3b4354f21dbbea4086d7bac23"), "targets"),
            ("receipt pin", lambda f:f.receipt_path.write_text('{}'), "targets"),
            ("artifact", lambda f:(f.root/"target/program").write_bytes(b"changed"), "targets"),
            ("unsealed symlink", lambda f:(f.root/"target/link").symlink_to("program"), "targets"),
            ("missing target", lambda f:f.state["old"].pop("api"), "targets"),
            ("missing active", lambda f:f.state["active"].pop("api"), "active-set"),
            ("null active", lambda f:f.state["active"].update(api=None), "api-identity"),
        ]
        for name, mutate, gate in cases:
            with self.subTest(case=name), tempfile.TemporaryDirectory() as folder:
                fixture = self.fixture(folder)
                mutate(fixture)
                self.save_state(fixture)
                before = {p.relative_to(folder):p.read_bytes() for p in Path(folder).rglob("*") if p.is_file()}
                code, report = self.invoke(fixture)
                self.assertEqual(code, 1, report)
                self.assertIn(gate, [r["check"] for r in report["reasons"]], report)
                self.assertFalse(report["rollbackExecuted"])
                self.assertEqual(before, {p.relative_to(folder):p.read_bytes() for p in Path(folder).rglob("*") if p.is_file()})

    def test_state_and_metadata_changes_during_health_are_refused(self):
        for gate in ("state-after", "metadata-CAS-after"):
            with self.subTest(gate=gate), tempfile.TemporaryDirectory() as folder:
                fixture = self.fixture(folder)
                def health(name, port):
                    if gate == "state-after":
                        fixture.state_path.write_text('{"pending":"external-change"}')
                    else:
                        fixture.metadata["session"].write_text('{"external-change":true}')
                    return True
                fixture.host.health = health
                code, report = self.invoke(fixture)
                self.assertEqual(code, 1)
                self.assertIn(gate, [r["check"] for r in report["reasons"]])

    def test_artifact_changed_during_inspection_is_refused(self):
        with tempfile.TemporaryDirectory() as folder:
            fixture = self.fixture(folder)
            def health(name, port):
                (fixture.root / "target/program").write_bytes(b"changed after initial target check")
                return True
            fixture.host.health = health
            code, report = self.invoke(fixture)
            self.assertEqual(code, 1, report)
            self.assertIn("targets-after", [r["check"] for r in report["reasons"]])

    def test_invalid_json_cli_exit_is_sanitized_and_does_not_create_files(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "state.json"
            for body in (b'{"pending":null,"pending":"PRIVATE"}', b'{"secret":"PRIVATE",', b'[]', b'{"pending":NaN}'):
                with self.subTest(body=body):
                    path.write_bytes(body)
                    result = subprocess.run([sys.executable, "-B", str(SCRIPT), "--state", str(path)], capture_output=True)
                    self.assertEqual(result.returncode, 1)
                    self.assertEqual(json.loads(result.stdout)["status"], "refused")
                    self.assertNotIn(b"PRIVATE", result.stdout + result.stderr)
                    self.assertEqual(result.stderr, b"")
                    self.assertEqual(path.read_bytes(), body)
                    self.assertEqual([p.name for p in Path(folder).iterdir()], ["state.json"])


if __name__ == "__main__":
    unittest.main()
