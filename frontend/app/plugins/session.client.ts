export default defineNuxtPlugin((nuxtApp) => {
  const session = useSession()
  const router = useRouter()
  const onUnauthorized = () => {
    if (router.currentRoute.value.path === '/login') return
    void session.signOut(true, router.currentRoute.value.fullPath, 'expired')
  }
  window.addEventListener('unauthorized', onUnauthorized)
  const onStorage = (event: StorageEvent) => {
    if (event.storageArea !== localStorage) return
    if (event.key === 'token' || event.key === 'token_timestamp' || event.key === null) {
      session.changed()
      if (event.key !== 'token_timestamp') window.location.reload()
    }
  }
  window.addEventListener('storage', onStorage)
  nuxtApp.vueApp.onUnmount(() => {
    window.removeEventListener('unauthorized', onUnauthorized)
    window.removeEventListener('storage', onStorage)
  })
})
