# BOSS 浏览器招聘采样 CLI（线上未验收）

本工具与原业务采集路径隔离，唯一入口为显式 CLI。不启动 Spring，不连接业务数据库，不调用 Jev/AI，不点击投递、聊天、发送简历，不绕过验证。仅支持 BOSS；旧四平台匿名 HTTP/JSON-LD 方案已归档、不随本版本提交。

## 构建与离线检查

在仓库根目录，使用 Java 17、Maven 3.9.6+：

```sh
export JAVA_HOME=/home/david/.local/share/mise/installs/java/zulu-17
export PATH="$JAVA_HOME/bin:$PATH"
/home/david/.local/share/mise/installs/maven/3.9.16/apache-maven-3.9.16/bin/mvn -f backend/pom.xml -pl crawler -am verify
```

仅运行定向离线测试：同一 Maven 命令改为 `test -Dtest=RecruitmentSamplerTest -Dsurefire.failIfNoSpecifiedTests=false`。fixture `backend/crawler/src/test/resources/sample/boss-detail.json` 与 `boss-detail.html` 是自写合成响应/SSR，含用于验证白名单的虚构敏感节点，不是真实招聘或登录数据。测试不启动任何浏览器、不访问平台、业务库或 AI。

## 父任务人工运行方式

以下命令会操作浏览器，只供父任务在授权环境验收使用；本实现任务没有执行这些采集命令。不要用普通 `java -jar`，它仍是原 Spring 服务入口。

已有本机 CDP（工具不会启动调试端口或改浏览器安全配置）：

```sh
java -Dloader.main=com.david.hlp.crawler.sample.RecruitmentSampler \
  -cp backend/crawler/target/spring-crawler-0.0.1-SNAPSHOT.jar \
  org.springframework.boot.loader.launch.PropertiesLauncher \
  --url 'https://www.zhipin.com/web/geek/jobs?query=Java&city=101010100' \
  --output /absolute/private/new-boss-run \
  --cdp http://127.0.0.1:9222 --limit 5 \
  --cancel-file /absolute/private/stop-boss-sampling
```

CDP 只接受 `http://127.0.0.1:端口` 或 `http://[::1]:端口`（可带尾部 `/`）；不允许远端、userinfo、query 或其他路径。复用首个已有上下文，**不读取 Cookie、不枚举 Profile、不操作现有标签页**；仅创建自己的标签页。结束时关闭自己的标签页并断开客户端，不关闭附着浏览器及其他标签页。

没有有效 CDP 时，可以去掉 `--cdp`：工具启动非持久化的临时可见 Chromium，在 BOSS 首页等待人工扫码/登录，完成后需在终端按 Enter，最多等待 120 秒。临时上下文不读取个人浏览器资料，不调用 storageState/save Cookie；退出时关闭自己创建的浏览器，不保存新认证。需要本机已有 Playwright 1.39 对应 Chromium；工具不安装浏览器或启动新服务。

可用 `--cookies /absolute/private/boss-cookies.json` 替代 `--cdp`，将用户显式提供的 BOSS Cookie 注入临时上下文，不读取其他位置。文件应是 1–100 个对象的 JSON 数组，每项 name/value/domain/path；domain 只能为 `www.zhipin.com`、`zhipin.com` 或 `.zhipin.com`，path 必须 `/`，可含 httpOnly。工具强制 secure，不保存回文件。只接受普通、非符号链接、上限 128 KiB 的私有文件，组和其他用户不得有任何权限（例如 0600）。整份文件出现外平台域名即拒绝。不要把真实 Cookie 写入仓库、终端命令或报告。**有 Cookie 不代表登录有效**；不会按 Cookie 名称判定登录。CDP 与 Cookie 参数不能同时使用。

`--url` 只接受 `https://www.zhipin.com/web/geek/job` 或 `/web/geek/jobs`，搜索参数仅 `query`（1–100 字符）与 `city`（3–12 位数字），参数不可重复；也接受 `/job_detail/字母数字下划线短横线.html` 的明确详情 URL（无 query）。禁止 userinfo、端口、fragment、HTTP、外域和任意签名参数。卡片链接允许包含站内临时 query，但只取严格校验后的详情路径作为身份，既不记录也不重放签名参数。正常页面资源仍由浏览器读取，不拼造或重放私有接口。

输出目录必须不存在，建议位于仓库外。创建目录 0700、`samples.jsonl` 与 `labels.jsonl` 0600，需要 POSIX 文件权限支持；不追加或覆盖既有输出。异常时可能留下部分新文件，保留检查，下一次换新目录。

## 浏览器读取与停止边界

监听器在导航/点击前安装。列表重新定位 `li.job-card-box` / `li.job-card-wrapper`，从详情链接确认 encryptId，点击唯一 `.job-card-body` 阅读区域，不查找沟通或投递按钮。布局不支持这些结构则明确停止，不扩大点击范围。首张默认选中且有第二张卡片时先读取第二张再回第一张；首张仍无响应时只允许一次普通详情导航回退。每次动作重置请求轮次，旧动作发起的请求不参与新卡片匹配。

只接受精确 HTTPS host `www.zhipin.com`、路径 `/wapi/zpgeek/job/detail.json`、GET、HTTP 200、`application/json`，并拒绝来自其他来源/路径/方法的重定向链；响应 `code` 必须为整数 0，`zpData.jobInfo.encryptId` 必须等于目标卡片/详情身份。只读取已完成请求的正文，防止未完成响应阻塞等待循环。错误身份的正文不会输出，即使标题看起来相同。后台旧响应不作为“首条成功”。

明确详情导航（直接详情 URL 或首卡的一次导航回退）等待 JSON 后，仅在该轮没有详情 JSON 请求且没有 JSON 错误时，允许从 `page.content()` 读取 SSR。读取前后检查停止状态，并要求真实 currentURL 未变化、与目标严格同源/同详情路径/同 jobId；外域、异 ID、登录/验证状态、空标题或空 JD 均不成功。只读取唯一 `h1` 与职位区内唯一 `.job-detail-section .job-sec-text`（公司介绍也使用 `.job-sec-text`，不作为 JD），使用 Jsoup `wholeText()` 保留 Unicode、文本节点换行和 `<br>` 换行，来源标为 `boss_browser_detail_ssr`。卡片摘要和 job-tags 不参与正文/福利填充；SSR 的公司、城市、薪资、经验、学历与福利均为 null。JSON 主路径来源仍为 `boss_browser_detail_response`，错误或未完成 JSON 不允许用 SSR 掩盖。

默认限 5 个不同岗位，`--limit` 可为 1–20，尝试失败也计入额度。最多滚动 5 次，连续两轮无新身份停止；每轮至多检查 100 张卡片。点击超时 3 秒，导航 15 秒，详情响应等待 8 秒，收集总时限 120 秒（手工登录另计 120 秒）。通常岗位间隔 1 秒，首次切换等待 500ms；没有无限重试或持续任务。

创建 `--cancel-file` 指定文件、关闭工具标签页/窗口、线程中断，或遇到明确登录/验证/401/403/429，均停止。启动前已有取消文件或线程中断时，在创建 Playwright、连接/创建浏览器之前写出 cancelled；取消在等待循环和每次导航/点击/滚动之前检查，首卡切换等待结束后先检查停止状态再切回目标，单次导航最长可能等待 15 秒。恢复需手工解决前提后另开新输出目录，不自动绕过或反复登录。检测未覆盖所有平台验证页：未识别的验证/登录壳会以 timeout/unsupported_layout 等失败，不能据此推断已登录。

典型状态：`ok`、`unsupported_layout`、`identity_mismatch`、`missing_detail`、`invalid_detail_json`、`api_error`、`invalid_detail_response`、`redirect_rejected`、`detail_response_timeout`、`detail_read_failed`、`detail_too_large`、`auth_required`、`verification_required`、`cancelled`、`window_closed`、`time_limit`、`browser_unavailable`。非零 API code 保守返回 api_error，不猜测未确认 code 的登录含义。控制台只打印固定状态和数量，不输出异常对象、响应正文、请求 query、Cookie、响应头或 recruiter 信息。

列表结尾有 `recordType=run_status` 行，说明 limit_reached/no_new_cards/scroll_limit/unsupported_layout；它不生成标签。退出 0 表示本次尝试至少得到一个且没有失败的详情；提前耗尽发现额度不保证收齐 limit，也不证明列表已全部遍历。失败或取消退出 2，已有成功样本保留。单详情模式只处理该身份。

## 样本与独立标注

`ok` 只说明符合已实现的字段/身份契约，不是线上验收或有效登录声明。成功行包含 platform/sourceJobId/sourceUrl/fetchedAt、schemaVersion/extractorVersion、稳定 identity、内容 revision、samplingSource，列表模式附 samplingListUrl。仅白名单读取 `zpData.jobInfo` 的 encryptId/jobName/salaryDesc/locationName/experienceName/degreeName/postDescription，以及 `zpData.brandComInfo.brandName`。不保留 bossInfo、encryptUserId、招聘者姓名、聊天 ID、token 或完整原始 JSON。

facts 保留 title/company/city/salary/experience/education/rawJd/rawBenefits，缺失为 null；rawJd 保持原始 Unicode/换行，已确认字段不足时不猜测。福利字段尚未可靠确认，因此 rawBenefits=null，技能标签不当作福利。detailData 提供既有 `basicInfo`、`jobDescription.fullDescription`、`jobTags` 形状；不推断 requirements/responsibilities/keywords 或福利标签，保持 null，不接业务写入。

同 identity+revision 在单次导出内去重；时间变化不制造新 revision，内容变化产生新 revision。所有样本保留 blind_holdout，不自动划分训练集；正式评测前仍需独立公司/职位映射并统一所有修订。

labels.jsonl 只对应成功样本，status=pending，requirements/benefits/evidenceSpans/anonymizedResume/jobGroup/ranking/relevance/annotator/annotationVersion/adjudication 均为 null。人工标注需定义原文字段及跨度坐标；pending 不是负例，不把 Jev 结果填作 gold。没有独立简历/排序标签，不能宣称匹配质量通过。

## 参考与验收限制

设计参考固定为 get_jobs commit `36bf5f986fad93374d77b99205c7684f8450874a`，PolyForm Noncommercial 1.0.0：只阅读行为、数据形态和会话边界，独立实现，没有复制源码或引入其运行时依赖。

- [Boss.java 340–387：首卡与详情响应](https://github.com/loks666/get_jobs/blob/36bf5f986fad93374d77b99205c7684f8450874a/src/main/java/com/getjobs/worker/boss/Boss.java#L340-L387)
- [Boss.java 1188–1229：详情监听与身份字段](https://github.com/loks666/get_jobs/blob/36bf5f986fad93374d77b99205c7684f8450874a/src/main/java/com/getjobs/worker/boss/Boss.java#L1188-L1229)
- [PlaywrightManager.java：参考会话管理](https://github.com/loks666/get_jobs/blob/36bf5f986fad93374d77b99205c7684f8450874a/src/main/java/com/getjobs/worker/manager/PlaywrightManager.java)

本实现未启动/连接真实浏览器、读取本机用户资料、真实 Cookie 或私有首条样本，也未真实采集。用户已授权使用现有登录态，但父任务当前可连接浏览器跳登录页；缺有效 CDP/Cookie 入口仍是运行阻塞，不能把授权或合成 fixture 成功等同有效登录。本轮 Spec 修复仅使用内存 mock 和合成 SSR/JSON 验证；父任务此前公开页观察不算本轮在线证明，父任务后续遇安全检查已停止，本轮没有重访。卡片结构、公司字段、SSR 和真实详情响应需父任务另行验收；页面漂移可能明确 unsupported_layout。其他招聘平台不属于当前版本。
