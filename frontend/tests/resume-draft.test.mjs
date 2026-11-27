import { test } from 'node:test'
import assert from 'node:assert/strict'
import { resumeDraftStore } from '../src/lib/resumes.ts'

function storage() {
  const items = new Map()
  return {
    items,
    get length() { return items.size },
    key: index => [...items.keys()][index] ?? null,
    getItem: key => items.get(key) ?? null,
    setItem: (key, value) => items.set(key, value),
    removeItem: key => items.delete(key),
  }
}

const draft = { content: '# 未保存正文', message: '备注', baseHeadCommit: 'head-1' }

test('draft stores never sweep other accounts, sessions or resumes', () => {
  const store = storage()
  const alice = resumeDraftStore(store, 'id:1', 'session-a', 'resume-1')
  alice.write(draft)

  // Constructing another scope must not delete the first scope's draft.
  resumeDraftStore(store, 'id:2', 'session-a', 'resume-1')
  resumeDraftStore(store, 'id:1', 'session-b', 'resume-1')
  resumeDraftStore(store, 'id:1', 'session-a', 'resume-2')

  assert.deepEqual(alice.read(), draft)
  assert.equal(resumeDraftStore(store, 'id:2', 'session-a', 'resume-1').read(), null)
  assert.equal(resumeDraftStore(store, 'id:1', 'session-b', 'resume-1').read(), null)
  assert.equal(resumeDraftStore(store, 'id:1', 'session-a', 'resume-2').read(), null)
  assert.equal(store.items.size, 1)
})

test('corrupt or mismatched payloads clear only the current key', () => {
  const store = storage()
  const first = resumeDraftStore(store, 'id:1', 'session-a', 'resume-1')
  const second = resumeDraftStore(store, 'id:1', 'session-a', 'resume-2')
  first.write(draft)
  second.write({ ...draft, content: '# 第二份' })
  const secondKey = [...store.items.keys()].find(key => key.includes('resume-2'))

  store.items.set(secondKey, '{"content":')
  assert.equal(second.read(), null)
  assert.equal(store.items.has(secondKey), false)
  assert.deepEqual(first.read(), draft)

  const firstKey = [...store.items.keys()].find(key => key.includes('resume-1'))
  store.items.set(firstKey, JSON.stringify({
    account: 'id:1', resumeId: 'resume-2', content: '# 串号', message: '', baseHeadCommit: null,
  }))
  assert.equal(first.read(), null)
  assert.equal(store.items.size, 0)
})

test('storage failures surface instead of silently losing the draft', () => {
  const blocked = {
    get length() { return 0 },
    key: () => null,
    getItem: () => { throw new DOMException('blocked', 'SecurityError') },
    setItem: () => { throw new DOMException('quota', 'QuotaExceededError') },
    removeItem: () => { throw new DOMException('blocked', 'SecurityError') },
  }
  const store = resumeDraftStore(blocked, 'id:1', 'session-a', 'resume-1')
  assert.throws(() => store.write(draft), /blocked|quota/)
  assert.throws(() => store.read(), /blocked/)
})

test('draft keys carry a version and the payload records the base head commit', () => {
  const store = storage()
  const resume = resumeDraftStore(store, 'id:1', 'session-a', 'resume-1')
  resume.write(draft)
  const [key] = [...store.items.keys()]
  assert.match(key, /^bossanalyze:resume-draft:v2:/)
  assert.deepEqual(resume.read(), draft)
  resume.clear()
  assert.equal(store.items.size, 0)
})
