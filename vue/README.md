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
  pages/console/          Protected console pages and remaining rebuild placeholders
  layouts/console.vue     Sidebar-07 console shell
  composables/useSession.ts  Existing authentication API integration
  app.vue                 Nuxt layout and page outlets
nuxt.config.ts            Theme, environment compatibility and Nitro proxy rules
```

The original `vue/src/api/` tree and required declarations moved to `vue/app/` during
initialization. The dashboard and health wrappers now propagate request errors instead of
returning synthetic zero statistics or `DOWN`; other wrappers retain their existing behavior. Nuxt's `@` alias
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

## Workbench

`/console` now shows a real welcome area, three log-snapshot metrics, authorized quick
entries and the current role/access summary. Quick entries use the same API-derived
session menu tree as the sidebar, keeping leaf destinations in order and excluding
groups, the workbench itself and duplicate paths. Sidebar and workbench reuse one semantic
icon mapping. The root breadcrumb and document title identify the page as the workbench.

The page calls the existing role/permission APIs and only requests dashboard statistics
when the current session grants `/console/dashboard`. It never fetches administrator
user/role lists for invented global counts. Log metrics reuse the dashboard normalizer:
visits, IP-deduplicated visitors and peak recorded-hour visits share the returned snapshot
scope. No snapshot shows em dashes; valid zero values remain zero. Missing/default
snapshots, request failures and forbidden access are separate states. Role reads can fail
and retry independently. Refresh only reads data; no analysis or collection is triggered.
Quick-entry availability follows the login session, matching the existing route guard;
sign in again after menu/authorization changes to update navigation.

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

## User management

`/console/users` follows the query-table pattern from the B-end design system: inline
name/role/status filters, a table with soft status badges, pagination, detail/edit drawers
and password-confirmed deletion. Few role choices use radio buttons; larger role lists use
a searchable combobox. Mobile editing uses an 80vh bottom sheet with fixed actions.
Unsaved forms block accidental dismissal and navigation. No bulk or department API exists,
so no bulk action bar or organization tree is advertised.

The existing user query is a read-only **POST** to `/api/user/getUserManageInfo`;
roles use `GET /api/role/getRoleList`. The user service subtracts one from `number` and
passes it directly to SQL `LIMIT offset, size`. The frontend therefore requests
`number = (page - 1) * size + 1` and derives its page controls from `totalElements`.
This is an adapter for the current implementation, not the zero-based convention described
by the shared PageInfo declaration. The backend is unchanged.

Create, update and delete use the preserved wrappers and require explicit submission.
Editing sends only supported editable fields, never a password or login metadata, because
the existing update endpoint does not hash passwords. Delete asks for the acting user's
password and disables deleting the signed-in account. Updating the current account ends
the local session so stale account/permission context cannot survive. Business error codes
are checked even inside HTTP 200 responses. Passwords are cleared on closing or success.

## Role management

`/console/roles` uses shadcn-vue filters, a paginated table, detail/edit sheets and a
collapsible checkbox authorization tree. It reads full role grants from
`GET /api/auth/getRoleList`, the current role from `/api/auth/getUserRole`, and the
complete menu tree from `/api/auth/getRouters`. Filtering and pagination run locally
because the existing role endpoint returns the whole list.

Create/edit/delete use the existing auth wrappers. Creation saves metadata first;
menu authorization is a separate explicit operation through `/api/auth/updateRoleRouters`.
Selected children include ancestors; shared permission identifiers select together;
removing a parent removes its descendants. Disabled and action entries remain visible.
Leaves have no expand control. Existing grants missing from the menu tree block replacement
to prevent silent loss. Successful authorization must match a fresh role-list read before
the page reports success; uncertain results require another read before editing again.

The registration default `USER` role cannot be renamed or deleted, and the current role
cannot be deleted. Changing the current role ends the local session. Clearing all grants
requires a separate confirmation. Drawers protect unsaved changes, support mobile bottom
sheets and lock dismissal during submissions. Backend authorization remains authoritative.

## Menu management

`/console/menus` uses a searchable tree table and shadcn-vue detail/create/edit sheets.
It reads `GET /api/auth/getRouters` and preserves disabled, hidden and action entries.
Search matches title, route name, path or permission, keeps ancestors for context and
temporarily overrides folding. Leaves have no expand button. Menu types follow the SQL
contract: `C` directory, `M` menu, `F` action. The tree has no invented server pagination.

Create/edit/delete use the existing router wrappers. New roots send `pid: null`; child
creation starts from the selected parent and rejects permission identifiers already used
by menus or role grants, including grants without a menu. Editing fixes parent and permission because
the current mapper cannot clear a parent and permission replacement deletes the old
permission record, cascading role grants. Role metadata is preserved; nullable empty
role metadata uses the backend's `[]` default. Unsupported parents cannot receive new
children because the current backend tree builder depends on parent ordering.

Deletion is limited to leaves with independent permissions and no role references,
checked through the existing auth role-list endpoint. The page checks again immediately
before submission, sends no descendants, and confirms removal with a fresh tree read.
Shared permissions and assigned grants must be resolved first. This is a client guard,
not an atomic backend concurrency guarantee. Save also checks business codes and read-back;
uncertain results block another submission until the user rereads the menu. Current
navigation remains session-cached; sign in again to apply changed navigation. Component
identifiers are stored configuration and do not dynamically create frontend pages.

## Settings

`/console/settings` follows the B-end settings skeleton: category navigation on the left
and shadcn-vue cards on the right, stacked on small screens. Account/access and service
status are read-only because the original settings page was a placeholder and the
preserved API layer has no system-settings, profile-save or password-change operation.
There are no unconnected save buttons, notification toggles or invented configuration APIs.

Account details use `/api/auth/getUserBaseInfo` and whitelist only ID, name, email and
role ID. That service does not populate account status. Role/access details use
`/api/auth/getUserRole` and `/api/auth/getUserPrivateInformation`, accepting the actual
string permission list and distinguishing zero permissions from read failure. The three
data sections load and refresh independently, with loading, retry and forbidden states.

The existing `/api/v1/system/logs/health` wrapper now propagates request failures instead
of converting them into a synthetic `DOWN`. The page distinguishes transport/invalid-data
failures from the gateway's explicit `DEGRADED`/Spark `DOWN` result. Successful connectivity
only establishes that the gateway received the Spark log-service health response; it does
not establish database health, running collection jobs or available analysis snapshots.
The server timestamp is displayed in the browser's local timezone. Raw backend errors and
upstream response strings are excluded. Refresh performs reads and never triggers analysis.

## Personal center

`/console/profile` is a shadcn-vue account-detail page with an initial avatar, identity
summary, basic details and role/access information. It calls only the existing current-user
`getUserBaseInfo`, `getUserRole` and `getUserPrivateInformation` wrappers, reusing the settings
page's validated account/access projections. No administrator user-list API is used.
Account details and access load independently; refreshing all data or retrying one failed
section does not imply a failed request is an empty permission set.

The base-information service exposes name, email, account ID and role ID only. There is
no self-service profile, avatar or password-save API, so this page is read-only and directs
account changes to an administrator. Avatar display uses a name/email initial, without
requesting an image URL. Actual permissions are shown in an accessible collapsible list,
with a separate explanation for a role that has no grants. No activity or login-history
records are invented.

## Recruitment analysis

`/recruitment` is an authenticated, read-only analytics page with a fixed sidebar entry,
like the workbench. It does not create database menu grants or relax the existing
permission checks for `/console/**`. Login continuation allows this exact route.
It uses the four existing Spark GET wrappers for cities, city-dependent positions,
two-dimensional job samples and global salary rankings. The unimplemented company-list
and three-dimensional endpoints are not called.

The shadcn-vue filter, table, pagination and detail sheet retain the Small radius theme.
Queries apply explicitly; changing draft filters does not relabel previous results.
Numeric axes are allowlisted. Unsupported city strings are excluded from the existing
SQL-interpolated position lookup. Details project only needed fields and permit absolute
HTTP(S) source links without embedded credentials. Missing salary values remain unknown;
zero remains zero. The mean uses only returned, parsed K salary values. Experience and
degree codes are comparison values rather than exact years or qualifications.

The global ranking is independent of filters and documents the backend thresholds of
10 samples per position and 5 per city. Returned samples are paginated locally. The plot
merges identical coordinates and displays at most 1000 evenly selected coordinate groups;
summary counts and the table retain all returned records. The existing API still returns
the whole matching dataset, so this is not server-side pagination.

Real analysis returned HTTP 200 with no job records. Browser-only fixtures exercised
23-row pagination, city/position selection, query parameters, populated plot/ranking,
null/zero salary, safe detail links, forbidden/error/retry and 390px layouts without
document overflow. These fixtures were never inserted into storage. Six additional
normalization/query/link/plot tests bring the suite to 42 tests.

## Verification boundaries

`pnpm test` covers the preserved request boundary plus permission filtering, safe redirect
destinations, local session expiry, dashboard normalization/error propagation, user pagination,
field whitelisting, role grant linkage, menu hierarchy/filtering, workbench shortcuts,
settings normalization, health error propagation and write-payload safety (42 tests,
no HTTP). Type-checking and production build cover the Nuxt app. Existing local accounts
were used for real login, menu filtering and route protection checks. The dashboard's real
API returned an unavailable snapshot; no analysis job was triggered or fixture inserted.
Isolated browser response fixtures covered populated charts, rankings, keyboard access,
loading, request failure and retry. Mobile layout was checked at 390px with no document
overflow. Populated fixture checks do not establish real log-analysis acceptance. SMTP,
object storage and export flows were not exercised. Populated recruitment acceptance
against real stored jobs remains unverified because the existing job-detail table is empty.

Real user/role reads, name filtering and details were checked against existing local data.
Create/edit/delete, multi-page and multi-role cases, failure/retry, dirty-form dismissal,
loading and forbidden states used isolated browser response fixtures. Mutation requests
were intercepted and never reached backend services. All 14 existing MySQL tables were
compared before/after: only normal login token records changed; account, role, permission,
menu and recruitment data stayed unchanged. Real mutations remain unverified. The Skill's
audit script and the applicable visual/interaction checklist were checked for the user page
and its drawers/dialogs.

Role-list/current-role/menu reads were also verified against real APIs. Isolated browser
responses covered role creation, metadata edits, deletion, permission save/read-back,
read-back mismatch, business failures, orphan-grant protection, clearing confirmation,
dirty dismissal, empty/forbidden/error states and a 390px mobile authorization sheet.
All role mutation requests were intercepted in the browser and never reached the backend.

Real menu-tree and role-reference reads returned HTTP 200. Menu CRUD, HTTP 200 business
failure, read-back mismatch, tree folding/filtering, dirty dismissal and all five data
states were checked with browser-only fixtures. A 390px mobile detail sheet and document
overflow were checked. Menu mutation requests never reached services; real mutations
remain unverified. The B-end design audit covers the menu page and its sheet.

Real settings reads (account, role, permissions and service health) returned HTTP 200;
the gateway reported a successful Spark connection. Browser-only response fixtures covered
empty permissions, 403, independent section failures/retries, explicit degradation and
transport failure without a false `DOWN`. Category anchors/current-location indication,
sensitive-field exclusion and a 390px layout without document overflow were checked.
The settings flow issued only GET requests and did not modify business records.

Real personal-center account, role and permission reads returned HTTP 200. Browser
assertions verified keyboard permission expansion, sensitive-field exclusion, loading,
empty permissions, independent failure/retry and forbidden states, recovery and GET-only
interactions. Long-name/email cases at 390px had no document overflow. The existing 34
tests cover its reused normalizers and request/session boundaries; no duplicate unit
suite was added. Type-check, production build and B-end design audit passed for the page.

Real workbench statistics/role/permission reads returned HTTP 200. The statistics endpoint
returned no usable analysis snapshot, so the real page displayed em dashes. Browser
assertions covered a populated snapshot, genuine zero counts, missing snapshots,
error/forbidden states, independent role recovery, quick-entry navigation and a 390px
layout without document overflow. An existing zero-permission account showed no quick
entries and issued zero dashboard requests. Workbench interactions used GET only;
normal login was the only token-table change across the 14-table comparison.
