# 冻结输出转换为离线评估输入

`app.freeze_predictions` 实现 [规范 C / #69](../docs/specs/ai-closeout-20261112.md)：只读取显式指定的冻结文件，生成现有 `app.evaluate.Prediction` JSONL 和同步的 `Record` 来源清单。复用现有候选切分器及响应 schema；不连接服务、数据库、模型、账单或预算账本，不改变匹配暂停、质量批准、调度及写回门禁。

## 输入契约

两个 UTF-8 JSONL 文件按 `id` 对应，顺序可以不同。每行对应一次完整评估操作；`id` 是评估记录身份，`jobId` 是该操作中的职位身份，不能互换。禁止重复 JSON key、重复记录 id、空白身份或额外字段。

冻结请求每行包含：

| 字段 | 含义 |
| --- | --- |
| `id` | 非空评估记录 id，事前指定，不从 gold 获取 |
| `groupId` | 非空来源分组，由数据负责人确定，同一 JD/近重复来源不得人为拆组 |
| `jobs` | 非空数组，元素为 `{jobId, detailData}`；从本次实际请求原样摘录，jobId 必须为唯一字符串 |
| `resume` | 可选字符串；仅在已有合法冻结匹配请求时提供，不借此重新调用暂停的匹配接口 |

冻结响应每行包含：

| 字段 | 含义 |
| --- | --- |
| `id` | 对应冻结请求记录 id |
| `analyses` | 每个请求职位恰好一份原始 Python `AnalysisResponse`，包含完整 requirements/benefits/coreRequirements、版本、requestId、usage 等字段；响应顺序不限 |
| `match` | 有 resume 时必需，原始 Python `MatchResponse`；必须精确覆盖本次请求 jobId，不能使用外部 Java v2 的排名 envelope 替代 |
| `latencyMs` | 可选；完整操作开始至终态的实际非负有限数值毫秒，包含重试/等待；缺失为 null |
| `costUsd` | 可选；完整操作实际可审计费用的非负 USD 定点十进制字符串，包含失败尝试/重试；缺失为 null |

请求清单是本地封装，不是新的服务 API。多次内部分析与匹配合成一条评估记录时，只能放入确属同一冻结操作的响应；不能拼接不同请求、职位版本或模型的输出。输入响应采用 [现有内部协议](../docs/specs/commandcode-jev-contract-20261108.md)；本入口只支持固定 `typesafe/jev-1.14.0`、`jev-1`、`jev-policy-1` 的分析快照。文件封装的 id 和 groupId 是声明，不是来源身份认证。

本工具不采样时钟，不从 token 数/公开单价推算成本；观测无法完整归属或有重复计费风险时留 null。实际零值 `0` / `"0"` 保持零；不把未知补为零。响应中的概率、importance、rawScore、最终连续 score 和 usage 保持在原冻结文件中，本工具不改写输入。Prediction schema 不包含原始数值列，排序直接使用原始最终 score，不经整数舍入、不用 importance 或维度 rawScore 代替最终 score；相同 score 按 jobId 字符串升序确定顺序。人工平局/不可比较仍按既有 runner 的限制处理，工具不生成金标准排序。

## 转换及使用现有 runner

在 `ai-service/` 下使用仓库实际工具链：

```sh
uv sync --frozen
uv run --frozen python -m app.freeze_predictions \
  --requests /private/frozen-requests.jsonl \
  --responses /private/frozen-responses.jsonl \
  --predictions /private/new-predictions.jsonl \
  --sources /private/new-sources.jsonl
```

成功退出 0，stdout 仅报告 `status=converted` 和样本数；这不是质量验收。输出必须为不同的新文件，拒绝覆盖已有文件或输入；创建权限为 0600。全部记录验证通过才开始写出，错误退出 2，stderr 仅报告 `unverifiable`，不回显原文；写出失败时清理本次新建输出，不删除既存文件。

公开函数 `convert(request_dict, response_dict)` 返回 `(Prediction, Record)`，失败抛出 `ValueError`（含 schema 验证错误），不修改调用方输入。转换规则：

- 候选只来自 `detailData.jobDescription/jobTags/basicInfo` 和显式 resume，沿用现有切分、长窗口与重复文本多来源语义；旧 AI 输出不作事实。
- 单职位根为 `/job`，多职位根为 `/jobs/<jobId>`；jobId 和事实字段键均按 RFC 6901 转义（先 `~` → `~0`，再 `/` → `~1`）。例如 `岗位/~甲` → `/jobs/岗位~1~0甲`。来源与全部导出 Evidence 同步，原文和 Unicode code point 半开偏移不变。
- analyses 的 extraction.evidence 展平后按 `(sourcePath,start,end,text)` 去重；同文不同路径继续保留。核心 evidence 必须是 requirements 子集；每条 evidence 必须精确匹配本次来源切片。
- unknown 键为原始 `jobId:dimension`（jobId 不做 Pointer 转义），值仅取响应 dimension.status 是否为 `unknown`；`not_applicable` 为 false，不代表满足或零分。不从最终分数猜 unknown。
- 仅 `scored` 且有连续最终分数的职位进入 ranking；`insufficient_evidence` 保留实际各维状态，不生成分数或排名。拒绝矛盾的最终分数/维度状态、缺维度、身份不匹配。
- failed 项、HTTP 错误、缺响应或缺抽取结果无法在现有 Prediction 中无损表达失败：整批明确拒绝，不漏样本、不填空成功、不把失败伪装 unknown。保留原失败快照，整次评估仍不可验证。
- 无 resume 且无 match 的纯抽取快照可以转换，但 unknown 为 `{}`、ranking 为 `[]`。当前完整 runner 要求 gold 有非空 unknown 且键与预测一致，因此这种快照不足以完成整体评估；不能补造匹配维度来规避限制。

`new-sources.jsonl` 是 `{id,groupId,source}` 的 `Record` 清单。它不含人工标签、reviewStatus、reviewer 或 qualityApproval。由独立标注流程使用这份 canonical source 生成对应 gold；不要把其他路径版本的 gold 直接与新预测混用。本入口没有 gold 参数，不读取、生成或更新 gold。完整 development manifest 也须按实际来源独立维护；只包含本次留出请求的 sources 不能冒充完整 development manifest。

拿到独立人工复核、事前冻结的标签及完整开发来源后，按 [既有评估约定](QUALITY-PRIVACY.md) 运行：

```sh
uv run --frozen python -m app.evaluate \
  --gold /private/human-gold.jsonl \
  --predictions /private/new-predictions.jsonl \
  --development /private/development.jsonl \
  --min-samples "$APPROVED_MIN_SAMPLES" \
  --min-ranking-agreement "$APPROVED_MIN_RANKING_AGREEMENT"
```

阈值须由独立验收者事前明确，变量不代表已有批准。runner 的退出码仍为 pass=0、fail=1、unverifiable=2，指标语义不变，qualityApproval 始终不能由转换器推断。

## 离线证明与限制

```sh
uv run --frozen python -m pytest -q tests/test_freeze_predictions.py tests/test_evaluate.py
```

定向测试覆盖 CLI 到现有 runner、多职位、Unicode/Pointer 转义、重复来源、小数排序、显式 unknown/not_applicable、失败/缺失拒绝、缺遥测、身份错配和不覆盖文件。测试中的所有请求、响应、标签及遥测都是合成数据；即使测试以 human/reviewed 字段形状验证 runner 契约，也始终标记 synthetic，不能当作人工真值、真实费用/延迟、模型质量或解禁证据。本工具不证明冻结文件的真实性，也不替代人工来源核对与隐私授权。
