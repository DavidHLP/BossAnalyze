# BossAnalyze Nuxt frontend

A fresh Nuxt 4 frontend initialized with the requested command:

```sh
pnpm dlx shadcn-vue@latest init --preset a2lQlAlu --template nuxt --pointer
```

The CLI resolved this preset to `reka-mira`, olive base color, Roboto, Hugeicons,
translucent menus and bold menu accents. `components.json` and the generated Tailwind CSS
are the source of truth. The login uses the official `login-02` block; the console uses
`sidebar-07`. The current `reka-mira` registry has no corresponding block JSON, so both
layouts were installed from the official `new-york-v4` registry URLs through the latest
CLI, then adapted to the existing Mira primitives and Hugeicons renderer.

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
  components/ui/          Generated shadcn-vue primitives
  lib/utils.ts            Generated cn helper
  pages/login.vue         Responsive login-02 page
  pages/console/          Protected dashboard and authorized menu placeholders
  layouts/console.vue     Sidebar-07 console shell
  composables/useSession.ts  Existing authentication API integration
  app.vue                 Nuxt layout and page outlets
nuxt.config.ts            Theme, environment compatibility and Nitro proxy rules
```

The original `vue/src/api/` tree and required declarations moved to `vue/app/` during
initialization. The dashboard wrapper now propagates request errors instead of returning
synthetic zero statistics; other wrappers retain their existing behavior. Nuxt's `@` alias
resolves to `app/`. The Axios client preserves token
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
backend contract defects. `/` redirects to `/console`; unauthenticated visitors reach
`/login`. Login calls the existing login, profile, permission and router wrappers. Menus
are filtered by exact permissions, status and visibility; legacy paths are mapped under
`/console`. The console is client-rendered because the preserved client stores its token
in localStorage; the login page retains SSR. Logout clears local session state and menus
(there is no server logout wrapper). No social login or account recovery is advertised.
Recruitment analytics, resume editing and export screens still need to be rebuilt. No backend or
database changes are part of this frontend work.

## Dashboard

`/console/dashboard` uses shadcn-vue cards, badges, alerts, skeletons and tables, with an
accessible hourly bar chart. It calls only the existing
`GET /api/v1/system/logs/dashboard/stats` through the shared client and proxy. Refresh
reads the cached result; it never triggers analysis, collection or data writes.

The page shows log-derived visits, visitors, peak hourly visits, active hours, top paths,
browsers and request methods. It excludes backend sample values for real-time users,
performance, security and operating systems, plus simplified data-quality metrics.
Hourly buckets retain their original labels, show up to 24 recorded hours, and do not
invent zero buckets or claim a selectable time range. Table percentages use actual counts.

The gateway returns a small default object when Spark has no cached snapshot **or when
its upstream request fails**. These cases cannot be distinguished from this API alone:
the page reports an unavailable snapshot, not zero traffic or healthy upstream analysis.
Malformed snapshots and transport failures show an error with retry. Missing snapshots
show em dashes; complete snapshots containing actual zero counts still show zero.

## Verification boundaries

`pnpm test` covers the preserved request boundary plus permission filtering, safe redirect
destinations, local session expiry and dashboard normalization/error propagation (13 tests,
no HTTP). Type-checking and production build cover the Nuxt app. Existing local accounts
were used for real login, menu filtering and route protection checks. The dashboard's real
API returned an unavailable snapshot; no analysis job was triggered or fixture inserted.
Isolated browser response fixtures covered populated charts, rankings, keyboard access,
loading, request failure and retry. Mobile layout was checked at 390px with no document
overflow. Populated fixture checks do not establish real log-analysis acceptance. SMTP,
object storage, export and recruitment business flows were not exercised.
