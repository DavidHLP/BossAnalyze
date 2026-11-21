import { test } from 'node:test'
import assert from 'node:assert/strict'
import { authorizeRoute } from '../src/router/guard.ts'
import { loginDestination, safeReturnPath } from '../src/lib/session.ts'
import { assetMarkdown, countDuplicateCommits, exportDestination, resumeMainline, resumePreview } from '../src/lib/resumes.ts'
import { heatBuckets, heatLevel, ipDistribution, logTotals, weekdayAverages, weekdayDistribution } from '../src/lib/log-analysis.ts'

const dashboard = { title: '数据看板', path: '/console/dashboard', children: [] }
const logRoute = { fullPath: '/console/logs', path: '/console/logs', meta: { access: 'menu' } }
const session = menus => ({ restore: async () => true, state: { value: { menus } } })

test('ordinary signed-in users can return directly to an owned resume editor', async () => {
  const path = '/resumes/07abc09/edit'
  assert.equal(safeReturnPath(path), path)
  assert.equal(loginDestination(path, []), path)
  assert.equal(safeReturnPath('/resumes/some%2Fone/edit'), null)
  assert.equal(safeReturnPath('//other.example/resumes'), null)
  assert.equal(await authorizeRoute(session([]), { path, fullPath: path, meta: { access: 'session' } }), true)
})

test('the logs page accepts dashboard access and ignores a standalone logs menu grant', async () => {
  assert.equal(await authorizeRoute(session([dashboard]), logRoute), true)
  assert.equal(loginDestination('/console/logs', [dashboard]), '/console/logs')
  const isolated = { title: '扩展日志分析', path: '/console/logs', children: [] }
  assert.equal((await authorizeRoute(session([isolated]), logRoute)).params.code, '403')
  assert.equal(loginDestination('/console/logs', [isolated]), '/console')
})

test('resume preview preserves private image references without rendering executable markup', () => {
  const fileName = '1042_证书 (1).png'
  const blocks = resumePreview('# 标题\n<script>alert(0)</script>\n![远程](https://other.example/tracker)\n' + assetMarkdown(fileName))
  assert.equal(blocks[0].kind, 'heading')
  assert.equal(blocks[1].kind, 'text')
  assert.equal(blocks[2].kind, 'text')
  assert.equal(blocks[3].fileName, fileName)
  assert.equal(exportDestination('https://account:secret@example.invalid/export'), null)
  assert.equal(exportDestination('http://example.invalid/export'), null)
  assert.equal(exportDestination('https://example.invalid/export'), 'https://example.invalid/export')
})

test('reset eligibility follows only first parents and terminates on malformed cycles', () => {
  const commits = [{ id: 'head', parentCommits: ['left', 'right'] }, { id: 'left', parentCommits: ['head'] }]
  assert.deepEqual([...resumeMainline('head', commits)], ['head', 'left'])
  assert.equal(countDuplicateCommits([...commits, commits[0]]), 1)
})

test('log projections distinguish empty caches, missing buckets and observed zero', () => {
  assert.deepEqual(logTotals({ totalVisits: 0, uniqueIps: 0 }), { visits: 0, ips: 0 })
  assert.deepEqual(heatBuckets({}), { hours: [], minutes: [] })
  const data = heatBuckets({ hourHeatmap: {
    '2026-11-20 10': { count: 0 }, '2026-11-21 10': { count: 4 }, '2026-11-21 11': { count: 2 },
  } })
  assert.deepEqual(weekdayAverages(data.hours), { workdays: 0, weekends: 6 })
  assert.notEqual(heatLevel(undefined), heatLevel(0))
  assert.throws(() => heatBuckets({ minuteHeatmap: { '2026-11-20 10:61': { count: 1 } } }))
  assert.equal(weekdayDistribution({ weekdayStatistics: { weekdayStats: { 2: { count: 0 } } } }).days[0].count, 0)
  assert.equal(ipDistribution({ ipStats: { '203.0.113.1': { count: 1 } } }).countries[0].name, '未知')
})
