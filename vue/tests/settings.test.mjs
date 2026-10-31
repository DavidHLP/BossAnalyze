import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  accountSnapshot,
  accessSnapshot,
  healthSnapshot,
  settingsFailure,
} from '../app/lib/settings.ts'

test('settings account data excludes credentials and private unneeded fields', () => {
  assert.deepEqual(
    accountSnapshot({
      id: 15,
      name: '测试账号',
      email: 'fixture@example.invalid',
      status: 0,
      roleId: 6,
      password: 'never-render',
      token: 'never-render',
      lastLoginIp: 'private',
      address: 'private',
    }),
    { id: 15, name: '测试账号', email: 'fixture@example.invalid', roleId: 6 },
  )
  for (const value of [null, {}, { id: 1, name: 'Test', email: '' }, { code: 500 }])
    assert.throws(() => accountSnapshot(value))
})
test('settings access accepts a genuinely empty permission list and validates permission strings', () => {
  const role = { id: 6, roleName: 'ADMIN', status: 1, remark: 'existing', permissions: ['ignored'] }
  assert.deepEqual(accessSnapshot(role, []).permissions, [])
  assert.deepEqual(accessSnapshot(role, ['b.read', 'a.read', 'b.read']).permissions, [
    'a.read',
    'b.read',
  ])
  assert.throws(() => accessSnapshot(role, [{ permission: 'a.read' }]))
  assert.throws(() => accessSnapshot({ ...role, id: 0 }, []))
})
test('health reports only validated gateway connectivity and excludes raw internal errors', () => {
  const timestamp = 1793380000000
  assert.deepEqual(
    healthSnapshot({ status: 'UP', sparkService: 'UP', timestamp, sparkResponse: 'internal' }),
    { connected: true, checkedAt: timestamp },
  )
  assert.deepEqual(
    healthSnapshot({
      status: 'DEGRADED',
      sparkService: 'DOWN',
      timestamp,
      error: 'private backend address',
    }),
    { connected: false, checkedAt: timestamp },
  )
  for (const value of [
    null,
    {},
    { status: 'DOWN' },
    { status: 'UP', sparkService: 'DOWN', timestamp },
    { status: 'UP', sparkService: 'UP', timestamp: '1' },
    { status: 'UP', sparkService: 'UP', timestamp: -1 },
  ])
    assert.throws(() => healthSnapshot(value))
})
test('forbidden/business failures are distinct from missing data and service degradation', () => {
  assert.equal(settingsFailure({ code: 403 }), 'forbidden')
  assert.equal(settingsFailure({ code: 503 }), 'error')
  assert.equal(settingsFailure(new Error('Invalid')), 'error')
  assert.throws(
    () => healthSnapshot({ code: 403, data: null }),
    (error) => settingsFailure(error) === 'forbidden',
  )
  assert.throws(
    () => accessSnapshot({ id: 6, roleName: 'ADMIN' }, { code: 403, data: null }),
    (error) => settingsFailure(error) === 'forbidden',
  )
})
