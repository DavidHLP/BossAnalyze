# Jev 完整身份链隔离验收

日期：2026-11-08（America/Los_Angeles）；预算 UTC 日为 2026-11-09。

结论：**单次合法外部 v2 POST 已贯通实时主 API 身份校验、真实 Java MVC/业务/MyBatis、真实 Python HTTP 和 CommandCode Jev。返回200，但四维均 unknown，最终 score=null；没有完成真实 scored 分支或领域质量验收。真实跨账号简历被模型前拒绝，但错误映射为503，是已确认的语义缺陷。** 正式调度、生产写回和 AI_QUALITY_APPROVED 均未开启。

## 范围、来源与方法

独立 worktree `/home/david/.t3/worktrees/BossAnalyze/t3-213ca7a6`，起始干净，基线 `7255a96e4583d2cedeb10c75ca557d995bc252e0`。它相对业务版本 `41420e06666036e74386ead6b134c859c4f32d00` 只有上一轮运行报告不同；本轮不改业务实现、配置、现行服务或其他 worktree，仅提交本报告。

先读 AGENTS 和四份共享规则、两份 Jev specs、`/tmp/boss-jev-postfix-runtime-handoff.md` 及上一轮报告。Hindsight 搜索/Conventions 页没有给出此轮 Jev 身份链结论，不以历史记录替代实际运行。图索引后使用 search_graph/get_code_snippet，所依赖12个 Java/XML/Python 文件 exact-path coverage 均 metadata_match/no_recorded_issue；这是 best-effort，关键组件仍直接读原文。无全仓审计结论。

生产类复用上一轮已验证41420e0新构建 JAR 的 `/tmp/boss-jev-postfix-41420e0/java/BOOT-INF/classes` 和 `BOOT-INF/lib/*`。该产物来源见前报告 java-build.log/run-java.py；本轮 Git 比较确认业务源码一致。classpath **没有**旧证据目录根部的 harness classes，也没有157ee31.class。新增 `/tmp/boss-jev-identity-7255a96/IdentityMvc.java` 使用 Java17重新javac；未重新构建生产 JAR、未重跑全套单测或DB竞争。

最小嵌入式 Tomcat + Spring MVC，不启动 crawler 主程序，不扫描/加载调度。显式注册真实 `DownstreamAuthFilter`、`AuthorizedResume`、`AIRequirementsController`、`RequirementAnalyzer`、`FormatDataService`、`AIExtractionOfJobRequirements`；使用真实 MyBatis Configuration/XMLMapperBuilder、生产 mapper XML 和 mapper，非 mock mapper。观察 filter 仅包裹真实 filter 并记录 verified/status，不设置认证成功属性。

唯一外部请求入口为临时随机 loopback 端口的 `/api/v2/ai/matches`；这不是现行主API已经部署新路由的证明。该入口的真实 filter 向保持运行的主API8080实时请求 `/api/auth/getUserPrivateInformation`。地址来自安全manifest和当前 `ss -ltnp`，监听确认为现行Java PID1445378；未依据旧PID操作服务。

Python 使用当前 worktree 的 app 源码（PYTHONPATH）和上一轮锁定环境的 venv。仅Python读取指定0600 `identity-env.json`，固定230000 cap、原预算路径、retries=0、CMD_ZDR=0、quality=false。Java子进程显式剔除 CMD_API_KEY，只接内部token；未打印/持久化密码、token或原始敏感headers。

## 隔离数据与单次请求

MySQL凭据只程序化读取指定 application-local.properties 的 datasource username/password；地址从公开 local-dev YAML 的127.0.0.1:3306取得。连接先不选业务库，创建两个随机新schema，均保留审计：

- `jev_acceptance_f3b17b6cc38f`：完整链及初始拒绝用例。
- `jev_acceptance_13858e8f1381`：第二轮纯拒绝用例，无provider调用。

各自仅最小 t_job_detail、正式 job_ai_analysis 增量表及一条合成职位9001。真实 mapper 按“合成城/数据分析师”查询。无DROP快照、无现业务schema DDL/DML、无新业务简历记录。

JD明确写“必须熟练Python编程；必须具有数据分析项目经历；必须具有三年工作经验；必须具有本科学历”，basicInfo含相同城市/职位、3年以上/本科。请求内简历为“合成测试简历：熟练Python编程；具有数据分析项目经历；具有三年工作经验；具有本科学历。”所有出站内容均为此合成输入。

先程序化主API login取得会话，随后**一次真实外部v2 POST**携带该token，通过真实filter实时验证、AuthorizedResume允许本次合成文本、controller调用RequirementAnalyzer，真实mapper取职位9001，真实Java client携内部token请求Python `/internal/v1/match-batch`，Python生产pipeline经真实TLS/httpx请求官方CommandCode。没有将独立login和独立client结果拼成全链。

证据 `mvc-events.jsonl` 对这一次记录 verified=true/status200；`results.json` 同时记录一个内部请求、internalAuth=true、noCookie=true、noUserId=true、一次provider调用、返回consideredCount=1和jobId9001。内部认证等于独立内部token，说明用户JWT没有作为Python认证转发。

## 模型前拒绝与归属

| 用例 | 实际状态 | Python/provider调用 |
| --- | --- | --- |
| 匿名v2 POST | 401，verified=false | 0/0 |
| 无效Bearer token | 401，verified=false | 0/0 |
| 合法token自称userId | 422，verified=true | 0/0 |
| 非法resumeId `../illegal`，不带resume文本 | 422 | 0/0 |
| v1合法token自称userId | 422 | 0/0 |
| 不存在的合法格式resumeId | 503 | 0/0 |
| 第二QA请求第一QA列表内已有resumeId | 503，verified=true | 0/0 |

第一轮两个QA列表返回200，分别12个/0个ID；最初反向差集为空，明确保留 `crossOwnerAvailable=false`，该轮不存在ID不是归属证明。第二轮反向使用第二QA token请求第一QA已有ID，真实AuthorizedResume路径拒绝，`negative-results.json` 记录 crossOwnerAvailable=true、calls=[]、internal=[]。只读取列表ID用于拒绝，不保存ID或真实正文、不把真实简历送模型。

补充 `primary-owner-status.json` 使用相同两个授权QA重新login，第二QA直接请求第一QA已有列表ID，主API返回404；使用stream只检查status、不读取正文。所有取得会话均正常logout200。不能由列表数量声称这12份简历已经审计为独立人工构造fixture；这里证明的是两个指定合成QA账户之间真实已有ID的访问拒绝。

### 已确认错误映射

`backend/crawler/src/main/java/com/david/hlp/crawler/ai/service/AuthorizedResume.java:25` 的exchange收到主API404后，在第29行被 `catch(RestClientException)` 转为503/IDENTITY_UNAVAILABLE。最小反例：第二QA合法token + 第一QA已有resumeId（不带resume文本）POST v2；实际503，模型前拒绝。相同逻辑也影响不存在ID。

这是授权拒绝与身份服务故障混淆，未观察到绕过授权或正文泄漏；不把拒绝503冒称正确403协议已通过。最小修复建议：先单独捕获RestClientResponseException，将明确404/403映射为受控RESUME_NOT_OWNED/403，401保留认证语义；仅网络/服务故障映射503。本轮按授权不改业务实现。

## 一次真实模型结果与预算

真实provider尝试 **1次**，HTTP200，model=`typesafe/jev-1.14.0`，耗时 **1.979秒**（provider段，非全链延迟/p95）。usage input5588/output938。固定官方host、TLS、httpx trust_env=False、显式User-Agent；无重试、备用host/模型、ZDR422重试或为得到scored再采样。

完整外部v2结果：HTTP200，items=[]，unrankable内职位9001为insufficient_evidence，score=null；consideredCount=1、complete=false、scope.truncated=false。四维skills/relevant_experience/experience/education均unknown，rawScore/probabilities=null。provider原始Score分别3.82、3.86、3.87、3.85（以results.json原值为准），它们不是最终业务分数。

生产pipeline存在(0.1,0.9)的不确定证据即降unknown，还要求status/Score confidence及两侧证据；本次如skills岗位证据0.86、Score confidence0.87，相关经历多处0.74/0.79，学历岗位0.88，均未满足既有条件。没有改门槛、分数、权重或quality标志。真实Java校验通过unknown协议；**Java非null rawScore/probabilities与最终scored分支仍未自然覆盖**，不能以provider原始Score存在冒称通过。

原200000 python-env.json保留不改；本轮父明确授权的identity profile cap230000。只读账本before188658、after213772，保守预留增加25114，剩16228；不是实付usage。第二轮纯拒绝前后均213772。没有编辑/重置账本或购买额度。此前13次及修复后3次记录保留，本轮只增加1次（这三份运行验收合计17次，低于30次；不把该合计冒称独立审计全部历史设计探测次数）。

## 证据、清理与未验收

证据目录 `/tmp/boss-jev-identity-7255a96/`（0700；文件0600）：IdentityMvc.java及新class、run.py、run-negative.py、results.json、negative-results.json、追加式mvc-events.jsonl、primary-owner-probe.py/status JSON、schema-initial.txt/schema.txt、cleanup.json、java-closed.txt、preparation-notes.txt。旧失败报告、旧通过报告、原schema、原日志和全部账本记录均未修改。

准备时javac曾因Configuration类名冲突失败；仅修临时harness注解全限定名后通过，未产生provider请求。java-console.log被第二次同一harness启动覆盖，只代表第二次启动，不能当作第一轮完整日志；第一轮results.json与追加式MVC事件仍在，限制如实保留。

两个Python HTTP进程和两个Java进程都记录PID+/proc starttime。只在本轮Java身份匹配时SIGTERM/wait；Java退出143为主动清理，shutdown hook关闭Tomcat/SqlSession；Python关闭server/http client正常退出。cleanup.json核实四个确切身份均已退出。原8080仍保持原监听，未替换/重启服务；无现行账号/角色/业务数据变更。

未验收：真实scored数值分支、v1成功/unknown映射完整返回（仅测试v1身份拒绝）、现行主API网关新路由部署、独立人工gold/holdout及领域质量、排序、生产性能/p95、正式调度/writeback、部署/回退、浏览器、全量unit suite和CI。quality仍不批准；完整身份链通过不覆盖这些门槛。
