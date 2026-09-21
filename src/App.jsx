import { useEffect } from 'react'
import AppRouter from './router/AppRouter'
import { useAuthStore } from './store/AuthStore'

function App() {
  const loadFromStorage = useAuthStore((s) => s.loadFromStorage)

  useEffect(() => {
    loadFromStorage()
  }, [loadFromStorage])

  return <AppRouter />
}

export default App
