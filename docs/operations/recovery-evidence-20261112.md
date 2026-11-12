# 当前恢复证据核对（2026-11-12，#71）

本报告落实 [收尾规范 E](../specs/ai-closeout-20261112.md#e恢复证据核对)，关联 [#71](https://github.com/DavidHLP/BossAnalyze/issues/71) / [#66](https://github.com/DavidHLP/BossAnalyze/issues/66)。仅核对本机现场，不执行恢复、重启、部署或回退。

## 结论和时间口径

- **verified**：采集时五组件身份和独占监听与私有活动记录一致；现行恢复 specification 的启动字段一致；现有只读 checkpoint 在 HTTP 核验前后均通过。
- **verified**：五健康入口为 200；限定的匿名拒绝与 Python 认证后暂停拒绝符合预期；14 项既有离线安全场景通过。
- **unverifiable**：无人登录恢复、完整 31 项当前恢复验收、Java 认证后正文前拒绝，以及下一次实际重启的结果。历史 `accepted`、linger、健康响应均不能补足这些证据。

运行证据采集于主机墙钟 **2026-11-12 17:54:47–18:01:00 UTC**（09:54:47–10:01:00 PST）。这些是主机读数，不是独立可信授时。当前 boot ID 为 `31275199-ab5d-4dc7-aa1f-f172c0000087`；HTTP 核验结束时 uptime 为 15823.08 秒。以下“当前”仅指该采集窗口，不承诺报告阅读时仍然成立。

文档工作区起点为 `f4c4c084e1f42534639cbd727cdc28d5df021bf7`，核对完成后合入集成分支 `7e7215cb2c37a9d026a9e0165a860d2f63a02bf8`。该合入只增加 B 的文件及其文档，不改变本次所核对的运行服务；运行修订另列如下，不能用工作区 HEAD 代替部署修订。

## 来源与可复核边界

| 来源 | 时间 / 修订 | 读取内容与限制 |
| --- | --- | --- |
| S1 系统状态 | 上述窗口 / 当前 boot | `systemctl --user show`、`loginctl show-user`、`journalctl --list-boots`；不启动单元 |
| S2 journal | 上述窗口读取 / 各记录自己的 boot 与单调时间 | 恢复用户单元及当前 boot 的 logind 记录；仅保留去敏事件和时间，不公开完整日志 |
| S3 私有恢复现场 | 上述窗口 / 私有文件无可独立确认的 Git 修订 | 已加载单元所指 `restore.py`、其 `state-private.json`、引用的 `release.py` / `control.py`；仅公开逻辑和比较结果，不公开路径、环境、凭据、完整清单或预算内容 |
| S4 实际进程与监听 | 上述窗口 / 下表记录修订 | `/proc` 的 PID/starttime/exe/argv/cwd 与环境一致性比较、`ss -ltnp`、既有 listener inode 所有者断言；只输出比较结果 |
| S5 HTTP | 上述窗口（checkpoint 后采样为 17:58:05 UTC）/ 当时运行进程 | loopback 直连，禁用代理；仅输出状态码及必要的公开拒绝码，不记录 token 或响应全文 |
| S6 离线安全用例 | 上述窗口 / 当时私有 `test_restore.py` 与恢复源码 | `PYTHONDONTWRITEBYTECODE=1 python <私有恢复目录>/test_restore.py`，退出 0；未导入执行恢复主流程 |
| S7 历史摘要和脚本 | 上述窗口读取 / 摘要自行声明修订 | `acceptance.json`、`analytics-acceptance.json` 和下述已有脚本；历史摘要不能证明当前再次通过 |

结构核查先使用 codebase-memory 图定位 Python 暂停守卫及 HTTP 边界，并读取 snippet；索引 generation 为 `2026-11-12T17:54:49Z`。coverage 对仓库 `ai-service/app/main.py`、`privacy.py`、`config.py` 未报告缺口；`docs/` 被排除，私有文件为 outside_project，均改为直接阅读对应源文件/相关函数。图不包含私有恢复链，不能据其缺失推断私有功能不存在。

## boot、journal、linger

| 核对项 | 结论 | 证据与限制（S1、S2；采集窗口同上） |
| --- | --- | --- |
| 用户单元配置/状态 | verified | `bossanalyze-local-stack.service` 为 loaded、enabled、active/exited，ExecMainStatus=0；类型 oneshot，RemainAfterExit=yes，挂到 default.target。active/exited 是已完成单元状态，不能证明持续监督五应用 |
| linger 配置 | verified | 当前 Linger=yes；仅证明配置可见，不能证明无交互登录启动已验收 |
| 历史恢复输出存在 | verified | 可见 journal 中 5 个不同 boot 各有一条含 accepted 的恢复输出；这是输出记录数量，不是 5 次独立正式验收，也不累加历史断言数 |
| 当前 boot 顺序 | verified | 同一 boot 单调时间：manager session 124.535581 秒；恢复开始 124.619230 秒；Wayland user session 124.686938 秒；accepted 147.829058 秒；单元完成 147.835931 秒 |
| 无人登录恢复 | unverifiable | 交互 session 已在单元完成前建立；不能排除登录对应用启动的影响，未实施无登录控制实验 |
| 墙钟可用于跨 boot 成功排序 | unverifiable | 当前 boot 第一条显示 13:36:24 PST，采集时已显示 09:54 PST；accepted 为 21:36:48 UTC，晚于本次 17:58 UTC 采集读数。另有历史 boot 首尾墙钟倒序。不能据此计算真实绝对时刻、持续时间或跨 boot 成功顺序；仅同 boot 单调时间支持上行事件顺序 |

## 五组件身份和恢复 specification

下表均为 **verified**（S3、S4；上述采集窗口及当前 boot）。逐项核对 PID、内核 starttime ticks、exe、argv、cwd；环境仅在内存中比较，结果一致，不输出其内容。每个端口的可见监听所有者集合恰为该 PID，既有断言还检查监听 inode 未在扫描中变化且均有可见所有者。

| 组件 | PID | starttime ticks | 端口 / 唯一 PID | active 与 specification 所记 sourceSHA |
| --- | --- | --- | --- | --- |
| Python | 7460 | 12871 | 8090 / 7460 | `a27c559cbf6d26a480bc2eb59d3b7230171073d2` |
| crawler | 9075 | 13739 | 8081 / 9075 | `a529f5612d3cc735ab64f0fd9711ca49636b1b1e` |
| API | 8592 | 12987 | 8080 / 8592 | `6aa3b079ffa712a3b4354f21dbbea4086d7bac23` |
| analytics | 9340 | 14087 | 8082 / 9340 | `911fad2e852bc5aceb76d4a4cf4e65960197c037` |
| Nuxt | 9912 | 14630 | 5020 / 9912 | `6aa3b079ffa712a3b4354f21dbbea4086d7bac23` |

sourceSHA 是私有部署记录声明；本次确认该声明在 active/specification 一致、产物路径存在以及运行身份匹配，**未重新构建或对产物内容建立独立的源码溯源证明（unverifiable）**。PID/ticks 仅在当前 boot 内解释。旧文档 Python `a529f561` 是历史状态，不能覆盖当前 `a27c559c`；其他四组件记录与旧文档描述相符。

S3 中当前 phase=accepted、pending=null。五组件的 exe、cwd、argv、环境、sourceSHA、artifact 均与现行 specification 一致。旧 PID/ticks 作为上次启动身份留在 specification 并不等于当前身份错误：恢复入口采用其启动字段创建新进程，当前身份由 active 记录和现场证明。

对审阅过的现有纯读取函数按 AST 仅加载定义，执行 `release.checkpoint(state)`；没有执行模块顶层恢复流程、锁文件入口或任何 persist/save/start/stop。HTTP 前后两次均 **verified**：

- 五份 expectedMetadata 与当前元数据全文相等；pending 为空。
- 四个基础容器的 ID、运行状态、PID、启动时间、端口记录与私有状态一致；Mongo 身份及 27017 监听一致。
- 以 SQLite `mode=ro` 计算的现行共享预算逻辑摘要与已记录摘要一致。
- 五个应用身份及独占监听一致。

这是原有 checkpoint 在采集时的只读一致性证明；没有重置 CAS 或更新清单，也不能说明旧 release 的真实回退目标安全。B 的回退预检与回退文档由其他任务负责，本次未修改、未运行真实回退。

现行恢复入口源码（S3，私有修订不可独立确认）可直接确认的契约为 **verified**：健康 accepted 状态先 checkpoint 后退出；仅在全应用端口空闲且原 PID 均不存在、无 pending、CAS 与预算一致时转入恢复准备；prepared 路径在动作前重验阶段、身份、CAS 与预算；核对全部原容器 ID 后才可能启动原容器。源码还包含启动/写状态/发布动作，因此没有运行 `restore.py` 来“检查健康”。这些源码结论与离线场景均不替代真实启动路径验收。

## 安全 HTTP 观察

以下结果均为 **verified**（S5，窗口与修订同上），只发送健康 GET、无凭据/无业务正文的拒绝请求，以及 Python 暂停路径的畸形 JSON。未登录 QA、未创建会话或写入业务数据；Python 内部认证使用现场已有凭据，只存在于内存和本机请求头。

| 入口 | 当前结果 | 证明范围 |
| --- | --- | --- |
| API 8080、crawler 8081、analytics 8082 `/actuator/health`；Python 8090 `/health/ready`；Nuxt 5020 `/` | 五入口均 200 | 当时可响应；不证明全业务路径或无人登录恢复 |
| API `/actuator/health/readiness` | 200 | 附加就绪观察，不重复计为第六组件 |
| Python ready 的公开门禁字段 | resumeMatching.enabled=false；qualityGate.calibrated=false；automaticWritebackAllowed=false | 当前公开状态；不能代替所有组件调度配置的完整审计 |
| Nuxt `/api/auth/getUserBaseInfo` 无凭据 GET | 401 | 当前同源匿名账户读取被拒绝 |
| 8081、8080、5020 的 `/api/v2/ai/matches`、`/api/v2/ai/resumes/matches`，无凭据 POST `{}` | 均 401 | 仅证明这些 URL 的匿名边界；鉴权可能先于路由匹配，不据此证明每个 URL 已映射到业务控制器 |
| Python `/internal/v1/match-batch` 无凭据 POST `{}` | 401 / internal_unauthorized | 内部认证缺失被拒绝 |
| 同一 Python 路径，有内部认证、正文为单个 `{` | 503 / resume_matching_disabled | 此运行实例在 JSON 校验前命中暂停；未发送简历或职位，也未进入模型处理 |

Java 认证后暂停/正文前拒绝 **unverifiable**：本次没有创建或借用 QA 会话进行该核验；匿名 401 和 Python 503 不替代 A 的 Java 请求边界测试。

## 既有验收入口适用性

| 入口 / 来源 | 结论 | 本次处理与限制（S6、S7；采集窗口同上） |
| --- | --- | --- |
| 当前私有 `test_restore.py` | verified | 已审阅：AST 提取恢复守卫及 with 分支，在内存假对象上复现原实现反例和当前拒绝路径。实跑 14 场景、退出 0；stdout 的 “already healthy” 来自模拟，不是现场恢复执行 |
| 两处 `accept_http.py` | verified（不适用） | 源码包含登录/注销、对象上传/删除、写私有结果；还预期认证无效匹配为 400/422，早于现行 503 暂停语义。未运行 |
| `accept_routes.py` | verified（不适用） | 登录/注销、写结果，并预期认证空匹配返回 200，与当前暂停语义冲突。未运行 |
| `registration_acceptance.py`、`resume_acceptance.py`、`resume_version_http.py` | verified（不适用） | 源码含注册或简历/版本的 POST/PUT/DELETE 和结果写入；不满足本次只读范围，未运行 |
| 恢复目录 `acceptance.json`、`analytics-acceptance.json` | verified（仅摘要存在） | 各自声明 31 条通过，后者声明 analytics `911fad2e`；本次只读摘要，不把它们合为 62 条，也不认作当前 31 条通过 |
| 与上述 31 条摘要逐项绑定的完整只读复跑入口 | unverifiable | 在本次定位的恢复/验收目录及所读脚本中未建立完整对应；不凭摘要重建或猜测通过，不运行有写入副作用的旧脚本 |

没有新增测试或持久化核验脚本。文档交付检查为范围内 diff 审阅及 `git diff --check`；14 项离线、限定 HTTP、checkpoint 与历史 31 项分别陈述，不能相加作为一次验收成绩。

## 保留的未验收事项

未执行无人登录控制实验、真实重启/恢复/回退、Java 认证路径整体验收、账号或简历写入、模型/采集/邮件调用。未重新核验人工 gold、隐私授权、供应商能力和正式模型质量；本报告不批准质量、不开放匹配、不启用调度/写回，不关闭 #40/#44/#48/#54 或 GitHub 工单。
