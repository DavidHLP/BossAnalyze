import { computed, onMounted, onUnmounted, ref, shallowRef } from 'vue'
import { permissionDenied } from '@/lib/rbac-mutations'

const cooldownMs = 30_000
const receiptChanged = 'bossanalyze:log-trigger-changed'

type Attempt = {
  status: 'pending' | 'accepted' | 'unconfirmed'
  until: number
}

export function useLogTrigger(account: string | null) {
  const key = account === null ? null : 'bossanalyze:log-trigger:' + account
  const available = Boolean(key && navigator.locks)
  const attempt = shallowRef<Attempt | null>(null)
  const now = ref(Date.now())
  const error = ref('')
  const remaining = computed(() => Math.max(0, Math.ceil(((attempt.value?.until ?? 0) - now.value) / 1000)))
  const status = computed(() => remaining.value ? attempt.value?.status : undefined)

  function read() {
    const raw = key && localStorage.getItem(key)
    if (!raw) return null
    try {
      const value = JSON.parse(raw)
      if ((value?.status === 'pending' || value?.status === 'accepted' || value?.status === 'unconfirmed') &&
          Number.isFinite(value.until) && value.until > Date.now()) return value as Attempt
    } catch {
      // A malformed record carries no receipt.
    }
    return null
  }

  function sync() {
    now.value = Date.now()
    attempt.value = read()
  }
  function save(storageKey: string, next: Attempt | null) {
    if (next) localStorage.setItem(storageKey, JSON.stringify(next))
    else localStorage.removeItem(storageKey)
    // The storage event only reaches other documents.
    window.dispatchEvent(new Event(receiptChanged))
  }
  function storageChanged(event: StorageEvent) {
    if (event.storageArea === localStorage && (event.key === key || event.key === null)) sync()
  }
  let timer: ReturnType<typeof setInterval>
  onMounted(() => {
    sync()
    window.addEventListener('storage', storageChanged)
    window.addEventListener(receiptChanged, sync)
    timer = setInterval(() => { now.value = Date.now() }, 250)
    if (key && navigator.locks && attempt.value?.status === 'pending') {
      // A reload can lose the response. Only an abandoned attempt is still pending once we hold the lock.
      void navigator.locks.request(key, () => {
        const previous = read()
        if (previous?.status === 'pending') save(key, { ...previous, status: 'unconfirmed' })
      }).catch(() => {
        error.value = '无法同步日志触发状态，请稍后重试。'
      })
    }
  })
  onUnmounted(() => {
    window.removeEventListener('storage', storageChanged)
    window.removeEventListener(receiptChanged, sync)
    clearInterval(timer)
  })

  async function start(canSend: () => boolean, send: () => Promise<void>) {
    if (!key || !navigator.locks || !canSend()) return
    error.value = ''
    try {
      await navigator.locks.request(key, { ifAvailable: true }, async lock => {
        if (!lock || read() || !canSend()) return
        // Keep the account lock through the response and leave a guard if this tab is closed.
        save(key, { status: 'pending', until: Date.now() + cooldownMs })
        try {
          await send()
        } catch (cause) {
          if (permissionDenied(cause)) {
            save(key, null)
            error.value = '当前账号缺少手动触发权限。'
          } else {
            save(key, { status: 'unconfirmed', until: Date.now() + cooldownMs })
          }
          return
        }
        save(key, { status: 'accepted', until: Date.now() + cooldownMs })
      })
    } catch {
      error.value = '无法同步日志触发状态，请稍后重试。'
    } finally {
      sync()
    }
  }
  return { available, status, remaining, error, start }
}
