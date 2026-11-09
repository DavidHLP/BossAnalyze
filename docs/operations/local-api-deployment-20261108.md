# 本地 API 部署与回退验收（2026-11-08）

仅本机 local-dev；不是生产发布。执行时间为 2026-11-09 UTC（本机 11 月 8 日）。本次只部署 Java API，仓库只新增本文；未推送、创建 PR 或监控 PR。

## 来源与构建

- 源码 HEAD：`adee0f0fe0003ef1a27dbc64b417fe83da8f3268`。API、commons、backend/pom.xml 相对 `066bad562bbaf4a9d20a85b3984496e33beaf514` 无差异。
- Java：Zulu 17.68.203.0；Maven 3.9.16，使用已安装绝对路径，没有更改全局工具配置。
- 命令（本 worktree 根目录）：

```sh
JAVA_HOME=/home/david/.local/share/mise/installs/java/zulu-17.68.203.0 \
PATH=/home/david/.local/share/mise/installs/java/zulu-17.68.203.0/bin:$PATH \
/home/david/.local/share/mise/installs/maven/3.9.16/apache-maven-3.9.16/bin/mvn \
  -f backend/pom.xml -pl api -am verify
```

实际退出码 **0**。Surefire XML 合计 **58 tests / 0 failures / 0 errors / 0 skipped**：commons 15，api 43。未重复 frontend/Python 测试。构建日志、退出码及 XML 汇总保存在下面私有目录。未新增源码或测试。

## 私有操作资料

所有路径均为当前机器，不能作为可移植部署包或生产配置。

私有版本目录 `R=/home/david/.local/share/bossanalyze/local-dev/api-deployment-20261108`（0700）；操作文件、日志、manifest 均限制为 0600。

- `R/api.jar`：本 worktree 最新构建产物的独立副本；运行不依赖 target。
- `R/original-api.jar`：原运行 JAR 的字节相同备份；原路径文件也保留。
- `R/original.json`：原 PID/starttime、完整 argv、环境、exe、cwd、artifact 路径；包含私有环境，仅由本机程序读取，不展示。
- `R/control.py`、`R/commands.txt`：精确启动、PID/starttime 守卫、优雅停止及回退/再次部署命令。
- `R/provenance.json`：新旧 JAR SHA256、源码 SHA、模块测试计数。
- `R/build.log`、`R/build-exit.txt`、`R/build-summary.json`：实际构建证据。
- `R/canary-evidence.json`、`R/rollback-probe-evidence.json`、`R/deployed-evidence.json`、`R/final-evidence.json`：结构化验收；各阶段 JSON 保存 argv、PID/starttime、cwd，各阶段子目录保存私有日志。
- `R/runtime-manifest-before-api-deployment.json`：更新前 runtime manifest 完整备份。

原 cwd `/home/david/.local/share/bossanalyze/local-dev/final-runtime-e6df9cf38b` 保留。原 JAR 位于其 `boundaries-6e8609e38e/final-artifacts/api.jar`，未覆盖。原 JVM `-Xmx768m`、全部环境和其他参数保留。完整 additional-location 顺序为：

1. `file:/home/david/.local/share/bossanalyze/local-dev/application-local.properties`
2. `file:/home/david/.local/share/bossanalyze/rustfs-20261108/application-store.properties`
3. `file:/home/david/.local/share/bossanalyze/local-dev/application-mailpit-issue39.properties`（仅追加此现有 overlay）

三个原私有配置文件均保持原位且未修改。local-dev 保留，未禁用 mail health，未开放敏感健康详情。实际新 JAR 的 local-dev SQL init=never、ddl-auto=none 已检查。

## 运行验收

| 阶段 | PID / Linux starttime | aggregate | readiness | 其他 |
| --- | --- | --- | --- | --- |
| 切换前原 API，8080 | 1445378 / 4219281 | 503 / DOWN | 200 / UP | `/proc` exe、cwd、argv、环境、starttime 已核实并私有保存 |
| 新 JAR canary，127.0.0.1:18080 | 2123895 / 5748650 | 200 / UP | 200 / UP | 已优雅停止 |
| 原 JAR + 原配置回退启动验证，127.0.0.1:18080 | 2124819 / 5750771 | 503 / DOWN | 200 / UP | Started Springweb；原 SMTP 缺密码导致 AuthenticationFailedException；已优雅停止 |
| 最终新版 API，8080 | **2125922 / 5753450** | **200 / UP** | **200 / UP** | 保持运行，实际 cmdline 指向 R/api.jar |

canary 隔离 cwd、控制台/文件日志、JVM tmp，以及 `hadoop.hdfs.log.dir`；未占用原日志目录。第一轮 canary PID 2122517 的检查脚本将 WARN 文本里的单词 ERROR 误判为 ERROR 级日志，检查中止并清理该进程；随后改为匹配实际 ERROR 日志级别，重新启动并完整验收。未改变应用健康门槛。

新版 canary 和最终 API 均观察到 Started Springweb，无 APPLICATION FAILED TO START、ERROR 级日志或异常类名。未授权 GET `/api/auth/getUserBaseInfo` 均返回 401。匿名 `/actuator/health/mail` 返回 401；实际 aggregate 仅展示 status，因此 **mail UP 是从 aggregate UP 和启用的标准 mail contributor 推断，未直接取得 mail 组件 JSON**。没有开放详情或关闭检查。

使用本机专用 0600 `visual-audit-login.json` 的现有纯合成 QA 账号，凭据仅在内存使用。canary 与最终 API：POST login=200、GET getUserBaseInfo=200、POST logout=200；最终 API 登出后同一 token 再访问为 401。未保存或输出 token、账号内容或业务响应正文，未读取真实简历。

Mailpit 容器 `bossanalyze-mailpit-issue39` 保持运行；消息总数检查前后均为 1，没有新增邮件。未发送外部邮件，未调用 AI，未执行 schema、seed、Redis 清理或账号/权限修改。登录/登出会产生正常认证会话及日志副作用；未做业务数据库快照或逐表差异验证。

## 切换与回退

新版 canary 成功及原版隔离启动验证后，重新确认原 PID/starttime，只对 1445378/4219281 发送 SIGTERM 并等待退出。以原完整环境、原 cwd、原 argv（只替换独立新版 JAR 并追加 Mailpit overlay）启动 8080；切换逻辑在新进程启动或验收失败时会停止自己的新进程，并自动用原 argv/环境恢复原服务。此次切换成功，未触发自动恢复分支。

精确本机回退命令（会短暂停止 8080；仅对 current.json 的匹配 PID/starttime 优雅停止）：

```sh
python3 /home/david/.local/share/bossanalyze/local-dev/api-deployment-20261108/control.py rollback
```

此命令使用 `R/original-api.jar` 和保存的原完整 argv/环境/cwd，不加载 Mailpit overlay。原备份与已隔离启动验证的原 JAR SHA256 相同。**预期回到原 readiness200 / aggregate503 的 SMTP 降级状态，不是健康回退**。原 provider 的有效凭据未建立。

再次部署新版及 Mailpit overlay：

```sh
python3 /home/david/.local/share/bossanalyze/local-dev/api-deployment-20261108/control.py redeploy
```

两个命令将新 PID/starttime 保存到私有 `R/current.json`；运行后应复查 aggregate/readiness 和日志，并根据 current.json 备份后同步主 runtime manifest 的 api-final 条目。不要打印 original.json 或把凭据展开到命令行。回退命令本身未在 8080 实际执行；避免不必要的额外停机，回退启动已在隔离端口验证。自动恢复失败分支未故障注入。

主私有 manifest `/home/david/.local/share/bossanalyze/local-dev/final-runtime-e6df9cf38b/manifest.json` 更新前已完整备份；仅更新 `processes.api-final` 的运行信息并追加 `localApiDeployment20261108`，记录 sourceSHA、artifact、overlay、证据和回退命令位置。逐字段确认原顶层字段及其他服务条目均保留。

## 最终状态与边界

最终复查 8080 为 200/UP（aggregate/readiness），新版 PID/starttime 匹配，18080 无监听；两个 canary 均已清理，私有资料保留。Mailpit 保持运行。未重启 Nuxt、analytics、crawler、Python 或其他会话。

未验收：生产部署、外部 SMTP 投递、显式 mail 组件详情、完整 Jev 部署/AI质量、其他服务的新版本部署，以及真实简历/全部业务流程。本文仅代表本机 Java API 部署与上述回退证据；不代表整个系统剩余任务全部完成。
