# 受控流程：合成输入、真实组件

在仓库根目录运行：

```sh
python tools/workflow-fixture/run.py
```

入口只构建 `commons` 和 `crawler` 所需 reactor（`-DskipTests package`），从刚构建的 crawler JAR 提取当前类和已有依赖，用 Java 17 编译 fixture 组装代码；不使用旧 `/tmp` Java class，不运行付费重放脚本、不安装依赖。默认 Java 为 mise `zulu-17`，Maven 为 mise `3.9.16/apache-maven-3.9.16/bin/mvn`，Python 为已有 `/tmp/boss-jev-rereview-evidence/venv/bin/python`。机器路径变化时分别设置 `WORKFLOW_JAVA_HOME`、`WORKFLOW_MAVEN`、`WORKFLOW_PYTHON` 指向已有工具链/venv。需要 `/usr/bin/chromium` 和已有 loopback MySQL；不需要 BOSS 登录、provider key、真实账号或简历。

输出首行给出本轮私有目录 `/tmp/boss-workflow-20261108-*`，末行是 JSON 状态和 DTO 路径。目录和文件默认仅当前用户可读。正常退出和 SIGTERM/键盘中断执行清理；`cleanup.json` 中 `liveOwnedProcesses: []` 才表示未发现存活的自有进程。`processes.json` 记录进程及观察到的浏览器子进程的 PID、内核 starttime、父 PID 和进程组。SIGKILL/断电无法运行 finally，需要按该清单核对身份后清理。

## 边界与来源

| 环节 | 实际执行 | 合成/组装部分 |
| --- | --- | --- |
| 浏览器 | 系统 Chromium、已有 Java Playwright、独立 context/Page | `boss.json` 的两条职位；fixture HTML 列表 |
| 页面网络 | 每个请求仅 fulfill 或 abort；无 resume/fallback | 仅 `https://www.zhipin.com/web/geek/job` 无 query，及 `/wapi/zpgeek/job/detail.json?jobId=fixture1/fixture2` 精确匹配；未知请求 abort |
| 采集 | 当前 `RecruitmentSampler.collect`、`BossResponseCapture`、`DetailParser`、`PrivateOutput` | 组装 Page，不声称 sampler CLI main、真实平台登录或兼容性验收 |
| AI | loopback Uvicorn/FastAPI HTTP、当前 Jev 客户端、候选抽取、校验、证据与匹配流水线 | 仅本 fixture app 注入 `httpx.MockTransport`；官方 host、模型、ZDR header 仍校验，无外部 provider 网络 |
| 写入 | 当前 Java `AIExtractionOfJobRequirements`、`JobAnalysisStore.process`、Spring 事务、MySQL | fixture 构造的 `quality_approved=True` 和 `writeback=true`，只对新 schema 内的合成样例 |
| 读取 | 当前 MyBatis mapper、`JobAnalysisReader`、`FormatDataService`，实际 HTTP 请求 | 最小 JDK HTTP envelope 和排名组装，非生产 API 鉴权路由 |

Provider 回应形状复用 `ai-service/tests/test_contract.py` 的 `fixture`：noul=0.99、choice=scored、score 分布为相邻两档各 0.5。为验证排序，jobId=2 的匹配 rawScore 为 2.5，其余为 3.5。这里故意将每个候选同时判为要求和福利；这是确定性传输/校验 fixture，不是语义质量、独立 gold 或人工标注。标签保持 pending；不改变生产 quality/writeback 全局开关。

组装结构参考 `/tmp/boss-jev-postfix-41420e0` 的 MockTransport/事务 harness 和 `/tmp/boss-jev-identity-7255a96` 的 MyBatis/HTTP harness；交付入口不依赖它们的脚本、类或导出。只使用现有独立 venv 的运行时依赖。

## 数据流与证据

`sample/samples.jsonl` 是真实 PrivateOutput 导出，`sample/labels.jsonl` 为 pending。Java 从该文件逐行读 `detailData`，同一对象用于新 schema 的 `detail_data` 和真实 AI 请求，不手写分析结果。`analysis-1.json`、`analysis-2.json` 来自实际 Python HTTP 经 Java 校验后的数据库 `result_json`；`provider-requests.jsonl` 是 MockTransport 收到的真实官方格式业务请求（无授权 header）。`source.json` 标识构建来源和工具链。

每次新建 `workflow_20261108_<12位随机值>`；确切名称见 `schema.txt`。仅从私有 properties 程序化读取 MySQL 用户名/密码；若文件没有 URL，使用 `127.0.0.1:3306`，如有 URL 则强制 loopback。先连接不选择任何现有业务 schema 的地址，新建 schema 后只在其中创建必要 `t_job_detail` 和执行当前 `20261108_job_ai_analysis.sql`。两条正例来自本轮采集，第三条失败例复用本轮第一条合成导出。schema 留作审计，不删除旧库、不导入快照、不接触 Redis。

`checks.json` / `result.json` 包含执行断言：

- 两条采集成功、浏览器全部本地 fulfill、标签 pending。
- 无内部 token 的真实 Python HTTP 请求返回 401，provider 调用数前后不变（`negative-auth.json`）。此处只验内部 token 门禁；不宣称主 API 用户身份链已验收，也不需要 QA 登录。
- 两条真实 `JobAnalysisStore.process` 完成；sidecar source 与采集导出结构一致；两列 JSON 数组等于模型返回原文，保留 Unicode/emoji/标点。
- provider 503 的真实 HTTP 失败，经 Jev 有界重试/Java 错误处理后零完成、两列仍为 `[]`，记录在 `failed-row.json`。sidecar `retry_wait` 被 reader 映射为 pending。
- 实际 match `scored`、各维度数字与证据通过 Java 校验；加权分数为 8.75/6.25，排名降序。
- 实际 loopback reader HTTP 返回完整 DTO，并验证 completed/pending 状态。

## 后续 frontend 消费

本机 Nuxt 已运行于 5020 时，可把入口输出的 DTO 路径传入持久化展示验证：

```sh
node tools/workflow-fixture/frontend.cjs /tmp/boss-workflow-20261108-<本轮目录>/frontend-dto.json
```

该脚本使用本机已缓存的 Playwright 和系统 Chromium 外部窗口，不安装依赖。它仅在独立浏览器 context 中适配同一 DTO 的招聘 API 与合成会话；页面和静态资源来自实际 Nuxt，其余网络拒绝。私有输出目录含截图、原 DTO、适配映射及 DOM 断言结果，浏览器结束即关闭。它验列表、详情、Unicode 数组、分析来源和空态，不代表主 API 身份链、真实模型质量或不存在的匹配页面已验收。

`reader-route.txt` 记录本轮真实 GET `http://127.0.0.1:<随机空闲端口>/fixture/v1/jobs`。该路由在本轮查询结束即关闭；保留的 `frontend-dto.json` 是其实际 HTTP 响应体。后续 frontend 可以将该私有文件作为合成展示输入，或重新运行入口生成新文件；不能把已关闭路由当常驻服务。入口没有常驻模式、没有孤儿服务。

Envelope：`schema`、`synthetic=true`、`items[]`、`match`、`rankedMatch[]`。每个 item 的 `job` 是真实 `FormatDataService` 的 `JobAnalysisData` DTO；`analysisStatus` 来自真实 reader；`model`/`pipelineVersion` 来自 sidecar；`sampling` 是实际采集记录，带 `sourceUrl`、`sourceJobId`、`identity`、`revision`、`fetchedAt`、`samplingSource`、`detailData`。`match` 保留真实 Java AI 客户端返回值，`rankedMatch` 是 fixture 按 score 降序的展示视图。该 envelope 需要前端显式适配，不能冒称现有生产 API route/DTO 已替换。

联调发现的 DTO 转换问题已修复：缺失地址保持 JSON null，mapper 读取的职位 URL 由共享转换器填入 `jobUrl`；入口通过真实 HTTP 响应断言这两项。

本次不验真实 BOSS、模型语义质量/付费服务、主 API 用户鉴权、frontend UI 或生产写回；没有运行全量测试。新增逻辑以本入口内的真实整合断言验收，不将跳过的测试算作通过。
