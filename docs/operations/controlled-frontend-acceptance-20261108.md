# 受控前端展示验收（2026-11-08）

本轮**通过受控展示验收**，不代表真实 BOSS、真实模型质量、完整身份链或业务数据验收。仅提交本文档，脚本、原 HTTP DTO、适配响应、截图及脱敏请求记录留在私有 `/tmp/boss-frontend-acceptance-20261108/`。无 production 源码或现有 artifact 修改，无 push/PR/watch、另委派、重建或服务重启。

> 🧠 **From Hindsight memory (Rebuild recruitment analytics with shadcn-vue and existing Spark read APIs)** — 历史记录指出 `/recruitment` 使用四个只读 API、职位详情 sheet 和安全外链。此信息仅用于定位；以下结果来自当前源码和本轮部署页面，未沿用历史通过声明。

## 实际目标及契约

- 工作树 `/home/david/.t3/worktrees/BossAnalyze/t3-1a150d21`，分支 `t3/controlled-frontend-acceptance`，验收源码基线 `d40d0229468f8d5566ee5a70828f7c69f2e37db8`。
- 使用现行 Nuxt **production 5020**：`http://127.0.0.1:5020/recruitment`，不是开发服务器。部署 artifact 为 `/home/david/.local/share/bossanalyze/stack-deployment-20261108/latest/frontend-pnpm1124/.output`，Nitro 元数据为 Nuxt 4.6.3 / node-server，未重建。
- 真实页面 `frontend/app/pages/recruitment.vue`，真实 `RecruitmentJobSheet`、`jobSamples` normalizer、Axios 请求/session helpers、`app/api/boss/user/user.ts` wrappers 全部由部署 bundle 执行。
- API：`GET /api/v1/boss/basic/city-name-list`；`GET /api/v1/boss/basic/position-name-list?cityName=all`；`GET /api/v1/boss/user/salary-hot-job?limit=10`；`GET /api/v1/boss/user/two-dimensional-analysis-chart?cityName=all&positionName=all&xAxis=salary_value&yAxis=degree_value`。
- 当前 Java controller / authenticated gateway 声明上述四个只读路由；chart 返回 `List<JobAnalysisData>`，不是 fixture envelope。浏览器拦截不会验证 Nuxt→8080→8082 的真实招聘读链。
- 索引已建立；上述路由、normalizer、sheet、session、wrapper、proxy/controller 文件 coverage 返回 `no_recorded_issue / metadata_match`，只是 best-effort 信号，同时直接读取源码核实。grep 检查 pages/API 调用边界。

## 同次数据及透明适配

最终输入：`/tmp/boss-workflow-20261108-4v4wyaqq/frontend-dto.json`，root 提供的修复后同次真实 HTTP 查询 DTO。没有在本工作树 cherry-pick 后端修复，也没有重跑后端流程。原文复制保存为私有 `original-http-dto.json`。

| 现有 API 返回字段 | 同一输入来源 |
| --- | --- |
| 每条职位全部原字段 | `items[].job` 原样展开 |
| `analysisStatus` | `items[].analysisStatus` |
| `analysisModel` | `items[].model` |
| `analysisVersion` | `items[].pipelineVersion` |
| 城市 / 职位选项 | 从同批 job 的 cityName / positionName 去重 |
| 薪资热门排行 | 明确返回 `[]`，不制造另一批统计或混用 match 分数 |

`adapted-jobs.json` 保存实际适配数组，`verify.cjs` 是完整 adapter 与断言来源。未从 sampling.sourceUrl 补 jobUrl，未改地址、数组或状态；公司名称缺失仍由既有 UI 显示“未提供公司名称”。最后一次查询刻意把 chart 响应置 `[]`，用于已有空态验收，不是新造职位。

身份**只使用 fixture 会话**：隔离 storage 中的非真实标识及三个精确 GET `/api/auth/getUserBaseInfo`、`getUserPrivateInformation`、`getRouters` 合成回应。没有真实账号、登录/登出、真实权限链或真实凭据/token 验收，fixture 标识不会发送给后端。

## 实际浏览器结果

系统 `/usr/bin/chromium`，Playwright `headless:false` 外部窗口，1440×1100，独立新建 `/tmp/boss-frontend-profile-*`，未读/复制个人 profile，未用 preview/CUA/内置浏览器。允许真实网络访问的仅 loopback 页面与静态资源，API 按**完整 URL + GET** fulfill，其余中止。

`result.json` 最终 `status=verified`，7 个场景分组断言通过，64 条页面请求记录：8 条精确 fixture API、55 条本地页面/静态资源、1 条外部 Google 字体请求被中止。请求日志不含 headers、token 或用户内容。无页面 JS error。字体使用系统回退；截图不能证明原远程字体加载。

- 列表：三条同次 DTO，ID 1、2、3 顺序不变，职位名称、城市、薪资与输入一致；逐项打开详情并验证 ID，避免重复职位名混淆。
- ID 1/2：要求和福利各 9 项，DOM `allTextContents()` 与原数组逐项严格相等；包含 `必须熟练 Python, SQL (test) 😀`、`提供五险一金、带薪年假。` 等，未按逗号/括号拆分或改写标点。
- ID 1/2 状态“已完成（当前来源）”；ID 3 状态“等待分析”，其原数组均 `[]`，展示“暂无可用福利信息。”、“暂无可用工作要求。”及未确认提醒。三个详情的来源均为 `typesafe/jev-1.14.0 · jev-1`。
- 修复后真实输入的 address 均为 JSON null，DOM 均显示“—”。jobUrl 均非空：ID 1/3 指向 fixture1，ID 2 指向 fixture2 的 HTTPS zhipin job_detail URL；DOM href 逐项等于输入，并具有 `target="_blank"`、`rel="noopener noreferrer"`。**未点击外链，未访问 BOSS。**
- 既有初始态“选择条件，开始查询”和 chart `[]` 后“暂无匹配的职位样本”均通过，旧行清空。
- 额外直接调用真实 `safeSourceUrl` / `jobSamples`：原数组保持；HTTP/HTTPS 无凭据 URL 接受；null、空、javascript、data、协议相对、含用户名密码、FTP、无效 URL 共 8 类拒绝。该 helper 不是域名白名单，不能把它说成只允许 BOSS；正链接 DOM 来自未改动的新 DTO，拒绝类仅是 helper 断言。

`detail.png` 为本轮新 DTO 的实际完整窗口截图，显示数组、来源、空地址和“职位来源”按钮；这是合成展示证据，不能证明模型分类或语义质量。fixture provider 故意把候选同时作为要求/福利，因此 UI 原样保留的内容不等于准确分类。

## 排序及证据边界

当前 pages 无匹配结果 UI；`frontend/app/api/ai/matches.ts` 有 `getMatches` wrapper，但未见页面调用。未新增页面、未调用 `/api/v2/ai/matches`，未把薪资排行当匹配排序。8.75 / 6.25 及降序证据只属于交接中的真实后端 HTTP 输出，前端未验收该排序。

最初旧 DTO 的检查在 harness locator 不匹配 aria-label、以及两个“关闭”按钮造成 strict locator 歧义处失败；修正定位器后换用新 DTO。新 DTO 首次完整 UI 检查通过，但最终“零中止请求”断言遇到外部字体被隔离规则阻断；修正为仅允许该已知字体被阻断后最终通过。这些是验收脚本问题，保留于 `initial-selector-failure.json`、`old-dto-partial-selector-failure.json`、`font-block-assertion.json`，不算产品回归或旧 DTO 全通过。

## 重现与清理

在本机当前现行 5020 服务与输入文件仍存在时：

```sh
node /tmp/boss-frontend-acceptance-20261108/verify.cjs
node --experimental-strip-types /tmp/boss-frontend-acceptance-20261108/safe-links.mjs
```

使用现有 `/home/david/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/test`，没有安装依赖或增加框架；helper 脚本导入上述工作树源码。目录 0700，保留脚本和结果供 root 复查，未纳入 repository。

最终 context.close 后自有 profile 相关进程为 `[]`，自有 profile 已删除；result 保存 9 个观察到的 PID/starttime。`preserved-services.json` 核实 Nuxt2239333、API2125922、crawler2232981、analytics2233357、Python2231337 的 starttime 与部署交接完全一致，五个原端口仍监听。只清理自有 Chromium，保留全部现行服务。

未重跑全套测试、构建或 CI；未验证真实身份、真实 BOSS、真实推理/付费、真实模型质量、匹配结果 UI、移动设备或完整视觉/无障碍审计；无业务库写入/seed、抓取和 artifact 替换。私有 /tmp 结果可能随环境清理失效。

Signed-off-by: DavidHLP <lysf15520112973@163.com>
