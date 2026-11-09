# 会话身份与简历版本：本机验收（2026-11-09）

关联 [#56](https://github.com/DavidHLP/BossAnalyze/issues/56)、[#54](https://github.com/DavidHLP/BossAnalyze/issues/54)、[PR #57](https://github.com/DavidHLP/BossAnalyze/pull/57)。业务源码固定为 `60250af2a188716284067f3c44bf410207f27cb7`，基础为 `4d5ff4bdbe4bc5d444536a4e7dae19d66740aac4`。本记录仅证明本机、所列修订与验收时点的行为。

## 代码与独立审查

- Java 17 commons/API verify：15 + 47 项，零失败、错误、跳过，包含两个监听端口的集成测试；凭据契约 56 项通过。
- 固定源码的 Standards / Spec 独立审查均无确认发现；Spec 另亲跑 28 项定向测试通过。这些次数独立记录，不与父任务测试相加充当覆盖率。
- 原实例实际复现：显式提交后 `data=null`，历史已有 3 条提交。失败证据保留；修复后四个字符串接口均通过真实 HTTP 验证。

## MySQL：演练与现库迁移

隔离 MySQL 8.0.46、16KiB 页演练 47 项通过：干净/部分目标/异名等价索引/重跑、完整值容量与大小写、历史重复和脏数据拒绝、未知结构/外键/错误索引拒绝、真实 metadata lock 超时及释放后重跑。最大身份的九个实际 JwtService 合成 JWT 可逐字节保存，重复插入拒绝。保留 28 个隔离 schema、失败夹具及私有备份；无测试连接或锁残留。未改变或验证其他全局 page_size。

实际迁移前核对本机当前用户的三个 JAR JVM，仅一个预期 API 实例；停止该已核验 PID/starttime/exe/argv/cwd 的会话写入者。使用 pidfd 发送 SIGTERM，未 SIGKILL。随后重新备份 `blog_data`，恢复到独立新 schema，核对全表 data-only dump 并演练精确 SQL 两次。

在明确选中的 `blog_data` 执行同一增量迁移两次，全部退出 0。迁移前后所有表的 data-only dump 逐字节一致，包括 token 值、ID、标志与时间字段。目标列为 `VARCHAR(2560) ascii ascii_bin NOT NULL`，完整唯一索引已存在；未导入 DROP 快照、删行、撤销旧会话或更换签名密钥。迁移之后的合成登录/注销会正常新增/更新自己的会话行，不在“迁移未改行”的比较窗口内。

## 发布与真实业务验收

- 新版 `18080` canary：健康通过，实际旧无 jti 会话仍有效；同秒双登录产生不同 token/jti，均可访问，注销一个后其 replay 为 401，另一个仍为 200。测试会话清理。
- 旧版二进制在隔离 `18080` 启动、聚合健康及旧会话读取通过，探针已停止。这是回退启动/读取证明，**没有实际把最终 8080 回退为旧版本**。
- 新版最终 `8080`：同样的双会话与旧会话验收通过；旧合成会话最终注销且 replay 为 401。发布脚本共 35 项检查通过。
- 真实同源简历创建、列表、详情、重命名/保存、显式提交、历史、head/content 一致、按 resumeId 的空范围匹配及删除：27 项通过。
- 另一个独立合成简历验证 commit/merge/revert/stashPop 的字符串 `data`、分支切换和实际内容回读：18 项通过，夹具删除。
- 健康、真实 QA 登录注销、RustFS 图片上传/精确字节/安全响应头、跨用户删除拒绝与清理：24 项通过。
- 系统 Chromium 的真实 LoginForm、招聘查询空态、控制台、设置、账户菜单注销与服务端撤销：9 项通过，页面错误 0，浏览器关闭。非本机网络与无关写入被阻止。

最终 API 为 PID `219648` / starttime `515034`，来源为上述固定源码。发布时只替换 API；analytics、Nuxt、crawler/Python、Mongo 与 Docker 基础设施身份保留。主清单仅更新 API 记录及本次回执；成对部署的保护基线显式记录本次授权的 API 身份变更，再次核查通过。临时 18080 无监听。

## 私有证据与回退边界

本机私有证据根：`/home/david/.local/share/bossanalyze/delivery-recovery-20261109-sac9b48g/`。本次不可变 API 产物及回退副本在 `api-session-identity-6164ae94/`，包括 `artifact.json`、`previous-api.json`、`accepted-api.json`、`migration-result.json`、`deployment-acceptance.json`、`metadata-publication.json`、数据库备份和原始日志。该目录的环境/会话/备份文件含秘密，禁止输出或提交。部署入口 `deploy_token_api.py` 是有重复执行保护的一次性操作记录，不能盲目重跑。

回退应先读取上述当前进程记录并重新核验完整身份/端口归属，优雅停止该 API，再用保留的 `previous-api.jar`、原私有环境/配置启动；不得按裸 PID 或历史清单停止其他进程。保留宽列和唯一索引，禁止缩列、删除会话或恢复旧 SQL 快照。旧版本可再次生成同秒重复 token，因此回退后须限制登录写入并尽快恢复修复版。若更换 API 身份，须明确更新主清单与成对部署保护基线；身份/所有权不明或退出超时须停止，不自动强杀/覆盖。

本轮未调用真实 BOSS、付费模型或外部邮件；共享 provider 账本逐逻辑内容核查未变，ZDR 保持开启、质量批准/正式调度/自动写回保持关闭。人工 gold、Jev ZDR 可用性、真实模型质量、外部 SMTP 与生产环境仍未验收；本次通过不消除这些独立门槛。
