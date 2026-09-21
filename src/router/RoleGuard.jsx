import { Navigate } from 'react-router-dom'
import { useAuthStore } from '../store/AuthStore'

const ROLE_HOME = {
  CANDIDATE: '/dashboard/candidate',
  EMPLOYER: '/dashboard/employer',
  ADMIN: '/dashboard/admin',
}

/**
 * @param {{ roles: string[], children: import('react').ReactNode }} props
 */
export default function RoleGuard({ roles, children }) {
  const user = useAuthStore((s) => s.user)

  if (!user) return <Navigate to="/login" replace />

  if (!roles.includes(user.role)) {
    return <Navigate to={ROLE_HOME[user.role] ?? '/login'} replace />
  }

  return children
}
