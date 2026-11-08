import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  consolePath,
  findMenu,
  loginDestination,
  loginFailure,
  menuTree,
  menuShortcuts,
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
  return new Function('bindings', `with (bindings) { ${js}\nreturn { ${exports.join(',')} } }`)(bindings)
}

async function sessionFixture() {
  const vue = await import('vue')
  const { createRequire } = await import('node:module')
  const require = createRequire(import.meta.url)
  const nuxtRequire = createRequire(require.resolve('nuxt/package.json'))
  const { createRouter, createMemoryHistory } = await import(nuxtRequire.resolve('vue-router'))
  const sessionLib = await import('../app/lib/session.ts')
  const router = createRouter({ history: createMemoryHistory(), routes: ['/console/users', '/console/roles', '/console/menus', '/login'].map(path => ({ path, component: {} })) })
  await router.push('/console/users')
  const storage = new Map([['token', 'synthetic'], ['token_timestamp', String(Date.now())]])
  const states = new Map()
  const api = { failure: null }
  const { useSession } = await scriptRuntime('composables/useSession.ts', {
    ...sessionLib,
    useState: (key, init) => { if (!states.has(key)) states.set(key, vue.ref(init())); return states.get(key) },
    useRouter: () => router,
    localStorage: { getItem: key => storage.get(key) ?? null, removeItem: key => storage.delete(key), setItem: (key, value) => storage.set(key, value) },
    sessionStorage: { removeItem() {} },
    getUserBaseInfo: async () => ({ email: 'fixture@example.invalid' }),
    getUserPrivateInformation: async () => { if (api.failure) throw api.failure; return [] },
    getUserRoutes: async () => [node('/console/users')],
    login: async () => ({ token: 'synthetic-login' }),
  }, ['useSession'])
  return { vue, router, storage, api, session: useSession(), useSession }
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
      ...vue, ...helper, useSession, useMediaQuery: () => vue.ref(false),
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
    assert.equal(panel.draft[draftName], 'unsaved fixture')
    assert.equal(session.state.value.ready, true)
    assert.equal(await session.signOut(), false)
    await panel.discard()
    assert.equal(router.currentRoute.value.path, '/login', file)
    assert.equal(storage.has('token'), false, file)
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
  for (const file of ['middleware/auth.ts', 'middleware/signed-in.ts']) {
    const { session, useSession, storage, api } = await sessionFixture()
    const sessionLib = await import('../app/lib/session.ts')
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

test('menu icons accept only installed names and retain path/default fallbacks', async () => {
  const { menuIcon } = await import('../app/lib/menu-icons.ts')
  const { ShieldUserIcon, UserGroupIcon, File01Icon } = await import('@hugeicons/core-free-icons')
  assert.equal(menuIcon('/console/users', 'ShieldUserIcon'), ShieldUserIcon)
  assert.equal(menuIcon('/console/users', '<script>'), UserGroupIcon)
  assert.equal(menuIcon('/console/unknown', '__proto__'), File01Icon)
})
