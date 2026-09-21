import { useEffect } from 'react'
import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom'
import { useAuthStore }    from '../store/AuthStore'
import { useUserDataStore } from '../store/UserDataStore'
import ChatbotWidget       from '../features/chatbot/ChatbotWidget'
import LoginPage           from '../pages/shared/LoginPage'
import RegisterPage        from '../pages/shared/RegisterPage'
import OnboardingPage      from '../pages/shared/OnboardingPage'
import MessageCenter       from '../pages/shared/MessageCenter'
import NotificationsPage   from '../pages/shared/NotificationsPage'
import SettingsPage        from '../pages/shared/SettingsPage'
import CandidateDashboard  from '../pages/candidate/CandidateDashboard'
import ProfileSettings     from '../pages/candidate/ProfileSettings'
import JobSearch           from '../pages/candidate/JobSearch'
import MyApplications      from '../pages/candidate/MyApplications'
import EmployerDashboard   from '../pages/employer/EmployerDashboard'
import PostJobPage         from '../pages/employer/PostJobPage'
import EmployerJobs        from '../pages/employer/EmployerJobs'
import CandidateReview     from '../pages/employer/CandidateReview'
import CompanyProfile      from '../pages/employer/CompanyProfile'
import AdminStats             from '../pages/admin/AdminStats'
import AdminAnalytics         from '../pages/admin/AdminAnalytics'
import ModerationPanel        from '../pages/admin/ModerationPanel'
import UserManagement         from '../pages/admin/UserManagement'
import CareersPage            from '../pages/public/CareersPage'
import HomePage                from '../pages/public/HomePage'
import NotificationListener   from '../features/notifications/NotificationListener'
import ConsentBanner          from '../features/consent/ConsentBanner'
import ProtectedRoute         from './ProtectedRoute'
import RoleGuard              from './RoleGuard'
import TalentPoolList         from '../features/talentpool/TalentPoolList'
import TalentPoolDetail       from '../features/talentpool/TalentPoolDetail'
import IntegrationsPage       from '../features/integrations/IntegrationsPage'
import GoogleAuthCallback     from '../features/integrations/GoogleAuthCallback'

const NO_STELLA_ROUTES = ['/login', '/register', '/onboarding']

// Renders STELLA on every authenticated page without remounting (preserves chat history)
function GlobalChatbot() {
  const { user }     = useAuthStore()
  const { loadUser } = useUserDataStore()
  const { pathname } = useLocation()

  useEffect(() => {
    if (user?.id) loadUser(user.id)
  }, [user?.id, loadUser])

  if (!user) return null
  if (NO_STELLA_ROUTES.includes(pathname)) return null

  return <ChatbotWidget key={user.id} />
}

const AppRouter = () => (
  <BrowserRouter>
    <Routes>
      {/* Public */}
      <Route path="/"                  element={<HomePage />} />
      <Route path="/login"             element={<LoginPage />} />
      <Route path="/register"          element={<RegisterPage />} />
      <Route path="/careers/:slug"     element={<CareersPage />} />

      {/* Onboarding */}
      <Route path="/onboarding" element={
        <ProtectedRoute><OnboardingPage /></ProtectedRoute>
      } />

      {/* ── Candidate ── */}
      <Route path="/dashboard/candidate" element={
        <ProtectedRoute><RoleGuard roles={['CANDIDATE']}><CandidateDashboard /></RoleGuard></ProtectedRoute>
      } />
      <Route path="/jobs" element={
        <ProtectedRoute><RoleGuard roles={['CANDIDATE']}><JobSearch /></RoleGuard></ProtectedRoute>
      } />
      <Route path="/my-jobs" element={
        <ProtectedRoute><RoleGuard roles={['CANDIDATE']}><MyApplications /></RoleGuard></ProtectedRoute>
      } />
      <Route path="/applications" element={
        <ProtectedRoute><RoleGuard roles={['CANDIDATE']}><MyApplications /></RoleGuard></ProtectedRoute>
      } />
      <Route path="/profile" element={
        <ProtectedRoute><ProfileSettings /></ProtectedRoute>
      } />

      {/* ── Employer ── */}
      <Route path="/dashboard/employer" element={
        <ProtectedRoute><RoleGuard roles={['EMPLOYER']}><EmployerDashboard /></RoleGuard></ProtectedRoute>
      } />
      <Route path="/employer/jobs" element={
        <ProtectedRoute><RoleGuard roles={['EMPLOYER']}><EmployerJobs /></RoleGuard></ProtectedRoute>
      } />
      <Route path="/employer/jobs/new" element={
        <ProtectedRoute><RoleGuard roles={['EMPLOYER']}><PostJobPage /></RoleGuard></ProtectedRoute>
      } />
      <Route path="/employer/candidates" element={
        <ProtectedRoute><RoleGuard roles={['EMPLOYER']}><CandidateReview /></RoleGuard></ProtectedRoute>
      } />
      <Route path="/employer/company" element={
        <ProtectedRoute><RoleGuard roles={['EMPLOYER']}><CompanyProfile /></RoleGuard></ProtectedRoute>
      } />
      <Route path="/employer/talent-pools" element={
        <ProtectedRoute><RoleGuard roles={['EMPLOYER']}><TalentPoolList /></RoleGuard></ProtectedRoute>
      } />
      <Route path="/employer/talent-pools/:poolId" element={
        <ProtectedRoute><RoleGuard roles={['EMPLOYER']}><TalentPoolDetail /></RoleGuard></ProtectedRoute>
      } />

      {/* ── Shared ── */}
      <Route path="/messages" element={
        <ProtectedRoute><MessageCenter /></ProtectedRoute>
      } />
      <Route path="/notifications" element={
        <ProtectedRoute><NotificationsPage /></ProtectedRoute>
      } />
      <Route path="/settings" element={
        <ProtectedRoute><SettingsPage /></ProtectedRoute>
      } />
      <Route path="/settings/integrations" element={
        <ProtectedRoute><IntegrationsPage /></ProtectedRoute>
      } />

      {/* ── OAuth Callbacks ── */}
      <Route path="/oauth/callback/google" element={
        <ProtectedRoute><GoogleAuthCallback /></ProtectedRoute>
      } />

      {/* ── Admin ── */}
      <Route path="/dashboard/admin" element={
        <ProtectedRoute><RoleGuard roles={['ADMIN']}><AdminStats /></RoleGuard></ProtectedRoute>
      } />
      <Route path="/admin/users" element={
        <ProtectedRoute><RoleGuard roles={['ADMIN']}><UserManagement /></RoleGuard></ProtectedRoute>
      } />
      <Route path="/admin/moderation" element={
        <ProtectedRoute><RoleGuard roles={['ADMIN']}><ModerationPanel /></RoleGuard></ProtectedRoute>
      } />
      <Route path="/admin/analytics" element={
        <ProtectedRoute><RoleGuard roles={['ADMIN']}><AdminAnalytics /></RoleGuard></ProtectedRoute>
      } />

      {/* Fallback */}
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>

    {/* STELLA persists across all pages — rendered outside <Routes> so it never remounts */}
    <GlobalChatbot />
    {/* WebSocket notification listener — subscribes to /topic/notifications/{userId} */}
    <NotificationListener />
    {/* RGPD consent banner — shown once to authenticated users */}
    <ConsentBanner />
  </BrowserRouter>
)

export default AppRouter
