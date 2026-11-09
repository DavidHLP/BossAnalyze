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

source_json 为原 detail_data 的 JSON 快照；摘要由 Java 对规范化来源计算。唯一约束 `(job_id,input_digest,pipeline_version,model_id,policy_version)`。state 使用 pending/running/completed/needs_review/retry_wait/failed/superseded，no_evidence 是 result_json.status，不是失败。fence 用于阻止过期 worker 提交。

只在复核当前 source 与 is_deleted=0 后，将已完成完整结果投影到原两个 TEXT 列，内容是真正 JSON 数组。Python 不连接数据库。新表迁移先在隔离 schema 验证，再按部署文档显式应用；应用不在启动时自动 DDL。

## 读侧和前端

外部既有 jobRequirements/employeeBenefits 保持 string[]。增加可空 `analysisStatus`（unparsed/legacy_unverified/pending/running/complete/no_evidence/needs_review/failed/stale）及 `analysisModel`、`analysisVersion`。无 sidecar 来源的非空历史值是 legacy_unverified，无值是 unparsed。

读侧仅将 source_json 与当前 detail_data 一致的完成分析标记为 complete/no_evidence；不能把历史值标成 Jev。读取最新分析状态与失败时可保留旧显示值，但必须显式标记非当前成功状态。新增表不存在时的兼容/部署顺序必须在运行文档明确，不能吞 SQL 错误伪造成功。

## 文件所有权

- A：ai-service/**、Python CI job、Python 运行文档；不得读 CC Switch 或调用真实模型，父任务独自处理凭据与真实调用。
- B：backend/crawler 除 FormatDataService 的 AI 代码/配置/测试，backend/pom.xml、database/migrations、必要 commons 下游 v2 安全契约；Java 运行文档。
- C：commons 数组解析/招聘读源辅助、crawler FormatDataService、analytics 读侧/模型/测试、frontend/**。不得改 B 的 mapper/AI 编排/迁移、A 的 Python 或 CI。
- 父任务：规范、issues/PR、工作树整合、跨栈契约、凭据、统一验证和运行验收。

所有人都不是独占仓库；保留其他人的改动和已有运行进程。跨边界变更先联系父任务，以小的独立补丁衔接。
