import PropTypes from 'prop-types'
import { useTranslation } from '../../i18n/translations'
import './MatchScoreBadge.css'

/**
 * Badge affichant le score de matching IA (0-100).
 * Si une explication est fournie, un tooltip s'affiche au survol.
 *
 * Props:
 *   score       number   — Score 0-100
 *   explanation string   — Texte d'explication Claude (optionnel)
 *   size        'sm'|'md'— Taille ('sm' par défaut pour le Kanban)
 */
export default function MatchScoreBadge({ score, explanation, size = 'sm' }) {
  const t = useTranslation().matching
  if (score == null) return null

  const pct = Math.round(score)
  const tier = pct >= 70 ? 'high' : pct >= 45 ? 'mid' : 'low'

  return (
    <span className={`msb msb--${tier} msb--${size}${explanation ? ' msb--tip' : ''}`}>
      {pct}%&nbsp;{t.aiLabel}
      {explanation && (
        <span className="msb-tooltip" role="tooltip">{explanation}</span>
      )}
    </span>
  )
}

MatchScoreBadge.propTypes = {
  score:       PropTypes.number,
  explanation: PropTypes.string,
  size:        PropTypes.oneOf(['sm', 'md']),
}
