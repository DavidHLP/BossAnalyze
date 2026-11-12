# 待批准：离线人工评估标注协议（2026-11-12）

> 状态：**待批准（PENDING）**。本文是规范 D（#70）的待批准协议草案，不是人工 gold、不是质量标准、不是部署或出库授权，也不是供应商合同。所有决策值保持待批准；正文与模板里的任何数字示例都不是已批准门槛。完成本文只关闭 #70 的编写子任务：不重开简历匹配、不解除隐私 hold、不填写人类批准、不生成 `reviewStatus="reviewed"` / `annotationOrigin="human"` 的真实标签。

关联：上游规范 [ai-closeout-20261112.md](ai-closeout-20261112.md)（D；总任务 #66；工单 [#70](https://github.com/DavidHLP/BossAnalyze/issues/70)）；现行质量与隐私边界 [../../ai-service/QUALITY-PRIVACY.md](../../ai-service/QUALITY-PRIVACY.md)；辅助标注口径 [../operations/ai-assisted-annotation-rubric-20261110.md](../operations/ai-assisted-annotation-rubric-20261110.md)；隐私授权协议 [resume-privacy-authorization-20261112.md](resume-privacy-authorization-20261112.md)；恢复 hold [../operations/resume-matching-privacy-hold.md](../operations/resume-matching-privacy-hold.md)。

## 1. 效力、所有权与不变量

- 归属：#70 的 `docs/specs/` 新规范与必要文档链接；`ai-service/app/**` 生产代码只读。
- 本文只提供字段说明与空白模板，供独立人工填写真实 gold。AI 不生成、不代填、不冒充 reviewer，不把辅助材料提升为人工标签。
- 现有 `ai-service/app/evaluate.py` 的硬编码阈值（候选召回 ≥0.98、已接受 precision ≥0.95、证据一致性 =1、unknownAgreement =1、核心选择 precision ≥0.95）是**既有实现行为**，本文只引用、不降低，也不把它们当成本文新批准的门槛。
- 样本量、排序一致性门槛、分类/核心 recall 门槛、核心项定义、跨度粒度、子组清单与划分比例在本文一律标 `PENDING`。
- 本文完成不改变 `qualityApproval`（永远 `unverifiable`）、不授权调度/写回、不改变 ZDR/预算策略、不解除 [resume-matching-privacy-hold.md](../operations/resume-matching-privacy-hold.md)。

## 2. 与现有 runner 的冻结兼容边界

`ai-service/app/evaluate.py`（`Gold` / `Prediction` / `Record`）与 `ai-service/app/schemas.py`（`Evidence`）是兼容基准。本协议不得要求 runner 接受新字段。

- 三个模型都 `extra="forbid"`、`strict=True`；**本协议引入的全部待批准元数据都不能写进 runner JSONL**：优先项、职责、否定与排除、unknown 理由、平局/不可比较理由、仲裁、reviewer 身份证据、子组定义、划分清单，一律放旁路文件（§10、§11）。
- 每行一个 JSON 对象；`jev.unique_object` 拒绝重复 JSON key；`id` 非空且唯一；`prediction.id` 集合必须与 gold 完全一致，失败样本也不得静默丢弃。
- `Evidence`：`sourcePath` 以 `/` 开头；`start`/`end` 非负整数；`end - start == len(text)`，坐标是 Unicode code point（§5）。
- 一条正式留出 gold 记录在 `evaluate()` 中必须同时满足：`split="test"`、`reviewStatus="reviewed"`、`reviewer` 非空、`annotationOrigin="human"`、`subgroup` 非空、`unknown` 非空、`dataOrigin ∈ {real_public, real_authorized, synthetic}`。合成集永不产生总体 `pass`。
- `model` 固定 `typesafe/jev-1.14.0`；gold 与预测的 `unknown` 键集合必须一致；`ranking` ID 非空白且不重复，预测不得出现 gold 之外的 ID。
- 核心证据必须是各自 `requirements` 的证据子集。
- 退出码 `pass=0 / fail=1 / unverifiable=2`；`qualityApproval` 永远 `unverifiable`。

### 2.1 字段说明

| 字段 | schema | 类型/取值 | 说明 | 状态 |
| --- | --- | --- | --- | --- |
| `id` | Gold/Prediction/Record | 非空唯一字符串 | 记录身份；预测必须覆盖全部 gold id | 冻结 |
| `groupId` | Gold/Record | 非空字符串 | 同一 JD/近重复职位分组；跨开发/校准/测试一致 | 冻结 |
| `source` | Gold/Record | `dict[str,str]` JSON Pointer → 原始字符串叶子 | canonical 路径见 §10.4；禁止放旧 AI 输出 | 冻结 |
| `split` | Gold | 正式留出必为 `test` | 划分见 §9 | 取值冻结；划分比例 PENDING |
| `reviewStatus` | Gold | 正式 gold 必为 `reviewed` | 模板占位不得当作已复核 | 冻结 |
| `reviewer` | Gold | 非空人类身份 | 只能由真人填写；可审计声明，runner 不验证身份 | PENDING（身份与留证方式） |
| `annotationOrigin` | Gold | 正式 gold 必为 `human` | `ai_assisted` 不得进入正式 gold | 冻结 |
| `dataOrigin` | Gold | `real_public` / `real_authorized` / `synthetic` | 允许使用范围 PENDING | PENDING |
| `subgroup` | Gold | 非空字符串 | 子组维度与清单见 §8 | PENDING（维度/清单） |
| `requirements` | Gold/Prediction | `list[Evidence]` | 集合口径见 §3 | PENDING（优先项是否计入） |
| `benefits` | Gold/Prediction | `list[Evidence]` | 只收明确正向承诺 | 冻结（口径见 §3） |
| `coreRequirements` | Gold/Prediction | `list[Evidence] \| null` | `null`=缺标注/缺输出；`[]`=已确认无核心阳性 | PENDING（核心定义） |
| `unknown` | Gold/Prediction | 非空 `dict[str,bool]` | 键 `jobId:dimension`；见 §6 | PENDING（映射） |
| `ranking` | Gold/Prediction | `list[str]` | 同一请求内严格序；无平局表示；见 §7 | PENDING（映射） |
| `candidates` | Prediction | `list[Evidence]` | 来自冻结实际输出，展平去重 | 冻结 |
| `latencyMs` | Prediction | 有限非负数值或 `null` | 完整操作观测；真实 0 与未知 `null` 分开 | 冻结 |
| `costUsd` | Prediction | 非负 USD 定点十进制字符串或 `null` | 来自可审计计费；不从 token 估算 | 冻结 |

## 3. 集合口径：要求 / 职责 / 优先项 / 福利 / 否定

- **要求（requirements）**：可核验的资格、技能、经验或学历条件。
- **职责（responsibility）**：如"维护服务""电话销售"等动作，单独记入职责旁路日志（§10.2），不由动作名称推导任职资格。
- **优先项（preferred）**：可纳入 requirements，但必须保留"优先 / Nice to have"等限定语，并在旁路附录标 `preferred` 与理由；不得提升成强制项。优先项是否计入 precision/recall 的**集合边界**由验收者事前冻结（`PENDING`）。现有 `Evidence` schema 没有 modality，限定日志不得塞进 runner 额外字段。
- **福利（benefits）**：只收明确正向承诺。"面议""未提供""none specified"不产生正福利；"试用期后可申请住宿，具体以制度为准"只记条件机会并保留条件全文，当前不进入 benefits 正例，也不改写成确定提供。
- **否定（negation）**：保留完整作用域。"不要求销售经验""学历不限"不是销售/学历正要求；"不提供住宿"不是住宿福利。明确无某经历可作反证，但只限相同技能、领域与时间范围。
- 空列表只表示"人工确认无阳性"，不表示 pending；排除项不因不进入正例而静默丢弃（§10.2）。

## 4. 核心项（coreRequirements）

- 现有 `coreRequirements` 是各自 `requirements` 的证据子集，沿用原始 `sourcePath/start/end/text` 身份、跨度、去重与原文核验。
- "全部显式强制条件"和"仅关键技术/经验、排除学历"是两份预标注的不同自定规则，**均不是已批准核心定义**；业务重要性不等同强制资格。不得多数票决定学历、年限是否核心，不得自赋权重或新增召回门槛。
- 正式核心定义与标签未冻结前，`coreRequirements` 保持 `null`，不能用 `[]` 代表"尚未决定"。
- 非空核心证据必须是 requirements 子集；优先项不得静默提升为核心。
- runner 输出核心选择的 tp/predicted/gold/precision/recall；任一侧缺失时完整 precision/recall 为 `null`。核心 precision 复用既有 ≥0.95 门槛；**核心 recall 的数值门槛 PENDING**（当前 runner 只要求非 `null`）。

## 5. 跨度粒度

- 坐标是原始未规范化字符串的 **Unicode code point**，从 0 起，半开 `[start,end)`；必须满足 `text == source[start:end]`。禁止 UTF-8 字节坐标与 JavaScript UTF-16 code unit 坐标。
- emoji 与组合字符按码点计：🌱 是一个码点，组合字符/emoji 序列可能有多个码点，不按视觉字符数。
- 默认取"包含限定语的最小自包含资格/福利子句"；同一联合条件保留在同一子句；不为提高命中率任意拆分。保留内部空格、标点、emoji；不先 NFKC、去标点或翻译再算跨度。
- 正式比较前双方须冻结粒度规则（`PENDING`：最小子句 vs 其他规则）。不同合理切法仍影响精确集合指标；原文一致性 =1 只说明切片一致，不证明语义正确。
- 来源采用 canonical 路径：单职位 `/job/jobDescription/...`、多职位 `/jobs/<jobId>/jobDescription/...`、简历单列 `/resume/...`。

## 6. unknown / not_applicable

- 现有 runner 的 `unknown` 是"维度键集合 → bool"，键形如 `jobId:dimension`，四个维度为 `skills`、`relevant_experience`、`experience`、`education`。runner 只核验非空键集合与布尔一致性，**不验证维度集合、理由或 not_applicable**。
- unknown 判定：JD 比较标准或简历事实不足时为 `true`；明确冲突可判 `false`，但不表示满分，也不直接规定零分。"No React experience stated"是未陈述，不是"明确没有 React 经历"；未提及不转成否定，证据缺失不转成零分。
- not_applicable 在线上是 `Dimension.status` 的独立取值，现有 runner 的布尔无法无损表达；辅助材料单独保存状态与理由，**正式映射 PENDING**，未冻结前不得压成可评分或满分。
- 每个 job 分别记录四个维度：一个已知技能不能填满其他技能未知；TypeScript 年限不能挪作 React 年限；泛化参与项目不自动证明特定领域经验。
- 每条 unknown 的理由、已知证据与缺失标准放旁路日志（§10.3）；分数未被实际可靠判断时为 `null`。

## 7. 平局与不可比较

- `ranking` 属于同一冻结请求；一条 gold 记录可持有多个 job 的完整来源与组级排序，记录 `id` 与 `jobId` 是两种身份。
- 只有比较对象、简历、维度定义、权重与可评分规则一致且证据足够时才给严格顺序。backend 场景的显式 Kubernetes 反证不能套到 unicode 场景的 React 未陈述上。
- 未冻结权重、证据不足、平局或仅有局部优势时保留 `null` 与原因，**不强制全排序**。现有 runner 无平局/部分序表示：正式适配时把不可比较映射为 `[]`，并另存原因与成员（§10.3）。
- 空阳性和无 pair 仍报告实际分母与 `null`/`unverifiable`，不当满分，不删除稀疏子组美化结果；已测失败继续优先 `fail`。
- 排序成员须有非空稳定 `jobId`，由复核者对照本次请求确认归属；runner 当前不提供职位身份注册表，不声称自动证明同请求归属。

## 8. 子组

- runner 要求 `subgroup` 非空，并按 `subgroup` 分别出指标；`min-samples` 同时应用整体与每个子组。
- 子组维度与冻结清单 `PENDING`。候选维度（须验收者选择并冻结后再用）：JD 来源/渠道、语言/地区、职位族、资历层级、dataOrigin。不得把子组当作事后挑择通过率的手段。
- 稀疏子组保留实际分母与 `null`，不合并、不删除；合成子组永不产生总体 `pass`。

## 9. 开发 / 留出划分（按 JD 来源分组）

- 划分在 **`groupId` 粒度**执行：同一 JD 或近重复职位必须整体落在同一侧，开发、提示调优、阈值校准与测试之间共享同一 `groupId`。
- `split` 取值：正式留出 gold 必为 `test`；训练/校准来源进入 development manifest（`Record`：`id, groupId, source`，必传；确实没有开发集时给空文件并由审核者确认）。
- runner 检测测试集与 development 的 id/groupId 冲突、规范化内容相同、相似度 ≥0.9 的近重复，并在 job 根内做职位级比较（阈值 0.9）。检测不完备，人工分组与完整 manifest 仍是前提；不得只提交部分开发来源。
- 同一职位被拆成不同结构、额外元数据稀释内容或任意路径不暴露职位身份时，自动检测不完备；审核者须核验原始来源、完整开发 manifest 与稳定 `groupId`，必要时离线转换为 canonical 路径后重评。
- 划分比例、分配权与冻结时点 `PENDING`；划分必须"看测试结果前"冻结。

## 10. 旁路记录（非 runner 字段）

下列记录独立于 runner 输入，建议每行一个 JSON 对象；字段为**待批准建议**，可由验收者调整，但不得移入 runner JSONL。

### 10.1 优先项日志
`{"recordId","jobId","sourcePath","start","end","text","qualifier","modality":"preferred","annotator","note"}`

### 10.2 排除日志
`{"recordId","jobId","sourcePath","start","end","text","excludedType":"negation|responsibility|conditional|exemption|dispute|other","reason","annotator","note"}`

### 10.3 分歧与不可比较日志
- unknown 理由：`{"recordId","jobId","dimension","value":true|false|null,"reason","knownEvidence":[],"missingStandard","annotator"}`
- 平局/不可比较：`{"recordId","requestId","members":[],"kind":"tie|incomparable|partial","reason","annotator"}`

### 10.4 来源身份与分组
- canonical 路径：单职位 `/job/jobDescription/...`；多职位 `/jobs/<jobId>/jobDescription/...`，`<jobId>` 按 RFC 6901 转义；`<jobId>` 不得含未转义路径分隔符。
- 分组登记：`{"groupId","canonicalJdIdentity","dataOrigin","nearDuplicateOf":[],"assignedBy","status":"PENDING"}`
- 划分清单：`{"groupId","split":"development|calibration|test","assignedBy","frozenAt","status":"PENDING"}`
- 历史 `/jobDescription/...`、字符串根与旧路径继续可加载并参与全记录/`groupId` 检查，但不保证职位级检测。

### 10.5 仲裁记录
`{"recordId","issue","positions":[],"decision","arbiter","decidedAt","scope","supersedes","evidence"}`

- 仲裁者身份、升级路径与排除记录保留期 `PENDING`；仲裁决定不得由 AI 代填。独立复核须另留真实身份、版本与仲裁记录，不能把 AI 辅助材料计为独立人工复核。

## 11. 空白模板（占位符，不是 gold）

> 下列尖括号占位符保持原样即**无法通过 runner 校验**，这是有意的：模板不是 gold，也不是 reviewed/human 记录。真实值只能由人工填写。

### 11.1 留出 gold（每行一个对象）
```json
{"id":"<DOC-ID>","groupId":"<GROUP-ID>","source":{"/job/jobDescription/requirements":"<原始未规范化字符串>"},"split":"<test>","reviewStatus":"<reviewed-仅正式人工>","reviewer":"<真实人工身份-禁止AI填写>","annotationOrigin":"<human-仅正式人工>","dataOrigin":"<real_public|real_authorized|synthetic>","subgroup":"<已冻结子组>","requirements":[{"sourcePath":"/job/jobDescription/requirements","start":0,"end":0,"text":"<必须等于 source[start:end]>"}],"benefits":[],"coreRequirements":null,"unknown":{"<jobId>:skills":false},"ranking":[]}
```

### 11.2 冻结预测（每行一个对象）
```json
{"id":"<DOC-ID>","model":"typesafe/jev-1.14.0","candidates":[],"requirements":[],"benefits":[],"coreRequirements":null,"unknown":{"<jobId>:skills":false},"ranking":[],"latencyMs":null,"costUsd":null}
```

### 11.3 development manifest（每行一个对象）
```json
{"id":"<DEV-ID>","groupId":"<GROUP-ID>","source":{"/job/jobDescription/requirements":"<原始未规范化字符串>"}}
```

## 12. 待批准决定清单（人类）

每项都需独立验收者/数据负责人书面决定；`PENDING` 表示当前无已批准值，模板与示例数字不构成默认。

| ID | 决定 | 当前状态 | 影响 | 决定人 |
| --- | --- | --- | --- | --- |
| D-H1 | 样本量 `min-samples`（整体与每子组） | PENDING | 决定 `eligible`，进而 `pass`/`unverifiable` | 独立验收者 |
| D-H2 | 排序一致性门槛 `min-ranking-agreement` | PENDING（CLI 必传，无默认） | `rankingAgreement` 通过与否 | 独立验收者 |
| D-H3 | 分类 recall 数值门槛 | PENDING（runner 当前只检查 precision） | requirements/benefits recall | 独立验收者 |
| D-H4 | 核心 recall 数值门槛 | PENDING（runner 当前只要求非 null） | coreSelection recall | 独立验收者 |
| D-H5 | 核心项定义（含学历/年限是否核心） | PENDING | `coreRequirements` 集合 | 业务 + 验收者 |
| D-H6 | 优先项是否计入 precision/recall | PENDING | requirements 集合边界 | 业务 + 验收者 |
| D-H7 | 跨度粒度冻结规则 | PENDING | 精确集合指标 | 验收者 |
| D-H8 | 子组维度与冻结清单 | PENDING | 分组指标与每子组样本量 | 验收者 |
| D-H9 | 开发/校准/留出划分比例与分配权 | PENDING | 泄漏与泛化判断 | 数据负责人 |
| D-H10 | unknown/not_applicable → runner bool 的映射 | PENDING | `unknown` 键集合与一致性 | 验收者 |
| D-H11 | 平局/不可比较 → `ranking=[]` 的映射 | PENDING | 排序 pair agreement | 验收者 |
| D-H12 | 仲裁者身份、升级路径与留证方式 | PENDING | 分歧裁决可信度 | 数据负责人 |
| D-H13 | `dataOrigin` 各类允许使用范围 | PENDING | 可评估数据范围 | 业务 + 法务/合规 |
| D-H14 | 排除日志保留期与审计访问 | PENDING | 可追溯性 | 数据负责人 |
| D-H15 | 既有硬编码门槛（0.98/0.95/1.0）是否需正式复核 | PENDING | 是否沿用现有实现 | 独立验收者 |

## 13. 审阅清单

- [ ] 全部 15 项决定已由具名真人填写，未留 `PENDING` 的项都有书面依据。
- [ ] 无 AI 生成/代填的 `reviewer`、`reviewStatus="reviewed"` 或 `annotationOrigin="human"`。
- [ ] 每条 Evidence 满足 `text == source[start:end]`，坐标为码点。
- [ ] `groupId` 跨开发/校准/测试一致；无近重复跨侧。
- [ ] 未知维度按 `jobId:dimension` 分离；未陈述未转否定。
- [ ] 平局/不可比较映射为 `[]` 且旁路留存原因。
- [ ] 旁路元数据未写入 runner JSONL 额外字段。
- [ ] 本协议未被当作质量批准、供应商合同或解 hold 依据。

## 14. 明确不做

- 不生成人类或 `reviewed` gold；不填写人类批准；不设定样本量或准入门槛。
- 不修改 `ai-service/app/**` 生产代码，不改 runner 指标语义，不为新元数据放宽 `extra="forbid"`。
- 不解除简历匹配 hold，不改变 ZDR/预算/调度/写回策略，不触碰真实服务或数据。
