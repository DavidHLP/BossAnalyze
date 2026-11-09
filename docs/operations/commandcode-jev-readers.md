# Jev 招聘读侧发布与回退

冻结协议：`docs/specs/commandcode-jev-contract-20261108.md`。此变更只改 reader、招聘详情和 v2 客户端声明；不启动调度、不执行 DDL、不回填历史数据。

## 发布顺序

1. 先发布包含 commons JobArrays 的 crawler 和 analytics reader，再允许任何新 writer 写 JSON 数组。
2. 由数据库负责人在隔离 schema 验证 B 提供的增量 `job_ai_analysis` 迁移后，显式应用至部署目标。应用不做自动 DDL。
3. 再发布 B 的 Java writer 与 A 的 Python 服务，保持调度关闭，完成跨栈和质量验收后另行启用。
4. Nuxt 必须运行 `.output/` Node 服务；`/api/v2/ai/**` 和 v1 都代理至 crawler :8081，保留 `/api`。已有 Nitro request hook 继续先清洗来源头，保留 Authorization。后端安全过滤由 B 维护，前端代理不构成授权。

## 读取规则与兼容

- 真正 JSON string[] 保留元素内逗号、空白、引号、括号和 Unicode。空数组仍是空数组，不塞入“未解析”占位文本。
- 历史 List.toString 格式无法可靠恢复边界，整段作为一个 string 元素原样保留；不猜测拆分、不重写数据库。没有 sidecar 的非空值为 `legacy_unverified`，无值为 `unparsed`，即使历史值碰巧是合法 JSON 也不认证为 Jev。
- analytics 用原始 `d.is_deleted = 0` 在 JDBC 转布尔前筛选；保留后续防御筛选。
- 读取同 job_id 按 updated_at、id 降序的最新 sidecar（不会回退选择旧成功记录）。按 JSON 结构比较 source_json 与本次 detail_data，只有相同来源、completed、result_json.status 为 complete/no_evidence 且两个投影均为 JSON string[] 才标完成。no_evidence 还要求投影均为空。
- 不同来源为 stale；失败/待复核/运行/等待保留旧数组，显式显示状态。retry_wait 映射 pending，superseded 映射 stale；未来未知状态为 null，前端显示“分析状态未知”。model/version 表示最近分析尝试的来源，不表示旧显示值来自该模型。
- 旧 schema：通过 JDBC metadata 检查当前 catalog 的 job_ai_analysis 表。确实不存在时使用带 NULL sidecar 列的查询，仍按历史值分类。数据库用户必须可见目标表 metadata；权限不足导致表不可见时不会认证为 Jev，但应按部署错误排查。
- 表存在但字段不完整、SQL/连接/认证失败均传播为读取错误；绝不捕获 SQL 异常改成成功/空列表。metadata 检查与 Spark 查询是独立连接，迁移期间发生竞争可导致请求失败，完成迁移后重试。查询增加最新 sidecar 的关联成本，生产索引与规模尚需验收。

## 前端与整合

招聘详情使用现有 JobSheet、Badge 展示状态、model、pipeline version；旧后端缺少状态时显示 unknown。数组保持 string[]，旧数值/null、204 快照语义与 request/session 错误处理不变。

`app/api/ai/matches.ts` 提供 `/api/v2/ai/matches` wrapper 与 envelope/types，不新增匹配页面。外部请求沿用 resume/resumeId、city、position；userId 不作为客户端权限凭据。冻结协议未固定外部 scope 的结构，暂以 unknown 保留，父任务须与 B 的最终外部 DTO 核对字段及可空性后完成跨栈验收。Python 的内部 match-batch DTO 不能直接当作外部 Java DTO。

## 回退与验收边界

回退至少保留本 reader 兼容版本。关闭新调度、处理在途 lease 后才能回退 writer；保留新表和 JSON 数据，不能为回退删表、清库或恢复逗号拆分 reader。

离线回归覆盖 JSON/legacy、来源相等/变更、状态降级、SQL 异常传播、Spark bean/JSON 输出、前端状态和代理配置。隔离 Spark 测试使用 local[1]、合成行，不连接现有 Spark/MySQL。metadata/SQL 选择测试使用 JDBC mock，不能当成真实 MySQL 查询/迁移验收。未调用真实 Jev、未操作现库、未跑浏览器与生产服务，未验收领域质量或发布效果。
