# 依赖整改补充（2026-11-10）

基线 `7f3b6f4`（main `a529f56`），仅依赖所有权范围；不操作业务服务、数据、模型、浏览器或部署，不提交其他工作者内容。

## 发布版本与本地缓解

| 告警 | 当前处理 | 证据与边界 |
| --- | --- | --- |
| #350 credential、#351 analytics，org.json | 两个 POM 从 20231013 升至 20260719 | [GHSA-ghc9-756g-vrcv](https://github.com/advisories/GHSA-ghc9-756g-vrcv) 标注首个修复版 20260719；[Maven Central](https://repo.maven.apache.org/maven2/org/json/json/maven-metadata.xml) 与 [官方发布](https://github.com/stleary/JSON-java/releases/tag/20260719) 均存在该版。仅声明本地依赖达到官方修复门槛，未声明 GitHub 告警已关闭。 |
| #338 Zod | 保留 3.25.76 + 既有 pnpm patch | [官方公告](https://github.com/advisories/GHSA-mrhx-c7xc-m3w5) 首个修复版 4.6.6；[npm](https://registry.npmjs.org/zod/latest) 当前最新 4.6.7，v3 最新稳定版仍 3.25.76。直接采用官方 4.6.6 仍丢失实际 MCP schema 约束，因此不做不兼容 override、不改包名。仍是本地缓解，版本仍在告警范围。 |
| #339 deepmerge | 保留 4.3.1 + 既有 pnpm patch | [官方公告](https://github.com/advisories/GHSA-gcqx-w48m-w2h4) 无修复版；[npm](https://registry.npmjs.org/deepmerge/latest) 最新仍 4.3.1。保留 source/CJS/UMD 原型污染回补与现有回归，仍是本地缓解。 |

[shadcn-vue 最新 npm 清单](https://registry.npmjs.org/shadcn-vue/latest) 仍为 2.9.0，依赖 `zod:^3.25.76` 与 `zod-to-json-schema:^3.25.2`。未发现可直接替换且保留契约的官方升级路径；不扩展为 MCP 转换器重写。后续升级需上游消费者同时支持 v4，且通过实际 schema/registry 与安全回归。

## 范围、兼容与检查

修改仅两个 POM 和本文。父 POM、frontend 清单/锁文件/patches/tests、String 缓存实现与回归均保留。图项目 `boss-deps-closeout` 已索引并检查路径覆盖；tools/docs/lockfile/patches/tests 的索引排除部分以直接读源补足。Java 全源码检索没有直接 `org.json` 调用；不据此删除依赖。官方 20260719 POM 编译目标为 Java 8，可用于本次 Java 17。

日志和外部元数据保留于 `/tmp/boss-deps-20261110/`（本机临时证据，不纳入提交；父任务需在清理临时目录前归档）。

- Java：`JAVA_HOME=/home/david/.local/share/mise/installs/java/zulu-17`，Maven `/home/david/.local/share/mise/installs/maven/3.9.16/apache-maven-3.9.16/bin/mvn`。
- `mvn -B -f backend/pom.xml clean verify`：exit 0；commons 18、API 47、crawler 52、analytics 12 项报告；analytics 其中 1 项原有跳过，未计为通过。无失败/错误。`backend-verify.log` 保留全过程。
- `mvn -B -f tools/credential-contract-tests/pom.xml clean verify`：exit 0，56 项，无失败/错误/跳过；`credential-verify.log`。
- 有限安全反例 `JsonBounds.java`（临时脚本，非仓库新测试）：1001 位数字在旧版转换为 BigInteger，断言失败 exit 1；20260719 保留为 String，普通字段解析不变，exit 0。`json-original.log` 与 `json-patched.log` 均保留。
- credential `dependency:tree -Dincludes=org.json:json`：exit 0，解析到 20260719；`credential-json-tree.log`。analytics 实际打包 JAR 包含 `BOOT-INF/lib/json-20260719.jar`。
- 官方 Zod 4.6.6 反例：从 npm 原包解压到临时目录，执行实际 shadcn MCP list-tools 注册代码，仅注入官方 v4 替代原 Zod，沿用实际转换器。原回补版生成 `query: {type: 'string'}`，官方 v4 结果只有 `$schema`；断言失败 exit 1。脚本与完整失败日志：`zod-mcp-counterexample.mjs`、`zod-mcp-counterexample.log`。最初运行 Node 26.12.0；已在本 worktree 的安装依赖与 Node 22.23.1 下复验同样 exit 1，保留 `zod-mcp-counterexample-node22.log`。
- 前端文件无修改，但仍完成 Node 22.23.1 + pnpm 11.24.0 的 `install --frozen-lockfile`、`type-check`、全量 `test`（73 项全部通过）与 `build`，均 exit 0；日志 `frontend-node22-{install,type-check,test,build}.log`。原有 Zod CJS/ESM/v3/v4/mini 集合边界、deepmerge CJS/UMD 与实际 MCP schema/registry 回归全部保留执行。另保留此前 Node 26 的 73 项通过日志，不混作 Node 22 证据。
- analytics 跳过项为 `IpGeoServiceTest.officialDatabaseWhenExplicitlyProvided`（未提供可选数据库），不称该项通过。
- 工具准备失败也保留：最初 Node 下载 120 秒超时；一次断点续传后的归档仍不完整，不能作为 Node 22 证据，该轮 shell 后续冻结安装实际运行 Node 26.12.0。随后重新下载又遇连接中断（exit 56）；最终从官方 `https://nodejs.org/download/release/v22.23.1/node-v22.23.1-linux-x64.tar.xz` 完成归档并解压，显式确认版本后重跑上述完整检查。所有工具只放在临时目录，未改全局配置。

GitHub 公共 advisory API 只读请求遇到 403 rate limit，改读官方公告页面与注册表；仓库告警编号/manifest 来自父任务最新只读盘点，未擅自 dismiss、push、建 PR 或 watch。公开公告阈值、清单升级、本地回补与远端告警状态分别记录，不互相替代。
