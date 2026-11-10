# Jev 隐私保护与独立评估

核查日期：2026-11-08 America/Los_Angeles（UTC 2026-11-09）。仅 ai-service，不替代产品数据处理审批。

## 官方说明与实际证据

- [官方隐私政策](https://commandcode.ai/privacy)，页面更新 October 21 2026：普通 AI 请求内容（输入、附件、代码、输出等）最多保留 30 天，套餐/书面协议可能另有要求；启用 ZDR 才瞬时处理、不保存正文。仍保存不含正文的计费、安全元数据。普通请求也可能涉及跨境及上游自己的条款，不能以“未用于训练”推导“不保存”。
- [官方 ZDR 文档](https://commandcode.ai/docs/resources/zdr)：API 使用 `x-cmd-zdr: 1`，只路由具备 ZDR/no-training 协议且有容量的上游，无可用上游则 422 / `cmd_zdr_no_providers`；可改变上游及成本，不能假定所有模型/时刻可用。
- [官方安全说明](https://commandcode.ai/docs/resources/security) 将请求内容保留规则指向隐私政策。桌面本地历史/源码等描述不能代替 Provider API 的正文保留政策。
- 仓库既有运行记录 `docs/operations/commandcode-jev-acceptance-20261108.md` 曾实际得到 Jev ZDR 422 / `cmd_zdr_no_providers`，Python 对外为 503 / `zdr_unavailable`。后续 postfix、identity 报告里的普通 HTTP 200 均不能证明零保留。本次没有重跑真实 API，当前该账户/Jev 的 ZDR 可用性仍无法验证；官方承诺与已观察到的拒绝并不矛盾，拒绝正是失败关闭。

## 保护与部署影响

原 `Settings.zdr=False` / `CMD_ZDR` 缺省 `0` 允许真实文本直接出站，`AI_QUALITY_APPROVED=0` 只约束质量/投影，不阻止推理。历史迁移规范明确将“是否要求 ZDR”留给产品政策，并未证明已有 blanket ZDR 审批。本次按最新任务“不得静默让用户数据出站”在策略未明确、且不能可靠判断输入是否合成的边界保守拒绝所有非 ZDR 出站；没有把历史说明改写成已批准政策。

保留原有配置默认，不偷偷开启可能增加费用的路由。运营者须显式设置 `CMD_ZDR=1`。配置缺失仍 `not_configured` / 503；凭据等齐全但 `CMD_ZDR!=1` 时 readiness 和内部业务入口为 `zdr_required` / 503，在读正文、provider 请求和预算预留前拒绝；共享 Jev 客户端另在出站前同样拒绝，覆盖直接调用。存活探针正常。没有 synthetic 请求标志、脱敏绕过、备用模型或去掉 ZDR 的重试。

Java JSON 成功契约、模型/策略版本与阈值不变；Python 对 Java 返回既有安全错误形状及 503，Java 的既有 unavailable 路径会阻塞该次分析/匹配。调度现有 503 可能延后重试，仍须保持调度/写回关闭，避免无效任务消耗调度尝试。输入非法在配置就绪后仍是 400/422；上游 ZDR 422 映射服务 503，不是用户输入 422。未改 Java，也未新验收 Java 映射。默认非 ZDR 的旧合成在线 harness 现在也会阻塞；不能增加请求标志绕过。

ZDR 是供应商政策及路由保证，不是本次独立审计其存储系统；字段最小化、授权、同意及跨境政策仍需父任务确认。脱敏后的经历/教育等组合可能再识别个人，不是绝对匿名。真实简历没有在本次被读取、记录或发送。

## 离线评估入口

`app.evaluate` 是独立离线人工 gold runner，仅读本地文件、标准输出汇总，不调用 provider、不读密钥/预算账本、不写质量开关或数据库。

```sh
cd ai-service
uv run --frozen python -m app.evaluate \
  --gold /private/reviewed-test.jsonl \
  --predictions /private/frozen-predictions.jsonl \
  --development /private/training-and-calibration-sources.jsonl \
  --min-samples 100 --min-ranking-agreement 0.9
```

最后两个数值只是运行示例，不是本项目已批准样本量或排序门槛。应在看测试结果前由独立验收者冻结；`min-samples` 同时应用整体和每个 subgroup。既有候选召回 >=0.98、已接受 precision >=0.95、证据一致性 =1 不降低。unknownAgreement=1 为该份人工标签的明确一致性检查，不替代概率校准。分类 recall 单独报告，不用拒答比例掩盖召回。排序门槛须显式提供，无默认已批准值。

所有 JSONL 每行一个对象，禁止重复 id/JSON key、缺必填字段、额外字段。格式如下（仅形状说明，无 gold 标签）：

- gold：`id, groupId, source, split, reviewStatus, reviewer, annotationOrigin, dataOrigin, subgroup, requirements, benefits, unknown, ranking`，另有可空 `coreRequirements`（默认 null）。
- `source` 是本地原始字符串叶子的 JSON Pointer → 原字符串映射，例如 `/job/jobDescription/requirements`、`/resume`。不能放旧 AI 输出当事实。`groupId` 由数据负责人按同一 JD/近重复职位分组，跨训练/阈值校准/测试保持一致。
- `split="test"`, `reviewStatus="reviewed"`, `annotationOrigin="human"`，非空人工 reviewer、subgroup 必填。`dataOrigin` 仅 `real_public` / `real_authorized` / `synthetic`；synthetic 永不产生总体 pass。独立人工先复核原文、要求/福利、unknown 和排序，再冻结 gold；不允许 Jev 或脚本生成标签冒充人工。元数据是可审计声明，runner 无法验证签名或人类身份。
- requirements/benefits 均为完整 `Evidence` 列表：`{sourcePath,start,end,text}`，Unicode code point 半开跨度，必须精确原文切片。空列表须表示人工确认无阳性，不能表示 pending。预测沿用相同证据形状；将既有 extraction 的 evidence 展平、去重，保留原始 sourcePath 和跨度。gold 和预测必须事前约定同一跨度粒度；precision/recall 按精确跨度匹配，候选召回允许较大候选包含标注子跨度。
- `coreRequirements` 是各自 requirements 的证据子集，沿用原始 sourcePath/start/end/text 身份、跨度、去重和原文核验。gold 由独立人工选择，预测来自冻结实际输出；null 表示缺标注/输出，`[]` 只表示已确认无核心阳性。输出 `coreSelection` 的 tp/predicted/gold/precision/recall；计数为已提供标签的观测数，任一侧缺失时完整 precision/recall 为 null。核心 precision 复用 >=0.95 门槛，缺失标签也不能掩盖可比较样本已观察到的 precision 失败。
- unknown 为相同非空维度键集合 → bool（例如 `jobId:skills`），gold 人工判断是否证据不足，预测取实际 unknown，不能由分数猜。输出 unknownCoverage、unknownAgreement 与计数，缺预测项不能从样本中删除。
- ranking 为同一请求中人工严格排序的可比较 jobId 列表，不支持平局；ID 必须非空白。每条 gold 是评估请求记录，ranking ID 是该请求内职位 ID，不必等于 gold 记录 ID；一条记录可包含多个职位，禁止重复 groupId 不禁止这种组级排序。职位身份和同请求归属须由审核者对照冻结请求核验，当前 schema 不提供独立的请求职位注册表，runner 不声称自动证明这一点。无可比较排序填 `[]`，该指标 unverifiable。预测列表必须来自冻结的实际分数降序，不能从 gold 抄写，不可评分的职位不补零/不填 ID。缺失预测职位对该 gold 相关 pair 计不一致；额外或重复 ID 拒绝。
- predictions：`id, model, candidates, requirements, benefits, unknown, ranking`；model 固定 `typesafe/jev-1.14.0`，candidates 为切分候选证据列表。必须覆盖全部 gold id，失败样本也不得静默丢弃；如果没有可用完整冻结预测，整次无法验证。
- predictions 另有可空 `latencyMs` 和 `costUsd`（默认 null）。latencyMs 只接受有限非负数值毫秒，拒绝 bool、字符串、NaN/Infinity 和负数；costUsd 只接受非负 USD 定点十进制字符串（数字加可选小数部分），拒绝数值、bool、非有限值。真实 0 与未知 null 分开；不从 token 预留或公开单价推算实际费用。
- 观测以每条 prediction 对应的完整操作为单位：延迟从操作开始到成功/失败终态，包含重试和等待；成本来自可审计实际计费记录，包含该操作失败尝试与重试费用。失败样本继续纳入同一总体；跨记录共享请求费用只能在有可靠归属、无重复计费时分配，否则保留 null。无法可靠获得完整观测时不补零，也不删除该样本。runner 不采样时钟、不请求账单、不生成这些观测。
- development：所有训练、提示调优、阈值校准来源的 `id, groupId, source` manifest，必传，确实没有开发集可给空文件并由审核者确认。检测 test 与 development 的 id/groupId、规范化内容和相似度 >=0.9 的近重复，同时拒绝测试集自身重复组。不保证检测所有语义改写/泄漏；人工分组及完整 manifest 仍是前提，不能只提交部分开发来源。

汇总包含 overall/subgroups 的样本及分母、两类 tp/predicted/gold/precision/recall、候选召回、原文证据一致性（含候选）、unknown 覆盖/一致性、排序 pair agreement，以及核心选择、成本与延迟。`cost` 包含 `totalUsd`（精确求和字符串或 null）、`knownSamples`、`coverage`；`latency` 包含 `p95Ms`（毫秒数或 null）、`knownSamples`、`coverage`、`percentileMethod="nearest-rank"`。仅整体/该子组 N>0 且全部样本均有相应观测时输出聚合，否则聚合为 null；coverage 为 knownSamples/N，空总体为 null。p95 是该总体延迟排序后第 ceil(0.95*N) 个值，不使用已知子集替代完整总体。不回显原文。零分母用 null；缺样本、合成集、无排序/阳性等不可判断情形为 unverifiable；已观察指标失败为 fail。无效/pending/泄漏输入拒绝，accepted samples=0，不报告虚构质量分数。

退出码 pass=0、fail=1、unverifiable=2。`qualityApproval` 始终 unverifiable：离线指标 pass 也不是 `AI_QUALITY_APPROVED` 授权。历史 JSONL 无新增字段仍可加载，但缺新标注、完整成本/延迟观测或可比较核心阳性时不能产生 pass，而为 unverifiable；已知指标失败仍优先为 fail。生产身份/部署验收须分别完成。当前无真实人工 gold，本次仅合成单测；单测来源元数据、标签和遥测均为模拟，不能作为真实人工 gold、成本、延迟或领域质量通过率。


### 职位来源路径与兼容边界（2026-11-10）

新数据统一从原始请求保留 canonical JSON Pointer：单职位 `/job/jobDescription/...`，多职位 `/jobs/<jobId>/jobDescription/...`；纯抽取旧数据的 `/jobDescription/...` 继续有效，字符串根可省略尾部 `/...`。`<jobId>` 按 RFC 6901 转义，不能包含未转义的路径分隔符。只可在生成 manifest 时按事实结构约定路径，同时同步全部 evidence 引用；不得改写原文、偏移或把 AI 输出当源数据。

已存在的 `/job/requirements`、`/jobs/<jobId>/text` 等可识别职位路径无需迁移即可参与职位级泄漏检测：同一职位根下字符串叶子合并比较，简历不参与该比较；包含 `jobDescription` 的历史路径保持原有分组。不同路径表示的同一职位文本也进行比较，阈值沿用 0.9。全记录比较仍保留。不要把多个职位压成一个未区分的文本池；若同一职位被拆成不同结构、额外元数据稀释了内容或任意路径不暴露职位身份，自动检测并不完备。

任意其他合法历史 sourcePath 仍能加载及按原文验证，也仍参与全记录和 groupId 检查，但不能保证职位级检测；审核者必须核验原始来源、完整开发 manifest 和稳定 groupId，必要时离线转换为 canonical 路径后重评。路径识别不是语义去重或来源身份认证。零阳性、无可比较排序继续为 null/unverifiable，不新增召回阈值。辅助标注口径见 `docs/operations/ai-assisted-annotation-rubric-20261110.md`，不能作为人工 gold 或正式质量批准。
