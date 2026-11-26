import type { Router } from 'vue-router'
import type { useSession } from './useSession'

const dirtyDrafts = new Map<string, { isDirty: () => boolean }>()

export function registerDirtyDraft(id: string, isDirty: () => boolean): () => void {
  const draft = { isDirty }
  dirtyDrafts.set(id, draft)
  return () => {
    if (dirtyDrafts.get(id) === draft) dirtyDrafts.delete(id)
  }
}

export function hasDirtyDraft(): boolean {
  for (const draft of dirtyDrafts.values()) {
    if (draft.isDirty()) return true
  }
  return false
}

export function listenToSessionEvents(session: ReturnType<typeof useSession>, router: Router) {
  function unauthorized() {
    if (session.crossTabChanged.value) return
    const route = router.currentRoute.value
    if (route.path !== '/login') void session.signOut(true, route.fullPath, 'expired')
  }
  function storageChanged(event: StorageEvent) {
    if (event.storageArea !== localStorage) return
    if (event.key !== 'token' && event.key !== 'token_timestamp' && event.key !== null) return
    session.changed()
    if (event.key === 'token_timestamp') return
    if (hasDirtyDraft()) {
      session.crossTabChanged.value = event.newValue ? 'changed' : 'cleared'
    } else {
      window.location.reload()
    }
  }
  window.addEventListener('unauthorized', unauthorized)
  window.addEventListener('storage', storageChanged)
  return () => {
    window.removeEventListener('unauthorized', unauthorized)
    window.removeEventListener('storage', storageChanged)
  }
}
