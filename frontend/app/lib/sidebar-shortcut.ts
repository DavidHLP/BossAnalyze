type ShortcutEvent = Pick<
  KeyboardEvent,
  'key' | 'ctrlKey' | 'metaKey' | 'altKey' | 'shiftKey' | 'repeat' | 'isComposing' | 'defaultPrevented'
>
type FocusTarget = Pick<HTMLElement, 'closest' | 'isContentEditable'>

const blockingModal = `:is(
  dialog:modal,
  [aria-modal="true"],
  [role="alertdialog"],
  [role="dialog"]:not([aria-modal="false"], [data-slot="popover-content"]),
  [data-state="open"]:is([data-slot="dialog-content"], [data-slot="sheet-content"])
):not([hidden], [hidden] *, [data-state="closed"])`

export function shouldToggleSidebar(
  event: ShortcutEvent,
  target: FocusTarget | null,
  root: Pick<Document, 'querySelector'>,
) {
  return event.key.toLowerCase() === 'b'
    && (event.ctrlKey || event.metaKey)
    && !event.altKey && !event.shiftKey
    && !event.defaultPrevented && !event.repeat && !event.isComposing
    && !target?.isContentEditable && !target?.closest('input, textarea, select')
    && !root.querySelector(blockingModal)
}
