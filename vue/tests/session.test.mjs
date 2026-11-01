import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  consolePath,
  findMenu,
  loginDestination,
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
  assert.equal(loginDestination('/console/reports?tab=all', menus), '/console/reports')
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
