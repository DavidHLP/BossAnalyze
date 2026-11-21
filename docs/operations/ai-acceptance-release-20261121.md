# AI 验收发布准备记录（2026-11-21）

关联 [#54](https://github.com/DavidHLP/BossAnalyze/issues/54) 与 [#72](https://github.com/DavidHLP/BossAnalyze/pull/72)。本轮在 `ai-acceptance/release` 工作树基于 `ad87f29d9adf4fff28260b6b26c620c4b77e79a0` 生成候选包，执行离线测试和只读预检。未发布产物、启停服务、写数据库、登录 BOSS 或调用模型。

## 候选包与简历隐私

| 命令 | 结果 |
| --- | --- |
| `mvn -f backend/pom.xml -pl crawler -am package -B -DskipTests` | 退出 0，`BUILD SUCCESS`。Reactor 包含父项目、commons、crawler |
| `mvn -f backend/pom.xml -pl crawler -am test -B` | 退出 0，`BUILD SUCCESS`。commons 18 项、crawler 55 项，失败、错误和跳过均为 0 |
| `sha256sum -c backend/target/release-evidence/crawler.sha256` | 退出 0，候选 JAR `OK` |
| `git diff --check`、`git diff --cached --check` | 退出 0，无输出 |

Maven 3.9.16、Azul JDK 17.0.20.1。两条 Maven 命令在隔离网络中执行。测试首轮因隔离环境中的 Mockito 动态 attach 失败而退出 1，commons 有 4 项错误、crawler 未运行。重跑同一测试命令时通过 `JAVA_TOOL_OPTIONS` 增加 `-javaagent:<本地 Maven 缓存>/net/bytebuddy/byte-buddy-agent/1.15.11/byte-buddy-agent-1.15.11.jar`，复用仓库已解析的测试依赖。未改 POM 或测试。

- JAR 路径为 `backend/crawler/target/spring-crawler-0.0.1-SNAPSHOT.jar`。打包后和测试后的 SHA-256 均为 `be58adeb21832c084797e9989513dbc73801e36556040532601a8737471763e0`。
- `ResumePrivacyTest` 6 项全部通过。合成身份覆盖 legacy 和 v2 两条匹配入口。已认证的请求在读取正文前收到 503（`AI_RESUME_MATCHING_DISABLED`），匿名或撤销身份收到 401。测试跟踪正文读取，并断言没有访问简历、职位 mapper 或模型的交互。
- 逐字节比较 JAR 内 `BOOT-INF/classes/` 的全部 87 个类与测试类路径上的 crawler 主类。嵌套 `commons-0.0.1-SNAPSHOT.jar` 与本轮 commons JAR 逐字节一致，包内全部 45 个类也与测试类路径一致，含 `DownstreamAuthFilter`。嵌套 JAR 的 SHA-256 为 `e99d4779d26a7c9f9be0b569a22ea161727599769ef5203628e4e3e0903f7eec`。
- 从该哈希的 JAR 提取字节码并用 `javap` 核对。`AIRequirementsController` 用 `@ModelAttribute` 在 MVC 解析 `@RequestBody` 参数前调用 `AuthorizedResume.requireSafeMatching`。后者先判断 `boss.verified`（缺失时 401），再调用 `ResumePrivacy.requireSafeMatching` 抛出 503。

可在工作树根目录复核上述三处关键类。完整日志、`javap` 输出和校验结果在本工作树忽略的 `backend/target/release-evidence/` 下，测试报告在各模块的 `target/surefire-reports/` 下。

```sh
set -e
jar=backend/crawler/target/spring-crawler-0.0.1-SNAPSHOT.jar
evidence=backend/target/release-evidence
sha256sum -c "$evidence/crawler.sha256"
for class in controller/AIRequirementsController service/AuthorizedResume service/ResumePrivacy; do
  entry="com/david/hlp/crawler/ai/${class}.class"
  extracted="$evidence/${class##*/}.class"
  unzip -p "$jar" "BOOT-INF/classes/$entry" > "$extracted"
  cmp "$extracted" "backend/crawler/target/classes/$entry"
  javap -p -c -v "$extracted"
done
```

当前 8081 已认证请求返回 503 仍待验证。#54 记录的当时实例返回 400。离线结果只覆盖上列候选包及测试边界。

## 整体回退预检

按[工具文档](../../tools/rollback-preflight/README.md)使用 `PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover -s tools/rollback-preflight -v`，退出 0，11 项通过。实际 CLI 调用如下，参数指向工作树范围内缺失的恢复输入，未创建这些文件。

```sh
release_record="$PWD/backend/target/release-evidence/recovery-inputs"
python3 -B tools/rollback-preflight/preflight.py \
  --state "$release_record/release-state-private.json" \
  --repository "$PWD" \
  --metadata "current=$release_record/current.json" \
  --metadata "manifest=$release_record/manifest.json" \
  --metadata "session=$release_record/session.json" \
  --metadata "pair-state=$release_record/pair-state.json" \
  --metadata "config=$release_record/config.json"
```

退出 1，输出如下。缺少同一次历史发布的私有 state 和五份匹配元数据，也未提供独立审阅的安全回退目标回执及其固定 SHA-256。由于 state 缺失，CLI 未核对进程、健康、CAS、容器、Mongo 或账本。#54 另记录了当时现场的 5020 进程身份漂移和旧目标不满足安全基线。本轮未复验这些现场差异，未执行回退。

```json
{"status": "refused", "reasons": [{"check": "state", "reason": "unreadable or invalid release state"}], "checked": [], "rollbackExecuted": false}
```

## 5020 前端身份

- 开发模式由 Vite 监听 `127.0.0.1:5020`，生产由 `pnpm start` 执行 `server/index.ts` 并读取 `dist/`，默认也用这个端口。Vite dev/preview 和生产入口均调用 `server/api-proxy.ts`，同源转发 `/api/v1/ai/**`、`/api/v2/ai/**` 到 8081，其余 `/api/**`（含账户）到 8080。代理转发上游端到端响应头，自身不生成 Mock 标记。[前端运行说明](../../frontend/README.md)
- `VITE_BOSS_MOCK_PREVIEW=1` 的两个引用都受 `import.meta.env.DEV` 限制，只影响简历编辑的图片入口和导出目的地。浏览器审计测试才要求独立 Mock 端点返回 `X-BossAnalyze-Preview: mock`。
- 本轮用 `ss -H -ltnp 'sport = :5020'` 核对监听者，并从对应 `/proc/<PID>/cmdline` 仅核对入口路径。Node 运行的 `start.mjs` 位于仓库之外的 Mock／预览路径。#54 已记录 5020 账户接口带 Mock 标记，并在独立端口验证了基于 `ad87f29` 的生产前端对接真实 API。端口复用掩盖了启动入口差异。现有代码的代理和环境开关与该差异无关，本轮仅记录部署身份，无前端代码改动。

## 受控发布清单

- [ ] 固定发布源码、干净构建命令和各产物 SHA-256。集成后如 crawler 或其构建输入有变，重新构建、运行隐私测试并核对包内类。记录预期的 5020 生产启动入口及 `dist/` 身份。
- [ ] 按同一次历史发布获取私有恢复记录和五份元数据。独立审阅旧目标源码、产物、依赖、运行时和完整配置，核对工具的冻结安全基线并通过受信渠道固定目标回执摘要。仅持有候选 JAR 或历史 `accepted` 记录不能满足回退条件。
- [ ] 正式变更前用真实私有输入执行只读预检。遇到身份、CAS、健康、受保护状态或安全目标拒绝时停止切换，查明差异并另行建立受审恢复基线，禁止自动刷新 `expectedMetadata`。回退执行及验证须另行授权，临动作前重新核对身份与 CAS。
- [ ] 获准后按受控流程校正 5020：核对监听 PID、启动时间、入口、产物与恢复清单；通过 5020 核验账户请求到真实 8080，并检查响应没有 Mock 标记。
- [ ] 获准切换 crawler 后，确认 8081 运行包 SHA-256。以现有合成 QA 身份和无个人资料的畸形正文直接核验 v1/v2 已认证 503、匿名 401，再经 5020 验证同源结果。核对 Python readiness 保持匹配暂停，并按单独审批核对质量、调度与写回门禁。AI 评估通过时仅可标记 `ai_accepted`，保留其非人工真值的限制。
