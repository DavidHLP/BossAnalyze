import assert from 'node:assert/strict'
import { test } from 'node:test'
import { readFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import { createServer } from 'vite'
import { createRenderer, createSSRApp, defineComponent, h, nextTick, reactive } from 'vue'
import { compileScript, parse } from 'vue/compiler-sfc'
import { renderToString } from 'vue/server-renderer'
import { matchedRouteKey, routeLocationKey, routerKey } from 'vue-router'

test('role authorization renders the checked state of a nested action', async (t) => {
  const vite = await viewServer(t)
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

function renderView(component, props) {
  const node = (tag, text = '') => ({ tag, text, props: {}, style: {}, children: [], parent: null, focus() {} })
  const root = node('root')
  const detach = (child) => {
    if (!child.parent) return
    child.parent.children.splice(child.parent.children.indexOf(child), 1)
    child.parent = null
  }
  const renderer = createRenderer({
    createElement: (tag) => node(tag),
    createText: (text) => node('#text', text),
    createComment: () => node('#comment'),
    setText: (child, text) => { child.text = text },
    setElementText: (child, text) => { child.text = text; child.children = [] },
    parentNode: (child) => child.parent,
    nextSibling: (child) => child.parent?.children[child.parent.children.indexOf(child) + 1],
    insert(child, parent, anchor) {
      detach(child)
      const index = anchor ? parent.children.indexOf(anchor) : parent.children.length
      parent.children.splice(index, 0, child)
      child.parent = parent
    },
    remove: detach,
    patchProp: (child, key, _old, value) => { child.props[key] = value },
  })
  const app = renderer.createApp({ render: () => h(component, props) })
  app.provide(routerKey, { replace() {} })
  app.provide(routeLocationKey, { query: {} })
  app.provide(matchedRouteKey, { value: { leaveGuards: new Set() } })
  // Keep the management form live and use plain elements in place of the overlay primitives.
  for (const name of [
    'UiSheet', 'UiSheetContent', 'UiSheetHeader', 'UiSheetTitle', 'UiSheetDescription', 'UiSheetFooter',
    'UiAlert', 'UiAlertDescription', 'UiAlertTitle', 'UiButton', 'UiCheckbox', 'UiField', 'UiFieldError',
    'UiFieldLabel', 'UiInput', 'UiRadioGroup', 'UiRadioGroupItem', 'UiTextarea', 'StatusBadge',
    'UiAvatar', 'UiAvatarFallback', 'UiCombobox', 'UiComboboxAnchor', 'UiComboboxEmpty', 'UiComboboxInput',
    'UiComboboxItem', 'UiComboboxList', 'UiComboboxViewport',
    'UiDialog', 'UiDialogContent', 'UiDialogDescription', 'UiDialogFooter', 'UiDialogHeader', 'UiDialogTitle',
    'UiPagination', 'UiPaginationContent', 'UiPaginationEllipsis', 'UiPaginationItem',
    'UiPaginationNext', 'UiPaginationPrevious', 'RouterLink',
    'UiSkeleton', 'UiTable', 'UiTableBody', 'UiTableCaption', 'UiTableCell',
    'UiTableHead', 'UiTableHeader', 'UiTableRow', 'UiSeparator',
  ]) {
    app.component(name, defineComponent({
      inheritAttrs: false,
      setup: (_, { attrs, slots }) => () => {
        if ((name === 'UiSheet' || name === 'UiDialog') && !attrs.open) return null
        const tag = name === 'UiInput' ? 'input' : name === 'UiButton' ? 'button' : 'div'
        return h(tag, attrs, slots.default?.({ items: [] }))
      },
    }))
  }
  app.mount(root)
  function find(predicate, parent = root) {
    if (predicate(parent)) return parent
    for (const child of parent.children) {
      const result = find(predicate, child)
      if (result) return result
    }
  }
  return { app, find }
}

async function viewServer(t) {
  const vite = await createServer({
    root: fileURLToPath(new URL('../', import.meta.url)),
    appType: 'custom',
    server: { middlewareMode: true, ws: false, watch: null },
    optimizeDeps: { noDiscovery: true },
    plugins: [{
      name: 'rbac-view-test',
      enforce: 'pre',
      resolveId(id, importer) {
        if (importer?.includes('.view-test.ts')) {
          const component = id.match(/\/components\/(roles\/RolePanel|users\/(?:UserEditor|DeleteUserDialog|RolePicker))\.vue$/)
          if (component) id = `/src/components/${component[1]}.vue.view-test.ts`
          else if (id === './RolePicker.vue') id = '/src/components/users/RolePicker.vue.view-test.ts'
        }
        if (id.startsWith('/src/') && id.endsWith('.vue.view-test.ts'))
          return fileURLToPath(new URL(`..${id}`, import.meta.url))
      },
      async load(id) {
        if (!id.endsWith('.vue.view-test.ts')) return
        const filename = id.replace(/\.view-test\.ts$/, '')
        const { descriptor } = parse(await readFile(filename, 'utf8'), { filename })
        return compileScript(descriptor, { id, inlineTemplate: true }).content
      },
    }],
  })
  t.after(() => vite.close())
  return vite
}

function mockBrowser(t) {
  const previousWindow = globalThis.window
  const storage = Object.getOwnPropertyDescriptor(globalThis, 'localStorage')
  const document = Object.getOwnPropertyDescriptor(globalThis, 'document')
  globalThis.window = new EventTarget()
  const values = { token: 'test-session', token_timestamp: String(Date.now()) }
  Object.defineProperty(globalThis, 'localStorage', {
    configurable: true, value: { getItem: (key) => values[key] ?? null },
  })
  Object.defineProperty(globalThis, 'document', {
    configurable: true, value: { querySelector: () => null, getElementById: () => null },
  })
  t.after(() => {
    globalThis.window = previousWindow
    if (storage) Object.defineProperty(globalThis, 'localStorage', storage)
    else delete globalThis.localStorage
    if (document) Object.defineProperty(globalThis, 'document', document)
    else delete globalThis.document
  })
}

test('Enter in the permission filter is consumed while IME composition can finish', async (t) => {
  const vite = await viewServer(t)
  const { default: RolePanel } = await vite.ssrLoadModule('/src/components/roles/RolePanel.vue.view-test.ts')
  mockBrowser(t)
  const props = reactive({
    open: false, mode: 'grant', role: null, tree: [],
    treeUnavailable: false, treePending: false, treeRestricted: false, actorRoleId: null,
    locked: false, awaitingRead: false, writeUnconfirmed: false, listPending: false, listFailed: false,
  })
  const { app, find } = renderView(RolePanel, props)
  try {
    props.open = true
    await nextTick()
    const search = find((element) => element.props.id === 'permission-search')
    assert.ok(search)
    const enter = new Event('keydown', { cancelable: true })
    Object.assign(enter, { key: 'Enter', isComposing: false, keyCode: 13 })
    search.props.onKeydown?.(enter)
    assert.equal(enter.defaultPrevented, true, 'search Enter must cancel the native form submit')
    for (const composition of [{ isComposing: true }, { keyCode: 229 }]) {
      const event = new Event('keydown', { cancelable: true })
      Object.assign(event, { key: 'Enter' }, composition)
      search.props.onKeydown(event)
      assert.equal(event.defaultPrevented, false)
    }
  } finally {
    app.unmount()
  }
})

function elementText(element) {
  return element.text + element.children.map(elementText).join('')
}

async function flushView() {
  await new Promise((resolve) => setImmediate(resolve))
  await nextTick()
}

function viewControls(find) {
  return {
    byId: (id) => find((element) => element.props.id === id),
    byLabel: (label) => find((element) => element.props['aria-label'] === label),
    button: (label, parent) => find((element) => element.tag === 'button' && elementText(element).trim() === label, parent),
    click: (element) => element.props.onClick({ currentTarget: element }),
  }
}

test('uncertain role saves and deletes require a successful list refresh before retry', async (t) => {
  const vite = await viewServer(t)
  const { default: Roles } = await vite.ssrLoadModule('/src/pages/console/roles.vue.view-test.ts')
  const { default: request } = await vite.ssrLoadModule('/src/utils/request/request.ts')
  mockBrowser(t)
  const fixture = JSON.parse(await readFile(new URL('./fixtures/roles.json', import.meta.url)))
  let readFails = false
  const writes = []
  request.defaults.adapter = async (config) => {
    let data
    switch (config.url) {
      case '/api/auth/getRoleList':
        if (readFails) throw { config, response: { status: 503, data: { code: 503 } } }
        data = fixture.roles
        break
      case '/api/auth/getRouters': data = fixture.routers; break
      case '/api/auth/getUserRole': data = { id: fixture.actorId }; break
      case '/api/auth/editRole':
      case '/api/auth/deleteRole':
        writes.push(config.url)
        throw { config, code: 'ECONNABORTED' }
      default: assert.fail(config.url)
    }
    return { config, status: 200, data: { code: 200, data } }
  }
  const { app, find } = renderView(Roles, {})
  const { byId, byLabel, button, click } = viewControls(find)
  const submit = () => byId('role-panel-form').props.onSubmit(new Event('submit', { cancelable: true }))
  const target = fixture.roles[1].roleName
  try {
    await flushView()
    click(byLabel('编辑USER'))
    await nextTick()
    const status = byId('role-status')
    assert.equal(find((element) => element.props.value === '0', status).props.disabled, true)
    assert.ok(byId('default-role-status-note'))
    click(button('取消'))
    await nextTick()

    click(byLabel(`编辑${target}`))
    await nextTick()
    byId('role-remark').props['onUpdate:modelValue']('更新测试角色备注')
    await submit()
    await nextTick()
    assert.match(elementText(byId('role-panel-form').parent), /请求可能已提交，请先刷新列表核对实际状态/)
    await submit()
    assert.deepEqual(writes, ['/api/auth/editRole'])

    readFails = true
    await click(button('刷新列表核对', byId('role-panel-form').parent))
    await flushView()
    assert.match(elementText(byId('role-panel-form').parent), /列表读取失败，请重试/)
    assert.equal(byId('role-remark').props.modelValue, '更新测试角色备注')
    await submit()
    assert.deepEqual(writes, ['/api/auth/editRole'])

    readFails = false
    await click(button('刷新列表核对', byId('role-panel-form').parent))
    await flushView()
    assert.equal(byId('role-panel-form'), undefined)
    click(byLabel(`删除${target}`))
    await nextTick()
    await click(button('删除角色'))
    await flushView()
    const remove = button('删除角色')
    assert.equal(remove.props['aria-disabled'], true)
    await click(remove)
    assert.deepEqual(writes, ['/api/auth/editRole', '/api/auth/deleteRole'])
    await click(button('刷新列表核对', remove.parent.parent))
    await flushView()
    assert.equal(button('删除角色'), undefined)
    assert.equal(byLabel(`删除${target}`).props['aria-disabled'], false)
  } finally {
    app.unmount()
  }
})

test('uncertain user writes stay locked across reopening, while code 1006 permits password retry', async (t) => {
  const vite = await viewServer(t)
  const { default: Users } = await vite.ssrLoadModule('/src/pages/console/users.vue.view-test.ts')
  const { default: request } = await vite.ssrLoadModule('/src/utils/request/request.ts')
  mockBrowser(t)
  const fixture = JSON.parse(await readFile(new URL('./fixtures/users.json', import.meta.url)))
  const role = { id: 901, roleName: '测试管理员', status: 1 }
  const writes = []
  let wrongPassword = true
  request.defaults.adapter = async (config) => {
    if (config.url === '/api/user/getUserManageInfo')
      return { config, status: 200, data: { data: fixture } }
    if (config.url === '/api/role/getRoleList?roleName=')
      return { config, status: 200, data: { data: [role] } }
    assert.match(config.url, /^\/api\/user\/(addUser|updateUser|deleteUser)$/)
    writes.push(config.url)
    if (config.url.endsWith('/deleteUser') && wrongPassword)
      throw { config, response: { status: 400, data: { code: 1006 } } }
    return { config, status: 204 }
  }
  const { app, find } = renderView(Users, {})
  const { byId, byLabel, button, click } = viewControls(find)
  const fill = (id, value) => byId(id).props['onUpdate:modelValue'](value)
  const submit = (id) => byId(id).props.onSubmit(new Event('submit', { cancelable: true }))
  const target = fixture.content[0].name
  const editor = 'user-editor-form'
  const confirmation = 'delete-user-form'
  try {
    await flushView()
    click(button('新增用户'))
    await nextTick()
    fill('user-name', '测试创建')
    fill('user-email', 'fixture-new@example.test')
    fill('user-password', 'fixture-password')
    fill('user-role', String(role.id))
    await submit(editor)
    await nextTick()
    await submit(editor)
    assert.deepEqual(writes, ['/api/user/addUser'])
    click(button('取消'))
    await nextTick()
    click(button('放弃修改'))
    await nextTick()
    assert.equal(button('新增用户').props.disabled, true)
    await click(button('刷新列表核对'))
    await flushView()

    click(byLabel(`编辑${target}`))
    await nextTick()
    fill('user-name', '测试编辑')
    await submit(editor)
    await nextTick()
    await submit(editor)
    assert.deepEqual(writes, ['/api/user/addUser', '/api/user/updateUser'])
    await click(button('刷新列表核对'))
    await flushView()

    click(byLabel(`删除${target}`))
    await nextTick()
    fill('delete-user-password', 'fixture-operator-password')
    await submit(confirmation)
    assert.match(elementText(byId(confirmation)), /操作者密码不正确，请重试/)
    wrongPassword = false
    await submit(confirmation)
    await nextTick()
    fill('delete-user-password', 'fixture-operator-password')
    await submit(confirmation)
    assert.deepEqual(writes, ['/api/user/addUser', '/api/user/updateUser', '/api/user/deleteUser', '/api/user/deleteUser'])
    await click(button('刷新列表核对'))
    await flushView()
    assert.equal(byId(confirmation), undefined)
    assert.equal(byLabel(`删除${target}`).props['aria-disabled'], false)
  } finally {
    app.unmount()
  }
})
