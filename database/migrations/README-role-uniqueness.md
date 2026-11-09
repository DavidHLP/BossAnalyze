# Safe role constraint deployment

Application startup never migrates data. Never import `system.sql` into an existing database: it drops tables.

Follow [the role uniqueness runbook](../../docs/operations/database-role-uniqueness.md) for backup, isolated restore, data/index/lock audits, concurrency checks, explicit migration, and rollback boundaries.

Run `20261108-role-name-unique.sql` with MySQL 8 batch mode **without `--force`**, against the explicitly selected schema. It rejects dirty/default-role ambiguity and wrong same-name index definitions, skips an existing full single-column visible unique index regardless of name, and otherwise adds only `uk_role_name(role_name)` with `ALGORITHM=INPLACE, LOCK=NONE`. No stored routine privileges are required. Do not bypass a failed preflight with a manual ALTER.

Token environments already using `create_time` skip `20261108-token-time.sql`. That optional legacy script is not part of this role migration; do not add token uniqueness or change sessions.

# Gateway and permission deployment

Nuxt recruitment URLs now go through API port 8080. Only the four existing GET endpoints forward to spark.service.url. Keep analytics port 8082 and its log/analysis endpoints reachable only from the trusted API network (loopback/private network/firewall). Analytics has no application authentication; a publicly exposed analytics listener remains unsafe. No hardcoded credentials were added.

Management access resolves each enabled configured console page's existing permission, not an ADMIN role name. Page access grants existing page read/write actions. The simple user-role selector accepts users/roles page permissions; the full permission/role tree accepts roles/menus page permissions. Own account/role/permission/menu reads remain authenticated reads. Dashboard statistics require the dashboard page permission. Sanitized health remains an authenticated read for settings/profile; no new write endpoint was introduced. Trigger requires separate logs.analysis.trigger authority, never automatically granted; configure/grant explicitly through existing permission management if needed.

Role state and authorities reload each authenticated request without userAuthorities cache, so disabling/deleting roles or changing grants affects already issued sessions on their next request; requests already in flight may finish. Old unused Redis entries need no flush.

Unused Upstash browser token/build definitions were removed. getExportCount remains disabled and returns zero. Never provide private credentials in public VITE variables. Rotation of any previously published token requires deployment evidence and is separate.
