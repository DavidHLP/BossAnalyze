# BossAnalyze 基础设施与 Java/Python 统一决策

日期：2026-11-08。方案基线：958a9347ae3517f8bbfc813a14836253bec79a4e。
状态：用户已批准实施。2026-11-08 最新范围明确排除全部 AI 改造；不迁 Python。本文保留原方案背景，AI 章节仅作延期记录，不是本轮任务。

## 决策

| 范围 | 首轮选择 | 理由 |
| --- | --- | --- |
| AI | Java ProcessBuilder 直接调用本机 codex exec，精确 gpt-6-luna、reasoning low | 当前无 Python 专属算法/库；只为启动 CLI 加 Python 会增加协议、进程管理与部署成本 |
| Java → Python | 首轮不迁生产模块；Python AI 任务包保留为有实际 Python 依赖时的后续路径 | 不把语言迁移数量当作收益；不同时维护 Java/Python 两套 AI 执行器 |
| Spark | 保留 Java 招聘/日志分析与 Session 生命周期 | 迁语言不减少 JVM 依赖，且当前没有性能/维护收益对照证据 |
| 采集、业务 API、鉴权、简历版本、数据库写入 | 保留 Java | 现有调用链和资源归属边界完整；避免双写者与授权分叉 |
| 定位 | Java 读取 DB-IP Lite City MMDB，去掉在线 API/key；库可选 | 提供离线国家/城市及近似坐标，许可与更新渠道明确；缺库不阻断 analytics |
| 对象存储 | RustFS 服务端，新卷；先保留现有 S3/MinIO Java 客户端与图片 HTTP 接口 | 不为服务端替换重写客户端；S3 兼容仍须逐项实测 |
| 数据库 | 显式增量迁移 role_name 唯一约束，预检查与恢复演练先行 | 应用层 count 后写不能代替约束；禁止导入带 DROP 的快照到现库 |

## 如何处理独立分析的分歧

Python 方案一建议迁移提示、schema 校验和 CLI 生命周期；方案二认为这些用当前 Java 就能完成。选方案二：目前没有 Python 独有能力、团队维护收益或实测数据支撑新增运行时。若未来引入 Python 模型/向量/NLP 库或独立数据处理模块，优先选 Java 后方的一次性 Python 计算任务；Java 保留鉴权、调度与数据库写入，不先引入 Python HTTP 服务、队列或 PySpark。

定位曾有 ip2region 与 DB-IP MMDB 两个候选。选 DB-IP Lite：本轮官方页面核实提供 MMDB、月更、近似坐标及 CC BY 4.0，适合现有地图数据接口；在使用结果的网页放置官方要求的 DB-IP 链接。GitHub 未识别 license 不能直接等同于许可证不合法，也不将该报告判断当法律结论。此处选择只根据当前数据能力和明确的发布条款，不额外引入双库。

## 实施与验收顺序

### 1. analytics 可启动与定位

- 替换 IpGeoService 在线查询，删强制 key 初始化与旧配置；MMDB 外部只读挂载，不启动自动下载。
- 无库/坏库只降级地区增强，输出明确 unknown/诊断；不吞掉 Spark、文件或数据库实际错误，不生成 (0,0) 伪坐标。
- 核验真实部署的代理链，只信任配置中的代理。入口覆盖伪造转发头，不能无条件相信 X-Forwarded-For；不采集第三方 cookie，不把客户端自报位置当日志事实。
- 历史 loopback 日志不能恢复真实位置。当前用户手动选择的城市可作为偏好，不能作为权限依据。
- 验收无在线 key/无库仍启动；有库的 IPv4/IPv6、私网/非法输入、缺失坐标、并发和关闭；真实招聘空集及隔离非空样本、日志统计、鉴权分别验证。

### 2. 数据库约束

- 新鲜读取目标 schema/版本/collation、完整索引定义、重名与 canonical USER 状态，不凭旧报告直接 DDL。
- 在隔离 schema 恢复备份、预演迁移；明确实际权限、锁窗口、失败处置。
- 已有完整单列唯一 role_name 则跳过；无约束才 ADD UNIQUE。索引名相同、复合或前缀索引不能冒充。
- 真实库增量 DDL 前确认无重名；不自动删除、合并角色或重挂用户。MySQL DDL 隐式提交，不许承诺事务回滚。
- 独立连接并发验证冲突与 API 409；验证原行数/引用不变。token 已为 create_time 的环境跳过时间列迁移。报告中的 JWT 重复线索需另核当前实现，不据此直接添加 token 唯一键。

### 3. RustFS

- 先盘点实际源存储、bucket、对象版本、锁/加密/策略与备份；此前没跑 MinIO 不证明源数据为空。
- 固定目标版本，在新卷/独立端口启动；不把 MinIO 数据目录直接挂给 RustFS。
- 先合成对象实测 put/get/delete、预签名、内容类型、大对象、中文键、跨用户拒绝与旧图片读取。
- 通过 S3 复制与对账：当前对象之外，版本/删除标记/metadata/锁/加密须单独确认。冻结写入窗口内做最终差异复制和删除对账，不删除源。
- 切换后如产生新写入，回退前须反向对账；不能只切回旧 endpoint。旧源保留至验收与备份满足条件。

### 4. 唯一 AI 执行器

- 在 AIExtractionOfJobRequirements 实际业务入口替换 ChatClient；确认无引用后删 Ollama/OpenRouter 依赖与死实现。
- 绝对可执行路径，参数数组与 stdin；固定 gpt-6-luna 和 low，不静默改模型；只解析最终 schema 输出，Java 再验分数/长度/类型。
- 专用受限服务身份、独立 cwd、最小环境与文件/工具权限；read-only sandbox 不能单独证明无法读敏感文件。不得向进程/模型传 JWT、cookie 或业务数据库密钥。
- 持续有界消费 stdout/stderr，进程树取消、超时/并发/批次总量上限；失败不伪造中间分数、不写成有效 enrichment。
- 调度锁覆盖实际异步任务；数据更新包含有效行/源内容未变/目标仍空条件，保留 Java 单写者。
- 先 fake-process 回归，后合成数据真实调用，确认服务身份、模型/effort、额度及延迟。CLI 在本机不代表模型离线或数据不出站。

## 验证与回退边界

先跑受影响模块与既有安全/凭据契约；整合版本再跑仓库必需检查。运行验收单独记录，不能用 mock、缓存模型列表、S3 兼容声明或旧 CI 结果代替。
定位可禁用增强；数据库约束一般随应用回退保留；存储需对账后回切；AI 失败停用任务并保留已有结果，不自动启用另一供应商。各步骤不混在同一个切换窗口。

城市/职位列表改直接 SQL 可能独立有收益，但先测排序/collation/延迟与负载等价，不把它塞进本轮语言迁移；当前招聘库空不证明未来规模或 Spark 无价值。

## 证据与参考

- [四项方案 A](/tmp/bossanalyze-infra-plan-codex-20261108.md)
- [四项方案 B](/tmp/bossanalyze-infra-plan-claude-20261108.md)
- [Python 边界方案 A](/tmp/bossanalyze-python-migration-plan-codex-20261108.md)
- [Python 边界方案 B](/tmp/bossanalyze-python-migration-plan-claude-20261108.md)
- [此前真实本地运行报告](/tmp/bossanalyze-runtime-validation-20261108.md)：本次不重新声明为实时验收。
- [DB-IP 官方下载与许可](https://db-ip.com/db/download/ip-to-city-lite)：月更/格式/署名；本次在线核实。
- [RustFS 官方仓库](https://github.com/rustfs/rustfs)：S3 兼容定位；不是本项目实际兼容证明。
- [Spark 4.0.0 官方文档](https://spark.apache.org/docs/4.0.0/)：运行环境；本次在线核实。
- 本轮读取当前 codex exec --help，核对 stdin/model/config/schema/ephemeral/ignore-user-config 等参数存在；未发起推理，帮助不证明模型在线权限。

尚未实施、迁移、重启、调用真实 AI/存储或验收真实 DDL。存储源清单、离线库落盘、Codex 服务身份隔离与权限、DDL 预演仍是执行阶段必须解决的事项。

## 本轮实施任务图

- GEO：离线 MMDB、可信代理、署名与聚合结果；独立实施。
- DB：幂等角色唯一约束、隔离恢复及并发预演、本地实际增量迁移；独立实施。
- STORE：RustFS 新卷部署、S3 兼容与保留数据切换；独立实施。
- INTEGRATE：依赖 GEO/DB/STORE，统一构建、真实运行与独立审查，修复后完成单一 PR。
- AI 与语言迁移全部延期，不修改其代码、依赖、调度配置或模型调用。
- 仅操作明确的本机开发环境；不进行未指派的生产部署。
