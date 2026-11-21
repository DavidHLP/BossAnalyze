import assert from 'node:assert/strict'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'
import { createServer } from 'vite'
import { createSSRApp } from 'vue'
import { renderToString } from 'vue/server-renderer'

test('historical roles are disabled for assignment and remain available as a list filter', async (t) => {
  const vite = await createServer({
    root: fileURLToPath(new URL('../', import.meta.url)),
    appType: 'custom',
    server: { middlewareMode: true, ws: false, watch: null },
  })
  t.after(() => vite.close())
  const { default: RolePicker } = await vite.ssrLoadModule('/src/components/users/RolePicker.vue')
  const { installComponents, ui } = await vite.ssrLoadModule('/src/ui-components.ts')
  const roles = [{ id: 2, roleName: '当前角色', status: 1 }, { id: 6, roleName: '历史角色', status: 0 }]

  async function render(props) {
    const app = createSSRApp(RolePicker, { modelValue: 6, roles, label: '角色', ...props })
    installComponents(app, ui)
    return await renderToString(app)
  }

  const assignment = await render({ assignment: true })
  const filter = await render({ allowAll: true })
  const selectedRadio = html => html.match(/<button\b[^>]*aria-checked="true"[^>]*>/)?.[0] ?? ''
  const disabled = /\sdisabled(?:="")?(?=\s|>)/
  assert.match(assignment, /历史角色/)
  assert.match(assignment, /停用，无法指派/)
  assert.match(selectedRadio(assignment), disabled)
  assert.match(selectedRadio(filter), /\brole="radio"/)
  assert.doesNotMatch(selectedRadio(filter), disabled)
})
