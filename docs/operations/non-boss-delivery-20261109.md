# 非 BOSS 采集部分交付记录

日期：2026-11-09。任务 [#54](https://github.com/DavidHLP/BossAnalyze/issues/54)：除 BOSS 数据获取外，将剩余任务推进到可交付；自主恢复本机整栈并重新验收。本文区分已验证的工程交付与未满足的外部上线门槛，不宣称整个目标已经完成。

## 已验证的工程与本机交付

| 要求 | 当前证据 | 结论 |
| --- | --- | --- |
| 最终 crawler/Python 部署 | [配对部署与恢复记录](final-crawler-python-deployment-20261109.md)：固定 b97ab94 产物、新版成对 canary、旧 latest 冷启动、正式串行切换与接受 | 已验证 |
| 整套本机服务恢复 | API8080、crawler8081、analytics8082、Python8090、Nuxt5020、Mongo及原 MySQL/Redis/RustFS/Mailpit；原数据卷与配置保留 | 已验证；属于本机运行，不是生产发布 |
| 身份和同源代理 | 24条实际HTTP断言含登录、账户读、注销、撤销后401；归属拒绝403；四招聘查询200/合法空数组；无快照204；无重复Transfer-Encoding | 已验证 |
| RustFS业务存储 | 同源上传、真实PNG字节读取、安全头、跨用户删除403、自身测试图片删除；此前重启持久化记录保留 | 已验证 |
| 数据库增量部署 | 私有一致性备份→新schema恢复→迁移两遍→boss_data执行并重跑；job_ai_analysis 18列、五列唯一身份索引、0新分析行；原业务行/角色唯一约束不变 | 已验证；token迁移跳过 |
| 本地邮件 | 实际API JAR的EmailService→认证Mailpit，唯一example.invalid合成邮件捕获1条；aggregate/readiness均200 | 已验证；外部投递不包含在此结论内 |
| 运行安全与回退守卫 | 手动rollback在任何组件动作前检查phase、保护基线与metadata CAS；26项离线反例及两轴独立复核通过 | 已验证指定边界；多文件发布仍需独占/协作锁 |
| 测试稳定性 | [PR #49](https://github.com/DavidHLP/BossAnalyze/pull/49)合并，保留部分结果、deadline、cancel/join与资源清理断言；当前完整Python套件78通过 | 已验证 |
| 受控非付费全链路 | 47条实际断言：系统Chromium本地fixture、Java→Python HTTP、当前Store/事务、隔离MySQL、缓存恢复、503零写回、部分超时保留结果 | 已验证；provider回应为明确合成数据 |
| DTO前端展示 | 上述真实HTTP DTO由现行Nuxt消费，7条展示/Unicode数组/来源/空态检查通过 | 已验证；会话为隔离fixture |
| 真实浏览器用户流程 | 系统Chromium独立context，现行LoginForm+API合成QA登录，招聘查询空态、dashboard/profile/settings、账号菜单注销、服务器撤销401；9条检查、0页面错误、浏览器关闭 | 已验证；无真实个人账号或平台访问 |
| 无标注拒绝批准 | 空gold/predictions/development实际CLI返回exit2、status=unverifiable、samples=0；现行readiness的calibrated/automaticWritebackAllowed均false | 已验证拒绝路径；不是质量验收 |

以上过程中未访问真实 BOSS、未发送真实简历给供应商、未调用外部模型推理、未改变既有230000预算cap；共享账本保持222121。独立fixture有自己的合成配置和账本，不能据此开启正常实例的quality/writeback。

## 版本与合并

- [PR #53](https://github.com/DavidHLP/BossAnalyze/pull/53) 已合并：`77ffc8dc13e3854b1aaf3f6ed3c089c238ea2524`。
- [PR #49](https://github.com/DavidHLP/BossAnalyze/pull/49) 已合并：`12dda232df59c4060e2569e41b8f6fb87a92ced7`。
- 二者各自精确head合并前四项远端检查均SUCCESS，无未解决review thread；Standards/Spec独立审查无确认剩余问题。这里不是对后续新提交的CI结论。
- 正常crawler/Python仍使用可追溯的b97ab94不可变产物；两个merge只引入部署文档和测试改动，未替换业务实现。API/analytics/Nuxt原产物业务源码相对b97ab94亦无差异。
- 本轮代码变更仅将受控入口默认Python从已失效的临时venv改为仓库`ai-service/.venv/bin/python`，保留WORKFLOW_PYTHON显式覆盖；实际完整入口已用该默认值成功运行。已有PID复用离线回归1项通过。

## 可复现入口

基础配置和启动见[本地开发说明](../local-development.md)，不要使用初始化快照覆盖现有数据库。安装锁定Python依赖后运行：

```sh
uv sync --frozen --project ai-service
(cd ai-service && uv run --frozen pytest)
python tools/workflow-fixture/run.py
# 使用上一步输出的实际DTO路径：
node tools/workflow-fixture/frontend.cjs /private/current-run/frontend-dto.json
```

受控入口依赖已有Java17/Maven、系统Chromium和loopback MySQL；机器路径不同时按[受控流程说明](controlled-workflow-20261108.md)设置已有工具链覆盖。它会创建唯一隔离schema，保留审计，关闭自己的服务/浏览器，不修改正常实例全局门槛或业务表。不把里面的确定性评分当真实模型效果。

当前本机私有证据根：`/home/david/.local/share/bossanalyze/delivery-recovery-20261109-sac9b48g`。实际HTTP/恢复/迁移/邮件/浏览器结果分别为`http-acceptance.json`、`full-stack-acceptance.json`、`read-api-acceptance.json`、`migration-result.json`、`mail-probe/result.json`、`browser-acceptance.json`。配置/环境/备份仅本机0600，禁止打印或上传。

本轮受控全链路：`/tmp/boss-workflow-20261108-43j873lf`，47条checks、result=verified、cleanup.liveOwnedProcesses=[]、隔离schema=`workflow_20261108_af779f82dfe1`。DTO展示证据：`/tmp/boss-workflow-frontend-TZSYCN`，7条检查，remainingOwnedProcesses=[]。这些临时证据可能被系统清理；重新运行会产生新目录，不能把已关闭fixture端口当常驻服务。

浏览器验收曾因脚本未点击查询、以及未等待异步注销完成而误判；修正验收时序后9条通过，产品会话逻辑未为迎合脚本改变。失败记录保留并脱敏，相关测试会话已确认撤销。正常实例在验收后保留运行；前端外部字体请求在隔离浏览器里阻止，没有真实平台导航或未预期写入。

## 尚未满足的非BOSS外部门槛

| 门槛 | 权威证据与限制 | 当前处理 |
| --- | --- | --- |
| 固定Jev 1.14.0与ZDR兼容 | 2026-11-09[官方Provider API说明](https://commandcode.ai/docs/provider#zero-data-retention-zdr)明确Jev和jev-1.14.0均无ZDR上游，带header必然422/cmd_zdr_no_providers | 不重复付费探测、不换固定模型、不去掉ZDR；真实数据推理不能验收 |
| 独立人工标注及领域质量 | 当前没有满足来源/福利/简历匹配排序要求的reviewed human gold/holdout；AI生成标签和合成fixture不提供独立真值 | 评估入口可交付，质量明确unverifiable；不批准调度或自动写回 |
| 外部SMTP授权与投递 | 本机Mailpit已通过，当前没有经此项目验收的外部SMTP配置/投递证据 | 保留本地捕获；不把其成功冒称外部邮件送达 |

上述门槛继续由[#48](https://github.com/DavidHLP/BossAnalyze/issues/48)、[#54](https://github.com/DavidHLP/BossAnalyze/issues/54)跟踪。Provider metadata GET200、readiness200、合成scored分数、绿色CI都不能替代它们。BOSS真实获取及登录态验收按用户要求排除（#46）；当前简历编辑UI占位及技能图谱/薪资预测属于已披露的后续产品方向，不在此记录中冒称完成。
