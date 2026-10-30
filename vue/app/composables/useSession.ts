import { getUserBaseInfo, getUserPrivateInformation, login } from '@/api/auth/auth'
import { getUserRoutes } from '@/api/router/router'
import { menuTree, validSession } from '@/lib/session'
import type { MenuItem } from '@/lib/session'

interface Session {
  user: { name: string; email: string } | null
  menus: MenuItem[]
  ready: boolean
}

let loadedToken: string | null = null

export function useSession() {
  const state = useState<Session>('session', () => ({ user: null, menus: [], ready: false }))

  function clear() {
    loadedToken = null
    state.value = { user: null, menus: [], ready: false }
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
    const [user, permissions, routes] = await Promise.all([
      getUserBaseInfo(),
      getUserPrivateInformation(),
      getUserRoutes(),
    ])
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
      clear()
      throw error
    }
  }

  async function signOut() {
    clear()
    await navigateTo('/login', { replace: true })
  }

  return { state, restore, signIn, signOut, clear }
}
