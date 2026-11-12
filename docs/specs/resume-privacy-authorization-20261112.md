# 待批准：简历数据最小化与出站授权规范（2026-11-12）

> 状态：**待批准（PENDING）**。本文是规范 D（#70）的待批准协议草案，不是供应商合同、不是数据处理协议、不是质量批准，也不是解除简历匹配 hold 的依据。所有决策值保持待批准；正文与模板里的任何字段、期限、供应商或示例都不是已批准值。完成本文只关闭 #70 的编写子任务：不重开匹配、不解除 hold、不填写人类或供应商批准、不触碰真实服务或数据。

关联：上游规范 [ai-closeout-20261112.md](ai-closeout-20261112.md)（D；总任务 #66；工单 [#70](https://github.com/DavidHLP/BossAnalyze/issues/70)）；现行质量与隐私边界 [../../ai-service/QUALITY-PRIVACY.md](../../ai-service/QUALITY-PRIVACY.md)；恢复 hold [../operations/resume-matching-privacy-hold.md](../operations/resume-matching-privacy-hold.md)；标注协议 [ai-human-annotation-protocol-20261112.md](ai-human-annotation-protocol-20261112.md)；辅助标注口径 [../operations/ai-assisted-annotation-rubric-20261110.md](../operations/ai-assisted-annotation-rubric-20261110.md)；普通路由策略 [../operations/optional-zdr-policy-20261110.md](../operations/optional-zdr-policy-20261110.md)。

## 1. 效力、所有权与不变量

- 归属：#70 的 `docs/specs/` 新规范与必要文档链接；`ai-service/**`、`backend/**` 生产代码只读。
- 本文不解除 [resume-matching-privacy-hold.md](../operations/resume-matching-privacy-hold.md)：两条 Java 公开匹配路由（含遗留路由）、Java 直接匹配入口、Python HTTP 匹配端点与 Python pipeline 仍须在认证后、消费简历正文/请求正文前失败关闭为 503，匿名仍为 401。
- 重新开放需要：经审阅的数据最小化与显式出站授权设计、覆盖全部简历表示与入口的独立对抗测试、**已验证的供应商合同**、以及单独的真实服务验收。CI 变绿或启用 ZDR 都不足以解除。
- 现有正则脱敏**不构成安全边界**；白名单决策不得以"已脱敏"为由放行。
- 本文不改变 ZDR/预算/模型/调度/写回策略，不授权发送任何真实简历，不触碰真实服务、账号、数据库或凭据。

## 2. 冻结的现行行为（证据基线）

| 入口 | 现行行为 | 现有证据 |
| --- | --- | --- |
| Java `POST /api/v1/ai/get-user-similarity`（遗留） | 匿名 401 优先；认证请求在读取正文前 503 `AI_RESUME_MATCHING_DISABLED` | `backend/crawler/src/test/java/com/david/hlp/crawler/ai/service/ResumePrivacyTest.java` |
| Java `POST /api/v2/ai/matches` | 同上 | 同上 |
| Java 直接 service/analyzer 匹配入口 | 在数据库、传输与部分成功前停止 | 同上（`analyzerStopsBeforeDatabaseOrTransportAndDoesNotReturnPartialSuccess`） |
| Python `POST /internal/v1/match-batch`（容忍尾部 `/`） | 未消费请求正文即 503 `resume_matching_disabled`，不回显内容 | `ai-service/tests/test_resume_privacy.py`（`test_http_rejects_before_consuming_body_and_never_echoes_it`） |
| Python 直接 `Pipeline.match` | 在 provider 传输与预算文件创建前 503 | 同上（`test_direct_pipeline_rejects_before_any_provider_or_budget_use`） |
| readiness / liveness | `resumeMatching.enabled=false`；liveness 200；普通职位分析不受影响 | 同上，及 `ai-service/QUALITY-PRIVACY.md` |
| 离线 `app.evaluate` | 只读本地文件，不调 provider、不读密钥/预算、不写质量开关 | `ai-service/app/evaluate.py` |

上述测试使用合成文本，仅证明守卫生效，不证明端到端匹配、真实人工 gold 或正式质量。

## 3. 简历字段白名单与转换（全部待批准）

- 目标：在**任何**出站前，只保留完成已批准用途所必需的最小字段，其余字段丢弃或不可逆转换。每一行的允许性与转换规则都必须由人类决定并留证；下表是待审阅结构，不是已批准白名单。
- 路径沿用 canonical pointer `/resume/...`（简历与职位身份分开；见 `ai-service/QUALITY-PRIVACY.md` 的职位来源路径与兼容边界）。
- 转换候选：`drop`（不出站）、`redact`（不可逆移除）、`hash`（仅用于去重且不可反推）、`truncate`（限长）、`normalize`（格式统一，**不**用于隐藏语义内容）、`allow`（原文出站）。除 `drop`/`redact`/`hash` 外，其余均不视为最小化。
- 正则脱敏不算安全边界（§1）；若采用，必须另有结构化白名单作为主控制。

| 类别 | 候选字段/路径 | 允许性 | 转换 | 用途 | 状态 |
| --- | --- | --- | --- | --- | --- |
| 身份标识 | `/resume/name`、`/resume/idNumber`、`/resume/nationality`、`/resume/photo` | 待批准 | 待批准 | — | PENDING |
| 联系方式 | `/resume/phone`、`/resume/email`、`/resume/im`、`/resume/address` | 待批准 | 待批准 | — | PENDING |
| 敏感/受保护属性 | `/resume/birthDate`、`/resume/gender`、`/resume/maritalStatus`、`/resume/health`、`/resume/ethnicity`、`/resume/political` | 待批准 | 待批准 | — | PENDING |
| 学历 | `/resume/education/*` | 待批准 | 待批准 | — | PENDING |
| 工作经历 | `/resume/experience/*`（单位、职位、时间、描述） | 待批准 | 待批准 | — | PENDING |
| 技能与证书 | `/resume/skills`、`/resume/certifications`、`/resume/languages` | 待批准 | 待批准 | — | PENDING |
| 项目 | `/resume/projects/*` | 待批准 | 待批准 | — | PENDING |
| 自由文本 | `/resume/summary`、`/resume/other` | 待批准 | 待批准 | — | PENDING |
| 文件元数据 | 文件名、EXIF、附件、内嵌链接、简历文件 ID | 待批准 | 待批准 | — | PENDING |
| 职位事实中的简历 | 被伪装成 `jobTags`/职位描述的简历文本 | 一律拒绝（待批准正式措辞） | `drop` + 拒绝 | — | PENDING |

- 白名单粒度、是否允许保留"已脱敏的自由文本"、以及 `/resume/*` 子路径登记方式 PENDING。
- 每个字段必须记录：用途 ID、保留期、转换可逆性、责任人与批准时间；空缺即不得出站。

## 4. 用途登记（目的限制）

只有登记且被批准的用途才允许处理简历；未登记用途一律拒绝，不用"技术上可达"推导授权。

建议字段（待批准）：`{"purposeId","description","allowedFields":[],"allowedProcessing":[],"prohibited":[],"retentionRef","approvedBy","status":"PENDING"}`

候选用途（仅占位，待业务与法务逐项批准）：离线人工评估标注准备、离线评估运行、质量缺陷诊断。**禁止**：训练/微调、广告画像、对外共享、二次销售、与登记用途无关的分析。

## 5. 供应商与处理方登记（待批准，不构成合同）

本文不创造任何供应商授权。每个供应商/上游/子处理方必须逐项批准并附合同证据；缺任一项即不得出站。

建议字段（待批准）：`{"vendorId","legalName","service","model","dataCategories":[],"entryPoints":[],"retention":{"content","metadata","basis"},"training":{"zdrOrNoTraining","contractRef"},"crossBorder":[],"subprocessors":[],"breachNotice","deletionSupport","auditRights","verifiedBy","verifiedAt","contractRef","status":"PENDING"}`

- `contractRef`、`verifiedBy`、`verifiedAt` 留空或 `PENDING` 时，该供应商不得授权出站；本文不把任何承诺当合同。
- 已知供应商事实（如 CommandCode/Jev 的隐私政策、ZDR 文档、安全说明）只是当前参考材料，仍须由人类核验并落合同证据，不能由 AI 认证。
- 跨境与上游子处理方、计费与成本归属分别登记（成本归属见标注协议与 `QUALITY-PRIVACY.md`）。

## 6. 保留、删除与撤回（期限全部待批准）

- **保留**：`{"dataCategory","purposeId","location","retentionPeriod","deletionTrigger","deletionMethod","evidence","status":"PENDING"}`。正文与元数据分开；普通路由的供应商保留不等于零保留，也不等于本机已删除。
- **删除**：删除须有可核验证据（方法、时间、范围、执行人），不能只以"已请求"计为完成；跨缓存/备份/账本的级联 PENDING。
- **撤回**：`{"subjectRequestChannel","scope","deadline","cascade":[],"evidence","owner","status":"PENDING"}`。撤回须覆盖已出站副本与供应商侧删除能力；时限与流程 PENDING。
- 现有账本（预算）与计费元数据的保留策略不变；本文不授权重置或删除既有账本。

## 7. 入口到门禁矩阵

门禁顺序不变量：**认证优先**（匿名 401）→ **hold 503 在消费简历正文/请求正文之前** → 字段白名单 → 用途检查 → 供应商授权 → 预算 → 日志去敏 → 无写回。任一后续门禁不得把前述拒绝降级为放行。

| 入口 | 认证 | hold | 白名单 | 用途 | 供应商 | 期望 | 现有证据 | 待批准缺口 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Java 公开匹配 v1（遗留） | 401 优先 | 读正文前 503 | 不适用（hold 期间不达） | 不适用 | 不适用 | 匿名 401 / 认证 503，正文未读 | ResumePrivacyTest | 白名单与用途实现未批准 |
| Java 公开匹配 v2 | 同上 | 同上 | 不适用 | 不适用 | 不适用 | 同上 | ResumePrivacyTest | 同上 |
| Java 直接匹配入口 | 由调用方认证 | 服务层 503 | 不适用 | 不适用 | 不适用 | 不触达 DB/传输 | ResumePrivacyTest | 同上 |
| Python HTTP 匹配 | 401 优先 | 未消费正文即 503 | 不适用 | 不适用 | 不适用 | 503，无回显 | test_resume_privacy.py | 同上 |
| Python 直接 pipeline | 由调用方持有 token | 预算/provider 前 503 | 不适用 | 不适用 | 不适用 | 无预算文件、无网络 | test_resume_privacy.py | 同上 |
| 离线 evaluate | 本机文件权限 | 不适用 | 不适用 | 本地只读评估 | 不调 provider | 无网络、无密钥 | evaluate.py | 真实 gold 使用范围 PENDING |
| 调度/写回入口 | 未启用 | 未启用 | 未启用 | 未启用 | 未启用 | 保持关闭 | QUALITY-PRIVACY.md | 全部 PENDING |

## 8. 对抗测试矩阵

每一行都必须验证"失败关闭"：拒绝发生在消费正文/简历、职位查询、预算创建、provider 传输、日志回显与写回之前。现有测试是基线；新增表示需独立验收者批准。

| 对抗/表示 | 入口 | 期望 | 现有证据 | 缺口 |
| --- | --- | --- | --- | --- |
| 内联简历含 PII（姓名/地址/电话） | Java 双路由、Python HTTP/pipeline | 503，不回显、不落日志 | ResumePrivacyTest；test_resume_privacy.py | 供应商侧表示 |
| 空简历字符串 | Python pipeline | 503，无预算文件 | test_resume_privacy.py | Java 侧空简历 |
| 超长 Unicode（含 emoji，长码点） | Python pipeline/HTTP | 503，坐标不溢出 | test_resume_privacy.py（`合成😀`×3000） | 多字节/组合字符边界 |
| 存储简历 ID（不取正文） | Java 双路由/直接入口 | 取回前 503 | ResumePrivacyTest（inline/stored/empty/unreadable） | 存储后端变体 |
| 不可读/流式请求正文 | Java、Python HTTP | 未消费正文即 503 | ResumePrivacyTest；test_resume_privacy.py（unread body） | 分块/压缩/multipart |
| 畸形 JSON 正文 | Java 双路由 | 503 先于解析错误 | ResumePrivacyTest（malformed body） | Python 侧畸形正文 |
| 无正文/仅请求头 | Java、Python | 503 | optional-zdr 报告（仅请求头） | 统一断言 |
| 匿名 / 已撤销身份 | 双栈 | 401 优先，且不读正文 | ResumePrivacyTest；Python 匿名 401 | 撤销身份在 Python |
| 空职位批 | Python pipeline/HTTP | 503，无职位查询 | test_resume_privacy.py（jobs=[]） | Java 空批 |
| 简历伪装成职位事实（jobTags/职位描述） | 普通分析入口 | 按业务边界拒绝 | QUALITY-PRIVACY 说明 | 自动检测不完备 |
| 尾部斜杠/路径别名 | Python HTTP | 503 | test_resume_privacy.py（suffix `""`/`"/"`） | 其他别名 |
| 嵌套/别名/重复简历字段 | 双栈 | 拒绝，不取第一段 | — | 需新增（待批准） |
| gzip/chunked/multipart 正文 | 双栈 | 拒绝且不解压后消费 | — | 需新增（待批准） |
| 缓存/重放请求 | 双栈 | 仍 503，不命中旧结果 | — | 需新增（待批准） |
| 日志/异常/错误体回显 | 双栈 | 无原文、无 PII | test_resume_privacy.py（caplog、response.text） | Java 日志断言 |
| provider 传输可达性 | Python | 传输函数被调用即测试失败 | test_resume_privacy.py（MockTransport forbidden） | 真实网络隔离验证 |

## 9. 回滚条件（触发与责任人待批准）

回滚 = 重新收紧到"一律 503"并把相关出站授权置为撤销；不是删除测试或降低断言。

| 触发 | 检出信号 | 回滚动作 | 责任人 | 状态 |
| --- | --- | --- | --- | --- |
| 任一 fail-closed 测试失败 | 双栈测试红 | 重新启用 hold，停止出站 | PENDING | PENDING |
| 白名单/转换回归 | 禁止字段出站证据 | 停止出站，撤销用途授权 | PENDING | PENDING |
| 供应商/上游或保留政策变化 | 合同或政策变更 | 撤销该供应商授权并重跑对抗矩阵 | PENDING | PENDING |
| 模型/路由/版本变化 | 冻结模型或路由变更 | 重新冻结预测与门槛，重跑矩阵 | PENDING | PENDING |
| 疑似泄露/事件 | 事件报告或审计发现 | 停止出站、撤销授权、按流程通知与清理 | PENDING | PENDING |
| 授权过期/撤回 | 授权记录过期或被撤回 | 立即停止对应处理 | PENDING | PENDING |

回滚执行后必须留证（时间、范围、执行人、验证结果），且不得以"CI 绿"为由重新放行。

## 10. 空白模板（占位符，全部未批准）

### 10.1 用途登记
```json
{"purposeId":"<PURPOSE-ID>","description":"<用途描述>","allowedFields":["<待批准字段路径>"],"allowedProcessing":["<待批准处理>"],"prohibited":["<禁止处理>"],"retentionRef":"<保留记录ID>","approvedBy":"<真实人工-禁止AI填写>","status":"PENDING"}
```

### 10.2 供应商登记
```json
{"vendorId":"<VENDOR-ID>","legalName":"<法定名称>","service":"<服务>","model":"<固定模型或版本>","dataCategories":["<数据类别>"],"entryPoints":["<入口>"],"retention":{"content":"<待批准>","metadata":"<待批准>","basis":"<依据>"},"training":{"zdrOrNoTraining":"<待批准>","contractRef":"<合同引用或空>"},"crossBorder":[],"subprocessors":[],"breachNotice":"<待批准>","deletionSupport":"<待批准>","auditRights":"<待批准>","verifiedBy":"<真实人工-禁止AI填写>","verifiedAt":"<日期>","contractRef":"<合同引用或空>","status":"PENDING"}
```

### 10.3 出站授权记录
```json
{"authorizationId":"<AUTH-ID>","scope":"<入口/用途/字段>","purposeId":"<用途ID>","vendorId":"<供应商ID>","grantedBy":"<真实人工-禁止AI填写>","grantedAt":"<日期>","evidence":"<证据位置>","expiresAt":"<日期>","revocation":"<撤销条件>","status":"PENDING"}
```

### 10.4 保留与删除
```json
{"dataCategory":"<类别>","purposeId":"<用途ID>","location":"<存储位置>","retentionPeriod":"<待批准>","deletionTrigger":"<触发>","deletionMethod":"<方法>","evidence":"<证据>","status":"PENDING"}
```

### 10.5 撤回流程
```json
{"subjectRequestChannel":"<渠道>","scope":"<范围>","deadline":"<待批准>","cascade":["<级联位置>"],"evidence":"<证据>","owner":"<真实人工-禁止AI填写>","status":"PENDING"}
```

### 10.6 回滚记录
```json
{"trigger":"<触发>","detectedAt":"<时间>","detectedBy":"<真实人工-禁止AI填写>","action":"<动作>","scope":"<范围>","verification":"<验证结果>","status":"PENDING"}
```

## 11. 待批准决定清单（人类与供应商逐项）

人类决定：

| ID | 决定 | 当前状态 | 决定人 |
| --- | --- | --- | --- |
| D-P1 | 白名单最终字段集合与每字段转换 | PENDING | 业务 + 法务/合规 |
| D-P2 | 用途清单与允许处理 | PENDING | 业务 + 法务/合规 |
| D-P3 | 逐入口/逐用途出站授权 | PENDING | 数据负责人 |
| D-P4 | 保留期与删除方法及证据要求 | PENDING | 法务/合规 + 数据负责人 |
| D-P5 | 撤回渠道、时限与流程 | PENDING | 法务/合规 |
| D-P6 | 回滚触发与具名责任人 | PENDING | 数据负责人 |
| D-P7 | 是否允许 `real_authorized` 数据及其范围 | PENDING | 法务/合规 |
| D-P8 | 对抗矩阵验收者与通过证据标准 | PENDING | 独立验收者 |
| D-P9 | 日志/审计保留与访问 | PENDING | 数据负责人 |

供应商/合同决定：

| ID | 决定 | 当前状态 | 决定人 |
| --- | --- | --- | --- |
| D-V1 | 供应商、上游与子处理方清单 | PENDING | 采购/法务 |
| D-V2 | 合同/数据处理协议存在与条款 | PENDING | 法务 |
| D-V3 | 正文与元数据保留期限 | PENDING | 法务 + 供应商 |
| D-V4 | 训练使用与 ZDR/no-training 状态 | PENDING | 法务 + 供应商 |
| D-V5 | 跨境与子处理方 | PENDING | 法务 |
| D-V6 | 泄露通知与审计权 | PENDING | 法务 |
| D-V7 | 删除/撤回支持 | PENDING | 法务 + 供应商 |
| D-V8 | 计费与成本归属口径 | PENDING | 采购 + 财务 |

## 12. 明确不做

- 不解除 hold，不放行任何真实简历，不启用调度或写回。
- 不认证供应商、不代替合同、不填写人类或供应商批准。
- 不修改生产代码、测试断言、ZDR/预算/模型配置。
- 不设置保留期、撤回时限或样本量等具体数字门槛。
- 不把本文当作质量批准、供应商承诺或正式验收结论。
