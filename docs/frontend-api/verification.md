# Verification record

Snapshot: `832bfde`; checked locally on 2026-10-30. This is documentation-only verification.

## Source and consistency checks

- Enumerated all 10 API implementation modules and their 63 concrete HTTP call expressions with the installed TypeScript parser, including generic Axios calls, shorthand request methods, direct fetch and namespace imports. The extraction asserted that every application transport call was cataloged and no such call existed outside src/api.
- Confirmed 62 unique method/path operations; the dashboard has two wrappers for one request. Confirmed 5 missing local controller mappings and 2 external calls separately.
- Cross-checked all checked-in Java controller mappings and relevant request/response models. Source references and type snapshots refer to the exact current checkout, not another migration branch.
- Checked graph index coverage for 166 source/config evidence paths in batches: no recorded gaps or metadata mismatches. This is a best-effort index signal, not proof of completeness; direct source scans and reads were used as well.
- Checked catalog IDs, module totals, source line references, local Markdown link targets, balanced code fences, JSON parsing, declaration presence and git diff whitespace. No runtime source files were edited.

## Local HTTP observations

The existing local account was used to log in through port 5020. Login returned HTTP 200, business code 200 and a token object. Credentials, token values, user data, resume IDs and full responses are not included. The login updates normal authentication state; the other requests below are GETs. These observations test raw HTTP through the existing frontend proxy, not execution of every Axios wrapper or UI interaction.

Of 19 GET operations sent, 17 returned HTTP 200 and 2 legacy unmatched routes returned HTTP 500. Six resume-dependent reads were skipped because this account had no visible resume; no fixture was created. API-021 shares API-020's request path, but its legacy response adapter was not executed.

| ID | Function | HTTP / result | Response shape |
| --- | --- | --- | --- |
| API-002 | `getUserPrivateInformation` | 200 | list; business code 200; items=0 |
| API-003 | `getUserRole` | 200 | dict; business code 200 |
| API-004 | `getUserBaseInfo` | 200 | dict; business code 200 |
| API-005 | `getRoleList` | 200 | list; business code 200; items=2 |
| API-014 | `getSalaryHotJob` | 200 | list; items=0 |
| API-015 | `getTwoDimensionalAnalysisChart` | 200 | list; items=0 |
| API-016 | `getCityNameList` | 200 | list; items=0 |
| API-017 | `getPositionNameList` | 200 | list; items=0 |
| API-018 | `getCompanyList` | 500 | NoneType; business code 500 |
| API-019 | `getThreeDimensionalAnalysisChart` | 500 | NoneType; business code 500 |
| API-020 | `getDashboardStats` | 200 | dict |
| API-022 | `getHeatMapData` | 200 | dict |
| API-023 | `getIpStatistics` | 200 | dict |
| API-024 | `getHttpMethodStatistics` | 200 | dict |
| API-025 | `getBrowserStatistics` | 200 | dict |
| API-027 | `checkServiceHealth` | 200 | dict |
| API-034 | `getResumes` | 200 | list; business code 200; items=0 |
| API-035 | `getResumeById` | skipped | no existing visible resume; no fixture created |
| API-044 | `getCommitHistory` | skipped | no existing visible resume; no fixture created |
| API-048 | `getRecentCommits` | skipped | no existing visible resume; no fixture created |
| API-049 | `getCommitsBetween` | skipped | no existing visible resume; no fixture created |
| API-052 | `getCommitDebugInfo` | skipped | no existing visible resume; no fixture created |
| API-053 | `validateVersionControl` | skipped | no existing visible resume; no fixture created |
| API-055 | `getRoleList` | 200 | list; business code 200; items=2 |
| API-056 | `getUserRoutes` | 200 | list; business code 200; items=1 |

The two old company/3D requests are routed to port 8080 and have no controller mapping in the source. The local server returned HTTP 500, not a clean 404; the inventory does not invent a 404 guarantee.

Recruitment arrays were empty. Log gateway HTTP 200 can represent cached/default/empty data; no real analysis job was triggered, and these responses do not validate the underlying statistics or data freshness.

## Deliberately untested behavior

No create/update/delete, registration/email, upload, branch/merge/checkout/reset/revert/stash or duplicate-ID repair operation was replayed. No MinIO or external export/count service was called. No production environment, browser redesign, authorization audit, fresh-account scenario or full end-to-end suite was exercised. Backend/ frontend builds were not rerun for this documentation-only change.

Existing services and repository changes outside this documentation were preserved. The application source remains at the recorded snapshot; the new documentation does not fix the compatibility defects it describes.
