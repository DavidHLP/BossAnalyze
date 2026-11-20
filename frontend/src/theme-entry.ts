import { applyTheme, DARK_THEME_QUERY, resolveTheme, THEME_STORAGE_KEY } from './lib/theme'

let preference: string | null = null
try { preference = localStorage.getItem(THEME_STORAGE_KEY) } catch { /* Storage may be blocked. */ }
applyTheme(resolveTheme(preference, matchMedia(DARK_THEME_QUERY).matches))
