# 最终 crawler/Python 固定版本构建记录

日期：2026-11-09。Refs #52；父任务 #50、父 PR #53。仅构建完成，尚未部署验收。

## 固定来源与归属

- 运行源码：`b97ab94da701630b78bae7553622919fc7efd8bd`。
- 源码树：`5486dec043a5c9bf589a95080261e9516da2bc09`。
- 版本根目录（下文 N）：`/home/david/.local/share/bossanalyze/stack-deployment-20261108/releases/b97ab94-20261109-_2172r6g`。
- 在新建的 `N/src` 中直接解包 `git archive <完整目标 SHA>`；构建前后核对 606 个受跟踪文件内容均与目标一致。父分支 spec 种子及并发 #49 均未进入源码。
- 本任务仅写 `N/src`、`N/crawler.jar`、`N/ai-service`、`N/build-logs`、`N/provenance.json` 和本说明；未修改控制器、配置、current/manifest 或其他 worktree。

## 实际工具与命令

Java：Zulu OpenJDK `17.0.20.1`（Zulu17.68+203-CA），`JAVA_HOME=/home/david/.local/share/mise/installs/java/zulu-17`。
Maven：`/home/david/.local/share/mise/installs/maven/3.9.16/apache-maven-3.9.16/bin/mvn`，版本 `3.9.16`。
Python：`/home/david/.local/share/uv/python/cpython-3.13.15-linux-x86_64-gnu/bin/python3.13`，版本 `3.13.15`。
uv：`/usr/bin/uvx --offline --from uv==0.9.9 uv`，实际版本 `0.9.9`。

在 `N/src` 执行指定 Maven 的 `-o -f backend/pom.xml -pl crawler -am verify`。
将固定源码中的 `ai-service` 复制到最终 `N/ai-service` 后执行：

```sh
uvx --offline --from uv==0.9.9 uv sync --offline --frozen \
  --python /home/david/.local/share/uv/python/cpython-3.13.15-linux-x86_64-gnu/bin/python3.13 \
  --project "$N/ai-service"
# 以下 cwd 为 N/ai-service
uvx --offline --from uv==0.9.9 uv run --offline --frozen python -m pytest -q
uvx --offline --from uv==0.9.9 uv run --offline --frozen python -m compileall -q app
```

构建进程使用独立最小环境，不继承真实 token/key/密码/Cookie。设定 `CMD_ZDR=1`、`AI_QUALITY_APPROVED=0`、scheduling/writeback=false、`PYTHONDONTWRITEBYTECODE=1`、禁止 Python/浏览器下载；测试预算指向本任务私有独立路径，不读取现行预算。没有加载真实服务环境或启动采集。

## 首次实际结果

| 检查 | 实际结果 | 私有日志（相对 N） |
| --- | --- | --- |
| Java/Maven/Python/uv 版本 | 各 exit 0，版本如上 | `build-logs/{java,maven,python,uv}-version.log` |
| Maven 离线 verify | exit 0；commons 15 项、crawler 49 项；失败/错误/跳过各 0 | `build-logs/maven-verify.log` |
| uv 离线 frozen sync | exit 0；安装 22 个锁定依赖；无缓存缺项 | `build-logs/uv-sync.log` |
| pytest | exit 0；75 passed，失败/错误/跳过各 0 | `build-logs/pytest.log` |
| compileall app | exit 0 | `build-logs/compileall.log` |

所有检查仅运行一次，没有通过重复运行掩盖首次失败。原始日志完整保留在私有目录，未提交仓库；结构化命令、cwd、开始时间、退出码见 `build-logs/checks.json`。Java 实际计数来自两个模块的 `target/surefire-reports/TEST-*.xml`。

## 产物与封存

唯一主 JAR：`N/src/backend/crawler/target/spring-crawler-0.0.1-SNAPSHOT.jar`，排除 `.jar.original` 后复制为 `N/crawler.jar`；两者摘要一致。已核对 Spring Boot 的 Start-Class 为 `com.david.hlp.crawler.SpringCrawler`。

- JAR 大小：275359862 字节。
- JAR SHA-256：`40eba481e4cfcafc452351ef2f9d31844eefeebc4f235f2b67dd410872de0063`。
- Python 产物：`N/ai-service`；venv 直接在 `N/ai-service/.venv` 创建，没有搬移。控制台入口指向最终路径的 venv Python，底层解释器为上述固定 CPython。
- 锁文件未修改；必要来源、JAR/锁文件摘要、版本、实际计数及每项退出码记于 `N/provenance.json`，未对整个目录制作泛化摘要。
- 已对本任务五个归属路径移除写权限及 group/other 权限，目录 0500、普通文件 0400，原本 owner 可执行文件保留 0500；不跟随符号链接，不改变共享解释器或控制器目录。封存权限核对通过。

运行时必须由 root 设置 `PYTHONDONTWRITEBYTECODE=1` 并沿用原外置绝对预算路径；本任务未用真实环境启动验证。只读产物不应原地更新，后续构建使用新目录。

未运行前端/API/analytics 全套、`tools/workflow-fixture`、服务启动、canary、切换、真实模型/付费请求、BOSS/浏览器或业务库读写。controller、配对配置、真实 HTTP 和部署门槛由 root 后续独占验收；本记录不表示这些已通过。产物依赖现有固定 CPython 安装路径，不可搬移目录或删除该解释器。
