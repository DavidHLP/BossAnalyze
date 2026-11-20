import assert from 'node:assert/strict'
import test from 'node:test'
import { shouldToggleSidebar } from '../app/lib/sidebar-shortcut.ts'

const ctrlB = {
  key: 'b',
  ctrlKey: true,
  metaKey: false,
  altKey: false,
  shiftKey: false,
  repeat: false,
  isComposing: false,
  defaultPrevented: false,
}

test('Ctrl+B and Meta+B toggle the sidebar outside text controls', () => {
  const button = { closest: () => null, isContentEditable: false }
  assert.equal(shouldToggleSidebar(ctrlB, button), true)
  assert.equal(
    shouldToggleSidebar({ ...ctrlB, key: 'B', ctrlKey: false, metaKey: true }, null),
    true,
  )
})

test('text fields, selects, and descendants of editable regions leave the shortcut to the editor', () => {
  for (const tag of ['input', 'textarea', 'select']) {
    const target = {
      closest: (selector) => selector.split(', ').includes(tag) ? target : null,
      isContentEditable: false,
    }
    assert.equal(shouldToggleSidebar(ctrlB, target), false, tag)
  }

  const editableDescendant = { closest: () => null, isContentEditable: true }
  assert.equal(shouldToggleSidebar(ctrlB, editableDescendant), false)
})

test('unrelated or already handled keystrokes do not toggle the sidebar', () => {
  for (const different of [
    { key: 'a' }, { ctrlKey: false }, { altKey: true }, { shiftKey: true },
    { repeat: true }, { isComposing: true }, { defaultPrevented: true },
  ]) {
    assert.equal(shouldToggleSidebar({ ...ctrlB, ...different }, null), false)
  }
})
