import assert from 'node:assert/strict'
import { test } from 'node:test'
import { readFile } from 'node:fs/promises'
import {
  managedMenus,
  menuRows,
  visibleMenus,
  menuDraft,
  menuErrors,
  menuPayload,
  menuDeleteReason,
  menuDeleteReasons,
  menuWriteSucceeded,
} from '../src/lib/menus.ts'

const raw = JSON.parse(await readFile(new URL('./fixtures/menus.json', import.meta.url)))
const tree = managedMenus(raw)
const root = tree[0]
const leaf = root.children[1]

test('menu normalization retains action/hidden/disabled records and rejects broken hierarchy', () => {
  assert.equal(menuRows(tree).length, 4)
  assert.equal(root.children[0].children[0].status, 0)
  assert.equal(root.children[0].children[0].meta.metaHidden, true)
  assert.deepEqual(managedMenus([]), [])
  for (const value of [{}, [...raw, raw[0]], [{ ...raw[0], pid: 1 }], [{ ...raw[0], meta: {} }]])
    assert.throws(() => managedMenus(value))
})
test('tree filters preserve ancestors and override folding without matching unrelated siblings', () => {
  assert.deepEqual(
    visibleMenus(tree, '', 'all', [900]).map((r) => r.node.id),
    [900],
  )
  assert.deepEqual(
    visibleMenus(tree, 'fixture.edit', '0', [900, 901]).map((r) => r.node.id),
    [900, 901, 902],
  )
  assert.deepEqual(visibleMenus(tree, 'missing', 'all', []), [])
})
test('edit payload preserves parent, role metadata and permission without resubmitting descendants', () => {
  const draft = menuDraft(root)
  draft.title = '新版目录'
  const payload = menuPayload(draft, tree, root, leaf)
  assert.equal(payload.pid, null)
  assert.equal(payload.meta.metaRoles, '["ADMIN"]')
  assert.equal(payload.permission, root.permission)
  assert.equal('children' in payload, false)
  const nullable = { ...leaf, meta: { ...leaf.meta, metaHidden: null, metaKeepAlive: null } }
  const nullablePayload = menuPayload(menuDraft(nullable), tree, nullable, null)
  assert.equal(nullablePayload.meta.metaHidden, null)
  assert.equal(nullablePayload.meta.metaKeepAlive, null)
  draft.permission = 'replacement'
  assert.throws(() => menuPayload(draft, tree, root, null))
  draft.permission = root.permission
  draft.type = 'F'
  assert.ok(menuErrors(draft, tree, root).type)
})
test('creation rejects existing grants, invalid order and unsupported parent while using null for roots', () => {
  const draft = {
    ...menuDraft(),
    title: '新菜单',
    name: 'New',
    path: 'new',
    permission: 'new.view',
  }
  assert.equal(menuPayload(draft, tree, null, null).pid, null)
  assert.equal(menuPayload(draft, tree, null, null).meta.metaRoles, '[]')
  assert.equal(menuPayload(draft, tree, null, root).pid, root.id)
  assert.throws(() => menuPayload(draft, tree, null, root.children[0].children[0]))
  assert.throws(() => menuPayload(draft, tree, null, { ...leaf, pid: 999 }))
  draft.permission = leaf.permission
  assert.ok(menuErrors(draft, tree).permission)
  draft.order = '2147483648'
  assert.ok(menuErrors(draft, tree).order)
})
test('deletion cannot remove descendants, shared permissions or role grants; business errors fail closed', () => {
  assert.match(menuDeleteReason(root, tree, []), /子菜单/)
  assert.match(menuDeleteReason(leaf, tree, [leaf.permission]), /角色/)
  assert.match(menuDeleteReason(leaf, [...tree, { ...leaf, id: 999 }], []), /共享/)
  assert.equal(menuDeleteReason(leaf, tree, []), '')
  assert.match(menuDeleteReason({ ...leaf, permission: null }, tree, []), /缺少/)
  menuWriteSucceeded({ code: 200 })
  assert.throws(() => menuWriteSucceeded({ code: 500 }))
})

test('delete preflight reports every applicable blocker', () => {
  const shared = { ...leaf, id: 999, permission: root.permission }
  const reasons = menuDeleteReasons(root, [...tree, shared], [root.permission])
  assert.equal(reasons.length, 3)
  assert.match(reasons[0], /存在后代/)
  assert.match(reasons[1], /共享/)
  assert.match(reasons[2], /角色仍引用/)
  assert.equal(menuDeleteReasons({ ...root, permission: null }, tree, []).length, 2)
  assert.deepEqual(menuDeleteReasons(leaf, tree, []), [])
})

test('menu edits retain stored permission and nullable historical role metadata', () => {
  const legacy = { ...leaf, permission: `${leaf.permission} ` }
  const payload = menuPayload(menuDraft(legacy), tree, legacy, null)
  assert.equal(payload.permission, legacy.permission)
  assert.equal(payload.meta.metaRoles, null)
})

test('delete keeps the leave guard active during the second preflight and locks unknown writes', async () => {
  const vue = await import('vue')
  const ts = await import('typescript')
  const menus = await import('../src/lib/menus.ts')
  const roles = await import('../src/lib/roles.ts')
  const mutations = await import('../src/lib/rbac-mutations.ts')
  const source = await readFile(new URL('../src/pages/console/menus.vue', import.meta.url), 'utf8')
  const script = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
    .replace(/^import[\s\S]*?from ['"][^'"]+['"]\s*$/gm, '')
  const { outputText } = ts.transpileModule(script, { compilerOptions: { target: ts.ScriptTarget.ES2022 } })
  const state = vue.ref({ invalidated: false })
  const session = { capture: () => ({}), owns: () => !state.value.invalidated, state }
  const mount = new Function('bindings', `with (bindings) {
    ${outputText}
    return { load, prepareDelete, remove, deletion, deleting, checking, beforeUnload, awaitingRead }
  }`)
  let preflight
  let guard
  let writes = 0
  const page = mount({
    ...vue, ...menus, ...roles, ...mutations, useSession: () => session, definePageMeta() {},
    useRoute: () => ({ query: {} }), useRouter: () => ({ replace() {} }), onMounted() {}, onUnmounted() {},
    onBeforeRouteLeave: callback => { guard = callback },
    withSessionOwner: (_owner, call) => call(),
    getUserRoutes: async () => raw,
    getRoleList: async () => preflight ?? [],
    deleteRouter: async () => { writes++; throw { code: 500, resultUnknown: true } },
  })
  await page.load()
  await page.prepareDelete({ currentTarget: null }, leaf)
  const second = Promise.withResolvers()
  preflight = second.promise
  const deleting = page.remove()
  assert.equal(page.checking.value, true)
  assert.equal(page.deleting.value, true)
  assert.equal(guard(), false)
  const unload = new Event('beforeunload', { cancelable: true })
  page.beforeUnload(unload)
  assert.equal(unload.defaultPrevented, true)
  state.value.invalidated = true
  assert.equal(guard(), true)
  second.resolve([])
  await deleting
  assert.equal(writes, 0)

  state.value.invalidated = false
  await page.remove()
  assert.equal(page.awaitingRead.value, 'delete')
  assert.equal(page.deletion.value, null)
  assert.equal(writes, 1)
  await page.prepareDelete({ currentTarget: null }, leaf)
  assert.equal(page.deletion.value, null)
  await page.load()
  assert.equal(page.awaitingRead.value, null)
})
