import { Calendar, AlertCircle } from 'lucide-react'
import { useState } from 'react'

export default function CalendarEventStatus({ googleEventId, interviewId }) {
  const [showTooltip, setShowTooltip] = useState(false)

  if (googleEventId) {
    return (
      <div
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: '6px',
          padding: '6px 10px',
          borderRadius: '4px',
          fontSize: '12px',
          fontWeight: 600,
          background: '#e3f2fd',
          color: '#1565c0',
          border: '1px solid #90caf9',
        }}
      >
        <Calendar size={12} />
        Ajouté au calendrier
      </div>
    )
  }

  return (
    <div
      style={{ position: 'relative', display: 'inline-block' }}
      onMouseEnter={() => setShowTooltip(true)}
      onMouseLeave={() => setShowTooltip(false)}
    >
      <div
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: '6px',
          padding: '6px 10px',
          borderRadius: '4px',
          fontSize: '12px',
          fontWeight: 600,
          background: '#f5f5f5',
          color: '#999',
          border: '1px solid #eee',
          cursor: 'help',
        }}
      >
        <AlertCircle size={12} />
        Calendrier non connecté
      </div>

      {/* Tooltip */}
      {showTooltip && (
        <div
          style={{
            position: 'absolute',
            bottom: '100%',
            left: '50%',
            transform: 'translateX(-50%)',
            marginBottom: '8px',
            padding: '8px 12px',
            borderRadius: '4px',
            fontSize: '12px',
            background: '#333',
            color: '#fff',
            whiteSpace: 'nowrap',
            zIndex: 1000,
            boxShadow: '0 2px 8px rgba(0,0,0,0.2)',
          }}
        >
          Connectez votre Google Calendar pour synchroniser
          <div
            style={{
              position: 'absolute',
              top: '100%',
              left: '50%',
              transform: 'translateX(-50%)',
              width: 0,
              height: 0,
              borderLeft: '4px solid transparent',
              borderRight: '4px solid transparent',
              borderTop: '4px solid #333',
            }}
          />
        </div>
      )}
    </div>
  )
}
