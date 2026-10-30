import assert from 'node:assert/strict'
import { test } from 'node:test'
import { readFile } from 'node:fs/promises'
import {
  canonicalSelection,
  currentRoleId,
  managedRoles,
  permissionTree,
  permissionRows,
  roleDraft,
  roleErrors,
  rolePayload,
  roleWriteSucceeded,
  selectionPermissions,
  togglePermission,
  unrepresentedGrants,
} from '../app/lib/roles.ts'

const fixture = JSON.parse(await readFile(new URL('./fixtures/roles.json', import.meta.url)))
const tree = permissionTree(fixture.routers)
const roles = managedRoles(fixture.roles)

test('role reads whitelist fields, preserve zero status, and reject malformed responses', () => {
  assert.equal(roles.find((role) => role.id === 902).status, 0)
  assert.deepEqual(managedRoles([]), [])
  assert.equal('userId' in roles[0], false)
  assert.equal(currentRoleId({ id: 999 }), 999)
  for (const value of [
    null,
    {},
    [{ id: 0, roleName: 'bad' }],
    [fixture.roles[0], fixture.roles[0]],
  ])
    assert.throws(() => managedRoles(value))
  assert.throws(() => currentRoleId({}))
})

test('metadata save does not send permissions and default role cannot be renamed', () => {
  const role = roles.find((role) => role.id === 901)
  const draft = roleDraft(role)
  draft.roleName = ' 新角色名称 '
  const payload = rolePayload(draft, role)
  assert.equal(payload.roleName, '新角色名称')
  assert.equal('permissions' in payload, false)
  assert.equal('routerIds' in payload, false)
  assert.equal('id' in rolePayload(draft), false)
  const defaultRole = roles.find((role) => role.roleName === 'USER')
  assert.throws(() => rolePayload(draft, defaultRole))
  assert.ok(roleErrors(roleDraft()).roleName)
})

test('tree retains disabled and action nodes but rejects duplicate or unrepresentable IDs', () => {
  assert.equal(permissionRows(tree).length, 5)
  assert.ok(permissionRows(tree).some((row) => row.node.id === 14 && row.node.status === 0))
  assert.throws(() => permissionTree([{ id: 1, children: [{ id: 1 }] }]))
  assert.throws(() => permissionTree([{ id: 2147483648 }]))
  assert.throws(() => canonicalSelection(tree, [1000]))
})

test('selecting a leaf includes its ancestors without automatically granting siblings', () => {
  assert.deepEqual(togglePermission(tree, [], 13, true), [10, 12, 13])
  assert.deepEqual(selectionPermissions(tree, [13]), ['console.view', 'roles.edit', 'roles.view'])
  assert.deepEqual(togglePermission(tree, [10, 11, 12, 13], 12, false), [10, 11])
  assert.deepEqual(togglePermission(tree, [10, 11, 12, 13], 10, false), [])
})

test('shared permission keys select aliases and their ancestors; removal clears linked branches', () => {
  const aliasTree = [
    ...tree,
    ...permissionTree([
      { id: 20, permission: 'other.view', children: [{ id: 21, permission: 'users.view' }] },
    ]),
  ]
  assert.deepEqual(canonicalSelection(aliasTree, [11]), [10, 11, 20, 21])
  assert.deepEqual(togglePermission(aliasTree, [10, 11, 20, 21], 11, false), [10, 20])
})

test('unrepresented grants are detected and HTTP 200 business errors cannot pass', () => {
  assert.deepEqual(
    unrepresentedGrants(
      roles.find((role) => role.id === 902),
      tree,
    ),
    ['standalone.custom'],
  )
  assert.deepEqual(unrepresentedGrants({ ...roles[0], permissions: [], routerIds: [987] }, tree), [
    '路由 #987',
  ])
  roleWriteSucceeded({ code: 200, data: null })
  for (const value of [{ code: 500 }, {}, undefined]) assert.throws(() => roleWriteSucceeded(value))
})
