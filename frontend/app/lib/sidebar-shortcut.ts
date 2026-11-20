type ShortcutEvent = Pick<
  KeyboardEvent,
  'key' | 'ctrlKey' | 'metaKey' | 'altKey' | 'shiftKey' | 'repeat' | 'isComposing' | 'defaultPrevented'
>
type FocusTarget = Pick<HTMLElement, 'closest' | 'isContentEditable'>

export function shouldToggleSidebar(event: ShortcutEvent, target: FocusTarget | null) {
  return event.key.toLowerCase() === 'b'
    && (event.ctrlKey || event.metaKey)
    && !event.altKey && !event.shiftKey
    && !event.defaultPrevented && !event.repeat && !event.isComposing
    && !target?.isContentEditable && !target?.closest('input, textarea, select')
}
