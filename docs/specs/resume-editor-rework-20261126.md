# 简历编辑器主流化改造规范（2026-11-26）

> 状态：**执行中**。规格 issue [#119](https://github.com/DavidHLP/BossAnalyze/issues/119)；集成分支 `feat/resume-editor-rework`。任务图为 GitHub Issues 子工单，依赖以 `Blocked by` 记录。

## 1. 背景与证据

简历编辑器（`/resumes/:id/edit`）以 Markdown 为正文源，带 git 式版本、A4 预览、KaTeX 公式与本地导出（打印 PDF、HTML、Markdown、.tex）。

市场对照（2026-11 检索）：导入/粘贴现有简历、自动保存与状态、模板即时预览、分页提示已是标配；国内产品（五百丁、超级简历）提供完整度诊断与 JD 关键词；CN 网申常要求 DOCX；ATS 官方指南要求避免表格、多栏、图形与异形项目符号。

代码审计确认的现状缺口：

- 编辑区五个同权重按钮（`frontend/src/pages/resumes/edit.vue:451-457`），git 术语直接暴露。
- 草稿存 `sessionStorage`，写入每次按键同步；构造 store 时横扫其他账号/简历的键（`frontend/src/lib/resumes.ts:35-69`）。
- 预览与打印不是同一排版：预览内容宽 600px，打印正文 672px，字号也不同（`ResumePreview.vue:28`；`resumes.ts:495-505`）。
- 图片在 HTML/PDF/.tex 中仅为文字占位（`resumes.ts:470`；`ResumeExport.vue:209`）。
- 新建只生成 `# 标题`（`api/resume/resume.ts:81`），无骨架、无导入。
- 服务端提交无事务、无乐观锁（`ResumeVersionControlService.java:64-86`）。
- 导出使用编辑区当前内容，无未保存提示；无 DOCX；无主题、无页数、无内容检查。

## 2. 目标与不变量

目标：**非开发者首次使用可独立完成 新建 → 填写 → 保存 → 可信导出。**

- I1 Markdown 是唯一正文源；骨架、粘贴导入、章节表单只生成 Markdown。
- I2 只有回读确认（`headCommit === commitId` 且 `content` 一致）才显示“已保存”，才清草稿。
- I3 版本只由明确用户动作产生；主题等外观字段不产生 commit。
- I4 所有工作区写入口校验基线 `workspaceRevision`；失败/落败快照对所有读取路径不可见。
- I5 “恢复服务器正文”只改编辑区，不调用 reset/checkout。
- I6 导出必须说明来源（当前内容/服务器正文）、是否含未保存修改、图片是否嵌入、文件名。

## 3. 三方裁决记录

三轮协商：codex（架构）、Claude 产品评审、Claude 技术审计并行 → 交叉裁决 → 双向签发。签发条件已并入工单。

| 争议 | 最终裁决 |
| --- | --- |
| 草稿 7 天持久化 | 移出本期。只修会话草稿的横扫清理、状态如实、存储失败降级。7 天涉及共享设备隐私与 24h 会话语义，单独立项 |
| 预览主题 | P1 两套（basic/modern），贯穿预览/HTML/PDF/DOCX；`.tex` 独立，不宣称同款 |
| DOCX | P1，Word/WPS 实际打开验收通过才点亮按钮；不做 HTML 改后缀 |
| 页数 | P0 只给“约 N 页”+ 手动紧凑（10pt 下限）；不自动删内容；共享排版未统一前不写精确值 |
| 并发 | P0：`workspaceRevision` + 409；拓扑不支持事务时用 Redis 简历级锁 + 条件更新 |
| 章节输入 | P0 无条件做三章单向表单，Markdown 为高级入口；可用性测试只决定是否要做双向 |
| 术语 | P0 编辑页与历史页同改；分支/合并/检出/重置默认折叠 |
| 导出图片 | P0 图片真实渲染进 HTML/打印（client-side 缩放 + data URL） |
| 定制版本 | P1 复制为独立简历；分支留在高级区 |
| 恢复 | GET 重读服务器正文，只改编辑区 |

## 4. 工单与依赖

### P0

| 工单 | 范围 | 依赖 |
| --- | --- | --- |
| [#120](https://github.com/DavidHLP/BossAnalyze/issues/120) | 列表时间戳容忍 epoch 毫秒 | — |
| [#121](https://github.com/DavidHLP/BossAnalyze/issues/121) | 术语、动作分层、移动吸底 | — |
| [#122](https://github.com/DavidHLP/BossAnalyze/issues/122) | 导出来源与格式损失说明 | — |
| [#123](https://github.com/DavidHLP/BossAnalyze/issues/123) | 草稿隔离、状态如实、恢复服务器正文 | — |
| [#124](https://github.com/DavidHLP/BossAnalyze/issues/124) | 新建三入口与纯文本粘贴导入 | — |
| [#125](https://github.com/DavidHLP/BossAnalyze/issues/125) | 三章单向表单 | #124 |
| [#126](https://github.com/DavidHLP/BossAnalyze/issues/126) | 工作区 revision 并发护栏（后端） | — |
| [#127](https://github.com/DavidHLP/BossAnalyze/issues/127) | 409 冲突界面 | #123, #126 |
| [#128](https://github.com/DavidHLP/BossAnalyze/issues/128) | 共享渲染排版与图片真实导出 | — |
| [#129](https://github.com/DavidHLP/BossAnalyze/issues/129) | 页数估算与手动紧凑排版 | #128 |

### P1

| 工单 | 范围 | 依赖 |
| --- | --- | --- |
| [#130](https://github.com/DavidHLP/BossAnalyze/issues/130) | 本地 DOCX 导出（含 Word/WPS 人工验收） | #128 |
| [#131](https://github.com/DavidHLP/BossAnalyze/issues/131) | 两套主题与外观 PATCH | #126, #128, #130 |
| [#132](https://github.com/DavidHLP/BossAnalyze/issues/132) | 内容检查（可补充项，无评分） | — |
| [#133](https://github.com/DavidHLP/BossAnalyze/issues/133) | 复制简历为独立记录 | #124, #126 |

## 5. 顺序与并行

- 首批并行：#120、#121、#122、#123、#124、#126、#128。
- 第二批：#125（#124 后）、#127（#123、#126 后）、#129（#128 后）、#132（独立）。
- 第三批：#130（#128 后）、#133（#124、#126 后）。
- 最后：#131（#126、#128、#130 后）。
- 关键路径：#126 → #127；#128 → #129 → #130 → #131。

## 6. 发布门槛

1. 产品 + QA：窄屏与桌面各走 新建（骨架）→ 三章表单 → 保存 → 版本历史 → 导出 PDF；旧 Markdown 文档高级内容无损。
2. 前端 + QA：三种导出来源产物；有图/无图、取回储藏、回读失败；两标签两账号草稿隔离。
3. 后端 + 运维：与生产同拓扑 Mongo 验证 409、竞争后无可见孤立版本、旧记录兼容、跨账号拒绝。
4. P1：Word 与 WPS 各打开/编辑/打印 DOCX；预览、HTML、打印 PDF 三处对照；页数文案按实测精度发布。

## 7. 风险与未决

- 生产 Mongo 拓扑未知；#126 的 spike 决定事务或 Redis 锁。两者都不可行时完整 P0 暂缓，只发 #121–#124。
- 图片 P0 进 HTML/打印；DOCX 仍文字占位（P1），需在导出界面对格式分别说明。
- KaTeX MathML 异步加载影响页数测量时点，字体就绪后重测。
- 未做真实 ATS 抽取、Word/WPS 实测、并发实测；上述验收是发布前必做项。

## 8. 非目标

WYSIWYG 块编辑器、PDF/DOCX 解析导入、自动压缩或删内容、ATS 分数、拼写检查、默认 7 天 localStorage 草稿、分支派生定制稿。
