# RustFS 本机部署与保留数据迁移

适用 issue #37 / 规范 `docs/specs/offline-infrastructure-20261108.md`。服务端替换保留应用的 MinIO Java SDK 8.5.7、`minio.*` 属性和图片 HTTP 接口；无需修改 MinioConfig。AI、数据库与现有应用进程不属于本操作范围。

## 固定版本与兼容边界

2026-11-08 核实官方 [1.0.4 release](https://github.com/rustfs/rustfs/releases/tag/1.0.4) 标记 Latest（10 月 25 日发布）。Compose 固定该 tag 及实际拉取的 digest `sha256:ed9210d6411d77ffceb70d94b964a3cb7d225983368bc25bebe01e995293dcc2`，不要改成 latest。单节点单盘仅供本地开发，无冗余。安装参数见[官方 Docker 原文](https://docs.rustfs.com/en/installation/container/docker)。

[官方 S3 矩阵](https://docs.rustfs.com/en/reference/s3-compatibility)只是特定测试子集：ACL 明确排除，bucket ownership controls/access logging 等尚待实现；版本、Object Lock、multipart 边界和 SSE 仅选定用例通过。加密对象的底层格式不跨产品兼容。禁止把 MinIO 目录挂给 RustFS，禁止把官方兼容声明当作本项目验收。

## 先盘点源，只读

1. 从实际进程的私有配置定位 endpoint/bucket；输出只允许 host/port/bucket。不要打印进程完整环境、Docker inspect 完整结果、凭据或预签名 URL。
2. 核实本机容器（包括停止的）、bind mounts、命名卷、原始目录和备份所在机器。端口未监听、容器不存在均不能证明数据为空。
3. 用当前机器上的原有凭据建立私有 `source.properties`（0600），字段 `endpoint`、`accessKey`、`secretKey`、`bucket`。不要将凭据复制到其他机器或提交。
4. `run-check.sh audit /absolute/private/source.properties` 必须能读取源 bucket；任一权限/网络错误均阻止迁移。未找到真实源/备份时保留阻塞，不能创建空源替代。

## 新卷部署

先确认 19000/19001 未占用，并确认 `bossanalyze-rustfs-20261108-data` 不存在。若已存在，先查其归属与数据，不清空、不重建。生成本机专用随机凭据，目录 0700、文件 0600；禁止使用默认管理员凭据。私有 env 文件只含 `RUSTFS_ACCESS_KEY` 与 `RUSTFS_SECRET_KEY`。Docker 管理员可以读取容器环境，应限制 Docker 权限。

```sh
export RUSTFS_PRIVATE_ENV=/absolute/private/rustfs.env
docker compose -f deploy/rustfs/compose.yaml up -d
```

Compose 只发布 loopback 19000（S3）/19001（console），专用新卷 `/data`；不挂载源卷，不自动重启现有应用。不要执行 `down -v`。不要用 `docker compose config` 输出已展开的凭据。

## SDK 验证与复制工具

工具拥有独立 POM，仅复用当前应用同版本 SDK；未改后端 parent/POM。使用 Java 17、Maven 3.9.6+，将两者真实 bin 目录加到 PATH，不更改系统默认版本。目标私有 properties 与源字段相同，bucket 是计划迁移的目标 bucket。

```sh
export JAVA_HOME=/absolute/java17
export PATH="$JAVA_HOME/bin:/absolute/maven/bin:$PATH"
deploy/rustfs/run-check.sh probe /absolute/private/target.properties
deploy/rustfs/run-check.sh self-test /absolute/private/target.properties
deploy/rustfs/run-check.sh audit /absolute/private/source.properties
```

`probe` 在全新随机 bucket 内真实验证 bucket/put/get/delete、中文空格键、content type、metadata/tags、7 天 GET 预签名、篡改签名 403、匿名 GET 403、11 MiB / 5 MiB 分片 multipart 字节摘要，并删除专属测试对象/bucket。不会写应用 bucket。`self-test` 在两个随机 bucket 验证复制对账，检测非空目标、对象被修改、缺失键及版本开启的源。失败可能留下测试 bucket，先调查再清理，仅限确认属于该次测试的数据。

跨用户拒绝属于应用身份边界，已有 `ImageIntegrityTest`（mock）必须保留；匿名 S3 拒绝不能替代用户 1/用户 2 的应用验收。最终集成应使用实际应用临时测试用户进行上传、旧图片 HTTP 读取及跨用户删除拒绝，禁止修改现有业务记录。

## 切换前逐项审批表

| 特性 | 工具自动检查 | 切换条件 |
| --- | --- | --- |
| 当前对象 | 完整递归分页、名称集合、大小、流式 SHA-256 | 源/目标集合与字节一致，禁止用 multipart ETag 充当内容摘要 |
| metadata / tags | 用户 metadata、content type、Cache-Control/Disposition/Encoding/Language/Expires、tags | 全部保留且逐对象相等；其他供应商 metadata/storage class 由操作者盘点 |
| 版本 / 删除标记 | versioning 状态和含版本列表 | 仅从未启用版本的无历史源自动复制；存在历史即阻止，另做逐版本保留方案 |
| locks / retention / legal hold | bucket lock 与对象 headers | 存在即阻止；不能只复制当前字节抹掉合规属性 |
| SSE | bucket encryption 与对象 headers | 存在即阻止；另验 S3 解密读取、目标重新加密及密钥恢复，禁止复制底层盘格式 |
| IAM / ACL / bucket policy | 非空 bucket policy 阻止 | IAM、ACL、公共访问、应用服务身份权限需独立人工清单与目标实测；ACL 不受支持时禁止切换 |
| lifecycle / replication / notifications / CORS / incomplete multipart | 人工独立清单 | 不会自动迁移；存在未实现/未验证需求即阻止切换 |
| 备份 | 要求本机证据文件存在 | 操作者确认完整备份已在隔离环境恢复、版本/权限与源一致；文件存在本身不是备份证明 |

复制只支持表中简单私有、未版本化/未锁定/未加密 bucket。任何 API 不支持、AccessDenied 或检查失败都会终止，不把未知当“无”。目标 bucket 由操作者确认新建（工具不会自动创建生产目标）。`copy` 要求目标为空，从不覆盖或删除已有对象；中断后留下的目标要先对账和人工处置，不能通过清空重跑掩盖失败。

冻结所有写入者（本轮仅父任务可统筹），确认备份恢复后建立私有 review.properties：

```properties
backupRestoredAndVerified=true
writersFrozen=true
iamAclLifecycleReplicationReviewed=true
specialFeaturesAbsent=true
backupEvidenceFile=/absolute/private/backup-restore-report.md
```

```sh
deploy/rustfs/run-check.sh copy /absolute/private/source.properties /absolute/private/target.properties /absolute/private/review.properties
deploy/rustfs/run-check.sh verify /absolute/private/source.properties /absolute/private/target.properties
```

工具不执行切换；review 是操作者声明，不能自动证明冻结/备份/IAM。保持冻结直到最终对账与切换完成。源发生新增/删除会阻止完成，工具不自动传播删除；同名写入依赖冻结与逐字节校验。需要增量时先另行核验差异，明确批准新增/覆盖/删除集合，不使用 `sync --delete`。写入未冻结时工具成功也不是一致快照证明。

父任务在统一验收后才修改应用私有 `minio.endpoint/accessKey/secretKey` 并重启 API；保留 bucketName、same-origin 图片路径。检查旧图片可读、新上传、预签名与跨用户拒绝。源目录、卷、备份至少保留 7 天且直到验收与备份政策满足，不由工具删除。

回退时先冻结目标写入，核对切换后新增/覆盖/删除与版本历史，反向 S3 复制和逐字节对账再切回；不能只改旧 endpoint，否则丢失新数据。任何未支持特性、差异或备份不完整继续阻止切换。

## 2026-11-08 本机验收界限

本轮源默认 `http://127.0.0.1:18988` / `default-bucket` 连接拒绝；当前应用私有配置未覆盖 MinIO，Docker 无 MinIO 容器，未发现带 MinIO 名的卷。这不是源为空的证据，源历史目录、备份、版本及对象数量仍未知。未迁移真实对象、未切换 endpoint、未重启应用。真实新目标合成验证和 mock 授权回归分别记在 `/tmp/boss-infra-store-handoff.md`，不要将其等同于真实数据迁移完成。
