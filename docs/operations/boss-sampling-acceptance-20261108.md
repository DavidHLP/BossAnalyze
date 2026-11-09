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
