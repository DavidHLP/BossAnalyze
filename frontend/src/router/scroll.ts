import type { RouterScrollBehavior } from 'vue-router'

let cancelRestoration: (() => void) | undefined

function restorePosition(position: { left: number; top: number }) {
  const canReachPosition = () => {
    const root = document.scrollingElement
    return !!root && root.scrollHeight - root.clientHeight >= position.top
  }
  if (canReachPosition()) return position

  // A page can be shorter while its API data is still loading after a history traversal.
  window.scrollTo(position)
  return new Promise<typeof position | false>((resolve) => {
    const input = new AbortController()
    const observer = new ResizeObserver(() => {
      if (canReachPosition()) finish(position)
    })
    const timer = window.setTimeout(() => finish(false), 5000)
    function finish(result: typeof position | false) {
      window.clearTimeout(timer)
      observer.disconnect()
      input.abort()
      cancelRestoration = undefined
      resolve(result)
    }
    const cancel = () => finish(false)
    cancelRestoration = cancel
    for (const event of ['pointerdown', 'wheel', 'keydown']) {
      window.addEventListener(event, cancel, { signal: input.signal, passive: true })
    }
    observer.observe(document.body)
  })
}

export const consoleScrollBehavior: RouterScrollBehavior = (to, from, savedPosition) => {
  if (!savedPosition && to.path === from.path && to.hash === from.hash) return false
  cancelRestoration?.()
  if (savedPosition) return restorePosition(savedPosition)
  const anchor = to.hash && document.getElementById(to.hash.slice(1))
  if (anchor) {
    const top = Number.parseFloat(getComputedStyle(anchor).scrollMarginTop) || 0
    return { el: anchor, top }
  }
  return to.path === from.path ? false : { left: 0, top: 0 }
}
