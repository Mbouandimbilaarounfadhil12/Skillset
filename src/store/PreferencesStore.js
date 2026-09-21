import { create } from 'zustand'

const LANG_KEY  = 'ss_lang'
const THEME_KEY = 'ss_theme'

const getInitialLanguage = () => {
  const saved = localStorage.getItem(LANG_KEY)
  if (saved === 'fr' || saved === 'en') return saved
  return navigator.language?.startsWith('fr') ? 'fr' : 'en'
}

const getInitialTheme = () => {
  const saved = localStorage.getItem(THEME_KEY)
  if (saved === 'light' || saved === 'dark') return saved
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

const applyTheme = (theme) => {
  document.documentElement.setAttribute('data-theme', theme)
}

const initialTheme = getInitialTheme()
applyTheme(initialTheme)

export const usePreferencesStore = create((set, get) => ({
  language: getInitialLanguage(),
  theme: initialTheme,

  setLanguage: (language) => {
    localStorage.setItem(LANG_KEY, language)
    set({ language })
  },

  toggleTheme: () => {
    const next = get().theme === 'light' ? 'dark' : 'light'
    localStorage.setItem(THEME_KEY, next)
    applyTheme(next)
    set({ theme: next })
  },
}))
