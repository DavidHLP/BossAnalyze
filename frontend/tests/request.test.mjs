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

test('logout is public and reaches the server without a token or redirect', async () => {
  globalThis.window = { location: { href: '/console' } }
  globalThis.localStorage = { getItem: () => null }
  let sent = false
  request.defaults.adapter = async config => {
    sent = true
    assert.equal(config.headers.Authorization, undefined)
    return { config, status: 200, headers: {}, data: { code: 200, data: null } }
  }
  await request.post('/api/auth/logout')
  assert.equal(sent, true)
  assert.equal(window.location.href, '/console')
})

test('wrong operator confirmation preserves the session for inline retry', async () => {
  const removed = []
  const events = []
  globalThis.localStorage = {
    getItem: () => 'test-session-token',
    removeItem: key => removed.push(key),
  }
  window.dispatchEvent = event => events.push(event.type)
  request.defaults.adapter = async config => {
    const response = { config, status: 400, headers: {}, data: { code: 1006, message: '密码错误' } }
    throw new AxiosError('Bad request', 'ERR_BAD_REQUEST', config, undefined, response)
  }
  await assert.rejects(request.post('/api/user/deleteUser', { id: 2, password: 'wrong' }),
    error => error.code === 400 && error.data.code === 1006)
  assert.deepEqual(removed, [])
  assert.deepEqual(events, [])
})

test('logout 401 leaves revocation feedback to signOut instead of the expiry flow', async () => {
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
  await assert.rejects(request.post('/api/auth/logout'), error => error.code === 401)
  assert.deepEqual(removed, [])
  assert.deepEqual(events, [])
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

// Exercise the installed consumers, including pnpm patches, without external services.
test('dependency merges reject prototype setters in source and target without losing merge behavior', async () => {
  const { createRequire } = await import('node:module')
  const require = createRequire(import.meta.url)
  const shadcn = createRequire(require.resolve('shadcn-vue'))
  for (const entry of ['deepmerge', 'deepmerge/dist/umd.js']) {
    const merge = shadcn(entry)
    const payload = JSON.parse('{"__proto__":{"isAdmin":true}}')
    for (const [target, source] of [
      [Object.create(null), payload], [payload, {}],
      [{ session: null }, { session: payload }], [{ session: 0 }, { session: payload }],
    ]) {
      for (const options of [{}, { clone: false }]) {
        const result = merge(target, source, options)
        const value = result.session ?? result
        assert.equal(Object.getPrototypeOf(value), Object.prototype)
        assert.equal(value.isAdmin, undefined)
        assert.equal({}.isAdmin, undefined)
      }
    }
    const symbol = Symbol('metadata')
    assert.deepEqual(merge({ nested: { a: 1 }, items: [1] }, { nested: { b: 2 }, items: [2], [symbol]: 3 }),
      { nested: { a: 1, b: 2 }, items: [1, 2], [symbol]: 3 })
    assert.deepEqual(merge({ items: [1] }, { items: [2] }, { arrayMerge: (_, source) => source }), { items: [2] })
  }
})

test('source map consumer keeps Unicode, spaces and literal plus paths with the ESM decoder', async () => {
  const { createRequire } = await import('node:module')
  let require = createRequire(import.meta.url)
  for (const name of ['nuxt/package.json', '@nuxt/vite-builder/package.json', 'vite', 'stylus', 'css']) {
    require = createRequire(require.resolve(name))
  }
  const resolve = require('source-map-resolve')
  let readPath
  const result = resolve.resolveSourceMapSync('/*# sourceMappingURL=a%20b+%E4%B8%AD.map */', '/tmp/style.css', path => {
    readPath = path
    return '{"version":3,"sources":[],"names":[],"mappings":""}'
  })
  assert.equal(readPath, '/tmp/a b+中.map')
  assert.equal(result.map.version, 3)
})

test('updated glob and minimatch preserve archiving, tracing and source file selection', async () => {
  const { createRequire } = await import('node:module')
  const { mkdtemp, writeFile, rm } = await import('node:fs/promises')
  const { tmpdir } = await import('node:os')
  const { join } = await import('node:path')
  const root = createRequire(import.meta.url)
  const shadcn = createRequire(root.resolve('shadcn-vue'))
  const { Project } = shadcn('ts-morph')
  const project = new Project({ useInMemoryFileSystem: true })
  project.createSourceFile('/src/one.ts', 'export const one = 1')
  project.createSourceFile('/src/two.ts', 'export const two = 2')
  assert.equal(project.getSourceFiles(['/src/*.ts', '!/src/two.ts']).length, 1)
  const nuxt = createRequire(root.resolve('nuxt/package.json'))
  const nitroServer = createRequire(nuxt.resolve('@nuxt/nitro-server'))
  const nitro = createRequire(nitroServer.resolve('nitropack/package.json'))
  const archiver = createRequire(nitro.resolve('archiver'))
  const utils = archiver('archiver-utils')
  const { nodeFileTrace } = nitro('@vercel/nft')
  const dir = await mkdtemp(join(tmpdir(), 'boss-dependency-'))
  try {
    await writeFile(join(dir, 'entry.cjs'), "module.exports = require('./value.cjs')")
    await writeFile(join(dir, 'value.cjs'), 'module.exports = 1')
    assert.deepEqual(utils.file.expand({ cwd: dir }, ['*.cjs', '!value.cjs']), ['entry.cjs'])
    const trace = await nodeFileTrace([join(dir, 'entry.cjs')], { base: dir })
    assert.ok(trace.fileList.has('value.cjs'))
    const nft = createRequire(nitro.resolve('@vercel/nft'))
    assert.equal((await nft('glob').glob('*.cjs', { cwd: dir })).length, 2)
  } finally {
    await rm(dir, { recursive: true, force: true })
  }
})
