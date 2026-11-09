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

未在本仓库找到独立人工 gold runner；新增 `app.evaluate`，仅读本地文件、标准输出汇总，不调用 provider、不读密钥/预算账本、不写质量开关或数据库。

```sh
cd ai-service
uv run --frozen python -m app.evaluate \
  --gold /private/reviewed-test.jsonl \
  --predictions /private/frozen-predictions.jsonl \
  --development /private/training-and-calibration-sources.jsonl \
  --min-samples 100 --min-ranking-agreement 0.9
```

最后两个数值只是运行示例，不是本项目已批准样本量或排序门槛。应在看测试结果前由独立验收者冻结；`min-samples` 同时应用整体和每个 subgroup。既有候选召回 >=0.98、已接受 precision >=0.95、证据一致性 =1 不降低。unknownAgreement=1 为该份人工标签的明确一致性检查，不替代概率校准。分类 recall 单独报告，不用拒答比例掩盖召回。排序门槛须显式提供，无默认已批准值。

所有 JSONL 每行一个对象，禁止重复 id/JSON key、缺字段、额外字段。格式如下（仅形状说明，无 gold 标签）：

- gold：`id, groupId, source, split, reviewStatus, reviewer, annotationOrigin, dataOrigin, subgroup, requirements, benefits, unknown, ranking`。
- `source` 是本地原始字符串叶子的 JSON Pointer → 原字符串映射，例如 `/job/jobDescription/requirements`、`/resume`。不能放旧 AI 输出当事实。`groupId` 由数据负责人按同一 JD/近重复职位分组，跨训练/阈值校准/测试保持一致。
- `split="test"`, `reviewStatus="reviewed"`, `annotationOrigin="human"`，非空人工 reviewer、subgroup 必填。`dataOrigin` 仅 `real_public` / `real_authorized` / `synthetic`；synthetic 永不产生总体 pass。独立人工先复核原文、要求/福利、unknown 和排序，再冻结 gold；不允许 Jev 或脚本生成标签冒充人工。元数据是可审计声明，runner 无法验证签名或人类身份。
- requirements/benefits 均为完整 `Evidence` 列表：`{sourcePath,start,end,text}`，Unicode code point 半开跨度，必须精确原文切片。空列表须表示人工确认无阳性，不能表示 pending。预测沿用相同证据形状；将既有 extraction 的 evidence 展平、去重，保留原始 sourcePath 和跨度。gold 和预测必须事前约定同一跨度粒度；precision/recall 按精确跨度匹配，候选召回允许较大候选包含标注子跨度。
- unknown 为相同非空维度键集合 → bool（例如 `jobId:skills`），gold 人工判断是否证据不足，预测取实际 unknown，不能由分数猜。输出 unknownCoverage、unknownAgreement 与计数，缺预测项不能从样本中删除。
- ranking 为同一请求中人工严格排序的可比较 jobId 列表，不支持平局；无可比较排序填 `[]`，该指标 unverifiable。预测列表必须来自冻结的实际分数降序，不能从 gold 抄写，不可评分的职位不补零/不填 ID。缺失预测职位对该 gold 相关 pair 计不一致；额外或重复 ID 拒绝。
- predictions：`id, model, candidates, requirements, benefits, unknown, ranking`；model 固定 `typesafe/jev-1.14.0`，candidates 为切分候选证据列表。必须覆盖全部 gold id，失败样本也不得静默丢弃；如果没有可用完整冻结预测，整次无法验证。
- development：所有训练、提示调优、阈值校准来源的 `id, groupId, source` manifest，必传，确实没有开发集可给空文件并由审核者确认。检测 test 与 development 的 id/groupId、规范化内容和相似度 >=0.9 的近重复，同时拒绝测试集自身重复组。不保证检测所有语义改写/泄漏；人工分组及完整 manifest 仍是前提，不能只提交部分开发来源。

汇总包含 overall/subgroups 的样本及分母、两类 tp/predicted/gold/precision/recall、候选召回、原文证据一致性（含候选）、unknown 覆盖/一致性、排序 pair agreement。不回显原文。零分母用 null；缺样本、合成集、无排序/阳性等不可判断情形为 unverifiable；已观察指标失败为 fail。无效/pending/泄漏输入拒绝，accepted samples=0，不报告虚构质量分数。

退出码 pass=0、fail=1、unverifiable=2。`qualityApproval` 始终 unverifiable：离线指标 pass 也不是 `AI_QUALITY_APPROVED` 授权。成本、p95、核心项质量及生产身份/部署验收不在此 runner 范围，须分别完成。当前无真实人工 gold，本次仅合成单测，不输出领域质量通过率。
