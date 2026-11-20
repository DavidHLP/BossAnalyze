import type { Router } from 'vue-router'
import type { useSession } from './useSession'

export function listenToSessionEvents(session: ReturnType<typeof useSession>, router: Router) {
  function unauthorized() {
    const route = router.currentRoute.value
    if (route.path !== '/login') void session.signOut(true, route.fullPath, 'expired')
  }
  function storageChanged(event: StorageEvent) {
    if (event.storageArea !== localStorage) return
    if (event.key === 'token' || event.key === 'token_timestamp' || event.key === null) {
      session.changed()
      if (event.key !== 'token_timestamp') window.location.reload()
    }
  }
  window.addEventListener('unauthorized', unauthorized)
  window.addEventListener('storage', storageChanged)
  return () => {
    window.removeEventListener('unauthorized', unauthorized)
    window.removeEventListener('storage', storageChanged)
  }
}
