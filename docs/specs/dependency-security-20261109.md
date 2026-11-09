# 非 BOSS 交付：已确认依赖安全告警修复（2026-11-09）

用户目标：除真实 BOSS 数据获取外，其余任务做到可交付，自主解决。发布核查从 GitHub 读取 51 个 open Dependabot alerts；冻结快照 [dependency-alerts-20261109.json](../security/dependency-alerts-20261109.json)。当前已发布 API 的 JAR 确实包含 hutool-all 5.8.16、redisson 3.17.6、Jackson core/databind 2.18.2，不可将告警当过期声明忽略。

基线为合并 PR #57 后 main `059303f0186cade96feb697bc9f1ad410fb20bae`。此次只解决该冻结快照涉及的依赖、必要的调用/测试/配置兼容，不改产品范围、真实采集或 AI 模型/隐私门槛。不得删除或关闭告警来伪造成功。新增发现需单列，不能默认宣称整个依赖生态无漏洞。

关联 [issue #58](https://github.com/DavidHLP/BossAnalyze/issues/58)，属于 #54 的发布安全核查修复。

## 所有权与并行前沿

- A / Maven：backend/pom.xml、backend/{api,commons,analytics,crawler}/pom.xml、tools/credential-contract-tests/pom.xml、deploy/rustfs/pom.xml；必要 Redis codec 配置及其现有聚焦测试，Java 因升级确实不兼容的最小修复。负责 Hutool、Redisson、Jackson core/databind、JSoup、MinIO SDK、Spark 及其告警在所有声明处的一致性。报告 `/tmp/boss-dependency-java-handoff.md`。
- B / Python：ai-service/pyproject.toml、uv.lock，必要且最小 Python/test 兼容修复；Starlette/FastAPI、pytest。报告 `/tmp/boss-dependency-python-handoff.md`。
- C / frontend：frontend/package.json、pnpm-lock.yaml、pnpm-workspace.yaml（pnpm11正式patch/override配置）、必要本地 package patch/已有测试；minimatch、glob、decode-uri-component、Zod、deepmerge。报告 `/tmp/boss-dependency-frontend-handoff.md`。
- Root：任务追踪、精确合并、统一验证、独立审查、最终 PR 和运行部署/回退。三个实现前沿互不依赖，集成依赖 A/B/C。

## 验收与禁止

1. 每个原始 alert（number/GHSA/manifest）对照实际解析/锁定/打包版本，提供已修复或仍未解决的明确证据；不得只看父级 version 或 Dependabot 数字。官方发布/Advisory/Registry 是版本和修复依据，联网核验。优先同一兼容系列最小安全版本，不强迫全栈追最新。必要 Boot 3.x 小版本允许；保留 Java17、Spark4/Scala2.13、Node>=22.12/pnpm11.24、Python3.12–3.14；不跨 Spring/Spark 主版本或减少现有能力。
2. Redisson 官方 GHSA-4hvc-qwr2-f8rv 明示仅升到3.22仍须检查 codec；原配置未 setCodec。检查所有 Redisson 调用方和既有 Redis 数据兼容，避免默认不受限对象反序列化。不得通过清 Redis、换 DB 或删除数据解决兼容。只锁场景可使用安全标量 codec，但要证明调用范围并验证。
3. 不能简单使用跨主版本 override 破坏消费者或 --force/忽略 peer/engine/锁校验。没有已发布修复的 deepmerge 必须沿实际调用路径判断，优先上游修复；确需 pnpm 正式 patch 机制时保留最小补丁、来源/许可和能检出原漏洞的聚焦反例，不伪造新的发布版本或撤销安全告警。
4. Java17 backend reactor verify + 独立 credential contracts，RustFS probe工程显式编译；Python冻结uv sync/lockcheck、完整既有套件/compile；前端冻结安装、typecheck/test/build。环境缺件或失败要保留证据并解决，不降门槛。新测试仅为有具体未覆盖风险的分支，复用现有测试；无新框架。
5. 实现子任务禁止启停/修改现有服务、业务数据库/Redis/对象/私有配置/manifest，禁止真实 provider/BOSS/邮件；不得读取密钥。可联网获取官方依赖、用合成/内存测试；真实隔离运行由 root 负责。不得删除已有反例、降低断言、自动调整安全开关、全局镜像或 TLS。
6. 不独自占用仓库，严格各自 ownership，不回退其他人更改。每人隔离worktree、明确Signed-off-by提交并保持干净；不push/PR/watch/另委派。保留失败日志，完整交接路径/版本/测试数及未验收边界。未提交配置/下载产物/秘密。
7. 集成后双轴审查；精确 head CI、真实本机基础组件/认证/存储/Spark/同源与无模型 Python 协议验收，保留可回退产物与数据。现行服务保持到新版本验收具备条件；不以单测或告警列表缩短代替部署证明。

## 冻结告警族及官方补丁门槛（仍需核验可用版本和兼容）

- Hutool <5.8.21；Redisson <3.22.0（4 critical entries合计，两个GHSA）。
- Jackson core <=2.18.10、databind <2.18.11；tools 独立声明同修。
- JSoup <1.23.2；MinIO SDK <8.6.0（backend及deploy）；Spark4.0.0 → >=4.0.1同兼容系列。
- Starlette <1.3.1（需要相容 FastAPI约束）；pytest<9.0.3。
- minimatch >=7,<11.0.1；glob>=9,<=14.0.1；decode-uri-component<=0.5.0；Zod<4.6.6。
- deepmerge4.2–4.3.1：本次官方 alert 暂无 patched version；不得谎称简单版本更新已关闭。

告警是具体潜在风险而非已发生攻击；不得声称现网已可利用或已经被入侵。外部 ZDR/人工 gold/真实模型质量/外部SMTP仍独立未验收，不通过依赖升级假装完成。

## 运行基线补充：字符串缓存契约

父任务在当前本机 API 上补验验证码邮件注册，邮件已送达，但立即提交错误验证码返回 `HTTP400/1004`（过期），不是 `1003`。离线最小反例确认 RedisCacheUtil 把 Hutool 原样保存的字符串按 JSON 对象解析而返回 null。父任务独占 `RedisCacheUtil.java` 与 `RedisCacheUtilTest.java`，修复 String.class 的无损读取，保留对象/缺失条目行为和已有缓存格式。该变更是本次 Redis/Hutool 升级必须保留的实际业务兼容门槛；A 不修改这两文件。原运行反例及离线失败日志保留，最终需实际验证码→注册→登录/注销与精确夹具清理验收。

## 剩余 Zod 兼容修复路径

C 已保留直接升级 Zod4 导致 shadcn MCP schema 约束消失的失败反例。父任务授权比较两种完整修复：优先可追溯上游安全修复、能完整关闭原漏洞的 Zod3 最小 backport（pnpm 正式补丁与检测原版的回归）；若不能证明完整修复，再做必要的 shadcn/Zod4 调用迁移。两者都必须保留 MCP/registry/CLI 能力和 schema 约束，不得用放宽校验、移除功能或盲目 override 换取告警数字。局部补丁仍不是官方已发布安全版本，不能宣称 GitHub 告警已关闭。

统一 patch 文件必须保留上游上下文和许可。`frontend/.gitattributes` 仅对 `patches/*.patch` 关闭 Git 的文本空白诊断，因为上下文行的 diff 前缀会把原有 tab/空行误认成新增空白；所有其他源码继续 `git diff --check`，实际补丁应用、冻结安装、回归和构建门槛不变。不改变全局 Git 配置。
