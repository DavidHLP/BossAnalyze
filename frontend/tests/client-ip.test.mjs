import test from 'node:test'
import assert from 'node:assert/strict'
import { clientIp, sanitizeIpHeaders } from '../server/utils/client-ip.ts'

test('only explicit socket peers can supply one literal IP', () => {
  assert.equal(clientIp('127.0.0.1', '8.8.8.8'), '127.0.0.1')
  assert.equal(clientIp('127.0.0.1', '8.8.8.8', '127.0.0.1'), '8.8.8.8')
  for (const invalid of ['8.8.8.8, 1.1.1.1', 'example.com', '127.1', '::1%lo', ['8.8.8.8']]) {
    assert.equal(clientIp('::1', invalid, '::1'), '::1')
  }
  assert.equal(clientIp('::1', '2001:4860:4860::8888', '::1'), '2001:4860:4860::8888')
})

test('entry overwrites forged forwarding headers, preserves authentication', () => {
  const headers = { authorization: 'Bearer synthetic', forwarded: 'for=8.8.8.8', 'x-real-ip': '8.8.8.8', 'x-forwarded-for': '8.8.8.8', 'x-forwarded-host': 'evil', 'x-boss-client-ip': '8.8.8.8' }
  sanitizeIpHeaders(headers, '127.0.0.1')
  assert.deepEqual(headers, { authorization: 'Bearer synthetic', 'x-boss-client-ip': '127.0.0.1' })
})

test('Vite and production proxy preserve backend routing and the request trust boundary', async () => {
  const { apiPort } = await import('../server/api-proxy.ts')
  assert.equal(apiPort('/api/auth/login'), 8080)
  assert.equal(apiPort('/api/v1/system/logs/dashboard/stats'), 8080)
  assert.equal(apiPort('/api/v1/ai/matches'), 8081)
  assert.equal(apiPort('/api/v2/ai/matches?x=1'), 8081)
  assert.equal(apiPort('/api/v2/air'), 8080)
  assert.equal(apiPort('/console/dashboard'), null)
  assert.equal(apiPort('/_nuxt/api/hint'), null)
})
