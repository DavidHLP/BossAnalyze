import assert from 'node:assert/strict'
import test from 'node:test'
import { runInNewContext } from 'node:vm'
import { DARK_THEME_QUERY, resolveTheme, THEME_STORAGE_KEY, themeHeadScript } from '../src/lib/theme.ts'

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

test('the head script synchronously applies the same theme before Vue mounts', () => {
  for (const stored of ['light', 'dark', 'invalid', null, undefined]) {
    for (const prefersDark of [true, false]) {
      const root = {
        classes: new Set([resolveTheme(stored ?? null, prefersDark) === 'dark' ? 'light' : 'dark']),
        style: {},
        classList: {
          toggle(name, enabled) {
            enabled ? root.classes.add(name) : root.classes.delete(name)
          },
        },
      }
      runInNewContext(themeHeadScript, {
        document: { documentElement: root },
        localStorage: {
          getItem(key) {
            assert.equal(key, THEME_STORAGE_KEY)
            if (stored === undefined) throw new Error('Storage denied')
            return stored
          },
        },
        matchMedia(query) {
          assert.equal(query, DARK_THEME_QUERY)
          return { matches: prefersDark }
        },
      })
      const expected = resolveTheme(stored ?? null, prefersDark)
      assert.equal(root.classes.has('dark'), expected === 'dark')
      assert.equal(root.style.colorScheme, expected)
    }
  }
})
