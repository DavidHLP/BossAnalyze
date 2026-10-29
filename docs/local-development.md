# Local Development

## Requirements

- Java 17 and Maven 3.9.6 or newer.
- Node.js 20.19 or 22+, Yarn 1.x, and the checked-in `vue/yarn.lock`.
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
yarn install --frozen-lockfile
yarn dev --host 127.0.0.1 --port 5020 --strictPort
```

The frontend routes `/api/v1/boss` to Spark on 8082, `/api/v1/ai` to the crawler
on 8081, and other `/api` requests to the main API on 8080. Request paths retain
their `/api` prefix. `yarn build` includes TypeScript checking and image
optimization; native image dependencies may require a C/C++ toolchain and NASM.
The Node configuration skips incompatible third-party declaration checking for
legacy Vite plugins; application TypeScript checks still run during the build.

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

After logging in through the frontend, verify an authenticated user-info
request and the resume list. Recruitment endpoints return empty arrays for an
empty `boss_data` database. To check actual log processing, trigger analysis via
`POST /api/v1/system/logs/analysis/trigger`, then verify a fresh result at Spark's
`/api/log/access/analysis/complete` and a heatmap through the frontend gateway.
An initial `204` means no analysis result has been cached yet.

SMTP, object storage, and external resume export/statistics services require
their own configuration. Existing dashboard performance/security metrics contain
sample values; a successful HTTP response does not verify those calculations.
