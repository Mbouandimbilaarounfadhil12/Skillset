import { useState, useEffect } from 'react'
import { useNavigate, NavLink } from 'react-router-dom'
import {
  BarChart, Bar, Cell, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid,
  LineChart, Line,
} from 'recharts'
import {
  LayoutDashboard, Users, ShieldCheck, BarChart2,
  LogOut, Bell, Clock, TrendingUp, CircleUser, Loader2, AlertTriangle, Download,
  Sun, Moon,
} from 'lucide-react'
import { useAuthStore }                      from '../../store/AuthStore'
import { usePreferencesStore }               from '../../store/PreferencesStore'
import { getAdminAnalytics, exportApplicationsCsv } from '../../api/AdminApi'
import { useTranslation } from '../../i18n/translations'
import './AdminStats.css'
import './AdminAnalytics.css'

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

const FUNNEL_COLORS = {
  SUBMITTED: '#2b4fbf',
  SCREENING: '#a05a00',
  INTERVIEW: '#7c3aed',
  OFFER:     '#0d7a5f',
  APPROVED:  '#1a6e44',
  REJECTED:  '#c42033',
}

export default function AdminAnalytics() {
  const navigate   = useNavigate()
  const { user, logout } = useAuthStore()
  const t = useTranslation().admin
  const ta = t.analytics
  const [data,       setData]       = useState(null)
  const [loading,    setLoading]    = useState(true)
  const [error,      setError]      = useState('')
  const [exporting,  setExporting]  = useState(false)

  const handleLogout = () => { logout(); navigate('/login') }

  const fmtHours = (h) => {
    if (h == null) return '—'
    if (h < 24) return `${Math.round(h)} ${ta.hUnit}`
    return `${(h / 24).toFixed(1)} ${ta.dUnit}`
  }

  useEffect(() => {
    getAdminAnalytics()
      .then(setData)
      .catch(() => setError(ta.loadError))
      .finally(() => setLoading(false))
  }, [ta.loadError])

  const handleExportCsv = async () => {
    setExporting(true)
    try {
      const blob = await exportApplicationsCsv()
      const url  = URL.createObjectURL(blob)
      const a    = document.createElement('a')
      a.href     = url
      a.download = 'candidatures.csv'
      a.click()
      URL.revokeObjectURL(url)
    } catch {
      setError(ta.exportError)
    } finally {
      setExporting(false)
    }
  }

  if (loading) {
    return (
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: '100vh', background: 'var(--surface-page-alt)' }}>
        <Loader2 size={36} style={{ animation: 'spin 1s linear infinite', color: '#D97706' }} />
      </div>
    )
  }

  const funnelData = data?.conversionFunnel
    ? Object.entries(data.conversionFunnel).map(([key, value]) => ({
        name:  ta.funnelLabels[key] ?? key,
        key,
        count: value,
        fill:  FUNNEL_COLORS[key] ?? '#888',
      }))
    : []

  const weeklyData = data?.weeklyApplications ?? []
  const topJobs    = data?.topJobs ?? []
  const rates      = data?.conversionRates ?? {}
  const totalApps  = Object.values(data?.conversionFunnel ?? {}).reduce((a, b) => a + b, 0)

  return (
    <div className="ad-shell">
      <Sidebar onLogout={handleLogout} t={t} />

      <main className="ad-main">
        <div className="ad-header">
          <div>
            <p className="ad-greeting">{ta.pagePrefix} <span className="ad-greeting-name">{user?.firstName || 'Admin'}</span></p>
            <p className="ad-greeting-sub">{ta.pageSub}</p>
          </div>
          <div className="ad-header-right">
            <button
              className="an-export-btn"
              onClick={handleExportCsv}
              disabled={exporting}
              title={ta.exportTitle}
            >
              {exporting
                ? <Loader2 size={15} className="an-spin" />
                : <Download size={15} />}
              {exporting ? ta.exporting : ta.exportCsv}
            </button>
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

        {/* KPI row */}
        <div className="an-grid">
          <div className="an-kpi">
            <div className="an-kpi-icon an-kpi-icon--blue"><Clock size={20} /></div>
            <div>
              <p className="an-kpi-label">{ta.avgTimeToHire}</p>
              <p className="an-kpi-value">{fmtHours(data?.avgTimeToHireHours)}</p>
              <p className="an-kpi-sub">{ta.avgTimeToHireSub}</p>
            </div>
          </div>
          <div className="an-kpi">
            <div className="an-kpi-icon an-kpi-icon--green"><TrendingUp size={20} /></div>
            <div>
              <p className="an-kpi-label">{ta.acceptedApplications}</p>
              <p className="an-kpi-value">{data?.conversionFunnel?.APPROVED ?? 0}</p>
              <p className="an-kpi-sub">{ta.outOfTotal.replace('{n}', totalApps)}</p>
            </div>
          </div>
        </div>

        {/* Weekly trend */}
        {weeklyData.length > 0 && (
          <div className="an-card">
            <p className="an-card-title">{ta.weeklyTitle}</p>
            <p className="an-card-sub">{ta.weeklySub}</p>
            <ResponsiveContainer width="100%" height={200}>
              <LineChart data={weeklyData} margin={{ top: 8, right: 16, bottom: 0, left: 0 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--surface-border)" />
                <XAxis dataKey="date" tick={{ fontSize: 12, fill: 'var(--text-secondary)' }} axisLine={false} tickLine={false} />
                <YAxis allowDecimals={false} tick={{ fontSize: 12, fill: 'var(--text-muted)' }} axisLine={false} tickLine={false} />
                <Tooltip
                  formatter={(v) => [v, ta.applicationsTooltip]}
                  contentStyle={{ borderRadius: 8, border: '1px solid var(--surface-border)', fontSize: 12, background: 'var(--surface-card)', color: 'var(--text-primary)' }}
                />
                <Line type="monotone" dataKey="count" stroke="#2b4fbf" strokeWidth={2} dot={{ r: 4, fill: '#2b4fbf' }} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        )}

        {/* Conversion funnel chart */}
        <div className="an-card">
          <p className="an-card-title">{ta.funnelTitle}</p>
          <p className="an-card-sub">{ta.funnelSub}</p>
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={funnelData} barCategoryGap="30%" margin={{ top: 8, right: 16, bottom: 0, left: 0 }}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="var(--surface-border)" />
              <XAxis dataKey="name" tick={{ fontSize: 12, fill: 'var(--text-secondary)' }} axisLine={false} tickLine={false} />
              <YAxis allowDecimals={false} tick={{ fontSize: 12, fill: 'var(--text-muted)' }} axisLine={false} tickLine={false} />
              <Tooltip
                formatter={(v) => [v, ta.applicationsTooltip]}
                contentStyle={{ borderRadius: 8, border: '1px solid var(--surface-border)', fontSize: 12, background: 'var(--surface-card)', color: 'var(--text-primary)' }}
              />
              <Bar dataKey="count" radius={[6, 6, 0, 0]}>
                {funnelData.map((entry) => (
                  <Cell key={entry.key} fill={entry.fill} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>

        {/* Conversion rates */}
        {Object.keys(rates).length > 0 && (
          <div className="an-card">
            <p className="an-card-title">{ta.ratesTitle}</p>
            <p className="an-card-sub">{ta.ratesSub}</p>
            <div className="an-rates">
              {Object.entries(rates).map(([key, rate]) => (
                <div key={key} className="an-rate-item">
                  <span className="an-rate-label">{ta.rateLabels[key] ?? key}</span>
                  <div className="an-rate-bar-wrap">
                    <div className="an-rate-bar" style={{ width: `${Math.min(rate, 100)}%` }} />
                  </div>
                  <span className="an-rate-pct">{rate}%</span>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Top jobs table */}
        {topJobs.length > 0 && (
          <div className="an-card">
            <p className="an-card-title">{ta.topJobsTitle}</p>
            <table className="an-table">
              <thead>
                <tr>
                  <th>{ta.jobColumn}</th>
                  <th className="an-th-num">{ta.acceptedColumn}</th>
                  <th className="an-th-num">{ta.avgDelayColumn}</th>
                </tr>
              </thead>
              <tbody>
                {topJobs.map((j) => (
                  <tr key={j.jobTitle}>
                    <td>{j.jobTitle}</td>
                    <td className="an-td-num">{j.applicationCount}</td>
                    <td className="an-td-num">{fmtHours(j.avgTimeToHireHours)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </main>
    </div>
  )
}
