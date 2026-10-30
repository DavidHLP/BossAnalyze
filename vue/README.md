# BossAnalyze Nuxt frontend

A fresh Nuxt 4 frontend initialized with the requested command:

```sh
pnpm dlx shadcn-vue@latest init --preset a2lQlAlu --template nuxt --pointer
```

The CLI resolved this preset to `reka-mira`, olive base color, Roboto, Hugeicons,
translucent menus and bold menu accents. `components.json` and the generated Tailwind CSS
are the source of truth. Button and Card components were added through the same CLI;
the home page is a starter, not a restored business application.

## Development

Use Node.js 22.12+ and pnpm 11.24.0 (pinned in `packageManager`):

```sh
pnpm install --frozen-lockfile
pnpm dev
pnpm type-check
pnpm test
pnpm build
```

Development binds to `127.0.0.1:5020`. Run the production server with:

```sh
HOST=127.0.0.1 PORT=5020 pnpm start
```

Nuxt/Nitro is required for the proxy rules below; deploy `.output/` as a Node server.
Serving only `.output/public/` does not run those proxies. `pnpm-workspace.yaml` permits
only esbuild's required dependency build script; no global pnpm configuration is changed.

## Preserved API layer

```text
app/
  api/                    Existing 63 API wrappers and payload types
  utils/request/          Axios client and declarations
  router/index.d.ts       API menu/route data types, not router implementation
  types/common.d.ts       Pagination data type
  assets/css/tailwind.css  Generated preset theme and pointer styles
  components/ui/          Generated shadcn-vue Button/Card components
  lib/utils.ts            Generated cn helper
  pages/index.vue         Starter home page
  app.vue                 Nuxt page outlet
nuxt.config.ts            Theme, environment compatibility and Nitro proxy rules
```

The original `vue/src/api/` tree and required declarations moved to `vue/app/` with their
contents unchanged. Nuxt's `@` alias resolves to `app/`. The Axios client preserves token
injection, timeout, credentials, response unwrapping, redirect and unauthorized-event
behavior. It now rejects server-side calls explicitly before accessing browser storage.
Call these authenticated wrappers from browser interactions or `onMounted`; use a separate
server-aware mechanism when implementing SSR-authenticated data fetching.

| Request prefix | Nitro proxy target (development and production) |
| --- | --- |
| `/api/v1/boss/**` | `http://127.0.0.1:8082/api/v1/boss/**` |
| `/api/v1/ai/**` | `http://127.0.0.1:8081/api/v1/ai/**` |
| `/api/**` | `http://127.0.0.1:8080/api/**` |

The `/api` prefix is retained. Nitro selects the more-specific recruitment/AI rules.
External export/count wrappers retain `VITE_EXPORT_URL`, `VITE_UPSTASH_BASE_URL` and
`VITE_UPSTASH_GET_TOKEN`. Nuxt config explicitly defines only those legacy browser values;
supply them before dev/build. They are public client configuration, not a place for private
server credentials. Do not commit actual values. The template imports Roboto from Google
Fonts; font loading needs external connectivity and has a sans-serif fallback.

Read the [API handoff](../docs/frontend-api/README.md) for requests, responses and existing
backend contract defects. Login, menus, analytics, resume editing and export UI still need
to be rebuilt. No backend or database changes are part of this frontend initialization.

## Verification boundaries

`pnpm test` checks the new SSR boundary plus the preserved browser authentication,
response unwrapping and unauthorized behavior without sending HTTP. Type-checking and
production build cover the new Nuxt app. The starter does not call business APIs or change
browser storage. Backend ports 8080/8081/8082 were not listening during this task, so real
business integration, SMTP, object storage and export services were not exercised.
