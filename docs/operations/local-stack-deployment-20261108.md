# Local Nuxt / crawler / analytics / Python deployment — 2026-11-08

Local deployment only, source `58aa2dbbf52769688aa570a37ce95f49cef6f76e`. This worker owns only this report; no source edits, delegation, push, PR, browser or real AI call. API 8080 was deployed by another worker and was never restarted: PID 2125922 / Linux starttime 5753450, aggregate health 200/UP throughout final verification.

## Runtime and immutable artifacts

Private deployment root (`R` below):
`/home/david/.local/share/bossanalyze/stack-deployment-20261108` (0700).

| Component | Port | PID / starttime | Artifact relative to R | Observed health |
| --- | --- | --- | --- | --- |
| Nuxt production | 5020 | 2239333 / 5995639 | `latest/frontend-pnpm1124/.output/server/index.mjs` | GET / 200 |
| crawler | 8081 | 2232981 / 5986464 | `latest/crawler.jar` | actuator health 200/UP |
| analytics | 8082 | 2233357 / 5986860 | `latest/analytics.jar` | actuator health 200/UP |
| Python FastAPI | 8090 | 2231337 / 5984958 | `latest/ai-service` | live/ready 200; configuration only |

Nuxt's full immutable `.output` directory for parent-controlled display tests is
`/home/david/.local/share/bossanalyze/stack-deployment-20261108/latest/frontend-pnpm1124/.output`.
Do not rebuild or replace it while running. No final service runs from target or a worktree. Python uses a copied locked venv and copied app; its interpreter resolves to the existing uv-managed installation, which must also be preserved. Nuxt uses installed Node 26.7.0; Java uses Zulu 17.68.203.0. Service cwd, full argv and original environment were read programmatically and retained privately, never printed. Inspect safe metadata using `current.json`; it is not safe to dump original-processes.json or logs.

## Actual builds and tests

- Java: explicit installed Java17/Maven3.9.16, `mvn -f backend/pom.xml -pl crawler,analytics -am verify -Dgeo.validation.mmdb=/tmp/boss-geo-official-20261108/dbip.mmdb`; official MMDB existed. Exit0, commons15 + crawler41 + analytics12 = **68 tests**, failures0/errors0/skipped0. No skip counted as pass. Artifacts copied after build.
- Frontend: Node26.7.0, pinned `npm exec --yes --package=pnpm@11.24.0 -- pnpm <command>` in frontend; frozen install, type-check, test, build all exit0. **68 tests**, pass68/fail0/skipped0. Initial installed pnpm11.25.0 also passed; all required checks repeated with11.24.0 and its output is the final deployed artifact.
- Python: `uvx --from uv==0.9.9 uv sync --frozen --project ai-service`; then in ai-service `uvx --from uv==0.9.9 uv run --frozen python -m pytest -q` and `python -m compileall -q app` through the same uv runner. All exit0, **69 passed**. These are synthetic/offline tests, not real model quality or provider acceptance.
- Private evidence: `maven.log`, `frontend-pinned-{install,type-check,test,build}.log`, `pnpm-version.log`, `python-{sync,check}.log`; Java Surefire XML remains in this worktree. No new repository tests or dependencies.

## Deployment and verification

Each latest artifact was started on an isolated loopback canary before switching. Latest canaries: frontend15020 and final pinned15022, crawler18081, analytics18082, Python18090; all healthy and stopped. Original-path artifact snapshots also independently started: frontend15021, crawler18083, analytics18084; healthy and stopped. These prove preserved artifact startup, not original in-memory byte identity of a previously mutable crawler target path.

Initial Nuxt canary inherited NITRO_PORT=5020 and failed EADDRINUSE without affecting the live service. Private controller now sets both PORT/NITRO_PORT and HOST/NITRO_HOST explicitly. First probe controller was stopped; its queued Python canary completed and cleaned up. Final owned canary ports all verified closed. No SIGKILL used.

Spark4040 API reported 0 RUNNING, 103 SUCCEEDED, 0 FAILED before switching; the controller checked again immediately before stopping analytics. Production switching used exact PID/starttime guards and SIGTERM, retained original argv/env/cwd with only versioned artifact, necessary port and safety overrides. Original processes: Nuxt742303/1612426, crawler429643/910227, analytics739675/1606232. Never stopped API, database, Mailpit or RustFS.

Final `verification.json` and `readonly.json` under R record:

- API/crawler/analytics health200, Python ready200. Nuxt GET /200 and same-origin API routing retained. Python live200 was also checked separately at closeout.
- Synthetic existing QA login200, own basic-info200, logout200; revoked token401. Credentials/token stayed in memory. No real resume read.
- Authenticated existing city-list GET directly on8082 and through5020 both200; position-list through5020 also200. Response business contents not printed or stored. Anonymous versions401.
- Anonymous v1/v2 AI POST via5020 each401; Python internal anonymous401 and authenticated empty-invalid payload422. No valid inference payload, provider request, fixture transport injected into production, or matching request was used.
- Final startup log classification: analytics/Python/Nuxt no ERROR-level lines; crawler one ERROR from WebDriverConfig because existing ChromeDriver path is missing/non-executable. Health does not establish browser collection readiness. No browser or collection was started; real BOSS login/driver setup remains parent/user work.

## Safety, dependencies and remaining acceptance

Nuxt retains same-origin production proxies: v1/v2 AI to8081 and other API to8080; recruitment is routed by the existing API gateway to analytics. No proxy/source/security change. Existing infrastructure connections reused; health and authenticated queries establish bounded live dependency evidence, not every endpoint or production availability.

Private `python-env.json` (0600) copies existing 0600 identity-env.json into a new runtime overlay, explicitly sets CMD_ZDR=1 and AI_QUALITY_APPROVED=0, and retains official endpoint, fixed `typesafe/jev-1.14.0`, original internal token, original budget path and cap230000. Original identity profile untouched. Live crawler/Python token/model equality checked without printing. Ledger `/home/david/.local/share/bossanalyze/jev-20261108/acceptance-157ee31/budget.sqlite3` retains UTC day2026-11-09 used222121, remaining7879; unchanged after checks. Never reset ledger or increased cap. Quality/writeback and AI scheduling remain false, log-analysis scheduling false. No enabled collection schedules. No Ollama/OpenRouter requests or fallback enabled.

Python ready means valid configuration with ZDR requested. It does not call/prove provider availability or quality. Prior handoff reports official ZDR422/cmd_zdr_no_providers mapped to503/zdr_unavailable; this worker did not replay a real request. Preserve that failed-closed behavior; no mock production inference or non-ZDR retry. If config is unsafe/missing, leave AI unavailable503.

No seed, DDL, DML, cache flush, account change or schema migration. Authentication has normal session/log side effects; no whole-database before/after equivalence claimed. Missing job_ai_analysis may retain current legacy read fallback; no new writer/scheduler enabled. Future DBA-reviewed migration command (NOT EXECUTED here), after isolated validation and selecting the correct empty/approved schema with private authentication:

```sh
mysql --defaults-extra-file=/absolute/private/mysql-client.cnf boss_data < database/migrations/20261108_job_ai_analysis.sql
```

This is an explicit future schema write requiring its own reviewed target; do not run it as startup or rollback. Model quality, real provider availability, BOSS collection, holdout ranking/performance and production acceptance remain unverified.

## Cold start, rollback and private manifest

`R/control.py` is the private operational controller (0600), run with python3. It reconstructs complete argv/env from private originals and overlay, checks ports and identity, waits for health, and records replacement PIDs in current.json. Commands below are operational, not printed secrets:

```sh
R=/home/david/.local/share/bossanalyze/stack-deployment-20261108
python3 "$R/control.py" probe frontend latest 15022
python3 "$R/control.py" probe crawler latest 18081
python3 "$R/control.py" probe analytics latest 18082
python3 "$R/control.py" probe python latest 18090
# Switch/restart exactly one owned component; short outage on that port:
python3 "$R/control.py" deploy frontend
python3 "$R/control.py" deploy crawler
python3 "$R/control.py" deploy analytics
python3 "$R/control.py" deploy python
# Roll back selected Java/Nuxt service; originals retained under R/rollback:
python3 "$R/control.py" rollback frontend
python3 "$R/control.py" rollback crawler
python3 "$R/control.py" rollback analytics
# Python had no prior8090 listener: rollback is stop, preserving app/env/ledger:
python3 "$R/control.py" stop python
```

Deploy also cold-starts a service when its recorded process is absent, using the preserved complete environment. A reused PID with a different starttime causes refusal; never remove that guard to restart. After intentional stop via controller, deploy starts it again. All mutable process records must then be synced to the master manifest.

Rollback probes tested old artifacts without interrupting final ports. Failure restoration exists in controller but was not failure-injected; it does not prove all recovery branches. Analytics operations refuse when Spark status unavailable/active. Keep final processes running; do not run commands merely to demonstrate rollback again.

Master manifest:
`/home/david/.local/share/bossanalyze/local-dev/final-runtime-e6df9cf38b/manifest.json`.
Backups in R: `manifest-before.json` and immediate `manifest-preupdate.json`. Update preserved all unrelated top-level fields and process entries, including exact api-final, Mailpit, RustFS/database metadata; updated frontend-final/analytics-final, added crawler-final/python-final and localStackDeployment20261108 with source/artifact/cleanup/rollback pointers. After future control operations back up and update corresponding master entries from current.json. Historical manifest health/revision fields remain historical; use new section/current.json for this deployment. Do not print the manifest's private argv.
