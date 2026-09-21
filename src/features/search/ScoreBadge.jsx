import { Info } from 'lucide-react'

export default function ScoreBadge({ score = 0, breakdown = null }) {
  const getColor = (s) => {
    if (s >= 80) return { bg: '#e0f5e3', text: '#1a6e44', label: 'Excellent' }
    if (s >= 60) return { bg: '#fff8e0', text: '#9a5700', label: 'Bon' }
    if (s >= 40) return { bg: '#fff3e0', text: '#c0652c', label: 'Acceptable' }
    return { bg: '#fff0f2', text: '#c42033', label: 'Faible' }
  }

  const color = getColor(score)
  const scoreInt = Math.round(score)

  return (
    <div className="sb-wrapper">
      <div className="sb-badge" style={{ background: color.bg, color: color.text }}>
        {scoreInt}%
      </div>
      {breakdown && (
        <div className="sb-tooltip">
          <Info size={12} className="sb-icon" />
          <div className="sb-breakdown">
            <div className="sb-row">Compétences: {Math.round(breakdown.skillsPoints)}/40</div>
            <div className="sb-row">Expérience: {Math.round(breakdown.experiencePoints)}/20</div>
            <div className="sb-row">Localisation: {Math.round(breakdown.locationPoints)}/15</div>
            <div className="sb-row">Disponibilité: {Math.round(breakdown.availabilityPoints)}/15</div>
            <div className="sb-row">Titre: {Math.round(breakdown.titlePoints)}/10</div>
          </div>
        </div>
      )}

      <style>{`
        .sb-wrapper {
          position: relative;
          display: inline-block;
        }
        .sb-badge {
          display: inline-flex;
          align-items: center;
          justify-content: center;
          width: 40px;
          height: 40px;
          border-radius: 8px;
          font-size: 12px;
          font-weight: 700;
          cursor: ${breakdown ? 'help' : 'default'};
          transition: transform 0.2s;
        }
        .sb-badge:hover {
          transform: ${breakdown ? 'scale(1.05)' : 'none'};
        }
        .sb-icon {
          display: none;
        }
        .sb-wrapper:hover .sb-icon {
          display: inline;
        }
        .sb-tooltip {
          position: absolute;
          bottom: 100%;
          left: 50%;
          transform: translateX(-50%);
          background: #1a1a1a;
          color: #fff;
          padding: 8px 12px;
          border-radius: 6px;
          font-size: 11px;
          white-space: nowrap;
          margin-bottom: 8px;
          z-index: 10;
          opacity: 0;
          pointer-events: none;
          transition: opacity 0.2s;
        }
        .sb-wrapper:hover .sb-tooltip {
          opacity: 1;
        }
        .sb-tooltip::after {
          content: '';
          position: absolute;
          top: 100%;
          left: 50%;
          transform: translateX(-50%);
          border: 4px solid transparent;
          border-top-color: #1a1a1a;
        }
        .sb-breakdown {
          display: flex;
          flex-direction: column;
          gap: 3px;
        }
        .sb-row {
          font-size: 10px;
          line-height: 1.3;
        }
      `}</style>
    </div>
  )
}
