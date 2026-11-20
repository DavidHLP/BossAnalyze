export type Theme = 'light' | 'dark'

export const THEME_STORAGE_KEY = 'bossanalyze_theme'
export const DARK_THEME_QUERY = '(prefers-color-scheme: dark)'

export function resolveTheme(stored: string | null, prefersDark: boolean): Theme {
  if (stored === 'light' || stored === 'dark') return stored
  return prefersDark ? 'dark' : 'light'
}

// Keep this synchronous so the theme is applied before the page is painted.
export const themeHeadScript = `(() => {
  let theme
  try { theme = localStorage.getItem(${JSON.stringify(THEME_STORAGE_KEY)}) } catch {}
  if (theme !== 'light' && theme !== 'dark') {
    theme = matchMedia(${JSON.stringify(DARK_THEME_QUERY)}).matches ? 'dark' : 'light'
  }
  const root = document.documentElement
  root.classList.toggle('dark', theme === 'dark')
  root.style.colorScheme = theme
})()`
