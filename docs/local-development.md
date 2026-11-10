# Local Development

For frontend replacement work, use the [frontend API preservation guide](frontend-api/README.md).
It inventories every current frontend HTTP wrapper, payload type, transport dependency,
backend mismatch and verification boundary before removing the existing UI.

## Requirements

- Java 17 and Maven 3.9.6 or newer.
- Node.js 22.12+, pnpm 11.24.0, and the checked-in `frontend/pnpm-lock.yaml`.
- MySQL 8 with separate `blog_data` and `boss_data` databases, Redis, and MongoDB.
- Python 3.12–3.14 and uv for `ai-service/`; the current local build was verified with Python 3.13.15 and uv 0.9.9.
- RustFS for S3 object storage and either a real SMTP provider or the explicit local Mailpit capture profile.

For a new, empty database only, import `database/system.sql` into `blog_data` and
`database/boss.sql` into `boss_data`. These scripts drop tables; do not run them against
an existing database. They provide schemas, not users or recruitment data.

Supply `BOSSANALYZE_MYSQL_USERNAME`, `BOSSANALYZE_MYSQL_PASSWORD`,
`BOSSANALYZE_REDIS_PASSWORD`, and `BOSSANALYZE_JWT_SECRET` in the environment.
The JWT secret must be a Base64-encoded random key of at least 32 bytes; generate
one with `openssl rand -base64 32` and keep it stable in private configuration.
The optional
`BOSSANALYZE_MONGO_URI` defaults to a loopback MongoDB instance without
authentication. Keep credentials outside the repository.

## Build and Run

From the repository root:

```sh
mvn -f backend/pom.xml install
```

Start each backend in a separate terminal:

```sh
java -Xmx768m -jar backend/api/target/spring-web-0.0.1-SNAPSHOT.jar --spring.profiles.active=local-dev
java -Xmx768m -jar backend/crawler/target/spring-crawler-0.0.1-SNAPSHOT.jar --spring.profiles.active=local-dev
java -Xmx1g -jar backend/analytics/target/spring-spark-0.0.1-SNAPSHOT.jar --spring.profiles.active=local-dev
```

An external properties file can also provide infrastructure credentials; append
`--spring.config.additional-location=file:/absolute/path/application-local.properties`.
Restrict its permissions to the current user. Avoid sharing a datasource URL
between modules: the API and crawler use different databases.

In `frontend/`:

```sh
pnpm install --frozen-lockfile
pnpm dev
```

The frontend uses Nuxt 4 + shadcn-vue, with login, console management, dashboards and
read-only recruitment analytics. Remaining business routes use explicit rebuild placeholders.
Preserved API wrappers and their required types/request client now live under `frontend/app/`.
Nitro routes `/api/v1/boss/**` to Spark on 8082, `/api/v1/ai/**` to the crawler on 8081,
and other `/api/**` requests to the main API on 8080, retaining the `/api` prefix.
These proxies run in development and in the production Node server. `pnpm type-check`,
`pnpm test` and `pnpm build` verify the frontend foundation. To run the production build:

```sh
HOST=127.0.0.1 PORT=5020 pnpm start
```

Deploy the Nuxt `.output/` Node server; static hosting of `.output/public/` alone does not
provide the proxies. See `frontend/README.md` for the preserved browser-only request semantics
and legacy `VITE_` export configuration.

## Local Profile Boundaries

`local-dev` binds application ports to loopback, uses a real local Spark session,
and stores logs and Spark events under the JVM temporary directory. It disables
Consul and automatic AI/log-analysis scheduling. Existing cluster configuration
and default scheduling behavior remain available outside this profile.

Manual log analysis remains available. Enable `spark.logs.scheduling.enabled` only
after supplying access logs and accepting its effects. AI runs through the Python
service described below; Ollama and OpenRouter are no longer the application AI backend.
Keep `crawler.ai.scheduling.enabled=false`, `crawler.ai.writeback-enabled=false` and
`AI_QUALITY_APPROVED=0` until privacy and independent quality approval both pass.

## Check Connectivity

The API's `/actuator/health/readiness` checks application availability, MySQL,
Redis, and MongoDB. `/actuator/health/liveness` checks process availability.
Crawler and Spark expose `/actuator/health`. A failed SMTP connection still
appears in the API's overall health result; local readiness does not test email.

Verify an authenticated user-info request after login; resume API checks are separate
from the still-unbuilt resume UI. Recruitment endpoints return empty arrays for an
empty `boss_data` database. To check actual log processing, trigger analysis via
`POST /api/v1/system/logs/analysis/trigger`, then verify a fresh result at Spark's
`/api/log/access/analysis/complete` and a heatmap through the frontend gateway.
An initial `204` means no analysis result has been cached yet.

SMTP, object storage, and external resume export/statistics services require
their own configuration. Unavailable dashboard measurements remain unavailable rather than being filled
with sample values. A successful HTTP response does not verify model quality or external delivery.

## Offline geolocation and object storage

Analytics uses an optional external read-only DB-IP Lite City MMDB, configured by
`GEOLOCATION_MMDB_PATH` (or `geolocation.mmdb-path`). No online geolocation key is
required; absent or invalid libraries leave locations unknown without blocking startup.
See the [offline geolocation guide](operations/offline-geolocation.md) for attribution,
trusted proxy configuration and validation.

The [RustFS migration guide](operations/rustfs-migration.md) covers the isolated target,
S3 checks, backup gates, reconciliation and rollback. For this local deployment, the
user confirmed there was no old MinIO data; RustFS was explicitly activated and its
HTTP image path verified. Existing installations with data must still perform backup
and reconciliation before switching. The MinIO Java SDK and `minio.endpoint`,
`minio.accessKey`, `minio.secretKey`, `minio.bucketName` configuration names are retained;
point them at the RustFS endpoint with a restricted application identity. See the
[activation record](operations/rustfs-activation-20261108.md).

## Python AI service and privacy boundary

Create the service environment from the committed lockfile:

```sh
uv sync --frozen --project ai-service
(cd ai-service && uv run --frozen pytest)
```

Supply `CMD_API_KEY`, `CMD_API_BASE`, `JEV_MODEL=typesafe/jev-1.14.0`,
`AI_SERVICE_TOKEN`, `CMD_ZDR=0`, `AI_QUALITY_APPROVED=0`, `AI_DAILY_TOKENS`
and an absolute private `AI_BUDGET_PATH` outside the source tree. Reuse existing
approved values and budget files; do not generate a replacement token or reset a ledger
on restart. The Java crawler needs the same internal token/model and shared
`AI_MAX_INPUT_BYTES`, `AI_MAX_JOBS`, `AI_MAX_CANDIDATES` limits. Budget, concurrency
and provider retry settings belong to Python. No private value belongs in `VITE_*`
or frontend public configuration.

```sh
# After injecting private configuration into this terminal, without printing it:
(cd ai-service && uv run --frozen uvicorn app.main:app --host 127.0.0.1 --port 8090)
```

The crawler uses `AI_SERVICE_URL=http://127.0.0.1:8090`. User Bearer tokens stay on
the authenticated Java API chain; the dedicated internal token protects Python.
`/health/ready` means configuration readiness, **not provider availability or model quality**.
Empty-fact analysis and empty matching requests can be checked without inference.

As checked on 2026-11-09, [CommandCode's Provider API documentation](https://commandcode.ai/docs/provider#zero-data-retention-zdr)
explicitly says both `typesafe/jev` and `typesafe/jev-1.14.0` have no ZDR-capable
upstream. `x-cmd-zdr: 1` therefore returns `422/cmd_zdr_no_providers`. The application
fails closed and must not send real resumes by dropping that header. Do not interpret
successful synthetic HTTP fixtures as evidence that this external restriction is fixed.
The offline gold evaluator rejects missing/pending labels and detectable split leakage;
AI-generated or synthetic annotations cannot establish independent human quality approval.

Before using existing recruitment data for analysis, back up the database and rehearse
`database/migrations/20261108_job_ai_analysis.sql` in a separate restored schema, then
apply it explicitly to `boss_data`. It adds the analysis sidecar without replacing
existing job rows. Startup never applies it automatically. The migration is repeatable,
but a pre-existing table still needs a schema/index compatibility check. Role uniqueness
and token timestamp changes have separate migration instructions; do not blindly run
all files or import the destructive snapshots.

See the [deployment/recovery evidence](operations/final-crawler-python-deployment-20261109.md)
and the [controlled workflow](operations/controlled-workflow-20261108.md) for the exact
local and synthetic acceptance boundaries. Normal application launch never creates demo
users, jobs, model outputs or approved quality labels.

The default/local Spring profiles continue to externalize the Redis password. The local
profile supports an explicitly passwordless loopback Redis when no password is configured;
this is separate from authenticating to a protected Redis instance. JWT signing requires
a stable private Base64 key of at least 32 decoded bytes.

## Credential contract verification

The focused Spring contracts compile the real production services and use only
synthetic credentials and stubbed HTTP. See
[`tools/credential-contract-tests/README.md`](../tools/credential-contract-tests/README.md)
for dependency preparation and the offline test command. They do not validate
live API keys, historical secret revocation or a full application deployment.

## SMTP and JWT configuration validation

The default API profile reads `BOSSANALYZE_SMTP_USERNAME`, `BOSSANALYZE_SMTP_PASSWORD`
and `BOSSANALYZE_JWT_SECRET`; the corresponding Spring properties can also be
provided by private external configuration. No secret is generated at startup.
JWT configuration is validated at service initialization: a standard Base64 key
of at least 32 decoded bytes is required. Keep the existing approved key stable;
changing it is a separately coordinated rotation that may invalidate sessions.
The existing `local-dev` JWT expression and all token expiration/refresh logic
are unchanged. Historical credential revocation is not established by this fix.

SMTP credentials are validated before either EmailService send method, not at
application initialization, preserving startup without SMTP for local development.
Missing, blank, unresolved placeholders and `your_` example values fail with a
fixed message before message creation/send. Other consumers such as SMTP health
checks remain independent; this is not provider authentication or TLS validation.

### Jev 数据保留策略更新（2026-11-10）

用户已取消强制零数据保留要求。普通 Jev 请求使用 `CMD_ZDR=0`（也是代码默认值）；开启 `CMD_ZDR=1` 仍严格要求 ZDR 上游，不自动降级。普通路由按供应商实际保留条款处理，不是本机离线推理。此变更不解除简历匹配暂停、正式质量、调度/写回和预算限制。部署时同步更新当前服务与重启恢复清单，避免重启后恢复旧的强制 ZDR 配置。
