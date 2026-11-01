export default defineNuxtPlugin((nuxtApp) => {
  const session = useSession()
  const onUnauthorized = () => {
    session.clear()
    void navigateTo({ path: '/login', query: { reason: 'expired' } }, { replace: true })
  }
  window.addEventListener('unauthorized', onUnauthorized)
  const onStorage = (event: StorageEvent) => {
    if (event.key === 'token') window.location.reload()
  }
  window.addEventListener('storage', onStorage)
  nuxtApp.vueApp.onUnmount(() => {
    window.removeEventListener('unauthorized', onUnauthorized)
    window.removeEventListener('storage', onStorage)
  })
})
