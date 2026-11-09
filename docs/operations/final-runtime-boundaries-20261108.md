# 三个 runtime 边界修复验收（2026-11-08）

结论：本次 R1/R2/R3 **Verified**，本机最终服务已保留；旧对象存储迁移仍阻塞，API SMTP aggregate health 仍为 503，不宣称整个基础设施或生产验收完成。

## 源码与边界

- 独立 worktree `/home/david/.t3/worktrees/BossAnalyze/t3-45fbd205`，分支 `t3/fix-offline-runtime-defects`，基线 `4f6e9b34f8a2710d132d14755b01a8668c262ec8`。
- 主修复 `c2a8b933feb2d597a82775f4d256e2ac40d61fee`；补充前端 200/null 非法响应防御 `50e38be5b3e8faae128384b8ac93c72b7d230b3f`。均含要求的 Signed-off-by。
- 仅修改三个招聘 service、两个 API gateway/controller、相关既有测试、dashboard API adapter/normalizer 和本报告。父源码/worktree、业务数据、真实 schema、AI/crawler、旧 MinIO 入口均未修改。未委派、push、PR、watch 或运行浏览器。

## 实现与回归

R1：四个 JDBC 入口共享固定 dbtable `(SELECT * FROM t_job_detail WHERE is_deleted = 0) active_jobs`。在 MySQL 原始数值仍可见时过滤，不依赖驱动布尔映射或 Spark predicate pushdown。保留 `activeRows` 作为冗余防御，既有测试明确改名为“已解码行的防御过滤”，不把内存整数测试当作 JDBC 证明。未改 schema、驱动配置或异常语义。

R2：RecruitmentGatewayController 新建响应，保留 status/body，仅从上游白名单复制 Content-Type。Connection、Transfer-Encoding、Keep-Alive 和 Connection 提名 header 均不复制。既有编码查询和 host+port bearer 信任限制未改；MockRestServiceServer 回归验证上游 framing/提名 header 不泄漏。

R3：LogController 先保留上游明确 204；200 null/空对象仍 503，异常/403 仍失败。前端 adapter 仅将共享请求层的 code204 转为 null，200/null 明确抛错；normalizer 接受 null，其他无效/不完整对象抛错。两个现有页面已能用 null 显示暂无快照及破折号，无需修改页面/共享请求层。DB-IP 署名保留。未触发分析、启用调度、写 snapshot、重放日志或 flush。

## 新鲜真实验收

私有证据目录：`/home/david/.local/share/bossanalyze/local-dev/final-runtime-e6df9cf38b/boundaries-6e8609e38e`。目录 0700，配置、session、日志私有，禁止输出凭据/token。已有合成账号仅经正常登录消费 private JSON；每轮新 token 均正常 logout200。

| 验收项 | 实际结果 | 证据文件 |
| --- | --- | --- |
| 全新 nullable tinyint(1) schema，0/1/NULL/2/-7 各10行 | 三层四 API200；城市/职位仅 fixture-active；排名一组 jobCount10、avgSalary4.5；散点10行且 salaryValue4.5 | sql-before.json、isolated-results.json、isolated-final-results.json |
| 原始 flags 无损保留 | 所有50行保留到完整验收结束；每个 flag 仍10行，未删除2/-7再宣称通过 | sql-after-acceptance.json、cleanup.json |
| urllib/curl 普通 JSON 解码 | 18082→18080→15020 四路径均 JSON 一致；无重复 Transfer-Encoding | isolated-final-results.json |
| 真实原库只读空集 | boss_data.t_job_detail 前后 count0；8082/8080/5020 四路径全部200合法 `[]`；urllib/curl一致、无重复 TE | final-results.json、final-r2-results.json、cleanup.json |
| 无 snapshot | direct/API/Nuxt 全部204、body0字节；原 Redis EXISTS dashboard:stats:overview =0，只读 | isolated-final-results.json、final-r2-results.json、final-summary.json |
| 鉴权 | 三层匿名/无效 bearer 均401；有效合成登录后读取成功；每轮 logout200 | 同上检查脚本与结果 |

四路径：`/api/v1/boss/basic/city-name-list`、`/api/v1/boss/basic/position-name-list`、`/api/v1/boss/user/salary-hot-job`、`/api/v1/boss/user/two-dimensional-analysis-chart`。

dashboard：direct `/api/log/access/dashboard/stats`；API/Nuxt `/api/v1/system/logs/dashboard/stats`。此处是 HTTP runtime 证据，前端 null/非法对象/403/503 的处理来自执行实际 adapter/normalizer 的既有 Node 测试；未冒称浏览器验收。

此前 `/tmp/boss-infra-recruitment-final-handoff.md` 的原 flags2/-7 失败和旧重复 chunked 失败未覆写；此前500及子集pass也保持原样。本次新 fixture 与旧 fixture schema 不同。新证据不将旧失败改写为成功。

## 构建与测试

日志 `/tmp/boss-boundaries-build/`（目录0700），依赖安装日志 `/tmp/boss-boundaries-install.log`。

- Java17/Maven3.9.16：`mvn -f backend/pom.xml -pl analytics,api -am clean verify` 通过（backend-r2.log）。API43项，0失败/错误/跳过；analytics12项，0失败/错误、1跳过（未提供 geo.validation.mmdb 的可选官方库测试）。不能将该跳过说成通过。
- 修改既有 analytics 测试命名后再次 `-pl analytics -am verify` 通过（analytics-final.log）。真实 tinyint JDBC 验收另外运行，不依赖上述内存测试。
- 冻结依赖安装通过；frontend typecheck、66项测试、生产build全部通过；最终补充200/null后再次全部通过（typecheck-final.log、frontend-tests-final.log、frontend-build-final.log）。
- 首轮后端新增 Mockito 用例重设 throwing stub 失败，修正为 doThrow 后全量通过；backend.log 保留原失败。初次工具链路径定位失败未计为验证通过。
- 未触及 credential/offline helper，因此未重复其隔离套件；未执行 crawler reactor、远程CI。父整合门仍需执行其完整CI。

## 保留服务与回退

受控替换前核对指定 manifest 与 /proc 完整 cmd/cwd/starttime；只对匹配三个进程 SIGTERM，无 widekill/SIGKILL。原 manifest、process metadata、环境和配置保存在本轮 `before-update/`，旧 jar、Nuxt output、配置及日志均留原路径。新产物先拷贝到稳定私有 `final-artifacts/`，不从会被 clean 的 target 启动。

最终 manifest：`/home/david/.local/share/bossanalyze/local-dev/final-runtime-e6df9cf38b/manifest.json`。

| 服务 | 端口 | PID | /proc starttime |
| --- | --- | --- | --- |
| analytics |8082|739675|1606232|
| API |8080|740055|1606706|
| Nuxt |5020|742303|1612426|

后端使用 c2a8b93 构建的 final-artifacts/api.jar、analytics.jar；Nuxt 使用50e38be构建的 final-artifacts/nuxt-50e38be5b3e8。最终身份新鲜核对见 final-summary.json；manifest 记录各层 revision，未将旧产物重标为新构建。

自有18080/18082/15020已按精确身份停止；独立 Redis 容器按 ID/name/image/端口校验后停止移除。全新 schema `boss_boundaries_7ff2c9e94200` 的实际名称以 state.json/cleanup.json 为准，50行仅在全部验收后删除，空 schema保留。synthetic bucket namespace仅用于隔离 API 配置；本轮无 storage 操作，未创建bucket/对象。原 bucket不碰。

## 剩余边界

- API readiness200，analytics aggregate health200，Nuxt/login200；API aggregate health503，private log明确 MailHealthIndicator失败，未关闭检测/修SMTP/发邮件。
- 旧 MinIO18988新鲜探测 curl exit7不可达，源/备份仍未知；未切真实应用入口、迁移或接触原bucket。
- 无 browser/真实设备/生产验收；本次没有生成真实 dashboard snapshot，缺失本身是正常204。
- Hindsight/context resume先行，graph search→trace→snippet用于结构探索；所有依赖路径检查coverage。变更后metadata_changed直接读diff/源码，frontend测试/type声明为excluded亦直读；不宣称graph穷尽。知识页旧“全部尚未实施”状态已用当前源码/运行证据纠正，未作为本次通过依据。
