import { findMenu, sessionFailure } from '@/lib/session'

export default defineNuxtRouteMiddleware(async (to) => {
  if (import.meta.server) return
  const session = useSession()
  try {
    if (!(await session.restore())) {
      return navigateTo({ path: '/login', query: { redirect: to.fullPath } }, { replace: true })
    }
  } catch (error) {
    if (sessionFailure(error) === 'expired') {
      session.clear()
      return navigateTo({ path: '/login', query: { reason: 'expired' } }, { replace: true })
    }
    // Transient failure: keep the possibly valid token and block the protected view.
    return abortNavigation(
      createError({ statusCode: 503, message: '会话暂时无法恢复，请稍后重试' }),
    )
  }
  if (to.path !== '/console' && !findMenu(session.state.value.menus, to.path)) {
    return abortNavigation(createError({ statusCode: 403, message: '无权访问此页面' }))
  }
})
