import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { getUserBaseInfo, getUserPrivateInformation, login, logout } from '@/api/auth/auth'
import { getUserRoutes } from '@/api/router/router'
import { requireMutationSuccess } from '@/lib/rbac-mutations'
import {
  advanceSession, bindDocumentSession, loginLocation, menuTree, ownsSession,
  sessionOwner, validSession, withSessionOwner,
} from '@/lib/session'
import type { MenuItem, SessionOwner } from '@/lib/session'

interface Session {
  user: { id?: number; name: string; email: string } | null
  menus: MenuItem[]
  permissions: string[]
  invalidated?: boolean
  ready: boolean
}

let loadedOwner: SessionOwner | null = null
let loadRevision = 0
let appliedRevision = 0

const state = ref<Session>({ user: null, menus: [], permissions: [], ready: false })
const crossTabChanged = ref<'changed' | 'cleared' | null>(null)

export function useSession(router = useRouter()) {

  function capture() {
    return sessionOwner()
  }
  function owns(owner: SessionOwner | null) {
    return !crossTabChanged.value && !state.value.invalidated && !!owner &&
      validSession(owner.token, owner.timestamp) && ownsSession(owner)
  }
  function changed() {
    advanceSession()
    state.value = { ...state.value }
  }
  function clear() {
    advanceSession()
    loadedOwner = null
    crossTabChanged.value = null
    state.value = { user: null, menus: [], permissions: [], ready: false, invalidated: true }
    for (const key of ['token', 'token_timestamp', 'cachedRoutes']) localStorage.removeItem(key)
    sessionStorage.removeItem('token_timestamp')
  }

  async function load(owner: SessionOwner) {
    const revision = ++loadRevision
    let result
    try {
      result = await withSessionOwner(owner, () => Promise.all([
        getUserBaseInfo(),
        getUserPrivateInformation(),
        getUserRoutes(),
      ]))
    } catch (error) {
      if (ownsSession(owner) && error && typeof error === 'object' && 'code' in error && error.code === 401) {
        clear()
      }
      throw error
    }
    const [user, permissions, routes] = result
    if (!ownsSession(owner) || !validSession(owner.token, owner.timestamp)) return false
    if (revision < appliedRevision) return true
    if (!user || typeof user !== 'object' || typeof user.email !== 'string') {
      throw new Error('Invalid user response')
    }
    state.value = {
      user: {
        ...(typeof user.id === 'number' ? { id: user.id } : {}),
        name: typeof user.name === 'string' ? user.name : user.email,
        email: user.email,
      },
      menus: menuTree(routes, permissions),
      permissions: [...permissions],
      ready: true,
    }
    loadedOwner = owner
    appliedRevision = revision
    bindDocumentSession(owner)
    return true
  }

  async function restore() {
    if (crossTabChanged.value) return false
    const owner = sessionOwner()
    if (!validSession(owner.token, owner.timestamp)) {
      clear()
      return false
    }
    if (state.value.ready && loadedOwner && ownsSession(loadedOwner)) return true
    state.value = { user: null, menus: [], permissions: [], ready: false }
    try {
      if (await load(owner)) return true
    } catch (error) {
      if (crossTabChanged.value) return false
      if (ownsSession(owner) || !sessionOwner().token) throw error
    }
    return restore()
  }

  async function refresh(): Promise<boolean> {
    const owner = sessionOwner()
    if (!owns(owner)) return false
    try {
      return await load(owner)
    } catch {
      return false
    }
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
      const signedIn = sessionOwner()
      if (!(await restore()) || !ownsSession(signedIn)) throw new Error('Session changed')
      bindDocumentSession(signedIn, true)
    } catch (error) {
      if (ownsSession(owner)) clear()
      throw error
    }
  }

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
        requireMutationSuccess(await withSessionOwner(owner, logout))
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

  return { state, crossTabChanged, restore, refresh, signIn, signOut, clear, capture, owns, changed }
}
