# 依赖安全修复与本机验收（2026-11-09）

关联 [#58](https://github.com/DavidHLP/BossAnalyze/issues/58)、[#54](https://github.com/DavidHLP/BossAnalyze/issues/54)、[PR #59](https://github.com/DavidHLP/BossAnalyze/pull/59)。业务修订固定为 `6aa3b079ffa712a3b4354f21dbbea4086d7bac23`；原始基线 `059303f0186cade96feb697bc9f1ad410fb20bae`。这是冻结快照的修复记录，不是所有依赖无漏洞的保证。

## 冻结范围与结论

冻结 [原始51项快照](dependency-alerts-20261109.json)，4 critical、21 high、24 medium、2 low。其中49项采用达到各自官方补丁门槛的发布版本；deepmerge #339、Zod #338采用本地 pnpm backport，包版本仍落在官方告警范围，不宣称这两项官方告警关闭。未删除或忽略告警来替代修复。

修复前已部署 API 含 Hutool5.8.16、Redisson3.17.6、Jackson core/databind2.18.2；因此不能把原快照解释为过期的声明。下表是源码、解析依赖/锁文件和新产物证据，尚不能单独证明现行进程已更换。

| Alert | GHSA | Manifest | 包 | 当前修复版本/方式 | 结论 |
|---|---|---|---|---|---|
| #349 | [GHSA-82w8-qh3p-5jfq](https://github.com/advisories/GHSA-82w8-qh3p-5jfq) | `ai-service/uv.lock` | `starlette` | 1.3.1 | 发布版本脱离本项受影响范围 |
| #348 | [GHSA-jp82-jpqv-5vv3](https://github.com/advisories/GHSA-jp82-jpqv-5vv3) | `ai-service/uv.lock` | `Starlette` | 1.3.1 | 发布版本脱离本项受影响范围 |
| #347 | [GHSA-wqp7-x3pw-xc5r](https://github.com/advisories/GHSA-wqp7-x3pw-xc5r) | `ai-service/uv.lock` | `starlette` | 1.3.1 | 发布版本脱离本项受影响范围 |
| #346 | [GHSA-x746-7m8f-x49c](https://github.com/advisories/GHSA-x746-7m8f-x49c) | `ai-service/uv.lock` | `starlette` | 1.3.1 | 发布版本脱离本项受影响范围 |
| #345 | [GHSA-86qp-5c8j-p5mr](https://github.com/advisories/GHSA-86qp-5c8j-p5mr) | `ai-service/uv.lock` | `starlette` | 1.3.1 | 发布版本脱离本项受影响范围 |
| #344 | [GHSA-6w46-j5rx-g56g](https://github.com/advisories/GHSA-6w46-j5rx-g56g) | `ai-service/uv.lock` | `pytest` | 9.0.3 | 发布版本脱离本项受影响范围 |
| #343 | [GHSA-h7rh-xfpj-hpcm](https://github.com/advisories/GHSA-h7rh-xfpj-hpcm) | `deploy/rustfs/pom.xml` | `io.minio:minio` | 8.6.0 | 发布版本脱离本项受影响范围 |
| #342 | [GHSA-g3hv-75wg-m249](https://github.com/advisories/GHSA-g3hv-75wg-m249) | `frontend/pnpm-lock.yaml` | `minimatch` | 11.0.1（仅受影响现代范围；旧3/5不在本告警范围） | 发布版本脱离本项受影响范围 |
| #341 | [GHSA-3g3m-2fjr-6pw4](https://github.com/advisories/GHSA-3g3m-2fjr-6pw4) | `frontend/pnpm-lock.yaml` | `glob` | 14.0.2（旧7回调消费者不在本告警范围） | 发布版本脱离本项受影响范围 |
| #340 | [GHSA-qr28-fpjm-wq7f](https://github.com/advisories/GHSA-qr28-fpjm-wq7f) | `frontend/pnpm-lock.yaml` | `decode-uri-component` | 0.5.1 | 发布版本脱离本项受影响范围 |
| #339 | [GHSA-gcqx-w48m-w2h4](https://github.com/advisories/GHSA-gcqx-w48m-w2h4) | `frontend/pnpm-lock.yaml` | `deepmerge` | 4.3.1 + pnpm patch | 本地回补；不声称官方关闭 |
| #338 | [GHSA-mrhx-c7xc-m3w5](https://github.com/advisories/GHSA-mrhx-c7xc-m3w5) | `frontend/pnpm-lock.yaml` | `zod` | 3.25.76 + pnpm patch（含包内v4/mini） | 本地回补；不声称官方关闭 |
| #337 | [GHSA-vcc3-ghjq-m6fr](https://github.com/advisories/GHSA-vcc3-ghjq-m6fr) | `frontend/pnpm-lock.yaml` | `decode-uri-component` | 0.5.1 | 发布版本脱离本项受影响范围 |
| #336 | [GHSA-5gvw-p9qm-jgwh](https://github.com/advisories/GHSA-5gvw-p9qm-jgwh) | `backend/pom.xml` | `com.fasterxml.jackson.core:jackson-databind` | 2.18.11 BOM | 发布版本脱离本项受影响范围 |
| #335 | [GHSA-65r4-943x-97jj](https://github.com/advisories/GHSA-65r4-943x-97jj) | `backend/pom.xml` | `org.jsoup:jsoup` | 1.23.2 | 发布版本脱离本项受影响范围 |
| #334 | [GHSA-7hhh-6rmp-j9qf](https://github.com/advisories/GHSA-7hhh-6rmp-j9qf) | `backend/pom.xml` | `com.fasterxml.jackson.core:jackson-core` | 2.18.11 BOM | 发布版本脱离本项受影响范围 |
| #333 | [GHSA-p6pp-m3f8-5c89](https://github.com/advisories/GHSA-p6pp-m3f8-5c89) | `backend/pom.xml` | `com.fasterxml.jackson.core:jackson-core` | 2.18.11 BOM | 发布版本脱离本项受影响范围 |
| #332 | [GHSA-cxp5-3px4-pw24](https://github.com/advisories/GHSA-cxp5-3px4-pw24) | `backend/pom.xml` | `com.fasterxml.jackson.core:jackson-databind` | 2.18.11 BOM | 发布版本脱离本项受影响范围 |
| #331 | [GHSA-wv8q-qhhj-9h54](https://github.com/advisories/GHSA-wv8q-qhhj-9h54) | `backend/pom.xml` | `com.fasterxml.jackson.core:jackson-databind` | 2.18.11 BOM | 发布版本脱离本项受影响范围 |
| #330 | [GHSA-gx83-3vf8-gh7j](https://github.com/advisories/GHSA-gx83-3vf8-gh7j) | `backend/pom.xml` | `com.fasterxml.jackson.core:jackson-databind` | 2.18.11 BOM | 发布版本脱离本项受影响范围 |
| #329 | [GHSA-q4xh-88c3-wmh7](https://github.com/advisories/GHSA-q4xh-88c3-wmh7) | `backend/pom.xml` | `com.fasterxml.jackson.core:jackson-databind` | 2.18.11 BOM | 发布版本脱离本项受影响范围 |
| #328 | [GHSA-wjgm-6hv5-3cvf](https://github.com/advisories/GHSA-wjgm-6hv5-3cvf) | `backend/pom.xml` | `com.fasterxml.jackson.core:jackson-databind` | 2.18.11 BOM | 发布版本脱离本项受影响范围 |
| #327 | [GHSA-vvgp-rfg2-7rr6](https://github.com/advisories/GHSA-vvgp-rfg2-7rr6) | `backend/pom.xml` | `com.fasterxml.jackson.core:jackson-databind` | 2.18.11 BOM | 发布版本脱离本项受影响范围 |
| #326 | [GHSA-pmhh-3w7g-xqp8](https://github.com/advisories/GHSA-pmhh-3w7g-xqp8) | `backend/pom.xml` | `org.jsoup:jsoup` | 1.23.2 | 发布版本脱离本项受影响范围 |
| #325 | [GHSA-72hv-8253-57qq](https://github.com/advisories/GHSA-72hv-8253-57qq) | `backend/pom.xml` | `com.fasterxml.jackson.core:jackson-core` | 2.18.11 BOM | 发布版本脱离本项受影响范围 |
| #324 | [GHSA-r7wm-3cxj-wff9](https://github.com/advisories/GHSA-r7wm-3cxj-wff9) | `backend/pom.xml` | `com.fasterxml.jackson.core:jackson-core` | 2.18.11 BOM | 发布版本脱离本项受影响范围 |
| #323 | [GHSA-mhm7-754m-9p8w](https://github.com/advisories/GHSA-mhm7-754m-9p8w) | `backend/pom.xml` | `com.fasterxml.jackson.core:jackson-databind` | 2.18.11 BOM | 发布版本脱离本项受影响范围 |
| #322 | [GHSA-3pjw-73gf-8qr5](https://github.com/advisories/GHSA-3pjw-73gf-8qr5) | `backend/pom.xml` | `com.fasterxml.jackson.core:jackson-databind` | 2.18.11 BOM | 发布版本脱离本项受影响范围 |
| #321 | [GHSA-5jmj-h7xm-6q6v](https://github.com/advisories/GHSA-5jmj-h7xm-6q6v) | `backend/pom.xml` | `com.fasterxml.jackson.core:jackson-databind` | 2.18.11 BOM | 发布版本脱离本项受影响范围 |
| #320 | [GHSA-hgj6-7826-r7m5](https://github.com/advisories/GHSA-hgj6-7826-r7m5) | `backend/pom.xml` | `com.fasterxml.jackson.core:jackson-databind` | 2.18.11 BOM | 发布版本脱离本项受影响范围 |
| #319 | [GHSA-rmj7-2vxq-3g9f](https://github.com/advisories/GHSA-rmj7-2vxq-3g9f) | `backend/pom.xml` | `com.fasterxml.jackson.core:jackson-databind` | 2.18.11 BOM | 发布版本脱离本项受影响范围 |
| #318 | [GHSA-j3rv-43j4-c7qm](https://github.com/advisories/GHSA-j3rv-43j4-c7qm) | `backend/pom.xml` | `com.fasterxml.jackson.core:jackson-databind` | 2.18.11 BOM | 发布版本脱离本项受影响范围 |
| #317 | [GHSA-jwp6-cvj8-fw65](https://github.com/advisories/GHSA-jwp6-cvj8-fw65) | `backend/pom.xml` | `org.apache.spark:spark-core_2.13` | 4.0.1 / Scala2.13 | 发布版本脱离本项受影响范围 |
| #316 | [GHSA-h7rh-xfpj-hpcm](https://github.com/advisories/GHSA-h7rh-xfpj-hpcm) | `backend/pom.xml` | `io.minio:minio` | 8.6.0 | 发布版本脱离本项受影响范围 |
| #315 | [GHSA-6c25-cxcc-pmc4](https://github.com/advisories/GHSA-6c25-cxcc-pmc4) | `backend/pom.xml` | `cn.hutool:hutool-all` | 5.8.21 | 发布版本脱离本项受影响范围 |
| #314 | [GHSA-4hvc-qwr2-f8rv](https://github.com/advisories/GHSA-4hvc-qwr2-f8rv) | `backend/commons/pom.xml` | `org.redisson:redisson` | 3.22.0 + StringCodec | 发布版本脱离本项受影响范围 |
| #313 | [GHSA-4hvc-qwr2-f8rv](https://github.com/advisories/GHSA-4hvc-qwr2-f8rv) | `backend/api/pom.xml` | `org.redisson:redisson` | 3.22.0 + StringCodec | 发布版本脱离本项受影响范围 |
| #312 | [GHSA-4hvc-qwr2-f8rv](https://github.com/advisories/GHSA-4hvc-qwr2-f8rv) | `backend/analytics/pom.xml` | `org.redisson:redisson` | 3.22.0 + StringCodec | 发布版本脱离本项受影响范围 |
| #309 | [GHSA-5gvw-p9qm-jgwh](https://github.com/advisories/GHSA-5gvw-p9qm-jgwh) | `tools/credential-contract-tests/pom.xml` | `com.fasterxml.jackson.core:jackson-databind` | 2.18.11 BOM | 发布版本脱离本项受影响范围 |
| #308 | [GHSA-cxp5-3px4-pw24](https://github.com/advisories/GHSA-cxp5-3px4-pw24) | `tools/credential-contract-tests/pom.xml` | `com.fasterxml.jackson.core:jackson-databind` | 2.18.11 BOM | 发布版本脱离本项受影响范围 |
| #307 | [GHSA-wv8q-qhhj-9h54](https://github.com/advisories/GHSA-wv8q-qhhj-9h54) | `tools/credential-contract-tests/pom.xml` | `com.fasterxml.jackson.core:jackson-databind` | 2.18.11 BOM | 发布版本脱离本项受影响范围 |
| #306 | [GHSA-gx83-3vf8-gh7j](https://github.com/advisories/GHSA-gx83-3vf8-gh7j) | `tools/credential-contract-tests/pom.xml` | `com.fasterxml.jackson.core:jackson-databind` | 2.18.11 BOM | 发布版本脱离本项受影响范围 |
| #305 | [GHSA-q4xh-88c3-wmh7](https://github.com/advisories/GHSA-q4xh-88c3-wmh7) | `tools/credential-contract-tests/pom.xml` | `com.fasterxml.jackson.core:jackson-databind` | 2.18.11 BOM | 发布版本脱离本项受影响范围 |
| #304 | [GHSA-wjgm-6hv5-3cvf](https://github.com/advisories/GHSA-wjgm-6hv5-3cvf) | `tools/credential-contract-tests/pom.xml` | `com.fasterxml.jackson.core:jackson-databind` | 2.18.11 BOM | 发布版本脱离本项受影响范围 |
| #303 | [GHSA-vvgp-rfg2-7rr6](https://github.com/advisories/GHSA-vvgp-rfg2-7rr6) | `tools/credential-contract-tests/pom.xml` | `com.fasterxml.jackson.core:jackson-databind` | 2.18.11 BOM | 发布版本脱离本项受影响范围 |
| #302 | [GHSA-mhm7-754m-9p8w](https://github.com/advisories/GHSA-mhm7-754m-9p8w) | `tools/credential-contract-tests/pom.xml` | `com.fasterxml.jackson.core:jackson-databind` | 2.18.11 BOM | 发布版本脱离本项受影响范围 |
| #301 | [GHSA-3pjw-73gf-8qr5](https://github.com/advisories/GHSA-3pjw-73gf-8qr5) | `tools/credential-contract-tests/pom.xml` | `com.fasterxml.jackson.core:jackson-databind` | 2.18.11 BOM | 发布版本脱离本项受影响范围 |
| #300 | [GHSA-5jmj-h7xm-6q6v](https://github.com/advisories/GHSA-5jmj-h7xm-6q6v) | `tools/credential-contract-tests/pom.xml` | `com.fasterxml.jackson.core:jackson-databind` | 2.18.11 BOM | 发布版本脱离本项受影响范围 |
| #299 | [GHSA-hgj6-7826-r7m5](https://github.com/advisories/GHSA-hgj6-7826-r7m5) | `tools/credential-contract-tests/pom.xml` | `com.fasterxml.jackson.core:jackson-databind` | 2.18.11 BOM | 发布版本脱离本项受影响范围 |
| #298 | [GHSA-rmj7-2vxq-3g9f](https://github.com/advisories/GHSA-rmj7-2vxq-3g9f) | `tools/credential-contract-tests/pom.xml` | `com.fasterxml.jackson.core:jackson-databind` | 2.18.11 BOM | 发布版本脱离本项受影响范围 |
| #297 | [GHSA-j3rv-43j4-c7qm](https://github.com/advisories/GHSA-j3rv-43j4-c7qm) | `tools/credential-contract-tests/pom.xml` | `com.fasterxml.jackson.core:jackson-databind` | 2.18.11 BOM | 发布版本脱离本项受影响范围 |

## 兼容与安全保护

- Redisson只用于锁/读写锁，原持久化锁hash、数字计数、拥有者及内部LongCodec/StringCodec保留；全局codec显式StringCodec，不选择Kryo5或不受限对象codec。未清Redis、改DB或改缓存格式。原RedisTemplate的JDK序列化路径独立存在，不宣称被这次Redisson修复覆盖。
- Jackson通过BOM统一模块与独立工具；MinIO8.6使用匹配的OkHttp5.1 JVM artifact。Boot仍管理Kotlin1.9.25，独立RustFS工程解析2.2.0；已做实际SDK图片canary，但不以构造/下载成功保证SDK每个路径兼容。
- FastAPI0.133.0支持Starlette1.x；Starlette1.3.1、pytest9.0.3已锁定。应用DTO/路由不变；通用ValidationError文档增加两个可选属性input/ctx，完整OpenAPI字节相等检查保留为失败，不能改称通过。框架现在拒绝缺少正确JSON Content-Type的请求（422），Java调用方已有application/json，不关闭此安全校验。
- 字符串缓存原写法保留原值，却经JSON对象解析返回null，导致新验证码误报过期。现在String在blank判定前直接回读，包含空串、ASCII/全角空白、数字、Unicode、JSON文本和引号文本；TTL/无TTL两个writer均覆盖。非String对象、缺失条目及解析失败路径保留，不做缓存迁移。
- 前端只覆盖受影响的现代glob/minimatch范围，保留旧回调消费者；decoder ESM升级配套修复其CJS消费者。deepmerge正式补丁阻止返回对象prototype poisoning，保留普通对象、数组、symbol和既有配置语义。
- Zod回移[上游安全修复](https://github.com/colinhacks/zod/commit/a0ebf970f2d752e5f58527cd2942ff48be4f46bd)，在leading上界检查时拒绝超限集合，避免先解析/分配。覆盖v3及包内v4/mini、CJS/ESM十入口，保留可能重写输入的检查顺序。保留shadcn/MCP schema和registry/CLI能力，不盲目覆盖为Zod4；不承诺无界schema没有资源成本。
- `frontend/.gitattributes`仅对统一diff patch payload保留上游tab/空行上下文，其他源码diff检查继续生效；实际补丁应用、冻结安装和回归均保留。未改全局Git/工具配置、TLS、私有凭据或安全门槛。

## 验证证据分层

- `05758057`的backend输入（与`e197595`相同）执行Java17完整reactor126项，零失败/错误/跳过，含实际MMDB；credential56项、Python78项通过。最后空白缓存修复另跑commons/API65项和聚焦缓存2项；不将旧组件树的完整套件冒充最终修订亲测。最终候选来自固定修订的独立源码目录 `clean package -DskipTests`；该次只算打包成功，不能算测试通过。此前父工作区增量 package 虽然 exit0，却内嵌陈旧 RedisCacheUtil.class，已保留失败并禁止发布；新三应用内嵌 commons 与同次 clean 构建 archive 完全一致，缓存类也与隔离运行通过的 clean 类一致。
- 当前集成前端在排除私有.env的独立目录、Node22.23.4/pnpm11.24.0完成冻结安装、73/73测试、typecheck及生产构建。Nuxt本身的实际engine高于声明的Node22.12最低值；没有宣称Node22.12经过验收。
- 新API隔离canary：真实验证码邮件到本地Mailpit、错误/重复/一次性验证码、注册默认USER、登录/鉴权/注销及精确清理30项；新MinIO/OkHttp→RustFS图片上传/精确字节/权限/清理等15项通过。原8080及其余受保护进程未替换。此canary在e197595业务树上；后续空白分支由独立回归/最终部署验收补强，不假称这份canary来自更新head。
- 新Nuxt隔离15020实际浏览器9项通过、页面错误0，浏览器和canary停止，原服务身份/清单未变。它通过同源代理使用现有后端，不等于整套升级版本已验收。
- 后端三应用包内Hutool/Redisson/Jackson/JSoup/MinIO（存在的模块）及analytics的Spark4.0.1已直接核对；没有只看父POM声明。
- 原始离线String失败日志/XML、deepmerge/Zod有限反例、MinIO JVM编译失败和框架完整OpenAPI比较失败均保留。一个旧注册HTTP回执文件被重跑覆盖，其旧400/1004观测只能作为已标注的历史转录；原始独立离线失败仍存在，不能把转录当原始HTTP文件。

## 最终本机部署与回退条件

固定修订独立隔离第二轮实际通过149条断言：120条运行/HTTP/安全门和29条实际Java→Python协议；真实MySQL 50行覆盖0/1/NULL/2/-7，Spark只返回10条flag0，四个入口、薪资及参数拒绝均通过。新crawler真实QA鉴权/空范围与空分析/空match/unknown=null协议通过，provider请求0。原服务身份、清单和共享预算未变，自有进程/50行已清理。完整历史账本仍含3条历史失败，不称全账本零失败。

最终干净API包在固定6aa修订再次通过30条实际验证码注册断言及15条RustFS/鉴权/清理断言；此前e197 canary仅保留为历史证据。所有临时进程停止。远端四项CI已在精确6aa通过；最终整栈已串行切换，并完成下述实际端口验收；没有执行真实五组件整体回退演练，不把合成拒绝测试当实际回退成功。

本次发布使用独立源码的 clean 三包、固定绝对路径的 Python3.13 venv、冻结构建的 Nuxt产物及永久目录内 Node22.23.4；运行产物设为只读，日志/临时目录/现有预算留在产物之外。五个组件均标记代码来源 `6aa3b079ffa712a3b4354f21dbbea4086d7bac23`；后续文档提交不改变这份已部署业务代码。

| 组件 | 本机端口 | 最终 PID / starttime |
|---|---:|---|
| Python | 8090 | 469536 / 1035971 |
| crawler | 8081 | 469644 / 1036100 |
| API | 8080 | 469996 / 1036459 |
| analytics | 8082 | 470340 / 1037125 |
| Nuxt | 5020 | 470696 / 1037573 |

这些是验收时的进程身份，不是永久不变的在线声明。API aggregate/readiness、其他组件健康、Nuxt响应均200；Python ready200只证明配置/进程就绪，不证明供应商可用。

最终服务实际验收167条：基础HTTP/图片/权限/注销24，标准库严格客户端三层招聘JSON与分帧/空范围匹配30，简历CRUD/归属/历史27，commit/merge/revert/stash-pop18，本地Mailpit验证码注册30，系统Chromium实际业务流程9，实际Java→Python无模型HTTP29。浏览器页面错误0，浏览器已关闭；自有账号/邮件/简历/图片及会话均按唯一归属清理。未对既有业务行手工DDL/DML。四个原基础容器和Mongo身份/监听保护保持，模型账本逻辑内容未变，provider调用0。

切换依次Python→crawler→API→analytics→Nuxt；每个stop前核对PID/starttime/exe/argv/cwd、全部已知listener归属、元数据CAS、基础组件及预算，analytics另核对Spark UI归属及无RUNNING任务，使用pidfd有界SIGTERM，无裸PID/进程组/强杀或不确定状态自动恢复。已修正并定向复核未监听child回退、restoring重入和Spark idle三项问题；14+12项纯合成检查通过，不能替代实际整体回退。最终清单仅改五个归属进程记录并新增本次发布回执，其余历史字段逐项保留。旧b97 pair控制器保留历史并标记superseded，不继续作为新版发布入口。

本机永久私有证据目录：`/home/david/.local/share/bossanalyze/delivery-recovery-20261109-sac9b48g/security-final-6aa3b07-4xi7hgsk`。`acceptance.json`、`metadata-preservation.json`、`packaged-provenance.json`和各分项回执记录本次事实；所选149条隔离运行证据及历史失败已另保留到其`runtime149/`。含环境的release-state-private.json与before-*备份仅本机私有保存，禁止粘贴或提交。

可操作回退入口是该目录的`release.py rollback`：动作前重新检查完整身份、CAS、基础组件与预算，恢复保存的原argv/env/cwd/产物，发布实际恢复PID及原来源，保留数据库约束。身份或清单变化时保守停止，不能强行重跑；原路径/配置须保留。**没有执行实际整体回退**，也没有宣称重启后持久运行、生产部署或外部邮件已验收；旧依赖回退版本包含本次已修风险，只能作为故障恢复路径。

原始运行证据含私有环境、账号、会话和备份的目录不得提交或输出。公开文档仅记录版本、接口状态、断言与清理结论；本机路径由最终运行交接提供。

本轮不做真实BOSS数据获取、付费模型或外部邮件投递。Jev ZDR能力、独立人工gold、真实模型质量、外部SMTP及生产验收仍未通过；质量批准/正式调度/自动写回保持关闭，不改固定模型或提高既定provider预算cap。
