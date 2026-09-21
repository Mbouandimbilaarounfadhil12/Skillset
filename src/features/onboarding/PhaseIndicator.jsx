import { useTranslation } from '../../i18n/translations'
import './PhaseIndicator.css'

/**
 * Barre de progression simple sur l'ensemble du parcours d'onboarding
 * (questions initiales + questions générées par l'IA, une seule séquence continue).
 */
export default function PhaseIndicator({ current, total }) {
  const t = useTranslation().onboarding.phaseIndicator

  if (!total) return null

  return (
    <div className="pi-container">
      <div className="pi-progress-bar">
        <div className="pi-progress-track">
          <div
            className="pi-progress-fill"
            style={{ width: `${Math.min(100, (current / total) * 100)}%` }}
          />
        </div>
        <span className="pi-progress-label">
          {t.question} {current} / {total}
        </span>
      </div>
    </div>
  )
}
