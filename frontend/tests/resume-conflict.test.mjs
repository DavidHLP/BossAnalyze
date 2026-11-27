import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { resumeConflictCopyTitle, resumeWriteConflict } from '../src/lib/resumes.ts'

const conflictFailure = {
  code: 409,
  message: '简历工作区已更新，请重新读取后再试',
  resultUnknown: false,
  data: {
    code: 409,
    message: '简历工作区已更新，请重新读取后再试',
    data: { workspaceRevision: 12, headCommit: 'commit-b' },
    timestamp: 1,
  },
}

test('a 409 write failure exposes the server revision and head for the conflict dialog', () => {
  assert.deepEqual(resumeWriteConflict(conflictFailure), { workspaceRevision: 12, headCommit: 'commit-b' })
  assert.deepEqual(resumeWriteConflict({ ...conflictFailure, data: { data: { workspaceRevision: 0, headCommit: null } } }),
    { workspaceRevision: 0, headCommit: null })
})

test('other failures are not conflicts and a state-less 409 still opens the dialog', () => {
  assert.equal(resumeWriteConflict({ code: 500, data: {} }), null)
  assert.equal(resumeWriteConflict(new Error('network')), null)
  assert.deepEqual(resumeWriteConflict({ code: 409, data: {} }), { workspaceRevision: null, headCommit: null })
  assert.deepEqual(
    resumeWriteConflict({ ...conflictFailure, data: { data: { workspaceRevision: -1, headCommit: 7 } } }),
    { workspaceRevision: null, headCommit: null },
  )
})

test('the conflict copy gets a distinct, non-empty title', () => {
  assert.equal(resumeConflictCopyTitle('张三的简历'), '张三的简历（冲突副本）')
  assert.equal(resumeConflictCopyTitle('   '), '简历（冲突副本）')
})

async function resumeScript(file, bindings, names) {
  const vue = await import('vue')
  const ts = await import('typescript')
  const source = await readFile(new URL(`../src/${file}.vue`, import.meta.url), 'utf8')
  const script = source.match(/<script setup lang="ts">([\s\S]*?)<\/script>/)[1]
    .replace(/^import[\s\S]*?from ['"][^'"]+['"]\s*$/gm, '')
    .replaceAll('import.meta.env', 'env')
  const { outputText } = ts.transpileModule(script, {
    compilerOptions: { target: ts.ScriptTarget.ES2022, module: ts.ModuleKind.ESNext },
  })
  return new Function('bindings', `with (bindings) { ${outputText.replace(/^export \{\};$/m, '')}\nreturn { ${names.join(',')} } }`)({
    ...vue, ...await import('../src/lib/resumes.ts'), ...await import('../src/lib/resume-copy.ts'),
    ...await import('../src/lib/resume-theme.ts'),
    env: {}, onMounted() {}, onUnmounted() {}, onBeforeRouteLeave() {}, onBeforeRouteUpdate() {},
    useResumeImages: () => ({ images: vue.ref({}) }),
    ...bindings,
  })
}

const conflictStateFailure = state => ({
  code: 409,
  message: '简历工作区已更新，请重新读取后再试',
  resultUnknown: false,
  data: { code: 409, message: '简历工作区已更新，请重新读取后再试', data: state, timestamp: 1 },
})

async function resumeEditor() {
  const { ref } = await import('vue')
  const values = new Map()
  const storage = {
    get length() { return values.size },
    key: index => [...values.keys()][index] ?? null,
    getItem: key => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value),
    removeItem: key => values.delete(key),
  }
  const { resumeDraftStore } = await import('../src/lib/resumes.ts')
  const store = resumeDraftStore(storage, 'id:1', '100', 'fixture')
  const owner = { token: 'fixture-token', timestamp: '100' }
  const server = {
    item: {
      id: 'fixture', title: '原简历', content: '# 已保存', currentBranch: 'main',
      headCommit: 'head-a', workspaceRevision: 3, stashStack: [],
    },
    reads: 0, writes: [], copies: [], pushed: null, list: [], lostCopy: false,
  }
  const editor = await resumeScript('pages/resumes/edit', {
    useRoute: () => ({ params: { id: 'fixture' } }),
    useRouter: () => ({ push: async path => { server.pushed = path } }),
    useSession: () => ({
      state: ref({ user: { id: 1 } }), crossTabChanged: ref(null),
      capture: () => owner, owns: () => true,
    }),
    window: { sessionStorage: storage },
    document: { getElementById: () => null },
    mutationResultUnknown: cause => Boolean(cause && typeof cause === 'object' && 'resultUnknown' in cause && cause.resultUnknown),
    getResume: async () => { server.reads++; return server.item },
    resumeHistory: async () => [],
    listResumes: async () => server.list,
    createResume: async (title, content) => {
      server.copies.push({ title, content })
      if (server.lostCopy) {
        server.lostCopy = false
        server.list.push({ id: 'copy-new', title, content })
        throw Object.assign(new Error('create response lost'), { resultUnknown: true })
      }
      return { id: 'copy-1', title, content, workspaceRevision: 0, headCommit: null }
    },
    changeResumeVersion: async (resumeId, action, data, revision) => {
      server.writes.push({ action, data, revision })
      if (action === 'stash' && server.bumpOnStash) {
        server.bumpOnStash = false
        server.item = {
          ...server.item, content: '# 第四个标签页的正文', headCommit: 'head-d',
          workspaceRevision: server.item.workspaceRevision + 1,
        }
      }
      // Simulate the backend comparing the supplied baseline with its current revision.
      if (revision !== server.item.workspaceRevision) {
        throw conflictStateFailure({ workspaceRevision: server.item.workspaceRevision, headCommit: server.item.headCommit })
      }
      if (action === 'stash') {
        server.item = {
          ...server.item,
          workspaceRevision: server.item.workspaceRevision + 1,
          stashStack: [{ content: data.content, branch: 'main', head: server.item.headCommit, timestamp: 1 }],
        }
      }
      if (action === 'commit') {
        server.item = {
          ...server.item, content: data.content, headCommit: 'commit-1',
          workspaceRevision: server.item.workspaceRevision + 1,
        }
        return 'commit-1'
      }
    },
  }, [
    'load', 'write', 'markdown', 'message', 'record', 'dirty', 'localDraft', 'feedback', 'writeError', 'writeLocked',
    'pendingAction', 'conflictOpen', 'conflictState', 'conflictPending', 'conflictError',
    'resolveConflictWithServer', 'saveConflictCopy', 'flushDraft',
    'openCopy', 'createCopy', 'copyOpen', 'copyUnverified', 'copyError', 'copyNote',
  ])
  await editor.load(true)
  return { editor, server, store }
}

test('a conflicted save keeps the edit and draft, opens the dialog and stays editable', async () => {
  const { editor, server, store } = await resumeEditor()
  editor.markdown.value = '# 我的修改'
  editor.message.value = '本地备注'
  editor.flushDraft()
  server.item = { ...server.item, content: '# 另一标签页的正文', headCommit: 'head-b', workspaceRevision: 4 }

  await editor.write('save')

  assert.deepEqual(server.writes, [{
    action: 'commit', data: { message: '本地备注', content: '# 我的修改' }, revision: 3,
  }])
  assert.equal(server.reads, 1, 'a conflict must not reload and replace the editor')
  assert.equal(editor.markdown.value, '# 我的修改')
  assert.equal(editor.message.value, '本地备注')
  assert.deepEqual(store.read(), { content: '# 我的修改', message: '本地备注', baseHeadCommit: 'head-a' })
  assert.equal(editor.conflictOpen.value, true)
  assert.deepEqual(editor.conflictState.value, { workspaceRevision: 4, headCommit: 'head-b' })
  assert.equal(editor.conflictError.value, '')
  assert.equal(editor.writeLocked.value, false)
  assert.equal(editor.pendingAction.value, null)
})

test('using the server version reloads the editor and rebases the next save', async () => {
  const { editor, server, store } = await resumeEditor()
  editor.markdown.value = '# 我的修改'
  editor.flushDraft()
  server.item = { ...server.item, content: '# 另一标签页的正文', headCommit: 'head-b', workspaceRevision: 4 }
  await editor.write('save')

  await editor.resolveConflictWithServer(false)

  assert.equal(editor.markdown.value, '# 另一标签页的正文')
  assert.equal(store.read(), null)
  assert.equal(editor.conflictOpen.value, false)
  assert.equal(editor.record.value.workspaceRevision, 4)
  assert.equal(editor.dirty.value, false)

  editor.markdown.value = '# 冲突解决后的修改'
  await editor.write('save')
  assert.equal(server.writes.at(-1).revision, 4)
})

test('stashing before using the server version writes with the conflict revision', async () => {
  const { editor, server } = await resumeEditor()
  editor.markdown.value = '# 我的修改'
  server.item = { ...server.item, content: '# 另一标签页的正文', headCommit: 'head-b', workspaceRevision: 4 }
  await editor.write('save')

  await editor.resolveConflictWithServer(true)

  assert.deepEqual(server.writes.map(item => [item.action, item.revision]), [['commit', 3], ['stash', 4]])
  assert.equal(server.item.stashStack[0].content, '# 我的修改')
  assert.equal(editor.markdown.value, '# 另一标签页的正文')
  assert.equal(editor.conflictOpen.value, false)
})

test('a second conflict while stashing keeps the edit instead of replacing it', async () => {
  const { editor, server, store } = await resumeEditor()
  editor.markdown.value = '# 我的修改'
  editor.flushDraft()
  server.item = { ...server.item, content: '# 另一标签页的正文', headCommit: 'head-b', workspaceRevision: 4 }
  await editor.write('save')
  server.item = { ...server.item, content: '# 第三个标签页的正文', headCommit: 'head-c', workspaceRevision: 5 }
  server.bumpOnStash = true

  await editor.resolveConflictWithServer(true)

  assert.equal(server.writes.at(-1).action, 'stash')
  assert.equal(editor.markdown.value, '# 我的修改')
  assert.deepEqual(store.read(), { content: '# 我的修改', message: '', baseHeadCommit: 'head-c' })
  assert.equal(editor.conflictOpen.value, true)
  assert.deepEqual(editor.conflictState.value, { workspaceRevision: 6, headCommit: 'head-d' })
  assert.match(editor.conflictError.value, /未能暂存/)
})

test('saving the conflict as a copy creates an independent resume and leaves the original alone', async () => {
  const { editor, server, store } = await resumeEditor()
  editor.markdown.value = '# 我的修改'
  editor.message.value = '本地备注'
  editor.flushDraft()
  server.item = { ...server.item, content: '# 另一标签页的正文', headCommit: 'head-b', workspaceRevision: 4 }
  await editor.write('save')

  await editor.saveConflictCopy()

  assert.deepEqual(server.copies, [{ title: '原简历（冲突副本）', content: '# 我的修改' }])
  assert.deepEqual(server.writes.map(item => item.action), ['commit'], 'the original record is not written again')
  assert.equal(server.pushed, '/resumes/copy-1/edit')
  assert.equal(editor.conflictOpen.value, false)
  assert.equal(editor.markdown.value, '# 我的修改')
  assert.equal(store.read(), null)
})

test('a lost copy reply skips an older same-title copy and confirms the new one', async () => {
  const { editor, server } = await resumeEditor()
  server.list = [{ id: 'older-copy', title: '原简历（副本）', content: '# 已保存' }]
  server.lostCopy = true

  editor.openCopy()
  await editor.createCopy()

  assert.equal(server.copies.length, 1)
  assert.equal(editor.copyUnverified.value, false)
  assert.equal(server.pushed, '/resumes/copy-new/edit')
})

test('closing the conflict dialog does not lock editing and a retry conflicts again', async () => {
  const { editor, server } = await resumeEditor()
  editor.markdown.value = '# 我的修改'
  server.item = { ...server.item, content: '# 另一标签页的正文', headCommit: 'head-b', workspaceRevision: 4 }
  await editor.write('save')

  editor.conflictOpen.value = false
  editor.markdown.value = '# 我再改一次'
  await editor.write('save')

  assert.equal(editor.conflictOpen.value, true)
  assert.equal(server.writes.at(-1).revision, 3)
  assert.equal(editor.markdown.value, '# 我再改一次')
  assert.equal(editor.writeLocked.value, false)
})

async function resumeVersions() {
  const { ref } = await import('vue')
  const owner = { token: 'versions-token', timestamp: '100' }
  const server = {
    record: {
      id: 'fixture', title: '简历', content: '# 已保存', currentBranch: 'main',
      headCommit: 'head-a', workspaceRevision: 4,
      branches: { main: 'head-a', feature: 'head-b' }, stashStack: [],
    },
    commits: [
      { id: 'head-a', content: '# 已保存', message: '初始', branch: 'main', parentCommits: [] },
      { id: 'head-b', content: '# 分支', message: '分支', branch: 'feature', parentCommits: [] },
    ],
    calls: [], fail: null, pushed: null,
  }
  const page = await resumeScript('pages/resumes/versions', {
    useRoute: () => ({ params: { id: 'fixture' } }),
    useRouter: () => ({ push: async path => { server.pushed = path } }),
    useSession: () => ({ state: ref({ invalidated: false }), capture: () => owner, owns: () => true }),
    useMediaQuery: () => ref(false),
    useResumeImages: () => ({ images: ref({}) }),
    getResume: async () => server.record,
    resumeHistory: async () => server.commits,
    inspectResume: async () => ({}),
    inspectCommit: async () => ({}),
    changeResumeVersion: async (resumeId, action, data, revision) => {
      server.calls.push({ action, data, revision })
      if (server.fail) throw server.fail
      if (action === 'checkout') server.record = { ...server.record, headCommit: data.commitId }
      if (action === 'merge') {
        server.record = { ...server.record, branches: { ...server.record.branches, [data.target]: 'merged' } }
        return 'merged'
      }
      if (action === 'stash') {
        server.record = { ...server.record, stashStack: [...server.record.stashStack, { content: data.content }] }
      }
      if (action === 'revert') return 'reverted'
      return undefined
    },
  }, [
    'refresh', 'submit', 'operation', 'selection', 'record', 'detailError', 'feedback', 'writeLocked', 'stashCount',
    'source', 'target', 'confirmed', 'draft', 'branchName',
  ])
  await page.refresh(true)
  return { page, server }
}

test('version actions send the current workspace revision', async () => {
  const { page, server } = await resumeVersions()
  page.operation.value = 'checkout'
  page.selection.value = server.commits[1]
  await page.submit()
  assert.deepEqual(server.calls.at(-1), { action: 'checkout', data: { commitId: 'head-b' }, revision: 4 })
  assert.equal(page.detailError.value, '')
  assert.equal(page.operation.value, null)

  page.operation.value = 'merge'
  page.source.value = 'feature'
  page.target.value = 'main'
  page.confirmed.value = true
  await page.submit()
  assert.deepEqual(server.calls.at(-1), { action: 'merge', data: { source: 'feature', target: 'main' }, revision: 4 })

  page.operation.value = 'stash'
  page.draft.value = '# 草稿'
  await page.submit()
  assert.deepEqual(server.calls.at(-1), { action: 'stash', data: { content: '# 草稿' }, revision: 4 })
})

test('a stale version write reports the conflict instead of overwriting silently', async () => {
  const { page, server } = await resumeVersions()
  server.fail = { code: 409, data: { data: { workspaceRevision: 5, headCommit: 'head-c' } } }
  page.operation.value = 'reset'
  page.selection.value = server.commits[0]
  await page.submit()
  assert.equal(server.calls.length, 1)
  assert.equal(server.calls[0].revision, 4)
  assert.match(page.detailError.value, /另一个标签页或设备已保存新版本/)
  assert.equal(page.operation.value, null)
  assert.equal(page.writeLocked.value, false)
})
