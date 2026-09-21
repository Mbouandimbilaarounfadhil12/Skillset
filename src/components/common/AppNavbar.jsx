import { useState } from 'react'
import { NavLink, useNavigate } from 'react-router-dom'
import { Bell, MessageSquare, CircleUser, LogOut, Plus, Menu, X, Sun, Moon } from 'lucide-react'
import { useAuthStore }          from '../../store/AuthStore'
import { useNotificationStore }  from '../../store/NotificationStore'
import { useMessageStore }       from '../../store/MessageStore'
import { usePreferencesStore }   from '../../store/PreferencesStore'
import { useTranslation }        from '../../i18n/translations'
import './AppNavbar.css'

export default function AppNavbar() {
  const navigate = useNavigate()
  const { user, logout }             = useAuthStore()
  const { unreadCount: notifCount }  = useNotificationStore()
  const { totalUnread: msgCount }    = useMessageStore()
  const [drawerOpen, setDrawerOpen]  = useState(false)
  const { language, setLanguage, theme, toggleTheme } = usePreferencesStore()
  const t = useTranslation().app

  const role  = user?.role ?? 'CANDIDATE'
  const links = role === 'EMPLOYER' ? t.employerLinks : role === 'ADMIN' ? t.adminLinks : t.candidateLinks
  const home  = role === 'EMPLOYER' ? '/dashboard/employer' : role === 'ADMIN' ? '/dashboard/admin' : '/dashboard/candidate'

  const handleLogout = () => { logout(); navigate('/login') }

  return (
    <>
      <header className="an-navbar">
        {/* Logo */}
        <NavLink to={home} className="an-brand">
          <div className="an-logo-icon">S</div>
          <span className="an-logo-text">SkillSet</span>
        </NavLink>

        {/* Desktop nav */}
        <nav className="an-nav">
          {links.map(({ to, label }) => (
            <NavLink
              key={to} to={to} end={to === home}
              className={({ isActive }) => `an-nav-link${isActive ? ' an-nav-link--active' : ''}`}
            >
              {label}
            </NavLink>
          ))}
        </nav>

        {/* Actions */}
        <div className="an-actions">
          {role === 'EMPLOYER' && (
            <button className="an-post-btn" onClick={() => navigate('/employer/jobs/new')}>
              <Plus size={15} /> {t.publish}
            </button>
          )}

          <div className="an-lang-switch" role="group" aria-label="Langue / Language">
            <button
              type="button"
              className={`an-lang-btn${language === 'fr' ? ' an-lang-btn--active' : ''}`}
              onClick={() => setLanguage('fr')}
            >
              FR
            </button>
            <button
              type="button"
              className={`an-lang-btn${language === 'en' ? ' an-lang-btn--active' : ''}`}
              onClick={() => setLanguage('en')}
            >
              EN
            </button>
          </div>

          <button
            type="button"
            className="an-icon-btn"
            onClick={toggleTheme}
            aria-label={theme === 'light' ? 'Activer le mode sombre' : 'Activer le mode clair'}
          >
            {theme === 'light' ? <Moon size={18} /> : <Sun size={18} />}
          </button>

          <NavLink to="/messages" className="an-icon-btn" aria-label={t.messages}>
            <MessageSquare size={20} />
            {msgCount > 0 && <span className="an-badge">{msgCount}</span>}
          </NavLink>
          <NavLink to="/notifications" className="an-icon-btn" aria-label={t.notifications}>
            <Bell size={20} />
            {notifCount > 0 && <span className="an-badge">{notifCount}</span>}
          </NavLink>
          <NavLink to="/profile" className="an-icon-btn an-avatar-link" aria-label={t.profile}>
            {user?.profilePictureUrl
              ? <img src={user.profilePictureUrl} alt="" className="an-avatar-img" />
              : <CircleUser size={22} />}
          </NavLink>
          <button className="an-icon-btn" onClick={handleLogout} aria-label={t.logout}>
            <LogOut size={20} />
          </button>
          {/* Mobile hamburger */}
          <button className="an-hamburger" onClick={() => setDrawerOpen(true)} aria-label={t.menu}>
            <Menu size={22} />
          </button>
        </div>
      </header>

      {/* Mobile drawer */}
      {drawerOpen && (
        <>
          <div className="an-drawer-overlay" onClick={() => setDrawerOpen(false)} />
          <div className="an-drawer">
            <NavLink to={home} className="an-drawer-brand" onClick={() => setDrawerOpen(false)}>
              <div className="an-logo-icon">S</div>
              <span className="an-logo-text">SkillSet</span>
            </NavLink>
            {links.map(({ to, label }) => (
              <NavLink
                key={to} to={to} end={to === home}
                className={({ isActive }) => `an-drawer-link${isActive ? ' an-drawer-link--active' : ''}`}
                onClick={() => setDrawerOpen(false)}
              >
                {label}
              </NavLink>
            ))}
            <hr className="an-drawer-divider" />
            <NavLink to="/messages" className="an-drawer-link" onClick={() => setDrawerOpen(false)}>
              {t.messages} {msgCount > 0 && `(${msgCount})`}
            </NavLink>
            <NavLink to="/notifications" className="an-drawer-link" onClick={() => setDrawerOpen(false)}>
              {t.notifications} {notifCount > 0 && `(${notifCount})`}
            </NavLink>
            <NavLink to="/profile" className="an-drawer-link" onClick={() => setDrawerOpen(false)}>
              {t.myProfile}
            </NavLink>
            <button className="an-drawer-logout" onClick={() => { setDrawerOpen(false); handleLogout() }}>
              {t.logout}
            </button>
            <button className="an-drawer-close" onClick={() => setDrawerOpen(false)}>
              <X size={20} />
            </button>
          </div>
        </>
      )}
    </>
  )
}
