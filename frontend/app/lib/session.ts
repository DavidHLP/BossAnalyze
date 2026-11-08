export interface MenuItem {
  title: string
  path: string | null
  icon?: string
  hidden?: boolean
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
      if (item.status !== 1 || meta.type === 'F' || meta.hidden === true) {
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
      return [{
        title, path, children,
        hidden: meta.metaHidden === true,
        icon: typeof meta.metaIcon === 'string' ? meta.metaIcon : undefined,
      }]
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
  function leaves(nodes: MenuItem[]): Array<{ title: string; path: string; icon?: string }> {
    return nodes.flatMap((node) => {
      if (node.children.length) return leaves(node.children)
      if (!node.path || node.path === '/console' || seen.has(node.path)) return []
      seen.add(node.path)
      return [{ title: node.title, path: node.path, ...(node.icon ? { icon: node.icon } : {}) }]
    })
  }
  return leaves(navigationMenus(items))
}

export function navigationMenus(items: MenuItem[]): MenuItem[] {
  return items.filter((item) => !item.hidden).map((item) => ({
    ...item, children: navigationMenus(item.children),
  }))
}

export function safeReturnPath(value: unknown) {
  if (typeof value !== 'string' || !value.startsWith('/') || /[\\\s]|%(?:2f|5c)/i.test(value)) return null
  const url = new URL(value, 'https://bossanalyze.invalid')
  if (url.origin !== 'https://bossanalyze.invalid') return null
  if (url.pathname !== '/recruitment' && url.pathname !== '/console' && !url.pathname.startsWith('/console/')) return null
  return `${url.pathname}${url.search}${url.hash}`
}

export function loginLocation(value: unknown, reason?: string) {
  return { path: '/login', query: { redirect: safeReturnPath(value) ?? '/console', ...(reason ? { reason } : {}) } }
}

export function loginDestination(value: unknown, menus: MenuItem[]) {
  const fullPath = safeReturnPath(value)
  const path = fullPath ? new URL(fullPath, 'https://bossanalyze.invalid').pathname : null
  if (!fullPath || !path) return '/console'
  return path === '/recruitment' || path === '/console' || findMenu(menus, path) ? fullPath : '/console'
}

export function loginFailure(error: unknown) {
  const code = error && typeof error === 'object' && 'code' in error ? error.code : null
  if (code === 401) return '邮箱或密码不正确，请检查后重试。'
  if (code === 502 || code === 503 || code === 504)
    return '暂时无法完成登录，请稍后重试或检查服务连接。'
  // The login controller uses the same 500 response for credentials and internal failures.
  return '登录失败，请检查邮箱、密码及服务连接后重试。'
}
