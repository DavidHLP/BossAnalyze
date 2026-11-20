import assert from 'node:assert/strict'
import { once } from 'node:events'
import { mkdtemp, mkdir, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import test from 'node:test'
import { createFrontendServer } from '../server/frontend.ts'

async function serveFixture(t) {
  const dir = await mkdtemp(join(tmpdir(), 'bossanalyze-static-'))
  t.after(() => rm(dir, { recursive: true, force: true }))
  await mkdir(join(dir, 'assets'))
  await writeFile(join(dir, 'assets', 'sample.txt'), '0123456789')
  await writeFile(join(dir, 'index.html'), '<main>Frontend shell</main>')
  const server = createFrontendServer(dir).listen(0, '127.0.0.1')
  await once(server, 'listening')
  t.after(() => new Promise((resolve, reject) => {
    server.close(error => error ? reject(error) : resolve())
  }))
  return (path, options) => fetch(`http://127.0.0.1:${server.address().port}${path}`, options)
}

test('single byte ranges serve the requested bytes and invalid ranges leave the server usable', async (t) => {
  const request = await serveFixture(t)
  const asset = '/assets/sample.txt'
  const first = await request(asset, { headers: { Range: 'bytes=0-0' } })
  assert.equal(first.status, 206)
  assert.equal(first.headers.get('content-range'), 'bytes 0-0/10')
  assert.equal(await first.text(), '0')

  const suffix = await request(asset, { headers: { Range: 'bytes=-2' } })
  assert.equal(suffix.status, 206)
  assert.equal(suffix.headers.get('content-range'), 'bytes 8-9/10')
  assert.equal(await suffix.text(), '89')

  for (const range of ['bytes=10-', 'bytes=5-2', 'bytes=999999999999999999999-']) {
    const invalid = await request(asset, { headers: { Range: range } })
    assert.equal(invalid.status, 416)
    assert.equal(invalid.headers.get('content-range'), 'bytes */10')
    await invalid.text()
  }

  const full = await request(asset, { headers: { Range: 'bytes=none' } })
  assert.equal(full.status, 200)
  assert.equal(await full.text(), '0123456789')
})

test('the production server serves the shell for dotted console HTML navigations', async (t) => {
  const request = await serveFixture(t)
  const headers = { Accept: 'text/html' }
  for (const path of ['/', '/console/team.v2?tab=overview', '/a-missing-page']) {
    const response = await request(path, { headers })
    assert.equal(response.status, 200)
    assert.equal(await response.text(), '<main>Frontend shell</main>')
  }
  const head = await request('/console/team.v2', { method: 'HEAD', headers })
  assert.equal(head.status, 200)
  assert.equal(await head.text(), '')
  for (const path of ['/assets/missing.js', '/assets/missing', '/missing.js']) {
    assert.equal((await request(path, { headers })).status, 404)
  }
  assert.equal((await request('/console/team.v2')).status, 404)
})
