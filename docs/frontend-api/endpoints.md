# Frontend HTTP interface inventory

Source snapshot: `832bfde` on 2026-10-30. There are **63 frontend wrappers / transport calls**, representing **62 distinct HTTP operations**, in **10 implementation modules**. Two wrappers intentionally call the same dashboard operation. This includes unused exports and external services; it is not a list of only healthy endpoints.

[Start here](README.md) · [Type and transport snapshots](contracts.md) · [Machine-readable inventory](inventory.json)

Each entry records the exported signature, path interpolation, body/query expressions, declared response type, client authentication, original consumers and a matching backend controller where present. Parameters marked optional/defaulted in the signature retain those semantics. Request fields shown as a type or identifier expand in [contracts.md](contracts.md). No body/query expression means the wrapper sends none. `Promise<void>` does not guarantee an undefined result; see the common response rules. Call sites are static application references, including namespace imports; they do not prove the component is mounted or the call succeeds.

## Index

| ID | Module / function | Method | Request path | Backend |
| --- | --- | --- | --- | --- |
| [API-001](#api-001) | `auth/auth.ts` / `login` | POST | `/api/auth/login` | matched-controller |
| [API-002](#api-002) | `auth/auth.ts` / `getUserPrivateInformation` | GET | `/api/auth/getUserPrivateInformation` | matched-controller |
| [API-003](#api-003) | `auth/auth.ts` / `getUserRole` | GET | `/api/auth/getUserRole` | matched-controller |
| [API-004](#api-004) | `auth/auth.ts` / `getUserBaseInfo` | GET | `/api/auth/getUserBaseInfo` | matched-controller |
| [API-005](#api-005) | `auth/auth.ts` / `getRoleList` | GET | `/api/auth/getRoleList` | matched-controller |
| [API-006](#api-006) | `auth/auth.ts` / `addRole` | POST | `/api/auth/addRole` | matched-controller |
| [API-007](#api-007) | `auth/auth.ts` / `editRole` | POST | `/api/auth/editRole` | matched-controller |
| [API-008](#api-008) | `auth/auth.ts` / `deleteRole` | POST | `/api/auth/deleteRole` | matched-controller |
| [API-009](#api-009) | `auth/auth.ts` / `updateRolePermissions` | POST | `/api/auth/updateRolePermissions` | missing-controller |
| [API-010](#api-010) | `auth/auth.ts` / `updateRoleRouters` | POST | `/api/auth/updateRoleRouters` | matched-controller |
| [API-011](#api-011) | `auth/auth.ts` / `updateUserRole` | POST | `/api/auth/updateUserRole` | missing-controller |
| [API-012](#api-012) | `auth/auth.ts` / `register` | POST | `/api/auth/register` | matched-controller |
| [API-013](#api-013) | `auth/auth.ts` / `sendRegisterEmail` | POST | `/api/auth/sendRegisterEmail` | matched-controller |
| [API-014](#api-014) | `boss/user/user.ts` / `getSalaryHotJob` | GET | `/api/v1/boss/user/salary-hot-job` | matched-controller |
| [API-015](#api-015) | `boss/user/user.ts` / `getTwoDimensionalAnalysisChart` | GET | `/api/v1/boss/user/two-dimensional-analysis-chart` | matched-controller |
| [API-016](#api-016) | `boss/user/user.ts` / `getCityNameList` | GET | `/api/v1/boss/basic/city-name-list` | matched-controller |
| [API-017](#api-017) | `boss/user/user.ts` / `getPositionNameList` | GET | `/api/v1/boss/basic/position-name-list` | matched-controller |
| [API-018](#api-018) | `boss/user/user.ts` / `getCompanyList` | GET | `/api/boss/basic/company-list` | missing-controller |
| [API-019](#api-019) | `boss/user/user.ts` / `getThreeDimensionalAnalysisChart` | GET | `/api/boss/user/three-dimensional-analysis-chart` | missing-controller |
| [API-020](#api-020) | `log/accesslog.ts` / `getDashboardStats` | GET | `/api/v1/system/logs/dashboard/stats` | matched-controller |
| [API-021](#api-021) | `log/accesslog.ts` / `getAccessLogStats` | GET | `/api/v1/system/logs/dashboard/stats` | matched-controller |
| [API-022](#api-022) | `log/accesslog.ts` / `getHeatMapData` | GET | `/api/v1/system/logs/heatmap` | matched-controller |
| [API-023](#api-023) | `log/accesslog.ts` / `getIpStatistics` | GET | `/api/v1/system/logs/ip-statistics` | matched-controller |
| [API-024](#api-024) | `log/accesslog.ts` / `getHttpMethodStatistics` | GET | `/api/v1/system/logs/http-methods` | matched-controller |
| [API-025](#api-025) | `log/accesslog.ts` / `getBrowserStatistics` | GET | `/api/v1/system/logs/browser-usage` | matched-controller |
| [API-026](#api-026) | `log/accesslog.ts` / `triggerLogAnalysis` | POST | `/api/v1/system/logs/analysis/trigger` | matched-controller |
| [API-027](#api-027) | `log/accesslog.ts` / `checkServiceHealth` | GET | `/api/v1/system/logs/health` | matched-controller |
| [API-028](#api-028) | `minio/minio.ts` / `uploadImage` | POST | `/api/image/upload` | matched-controller |
| [API-029](#api-029) | `minio/minio.ts` / `getImageUrl` | GET | `/api/image/url/${fileName}` | matched-controller |
| [API-030](#api-030) | `minio/minio.ts` / `deleteImage` | DELETE | `/api/image/delete` | matched-controller |
| [API-031](#api-031) | `minio/minio.ts` / `getImage` | GET | `/api/image/view/${fileName}` | matched-controller |
| [API-032](#api-032) | `modules/resume.ts` / `resumeExport` | POST | `${VITE_EXPORT_URL}` | external-not-in-repository |
| [API-033](#api-033) | `modules/resume.ts` / `getExportCount` | GET | `${VITE_UPSTASH_BASE_URL}/get/count` | external-not-in-repository |
| [API-034](#api-034) | `resume/resume.ts` / `getResumes` | GET | `/api/resume` | matched-controller |
| [API-035](#api-035) | `resume/resume.ts` / `getResumeById` | GET | `/api/resume/${id}` | matched-controller |
| [API-036](#api-036) | `resume/resume.ts` / `createResume` | POST | `/api/resume` | matched-controller |
| [API-037](#api-037) | `resume/resume.ts` / `updateResume` | PUT | `/api/resume/${id}` | matched-controller |
| [API-038](#api-038) | `resume/resume.ts` / `deleteResume` | DELETE | `/api/resume/${id}` | matched-controller |
| [API-039](#api-039) | `resume/version.ts` / `initVersionControl` | POST | `/api/resume/${resumeId}/version/init` | missing-controller |
| [API-040](#api-040) | `resume/version.ts` / `commitChanges` | POST | `/api/resume/${resumeId}/version/commit` | matched-controller |
| [API-041](#api-041) | `resume/version.ts` / `createBranch` | POST | `/api/resume/${resumeId}/version/branch` | matched-controller |
| [API-042](#api-042) | `resume/version.ts` / `switchBranch` | POST | `/api/resume/${resumeId}/version/switch` | matched-controller |
| [API-043](#api-043) | `resume/version.ts` / `mergeBranch` | POST | `/api/resume/${resumeId}/version/merge` | matched-controller |
| [API-044](#api-044) | `resume/version.ts` / `getCommitHistory` | GET | `/api/resume/${resumeId}/version/history` | matched-controller |
| [API-045](#api-045) | `resume/version.ts` / `checkoutCommit` | POST | `/api/resume/${resumeId}/version/checkout` | matched-controller |
| [API-046](#api-046) | `resume/version.ts` / `resetToCommit` | POST | `/api/resume/${resumeId}/version/reset` | matched-controller |
| [API-047](#api-047) | `resume/version.ts` / `revertCommit` | POST | `/api/resume/${resumeId}/version/revert` | matched-controller |
| [API-048](#api-048) | `resume/version.ts` / `getRecentCommits` | GET | `/api/resume/${resumeId}/version/recent` | matched-controller |
| [API-049](#api-049) | `resume/version.ts` / `getCommitsBetween` | GET | `/api/resume/${resumeId}/version/diff` | matched-controller |
| [API-050](#api-050) | `resume/version.ts` / `stashChanges` | POST | `/api/resume/${resumeId}/version/stash` | matched-controller |
| [API-051](#api-051) | `resume/version.ts` / `stashPop` | POST | `/api/resume/${resumeId}/version/stash/pop` | matched-controller |
| [API-052](#api-052) | `resume/version.ts` / `getCommitDebugInfo` | GET | `/api/resume/${resumeId}/version/debug/commit/${commitId}` | matched-controller |
| [API-053](#api-053) | `resume/version.ts` / `validateVersionControl` | GET | `/api/resume/${resumeId}/version/debug/validate` | matched-controller |
| [API-054](#api-054) | `resume/version.ts` / `fixDuplicateCommitIds` | POST | `/api/resume/${resumeId}/version/fix-duplicate-ids` | matched-controller |
| [API-055](#api-055) | `role/role.ts` / `getRoleList` | GET | `/api/role/getRoleList?roleName=${roleName || ''}` | matched-controller |
| [API-056](#api-056) | `router/router.ts` / `getUserRoutes` | GET | `/api/auth/getRouters` | matched-controller |
| [API-057](#api-057) | `router/router.ts` / `editRouter` | POST | `/api/auth/editRouter` | matched-controller |
| [API-058](#api-058) | `router/router.ts` / `addRouter` | POST | `/api/auth/addRouter` | matched-controller |
| [API-059](#api-059) | `router/router.ts` / `deleteRouter` | POST | `/api/auth/deleteRouter` | matched-controller |
| [API-060](#api-060) | `user/user.ts` / `getUserManageInfo` | POST | `/api/user/getUserManageInfo` | matched-controller |
| [API-061](#api-061) | `user/user.ts` / `deleteUser` | POST | `/api/user/deleteUser` | matched-controller |
| [API-062](#api-062) | `user/user.ts` / `updateUser` | POST | `/api/user/updateUser` | matched-controller |
| [API-063](#api-063) | `user/user.ts` / `addUser` | POST | `/api/user/addUser` | matched-controller |

## API-001

**`login` — POST `/api/auth/login`**

Source: [vue/src/api/auth/auth.ts:4](../../vue/src/api/auth/auth.ts#L4). Service: `spring:8080`.

```ts
login(data: { email: string; password: string }): Promise<Token>
```

- Authentication: Public in client; attaches token if present.
- Query: `none`.
- Body: `{ ...data }`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/AuthController.java:107](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/AuthController.java#L107) — `login` returns `Result<Token>`.
- Backend parameter declarations: `@RequestBody final LoginDTO request`.

Current consumers: [vue/src/views/front/login/LoginView.vue:85](../../vue/src/views/front/login/LoginView.vue#L85)

## API-002

**`getUserPrivateInformation` — GET `/api/auth/getUserPrivateInformation`**

Source: [vue/src/api/auth/auth.ts:11](../../vue/src/api/auth/auth.ts#L11). Service: `spring:8080`.

```ts
getUserPrivateInformation(): Promise<Array<Permissions>>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `none`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/AuthController.java:145](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/AuthController.java#L145) — `getUserPrivateInformation` returns `Result<List<String>>`.
- Backend parameter declarations: `none`.

Backend data is List<String> (permission strings); the frontend Permissions[] declaration does not describe this wire shape.

Current consumers: [vue/src/store/user/userStore.ts:18](../../vue/src/store/user/userStore.ts#L18)

## API-003

**`getUserRole` — GET `/api/auth/getUserRole`**

Source: [vue/src/api/auth/auth.ts:17](../../vue/src/api/auth/auth.ts#L17). Service: `spring:8080`.

```ts
getUserRole(): Promise<Role>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `none`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/AuthController.java:156](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/AuthController.java#L156) — `getUserRole` returns `Result<Role>`.
- Backend parameter declarations: `none`.

Current consumers: [vue/src/store/user/userStore.ts:27](../../vue/src/store/user/userStore.ts#L27)

## API-004

**`getUserBaseInfo` — GET `/api/auth/getUserBaseInfo`**

Source: [vue/src/api/auth/auth.ts:23](../../vue/src/api/auth/auth.ts#L23). Service: `spring:8080`.

```ts
getUserBaseInfo(): Promise<UserBaseInfo>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `none`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/AuthController.java:168](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/AuthController.java#L168) — `getUserBaseInfo` returns `Result<User>`.
- Backend parameter declarations: `none`.

Backend User serializes both id/name and computed userId/username aliases. Frontend type omits password but backend entity includes it; do not log/persist entire raw user responses.

Current consumers: [vue/src/store/user/userStore.ts:45](../../vue/src/store/user/userStore.ts#L45)

## API-005

**`getRoleList` — GET `/api/auth/getRoleList`**

Source: [vue/src/api/auth/auth.ts:29](../../vue/src/api/auth/auth.ts#L29). Service: `spring:8080`.

```ts
getRoleList(): Promise<Role[]>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `none`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/AuthController.java:233](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/AuthController.java#L233) — `getRoleList` returns `Result<List<Role>>`.
- Backend parameter declarations: `none`.

Current consumers: [vue/src/views/console/system/rolemanagement/RoleManagementView.vue:471](../../vue/src/views/console/system/rolemanagement/RoleManagementView.vue#L471)

## API-006

**`addRole` — POST `/api/auth/addRole`**

Source: [vue/src/api/auth/auth.ts:36](../../vue/src/api/auth/auth.ts#L36). Service: `spring:8080`.

```ts
addRole(data: Role): Promise<void>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `data`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/AuthController.java:245](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/AuthController.java#L245) — `addRole` returns `Result<Void>`.
- Backend parameter declarations: `@RequestBody final Role role`.

Current consumers: [vue/src/views/console/system/rolemanagement/RoleManagementView.vue:528](../../vue/src/views/console/system/rolemanagement/RoleManagementView.vue#L528)

## API-007

**`editRole` — POST `/api/auth/editRole`**

Source: [vue/src/api/auth/auth.ts:44](../../vue/src/api/auth/auth.ts#L44). Service: `spring:8080`.

```ts
editRole(data: Role): Promise<void>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `data`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/AuthController.java:263](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/AuthController.java#L263) — `editRole` returns `Result<Void>`.
- Backend parameter declarations: `@RequestBody final Role role`.

Current consumers: [vue/src/views/console/system/rolemanagement/RoleManagementView.vue:451](../../vue/src/views/console/system/rolemanagement/RoleManagementView.vue#L451); [vue/src/views/console/system/rolemanagement/RoleManagementView.vue:525](../../vue/src/views/console/system/rolemanagement/RoleManagementView.vue#L525)

## API-008

**`deleteRole` — POST `/api/auth/deleteRole`**

Source: [vue/src/api/auth/auth.ts:52](../../vue/src/api/auth/auth.ts#L52). Service: `spring:8080`.

```ts
deleteRole(roleId: number): Promise<void>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `{ id: roleId }`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/AuthController.java:303](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/AuthController.java#L303) — `deleteRole` returns `Result<Void>`.
- Backend parameter declarations: `@RequestBody final Role role`.

Current consumers: [vue/src/views/console/system/rolemanagement/RoleManagementView.vue:552](../../vue/src/views/console/system/rolemanagement/RoleManagementView.vue#L552)

## API-009

**`updateRolePermissions` — POST `/api/auth/updateRolePermissions`**

Source: [vue/src/api/auth/auth.ts:60](../../vue/src/api/auth/auth.ts#L60). Service: `spring:8080`.

```ts
updateRolePermissions(data: {
  roleId: number
  permissionIds: number[]
}): Promise<void>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `data`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: no matching method/path in the checked-in controllers; unavailable contract, not an implementation promise.

NO BACKEND ROUTE in this checkout. The implemented route is updateRoleRouters with { roleId, routerIds }; this wrapper is not an alias for it.

Current consumers: **No current application reference found** (retained exported API surface).

## API-010

**`updateRoleRouters` — POST `/api/auth/updateRoleRouters`**

Source: [vue/src/api/auth/auth.ts:71](../../vue/src/api/auth/auth.ts#L71). Service: `spring:8080`.

```ts
updateRoleRouters(data: { roleId: number; routerIds: number[] }): Promise<void>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `data`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/AuthController.java:281](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/AuthController.java#L281) — `updateRolePermissions` returns `Result<Void>`.
- Backend parameter declarations: `@RequestBody final RolePermissionUpdateResponse rolePermissionUpdateResponse`.

Backend accepts { roleId: Long, routerIds: List<Integer> }. Caller includes ancestor menu IDs in the selection; preserve that rule when rebuilding the permission tree.

Current consumers: [vue/src/views/console/system/rolemanagement/RoleManagementView.vue:703](../../vue/src/views/console/system/rolemanagement/RoleManagementView.vue#L703)

## API-011

**`updateUserRole` — POST `/api/auth/updateUserRole`**

Source: [vue/src/api/auth/auth.ts:79](../../vue/src/api/auth/auth.ts#L79). Service: `spring:8080`.

```ts
updateUserRole(data: { userId: number; roleId: number }): Promise<void>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `data`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: no matching method/path in the checked-in controllers; unavailable contract, not an implementation promise.

NO BACKEND ROUTE in this checkout. User editing passes roleId through updateUser instead.

Current consumers: **No current application reference found** (retained exported API surface).

## API-012

**`register` — POST `/api/auth/register`**

Source: [vue/src/api/auth/auth.ts:86](../../vue/src/api/auth/auth.ts#L86). Service: `spring:8080`.

```ts
register(data: {
  name: string
  email: string
  password: string
  code: string
}): Promise<void>
```

- Authentication: Public in client; attaches token if present.
- Query: `none`.
- Body: `{ ...data }`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/AuthController.java:86](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/AuthController.java#L86) — `registerUser` returns `Result<String>`.
- Backend parameter declarations: `@RequestBody final RegistrationDTO request`.

Backend Result.success(String) places success text in message with no data, not a string payload. Frontend declares Promise<void>, but shared client resolves the envelope. Public; not exercised.

Current consumers: [vue/src/views/front/register/RegisterView.vue:139](../../vue/src/views/front/register/RegisterView.vue#L139)

## API-013

**`sendRegisterEmail` — POST `/api/auth/sendRegisterEmail`**

Source: [vue/src/api/auth/auth.ts:98](../../vue/src/api/auth/auth.ts#L98). Service: `spring:8080`.

```ts
sendRegisterEmail(data: { email: string }): Promise<void>
```

- Authentication: Public in client; attaches token if present.
- Query: `none`.
- Body: `{ email: data.email }`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/AuthController.java:58](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/AuthController.java#L58) — `registerSendEmail` returns `Result<Void>`.
- Backend parameter declarations: `@RequestBody final RegistrationDTO request`.

Public registration-email request: email required by wrapper. Success text lives in envelope.message. Requires SMTP and writes verification-code state; not exercised.

Current consumers: [vue/src/views/front/register/RegisterView.vue:157](../../vue/src/views/front/register/RegisterView.vue#L157)

## API-014

**`getSalaryHotJob` — GET `/api/v1/boss/user/salary-hot-job`**

Source: [vue/src/api/boss/user/user.ts:9](../../vue/src/api/boss/user/user.ts#L9). Service: `spark:8082`.

```ts
getSalaryHotJob(limit: number): Promise<SalaryJob[]>
```

- Authentication: Bearer token required by client.
- Query: `{ limit }`.
- Body: `none`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spark/src/main/java/com/david/hlp/spark/controller/User/CityAndPositonController.java:21](../../JDK17SERVE/spark/src/main/java/com/david/hlp/spark/controller/User/CityAndPositonController.java#L21) — `getSalaryHotJob` returns `List<SalaryJob>`.
- Backend parameter declarations: `@RequestParam(defaultValue = "10") Long limit`.

Raw JSON array, not Result<T>. Backend limit defaults to 10 (Long). No current frontend call site found.

Current consumers: **No current application reference found** (retained exported API surface).

## API-015

**`getTwoDimensionalAnalysisChart` — GET `/api/v1/boss/user/two-dimensional-analysis-chart`**

Source: [vue/src/api/boss/user/user.ts:21](../../vue/src/api/boss/user/user.ts#L21). Service: `spark:8082`.

```ts
getTwoDimensionalAnalysisChart(cityName: string, positionName: string, xAxis: string, yAxis: string): Promise<JobData[]>
```

- Authentication: Bearer token required by client.
- Query: `{ cityName, positionName, xAxis, yAxis }`.
- Body: `none`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spark/src/main/java/com/david/hlp/spark/controller/User/CityAndPositonController.java:30](../../JDK17SERVE/spark/src/main/java/com/david/hlp/spark/controller/User/CityAndPositonController.java#L30) — `getJobAnalysisData` returns `List<JobAnalysisData>`.
- Backend parameter declarations: `@RequestParam(required = false, defaultValue = "all") String cityName, @RequestParam(required = false, defaultValue = "all") String positionName, @RequestParam(required = false, defaultValue = "salary_value") String xAxis, @RequestParam(required = false, defaultValue = "degree_value") String yAxis`.

Raw JSON array. Backend defaults: cityName=all, positionName=all, xAxis=salary_value, yAxis=degree_value. JobAnalysisData has no industry field although frontend JobData requires it. No current frontend call site found.

Current consumers: **No current application reference found** (retained exported API surface).

## API-016

**`getCityNameList` — GET `/api/v1/boss/basic/city-name-list`**

Source: [vue/src/api/boss/user/user.ts:36](../../vue/src/api/boss/user/user.ts#L36). Service: `spark:8082`.

```ts
getCityNameList(): Promise<string[]>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `none`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spark/src/main/java/com/david/hlp/spark/controller/User/ListBasicSetController.java:19](../../JDK17SERVE/spark/src/main/java/com/david/hlp/spark/controller/User/ListBasicSetController.java#L19) — `getCityNameList` returns `List<String>`.
- Backend parameter declarations: `none`.

Raw string array; an empty array is valid. No current frontend call site found.

Current consumers: **No current application reference found** (retained exported API surface).

## API-017

**`getPositionNameList` — GET `/api/v1/boss/basic/position-name-list`**

Source: [vue/src/api/boss/user/user.ts:45](../../vue/src/api/boss/user/user.ts#L45). Service: `spark:8082`.

```ts
getPositionNameList(cityName?: string): Promise<string[]>
```

- Authentication: Bearer token required by client.
- Query: `{ cityName }`.
- Body: `none`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spark/src/main/java/com/david/hlp/spark/controller/User/ListBasicSetController.java:24](../../JDK17SERVE/spark/src/main/java/com/david/hlp/spark/controller/User/ListBasicSetController.java#L24) — `getPositionNameList` returns `List<String>`.
- Backend parameter declarations: `@RequestParam(defaultValue = "all") String cityName`.

Raw string array. Backend cityName defaults to all. No current frontend call site found.

Current consumers: **No current application reference found** (retained exported API surface).

## API-018

**`getCompanyList` — GET `/api/boss/basic/company-list`**

Source: [vue/src/api/boss/user/user.ts:55](../../vue/src/api/boss/user/user.ts#L55). Service: `spring:8080`.

```ts
getCompanyList(cityName: string, positionName: string): Promise<JobData[]>
```

- Authentication: Bearer token required by client.
- Query: `{ cityName, positionName }`.
- Body: `none`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: no matching method/path in the checked-in controllers; unavailable contract, not an implementation promise.

NO BACKEND ROUTE. This old /api/boss prefix also falls through to port 8080, unlike /api/v1/boss on port 8082. No replacement company endpoint was found.

Current consumers: **No current application reference found** (retained exported API surface).

## API-019

**`getThreeDimensionalAnalysisChart` — GET `/api/boss/user/three-dimensional-analysis-chart`**

Source: [vue/src/api/boss/user/user.ts:68](../../vue/src/api/boss/user/user.ts#L68). Service: `spring:8080`.

```ts
getThreeDimensionalAnalysisChart(cityName: string, positionName: string): Promise<JobData[]>
```

- Authentication: Bearer token required by client.
- Query: `{ cityName, positionName }`.
- Body: `none`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: no matching method/path in the checked-in controllers; unavailable contract, not an implementation promise.

NO BACKEND ROUTE. This old /api/boss prefix also falls through to port 8080. No implemented three-dimensional endpoint was found.

Current consumers: **No current application reference found** (retained exported API surface).

## API-020

**`getDashboardStats` — GET `/api/v1/system/logs/dashboard/stats`**

Source: [vue/src/api/log/accesslog.ts:7](../../vue/src/api/log/accesslog.ts#L7). Service: `spring:8080`.

```ts
getDashboardStats(): Promise<DashboardStats>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `none`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/LogController.java:37](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/LogController.java#L37) — `getDashboardStats` returns `ResponseEntity<Map<String, Object>>`.
- Backend parameter declarations: `none`.

Shares the same HTTP operation with getAccessLogStats. Catches errors and resolves a complete zero/default DashboardStats object. Main API also substitutes defaults on upstream failure; 200 does not establish live statistics.

Current consumers: **No current application reference found** (retained exported API surface).

## API-021

**`getAccessLogStats` — GET `/api/v1/system/logs/dashboard/stats`**

Source: [vue/src/api/log/accesslog.ts:24](../../vue/src/api/log/accesslog.ts#L24). Service: `spring:8080`.

```ts
getAccessLogStats(): Promise<AccessLogStats>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `none`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/LogController.java:37](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/LogController.java#L37) — `getDashboardStats` returns `ResponseEntity<Map<String, Object>>`.
- Backend parameter declarations: `none`.

Calls dashboard/stats, NOT the backend /stats endpoint. Adapts heatMapData, geoStats, deviceStats and overview to AccessLogStats. Reads httpMethodStatistics and weekdayStatistics, which are not declared on DashboardStats. Errors resolve zero/empty fallback data. Exact adapter is archived in contracts.md.

Current consumers: [vue/src/views/console/system/dashboard/DashboardView.vue:896](../../vue/src/views/console/system/dashboard/DashboardView.vue#L896)

## API-022

**`getHeatMapData` — GET `/api/v1/system/logs/heatmap`**

Source: [vue/src/api/log/accesslog.ts:69](../../vue/src/api/log/accesslog.ts#L69). Service: `spring:8080`.

```ts
getHeatMapData(): Promise<any>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `none`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/LogController.java:87](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/LogController.java#L87) — `getHeatMapData` returns `ResponseEntity<Map<String, Object>>`.
- Backend parameter declarations: `none`.

Raw map matching backend HeatMapData when present; resolves {} on errors/empty response. Main API can also replace upstream errors with {}.

Current consumers: **No current application reference found** (retained exported API surface).

## API-023

**`getIpStatistics` — GET `/api/v1/system/logs/ip-statistics`**

Source: [vue/src/api/log/accesslog.ts:85](../../vue/src/api/log/accesslog.ts#L85). Service: `spring:8080`.

```ts
getIpStatistics(): Promise<any>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `none`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/LogController.java:102](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/LogController.java#L102) — `getIpStatistics` returns `ResponseEntity<Map<String, Object>>`.
- Backend parameter declarations: `none`.

Raw map matching backend IpStatistics when present; resolves {} on errors/empty response. Main API can also replace upstream errors with {}.

Current consumers: **No current application reference found** (retained exported API surface).

## API-024

**`getHttpMethodStatistics` — GET `/api/v1/system/logs/http-methods`**

Source: [vue/src/api/log/accesslog.ts:101](../../vue/src/api/log/accesslog.ts#L101). Service: `spring:8080`.

```ts
getHttpMethodStatistics(): Promise<any>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `none`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/LogController.java:117](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/LogController.java#L117) — `getHttpMethodStatistics` returns `ResponseEntity<Map<String, Object>>`.
- Backend parameter declarations: `none`.

Raw map matching backend HttpMethodStatistics when present; resolves {} on errors/empty response. Main API can also replace upstream errors with {}.

Current consumers: **No current application reference found** (retained exported API surface).

## API-025

**`getBrowserStatistics` — GET `/api/v1/system/logs/browser-usage`**

Source: [vue/src/api/log/accesslog.ts:117](../../vue/src/api/log/accesslog.ts#L117). Service: `spring:8080`.

```ts
getBrowserStatistics(): Promise<any>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `none`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/LogController.java:132](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/LogController.java#L132) — `getBrowserStatistics` returns `ResponseEntity<Map<String, Object>>`.
- Backend parameter declarations: `none`.

Raw map matching backend BrowserStatistics when present; resolves {} on errors/empty response. Main API can also replace upstream errors with {}.

Current consumers: **No current application reference found** (retained exported API surface).

## API-026

**`triggerLogAnalysis` — POST `/api/v1/system/logs/analysis/trigger`**

Source: [vue/src/api/log/accesslog.ts:133](../../vue/src/api/log/accesslog.ts#L133). Service: `spring:8080`.

```ts
triggerLogAnalysis(): Promise<string>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `none`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/LogController.java:147](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/LogController.java#L147) — `triggerAnalysis` returns `ResponseEntity<String>`.
- Backend parameter declarations: `none`.

Mutating asynchronous trigger. Response text only acknowledges submission; it is not completion evidence. Frontend catches errors and returns a failure string; the gateway can also send HTTP 200 with failure text. Not invoked during documentation.

Current consumers: **No current application reference found** (retained exported API surface).

## API-027

**`checkServiceHealth` — GET `/api/v1/system/logs/health`**

Source: [vue/src/api/log/accesslog.ts:149](../../vue/src/api/log/accesslog.ts#L149). Service: `spring:8080`.

```ts
checkServiceHealth(): Promise<any>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `none`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/LogController.java:162](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/LogController.java#L162) — `healthCheck` returns `ResponseEntity<Map<String, Object>>`.
- Backend parameter declarations: `none`.

Raw map: timestamp, status and sparkService; sparkResponse on success, error on failure. Main API returns UP or DEGRADED. Frontend catch fallback is { status: DOWN, error }. No frontend caller found.

Current consumers: **No current application reference found** (retained exported API surface).

## API-028

**`uploadImage` — POST `/api/image/upload`**

Source: [vue/src/api/minio/minio.ts:5](../../vue/src/api/minio/minio.ts#L5). Service: `spring:8080`.

```ts
uploadImage(file: File): Promise<ImageResponse>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `formData`.
- Headers overridden by wrapper: `{
      'Content-Type': 'multipart/form-data'
    }`.
- Response mode: `JSON`; declared result is shown in the signature.
- `formData` contains one `file: File` field; the browser supplies the multipart boundary.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/minio/controller/ImageController.java:39](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/minio/controller/ImageController.java#L39) — `uploadImage` returns `Result<ImageResponse>`.
- Backend parameter declarations: `@RequestParam("file") MultipartFile file`.

Multipart field file; backend rejects empty files, non-image content types and missing original filename. Stored filename is userId_originalFilename. .d.ts declares Result<ImageResponse> but implementation returns unwrapped ImageResponse. Requires MinIO; not exercised.

Current consumers: [vue/src/views/console/system/usermanagement/components/AddUser.vue:451](../../vue/src/views/console/system/usermanagement/components/AddUser.vue#L451); [vue/src/views/console/system/usermanagement/components/EditUser.vue:364](../../vue/src/views/console/system/usermanagement/components/EditUser.vue#L364)

## API-029

**`getImageUrl` — GET `/api/image/url/${fileName}`**

Source: [vue/src/api/minio/minio.ts:19](../../vue/src/api/minio/minio.ts#L19). Service: `spring:8080`.

```ts
getImageUrl(fileName: string): Promise<ImageResponse>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `none`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/minio/controller/ImageController.java:134](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/minio/controller/ImageController.java#L134) — `getImageUrl` returns `Result<ImageResponse>`.
- Backend parameter declarations: `@PathVariable("fileName") String fileName`.

Result<ImageResponse> on the wire, unwrapped ImageResponse at runtime. .d.ts incorrectly adds another Result wrapper. Returned url may be a storage URL, not an application endpoint.

Current consumers: [vue/src/components/common/UserQuickView.vue:254](../../vue/src/components/common/UserQuickView.vue#L254); [vue/src/views/console/system/usermanagement/UserManagementView.vue:406](../../vue/src/views/console/system/usermanagement/UserManagementView.vue#L406); [vue/src/views/console/system/usermanagement/components/AddUser.vue:425](../../vue/src/views/console/system/usermanagement/components/AddUser.vue#L425); [vue/src/views/console/system/usermanagement/components/EditUser.vue:344](../../vue/src/views/console/system/usermanagement/components/EditUser.vue#L344)

## API-030

**`deleteImage` — DELETE `/api/image/delete`**

Source: [vue/src/api/minio/minio.ts:27](../../vue/src/api/minio/minio.ts#L27). Service: `spring:8080`.

```ts
deleteImage(fileName: string): inferred (not explicitly declared)
```

- Authentication: Bearer token required by client.
- Query: `{ fileName }`.
- Body: `none`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/minio/controller/ImageController.java:110](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/minio/controller/ImageController.java#L110) — `deleteImage` returns `Result<ImageResponse>`.
- Backend parameter declarations: `@RequestParam("fileName") String fileName`.

Backend Result<ImageResponse>; success may omit url although frontend ImageResponse requires it. Mutation; not exercised.

Current consumers: **No current application reference found** (retained exported API surface).

## API-031

**`getImage` — GET `/api/image/view/${fileName}`**

Source: [vue/src/api/minio/minio.ts:36](../../vue/src/api/minio/minio.ts#L36). Service: `spring:8080`.

```ts
getImage(fileName: string): inferred (not explicitly declared)
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `none`.
- Headers overridden by wrapper: `none`.
- Response mode: `'blob'`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/minio/controller/ImageController.java:87](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/minio/controller/ImageController.java#L87) — `viewImage` returns `Result<InputStreamResource>`.
- Backend parameter declarations: `@PathVariable("fileName") String fileName`.

Client requests responseType=blob. Backend uses Result.file with image Content-Type but a ResultBody<InputStreamResource> body, not a bare ResponseEntity<Resource>. Binary response compatibility is unverified; do not infer it works from the Blob declaration.

Current consumers: **No current application reference found** (retained exported API surface).

## API-032

**`resumeExport` — POST `${VITE_EXPORT_URL}`**

Source: [vue/src/api/modules/resume.ts:11](../../vue/src/api/modules/resume.ts#L11). Service: `external`.

```ts
resumeExport(data: IResumeConfig): inferred (not explicitly declared)
```

- Authentication: No Authorization header.
- Query: `none`.
- Body: `JSON.stringify(data)`.
- Headers overridden by wrapper: `{
      'Content-Type': 'application/json',
    }`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: external implementation is not checked in.

External POST; bypasses shared Axios authentication, timeout and error handling. Sends HTML content, CSS style, stylesheet link, template name, and type (0=PDF, 1=PNG). Calls res.json() without checking res.ok. Caller expects pdf.data or picture.data byte arrays. External implementation is not in this repository; not exercised.

Current consumers: [vue/src/views/resume/hook.ts:121](../../vue/src/views/resume/hook.ts#L121)

## API-033

**`getExportCount` — GET `${VITE_UPSTASH_BASE_URL}/get/count`**

Source: [vue/src/api/modules/resume.ts:22](../../vue/src/api/modules/resume.ts#L22). Service: `external`.

```ts
getExportCount(): inferred (not explicitly declared)
```

- Authentication: Authorization: VITE_UPSTASH_GET_TOKEN (verbatim).
- Query: `none`.
- Body: `none`.
- Headers overridden by wrapper: `{
        Authorization: token,
      }`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: external implementation is not checked in.

External GET with Authorization set verbatim to VITE_UPSTASH_GET_TOKEN (no Bearer added). Returns result as-is from JSON; numeric text uses parseInt; missing config, bad status, HTML, empty body, invalid JSON/result and network failure resolve 0. Return type is not explicitly declared; it is not guaranteed numeric for JSON result. Not exercised.

Current consumers: [vue/src/components/exportTotal.vue:9](../../vue/src/components/exportTotal.vue#L9)

## API-034

**`getResumes` — GET `/api/resume`**

Source: [vue/src/api/resume/resume.ts:7](../../vue/src/api/resume/resume.ts#L7). Service: `spring:8080`.

```ts
getResumes(): Promise<Resume[]>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `none`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeController.java:24](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeController.java#L24) — `getResumes` returns `Result<List<Resume>>`.
- Backend parameter declarations: `none`.

Current consumers: [vue/src/views/resume/resumelist.vue:97](../../vue/src/views/resume/resumelist.vue#L97)

## API-035

**`getResumeById` — GET `/api/resume/${id}`**

Source: [vue/src/api/resume/resume.ts:18](../../vue/src/api/resume/resume.ts#L18). Service: `spring:8080`.

```ts
getResumeById(id: string): Promise<Resume>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `none`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeController.java:33](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeController.java#L33) — `getResumeById` returns `Result<Resume>`.
- Backend parameter declarations: `@PathVariable String id`.

Current consumers: [vue/src/store/modules/editor.ts:33](../../vue/src/store/modules/editor.ts#L33); [vue/src/views/resume/components/version-control/resumeVersionControl.vue:789](../../vue/src/views/resume/components/version-control/resumeVersionControl.vue#L789)

## API-036

**`createResume` — POST `/api/resume`**

Source: [vue/src/api/resume/resume.ts:29](../../vue/src/api/resume/resume.ts#L29). Service: `spring:8080`.

```ts
createResume(data: Partial<Resume>): Promise<Resume>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `data`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeController.java:43](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeController.java#L43) — `createResume` returns `Result<Resume>`.
- Backend parameter declarations: `@RequestBody Resume resume`.

Backend auto-creates initial main commit. IMPORTANT: current service does not set docType=meta, and callers send only title/content. Reads filter meta, so a successful create can become invisible. Frontend Resume omits docType. No create performed during documentation.

Current consumers: [vue/src/views/resume/resumelist.vue:107](../../vue/src/views/resume/resumelist.vue#L107); [vue/src/views/resume/resumelist.vue:139](../../vue/src/views/resume/resumelist.vue#L139)

## API-037

**`updateResume` — PUT `/api/resume/${id}`**

Source: [vue/src/api/resume/resume.ts:42](../../vue/src/api/resume/resume.ts#L42). Service: `spring:8080`.

```ts
updateResume(id: string, data: Partial<Resume>): Promise<Resume>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `data`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeController.java:52](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeController.java#L52) — `updateResume` returns `Result<Resume>`.
- Backend parameter declarations: `@PathVariable String id, @RequestBody Resume resume`.

Current backend only commits content; it does not assign incoming title. Rename can appear successful without persisting. Sending only a title can also pass null content into commit. Current callers send title and content or the full resume. Not exercised.

Current consumers: [vue/src/store/modules/editor.ts:60](../../vue/src/store/modules/editor.ts#L60); [vue/src/views/resume/resumelist.vue:172](../../vue/src/views/resume/resumelist.vue#L172)

## API-038

**`deleteResume` — DELETE `/api/resume/${id}`**

Source: [vue/src/api/resume/resume.ts:54](../../vue/src/api/resume/resume.ts#L54). Service: `spring:8080`.

```ts
deleteResume(id: string): Promise<void>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `none`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeController.java:63](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeController.java#L63) — `deleteResume` returns `Result<Void>`.
- Backend parameter declarations: `@PathVariable String id`.

Deletes owned metadata and associated commit records; this is destructive. Backend returns Result<Void>, leaving the shared client to return the envelope when data is null. Not exercised.

Current consumers: [vue/src/views/resume/resumelist.vue:128](../../vue/src/views/resume/resumelist.vue#L128); [vue/src/views/resume/resumelist.vue:215](../../vue/src/views/resume/resumelist.vue#L215)

## API-039

**`initVersionControl` — POST `/api/resume/${resumeId}/version/init`**

Source: [vue/src/api/resume/version.ts:23](../../vue/src/api/resume/version.ts#L23). Service: `spring:8080`.

```ts
initVersionControl(resumeId: string, content: string = ''): Promise<Resume>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `{ content }`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: no matching method/path in the checked-in controllers; unavailable contract, not an implementation promise.

NO BACKEND ROUTE. Creation already auto-initializes version metadata; do not rely on this legacy wrapper.

Current consumers: **No current application reference found** (retained exported API surface).

## API-040

**`commitChanges` — POST `/api/resume/${resumeId}/version/commit`**

Source: [vue/src/api/resume/version.ts:34](../../vue/src/api/resume/version.ts#L34). Service: `spring:8080`.

```ts
commitChanges(resumeId: string, message: string, content: string): Promise<string>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `{ message, content }`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeVersionController.java:31](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeVersionController.java#L31) — `commit` returns `Result<String>`.
- Backend parameter declarations: `@PathVariable String resumeId, @RequestBody Map<String, String> request`.

Backend requires a nonblank message. Result.success(String) selects the message overload: commit ID is in envelope.message and data is null, so the current client resolves an envelope instead of the declared string. Mutation not exercised.

Current consumers: [vue/src/views/resume/components/version-control/resumeVersionControl.vue:629](../../vue/src/views/resume/components/version-control/resumeVersionControl.vue#L629)

## API-041

**`createBranch` — POST `/api/resume/${resumeId}/version/branch`**

Source: [vue/src/api/resume/version.ts:45](../../vue/src/api/resume/version.ts#L45). Service: `spring:8080`.

```ts
createBranch(resumeId: string, branchName: string): Promise<void>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `{ name: branchName }`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeVersionController.java:57](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeVersionController.java#L57) — `createBranch` returns `Result<Void>`.
- Backend parameter declarations: `@PathVariable String resumeId, @RequestBody Map<String, String> request`.

Backend requires a nonblank branch name. Mutation; not exercised. Result<Void> leaves the envelope intact in the current client.

Current consumers: [vue/src/views/resume/components/version-control/resumeVersionControl.vue:651](../../vue/src/views/resume/components/version-control/resumeVersionControl.vue#L651)

## API-042

**`switchBranch` — POST `/api/resume/${resumeId}/version/switch`**

Source: [vue/src/api/resume/version.ts:52](../../vue/src/api/resume/version.ts#L52). Service: `spring:8080`.

```ts
switchBranch(resumeId: string, branch: string): Promise<void>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `{ branch }`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeVersionController.java:81](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeVersionController.java#L81) — `switchBranch` returns `Result<Void>`.
- Backend parameter declarations: `@PathVariable String resumeId, @RequestBody Map<String, String> request`.

Switches active content/branch. Mutation; not exercised. Result<Void> leaves the envelope intact.

Current consumers: [vue/src/views/resume/components/version-control/resumeVersionControl.vue:667](../../vue/src/views/resume/components/version-control/resumeVersionControl.vue#L667)

## API-043

**`mergeBranch` — POST `/api/resume/${resumeId}/version/merge`**

Source: [vue/src/api/resume/version.ts:63](../../vue/src/api/resume/version.ts#L63). Service: `spring:8080`.

```ts
mergeBranch(resumeId: string, source: string, target: string): Promise<string>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `{ source, target }`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeVersionController.java:101](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeVersionController.java#L101) — `mergeBranch` returns `Result<String>`.
- Backend parameter declarations: `@PathVariable String resumeId, @RequestBody Map<String, String> request`.

Backend Result.success(String) puts merge commit ID in envelope.message, not data; current client does not return the declared string. Merges and changes version state; not exercised.

Current consumers: [vue/src/views/resume/components/version-control/resumeVersionControl.vue:697](../../vue/src/views/resume/components/version-control/resumeVersionControl.vue#L697)

## API-044

**`getCommitHistory` — GET `/api/resume/${resumeId}/version/history`**

Source: [vue/src/api/resume/version.ts:74](../../vue/src/api/resume/version.ts#L74). Service: `spring:8080`.

```ts
getCommitHistory(resumeId: string): Promise<ResumeCommit[]>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `none`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeVersionController.java:123](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeVersionController.java#L123) — `getCommitHistory` returns `Result<List<Resume>>`.
- Backend parameter declarations: `@PathVariable String resumeId`.

Backend returns Result<List<Resume>> commit documents, not a dedicated ResumeCommit DTO. Date fields arrive as JSON date strings, not JavaScript Date instances.

Current consumers: [vue/src/views/resume/components/version-control/resumeVersionControl.vue:811](../../vue/src/views/resume/components/version-control/resumeVersionControl.vue#L811)

## API-045

**`checkoutCommit` — POST `/api/resume/${resumeId}/version/checkout`**

Source: [vue/src/api/resume/version.ts:84](../../vue/src/api/resume/version.ts#L84). Service: `spring:8080`.

```ts
checkoutCommit(resumeId: string, commitId: string): Promise<void>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `{ commitId }`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeVersionController.java:141](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeVersionController.java#L141) — `checkout` returns `Result<Void>`.
- Backend parameter declarations: `@PathVariable String resumeId, @RequestBody Map<String, String> request`.

Changes current content/HEAD. Mutation; not exercised. Result<Void> leaves the envelope intact.

Current consumers: [vue/src/views/resume/components/version-control/resumeVersionControl.vue:762](../../vue/src/views/resume/components/version-control/resumeVersionControl.vue#L762); [vue/src/views/resume/components/version-control/resumeVersionControl.vue:845](../../vue/src/views/resume/components/version-control/resumeVersionControl.vue#L845)

## API-046

**`resetToCommit` — POST `/api/resume/${resumeId}/version/reset`**

Source: [vue/src/api/resume/version.ts:95](../../vue/src/api/resume/version.ts#L95). Service: `spring:8080`.

```ts
resetToCommit(resumeId: string, commitId: string): Promise<void>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `{ commitId }`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeVersionController.java:199](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeVersionController.java#L199) — `resetToCommit` returns `Result<Void>`.
- Backend parameter declarations: `@PathVariable String resumeId, @RequestBody Map<String, String> request`.

Requires nonblank commitId. Hard reset changes branch history/content and records a rollback operation. Not exercised. Result<Void> leaves the envelope intact.

Current consumers: [vue/src/views/resume/components/version-control/resumeVersionControl.vue:882](../../vue/src/views/resume/components/version-control/resumeVersionControl.vue#L882)

## API-047

**`revertCommit` — POST `/api/resume/${resumeId}/version/revert`**

Source: [vue/src/api/resume/version.ts:106](../../vue/src/api/resume/version.ts#L106). Service: `spring:8080`.

```ts
revertCommit(resumeId: string, commitId: string): Promise<string>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `{ commitId }`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeVersionController.java:228](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeVersionController.java#L228) — `revertCommit` returns `Result<String>`.
- Backend parameter declarations: `@PathVariable String resumeId, @RequestBody Map<String, String> request`.

Requires nonblank commitId. Creates a revert commit. Result.success(String) puts returned ID in message instead of data, so declared string differs from actual shared-client result. Not exercised.

Current consumers: [vue/src/views/resume/components/version-control/resumeVersionControl.vue:923](../../vue/src/views/resume/components/version-control/resumeVersionControl.vue#L923)

## API-048

**`getRecentCommits` — GET `/api/resume/${resumeId}/version/recent`**

Source: [vue/src/api/resume/version.ts:117](../../vue/src/api/resume/version.ts#L117). Service: `spring:8080`.

```ts
getRecentCommits(resumeId: string, limit = 10): Promise<ResumeCommit[]>
```

- Authentication: Bearer token required by client.
- Query: `{ limit }`.
- Body: `none`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeVersionController.java:257](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeVersionController.java#L257) — `getRecentCommits` returns `Result<List<Resume>>`.
- Backend parameter declarations: `@PathVariable String resumeId, @RequestParam(defaultValue = "10") int limit`.

Backend returns Result<List<Resume>>. limit defaults to 10; values <=0 or >50 are reset to 10. Date fields are strings on the wire.

Current consumers: [vue/src/views/resume/components/version-control/resumeVersionControl.vue:831](../../vue/src/views/resume/components/version-control/resumeVersionControl.vue#L831)

## API-049

**`getCommitsBetween` — GET `/api/resume/${resumeId}/version/diff`**

Source: [vue/src/api/resume/version.ts:128](../../vue/src/api/resume/version.ts#L128). Service: `spring:8080`.

```ts
getCommitsBetween(resumeId: string, fromCommit: string, toCommit: string): Promise<ResumeCommit[]>
```

- Authentication: Bearer token required by client.
- Query: `{ fromCommit, toCommit }`.
- Body: `none`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeVersionController.java:280](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeVersionController.java#L280) — `getCommitsBetween` returns `Result<List<Resume>>`.
- Backend parameter declarations: `@PathVariable String resumeId, @RequestParam String fromCommit, @RequestParam String toCommit`.

Backend returns a list of commits between the two IDs, not a textual diff. Date fields are strings on the wire. No current frontend call site found.

Current consumers: **No current application reference found** (retained exported API surface).

## API-050

**`stashChanges` — POST `/api/resume/${resumeId}/version/stash`**

Source: [vue/src/api/resume/version.ts:139](../../vue/src/api/resume/version.ts#L139). Service: `spring:8080`.

```ts
stashChanges(resumeId: string, content: string): Promise<void>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `{ content }`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeVersionController.java:161](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeVersionController.java#L161) — `stash` returns `Result<Void>`.
- Backend parameter declarations: `@PathVariable String resumeId, @RequestBody Map<String, String> request`.

Stashes content with message, branch, head and timestamp metadata. Mutation; not exercised. Result<Void> leaves the envelope intact.

Current consumers: [vue/src/views/resume/components/version-control/resumeVersionControl.vue:725](../../vue/src/views/resume/components/version-control/resumeVersionControl.vue#L725)

## API-051

**`stashPop` — POST `/api/resume/${resumeId}/version/stash/pop`**

Source: [vue/src/api/resume/version.ts:150](../../vue/src/api/resume/version.ts#L150). Service: `spring:8080`.

```ts
stashPop(resumeId: string): Promise<string>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `none`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeVersionController.java:181](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeVersionController.java#L181) — `stashPop` returns `Result<String>`.
- Backend parameter declarations: `@PathVariable String resumeId`.

Mutates stash/content. Result.success(String) puts content in envelope.message rather than data, so actual client result differs from declared string. Not exercised.

Current consumers: [vue/src/views/resume/components/version-control/resumeVersionControl.vue:740](../../vue/src/views/resume/components/version-control/resumeVersionControl.vue#L740)

## API-052

**`getCommitDebugInfo` — GET `/api/resume/${resumeId}/version/debug/commit/${commitId}`**

Source: [vue/src/api/resume/version.ts:160](../../vue/src/api/resume/version.ts#L160). Service: `spring:8080`.

```ts
getCommitDebugInfo(resumeId: string, commitId: string): Promise<any>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `none`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeVersionController.java:301](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeVersionController.java#L301) — `getCommitDebugInfo` returns `Result<Map<String, Object>>`.
- Backend parameter declarations: `@PathVariable String resumeId, @PathVariable String commitId`.

Open map: resumeExists, currentBranch, headCommit, commitExists; if found: commitMessage, commitBranch, commitType, commitTime, parentCommits, parentCount, isInitialCommit, parentCommitsInfo[{parentId,parentExists,parentMessage?,parentTime?}]; success, error on exception. Missing-related fields can be absent.

Current consumers: [vue/src/views/resume/components/version-control/resumeVersionControl.vue:968](../../vue/src/views/resume/components/version-control/resumeVersionControl.vue#L968)

## API-053

**`validateVersionControl` — GET `/api/resume/${resumeId}/version/debug/validate`**

Source: [vue/src/api/resume/version.ts:170](../../vue/src/api/resume/version.ts#L170). Service: `spring:8080`.

```ts
validateVersionControl(resumeId: string): Promise<any>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `none`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeVersionController.java:320](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeVersionController.java#L320) — `validateVersionControl` returns `Result<Map<String, Object>>`.
- Backend parameter declarations: `@PathVariable String resumeId`.

Open map: errors and warnings string arrays, isValid boolean, totalCommits, orphanCommits and corruptedCommits counts, branchCount, stashCount. Early missing-resume path returns errors only; do not require all keys.

Current consumers: [vue/src/views/resume/components/version-control/resumeVersionControl.vue:949](../../vue/src/views/resume/components/version-control/resumeVersionControl.vue#L949)

## API-054

**`fixDuplicateCommitIds` — POST `/api/resume/${resumeId}/version/fix-duplicate-ids`**

Source: [vue/src/api/resume/version.ts:180](../../vue/src/api/resume/version.ts#L180). Service: `spring:8080`.

```ts
fixDuplicateCommitIds(resumeId: string): Promise<any>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `none`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeVersionController.java:339](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/resume/controller/ResumeVersionController.java#L339) — `fixDuplicateIds` returns `Result<Map<String, Object>>`.
- Backend parameter declarations: `@PathVariable String resumeId`.

DESTRUCTIVE repair: if duplicate IDs exist, deletes commit history and reinitializes main. Returns success, actions, message; repaired path also initialCommitId/duplicateIdsCount; failure may return error. This is not a harmless diagnostic. Not exercised.

Current consumers: [vue/src/views/resume/components/version-control/resumeVersionControl.vue:1004](../../vue/src/views/resume/components/version-control/resumeVersionControl.vue#L1004)

## API-055

**`getRoleList` — GET `/api/role/getRoleList?roleName=${roleName || ''}`**

Source: [vue/src/api/role/role.ts:4](../../vue/src/api/role/role.ts#L4). Service: `spring:8080`.

```ts
getRoleList(roleName?: string): Promise<Role[]>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `none`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/RoleController.java:37](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/RoleController.java#L37) — `getRoleList` returns `Result<List<Role>>`.
- Backend parameter declarations: `@RequestParam(required = false)String roleName`.

Distinct from auth/getRoleList. Optional roleName is interpolated directly into the query without URL encoding; use Axios params when rebuilding. Returns Result<List<Role>> on the wire.

Current consumers: [vue/src/views/console/system/usermanagement/UserManagementView.vue:452](../../vue/src/views/console/system/usermanagement/UserManagementView.vue#L452); [vue/src/views/console/system/usermanagement/components/AddUser.vue:336](../../vue/src/views/console/system/usermanagement/components/AddUser.vue#L336); [vue/src/views/console/system/usermanagement/components/AddUser.vue:353](../../vue/src/views/console/system/usermanagement/components/AddUser.vue#L353); [vue/src/views/console/system/usermanagement/components/EditUser.vue:285](../../vue/src/views/console/system/usermanagement/components/EditUser.vue#L285); [vue/src/views/console/system/usermanagement/components/EditUser.vue:322](../../vue/src/views/console/system/usermanagement/components/EditUser.vue#L322)

## API-056

**`getUserRoutes` — GET `/api/auth/getRouters`**

Source: [vue/src/api/router/router.ts:3](../../vue/src/api/router/router.ts#L3). Service: `spring:8080`.

```ts
getUserRoutes(): Promise<Router[]>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `none`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/AuthController.java:134](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/AuthController.java#L134) — `getRouters` returns `Result<List<Router>>`.
- Backend parameter declarations: `none`.

Current consumers: [vue/src/store/router/routerStore.ts:23](../../vue/src/store/router/routerStore.ts#L23); [vue/src/store/user/userStore.ts:36](../../vue/src/store/user/userStore.ts#L36); [vue/src/views/console/system/menumanagement/MenuManagementView.vue:268](../../vue/src/views/console/system/menumanagement/MenuManagementView.vue#L268); [vue/src/views/console/system/rolemanagement/RoleManagementView.vue:484](../../vue/src/views/console/system/rolemanagement/RoleManagementView.vue#L484)

## API-057

**`editRouter` — POST `/api/auth/editRouter`**

Source: [vue/src/api/router/router.ts:9](../../vue/src/api/router/router.ts#L9). Service: `spring:8080`.

```ts
editRouter(data: Router): Promise<void>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `data`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/AuthController.java:180](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/AuthController.java#L180) — `editRouter` returns `Result<Void>`.
- Backend parameter declarations: `@RequestBody final Router router`.

Current consumers: [vue/src/views/console/system/menumanagement/MenuManagementView.vue:405](../../vue/src/views/console/system/menumanagement/MenuManagementView.vue#L405); [vue/src/views/console/system/menumanagement/MenuManagementView.vue:611](../../vue/src/views/console/system/menumanagement/MenuManagementView.vue#L611)

## API-058

**`addRouter` — POST `/api/auth/addRouter`**

Source: [vue/src/api/router/router.ts:16](../../vue/src/api/router/router.ts#L16). Service: `spring:8080`.

```ts
addRouter(data: Router): Promise<void>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `data`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/AuthController.java:198](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/AuthController.java#L198) — `addRouter` returns `Result<Void>`.
- Backend parameter declarations: `@RequestBody final Router router`.

Current consumers: [vue/src/views/console/system/menumanagement/MenuManagementView.vue:607](../../vue/src/views/console/system/menumanagement/MenuManagementView.vue#L607)

## API-059

**`deleteRouter` — POST `/api/auth/deleteRouter`**

Source: [vue/src/api/router/router.ts:23](../../vue/src/api/router/router.ts#L23). Service: `spring:8080`.

```ts
deleteRouter(data: Router): Promise<void>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `data`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/AuthController.java:216](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/AuthController.java#L216) — `deleteRouter` returns `Result<Void>`.
- Backend parameter declarations: `@RequestBody final Router router`.

Current consumers: [vue/src/views/console/system/menumanagement/MenuManagementView.vue:569](../../vue/src/views/console/system/menumanagement/MenuManagementView.vue#L569)

## API-060

**`getUserManageInfo` — POST `/api/user/getUserManageInfo`**

Source: [vue/src/api/user/user.ts:5](../../vue/src/api/user/user.ts#L5). Service: `spring:8080`.

```ts
getUserManageInfo(number: number, size: number, params?: User): Promise<PageInfo<User>>
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `{
      number,
      size,
      query: params,
    }`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/UserController.java:40](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/UserController.java#L40) — `getUserManageInfo` returns `Result<PageInfo<User>>`.
- Backend parameter declarations: `@RequestBody PageInfo<User> pageInfo`.

Backend PageInfo is zero-based (number=0 is first page), but existing screen passes page=1 for its initial query. Record this mismatch; do not adopt that UI convention as API truth. query is a User-shaped optional object.

Current consumers: [vue/src/views/console/system/usermanagement/UserManagementView.vue:421](../../vue/src/views/console/system/usermanagement/UserManagementView.vue#L421)

## API-061

**`deleteUser` — POST `/api/user/deleteUser`**

Source: [vue/src/api/user/user.ts:20](../../vue/src/api/user/user.ts#L20). Service: `spring:8080`.

```ts
deleteUser(id: number, password: string): inferred (not explicitly declared)
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `{ id, password }`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/UserController.java:61](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/UserController.java#L61) — `deleteUser` returns `Result<Void>`.
- Backend parameter declarations: `@RequestBody DelUser user`.

Body password is the acting administrator/current user password, not the target user password; backend checks it before deletion. Mutation not exercised.

Current consumers: [vue/src/views/console/system/usermanagement/UserManagementView.vue:612](../../vue/src/views/console/system/usermanagement/UserManagementView.vue#L612)

## API-062

**`updateUser` — POST `/api/user/updateUser`**

Source: [vue/src/api/user/user.ts:27](../../vue/src/api/user/user.ts#L27). Service: `spring:8080`.

```ts
updateUser(data: User): inferred (not explicitly declared)
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `data`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/UserController.java:84](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/UserController.java#L84) — `updateUser` returns `Result<Void>`.
- Backend parameter declarations: `@RequestBody User user`.

Backend accepts User including id, profile fields, roleId and password if sent. Frontend type is extensible. Current role edits occur here. Mutation not exercised.

Current consumers: [vue/src/views/console/system/usermanagement/components/EditUser.vue:394](../../vue/src/views/console/system/usermanagement/components/EditUser.vue#L394)

## API-063

**`addUser` — POST `/api/user/addUser`**

Source: [vue/src/api/user/user.ts:34](../../vue/src/api/user/user.ts#L34). Service: `spring:8080`.

```ts
addUser(data: User): inferred (not explicitly declared)
```

- Authentication: Bearer token required by client.
- Query: `none`.
- Body: `data`.
- Headers overridden by wrapper: `none`.
- Response mode: `JSON`; declared result is shown in the signature.
- Backend: [JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/UserController.java:95](../../JDK17SERVE/spring/src/main/java/com/david/hlp/web/system/auth/controller/UserController.java#L95) — `addUser` returns `Result<Void>`.
- Backend parameter declarations: `@RequestBody User user`.

Frontend uses extensible UserBaseInfo; actual backend User also accepts password:string (not explicitly declared in UserBaseInfo). Current form sends password. Mutation not exercised.

Current consumers: [vue/src/views/console/system/usermanagement/components/AddUser.vue:407](../../vue/src/views/console/system/usermanagement/components/AddUser.vue#L407)
