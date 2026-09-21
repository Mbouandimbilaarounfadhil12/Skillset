import { Link } from 'react-router-dom'
import { ArrowUpRight, Sun, Moon } from 'lucide-react'
import { usePreferencesStore } from '../../store/PreferencesStore'
import { useTranslation } from '../../i18n/translations'
import './PublicNavbar.css'

export default function PublicNavbar() {
  const t = useTranslation().nav
  const { language, setLanguage, theme, toggleTheme } = usePreferencesStore()

  return (
    <header className="pn-nav">
      <div className="pn-nav-inner">
        <div className="pn-nav-left">
          <Link to="/" className="pn-logo">
            <span className="pn-logo-icon">S</span>
            <span className="pn-logo-text">SkillSet</span>
          </Link>
          <Link to="/login" className="pn-pill pn-pill-ghost">
            {t.connexion} <ArrowUpRight size={14} />
          </Link>
        </div>

        <div className="pn-nav-right">
          <div className="pn-lang-switch" role="group" aria-label="Langue / Language">
            <button
              type="button"
              className={`pn-lang-btn${language === 'fr' ? ' pn-lang-btn--active' : ''}`}
              onClick={() => setLanguage('fr')}
            >
              FR
            </button>
            <button
              type="button"
              className={`pn-lang-btn${language === 'en' ? ' pn-lang-btn--active' : ''}`}
              onClick={() => setLanguage('en')}
            >
              EN
            </button>
          </div>

          <button
            type="button"
            className="pn-theme-btn"
            onClick={toggleTheme}
            aria-label={theme === 'light' ? 'Activer le mode sombre' : 'Activer le mode clair'}
          >
            {theme === 'light' ? <Moon size={16} /> : <Sun size={16} />}
          </button>
        </div>
      </div>
    </header>
  )
}
