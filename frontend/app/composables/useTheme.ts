import { createSharedComposable, useMediaQuery, useMounted, useStorage } from '@vueuse/core'
import { DARK_THEME_QUERY, resolveTheme, THEME_STORAGE_KEY } from '@/lib/theme'

export const useTheme = createSharedComposable(() => {
  const preference = useStorage(THEME_STORAGE_KEY, '', undefined, {
    initOnMounted: true,
    writeDefaults: false,
    onError: () => {}, // The toggle also works when the browser blocks storage.
  })
  const mounted = useMounted()
  const prefersDark = useMediaQuery(DARK_THEME_QUERY)
  const theme = computed(() => mounted.value ? resolveTheme(preference.value, prefersDark.value) : 'light')

  if (import.meta.client) {
    watchEffect(() => {
      if (!mounted.value) return
      const value = theme.value
      const root = document.documentElement
      root.classList.toggle('dark', value === 'dark')
      root.style.colorScheme = value
    }, { flush: 'sync' })
  }

  function toggleTheme() {
    preference.value = theme.value === 'dark' ? 'light' : 'dark'
  }

  return { theme, toggleTheme }
})
