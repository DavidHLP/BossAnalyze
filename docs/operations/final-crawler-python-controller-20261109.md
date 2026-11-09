# 最终 crawler/Python 控制器交付（issue #51）

此文说明实现接口，不宣告部署验收。冻结计划见 [部署计划](../specs/final-crawler-python-deployment-20261109.md)。目标固定为 `b97ab94da701630b78bae7553622919fc7efd8bd`；只有 crawler/Python，旧版仅 existingRoot/latest。禁止构建、真实推理、采集、Store、业务库变更和其他组件操作。

## 私有产物及归属

本批 N 为 `/home/david/.local/share/bossanalyze/stack-deployment-20261108/releases/b97ab94-20261109-_2172r6g`。issue #51 只拥有 `N/control.py`、`N/controller/` 和本文；不拥有 JAR、ai-service、源码导出或构建 provenance。基于 existingRoot/control.py 的 PID/starttime、私有 umask、loopback、健康等待及有界 SIGTERM 逻辑改为两组件配对；未修改现行 controller、env、latest、rollback 或状态文件。

`N/controller/config.json` 固定 root/release/target/主 manifest 绝对路径。控制文件与运行记录 0600、目录 0700。不能打印 session/state、完整进程参数、环境或日志；inventory/status 只输出布尔、安全数值、PID/starttime 和阶段。真实配置仅由 root 执行 inventory 时从目标进程读取，worker 离线检查只使用合成数据。

## root 命令顺序

以下 `N` 指上述绝对路径。先独立审查私有实现。命令不隐含构建完成或验收通过。

1. `python "$N/control.py" inventory`：只读现行进程及状态，校验 latest 产物、PID/starttime、argv/exe/cwd、端口属主、token/model 配对、AI_MAX_* 与 deadline/cap 一致、ZDR/quality 和 crawler 关闭开关；预算路径必须绝对。输出比较布尔与数值。在 N 私有 session 中保存原参数/环境、current/manifest 原文及其他服务监听身份；保存旧 controller 源快照。既有 session 拒绝覆盖，重新调查必须另行保存证据后由 root 处理，不能直接重跑覆盖。
2. root 核对 B 的本轮构建、固定来源及独立审查后，以程序化 0600 原子写 `N/controller/build-ready.json`，内容仅 `{"target":"b97ab94da701630b78bae7553622919fc7efd8bd","verified":true}`。这是人工审核门槛，不代替构建证明；控制器不会写它，也不会尝试构建或启动未批准产物。
3. `python "$N/control.py" canary-start`：持有新 Python18090→crawler18081，crawler 明确连18090。启动后保留两个实例供 root 检查；异常清理 crawler→Python。`--version old-latest` 用保留 latest 在同一临时端口做旧版冷启动检查，必须先停另一整对。
4. `python "$N/controller/probe.py" compile`：仅从已批准的新 JAR 提取 BOOT-INF 类/依赖，在 controller/java-probe 内用 Java17 javac 编译 EmptyContractProbe。没有 Spring/mapper/Store。`python "$N/controller/probe.py" run --port 18090`：检查 Python 匿名401、带认证非法结构422、crawler及5020同源匿名401，调用真实 Java client 的空 analysis、空 jobs、单 job 空 detail。输出只有 exit 是否为零；详细输出在0600 probe日志。Java断言包括 needs_review、空数组、envelope版本/model、usage对象内两项null，以及 insufficient_evidence/null score/四维unknown。此处 usage=null 指无token计数，不是把API usage对象改为null。
5. root 另行只读核对共享预算账本前后、无 provider/写回、安全开关及其他服务身份。`python "$N/control.py" canary-stop` 停临时 crawler→Python；确认临时端口空闲、state.canary为空，再启动旧版canary冷启动验证并停干净。旧版不运行新版 Java契约探针。
6. `python "$N/control.py" switch`：重新核验进程、保护服务和元数据基线，停旧crawler→停旧Python→新Python8090健康→新crawler8081健康。不并行、不滚动混用。成功仅进入 `new-awaiting-acceptance`，尚不发布success。启动失败、超时或中断均清本轮整对并恢复旧Python→旧crawler；恢复失败保留实况停止，不继续替换。
7. root 用 `probe.py run --port 8090` 及预算/身份检查验收。任一失败执行 `python "$N/control.py" rollback`，整对停当前crawler→Python，恢复旧latest Python→crawler。此命令也可在已accept的新版本上回退。不是 existingRoot/rollback 中的更老版本。回退后发布真实重启身份，success=false、阶段 restored-awaiting-acceptance；root另做旧版验收后方可accept。
8. `python "$N/control.py" accept` 是 root 对已完成外部契约、预算、门槛检查的明确确认，不能只因health200执行。控制器再查健康、配对、真实身份及保护服务，才发布 current 和 manifest。`python "$N/control.py" status` 随时输出实况存活布尔，不读取/输出secret。accept 本身配对或健康失败会回退新版本；CAS冲突仅报告元数据未对齐，保留实际日志，不能强写。

## 状态、失败和并发

`journal.jsonl` 立即持久化阶段与真实PID/starttime、port、存活布尔；`state.json` 持久化待检/运行实例，包括私有配置，不能作为成功凭据。pending先登记再健康等待。启动登记期间延迟终端信号；恢复期间忽略后续SIGINT/SIGTERM，仍受每次健康等待和SIGTERM期限限制。停止先验证 name/port/PID/starttime/argv/exe/cwd，再用 pidfd 绑定身份发送SIGTERM，最多60秒，不SIGKILL；PID复用拒绝操作。

current 只替换 python/crawler；主manifest只替换 processes.python-final / crawler-final 和独立 finalCrawlerPython20261109 版本记录。其他字段保留。使用固定 controller flock 串行命令；发布时取得 current.json.pair.lock 与 manifest.json.pair.lock，逐次比较inventory/上次发布的完整原文再原子替换。两文件非事务：第一份成功而第二份冲突时保留第一份真实状态与日志，明确未对齐，不恢复旧PID快照。

严格CAS依赖所有状态写入者遵守上述advisory锁或root确保发布时独占；不能防止不遵守锁的外部写入者在最后比较与rename之间竞态。root执行前须确保现行旧controller及其他manifest写入者不并发运行，不能把离线竞态检查当跨进程事务证明。冲突不得通过重建session覆盖解除。

Python使用专属run cwd与显式PYTHONPATH指向版本ai-service；crawler专属run cwd、JVM tmp及logging.file.path，并强制controller/logback.xml仅输出控制台至私有console.log，避免沿用共享/HDFS日志写入器；相对路径留在专属cwd，不覆盖共享日志。目标crawler源码未配置HDFS日志初始化器。原有JVM外部agent或其他非日志框架配置仍须root审查，控制器不解析任意外部配置文件。

## 本轮验证与限制

实际运行 `PYTHONDONTWRITEBYTECODE=1 python N/controller/offline_checks.py`：11项通过，失败0、错误0、跳过0；覆盖PID复用拒绝发信号、非法组件/端口、有界SIGTERM不强杀、pending先于health、失败清理、Python先恢复、切换中断恢复、manifest初始冲突及第一文件更新后冲突不覆盖、保留无关字段与真实重启身份、合成配置配对反例。control.py、offline_checks.py、probe.py内存语法编译通过。

未执行 inventory、构建gate、Java编译/探针、真实canary/冷启动、切换、回退、accept或业务库操作。离线检查多数替换进程/端口/时钟，不证明真实启动和HTTP可用；root必须独立审核和真实验收。外部配置覆盖和不协作元数据写入仍是明确风险。控制器不自动检测provider或预算变化，root负责外部验收；accept不是自动质量批准，AI质量和写回始终关闭。
