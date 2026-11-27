import { computed, onScopeDispose, shallowRef, watch, type ComputedRef } from 'vue'
import { ownsResumeAsset, readResumeAsset } from '@/api/resume/assets'
import { useSession } from '@/composables/useSession'
import { resumeImageDataUrl } from '@/lib/resume-render'

export function useResumeImages(keys: ComputedRef<string[]>) {
  const session = useSession()
  const userId = computed(() => session.state.value.user?.id)
  const enabled = import.meta.env.VITE_RESUME_PRIVATE_IMAGES === 'true' ||
    (import.meta.env.DEV && import.meta.env.VITE_BOSS_MOCK_PREVIEW === '1')
  const images = shallowRef<Record<string, string>>({})
  const requests = new Map<string, Promise<string | undefined>>()

  function forgetImage(key: string) {
    requests.delete(key)
    if (!Object.hasOwn(images.value, key)) return
    const remaining = { ...images.value }
    delete remaining[key]
    images.value = remaining
  }

  async function loadImage(key: string) {
    const actor = userId.value
    const owner = session.capture()
    if (!enabled || actor === undefined || !session.owns(owner) || !ownsResumeAsset(actor, key)) return
    if (Object.hasOwn(images.value, key)) return images.value[key]
    const pending = requests.get(key)
    if (pending) return pending
    const request = readResumeAsset(actor, key).then(async blob => {
      if (!blob || requests.get(key) !== request || !session.owns(owner)) return
      const url = await resumeImageDataUrl(blob)
      if (requests.get(key) !== request || !session.owns(owner)) return
      images.value = { ...images.value, [key]: url }
      return url
    }).catch(() => undefined).finally(() => {
      if (requests.get(key) === request) requests.delete(key)
    })
    requests.set(key, request)
    return request
  }

  watch([() => JSON.stringify(keys.value), userId, session.crossTabChanged, () => session.state.value.invalidated], (_, previous) => {
    const current = new Set(keys.value)
    const owner = session.capture()
    const sameSession = userId.value === previous?.[1] && session.owns(owner)
    for (const key of new Set([...Object.keys(images.value), ...requests.keys()])) {
      if (!sameSession || !current.has(key)) forgetImage(key)
    }
    for (const key of current) void loadImage(key)
  }, { immediate: true })

  onScopeDispose(() => requests.clear())
  return { enabled, images, loadImage, forgetImage }
}
