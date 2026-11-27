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

function renderView(component, props, location = {}) {
  const node = (tag, text = '') => ({
    tag, text, props: {}, style: {}, children: [], parent: null, focus() {},
    addEventListener() {}, removeEventListener() {}, options: [], value: '',
  })
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
  const currentRoute = { value: { path: '/console', fullPath: '/console' } }
  const router = {
    currentRoute,
    async replace(path) {
      if (typeof path === 'string') currentRoute.value = { path, fullPath: path }
    },
    async push(path) {
      if (typeof path === 'string') currentRoute.value = { path, fullPath: path }
    },
  }
  app.provide(routerKey, router)
  app.provide(routeLocationKey, { query: {}, ...location })
  app.provide(matchedRouteKey, { value: { leaveGuards: new Set(), updateGuards: new Set() } })
  // Keep the management form live and use plain elements in place of the overlay primitives.
  for (const name of [
    'UiSheet', 'UiSheetContent', 'UiSheetHeader', 'UiSheetTitle', 'UiSheetDescription', 'UiSheetFooter',
    'UiAlert', 'UiAlertDescription', 'UiAlertTitle', 'UiBadge', 'UiButton', 'UiCheckbox', 'UiField', 'UiFieldError',
    'UiFieldLabel', 'UiInput', 'UiRadioGroup', 'UiRadioGroupItem', 'UiTextarea', 'StatusBadge',
    'UiAvatar', 'UiAvatarFallback', 'UiCombobox', 'UiComboboxAnchor', 'UiComboboxEmpty', 'UiComboboxInput',
    'UiComboboxItem', 'UiComboboxList', 'UiComboboxViewport',
    'UiDialog', 'UiDialogContent', 'UiDialogDescription', 'UiDialogFooter', 'UiDialogHeader', 'UiDialogTitle',
    'UiDropdownMenu', 'UiDropdownMenuTrigger', 'UiDropdownMenuContent', 'UiDropdownMenuItem', 'UiDropdownMenuSeparator',
    'UiPagination', 'UiPaginationContent', 'UiPaginationEllipsis', 'UiPaginationItem',
    'UiPaginationNext', 'UiPaginationPrevious', 'RouterLink',
    'UiSkeleton', 'UiTable', 'UiTableBody', 'UiTableCaption', 'UiTableCell',
    'UiTableHead', 'UiTableHeader', 'UiTableRow', 'UiSeparator', 'UiCollapsible',
    'UiCollapsibleContent', 'UiCollapsibleTrigger',
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
  return { app, find, router }
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
          const component = id.match(/\/components\/(menus\/MenuPanel|roles\/RolePanel|users\/(?:UserEditor|DeleteUserDialog|RolePicker)|resumes\/Resume\w+)\.vue$/)
          if (component) id = `/src/components/${component[1]}.vue.view-test.ts`
          else if (id === './RolePicker.vue') id = '/src/components/users/RolePicker.vue.view-test.ts'
          else if (id === './PermissionBranch.vue') id = '/src/components/roles/PermissionBranch.vue.view-test.ts'
          else if (id === './ResumePreviewInline.vue') id = '/src/components/resumes/ResumePreviewInline.vue.view-test.ts'
          else if (id === '/src/pages/resumes/index.vue') id = '/src/pages/resumes/index.vue.view-test.ts'
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
  function storage(values = {}) {
    const entries = new Map(Object.entries(values))
    return {
      getItem: (key) => entries.get(key) ?? null,
      setItem: (key, value) => entries.set(key, value),
      removeItem: (key) => entries.delete(key),
      key: (index) => [...entries.keys()][index] ?? null,
      get length() { return entries.size },
    }
  }
  const localStorage = storage({ token: 'test-session', token_timestamp: String(Date.now()) })
  const sessionStorage = storage()
  const window = Object.assign(new EventTarget(), { sessionStorage, location: { reload: t.mock.fn() } })
  const document = { querySelector: () => null, getElementById: () => null }
  for (const [name, value] of Object.entries({ window, localStorage, sessionStorage, document })) {
    const previous = Object.getOwnPropertyDescriptor(globalThis, name)
    Object.defineProperty(globalThis, name, { configurable: true, value })
    t.after(() => {
      if (previous) Object.defineProperty(globalThis, name, previous)
      else delete globalThis[name]
    })
  }
  return { window, localStorage }
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

test('pasted resume text is previewed and edited before a single create request', async (t) => {
  const vite = await viewServer(t)
  const { default: Resumes } = await vite.ssrLoadModule('/src/pages/resumes/index.vue.view-test.ts')
  const { default: request } = await vite.ssrLoadModule('/src/utils/request/request.ts')
  mockBrowser(t)
  const writes = []
  let created = null
  request.defaults.adapter = async (config) => {
    let data
    if (config.url === '/api/resume' && config.method === 'get') {
      data = created ? [created] : []
    } else if (config.url === '/api/resume' && config.method === 'post') {
      const body = JSON.parse(config.data)
      writes.push(body)
      created = { id: 'created', userId: 7, title: body.title, content: body.content }
      return { config, status: 200, data: { code: 200, data: created } }
    } else if (config.url === '/api/resume/created/version/recent') {
      data = [{ id: 'initial', content: created.content }]
    } else {
      assert.fail(config.method + ' ' + config.url)
    }
    return { config, status: 200, data: { code: 200, data } }
  }
  const view = renderView(Resumes, {})
  t.after(() => view.app.unmount())
  await flushView()
  const { byId, button, click } = viewControls(view.find)
  click(button('新建简历'))
  await nextTick()
  byId('resume-title').props['onUpdate:modelValue']('张三')
  const group = view.find((element) => element.props['aria-labelledby'] === 'resume-entry-label')
  group.props['onUpdate:modelValue']('paste')
  await nextTick()
  byId('resume-import-source').props['onUpdate:modelValue']('张三\n\n教育经历\n• 某某大学')
  await nextTick()
  assert.equal(byId('resume-import-preview').props.modelValue, '# 张三\n\n## 教育经历\n- 某某大学')
  assert.equal(writes.length, 0, 'typing and previewing must not create a resume')

  const edited = '# 张三\n\n## 教育经历\n- 某某大学（已修改）'
  byId('resume-import-preview').props['onUpdate:modelValue'](edited)
  await nextTick()
  assert.equal(writes.length, 0, 'editing the preview must not create a resume')

  const contains = (element, id) => element.props?.id === id ||
    element.children.some((child) => contains(child, id))
  const form = view.find((element) => element.tag === 'form' && contains(element, 'resume-title'))
  await form.props.onSubmit(new Event('submit', { cancelable: true }))
  await flushView()
  await flushView()

  assert.deepEqual(writes, [{ title: '张三', content: edited }])
  assert.equal(view.router.currentRoute.value.path, '/resumes/created/edit')
})

test('confirmed menu changes and manual readback refresh navigation without losing credentials on failure', async (t) => {
  const vite = await viewServer(t)
  const { default: Menus } = await vite.ssrLoadModule('/src/pages/console/menus.vue.view-test.ts')
  const { default: request } = await vite.ssrLoadModule('/src/utils/request/request.ts')
  const { useSession } = await vite.ssrLoadModule('/src/composables/useSession.ts')
  const { findMenu } = await vite.ssrLoadModule('/src/lib/session.ts')
  const { authorizeRoute } = await vite.ssrLoadModule('/src/router/guard.ts')
  let view
  t.after(() => view?.app.unmount())
  const { localStorage } = mockBrowser(t)
  const routes = JSON.parse(await readFile(new URL('./fixtures/menus.json', import.meta.url)))
  const leaf = routes[0].children.find((node) => node.name === 'FixtureLeaf')
  let refreshFails = false
  let deleteAccepted = false
  let deleteConfirmed = false
  let sessionReads = 0
  request.defaults.adapter = async (config) => {
    let data
    switch (config.url) {
      case '/api/auth/getRouters':
        data = structuredClone(routes)
        if (deleteAccepted && deleteConfirmed) data[0].children = data[0].children.filter((node) => node.id !== leaf.id)
        break
      case '/api/auth/getRoleList': data = []; break
      case '/api/auth/getUserBaseInfo': data = { email: 'fixture@example.test' }; sessionReads++; break
      case '/api/auth/getUserPrivateInformation':
        if (refreshFails) throw { config, response: { status: 503, data: { code: 503 } } }
        data = ['fixture.console', 'fixture.leaf']
        break
      case '/api/auth/editRouter':
        Object.assign(leaf, JSON.parse(config.data))
        break
      case '/api/auth/deleteRouter': deleteAccepted = true; break
      default: assert.fail(config.url)
    }
    return { config, status: 200, data: { code: 200, data } }
  }
  view = renderView(Menus, {})
  const { byId, byLabel, button, click } = viewControls(view.find)
  const session = useSession(view.router)
  await session.restore()
  await flushView()
  const oldPath = '/console/leaf'
  const path = '/console/leaf-changed'
  const canNavigate = (path) => authorizeRoute(session, { path, fullPath: path, meta: { access: 'menu' } })
  assert.equal(findMenu(session.state.value.menus, oldPath)?.title, '测试独立菜单')
  assert.equal(await canNavigate(oldPath), true)

  click(byLabel('编辑测试独立菜单'))
  await nextTick()
  byId('menu-title').props['onUpdate:modelValue']('新菜单名称')
  byId('menu-path').props['onUpdate:modelValue']('leaf-changed')
  await byId('menu-panel-form').props.onSubmit(new Event('submit', { cancelable: true }))
  await flushView()
  assert.equal(findMenu(session.state.value.menus, path)?.title, '新菜单名称')
  assert.equal(await canNavigate(path), true)
  assert.equal((await canNavigate(oldPath)).params.code, '403')
  assert.equal(sessionReads, 2)

  await click(byLabel('删除新菜单名称'))
  await flushView()
  await click(button('删除菜单'))
  await flushView()
  assert.equal(sessionReads, 2, 'unconfirmed deletion must not refresh the session')
  leaf.path = 'leaf-after-read'
  const recoveredPath = '/console/leaf-after-read'
  await click(button('重新读取菜单'))
  await flushView()
  assert.equal(sessionReads, 3)
  assert.equal(findMenu(session.state.value.menus, recoveredPath)?.title, '新菜单名称')
  assert.equal(await canNavigate(recoveredPath), true)
  assert.equal((await canNavigate(path)).params.code, '403')

  deleteConfirmed = true
  refreshFails = true
  deleteAccepted = false
  await click(byLabel('删除新菜单名称'))
  await flushView()
  await click(button('删除菜单'))
  await flushView()
  const notice = view.find((element) => element.props.role === 'status' && elementText(element).includes('菜单已删除并回读确认。'))
  assert.match(elementText(notice), /变更已生效，但当前侧栏未能自动刷新，可稍后刷新或重新登录/)
  assert.equal(sessionReads, 4)
  assert.equal(localStorage.getItem('token'), 'test-session')
  assert.equal(findMenu(session.state.value.menus, recoveredPath)?.title, '新菜单名称')
})

test('only confirmed changes to other roles refresh the session, and editing the current role still signs out', async (t) => {
  const vite = await viewServer(t)
  const { default: Roles } = await vite.ssrLoadModule('/src/pages/console/roles.vue.view-test.ts')
  const { default: request } = await vite.ssrLoadModule('/src/utils/request/request.ts')
  const { useSession } = await vite.ssrLoadModule('/src/composables/useSession.ts')
  let view
  t.after(() => view?.app.unmount())
  const { localStorage } = mockBrowser(t)
  const fixture = JSON.parse(await readFile(new URL('./fixtures/roles.json', import.meta.url)))
  const target = fixture.roles[1].roleName
  let permissions = []
  let sessionReads = 0
  let confirmWrite = false
  let refreshFails = false
  request.defaults.adapter = async (config) => {
    let data
    switch (config.url) {
      case '/api/auth/getRoleList': data = structuredClone(fixture.roles); break
      case '/api/auth/getRouters': data = fixture.routers; break
      case '/api/auth/getUserRole': data = { id: fixture.actorId }; break
      case '/api/auth/getUserBaseInfo': data = { email: 'fixture@example.test' }; sessionReads++; break
      case '/api/auth/getUserPrivateInformation':
        if (refreshFails) throw { config, response: { status: 503, data: { code: 503 } } }
        data = permissions
        break
      case '/api/auth/editRole': {
        const edited = JSON.parse(config.data)
        if (confirmWrite) Object.assign(fixture.roles.find((role) => role.id === edited.id), edited)
        break
      }
      case '/api/auth/updateRoleRouters': {
        const { roleId, routerIds } = JSON.parse(config.data)
        const role = fixture.roles.find((item) => item.id === roleId)
        role.permissions.push({ permission: 'roles.view' })
        role.routers = routerIds.map((id) => ({ id }))
        break
      }
      case '/api/auth/deleteRole':
        if (confirmWrite) fixture.roles.splice(1, 1)
        break
      default: assert.fail(config.url)
    }
    return { config, status: 200, data: { code: 200, data } }
  }
  view = renderView(Roles, {})
  const session = useSession(view.router)
  await session.restore()
  await flushView()
  const { byId, byLabel, button, click } = viewControls(view.find)
  const submit = () => byId('role-panel-form').props.onSubmit(new Event('submit', { cancelable: true }))
  const edit = async (name) => {
    click(byLabel(`编辑${name}`))
    await nextTick()
    byId('role-remark').props['onUpdate:modelValue']('更新角色备注')
    await submit()
    await flushView()
  }
  const remove = async () => {
    click(byLabel(`删除${target}`))
    await nextTick()
    await click(button('删除角色'))
    await flushView()
  }

  await edit(target)
  assert.equal(sessionReads, 1)
  assert.ok(byId('role-write-unconfirmed'))
  await click(button('刷新列表核对', byId('role-panel-form').parent))
  await flushView()

  confirmWrite = true
  permissions = ['console.view']
  await edit(target)
  assert.deepEqual(session.state.value.permissions, permissions)
  assert.equal(sessionReads, 2)

  permissions = ['console.view', 'roles.view']
  click(byLabel(`菜单授权 ${target}`))
  await nextTick()
  byLabel('授权角色管理').props['onUpdate:modelValue'](true)
  await submit()
  await flushView()
  assert.deepEqual(session.state.value.permissions, permissions)
  assert.equal(sessionReads, 3)

  confirmWrite = false
  await remove()
  assert.equal(sessionReads, 3)
  assert.equal(button('删除角色').props['aria-disabled'], true)
  await click(button('刷新列表核对', button('删除角色').parent.parent))
  await flushView()

  confirmWrite = true
  refreshFails = true
  await remove()
  assert.equal(byLabel(`删除${target}`), undefined)
  const notice = view.find((element) => element.props.role === 'status' && elementText(element).includes('角色已删除'))
  assert.match(elementText(notice), /变更已生效，但当前侧栏未能自动刷新，可稍后刷新或重新登录/)
  assert.equal(sessionReads, 4)
  assert.equal(localStorage.getItem('token'), 'test-session')

  await edit('当前测试角色')
  assert.equal(view.router.currentRoute.value.path, '/login')
  assert.equal(localStorage.getItem('token'), null)
  assert.equal(sessionReads, 4)
})

for (const editor of [
  {
    name: 'user', file: 'components/users/UserEditor', form: 'user-editor-form', field: 'user-name',
    props: {
      mode: 'create', user: null, roles: [{ id: 901, roleName: '测试角色', status: 1 }],
      listedEmailInUse: async () => false, reloadRoles: async () => [], rolesUnavailable: false,
      rolePending: false, roleRestricted: false, writeUnconfirmed: false, listMissingTarget: false, listPending: false, listFailed: false,
    },
    fields: { 'user-email': 'fixture@example.test', 'user-password': 'fixture-password', 'user-role': '901' },
  },
  {
    name: 'role', file: 'components/roles/RolePanel', form: 'role-panel-form', field: 'role-remark',
    props: {
      mode: 'create', role: null, tree: [], actorRoleId: 999, treeUnavailable: false,
      treePending: false, treeRestricted: false, locked: false, awaitingRead: false,
      writeUnconfirmed: false, listPending: false, listFailed: false,
    },
    fields: { 'role-name': 'DraftRole' },
  },
  {
    name: 'menu', file: 'components/menus/MenuPanel', form: 'menu-panel-form', field: 'menu-title',
    props: { mode: 'create', node: null, parent: null, tree: [], awaitingRead: false },
    fields: { 'menu-name': 'DraftMenu', 'menu-path': '/console/draft', 'menu-permission': 'draft.view' },
  },
  { name: 'resume', file: 'pages/resumes/edit', field: 'resume-markdown' },
]) {
  test(`a dirty ${editor.name} editor survives a cross-tab session change and releases its guard on unmount`, async (t) => {
    const vite = await viewServer(t)
    const { default: Editor } = await vite.ssrLoadModule(`/src/${editor.file}.vue.view-test.ts`)
    const { default: request } = await vite.ssrLoadModule('/src/utils/request/request.ts')
    const { useSession } = await vite.ssrLoadModule('/src/composables/useSession.ts')
    const { hasDirtyDraft, listenToSessionEvents } = await vite.ssrLoadModule('/src/composables/session-events.ts')
    let stop
    t.after(() => stop?.())
    const { window, localStorage } = mockBrowser(t)
    const resume = { id: 'fixture', userId: 7, title: '简历草稿', content: '# 简历', headCommit: 'head' }
    let writes = 0
    request.defaults.adapter = async (config) => {
      let data
      if (config.method !== 'get') writes++
      switch (config.url) {
        case '/api/auth/getUserBaseInfo': data = { id: 7, email: 'fixture@example.test' }; break
        case '/api/auth/getUserPrivateInformation':
        case '/api/auth/getRouters': data = []; break
        case '/api/resume/fixture': data = resume; break
        case '/api/resume/fixture/version/recent': data = [{ id: 'head', content: resume.content }]; break
        default: assert.fail(config.url)
      }
      return { config, status: 200, data: { code: 200, data } }
    }
    const session = useSession({})
    await session.restore()
    const props = reactive({ open: false, ...editor.props })
    const view = renderView(Editor, props, { params: { id: resume.id } })
    const { byId, button, click } = viewControls(view.find)
    stop = listenToSessionEvents(session, view.router)
    const changeToken = (token) => {
      localStorage.setItem('token', token)
      window.dispatchEvent(Object.assign(new Event('storage'), { key: 'token', newValue: token, storageArea: localStorage }))
    }
    const fill = (id, value) => byId(id).props['onUpdate:modelValue'](value)
    try {
      await flushView()
      assert.equal(hasDirtyDraft(), false)
      props.open = true
      await nextTick()
      assert.equal(hasDirtyDraft(), false)
      for (const [id, value] of Object.entries(editor.fields ?? {})) fill(id, value)
      fill(editor.field, '未保存更改')
      await flushView()
      changeToken('other-session')
      await nextTick()
      assert.equal(window.location.reload.mock.callCount(), 0)
      assert.equal(session.crossTabChanged.value, 'changed')
      assert.equal(byId(editor.field).props.modelValue, '未保存更改')
      if (editor.form) {
        await byId(editor.form).props.onSubmit(new Event('submit', { cancelable: true }))
      } else {
        for (const label of ['保存']) {
          const action = button(label)
          assert.equal(action.props.disabled, true)
          await click(action)
        }
      }
      assert.equal(writes, 0)
    } finally {
      view.app.unmount()
    }
    assert.equal(hasDirtyDraft(), false)
    changeToken('fresh-session')
    assert.equal(window.location.reload.mock.callCount(), 1)
  })
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

test('uncertain user edits stay locked until a refreshed page contains the affected id', async (t) => {
  const vite = await viewServer(t)
  const { default: Users } = await vite.ssrLoadModule('/src/pages/console/users.vue.view-test.ts')
  const { default: request } = await vite.ssrLoadModule('/src/utils/request/request.ts')
  let view
  t.after(() => view?.app.unmount())
  mockBrowser(t)
  const fixture = JSON.parse(await readFile(new URL('./fixtures/users.json', import.meta.url)))
  const target = fixture.content[0]
  const reads = []
  let renamed = false
  request.defaults.adapter = async (config) => {
    if (config.url === '/api/user/getUserManageInfo') {
      const params = JSON.parse(config.data)
      reads.push(params)
      const content = !renamed ? [target] : params.number === 11 && !params.query.name
        ? [{ ...target, name: '更新后用户' }]
        : [fixture.content[1]]
      return { config, status: 200, data: { data: { ...fixture, content, totalElements: 22 } } }
    }
    if (config.url === '/api/role/getRoleList?roleName=')
      return { config, status: 200, data: { data: [{ id: 901, roleName: '测试管理员', status: 1 }] } }
    assert.equal(config.url, '/api/user/updateUser')
    renamed = true
    return { config, status: 204 }
  }
  view = renderView(Users, {}, { query: {
    name: target.name, status: '1', roleId: '901', page: '2',
  } })
  const { find } = view
  const { byId, byLabel, button, click } = viewControls(find)
  await flushView()
  assert.deepEqual(reads[0], { number: 11, size: 10, query: { name: target.name, status: 1, roleId: 901 } })
  click(byLabel(`编辑${target.name}`))
  await nextTick()
  byId('user-name').props['onUpdate:modelValue']('更新后用户')
  await byId('user-editor-form').props.onSubmit(new Event('submit', { cancelable: true }))
  await flushView()
  assert.equal(button('新增用户').props.disabled, true)

  await click(button('刷新列表核对'))
  await flushView()
  assert.deepEqual(reads.at(-1), { number: 1, size: 10, query: {} })
  assert.equal(button('新增用户').props.disabled, true)
  assert.equal(byId('user-name').props.modelValue, '更新后用户')
  assert.match(elementText(byId('user-write-unconfirmed')), /调整筛选条件或页码/)
  assert.equal(byLabel('编辑更新后用户'), undefined)

  click(button('取消'))
  await nextTick()
  click(button('放弃修改'))
  await nextTick()
  byLabel('用户列表分页').props['onUpdate:page'](2)
  await flushView()
  assert.equal(button('新增用户').props.disabled, true)
  await click(button('刷新列表核对'))
  await flushView()
  assert.deepEqual(reads.at(-1), { number: 11, size: 10, query: {} })
  assert.ok(byLabel('编辑更新后用户'))
  assert.equal(button('新增用户').props.disabled, false)
  assert.ok(find((element) => element.props.role === 'status' &&
    elementText(element).includes('已刷新用户列表，请核对实际状态')))
})
