import { test } from 'node:test'
import assert from 'node:assert/strict'
import { resumeStamp, resumeTime } from '../src/lib/resumes.ts'

const epoch = 1704164645000
const iso = '2024-01-02T03:04:05Z'
const shown = '2024-01-02 03:04:05'

test('resumeStamp normalizes epoch millis, ISO strings and garbage', () => {
  assert.equal(resumeStamp(epoch), epoch)
  assert.equal(resumeStamp(iso), epoch)
  assert.equal(resumeStamp(String(epoch)), epoch)
  assert.equal(resumeStamp(undefined), 0)
  assert.equal(resumeStamp(''), 0)
  assert.equal(resumeStamp('not-a-time'), 0)
})

test('resume sorting tolerates mixed timestamp shapes without throwing', () => {
  const rows = [
    { id: 'a', updatedAt: undefined, createdAt: iso },
    { id: 'b', updatedAt: epoch + 2000 },
    { id: 'c', updatedAt: '2024-01-02T03:04:06Z' },
    { id: 'd', updatedAt: 'garbage' },
  ]
  const sorted = rows.sort((a, b) =>
    resumeStamp(b.updatedAt ?? b.createdAt) - resumeStamp(a.updatedAt ?? a.createdAt))
  assert.deepEqual(sorted.map(row => row.id), ['b', 'c', 'a', 'd'])
})

test('resumeTime shows epoch and ISO values identically and masks invalid input', () => {
  assert.equal(resumeTime(iso), shown)
  assert.equal(resumeTime(epoch), shown)
  assert.equal(resumeTime(String(epoch)), shown)
  assert.equal(resumeTime(undefined), '—')
  assert.equal(resumeTime('nope'), '—')
})
