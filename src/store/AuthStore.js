import { create } from 'zustand'

const STORAGE_KEY = 'ss_auth'

export const useAuthStore = create((set) => ({
  user: null,
  token: null,
  isAuthenticated: false,
  initialized: false,   // true once loadFromStorage has run

  setAuth: (data) => {
    const { token, ...user } = data
    if (token) localStorage.setItem('authToken', token)
    localStorage.setItem(STORAGE_KEY, JSON.stringify(user))
    set({ user, token, isAuthenticated: true, initialized: true })
  },

  logout: () => {
    localStorage.removeItem('authToken')
    localStorage.removeItem(STORAGE_KEY)
    set({ user: null, token: null, isAuthenticated: false, initialized: true })
  },

  updateUser: (updates) => set((state) => {
    const updatedUser = { ...state.user, ...updates }
    localStorage.setItem(STORAGE_KEY, JSON.stringify(updatedUser))
    return { user: updatedUser }
  }),

  // Called on app mount — must complete before ProtectedRoute evaluates
  loadFromStorage: () => {
    try {
      const raw   = localStorage.getItem(STORAGE_KEY)
      const token = localStorage.getItem('authToken')
      if (raw && token) {
        set({ user: JSON.parse(raw), token, isAuthenticated: true, initialized: true })
        return
      }
    } catch {
      // corrupted storage — ignore
    }
    set({ initialized: true })
  },
}))
