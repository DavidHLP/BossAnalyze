# Repository Guidelines

## Project Structure & Module Organization

- `backend/` is the Java 17 Maven reactor: `commons` holds shared models/utilities, `api` handles authentication and business APIs, `crawler` collects recruitment data and integrates AI, and `analytics` runs Spark analysis. Modules use `src/main/java`, `src/main/resources`, and `src/test/java`.
- `frontend/` is the Nuxt 4/Vue 3 TypeScript application. Routes live in `app/pages`, reusable UI in `app/components`, HTTP wrappers in `app/api`, and shared logic in `app/lib` and `app/composables`. Assets live in `app/assets` and `public`; tests and fixtures live in `tests/`.
- `tools/credential-contract-tests/` contains isolated Java credential tests. `database/` holds SQL snapshots; `docs/` contains API contracts, local setup, and directory migration notes.

## Build, Test, and Development Commands

Use Java 17, Maven 3.9.6+, Node.js 22.12+, and pnpm 11.24.0.

From the repository root:

```sh
mvn -f backend/pom.xml clean verify
mvn -f tools/credential-contract-tests/pom.xml clean verify
```

These verify the backend reactor and isolated credential contracts, respectively.
From `frontend/`:

```sh
pnpm install --frozen-lockfile
pnpm dev          # http://127.0.0.1:5020
pnpm type-check
pnpm test
pnpm build
```

See `docs/local-development.md` for backend JAR startup and infrastructure prerequisites. Production requires the Nuxt `.output/` Node server for API proxies.

## Coding Style & Naming Conventions

Follow `frontend/.editorconfig`: two-space indentation, LF, final newline, and no trailing whitespace. Match surrounding TypeScript/Vue style; reuse shadcn-vue primitives and existing request/session helpers. Use camelCase for functions and PascalCase for Java classes; retain existing Java package names and local indentation. No frontend lint/format script is currently configured; run type checking and avoid unrelated reformatting.

## Testing Guidelines

Frontend tests use Node's test runner and strict assertions in `tests/*.test.mjs`; Java tests use JUnit Jupiter with `*Test.java` names. Extend focused tests for changed behavior, especially permissions, API contracts, and failure states. No numeric coverage threshold is configured. Run affected checks above; CI runs both stacks. Use synthetic credentials and isolated fixtures rather than modifying existing business records.

## Commit & Pull Request Guidelines

History uses prefixes such as `fix(config):`, `build(deps):`, `refactor:`, and `ci:`. Keep commits focused and imperative. Include the required `Signed-off-by: DavidHLP <lysf15520112973@163.com>` trailer. PRs should explain behavior changes, link relevant issues, report checks and limitations, and include screenshots for UI changes.

## Security & Configuration

Keep secrets in environment variables or private external configuration. Preserve same-origin `/api` proxy paths. SQL snapshots drop tables: import only into new, empty databases. Never use public `VITE_*` configuration for private credentials.
