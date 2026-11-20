import { getUserBaseInfo, getUserPrivateInformation, login, logout } from '@/api/auth/auth'
import { getUserRoutes } from '@/api/router/router'
import { advanceSession, loginLocation, menuTree, ownsSession, sessionOwner, validSession, withSessionOwner } from '@/lib/session'
import type { MenuItem, SessionOwner } from '@/lib/session'

interface Session {
  user: { name: string; email: string } | null
  menus: MenuItem[]
  invalidated?: boolean
  ready: boolean
}

let loadedOwner: SessionOwner | null = null

export function useSession() {
  const state = useState<Session>('session', () => ({ user: null, menus: [], ready: false }))

  function capture() {
    return import.meta.client ? sessionOwner() : null
  }
  function owns(owner: SessionOwner | null) {
    return import.meta.client && !state.value.invalidated && !!owner?.token && ownsSession(owner)
  }
  function changed() {
    advanceSession()
    state.value = { ...state.value }
  }
  function clear() {
    advanceSession()
    loadedOwner = null
    state.value = { user: null, menus: [], ready: false, invalidated: true }
    if (import.meta.client) {
      for (const key of ['token', 'token_timestamp', 'cachedRoutes']) localStorage.removeItem(key)
      sessionStorage.removeItem('token_timestamp')
    }
  }

  async function restore() {
    if (import.meta.server) return false
    const owner = sessionOwner()
    if (!validSession(owner.token, owner.timestamp)) {
      clear()
      return false
    }
    if (state.value.ready && loadedOwner && ownsSession(loadedOwner)) return true
    state.value = { user: null, menus: [], ready: false }
    let result
    try {
      result = await Promise.all([
        getUserBaseInfo(),
        getUserPrivateInformation(),
        getUserRoutes(),
      ])
    } catch (error) {
      if (!ownsSession(owner)) {
        if (sessionOwner().token) return restore()
      } else if (error && typeof error === 'object' && 'code' in error && error.code === 401) clear()
      throw error
    }
    const [user, permissions, routes] = result
    if (!ownsSession(owner)) return restore()
    if (!user || typeof user !== 'object' || typeof user.email !== 'string') {
      throw new Error('Invalid user response')
    }
    state.value = {
      user: {
        name: typeof user.name === 'string' ? user.name : user.email,
        email: user.email,
      },
      menus: menuTree(routes, permissions),
      ready: true,
    }
    loadedOwner = owner
    return true
  }

  async function signIn(email: string, password: string) {
    clear()
    const owner = sessionOwner()
    try {
      const result = await login({ email, password })
      if (!result?.token || typeof result.token !== 'string') throw new Error('Missing token')
      if (!ownsSession(owner)) throw new Error('Session changed')
      localStorage.setItem('token', result.token)
      localStorage.setItem('token_timestamp', String(Date.now()))
      if (!(await restore())) throw new Error('Session changed')
    } catch (error) {
      if (ownsSession(owner)) clear()
      throw error
    }
  }

  const router = useRouter()
  async function signOut(
    force = false,
    destination: unknown = null,
    reason?: string,
    owner = sessionOwner(),
  ) {
    if (!ownsSession(owner)) return false
    if (force) clear()
    const target = destination ? loginLocation(destination, reason) : '/login'
    const failure = await router.replace(target)
    if (failure || router.currentRoute.value.path !== '/login') return false
    if (force || !ownsSession(owner)) return true
    if (owner.token) {
      try {
        await withSessionOwner(owner, logout)
      } catch {
        if (ownsSession(owner)) {
          clear()
          await router.replace({ path: '/login', query: { logout: 'unconfirmed' } })
        }
        return true
      }
    }
    if (ownsSession(owner)) clear()
    return true
  }

  return { state, restore, signIn, signOut, clear, capture, owns, changed }
}
