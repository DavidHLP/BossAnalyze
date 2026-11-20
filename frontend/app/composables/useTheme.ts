import { createSharedComposable, useMounted, usePreferredDark, useStorage } from '@vueuse/core'
import { resolveTheme, THEME_STORAGE_KEY } from '@/lib/theme'

export const useTheme = createSharedComposable(() => {
  const preference = useStorage(THEME_STORAGE_KEY, '', undefined, {
    initOnMounted: true,
    writeDefaults: false,
    onError: () => {}, // The toggle also works when the browser blocks storage.
  })
  const mounted = useMounted()
  const prefersDark = usePreferredDark()
  const theme = computed(() => mounted.value ? resolveTheme(preference.value, prefersDark.value) : 'light')

  if (import.meta.client) {
    watch(theme, (value) => {
      const root = document.documentElement
      root.classList.toggle('dark', value === 'dark')
      root.style.colorScheme = value
    }, { immediate: true, flush: 'sync' })
  }

  function toggleTheme() {
    preference.value = theme.value === 'dark' ? 'light' : 'dark'
  }

  return { theme, toggleTheme }
})
