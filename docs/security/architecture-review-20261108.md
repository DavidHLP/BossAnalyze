# BossAnalyze 全栈严格审查 · 合并裁决版

基线：main `440d8b658f9aba1309b752a9a4b8512c72a88abc`。两轮独立阅读，主审复查关键调用链后，收敛为 **21项源码确认问题：8项P1、13项P2，未确认P0**。这些是应用源码的确定缺口，不是生产利用已复现，也不表示每一行、每一种运行组合均已验收。

执行配置：Codex provider / gpt-6.1-sol / high / default；Claude Code provider / deepseek/deepseek-v4.1-flash / high。后者是Claude Code工具运行，实际模型不是Anthropic Claude。各轮原始结论不等于主审认可，以下裁决优先。

> 🧠 **From Hindsight memory (Key decisions and rationale; Core concepts)** — 历史记录要求后端承担授权、简历创建设置meta类型、请求失败区别于空结果；招聘页仅要求登录是已有产品决定。这些历史主张只作核查线索，当前实现与历史记录不一致时以本次源码为准。

建议顺序：先修后端授权/归属和不可逆写入，再修认证敏感信息、撤销与共享权限，随后统一错误及招聘数据语义。当前前端有确认、字段白名单、权限过滤、回读和状态区分，应保留；不能继续让它们承担后端安全职责。

## 验证证据

- 第一轮：`node --experimental-strip-types --test frontend/tests/*.test.mjs`，47通过、0失败、0跳过。
- 第二轮：`mise x node@26.7.0 -- pnpm test`，47通过；`pnpm type-check`退出0。两轮运行的是同一组47测试，不应相加成94项不同测试。
- 主审：纯内存QueryMapper自检，使用仓库对应Spring Data MongoDB 4.4.5 / Spring 6.2.6，属性名映射为`_id/user_id/doc_type`通过。无Mongo连接，无业务数据；早期自检编译/类路径错误已修正，最终退出0。
- 主审核对依赖文件的图索引覆盖；SQL partial缺口逐行阅读。图索引是best-effort，不是完整审查或正确性证明。两轮对大范围路径做覆盖检查也不等于逐行审计。
- 未执行Maven reactor/凭据契约、真实HTTP利用、数据库或Spark/Redis/Minio集成、浏览器/移动真机/屏幕阅读器、生产网络和容量验证；未启动或重启服务，未改仓库、未提交、未写业务数据。

## 主审裁决与排除项

| 原结论 | 裁决 | 依据 |
|---|---|---|
| 第二轮三项 P0 | 调整为 P1/P2 | 严重度按已知触发条件与影响校准：需已登录或已知对象ID；未证明无条件灾难性影响。保留修复优先级，不因两轮一致就升级。 |
| Mongo 属性名/存储名混用导致所有查询失效 | 排除该归因；保留F06 | 使用仓库对应Spring Data MongoDB 4.4.5的QueryMapper做隔离自检，id/userId/docType正确映射为_id/user_id/doc_type；缺meta类型和标题未保存仍成立。 |
| AI循环只处理第一页 | 排除 | RequirementAnalyzer.clear()后isEmpty为true，会继续循环；查询空页才break。全局游标跨城市口径仍需单独数据反例。 |
| 调用重复ID修复就删除全部历史 | 降为条件风险 | 删除分支仅在发现重复_id时执行；普通集合_id唯一，未证明该分支正常可达。不作为常规调用必现的数据删除缺陷。 |
| 当前看板展示硬编码在线人数等 | 修正为F20后端契约问题 | 当前dashboard投影明确不使用这些字段，界面也有未纳入实测展示说明；不能将后端字段存在等同于UI展示。 |
| SVG同源XSS、对象路径穿越、上传无大小限制 | 待验证 | 有SVG MIME/内联候选链；缺浏览器认证上下文、响应头实测与有效multipart限制核对，不认定已实现脚本执行或无限上传。对象删除F03独立成立。 |
| 同秒JWT重复→多行认证异常 | 高价值待验证 | JwtService没有jti且时间按秒编码、DDL无唯一键，存在碰撞可能；边界时刻iat/exp也可能不同，未实际双登录/执行MyBatis，不写成必现。唯一索引无需再叠加同列普通索引。 |
| API-01 所有HTTP200业务错误均可当成功 | 契约风险 | request客户端确实做data??envelope兼容；现有测试有意保留旧登录错误包，尚未给出当前写入口误报成功反例，不盲改共享客户端契约。 |
| 菜单未在返回端过滤 | 并入F01/设计权衡 | 导航可见性由前端menuTree处理；不把完整路由树本身当独立提权证据，真正问题是管理操作缺少后端授权。 |
| 无迁移、Mongo无索引、重复SQL索引 | 设计/部署待验证 | 快照不是增量迁移；源码缺@Indexed不能证明实际库无索引。索引是否冗余、唯一性范围与清理策略需实际schema、EXPLAIN和数据分布。禁止直接重导DROP快照。 |
| 爬虫调度关闭、状态机/INSERT IGNORE | 待验证产品与数据口径 | 不能未经产品要求恢复采集；需以重复处理、状态流转与字段持久化反例分别验证，不把多个潜在问题捆成已确认缺陷。 |
| CORS宽松、审计缺失、死adapter/文档 | 改善候选 | 当前Bearer来自localStorage，浏览器不会自动为恶意站点附加它；未来Cookie风险不等于当前CSRF已确认。审计、文档和保留adapter按业务必要性排序，不用文件数量替代缺陷证据。 |

## 三领域覆盖矩阵


“静态”表示阅读源码、检索调用者与保护；“运行”仅指下文实际执行的隔离 Node 测试。入口覆盖不等于逐行形式化证明。

### 前端业务设计与使用体验

| 路由/检查路径 | 检查方法与闭环 | 静态/运行 | 未覆盖 |
|---|---|---|---|
| `/`、`/login`；pages/index.vue、login.vue、LoginForm.vue | 跳转、登录提交、错误、重复点击、字段标签、重定向限制 | 静态；session/request 测试 | 真实登录、浏览器自动填充、视觉与焦点实测 |
| `/console`；pages/console/index.vue | 授权快捷入口、仅获授权时读取统计、缺快照/错误/零值、独立加载账户权限 | 静态；dashboard/session/settings 测试 | 实时权限变更、真实断网 |
| `/console/dashboard` | 日志统计→快照校验→指标/图表/热门路径，刷新、空/错/加载 | 静态；dashboard 测试 | 图表视觉、真实吞吐/大数据量 |
| `/console/users`、users/UserEditor、DeleteUserDialog | 筛选/分页→详情→创建/编辑/删除；密码确认、255字上限、离开确认、当前账户改动后退出 | 静态；users 测试 | 真服务写入、并发编辑、键盘焦点恢复 |
| `/console/roles`、roles/RolePanel | 角色元信息与授权分开；父子选择、遗漏权限阻断、清空确认、回读确认、默认/当前角色前端保护 | 静态；roles 测试 | 两管理员并发授权、后台角色失效 |
| `/console/menus`、menus/MenuPanel | 树筛选/折叠→编辑→回读；权限标识与父级不可变、引用核对、删除确认 | 静态；menus 测试 | 服务端同时变更引用后的 TOCTOU |
| `/console/profile` | 账户/角色/权限独立读取、字段白名单、空/错/加载 | 静态；settings 测试 | 服务端权限缓存失效时效 |
| `/console/settings` | 只读账户/访问与日志网关健康；降级与传输错误分开 | 静态；settings/dashboard 测试 | 对底层服务真实健康的证明 |
| `/console/[...slug]` | 必须先通过 auth；已授权但无具体实现时显示重建提示 | 静态 | 动态菜单中实际有哪些未完成业务页，未连库枚举 |
| `/recruitment`、OptionPicker、JobSheet、ScatterPlot | 城市→职位→维度分析→分页详情；独立高薪榜；职位请求序号、防重复分析、外链白名单 | 静态；recruitment 测试 | 真实 Spark、SVG 悬停/键盘与颜色对比实测 |
| middleware、useSession、plugins/session.client、AppSidebar、NavUser、layout、request | 登录/权限菜单→路由守卫→请求→401清理→退出/跨标签切换 | 静态；session/request 测试 | Vue composable 的完整集成测试、会话恢复故障与退出撤销 |
| 全部上述页面的响应式 class、aria-busy/role/label、Sheet/Dialog | 检查模板状态、标签、移动断点、溢出与确认交互；存在大量明确无障碍标记 | 静态 | 未开启浏览器，不判定 WCAG/视觉/移动端验收通过；未逐个审计 shadcn 基础控件内部 |

全量页面清单共 11：index、login、recruitment、console/index、dashboard、users、roles、menus、profile、settings、[...slug]。

### 后端设计

| 检查路径 | 方法与实际覆盖 | 静态/运行 | 未覆盖 |
|---|---|---|---|
| api/system/auth/filter、UserDetailsServiceImpl、JwtService 调用点、UserMapper、TokenMapper | controller 前的认证、公开前缀、权限查询、用户停用、角色状态、令牌撤销 | 静态 | 未发真实 token 请求、未校验外部部署网关 |
| AuthController→AuthServiceImp/RoleServiceImp/RouterServiceImp→Mapper | 登录注册、菜单/角色增删改、权限替换、事务注解、默认角色与唯一性、调用者检索 | 静态 | SQL 事务隔离/死锁实测、注册邮件与 Redis 故障 |
| UserController→UserServiceImp→UserMapper | 管理读取/创建/编辑/删除、分页 offset、密码确认、编码、输入信任 | 静态；前端分页/载荷测试 | 后端 controller 授权与验证测试未运行 |
| ResumeController/ResumeVersionController→两项 Resume 实现→MongoTemplate | userId 来源、create/update/delete、commit/checkout/reset/debug/helper、文档类型及归属、并发写窗口 | 静态 | 无 Mongo fixture 或复制集事务运行；merge/revert 内容算法未穷举 |
| ImageController→MinioService | 上传 MIME/文件名、同名对象、读取/预签名/删除、所属用户检查 | 静态 | 实际 Minio ACL、恶意图片解析、存储桶版本策略 |
| api LogController→RestTemplate→analytics LogController/LogServiceImp/LogAnalysisScheduler | 公开路径、统计转发、健康返回、手动/定时分析与 Redis 多键发布 | 静态入口及关键段 | 未重放 HDFS 日志、未全量验证每个统计聚合公式 |
| analytics User controllers→ListBasicSet/TwoDimensionalAnalysisChart、SalaryHotJob 入口 | 查询表达式、数值编码、未知值、驱动端收集、软删除 | 静态 | 未起 Spark、不执行 JDBC 全表读取、排序/采样性能未测 |
| crawler AIRequirementsController→RequirementAnalyzer；DataScrapingService、StartScheduled、Mapper XML | AI 请求入口、采集初始化/调度状态、状态标识、持久化删除语义 | 静态入口/关键写读段 | Selenium/外部站点/AI 调用和全任务恢复未运行；不是完整爬虫算法正确性证明 |
| backend reactor 与三个业务模块 pom、commons 日志模型及 RedisMutex/RedisLock | 业务模块均依赖 commons；没有为现有三模块拆分提出新网络层；缓存加载和锁租约关键语义 | 静态 | 锁过期/抢占、缓存冷启动、多实例故障注入 |
| backend/api/src/test、commons/src/test、tools/credential-contract-tests | 定位 Hadoop、日志模型、凭据隔离契约测试；未把这些当作授权测试 | 文件/测试范围检查 | Maven、凭据契约、后端 reactor 测试均未运行 |

### 数据库及持久化

| 检查路径 | 方法与检查项 | 静态/运行 | 未覆盖 |
|---|---|---|---|
| database/system.sql 全部 7 表：nginx_access_log、permission、role、role_permission、router、token、user | DDL、主键/唯一键、级联/SET NULL、字段类型与 Mapper 对照；email、permission 唯一，role_permission 联合主键 | 静态直接读 DDL | 未读取实际 schema、数据基数、执行计划；不执行 DROP 快照 |
| database/boss.sql 全部 7 表：city_data、degree、html_data、industry_data、job_list、position_data、t_job_detail | URL/编码唯一性、前缀唯一索引、层级外键、JSON、is_deleted、时间与生命周期 | 静态直接读 DDL | 空库导入兼容性、真实重复数据及 URL 分布未测 |
| User/Role/Permission/Router/Token Mapper 和实体 | 关联删除、字段映射、权限读取缓存、默认角色查询与并发唯一性、token 字段差异 | 静态 | 事务实测、真实约束漂移 |
| crawler Mapper XML、JobDetailEntity、analytics JDBC 读取 | SQL 参数绑定与 Spark 表达式差异、软删除读范围 | 静态关键 SQL | 所有采集 upsert 与故障恢复组合未穷举 |
| Resume.java、MongoConfig、两项 Resume 实现 | 同集合 meta/commit、Id 绑定、查询归属、save 替换、无 @Version 的并发更新窗口 | 静态 | 实际 Mongo 索引/验证器/TTL、历史损坏记录数量 |
| CacheConfig、UserMapper、commons Redis utils、日志结果发布 | 本地权限缓存及 Redis 业务缓存分开；权限缓存未检索到对应 CacheEvict；锁与多键快照一致性候选 | 静态 | 实际 Redis key/TTL/内存、跨进程一致性 |
| MinioService | key 命名、对象归属、覆盖、删除与签名生命周期 | 静态 | 桶策略、版本保留与孤儿对象数量 |
| database/README.md、迁移文件发现 | 明确为初始化快照，不是增量迁移；CONTEXT/ADR 缺失 | 静态 | 未宣称实际环境版本等于快照；本次不新增迁移 |


## 源码确认发现

### F01 · P1 · 普通登录用户能够调用管理写入口

- 证据：`backend/api/src/main/java/com/david/hlp/web/system/auth/filter/SecurityConfiguration.java:45`；`system/auth/controller/UserController.java:84`；`system/auth/controller/AuthController.java:282`（路径均在 backend/api/src/main/java/com/david/hlp/web 下）。UserServiceImp.java:54 直接调用 UserMapper.updateById，UserMapper.java:103 更新 role_id 等字段。
- 反例/链路：仅拥有普通 USER 有效令牌的用户直接 POST `/api/user/updateUser`，传自己的 id 和管理角色 roleId；或者 POST `/api/auth/updateRoleRouters` 修改任意角色授权。JWT 过滤器只做认证，controller/业务实现/Mapper 没有操作权限检查。
- 影响：提升自身角色、修改其他用户和角色/菜单；可越过整个前端授权导航。
- 排除保护：已检索 PreAuthorize、EnableMethodSecurity、hasAuthority、hasPermission、HandlerInterceptor、SecurityContextHolder 与全部管理控制器；没有发现兜底授权。删除用户的“当前操作者密码”仅证明知道自己密码。用户 status=1 查询保护仍存在，不能替代角色权限；role.status 没有在该认证链中复查，userAuthorities 又是无显式失效的本地缓存。
- 最小方向：在现有后端授权 seam 统一落实管理操作对应权限，角色状态及授权失效由后端承担；不靠菜单是否显示。
- 应验证：无令牌→401、普通 USER→403、有对应权限→成功；越权请求不得触达 Mapper。另测停用角色后旧令牌失效。

### F02 · P1 · 日志分析触发与日志数据入口允许匿名调用

- 证据：`backend/api/src/main/java/com/david/hlp/web/system/auth/filter/JwtAuthenticationFilter.java:44,75,89`；SecurityConfiguration.java:35；`backend/api/src/main/java/com/david/hlp/web/system/auth/controller/LogController.java:147`。
- 反例：不携带 Authorization POST `/api/v1/system/logs/analysis/trigger`。公开前缀 startsWith 放行，permitAll 再放行，控制器立即向 Spark 触发分析。GET 统计/健康也在同一前缀下；healthCheck:175 返回 sparkResponse，失败时 :181 返回原始异常文本。
- 影响：匿名消耗分析资源、读取访问统计/IP 聚合及内部响应/错误信息。具体公网可达性取决于未审计的部署网络，不影响应用入口缺少保护的结论。
- 已有保护不足：Nuxt 请求客户端要求 token 只约束浏览器代码；直接 HTTP 不经过它。未发现日志控制器本地鉴权。
- 最小方向：日志读需认证/相应权限，触发单独检查权限，公开健康最小化字段。
- 应验证：匿名及无触发权限不得调用下游；健康不泄漏上游响应和异常细节；失败状态不返回“成功触发”。

### F03 · P1 · 任意登录用户能删除其他用户的 Minio 对象

- 证据：`backend/api/src/main/java/com/david/hlp/web/minio/controller/ImageController.java:110`；`backend/api/src/main/java/com/david/hlp/web/minio/service/MinioService.java:156,162`。
- 反例：A 上传后得到 `AID_name.png`，B 持有效令牌请求 DELETE `/api/image/delete?fileName=AID_name.png`；控制器直接把该 key 交给 removeObject。同样，读取与签名 URL 只接收 key。
- 影响：跨用户对象删除；若对象预期私有，还存在跨用户读取/预签名分享风险（后者需产品确认，不作为独立确认漏洞）。
- 保护不足：上传拼接 userId 只定义名字；删除不验证归属，Minio 使用统一应用身份。
- 最小方向：在对象存取 module 内校验持久化所属用户或受控 key 命名；明确公开图片与私有对象规则。
- 应验证：B 删除 A 对象返回403且不调用 removeObject，A 正常删除；奇异文件名不能突破归属规则。

### F04 · P1 · 创建简历绑定客户端 id 后 save，可覆盖别人文档

- 证据：`backend/api/src/main/java/com/david/hlp/web/resume/controller/ResumeController.java:44`；`backend/api/src/main/java/com/david/hlp/web/resume/service/ResumeService.java:38,49`；`backend/api/src/main/java/com/david/hlp/web/resume/entity/Resume.java:25`。
- 反例：A 的文档 id 已知，B POST `/api/resume` 传 `{"id":"A的id","docType":"meta","content":"replacement"}`。createResume 只覆盖 userId/时间，MongoTemplate.save 对已存在 _id 执行替换，从而把 A 文档内容与归属改成 B。
- 影响：跨账户覆盖/夺取简历或同集合 commit，原数据可能丢失。
- 保护不足：读取/更新有 userId 条件，但 create 的第一次 save 之前没有归属查询，也没有清除 id 或只允许 insert。UUID repositoryId 不保护 Mongo _id。
- 最小方向：服务端构造 meta 文档、服务端生成 id、使用拒绝已有 id 的插入语义；不绑定版本控制内部字段。
- 应验证：客户端指定别人 id 不可替换；普通创建→列表→详情闭环可见；异常不得留下被覆盖文档。

### F05 · P1 · 版本控制按全局 commitId 读取，未检查简历及用户归属

- 证据：`backend/api/src/main/java/com/david/hlp/web/resume/service/ResumeVersionControlService.java:186,193,718`；ResumeVersionController.java:144 起 checkout 入口。
- 反例：B 对自己的合法 meta 调 checkout，commitId 填 A 的已有 commit。getResumeById 校验 B 的 meta，但 getCommitById 只过滤 `_id` 与 `doc_type=commit`，随后把 A 的内容复制到 B 文档。debug/commit 也复用该 helper。
- 影响：已知 commitId 时可跨用户读取内容并污染版本链。commitId 随机只减少猜测概率，不是授权。
- 保护不足：历史列表按 user_id 过滤，但按 id 的 helper 未过滤；检索了10个 helper 调用者，checkout/reset/debug 等接受外部提交标识，不能假设来自自身历史。
- 最小方向：在同一版本控制 module 的查询 seam 绑定 resumeId 与 userId，所有直接/递归 commit 读取复用归属规则。
- 应验证：同用户其他简历 commit 与其他用户 commit 均拒绝；正常分支历史仍工作，拒绝时 meta 不变。

### F06 · P2 · 简历创建不设置 meta 类型，保存标题不落库

- 证据：`backend/api/src/main/java/com/david/hlp/web/resume/service/ResumeService.java:28,38,71`；Resume.java:35 的 docType 没有默认值。
- 反例：正常 POST 只传 title/content，保存文档 docType=null；列表和详情仅查询 meta，创建成功后找不到。对已有 meta PUT 新 title 与 content，updateResume 只取 content 提交，不设置 title。
- 影响：新建后“消失”、改名刷新回退，破坏简历任务闭环。
- 保护不足：Mongo auditing 只补时间；前端适配不能保证所有调用者补内部 docType；历史修复记录与当前源码不一致。
- 最小方向：服务端创建强制 meta；更新明确保存允许修改的 title/content。
- 应验证：最小载荷创建→列表/详情可见；改名→重新读取仍是新标题；保留权限与版本历史。

### F07 · P2 · 城市参数被拼进 Spark SQL 表达式

- 证据：`backend/analytics/src/main/java/com/david/hlp/spark/service/User/ListBasicSet.java:52`；ListBasicSetController.java:25；`frontend/nuxt.config.ts:11` 直接代理 analytics。
- 反例：cityName 为 `x' OR 1=1 OR city_name='x`，生成表达式恒真，返回全城市职位而非精确匹配；合法带单引号城市值也可导致解析失败。
- 影响：查询含义被改变、筛选越界/故障。本轮不将其夸大为可执行任意 MySQL 写语句。
- 保护不足：前端 safeCity 只约束 UI；直接调用不会走该 allowlist。二维分析中的 col.equalTo 已是安全做法，但没有用于此入口。
- 最小方向：复用 typed Column/equalTo，参数始终作为数据。
- 应验证：引号和表达式样式字符串按字面匹配，无匹配返回空；不能扩大数据集。

### F08 · P2 · 薪资被整数截断，未知学历/经验混入“不限”

- 证据：`backend/analytics/src/main/java/com/david/hlp/spark/service/User/TwoDimensionalAnalysisChart.java:186,203,211,221,229,288`；`backend/analytics/src/main/java/com/david/hlp/spark/model/User/JobAnalysisData.java:22`。
- 反例：4-5K 返回整数4，真实中点4.5丢失；null 或不认识的学历/经验返回0，与显式“不限”合并。applyFilters 仅排除 null，无法再排除被改成0的未知数据。
- 影响：图表/均值偏移，误导招聘比较；前端保留小数/null 的测试无法修正服务端已经丢掉的信息。
- 最小方向：薪资 UDF→编码模型→JSON 保持 Double，未知维度保留null；0只表示明确不限。
- 应验证：4-5K 全链路为4.5；未知维度不参与对应轴分析；不限仍为0。

### F09 · P2 · 角色名无唯一约束，默认角色依赖不确定的 LIMIT 1

- 证据：`database/system.sql:64` role 仅主键；`backend/api/src/main/java/com/david/hlp/web/system/auth/mapper/RoleMapper.java:35`；RoleServiceImp.java:107,127,180。
- 反例：重复创建 USER（或把另一角色改为 USER）被接受；注册按 `WHERE role_name='USER' LIMIT 1` 选角色，没有唯一性与确定性保证。默认 USER 还可通过直接后端编辑改名；无引用时可删，导致新注册拿不到默认角色。
- 影响：新用户授予错误角色、注册失败、角色含义歧义。
- 保护不足：UI 禁止默认角色改名/删除，不约束直接接口；事务不会阻止并发相同名字插入；user外键仅保护被引用角色的删除。
- 最小方向：后端保护默认角色标识；审计存量数据后确立应用需要的唯一键，禁止直接对现库运行快照。
- 应验证：串行/并发重名均拒绝；默认角色不可改名/删除；正常角色不受影响；重复旧数据需有明确处置方案。

### F10 · P2 · Token Mapper 与初始化 DDL 的时间字段不一致

- 证据：`backend/api/src/main/java/com/david/hlp/web/system/auth/mapper/TokenMapper.java:18,29` 查询 created_at；`database/system.sql:127` 定义 create_time，没有 created_at。
- 反例：按仓库快照初始化空库后调用 getByToken 或 listValidTokensByUser，SQL 引用不存在的列，撤销/令牌读取失败。
- 影响：初始化 schema 与代码无法完整配套；这是静态确认的契约不一致，不声称当前运行数据库必然有同样字段。
- 保护不足：save 只写 token 字段可能成功，不能证明后续读取也可用；README明确快照而非增量迁移，没有自动同步证明。
- 最小方向：明确标准字段名并统一映射；对已存在环境提供增量升级路径。
- 应验证：隔离空库中所有 TokenMapper 语句可执行且时间映射正确，已有环境升级保留令牌记录。

### F11 · P2 · “退出登录”只清本地，服务端令牌仍有效

- 证据：`frontend/app/composables/useSession.ts:70`；`backend/api/src/main/java/com/david/hlp/web/system/auth/filter/SecurityConfiguration.java:42` 起；`backend/api/src/main/java/com/david/hlp/web/system/auth/LogoutService.java:33`。
- 反例：登录后保留令牌副本，点击侧栏退出；signOut 仅 clear+导航，没有请求撤销。全仓调用检索未见 LogoutService 注册为 Security logout handler，也无实现 /api/auth/logout 的 controller。
- 影响：用户认为已退出，但被复制的凭据可继续使用直至到期；跨标签清理只处理存储，不改变数据库有效性。
- 保护不足：存在 LogoutHandler 类不代表被调用；本地 timestamp 有效期不约束服务端。
- 最小方向：用现有令牌持久化机制接通服务端撤销，再定义撤销失败时的明确体验；不引入新认证系统。
- 应验证：退出后重放旧 token→401；无 token 退出幂等；退出失败不会虚报服务器已撤销。

### F12 · P2 · 恢复会话时一次临时失败会永久删除本地令牌

- 证据：`frontend/app/middleware/auth.ts:10`、`signed-in.ts:6`；`frontend/app/composables/useSession.ts:35` 的 Promise.all。
- 反例：已有有效令牌，刷新页面时 getRouters 返回503或超时。restore 抛错，任一 middleware catch 调 clear，删除 token 并跳登录；重试恢复已不可能，只能重新登录。
- 影响：后台短时故障导致大量正常用户被迫退出；本地清理还遮蔽实际故障。
- 保护不足：请求拦截器只对401触发 unauthorized，但外层守卫把所有异常都升级成失效；47项测试未覆盖 Vue 会话恢复集成。
- 最小方向：区分授权失效与瞬时失败；失败时阻断受保护界面并提供恢复/重试，保留仍可能有效的令牌。
- 应验证：401清理，503/超时保留token、不给未确认权限通行，重试成功恢复。

### F13 · P2 · 招聘读分析绕过采集模块的软删除约束

- 证据：`backend/crawler/src/main/resources/mapper/boss/JobDetailEntityMapper.xml:47` 起 deleteById 设置 is_deleted=1；`backend/analytics/src/main/java/com/david/hlp/spark/service/User/TwoDimensionalAnalysisChart.java:115` 和 ListBasicSet.java:25,47 直接读取 t_job_detail，未过滤 is_deleted。
- 反例：某职位通过 crawler mapper 软删除，常规getById/listPage不再返回；招聘分析仍从整表读取该行，继续计入城市/职位选项、样本/统计。
- 影响：已移除职位仍展示，分析口径与业务读取不一致。
- 保护不足：前端不知道 is_deleted 字段；Mapper 的条件不会自动传给 Spark JDBC。
- 最小方向：在现有 analytics 数据读取 module 统一生效记录范围，清楚区分历史分析与当前职位（当前入口没有历史口径声明）。
- 应验证：同一隔离数据集中活动行保留、软删除行从选项/样本排除；若产品要历史口径，需显式标注并独立验证。

### F14 · P2 · 登录响应包含密码哈希

- 证据：backend/api/src/main/java/com/david/hlp/web/system/auth/service/imp/AuthServiceImp.java:117 将 AuthUser 放入 Token；token/Token.java:17；entity/auth/AuthUser.java:79 公开 getPassword；controller/AuthController.java:119-126 缓存并返回 Token。JacksonConfig.java:33 无隐藏密码规则。
- 反例：正常登录的响应 data.authUser.password 可序列化出数据库密码哈希；该用户自己的哈希也没有返回客户端的业务必要。缓存同一 Token 对象扩大了敏感信息范围，实际 Redis 序列化未运行。
- 影响与保护：增加日志、浏览器插件或错误上报捕获哈希的机会；不是“凭此直接获得他人明文密码”。管理 User 的字段裁剪不保护 AuthUser。
- 最小修复：从返回及缓存对象排除密码，优先使用现有字段白名单或 Jackson 隐藏机制，不为此重建认证系统。
- 应验证：合成用户登录响应与缓存序列化不含 password，同时认证本身仍能读取哈希。

### F15 · P1 · 用户更新接口将密码原样写入数据库

- 证据：backend/api/src/main/java/com/david/hlp/web/system/auth/service/imp/UserServiceImp.java:54 直接 updateById；mapper/UserMapper.java:102 对非空 password 原样 UPDATE。addUser 则有编码，说明更新路径遗漏。
- 反例：有管理权限的合法调用者通过 updateUser 提交普通新密码，数据库被写成明文而登录仍用 passwordEncoder.matches，导致密码泄露及账号无法正常登录。当前越权使影响更大，但修复授权后此问题仍在。
- 保护：前端编辑会排除 password，不能约束其他调用者。
- 最小修复：若此 interface 不支持改密，应在后端拒绝密码字段；若支持则按现有编码和校验机制处理。不能静默原样持久化。
- 应验证：直接接口提交 password 时要么明确拒绝且不写库，要么编码后可正常登录；普通资料更新不得改动哈希。

### F16 · P1 · 菜单权限改名与客户端 children 可破坏共享授权

- 证据：backend/api/src/main/java/com/david/hlp/web/system/auth/service/imp/RouterServiceImp.java:61-72 改名后删除旧权限；:84-98 信任客户端 children 删除；database/system.sql:83-84 role_permission 级联删除，:116 router.permission 置空。
- 反例：两个路由共用同一 permission，改其中一个标识即删除旧 permission，另一菜单及所有引用角色失去授权。deleteRouter 还可带入不属于该父项的 children，删除无关路由。
- 影响：授权静默丢失。事务只保证错误操作一起提交，不能保证共享关系安全。
- 最小修复：服务端限制标识修改或显式迁移引用；删除前查询真实子项及引用，拒绝不安全删除。保留前端现有保护。
- 应验证：共享 permission 改动不影响其他路由/角色；伪造 children 无法删除无关项；拒绝时整个事务不变。

### F17 · P2 · 业务错误的 HTTP 状态统一被设成 500

- 证据：backend/api/src/main/java/com/david/hlp/web/common/entity/Result.java:71-99 三个 error 重载均构造 HttpStatus.INTERNAL_SERVER_ERROR；ResumeController.java:39 请求不存在记录时传 404 也进入该方法。GlobalExceptionHandler.java:39 起返回同一 Result。
- 反例：已认证用户请求不存在的简历，body code 可为404，而 ResponseEntity 状态仍为500。
- 影响：客户端和监控难以区分输入错误、资源不存在与服务故障。JWT filter 自己写出的401不受该方法影响，因此不接受“所有未认证响应都是500”的表述。
- 最小修复：在现有 Result/异常 module 内统一业务 code 到 HTTP 状态的映射，保留文件响应和已有 envelope；无须先拆掉所有 controller 的 interface。
- 应验证：对应400/401/403/404/409与真正500分开；合成 MockMvc 测试核对 HTTP 与 body，保持请求客户端兼容。

### F18 · P1 · Nuxt 对 analytics/crawler 的直接代理绕过主后端认证

- 证据：frontend/nuxt.config.ts:12-13 直接转发 /api/v1/boss/** 和 /api/v1/ai/**；backend/crawler/src/main/java/com/david/hlp/crawler/ai/controller/AIRequirementsController.java:21-23 直接执行匹配；RequirementAnalyzer.java:39 调用 AI；analytics 对应控制器无本地授权。两模块源码检索未发现 Spring Security filter/权限检查。
- 反例：匿名客户端向 Nuxt 的 /api/v1/ai/get-user-similarity 提交匹配载荷，应用代码路径可直达消耗 AI 资源的实现，不经过8080的 JWT filter。招聘读入口也有同样绕行路径。
- 影响与限制：绕过前端要求登录的行为，匹配请求可消耗外部 AI 预算；是否已公网暴露、下游是否可用取决于部署，未在生产请求。
- 最小修复：在现有实际请求 seam 统一认证并由下游验证可信身份；核对网络规则。不要假设只关闭8081/8082公网端口就能保护同源代理。
- 应验证：从 Nuxt 同源入口匿名请求被拒绝且不会调用 AI；合法用户和现有招聘查询继续工作。

### F19 · P2 · 招聘查询缺少服务端坐标轴和结果数量校验

- 证据：backend/analytics/src/main/java/com/david/hlp/spark/service/User/TwoDimensionalAnalysisChart.java:248-252 使用 col(xAxis/yAxis)；controller/User/CityAndPositonController.java:21 接受 Long limit；service/User/SalaryHotJob.java:125 拼入 LIMIT，未作上下限限制。
- 反例：直接请求不存在的轴列或负 limit 会进入 Spark 解析/执行错误；超大 limit 允许返回大量结果。Long 绑定不支持将其描述为任意字符串 SQL 注入。
- 影响：可避免的500和无约束资源消耗；具体容量未压测。前端 allowlist 无法约束直接 HTTP 调用。
- 最小修复：复用已确定的数值轴集合并在服务端限制 limit，非法值在查询前明确拒绝。无需新建可插拔查询框架。
- 应验证：未知轴、负数、零及超大limit有明确错误且不执行Spark；正常查询语义不变。

### F20 · P2 · 统计 interface 输出未经计算的示例指标

- 证据：backend/analytics/src/main/java/com/david/hlp/spark/service/Log/LogServiceImp.java:454-481 固定返回5.5、65.2、2.1、156、342、23；:413-421 将其放入概览/实时统计。
- 反例：日志数据变化时这些字段保持不变，却没有 unavailable/source 标记。消费完整统计 interface 的调用者无法区分示例与计算结果。
- 影响与修订：当前 frontend/app/lib/dashboard.ts:5-13、:90 起只投影访问量、访客、小时、路径、浏览器及方法；dashboard.vue 还声明未纳入在线人数/性能/安全实测展示。因此不接受“当前看板展示156在线用户”的指控，本项仅确认后端数据契约污染。
- 最小修复：无法计算的指标明确标记不可用或暂不发布；不为了填数而扩建一套分析系统。
- 应验证：合成不同日志时已实现指标随数据变化；未实现指标不能以固定值冒充实测；当前前端继续隐藏它们。

### F21 · P2 · 认证失败日志记录完整 JWT

- 证据：backend/api/src/main/java/com/david/hlp/web/system/auth/service/imp/UserDetailsServiceImpl.java:49、:78 将 jwt 直接写入 warn 日志。
- 反例：提交被撤销、过期或不合法的 token 后，完整凭据出现在日志文本中。
- 影响与限制：扩大凭据留存范围；这些分支记录的是认证失败 token，不能直接断言日志中的每个 token 都能被有效重放。
- 最小修复：去掉完整 token，保留请求关联信息和错误类型；确需排查时用不可逆摘要，不打印凭据前缀作为授权证明。
- 应验证：合成失败认证的日志中无完整 token，仍能关联请求和失败原因。

## 架构深化候选

### 深化授权与对象归属 module · Strong

文件：SecurityConfiguration · JwtAuthenticationFilter · User/Role/Router 操作 · Minio · Resume

问题：调用者必须知道菜单、对象key和角色状态的隐含规则，interface 很宽，depth不足。

方向：把现有请求与持久化 seam 的权限、归属规则放回后端 module，让 HTTP adapter 与测试跨相同检查。

收益：locality：安全规则集中；leverage：所有入口共用校验；interface 是 test surface。

deletion test：删除前端菜单不应解除后端授权；删除后端检查会把规则推回多个调用者，说明它有保留价值。

Before：菜单/确认 → 管理写入 → 直接HTTP → 管理写入 → 对象id/key → 持久化

After：HTTP / 测试 adapter → 授权与归属 module → 受保护的持久化操作

### 深化简历持久化 module · Strong

文件：ResumeService · ResumeVersionControlService · Resume · 两项controller

问题：create、meta、commit归属与版本规则散在多个入口，客户端可传内部字段。

方向：保留 Mongo adapter，在现有 seam 集中服务端字段、插入语义、归属与版本不变量。

收益：depth：一次调用承载完整不变量；locality：错误集中；leverage：CRUD与版本共享。

deletion test：删除业务版本实现会丢能力；删除重复裸查询并集中不变量才减少复杂性。不先造通用版本库。

Before：CRUD → 裸save → 版本操作 → 全局commit查询 → 调用者 → docType/head规则

After：现有调用者与测试 → 简历 module interface → 归属/类型/版本 implementation

### 统一招聘读取与投影 module · Worth exploring

文件：ListBasicSet · TwoDimensionalAnalysisChart · SalaryHotJob · JobAnalysisData

问题：有效记录、字面过滤与数值语义不一致，interface让调用者猜测0与未知值。

方向：在现有analytics module内部统一读取/投影 seam，沿用Spark adapter，不新增可插拔interface。

收益：locality：同一记录范围；leverage：多入口复用；depth：隐藏数值与筛选细节。

deletion test：删除重复配置可以减少复杂性；删除口径规则只会让每个调用者重新实现，规则应集中。

Before：城市职位 → 字符串过滤 → 二维分析 → 整数投影 → 采集删除 → 分析仍计入

After：现有读取入口 → 统一过滤/数值 module → 现有 Spark adapter

### 收敛会话与错误语义 module · Worth exploring

文件：useSession · 两项middleware · Result · LogoutService · TokenMapper

问题：临时失败、令牌失效、退出撤销和HTTP状态散在不同调用者，interface语义不一致。

方向：在已有会话及响应 seam 修正状态转换和撤销，保留当前envelope与错误处理能力。

收益：locality：一个状态归因；leverage：守卫与页面共享语义；interface可直接测试。

deletion test：删除共享状态规则会把异常分类复制到每个页面；删除无行为转发才有意义。不要重建认证系统。

Before：503 → clear token → 退出 → 只清本地 → 业务错误 → HTTP500

After：现有页面/HTTP adapter → 明确的状态与错误 module → 撤销 / 重试 / 正确状态

## 尚未验收与设计权衡

Mongo并发提交、多键Redis快照一致性、Spark驱动端collect容量、SQL实际约束/索引漂移、JWT同秒重复、SVG执行与对象命名、用户删除后的跨存储保留期仍需隔离环境验证。不给这些假设套确认缺陷数量。

所有11个页面入口均纳入静态覆盖，但简历API暂无对应完整页面、catch-all为明确重建占位；设置/个人资料按现有interface保持只读，公司列表/三维分析无可用闭环。它们属于产品能力边界，不自动构成本次应补写的功能。视觉、键盘与触摸体验只读模板与测试不足以验收。

CONTEXT.md与docs/adr当前不存在，不创建推测性的词典或ADR。SQL快照定位为初始化资料，禁止直接向现有库重导。修复若需唯一约束，必须先审计存量重复和兼容迁移；本次不执行。

## 原始材料与复核材料

- 第一轮：/tmp/bossanalyze-architecture-readonly-20261108-063459-yidm39_o.md
- 第二轮：/tmp/bossanalyze-arch-review-r2-20261108-4f7c9a.md（含被本报告排除或降级的原始主张，不宜直接当修复清单）
- 主审笔记：/tmp/bossanalyze-primary-review-notes-20261108.md
- Mongo映射自检：/tmp/bossanalyze-querymapper-407uk5_y/MappingCheck.java 与 result.txt

下一步可选择一个深化候选，再讨论约束、测试与落地范围；本次不实施重构。

未检查真实浏览器、后端运行时、实际库与生产网络；静态问题仍有部署与数据条件，报告不构成上线安全验收。
