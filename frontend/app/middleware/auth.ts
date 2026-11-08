import { findMenu, loginLocation } from '@/lib/session'

export default defineNuxtRouteMiddleware(async (to) => {
  if (import.meta.server) return
  const session = useSession()
  try {
    if (!(await session.restore())) {
      return navigateTo(loginLocation(to.fullPath), { replace: true })
    }
  } catch (error) {
    if (error && typeof error === 'object' && 'code' in error && error.code === 401) {
      session.clear()
      return navigateTo(loginLocation(to.fullPath, 'expired'), { replace: true })
    }
    throw createError({ fatal: true, statusCode: 503, message: '会话恢复失败，请重试', data: { sessionRestore: true, redirect: to.fullPath } })
  }
  if (to.path !== '/console' && !findMenu(session.state.value.menus, to.path)) {
    return abortNavigation(createError({ fatal: true, statusCode: 403, message: '无权访问此页面' }))
  }
})
