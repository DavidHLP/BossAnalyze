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
    ...vue, ...await import('../src/lib/resumes.ts'), ...await import('../src/lib/resume-theme.ts'),
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
    reads: 0, writes: [], copies: [], pushed: null,
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
    getResume: async () => { server.reads++; return server.item },
    resumeHistory: async () => [],
    createResume: async (title, content) => {
      server.copies.push({ title, content })
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
