export default defineNuxtRouteMiddleware(async (to) => {
  if (import.meta.server) return
  const session = useSession()
  try {
    if (await session.restore()) return
  } catch {
    session.clear()
  }
  return navigateTo({ path: '/login', query: { redirect: to.fullPath } }, { replace: true })
})
