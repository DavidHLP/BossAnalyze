# 离线质量报告完整性修复（2026-11-09）

关联 [#60](https://github.com/DavidHLP/BossAnalyze/issues/60)，属于 #40/#44/#54。依据 [迁移规范第11节](commandcode-jev-migration-20261108.md)，成本、p95延迟和核心项选择质量也必须报告。目前 `Gold`/`Prediction` 没有这些输入字段，`metrics`也未输出；`QUALITY-PRIVACY.md`将其留待另外完成。这是独立于真实供应商/标注集的工程缺口。本次不把缺失的正式质量证据改成通过。

## 最小范围与任务图

- 唯一实现前沿：#60，归属 `ai-service/app/evaluate.py`、`ai-service/tests/test_evaluate.py`、`ai-service/QUALITY-PRIVACY.md` 的离线评估部分；实现者隔离worktree，提交含既有Signed-off-by，不push、不操作PR或另委派。
- Root：本规范、精确整合、统一验证、双轴审查及PR收尾；实现完成后整合和审查，不并发改实现归属文件。
- 不改HTTP schema/调用链/依赖锁、候选或模型推理、Java/前端、服务/数据库/凭据/共享预算、隐私与质量批准开关；不调用provider/BOSS/外部邮件。

## 冻结输入与输出语义

1. Gold与Prediction新增可空 `coreRequirements: list[Evidence] | None`，默认null保留历史JSONL可加载。null表示缺标注/缺冻结输出，显式空数组只表示已确认无核心阳性，二者不合并。按既有原始sourcePath/start/end/text身份计算核心选择tp/predicted/gold/precision/recall；校验核心证据来自各自requirements集合，复用跨度/重复/原文核验，预测错误不能被未知掩盖。总体与各subgroup均报告。
2. Prediction新增可空 `latencyMs` 与 `costUsd`，默认null。latencyMs为本样本完整操作的有限非负观测毫秒数，拒绝bool/字符串/NaN/Inf/负数；costUsd只接收精确非负USD十进制字符串，拒绝非有限值、bool和浮点数，不从token预留或公开单价伪造账单。不默认补零。无需新增计费、时钟采样或provider调用机制。
3. `metrics.coreSelection`返回选择计数与precision/recall；`metrics.cost`返回 `totalUsd`（十进制字符串或null）、`knownSamples`、`coverage`；`metrics.latency`返回 `p95Ms`（数值或null）、`knownSamples`、`coverage`、`percentileMethod="nearest-rank"`。原有指标键保留。
4. 成本与p95仅在该整体/子组全部样本均有相应观测且非空时给出完整聚合，否则为null，同时报告覆盖率；不能用已知子集代替完整总体。成本精确求和，包括大整数/长小数不能受默认Decimal context静默舍入；p95为排序后第ceil(0.95*N)个样本，明确单位和样本总体。
5. 复用既有已自动接受分类precision>=0.95门槛检查核心项precision，不新增其他数值准入门槛。新标注/完整观测或可比较核心阳性缺失时总体status不得pass，应为unverifiable；已观察到原有或核心precision失败时fail优先，不能用观测缺失掩盖错误。合成集、样本量、子组、来源/泄漏/未知和排序门槛保留，`qualityApproval`永远unverifiable，离线报告不授予调度/写回权限。
6. 文档说明真实观察来源、重试/失败样本纳入、完整操作的计费单位与避免跨记录重复计费；无法可靠获取或归属的观测保留null。单元模拟数据绝非真实延迟/成本或人工gold。历史文件无新字段可加载，但无法产生完整评估pass，这是明确的安全语义修复。

## 验证与交付

保留能检出原缺口的失败证据；聚焦扩展既有测试，覆盖核心误选/非法或缺失证据、旧输入、部分观测、零与未知区分、精确十进制、p95 N=1/20/21及子组、有限非负校验、CLI和原先泄漏/来源/质量守卫。Python冻结安装/lockcheck、完整既有套件、compile通过；双轴独立审查、精确head CI与PR交付。无HTTP业务变更时不为离线runner盲目重启现行应用。

## 公开标注来源的适用边界

以下是根据作者公布的任务范围作出的适用性判断，不是证明所有公开数据都不可用，也未下载或重新标注真实简历：

- [SkillSpan](https://arxiv.org/abs/2204.12811)：专家硬/软技能span，可辅助技能子任务；不能直接当本项目全部要求/福利/核心项与匹配排序gold。
- [Chinese-SkillSpan](https://arxiv.org/abs/2604.23009)：LLM初标后专家句级裁决的中文LSKT技能标签，不等于完整福利/四维匹配标注。
- [ConFit](https://arxiv.org/html/2401.16349v1)：主要匹配信号为是否接受面试，不能自动充当本项目细粒度证据及人工严格排序标签。
- [CareerCorpus](https://pmc.ncbi.nlm.nih.gov/articles/PMC13080643/)：双专家职业分类标注，不是给定JD的完整要求/福利和简历匹配排序gold。

正式人工gold、固定Jev/ZDR上游、真实质量/成本/延迟及外部SMTP仍未验收；本修复不关闭#40/#44或降低这些门槛。
