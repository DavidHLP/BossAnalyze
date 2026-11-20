import assert from 'node:assert/strict'
import test from 'node:test'
import { shouldToggleSidebar } from '../src/lib/sidebar-shortcut.ts'

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
const page = { querySelector: () => null }

test('Ctrl+B and Meta+B toggle the sidebar outside text controls', () => {
  const button = { closest: () => null, isContentEditable: false }
  assert.equal(shouldToggleSidebar(ctrlB, button, page), true)
  assert.equal(
    shouldToggleSidebar({ ...ctrlB, key: 'B', ctrlKey: false, metaKey: true }, null, page),
    true,
  )
})

test('text fields, selects, and descendants of editable regions leave the shortcut to the editor', () => {
  for (const tag of ['input', 'textarea', 'select']) {
    const target = {
      closest: (selector) => selector.split(', ').includes(tag) ? target : null,
      isContentEditable: false,
    }
    assert.equal(shouldToggleSidebar(ctrlB, target, page), false, tag)
  }

  const editableDescendant = { closest: () => null, isContentEditable: true }
  assert.equal(shouldToggleSidebar(ctrlB, editableDescendant, page), false)
})

test('an open dialog or sheet anywhere on the page blocks the shortcut until it closes', () => {
  let openModal = {}
  const pageWithModal = {
    querySelector(selector) {
      assert.match(selector, /\[role="dialog"\]/)
      assert.match(selector, /\[data-state="open"\]:is\(\[data-slot="dialog-content"\], \[data-slot="sheet-content"\]\)/)
      return openModal
    },
  }
  const metaB = { ...ctrlB, ctrlKey: false, metaKey: true }
  assert.equal(shouldToggleSidebar(ctrlB, null, pageWithModal), false)
  assert.equal(shouldToggleSidebar(metaB, null, pageWithModal), false)
  openModal = null
  assert.equal(shouldToggleSidebar(ctrlB, null, pageWithModal), true)
})

test('unrelated or already handled keystrokes do not toggle the sidebar', () => {
  for (const different of [
    { key: 'a' }, { ctrlKey: false }, { altKey: true }, { shiftKey: true },
    { repeat: true }, { isComposing: true }, { defaultPrevented: true },
  ]) {
    assert.equal(shouldToggleSidebar({ ...ctrlB, ...different }, null, page), false)
  }
})
