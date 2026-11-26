import assert from 'node:assert/strict'
import { after, test } from 'node:test'
import { mkdtemp, readFile, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { pathToFileURL } from 'node:url'
import { AxiosError } from 'axios'
import { dashboardSnapshot, formatPercentage, peakVisits, visitShare } from '../src/lib/dashboard.ts'
import { heatBuckets, ipDistribution, logReadStatus, logTotals, weekdayDistribution } from '../src/lib/log-analysis.ts'
import request from '../src/utils/request/request.ts'

const fixture = JSON.parse(await readFile(new URL('./fixtures/dashboard.json', import.meta.url)))

test('extended log adapters reject missing fields while accepting published empty snapshots', () => {
  for (const adapt of [logTotals, heatBuckets, ipDistribution, weekdayDistribution]) {
    assert.throws(() => adapt({}), /Invalid/)
  }
  assert.throws(() => logTotals({ totalVisits: 0 }), /Invalid/)
  assert.throws(() => heatBuckets({ hourHeatmap: {} }), /Invalid/)
  assert.throws(() => heatBuckets({ minuteHeatmap: {} }), /Invalid/)
  assert.throws(() => ipDistribution({ ipStats: null }), /Invalid/)
  assert.throws(() => weekdayDistribution({ generatedAt: fixture.generatedAt }), /Invalid/)
  assert.throws(() => weekdayDistribution({ generatedAt: fixture.generatedAt, weekdayStatistics: {} }), /Invalid/)
  assert.throws(() => weekdayDistribution({ generatedAt: '2026-02-31T12:00:00', weekdayStatistics: { weekdayStats: {} } }), /Invalid/)

  assert.deepEqual(logTotals({ totalVisits: 0, uniqueIps: 0 }), { visits: 0, ips: 0 })
  assert.deepEqual(heatBuckets({ hourHeatmap: {}, minuteHeatmap: {} }), { hours: [], minutes: [] })
  assert.deepEqual(ipDistribution({ ipStats: {} }).records, [])
  const weekday = weekdayDistribution({ generatedAt: fixture.generatedAt, weekdayStatistics: { weekdayStats: {} } })
  assert.equal(weekday.time, '2026-10-30T16:00:00.000Z')
  assert.ok(weekday.days.length === 7 && weekday.days.every(day => day.count === null))
})

test('extended log status reflects every requested section, including denied reads', () => {
  const status = (...results) => logReadStatus(results.map(result => ({ result, loading: false })))
  assert.equal(status(null, null, null, null), '')
  assert.equal(logReadStatus([{ result: 'updated', loading: true }]), '正在读取日志缓存…')
  assert.equal(status('updated', 'updated', 'updated', 'updated'), '已读取全部日志分项')
  assert.equal(status('unchanged', 'updated', 'updated', 'updated'), '已读取全部日志分项，星期快照暂无更新')
  assert.equal(status('updated', 'empty', 'updated', 'unchanged'), '部分日志分项暂无快照')
  assert.equal(status('updated', 'error', 'updated', 'updated'), '部分日志分项读取失败')
  assert.equal(status('empty', 'updated', 'error', 'empty'), '部分日志分项暂无快照或读取失败')
  assert.equal(status('empty', 'empty', 'empty', 'empty'), '暂无可用的日志快照')
  assert.equal(status('error', 'empty', 'error', 'error'), '未能读取日志快照，请重试')
  assert.equal(status('forbidden', 'updated', 'empty', 'error'), '部分日志分项读取被拒绝（403）')
  assert.equal(status('forbidden', 'forbidden', 'forbidden', 'forbidden'), '日志分项读取均被拒绝（403）')
})

test('IP counts group cities within each country and retain province and network metadata', () => {
  const data = ipDistribution({ ipStats: {
    '203.0.113.1': { countryName: '美国', city: '波特兰', region: '俄勒冈州', isLocal: false, count: 5 },
    '203.0.113.2': { countryName: ' 美国 ', city: '波特兰', count: 4 },
    '198.51.100.1': { countryName: '澳大利亚', city: '波特兰', province: '维多利亚州', isLocal: true, count: 3 },
  } })
  const namedCounts = rows => rows.map(({ name, count }) => [name, count])
  assert.deepEqual(namedCounts(data.cities), [['波特兰（美国）', 9], ['波特兰（澳大利亚）', 3]])
  assert.deepEqual(namedCounts(data.countries), [['美国', 9], ['澳大利亚', 3]])
  assert.deepEqual(data.records.map(({ province, isLocal }) => [province, isLocal]), [
    ['俄勒冈州', false], [null, null], ['维多利亚州', true],
  ])
})

test('weekday rows preserve published percentages, day types and hourly averages, including zero', () => {
  const data = weekdayDistribution({
    generatedAt: fixture.generatedAt,
    overview: { totalVisits: 48 },
    weekdayStatistics: { weekdayStats: {
      2: { count: 0, percentage: 0, isWorkday: true, isWeekend: false, avgHourlyVisits: 0 },
      1: { count: 18, percentage: 37.5, isWorkday: false, isWeekend: true, avgHourlyVisits: 0.75 },
    } },
  })
  assert.deepEqual(data.days[0], { name: '周一', count: 0, percentage: 0, isWorkday: true, isWeekend: false, avgHourlyVisits: 0 })
  assert.deepEqual(data.days.at(-1), { name: '周日', count: 18, percentage: 37.5, isWorkday: false, isWeekend: true, avgHourlyVisits: 0.75 })
  assert.deepEqual(data.days[1], { name: '周二', count: null, percentage: null, isWorkday: null, isWeekend: null, avgHourlyVisits: null })
})

test('weekday percentage is unavailable when the snapshot reports zero visits', () => {
  const data = weekdayDistribution({
    generatedAt: fixture.generatedAt,
    overview: { totalVisits: 0 },
    weekdayStatistics: { weekdayStats: { 2: { count: 0, percentage: 0 } } },
  })
  assert.equal(data.days[0].count, 0)
  assert.equal(formatPercentage(data.days[0].percentage), '—')
})

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

test('dashboard exposes published country totals and HTTP method metadata when available', () => {
  const snapshot = dashboardSnapshot({
    ...fixture,
    geoStats: { countryStats: { 中国: { visits: 38 }, 巴西: { visits: 55 } } },
    httpMethodStatistics: { methodStats: {
      GET: { count: 200, description: '获取资源', isSafe: true, isIdempotent: true },
      POST: { count: 40, isSafe: false, isIdempotent: false },
    } },
  })
  assert.deepEqual(snapshot.countries, [{ label: '巴西', count: 55 }, { label: '中国', count: 38 }])
  assert.deepEqual(snapshot.methods, [
    { label: 'GET', count: 200, detail: '获取资源 · 安全方法 · 幂等' },
    { label: 'POST', count: 40, detail: '非安全方法 · 非幂等' },
  ])
  assert.deepEqual(dashboardSnapshot(fixture).countries, [])
  assert.deepEqual(dashboardSnapshot({ ...fixture, geoStats: { countryStats: {} } }).countries, [])
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

test('empty buckets have no peak and zero denominators have no share', () => {
  const hours = [{ label: 'old', count: 100 }, ...Array.from({ length: 24 }, () => ({ count: 5 }))]
  assert.equal(peakVisits(hours), 100)
  assert.equal(peakVisits(hours.slice(-24)), 5)
  assert.equal(peakVisits([]), null)
  assert.equal(peakVisits([{ label: '2026-10-30 08', count: 0 }]), 0)
  assert.equal(visitShare(10, 0), null)
  assert.equal(formatPercentage(visitShare(10, 0)), '—')
  assert.equal(formatPercentage(visitShare(0, 3)), '0.0%')
  assert.equal(formatPercentage(visitShare(1, 3)), '33.3%')
})

test('snapshot timestamps share an explicit timezone policy without inventing server timezone', async () => {
  const { snapshotTime } = await import('../src/lib/dashboard.ts')
  assert.equal(snapshotTime('2026-10-30T16:00:00'), '2026-10-30 16:00:00（服务端时间，时区未提供）')
  assert.equal(snapshotTime('2026-10-30T16:00:00+08:00'), '2026-10-30 08:00:00.000Z（UTC）')
  assert.equal(snapshotTime('2026-10-30T16:00:00+0800'), '2026-10-30 08:00:00.000Z（UTC）')
  assert.equal(snapshotTime('2026-10-30T08:00:00.000Z'), '2026-10-30 08:00:00.000Z（UTC）')
})
