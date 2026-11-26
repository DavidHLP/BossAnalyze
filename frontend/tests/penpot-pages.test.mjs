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

test('external resume export requires an explicit HTTPS result that can be opened', async () => {
  const { resumeExportResult } = await import('../src/lib/resumes.ts')
  for (const payload of [{}, { success: true }, { url: 'http://example.invalid/file' },
    { url: 'https://user@example.invalid/file' }, { success: false, url: 'https://example.invalid/file' }]) {
    assert.equal(resumeExportResult(payload), null)
  }
  assert.equal(resumeExportResult({ downloadUrl: 'https://example.invalid/resume.pdf' }),
    'https://example.invalid/resume.pdf')
})

test('resume Markdown structures lists, code and tables and links only allowed protocols', () => {
  const blocks = resumePreview([
    '3. *经验*', '4. `Vue`', '', '| 技能 | 年限 |', '| --- | --- |', '| **Vue** | 3 |',
    '', '```html', '<script>plain code</script>', '```', '',
    '[网站](https://example.invalid) [文档](http://example.invalid) [邮箱](mailto:me@example.invalid)',
    '[危险](javascript:alert) [数据](data:text/html,unsafe) ![追踪](https://other.invalid/logo)',
  ].join('\n'))
  assert.deepEqual(blocks.map(block => block.kind), ['list', 'table', 'code', 'text', 'text'])
  assert.equal(blocks[0].start, 3)
  assert.deepEqual(blocks[0].items, [[{ kind: 'em', text: '经验' }], [{ kind: 'code', text: 'Vue' }]])
  assert.equal(blocks[1].rows[0][0][0].kind, 'strong')
  assert.equal(blocks[2].text, '<script>plain code</script>')
  assert.deepEqual(blocks[3].inline.filter(part => part.kind === 'link').map(part => part.href),
    ['https://example.invalid/', 'http://example.invalid/', 'mailto:me@example.invalid'])
  assert.equal(blocks[4].inline.filter(part => part.kind === 'link').length, 0)
})

test('standalone resume HTML escapes text and has A4 styles without automatic resource loads', async () => {
  const { resumeHtml } = await import('../src/lib/resumes.ts')
  const html = resumeHtml('</title><script>bad</script>', [
    '# 简历', '[链接](https://example.invalid/?a=1&b=2)', '![](<https://example.invalid/image>)',
    assetMarkdown('private.png'), '<img src="https://example.invalid/track">', '1. `Vue`',
  ].join('\n'))
  assert.match(html, /@page\s*\{\s*size:\s*A4/)
  assert.match(html, /<title>&lt;\/title&gt;&lt;script&gt;bad&lt;\/script&gt;<\/title>/)
  assert.match(html, /<ol start="1"><li><code>Vue<\/code><\/li><\/ol>/)
  assert.match(html, /href="https:\/\/example.invalid\/\?a=1&amp;b=2"/)
  assert.doesNotMatch(html, /<(?:script|img|link)\b/)
})

test('snapshot comparison follows first parents and preserves added, removed and unchanged lines', async () => {
  const { resumeFirstParentRelation, resumeLineDiff } = await import('../src/lib/resumes.ts')
  const commits = [
    { id: 'merge', parentCommits: ['main', 'side'] },
    { id: 'side', parentCommits: ['main'] },
    { id: 'main', parentCommits: [] },
  ]
  assert.equal(resumeFirstParentRelation('side', 'merge', commits), 'separate')
  assert.equal(resumeFirstParentRelation('main', 'merge', commits), 'forward')
  assert.equal(resumeFirstParentRelation('merge', 'main', commits), 'reverse')
  assert.deepEqual(resumeLineDiff('标题\r\n旧稿\r\n技能\r\n', '标题\n新稿\n技能\n').lines, [
    { kind: 'unchanged', text: '标题' }, { kind: 'removed', text: '旧稿' },
    { kind: 'added', text: '新稿' }, { kind: 'unchanged', text: '技能' },
  ])
  assert.deepEqual(resumeLineDiff('', '新稿').lines, [{ kind: 'added', text: '新稿' }])
})

test('resume list readback confirms the exact write and ignores pre-existing namesakes', async () => {
  const { resumeListReadback } = await import('../src/lib/resumes.ts')
  const list = [{ id: 'old', title: '简历' }, { id: 'new', title: '简历' }]
  const unknown = { kind: 'create', title: '简历', previousIds: new Set(['old']) }
  assert.equal(resumeListReadback(unknown, list.slice(0, 1)), false)
  assert.equal(resumeListReadback(unknown, list), true)
  assert.equal(resumeListReadback({ ...unknown, id: 'expected' }, list), false)
  assert.equal(resumeListReadback({ kind: 'rename', id: 'new', title: '新标题' }, list), false)
  assert.equal(resumeListReadback({ kind: 'delete', id: 'new' }, list.slice(0, 1)), true)
})

test('local resume drafts are confined to the current login and resume and can be discarded', async () => {
  const { resumeDraftStore } = await import('../src/lib/resumes.ts')
  const items = new Map()
  const storage = {
    get length() { return items.size },
    key: index => [...items.keys()][index] ?? null,
    getItem: key => items.get(key) ?? null,
    setItem: (key, value) => items.set(key, value),
    removeItem: key => items.delete(key),
  }
  const draft = { content: '# 未保存正文', message: '备注' }
  resumeDraftStore(storage, 'alice@example.invalid', '1', 'a').write(draft)
  assert.equal(resumeDraftStore(storage, 'alice@example.invalid', '1', 'b').read(), null)
  const same = resumeDraftStore(storage, 'alice@example.invalid', '1', 'a')
  assert.deepEqual(same.read(), draft)
  same.clear()
  assert.equal(same.read(), null)
  same.write(draft)
  assert.equal(resumeDraftStore(storage, 'bob@example.invalid', '2', 'a').read(), null)
  assert.equal(resumeDraftStore(storage, 'alice@example.invalid', '3', 'a').read(), null)
  assert.equal(items.size, 0)
})
