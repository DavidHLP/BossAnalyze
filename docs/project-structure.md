# Project structure

Directories describe responsibilities rather than a framework or Java version.
The repository remains one Nuxt application and one Java 17 Maven reactor;
this change does not split services or alter business behavior.

| Previous path | Current path | Responsibility |
| --- | --- | --- |
| `JDK17SERVE/` | `backend/` | Maven reactor and dependency management |
| `JDK17SERVE/commons/` | `backend/commons/` | Shared models and infrastructure utilities |
| `JDK17SERVE/spring/` | `backend/api/` | Business APIs, authentication and administration |
| `JDK17SERVE/crawler/` | `backend/crawler/` | Recruitment collection and AI enrichment |
| `JDK17SERVE/spark/` | `backend/analytics/` | Recruitment and access-log analytics |
| `vue/` | `frontend/` | Nuxt frontend, request client and isolated tests |
| `boss.sql`, `system.sql` | `database/boss.sql`, `database/system.sql` | Destructive initialization snapshots for empty databases only |
| `image/README/` | `docs/images/` | Historical UI screenshots |

## Compatibility

- Maven coordinates and output JAR filenames remain unchanged. For example,
  `backend/api/target/spring-web-0.0.1-SNAPSHOT.jar` is still the API artifact;
  the parent coordinate remains `com.david.hlp:JDK17Service` for compatibility.
  Maven display names now identify BossAnalyze and each module's responsibility.
- Java packages, class names, component scanning, mapper namespaces, service
  discovery identifiers, ports and HTTP paths are unchanged.
- The frontend package is named `bossanalyze-frontend`; its dependencies, lockfile,
  Nuxt routes, browser request behavior and proxy rules are unchanged.
- SQL contents and screenshot files are moved intact. Moving SQL is not a
  database migration; no import or data change is required.
- `docs/frontend-api/endpoints.md`, `contracts.md` and `inventory.json` are
  historical snapshots with pinned source links. Their old paths intentionally
  refer to that snapshot; use the mapping above for current backend paths and
  `frontend/app/` for the preserved frontend API layer.

## Updating a local checkout

Update IDE module roots, CI integrations outside this repository and any local
launch scripts to the new paths. The checked-in CI workflow, Maven module paths,
credential-contract source selection and configuration fixtures already use them.

Build the backend with `mvn -f backend/pom.xml clean install`. Start services
using the new JAR paths documented in [local development](local-development.md).
Run frontend commands from `frontend/`; install with
`pnpm install --frozen-lockfile` to regenerate local Nuxt metadata for that path.

Ignored configuration, IDE settings, generated output and dependency directories
are local state, not tracked migration inputs. Preserve any ignored private
configuration and copy it to the matching new module path before starting a
service. Reuse the existing approved credentials; do not generate or rotate them
as part of moving a directory. Existing running processes can keep using the old
checkout until you choose to restart them.

The workspace folder and GitHub repository remain `BossAnalyze`.
