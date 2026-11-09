# BossAnalyze：Python + CommandCode Jev 全量迁移设计

日期：2026-11-08（America/Los_Angeles）。交付类型：设计与接入调查，尚未实施业务代码、数据库迁移或服务切换。

## 1. 最终范围与决策

按用户最新要求，全部现有 AI 判断迁移到 **CommandCode Provider API 提供的 Jev**，实现语言为 Python。取消上一版 LiteLLM 多厂商生成模型方案，不保留 Ollama、OpenRouter、Codex CLI 或其他模型的自动回退。这里“全部迁移”指全部 AI 能力，不迁移 Java 鉴权、业务数据库写入、爬虫抓取或 Spark 计算。

原文解析、候选切分、字符串序列化和数学计算由确定性代码完成；要求/福利分类、核心程度及匹配程度由 Jev 完成。低置信、缺证据或上游失败必须显式表示，不能以旧模型补答案或伪装成成功。

接入采用 **Python FastAPI + httpx + Pydantic**，直接调用原生 System One 协议。由于本次只接一个已明确协议的供应商，不再引入通用 LLM 网关或另一层供应商框架。依赖版本在实施时核实并锁定，不以本设计冒充已安装和兼容的环境。

## 2. 已核查的 CommandCode 接入契约

| 项目 | 决策 |
| --- | --- |
| API base | `https://api.commandcode.ai/provider/v1` |
| 调用 | `POST /systemone`，非流式 |
| 模型 | `typesafe/jev-1.14.0`，版本写在外部配置中，避免 alias 无感升级 |
| 认证 | `Authorization: Bearer <CommandCode API key>` |
| 请求 | `model`、`state`、`questions` |
| 回答 | 按问题 ID 对应 `answers`，使用 `noul` / `choice` / `score` |
| 应用分批硬限制 | 每次不超过 100 个问题；上下文还须满足供应商 token 上限 |

CommandCode 官方说明：Jev 使用单独的 System One endpoint，模型发布版本可以显式指定；供应商限制是每请求最多 100 个问题、合计 64k、state 加单个问题最多 32k。不能把 TypeSafe 直连“问题数只受上下文约束”的描述照搬到聚合服务。[Provider API](https://commandcode.ai/docs/provider)、[版本模型页](https://commandcode.ai/models/jev-1-14-0)。

GOAT 支持 API 调用，消费共享该账户套餐额度；不是无限免费后台调用。官方当前标示 5 小时、每周、每月分别最多 $20、$50、$100 的用量窗口。项目必须自行限制候选数、并发、重试和日使用预算，不自动购买额度。[GOAT 说明](https://commandcode.ai/docs/plans/goat)。

## 3. 凭据来源与本轮实测边界

已只读找到 `/home/david/.cc-switch/cc-switch.db` 中名为 Command Code 的 Claude 配置：含 `ANTHROPIC_BASE_URL` 和 `ANTHROPIC_AUTH_TOKEN`。核查到 base 的 origin 是官方 `https://api.commandcode.ai`，path 为 `/provider`；这里只记录公开 endpoint 与字段名，不记录 key、指纹、前后缀或账号信息。

注意：CC Switch 的配置字段采用 Anthropic 命名，不代表 Jev 应调用 `/messages`。应用应映射到专用 `CMD_API_BASE`、`CMD_API_KEY`、`JEV_MODEL`；对当前 base 正确添加 `/v1/systemone`，不重复拼接 `/provider` 或 `/v1`。请求仅去经过核对的官方 host，禁止携密钥跟随跨主机重定向。

实施时由操作者在本机将所需单一 key 注入私有环境文件或进程环境；应用不需要读取整个 CC Switch 数据库，也不随桌面供应商切换改变生产配置。不修改 CC Switch、不切换当前 provider，不复制其他服务凭据，不把 key 交给 Java、浏览器、日志或报告。

本轮尝试仅使用合成职位与合成简历。初次 urllib 客户端请求返回 403；显式应用 User-Agent 的已认证 `/models` 返回 200、96 条记录，本轮筛选未找到 Jev 条目；随后 urllib 合成推理请求出现 `URLError`。改用 curl 诊断后，同一账户的 Noul 请求返回 HTTP 200、`model=typesafe/jev-1.14.0`、`noul=0.8`，usage 为输入 440 / 输出 20 token。目录未列出不妨碍该独立端点实际调用；同时，urllib 的失败根因尚未定位，不能将 curl 成功当成后续 Python httpx 集成已通过。

最终接入证据：`/tmp/bossanalyze-commandcode-jev-probe-20261108.json`。另对 `x-cmd-zdr: 1` 做合成测试，返回 HTTP 422；最小同输入确认的错误码为 `cmd_zdr_no_providers`。见 `/tmp/bossanalyze-commandcode-jev-zdr-probe-20261108.json`。普通 Noul 连通性通过，零数据保留路径当前不可用；Choice/Score 完整成功契约与领域质量仍未通过本轮运行验证。

## 4. 服务边界

```text
Nuxt / 既有业务调用者
          |
          v
Java crawler：认证授权、归属检查、查询、调度、有限重试任务、落库
          |
     内部服务令牌 + 有界输入 + deadline
          v
Python AI：解析/切分 → Jev 问题构造 → 回答校验 → 证据与评分
          |
          v
CommandCode /provider/v1/systemone → typesafe/jev-1.14.0
```

Python 无 MySQL/Mongo/Redis/RustFS 业务凭据。只绑定 loopback 或私有网络，使用独立内部服务认证；跨机部署使用 TLS。Java 不向 Python转发用户 JWT，Python 不向 CommandCode 转发用户 JWT/Cookie。Python 不接受客户端覆盖 endpoint、model、key、headers 或任意提示模板。

网络断开或 Jev 未配置时，AI readiness 不可用，其余 Java API 仍可启动。自动 AI 调度默认关闭；完成接入和质量验收后显式启用。

## 5. 要求、福利与核心要求：从生成改为带证据的选择

### 5.1 候选由代码产生，不能先漏掉原文再要求模型补回

当前数据模型已有 `jobDescription.requirements`、`responsibilities`、`fullDescription`、`keywords`、`jobTags` 和 `basicInfo`。优先保留已有结构化来源，但不把字段名当真值：对整段描述做补充候选，福利标签也需与来源一起核对。旧 `analysisReport` 和旧 AI 输出不作为新的事实来源。

切分规则 v1：

1. 按原字段保存文本，以段落、列表边界和分号为主构建候选，保留标题和相邻上下文。
2. 不按每个逗号盲切，不破坏“或”“并且”“无需”“优先”“至少”等条件和否定范围。超长段落采用有重叠的有界窗口；不能静默截掉尾部。
3. 每个候选含 `candidate_id`、`source_path`、`start`、`end`、`text`、`context`。offset 是源字段字符串的 Unicode code point 区间，不是 Java UTF-16 下标；Java 可用 `offsetByCodePoints` 校验，不能直接 substring 同一整数。
4. 输出 text 必须等于对应源字段切片。必要的换行规范化保留映射；第一版不做丢信息的全文去空白。
5. 重复项可合并展示，但保留所有来源。相似而不等价的要求不合并。已结构化的学历/经验条件可确定性加入候选，同样保留原始字段证据。

候选召回率是 Jev 单模型方案的关键上限：没有进入候选的内容不会被 Jev 创造出来。无法覆盖的输入必须标记为未完成或待复核，不承诺与自由生成模型完全等价。

### 5.2 Jev 做多标签判断

对每个候选分别问两个 Noul：是否表达明确的任职要求；是否表达雇主实际提供的福利。两者不是互斥标签，混合句允许同时命中。岗位职责不能因为含技术名就自动当作录用门槛；“不提供股票”不能生成“股票福利”。

一个批次最多 48 个候选，96 个候选问题，加 2 个文档范围检查，共 98 个问题。每个问题的 instructions 明确包含目标候选 ID 与定位说明，不能假设 Jev 能看见问题字典的 key；官方说明问题 ID 只用于请求/回答关联。[Noul 协议](https://docs.typesafe.ai/primitives/noul)。

文档范围检查用于发现“全文明确存在要求，但所有候选都未选中”之类不一致，不把它当作分类正确性的证明。否定、复合条件、跨段上下文由标注集验证。中间概率区间保留为 uncertain；阈值必须用中文招聘验证集校准，在校准前不启用自动写回。

### 5.3 核心要求与可读结果

仅对已接受的要求候选使用 Jev Score 评估核心程度，等级描述区分辅助项、加分项、常规要求、主要要求、关键要求；再由代码排序和选择。最多展示 20 项，实际只有 2 项就保留 2 项，不强行补够 5 项。

提取结果直接使用原文片段，不要求 Jev 重写、总结或输出任意字符串。当前核心要求方法存在但未找到业务调用者，迁移将其纳入统一分析结果与既有方法兼容层，不新增一个没有调用者的独立服务。

无任何阳性候选且全部问题完成、证据充分时，才可表示“已分析，原文未说明”；问题未完成、候选被截断或仍有不确定内容时，不得以 `[]` 代表成功。

## 6. 简历匹配：独立事实判断 + 多维评分

输入来自已授权用户的本次请求或经 Java 校验归属的简历。`userId` 和 `resumeId` 不能作为自行声明身份的证明。对与匹配无关的个人标识做字段级最小化，不以性别、民族、年龄等推断能力。

每职位一份 state（该职位与最小必要简历证据），一组相关问题；Python 批量入口通过有限并发处理多个职位，不把几十份职位混在同一个 state 中交叉评分。每份请求同时判断证据是否充分，以及技能、相关经历、明确经验门槛、明确学历/证书门槛的满足程度。岗位未提及的维度为 not_applicable；简历未写清为 unknown，不自动判不满足。

评分采用每维 5 个有具体语义的等级（0–4）。Jev Score 最多接受 10 个等级，且返回连续值；不能直接构造 0–10 的 11 个等级，也不能把小数解析成整数。[Score 协议](https://docs.typesafe.ai/primitives/score)。

首版可配置权重草案为技能 0.5、相关经历 0.3、经验门槛 0.1、学历/证书 0.1。这是待用人工排序样本校准的产品参数，不是模型文档给出的最佳权重。仅对岗位明确不适用的维度重分配权重；任何适用维度 unknown 时不生成完整最终分数，禁止删除未知维度以抬高分数。

所有适用维度均可评估时：

`score_0_10 = 10 × Σ(weight_i × raw_score_i / 4)`

内部保留小数，按原始精度排名。旧整数契约仅在兼容出口做统一 HALF_UP 四舍五入，并测试 0、4、边界和并列；不得用 Python 默认 ties-to-even 与 Java 不同规则。Jev `confidence` 不是实际正确率，也不拿它乘在分数上当作能力折扣。[Confidence](https://docs.typesafe.ai/confidence)。

输出还包含每维状态、分布、相关岗位候选 ID、简历证据 ID 和评分策略版本。证据关联同样由 Jev 在预先切分的候选集合内判断，代码校验引用存在且文本精确可回溯；不能由模型自由编造引用。

这是给求职者的岗位推荐参考，不把模型分数自动转为人员录用或淘汰。现有 TOP 10 规则只对有效可比评分生效；显示范围和未评分数量必须可见。

## 7. 内部接口与外部兼容

Python 内部提供两个接口：

- `POST /internal/v1/job-analysis`：有界职位输入；返回 requirements、benefits、coreRequirements，各项带原文证据，以及 complete/no_evidence/needs_review 等业务状态。
- `POST /internal/v1/match-batch`：简历、有限 jobs、剩余 deadline；返回逐职位 scored/insufficient_evidence/failed、维度和分数。输入输出 job ID 必须严格对应，不接受漏项、重复项、额外项或跨批次混入。

不暴露通用 `/chat/completions` 业务入口。供应商异常响应不直接向客户端转发；Python 检查 answers 的 ID 集合、类型、范围、有限数值、分布与等级映射。

Java 现有 `/api/v1/ai/get-user-similarity` 成功列表契约保留。设计新增 `/api/v2/ai/matches`，返回 envelope，含 items、unrankable、consideredCount、范围、complete、model 和 policyVersion，以表达未知/部分失败，不能在旧列表里偷偷塞入另一种顶层结构。新增路径必须进入现有安全过滤与代理契约测试。

旧入口只在承诺范围完整处理且所有候选均具备有效评分时返回既有结果；纯粹无职位返回合法空列表。证据不足明确返回业务不可评估错误，外部依赖失败为 503/504，不能丢弃失败职位后把剩余列表宣称全局 TOP 10。调用者迁到 v2 后再按弃用计划删除 v1。

本轮在 frontend/app 搜索未找到活跃简历匹配页面调用，但这不是外部消费者不存在的证明。招聘只读页面确实消费 jobRequirements/employeeBenefits 字符串数组，应保持字段类型，并添加分析状态/来源信息，区分“未解析、原文未说明、待复核、失败、已完成”。不新增整套人工审核后台。

## 8. 数据库、序列化与并发

原 `t_job_detail`、detail_data 及旧提取值全部保留。新增一张 Java 所有的 `job_ai_analysis` 表，用于避免重复计费、记录来源和原子投影：

- job_id、input_digest、pipeline_version、model_id、policy_version，组成分析身份；input_digest 是对选定源字段的固定规范序列化摘要，用于防止旧结果覆盖新输入，不是额外的文件校验流程。
- 工作状态、attempt_count、next_retry_at、lease_until、递增 fence、时间；结果 JSON、受控错误类别、厂商返回的 usage。
- 唯一约束限制同一分析身份的竞争；领取使用数据库原子条件更新。lease 过期后的旧 worker 不得提交结果。供应商超时可能已经计费，无法保证网络调用 exactly-once；只保证应用不重复提交已完成结果和不无限重试。

正常状态：pending → running → completed / needs_review / retry_wait / failed；源改变标记 superseded，新身份重新分析。no_evidence 是已完成分析结果，不再依赖“字段 NULL”无限调度。

网络调用期间不持有数据库长事务。提交时短事务锁定/复核当前源、`is_deleted=0`、身份和 fence；结果表与旧数组投影同事务更新。失败或 needs_review 不覆盖现有内容，也不把旧结果冒充当前 Jev 结果。跨多个实例使用数据库领取，不只依赖 JVM 内存锁。

新投影使用真正 JSON 数组字符串，禁止 `List.toString()`。必须先升级全部消费者：当前 crawler `FormatDataService` 逗号拆分；analytics `TwoDimensionalAnalysisChart` 先去方括号/空白再拆逗号，会破坏含逗号、括号和英文空格的原文。新 reader 保留原值后优先 JSON 解析，历史 List.toString 格式仅在明确 legacy 状态下兼容；无法无损解释的旧值保留原文并标记 legacy_unverified，不猜测后批量重写。

迁移使用新增幂等脚本，先在隔离 schema 演练；不导入带 DROP 的 SQL 快照。历史未带 Jev 来源的数据不能通过改 provider 字段被“认证”为 Jev。回填分批、可暂停，按用户选择的范围处理，不自动重算全库。

简历匹配第一版不持久化简历全文、证据片段或共享跨用户缓存；结果只在授权请求内返回。必要运行统计不含个人文本。

## 9. 限流、超时、隐私与错误

显式配置每请求候选数、问题数、输入字节/token 预算、总 deadline、并发和日 token 预算。问题数 100 是上游硬边界；下游没有可信 tokenizer 时用保守限制并检查上游错误，不能称按字符数精确满足 token 上限。超限返回完整性错误或要求分页，不能静默截断后标记 complete。

Java 不做 HTTP 重试，Python 独占单次推理重试策略；默认最多一次额外尝试，仅网络/429/5xx 在总 deadline 内允许。401、403、参数错误、无权限模型、内容拒绝不盲重试。调度任务延后重试和 HTTP 重试有统一次数预算。CommandCode key 失效映射为 AI 不可用，不映射为用户 JWT 失效造成登出。

应用 log 只记录 request ID、任务、模型版本、耗时、状态和实际 usage；禁用 httpx wire debug，异常不回显请求体或密钥。CC Switch 配置、私有文件、token 均不作为业务输入。

真实简历出站前落实既有数据处理边界和字段最小化。ZDR 必须是显式配置：开启时发送 `x-cmd-zdr: 1`，若返回 `cmd_zdr_no_providers` 则失败关闭，不能自动删除 header 重试。当前合成测试证实该账户/模型的此路径不可用；普通请求成功不能宣传为零数据保留。是否要求 ZDR 由产品的数据处理政策决定，不能把本次探测擅自新增为用户要求或掩盖其缺失；若现有政策要求 ZDR，真实简历启用就在这里阻塞。本轮没有发送真实个人或业务数据。

## 10. 文件级实施范围

1. 新 `ai-service/`：依赖锁、app/config、候选切分、Jev HTTP client、问题模板、输出 Schema、匹配计算、聚焦契约测试及启动文档；不增加数据库驱动或通用 agent 框架。
2. `backend/crawler/.../ai/service/AIExtractionOfJobRequirements.java` 改为 Python 业务客户端；保留现有业务方法，移除提示拼接和字符串取数字。
3. `RequirementAnalyzer`、`UserSimilarity`、controller/DTO：有界候选、批次、v2 状态及精度；核对默认排序与 TOP 10 行为。
4. `ExtractionOfJobRequirements`、task/Abstract、两个具体 task、Mapper/XML：一次分析产出要求与福利，领取/完成状态与有条件写回，不再两套任务重复计费；锁覆盖真实工作周期。
5. 新 analysis entity/Mapper、单表增量迁移及配置；不让 Python 直接落库。
6. crawler `FormatDataService`、analytics `TwoDimensionalAnalysisChart` 及相关 DTO/fixtures：先读兼容，再启用 JSON 数组新写入；仅修数据契约，不迁 Spark 到 Python。
7. frontend API types、recruitment normalizer/JobSheet、同源代理与安全配置：显示数据状态，兼容新的受保护 v2 路由；不增加无调用者的新页面。
8. 删除旧 OpenRouter AI 类、Ollama starter 及无效配置；父 Maven 依赖管理只有在确认其他模块无引用时清理。更新 CI、私有配置示例和运维文档。应用启动不依赖读取 CC Switch。

## 11. 迁移顺序、验收与回退

先完成真实 CommandCode 连通性与 ZDR 合成验证，再实现离线 Python 契约；随后先发布 JSON reader 兼容层和增量表结构，再发布 Java/Python 在调度关闭状态联调，最后对授权的小批合成/标注数据启用。旧结果保持可读，旧模型不得在迁移后继续自动运行。

验证集按职位分组划分训练阈值/验证/留出测试，避免同一 JD 的近重复泄漏。至少覆盖：中文为主及中英混合、未知技能名、否定、优先与必需、职责和条件混合、标签与正文冲突、逗号/引号/emoji、长描述和无要求/无福利；匹配包含证据缺失、相同平均分不同分布、职位 ID 错配、提示注入及选项顺序扰动。

验收必须分别报告候选召回、分类 precision/recall、原文证据一致性、核心项选择质量、人工匹配排序一致性、未知覆盖、成本和 p95 延迟。建议准入目标：证据切片一致性 100%，无无证据新增；候选召回不低于 98%、已自动接受分类 precision 不低于 95%，各主要子组单独报告。它们是设计提出的门槛，不是已取得的成绩；未达标时不能只调高拒答比例后宣称全量完成，必须同时报告覆盖率并改切分或问题。

系统契约须覆盖：无 key 不影响非 AI 启动；CommandCode 403/429/超时不引发全局登出或无限重试；101 个问题在本地分批；空/漏/重复 answers、NaN/Infinity、越界概率被拒绝；unknown 不变零分；提交前删除/修改输入以及 lease 竞争不会被旧 worker 覆盖；数组往返保留逗号、括号和空格；失败不污染旧记录；v1/v2 和同源代理/鉴权回归。

回退先关闭 AI 调度并使在途 lease 失效，再回退应用，保留新表和新 JSON 数据；因此 reader 兼容版本必须可回退。不得为回退清表、启用旧模型自动替换、重放全库或删除存储卷。

## 12. 本轮证据与尚未完成事项

设计所依赖源码已直接核查，并对引用文件做图覆盖检查。SQL 索引报告 partial，相关 `t_job_detail` 的 115–139 行已直接读取；不声称对全 SQL 或全仓做了完备审计。本地仍在 `4cb7ec975df4dd00f380213041295dc0412a50c9`，设计没有修改应用源码或现行服务。

> 🧠 **From Hindsight memory (BossAnalyze Python 多厂商 AI 接入)** — 旧页把“AI 暂缓、不迁 Python”保留为当前决定，已与用户后续要求冲突；本轮已提交纠正并更新同一 initiative。这里以最新“Python + CommandCode Jev 全量迁移设计”为准，不把旧批次排除项继续施加于新方案。

尚未实施：Python 应用、Java 替换、表结构、前端状态、全量/标注集测试、真实简历处理或部署。Jev 实测状态只看下面最终接入结果，不能由官方支持文档推定已通过。
