import assert from 'node:assert/strict'
import { after, test } from 'node:test'
import { AxiosError } from 'axios'
import request from '../app/utils/request/request.ts'

after(() => {
  delete globalThis.window
  delete globalThis.localStorage
})

test('server requests fail before accessing browser storage or sending HTTP', async () => {
  request.defaults.adapter = () => assert.fail('Server request reached the network')
  await assert.rejects(request.get('/api/resume'), error =>
    error.message === 'The API client requires a browser session')
})

test('browser requests keep bearer authentication and response unwrapping', async () => {
  globalThis.window = { location: { href: '/' } }
  globalThis.localStorage = { getItem: () => 'test-session-token' }
  request.defaults.adapter = async config => {
    assert.equal(config.headers.Authorization, 'Bearer test-session-token')
    return { config, status: 200, headers: {}, data: { code: 200, data: ['resume'] } }
  }
  assert.deepEqual(await request.get('/api/resume'), ['resume'])
})

test('missing browser token still redirects without sending HTTP', async () => {
  globalThis.localStorage = { getItem: () => null }
  request.defaults.adapter = () => assert.fail('Unauthenticated request reached the network')
  await assert.rejects(request.get('/api/resume'))
  assert.equal(window.location.href, '/login')
})

test('HTTP 401 still clears the token and dispatches unauthorized', async () => {
  const removed = []
  const events = []
  globalThis.localStorage = {
    getItem: () => 'test-session-token',
    removeItem: key => removed.push(key),
  }
  window.dispatchEvent = event => events.push(event.type)
  request.defaults.adapter = async config => {
    const response = { config, status: 401, headers: {}, data: { message: 'Expired' } }
    throw new AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, undefined, response)
  }
  await assert.rejects(request.get('/api/resume'), error => error.code === 401)
  assert.deepEqual(removed, ['token'])
  assert.deepEqual(events, ['unauthorized'])
})
