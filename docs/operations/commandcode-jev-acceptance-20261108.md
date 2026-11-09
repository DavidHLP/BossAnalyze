# issue44：Jev 固定基线运行验收

验收日期：2026-11-08（America/Los_Angeles，UTC 账本日期 2026-11-09）。被测业务代码固定为 `157ee31dfdcd4172751efd923d9f95bdac08767a`。本提交仅增加此报告，不包含后续修复。结论：**部分运行边界已验证，整体验收失败，不能开启正式调度、写回或质量批准。**

## 范围、方法与证据

独立 worktree `/home/david/.t3/worktrees/BossAnalyze/t3-84c2046c`，分支 `t3/issue44-runtime-acceptance`，开始时干净。已读 AGENTS、四份共享规则、两份冻结规范、Python/Java/readers 三份运维文档与 `/tmp/boss-jev-unified-integration-handoff.md`；追加阅读 `/tmp/boss-jev-review-spec.md` 后独立运行三个反例。不另委派，不启动浏览器，不 push/PR/watch。

临时证据根目录 `/tmp/boss-jev-runtime-157ee31/`（0700；文本证据0600），交接 `/tmp/boss-jev-runtime-handoff.md`。临时 Java/Python harness 不提交。图索引用于发现 Store/client/mapper，引用的 Python 四文件、Java Store/client/reader 和 mapper 做 exact-path coverage，均 metadata_match/no_recorded_issue；migration SQL excluded，已直接完整读取。图覆盖仅为 best-effort。历史知识搜索没有提供本轮结果，结论依据固定代码和本轮运行。

当前依赖：`uvx --from uv==0.9.9 uv sync --frozen --project ai-service`；Python 3.13.15、httpx0.28.1，使用该 worktree 的锁和新进程。Java 17 Zulu、Maven3.9.16，`mvn -o -f backend/pom.xml package -DskipTests` 成功；从本轮 crawler JAR 解包实际类/依赖，临时 javac harness 调用实际类。**没有重跑已通过的整套 pytest/JUnit/frontend 测试；跳过不是通过。** 构建记录 `python-install.log`、`java-build.log`。

只有 Python 新进程程序化读取指定 `python-env.json`，没有读取 CC Switch DB。Java 仅程序化读取授权 local properties 的 datasource username/password；该文件没有 URL，连接地址来自已读的公开 `application-local-dev.yml`（本机127.0.0.1:3306），从不选择业务 schema。没有读取其他凭据或 Docker env。仅发送合成文本。原服务/配置没有修改、重启或停止。

## A：真实 httpx → CommandCode

真实客户端维持 `trust_env=False`、TLS验证、官方 host、固定 `typesafe/jev-1.14.0`。未用 curl、备用传输、代理继承或其他 provider 替代成功。真实 Python readiness200，qualityGate calibrated/automaticWritebackAllowed 均 false；无认证401。无配置服务 readiness503。探针不调用模型。

真实出站尝试共 **13 次**，未达30次上限。其中11次HTTP200、1次ZDR HTTP422、1次ZDR尝试未收到可记录HTTP响应（受控 provider_unavailable，约5.005秒，不能推定HTTP状态/根因）。全部已知HTTP200响应的 model 均为固定发布版本。已报告 usage 合计 input16918/output2134 tokens；未返回 usage 的尝试保持未知。共享持久账本未重置，UTC日保守预留 **151527/200000**，不是实付token数或美元账单。真实请求停止后未再调用。

| 实际运行 | 结果 | usage / 延迟 |
| --- | --- | --- |
| 首次原生 Noul/Choice/Score 同请求 | HTTP200，但客户端502/invalid_provider_output | 519/61；1.705s HTTP |
| 原生三类诊断请求 | HTTP200，三类均通过原客户端校验 | 498/61；1.609s 总耗时 |
| ZDR诊断 | HTTP422 cmd_zdr_no_providers，客户端503/zdr_unavailable；保留 x-cmd-zdr=1，无去header重试 | usage未知；1.661s |
| HTTP job-analysis（含emoji、转义JSON Pointer） | 502/invalid_provider_output；分类HTTP200后核心Score HTTP200仍被拒绝 | 1556/182 + 824/18；2.064s 总耗时 |
| HTTP match-batch | 502/invalid_provider_output | 3839/628；0.850s 总耗时 |
| 中文规则构造标注例 | 返回needs_review，要求/福利/核心可回溯 | 2324/200；2.614s |
| 中英规则构造标注例 | 核心Score响应被拒绝，整份分析失败 | 2232/200；1.711s |
| 两职位排序例（Python岗位与Rust岗位） | 两个HTTP200 Score响应均被校验拒绝，整批失败 | 5126/784；没有可比较最终排名 |

证据：`python-results.json`、`native-diagnostic.json`、`quality-results.json`、`budget-summary.json`。仅保存合成回答和白名单状态/model/usage/延迟/受控错误；没有敏感header、请求token或上游原始异常日志。首次失败的原生混合响应没有保存完整answer，不据此猜测具体失败字段。

### 确认接入失败 R4（P2）：Score 数值精度与当前严格校验冲突

位置 `ai-service/app/jev.py:60`。真实中英核心回答：score3.70，probabilities `{0:0.01,1:0,2:0.06,3:0.12,4:0.81}`，和1、期望3.72，当前返回502。排序回答有 score3.94、分布期望3.96；另有 score0.05、期望0.04，浮点差实际 `0.010000000000000002 > 0.01`，即使十进制差恰为允许边界也拒绝。`selfcheck.py` 从真实回答构造最小输入，直接调用未修改的 validate_answers，重现后者拒绝（`float-repro.json`）。

这是已观测的供应商兼容性失败；尚未证明供应商舍入契约究竟承诺何种精度，不能把所有差异归为单一供应商错误或擅自移除验证。最小修复方向是先明确分数/分布精度契约，处理十进制边界，并保留对真正不一致输出的拒绝及相应反例。验收期间未改实现、阈值或答案。

### 小型公开合成集的限制与指标

`dataset.json` 含中文/中英各一份JD、每份4句，包括要求、福利、否定福利、职责；标签**由规则构造，不是独立人工gold**。两份候选均4/4召回，总8/8；不能外推98%正式门槛。原生端到端分析仅1/2份返回，不能只报成功子集。

中文返回的阈值选中要求1条、福利1条，precision2/2；两个阳性标签召回2/2，核心只选“必须熟练Python”，importance3.89。原文切片在要求/福利/核心共3处检查3/3一致。中英整份失败，其分类precision、核心质量不可验收。`AI_QUALITY_APPROVED=0`，因此真正自动接受/写回为0，自动写回precision不适用；上述是阈值选中项的探索指标。

真实匹配整批失败，没有可评分覆盖率、unknown维度覆盖或人工排序一致性结论；不能把错误当unknown覆盖。fixture unknown保持null见C。小样本没有独立留出集、人工排序或可靠p95/成本估计，不宣称正式质量门槛通过。普通成功请求**不是零保留**；ZDR当前失败，不能向真实个人数据降级。

## B：真实 MySQL、Java Store、MyBatis 和 reader

本轮只新建随机 `jev_acceptance_*` schema，不向现有业务schema执行DDL/DML，不导入DROP快照。主要最终审计schema：**`jev_acceptance_d166c92f4275`**，位置为本机127.0.0.1:3306。此前保留的本轮schema：`jev_acceptance_62173aabb59c`、`jev_acceptance_18b6ae9cc171`（早期harness错误的空表演练）、`jev_acceptance_995a5bf5a5f4`（第一次完整运行）。全部为本轮创建，仅合成数据。没有删除它们。

Store本身使用JdbcTemplate/TransactionTemplate，不能称其为MyBatis实现。测试分别实际调用 Store 和 XMLMapperBuilder/SqlSessionFactory 加载仓库真实 TJobDetailMapper.xml；MyBatis查询连接同一隔离schema。没有用手写SQL替代Store领取/完成，也没有启动原全栈。

| 边界 | 状态与实际证据 |
| --- | --- |
| migration重复执行 | 已验证：同一新schema连续两次；18列、五列唯一identity索引。原先harness误期望19列失败，纠正临时断言后重跑，未改migration |
| 两个独立连接争领 | 已验证：两个DataSource/事务管理器，同步起跑，恰一个非null lease |
| lease/fence | 已验证：隔离SQL使lease过期，旧lease提交false；重新领取fence增加，旧fence提交false |
| 源变更、删除 | 已验证：真实第二次SQL修改源或is_deleted，finish拒绝，旧两数组均保留 |
| 完整投影 | 已验证：合成complete结果一次事务更新sidecar和两JSON数组，保留emoji、英文空格/逗号/括号 |
| 失败/review/关闭写回 | 已验证：fail、needs_review、writeback=false下complete均不投影 |
| no_evidence | 已验证：合成no_evidence投影为两个真正空JSON数组 |
| SQL失败原子回滚 | 已验证：仅隔离schema的BEFORE UPDATE触发器抛错；finish异常后sidecar仍running、两旧数组均保留；测试finally删除自身trigger |
| reader真实查询 | 已验证：JobAnalysisReader.table(Connection)生成真实SELECT，来源结构相等为complete；序列化格式差异不改结果；最新failed覆盖旧success，同时间按较大id选择 |
| 无sidecar metadata分支 | 已验证：仅隔离schema临时rename侧表，实际metadata探测、SELECT NULL别名、legacy_unverified；finally恢复表名 |
| MyBatis | 已验证：真实mapper查询合成行、数组字符串，过滤已删除行 |

证据 `RuntimeAcceptance.java`、`Addendum.java`、`java-results.log`（18项）、`addendum.log`（4项）。合成complete fixture仅验证数据库代码，质量门槛仍关闭；它不是Jev正确输出的证据。没有进行生产表规模/索引代价评估。

### 基线问题 R1–R3（均P2，真实运行确认）

`Counterexamples.java` 使用同一真实 Store/MySQL、合成结果或受控输入异常，不访问provider。与Spec审查所述一致，但以下是本轮独立实跑结果：

1. **R1 A→B→A和饿死**，Store:33–35、52、64–67：ID20–29依次完成A、完成B、恢复A；claim(20,A)=null。新ID30正常可分析，但process(19,30)返回0，ID30没有任何sidecar。前十条superseded身份占住扫描窗口，基线失败。
2. **R2永久输入错误重试**，Store:58–59、69：ID40/41的实际process分别接收合成400/422异常，数据库state均为retry_wait；没有改成terminal failure，基线失败。
3. **R3非事实字段identity**，Store:32：ID50只改updateTime/analysisReport，同样jobDescription产生两条分析identity；Python明确忽略这些字段，基线失败。该反例证明重复身份准入，没有声称这里发生真实付费调用。

最小修复方向分别为带fence/来源复核的身份恢复并消除扫描空转、永久输入错误终止重试、统一事实摘要与快照/reader freshness规则。父任务后续修复不属于本报告证据。

## C：真实 Java → Python HTTP，合成provider fixture

使用真实 Java AIExtractionOfJobRequirements、随机loopback端口的实际 uvicorn/Python应用。仅provider响应由 httpx.MockTransport合成；临时HTTP relay转发并记录header是否符合，不记录header值。Java只持有synthetic-internal，未读取真实provider/service key。

8项通过，见 `HttpAcceptance.java`、`http-results.log`：Unicode/转义路径分析；49项requirements和benefits完整返回、core20；四维匹配8.75小数；unknown score=null/complete=false；错jobId502；缺必需benefits数组502；内部401映射Java503；200ms剩余预算遇800ms relay延迟返回504且总耗时<800ms。relay记录内部认证正确、无Cookie/用户身份header；错误令牌例按预期为false。客户端接口没有用户JWT参数，本轮未构造真实用户会话。

额外真实Python HTTP边界：数字jobId422、重复jobId422、49项中/英候选完整数组200；异步provider延迟400ms遇10ms总deadline返回504（观察约53ms，包含HTTP连接/调度开销），见 `boundary-results.json`。初版临时fixture用了同步sleep阻塞事件循环，观察约443ms；改为异步fixture后复测，前者不是生产HTTP的deadline证明。没有修改业务应用。

**没有执行真实主API身份登录/归属全链**，没有使用QA凭据、发真实用户JWT、改权限或触发crawler。relay/fixture验证不能替代主API认证验收、外部v1/v2实际排名或真实Java→Python→Jev完整成功链。

## 复现、清理与交付边界

临时复现（不发真实provider请求；每次RuntimeAcceptance新建一个随机隔离schema；后两类使用schema.txt最后一行）：

```sh
cd /home/david/.t3/worktrees/BossAnalyze/t3-84c2046c
JEV_TEST_CP='/tmp/boss-jev-runtime-157ee31/java:/tmp/boss-jev-runtime-157ee31/java/BOOT-INF/classes:/tmp/boss-jev-runtime-157ee31/java/BOOT-INF/lib/*'
/home/david/.local/share/mise/installs/java/zulu-17/bin/java -cp "$JEV_TEST_CP" RuntimeAcceptance
/home/david/.local/share/mise/installs/java/zulu-17/bin/java -cp "$JEV_TEST_CP" Counterexamples
/home/david/.local/share/mise/installs/java/zulu-17/bin/java -cp "$JEV_TEST_CP" Addendum
PYTHONPATH="$PWD/ai-service" ai-service/.venv/bin/python /tmp/boss-jev-runtime-157ee31/selfcheck.py
```

上述已编译类固定来自157ee31。新SHA复验必须重新构建、解包并编译harness，不能直接把这些类当新实现。Counterexamples有针对基线失败的断言，新修复改变结果后需要按新预期更新临时断言，不能要求新代码维持旧缺陷。不要重跑runtime.py/probe.py/quality.py冒充无网络测试。

长驻Python服务、relay均记录PID与/proc starttime，只对本轮拥有且starttime匹配的进程关闭；最终全部退出，见process.json、fixture-process.json、relay.json、cleanup.json。短命Java最终运行PID/starttime/退出码见java-processes.json。MySQL连接随JVM退出关闭，synthetic schema保留审计；预算账本保留原位置且未重置。只读监听盘点不依据旧PID终止任何服务。

harness排障记录保留：relay最初不支持Java chunked请求，修正临时relay后通过；合成fixture自己的预算在重复测试中耗尽，最终提高**纯MockTransport fixture**额度并保留其账本，复跑HTTP8项通过，java-processes.json保留此前exit1。真实provider共享额度始终200000、账本未改动/重置，不能混淆这两个账本。

未做浏览器、生产部署、全套单测重跑、真实身份全链、人工gold/留出质量门槛或生产性能验收。主要风险是R1–R4及ZDR不可用；正式调度/writeback和AI_QUALITY_APPROVED继续关闭。本报告不宣称issue44已全部通过。
