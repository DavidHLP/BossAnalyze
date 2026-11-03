# Local Development

For frontend replacement work, use the [frontend API preservation guide](frontend-api/README.md).
It inventories every current frontend HTTP wrapper, payload type, transport dependency,
backend mismatch and verification boundary before removing the existing UI.

## Requirements

- Java 17 and Maven 3.9.6 or newer.
- Node.js 22.12+, pnpm 11.24.0, and the checked-in `vue/pnpm-lock.yaml`.
- MySQL 8 with separate `blog_data` and `boss_data` databases, Redis, and MongoDB.

For a new, empty database only, import `system.sql` into `blog_data` and
`boss.sql` into `boss_data`. These scripts drop tables; do not run them against
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
mvn -f JDK17SERVE/pom.xml install
```

Start each backend in a separate terminal:

```sh
java -Xmx768m -jar JDK17SERVE/spring/target/spring-web-0.0.1-SNAPSHOT.jar --spring.profiles.active=local-dev
java -Xmx768m -jar JDK17SERVE/crawler/target/spring-crawler-0.0.1-SNAPSHOT.jar --spring.profiles.active=local-dev
java -Xmx1g -jar JDK17SERVE/spark/target/spring-spark-0.0.1-SNAPSHOT.jar --spring.profiles.active=local-dev
```

An external properties file can also provide infrastructure credentials; append
`--spring.config.additional-location=file:/absolute/path/application-local.properties`.
Restrict its permissions to the current user. Avoid sharing a datasource URL
between modules: the API and crawler use different databases.

In `vue/`:

```sh
pnpm install --frozen-lockfile
pnpm dev
```

The frontend is a fresh Nuxt 4 + shadcn-vue starter; business screens still need rebuilding.
Preserved API wrappers and their required types/request client now live under `vue/app/`.
Nitro routes `/api/v1/boss/**` to Spark on 8082, `/api/v1/ai/**` to the crawler on 8081,
and other `/api/**` requests to the main API on 8080, retaining the `/api` prefix.
These proxies run in development and in the production Node server. `pnpm type-check`,
`pnpm test` and `pnpm build` verify the frontend foundation. To run the production build:

```sh
HOST=127.0.0.1 PORT=5020 pnpm start
```

Deploy the Nuxt `.output/` Node server; static hosting of `.output/public/` alone does not
provide the proxies. See `vue/README.md` for the preserved browser-only request semantics
and legacy `VITE_` export configuration.

## Local Profile Boundaries

`local-dev` binds application ports to loopback, uses a real local Spark session,
and stores logs and Spark events under the JVM temporary directory. It disables
Consul and automatic AI/log-analysis scheduling. Existing cluster configuration
and default scheduling behavior remain available outside this profile.

Manual log analysis remains available. Enable `spark.logs.scheduling.enabled`
after supplying access logs. Enable `crawler.ai.scheduling.enabled` only after
configuring Ollama and recruitment data. The configured AI model is `qwen3:4b`;
starting the crawler does not install it.

## Check Connectivity

The API's `/actuator/health/readiness` checks application availability, MySQL,
Redis, and MongoDB. `/actuator/health/liveness` checks process availability.
Crawler and Spark expose `/actuator/health`. A failed SMTP connection still
appears in the API's overall health result; local readiness does not test email.

When the replacement login UI is implemented, verify an authenticated user-info
request and the resume list. Recruitment endpoints return empty arrays for an
empty `boss_data` database. To check actual log processing, trigger analysis via
`POST /api/v1/system/logs/analysis/trigger`, then verify a fresh result at Spark's
`/api/log/access/analysis/complete` and a heatmap through the frontend gateway.
An initial `204` means no analysis result has been cached yet.

SMTP, object storage, and external resume export/statistics services require
their own configuration. Existing dashboard performance/security metrics contain
sample values; a successful HTTP response does not verify those calculations.

## External API credentials


The OpenRouter crawler service requires `OPENROUTER_API_KEY` (or the Spring
property `openrouter.api.key`) when instantiated. It is initialized lazily so
the default Ollama workflow does not require an OpenRouter credential. The Spark IP geolocation service requires
`IPGEOLOCATION_API_KEY` (or `ipgeolocation.api.key`). Both services reject
missing or blank keys during initialization, before sending any API request.
Supply credentials through environment variables or an ignored local profile;
never place real values in tracked files.

The Spring and Spark default profiles read `BOSSANALYZE_REDIS_PASSWORD`
without a tracked password literal. This is configuration externalization, not
Redis fail-fast validation: Spring Boot binding can retain an unresolved
placeholder when the variable is missing, and accepts explicit empty or
whitespace values. The Java API credentials above and the JWT signing key have initialization
validation; SMTP credentials are checked before sending, not at startup.

The existing tracked `local-dev` profiles retain
`${BOSSANALYZE_REDIS_PASSWORD:}` and therefore preserve passwordless local Redis
when the variable is unset. Their behavior is unchanged. Supply appropriate
credentials for a password-protected Redis instance and verify its connection
separately; this change does not authenticate against Redis.


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
