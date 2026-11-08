import assert from 'node:assert/strict'
import { after, test } from 'node:test'
import { AxiosError } from 'axios'
import request from '../app/utils/request/request.ts'
import { loginFailure } from '../app/lib/session.ts'

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
  assert.equal(window.location.href, '/login?redirect=%2Fconsole')
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

test('a delayed 401 from a previous token cannot invalidate the newly signed-in session', async () => {
  let token = 'previous-token'
  let finish
  const sent = Promise.withResolvers()
  const events = []
  globalThis.localStorage = {
    getItem: () => token,
    removeItem: () => { token = null },
  }
  window.dispatchEvent = event => events.push(event.type)
  request.defaults.adapter = config => new Promise((resolve, reject) => {
    finish = () => reject(new AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, undefined,
      { config, status: 401, headers: {}, data: {} }))
    sent.resolve()
  })
  const oldRequest = request.get('/api/resume')
  const rejected = assert.rejects(oldRequest, error => error.code === 401)
  await sent.promise
  token = 'new-token'
  finish()
  await rejected
  assert.equal(token, 'new-token')
  assert.deepEqual(events, [])
})

test('login-controller-shaped 500 responses remain neutral after request normalization', async () => {
  for (const message of ['登录失败: 用户不存在', '登录失败: 密码错误', '登录失败: private database error']) {
    request.defaults.adapter = async config => {
      const response = {
        config, status: 500, headers: {}, data: { code: 500, message, data: null },
      }
      throw new AxiosError(
        'Request failed with status code 500', 'ERR_BAD_RESPONSE', config, undefined, response,
      )
    }
    await assert.rejects(
      request.post('/api/auth/login', { email: 'fixture@example.invalid', password: 'synthetic' }),
      error => {
        assert.equal(error.code, 500)
        assert.equal(loginFailure(error), '登录失败，请检查邮箱、密码及服务连接后重试。')
        return true
      },
    )
  }
})

test('legacy HTTP 200 login failures cannot be mistaken for a service outage after token validation', async () => {
  request.defaults.adapter = async config => ({
    config, status: 200, headers: {}, data: { code: 500, message: '登录失败: 密码错误', data: null },
  })
  const result = await request.post('/api/auth/login', {
    email: 'fixture@example.invalid', password: 'synthetic',
  })
  assert.equal(result.token, undefined)
  assert.equal(loginFailure(new Error('Missing token')), '登录失败，请检查邮箱、密码及服务连接后重试。')
})
