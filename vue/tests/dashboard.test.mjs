import assert from 'node:assert/strict'
import { after, test } from 'node:test'
import { mkdtemp, readFile, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { pathToFileURL } from 'node:url'
import { AxiosError } from 'axios'
import { dashboardSnapshot } from '../app/lib/dashboard.ts'
import request from '../app/utils/request/request.ts'

const fixture = JSON.parse(await readFile(new URL('./fixtures/dashboard.json', import.meta.url)))

test('gateway defaults are unavailable snapshots, while real zero snapshots remain zero', () => {
  assert.equal(
    dashboardSnapshot({ overview: { totalVisits: 0, uniqueVisitors: 0 }, generatedAt: 1 }),
    null,
  )
  const zero = structuredClone(fixture)
  zero.overview = { totalVisits: 0, uniqueVisitors: 0 }
  zero.heatMapData.hourHeatmap = {}
  zero.topPages.pages = []
  assert.equal(dashboardSnapshot(zero).visits, 0)
})

test('snapshot sorts hourly buckets and rankings, without exposing backend sample metrics', () => {
  const data = dashboardSnapshot(fixture)
  assert.equal(data.visits, 240)
  assert.equal(data.visitors, 81)
  assert.equal(data.hours[0].label, '2026-10-30 08')
  assert.equal(data.hours.at(-1).label, '2026-10-30 15')
  assert.equal(Math.max(...data.hours.map((row) => row.count)), 64)
  assert.equal(data.pages[0].label, '/console/dashboard')
  assert.deepEqual(
    data.browsers.map((row) => row.label),
    ['Chrome', 'Safari', 'Firefox'],
  )
  assert.equal('realTimeStats' in data, false)
  assert.equal('performanceStats' in data, false)
  assert.equal('securityStats' in data, false)
  assert.equal('operatingSystems' in data, false)
  assert.equal(fixture.topPages.pages[0].path, '/console/profile')
})

test('malformed snapshots and invalid counts fail instead of implying no data', () => {
  for (const value of [null, {}, { overview: { totalVisits: '0', uniqueVisitors: 0 } }]) {
    assert.throws(() => dashboardSnapshot(value), /Invalid/)
  }
  for (const invalid of [-1, NaN, Infinity, '10', null]) {
    const data = structuredClone(fixture)
    data.heatMapData.hourHeatmap['2026-10-30 08'].count = invalid
    assert.throws(() => dashboardSnapshot(data), /Invalid/)
  }
  const partial = structuredClone(fixture)
  delete partial.topPages
  assert.throws(() => dashboardSnapshot(partial), /Invalid/)
})

// Resolve the Nuxt alias for Node while executing the actual API wrapper.
const source = (
  await readFile(new URL('../app/api/log/accesslog.ts', import.meta.url), 'utf8')
).replace(
  "'@/utils/request/request'",
  JSON.stringify(new URL('../app/utils/request/request.ts', import.meta.url).href),
)
const temporary = await mkdtemp(join(tmpdir(), 'bossanalyze-dashboard-test-'))
await writeFile(join(temporary, 'package.json'), '{"type":"module"}')
await writeFile(join(temporary, 'accesslog.ts'), source)
const api = await import(pathToFileURL(join(temporary, 'accesslog.ts')).href)
after(async () => {
  delete globalThis.window
  delete globalThis.localStorage
  await rm(temporary, { recursive: true, force: true })
})

test('dashboard API preserves response and propagates transport errors for retry UI', async () => {
  globalThis.window = { location: { href: '/' } }
  globalThis.localStorage = { getItem: () => 'fixture-token' }
  request.defaults.adapter = async (config) => {
    assert.equal(config.url, '/api/v1/system/logs/dashboard/stats')
    assert.equal(config.method, 'get')
    return { config, status: 200, headers: {}, data: fixture }
  }
  assert.deepEqual(await api.getDashboardStats(), fixture)
  request.defaults.adapter = async (config) => {
    throw new AxiosError('Unavailable', 'ERR_BAD_RESPONSE', config, undefined, {
      config,
      status: 503,
      headers: {},
      data: { message: 'Unavailable' },
    })
  }
  await assert.rejects(api.getDashboardStats(), (error) => error.code === 503)
})

test('health API preserves a degraded response and propagates transport failures', async () => {
  globalThis.window = { location: { href: '/' } }
  globalThis.localStorage = { getItem: () => 'fixture-token' }
  const health = { status: 'DEGRADED', sparkService: 'DOWN', timestamp: 1793380000000 }
  request.defaults.adapter = async (config) => {
    assert.equal(config.url, '/api/v1/system/logs/health')
    assert.equal(config.method, 'get')
    return { config, status: 200, headers: {}, data: health }
  }
  assert.deepEqual(await api.checkServiceHealth(), health)
  request.defaults.adapter = async (config) => {
    throw new AxiosError('Unavailable', 'ERR_BAD_RESPONSE', config, undefined, {
      config,
      status: 503,
      headers: {},
      data: { message: 'Unavailable' },
    })
  }
  await assert.rejects(api.checkServiceHealth(), (error) => error.code === 503)
})
