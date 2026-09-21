import { useState, useEffect, useCallback } from 'react'
import { useNavigate, NavLink } from 'react-router-dom'
import {
  LayoutDashboard, Users, Briefcase, FileText, ShieldCheck, BarChart2,
  LogOut, Bell, TrendingUp, CheckCircle, XCircle, Clock,
  CircleUser, Search, AlertTriangle, Activity, ChevronRight,
  Loader2, Sun, Moon,
} from 'lucide-react'
import { useAuthStore } from '../../store/AuthStore'
import { usePreferencesStore } from '../../store/PreferencesStore'
import { getAdminStats, getAdminUsers, toggleUserStatus } from '../../api/AdminApi'
import { useTranslation } from '../../i18n/translations'
import './AdminStats.css'

const DOT_TYPE = { user: 'dot--user', job: 'dot--job', check: 'dot--check', alert: 'dot--alert', file: 'dot--file' }

function Sidebar({ onLogout, t }) {
  const { language, setLanguage, theme, toggleTheme } = usePreferencesStore()
  return (
    <aside className="ad-sidebar">
      <div className="ad-sidebar-logo">
        <div className="ad-logo-icon">S</div>
        <span className="ad-logo-text">SkillSet</span>
        <span className="ad-admin-badge">Admin</span>
      </div>

      <div className="ad-sidebar-prefs">
        <div className="ad-lang-switch" role="group" aria-label="Langue / Language">
          <button type="button" className={`ad-lang-btn${language === 'fr' ? ' ad-lang-btn--active' : ''}`} onClick={() => setLanguage('fr')}>FR</button>
          <button type="button" className={`ad-lang-btn${language === 'en' ? ' ad-lang-btn--active' : ''}`} onClick={() => setLanguage('en')}>EN</button>
        </div>
        <button type="button" className="ad-theme-btn" onClick={toggleTheme} aria-label="Toggle theme">
          {theme === 'light' ? <Moon size={15} /> : <Sun size={15} />}
        </button>
      </div>

      <nav className="ad-nav">
        <NavLink to="/dashboard/admin" end className={({ isActive }) => `ad-nav-item${isActive ? ' ad-nav-item--active' : ''}`}>
          <LayoutDashboard size={18} /> {t.nav.overview}
        </NavLink>
        <NavLink to="/admin/users" className={({ isActive }) => `ad-nav-item${isActive ? ' ad-nav-item--active' : ''}`}>
          <Users size={18} /> {t.nav.users}
        </NavLink>
        <NavLink to="/admin/moderation" className={({ isActive }) => `ad-nav-item${isActive ? ' ad-nav-item--active' : ''}`}>
          <ShieldCheck size={18} /> {t.nav.moderation}
        </NavLink>
        <NavLink to="/admin/analytics" className={({ isActive }) => `ad-nav-item${isActive ? ' ad-nav-item--active' : ''}`}>
          <BarChart2 size={18} /> {t.nav.analytics}
        </NavLink>
      </nav>
      <button className="ad-logout" onClick={onLogout}>
        <LogOut size={18} /> {t.logout}
      </button>
    </aside>
  )
}

export default function AdminStats() {
  const navigate = useNavigate()
  const { user, logout } = useAuthStore()
  const t = useTranslation().admin
  const [search, setSearch]             = useState('')
  const [stats, setStats]               = useState(null)
  const [users, setUsers]               = useState([])
  const [loading, setLoading]           = useState(true)
  const [usersLoading, setUsersLoading] = useState(false)
  const [error, setError]               = useState('')

  const handleLogout = () => { logout(); navigate('/login') }
  const adminName = user?.firstName || t.defaultAdminName

  useEffect(() => {
    getAdminStats()
      .then(data => { setStats(data); setUsers(data.recentUsers || []) })
      .catch(() => setError(t.loadStatsError))
      .finally(() => setLoading(false))
  }, [t.loadStatsError])

  const handleSearch = useCallback(async (q) => {
    setUsersLoading(true)
    try {
      const data = await getAdminUsers(q)
      setUsers(data)
    } catch {
      setError(t.searchError)
    } finally {
      setUsersLoading(false)
    }
  }, [t.searchError])

  useEffect(() => {
    const timer = setTimeout(() => handleSearch(search), 350)
    return () => clearTimeout(timer)
  }, [search, handleSearch])

  const handleToggle = async (userId) => {
    try {
      await toggleUserStatus(userId)
      setUsers(prev => prev.map(u =>
        u.id === userId ? { ...u, status: u.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE' } : u
      ))
      if (stats) {
        setStats(prev => ({ ...prev, activeUsers: prev.activeUsers + (users.find(u => u.id === userId)?.status === 'ACTIVE' ? -1 : 1) }))
      }
    } catch {
      setError(t.toggleStatusError)
    }
  }

  if (loading) {
    return (
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '100vh', background: 'var(--surface-page-alt)' }}>
        <Loader2 size={36} style={{ animation: 'spin 1s linear infinite', color: '#D97706' }} />
      </div>
    )
  }

  const totalUsers      = stats?.totalUsers        ?? 0
  const totalJobs       = stats?.totalJobs         ?? 0
  const totalApps       = stats?.totalApplications ?? 0
  const acceptedApps    = stats?.acceptedApplications ?? 0
  const totalCandidates = stats?.totalCandidates   ?? 0
  const totalEmployers  = stats?.totalEmployers    ?? 0
  const activity        = stats?.recentActivity    ?? []

  const candidatePct = totalUsers > 0 ? Math.round((totalCandidates / totalUsers) * 100) : 0
  const employerPct  = totalUsers > 0 ? Math.round((totalEmployers  / totalUsers) * 100) : 0
  const adminPct     = totalUsers > 0 ? Math.round(((totalUsers - totalCandidates - totalEmployers) / totalUsers) * 100) : 0

  const ROLE_META = {
    CANDIDATE: { label: t.roles.CANDIDATE, cls: 'role--candidate' },
    EMPLOYER:  { label: t.roles.EMPLOYER,  cls: 'role--employer'  },
    ADMIN:     { label: t.roles.ADMIN,     cls: 'role--admin'     },
  }

  return (
    <div className="ad-shell">
      <Sidebar onLogout={handleLogout} t={t} />

      <main className="ad-main">
        {/* Header */}
        <div className="ad-header">
          <div>
            <p className="ad-greeting">
              {t.panelPrefix} <span className="ad-greeting-name">{adminName}</span>
            </p>
            <p className="ad-greeting-sub">{t.manageSub}</p>
          </div>
          <div className="ad-header-right">
            <button className="ad-icon-btn" aria-label="Notifications" onClick={() => navigate('/notifications')}><Bell size={18} /></button>
            <CircleUser size={32} className="ad-avatar" />
          </div>
        </div>

        {error && (
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '12px 16px', background: '#fde8ea', border: '1px solid #f5c0c8', borderRadius: 10, color: '#c42033', fontSize: 14, marginBottom: 24 }}>
            <AlertTriangle size={18} style={{ flexShrink: 0 }} />
            <p style={{ margin: 0 }}>{error}</p>
          </div>
        )}

        {/* Stats grid */}
        <div className="ad-stats">
          <div className="ad-stat-card">
            <div className="ad-stat-icon ad-stat-icon--amber"><Users size={20} /></div>
            <div>
              <div className="ad-stat-value">{totalUsers}</div>
              <div className="ad-stat-label">{t.stats.users}</div>
            </div>
            <div className="ad-stat-trend up"><TrendingUp size={12} /> {stats?.activeUsers ?? 0} {t.stats.active}</div>
          </div>
          <div className="ad-stat-card">
            <div className="ad-stat-icon ad-stat-icon--indigo"><Briefcase size={20} /></div>
            <div>
              <div className="ad-stat-value">{totalJobs}</div>
              <div className="ad-stat-label">{t.stats.jobs}</div>
            </div>
            <div className="ad-stat-trend up"><TrendingUp size={12} /> {stats?.openJobs ?? 0} {t.stats.open}</div>
          </div>
          <div className="ad-stat-card">
            <div className="ad-stat-icon ad-stat-icon--cherry"><FileText size={20} /></div>
            <div>
              <div className="ad-stat-value">{totalApps}</div>
              <div className="ad-stat-label">{t.stats.applications}</div>
            </div>
            <div className="ad-stat-trend up"><TrendingUp size={12} /> {stats?.pendingApplications ?? 0} {t.stats.pending}</div>
          </div>
          <div className="ad-stat-card">
            <div className="ad-stat-icon ad-stat-icon--green"><CheckCircle size={20} /></div>
            <div>
              <div className="ad-stat-value">{acceptedApps}</div>
              <div className="ad-stat-label">{t.stats.completed}</div>
            </div>
          </div>
        </div>

        {/* Main grid */}
        <div className="ad-grid">
          {/* Users table */}
          <div className="ad-content">
            <div className="ad-tabs">
              <span className="ad-tab ad-tab--active">
                <Users size={15} /> {t.tabs.users}
              </span>
              <NavLink to="/admin/moderation" className="ad-tab">
                <ShieldCheck size={15} /> {t.tabs.moderation}
              </NavLink>
            </div>

            <div className="ad-search-bar">
              <Search size={16} className="ad-search-icon" />
              <input
                className="ad-search-input"
                placeholder={t.searchUserPlaceholder}
                value={search}
                onChange={e => setSearch(e.target.value)}
              />
              {usersLoading && <Loader2 size={16} style={{ animation: 'spin 1s linear infinite', color: '#D97706', flexShrink: 0 }} />}
            </div>
            <table className="ad-table">
              <thead>
                <tr>
                  <th>{t.table.user}</th>
                  <th>{t.table.role}</th>
                  <th>{t.table.status}</th>
                  <th>{t.table.joined}</th>
                  <th>{t.table.actions}</th>
                </tr>
              </thead>
              <tbody>
                {users.map(u => {
                  const roleMeta = ROLE_META[u.role] || ROLE_META.CANDIDATE
                  return (
                    <tr key={u.id}>
                      <td>
                        <div className="ad-user-cell">
                          <CircleUser size={22} className="ad-user-avatar" />
                          <div>
                            <div className="ad-user-name">{u.name}</div>
                            <div className="ad-user-email">{u.email}</div>
                          </div>
                        </div>
                      </td>
                      <td>
                        <span className={`ad-role-badge ${roleMeta.cls}`}>{roleMeta.label}</span>
                      </td>
                      <td>
                        <span className={`ad-status-badge ${u.status === 'ACTIVE' ? 'st--active' : 'st--inactive'}`}>
                          {u.status === 'ACTIVE' ? <CheckCircle size={12} /> : <XCircle size={12} />}
                          {u.status === 'ACTIVE' ? t.active : t.inactive}
                        </span>
                      </td>
                      <td><span className="ad-date">{u.joinedAt}</span></td>
                      <td>
                        <div className="ad-actions">
                          {u.status === 'ACTIVE'
                            ? <button onClick={() => handleToggle(u.id)} className="ad-action-btn ad-action-btn--danger">{t.deactivate}</button>
                            : <button onClick={() => handleToggle(u.id)} className="ad-action-btn ad-action-btn--success">{t.activate}</button>
                          }
                        </div>
                      </td>
                    </tr>
                  )
                })}
                {users.length === 0 && !usersLoading && (
                  <tr>
                    <td colSpan={5}>
                      <div className="ad-empty">
                        <Users size={36} className="ad-empty-icon" />
                        <p>{t.noUsersFound}</p>
                      </div>
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
            <NavLink to="/admin/users" className="ad-see-all">
              {t.manageAllUsers} <ChevronRight size={16} />
            </NavLink>

          </div>

          {/* Activity sidebar */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
            {/* Activity feed */}
            <div className="ad-activity">
              <div className="ad-section-title">
                <Activity size={16} /> {t.recentActivity}
              </div>
              <div className="ad-feed">
                {activity.length === 0
                  ? <span style={{ fontSize: 13, color: 'var(--text-muted)' }}>{t.noRecentActivity}</span>
                  : activity.map((a, i) => (
                    <div key={a.id || i} className="ad-feed-item">
                      <div className={`ad-feed-dot ${DOT_TYPE[a.type] ?? 'dot--check'}`} />
                      <div>
                        <div className="ad-feed-msg">{a.message}</div>
                        <div className="ad-feed-time"><Clock size={11} /> {a.time}</div>
                      </div>
                    </div>
                  ))
                }
              </div>
            </div>

            {/* User distribution bars */}
            <div className="ad-activity">
              <div className="ad-section-title"><TrendingUp size={16} /> {t.distribution}</div>
              <div className="ad-quick-bar">
                <div className="ad-bar-item">
                  <span className="ad-bar-label">{t.candidates}</span>
                  <div className="ad-bar-track">
                    <div className="ad-bar-fill ad-bar-fill--cherry" style={{ width: `${candidatePct}%` }} />
                  </div>
                  <span className="ad-bar-val">{totalCandidates}</span>
                </div>
                <div className="ad-bar-item">
                  <span className="ad-bar-label">{t.employers}</span>
                  <div className="ad-bar-track">
                    <div className="ad-bar-fill ad-bar-fill--indigo" style={{ width: `${employerPct}%` }} />
                  </div>
                  <span className="ad-bar-val">{totalEmployers}</span>
                </div>
                <div className="ad-bar-item">
                  <span className="ad-bar-label">{t.admins}</span>
                  <div className="ad-bar-track">
                    <div className="ad-bar-fill ad-bar-fill--amber" style={{ width: `${adminPct}%` }} />
                  </div>
                  <span className="ad-bar-val">{totalUsers - totalCandidates - totalEmployers}</span>
                </div>
              </div>
            </div>

            {/* Alert */}
            <div className="ad-alert">
              <AlertTriangle size={16} style={{ flexShrink: 0 }} />
              {stats?.pendingApplications ?? 0} {t.pendingApplicationsAlert}
            </div>
          </div>
        </div>
      </main>
    </div>
  )
}
