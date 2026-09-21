import { Navigate, useLocation } from 'react-router-dom'
import { useAuthStore } from '../store/AuthStore'

/** @param {{ children: import('react').ReactNode }} props */
export default function ProtectedRoute({ children }) {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const initialized     = useAuthStore((s) => s.initialized)
  const location = useLocation()

  // Wait for localStorage restore before deciding — prevents flicker to /login on refresh
  if (!initialized) return null

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  return children
}