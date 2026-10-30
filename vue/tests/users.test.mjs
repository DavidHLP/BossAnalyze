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
  userWriteSucceeded,
} from '../app/lib/users.ts'

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
