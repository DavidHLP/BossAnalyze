# 角色名唯一约束：显式增量迁移

应用启动不迁移数据库。本流程仅适用 MySQL 8，保留业务记录；不导入带 DROP 的 `database/system.sql` 到现库，不删除/合并/改名角色，不重挂用户。应用的 count 后写无法仲裁并发，唯一索引才是最终保护。

## 准备与备份恢复

1. 明确实际容器、schema、版本、role 表引擎、`role_name` 列 collation 与连接字符集。仅由脚本消费私有配置，不能打印配置值、token 或全量业务行。下述 CLI 凭据由私有 options 文件或受保护环境提供，勿放密码到命令行参数。
2. 在权限 0700 的私有目录，以 `mysqldump --single-transaction --skip-lock-tables --no-tablespaces --set-gtid-purged=OFF --hex-blob <schema>` 备份完整 schema，输出文件 0600。先确认表均为 InnoDB；备份期间不得并行做结构变更。保留原库，不停止现有服务。
3. 用随机后缀创建全新 `boss_role_rehearsal_*` schema，**CREATE DATABASE 不使用 IF NOT EXISTS**；若撞名立即停止，不清理同名库。将备份恢复到新 schema，比较各表行数及原始 role、user、role_permission、router 引用。备份不是恢复证明，成功恢复并核对才是。
4. 不从本手册复制个人机器路径或身份作为生产配置。迁移身份需 SELECT、ALTER/INDEX 及元数据检查权限；备份、建隔离库、恢复测试另需相应权限。日常应用身份不应为迁移提升权限。迁移不需要 CREATE ROUTINE。

## 只读预检查

```sql
SELECT VERSION(), DATABASE();
SELECT column_name, column_type, is_nullable, collation_name
FROM information_schema.columns
WHERE table_schema=DATABASE() AND table_name='role';
SELECT index_name, non_unique, seq_in_index, column_name, sub_part, is_visible
FROM information_schema.statistics
WHERE table_schema=DATABASE() AND table_name='role'
ORDER BY index_name, seq_in_index;
SELECT COUNT(*) FROM (SELECT role_name FROM role GROUP BY role_name HAVING COUNT(*)>1) d;
SELECT COUNT(*) FROM role WHERE role_name IS NULL OR TRIM(role_name)=''
  OR BINARY role_name<>BINARY TRIM(role_name);
SELECT COUNT(*), SUM(status=1), SUM(BINARY role_name=BINARY 'USER')
FROM role WHERE role_name='USER';
SELECT table_name,column_name,referenced_column_name
FROM information_schema.key_column_usage
WHERE referenced_table_schema=DATABASE() AND referenced_table_name='role';
SELECT COUNT(*), COALESCE(MAX(TIMESTAMPDIFF(SECOND,trx_started,NOW())),0)
FROM information_schema.innodb_trx;
SELECT COUNT(*) FROM performance_schema.data_lock_waits;
SELECT object_schema,object_name,lock_type,lock_status
FROM performance_schema.metadata_locks
WHERE lock_status='PENDING' OR (object_schema=DATABASE()
  AND object_name IN ('role','user','role_permission','router'));
```

重名按实际列 collation 分组；默认 USER 必须恰好一行、启用、精确大写。空名、trim 脏值、歧义或孤儿引用须停止，另获数据处置方案，不自动修复。审计实际 FK 之外的逻辑 `role_id` 引用（本库 router 也有），记录行数、完整 role 行与引用映射供执行后私下比较。不要输出账户内容。

完整唯一索引必须 unique、可见、恰好一列 role_name、无前缀；不能只按索引名或索引第一列判断。同名 `uk_role_name` 若定义错误即拒绝，即使另有正确索引。已有其他名字的合格索引则跳过。`utf8mb4_0900_ai_ci` 是 NO PAD：大小写/重音等价，尾空格在 SQL 中可不同；应用 trim 与迁移脏值检查仍必要。

## 隔离验证与执行

隔离库专用验证工具会改变其 role 夹具，必须在全新恢复副本运行；它不会对现库执行迁移。通过受保护环境提供 `MYSQL_PWD`，不要使用 shell trace：

```sh
python database/migrations/verify-role-uniqueness.py \
  --container <mysql-container> --user <operator> \
  --schema <new-boss_role_rehearsal_schema>
```

工具验证首次执行/重跑、同名非唯一/复合/前缀索引拒绝、其他名字正确索引跳过、重名/空白/禁用默认角色拒绝，以及实际 collation。create/create、create/rename、rename/rename 使用独立 mysql 连接与事务，必须观察到第二事务的实际锁等待，然后断言一方提交、另一方 1062。需要 performance_schema 读取权限；真实 MySQL 测试不能由 mock 代替。

隔离通过后，再核现库：备份之后的数据与引用仍一致、无重名/默认角色歧义/孤儿引用，无活动事务、角色相关元数据锁或锁等待，磁盘充足。无法确认时停止现库 DDL，记录具体阻塞；不 kill 连接或重启服务。操作窗口禁止其他迁移操作者并行执行，应用写入仍由 online DDL 原子校验。

```sh
mysql --defaults-extra-file=<private-options-file> --default-character-set=utf8mb4 \
  --batch <explicit-schema> < database/migrations/20261108-role-name-unique.sql
```

**禁止 `--force` 或继续执行错误语句的 runner**。MySQL 无 routine 的条件 SIGNAL 支持有限，本脚本在不合格条件下选择必定非法的 `ABORT MIGRATION ...`，PREPARE 以 1064 停止，错误包含脏值/错误索引原因；没有执行 ALTER。人工审计通过后仍由脚本检查数据，新增唯一索引也会原子拒绝检查后才出现的重名。

会话 `lock_wait_timeout` 与 `innodb_lock_wait_timeout` 均为 5 秒。`ALGORITHM=INPLACE, LOCK=NONE` 不支持时失败，不降级为阻塞表复制。锁超时限制等待，不承诺执行总时限或零停顿；MySQL DDL 隐式提交，不能用事务 rollback 撤销。超时/断连须先回读完整索引和锁状态，确认是否完成，不能盲重试。

执行后回读完整索引、零重复、默认 USER、原行数/完整 role 行/所有引用不变；重跑应跳过。可用已有合成账号安全请求已存在角色名，验证 HTTP 与响应体均 409且无角色写入。该 API 预检查路径与 MySQL 1062 并发结果分别报告，不能声称已通过 API 并发测试。无需往现库添加测试角色或改既有角色；新应用整合与重启由父任务负责。

## Token 与回退

先核 `token` 时间列。已有 `create_time` 则跳过全部 token 迁移；不添加 token 唯一键或 jti，不失效旧会话。仅旧环境为 `created_at` 时另行审计可选迁移，两列并存不能当作已对齐。

应用回退一般保留角色唯一约束。只有另获明确授权且控制角色写入窗口时才 `ALTER TABLE role DROP INDEX uk_role_name`；先确认它是本次创建的索引，不能删除原有其他名字约束。DROP INDEX 也是独立 DDL，删除后并发重名风险恢复。备份只能先恢复隔离库并核对，不能覆盖现库新写入。备份与隔离 schema 保留至父任务完成验收，不自动清理。
