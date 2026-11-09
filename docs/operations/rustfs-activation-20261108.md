# RustFS 本机业务存储启用 — 2026-11-08

状态：**用户确认无旧数据，因此无需复制，已启用新业务存储。** 这是用户对实际 MinIO 数据不存在的权威澄清，不是源服务返回空清单的实测，也不是旧对象迁移成功。先前 18988 不可达的失败记录保留在[迁移手册](rustfs-migration.md)及原交接中；该历史阻塞不再适用于本次新业务存储启用。仅本机，不涉及生产、AI、crawler 或 Python 迁移。

## 实际部署与权限

- 实时核验容器 `bossanalyze-store-20261108`，ID `2dcddefd57177633acbde283b3e5eb8aadfaf8c7121bc77ea373486b3060591e`；容器内 `rustfs --version` 为 1.0.4，镜像 digest `sha256:ed9210d6411d77ffceb70d94b964a3cb7d225983368bc25bebe01e995293dcc2` 与固定配置一致。
- 只发布 `127.0.0.1:19000/19001`，唯一数据挂载为专用命名卷 `bossanalyze-rustfs-20261108-data` → `/data`。单机单卷没有生产 HA 或备份恢复保证。
- 本轮 root 只用于盘点、初始化和验收。初始化前实际目标桶清单为空，随后创建 `default-bucket`；此观察只描述新 RustFS 目标，不证明旧 MinIO 源为空。没有清空或覆盖任何已有业务对象。
- 创建独立随机 IAM 用户，唯一绑定策略 `bossanalyze-images-20261108`。对桶 ARN 只允许 `s3:ListBucket`、`s3:GetBucketLocation`，对该桶 `/*` 只允许 `GetObject`、`PutObject`、`DeleteObject`、`AbortMultipartUpload`、`ListMultipartUploadParts`。`ListBucket` 用于现有 SDK 的 bucketExists/HEAD；桶由 root 预建，应用没有建桶权限。没有通配其他桶或 admin 权限。
- 实测另一个专属合成桶 get/put 均 403，应用创建桶 403、创建 IAM 策略 403。合成桶和对象随后删除；最终只有业务桶。应用身份真实 multipart 完成后字节一致并清理。

先核查[官方 IAM 文档](https://docs.rustfs.com/en/security-compliance/iam)及 **1.0.4 tag** 的 [user_lifecycle.rs](https://github.com/rustfs/rustfs/blob/1.0.4/rustfs/src/admin/handlers/user_lifecycle.rs)、[policies.rs](https://github.com/rustfs/rustfs/blob/1.0.4/rustfs/src/admin/handlers/policies.rs)、[utils.rs](https://github.com/rustfs/rustfs/blob/1.0.4/rustfs/src/admin/utils.rs)，再实际执行原生 `/rustfs/admin/v3/add-canned-policy`、`add-user`、`set-user-or-group-policy`，均 200；user-info 确认唯一策略。未盲套 mc/rc，也未新增管理服务。原生 RustFS 路径接收 JSON 与 MinIO 兼容路径的加密载荷不同；不要混用。凭据只由私有脚本读取，未放命令参数、仓库或报告。

## 应用切换与真实验收

保留原 `application-local.properties`、旧 jar、环境、进程清单和日志。新增私有目录下 `application-store.properties`（0600），内容为 `minio.endpoint/accessKey/secretKey/bucketName`；endpoint 为 19000，使用独立应用身份。该文件与用于 root 初始化的 `target.properties`、旧预备 `application-rustfs.properties` 分开，**不要将后二者作为 Java 应用配置加载**。

先用同一稳定 jar、原环境及追加隔离配置在 18080 启动 API，通过后才优雅替换 8080。停止前逐项匹配完整 cmd、cwd、PID starttime。旧 API PID740055/start1606706；新 API PID1445378/start4219281，cwd 保持 `/home/david/.local/share/bossanalyze/local-dev/final-runtime-e6df9cf38b`。唯一命令变化是在原 `--spring.config.additional-location` 后追加 `,file:/home/david/.local/share/bossanalyze/rustfs-20261108/application-store.properties`。未改变 jar、原配置或其他服务环境。

| 实际检查 | 结果 |
| --- | --- |
| 18080 隔离、5020→8080 最终链路 | 两个既有合成账号登录200；图片 upload/get200、中文空格名逐字节一致；跨用户delete403后仍可读；owner delete200 |
| SVG 与上传拒绝 | 现有契约允许 `image/svg+xml` 上传；下载为 `application/octet-stream`、attachment、nosniff、CSP sandbox/default-src none。`text/plain` 上传被拒绝。没有声称 SVG 上传被拒绝，也未改上传产品契约 |
| 新应用凭据预签名 | 从真实 API 返回19000/default-bucket URL；实际GET字节一致，篡改签名403、匿名GET403；未保存或打印签名URL |
| 新身份持久化 | 仅保留一个本轮唯一合成PNG标记，核实容器ID/固定镜像/专属卷后只重启该RustFS容器；最终5020 HTTP重读字节一致，owner清理200 |
| 对象保留 | 每组前后完整对象清单相等；最终业务桶0对象，无测试残留；未删除未知对象 |
| 最终业务与健康 | 5020四招聘接口200合法空数组，dashboard204空体；API readiness200、analytics health200；aggregate health503仍为既有SMTP问题，未关闭检测或发邮件 |
| 浏览器 | 真实5020登录表单进入工作台，用户信息200、dashboard204，正常注销200回/login，自有incognito标签关闭；未重复移动端验收 |

Nuxt PID742303/start1612426 与 analytics PID739675/start1606232 完整身份保持一致，crawler、数据库、Redis及其他项目容器未重启。隔离18080已优雅停止。最终服务保持运行；manifest 只更新 API 条目、追加历史及 `rustfsActivation`，保留其他服务条目。

## 私有证据、失败记录与限制

证据目录：`/home/david/.local/share/bossanalyze/local-dev/final-runtime-e6df9cf38b/rustfs-activation-20261108`（0700，文件0600）。包括 `inventory-before.json`、`app-policy.json`、`iam-results.json`、`18080-results.json`、`5020-results.json`、对象集before/after、`container-before-restart.json`、`rustfs-version.txt`、`ui-results.json`、`final-summary.json`、`before-activation/`、精确新旧进程及私有脚本。身份信息、环境、配置和日志不得整段公开。

5020首次脚本因代理响应头转为小写而触发 KeyError，保留 `5020-first-attempt-results.json`；修正脚本按HTTP大小写不敏感规则检查，最终全组通过，未修改服务代码。浏览器首次只派发input的自动填充尝试返回404，未归因为服务故障；改为input+change后表单登录成功，失败未抹去。官方源码下载一次TLS失败后重试成功，没有关闭TLS验证。

本次仓库仅文档变更，未重复全套构建或静态SDK probe；运行验证使用既有稳定 jar/SDK。临时 Python boto3 环境仅用于本机操作与验收，不是新增应用依赖或语言迁移。没有旧对象读取/迁移验收、生产/HA、远程CI、物理设备或灾难恢复证明。

## 回退条件

私有 `before-activation/` 保存旧 manifest、API完整命令、环境和原配置；旧 jar路径仍保留。恢复进程仍必须实时核对当前API的PID/cmd/cwd/starttime，优雅停止并使用保留的原命令与环境，更新manifest，不能重用已结束进程PID。18988当前不可用，回到旧命令不代表图片业务可用。

**业务有新对象后禁止直接切回18988或删除RustFS卷。** 应先冻结写入，导出目标当前对象及所用metadata/策略，核对新增、覆盖和删除，另行确认可用恢复目标、备份恢复与逐字节对账后再切换。保留当前卷、私有配置、独立IAM凭据和root恢复材料；用户“旧数据不存在”的澄清不授权未来丢弃新对象。不要执行 `down -v`、清桶或覆盖旧配置。
