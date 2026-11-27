# 简历工作区写入并发控制

## MongoDB 拓扑调查

- 对本机 `127.0.0.1:27017` 执行只读 `hello`，返回 `isWritablePrimary` 和 `logicalSessionTimeoutMinutes`，没有 `setName` 或 `msg: isdbgrid`。本机使用 MongoDB 7.0.45 standalone，支持会话但不支持多文档事务。
- 仓库的应用配置只给出单主机地址或可覆盖的 URI。生产部署的外部配置和 `hello` 结果未获确认，发布前应由运维在应用实际连接上复核。复制集或 `mongos` 才有条件使用多文档事务。
- 本次采用 standalone 可用的方案：Redis 简历级锁和 MongoDB `workspaceRevision` 条件更新。锁串行化正常写入，MongoDB 条件更新防止锁失效时覆盖新状态。旧文档缺少 revision 时按 0 处理。

## 请求约定

`GET /api/resume/{id}` 返回 `workspaceRevision` 和 `headCommit`。工作区写请求可在 JSON 中带上同一个 `workspaceRevision`。取回储藏和重复 ID 检查接受可选 JSON 请求体。删除请求通过查询参数传递 revision。省略字段时，服务端在锁内读取当前 revision 并用它做条件写入。已有的成功响应格式保持兼容。

基线不匹配返回 HTTP 409 和 `Result` 数据 `{"workspaceRevision": 12, "headCommit": "commit-…"}`。请按返回的状态重新读取简历后解决冲突。锁不可用返回 503。带 revision 的请求在简历已删除或不属于当前账号时返回 404。

## 发布与故障恢复

1. 新快照先以 `pending_commit` 类型写入。快照与简历正文、HEAD、分支及储藏指针的关联，只有在匹配账号、简历 ID 和旧 revision 的单文档更新成功时才生效。创建简历时，首次写入元数据文档完成同样的发布。
2. 历史、差异、最近提交、调试及按 ID 访问版本时，均要求存在归当前账号所有的简历元数据。读取只接受 `commit` 或该元数据的发布指针指向的 `pending_commit`。下一次写入替换发布指针前，会先将上一条已发布快照转为 `commit`。重置后仍可查看已发布的历史。
3. 条件更新落败、锁中断或进程在发布前退出时，暂存快照不可见。删除先条件删除元数据，再清理快照。即使清理中断，读取也找不到已删除简历的快照。若网络中断使发布结果未知，以重新读取的 revision 和 HEAD 为准。

部署时先停止旧版简历写入，再启用新版实例。旧版写入不检查 revision，且无法读取尚待转为 `commit` 的已发布快照。升级前已有的 `commit` 文档按历史格式继续读取。无法仅凭当前指针区分旧版的失败快照和重置前的正常历史。

故障后可能保留 `pending_commit` 文档。排查时以对应元数据的 `published_commit_id` 判断是否已发布。若需清理未发布文档，先暂停全部简历写入并确认无在途请求，以免删掉准备发布的快照。standalone 的 `_id` 唯一约束下，重复 ID 检查只推进 revision。若检测到异常的重复 ID，接口报告错误，保留原有历史供排查。

## 复核

生产实例的发布验收需要复核应用连接的 `hello` 结果，并使用独立数据库和测试账号复跑并发用例。开发环境可用以下命令连接本机 standalone MongoDB 和独立 Redis。测试创建并删除随机命名的 `resume_workspace_test_*` 数据库。

```sh
mvn -f backend/pom.xml -pl api -am \
  -Dtest=ResumeWorkspaceConcurrencyTest -Dsurefire.failIfNoSpecifiedTests=false \
  -Dbossanalyze.resume.integration=true \
  -Dbossanalyze.resume.test.redis=redis://127.0.0.1:16379 test
```
