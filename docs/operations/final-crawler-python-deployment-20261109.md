# 最终 crawler/Python 本机部署与中断后状态

日期：2026-11-09。Refs #50、#51、#52；PR #53。

**历史首次部署及授权后的整栈恢复验收均通过；截至本文最后一节的实际检查，本机服务已恢复运行。** 不得把下列历史通过项描述为当前在线，也不把正式模型质量或供应商可用性算作通过。

## 版本和产物

固定业务源码 `b97ab94da701630b78bae7553622919fc7efd8bd`，树 `5486dec043a5c9bf589a95080261e9516da2bc09`。执行前远端 main 仍为该提交，未混入独立 #49 测试分支。产物根（N）：`/home/david/.local/share/bossanalyze/stack-deployment-20261108/releases/b97ab94-20261109-_2172r6g`。

源码 archive、crawler.jar、Python app/最终路径 venv 均独立只读封存。Java 17/Maven 3.9.16：64 项通过；Python 3.13.15/uv 0.9.9：75 项通过，均首次执行、零失败/错误/跳过。详见 [构建记录](final-crawler-python-build-20261109.md)。控制器最终合成离线检查25项通过，日志 `N/controller/final-offline-checks.log`；不替代真实运行证据。

## 实际执行及历史验收

| 项目 | 结果与证据 |
| --- | --- |
| 配置与进程冻结 | 实际 PID/starttime、argv/exe/cwd、metadata 与旧环境保存在私有 session；旧 control 副本保留；外部 properties 无 AI 覆盖，未发现 JVM agent 或环境 JVM options |
| 新版成对 canary | Python18090、crawler18081 同时健康，crawler 明确连18090；实际新产物/来源/身份通过 |
| 真实新 Java client HTTP | 新 JAR 中的实际客户端编译及调用通过；空 analysis→needs_review/空数组，空 jobs→complete=true，空 detail job→insufficient_evidence/null score/四维unknown |
| 认证/结构拒绝 | Python匿名401、认证非法结构422、crawler直接和5020同源匿名401 |
| 旧latest冷启动 | 使用旧 `58aa2dbbf52769688aa570a37ce95f49cef6f76e` Python和crawler，在临时端口成对健康并配对；随后 crawler→Python 清理 |
| 正式串行切换 | 停旧crawler→停旧Python→起新Python→真实Java/Python无模型契约→起新crawler；阶段 2026-11-09T10:10:00.519815+00:00 至 2026-11-09T10:10:05.645934+00:00，共5.126秒。这是本轮实测，不是未来停机承诺 |
| 正式端口验收 | Python8090/crawler8081 健康、实际路径/sourceSHA/cwd符合目标；再次真实无模型HTTP通过 |
| 预算 | 原共享账本以只读模式比较逻辑内容，前后不变；used=222121，cap=230000，未增加cap、重置或退款 |
| 门槛 | CMD_ZDR=1、quality=false、AI scheduling=false、writeback=false、SQL init=never、ddl-auto=none；采集定时注解未启用 |
| 其他组件 | 验收时API8080、analytics8082、Nuxt5020原PID/starttime不变且入口200；RustFS/Mailpit容器身份与映射/套接字基线不变 |
| 清理与状态 | 临时18081/18090无监听、canary为空、probe-child.json不存在；accept完成，current与主manifest两组件身份/sourceSHA匹配实际新进程，其他字段由选择性更新保留 |

当时接受的新 Python：PID **2809793** / starttime **7230956**；crawler：PID **2810011** / starttime **7231099**。接受时间 2026-11-09T10:10:48.842185+00:00。这些是历史身份，禁止用裸PID发信号。

## 保留的失败及现场修正

1. 独立审查发现并修正身份/停止超时、未知监听 inode、启动pending、信号清理、旧恢复成功状态和Python先验契约问题。已保留审查及反例，未删除失败断言。后续探针身份/回收异常经定向复核关闭。
2. 首次现场 inventory 因把Python专用预算/并发/重试与Java不存在的设置比较而误拒绝。改为只比较共享契约限制，Python cap230000、并发2、重试0保持原值；没有提高任何设置。
3. root Docker代理fd不可读，直接监听owner检查失败关闭。改用受信Docker API记录固定两容器ID、PID/starttime、启动时间、loopback绑定和监听inode基线；普通三个服务和所有受操作进程仍用严格直接owner守卫。它依赖Docker daemon报告，不声称已读取root代理fd。
4. 首次新版canary Python成功、crawler启动失败，Spring重复server.port参数无法转Integer。临时整对清理成功，旧服务保留。改为替换受控CLI参数，保留其余原参数，新增回归断言。
5. 临时端口预检两次记录OSError、没有启动遗留；原日志未保留errno，不能确认全部根因。预检bind增加SO_REUSEADDR，以兼容已无LISTEN但处于TIME_WAIT的端口；随后新/旧成对启动及清理通过。

## 私有证据及权限

`N/controller/` 为0700，记录及源文件0600，含真实环境的 session/state 禁止打印、上传或提交。预算路径、token及key仅在私有环境中使用。以下只有非敏感证据路径：

- `journal.jsonl`：所有实际阶段、真实PID/starttime；登记未核验身份时显式null；不将日志里的历史verified当作实时存活。
- `new-canary-acceptance.json`、`old-cold-canary.json`、`live-acceptance-before-publication.json`：当时运行验证。
- `budget-before.json`、`config-audit.json`：预算/配置审核结果，无key值。
- `accepted-current.json`、`accepted-manifest.json`：发布后私有状态快照；session中的metadata为CAS基线，会在发布后更新，不是不可变原始快照。
- `post-interruption-observation.json`：下节当前停机观察。
- 构建日志在 `N/build-logs`，控制器和探针日志在 `N/controller`；首次失败保留，未只留最终成功输出。

当前控制器内容摘要：

- `control.py`：`e90de154a569cc91027c461f181cc01a9e8c57f91cbeb31c6b068c28ba4807d1`
- `controller/probe.py`：`ef41409aaa75b652b7c16a365649348fec6428a3aafd6ca78c1f91e5a930aea9`
- `controller/offline_checks.py`：`9bce136783cc93a4f0313fc01554ed9f014e1d55cdde55e75d703c4ef32c71ff`

## 中断后停机观察（历史状态，后续已授权恢复）

在 2026-11-09T14:12:43.857686+00:00 重新只读检查：8080/8081/8082/8090/5020均无监听；已接受的新Python/crawler身份均不存活。RustFS容器于 `2026-11-09T10:13:10.820911009Z`、Mailpit于 `2026-11-09T10:13:11.059287157Z` 退出，exit0且OOM=false；MySQL/Redis容器也已退出。Python/crawler日志均有正常关闭记录。**没有足够证据认定是谁或为何停机。** 本次恢复检查未发送信号、未启动容器或组件。

current/manifest 的成功条目记录的是上节历史部署接受，不能作为当前存活证明；`control.py status` 明确返回两项alive=false。当时保护基线不匹配；初版手动回退仅在发布时检查，不能保证动作前拒绝。该审查缺口已由下文26项版本修复。已询问应保持停止还是恢复整套服务；未获得范围选择前不扩展原计划，尤其不擅自重启DB/API/analytics/Nuxt/RustFS/Mailpit。

## 回退命令与当前前置限制

原运行环境完整且保护身份未漂移时，本批唯一回退入口：

```sh
N=/home/david/.local/share/bossanalyze/stack-deployment-20261108/releases/b97ab94-20261109-_2172r6g
PYTHONDONTWRITEBYTECODE=1 python "$N/control.py" status
PYTHONDONTWRITEBYTECODE=1 python "$N/control.py" rollback
# root另核验旧版健康、配对和预算后才执行：
PYTHONDONTWRITEBYTECODE=1 python "$N/control.py" accept
```

回退使用保留 `existingRoot/latest` 的旧Python→旧crawler，写真实重启身份，目标success保持false；不是更古老rollback目录，不复制旧PID快照。身份未知、SIGTERM失败/超时或metadata冲突立即停止，不强杀、不覆盖。

**对上述停机观察时点，整栈停止且基线已变，回退命令不能当作整栈恢复方案。** 必须先决定恢复范围并重新核验基础设施和保护身份，保留原session/journal，不以覆盖旧session规避守卫。未为测试而在正式端口额外回退；旧版真实冷启动已验，但没有故障切换演练的运行证据。

## 未覆盖及风险

未做真实BOSS/浏览器、真实provider/付费请求、ZDR可用性复测、正式领域质量、业务库DDL/DML/seed/flush或生产验证。ChromeDriver仍缺失/不可执行，日志为已知初始化提示，采集未运行。ready200不代表供应商可用；AI质量、调度和写回继续关闭。多文件metadata不是事务，发布仍需root独占或所有写入者遵守advisory锁。当前重新启动验收待定，PR/远程CI状态另行记录。

## 用户扩大授权后的整栈恢复（最新验收）

用户随后明确要求“除BOSS数据获取外全部做到可交付”并选择“恢复整套本机服务并重新验收”。本节是新授权范围，取代上文停机观察作为最新状态；原计划排除其他组件/业务DDL的限制仅适用于首次两服务部署，不再阻止本次明确授权的恢复和增量迁移。

在 2026-11-09T14:28:43.990591+00:00 完成重新接受，私有证据根（D）为 `/home/david/.local/share/bossanalyze/delivery-recovery-20261109-sac9b48g`：

- 原四个 MySQL/Redis/RustFS/Mailpit 容器直接启动，复用既有数据卷；Mongo复用原数据目录，未重建数据库或凭据。
- API PID41032、analytics PID41180、Nuxt PID41499、Mongo PID40988；最终 Python PID41534/start75987、crawler PID41573/start76139，二者继续使用目标b97ab94独立产物。验收时PID/starttime及监听再次核验。
- 六服务健康/入口200，真实Java→Python无模型HTTP重验通过。同源合成QA登录、账户读取、server logout及撤销后401通过；图片上传、实际字节、安全响应头、跨用户403及自身fixture删除通过；共24条HTTP断言。
- 四个实际招聘API均200/合法空数组，非法坐标轴400；仪表板缺失快照保持204，HTTP无重复Transfer-Encoding；主API简历归属到crawler链路返回403而非503。
- 实际部署API JAR的EmailService直接发送唯一example.invalid合成邮件，认证Mailpit捕获1条；保留为私有测试证据，无外部SMTP投递或真实收件人。
- 新表 `job_ai_analysis` 缺失：先对blog_data/boss_data做私有一致性备份，将boss_data恢复到唯一新schema并连续执行迁移两次，核对18列/五列唯一身份索引；再在boss_data执行同一迁移并重跑。原业务表行数、blog_data全部行数与角色唯一约束不变，新表0行。token迁移明确跳过。证据D/migration-result.json；备份600权限保留，未删除隔离schema。
- 共享预算仍222121/230000，未调用真实provider或BOSS；CMD_ZDR=1、quality/scheduling/writeback仍关闭。

原session、state、current和manifest已先私有备份到D的`*-before.json`，保留历史证据。新范围授权和替换后的保护身份单独写`replacement-baseline-authorization.json`；仅在明确授权的整体恢复中更新CAS基线，不把进程重启伪装成旧身份仍存活。最终current、master manifest及fullStackRecovery版本记录写入实际重启身份，阶段accepted。

最终两轴审查对同一手动回退缺口分别计Standards P2、Spec P1，已在实际控制器新增 `rollback()` 入口：phase→保护baseline→metadata CAS全部通过后才能调用restore。新增回归断言证明基线漂移或CAS冲突时restore未调用；26项离线检查通过。自动故障恢复原策略保持；未为验证而在正式端口回退。

当前最新控制器摘要（替代上文历史版本）：

- `control.py`：`5df984cf36a138ed93d1335086857e51e0eb81a3e9ed3c80f59e8ebf3d0f8d71`
- `controller/probe.py`：`ef41409aaa75b652b7c16a365649348fec6428a3aafd6ca78c1f91e5a930aea9`
- `controller/offline_checks.py`：`93d70af9564612b0c273cd6f0a12b0e528ebcd68419b203abea82bbbdc7006be`


回退仍需使用本批控制器及当前实际基线；若出现新的身份变化/状态冲突，它现在会在任何手动回退服务动作前拒绝，不应通过删除session绕过。完整全栈恢复命令不是简单rollback；本次私有D/restore.py仅记录已批准的单次恢复，重复执行会拒绝覆盖记录。

外部限制：2026-11-09重新读取[CommandCode官方Provider API文档](https://commandcode.ai/docs/provider#zero-data-retention-zdr)，明确说明Jev和固定jev-1.14.0没有ZDR上游，带header请求必然422。未重复消耗预算做已知不支持的推理，不降级隐私或更换固定模型。人工gold仍缺失，质量不批准；本节验收的是可运行应用与契约，不是模型质量、真实招聘数据或生产验收。
