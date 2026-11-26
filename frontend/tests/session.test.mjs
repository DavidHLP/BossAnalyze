import assert from 'node:assert/strict'
import { test } from 'node:test'
import { fileURLToPath } from 'node:url'
import {
  consolePath,
  findMenu,
  loginDestination,
  loginFailure,
  menuTree,
  menuShortcuts,
  sessionFailure,
  validSession,
} from '../src/lib/session.ts'

const node = (path, permission = '', extra = {}) => ({
  path,
  permission,
  name: path,
  status: 1,
  children: [],
  meta: { type: 'M' },
  ...extra,
})

test('login failures only classify explicit statuses and keep ambiguous failures neutral', () => {
  assert.equal(
    loginFailure({ code: 401, message: 'account does not exist' }),
    '邮箱或密码不正确，请检查后重试。',
  )
  for (const code of [502, 503, 504]) {
    const error = { code }
    assert.equal(loginFailure(error), '暂时无法完成登录，请稍后重试或检查服务连接。')
  }
  for (const error of [{ code: 500 }, new Error('Missing token'), null]) {
    assert.equal(loginFailure(error), '登录失败，请检查邮箱、密码及服务连接后重试。')
  }
})

test('only an explicit 401 expires the stored session; transient failures keep it', () => {
  assert.equal(sessionFailure({ code: 401 }), 'expired')
  const transientErrors = [
    { code: 500 }, { code: 502 }, { code: 503 }, { code: 504 }, new Error('timeout'), null,
  ]
  for (const error of transientErrors) {
    assert.equal(sessionFailure(error), 'unavailable')
  }
})

test('only granted menus and their granted ancestors survive', () => {
  const routes = [
    node('/console/system', 'system', {
      children: [node('users', 'users'), node('roles', 'roles')],
    }),
  ]
  assert.deepEqual(menuTree(routes, ['users']), [])
  const menus = menuTree(routes, ['system', 'users'])
  assert.equal(menus[0].children.length, 1)
  assert.equal(menus[0].children[0].path, '/console/system/users')
  assert.equal(findMenu(menus, '/console/system/roles'), undefined)
  assert.equal(findMenu(menus, '/console/system/users')?.title, 'users')
  assert.equal(routes[0].children.length, 2)
})

test('disabled, hidden, action and malformed permission entries are excluded', () => {
  const routes = [
    node('/console/disabled', '', { status: 0 }),
    node('/console/hidden', '', { meta: { hidden: true } }),
    node('/console/action', '', { meta: { type: 'F' } }),
    node('/console/malformed', 0),
    node('/console/visible'),
  ]
  assert.deepEqual(
    menuTree(routes, []).map((item) => item.path),
    ['/console/visible'],
  )
  assert.deepEqual(menuTree([node('/console/private', 'admin')], ['*']), [])
  assert.throws(() => menuTree({}, []), /Invalid/)
  assert.throws(() => menuTree([], [{ admin: true }]), /Invalid/)
})

test('menu order does not depend on unique titles and legacy paths stay in the console', () => {
  const menus = menuTree(
    [
      node('/reports', '', { menuOrder: 2, name: 'Same' }),
      node('/users', '', { menuOrder: 1, name: 'Same' }),
    ],
    [],
  )
  assert.deepEqual(
    menus.map((item) => item.path),
    ['/console/users', '/console/reports'],
  )
})

test('navigation rejects external, encoded, reserved and ungranted destinations', () => {
  for (const path of [
    'https://evil.test',
    '//evil.test',
    '/\\evil.test',
    '/%2f%2fevil.test',
    '/api/auth/login',
    '/login',
    '/_nuxt/source',
    '/console/../api/write',
  ]) {
    assert.equal(consolePath(path), null, path)
  }
  const menus = menuTree([node('/console/reports')], [])
  assert.equal(loginDestination('https://evil.test', menus), '/console')
  assert.equal(loginDestination('/console/admin', menus), '/console')
  assert.equal(loginDestination('/console/reports?tab=all', menus), '/console/reports?tab=all')
  assert.equal(loginDestination('/recruitment', []), '/recruitment')
  assert.equal(loginDestination('/recruitment/../console/users', []), '/console')
})

test('local session lifetime accepts fresh timestamps and rejects missing or expired state', () => {
  const now = 100_000_000
  assert.equal(validSession('test-token', String(now), now), true)
  for (const timestamp of [null, '', 'NaN', String(now + 1), String(now - 86_400_000)]) {
    assert.equal(validSession('test-token', timestamp, now), false)
  }
  assert.equal(validSession(null, String(now), now), false)
})

test('workbench shortcuts mirror authorized sidebar leaves without groups, home or duplicates', () => {
  const menus = [
    {
      title: 'Group',
      path: '/console',
      children: [
        { title: 'Users', path: '/console/users', children: [] },
        { title: 'Roles', path: '/console/roles', children: [] },
      ],
    },
    { title: 'Duplicate', path: '/console/users', children: [] },
    { title: 'Home', path: '/console', children: [] },
    { title: 'Label', path: null, children: [] },
  ]
  assert.deepEqual(menuShortcuts(menus), [
    { title: 'Users', path: '/console/users' },
    { title: 'Roles', path: '/console/roles' },
  ])
  assert.equal(menus[0].children.length, 2)
})

test('workbench cannot expose hidden, disabled or ungranted entries after session filtering', () => {
  const routes = [
    node('/console', 'console', {
      children: [
        node('dashboard', 'dashboard'),
        node('users', 'users'),
        node('hidden', 'dashboard', { meta: { hidden: true } }),
        node('disabled', 'dashboard', { status: 0 }),
      ],
    }),
  ]
  assert.deepEqual(menuShortcuts(menuTree(routes, [])), [])
  assert.deepEqual(menuShortcuts(menuTree(routes, ['console', 'dashboard'])), [
    { title: 'dashboard', path: '/console/dashboard' },
  ])
})

// Execute the production composable and editor guards with isolated storage/APIs.
// Vue Router is real: its aborted navigation return value is part of this regression.
async function scriptRuntime(file, bindings, exports) {
  const { readFile } = await import('node:fs/promises')
  const ts = await import('typescript')
  let source = await readFile(new URL(`../src/${file}`, import.meta.url), 'utf8')
  if (file.endsWith('.vue')) source = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
  source = source.replace(/^import[\s\S]*?from ['"][^'"]+['"]\s*$/gm, '')
    .replaceAll('import.meta.client', 'true').replaceAll('import.meta.server', 'false')
    .replaceAll('export function', 'function').replace('export default ', 'const defaultExport = ')
  const js = ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ESNext } }).outputText
  return new Function('bindings', `with (bindings) { ${js.replace(/^export \{\};$/m, '')}\nreturn { ${exports.join(',')} } }`)(bindings)
}

async function sessionFixture() {
  const vue = await import('vue')
  const { createRouter, createMemoryHistory } = await import('vue-router')
  const router = createRouter({ history: createMemoryHistory(), routes: ['/console/users', '/console/roles', '/console/menus', '/login'].map(path => ({ path, component: {} })) })
  await router.push('/console/users')
  const storage = new Map([['token', 'synthetic'], ['token_timestamp', String(Date.now())]])
  const api = { failure: null, logoutCalls: 0, logoutFailure: false }
  const localStorage = {
    getItem: key => storage.get(key) ?? null,
    removeItem: key => storage.delete(key),
    setItem: (key, value) => storage.set(key, value),
  }
  const sessionLib = await scriptRuntime('lib/session.ts', { localStorage }, [
    'advanceSession', 'bindDocumentSession', 'loginLocation', 'menuTree', 'ownsSession', 'requestSessionOwner',
    'sessionOwner', 'validSession', 'withSessionOwner',
  ])
  const { requireMutationSuccess } = await import('../src/lib/rbac-mutations.ts')
  const { useSession } = await scriptRuntime('composables/useSession.ts', {
    ...sessionLib,
    requireMutationSuccess,
    ref: vue.ref,
    useRouter: () => router,
    localStorage,
    sessionStorage: { removeItem() {} },
    getUserBaseInfo: async () => api.user ?? { email: 'fixture@example.invalid' },
    getUserPrivateInformation: async () => { if (api.pending) return api.pending; if (api.failure) throw api.failure; return api.permissions ?? [] },
    getUserRoutes: async () => api.routes ?? [node('/console/users')],
    login: async () => ({ token: 'synthetic-login' }),
    logout: async () => {
      api.logoutCalls++
      api.logoutStarted?.()
      if (api.logoutPending) return api.logoutPending
      if (api.logoutFailure) throw { code: 503 }
      return { code: 200 }
    },
  }, ['useSession'])
  return { vue, router, storage, localStorage, sessionLib, api, session: useSession(), useSession }
}

async function pageFixture(t, file, location, routes) {
  const { createServer } = await import('vite')
  const { createSSRApp } = await import('vue')
  const { renderToString } = await import('vue/server-renderer')
  const { createRouter, createMemoryHistory } = await import('vue-router')
  const vite = await createServer({
    root: fileURLToPath(new URL('../', import.meta.url)),
    appType: 'custom',
    optimizeDeps: { noDiscovery: true, include: [] },
    server: { middlewareMode: true, ws: false, watch: null },
  })
  t.after(() => vite.close())
  const { default: Page } = await vite.ssrLoadModule(`/src/${file}`)
  const { installComponents, ui } = await vite.ssrLoadModule('/src/ui-components.ts')
  const router = createRouter({
    history: createMemoryHistory(),
    routes: routes.map(path => ({ path, component: { render: () => null } })),
  })
  await router.push(location)
  return {
    router,
    load: vite.ssrLoadModule.bind(vite),
    render() {
      const app = createSSRApp(Page)
      app.use(router)
      installComponents(app, ui)
      return renderToString(app)
    },
  }
}

test('transient restore fails closed, retains token and retries; 401 invalidates it', async () => {
  const { session, storage, api } = await sessionFixture()
  await session.restore()
  session.state.value.ready = false
  api.failure = { code: 503 }
  await assert.rejects(session.restore())
  assert.equal(storage.get('token'), 'synthetic')
  assert.equal(session.state.value.ready, false)
  assert.deepEqual(session.state.value.menus, [])
  api.failure = null
  assert.equal(await session.restore(), true)
  session.state.value.ready = false
  api.failure = { code: 401 }
  await assert.rejects(session.restore())
  assert.equal(storage.has('token'), false)
  assert.equal(session.state.value.invalidated, true)
})

test('session refresh updates the user and route authorization without changing the session owner', async () => {
  const { authorizeRoute } = await import('../src/router/guard.ts')
  const { session, api } = await sessionFixture()
  await session.restore()
  const owner = session.capture()
  api.user = { id: 1, name: 'Updated name', email: 'fixture@example.invalid' }
  api.permissions = ['roles.read']
  api.routes = [node('/console/roles', 'roles.read')]
  assert.equal(await session.refresh(), true)
  assert.deepEqual(session.capture(), owner)
  assert.equal(session.state.value.user.name, 'Updated name')
  assert.deepEqual(session.state.value.permissions, ['roles.read'])
  assert.equal(await authorizeRoute(session, {
    path: '/console/roles', fullPath: '/console/roles', meta: { access: 'menu' },
  }), true)
  const revoked = await authorizeRoute(session, {
    path: '/console/users', fullPath: '/console/users', meta: { access: 'menu' },
  })
  assert.equal(revoked.params.code, '403')
})

test('a failed refresh leaves the restored account and authorization available for retry', async () => {
  const { session, api } = await sessionFixture()
  await session.restore()
  const owner = session.capture()
  const restored = session.state.value
  for (const failure of [new Error('network unavailable'), { code: 503 }]) {
    api.failure = failure
    assert.equal(await session.refresh(), false)
    assert.deepEqual(session.capture(), owner)
    assert.deepEqual(session.state.value, restored)
  }
  api.failure = null
  api.routes = [node('/console/roles')]
  assert.equal(await session.refresh(), true)
  assert.ok(findMenu(session.state.value.menus, '/console/roles'))
})

test('a refresh from a replaced session cannot publish old authorization or clear the new token', async () => {
  for (const expired of [false, true]) {
    const { session, api, storage } = await sessionFixture()
    await session.restore()
    const response = Promise.withResolvers()
    api.pending = response.promise
    const oldRefresh = session.refresh()

    api.pending = null
    api.routes = [node('/console/roles')]
    await session.signIn('fixture@example.invalid', 'synthetic')
    if (expired) response.reject({ code: 401 })
    else response.resolve([])

    assert.equal(await oldRefresh, false)
    assert.equal(storage.get('token'), 'synthetic-login')
    assert.ok(findMenu(session.state.value.menus, '/console/roles'))
    assert.equal(findMenu(session.state.value.menus, '/console/users'), undefined)
  }
})

test('a slow refresh does not replace the authorization published by a newer refresh', async () => {
  const { session, api } = await sessionFixture()
  await session.restore()
  const response = Promise.withResolvers()
  api.pending = response.promise
  const older = session.refresh()

  api.pending = null
  api.routes = [node('/console/roles')]
  assert.equal(await session.refresh(), true)
  response.resolve([])
  await older

  assert.ok(findMenu(session.state.value.menus, '/console/roles'))
  assert.equal(findMenu(session.state.value.menus, '/console/users'), undefined)
})

test('all production editor guards preserve session and draft on cancel, then complete logout on discard', async () => {
  for (const [file, library, draftName, props] of [
    ['users/UserEditor.vue', 'users', 'name', { mode: 'edit', roles: [] }],
    ['roles/RolePanel.vue', 'roles', 'roleName', { mode: 'edit', tree: [], role: null }],
    ['menus/MenuPanel.vue', 'menus', 'name', { mode: 'edit', tree: [], node: null }],
  ]) {
    const fixture = await sessionFixture()
    const { vue, router, session, useSession, storage } = fixture
    await session.restore()
    const panelProps = vue.reactive({ open: true, ...props })
    const helper = await import(`../src/lib/${library}.ts`)
    let guard
    const panel = await scriptRuntime(`components/${file}`, {
      ...vue, ...helper, ...fixture.sessionLib, useSession, useMediaQuery: () => vue.ref(false),
      defineProps: () => panelProps,
      defineEmits: () => (event, value) => { if (event === 'update:open') panelProps.open = value },
      onBeforeRouteLeave: callback => { guard = callback }, onMounted() {}, onUnmounted() {},
      useRouter: () => router,
    }, ['draft', 'cancelDiscard', 'discard', 'discardOpen'])
    panel.draft[draftName] = 'unsaved fixture'
    const remove = router.beforeEach(guard)
    assert.equal(await session.signOut(), false, file)
    assert.equal(panel.discardOpen.value, true)
    panel.cancelDiscard()
    assert.equal(storage.get('token'), 'synthetic', file)
    assert.equal(fixture.api.logoutCalls, 0, file)
    assert.equal(panel.draft[draftName], 'unsaved fixture')
    assert.equal(session.state.value.ready, true)
    assert.equal(await session.signOut(), false)
    await panel.discard()
    assert.equal(router.currentRoute.value.path, '/login', file)
    assert.equal(storage.has('token'), false, file)
    assert.equal(fixture.api.logoutCalls, 1, file)
    remove()
  }
})

test('forced permission refresh bypasses dirty guard and clears session before navigation', async () => {
  const { session, router, storage } = await sessionFixture()
  await session.restore()
  router.beforeEach(() => session.state.value.invalidated ? true : false)
  assert.equal(await session.signOut(true, '/console/users?tab=edit#form', 'expired'), true)
  assert.equal(storage.has('token'), false)
  assert.equal(router.currentRoute.value.query.redirect, '/console/users?tab=edit#form')
})

test('navigation-hidden nodes remain authorized while whole hidden nodes are removed', async () => {
  const { navigationMenus, loginLocation } = await import('../src/lib/session.ts')
  const menus = menuTree([
    node('/console/users', '', { meta: { metaHidden: true, metaIcon: 'UserGroupIcon' } }),
    node('/console/roles', '', { meta: { hidden: true } }),
  ], [])
  assert.equal(findMenu(menus, '/console/users')?.icon, 'UserGroupIcon')
  assert.equal(findMenu(menus, '/console/roles'), undefined)
  assert.deepEqual(navigationMenus(menus), [])
  assert.deepEqual(menuShortcuts(menus), [])
  assert.equal(loginDestination('/console/users?edit=1#form', menus), '/console/users?edit=1#form')
  for (const value of ['//evil.test', '/\\evil.test', '/login', '/api/test', 'https://evil.test']) {
    assert.equal(loginLocation(value).query.redirect, '/console')
  }
})


test('menu-protected destinations require authorization for the exact pathname', async () => {
  const { authorizeRoute } = await import('../src/router/guard.ts')
  const { session } = await sessionFixture()
  for (const path of ['/console/roles', '/console/USERS', '/Console/users', '/console/users/']) {
    const destination = { path, fullPath: path, meta: { access: 'menu' } }
    assert.deepEqual(await authorizeRoute(session, destination), {
      name: 'system-error', params: { code: '403' }, replace: true,
    })
  }
  assert.equal(await authorizeRoute(session, {
    path: '/console/users',
    fullPath: '/console/users?name=fixture#list',
    meta: { access: 'menu' },
  }), true)
})

test('protected routes fail closed during transient restoration and retain credentials for retry', async () => {
  const { authorizeRoute } = await import('../src/router/guard.ts')
  for (const path of ['/console/users', '/recruitment']) {
    const { session, storage, api } = await sessionFixture()
    const target = { path, fullPath: path + '?edit=1#form', meta: { access: path === '/recruitment' ? 'session' : 'menu' } }
    api.failure = { code: 503 }
    const failure = await authorizeRoute(session, target)
    assert.equal(failure.params.code, '503')
    assert.equal(failure.query.redirect, target.fullPath)
    assert.equal(storage.get('token'), 'synthetic')
    assert.deepEqual(session.state.value.menus, [])
    api.failure = null
    assert.equal(await authorizeRoute(session, target), true)
    storage.delete('token')
    assert.equal((await authorizeRoute(session, target)).query.redirect, target.fullPath)
  }
})

test('stale restoration failures and successes use the newer session on protected routes', async () => {
  const { authorizeRoute } = await import('../src/router/guard.ts')
  for (const path of ['/console/users', '/recruitment']) {
    for (const code of [401, 503, null]) {
      const { session, storage, api } = await sessionFixture()
      const pending = Promise.withResolvers()
      api.pending = pending.promise
      const oldNavigation = authorizeRoute(session, {
        path, fullPath: path, meta: { access: path === '/recruitment' ? 'session' : 'menu' },
      })
      api.pending = null
      await session.signIn('fixture@example.invalid', 'synthetic')
      if (code) pending.reject({ code })
      else pending.resolve([])
      assert.equal(await oldNavigation, true)
      assert.equal(storage.get('token'), 'synthetic-login')
      assert.equal(session.state.value.ready, true)
      assert.ok(findMenu(session.state.value.menus, '/console/users'))
    }
  }
})

test('menu icons accept only installed names and retain path/default fallbacks', async () => {
  const { menuIcon } = await import('../src/lib/menu-icons.ts')
  const { ShieldUserIcon, UserGroupIcon, File01Icon } = await import('@hugeicons/core-free-icons')
  assert.equal(menuIcon('/console/users', 'ShieldUserIcon'), ShieldUserIcon)
  assert.equal(menuIcon('/console/users', '<script>'), UserGroupIcon)
  assert.equal(menuIcon('/console/unknown', '__proto__'), File01Icon)
})

test('failed server logout is explicitly unconfirmed and forced expiry skips revocation', async () => {
  const { session, storage, router, api } = await sessionFixture()
  api.logoutFailure = true
  assert.equal(await session.signOut(), true)
  assert.equal(api.logoutCalls, 1)
  assert.equal(storage.has('token'), false)
  assert.equal(router.currentRoute.value.query.logout, 'unconfirmed')
  await session.signOut(true)
  assert.equal(api.logoutCalls, 1)
})

test('a completed old logout cannot clear a newly signed-in session', async () => {
  const { session, storage, api } = await sessionFixture()
  const started = Promise.withResolvers()
  const pending = Promise.withResolvers()
  api.logoutPending = pending.promise
  api.logoutStarted = started.resolve
  const signingOut = session.signOut()
  await Promise.race([started.promise, signingOut.then(() => assert.fail('logout did not dispatch'))])
  await session.signIn('fixture@example.invalid', 'synthetic')
  pending.resolve({ code: 200 })
  await signingOut
  assert.equal(storage.get('token'), 'synthetic-login')
  assert.equal(session.state.value.ready, true)
})

test('pending login and recovery respect navigation away from the login form', async () => {
  for (const action of ['submit', 'retrySession']) {
    for (const departure of ['register', 'pending register', 'unmount', 'stay']) {
      const { vue, router, localStorage, session } = await sessionFixture()
      router.addRoute({ path: '/register', component: {} })
      await router.replace({ path: '/login', query: { redirect: '/console/users' } })
      const result = Promise.withResolvers()
      let unmount
      const form = await scriptRuntime('components/LoginForm.vue', {
        ...vue,
        useSession: () => ({ ...session, signIn: () => result.promise, restore: () => result.promise }),
        useRouter: () => router, useRoute: () => router.currentRoute.value,
        onMounted: callback => callback(), onUnmounted: callback => { unmount = callback },
        onBeforeRouteLeave: guard => router.beforeEach(guard),
        localStorage, loginDestination, loginFailure,
      }, [action])
      await session.restore()

      const pending = form[action]()
      if (departure === 'register') await router.push('/register')
      if (departure === 'unmount') unmount?.()
      const entered = Promise.withResolvers()
      const leave = Promise.withResolvers()
      let registering
      if (departure === 'pending register') {
        router.beforeEach(to => {
          if (to.path !== '/register') return true
          entered.resolve()
          return leave.promise
        })
        registering = router.push('/register')
        await entered.promise
      }
      result.resolve(true)
      await pending
      leave.resolve(true)
      await registering

      const expected = { register: '/register', 'pending register': '/register', unmount: '/login', stay: '/console/users' }
      assert.equal(router.currentRoute.value.path, expected[departure], `${action}: ${departure}`)
    }
  }
})

test('retry navigates to the original destination and repeats the route guard', async () => {
  const { vue, router, sessionLib, storage } = await sessionFixture()
  const redirect = '/console/users?edit=1#form'
  router.addRoute({ path: '/system-error/:code', component: {} })
  await router.replace('/system-error/503')
  let guardCalls = 0
  router.beforeEach(() => { guardCalls++; return true })
  const { retry } = await scriptRuntime('pages/SystemError.vue', {
    ...vue,
    useRoute: () => ({ params: { code: '503' }, query: { redirect } }),
    useRouter: () => router,
    document: { title: '' },
    ...sessionLib,
    safeReturnPath: (await import('../src/lib/session.ts')).safeReturnPath,
  }, ['retry'])
  const timestamp = storage.get('token_timestamp')
  storage.set('token_timestamp', '0')
  await retry()
  assert.equal(guardCalls, 0)
  storage.set('token_timestamp', timestamp)
  await retry()
  assert.equal(guardCalls, 1)
  assert.equal(router.currentRoute.value.fullPath, redirect)
})

test('the 503 page offers session retry only when the local credentials are valid', async (t) => {
  const page = await pageFixture(t, 'pages/SystemError.vue', '/system-error/503?redirect=/console/users', [
    '/system-error/:code', '/console', '/login',
  ])
  const storage = new Map()
  for (const [name, value] of Object.entries({
    document: { title: '' },
    localStorage: { getItem: key => storage.get(key) ?? null },
  })) {
    const original = Object.getOwnPropertyDescriptor(globalThis, name)
    Object.defineProperty(globalThis, name, { configurable: true, value })
    t.after(() => {
      if (original) Object.defineProperty(globalThis, name, original)
      else delete globalThis[name]
    })
  }
  const now = Date.now()
  for (const [token, timestamp] of [
    [null, String(now)],
    ['synthetic', null],
    ['synthetic', 'invalid'],
    ['synthetic', String(now - 86_400_000)],
  ]) {
    storage.set('token', token)
    storage.set('token_timestamp', timestamp)
    const html = await page.render()
    assert.match(html, /本机没有有效的登录凭据/)
    assert.match(html, /href="\/login\?redirect=\/console\/users"[^>]*>前往登录/)
    assert.doesNotMatch(html, /已保留当前登录凭据|重试恢复会话/)
  }
  storage.set('token_timestamp', String(now))
  const valid = await page.render()
  assert.match(valid, /已保留当前登录凭据/)
  assert.match(valid, /重试恢复会话/)
})

test('the workbench explains the missing dashboard permission and shortcut access without reading logs', async (t) => {
  const page = await pageFixture(t, 'pages/console/index.vue', '/console', [
    '/console', '/console/dashboard', '/console/profile', '/resumes', '/recruitment',
  ])
  const { useSession: usePageSession } = await page.load('/src/composables/useSession.ts')
  const { state } = usePageSession(page.router)
  const html = await page.render()
  assert.match(html, /当前账号未获数据看板权限/)
  assert.match(html, /当前账号暂无可用的授权快捷入口/)
  assert.doesNotMatch(html, /进入数据看板|刷新概览/)

  const vue = await import('vue')
  let loadOverview
  let logReads = 0
  await scriptRuntime('pages/console/index.vue', {
    ...vue, findMenu, menuShortcuts,
    useSession: () => ({ state }), useRouter: () => page.router,
    onMounted: callback => { loadOverview = callback },
    getDashboardStats: async () => { logReads++; return null },
    getUserRole: async () => ({ id: 1, roleName: 'User', status: 1 }),
    getUserPrivateInformation: async () => [],
    ...await import('../src/lib/dashboard.ts'),
    ...await import('../src/lib/settings.ts'),
  }, [])
  await loadOverview()
  assert.equal(logReads, 0)

  state.value.menus = menuTree([node('/console/dashboard', 'logs.read')], ['logs.read'])
  await loadOverview()
  assert.equal(logReads, 1)
  const authorized = await page.render()
  assert.match(authorized, /进入数据看板/)
  assert.doesNotMatch(authorized, /当前账号未获数据看板权限|当前账号暂无可用的授权快捷入口/)
})

test('forced logout only clears the initiating session even when navigation is delayed', async () => {
  const { session, router, storage, api } = await sessionFixture()
  await session.restore()
  const oldOwner = session.capture()
  const entered = Promise.withResolvers()
  const leave = Promise.withResolvers()
  router.beforeEach(() => {
    entered.resolve()
    return leave.promise
  })
  const signingOut = session.signOut(true, null, undefined, oldOwner)
  await entered.promise
  assert.equal(storage.has('token'), false)
  assert.equal(session.state.value.invalidated, true)
  await session.signIn('new@example.invalid', 'synthetic')
  leave.resolve(true)
  assert.equal(await signingOut, true)
  assert.equal(storage.get('token'), 'synthetic-login')
  assert.equal(session.state.value.ready, true)
  assert.equal(await session.signOut(true, null, undefined, oldOwner), false)
  assert.equal(storage.get('token'), 'synthetic-login')
  assert.equal(api.logoutCalls, 0)
})

test('cross-tab token changes freeze the displayed draft until a manual reload', async () => {
  const fixture = await sessionFixture()
  const { session, sessionLib, vue, storage, localStorage, useSession, router } = fixture
  await session.restore()
  const owner = session.capture()
  const events = new EventTarget()
  let reloads = 0
  const window = {
    addEventListener: events.addEventListener.bind(events),
    removeEventListener: events.removeEventListener.bind(events),
    location: { reload: () => { reloads++ } },
  }
  const { listenToSessionEvents, registerDirtyDraft, hasDirtyDraft } = await scriptRuntime('composables/session-events.ts', {
    window, localStorage,
  }, ['listenToSessionEvents', 'registerDirtyDraft', 'hasDirtyDraft'])
  const stopListening = listenToSessionEvents(session, router)
  const users = await import('../src/lib/users.ts')
  const draftProps = vue.reactive({ open: false, mode: 'create', rolesUnavailable: false })
  let writes = 0
  const panel = await scriptRuntime('components/users/UserEditor.vue', {
    ...vue, ...users, ...sessionLib, useSession, useRouter: () => router, defineProps: () => draftProps,
    defineEmits: () => () => {}, useMediaQuery: () => vue.ref(false), onBeforeRouteLeave() {},
    onMounted() {}, onUnmounted() {}, addUser: async () => { writes++ },
  }, ['draft', 'save', 'sessionChanged'])
  draftProps.open = true
  await vue.nextTick()
  Object.assign(panel.draft, {
    name: 'Unsaved', email: 'draft@example.invalid', roleId: 1, password: 'synthetic-password',
  })
  const unregister = registerDirtyDraft('user-editor', () => panel.draft.name !== '')
  assert.equal(hasDirtyDraft(), true)
  const notify = (key, newValue) =>
    events.dispatchEvent(Object.assign(new Event('storage'), { key, newValue, storageArea: localStorage }))
  storage.set('token', 'other-account')
  assert.throws(() => sessionLib.requestSessionOwner(true), /Session changed/)
  notify('token', 'other-account')
  notify('token_timestamp', storage.get('token_timestamp'))
  assert.equal(reloads, 0)
  assert.equal(session.crossTabChanged.value, 'changed')
  assert.equal(session.state.value.invalidated, undefined)
  assert.equal(panel.sessionChanged.value, true)
  assert.equal(session.owns(session.capture()), false)
  await panel.save()
  assert.equal(writes, 0)
  assert.equal(panel.draft.name, 'Unsaved')
  storage.clear()
  notify(null, null)
  assert.equal(session.crossTabChanged.value, 'cleared')
  assert.equal(reloads, 0)
  events.dispatchEvent(new Event('unauthorized'))
  assert.equal(session.state.value.invalidated, undefined)
  storage.set('token', 'synthetic')
  storage.set('token_timestamp', owner.timestamp)
  assert.equal(session.owns(owner), false, 'a token round trip still invalidates the old generation')
  assert.throws(() => sessionLib.withSessionOwner(owner, () => {}), /Session changed/)
  unregister()
  assert.equal(hasDirtyDraft(), false)
  notify('token', 'synthetic')
  assert.equal(reloads, 1)
  stopListening()
})

test('the console announces cross-tab invalidation and offers a manual reload', async (t) => {
  const page = await pageFixture(t, 'layouts/ConsoleLayout.vue', '/console', [
    '/console', '/resumes', '/recruitment',
  ])
  const { useSession } = await page.load('/src/composables/useSession.ts')
  const session = useSession(page.router)
  assert.equal((await page.render()).includes('立即刷新页面'), false)
  for (const [status, message] of [['changed', '其他标签页已变更登录状态'], ['cleared', '其他标签页已清除登录凭据']]) {
    session.crossTabChanged.value = status
    const html = await page.render()
    assert.equal(html.includes('role="alert"'), true)
    assert.equal(html.includes(message), true)
    assert.equal(html.includes('已锁定提交'), true)
    assert.equal(html.includes('立即刷新页面'), true)
  }
})

test('request classification separates network, timeout and absent write bodies from explicit rejection', async () => {
  const { requestFailure } = await import('../src/utils/request/request.ts')
  const { checkReadAccess, mutationResultUnknown, mutationError, requireMutationSuccess } =
    await import('../src/lib/rbac-mutations.ts')
  for (const failure of [
    requestFailure(undefined, undefined, 'Network Error'),
    requestFailure(undefined, undefined, 'timeout of 5000ms exceeded'),
    requestFailure(200, ''), requestFailure(204, null), requestFailure(504, '<html>Gateway timeout</html>'),
  ]) assert.equal(mutationResultUnknown(failure), true)
  for (const [status, body] of [
    [403, undefined], [404, null], [409, { code: 1011 }], [500, { code: 500 }],
  ]) assert.equal(mutationResultUnknown(requestFailure(status, body)), false)
  assert.throws(() => requireMutationSuccess(undefined), mutationResultUnknown)
  assert.throws(() => requireMutationSuccess({ code: 403 }), error => !mutationResultUnknown(error))
  assert.throws(() => checkReadAccess({ code: 403 }), { code: 403 })
  assert.match(mutationError({ code: 400, data: { code: 404 } }, '删除'), /目标已不存在/)
  assert.match(mutationError({ code: 500, message: '数据库关联用户错误' }, '删除'), /服务暂不可用/)
  assert.doesNotMatch(mutationError({ code: 500, message: '数据库关联用户错误' }, '删除'), /关联用户/)
})

test('the synchronous Axios interceptor sends the owned bearer and ignores an older generation’s 401', async () => {
  const { session, sessionLib, localStorage, storage } = await sessionFixture()
  const axios = (await import('axios')).default
  const expired = []
  const window = { dispatchEvent: event => expired.push(event.type) }
  const { defaultExport: request } = await scriptRuntime('utils/request/request.ts', {
    ...sessionLib, axios, localStorage, window,
  }, ['defaultExport'])
  await session.restore()
  const owner = session.capture()
  const response = Promise.withResolvers()
  let config
  const original = sessionLib.withSessionOwner(owner, () => request({
    url: '/api/auth/updateRoleRouters',
    method: 'POST',
    adapter: sent => { config = sent; return response.promise },
  }))
  assert.equal(config.headers.Authorization, 'Bearer synthetic')
  session.changed()
  const rejected = assert.rejects(original, { code: 401, resultUnknown: false })
  response.reject({ config, response: { status: 401 } })
  await rejected
  assert.equal(storage.get('token'), 'synthetic')
  assert.deepEqual(expired, [])
  await assert.rejects(request({
    url: '/api/auth/updateRoleRouters',
    method: 'POST',
    adapter: () => assert.fail('a stale authenticated write was dispatched'),
  }), /Session changed/)
  const current = session.capture()
  await assert.rejects(sessionLib.withSessionOwner(current, () => request({
    url: '/api/auth/updateRoleRouters',
    method: 'POST',
    adapter: async sent => { throw new axios.AxiosError('timeout', 'ECONNABORTED', sent) },
  })), { resultUnknown: true })
  await assert.rejects(sessionLib.withSessionOwner(current, () => request({
    url: '/api/auth/updateRoleRouters',
    method: 'POST',
    adapter: async sent => ({ data: '', status: 200, headers: {}, config: sent }),
  })), { resultUnknown: true })
})

async function panelRuntime(fixture, file, props, bindings, exports) {
  const { vue, sessionLib, useSession, router } = fixture
  const events = []
  const panelProps = vue.reactive({ open: false, ...props })
  const mutationLib = await import('../src/lib/rbac-mutations.ts')
  const panel = await scriptRuntime(`components/${file}.vue`, {
    ...vue, ...sessionLib, ...mutationLib, useSession, useRouter: () => router,
    defineProps: () => panelProps, useMediaQuery: () => vue.ref(false),
    defineEmits: () => (...event) => {
      events.push(event)
      if (event[0] === 'unconfirmed') panelProps.awaitingRead = true
    },
    onMounted() {}, onUnmounted() {}, onBeforeRouteLeave() {},
    ...bindings,
  }, exports)
  panelProps.open = true
  await vue.nextTick()
  return { panel, events }
}

test('an uncertain grant keeps its selection locked and forces reauthentication for the current role', async () => {
  const roles = await import('../src/lib/roles.ts')
  const tree = roles.permissionTree([{ id: 10, permission: 'fixture.write' }])
  const role = { id: 1, roleName: 'Fixture', status: 1, remark: '', routerIds: [], permissions: [] }
  for (const actorRoleId of [1, 2]) {
    const fixture = await sessionFixture()
    await fixture.session.restore()
    let writes = 0
    const { panel, events } = await panelRuntime(fixture, 'roles/RolePanel', {
      mode: 'grant', actorRoleId, role, tree, awaitingRead: false, locked: false,
    }, {
      ...roles,
      updateRoleRouters: async () => { writes++; throw { code: 500, resultUnknown: true } },
      getRoleList: async () => assert.fail('unknown transport result needs an explicit reread'),
    }, ['toggle', 'save', 'selected'])
    panel.toggle(10, true)
    await panel.save()
    assert.deepEqual(events, [['unconfirmed']])
    assert.deepEqual(panel.selected.value, [10])
    assert.equal(fixture.storage.has('token'), actorRoleId !== 1)
    assert.equal(fixture.router.currentRoute.value.path, actorRoleId === 1 ? '/login' : '/console/users')
    assert.equal(fixture.api.logoutCalls, 0)
    await panel.save()
    assert.equal(writes, 1)
  }
})

test('menu save only applies the unknown-result lock after dispatching the write', async () => {
  const menus = await import('../src/lib/menus.ts')
  const roles = await import('../src/lib/roles.ts')
  const fixture = await sessionFixture()
  await fixture.session.restore()
  let dependencyFails = true
  let writes = 0
  const disconnected = { code: 500, resultUnknown: true }
  const { panel, events } = await panelRuntime(fixture, 'menus/MenuPanel', {
    mode: 'create', node: null, parent: null, tree: [], awaitingRead: false,
  }, {
    ...menus, ...roles,
    getRoleList: async () => { if (dependencyFails) throw disconnected; return [] },
    addRouter: async () => { writes++; throw disconnected },
  }, ['draft', 'save', 'requestFailure'])
  Object.assign(panel.draft, { name: 'fixture', title: 'Fixture', path: '/fixture', permission: 'fixture.write' })
  await panel.save()
  assert.ok(panel.requestFailure.value)
  assert.equal(writes, 0)
  assert.deepEqual(events, [])
  dependencyFails = false
  await panel.save()
  assert.deepEqual(events, [['unconfirmed']])
  assert.equal(panel.draft.name, 'fixture')
  await panel.save()
  assert.equal(writes, 1)
})
