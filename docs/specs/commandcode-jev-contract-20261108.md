# Jev 实施共用契约

对应 [完整规范](commandcode-jev-migration-20261108.md)。本文件固定并行实现的字段和责任边界；更改时先协调调用双方。

## Python 内部业务协议

仅接受 `Authorization: Bearer <AI_SERVICE_TOKEN>`，不接受用户 JWT。健康探针不调用模型；无配置时 readiness 503，存活探针不依赖外网。

`POST /internal/v1/job-analysis` 输入：`{"jobId":"42","detailData":{},"timeoutMs":30000}`。jobId 始终为字符串，detailData 是 JobDetailData JSON 对象。deadline 是剩余相对毫秒；服务有自己的上限。

输出必需字段：jobId、status（complete/no_evidence/needs_review）、requirements、benefits、coreRequirements、model、pipelineVersion、policyVersion、requestId、usage。前三个数组元素统一为 `{candidateId,text,evidence,probability,importance}`；probability/importance 可以为 null。evidence 为 `{sourcePath,start,end,text}` 数组，sourcePath 使用 RFC 6901 JSON Pointer，start/end 是该源字符串的 Unicode code point 半开区间；text 精确匹配切片。importance 是核心程度的 0–4 连续值，其他项可 null。

usage 固定 `{inputTokens:null,outputTokens:null}` 形状，真实返回时填非负整数，不虚构零。pipelineVersion 为 `jev-1`，policyVersion 为 `jev-policy-1`；模型通过外部 JEV_MODEL 配置，部署示例 `typesafe/jev-1.14.0`。

`POST /internal/v1/match-batch` 输入：`{resume:"合成或已授权文本",jobs:[{jobId:"42",detailData:{}}],timeoutMs:30000}`。

输出必需字段：items、complete、model、policyVersion、requestId、usage。items 精确对应输入 ID，每项 `{jobId,status,score,dimensions}`；status 为 scored/insufficient_evidence/failed，score 为 0–10 连续数或 null；dimensions 为 `{name,status,rawScore,confidence,probabilities,evidence}` 数组，name 为 skills/relevant_experience/experience/education，status 为 scored/unknown/not_applicable。rawScore 为 0–4 连续数或 null，probabilities 为对象或 null。evidence 同上，匹配时 sourcePath 以 `/job` 或 `/resume` 开头并可回溯到本次输入。成功 scored 才有最终 score，不将未知变成零分。

业务错误使用 `{error:{code,message},requestId}`；输入错误 400/422、内部认证失败 401、供应商坏输出 502、供应商配置/认证/限流不可用 503、耗尽 deadline 504。不可返回原始供应商响应、密钥、请求体或简历。

## Java 分析结果存储

新表固定名 `job_ai_analysis`，字段：id、job_id、input_digest、pipeline_version、model_id、policy_version、state、source_json、result_json、attempt_count、next_retry_at、lease_until、fence、error_code、input_tokens、output_tokens、created_at、updated_at。

source_json 为原 detail_data 的 JSON 快照；摘要由 Java 对下述选定事实规范化计算。唯一约束 `(job_id,input_digest,pipeline_version,model_id,policy_version)`。state 使用 pending/running/completed/needs_review/retry_wait/failed/superseded，no_evidence 是 result_json.status，不是失败。fence 用于阻止过期 worker 提交。

事实比较的唯一规则（同一个 jev-1 首版协议，不引入过渡身份）：只选 `jobDescription`、`jobTags`、`basicInfo` 三个完整事实子树。顶层缺失键规范化为 JSON null；空对象、空数组、空字符串和嵌套 null/缺失保留各自结构，不互换、不裁剪。递归按对象键排序、保留数组顺序和原始 Unicode，不 trim/归一化/改写文本；以紧凑 JSON 的 UTF-8 字节计算 SHA-256。其余元数据（含 updateTime、analysisReport、company）不进入摘要、源复核或 freshness 判断。`source_json` 仍为领取时当前活跃源的完整快照。Java commons `JobAnalysisFacts` 是摘要/领取/完成/读侧共用实现；Python candidates 仍只消费这三个事实子树。证据仍须精确指向事实子树中的原始字符串，不能以排除的元数据支持判断。

来源 A→B→A 命中同一唯一身份时，允许 superseded 重新领取。先锁定当前非删除源，事实相同才能更新完整快照、递增 fence、签发新 lease。只有重验 jobId/model/pipeline/policy/usage、完整数组及当前事实证据通过且 status 为 complete/no_evidence 时，才复用缓存；重投影仍须写回已获准；缓存重放不增加付费 attempt_count。无可用缓存则仍受最多三次有效领取限制。终态 completed/needs_review/failed 不自动重投；扫描按 ID 翻页，将不可领取身份排除在十次成功领取额度之外，前十条旧身份不能堵住后续。

调度层 400/422 为终态 failed，error_code 分别为 invalid_input_400/invalid_input_422，next_retry_at 为 NULL；仅可恢复的 429/502/503/504 延后五分钟重试，仍受三次预算。其他拒绝终止为 ai_rejected。过期第三次 running 无可用缓存时转 failed；若为完整缓存重放则可重新签发 lease/fence，不增加推理次数。Python 的有界 HTTP 重试和 deadline 规则保持不变。

只在复核当前 source 与 is_deleted=0 后，将已完成完整结果投影到原两个 TEXT 列，内容是真正 JSON 数组。Python 不连接数据库。新表迁移先在隔离 schema 验证，再按部署文档显式应用；应用不在启动时自动 DDL。

## 读侧和前端

外部既有 jobRequirements/employeeBenefits 保持 string[]。增加可空 `analysisStatus`（unparsed/legacy_unverified/pending/running/complete/no_evidence/needs_review/failed/stale）及 `analysisModel`、`analysisVersion`。无 sidecar 来源的非空历史值是 legacy_unverified，无值是 unparsed。

读侧仅将 source_json 与当前 detail_data 的选定事实一致的完成分析标记为 complete/no_evidence；不能把历史值标成 Jev。读取最新分析状态与失败时可保留旧显示值，但必须显式标记非当前成功状态。新增表不存在时的兼容/部署顺序必须在运行文档明确，不能吞 SQL 错误伪造成功。

## 文件所有权

- A：ai-service/**、Python CI job、Python 运行文档；不得读 CC Switch 或调用真实模型，父任务独自处理凭据与真实调用。
- B：backend/crawler 除 FormatDataService 的 AI 代码/配置/测试，backend/pom.xml、database/migrations、必要 commons 下游 v2 安全契约；Java 运行文档。
- C：commons 数组解析/招聘读源辅助、crawler FormatDataService、analytics 读侧/模型/测试、frontend/**。不得改 B 的 mapper/AI 编排/迁移、A 的 Python 或 CI。
- 父任务：规范、issues/PR、工作树整合、跨栈契约、凭据、统一验证和运行验收。

所有人都不是独占仓库；保留其他人的改动和已有运行进程。跨边界变更先联系父任务，以小的独立补丁衔接。
