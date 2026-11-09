# 最终 crawler/Python 本机部署执行计划

日期：2026-11-09。状态：规划完成，尚未实施。范围仅本机8081 crawler与8090 Python；不是生产发布、真实推理或质量批准。

## 目标与当前证据

固定目标：b97ab94da701630b78bae7553622919fc7efd8bd（main阶段合并提交）；树5486dec043a5c9bf589a95080261e9516da2bc09，与已验证fbc0fe9源码一致。执行前重新读远端：若main移动，不自动更换目标；先评估crawler/Python差异，涉及本部署则暂停并更新计划。

两份独立只读核查报告称：旧crawler为2232981/5986464、旧Python为2231337/5984958，均使用旧58aa2db时期产物；18081/18090当时空闲；现行服务健康200。以上只描述规划核查时，不得直接用旧PID操作。

现有根R=/home/david/.local/share/bossanalyze/stack-deployment-20261108，保留R/latest、R/rollback及现行配置。新版本N建议放R/releases/b97ab94-20261109-<唯一批次>，若存在则停止，禁止覆盖。临时构建和运行产物均不依赖共享工作区target。

## 独立计划裁决

| 项目 | 采用方案与原因 |
|---|---|
| 版本目录 | 独立新目录，保留全部现行产物；采用唯一批次便于失败后保留证据 |
| 控制器 | 以R/control.py为基础，在N中创建限定crawler/Python的版本控制副本；不修改共用旧controller |
| canary | 先Python18090，再crawler18081；crawler显式指向18090，两者同时保留供验证 |
| 正式切换 | 停旧crawler→停旧Python→起新Python8090并验→起新crawler8081并验；不保留新旧混用观察窗 |
| 回退 | 停新crawler→停新Python→恢复旧latest Python→恢复旧latest crawler；优先整对恢复 |
| 状态记录 | 进程动作立即写私有操作日志；验收后发布成功状态。失败回退也写实际新PID，不能恢复旧PID快照 |

现有controller已直接核对：Python版本被限制为latest，crawler固定连8090，sourceSHA硬编码58aa2db，probe启动后立即停止，Python失败不自动恢复，部分异常路径可能留下pending记录。因此下述能力属于实施前置，现有命令不能直接用于新版切换。

## 1. 执行前冻结

- 只读核对8081/8090的监听属主、PID/starttime、exe/cwd/产物、当前配置来源；同时记录8080、8082、5020及RustFS/Mailpit身份作为保留基线。
- 备份current.json、主manifest和旧controller至N，目录0700、含配置/进程元数据文件0600。不打印完整argv、env、日志或manifest。
- 程序内比较Java/Python的内部token、model、AI_MAX_INPUT_BYTES/JOBS/CANDIDATES、deadline及其他已批准设置；只输出安全值和相等布尔结果。保留既有密钥、模型、绝对预算路径、cap及策略，不重新生成或复制到其他机器。
- 固定CMD_ZDR=1、AI_QUALITY_APPROVED=0、crawler AI scheduling=false/writeback=false、SQL init=never/ddl-auto=none，采集任务全部关闭。
- 端口属主、身份、配置配对或保护开关不符即停止；不杀未知进程、不覆盖他人状态。

## 2. 构建与最小必要检查

在独占目录用git archive导出目标提交；不要在共享checkout构建。显式使用现有Java17、Maven3.9.16、Python3.13.15、uv0.9.9。

命令轮廓（仅计划，路径须由本轮创建者设置）：

```sh
# 在固定源码导出目录S执行
JAVA_HOME=/home/david/.local/share/mise/installs/java/zulu-17 \
PATH=/home/david/.local/share/mise/installs/java/zulu-17/bin:$PATH \
/home/david/.local/share/mise/installs/maven/3.9.16/apache-maven-3.9.16/bin/mvn \
  -o -f backend/pom.xml -pl crawler -am verify
# 将唯一crawler主JAR复制为N/crawler.jar；将目标ai-service复制到N/ai-service
uvx --offline --from uv==0.9.9 uv sync --offline --frozen \
  --python /home/david/.local/share/uv/python/cpython-3.13.15-linux-x86_64-gnu/bin/python3.13 \
  --project "$N/ai-service"
(cd "$N/ai-service" && uvx --offline --from uv==0.9.9 uv run --offline --frozen python -m pytest -q)
(cd "$N/ai-service" && uvx --offline --from uv==0.9.9 uv run --offline --frozen python -m compileall -q app)
```

离线缓存缺失则列出缺项，不改锁或偷偷改版本。venv在最终路径创建，不能建好后搬动。运行时设置PYTHONDONTWRITEBYTECODE=1，预算文件仍使用原外置绝对路径。构建和验证完成后保留独立产物，禁止原地更新。

门槛：所有命令exit0、无失败/错误/非预期跳过；记录实际计数、源码完整SHA、产物来源和必要摘要。历史CI及18项Java定向/75项Python结果只是背景，不替代本轮产物验证。不重跑无关前端/API/analytics。

## 3. 控制器最小修订与校验

在N中的副本保留现有start/stop、umask、PID/starttime守卫和SIGTERM逻辑，只增加：

1. old-latest/new-target的绝对产物映射、真实sourceSHA和正确Python artifact/cwd；组件白名单仅crawler/Python。
2. crawler显式Python地址覆盖、持有式成对canary、独立运行cwd/JVM tmp/HDFS日志/控制台日志。
3. 启动后立即登记pending身份；异常、超时、信号中断均只清自己且身份匹配的进程；旧版本支持两组件实际恢复。
4. 两组件操作前后状态日志与current/manifest的受控原子替换，拒绝覆盖并发变化。

必要校验：Python语法检查；最小离线反例覆盖PID复用不停止、启动失败清理、Python恢复后crawler恢复、manifest并发变化不覆盖。随后使用canary验证实际启动/停止及旧产物冷启动。无必要不新增通用部署框架或依赖。

## 4. 成对canary与无模型HTTP验收

先确认18090/18081空闲。启动新Python18090，再启动新crawler18081，后者显式指向http://127.0.0.1:18090；都只监听loopback。令牌只在私有环境/程序内存传递，不上命令行。

检查两者健康、实际产物路径、PID/starttime/cwd、保护开关及配置配对。不能把ready200当版本证明；版本由固定源码→新产物→真实进程使用路径证明。未知启动错误停止；已知ChromeDriver不可用须确认仅限制未启用的采集能力，列为明示限制，不借本部署修复或访问BOSS。

从新crawler.jar提取类/依赖，用一次性Java探针调用真实AIExtractionOfJobRequirements，不启动Spring/mapper/Store：

| 请求 | 预期 | 为什么不触发模型 |
|---|---|---|
| analyze("deploy-contract", "{}") | needs_review、三个空数组、正确模型/版本/requestId、usage=null | 无事实候选，提前返回 |
| match("synthetic", []) | items=[]、complete=true | 空jobs，不创建推理任务 |
| 单个job.detailData={}的match | insufficient_evidence、score=null、四维unknown、complete=false | 职位无候选，提前返回 |
| Python匿名内部请求 | 401 | 认证前拒绝 |
| 认证的非法结构 | 422 | 输入校验拒绝 |
| crawler直接/同源匿名AI入口 | 401 | 后端授权拒绝 |

这些请求验证真实HTTP、Bearer、字节发送及Java响应校验，不验证真实评分、召回或完整用户身份链。只使用空事实和明确合成文本，绝不运行现行Store或fixture写回实例。

只读核对原共享预算账本前后值；不能因未知并发变化重置或退款。出现provider请求、预算异常变化或写回迹象即停止调查。通过后先停canary crawler，再停canary Python，确认临时端口和pending均清理。

## 5. 正式串行切换

构建与canary阶段不停止现行服务。开始切换前再次核验所有目标身份与端口，备份最新元数据，随后：

1. 停旧crawler8081，SIGTERM有界等待；不SIGKILL。
2. 停旧Python8090。
3. 起新Python8090，验证配置、健康与无模型HTTP。
4. 起新crawler8081，指向8090，验证健康、实际新产物及匿名拒绝。
5. 再做真实新Java client→新Python无模型探针，核对预算与关闭门槛。
6. 只读确认API/analytics/Nuxt身份未变，入口仍可用。

不并行切换。8081在切换期间不可用，AI相关请求可能失败；这不是零停机升级。时长须以新canary启动实测确定，不承诺固定秒数。身份变化、优雅停止超时、端口归属不明时不得继续替换。

## 6. 状态发布与回退

每次启动/停止立即写N中的私有操作日志：阶段、组件、版本、真实PID/starttime、端口、健康和失败原因。验收成功后原子更新current.json，再以并发变化检查更新主manifest中的crawler-final/python-final与版本记录；其他组件及无关顶层字段必须逐项保持。

多文件更新不是跨文件事务。若一份更新成功而另一份冲突，保留操作日志和真实状态，报告“元数据尚未对齐”，禁止强行覆盖。不能以保留旧文件掩盖服务已经换过。

任何配对/认证/契约/健康/版本失败、保护开关异常或异常预算变化都触发整对回退：停本轮新crawler→停本轮新Python→用保留的现行latest产物/原环境恢复旧Python→恢复旧crawler。不是R/rollback里更早的Java产物，也不是单纯stop Python。恢复后复验健康和无模型HTTP，将重新启动的旧版真实PID写回current/manifest；不得复制旧PID快照。并发冲突仍只保留实际日志并报告，不能覆盖他人条目。

恢复失败则停止进一步操作，明确8081/8090实际缺口，保留新旧产物、日志、配置、数据及预算。旧版回退会重新带回尚未部署的缓存/部分结果/字节等缺陷，仅恢复旧版服务，不表示最终修复已生效。旧版空jobs原本可返回成功；不将本次迭代中的短暂空批次回归误列为58aa2db固有缺陷。

## 完成条件与不包含项

全部满足才记“最终版本机部署完成”：新产物构建验证通过；控制器清理/恢复门槛通过；成对canary和旧版冷启动可回退；8081/8090实际使用目标产物；真实无模型HTTP通过；配置配对及关闭门槛正确；预算没有本轮推理消耗；其他服务身份不变；状态文件准确；无本轮临时进程残留。

不执行：真实BOSS/浏览器、付费provider、非ZDR降级、模型替换、预算重置/加cap、业务库DDL/DML/seed/flush、AI正式调度/自动写回、API/analytics/Nuxt/RustFS/Mailpit变更、生产发布或正式质量批准。新分析表的业务库迁移仍属单独待办，不与这次版本部署捆绑。

## 输入与边界

独立计划A：/tmp/boss-final-crawler-python-plan-codex-20261109.md。
独立计划B：/tmp/boss-final-crawler-python-plan-claude-code-20261109.md。
规划调用分别为Codex/gpt-6.1-sol(high)与Claude Code运行端/deepseek/deepseek-v4.1-flash(high)，后者不是Anthropic Claude模型。

根计划另直接读取现有control.py核对限制，并修正了滚动混用、失败后状态记录和旧版空批次说明。未改仓库/控制器/配置，未运行构建、canary、模型或部署。上述新增控制能力和探针是后续实施要求，不是已可用的命令。

## 执行任务图

父任务 #50。控制器 #51 与产物构建 #52 可独立推进；两项通过后，父任务执行成对canary、串行切换与无模型验收，最后完成独立审查。真实采集和领域质量仍由 #48 跟踪。本文件为冻结部署要求，运行结果另写操作验收报告。
