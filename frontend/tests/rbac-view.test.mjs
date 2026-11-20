import assert from 'node:assert/strict'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'
import { createServer } from 'vite'
import { createSSRApp } from 'vue'
import { renderToString } from 'vue/server-renderer'

test('role authorization renders the checked state of a nested action', async (t) => {
  const vite = await createServer({
    root: fileURLToPath(new URL('../', import.meta.url)),
    appType: 'custom',
    server: { middlewareMode: true, ws: false, watch: null },
  })
  t.after(() => vite.close())
  const { default: PermissionBranch } = await vite.ssrLoadModule('/src/components/roles/PermissionBranch.vue')
  const { installComponents, ui } = await vite.ssrLoadModule('/src/ui-components.ts')
  const { permissionTree } = await vite.ssrLoadModule('/src/lib/roles.ts')
  const [node] = permissionTree([{
    id: 21,
    permission: 'reports.view',
    meta: { metaTitle: '报表', type: 'C' },
    children: [{
      id: 22,
      permission: 'reports.export',
      meta: { metaTitle: '导出报表', type: 'F' },
    }],
  }])
  const app = createSSRApp(PermissionBranch, { node, selected: [21, 22], collapsed: [] })
  installComponents(app, ui)
  const html = await renderToString(app)

  const action = html.match(/<[^>]*\baria-label="授权导出报表"[^>]*>/)?.[0] ?? ''
  assert.match(action, /\brole="checkbox"/)
  assert.match(action, /\baria-checked="true"/)
})
