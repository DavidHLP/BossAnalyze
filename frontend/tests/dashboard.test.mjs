import assert from 'node:assert/strict'
import { after, test } from 'node:test'
import { mkdtemp, readFile, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { pathToFileURL } from 'node:url'
import { AxiosError } from 'axios'
import { dashboardSnapshot, peakVisits, visitShare } from '../src/lib/dashboard.ts'
import request from '../src/utils/request/request.ts'

const fixture = JSON.parse(await readFile(new URL('./fixtures/dashboard.json', import.meta.url)))

test('explicit no-content is unavailable, while real zero snapshots remain zero', () => {
  assert.equal(dashboardSnapshot(null), null)
  assert.throws(() => dashboardSnapshot({ overview: { totalVisits: 0, uniqueVisitors: 0 }, generatedAt: 1 }), /Invalid/)
  const zero = structuredClone(fixture)
  zero.overview = { totalVisits: 0, uniqueVisitors: 0 }
  zero.heatMapData.hourHeatmap = {}
  zero.topPages.pages = []
  assert.equal(dashboardSnapshot(zero).visits, 0)
})

test('snapshot sorts hourly buckets and rankings, without exposing backend sample metrics', () => {
  const data = dashboardSnapshot(fixture)
  assert.equal(data.generatedAt, '2026-10-30T16:00:00.000Z')
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

test('generation timestamps normalize cache milliseconds and retain valid string timestamps', () => {
  for (const generatedAt of ['2026-10-30T16:00:00', '2026-10-30T16:00:00+08:00']) {
    assert.equal(dashboardSnapshot({ ...fixture, generatedAt }).generatedAt, generatedAt)
  }
  assert.equal(
    dashboardSnapshot({ ...fixture, generatedAt: 0 }).generatedAt,
    '1970-01-01T00:00:00.000Z',
  )
  for (const generatedAt of [
    null,
    undefined,
    '',
    'not-a-date',
    NaN,
    Infinity,
    8640000000000001,
    1.5,
    {},
    [],
  ]) {
    assert.throws(() => dashboardSnapshot({ ...fixture, generatedAt }), /Invalid/)
  }
})

test('snapshot dates and hourly buckets must exist in the Gregorian calendar', () => {
  for (const date of ['2026-02-31', '2026-02-29', '1900-02-29', '2026-04-31', '2026-13-01']) {
    for (const suffix of ['T01:00:00', 'T01:00:00+08:00', ' 01:00:00 UTC']) {
      assert.throws(() => dashboardSnapshot({ ...fixture, generatedAt: `${date}${suffix}` }), /Invalid/)
    }
    const heatMapData = { hourHeatmap: { [`${date} 01`]: { count: 0 } } }
    assert.throws(() => dashboardSnapshot({ ...fixture, heatMapData }), /Invalid dashboard hour/)
  }

  for (const date of ['2000-02-29', '2028-02-29', '2026-04-30']) {
    const generatedAt = `${date}T01:00:00+08:00`
    const label = `${date} 01`
    const heatMapData = { hourHeatmap: { [label]: { count: 0 } } }
    const snapshot = dashboardSnapshot({ ...fixture, generatedAt, heatMapData })
    assert.equal(snapshot.generatedAt, generatedAt)
    assert.deepEqual(snapshot.hours, [{ label, count: 0 }])
  }
})

test('malformed snapshots and invalid counts fail instead of implying no data', () => {
  for (const value of [undefined, {}, { overview: { totalVisits: '0', uniqueVisitors: 0 } }]) {
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
  await readFile(new URL('../src/api/log/accesslog.ts', import.meta.url), 'utf8')
)
  .replace(
    "'@/utils/request/request'",
    JSON.stringify(new URL('../src/utils/request/request.ts', import.meta.url).href),
  )
  .replace(
    "'@/lib/settings'",
    JSON.stringify(new URL('../src/lib/settings.ts', import.meta.url).href),
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
  request.defaults.adapter = async (config) => ({ config, status: 200, headers: {}, data: null })
  await assert.rejects(api.getDashboardStats(), { code: 200, data: null, resultUnknown: true })
  request.defaults.adapter = async (config) => ({ config, status: 204, headers: {}, data: '' })
  assert.equal(await api.getDashboardStats(), null)
  for (const status of [403, 503]) {
    request.defaults.adapter = async (config) => {
      throw new AxiosError('Unavailable', 'ERR_BAD_RESPONSE', config, undefined, {
        config,
        status,
        headers: {},
        data: { message: 'Unavailable' },
      })
    }
    await assert.rejects(api.getDashboardStats(), (error) => error.code === status)
  }
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

test('health API accepts a validated 503 degradation and preserves forbidden and malformed errors', async () => {
  globalThis.window = { location: { href: '/' } }
  globalThis.localStorage = { getItem: () => 'fixture-token' }
  const health = { status: 'DEGRADED', sparkService: 'DOWN', timestamp: 1793380000000 }
  const rejectWith = (status, data) => {
    request.defaults.adapter = async (config) => {
      throw new AxiosError('Request failed', 'ERR_BAD_RESPONSE', config, undefined, {
        config, status, headers: {}, data,
      })
    }
  }
  rejectWith(503, { ...health, error: 'internal detail' })
  assert.deepEqual(await api.checkServiceHealth(), health)
  rejectWith(403, health)
  await assert.rejects(api.checkServiceHealth(), (error) => error.code === 403)
  rejectWith(503, { ...health, timestamp: 0 })
  await assert.rejects(api.checkServiceHealth(), (error) => error.code === 503)
  rejectWith(503, { ...health, status: 'UP', sparkService: 'UP' })
  await assert.rejects(api.checkServiceHealth(), (error) => error.code === 503)
})

test('peak uses the supplied snapshot buckets and shares keep a zero denominator distinct', () => {
  const hours = [{ label: 'old', count: 100 }, ...Array.from({ length: 24 }, () => ({ count: 5 }))]
  assert.equal(peakVisits(hours), 100)
  assert.equal(peakVisits(hours.slice(-24)), 5)
  assert.equal(peakVisits([]), 0)
  assert.equal(visitShare(10, 0).toFixed(1), '0.0')
  assert.equal(visitShare(1, 3).toFixed(1), '33.3')
})

test('snapshot timestamps share an explicit timezone policy without inventing server timezone', async () => {
  const { snapshotTime } = await import('../src/lib/dashboard.ts')
  assert.equal(snapshotTime('2026-10-30T16:00:00'), '2026-10-30 16:00:00（服务端时间，时区未提供）')
  assert.equal(snapshotTime('2026-10-30T16:00:00+08:00'), '2026-10-30 08:00:00.000Z（UTC）')
  assert.equal(snapshotTime('2026-10-30T16:00:00+0800'), '2026-10-30 08:00:00.000Z（UTC）')
  assert.equal(snapshotTime('2026-10-30T08:00:00.000Z'), '2026-10-30 08:00:00.000Z（UTC）')
})
