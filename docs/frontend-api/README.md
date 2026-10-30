# Frontend API preservation guide

This is the interface handoff for rebuilding the frontend while retaining its API layer.
It records the **current checkout at `832bfde`, dated 2026-10-30**, including unused exports,
known contract defects and external integrations. It does not delete or redesign the UI,
change requests, migrate data, or claim that every operation works. Source links in the
catalog and snapshots are pinned to this baseline commit so they survive deletion of the
old frontend files.

## Current frontend state

The frontend now uses a fresh Nuxt 4 + shadcn-vue foundation initialized with preset
`a2lQlAlu` and `--pointer`. The preserved API and supporting types moved from `vue/src/`
to `vue/app/`; their module structure and request contracts are unchanged. The shared Axios
client now explicitly rejects server-side use before reading browser storage. Nitro route
rules replace the old Vite development proxy and also apply to the production Node server.
See [the frontend README](../../vue/README.md) for commands and the current layout.

The catalog, contracts, consumers and HTTP observations below are the historical pre-removal
snapshot. The old business consumers are absent; their pinned source links remain usable.
The new login uses `login-02`; the protected console shell uses `sidebar-07`, connected to
the preserved authentication and permission/menu APIs. `/console/dashboard` now reads the
existing log dashboard API. `/console/users` now uses the existing user/role APIs for a
filtered paginated list and detail/create/edit/delete flows. `/console/roles` now reads
the complete auth role list and menu tree, with metadata CRUD and menu authorization
through the existing auth wrappers. Authorization includes parent/shared-permission
linkage, unmatched-grant protection and a read-back check before reporting success.
Other business routes remain rebuilding placeholders. Real user and role reads were verified; write flows used browser-only
fixtures and were not executed against existing records.
The dashboard wrapper propagates request errors instead of manufacturing zero statistics.
The gateway's default response is shown as an unavailable snapshot, and known backend
sample metrics are excluded. Drafts, recruitment analytics and export assembly must be rebuilt.
Existing API defects remain documented; backend code, database schemas and stored data
are outside this change. Real local login and permission menus were verified using existing
accounts. The real dashboard API returned no usable analysis snapshot; populated and error
states were checked with isolated browser responses, without writing fixtures to services.

## Documents

- [Endpoint catalog](endpoints.md): all **63 wrappers / transport calls**, individually
  numbered, with method, URL, typed parameters, request body/query, response declaration,
  authentication, original consumers, backend handler and compatibility notes.
- [Contract snapshots](contracts.md): complete frontend data declarations, types outside
  `api/`, the shared request implementation, statistics transformations, external export
  handling, backend model fields and response/error helpers. These remain available after
  the old frontend files are removed.
- [Structured inventory](inventory.json): the same 63 entries in JSON, including source
  references, caller lists, matched controllers and per-entry limitations.
- [Verification](verification.md): source coverage, consistency checks and bounded local
  HTTP observations. Source matching is separate from runtime verification.

There are **62 distinct method/path operations**: `getDashboardStats` and
`getAccessLogStats` both call `GET /api/v1/system/logs/dashboard/stats`.

| Implementation module under `vue/src/api/` | Wrappers | Purpose |
| --- | ---: | --- |
| `auth/auth.ts` | 13 | Login, registration, identity, role management |
| `boss/user/user.ts` | 6 | Recruitment analysis and filter values |
| `log/accesslog.ts` | 8 | Dashboard, log analysis and health |
| `minio/minio.ts` | 4 | Image upload, URL, deletion and viewing |
| `modules/resume.ts` | 2 | External export and export count |
| `resume/resume.ts` | 5 | Resume CRUD |
| `resume/version.ts` | 16 | Resume history, branches, stash and repair |
| `role/role.ts` | 1 | Filtered role lookup |
| `router/router.ts` | 4 | Dynamic menu lookup and management |
| `user/user.ts` | 4 | User administration |

## Files required when retaining the API directory

Keeping only `vue/src/api/` is **not sufficient to compile or use the current API layer**.
These are the direct dependencies to retain or deliberately rehome before deleting other
frontend files:

| Existing dependency | What must survive |
| --- | --- |
| `vue/src/utils/request/request.ts` | Axios instance, base URL, timeout, credentials, token injection, response unwrapping and unauthorized event |
| `vue/src/utils/request/request.d.ts` | Existing transport declarations; these do not fully describe the actual Axios instance |
| `vue/src/types/common.d.ts` | `PageInfo<T>` used by user administration |
| `vue/src/router/index.d.ts` | `Router`, `Meta`, `BackendRoute` declarations; move these to a neutral API types file if replacing the router |
| `vue/src/api/**/*.d.ts`, `resume/types.ts`, `ResumeCommit`, `IResumeConfig` | Payload and response types; preserve their field definitions before removing files |
| `vue/vite.config.ts` | Alias `@` to `src` and the ordered proxy rules below; Vite development proxy does not configure a production reverse proxy |
| `vue/package.json` and lockfile | Axios dependency; retain a compatible TypeScript/build environment and lock dependency resolution deliberately |
| Private environment configuration | `VITE_EXPORT_URL`, `VITE_UPSTASH_BASE_URL`, `VITE_UPSTASH_GET_TOKEN`; variable names are recorded, secrets are not |

The wrappers do not import the old UI components or Pinia stores directly. Their runtime
still depends on browser `window`, `localStorage`, `FormData`, `fetch`, and `import.meta.env`.
If replacing Vite or the browser environment, adapt these explicitly.

The following behavior is currently implemented **outside the API directory** and must be
reimplemented by the new application if retained:

- Login receives a token, persists it, loads base information, permissions, role and dynamic
  routes, then installs the routes. See `store/token/tokenStore.ts`, `store/user/userStore.ts`,
  `store/router/routerStore.ts`, `router/index.ts` and `views/front/login/LoginView.vue`.
- Token is stored as the raw string at `localStorage.token`; the transport adds `Bearer `.
  `token_timestamp` is a millisecond timestamp. The router uses a local 24-hour expiry rule;
  it is not a server refresh-token contract. The login screen's remember-me branch also writes
  session storage, but `setToken` always writes local storage first.
- Missing token redirects to `/login` and rejects before sending a protected request.
  HTTP 401 removes `token` and emits `window` event `unauthorized`; the replacement shell
  must handle cleanup/navigation. Clear account-specific `cachedRoutes` and installed
  dynamic routes on logout/account change. The old cache is not keyed by account.
- Role and route data affect visibility and permission checks. Dynamic component names and
  route metadata are presentation-bound data: map old component identifiers to replacement
  screens instead of assuming the old view files still exist. Frontend visibility checks
  are not backend authorization.
- Pagination correction: PageInfo declares zero-based numbering, but the actual user
  service passes `number - 1` directly to SQL `LIMIT offset, size`. The rebuilt UI adapts
  page `p` and size `s` to `number=(p-1)*s+1`; the backend is unchanged. This corrects the
  earlier catalog note that inferred runtime pagination from the PageInfo declaration.
  Image URLs are resolved before rendering avatars. Role permission selection includes
  selected nodes' ancestors. Resume saves pass the current content, not only the title.
- Export is assembled from rendered HTML, CSS and a stylesheet link in
  `views/resume/hook.ts`; `type=0` expects `pdf.data`, `type=1` expects `picture.data`.
  `resumeExport` alone does not build the document or initiate the browser download.
- Unsaved editor drafts and template preferences use local storage and are not backend APIs.
  Removing their current stores/components does not preserve those browser-only drafts.

## Routing and transport

Keep request paths relative to the frontend origin and retain their `/api` prefix.
The development server listens on `127.0.0.1:5020` with this order:

| Prefix | Target | Notes |
| --- | --- | --- |
| `/api/v1/boss` | `http://127.0.0.1:8082` | Spark recruitment analysis; raw JSON arrays |
| `/api/v1/ai` | `http://127.0.0.1:8081` | Crawler AI service; no current frontend HTTP wrapper |
| `/api` | `http://127.0.0.1:8080` | Main Spring API, resumes, images and log gateway |
| External export/count URLs | Browser calls configured origin directly | Require external service and CORS configuration |

The shared Axios client uses `baseURL: '/'`, `timeout: 5000`, `withCredentials: true`,
and JSON content type by default. Upload overrides content type; image viewing requests
`blob`. Inline path/query interpolation does not apply `encodeURIComponent`.

Only login, registration and registration email are public in the **client**. All other
Axios wrappers require a local token before sending. The main backend's JWT filter also
treats `/api/v1/system/logs...` and health as public; its separate security whitelist is
broad. Do not infer administrator enforcement from UI menus or commented-out role matchers.
The Spark raw endpoints do not pass through the main backend JWT filter. Authentication
and authorization must be assessed at the target service, not inferred from the client.

### Responses and errors

Most main API operations return:

```ts
type Envelope<T> = {
  code: number
  message: string
  data: T | null
  timestamp: number
}
```

The current client accepts only HTTP 200 and resolves `response.data?.data ?? response.data`.
Consequences:

- `data: null` or absent data returns the **whole envelope**, even for `Promise<void>`.
- A successful string passed to Java `Result.success(String)` becomes `message`, not `data`.
  Version commit/merge/revert/stash-pop are declared strings but return envelopes through
  this client. Preserve this discrepancy in any adapter migration until fixed deliberately.
- Raw Spark arrays, gateway maps/text and blobs follow different response shapes.
- Other successful HTTP statuses such as 201/204 are rejected by this client.
- The client does not check business `code` on HTTP 200. Many backend `Result.error(...)`
  paths send HTTP 500 even when body.code is 400/401/404 or a custom business code.
- Axios errors are normalized as `{ code: HTTP status or 500, message, data: response body }`.
  Missing-token rejection is a string. External fetch errors bypass this normalization.
- Log wrappers and the main log gateway can resolve defaults/empty data on failure;
  a successful request or all-zero statistics do not establish that analytics ran.

The complete business code dictionary, response helpers and exact adapters are archived in
[contracts.md](contracts.md). No unified response schema has been invented for `any`/open maps.

## Known incomplete or incompatible contracts

These five frontend operations have **no matching backend controller route** in this snapshot:

| Function | Existing request | Observation |
| --- | --- | --- |
| `updateRolePermissions` | `POST /api/auth/updateRolePermissions` | Actual implemented role-menu update uses `/updateRoleRouters` and `routerIds` |
| `updateUserRole` | `POST /api/auth/updateUserRole` | Current user form changes `roleId` through `/api/user/updateUser` |
| `getCompanyList` | `GET /api/boss/basic/company-list` | Old prefix routes to main API; no company-list implementation found |
| `getThreeDimensionalAnalysisChart` | `GET /api/boss/user/three-dimensional-analysis-chart` | Old prefix routes to main API; no 3D implementation found |
| `initVersionControl` | `POST /api/resume/{resumeId}/version/init` | Creation already initializes version metadata; this explicit endpoint is absent |

Other concrete limitations are recorded per endpoint:

- Resume create service does not set `docType=meta`, while list/detail queries require it.
  The current caller supplies title/content without docType. The JSON property would be
  `docType`; `doc_type` is its MongoDB field name. The backend should own this invariant,
  but this documentation task does not change it.
- Resume update commits content but does not persist an incoming title. A successful rename
  request is not evidence that the title survives a reload.
- Permissions arrive as strings, unlike the frontend `Permissions` index-signature type.
  `JobData` requires `industry`, which the backend analysis DTO does not declare.
  The client Resume model declares `hasVersionControl`, absent from the Java entity.
- `ResumeCommit.commitTime: Date` is only a TypeScript declaration; JSON parsing leaves
  strings. History/recent/diff actually return the shared Java Resume document model.
- MinIO `.d.ts` declarations disagree with implementation unwrapping; deletion may omit url.
  Image viewing wraps a resource in ResultBody while setting an image content type; binary
  interoperability remains unverified.
- `fixDuplicateCommitIds` can delete history and reinitialize main; reset, checkout, stash,
  merge and revert mutate data. Treat them as operations requiring deliberate user intent.
- There is no implemented client logout, refresh token, password reset, profile-save or
  dedicated AI-similarity wrapper in the scanned application. A link/button or backend-only
  route is not an existing frontend data contract.

## Coverage boundary

All application `.ts`, `.js` and `.vue` sources under `vue/src` were scanned for HTTP
transports and API references, including namespace imports. All 63 concrete transport calls
are in `src/api`; none were found hidden in pages, stores or components. A broader scan also
checked HTML, config and alternative network APIs. Static fonts/icon styles, Markdown
example URLs, template placeholder images and runtime avatar/content image URLs are assets,
not extra JSON data interfaces. No active WebSocket, EventSource, XMLHttpRequest, sendBeacon,
useFetch or useAxios data transport was found in this snapshot.

Backend routes were cross-checked against checked-in controller mappings. Backend-only
health, AI similarity and log-analysis routes are not counted among the 63 frontend calls.
No missing endpoint was silently redirected to an invented replacement. This is a current
source inventory, not a production OpenAPI specification or exhaustive acceptance report.
