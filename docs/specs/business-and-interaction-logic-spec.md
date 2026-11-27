# BossAnalyze 业务逻辑与交互逻辑规格书

## 当前 Vite 前端

前端采用 Vue 3、Vue Router、Vite 和 shadcn-vue，源码位于 `frontend/src/`。开发使用 `pnpm dev`，生产使用 `pnpm build` 生成 `dist/`，再由 `pnpm start` 启动 Node 服务。工具链为 Node.js 22.13+、pnpm 11.24.0。开发与生产共享同源 `/api/**` 代理及来源 IP 清理规则。AI v1/v2 请求转至 8081，其余请求转至 8080，招聘及日志经主 API 网关。见 [前端运行说明](../../frontend/README.md)和[代理入口](../../frontend/server/api-proxy.ts)。

- `/register` 已接入邮件验证码和注册。邮件发送依赖 SMTP。
- `/resumes` 已接入本人简历列表、编辑、版本历史及仓库维护。列表区分明确写入失败、结果未知以及受理后未能回读确认。版本页提供第一父链说明、快照确认和正文逐行对比。
- 编辑器以结构化 Markdown 预览正文，按账号、登录会话和简历暂存本地草稿。可在浏览器打印或另存 A4 PDF、下载独立 HTML 或 Markdown 源码。外部导出需配置 HTTPS 服务、经用户同意，并返回可打开的 HTTPS 结果。私有图片默认关闭，简历职位匹配保持隐私停用。
- `/console/logs` 已接入扩展日志读侧和手动触发。访问沿用看板授权，触发另需 `logs.analysis.trigger`。
- `pnpm test` 执行 `frontend/tests/*.test.mjs`，`pnpm type-check` 和 `pnpm build` 提供类型及生产构建检查。Playwright 单独运行，使用隔离 Mock。见 [前端测试说明](../../frontend/README.md#playwright-浏览器测试)。

以下 §1–§6 保留历史源码盘点，其旧路径、框架、功能状态和数量对应固定提交。§7 标注已落地的交互变更。

## 1. 概述、盘点基线与功能范围四象限

本文供产品设计、UI 重设计、前端重构和验收使用。历史盘点基线为 `06dc97735acb55ca56950797e88d2510a6295199`。行为依据为该提交的路由、调用链、控制器、数据模型与测试源码。「现有」表示历史基线实现，「可选」表示盘点时可独立立项。浏览器及部署验证项见第 6 节。下文文件路径均相对仓库根目录，API 表中的前缀与相对路由直接拼接。

| 运行组件 | 基线与职责 | 依赖与入口 |
| --- | --- | --- |
| `frontend` | Nuxt 4.6.3、Vue 3.5.44、TypeScript、Tailwind CSS 4.3.3、shadcn-vue / Reka UI、Hugeicons、VueUse、Axios 1.21.0。11 个路由文件、1 个全局错误页、1 个控制台布局、16 个业务组件、24 组 UI 原语（143 个 Vue 文件） | 开发 `127.0.0.1:5020`。生产由 `.output/server/index.mjs` 提供同源 Nitro 代理。65 个运行时 API 封装，25 个由当前页面及其共享链路调用，40 个未接入 |
| `backend/api`（Springweb） | 58 条业务 HTTP 映射。账号、会话、RBAC、用户/角色/菜单、简历版本、对象存储、日志与招聘网关 | 8080。MySQL `blog_data`、Redis、MongoDB `resumedb`、SMTP、S3/MinIO、Hadoop 或本地访问日志 |
| `backend/crawler`（SpringCrawler） | 2 条 HTTP 映射，均为已停用的简历匹配。保留旧采集解析、独立样本 CLI、职位分析调度与写回 | 8081。MySQL `boss_data`、Redis、主 API 的 Bearer 身份回查、Python 内部服务。职位分析调度和写回默认关闭 |
| `backend/analytics`（SpringSpark） | Spark 4.0.1，15 条 HTTP 映射。4 条基于 JDBC 的招聘查询，11 条日志分析、触发与健康路由 | 8082。MySQL `boss_data`、访问日志文件、Redis 快照、可选离线 MMDB |
| `ai-service` | FastAPI / Uvicorn，4 条显式健康或内部推理映射。职位提取调用上游推理服务，SQLite 记录每日 token 预算，默认 1,000,000 | 本地 `127.0.0.1:8090`。内部 Bearer 与配置就绪校验，简历匹配在读取正文前停用 |
| `backend/commons`（共用库） | `DownstreamAuthFilter`、`JobAnalysisReader`、`JobArrays`、`JobAnalysisFacts`、日志 DTO 与 Redis 键 | Java 模块共享，无独立 HTTP 服务 |

```text
浏览器 ─ 同源 HTTP ─ Nuxt/Nitro
  ├─ /api/v2/ai/** 和 /api/v1/ai/** ─→ crawler:8081
  │    ├─ 身份/权限回查 ─→ api:8080
  │    └─ 职位提取 ─→ Python 内部服务:8090 ─→ 上游推理
  └─ 其余 /api/** ─→ api:8080
       ├─ 登录与业务数据 ─→ MySQL / Redis / MongoDB / S3 / SMTP
       └─ 招聘和日志网关 ─→ analytics:8082 ─→ MySQL / 日志文件 / Redis
```

Nitro 在路由规则代理前执行 `request` hook，剥离传入的 `Forwarded`、`X-Real-IP`、全部 `X-Forwarded-*` 和 `X-Boss-Client-IP`。仅对精确匹配 `BOSS_TRUSTED_PROXY_IPS` 的 socket 对端接受一个合法、不含 zone ID 的 `X-Forwarded-For` IP 字面值，否则回退到合法 socket 对端。以计算值重建 `X-Boss-Client-IP`，保留 `Authorization` 等其余请求头。前端产物需运行 Node 服务以维持该边界。见 [Nuxt 配置 · `frontend/nuxt.config.ts`](../../frontend/nuxt.config.ts)、[Nitro hook · `frontend/server/plugins/client-ip.ts`](../../frontend/server/plugins/client-ip.ts)、[IP 归一化 · `frontend/server/utils/client-ip.ts`](../../frontend/server/utils/client-ip.ts)、[本地运行说明 · `docs/local-development.md`](../local-development.md)。

| 象限 | 能力 | UI 重设计验收边界 |
| --- | --- | --- |
| ① 现有页面和交互 | 根跳转、登录、工作台、数据看板、用户、角色、菜单、设置、个人中心、招聘数据分析、授权占位页、全局错误页 | 全量保留业务行为、入口、授权判断、反馈、重试和可访问替代。视觉与信息组织可变 |
| ② 后端有契约，页面未接入 | 邮箱注册、简历 CRUD 与版本控制、外部简历导出、图片存储、扩展日志分析及手工触发 | 可独立规划。按第 4、5 节检查安全边界、服务可用性和不可逆动作 |
| ③ 停用、CLI 或无服务端能力 | 简历职位匹配。旧 BOSS 定时采集。`RecruitmentSampler`。自助改资料/改密、Refresh Token 刷新、收藏、投递、聊天、批量写操作、站内导出任务 | 保持停用或无入口。职位来源徽章只呈现可读结果。CLI 能力不转译为站内控制面板 |
| ④ 无后端映射 | `updateRolePermissions`、`updateUserRole`、`company-list`、三维分析、版本 `init` | 新页面忽略，清理需单独变更。替代契约见 §5.3 |

## 2. 全局壳层、路由守卫与会话/权限状态机

### 2.1 渲染、导航与主题

| 场景 | 当前行为与持有状态 |
| --- | --- |
| 渲染 | `/console`、`/console/**`、`/recruitment` 为 CSR。登录保留 SSR，客户端挂载后读取存储。根页重定向工作台 |
| 壳层 | 同一控制台布局承载控制台和招聘。固定品牌、工作台和招聘入口，动态树由会话生成。有子项的节点是展开触发器，点击折叠侧栏中的分组会展开侧栏。当前路径命中后自动展开对应分组，叶子按路径高亮，移动端选择链接后关闭面板。图标使用已安装的白名单映射，未知标识使用路径图标或默认图标 |
| 顶栏与账号 | 顶栏侧栏触发器、竖分隔线、面包屑。招聘只显示本页标题，控制台层级项在 `md` 以上显示。`page:finish` 聚焦主内容。账号菜单显示姓名、邮箱、姓名首字头像与退出。移动向下、桌面向右打开 |
| 侧栏 | 桌面 16rem，图标折叠 3rem，移动 Sheet 18rem。移动媒体查询 `max-width: 767px`。模态开闭独立于桌面折叠。桌面状态写 `sidebar_state` Cookie（路径 `/`、7 天），初始默认展开。`Ctrl+B` / `⌘+B` 切换，现有监听不区分输入焦点 |
| 编辑及详情面板 | 用户/角色/菜单与职位详情使用 Sheet。移动查询 `max-width: 639px`，底部 80dvh，桌面从右侧进入且最大 600px。面板主体独立滚动，标题、关闭与关键操作可达。放弃草稿、清空授权和删除确认使用 Dialog |
| 主题与辅助技术 | 全局 Tailwind 语义 token 映射 `background/foreground`、`card`、`primary`、`muted`、`destructive`、`border/input/ring`、`chart-*`、`sidebar-*` 和圆角变量。默认 Roboto / sans-serif，基础圆角 `--radius: 0.45rem`。存在 `.dark` 覆盖，壳层没有主题切换入口。保留「跳过导航」和 `main#main-content[tabindex="-1"]`，边栏、下拉、搜索选择器和模态均应可键盘操作 |

依据 [控制台布局 · `frontend/app/layouts/console.vue`](../../frontend/app/layouts/console.vue)、[侧栏 · `frontend/app/components/AppSidebar.vue`](../../frontend/app/components/AppSidebar.vue)、[动态菜单项 · `frontend/app/components/NavMenuItem.vue`](../../frontend/app/components/NavMenuItem.vue)、[账号菜单 · `frontend/app/components/NavUser.vue`](../../frontend/app/components/NavUser.vue)、[侧栏状态 · `frontend/app/components/ui/sidebar/SidebarProvider.vue`](../../frontend/app/components/ui/sidebar/SidebarProvider.vue)、[侧栏常量 · `frontend/app/components/ui/sidebar/utils.ts`](../../frontend/app/components/ui/sidebar/utils.ts)、[主题 · `frontend/app/assets/css/tailwind.css`](../../frontend/app/assets/css/tailwind.css)、[图标映射 · `frontend/app/lib/menu-icons.ts`](../../frontend/app/lib/menu-icons.ts)。

### 2.2 认证与会话

```text
保护路由 → 读取 token 与本地时间戳
  缺失/无效/到期 → 清理本机状态 → /login?redirect=安全目的地
  有效且同 token 已 ready → 守卫判定
  有效但未 ready → 并行读取基本资料 / 权限字符串 / 全量树
    成功且 token 仍相同 → 生成授权树 → ready → 守卫判定
    期间换 token → 按当前 token 重新恢复
    当前请求 401 → 失效清理 → 登录（reason=expired）
    其他失败 → 503 恢复页，保留凭据。重试走完整守卫
```

- localStorage 保存裸 `token` 和毫秒 `token_timestamp`。时间戳必须为有限正数、不晚于当前时刻，年龄严格小于 24 小时。会话缓存只复用于同一 token 的 ready 状态。清理时清 `token`、`token_timestamp`、旧 `cachedRoutes` 及 sessionStorage 同名时间戳。恢复接口为 `getUserBaseInfo`、`getUserPrivateInformation`、`getRouters`。基本资料须含字符串邮箱，姓名缺失回退邮箱。旧请求报错或成功均按 token 归属处理。
- 登录先清本机旧状态，再提交邮箱和密码。取到非空字符串 `token` 后写入新时间戳并恢复会话。服务端要求启用用户和启用角色，按令牌记录、签名与有效期校验 Bearer，并回查当前角色和授权。菜单权限变更以服务端为准。客户端 ready 树可暂存旧可见性。客户端 24 小时校验和服务端令牌生命周期是两层约束。
- 主动退出先执行 `router.replace('/login')`。取消丢弃草稿即取消导航和撤销。确认离开后，仅当发起退出的 token 仍是当前 token 时请求 `POST /api/auth/logout`。成功后清本机状态，撤销请求失败也清本机并改为 `/login?logout=unconfirmed`。旧退出完成时保留期间新登录的 token。撤销路由可无 Bearer 调用。带当前 Bearer 时按该令牌值发起撤销。
- 强退先置 `invalidated` 并清本机，再导航，绕过草稿保护且跳过服务端注销。当前 token 的 API 401 跳 `/login?redirect=…&reason=expired`，登录页上的全局事件跳转被抑制。**用户编辑自己、修改/授权当前角色**的 UI 传 `signOut(true)`，目标为无 query 的 `/login`。当前角色授权写入后回读未确认也有此保守处理。
- 请求层仅在 HTTP 401 对应实际发送的 Bearer 与当前 token 一致时移除 token 并派发 `unauthorized`，注销请求自管失败。401 与 403、业务密码校验、服务中断独立。跨标签页收到 `storage` 的 `key=token` 事件会调用整页刷新，草稿可能丢失。
- 安全回跳只接受站内 `/console`、`/console/*`、`/recruitment` 的字符串路径，可保留 query/hash。拒绝外部源、协议相对 URL、反斜线、空白及编码斜杠/反斜线，检查规范化后的 pathname。登录后目的地还需在授权树中精确匹配（两个固定入口除外），否则回工作台。禁止直接把原始 query 赋给导航目标。

依据 [会话 composable · `frontend/app/composables/useSession.ts`](../../frontend/app/composables/useSession.ts)、[客户端事件 · `frontend/app/plugins/session.client.ts`](../../frontend/app/plugins/session.client.ts)、[会话与回跳 · `frontend/app/lib/session.ts`](../../frontend/app/lib/session.ts)、[Axios 请求层 · `frontend/app/utils/request/request.ts`](../../frontend/app/utils/request/request.ts)、[用户编辑页 · `frontend/app/pages/console/users.vue`](../../frontend/app/pages/console/users.vue)、[角色页 · `frontend/app/pages/console/roles.vue`](../../frontend/app/pages/console/roles.vue)。

### 2.3 授权树、导航树与服务端判定

| 边界 | 处理规则 |
| --- | --- |
| `auth` 守卫 | 恢复会话后放行 `/console`，其他控制台路径须在授权树中按 pathname 精确查到。已登录但路径无授权，进入全局 403 |
| `signed-in` 守卫 | 招聘只需恢复会话成功，不读取「招聘菜单授权」来做二次门槛。两个守卫对暂时性恢复失败均返回标记 `sessionRestore` 的 503 |
| 原始管理数据 | `getRouters` 向登录用户返回全量已构建树。管理与授权操作保留停用、隐藏和操作类型记录。不得拿脱敏后的侧栏树重写角色或菜单 |
| 授权树 `menuTree` | 按 `menuOrder` 排序，递归。非启用 `status !== 1`、`meta.type === 'F'`、`meta.hidden === true`、缺少已声明的权限、非法路径或节点数据会剔除整枝。空权限标识本身不做权限匹配。无路径节点可作为分组，空分组退出。标题优先 `metaTitle`。相对路径按父路径解析，旧路径归到 `/console`，拒绝外部、歧义编码和保留路径 |
| 可见导航树 `navigationMenus` | 在授权树之上递归剪掉 `meta.metaHidden === true` 的枝。该枝仍留在授权树，能直达。快捷入口取可见叶子，排除 `/console`、空路径并按路径去重 |
| 服务端 `JwtAuthenticationFilter` / `ConsoleAccess` | 对每次受保护的请求重新回查身份及授权。页面权限检查按当前授权字符串匹配启用、非空权限的路由行，支持 `/console/<page>`、`/<page>`、`<page>`。该校验不重走客户端树与祖先/导航隐藏规则，API 分组见 §4.4。登录、会话撤销、注册等例外见接口表 |

依据 [控制台守卫 · `frontend/app/middleware/auth.ts`](../../frontend/app/middleware/auth.ts)、[登录守卫 · `frontend/app/middleware/signed-in.ts`](../../frontend/app/middleware/signed-in.ts)、[树语义 · `frontend/app/lib/session.ts`](../../frontend/app/lib/session.ts)、`backend/api/src/main/java/com/david/hlp/web/system/auth/filter/{JwtAuthenticationFilter,ConsoleAccess}.java`。

### 2.4 反馈与草稿保护

| 事件/状态 | 当前反馈 |
| --- | --- |
| 权限拒绝、路由不存在、其他全局错误 | `app/error.vue` 分别展示 403、404、通用标题，提供回工作台及前往登录。带恢复元数据的 503 提供回原目标并重新恢复。当前凭据保留 |
| 局部请求或表单 | 页面/区域骨架和忙态，空数据有单独提示，403 用权限专用文本，服务失败给重试。保存失败在表单内呈现。字段校验触发后定位首个错误。成功提示使用页面内 `role="status"`，错误使用 `role="alert"` 或相应提示。没有全局 Toast 总线 |
| 用户、角色、菜单脏草稿 | 打开时从记录建立初始快照，按序列化后的内容判断 dirty，详情只读。点击遮罩不关闭编辑面板。关闭按钮、Escape、路由离开走统一确认。取消保留输入和当前路由。确认丢弃后关闭，等待更新再前往目标，目标 `/login` 时重新走主动退出 |
| 提交期间 | 屏蔽重复提交、关闭及普通跳转。dirty 或正在提交时 `beforeunload` 发出浏览器提示。`invalidated` 优先解除拦截。读回未确认时以实际读回流程解锁写操作，细节见角色/菜单页 |

依据 [全局错误页 · `frontend/app/error.vue`](../../frontend/app/error.vue)、[用户编辑 · `frontend/app/components/users/UserEditor.vue`](../../frontend/app/components/users/UserEditor.vue)、[角色面板 · `frontend/app/components/roles/RolePanel.vue`](../../frontend/app/components/roles/RolePanel.vue)、[菜单面板 · `frontend/app/components/menus/MenuPanel.vue`](../../frontend/app/components/menus/MenuPanel.vue)。

## 3. 现网全量页面业务逻辑与交互规格

### 3.1 根路由 `/`

**入口与链路**　访问根路由直接替换到 `/console`，由控制台守卫继续处理。无独立内容与 API 状态。未登录、会话故障、成功分别转登录、全局恢复页或工作台。见 [根路由 · `frontend/app/pages/index.vue`](../../frontend/app/pages/index.vue)。

### 3.2 登录 `/login`

**入口与界面**　公开、SSR。品牌与邮箱、密码表单，桌面 `lg` 及以上显示装饰图，手机单列表单。邮箱 `type=email`、`required`、`autocomplete=username`，密码为掩码、`autocomplete=current-password`。邮箱 trim，密码原样提交。账号或权限指向联系管理员。注册、找回密码、第三方登录无入口。

| 阶段 | 操作、状态与失败处理 |
| --- | --- |
| 初始 | 挂载只检查是否有本机 token 以显示「重试恢复会话」。已有 token 时允许主动重试，页面不自动跳走。显示过期或服务器撤销未确认反馈，行内表单错误优先 |
| 提交 | 锁定输入与操作、标注忙态，`POST /api/auth/login` 后执行 §2.2 恢复。成功清密码并按安全目标替换导航 |
| 失败 | 有已取得凭据时允许重试恢复。无凭据时 401 提示邮箱或密码不正确，502/503/504 提示服务连接，其他使用中性失败文案。当前错误映射按 §4.5 执行，旧形状或其他 500 响应使用中性文案 |
| 重试恢复 | 恢复成功走安全回跳。失效要求重新登录。暂不可用在表单内提示，保留有效凭据与重试入口 |

见 [登录页 · `frontend/app/pages/login.vue`](../../frontend/app/pages/login.vue) 与 [登录表单 · `frontend/app/components/LoginForm.vue`](../../frontend/app/components/LoginForm.vue)。

### 3.3 工作台 `/console`

**入口与初始化**　固定登录入口。会话提供欢迎区、授权树与快捷入口。挂载、整页刷新并行加载角色+权限、授权时加载日志。按钮在任一刷新中处于忙态。

| 区块 | 数据、操作及状态 |
| --- | --- |
| 日志概览 | 授权树存在 `/console/dashboard` 才调用看板接口。读取访问次数、独立访客、峰值小时访问量与快照时间，提供进入看板与局部重试。无授权直接呈现受限。403、失败、204 无快照、真实零值分别处理 |
| 快捷入口 | 来源为可见授权叶子。保留图标、标题、可达路径。无条目显示空态。招聘独立固定在侧栏 |
| 角色与权限 | 并行 `getUserRole`、`getUserPrivateInformation`。显示角色名称/状态、权限数及概览。权限去重排序。零权限单独呈现。加载、403、失败与重试不依赖日志请求结果 |

见 [工作台 · `frontend/app/pages/console/index.vue`](../../frontend/app/pages/console/index.vue)、[账号数据投影 · `frontend/app/lib/settings.ts`](../../frontend/app/lib/settings.ts)。

### 3.4 数据看板 `/console/dashboard`

**入口与初始化**　页面菜单授权，挂载/刷新读 `GET /api/v1/system/logs/dashboard/stats`。生成时间支持毫秒时间戳与有效时间字符串。有时区值按 UTC 显示，无时区值标注「服务端时间，时区未提供」。

| 区块/状态 | 契约与交互 |
| --- | --- |
| 4 KPI | 访问次数 `overview.totalVisits`、独立访客 `overview.uniqueVisitors`、全快照小时峰值、全快照访问量大于零的小时数 |
| 访问趋势 | 按 `yyyy-MM-dd HH` 排序取最后 24 个有记录小时桶，缺失小时无补零。柱图以最近桶为默认高亮，悬停、聚焦或点击小时按钮更新读数，并有同源 `<details>` 表格。全局 KPI 使用全部桶 |
| 分布和榜单 | 浏览器和 HTTP 方法按次数降序，分布条占比按该组展示数据求和。热门访问路径按次数取最多 10 行。各区块独立支持空数据 |
| 全页状态 | 初始骨架、刷新忙态、403 权限不足、错误可重试、204 等待快照、200 合法快照。200 空体、缺结构、非法日期或负/非整数计数归错误。合法 `0` 参与计算并照常显示 |

见 [看板 · `frontend/app/pages/console/dashboard.vue`](../../frontend/app/pages/console/dashboard.vue)、[投影 · `frontend/app/lib/dashboard.ts`](../../frontend/app/lib/dashboard.ts)、[访问图 · `frontend/app/components/dashboard/VisitChart.vue`](../../frontend/app/components/dashboard/VisitChart.vue)、[分布卡 · `frontend/app/components/dashboard/DistributionCard.vue`](../../frontend/app/components/dashboard/DistributionCard.vue)。

### 3.5 用户管理 `/console/users`

**入口与初始化**　`users` 页面授权。并行读用户页和 `GET /api/role/getRoleList`，角色源失败/为空独立提示并限制角色相关编辑。用户列表仍可读取。

| 区块/动作 | 契约与状态闭环 |
| --- | --- |
| 查询表 | 姓名关键字、角色、启用状态。查询将草稿写入筛选快照并回第一页，重置清条件。服务端按创建时间降序，姓名为包含匹配，角色和状态按等值匹配。刷新按已应用条件。列表展示姓名、邮箱、角色、状态等，空态区分全量无数据与条件无结果，403 与请求/响应失败可独立处置 |
| 分页 | 默认第 1 页、每页 10，提供 10/20/50。请求体 `{number,size,query:{name?,roleId?,status?}}`，**`number=(page-1)*size+1`**。服务层传 `number-1` 给 SQL OFFSET。总量来自 `totalElements`。页大小变更回第 1 页，删除导致越界时夹回末页再查询 |
| 详情 | 由列表行打开只读 Sheet，包含用户 ID、姓名、邮箱、角色、状态、注册/创建日期、最近登录/IP、地址、简介。安全投影不透出密码、头像及未知字段。空/未知字段明确回退 |
| 新增/编辑 | 字段为姓名（trim 后 1–50）、有效邮箱（≤254）、角色 ID、状态 0/1、地址/简介（≤255）。新增默认启用并需初始密码 8–64 字符。编辑带 id 且不传密码、头像或时间。选项 ≤5 用单选，>5 用可搜索 Combobox（筛选器的「全部角色」也计入选项数）。停用角色带状态标签，回填原值。提交成功清密码、关闭、回读列表。新增回首页。编辑自身按原记录邮箱与当前会话邮箱比较，成功后强退 |
| 删除 | 自己的删除入口禁用。弹窗绑定目标，输入**操作者**密码，发送 `{id,password}`。空密码不可提交，提交锁定，失败留弹窗，关闭/目标切换清理密码。HTTP 400 正文 `code=1006` 允许就地重试。成功关弹窗并重读当前列表。服务端验证操作者密码后按目标 id 删除。禁止自删由当前页面控件落实，服务端没有同等判断 |

见 [用户页 · `frontend/app/pages/console/users.vue`](../../frontend/app/pages/console/users.vue)、[分页及表单投影 · `frontend/app/lib/users.ts`](../../frontend/app/lib/users.ts)、[角色选择器 · `frontend/app/components/users/RolePicker.vue`](../../frontend/app/components/users/RolePicker.vue)、[用户编辑 · `frontend/app/components/users/UserEditor.vue`](../../frontend/app/components/users/UserEditor.vue)、[删除确认 · `frontend/app/components/users/DeleteUserDialog.vue`](../../frontend/app/components/users/DeleteUserDialog.vue)。

### 3.6 角色管理 `/console/roles`

**入口与初始化**　`roles` 页面授权。并行读 `/api/auth/getRoleList`（含路由和权限）、当前角色 `getUserRole`、原始树 `getRouters`。三者状态独立。当前角色未确认时锁写。树未就绪影响授权。主列表 403、失败、加载、全量空和筛选空分别反馈。

| 区块/动作 | 契约与状态闭环 |
| --- | --- |
| 列表与详情 | 角色按 ID 降序。名称关键词和状态本地筛选，查询/重置回首页。10/20/50 本地分页。详情显示名称、ID、状态、备注及授权摘要。当前角色标识用于写入后的重认证 |
| 新增/编辑元数据 | 名称 1–255（trim），备注 ≤255，状态 0/1，新建默认启用。只发 `roleName,status,remark`，编辑加 `id`。默认 `USER` 名称在面板只读，删除禁用。状态控件仍可选择停用，但服务端拒绝停用、改名和删除，保护大小写变体。写入 `code=200` 后刷新列表，自编辑当前角色走强退 |
| 授权初始 | 使用管理全树，保留停用、隐藏和操作节点。初始选中来自 `routers[].id` 或同名 `permissions[].permission`。ID 必须唯一且可表示为正的 32 位整数。已有权限或路由 ID 无法在树中表示时，展示缺项并锁定覆盖 |
| 闭包与清空 | 勾选节点补齐全部祖先及共享同一权限字符串的别名，迭代到稳定。取消节点移除该枝、别名及相关后代，再求剩余闭包。勾父不自动勾子。已有权限且将权限集合置空时追加确认 |
| 保存与确认 | `POST /api/auth/updateRoleRouters` 发送 `{roleId,routerIds}`。服务端由路由 ID 得出权限关系并替换。客户端确认业务成功后重读完整角色列表，按排序去重后的权限字符串集合比对。写请求失败可修正重试。已接受但缺角色、失败或不一致则标「已提交、未确认」，锁住重复授权，提供重新读取。当前角色成功或已写未确认均强退 |
| 删除 | 禁止操作默认角色与当前角色，确认弹窗防重复。发 `{id}`，成功重读。失败保留弹窗。角色删除涉及角色权限关系和角色记录。已有用户引用时，基线外键可拒绝删除，事务回滚角色授权变更 |

见 [角色页 · `frontend/app/pages/console/roles.vue`](../../frontend/app/pages/console/roles.vue)、[角色面板 · `frontend/app/components/roles/RolePanel.vue`](../../frontend/app/components/roles/RolePanel.vue)、[授权树项 · `frontend/app/components/roles/PermissionBranch.vue`](../../frontend/app/components/roles/PermissionBranch.vue)、[角色投影/闭包 · `frontend/app/lib/roles.ts`](../../frontend/app/lib/roles.ts)。

### 3.7 菜单管理 `/console/menus`

**入口与初始化**　`menus` 页面授权。拉全量树并验证 id 去重、父子一致、字段类型。兄弟按 `menuOrder` 后按 id 排。目录 `C`、菜单 `M`、操作 `F` 同表管理。加载、权限不足、树非法/读取失败、空树和无匹配分别显示，失败可重试。

| 区块/动作 | 契约与状态闭环 |
| --- | --- |
| 树筛选 | 按显示名称、路由名称、路径、权限键进行不区分大小写的关键词匹配，另有状态条件。保留命中节点和祖先链，查询时展开命中链。无关兄弟不显示，清条件恢复用户折叠选择。未筛选时可逐枝展开/折叠 |
| 详情与表单 | 显示基本信息、路由信息、可见性元数据。可建根与允许的子项。根 `pid=null`，操作节点不承载子项，有子项的节点不能改为操作。前端也拦截 parent 的 `pid >= id`（非根），避免当前构树顺序无法返回新子项。编辑锁定 `pid`、`permission`，保留 `metaRoles`，不回传 `children`。必填标题、名称、路径、权限键 1–255，组件、备注、重定向、图标 ≤255。排序为有符号 32 位整数，状态 0/1 |
| 字段效果 | 生效项为标题、路径、图标、排序、状态、节点/导航隐藏。组件标识、重定向、始终显示、缓存和历史角色元数据不驱动当前 Nuxt 路由。新增默认 `M`、启用、排序 0、隐藏开关关 |
| 新增/编辑 | 路由名称在全树查重，新增权限键不得与其他菜单重复。新增前读取完整角色表，若任意角色已持该键则阻止，避免继承旧授权。提交后读取树，新增按名称+权限键定位，编辑按 id 定位，比较持久化字段。成功更新列表。已提交未确认时锁定保存，提供重新读树。请求失败保持草稿 |
| 删除 | 弹窗打开和点击确认**各并行重读树与角色**。目标消失、存在孩子、缺权限键、其他菜单共享该键、任意角色仍引用该键或依赖读取失败均阻断。确认时使用最新持久数据，写后重读确认目标消失。请求失败可重试，写入后无法确认则提示并刷新，阻止直接重复删除。服务端锁定并重查引用，在事务内删除菜单及可安全移除的权限记录。请求按基线发送 id、名称、权限键与空 `children`，服务端以锁定后的持久记录为准 |
| 范围 | 编辑结果更新管理树。会话恢复按 token 缓存授权树，侧栏/当前通行权的客户端呈现可能需要重新恢复才同步。新菜单有授权且没有实际静态页面时进入占位页 |

见 [菜单页 · `frontend/app/pages/console/menus.vue`](../../frontend/app/pages/console/menus.vue)、[菜单面板 · `frontend/app/components/menus/MenuPanel.vue`](../../frontend/app/components/menus/MenuPanel.vue)、[菜单投影与预检 · `frontend/app/lib/menus.ts`](../../frontend/app/lib/menus.ts)。

### 3.8 设置 `/console/settings`

**入口与初始化**　`settings` 页面授权。分类导航指向 `#account-access` 与 `#service-status`。账户访问区依据会话授权树，存在 `/console/profile` 才呈现个人中心入口。进入后自动读健康检查，刷新与错误重试使用同一路径。

| 状态 | 反馈及契约 |
| --- | --- |
| 加载 | 健康区骨架与忙态 |
| 200，`status=UP, sparkService=UP` | 已连通，展示检查时间 |
| 数据适配器收到合法 `DEGRADED/DOWN` | 可显示「连接降级」及检查时间。实际网关失败时以非 200 返回这组字段，共享请求层拒绝后进入下方失败状态 |
| 403、请求失败、异常形状 | 权限不足或检查失败及重试。合法检查时间为正的 epoch 毫秒。前端仅呈现投影后的连通性，隐藏原始内部错误。成功检查时间按浏览器本地时区显示 |

该请求 `GET /api/v1/system/logs/health` 只要求登录。它检查 Spark 日志接口连通性，一个正常响应不能推断招聘服务、数据完整性或快照新鲜度。见 [设置页 · `frontend/app/pages/console/settings.vue`](../../frontend/app/pages/console/settings.vue)、[健康数据投影 · `frontend/app/lib/settings.ts`](../../frontend/app/lib/settings.ts)。

### 3.9 个人中心 `/console/profile`

**入口与初始化**　`profile` 页面授权。可能由设置或已配置的菜单进入。挂载与刷新并行读取资料、角色权限两个区域。角色权限区内部并行读角色与权限。

| 区块 | 数据及状态 |
| --- | --- |
| 账号摘要和基本资料 | `getUserBaseInfo` 投影 `id,name,email,roleId`，显示姓名首字回退、邮箱、ID、关联角色 ID 及缺失字段。账号区域加载、403、响应失败分别显示，可局部重试 |
| 角色与访问 | `getUserRole` 与 `getUserPrivateInformation` 投影角色 ID、名称、状态、备注，权限去重排序并展示数量。清单支持展开/收起，零权限呈现专门说明。错误不阻断资料。可局部重试 |
| 可写性 | 基线为只读资料页，提供联系管理员的说明。管理侧用户修改接口仍要求 `users` 权限。无本人改资料或改密路由 |

见 [个人中心 · `frontend/app/pages/console/profile.vue`](../../frontend/app/pages/console/profile.vue)、[账号数据投影 · `frontend/app/lib/settings.ts`](../../frontend/app/lib/settings.ts)。

### 3.10 招聘分析 `/recruitment`

**入口与初始化**　固定侧栏入口，`signed-in`，与控制台共享壳层。挂载并发读城市、全部城市的职位、默认 Top 10 排行。二维分析等用户主动提交，从 idle 开始。

| 区块/动作 | 契约与状态闭环 |
| --- | --- |
| 筛选 | 草稿默认城市 `all`、职位 `all`、X 薪资、Y 学历。搜索选择器，城变先清职位为 `all` 再拉新选项。请求序号丢弃旧城市响应。原始选项去空、去重、过滤与 `all` 冲突项。城市 ≤255 字符，排除单引号、反斜线、控制字符。无效项展示为禁用。职位/城市值须存在于所读列表或为 `all` |
| 分析请求 | 轴允许 `salary_value`、`degree_value`、`experience_value` 且不同。职位读取中、分析中限制提交。验证通过后复制 `applied`，清旧结果/详情并回首页。草稿改动提示尚未应用，结果标题与坐标用 `applied`。重置只改草稿。当前「重试当前条件」再次读取草稿，可能换掉失败条件 |
| 结果状态 | 未查询、骨架、403/查询失败（可重试）、合法空结果、合法结果。城市、职位、排行各自有加载/错误/重试。三项皆为一般错误时显示总体提示。二维查询与排行互不覆盖 |
| 3 指标 | 返回的样本条数、非空公司名称去重数、有效薪资样本的平均值并展示其样本量。缺薪资用未知，零值保留。服务端已滤掉所选轴缺值的行，指标属于该返回集 |
| 散点图 | 按坐标聚合计数。原生 SVG 最多取 1000 组（按组索引等距选取），坐标范围和有效计数用全量组。重合点半径为 `min(8, 3 + log2(count))`，悬停可读数量，同轴范围相等时居中。读屏摘要/可展开坐标明细，明细保留全部组，每页 20，查询结果或轴变化回第一页 |
| 样本与详情 | 本地分页，默认 20 条，可选 10/20/50。以 `positionId:index` 标识样本，支持重名和重复职位分类码。职位详情 Sheet 展示公司、城市、薪资、学历、经验、规模、融资、地址、福利、要求和分析来源。缺值明确显示。公司和职位来源分别校验，只显示无内嵌账号密码的绝对 HTTP(S) 链接，打开新窗口使用 `rel="noopener noreferrer"`。9 个已知状态及未知回退见 §4.2，来源标识/版本可为空。除 `complete/no_evidence/unparsed` 外，额外说明详情条目未由当前来源的成功分析确认 |
| 高薪排行 | 独立全局查询，Top 5/10/20 可切换，默认 10。显示职位、样本量、最低/最高/平均统一用 K、推荐城市的薪资和样本数。无推荐城市保留未知。加载、错误、403、空榜及重试都在排行区域 |

见 [招聘页 · `frontend/app/pages/recruitment.vue`](../../frontend/app/pages/recruitment.vue)、[查询/样本投影 · `frontend/app/lib/recruitment.ts`](../../frontend/app/lib/recruitment.ts)、[选择器 · `frontend/app/components/recruitment/OptionPicker.vue`](../../frontend/app/components/recruitment/OptionPicker.vue)、[散点图 · `frontend/app/components/recruitment/ScatterPlot.vue`](../../frontend/app/components/recruitment/ScatterPlot.vue)、[职位详情 · `frontend/app/components/recruitment/JobSheet.vue`](../../frontend/app/components/recruitment/JobSheet.vue)。

### 3.11 授权占位路由 `/console/[...slug]`

**入口与链路**　静态页面未匹配时进入 catch-all。守卫恢复后若授权树中有匹配节点，展示对应标题和「此功能尚未开放」，可回工作台。否则全局 403。此页无业务请求、加载或写状态。见 `frontend/app/pages/console/[...slug].vue`。

### 3.12 全局错误页


**入口与状态**　Nuxt 全局错误边界。403 显示访问不足，404 显示页面不存在，其余显示暂时无法加载。展示状态码。固定按钮清错后进入工作台或登录。守卫附 `{sessionRestore:true,redirect}` 的 503 说明已保留凭据，提供「重试恢复会话」，清错回原路由并重新验证。见 [全局错误页 · `frontend/app/error.vue`](../../frontend/app/error.vue)。

## 4. 核心领域模型、数据口径与后端 API 契约

### 4.1 存储模型和可见字段

| 存储 | 结构、关系与时间边界 |
| --- | --- |
| MySQL `blog_data` | `user` 以 id 标识、邮箱唯一、单 `role_id`，含姓名、状态、地址/简介和登录时间/IP。密码为服务端散列，用户响应不依赖密码字段。`role` 角色名唯一，`USER` 为启用的默认角色。用户、角色和菜单常用状态为 1 启用、0 停用 |
| RBAC | `permission.permission` 唯一，含 `enabled` 字段（持久列 `status`），`role_permission` 复合主键关联角色和权限。一条权限可被多个路由节点共享。`router` 自关联 `pid`（根 null），有 `name,path,menuOrder,status,remark,permission,meta,children` 的 API 结构。`meta.type` 为 `C/M/F`，含两个隐藏字段及标题、图标和遗留配置 |
| 令牌 | `token` 记录用户、唯一令牌值、类型、过期/撤销状态、创建时间。枚举 `BEARER/ACCESS/REFRESH/PASS`，当前登录签发并持久化 `ACCESS`，默认 JWT 为 1 天。配置中的刷新有效期为 7 天，无刷新 HTTP 入口。注销标记关联令牌撤销 |
| MySQL `boss_data` | 来源字典/导入表 `city_data,degree,html_data,industry_data,job_list,position_data`。主读表 `t_job_detail` 有行主键 `id`、来源分类代码 `position_id`、城市、职位、来源 URL、`detail_data` JSON、福利/要求数组存储、删除标志。业务查询只读取原始 `is_deleted=0` |
| 职位分析附表 | `job_ai_analysis` 通过 `job_id=t_job_detail.id` 连接，唯一键为职位行 id、输入摘要、管线版本、模型标识、策略版本。保存来源/结果 JSON、状态、重试/租约/fence、错误码与用量。读取选最新 `updated_at`，再用最大附表 id 打破并列。表缺失仍保留旧职位读取，实际 SQL/身份/模式错误仍报错。迁移需显式部署 |
| MongoDB `resumedb.resumes` | 同集合 `docType=meta` 保存所有者、标题、当前内容、`currentBranch/headCommit/repositoryId/branches/stashStack` 与日期。`docType=commit` 保存 `resumeId,content,message,branch,parentCommits,commitType,author,authorEmail,commitTime,treeHash,metadata` 等快照。资源按当前用户与简历范围读取，初始分支 `main`。操作差异见 §5.1 |
| S3/MinIO | 对象键 `<userId>_<原始文件名>`，同一用户同名对象覆盖。公开客户端响应结构 `ImageResponse` 为 `success,message,fileName,url`。所有者边界见 §5.1 |
| Redis | 注册 `register:code:<email>` 1 分钟，注册互斥使用注册锁。登录控制器将返回的访问令牌结构缓存到 `token:refresh:<userId>`（7 天），该键名本身不提供刷新流程。枚举也定义 `token:access:`（2 小时）、`resume:list:`、`salary:analysis:`、`salary:bases:`（各 10 分钟），以实际调用判断是否使用 |
| 日志快照与预算 | 日志 Redis 键 `log:analysis:{complete,heatmap,ip-statistics,http-methods,weekday-patterns,browser-usage}`，TTL 24 小时。看板 `dashboard:stats:overview`，TTL 1 小时。Python SQLite 表 `budget(day TEXT PRIMARY KEY, used INTEGER NOT NULL)` 记录每日 token 使用。预算/锁定与浏览器无交互入口 |

依据 [系统 SQL · `database/system.sql`](../../database/system.sql)、[招聘 SQL · `database/boss.sql`](../../database/boss.sql)、[职位分析迁移 · `database/migrations/20261108_job_ai_analysis.sql`](../../database/migrations/20261108_job_ai_analysis.sql)、[简历模型 · `backend/api/src/main/java/com/david/hlp/web/resume/entity/Resume.java`](../../backend/api/src/main/java/com/david/hlp/web/resume/entity/Resume.java)、[Redis 枚举 · `backend/api/src/main/java/com/david/hlp/web/common/enums/RedisKeyEnum.java`](../../backend/api/src/main/java/com/david/hlp/web/common/enums/RedisKeyEnum.java) 与 [日志缓存键 · `backend/commons/src/main/java/com/david/hlp/commons/entity/logs/LogCacheKeys.java`](../../backend/commons/src/main/java/com/david/hlp/commons/entity/logs/LogCacheKeys.java)。

### 4.2 招聘指标与职位分析状态

| 口径 | 规则 |
| --- | --- |
| 二维薪资折算 | 只解析原串 `nK` 或 `a-bK`（整数、严格大写 K）。前者取 n，后者取算术中点，可有小数。无法解析为空 |
| 学历分值 | 按字符串包含关系映射，不限 0、初中 1、高中/中专 2、大专 3、本科 4、硕士 5、博士 6。空白或未命中为空，由关键词映射的首个匹配项决定，多关键词没有规定的排序优先级 |
| 经验折算 | 应届/无需/不限 0、1 年以内 1、1-3 取 2、3-5 取 4、5-10 取 7、10 年以上 10（源码按「1年以内」「10年以上」等词面匹配）。空白或未命中为空。该值是关键词折算 |
| 二维样本范围 | 城市与职位精确匹配或 `all`。只返回两个所选轴都非空的行，按 X 后 Y 排序。UI 从返回集计算样本、公司和平均薪资，非当前轴的未知字段保留。轴不同由 UI 校验，服务端仅校验轴白名单 |
| 城市/职位枚举 | 从未删除样本聚合去重。职位按指定城市列值筛选，默认 `all`。源码使用 Spark `col("city_name").equalTo(cityName)` 的列值比较，前端仍保留 `safeCity` 输入规则。返回顺序以接口为准 |
| 高薪排行 | 不随城市/职位草稿或已应用条件变化。只统计匹配 K 格式的薪资，按职位分组，职位 ≥10 条才入榜。取端点最小/最大，均薪为 `(AVG(min)+AVG(max))/2` 保留一位小数。推荐城市来自该职位城市组（≥5 条）中均薪最高的一组，缺组时相关字段 null。按均薪、样本数降序，同名标题不当作全局职位主键 |
| 返回字段 | 样本包含 `positionId,positionName,cityName,companyName,companySize,financingStage,address,companyUrl,jobUrl`（客户端兼容 `JobUrl`）、`salary/salaryValue,degree/degreeValue,experience/experienceValue,employeeBenefits,jobRequirements,analysisStatus,analysisModel,analysisVersion`。旧福利/需求数组由 `JobArrays` 读取，支持历史存量。前端过滤旧的「目前未解析」占位文字并保留条目内标点 |

| 接口 `analysisStatus` | 显示 | 触发 |
| --- | --- | --- |
| `unparsed` | 未分析 | 无附表状态且两个存量数组均无有效条目 |
| `legacy_unverified` | 历史结果（来源未验证） | 无附表状态，存在存量条目 |
| `pending` | 等待分析 | 存储为 `pending` 或 `retry_wait` 且来源一致 |
| `running` | 分析中 | 已领取且来源一致 |
| `complete` | 已完成（当前来源） | `completed`，结果标记 complete，来源一致且两组存储是有效 JSON 数组 |
| `no_evidence` | 已分析，原文未说明 | `completed`，结果标记 no_evidence，来源一致且两组均为合法空数组 |
| `needs_review` | 待复核 | 同名存储状态，或来源/结果解析异常，或完成记录的标记、数组或一致性校验失败 |
| `failed` | 分析失败 | 失败且来源仍一致 |
| `stale` | 来源已变更 | 当前事实与保存来源不一致，或存储状态 `superseded` |
| 前端 `unknown` | 分析状态未知 | API 空值或未识别值。9 个已知投影之外的防御态 |

状态描述来源可靠性，独立于详情里的福利/要求是否为空。不得用「等待分析」推断自动任务正在运行。持久化和读取边界见 [二维计算 · `backend/analytics/src/main/java/com/david/hlp/spark/service/User/TwoDimensionalAnalysisChart.java`](../../backend/analytics/src/main/java/com/david/hlp/spark/service/User/TwoDimensionalAnalysisChart.java)、[排行计算 · `backend/analytics/src/main/java/com/david/hlp/spark/service/User/SalaryHotJob.java`](../../backend/analytics/src/main/java/com/david/hlp/spark/service/User/SalaryHotJob.java)、[职位枚举 · `backend/analytics/src/main/java/com/david/hlp/spark/service/User/ListBasicSet.java`](../../backend/analytics/src/main/java/com/david/hlp/spark/service/User/ListBasicSet.java)、[状态读取 · `backend/commons/src/main/java/com/david/hlp/commons/recruitment/JobAnalysisReader.java`](../../backend/commons/src/main/java/com/david/hlp/commons/recruitment/JobAnalysisReader.java)、[前端投影 · `frontend/app/lib/recruitment.ts`](../../frontend/app/lib/recruitment.ts)。

### 4.3 日志指标与健康口径

- 主 API 在受保护请求通过身份和授权检查后发 `ACCESS|ts=…|ip=…|path=…|method=…|ua=…`，记录时点在处理链继续之前。受保护请求的日志与浏览器页面访问统计口径不同。无响应耗时或 HTTP 响应状态字段。独立访客为 IP 去重，路径为 API 记录的请求路径，地理位置依赖可选离线 MMDB，未命中为未知。
- Spark 扫描配置日志目录并取访问记录，生成分钟/小时热力、IP、方法、星期、浏览器分布与热门路径。浏览器 UA 顺序先 Chrome 后 Edg，带 Chrome 标记的 Edge 归入 Chrome。展示 `dataStartTime=now-24h` 和 `dataEndTime=now` 是服务端赋值。聚合没有对应的最近 24 小时过滤。`dataQuality` 用总访问数代替原始/解析行数，`failedLines=0`，无法用于真实解析失败率告警。
- 集群调度每 5 分钟运行，`local-dev` 关闭定时调度。看板起自缓存，`generatedAt` 表示生成时间，缺快照以 204 表示。看板原始字段还含 `geoStats,weekdayStatistics,dataQuality`，目前未渲染。日志健康直接探测 Spark HTTP，网关失败有 `DEGRADED/DOWN` 正文和非 200 状态，页面如何消费见 §3.8。

依据 [请求过滤器 · `backend/api/src/main/java/com/david/hlp/web/system/auth/filter/JwtAuthenticationFilter.java`](../../backend/api/src/main/java/com/david/hlp/web/system/auth/filter/JwtAuthenticationFilter.java)、[日志聚合与快照 · `backend/analytics/src/main/java/com/david/hlp/spark/service/Log/LogServiceImp.java`](../../backend/analytics/src/main/java/com/david/hlp/spark/service/Log/LogServiceImp.java)、[调度 · `backend/analytics/src/main/java/com/david/hlp/spark/service/Log/LogAnalysisScheduler.java`](../../backend/analytics/src/main/java/com/david/hlp/spark/service/Log/LogAnalysisScheduler.java)、[网关 · `backend/api/src/main/java/com/david/hlp/web/system/auth/controller/LogController.java`](../../backend/api/src/main/java/com/david/hlp/web/system/auth/controller/LogController.java)。

### 4.4 全部业务 HTTP 映射与访问范围（75 + 4）

按控制器源声明计数，主 API 58 + crawler 2 + analytics 15 = Java 75，另有 Python 4。招聘网关和 analytics 是两层映射，分别计数。框架自带设施（例如 Actuator 探针、预检、默认错误处理）及外部导出不计入。表中「登录」表示 Bearer 校验。「页面」按 §2.3 中服务端规则检查。「现用」指当前页面或共享会话调用。简历和图片细节见 §5.1。

#### 主 API 8080 的认证、管理、招聘与日志（34）

| 方法 | 路径 | 请求 → 响应 / 现用与权限 |
| --- | --- | --- |
| POST | `/api/auth/login` | `{email,password}` → `Result<Token>`（`token,tokenType,userId,authUser` 等）。公开、现用 |
| POST | `/api/auth/logout` | Bearer 可选，无请求体。撤销令牌 → 无数据成功信封。公开、现用 |
| POST | `/api/auth/sendRegisterEmail` | `{email}` → 成功信封。验证码过期/冲突规则见 §5.1。公开 |
| POST | `/api/auth/register` | `{name,email,password,code}` → 成功信封。公开 |
| GET | `/api/auth/hasPagePermission` | query `path` → `Result<Boolean>`。登录，下游看板鉴权使用 |
| GET | `/api/auth/getRouters` | 无参数 → 全量树 `Result<Router[]>`。登录、现用 |
| GET | `/api/auth/getUserPrivateInformation` | 无参数 → `Result<string[]>`。当前有效权限，剔除 `ROLE_` 角色标识。登录、现用 |
| GET | `/api/auth/getUserRole` | 无参数 → 当前 `Result<Role>`。登录、现用 |
| GET | `/api/auth/getUserBaseInfo` | 无参数 → 当前账号精简 `Result<User>`。登录、现用 |
| GET | `/api/auth/getRoleList` | 无参数 → `Result<Role[]>`，带 permissions/routers。roles 或 menus，现用 |
| POST | `/api/auth/addRole` | 角色元数据 → 成功信封。roles，现用 |
| POST | `/api/auth/editRole` | id + 角色元数据 → 成功信封。roles，现用 |
| POST | `/api/auth/updateRoleRouters` | `{roleId,routerIds:number[]}` → 成功信封，空集合清授权。roles，现用 |
| POST | `/api/auth/deleteRole` | `{id}` → 成功信封。roles，现用 |
| POST | `/api/auth/addRouter` | §3.7 菜单字段 → 成功信封。menus，现用 |
| POST | `/api/auth/editRouter` | id + §3.7 菜单字段。锁定 `pid,permission` → 成功信封。menus，现用 |
| POST | `/api/auth/deleteRouter` | 菜单对象，服务端按 id 重读 → 成功信封。menus，现用 |
| GET | `/api/role/getRoleList` | 可选 query `roleName` → 基础 `Result<Role[]>`。roles 或 users，现用 |
| POST | `/api/user/getUserManageInfo` | `{number,size,query:{name?,roleId?,status?}}` → `Result<PageInfo<User>>`，含 `content,query,number,size,totalElements,totalPages`。users，现用 |
| POST | `/api/user/addUser` | §3.5 新增白名单字段 → 成功信封。users，现用 |
| POST | `/api/user/updateUser` | id + §3.5 编辑白名单字段 → 成功信封。users，现用 |
| POST | `/api/user/deleteUser` | `{id,password}`（操作者确认密码）→ 成功信封。users，现用 |
| GET | `/api/v1/boss/basic/city-name-list` | 原始 `string[]`，同路径转发 analytics。登录、现用 |
| GET | `/api/v1/boss/basic/position-name-list` | query `cityName=all`（默认）→ 原始 `string[]`，同路径转发。登录、现用 |
| GET | `/api/v1/boss/user/salary-hot-job` | query `limit=10`（默认，1–1000）→ `SalaryJob[]`，同路径转发。登录、现用 |
| GET | `/api/v1/boss/user/two-dimensional-analysis-chart` | query `cityName,positionName,xAxis,yAxis`，默认 `all,all,salary_value,degree_value` → `JobAnalysisData[]`，同路径转发。登录、现用 |
| GET | `/api/v1/system/logs/dashboard/stats` | 原始看板 Map，或 204。转 `/api/log/access/dashboard/stats`。dashboard，现用 |
| GET | `/api/v1/system/logs/stats` | 完整日志转换后的 Map（计数 `totalVisits,uniqueIps`）。转 `/api/log/access/analysis/complete`，缺正文归 503。dashboard |
| GET | `/api/v1/system/logs/heatmap` | 热力 Map，转 `/api/log/access/analysis/heatmap`。上游无体回 200 `{}`。dashboard |
| GET | `/api/v1/system/logs/ip-statistics` | IP 分布 Map，转 `/api/log/access/analysis/ip-statistics`。上游无体回 200 `{}`。dashboard |
| GET | `/api/v1/system/logs/http-methods` | 方法分布 Map，转 `/api/log/access/analysis/http-methods`。上游无体回 200 `{}`。dashboard |
| GET | `/api/v1/system/logs/browser-usage` | 浏览器 Map，转 `/api/log/access/analysis/browser-usage`。上游无体回 200 `{}`。dashboard |
| POST | `/api/v1/system/logs/analysis/trigger` | 无请求体 → 任务已启动的文本，转 `/api/log/access/analysis/trigger`。dashboard + `logs.analysis.trigger` |
| GET | `/api/v1/system/logs/health` | 转 `/api/log/access/health` → `{status,sparkService,timestamp}`。失败保留上游 4xx/5xx 或 503。只需登录，现用 |

控制器源 `backend/api/src/main/java/com/david/hlp/web/system/auth/controller/{AuthController,UserController,RoleController,RecruitmentGatewayController,LogController}.java`。招聘网关转发 Bearer 与 query，上游响应码透传，传输错误为 502。日志网关的下游错误与无缓存判定以各路由为准。

#### 主 API 8080 的简历、版本与图片（24）

以下均要求登录。简历按本人范围读取与变更，图片只有删除检查对象键所有者前缀。简历具体对象路径以 `{id}` 或控制器中的 `{resumeId}` 表示。

| 方法 | 路径 | 请求 → 响应 |
| --- | --- | --- |
| GET | `/api/resume` | 当前用户的 meta 简历列表 |
| GET | `/api/resume/{id}` | 本人 meta 简历。未找到 404 |
| POST | `/api/resume` | `{title,content}` → 新 meta，自动初始化 main 和初始提交 |
| PUT | `/api/resume/{id}` | 可给 `title,content` → 更新后 meta。基础保存也产生提交 |
| DELETE | `/api/resume/{id}` | 删除本人 meta 和提交。不存在/非本人时服务不删除，控制器仍可返回成功 |
| POST | `/api/resume/{resumeId}/version/commit` | `{message,content}` → 新 commit ID |
| POST | `/api/resume/{resumeId}/version/branch` | `{name}` → 从当前 HEAD 创建分支 |
| POST | `/api/resume/{resumeId}/version/switch` | `{branch}` → 更新分支、HEAD、内容 |
| POST | `/api/resume/{resumeId}/version/merge` | `{source,target}` → 合并提交 ID |
| GET | `/api/resume/{resumeId}/version/history` | 提交列表，按提交时间降序 |
| POST | `/api/resume/{resumeId}/version/checkout` | `{commitId}` → 恢复 HEAD 与快照，目标是分支头时切到对应分支 |
| POST | `/api/resume/{resumeId}/version/stash` | `{content}` → 写入栈顶 |
| POST | `/api/resume/{resumeId}/version/stash/pop` | 无请求体 → 弹出并返回栈顶内容 |
| POST | `/api/resume/{resumeId}/version/reset` | `{commitId}` → 以该历史提交重置当前分支及内容 |
| POST | `/api/resume/{resumeId}/version/revert` | `{commitId}` → 新撤销提交 ID |
| GET | `/api/resume/{resumeId}/version/recent` | query `limit=10`（默认）→ 最新提交列表 |
| GET | `/api/resume/{resumeId}/version/diff` | query `fromCommit,toCommit` → 两提交间记录列表 |
| GET | `/api/resume/{resumeId}/version/debug/commit/{commitId}` | 单提交诊断 Map |
| GET | `/api/resume/{resumeId}/version/debug/validate` | 仓库指针、提交等校验 Map |
| POST | `/api/resume/{resumeId}/version/fix-duplicate-ids` | 修复 Map，可含 `success,message,actions,error` 和重建信息 |
| POST | `/api/image/upload` | multipart `file` → `Result<ImageResponse>` |
| GET | `/api/image/view/{fileName}` | 文件流，成功含内容类型、缓存与安全响应头 |
| DELETE | `/api/image/delete` | query `fileName` → 按当前用户键前缀校验，`Result<ImageResponse>` |
| GET | `/api/image/url/{fileName}` | `Result<ImageResponse>`，含相对查看 URL |

依据 `backend/api/src/main/java/com/david/hlp/web/resume/controller/{ResumeController,ResumeVersionController}.java` 与 [图片控制器 · `backend/api/src/main/java/com/david/hlp/web/minio/controller/ImageController.java`](../../backend/api/src/main/java/com/david/hlp/web/minio/controller/ImageController.java)。

#### Analytics 8082（15）与 crawler 8081（2）

Analytics 与 crawler 直连仍回查主 API 身份。受保护的日志路由还回查 dashboard 页面权限及触发权限。`/api/log/access/health` 是 analytics 内部公共健康路由，浏览器侧走需登录的网关。analytics 的缓存读取缺数据返回 204。

| 服务 | 方法 | 路径 | 输出或约束 |
| --- | --- | --- | --- |
| analytics | GET | `/api/v1/boss/basic/city-name-list` | 登录，城市列表 |
| analytics | GET | `/api/v1/boss/basic/position-name-list` | 登录，`cityName`，职位列表 |
| analytics | GET | `/api/v1/boss/user/salary-hot-job` | 登录，`limit`，薪资统计列表 |
| analytics | GET | `/api/v1/boss/user/two-dimensional-analysis-chart` | 登录，城市/职位及轴，对应二维样本 |
| analytics | GET | `/api/log/access/analysis/complete` | dashboard，完整访问分析快照 |
| analytics | GET | `/api/log/access/analysis/heatmap` | dashboard，分钟和小时桶 |
| analytics | GET | `/api/log/access/analysis/ip-statistics` | dashboard，IP 统计 |
| analytics | GET | `/api/log/access/analysis/http-methods` | dashboard，方法用量 |
| analytics | GET | `/api/log/access/analysis/weekday-patterns` | dashboard，按星期统计，无主 API 映射 |
| analytics | GET | `/api/log/access/analysis/browser-usage` | dashboard，浏览器用量 |
| analytics | POST | `/api/log/access/analysis/trigger` | dashboard + 触发权限，异步启动，返回文本 |
| analytics | GET | `/api/log/access/analysis/types` | dashboard，`complete,heatmap,ip-statistics,http-methods,weekday-patterns,browser-usage`，无主 API 映射 |
| analytics | GET | `/api/log/access/analysis/by-type/{analysisType}` | dashboard，按类型返回缓存，未知类型 400，无主 API 映射 |
| analytics | GET | `/api/log/access/dashboard/stats` | dashboard，原始看板 Map |
| analytics | GET | `/api/log/access/health` | 公共，存活文本 |
| crawler | POST | `/api/v1/ai/get-user-similarity` | 登录后命中隐私停用 503。遗留列表契约 |
| crawler | POST | `/api/v2/ai/matches` | 登录后命中隐私停用 503。保留结构化匹配契约 |

依据 `backend/analytics/src/main/java/com/david/hlp/spark/controller/{User,Log}/`、[下游鉴权 · `backend/commons/src/main/java/com/david/hlp/commons/security/DownstreamAuthFilter.java`](../../backend/commons/src/main/java/com/david/hlp/commons/security/DownstreamAuthFilter.java)、[匹配控制器 · `backend/crawler/src/main/java/com/david/hlp/crawler/ai/controller/AIRequirementsController.java`](../../backend/crawler/src/main/java/com/david/hlp/crawler/ai/controller/AIRequirementsController.java)。

#### Python 内部服务 8090（4）

| 方法 | 路径 | 契约与边界 |
| --- | --- | --- |
| GET | `/health/live` | 存活 `{status:"alive"}` |
| GET | `/health/ready` | 配置不足 503。否则返回 ready、模型/策略元数据、匹配停用声明和质量阈值 |
| POST | `/internal/v1/job-analysis` | 内部 Bearer，`{jobId,detailData,timeoutMs?}`，超时按请求值和服务上限约束。返回 `jobId,status,requirements,benefits,coreRequirements,model,pipelineVersion,policyVersion,requestId,usage`，条目含候选 ID、证据、置信与重要度，状态 `complete/no_evidence/needs_review` |
| POST | `/internal/v1/match-batch` | 内部 Bearer 且配置就绪后命中停用 503。存量协议字段 `resume,jobs,timeoutMs`。当前调用在读正文前终止 |

正文大小、JSON 结构、超时与内部权限由中间件先行检查。结构化错误为 `{error:{code,message},requestId}`，错误类型包括未授权 401、配置不足/隐私停用 503、JSON 无效 400、校验/大小 422、超时 504、内部错误 500。浏览器代理无内部服务目标。见 [路由与中间件 · `ai-service/app/main.py`](../../ai-service/app/main.py)、[返回约束 · `ai-service/app/schemas.py`](../../ai-service/app/schemas.py)。

### 4.5 响应包装与错误映射

| 情形 | 契约 |
| --- | --- |
| 标准请求 | Axios 同源 `baseURL='/'`、5 秒、`withCredentials`，按本机 token 带 Bearer。默认只解析 HTTP 200，返回 `response.data?.data ?? response.data`。拒绝值的外层 `code` 为 HTTP 状态（无状态归 500），业务码可能在 `error.data.code` |
| 标准信封 | `{code,message,data,timestamp}`，`Result.success(String)` 设消息、`data=null`，因此此类写操作用信封 `code===200`。有独立响应结构的图片、简历、日志与招聘按实际结构读取。必须分辨有效零、有效空集合、缺字段和错误正文 |
| HTTP 204 | 共享拦截器会拒绝。看板适配器只把 `code===204` 转为无快照。看板 200 空体应报错。日志分项还存在网关将上游无体转 200 `{}` 的情况 |
| 通用业务码 | `200` 成功。已识别的 4xx/5xx 码透传为对应 HTTP。校验/格式类通常 400，接口方法 405，数据库/异常通常 500，身份服务不可用 503。下游错误另有裸 Map、文本或空体 |
| `1001,1008,1011` → HTTP 409 | 用户已存在、数据重复、锁/资源已被使用 |
| `1002` → HTTP 401 | 登录凭据错误 |
| `1003,1004,1006,1007,1009` → HTTP 400 | 验证码错误、过期、确认密码错误、参数错误、请求错误。用户删除密码错为 `1006/400`，保留会话 |
| `1005,1010` → HTTP 404 | 用户不存在、简历不存在 |
| 401/403/服务故障 | 401 才可触发当前 token 失效逻辑。403 保留会话并显示权限不足。请求失败、超时与 5xx 在适用区域给重试。直接渲染后端内部错误文案会泄露实现细节，当前关键页面使用稳定的用户反馈 |

映射源 [Result · `backend/api/src/main/java/com/david/hlp/web/common/entity/Result.java`](../../backend/api/src/main/java/com/david/hlp/web/common/entity/Result.java)、[业务码 · `backend/api/src/main/java/com/david/hlp/web/common/enums/ResultCode.java`](../../backend/api/src/main/java/com/david/hlp/web/common/enums/ResultCode.java)、[异常处理 · `backend/api/src/main/java/com/david/hlp/web/common/exception/GlobalExceptionHandler.java`](../../backend/api/src/main/java/com/david/hlp/web/common/exception/GlobalExceptionHandler.java)、[浏览器请求 · `frontend/app/utils/request/request.ts`](../../frontend/app/utils/request/request.ts)、[看板请求 · `frontend/app/api/log/accesslog.ts`](../../frontend/app/api/log/accesslog.ts)。

## 5. 历史快照中的 UI 接入缺口与停用能力边界

### 5.1 盘点时可独立规划的能力

注册、简历、外部导出和扩展日志的现行页面参见文首。下表记录盘点时的接入约束。

| 能力 | 接入约束与需要设计的失败状态 |
| --- | --- |
| 自助注册与邮箱校验 | `sendRegisterEmail` 先检查邮箱占用，再送 6 位数字码并按邮箱缓存 1 分钟。`register` 需要姓名、邮箱、密码和验证码。邮箱锁内确认码、创建启用用户、绑定有效的默认 `USER` 并消费验证码。保留邮箱占用、验证码错误/过期、锁冲突和 SMTP 故障反馈。成功后需走现有登录获取令牌 |
| 简历列表、读写与删除 | 5 条 CRUD 路由均使用当前会话的 userId。创建顺序为插 meta、写初始提交、保存指针。基础 PUT 走 commit 更新内容（含标题单独更新）。删除逐步移除本人提交和 meta，结果没有恢复入口。并发、超时与半完成需要重新读真实状态 |
| 简历版本控制 | 15 条版本路由。普通提交保存完整内容并更新当前分支与 HEAD。分支从 HEAD 建立。切换/检出改工作内容。stash/pop 为栈操作。reset 移动当前分支指针，目标须在当前分支历史。revert 新建回滚快照。diff 返回提交列表，不能承诺逐行对比 |
| 版本风险与维护工具 | merge 新建含两个父节点的提交，直接使用源分支快照，仅更新相应目标指针，无冲突合并器。多个文档写操作无跨文档事务。检查/修复为维护入口。发现重复 ID 才删除旧历史、清空分支及暂存并重建 `main`，应核对 Map 的 `success` 和持久状态，不能仅看 HTTP 成功 |
| 外部简历导出 | 遗留 `resumeExport` 通过 `VITE_EXPORT_URL` 向外部地址发送 `{content,style,link,name,type?}` JSON 并读取 JSON，未接入页面，无仓库内任务、进度、下载保障。外发前须确认目的地、材料隐私、授权与可用性。`getExportCount()` 固定为 0，无外部统计请求 |
| 对象存储 | 接收非空 multipart 图片，基于声明的 `image/*` 判断，服务配置请求和单文件上限均为 50MB（请求还含 multipart 开销）。键同名可覆盖。上传和 `url` 返回查看路径。查看 Bearer 取流，缓存 3600 秒、`nosniff`、sandbox CSP，SVG 强制附件。带 Bearer 取文件才能用于页面受保护预览，裸 `<img src>` 不会自带该头。当前只在删除校验用户键前缀，view/url 没有对象所有者校验。私有素材接入前须补上服务端边界 |
| 扩展日志 | `stats/heatmap/ip-statistics/http-methods/browser-usage` 及看板未渲染指标可以单独立项。需保留 204、空 Map、错误和真实零值的区别，IP/地理字段按实际暴露范围设计。三条未代理的 analytics 路由参见 §4.4 |
| 手工日志触发 | 主 API 和 analytics 校验 dashboard 权限并检查 `logs.analysis.trigger`。成功文本只代表后台线程已启动。无任务 ID、查询进度、取消或互斥。不能以成功响应提示分析已完成，也不应主动重复提交 |

依据 [注册实现 · `backend/api/src/main/java/com/david/hlp/web/system/auth/service/imp/AuthServiceImp.java`](../../backend/api/src/main/java/com/david/hlp/web/system/auth/service/imp/AuthServiceImp.java)、[简历 CRUD · `backend/api/src/main/java/com/david/hlp/web/resume/service/ResumeService.java`](../../backend/api/src/main/java/com/david/hlp/web/resume/service/ResumeService.java)、[简历版本 · `backend/api/src/main/java/com/david/hlp/web/resume/service/ResumeVersionControlService.java`](../../backend/api/src/main/java/com/david/hlp/web/resume/service/ResumeVersionControlService.java)、[导出封装 · `frontend/app/api/modules/resume.ts`](../../frontend/app/api/modules/resume.ts)、[图片控制器 · `backend/api/src/main/java/com/david/hlp/web/minio/controller/ImageController.java`](../../backend/api/src/main/java/com/david/hlp/web/minio/controller/ImageController.java)、[日志触发 · `backend/analytics/src/main/java/com/david/hlp/spark/controller/Log/LogController.java`](../../backend/analytics/src/main/java/com/david/hlp/spark/controller/Log/LogController.java)。

### 5.2 停用、CLI 与无服务端能力

| 能力 | 红线与未来契约 |
| --- | --- |
| 简历职位匹配 | 两条 Java 路由要求已回查会话，随后在 MVC 解析正文前拒绝 503，原因标识 `AI_RESUME_MATCHING_DISABLED`。Python `match-batch` 在内部鉴权与就绪后、读取正文前拒绝 503，标识 `resume_matching_disabled`。两个服务均无配置开关可绕过。不得增加上传、粘贴、自动发送、打分或匹配进度入口，不得用不同路径绕过停用 |
| 保留的匹配口径 | 审批后才能评估启用。存量边界使用本人 `resumeId` 或文本，两者互斥，拒绝指定他人 `userId`。四维权重为技能 0.5、相关经历 0.3、经验 0.1、学历 0.1。有 `scored/insufficient_evidence/failed` 与未知维度语义，不能把未知分数当 0。当前规则不提供可用匹配结果 |
| 职位提取 | 分析职位来源的任务独立于简历处理，`crawler.ai.scheduling.enabled` 与 `crawler.ai.writeback-enabled` 默认关闭，调度启用后默认每 300000 毫秒尝试一次，写回还受质量门槛约束。读取端可呈现已存状态和来源，无手工分析或写回 UI/API |
| 旧 BOSS 采集 | 3 处旧 `@Scheduled` 均被注释。无启动、停止、进度、Cookie 管理接口。不得将本地配置或定时任务暴露为网页控件 |
| `RecruitmentSampler` | 本地 CLI，用于受控采样与独立标注，产出 `samples.jsonl`、`labels.jsonl` 等文件，不写业务库。不得把本地工件呈现成现网 API 数据 |
| 其他空缺 | 无自助改密码、编辑个人资料、刷新令牌、收藏/投递/聊天、批量写入、站内导出任务的配套后端。对应入口需要产品和后端契约变更 |

依据 [隐私停用记录 · `docs/operations/resume-matching-privacy-hold.md`](../operations/resume-matching-privacy-hold.md)、[隐私约束 · `docs/specs/resume-privacy-authorization-20261112.md`](resume-privacy-authorization-20261112.md)、[匹配入口保护 · `backend/crawler/src/main/java/com/david/hlp/crawler/ai/service/AuthorizedResume.java`](../../backend/crawler/src/main/java/com/david/hlp/crawler/ai/service/AuthorizedResume.java)、[内部管线 · `ai-service/app/pipeline.py`](../../ai-service/app/pipeline.py)、[采样说明 · `docs/recruitment-sampling.md`](../recruitment-sampling.md)。

### 5.3 五个废弃的同源封装

| 无映射的请求 | 当前做法 |
| --- | --- |
| POST `/api/auth/updateRolePermissions` | POST `/api/auth/updateRoleRouters`，发送角色 ID 与路由 ID |
| POST `/api/auth/updateUserRole` | POST `/api/user/updateUser`，通过受控用户表单发送 `roleId` |
| GET `/api/boss/basic/company-list` | 现有分析支持城市/职位。公司统计从返回样本读取 |
| GET `/api/boss/user/three-dimensional-analysis-chart` | 现有二维接口，轴来自三个允许维度 |
| POST `/api/resume/{id}/version/init` | POST `/api/resume` 自动初始化 |

封装源 `frontend/app/api/{auth/auth,boss/user/user,resume/version}.ts`。同一目录中的外部导出与固定返回的计数函数无需后端映射，按 §5.1 处理。

## 6. UI 重构高风险契约红线与自动化测试护栏

### 6.1 跨端红线速查

| 编号 | 回归风险 | 验收断言 |
| --- | --- | --- |
| 1 | 用户页码直接接偏移接口 | 例如第 2 页、20 条，发 `number=21`，SQL offset 为 20，数据不重叠。删除最后页回有效页 |
| 2 | 两个角色列表被合并 | 用户下拉用 `/api/role/getRoleList`（roles 或 users），复杂角色/菜单治理用 `/api/auth/getRoleList`（roles 或 menus） |
| 3 | 直接渲染全量树或以菜单隐藏替代 API 鉴权 | 授权树砍整枝，导航树额外隐藏。验证隐藏入口可直达、整枝隐藏不可达、后端仍独立鉴权 |
| 4 | 把 HTTP 成功、业务成功和无数据混淆 | `data ?? body`，无数据变更看 `code===200`，看板仅 204 为无快照，真零保留，`1006/400` 不登出 |
| 5 | 恢复、退出或强退的先后顺序变更 | 24 小时严格上界、旧 token 响应不清新登录、主动退出先确认草稿、强退先失效、服务器撤销未确认给反馈。跨标签刷新单独验收 |
| 6 | 提交后重复写入 | 角色授权、菜单新增/编辑及删除必须写后回读。未确认展示明确区别，禁止直接重放。菜单删除关闭原确认并重新读树，重新发起前仍须双预检 |
| 7 | 批量授权覆盖遗留权限或误删关联对象 | 祖先/别名闭包、不可表示项锁定、清空确认。菜单锁定父与权限，删除双读加服务端事务校验 |
| 8 | 招聘样本、筛选或来源语义失真 | 活跃原始记录、分类字段可重复、空/零区分、安全城市和外链、草稿与 applied、1000 绘制上限及明细全量、状态及 unknown 保留 |
| 9 | 把快照包装成近 24 小时监控 | 最新 24 个有记录桶、全部快照 KPI、时间与 TZ 清晰。质量、状态码/耗时、UA 与地理局限按 §4.3 |
| 10 | 重排 UI 丢失快捷键或辅助入口 | 768/640 两处分界、7 天 Cookie、跳过导航、焦点回归、状态播报、小时图按钮和表格、散点明细可键盘访问 |
| 11 | 从历史文档恢复失效契约 | 以基线源码核对代理、响应、退出、包版本和无效封装，漂移见 §8 |

### 6.2 前端单测入口与历史覆盖

当前入口为 `cd frontend && pnpm test`，执行目录内的 `tests/*.test.mjs`。简历行为及部分路由、日志投影可用 `node --experimental-strip-types --test --test-concurrency=1 tests/penpot-pages.test.mjs` 单独检查。以下 73 项为历史基线的顶层 `test()` 源码计数。

| 文件 | 项数 | 保护范围 |
| --- | ---: | --- |
| `client-ip.test.mjs` | 4 | 精确可信代理和单个 IP、剥离伪造头、保留鉴权、Nitro hook 在代理前执行、v2 匹配代理目标 |
| `dashboard.test.mjs` | 7 | 204 与真零、排序及去样例化、缓存毫秒与有效时间、非法快照、看板请求错误、健康请求状态、时区 |
| `menus.test.mjs` | 5 | 全树解析与坏树拒绝、筛选祖先、锁定 pid/permission 和不传后代、创建查重/非法父节点、删除四重限制 |
| `recruitment.test.mjs` | 8 | 选项白名单和城市、空零与重复样本、有效薪资均值、排行校验、外链、散点分组与限绘、小数/未知维度、来源和标点 |
| `request.test.mjs` | 15 | SSR 请求禁止、Bearer、解包、无 token、401、注销公开且错误自管、确认密码不登出、旧 token 竞态、登录错误归类。依赖合并、source map、glob、集合及组件注册解析回归 |
| `roles.test.mjs` | 6 | 数据投影与停用值、元数据白名单及默认角色、树 ID、祖先闭包、别名移除、不可表示与业务写入失败 |
| `session.test.mjs` | 18 | 登录提示、恢复故障、树与权限、路径/回跳/快捷入口、TTL、实际编辑守卫与强退、隐藏语义、两个守卫的旧响应隔离、菜单图标、未确认注销和旧注销竞态 |
| `settings.test.mjs` | 4 | 账号投影、真正空权限、健康形状及时间、403/业务失败与降级分类 |
| `users.test.mjs` | 6 | 分页偏移、列表安全投影及空页、编辑白名单、创建与初始密码、业务码、255 字符限制 |
| **合计** | **73** | 适配器和隔离行为测试，浏览器验收见 §6.4 |

### 6.3 后端 29 个测试类

按 `backend/**/src/test/java/**/*Test.java` 的类文件计数。括号内是保护主题，命令 `mvn -f backend/pom.xml clean verify`。独立 credential 套件的 4 类和 Python 测试另计。

| 模块 | 数量 | 测试类与保护范围 |
| --- | ---: | --- |
| api | 13 | `HadoopConfigTest`（本地/集群配置）。`ImageIntegrityTest`（图片边界）。`ResumeIntegrityTest`（本人简历和历史完整性）。`AccessBoundaryTest`、`DownstreamIntegrationTest`、`ClientIpTest`、`IdentityPersistenceTest`、`IdentitySecurityTest`、`JwtIdentityTest`（访问矩阵、下游、IP、身份持久化、安全与 JWT）。`LogGatewaySecurityTest`、`RecruitmentGatewayTest`（网关授权/状态）。`RoleSemanticsTest`、`RouterIntegrityTest`（默认角色、状态与菜单写边界） |
| analytics | 5 | `LogHealthSecurityTest`（健康/日志权限）。`CityAndPositonControllerTest`（轴与排行参数）。`GeoStatsTest`、`IpGeoServiceTest`（地理聚合、离线定位）。`TwoDimensionalAnalysisChartTest`（轴投影与计算） |
| commons | 5 | `RedisUtilsAutoConfigurationTest`、`RedisCacheUtilTest`（缓存配置与隔离）。`LogModelContractTest`（日志 DTO）。`JobAnalysisReaderTest`（附表与来源状态）。`DownstreamAuthFilterTest`（网关外路径和身份/权限保护） |
| crawler | 6 | `FormatDataServiceTest`（采集数据格式）。推理服务契约测试（输入、上游边界及返回状态）。`JobAnalysisStoreTest`（来源、状态及写回围栏）。`MatchBoundaryTest`、`ResumePrivacyTest`（停用与匹配边界）。`RecruitmentSamplerTest`（受控采样输出） |
| **合计** | **29** | 类文件数，与 Java 测试方法或参数实例数无直接对应 |

独立凭据契约使用 `mvn -f tools/credential-contract-tests/pom.xml clean verify`，覆盖配置校验、Redis 占位、SMTP/JWT 等。Python 合同、评估、预测冻结及隐私测试在 `ai-service/tests/`。生态运行依赖、锁文件和 CI 检查入口以 `.github/workflows/` 为准。

### 6.4 浏览器实测验收清单

- 使用受控的授权账号和最小权限账号，验证同源代理、生产启动方式、所有直接链接、锚点和侧栏跳转、授权占位与 403/404。单独验证 503 会话恢复重试及外部 redirect 被拒。
- 在用户、角色、菜单任一编辑草稿中试 Escape、关闭、跨页、刷新、主动退出取消/确认、权限失效和跨标签切换。还要验证键盘焦点、表单首个错误、忙态防重复、移动端滚动、弹窗返回焦点、状态播报。
- 以不同权限覆盖工作台和看板的 204、200 空体、真实 0、403、故障/恢复，验证 24 个有记录桶、峰值 KPI、时区与分布。健康用实际可用/不可用的 Spark 验证 HTTP 与文案。
- 用户管理验证第二页不重行、筛选后回第一页、末页删除，以及操作者密码失败会话仍在。角色验证祖先/别名和不可表示项，清空需确认。菜单验证删除两次预检、关联在确认前变化、写入后读取失败与无重复变更。
- 招聘用空数组、重复分类码、无效外链、未知状态、>1000 坐标组和城市切换竞态。验证草稿未提交提示与当前重试行为、全量明细、排序、分页和排行区域独立状态。
- 以手机/桌面和键盘/读屏检查两个断点、可见焦点、主内容跳转、抽屉闭环、图表等价数据、宽内容滚动、文本对比度和 200% 缩放下操作可达。
- 不带真实简历内容的现网边界验证仅检查停用响应及 UI 没有上传/匹配入口，使用隔离样本进行任何写操作验收。

## 7. 体验优化项的现行状态

| 纯前端 UI / 交互优化项 | 已落地行为或剩余契约 |
| --- | --- |
| 招聘条件重试 | 「重试上次条件」使用上次应用的条件。未应用的筛选草稿留在表单 |
| 侧栏快捷键 | 文本输入和活动模态期间抑制侧栏快捷键。按钮及正常键盘切换仍可使用 |
| 路由与权限变更 | 会话提供同 token 强制重读权限树。菜单与角色写后刷新以页面接线为准，修改本人或当前角色仍强制退出 |
| 删除、保存与写后回读 | 用户、角色和菜单写入显示操作反馈，简历列表区分写入失败、结果未知及受理后回读失败。删除确认密码错误保留会话 |
| 看板与散点信息 | 看板区分快照空桶与真实零值。散点图显示实际绘制组数、总组数及 1,000 组上限，明细保留全量分组 |
| 健康响应与网关 | 服务端状态码统一仍需跨层契约。前端区分权限拒绝、连接失败与已知健康状态 |
| 跨标签会话变化 | 壳层支持已登记脏草稿的页内失效提示和提交锁。登记范围以各编辑组件接线为准。简历编辑另在本标签页提供按会话隔离的草稿恢复 |
| 简历版本与导出 | 最近列表可刷新，合并目标改变需重新确认。版本校验显示明细，快照支持逐行对比。本地 PDF/HTML/Markdown 与经同意的外部导出均可使用 |

| 需同步改动后端的优化项 | 需要补齐的能力或数据契约 |
| --- | --- |
| 自助编辑、密码修改/恢复、令牌刷新 | 明确本人资源边界、重认证、撤销与 UI 状态。公共注册可复用邮箱契约，找回密码需要新的接口 |
| 日志真滑窗、可靠质量与可用性指标 | 明确真实时间范围与时区，记录状态、耗时和解析分母，修正 UA 规则。任务进度、去重、取消需任务契约 |
| 服务端招聘分页和稳定职位 ID | 增加范围、总数、稳定行标识和排序契约。跨页 KPI、组计数与排行需一致。扩展薪资格式、职位筛选和原始字段可用性需数据层支持 |
| 菜单/角色可并发编辑 | 若需处理多人同时覆盖，增加版本/冲突检测。服务端可扩展统一校验错误和明确的授权生效信号 |
| 私有图片或跨设备导出 | 增加读地址/流的对象级授权、冲突规则和受控传输。站内导出任务、进度及统计须定义新的后端契约 |
| 可恢复的简历版本操作 | 引入原子性/并发控制、明确合并与冲突 UX、修复前可恢复历史，给异步失败留可重试语义 |
| 简历职位匹配及其他空缺 | 匹配保持隐私停用，待授权和数据处理边界解决再评估。收藏、投递、聊天、批量操作需要独立领域约定 |

## 8. 既有文档地图与漂移说明

| 文件/目录 | 使用范围 | 基线核对与漂移 |
| --- | --- | --- |
| [项目结构 · `docs/project-structure.md`](../project-structure.md)、[本地开发 · `docs/local-development.md`](../local-development.md) | 目录迁移、工具链、生产 Node 代理、各服务启动和环境边界 | 与当前物理路径、代理和服务边界对照使用。发布或本机参数以一次真实运行和对应配置为准 |
| [前端审查修复规格 · `docs/frontend-audit-repair-spec.md`](../frontend-audit-repair-spec.md)、[验证记录 · `docs/frontend-audit-repair-validation.md`](../frontend-audit-repair-validation.md) | 服务端访问边界、会话竞态、导航隐藏、招聘口径、历史验证 | 验证记录固定在当时审查范围，当前页数、路由和用例数量以本规格对应提交重查 |
| [现有前端说明 · `frontend/README.md`](../../frontend/README.md) | Vite 开发及生产命令、已接入页面、测试入口 | 包含简历本地导出、安全预览、草稿与外部导出约束。旧外部计数地址属于历史快照 |
| [旧前端 API 总览 · `docs/frontend-api/README.md`](../frontend-api/README.md)、[端点 · `docs/frontend-api/endpoints.md`](../frontend-api/endpoints.md)、[结构 · `docs/frontend-api/contracts.md`](../frontend-api/contracts.md)、[验证 · `docs/frontend-api/verification.md`](../frontend-api/verification.md) | 遗留封装和固定提交的取证，了解旧 DTO 和历史端点 | 含 `vue/`、`JDK17SERVE/` 的旧路径与 pin，旧 401 直接清 token 未考虑请求归属。外部计数/浏览器凭据、映射数、登录和包装语义属于不同阶段。查当前接口用 §4 及当前源码。无映射封装见 §5.3 |
| [独立凭据测试 README · `tools/credential-contract-tests/README.md`](../../tools/credential-contract-tests/README.md)、[现有 POM · `tools/credential-contract-tests/pom.xml`](../../tools/credential-contract-tests/pom.xml) | 隔离 Maven 合同及离线重复入口、模拟网络范围 | README 仍描述旧推理供应商及相关初始化/HTTP 测试，当前 POM 的源集和测试用例是实际边界。登录服务不因此出现前端 refresh 路由 |
| [隐私停用 · `docs/operations/resume-matching-privacy-hold.md`](../operations/resume-matching-privacy-hold.md)、[授权规格 · `docs/specs/resume-privacy-authorization-20261112.md`](resume-privacy-authorization-20261112.md)、[收尾边界 · `docs/specs/ai-closeout-20261112.md`](ai-closeout-20261112.md) | 匹配处理权限、停用范围与各运行组件边界 | 当前上线限制以控制器前置保护、配置默认值和当前部署检查为准。历史验收只证明其记录时环境 |
| [采样 · `docs/recruitment-sampling.md`](../recruitment-sampling.md)、[离线地理位置 · `docs/operations/offline-geolocation.md`](../operations/offline-geolocation.md)、[对象存储迁移 · `docs/operations/rustfs-migration.md`](../operations/rustfs-migration.md)、[数据库角色唯一性 · `docs/operations/database-role-uniqueness.md`](../operations/database-role-uniqueness.md) | CLI 与专用基础设施运行手册 | 与对应 CLI、Hadoop/Redis、S3 及数据库配置和迁移文件配套使用。SQL 快照会删除表，只向明确的新空库导入 |
| `docs/specs/`、`docs/operations/`、`docs/security/` 其余专项记录 | 专项合同、上线记录、依赖和架构审查 | 历史治理和上线证据按记录日期与提交使用。当前页面和 API 以文首及对应源码为准 |
