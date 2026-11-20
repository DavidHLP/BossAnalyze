export type Theme = 'light' | 'dark'

export const THEME_STORAGE_KEY = 'bossanalyze_theme'

export function resolveTheme(stored: string | null, prefersDark: boolean): Theme {
  if (stored === 'light' || stored === 'dark') return stored
  return prefersDark ? 'dark' : 'light'
}
