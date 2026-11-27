import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
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
  assert.throws(() => heatBuckets({}), /Invalid log response/)
  assert.deepEqual(heatBuckets({ hourHeatmap: {}, minuteHeatmap: {} }), { hours: [], minutes: [] })
  const data = heatBuckets({ minuteHeatmap: {}, hourHeatmap: {
    '2026-11-20 10': { count: 0 }, '2026-11-21 10': { count: 4 }, '2026-11-21 11': { count: 2 },
  } })
  assert.deepEqual(weekdayAverages(data.hours), { workdays: 0, weekends: 6 })
  assert.notEqual(heatLevel(undefined), heatLevel(0))
  assert.throws(() => heatBuckets({ hourHeatmap: {}, minuteHeatmap: { '2026-11-20 10:61': { count: 1 } } }), /Invalid bucket time/)
  const weekday = weekdayDistribution({
    generatedAt: '2026-11-20T10:00:00Z',
    overview: { totalVisits: 0 },
    weekdayStatistics: { weekdayStats: { 2: { count: 0 } } },
  })
  assert.equal(weekday.days[0].count, 0)
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
  const content = '# 简历\n\n正文'
  const list = [
    { id: 'old', title: '简历', content: '# 旧简历' },
    { id: 'new', title: '简历', content },
  ]
  const unknown = { kind: 'create', title: '简历', content, previousIds: new Set(['old']) }
  assert.equal(resumeListReadback(unknown, list.slice(0, 1)), false)
  assert.equal(resumeListReadback(unknown, list), true)
  assert.equal(resumeListReadback(unknown, [{ ...list[1], content: '# 其他正文' }]), false)
  assert.equal(resumeListReadback({ ...unknown, id: 'new' }, list), true)
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
  const draft = { content: '# 未保存正文', message: '备注', baseHeadCommit: null }
  resumeDraftStore(storage, 'alice@example.invalid', '1', 'a').write(draft)
  assert.equal(resumeDraftStore(storage, 'alice@example.invalid', '1', 'b').read(), null)
  const same = resumeDraftStore(storage, 'alice@example.invalid', '1', 'a')
  assert.deepEqual(same.read(), draft)
  same.clear()
  assert.equal(same.read(), null)
  same.write(draft)
  assert.equal(resumeDraftStore(storage, 'bob@example.invalid', '2', 'a').read(), null)
  assert.equal(resumeDraftStore(storage, 'alice@example.invalid', '3', 'a').read(), null)
  // Reading another scope must not delete the remaining draft.
  assert.equal(items.size, 1)
})

async function resumeScript(file, bindings, names) {
  const vue = await import('vue')
  const ts = await import('typescript')
  const url = new URL(`../src/${file}.vue`, import.meta.url)
  const source = await readFile(url, 'utf8')
  const script = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
    .replace(/^import[\s\S]*?from ['"][^'"]+['"]\s*$/gm, '')
    .replaceAll('import.meta.env', 'env')
    .replaceAll('import.meta.url', JSON.stringify(url.href))
  const { outputText } = ts.transpileModule(script, {
    compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ESNext },
  })
  return new Function('bindings', `with (bindings) { ${outputText.replace(/^export \{\};$/m, '')}\nreturn { ${names.join(',')} } }`)({
    ...vue, ...await import('../src/lib/resumes.ts'),
    env: {}, onMounted() {}, onUnmounted() {}, onBeforeRouteLeave() {}, onBeforeRouteUpdate() {},
    useRouter: () => ({ push: async () => {} }),
    useResumeImages: () => ({ images: vue.ref({}) }),
    ...bindings,
  })
}

const stashEntry = (content, timestamp = 1) => ({ content, branch: 'main', head: 'head', timestamp })

async function resumeEditor(savedDraft, stashStack = [stashEntry('# 旧储藏')], options = {}) {
  const { ref } = await import('vue')
  const values = new Map()
  const storage = options.storage ?? {
    get length() { return values.size },
    key: index => [...values.keys()][index] ?? null,
    getItem: key => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value),
    removeItem: key => values.delete(key),
  }
  const { resumeDraftStore } = await import('../src/lib/resumes.ts')
  const store = resumeDraftStore(storage, 'id:1', '100', 'fixture')
  if (savedDraft) store.write({ baseHeadCommit: null, ...savedDraft })
  const crossTabChanged = ref(null)
  const owner = { token: 'fixture-token', timestamp: '100' }
  const server = {
    item: { id: 'fixture', content: '# 已保存', currentBranch: 'main', headCommit: 'head', stashStack },
    writes: [],
    failRead: false,
  }
  const editor = await resumeScript('pages/resumes/edit', {
    useRoute: () => ({ params: { id: 'fixture' } }),
    useRouter: () => ({ push: async () => {} }),
    useSession: () => ({
      state: ref({ user: { id: 1 } }), crossTabChanged,
      capture: () => owner, owns: () => !crossTabChanged.value,
    }),
    window: { sessionStorage: storage },
    document: { getElementById: () => null },
    getResume: async () => {
      if (server.failRead) throw { code: 503 }
      return server.item
    },
    resumeHistory: async () => [],
    changeResumeVersion: async (...args) => {
      server.writes.push(args)
      if (args[1] === 'commit') {
        server.item = { ...server.item, content: args[2].content, headCommit: 'commit-1' }
        return 'commit-1'
      }
    },
  }, ['load', 'write', 'resolveLocalDraft', 'markdown', 'message', 'localDraft', 'writeError', 'writeLocked',
    'feedback', 'flushDraft', 'draftSavedAt', 'draftUnavailable', 'savedAt', 'savedCommit',
    'restoreServerContent', 'restoreError', 'restorePending'])
  await editor.load(true)
  return { editor, server, store, crossTabChanged }
}

test('unrelated new stashes and an existing match leave the editor draft locked', async () => {
  const draft = { content: '# 未保存正文', message: '备注', baseHeadCommit: 'head' }
  const existing = stashEntry(draft.content)
  const { editor, server, store } = await resumeEditor(undefined, [existing])
  editor.markdown.value = draft.content
  editor.message.value = draft.message
  editor.flushDraft()
  server.item = { ...server.item, stashStack: [
    stashEntry('# 其他正文', 2),
    { ...stashEntry(draft.content, 3), branch: 'other' },
    { ...stashEntry(draft.content, 4), head: 'other' },
    { ...existing },
  ] }
  await editor.write('stash')
  assert.deepEqual(server.writes, [['fixture', 'stash', { content: draft.content }, undefined]])
  assert.deepEqual(store.read(), draft)
  assert.equal(editor.markdown.value, draft.content)
  assert.equal(editor.message.value, draft.message)
  assert.equal(editor.feedback.value, '')
  assert.match(editor.writeError.value, /暂存结果未确认，请先核对暂存记录/)
  assert.equal(editor.writeLocked.value, true)
  await editor.write('stash')
  assert.equal(server.writes.length, 1)
})

test('the editor confirms a new matching stash after another tab pops the previous entry', async () => {
  const content = '# 未保存正文'
  const { editor, server, store } = await resumeEditor(undefined, [stashEntry(content)])
  editor.markdown.value = content
  editor.message.value = '  '
  server.item = { ...server.item, stashStack: [stashEntry(content, 2)] }
  await editor.write('stash')
  assert.equal(store.read(), null)
  assert.equal(editor.markdown.value, '# 已保存')
  assert.match(editor.feedback.value, /草稿已暂存/)
})

test('the editor offers the remaining commit remark after reloading a successful stash', async () => {
  const { editor, server } = await resumeEditor()
  const remark = ' 本地备注 '
  editor.markdown.value = '# 未保存正文'
  editor.message.value = remark
  server.item = { ...server.item, stashStack: [stashEntry(editor.markdown.value, 2), ...server.item.stashStack] }

  await editor.write('stash')
  assert.equal(editor.message.value, remark)
  await editor.load(true)

  assert.deepEqual(editor.localDraft.value, { content: '# 已保存', message: remark, baseHeadCommit: 'head' })
})

test('renaming a resume cancels local HTML and print jobs waiting for the math renderer', async () => {
  const { reactive, ref } = await import('vue')
  const props = reactive({ title: '旧标题', content: '$x$' })
  const engine = Promise.withResolvers()
  const exportPanel = await resumeScript('components/resumes/ResumeExport', {
    defineProps: () => props,
    defineModel: () => ref(true),
    defineEmits: () => () => {},
    loadResumeMath: () => engine.promise,
    document: { createElement: () => assert.fail('The stale export must not create a download or print frame') },
  }, ['exportHtml', 'htmlBusy'])

  const html = exportPanel.exportHtml('html')
  assert.equal(exportPanel.htmlBusy.value, true)
  props.title = '新标题'
  assert.equal(exportPanel.htmlBusy.value, false)
  const print = exportPanel.exportHtml('pdf')
  assert.equal(exportPanel.htmlBusy.value, true)
  props.title = '最终标题'
  engine.resolve({ renderOrNull: () => '<math></math>' })
  await Promise.all([html, print])
  assert.equal(exportPanel.htmlBusy.value, false)
})

test('a stale tab can recover or clear the offered local draft while both writes stay blocked', async () => {
  const draft = { content: '# 本地正文', message: '本地备注', baseHeadCommit: null }
  for (const restore of [true, false]) {
    const { editor, server, store, crossTabChanged } = await resumeEditor(draft)
    crossTabChanged.value = 'changed'
    await editor.resolveLocalDraft(restore)
    assert.equal(editor.localDraft.value, null)
    assert.equal(editor.markdown.value, restore ? draft.content : '# 已保存')
    assert.equal(editor.message.value, restore ? draft.message : '')
    assert.deepEqual(store.read(), restore ? draft : null)
    await editor.write('save')
    await editor.write('stash')
    assert.deepEqual(server.writes, [])
  }
})

test('the editor debounces local drafts and reports only successful writes', async () => {
  const { editor, store } = await resumeEditor()
  editor.markdown.value = '# 新正文'
  assert.equal(store.read(), null)
  assert.equal(editor.draftSavedAt.value, '')
  editor.flushDraft()
  assert.deepEqual(store.read(), { content: '# 新正文', message: '', baseHeadCommit: 'head' })
  assert.match(editor.draftSavedAt.value, /^\d{2}:\d{2}$/)
})

test('blocked session storage reports drafts unavailable while editing continues', async () => {
  const blocked = {
    get length() { return 0 },
    key: () => null,
    getItem: () => { throw new Error('blocked') },
    setItem: () => { throw new Error('blocked') },
    removeItem: () => { throw new Error('blocked') },
  }
  const { editor } = await resumeEditor(undefined, [stashEntry('# 旧储藏')], { storage: blocked })
  assert.equal(editor.draftUnavailable.value, true)
  editor.markdown.value = '# 仍可编辑'
  editor.flushDraft()
  assert.equal(editor.markdown.value, '# 仍可编辑')
  assert.equal(editor.draftSavedAt.value, '')
})

test('the editor shows saved status and clears the draft only after readback', async () => {
  const { editor, store } = await resumeEditor()
  editor.markdown.value = '# 待提交正文'
  editor.message.value = '备注'
  editor.flushDraft()
  assert.ok(store.read())
  await editor.write('save')
  assert.equal(editor.savedCommit.value, 'commit-1')
  assert.match(editor.savedAt.value, /^\d{2}:\d{2}$/)
  assert.equal(store.read(), null)
  assert.equal(editor.localDraft.value, null)
})

test('restoring server content keeps the input when the read fails', async () => {
  const { editor, server, store } = await resumeEditor()
  editor.markdown.value = '# 本地修改'
  editor.flushDraft()
  server.failRead = true
  await editor.restoreServerContent(false)
  assert.equal(editor.markdown.value, '# 本地修改')
  assert.match(editor.restoreError.value, /暂时无法读取简历/)
  assert.ok(store.read())

  server.failRead = false
  server.item = { ...server.item, content: '# 服务器正文' }
  await editor.restoreServerContent(false)
  assert.equal(editor.markdown.value, '# 服务器正文')
  assert.equal(editor.restoreError.value, '')
  assert.deepEqual(server.writes, [])
  assert.equal(store.read(), null)
})

async function versionsPage({ getResume, resumeHistory }) {
  const { reactive, ref } = await import('vue')
  const route = reactive({ params: { id: 'first' } })
  const owner = { token: 'fixture-token', timestamp: '100' }
  let routeGuard
  const versions = await resumeScript('pages/resumes/versions', {
    useRoute: () => route, useRouter: () => ({}), useMediaQuery: () => ref(false),
    useSession: () => ({ state: ref({}), capture: () => owner, owns: () => true }),
    onBeforeRouteUpdate: guard => { routeGuard = guard },
    getResume, resumeHistory,
  }, ['refresh', 'record', 'history', 'recent', 'mode', 'busy', 'loadError', 'detailError', 'writeLocked'])
  return { versions, route, routeGuard }
}

test('versions refresh keeps fresh primary history and writes available when recent commits fail', async () => {
  const old = { id: 'old' }
  const current = { id: 'current' }
  let commit = old
  let recentFails = false
  const { versions } = await versionsPage({
    getResume: async (id) => ({ id, headCommit: commit.id }),
    resumeHistory: async (_id, recent) => {
      if (recent && recentFails) throw { code: 503 }
      return [commit]
    },
  })
  await versions.refresh(true)
  versions.mode.value = 'recent'
  recentFails = true
  commit = current
  await versions.refresh()
  assert.equal(versions.record.value.headCommit, 'current')
  assert.deepEqual(versions.history.value, [current])
  assert.equal(versions.loadError.value, '')
  assert.equal(versions.writeLocked.value, false)
  assert.ok(versions.detailError.value)
  recentFails = false
  await versions.refresh()
  assert.deepEqual(versions.recent.value, [current])
  assert.equal(versions.detailError.value, '')
})

test('versions switches resumes during a read, ignores the old failure, and guards writes', async () => {
  const first = Promise.withResolvers()
  const second = Promise.withResolvers()
  const { versions, route, routeGuard } = await versionsPage({
    getResume: id => id === 'first' ? first.promise : second.promise,
    resumeHistory: async id => [{ id: `${id}-head` }],
  })
  const pending = versions.refresh(true)
  const next = { params: { id: 'second' } }
  assert.equal(versions.busy.value, 'load')
  assert.equal(routeGuard(next, route), true)
  route.params = next.params
  const { nextTick } = await import('vue')
  await nextTick()
  first.reject({ code: 503 })
  await pending
  assert.equal(versions.loadError.value, '')
  assert.equal(versions.busy.value, 'load')
  second.resolve({ id: 'second', headCommit: 'second-head' })
  await new Promise(resolve => setImmediate(resolve))
  assert.equal(versions.record.value.id, 'second')
  assert.deepEqual(versions.history.value, [{ id: 'second-head' }])
  assert.equal(versions.busy.value, '')
  versions.busy.value = 'write-confirm'
  assert.equal(routeGuard({ params: { id: 'third' } }, route), false)
  assert.equal(routeGuard({ params: { id: 'second' } }, route), true)
})
