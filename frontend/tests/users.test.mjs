import assert from 'node:assert/strict'
import { test } from 'node:test'
import { readFile } from 'node:fs/promises'
import {
  userPageNumber,
  userPage,
  userRoles,
  userDraft,
  userErrors,
  userPayload,
  userSaveError,
  userDeleteError,
  userWriteSucceeded,
} from '../src/lib/users.ts'

const fixture = JSON.parse(await readFile(new URL('./fixtures/users.json', import.meta.url)))

test('pagination adapts page boundaries to the existing SQL offset contract', () => {
  assert.equal(userPageNumber(1, 10), 1)
  assert.equal(userPageNumber(2, 10), 11)
  assert.equal(userPageNumber(3, 20), 41)
  const pages = [1, 2, 3].flatMap((page) =>
    Array.from({ length: 10 }, (_, index) => userPageNumber(page, 10) - 1 + index),
  )
  assert.deepEqual(
    pages,
    Array.from({ length: 30 }, (_, index) => index),
  )
  assert.throws(() => userPageNumber(0, 10))
})

test('read normalization drops password, avatar and unknown fields and accepts a genuine empty page', () => {
  const result = userPage(fixture)
  assert.equal(result.total, 2)
  assert.equal(result.users[0].status, 1)
  assert.equal(result.users[1].status, 0)
  assert.equal(result.users[1].address, '')
  assert.equal('password' in result.users[0], false)
  assert.equal('avatar' in result.users[0], false)
  assert.deepEqual(userPage({ content: [], totalElements: 0 }), { users: [], total: 0 })
  for (const bad of [
    null,
    {},
    { content: [], totalElements: -1 },
    { content: [{}], totalElements: 1 },
  ])
    assert.throws(() => userPage(bad))
  assert.equal(
    userPage({ content: [{ id: 1, status: 9 }], totalElements: 1 }).users[0].status,
    null,
  )
  assert.throws(() => userRoles({}))
})

test('edit payload never sends a password or overwrites server-owned fields', () => {
  const draft = userDraft(userPage(fixture).users[0])
  assert.equal(draft.password, '')
  draft.password = 'must-not-be-sent'
  draft.name = '  新姓名  '
  draft.roleId = 902
  draft.status = '0'
  const payload = userPayload(draft, 9001)
  assert.equal(payload.name, '新姓名')
  assert.equal(payload.id, 9001)
  assert.equal(payload.roleId, 902)
  assert.equal(payload.status, 0)
  assert.equal('password' in payload, false)
  assert.equal('lastLogin' in payload, false)
  assert.equal('avatar' in payload, false)
})

test('creation validates fields and includes only the explicitly entered initial password', () => {
  const draft = userDraft()
  assert.equal(Object.keys(userErrors(draft, true)).length, 4)
  assert.throws(() => userPayload(draft))
  Object.assign(draft, {
    name: '新用户',
    email: 'new@example.test',
    roleId: 902,
    password: 'fixture-password-only',
  })
  assert.deepEqual(userErrors(draft, true), {})
  const payload = userPayload(draft)
  assert.equal('id' in payload, false)
  assert.equal(payload.password, 'fixture-password-only')
})

test('business failures in HTTP 200 responses cannot show a successful mutation', () => {
  userWriteSucceeded({ code: 200, data: null, message: '更新成功' })
  for (const response of [{ code: 500, message: 'Failed' }, undefined, {}, { code: 401 }])
    assert.throws(() => userWriteSucceeded(response))
})

test('user save errors provide safe actions for HTTP and business failures', () => {
  for (const [failure, message] of [
    [{ code: 403 }, /联系管理员/],
    [{ code: 409, data: { code: 1001 } }, /邮箱已被其他账号使用/],
    [{ code: 409 }, /数据发生冲突/],
    [{ code: 400, data: { code: 1007 } }, /参数未通过校验/],
    [{ code: 503 }, /服务暂不可用/],
    [new Error('internal stack trace'), /核对当前记录/],
  ]) {
    assert.match(userSaveError({ ...failure, message: 'internal stack trace' }).message, message)
    assert.doesNotMatch(userSaveError(failure).message, /internal stack trace/)
  }
  assert.throws(
    () => userWriteSucceeded({ code: 1001, message: 'internal stack trace' }),
    (error) => userSaveError(error).field === 'email' && !error.message.includes('stack trace'),
  )
  assert.equal(userSaveError({ code: 400 }, true).field, 'email')
  assert.equal(userSaveError({ code: 400, message: '邮箱已占用' }).field, null)
  assert.equal(userSaveError({ code: 500 }, true).field, null)
  assert.equal(userSaveError({ code: 403 }, true).field, null)
})

test('only the confirmation-password business code identifies an incorrect operator password', () => {
  const expected = '操作者密码不正确，请重试'
  assert.equal(userDeleteError({ code: 400, data: { code: 1006, message: 'internal' } }), expected)
  assert.throws(
    () => userWriteSucceeded({ code: 1006 }),
    (error) => userDeleteError(error) === expected,
  )
  assert.match(userDeleteError({ code: 400, data: { code: 1007 } }), /参数未通过校验/)
  assert.match(userDeleteError({ code: 403 }), /无权删除/)
})

test('create and edit introductions respect the existing 255-character column without truncation', () => {
  const draft = userDraft(userPage(fixture).users[0])
  draft.password = 'fixture-password-only'
  for (const userId of [undefined, 9001]) {
    for (const length of [0, 254, 255]) {
      draft.introduction = '简'.repeat(length)
      assert.equal(userErrors(draft, userId === undefined).introduction, undefined)
      assert.equal(userPayload(draft, userId).introduction, draft.introduction)
    }
    for (const length of [256, 500]) {
      draft.introduction = '简'.repeat(length)
      assert.equal(userErrors(draft, userId === undefined).introduction, '简介不能超过 255 个字符')
      assert.throws(() => userPayload(draft, userId), /Invalid user form/)
      assert.equal(draft.introduction.length, length)
    }
  }
})

test('a stale list response or error cannot reinstate a deleted row after rereading', async () => {
  const vue = await import('vue')
  const { useMediaQuery } = await import('@vueuse/core')
  const ts = await import('typescript')
  const usersLib = await import('../src/lib/users.ts')
  const mutations = await import('../src/lib/rbac-mutations.ts')
  const source = await readFile(new URL('../src/pages/console/users.vue', import.meta.url), 'utf8')
  const script = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
    .replace(/^import[\s\S]*?from ['"][^'"]+['"]\s*$/gm, '')
  const { outputText } = ts.transpileModule(script, { compilerOptions: { target: ts.ScriptTarget.ES2022 } })
  const session = { capture: () => ({}), owns: () => true, state: vue.ref({}) }
  const requests = []
  const mount = new Function('bindings', `with (bindings) {
    ${outputText}
    return { users, total, pending, failed, restricted, loadUsers, deleted, changeSize, size }
  }`)
  const page = mount({
    ...vue, ...usersLib, ...mutations, useMediaQuery,
    definePageMeta() {}, useSession: () => session, useRoute: () => ({ query: {} }),
    useRouter: () => ({ replace() {} }), onMounted() {}, onUnmounted() {}, onBeforeRouteLeave() {},
    withSessionOwner: (_owner, call) => call(),
    getUserManageInfo: () => {
      const request = Promise.withResolvers()
      requests.push(request)
      return request.promise
    },
  })
  const older = page.loadUsers()
  page.changeSize('50')
  assert.equal(page.size.value, 10)
  const newer = page.deleted()
  requests[1].resolve({ content: [], totalElements: 0 })
  await newer
  requests[0].resolve(fixture)
  await older
  assert.deepEqual(page.users.value, [])
  assert.equal(page.total.value, 0)
  const stale = page.loadUsers()
  const latest = page.loadUsers()
  requests[2].reject({ code: 403 })
  await stale
  assert.equal(page.pending.value, true)
  assert.equal(page.failed.value, false)
  assert.equal(page.restricted.value, false)
  requests[3].resolve({ content: [], totalElements: 0 })
  await latest
})
