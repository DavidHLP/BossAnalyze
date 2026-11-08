import { loginLocation } from '@/lib/session'
export default defineNuxtRouteMiddleware(async (to) => {
  if (import.meta.server) return
  const session = useSession()
  try {
    if (await session.restore()) return
  } catch (error) {
    if (error && typeof error === 'object' && 'code' in error && error.code === 401) {
      return navigateTo(loginLocation(to.fullPath, 'expired'), { replace: true })
    }
    throw createError({ fatal: true, statusCode: 503, message: '会话恢复失败，请重试', data: { sessionRestore: true, redirect: to.fullPath } })
  }
  return navigateTo(loginLocation(to.fullPath), { replace: true })
})
