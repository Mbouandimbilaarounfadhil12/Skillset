import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../../store/AuthStore'
import { updateConsent } from '../../api/GdprApi'
import { useTranslation } from '../../i18n/translations'

const CONSENT_TYPES = ['ANALYTICS', 'MARKETING', 'AI_PROCESSING']

export default function ConsentBanner() {
  const [visible, setVisible] = useState(
    () => localStorage.getItem('consent_initialized') === null
  )
  const [loading, setLoading] = useState(false)
  const { user } = useAuthStore()
  const navigate = useNavigate()
  const t = useTranslation().consent

  if (!visible || !user) return null

  const acceptAll = async () => {
    setLoading(true)
    try {
      await Promise.all(CONSENT_TYPES.map(t => updateConsent(t, true)))
    } catch (_) {}
    localStorage.setItem('consent_initialized', 'true')
    setVisible(false)
    setLoading(false)
  }

  const rejectAll = async () => {
    setLoading(true)
    try {
      await Promise.all(CONSENT_TYPES.map(t => updateConsent(t, false)))
    } catch (_) {}
    localStorage.setItem('consent_initialized', 'true')
    setVisible(false)
    setLoading(false)
  }

  const configure = () => {
    localStorage.setItem('consent_initialized', 'true')
    setVisible(false)
    navigate('/settings')
  }

  return (
    <div className="cb-banner">
      <p className="cb-text">{t.text}</p>
      <div className="cb-actions">
        <button className="cb-btn cb-btn--secondary" onClick={configure} disabled={loading}>
          {t.configure}
        </button>
        <button className="cb-btn cb-btn--outline" onClick={rejectAll} disabled={loading}>
          {t.rejectAll}
        </button>
        <button className="cb-btn cb-btn--primary" onClick={acceptAll} disabled={loading}>
          {loading ? t.saving : t.acceptAll}
        </button>
      </div>

      <style>{`
        .cb-banner {
          position: fixed; bottom: 0; left: 0; right: 0;
          background: var(--surface-card);
          border-top: 1px solid var(--surface-border);
          padding: 16px 24px; z-index: 999;
          display: flex; align-items: center; justify-content: space-between;
          gap: 16px; flex-wrap: wrap;
          box-shadow: 0 -4px 20px rgba(0,0,0,0.08);
        }
        .cb-text {
          font-size: 0.85rem; color: var(--text-secondary);
          line-height: 1.5; margin: 0; flex: 1; min-width: 260px;
        }
        .cb-actions { display: flex; gap: 8px; flex-wrap: wrap; }
        .cb-btn {
          padding: 8px 16px; border-radius: 8px; font-size: 0.875rem;
          font-weight: 500; cursor: pointer; white-space: nowrap; border: none;
        }
        .cb-btn:disabled { opacity: 0.6; cursor: not-allowed; }
        .cb-btn--primary {
          background: #c42033; color: #fff;
        }
        .cb-btn--outline {
          background: none; border: 1.5px solid var(--surface-border);
          color: var(--text-primary);
        }
        .cb-btn--secondary {
          background: var(--surface-border-soft);
          color: var(--text-secondary);
        }
      `}</style>
    </div>
  )
}
