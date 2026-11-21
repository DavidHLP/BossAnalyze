# 离线 AI 验收规范（2026-11-21）

本规范允许以离线 AI 复核的 gold 和合规样本作为自动验收证据，适用于 `ai-service` 的 `app.evaluate`。选择 `--acceptance ai` 启用该策略。默认模式为 `--acceptance human`，继续只接受人工复核标签，合成样本仍不能产生总体 `pass`，报告格式和退出码沿用原契约。

## 验收证据

| 输入 | 要求 |
| --- | --- |
| 留出 gold JSONL | 每条记录须为 `split="test"`，有非空白 `reviewer`、`subgroup` 和稳定的 `groupId`。人工标签用 `annotationOrigin="human"`、`reviewStatus="reviewed"`。AI 标签用 `annotationOrigin="ai"`、`reviewStatus="ai_reviewed"`，`reviewer` 记录离线复核者。离线 AI 复核计作独立复核，须与生成待评预测的过程分开。AI 模式可使用任一来源或混合使用。 |
| 原文与标注 | `dataOrigin` 可为 `real_public`（公开且可合法用于该用途）、`real_authorized`（用途已获授权）或 `synthetic`（合成）。AI 模式允许合成样本参与通过判定。保留原始来源映射与精确文本跨度，标注要求、福利、核心要求子集、非空维度的 unknown 判断及请求内可比较职位排序。 |
| 冻结预测 JSONL | 固定模型 `typesafe/jev-1.14.0`，逐条覆盖全部 gold ID，无重复或额外记录。保留候选、分类证据、核心项、unknown 和排序，并提供逐条 `costUsd` 与 `latencyMs`。预测取自独立的冻结输出，不能从 gold 反填。 |
| 开发来源清单 JSONL | 提交训练、提示调优和阈值校准使用的完整 `id, groupId, source` 清单。确认确无开发来源时方可提交空清单。检测测试集内部以及与开发集之间的 ID、分组、规范化内容和全记录或职位级近重复，沿用相似度 0.9 门槛。 |

字段形状、证据跨度、冻结观测口径和调用示例见 [Jev 隐私保护与独立评估](../../ai-service/QUALITY-PRIVACY.md#离线评估入口)。合成测试中的标签、来源声明和观测仅说明测试样例的结果，不能证明真实业务质量、供应商计费或线上时延。

## 通过条件与报告

- 整体与每个子组都须达到 `--min-samples`。候选召回 ≥0.98，证据一致性和 unknown 一致性均须为 1，要求、福利和核心项 precision 均须 ≥0.95，排序 pair agreement 须达到显式给出的 `--min-ranking-agreement`。样本量与排序门槛须在查看测试结果前确定。
- 整体与每个子组均须有可比较的核心阳性及完整成本和延迟观测。成本和 p95 延迟报告覆盖率和完整聚合，沿用既有完整性要求。可判定指标越界为 `fail`（退出码 1），优先于缺失观测。阈值无法判断、样本不足或输入被拒绝为 `unverifiable`（退出码 2）。泄漏或预测覆盖不匹配属于输入拒绝，报告样本数为 0。
- AI 模式报告包含 `acceptance="ai"`。一份报告内的标签来源由 `labelSource` 标记为 `ai`、`human` 或 `mixed`，空集或输入被拒绝时记为 `null`。只有所有门禁通过（`status="pass"`，退出码 0）时，`qualityApproval` 才为 `ai_accepted`。其余结果及人工模式的 `qualityApproval` 均为 `unverifiable`。

## 证据边界与现有控制

AI 标签和 AI 验收不能提供独立的人类真值。报告的 `reason` 明示这一限制。评估器检查元数据和文件内容，无法认证复核者身份、数据授权或复核过程的独立性，也无法穷尽语义泄漏。质量结论仅覆盖提交的留出样本及其标签口径。

`ai_accepted` 是离线报告值。评估器仅读本地文件并输出汇总。运行时 `AI_QUALITY_APPROVED`、调度和写回门禁、供应商及预算策略均沿用现有控制。简历匹配暂停和出站限制继续生效，真实简历禁止发送给供应商。BOSS 采样与会话控制沿用现有流程，离线验收不启动或使用 BOSS 会话。参见 [简历匹配隐私暂停](../operations/resume-matching-privacy-hold.md)及 [BOSS 采样验收](../operations/boss-sampling-acceptance-20261108.md)。
