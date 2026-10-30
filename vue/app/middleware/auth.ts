import { findMenu } from '@/lib/session'

export default defineNuxtRouteMiddleware(async (to) => {
  if (import.meta.server) return
  const session = useSession()
  try {
    if (!(await session.restore())) {
      return navigateTo({ path: '/login', query: { redirect: to.fullPath } }, { replace: true })
    }
  } catch {
    session.clear()
    return navigateTo({ path: '/login', query: { reason: 'session' } }, { replace: true })
  }
  if (to.path !== '/console' && !findMenu(session.state.value.menus, to.path)) {
    return abortNavigation(createError({ statusCode: 403, statusMessage: '无权访问此页面' }))
  }
})
