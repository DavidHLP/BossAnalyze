# Jev 官方 ZDR 路由单次可用性核验

日期：2026-11-08 America/Los_Angeles，2026-11-09 06:26:23–06:26:26 UTC。
被测源码 HEAD：`8e5eaf4f4d416cda49ebc2401f21853640fcfd60`。

结论：**当前账户与固定 `typesafe/jev-1.14.0` 的官方 ZDR 路由不可用。** 唯一一次 provider 请求返回 HTTP **422 / cmd_zdr_no_providers**；生产客户端映射为 **503 / zdr_unavailable**。没有成功回答，Noul 成功响应结构无法验证，input/output token usage 均未知。此结果不证明物理日志保留行为，也不代表领域质量或正式部署验收。

## 方法与边界

先阅读根 AGENTS、四份共享规则、当前 Settings/Jev/Budget、既有 Noul 测试协议、三份 Jev acceptance/postfix/identity 报告及 `/tmp/boss-final-ai-quality-privacy-handoff.md`。Hindsight 未提供本次路由实测依据。代码图索引和 exact-path coverage 为 best-effort：config.py、jev.py、test_contract.py、pyproject.toml 为 metadata_match/no_recorded_issue；docs 被排除，直接读取全文。

只在内存读取已授权的 0600 identity-env.json，使用原官方 key、内部 token、固定模型、原 AI_BUDGET_PATH 和 230000 cap；未打印或复制凭据、env 或原始 headers，未改私有 profile。直接调用当前 `app.jev.Jev`（本仓库实际客户端类名）及 `Settings`，没有接主 API、启动服务或调度。

请求沿用既有 tests/test_contract.py 的单个 Noul `type/instructions` 协议，仅极短人工合成颜色事实，无 JD、简历或个人资料。固定 HTTPS `https://api.commandcode.ai/provider/v1/systemone`，ZDR 开启（对应 CMD_ZDR=1），retries=0，quality_approved=false。生产 httpx 客户端保持默认 TLS 验证、trust_env=False、follow_redirects=False；事件 hook 在内存断言唯一请求、精确 URL、POST 和 `x-cmd-zdr: 1`，仅记录白名单状态/错误码。未切 host、模型、代理或去除 ZDR，未重试。

## 预算与结果

UTC 账本日：2026-11-09。先用 SQLite mode=ro 读取当日 used/cap；仅额度足够才执行。通过原生产 Budget.reserve 记录本次预留，未手工编辑、重置账本或涨 cap。

- 请求前：213772 / 230000，剩余 16228。
- 请求计划保守预留：8349（UTF-8 payload 字节数 + 8192），低于 12000 且低于剩余额度。
- provider 尝试：1；HTTP 422；错误分类 cmd_zdr_no_providers。
- 客户端：503 / zdr_unavailable；成功响应结构未验证；usage input/output 均 null。
- 请求后：222121 / 230000，剩余 7879；账本增量 8349。预留不是实际付费 token usage。

## 精确执行与证据

复用既有 `/home/david/.t3/worktrees/BossAnalyze/t3-f0baebb8/ai-service/.venv`，未安装依赖或修改源码父环境。执行前 `cmp -s` 确认其 uv.lock 与目标一致；Python 3.13.15、httpx 0.28.1。PYTHONPATH 指向被测 checkout 当前 ai-service。

实际执行命令：

```sh
cmp -s ai-service/uv.lock /home/david/.t3/worktrees/BossAnalyze/t3-f0baebb8/ai-service/uv.lock
/home/david/.t3/worktrees/BossAnalyze/t3-f0baebb8/ai-service/.venv/bin/python -c 'import sys,httpx; print(sys.version.split()[0], httpx.__version__)'
chmod 600 /tmp/boss-final-zdr-runtime-evidence/probe.py
PYTHONDONTWRITEBYTECODE=1 PYTHONPATH="$PWD/ai-service" /home/david/.t3/worktrees/BossAnalyze/t3-f0baebb8/ai-service/.venv/bin/python /tmp/boss-final-zdr-runtime-evidence/probe.py
```

执行目录：`/home/david/.t3/worktrees/BossAnalyze/t3-694a994c`。
证据：`/tmp/boss-final-zdr-runtime-evidence/`（0700），probe.py、attempt-started、before.json、result.json（0600）；脚本无凭据，结果无请求/回答正文或原始 headers。attempt-started 使用排他创建阻止意外重放；禁止删除标记重跑付费探测。初始独立只读预算检查同样返回 213772/230000。

清理：直接 Python 进程 exit0；finally 已 await client.close()，result.json 记录 http_client_closed=true。没有长驻服务、端口或子进程需要停止；原服务和其他开发者工作均未操作。仅保留脱敏证据与本报告，原 profile/账本/旧证据保留。

未运行浏览器、真实样本、主 API 身份链、领域质量、全套测试、CI、生产性能、部署/回退、正式调度或写回；未 push/PR/watch，不另委派。隐私运行门槛仍未通过，不批准真实个人数据调用或 AI 质量启用。
