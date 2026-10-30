# BossAnalyze frontend foundation

The old frontend has been removed for redesign. This directory retains the API layer,
its request client and required types, development proxies, and an unstyled Vue entry point.
There are no business pages, UI components, styles, templates, stores or runtime router.

## Retained files

```text
src/
  api/                    All existing API wrappers and payload types
  utils/request/          Shared Axios client and declarations
  router/index.d.ts       API menu/route data types only; no runtime router
  types/common.d.ts       Pagination data type
  App.vue                 Unstyled redesign placeholder
  main.ts                 Vue entry point
  vite-env.d.ts           Vite environment types
vite.config.ts            Vue plugin, @ alias and development proxies
```

API implementations, endpoint paths, payloads, authentication and response handling are
unchanged. Read the [API preservation guide](../docs/frontend-api/README.md) and its catalog
before rebuilding features; known missing backend routes and contract defects still apply.

## Development

Use Node.js 22.11+ and Yarn 1.22.22, selected by `packageManager`:

```sh
corepack yarn install --frozen-lockfile
corepack yarn dev
corepack yarn type-check
corepack yarn build
```

Vite binds to `127.0.0.1:5020` with a strict port. Proxies preserve the `/api` prefix:

| Prefix | Target |
| --- | --- |
| `/api/v1/boss` | `http://127.0.0.1:8082` |
| `/api/v1/ai` | `http://127.0.0.1:8081` |
| `/api` | `http://127.0.0.1:8080` |

External export/count calls still read `VITE_EXPORT_URL`, `VITE_UPSTASH_BASE_URL` and
`VITE_UPSTASH_GET_TOKEN` from private environment configuration. Keep secret values out of Git.
Production hosting needs equivalent reverse-proxy routing; Vite's proxy is development-only.

The placeholder does not log in, fetch business data, change browser storage or send writes.
Rebuild authentication, account-specific menu state, draft handling and export assembly
explicitly when creating the replacement UI. The request client still redirects protected
requests without a token to `/login`; that path currently shows the same placeholder.
