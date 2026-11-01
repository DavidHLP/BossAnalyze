export interface MenuItem {
  title: string
  path: string | null
  children: MenuItem[]
}

export function validSession(token: string | null, timestamp: string | null, now = Date.now()) {
  const issued = Number(timestamp)
  return Boolean(
    token &&
    timestamp &&
    Number.isFinite(issued) &&
    issued > 0 &&
    issued <= now &&
    now - issued < 24 * 60 * 60 * 1000,
  )
}

export function consolePath(value: unknown, parent = '/console') {
  if (typeof value !== 'string' || !value || /[\s\\]|%(?:2f|5c)/i.test(value)) return null
  if (/^(?:[a-z][a-z\d+.-]*:|\/\/)/i.test(value)) return null
  const path = value.startsWith('/') ? value : `${parent.replace(/\/$/, '')}/${value}`
  const url = new URL(path, 'https://bossanalyze.invalid')
  if (url.origin !== 'https://bossanalyze.invalid') return null
  if (
    ['/api', '/_nuxt', '/login'].some(
      (prefix) => url.pathname === prefix || url.pathname.startsWith(`${prefix}/`),
    )
  )
    return null
  const normalized = url.pathname.replace(/\/$/, '') || '/'
  return normalized === '/console' || normalized.startsWith('/console/')
    ? normalized
    : normalized === '/'
      ? '/console'
      : `/console${normalized}`
}

export function menuTree(raw: unknown, permissions: unknown, parent = '/console'): MenuItem[] {
  if (
    !Array.isArray(raw) ||
    !Array.isArray(permissions) ||
    !permissions.every((permission) => typeof permission === 'string')
  ) {
    throw new Error('Invalid menu or permission response')
  }
  return [...raw]
    .sort((a, b) => (a?.menuOrder ?? 0) - (b?.menuOrder ?? 0))
    .flatMap((item): MenuItem[] => {
      if (!item || typeof item !== 'object' || Array.isArray(item)) return []
      if (item.meta != null && (typeof item.meta !== 'object' || Array.isArray(item.meta)))
        return []
      const meta = item.meta ?? {}
      if (
        item.status !== 1 ||
        meta.type === 'F' ||
        meta.hidden === true ||
        meta.metaHidden === true
      ) {
        return []
      }
      if (
        item.permission != null &&
        item.permission !== '' &&
        (typeof item.permission !== 'string' || !permissions.includes(item.permission))
      )
        return []
      const path = item.path ? consolePath(item.path, parent) : null
      if (item.path && !path) return []
      const children = menuTree(item.children ?? [], permissions, path ?? parent)
      const title = meta.metaTitle || item.name
      if (typeof title !== 'string' || (!path && !children.length)) return []
      return [{ title, path, children }]
    })
}

export function findMenu(items: MenuItem[], path: string): MenuItem | undefined {
  for (const item of items) {
    if (item.path === path) return item
    const child = findMenu(item.children, path)
    if (child) return child
  }
}

export function menuShortcuts(items: MenuItem[]) {
  const seen = new Set<string>()
  function leaves(nodes: MenuItem[]): Array<{ title: string; path: string }> {
    return nodes.flatMap((node) => {
      if (node.children.length) return leaves(node.children)
      if (!node.path || node.path === '/console' || seen.has(node.path)) return []
      seen.add(node.path)
      return [{ title: node.title, path: node.path }]
    })
  }
  return leaves(items)
}

export function loginDestination(value: unknown, menus: MenuItem[]) {
  if (value === '/recruitment') return '/recruitment'
  const path = typeof value === 'string' && value.startsWith('/') ? consolePath(value) : null
  return path && (path === '/console' || findMenu(menus, path)) ? path : '/console'
}
