# Safe role constraint deployment

Application startup never migrates data. Do not import system.sql into an existing database; it drops tables.

Before deploying role writes, back up and inspect using the actual database collation:

~~~sql
SELECT role_name, COUNT(*) FROM role GROUP BY role_name HAVING COUNT(*) > 1;
SELECT id, role_name, status FROM role WHERE role_name = 'USER';
SHOW INDEX FROM role;
~~~

Require zero duplicates and exactly one enabled canonical USER. Stop on ambiguity: registration rejects missing, duplicate or disabled USER rather than choosing LIMIT 1. Resolve existing data only under a separately approved plan, never automatically rename/merge/delete roles. New names are trimmed; case/accent equivalence follows database collation.

An operator may explicitly apply once if absent:

~~~sql
ALTER TABLE role ADD CONSTRAINT uk_role_name UNIQUE (role_name);
~~~

The unique constraint is required before concurrent role writes: application validation cannot replace it. MySQL DDL can lock and implicitly commit; schedule separately. Rollback only the constraint via ALTER TABLE role DROP INDEX uk_role_name if required; no automatic data changes.

# Gateway and permission deployment

Nuxt recruitment URLs now go through API port 8080. Only the four existing GET endpoints forward to spark.service.url. Keep analytics port 8082 and its log/analysis endpoints reachable only from the trusted API network (loopback/private network/firewall). Analytics has no application authentication; a publicly exposed analytics listener remains unsafe. No hardcoded credentials were added.

Management access resolves each enabled configured console page's existing permission, not an ADMIN role name. Page access grants existing page read/write actions. The simple user-role selector accepts users/roles page permissions; the full permission/role tree accepts roles/menus page permissions. Own account/role/permission/menu reads remain authenticated reads. Dashboard statistics require the dashboard page permission. Sanitized health remains an authenticated read for settings/profile; no new write endpoint was introduced. Trigger requires separate logs.analysis.trigger authority, never automatically granted; configure/grant explicitly through existing permission management if needed.

Role state and authorities reload each authenticated request without userAuthorities cache, so disabling/deleting roles or changing grants affects already issued sessions on their next request; requests already in flight may finish. Old unused Redis entries need no flush.

Unused Upstash browser token/build definitions were removed. getExportCount remains disabled and returns zero. Never provide private credentials in public VITE variables. Rotation of any previously published token requires deployment evidence and is separate.
