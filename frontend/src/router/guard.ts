import type { RouteLocationNormalized, RouteLocationRaw } from 'vue-router'
import type { useSession } from '../composables/useSession'
import { findMenu, loginLocation, safeReturnPath } from '../lib/session.ts'

type Session = Pick<ReturnType<typeof useSession>, 'state' | 'restore'>
type Destination = Pick<RouteLocationNormalized, 'fullPath' | 'path'>

export async function authorizeRoute(session: Session, to: Destination): Promise<RouteLocationRaw | true> {
  try {
    if (!(await session.restore())) return { ...loginLocation(to.fullPath), replace: true }
  } catch (error) {
    if (error && typeof error === 'object' && 'code' in error && error.code === 401) {
      return { ...loginLocation(to.fullPath, 'expired'), replace: true }
    }
    const redirect = safeReturnPath(to.fullPath) ?? '/console'
    return { name: 'system-error', params: { code: '503' }, query: { redirect }, replace: true }
  }
  if (to.path.startsWith('/console/') && !findMenu(session.state.value.menus, to.path)) {
    return { name: 'system-error', params: { code: '403' }, replace: true }
  }
  return true
}
