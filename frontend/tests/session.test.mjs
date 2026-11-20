import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  consolePath,
  findMenu,
  loginDestination,
  loginFailure,
  menuTree,
  menuShortcuts,
  sessionFailure,
  validSession,
} from '../app/lib/session.ts'

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
  let source = await readFile(new URL(`../app/${file}`, import.meta.url), 'utf8')
  if (file.endsWith('.vue')) source = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
  source = source.replace(/^import[\s\S]*?from ['"][^'"]+['"]\s*$/gm, '')
    .replaceAll('import.meta.client', 'true').replaceAll('import.meta.server', 'false')
    .replaceAll('export function', 'function').replace('export default ', 'const defaultExport = ')
  const js = ts.transpileModule(source, { compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ESNext } }).outputText
  return new Function('bindings', `with (bindings) { ${js.replace(/^export \{\};$/m, '')}\nreturn { ${exports.join(',')} } }`)(bindings)
}

async function sessionFixture() {
  const vue = await import('vue')
  const { createRequire } = await import('node:module')
  const require = createRequire(import.meta.url)
  const nuxtRequire = createRequire(require.resolve('nuxt/package.json'))
  const { createRouter, createMemoryHistory } = await import(nuxtRequire.resolve('vue-router'))
  const router = createRouter({ history: createMemoryHistory(), routes: ['/console/users', '/console/roles', '/console/menus', '/login'].map(path => ({ path, component: {} })) })
  await router.push('/console/users')
  const storage = new Map([['token', 'synthetic'], ['token_timestamp', String(Date.now())]])
  const states = new Map()
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
  const { requireMutationSuccess } = await import('../app/lib/rbac-mutations.ts')
  const { useSession } = await scriptRuntime('composables/useSession.ts', {
    ...sessionLib,
    requireMutationSuccess,
    useState: (key, init) => { if (!states.has(key)) states.set(key, vue.ref(init())); return states.get(key) },
    useRouter: () => router,
    localStorage,
    sessionStorage: { removeItem() {} },
    getUserBaseInfo: async () => ({ email: 'fixture@example.invalid' }),
    getUserPrivateInformation: async () => { if (api.pending) return api.pending; if (api.failure) throw api.failure; return [] },
    getUserRoutes: async () => [node('/console/users')],
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
    const helper = await import(`../app/lib/${library}.ts`)
    let guard
    const panel = await scriptRuntime(`components/${file}`, {
      ...vue, ...helper, ...fixture.sessionLib, useSession, useMediaQuery: () => vue.ref(false),
      defineProps: () => panelProps,
      defineEmits: () => (event, value) => { if (event === 'update:open') panelProps.open = value },
      onBeforeRouteLeave: callback => { guard = callback }, onMounted() {}, onUnmounted() {},
      navigateTo: target => router.push(target),
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
  const { navigationMenus, loginLocation } = await import('../app/lib/session.ts')
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


test('both real route middlewares block transient restoration and retry without dropping credentials', async () => {
  const sessionLib = await import('../app/lib/session.ts')
  for (const file of ['middleware/auth.ts', 'middleware/signed-in.ts']) {
    const { session, useSession, storage, api } = await sessionFixture()
    const { defaultExport: guard } = await scriptRuntime(file, {
      ...sessionLib, useSession, defineNuxtRouteMiddleware: fn => fn,
      navigateTo: target => target, createError: error => error,
      abortNavigation: error => { throw error },
    }, ['defaultExport'])
    api.failure = { code: 503 }
    const target = { path: '/console/users', fullPath: '/console/users?edit=1#form' }
    await assert.rejects(guard(target), error => error.statusCode === 503 && error.data.redirect === target.fullPath)
    assert.equal(storage.get('token'), 'synthetic')
    assert.deepEqual(session.state.value.menus, [])
    api.failure = null
    assert.equal(await guard(target), undefined)
    storage.delete('token')
    assert.equal((await guard(target)).query.redirect, target.fullPath)
  }
})

test('stale restoration failures and successes use the new session in both route guards', async () => {
  const sessionLib = await import('../app/lib/session.ts')
  for (const file of ['middleware/auth.ts', 'middleware/signed-in.ts']) {
    for (const code of [401, 503, null]) {
      const { session, useSession, storage, api } = await sessionFixture()
      const { defaultExport: guard } = await scriptRuntime(file, {
        ...sessionLib, useSession, defineNuxtRouteMiddleware: fn => fn,
        navigateTo: target => target, createError: error => error,
        abortNavigation: error => { throw error },
      }, ['defaultExport'])
      const pending = Promise.withResolvers()
      api.pending = pending.promise
      const oldNavigation = guard({ path: '/console/users', fullPath: '/console/users' })
      api.pending = null
      await session.signIn('fixture@example.invalid', 'synthetic')
      if (code) pending.reject({ code })
      else pending.resolve([])
      assert.equal(await oldNavigation, undefined)
      assert.equal(storage.get('token'), 'synthetic-login')
      assert.equal(session.state.value.ready, true)
      assert.ok(findMenu(session.state.value.menus, '/console/users'))
    }
  }
})

test('menu icons accept only installed names and retain path/default fallbacks', async () => {
  const { menuIcon } = await import('../app/lib/menu-icons.ts')
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

test('retry from the global error page reruns guards even at the current URL', async () => {
  const { vue, router } = await sessionFixture()
  const redirect = '/console/users?edit=1#form'
  await router.replace(redirect)
  const outage = { statusCode: 503, data: { sessionRestore: true, redirect } }
  const denied = { statusCode: 403 }
  const activeError = vue.ref(outage)
  let guardCalls = 0
  let failure = outage
  router.onError(() => {})
  router.beforeEach(() => {
    guardCalls++
    if (failure) throw failure
  })

  const { leaveError } = await scriptRuntime('error.vue', {
    ...vue,
    defineProps: () => ({ error: activeError.value }),
    useRouter: () => router,
    useNuxtApp: () => ({ runWithContext: fn => fn() }),
    useError: () => activeError,
    useHead: () => {},
    isNuxtError: cause => Boolean(cause?.statusCode),
    clearError: async (options) => {
      if (options?.redirect) await router.replace(options.redirect)
      activeError.value = undefined
    },
  }, ['leaveError'])

  await leaveError(redirect)
  assert.equal(guardCalls, 1)
  assert.deepEqual(activeError.value, outage)

  failure = denied
  await leaveError(redirect)
  assert.equal(guardCalls, 2)
  assert.deepEqual(activeError.value, denied)

  failure = null
  await leaveError(redirect)
  assert.equal(guardCalls, 3)
  assert.equal(router.currentRoute.value.fullPath, redirect)
  assert.equal(activeError.value, undefined)
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

test('a storage change freezes the displayed draft and requests while reload is blocked', async () => {
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
  const { defaultExport: install } = await scriptRuntime('plugins/session.client.ts', {
    window, localStorage, useSession, useRouter: () => router, defineNuxtPlugin: callback => callback,
  }, ['defaultExport'])
  install({ vueApp: { onUnmount() {} } })
  const users = await import('../app/lib/users.ts')
  const draftProps = vue.reactive({ open: false, mode: 'create', rolesUnavailable: false })
  let writes = 0
  const panel = await scriptRuntime('components/users/UserEditor.vue', {
    ...vue, ...users, ...sessionLib, useSession, defineProps: () => draftProps,
    defineEmits: () => () => {}, useMediaQuery: () => vue.ref(false), onBeforeRouteLeave() {},
    onMounted() {}, onUnmounted() {}, addUser: async () => { writes++ },
  }, ['draft', 'save', 'sessionChanged'])
  draftProps.open = true
  await vue.nextTick()
  Object.assign(panel.draft, {
    name: 'Unsaved', email: 'draft@example.invalid', roleId: 1, password: 'synthetic-password',
  })
  storage.set('token', 'other-account')
  assert.throws(() => sessionLib.requestSessionOwner(true), /Session changed/)
  events.dispatchEvent(Object.assign(new Event('storage'), { key: 'token', storageArea: localStorage }))
  assert.equal(reloads, 1)
  assert.equal(session.state.value.invalidated, undefined)
  assert.equal(panel.sessionChanged.value, true)
  await panel.save()
  assert.equal(writes, 0)
  assert.equal(panel.draft.name, 'Unsaved')
  storage.set('token', 'synthetic')
  assert.equal(session.owns(owner), false, 'a token round trip still invalidates the old generation')
  assert.throws(() => sessionLib.withSessionOwner(owner, () => {}), /Session changed/)
})

test('request classification separates network, timeout and absent write bodies from explicit rejection', async () => {
  const { requestFailure } = await import('../app/utils/request/request.ts')
  const { checkReadAccess, mutationResultUnknown, mutationError, requireMutationSuccess, roleDeleteError } =
    await import('../app/lib/rbac-mutations.ts')
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
  assert.match(roleDeleteError({ code: 500 }), /关联用户/)
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
  const { vue, sessionLib, useSession } = fixture
  const events = []
  const panelProps = vue.reactive({ open: false, ...props })
  const mutationLib = await import('../app/lib/rbac-mutations.ts')
  const panel = await scriptRuntime(`components/${file}.vue`, {
    ...vue, ...sessionLib, ...mutationLib, useSession,
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
  const roles = await import('../app/lib/roles.ts')
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
  const menus = await import('../app/lib/menus.ts')
  const roles = await import('../app/lib/roles.ts')
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
