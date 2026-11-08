import { sessionFailure } from '@/lib/session'

export default defineNuxtRouteMiddleware(async (to) => {
  if (import.meta.server) return
  const session = useSession()
  try {
    if (await session.restore()) return
  } catch (error) {
    if (sessionFailure(error) === 'expired') session.clear()
    else
      return abortNavigation(
        createError({ statusCode: 503, message: '会话暂时无法恢复，请稍后重试' }),
      )
  }
  return navigateTo({ path: '/login', query: { redirect: to.fullPath } }, { replace: true })
})
