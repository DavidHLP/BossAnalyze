# BOSS 只读采样验收记录

范围：参考 get_jobs 的浏览器列表与详情响应设计，独立实现 BOSS 只读采样；不投递、聊天、发送简历、调用模型或写业务数据库。代码与离线检查已经落地，完整登录态 CLI 采集尚未验收。

## 已执行

- 整合后的 Java 17 commons/crawler verify：53 项通过，失败、错误、跳过均为 0。日志 `/tmp/boss-sampling-postfix-integrated-verify.log`。
- 三项 Spec 修复：无 JSON 的明确详情导航允许受约束 SSR 读取；启动预置取消在浏览器操作前退出；首卡切换等待出现验证、取消或关窗后不再点击下一张。
- 真实公开页 DOM 观察：标题唯一，但 `.job-sec-text` 有两个，分别属于 `.job-detail-section` 的职位描述及 `.company-info-box` 的公司介绍；这是浏览器读取证据，不是 CLI 采集成功。工具据此仅选择职位区的正文，合成 fixture 同时保留两个区域并断言公司介绍不会进入输出。
- 正文范围修正后的 verify：53 项通过，失败、错误、跳过均为 0，日志 `/tmp/boss-sampling-dom-scope-verify.log`。随后完善空正文 fixture 的区域结构，并定向执行两个相关测试，全部通过，日志 `/tmp/boss-sampling-dom-empty-focused.log`。
- 实际打包 CLI 预取消：使用预置取消文件及不可用 loopback CDP 地址，返回预期 exit 2 / cancelled，标注文件为空。证据 `/tmp/boss-sampling-cancel-_ti02a_z/result.json`。零 Playwright 创建由已有内存回归断言验证；此 CLI 调用不替代网络连接审计。

## 证据边界

此前浏览器已读到一条公开职位描述及福利文本，私有样本保持待标注；该记录不等于新 CLI 的执行结果。页面曾不产生详情 JSON，也曾跳转安全检查；检查时未解决验证码或反复请求。复查公开 DOM 只能证明所观察布局，不能保证所有 BOSS 页面一致。

尚未通过：真实有效登录态复用、完整列表点击到详情响应再导出的 CLI 链路、真实浏览器取消/窗口生命周期、福利 JSON 字段、长期采集稳定性。福利字段维持 null，不用技能标签代替。其他招聘平台未测试。

临时浏览器检查页已关闭；既有用户标签页、原服务、业务数据库及模型预算未改动。独立人工标签、领域质量和排序验收未完成，Jev 正式调度、质量批准和自动写回不由本次检查开启。

## SSR 期间到达 JSON 的竞态修复

独立复核曾用三个内存反例确认：SSR 资格检查之后、page.content() 期间到达的未完成请求、错配身份或 API 错误会被 SSR 成功掩盖。该失败记录保留在 `/tmp/boss-sampling-postfix-rereview.md`，不以此前 53 项通过覆盖它。

修复在正文读取、最后一次页面 URL 检查后，再确认停止信号及 capture 状态：合法 JSON 优先；未完成请求或错误不得转成 SSR 成功；api_error 保持停止行为。没有放宽来源、身份、代际或 JSON 校验。现有采样测试文件新增四种真实 capture 事件时序，验证三种失败均无 SSR 成功或标注，合法结果仍走 JSON 来源。

最终 Java17 commons/crawler 离线 verify 共54项通过，失败、错误、跳过均0；日志 `/tmp/boss-sampling-ssr-race-final-verify.log`。新增时序由 Mockito 在内存中触发，不是对真实 BOSS 发生频率或完整 CLI 的证明。真实登录态采集、福利字段与正式质量门槛仍未关闭。


## 本轮匿名 CLI 运行（2026-11-08，本地）

基线 `066bad562bbaf4a9d20a85b3984496e33beaf514`。最小修正：无 CDP/Cookie 的明确详情 URL 直接尝试公开读取，列表仍进入手工登录。没有新增 flags、合成 Cookie、依赖升级，JSON/SSR/身份/停止判断未改变。

- 现有 loopback Chromium 端口 40103 的 `/json/version` 返回 HTTP 错误，9222 不可连接；没有得到有效 CDP，也没有用户指定 Cookie 文件。未枚举个人 Profile、导出 Cookie、改用户浏览器配置或重启浏览器；列表未运行。
- 使用原 Playwright 1.39 Java CLI 从官方源安装 Chromium 1084/FFmpeg 1009 到仓库外 0700 私有缓存。`com.microsoft.playwright.CLI --version` 本地检查 exit 0，Version 1.39.0。
- 对明确公开详情 `https://www.zhipin.com/job_detail/125f665f897603f30nx_0tS-GFJR.html`、`--limit 1`，打包 PropertiesLauncher 实际调用在外层 50 秒上限超时；wrapper exit 1，未观察到 CLI 自身退出码或状态输出。未确认该次已发出网站请求。
- 随后以相同已编译 CLI 的直接 classpath 入口排查，实际启动私有缓存中的临时 Chromium，但仍在外层 40 秒上限超时，wrapper exit 1，未观察到 CLI 自身退出码或状态输出。两次调用相隔超过 2 秒，仅同一详情 URL，无列表滚动或后续详情尝试；这不是成功的完整采集链路，也未确认挂起发生在启动、导航还是清理。
- 两个新输出目录的 samples/labels 文件均为空，合法样本 **0**，真实标签 **0**；没有当前 DOM/JSON 福利证据，rawBenefits 维持 null。没有把技能标签当福利，也没有复用此前样本冒充本次结果。没有当前登录/验证码页的可读证据，不能将此次超时改称 auth_required 或 verification_required。
- 超时由外层 subprocess 终止 Java 子进程；事后按本轮私有缓存 executable 路径检查无遗留浏览器进程。用户浏览器/标签页未操作。该证据不等于正常 finally 关闭的真实验收，真实取消/关窗/附着会话生命周期仍未验收。
- 打包 CLI 预置取消实测 exit 2 / cancelled，labels 为空。零 Playwright 创建及 attached 不调用 Browser.close 的证据仍为离线 mock 断言，不冒称真实会话保护测试通过。
- 受影响 Java17 commons/crawler verify 最终 exit 0，55 项测试（commons 15 + crawler 40，采样 18），失败/错误/跳过均 0，日志 `/tmp/boss-final-runtime-verify-final.log`。首次运行因新增测试传 null 给既有 helper 而失败，修正测试输入后重跑；首次失败日志 `/tmp/boss-final-runtime-verify.log` 保留。

具体 argv、外层超时、权限和私有运行目录由本机交接 `/tmp/boss-final-boss-runtime-handoff.md` 指向。完整真实 JD/PII/会话未入 Git。本轮没有业务服务启停、数据库写入、模型调用、SMTP/ai-service 修改、push/PR/watch。没有真实有效会话与可读页面，线上采样、福利含义与独立标注评估仍未通过。
