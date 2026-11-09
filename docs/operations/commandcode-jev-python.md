# Python Jev 服务运行与验收

范围：issue #41；仅业务计算，不连接业务数据库、不读取 CC Switch、不接收用户 JWT。Java 负责用户授权、简历归属、字段最小化、任务领取、结果投影和重试任务预算。配置或依赖缺失不能启用旧模型回退。

## 安装与启动

Python 3.13；依赖由 `ai-service/uv.lock` 固定（包含传递依赖）。CI 使用 uv 0.9.9。

```sh
cd ai-service
uv sync --frozen
uv run --frozen python -m pytest -q
uv run --frozen python -m compileall -q app
# 先由运维通过私有进程环境注入下列配置，再显式启动新服务。
uv run --frozen uvicorn app.main:app --host 127.0.0.1 --port 8090 --workers 1 --no-access-log
```

此命令不会在安装时自动运行。本次实现未运行该常驻服务、未终止已有进程、未执行真实推理。跨机应置于私网和 TLS 后。不要开 reload、httpx/httpcore wire debug，也不要记录请求体、Authorization、简历或上游原始异常。应用对外错误固定为受控 code、安全 message 与随机 requestId；响应附 X-Request-ID。框架校验错误不会回显输入。

必需私有环境配置：

| 名称 | 含义 |
| --- | --- |
| `CMD_API_KEY` | 仅 Python 使用的 CommandCode key |
| `AI_SERVICE_TOKEN` | Java → Python 独立共享令牌，不能使用用户 JWT |
| `JEV_MODEL` | 固定 Jev 发布版本，例如 `typesafe/jev-1.14.0`；无默认模型 |
| `CMD_API_BASE` | 默认 `https://api.commandcode.ai/provider/v1`；也接受同 host `/provider`，客户端规范化一次 `/v1/systemone` |
| `CMD_ZDR` | `1` 时始终发 `x-cmd-zdr: 1`，不可用则失败关闭；默认 `0` 不代表零保留 |
| `AI_BUDGET_PATH` | 默认当前目录 `jev-budget.sqlite3`；生产必须指向持久私有目录，保持文件及目录仅服务账户可写 |

可选上限：`AI_DEADLINE_MS=30000`、`AI_CONCURRENCY=4`、`AI_DAILY_TOKENS=1000000`、`AI_MAX_INPUT_BYTES=48000`、`AI_MAX_CANDIDATES=192`、`AI_MAX_JOBS=10`、`AI_RETRIES=1`（只接受 0 或 1）。无配置 `/health/live` 200、`/health/ready` 503；探针不调用供应商。认证失败 401，供应商 key 失效是 503，不能映射为用户登出。

Java crawler 与 Python 须使用相同的 `AI_MAX_INPUT_BYTES`、`AI_MAX_JOBS` 上限。Java 匹配按实际 UTF-8 JSON 大小拆批，包含简历及 deadline 字段；单条职位仍超限时，仅将该条标为 failed/unrankable，不截断原文。简历上限为 12000 个 Unicode 码点。

只允许官方 host/path，禁用重定向及环境代理继承，禁止请求方覆盖 model、endpoint、key 或 headers。需要网络代理时应由部署网络提供受控出口；不要通过改代码放开任意 URL。

## 冻结接口与候选

接口与字段遵守 [冻结协议](../specs/commandcode-jev-contract-20261108.md)。输入 jobId 必须是非空字符串，拒绝额外字段、重复 JSON key、NaN/Infinity、重复职位 ID 与超限输入。

事实来源仅 `jobDescription`、`jobTags`、`basicInfo` 的字符串叶子，不消费 analysisReport 或历史 AI 列。按换行/分号分段，不按逗号分割；长段落使用 800 code point 窗口及 160 重叠。相同片段合并但保留全部来源。JSON Pointer 转义 `~`/`/`，保留真实数组下标；start/end 为原字符串 Unicode code point 半开区间，Java 必须用 offsetByCodePoints 校验。匹配证据相对 `{job: detailData, resume: resume}`，分别以 `/job` 和 `/resume` 开头。当前简历候选为有界授权文本整体 `/resume`；必须由 Java 在出站前移除无关个人标识。

每批至多 48 候选，两类独立 Noul 和两个全文范围检查，共 98 问；要求和福利可以同时命中。选中要求再做五级核心 Score，最多展示 20 项。无候选、概率中间区间、范围检查冲突、低核心置信度均需要复核。不会静默截断尾部或以空数组冒充完成。

每职位独立 state，四维各做状态 Choice、连续五级 Score 和候选证据 Noul；通用 HTTP 批次最多 100 问。岗位不适用维度才重分配权重；简历缺失或关联证据不足为 unknown，任何 unknown 都无最终分数。仅部分职位失败时保留原 ID 返回 failed 项、complete=false；全部供应商失败按 503/504/502 返回安全错误。不对失败/未知职位生成零分。单批输入顺序不代表全局排名，Java 负责完整范围和排序。兼容整数出口使用 HALF_UP，Python 主协议保留连续精度。

校验响应 model、精确问题 ID 集、回答类型、有限概率、完整分布与和、Choice 最大概率选项、Score 等级 legend 和期望值。缺失 usage 保持 null，不能虚构 0；HTTP 重试仅网络/429/5xx，最多额外一次且共享总 deadline。ZDR 错误码无论 HTTP 状态均终止，不删除 header 重试。Java 不应叠加 HTTP 重试；调度尝试应与 Java 的总 attempt_count 共同控制。

## 预算、完整性与质量闸门

每次出站前使用 SQLite 标准库的短事务保留日额度（UTC 日），跨进程及重启共享同一持久文件。它是本地运行额度账本，不是业务数据库。每次保守保留请求 UTF-8 字节数 + 8192 token 的输出预留；重试重新保留，未知 usage 或网络失败不退还，真实 usage 超过预留时追加记账，达到预算失败关闭。账本不可用则 503，不能退回无预算模式。多机器必须集中出口/共享额度分配，不能给每台都配置完整账户预算。供应商实际计费可能超过单次预留，应用准入预算不是账单金额保证；供应商账户限额仍需独立设置。不要删除账本来“恢复”预算。

没有可信 Jev tokenizer；UTF-8 字节数仅作保守 admission 上界，state + 单问题最多 30000 字节、整批最多 60000 字节，不宣称精确 token 计数。超限明确 422，不裁掉字段。长全文即使分问题批次仍可能因 state 过大被拒绝，父调用侧必须显示未完成，不能写成功投影。

`jev-policy-1` 权重草案 skills 0.5 / relevant_experience 0.3 / experience 0.1 / education 0.1；接受概率 0.9、拒绝概率 0.1。它们是待校准策略，不是最佳参数。修改这些参数、切分/问题语义或自动接受规则时需升级策略/管线版本，避免复用旧身份缓存。

默认 `AI_QUALITY_APPROVED=0`，即使离线模拟全成功，提取结果仍为 needs_review，禁止 Java 自动投影。`/health/ready` 显式暴露 qualityGate、权重、阈值和目标（候选召回 >=98%、自动接受 precision >=95%、证据一致性 100%）。只有父任务完成按职位分组的人工标注/留出质量验收并记录各子组覆盖、排序、未知覆盖、成本及 p95 后才能设为 `1`。该开关是操作员证明的前置条件，不会自动测量或证明质量；匹配仍需 UI 清楚标注为推荐参考，不能用于自动录用/淘汰。

本次 MockTransport 覆盖实际 HTTP URL、headers、payload、批次、错误/重试、Unicode 证据及数学契约；不能证明真实 CommandCode Choice/Score 兼容性、领域召回/precision、ZDR 可用性、真实成本/p95 或跨栈运行通过。真实调用由父任务独自做合成在线验收。

## 整合顺序

父任务先整合 Java 客户端及 reader/增量表，再以调度关闭状态联调。Java 只允许 complete/no_evidence 且来源/身份/fence 有效的结果原子投影，needs_review 不覆盖原值。本服务不自动迁移表、不自动回填、不持久化简历/模型输出。回退关闭调度并使 lease 失效，保留新表和 JSON 结果，不启用旧模型。

参考公开协议：[CommandCode Provider](https://commandcode.ai/docs/provider)、[Noul](https://docs.typesafe.ai/primitives/noul)、[Choice](https://docs.typesafe.ai/primitives/choice)、[Score](https://docs.typesafe.ai/primitives/score)。锁文件安装与离线测试独立于真实模型验收。

## 供应商数值精度

合成运行捕获到 Score 和各级概率独立保留两位小数：例如 score=3.70，显示概率的加权和却为3.72。校验按每个显示值±0.005构造区间，并要求区间内存在总和为1的分布及相容的期望分数；不再使用浮点差值固定0.01的判据。原始score、概率与confidence原样保留，不重新分配概率或修改模型分数。缺失等级、非有限数、越界、无法归一的分布和超出舍入可行范围的分数仍被拒绝。

这是对已观察到的 CommandCode 精度的有界兼容，并非供应商发布的精度保证；若精度契约变化须重新验证。原运行失败报告保留为历史，5份已捕获的拒绝响应可在无网络回放中通过；这不替代修复后真实模型、排序或质量门槛验收。
