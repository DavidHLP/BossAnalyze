import { getUserBaseInfo, getUserPrivateInformation, login } from '@/api/auth/auth'
import { getUserRoutes } from '@/api/router/router'
import { loginLocation, menuTree, validSession } from '@/lib/session'
import type { MenuItem } from '@/lib/session'

interface Session {
  user: { name: string; email: string } | null
  menus: MenuItem[]
  invalidated?: boolean
  ready: boolean
}

let loadedToken: string | null = null

export function useSession() {
  const state = useState<Session>('session', () => ({ user: null, menus: [], ready: false }))

  function clear() {
    loadedToken = null
    state.value = { user: null, menus: [], ready: false, invalidated: true }
    if (import.meta.client) {
      for (const key of ['token', 'token_timestamp', 'cachedRoutes']) localStorage.removeItem(key)
      sessionStorage.removeItem('token_timestamp')
    }
  }

  async function restore() {
    if (import.meta.server) return false
    const token = localStorage.getItem('token')
    if (!validSession(token, localStorage.getItem('token_timestamp'))) {
      clear()
      return false
    }
    if (state.value.ready && loadedToken === token) return true
    state.value = { user: null, menus: [], ready: false }
    let result
    try {
      result = await Promise.all([
        getUserBaseInfo(),
        getUserPrivateInformation(),
        getUserRoutes(),
      ])
    } catch (error) {
      if (error && typeof error === 'object' && 'code' in error && error.code === 401) clear()
      throw error
    }
    const [user, permissions, routes] = result
    if (token !== localStorage.getItem('token')) return false
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
    loadedToken = token
    return true
  }

  async function signIn(email: string, password: string) {
    clear()
    try {
      const result = await login({ email, password })
      if (!result?.token || typeof result.token !== 'string') throw new Error('Missing token')
      localStorage.setItem('token', result.token)
      localStorage.setItem('token_timestamp', String(Date.now()))
      if (!(await restore())) throw new Error('Session changed')
    } catch (error) {
      if (!localStorage.getItem('token')) clear()
      throw error
    }
  }

  const router = useRouter()
  async function signOut(force = false, destination: unknown = null, reason?: string) {
    if (force) clear()
    const target = destination ? loginLocation(destination, reason) : '/login'
    const failure = await router.replace(target)
    if (failure || router.currentRoute.value.path !== '/login') return false
    clear()
    return true
  }

  return { state, restore, signIn, signOut, clear }
}
