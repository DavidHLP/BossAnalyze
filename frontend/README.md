# BossAnalyze 前端

Vue 3、Vue Router、Vite 和 shadcn-vue。界面布局与业务状态依据 [Penpot 的 BossAnalyze 画板](https://design.penpot.app/?screen=workspace&team-id=371c6737-0d30-8051-8008-f66b52632bfa&project-id=371c6737-0d30-8051-8008-f66b5263d11f&file-id=42288968-6e5e-808d-8008-f86469e0027c&page-id=28f66bd8-6036-80ec-8008-f86a5c4db14c)，样式采用 `a6NRqzdI` preset。

## 本地运行

在 `frontend/` 中使用 Node.js 22.13+ 和 pnpm 11.24.0：

```sh
pnpm install --frozen-lockfile
pnpm dev
```

打开 `http://127.0.0.1:5020`。登录和授权以现有后端接口为准。后端启动步骤见[本地开发说明](../docs/local-development.md)。

## 验证与生产启动

```sh
pnpm type-check
pnpm test
pnpm build
pnpm start
```

`pnpm test` 使用 Node.js 测试运行器执行 `tests/*.test.mjs`。单独运行简历相关用例可执行 `node --experimental-strip-types --test --test-concurrency=1 tests/penpot-pages.test.mjs`。

生产入口读取 `dist/`。`HOST` 和 `PORT` 默认分别为 `127.0.0.1` 和 `5020`。
开发和生产都在原路径代理 `/api/**`。AI v1/v2 走 8081，其余走 8080。
`BOSS_TRUSTED_PROXY_IPS` 可配置可信代理的准确 IP 列表。只有匹配的直接代理可提供单个合法的转发地址。
生产入口为 HTML 导航提供页面回退，包括带点的 `/console/*` 路径。缺失的 `/assets/*` 返回 404，现有静态文件支持单段字节范围请求。
外置反向代理部署需要为前端页面启用 HTML 路由回退，并将 `/api/**` 交给保留来源 IP 清理规则的 API 代理。

## Penpot 05–07 页面

- `/register` 使用现有邮箱注册接口。验证码发送需要 SMTP 配置。
- `/resumes` 提供本人简历列表、编辑及版本历史。默认关闭私有图片素材接口。只有在后端已对图片查看和地址查询执行对象所有者校验的环境，才设置 `VITE_RESUME_PRIVATE_IMAGES=true`。
- Word 导出默认关闭。隔离验收环境可在构建或启动 Vite 前设置 `VITE_RESUME_DOCX_EXPORT=true`。Word 与 WPS 均通过打开、编辑、打印验收，且中文可选中、正文顺序与列表正确后，才在发布构建启用。
- 简历预览用结构化 Markdown 渲染标题、列表、分割线、表格、代码、加粗、斜体、链接和 `asset:` 图片引用。原始 HTML 按文本显示，链接仅接受 `https://`、`http://` 和 `mailto:`。
- 数学公式用 `$...$` 写在同一行，用单行 `$$...$$` 或独占行的 `$$` 包围块级公式。Pandoc 货币守卫要求开 `$` 后和闭 `$` 前没有空白，闭 `$` 后不能紧跟 ASCII 数字，所以 `$100-$200` 仍是普通文本。用 `\$` 输入字面的美元符号。在线预览、本地 HTML 和打印按需加载 KaTeX，并统一输出 MathML。
- 编辑器在当前标签页的 `sessionStorage` 按账号、登录会话和简历分开暂存正文及提交备注。刷新后可选择恢复或清除。已确认保存、储藏和主动丢弃会清理暂存，离开登录会话会使旧草稿失效。服务端储藏仍可在版本历史中取回。
- 「导出简历」支持浏览器打印或另存为 PDF、下载独立 A4 HTML、Markdown 原文和 LaTeX (`.tex`)。导出文件由浏览器生成，HTML、打印和 `.tex` 用文字标注图片素材。LaTeX 仅用于单向本地导出，请用本机 `xelatex -no-shell-escape -halt-on-error resume.tex` 编译，禁用 `--shell-escape`。`.tex` 不含私有图片，不能回导简历版本库。公式校验失败时，可从导出弹窗的行列提示跳到 Markdown 源码修正。
- `.tex` 模板仅在当前导出弹窗内生效，每次打开默认选「基础」。基础（A 档）和现代简洁（B 档）都是 16 mm 边距、无页码的 A4 单栏；学术简历（B 档）为 20 mm 边距、居中页码，分区留白更多，长表可跨页。
- B 档模板要求正文第一块为唯一的一级标题（`# 姓名`）。紧随其后的段落用作居中联系行，保留链接，不拆分邮箱、电话等字段。省略联系行不影响导出；没有二级标题（`## 分区名`）时，正文按原顺序排版。若缺少首块一级标题或有多个一级标题，弹窗会说明原因，可一键切换为基础模板；直接下载也使用基础模板。模板不会改写 Markdown 或影响外部导出格式。
- 如需外部导出，在构建前配置经过批准的 HTTPS `VITE_EXPORT_URL`。浏览器展示目标地址，用户确认后才发送当前简历正文和导出参数。服务须返回可打开的 HTTPS 结果，字段为 `url`、`downloadUrl` 或 `previewUrl`。
- `/console/logs` 使用数据看板访问权限。手动触发另外要求 `logs.analysis.trigger`，成功回执表示已启动后台分析。

## Playwright 浏览器测试

`tests/*.browser.mjs` 使用 Playwright 和 Chromium，单独于 `pnpm test` 执行。先准备隔离的本机审计 Mock 服务和合成数据。简历测试会写入测试记录，要求 Mock API 响应包含 `X-BossAnalyze-Preview: mock`，并在隔离的开发前端启用 `VITE_BOSS_MOCK_PREVIEW=1` 以覆盖图片素材流程。普通 `pnpm dev` 使用实际 API 代理，请勿将它直接作为写操作测试的目标。

在 `frontend/` 运行简历浏览器测试，按本机实际安装路径调整变量。

```sh
RESUME_MOCK_URL=http://127.0.0.1:5129 \
PLAYWRIGHT_MODULE=/absolute/path/to/playwright/index.mjs \
CHROMIUM_PATH=/usr/bin/chromium \
node --test --test-concurrency=1 tests/resume-interactions.browser.mjs
```

本仓库未内置上述审计 Mock 服务。各套测试所需的 Mock 地址和浏览器配置见对应脚本。

验收 Word 导出时，在测试命令中也设置 `VITE_RESUME_DOCX_EXPORT=true`，以运行懒加载、取消、失败重试和下载用例。
