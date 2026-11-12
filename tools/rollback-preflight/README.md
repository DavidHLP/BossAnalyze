# 只读整体回退预检

关联 [#68](https://github.com/DavidHLP/BossAnalyze/issues/68)，实现[规范 B](../../docs/specs/ai-closeout-20261112.md)。Python 3.11+ 标准库工具，Linux 现场还需 `git`、`ss`、Docker inspect 权限及现有本机健康端点。测试不需要 Docker 或业务服务。

```sh
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s tools/rollback-preflight -v
```

本工具没有 stop/start、信号、发布、写文件、写 SQLite、刷新 CAS 或锁文件创建入口；不导入或执行私有 `release.py` / `control.py`。只读取文件、`/proc`、`ss`、`docker inspect` 和固定 loopback GET。不会发送模型、匹配、采集或邮件请求。文件读取可能更新文件系统 atime，健康 GET 可能留下服务访问日志；“零变更”指不改变进程、业务数据、预算、元数据或控制状态。

## 直接接入现有现场

直接使用现有 `release-state-private.json`，不需要把它转换或复制为新 manifest。五个元数据路径按私有 `release.py` 的 `META` 映射**静态阅读**后显式传入，不执行脚本求值。选择同一份历史发布的 state 与路径，不选择另一个目录的相似文件。不输出或提交 state、env、argv、元数据原文、回执或预算内容。

```sh
python3 -B tools/rollback-preflight/preflight.py \
  --state "$RELEASE_STATE_PATH" \
  --repository "$REPOSITORY_PATH" \
  --metadata "current=$CURRENT_PATH" \
  --metadata "manifest=$MANIFEST_PATH" \
  --metadata "session=$SESSION_PATH" \
  --metadata "pair-state=$PAIR_STATE_PATH" \
  --metadata "config=$CONFIG_PATH"
```

所有路径必须为绝对路径；五个元数据文件必须不同。支持原 state 的 `phase`、`pending`、`expectedMetadata`、`active`、`old`、`mongodb`、`infrastructure`、`budgetPath`、`budgetDigest`，无需删除其他历史字段。仅支持 `switching`、`awaiting-acceptance`、`accepted`、`restoring`；未完成启动或缺少任何一个健康、完整监听的组件均保守拒绝，不能用于放宽中断恢复条件。

即使元数据与进程都一致，上述命令仍因缺少安全目标证明而拒绝。真正满足前置条件的目标还必须提供：

```sh
  --target-receipt "$REVIEWED_TARGET_RECEIPT_PATH" \
  --target-receipt-sha256 "$INDEPENDENTLY_REVIEWED_RECEIPT_SHA256"
```

这两项追加到前一条命令末尾。不要在每次运行时对一份新生成的回执自行求摘要并称其已获审阅；这会破坏信任边界。工具不生成目标回执，也不重建任何 expectedMetadata。

## 检查与结果

- 五份元数据与 `expectedMetadata` **UTF-8 原文字节**一致，检查末尾再次比较；不只比较解析后的 JSON。
- 五个组件的 `pid/starttime/exe/argv/cwd` 与 `/proc` 一致，其固定业务端口全部 listener owner 恰好为该 PID。进程及端口在健康检查后再次核对；不读取 `/proc/*/environ`。
- 四个容器的 id、PID、running、startedAt、ports 与原记录一致；Mongo 的完整身份与 27017 owner 一致；预算按原控制器的 `SHA256("\n".join(iterdump()))` 比较。
- 预算只读稳定文件字节并在内存 SQLite 中 deserialize，不打开真实账本连接。非空 WAL、文件变化或无法安全解析均拒绝，**不 checkpoint、不移走 WAL、不用 immutable 模式忽略 WAL**。WAL-free 是这个适配器的保守限制。
- 固定健康 GET 必须成功；Python readiness 还须明确匹配关闭、质量未批准、自动写回不允许；Java 健康及 API readiness 须 `UP`。Spark 4040 owner 须是 analytics，应用列表须可确认且无 RUNNING jobs。HTTP 禁止环境代理及重定向。
- 校验安全目标、回执 pin、启动参数和产物输入，并在结束前复查目标证据、state、元数据及受保护状态。

stdout 为去敏 JSON，含 `status`、`checked`、`reasons[{check,reason}]`、`rollbackExecuted:false`。退出码 0 表示 `passed`，1 表示 `refused`，2 表示 CLI 参数用法错误；拒绝不是“未执行但可用”。缺权限、工具或证据同样拒绝，不猜测通过。异常不输出私有路径、命令 stdout/stderr、env 或 traceback。

## 安全目标回执的信任边界

`sourceSHA` 标签或任意 JSON 相等都不是安全证明。安全目标同时需要以下条件：

1. 本地 Git 对象存在，来源是冻结安全基线 `0dcb7b6fb0532e4c652a5d3050b5524d25bc7052` 的后代，且 `backend/`、`ai-service/`、`frontend/`、`deploy/rustfs/` 构建输入与该基线一致。这保留已修依赖、Java/Python 匹配暂停、默认门禁；不能仅凭 ancestry 接受后来撤回安全修复的版本。这里刻意保守，构建输入有改变的版本需要新的代码/安全审查和工具基线更新。
2. 独立审阅者核对可信 clean 构建来源、包内依赖/backport、Java/Python 暂停实现、运行时与全部配置输入，保存一份**私有**目标回执，并通过独立可信渠道固定回执文件 SHA-256。工具不能从 hash 推导“已审阅”，也不能自行证明构建产物来自声明的源码；此来源绑定由受信回执承担。不能用未审阅、自签声明替代该前置条件。
3. 回执与 `old` 每个组件的源码、完整 argv、cwd、env 摘要一致；实际文件或目录内容与回执输入摘要一致。输入必须覆盖 artifact、exe、cwd、argv[0]，审阅还须枚举所有额外外部配置、类路径、库、解释器和运行时。未覆盖的额外动态依赖不能据此声称安全。
4. 目标 `AI_*`、`JEV_*`、`CMD_*` 环境值与 active 记录保持一致，拒绝启用质量批准或显式 Spring 环境调度/写回开关。外部配置及其他 JVM/Spring 覆盖方式必须在回执审阅中核对，工具不会启动目标来推测其有效配置。

回执结构如下（字段说明，不是已批准样例）：

```text
schemaVersion: 1
safetyBaseline: 固定上述基线 SHA
targets: 恰好 python/crawler/api/analytics/frontend 五项
  <component>:
    sourceSHA: 与 old 记录相同的完整 Git SHA
    argv: 完整字符串数组（仅私有保存，可能含敏感值）
    cwd: 绝对路径
    envSha256: SHA256(canonical(old.env))
    inputs:
      <绝对文件或目录路径>: 文件或目录摘要
```

`canonical` 为 Python `json.dumps(value, sort_keys=True, separators=(",", ":")).encode()`，保留默认 ASCII 转义。文件摘要是原始字节 SHA-256。目录摘要为 `SHA256(canonical(rows))`：按相对路径排序，文件行 `["relative/path", "文件SHA256"]`，目录行 `["relative/dir/", null]`，包括空目录。特殊文件、缺失输入和符号链接均拒绝；有 symlink 的 venv/运行目录需先由新的授权构建流程提供完整物化并审阅的产物，工具不修改它们。摘要 pin 与产物摘要是完整性校验，不是发布或质量批准。

## 当前现场结论与后续边界

2026-11-12 在本任务冻结基线的工作区，按私有控制器静态路径映射对真实 state 执行了预检，退出 **1 / refused**。观察到元数据 CAS 漂移、容器记录变化、五个历史 active 身份均不可确认；`old` 目标早于安全/匹配暂停基线。pending 为空、phase 可识别不改变拒绝结论。受保护检查在容器差异处拒绝，**没有据此声称 Mongo 或预算摘要已通过**。

检查前后 state、五份元数据、预算及已存在的 SQLite sidecar、控制器锁文件的内容/mtime/身份保持一致，未创建缺失的这些文件；未执行私有脚本、重启或回退。此记录只证明该时点拒绝和所列文件未变，不是当前在线健康声明，也不是实际回退验收。

原 `release.py rollback` 不包含这里新增的安全目标基线判定；禁止因为脚本存在、历史 accepted 或合成测试通过就称它现在可操作。只有另行授权的受审流程完成安全目标构建、全部配置/启动参数核对、当前身份及预算调查、CAS 差异逐项解释、保留原状态并建立新的受审恢复基线后，才可能重新评估。工具永远不把当前现场值自动写进旧 expectedMetadata。

预检是有界时间内的只读观察，不持有发布互斥锁、不提供跨文件/进程原子快照；通过也不授权执行回退。真正执行器仍必须保留并重新执行身份/CAS/容器/预算/目标完整性和安全门禁，处理竞态，另外做授权的恢复演练。这里不执行该后续阶段。
