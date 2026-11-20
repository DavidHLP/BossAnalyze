# BossAnalyze 前端

Vue 3、Vue Router、Vite 和 shadcn-vue。界面布局与业务状态依据 [Penpot 的 BossAnalyze 画板](https://design.penpot.app/?screen=workspace&team-id=371c6737-0d30-8051-8008-f66b52632bfa&project-id=371c6737-0d30-8051-8008-f66b5263d11f&file-id=42288968-6e5e-808d-8008-f86469e0027c&page-id=28f66bd8-6036-80ec-8008-f86a5c4db14c)，样式采用 `a6NRqzdI` preset。

## 本地运行

在 `frontend/` 中使用 Node.js 22.12+ 和 pnpm 11.24.0：

```sh
pnpm install --frozen-lockfile
pnpm dev
```

打开 `http://127.0.0.1:5020`。登录和授权以现有后端接口为准。后端启动步骤见[本地开发说明](../docs/local-development.md)。

## 验证与生产启动

```sh
pnpm type-check
pnpm test
pnpm build
pnpm start
```

生产入口读取 `dist/`。`HOST` 和 `PORT` 默认分别为 `127.0.0.1` 和 `5020`。
开发和生产都在原路径代理 `/api/**`。AI v1/v2 走 8081，其余走 8080。
`BOSS_TRUSTED_PROXY_IPS` 可配置可信代理的准确 IP 列表。只有匹配的直接代理可提供单个合法的转发地址。
外置反向代理部署需要为前端页面启用 HTML 路由回退，并将 `/api/**` 交给保留来源 IP 清理规则的 API 代理。
