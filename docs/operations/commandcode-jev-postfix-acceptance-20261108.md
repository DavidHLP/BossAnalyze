# Jev 修复后固定 HEAD 运行验收

被测业务 HEAD：`41420e06666036e74386ead6b134c859c4f32d00`。日期 2026-11-08（America/Los_Angeles），共享账本 UTC 日期 2026-11-09。本报告独立追加，旧失败报告 `commandcode-jev-acceptance-20261108.md` 原样保留。

结论：**S1/S2/S3 所列数据库反例已在新构建、真实 MySQL 下通过；真实 Python HTTP 分析与 Java→Python→CommandCode 匹配链路成功。质量启用仍不批准，完整用户身份到模型的链路尚未验收。** 不开启正式调度或写回。隔离 Store 的 complete fixture 写回只证明数据库行为。

## 范围和产物来源

工作目录 `/home/david/.t3/worktrees/BossAnalyze/t3-a0a61a72`，开始时干净，固定 HEAD 与交接一致。只新增本报告；不改业务源码、其他 worktree、旧证据或现行服务，不 push/PR/watch/issue、不浏览器、不另委派。

先读 AGENTS、四份共享规则、两份 Jev 规范、旧运行报告及两份 /tmp handoff。Hindsight 搜索及 conventions 页没有提供本轮 Jev 运行结果；旧 AI 暂缓决定不覆盖当前授权。本报告结论来自指定版本和本轮执行。

证据目录 `/tmp/boss-jev-postfix-41420e0/`，交接 `/tmp/boss-jev-postfix-runtime-handoff.md`。旧四个 Java harness 复制到这里后修改断言；没有复用旧 .class。新运行：
- Java 17 Zulu / Maven 3.9.16，离线 `mvn -o -f backend/pom.xml package -DskipTests` 成功，日志 `java-build.log`。未重跑全量 unit suite。
- 从本 worktree 新 crawler JAR 解包到证据目录 `java/BOOT-INF/{classes,lib}`，用 Java 17 javac 编译 RuntimeAcceptance、Counterexamples、Addendum、HttpAcceptance、OnlineMatch、Metrics。commons Facts/Reader 来自新 JAR 内依赖。
- `uvx --from uv==0.9.9 uv sync --frozen --project ai-service` 成功，见 `python-install.log`。新 worktree venv、新 Python HTTP 进程，使用当前 app 源码及锁文件。

结构发现先 index_repository/search_graph/get_code_snippet/trace_path，再读具体原文。图 trace 曾把部分通用方法连到无关符号，因此不据其作完备调用证明。Store、client、Facts、Reader、mapper Java/XML、Python config/jev/pipeline/main/candidates 以及身份探索路径做 exact-path coverage，均 metadata_match/no_recorded_issue；SQL migration 和 docs 被排除，已直接读原文。图仅 best-effort。曾查询错误 LoginDTO 路径并收到 missing，不以该路径支持结论。

## 真实 MySQL 与隔离 complete fixture

唯一新 schema：`jev_acceptance_08735cbea2ef`，本机 127.0.0.1:3306，仅合成数据，保留审计。凭据仅程序化读取用户指定 application-local.properties 的 datasource username/password；无 URL 时按公开 local-dev YAML 的本机地址连接。先连接无默认数据库 URL 创建随机新 schema，之后只访问新 schema；未向业务 schema 执行 DDL/DML，未读取 Docker env 或其他凭据。

Store 是真实 JdbcTemplate/TransactionTemplate；MyBatis 单独加载当前 TJobDetailMapper.xml 和真实 mapper，不能把 Store 称为 MyBatis Store。迁移在同一新 schema 执行两次，18列和五列唯一身份索引均通过。

| 场景 | 本轮实际结果 |
| --- | --- |
| 双连接争领 | 独立 DataSource、事务管理器同步起跑，恰一个 lease |
| lease / fence | 过期提交拒绝；重领 fence 增长；旧 fence 提交拒绝 |
| 源事实修改 / 删除 | finish 拒绝，原两列保留 |
| JSON 数组原子投影 | complete fixture 同时提交 sidecar、两列数组，保留 emoji、逗号、空格、括号 |
| SQL 触发器失败 | 两列保留，sidecar 仍 running，证明整个完成事务回滚 |
| needs_review / fail / writeback=false | 不投影 |
| no_evidence | 两列投影为真正空 JSON 数组 |
| Reader | 正常完成、JSON 格式差异、最新失败、相同时间按 id 决胜均通过 |
| 无 sidecar | metadata 缺表分支真实 SELECT 成功，历史非空列为 legacy_unverified |
| MyBatis | 合成行查询过滤删除记录并保留数组输入 |
| source_json | 等于领取时 MySQL 返回的完整 detail_data 字符串，含排除的 company 元数据；Java 未重写或只存事实摘要 |

MySQL JSON 类型会规范化输入空白/键格式，因此“原文保持”证明的是数据库当前完整快照与 source_json 一致、文本值不改写，不承诺 INSERT 前 JSON 字节排版逐字保留。

### S1 / S2 / S3 与坏缓存

`Counterexamples.java` 通过真实 Store/SQL 执行，每条新身份只使用本轮新 schema：
- A→B→A：第一次 A 和 B 各调用 analyzer 一次；第三次 A 重新验证完整缓存、签发新 fence 后投影，调用计数仍为2，两个身份 attempt_count 合计2；最初 A fence 提交被拒绝。
- 第100–109行交错五条 completed 与五条三次耗尽过期 running，第110行新任务仍完成，且只多调用一次 analyzer。明确测试了前十条不可领取身份不占成功领取额度。
- 400、422 分别为 failed，error_code 为 invalid_input_400/invalid_input_422，next_retry_at 为 NULL，再次 claim 返回 null。
- metadata updateTime/company 改变后仍一个身份，无新增 analyzer 调用，Reader 仍 complete；事实变化则产生第二个身份并分析。
- 三次耗尽且缓存损坏的 superseded 不再领取；过期第三次 running 无缓存转 failed；有效完整缓存即使三次耗尽也可获得新 fence、重投影。
- source_json 使用完整当前快照，不把 metadata 排除误解为删除快照元数据。

缓存 fixture 包含真实 client 所需 jobId/model/pipeline/policy/requestId/usage、三数组和精确事实证据；缓存成功不能由缺字段 fixture 绕过 validateAnalysis 推断。初始通用 Store 原子事务 harness 的 complete fixture 较小，直接测试 finish；它本身不证明 provider 校验。

证据：`java-processes.json` 三个 harness exit0、`RuntimeAcceptance.java`、`Counterexamples.java`、`Addendum.java`、各 console 日志、`java-results.log`、`addendum.log`。注意 java-results.log 被后运行的 Counterexamples 覆写，只列其16条断言；初始 RuntimeAcceptance 的成功证据为顺序无异常执行且进程 exit0，不把这份日志冒称包含全部初始断言。

### 小规模开销和回退边界

11行范围（十条不可领取、一条可领取）一次 process 实测 **36.244ms**。另一个合成行触发器失败并回滚实测 **17.267ms**；单行 fence 增长、lease 失效、旧 worker 提交拒绝合计 **4.410ms**。见 `scan-metrics.txt`、`rollback-metrics.txt`、`Metrics.java`。

这是本机单次小数据证据，没有查询计数、并发压力、全库扫描或生产 p95 结论。没有执行应用版本回退；仅验证一条隔离在途 lease 的失效和事务回滚。未清表、删除 JSON、新表或旧 schema，未启用旧模型。

## 隔离 Python HTTP provider fixture

新 Python app + httpx.MockTransport，仅合成 fixture，独立 fixture-http-budget.sqlite3。其10000000上限只属于这个 fixture，不关联真实共享账本。

Java HttpAcceptance 全部8项通过：Unicode及JSON Pointer证据；49项完整要求/福利和20项核心数组；四维小数分数8.75；unknown最终null；错误jobId拒绝502；缺数组拒绝502；内部401映射Java503；200ms deadline有界504。relay只记录认证是否符合、Cookie/用户身份header是否缺失的布尔值，不保存原始header。见 `http-results.log`、`relay-headers.json`、`fixture-processes.json`。这些结果不能批准模型质量。

## 本轮真实 HTTP → CommandCode

只有 Python 读取指定私有 python-env.json；provider key没有交给 Java。Java只获得内部服务token。固定官方 host、typesafe/jev-1.14.0、httpx trust_env=False、TLS校验，retries=0，无备用传输/模型/host。AI_QUALITY_APPROVED=false。本轮仅合成数据，显式 CMD_ZDR=0；旧 ZDR 422/cmd_zdr_no_providers 是历史记录，本轮未重复花预算，普通成功不代表零保留，真实个人数据不得据此降级。

先以 SQLite read-only URI 读取当日预算，151527/200000，剩48473。真实新增 **3次尝试，全部HTTP200**；最终188658/200000，剩11342，新增保守预留37131。未删库、改账本记录或提高cap。已知真实 usage 总计 input4874/output592；预留不是实际付费token数。

| 本轮调用 | 结果与原值 |
| --- | --- |
| Python HTTP job-analysis 分类 | provider200，input1528/output182，1.891s |
| 同次分析核心 Score | provider200，input796/output18，0.850s；score3.86，confidence0.88，probabilities [0.01,0,0.03,0.05,0.91] |
| Java真实client→Python真实HTTP→match-batch，一个小职位 | provider200，input2550/output392，0.814s；Java退出0，完整响应成功返回，complete=false、score=null |

Python HTTP job-analysis 本身200，status needs_review，三数组、requestId/model/policy/usage和证据完整。要求“必须熟练Python”probability0.96，福利“提供五险一金”0.96，核心 importance3.86。质量关闭且其他分类落于不确定区间，不能把 needs_review 写成 complete。

R4 新真实响应经当前 validate_answers 通过，包括 match 的四个 Score，分别3.96、3.95、3.95、3.93；原 probabilities 全部保存在 `online-results.json`，没有归一化或修改。例如 skills [0,0,0,0.01,0.99] 与 score3.96 由舍入可行区间接受。Java收到本次真实匹配的完整身份、四维状态/证据并通过 client 校验；四维均是 unknown，故 Java这次未走“非null rawScore/probabilities”的数值分支，不能宣称真实 scored 响应已经覆盖 Java该分支。

四维 unknown 原因可追溯：岗位侧证据 Noul 分别0.87/0.76/0.79/0.86，均低于既有0.9接受阈值；简历侧0.94/0.93/0.94/0.95，保留其原文证据。最终四维rawScore=null、probabilities=null，score=null，绝非零分。完整provider原始数值保存在合成白名单answers证据中；没有为产生排名改阈值。未追加第二职位或重复真实请求，剩余预算低于此次匹配约17000的保守预留量，且没有必要耗尽额度。

证据：`online.py`、`OnlineMatch.java`、`online-results.json`、`online-java.json`，无原始敏感header或真实个人内容。

## 质量指标与身份链限制

复用既有小型规则构造中文/中英四句样例。候选召回中文4/4、中英4/4，共8/8；本轮在线分析只跑中文1份，成功返回1/1。中文选中的要求/福利相对规则标签为2/2正确、阳性召回2/2；要求/福利/核心共3处原文切片3/3一致。未在线重跑中英，因此不能报告全样例在线成功率100%。这不是独立人工gold或正式holdout，不能用2/2宣称已达到95%生产precision。

真实匹配1个职位返回成功，但可评分职位0/1，unknown维度4/4，scored维度0/4。没有可比较排名、人工排序一致性、正式子组统计或可靠p95。AI质量批准和自动写回数均0；fixture分数8.75不纳入真实质量统计。

额外安全地验证了当前主API：按安全manifest的8080地址及当前监听核对；使用唯一获准的visual-audit-login.json合成QA登录200，匿名简历请求401，合法简历列表200，不存在/不归属ID返回404；最后正常logout200，token未打印或落证据。见 `auth-check.py`、`auth-results.json`。不存在ID的404不能证明真实其他账号简历归属拒绝。

**尚未完成完整用户身份链**：没有新建加载DownstreamAuthFilter/AuthorizedResume/真实controller/mapper的MVC上下文；未运行wrong-userId到模型前拒绝、真实跨账号简历拒绝、合法用户由主API验证后继续到Python模型的单次全链请求。独立主API登录测试和独立Java→Python成功不可拼接为这个结论。现行主API也未替换为被测HEAD。剩余预算不足以再跑同等真实匹配，未用fixture冒充此门槛，未放宽认证。

## 复现、清理与最终门槛

核心命令（本轮均实际执行）：

```sh
JAVA_HOME=/home/david/.local/share/mise/installs/java/zulu-17 \
PATH=/home/david/.local/share/mise/installs/java/zulu-17/bin:$PATH \
/home/david/.local/share/mise/installs/maven/3.9.16/apache-maven-3.9.16/bin/mvn \
  -o -f backend/pom.xml package -DskipTests
uvx --from uv==0.9.9 uv sync --frozen --project ai-service
python /tmp/boss-jev-postfix-41420e0/run-java.py
PYTHONPATH=$PWD/ai-service ai-service/.venv/bin/python /tmp/boss-jev-postfix-41420e0/online.py
python /tmp/boss-jev-postfix-41420e0/run-fixture.py
PYTHONPATH=$PWD/ai-service ai-service/.venv/bin/python /tmp/boss-jev-postfix-41420e0/auth-check.py
```

Metrics用同一javac/classpath编译并运行，见源码和metrics-process.json。上述脚本不是幂等重放工具：run-java会创建新随机schema，其他counterexample不可向已填schema重复执行；online会消费共享预算，禁止为了重现重复付费。

所有长期新进程记录PID+/proc starttime；Java执行器也记录类名、退出码。online进程自行退出；fixture/relay仅在PID/starttime匹配时terminate并wait。cleanup.json核实这些身份已退出；原服务未清理、未重启。新schema保留，旧schema/日志保留。

功能修复结论：S1/S2/S3已验证，R4真实Python响应兼容已验证，本次Java真实unknown响应协议已验证；Java真实scored数值分支与完整用户身份链仍无法验证。没有观察到需要在本轮改源码的失败。

质量启用结论：**不批准**。仍需独立标注/留出集、各子组precision/recall与覆盖、排序一致性、完整身份链及部署/回退门槛。未运行浏览器、全量unit suite、CI、正式调度、生产写回或生产性能测试；跳过不计通过。
