export type Theme = 'light' | 'dark'

export const THEME_STORAGE_KEY = 'bossanalyze_theme'
export const DARK_THEME_QUERY = '(prefers-color-scheme: dark)'

export function resolveTheme(stored: string | null, prefersDark: boolean): Theme {
  if (stored === 'light' || stored === 'dark') return stored
  return prefersDark ? 'dark' : 'light'
}

export function applyTheme(theme: Theme, root = document.documentElement) {
  root.classList.toggle('dark', theme === 'dark')
  root.style.colorScheme = theme
}
