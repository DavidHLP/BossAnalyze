import assert from 'node:assert/strict'
import test from 'node:test'
import { resolveTheme } from '../app/lib/theme.ts'

test('an explicit theme overrides the operating system preference', () => {
  assert.equal(resolveTheme('light', true), 'light')
  assert.equal(resolveTheme('dark', false), 'dark')
})

test('missing or invalid stored themes follow the operating system', () => {
  for (const stored of [null, '', 'auto', 'DARK']) {
    assert.equal(resolveTheme(stored, true), 'dark')
    assert.equal(resolveTheme(stored, false), 'light')
  }
})
