import { useState, useEffect } from 'react'
import { Upload, Trash2, ExternalLink, Loader2, Check } from 'lucide-react'
import { publishToFranceTravail, unpublishFromFranceTravail } from '../../api/IntegrationsApi'

export default function PublishToJobboardButton({ jobId, isPublished: initialPublished, franceTravailUrl, onSuccess }) {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [published, setPublished] = useState(initialPublished ?? false)
  const [toastMessage, setToastMessage] = useState('')

  // Sync with props changes
  useEffect(() => {
    setPublished(initialPublished ?? false)
  }, [initialPublished])

  const showToast = (msg, isError = false) => {
    setToastMessage(msg)
    setError(isError ? msg : '')
    setTimeout(() => {
      setToastMessage('')
      setError('')
    }, 3000)
  }

  const handlePublish = async () => {
    try {
      setLoading(true)
      setError('')
      await publishToFranceTravail(jobId)
      setPublished(true)
      showToast('Offre publiée sur France Travail')
      if (onSuccess) onSuccess()
    } catch (err) {
      console.error('Error publishing to France Travail:', err)
      const errorMsg = err.response?.data?.message || 'Erreur lors de la publication'
      showToast(errorMsg, true)
    } finally {
      setLoading(false)
    }
  }

  const handleUnpublish = async () => {
    if (!window.confirm('Êtes-vous sûr de vouloir retirer cette offre de France Travail ?')) return

    try {
      setLoading(true)
      setError('')
      await unpublishFromFranceTravail(jobId)
      setPublished(false)
      showToast('Offre retirée de France Travail')
      if (onSuccess) onSuccess()
    } catch (err) {
      console.error('Error unpublishing from France Travail:', err)
      const errorMsg = err.response?.data?.message || 'Erreur lors du retrait'
      showToast(errorMsg, true)
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      {/* Toast */}
      {toastMessage && (
        <div
          style={{
            position: 'fixed',
            bottom: '20px',
            right: '20px',
            zIndex: 2000,
            padding: '12px 20px',
            borderRadius: '6px',
            fontSize: '13px',
            background: error ? '#fee' : '#e8f5e9',
            color: error ? '#c42033' : '#2e7d32',
            border: error ? '1px solid #fcc' : '1px solid #c8e6c9',
            boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            animation: 'slideIn 0.3s ease-out',
          }}
        >
          {error ? '⚠️' : '✓'} {toastMessage}
        </div>
      )}

      {loading ? (
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            padding: '10px 16px',
            borderRadius: '6px',
            fontSize: '13px',
            color: '#999',
            background: '#f9f9f9',
            border: '1px solid #eee',
          }}
        >
          <Loader2 size={14} style={{ animation: 'spin 1s linear infinite' }} />
          Traitement...
        </div>
      ) : published ? (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
              padding: '10px 16px',
              borderRadius: '6px',
              fontSize: '13px',
              color: '#2e7d32',
              background: '#e8f5e9',
              border: '1px solid #c8e6c9',
              fontWeight: 600,
            }}
          >
            <Check size={14} />
            Publié sur France Travail
          </div>

          {franceTravailUrl && (
            <a
              href={franceTravailUrl}
              target="_blank"
              rel="noopener noreferrer"
              style={{
                display: 'flex',
                alignItems: 'center',
                justify: 'center',
                gap: '6px',
                padding: '10px 16px',
                borderRadius: '6px',
                fontSize: '13px',
                color: '#0066cc',
                textDecoration: 'none',
                background: '#f0f7ff',
                border: '1px solid #b3d9ff',
                transition: 'all 0.2s ease',
                cursor: 'pointer',
              }}
              onMouseEnter={(e) => {
                e.target.style.background = '#e6f2ff'
                e.target.style.borderColor = '#80bfff'
              }}
              onMouseLeave={(e) => {
                e.target.style.background = '#f0f7ff'
                e.target.style.borderColor = '#b3d9ff'
              }}
            >
              Voir l&apos;offre
              <ExternalLink size={12} />
            </a>
          )}

          <button
            onClick={handleUnpublish}
            disabled={loading}
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '8px',
              padding: '10px 16px',
              borderRadius: '6px',
              fontSize: '13px',
              fontWeight: 600,
              background: '#fff',
              color: '#c42033',
              border: '1px solid #fcc',
              cursor: loading ? 'not-allowed' : 'pointer',
              transition: 'all 0.2s ease',
              opacity: loading ? 0.7 : 1,
            }}
            onMouseEnter={(e) => {
              if (!loading) {
                e.target.style.background = '#fee'
                e.target.style.borderColor = '#fbb'
              }
            }}
            onMouseLeave={(e) => {
              e.target.style.background = '#fff'
              e.target.style.borderColor = '#fcc'
            }}
          >
            <Trash2 size={14} />
            Retirer
          </button>
        </div>
      ) : (
        <button
          onClick={handlePublish}
          disabled={loading}
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '8px',
            padding: '10px 16px',
            borderRadius: '6px',
            fontSize: '13px',
            fontWeight: 600,
            background: '#c42033',
            color: '#fff',
            border: 'none',
            cursor: loading ? 'not-allowed' : 'pointer',
            transition: 'all 0.2s ease',
            opacity: loading ? 0.7 : 1,
            boxShadow: '0 2px 4px rgba(196,32,51,0.15)',
          }}
          onMouseEnter={(e) => {
            if (!loading) {
              e.target.style.background = '#a81729'
              e.target.style.boxShadow = '0 4px 12px rgba(196,32,51,0.25)'
            }
          }}
          onMouseLeave={(e) => {
            e.target.style.background = '#c42033'
            e.target.style.boxShadow = '0 2px 4px rgba(196,32,51,0.15)'
          }}
        >
          <Upload size={14} />
          Publier sur France Travail
        </button>
      )}

      <style>{`
        @keyframes slideIn {
          from {
            transform: translateX(400px);
            opacity: 0;
          }
          to {
            transform: translateX(0);
            opacity: 1;
          }
        }
        @keyframes spin {
          from {
            transform: rotate(0deg);
          }
          to {
            transform: rotate(360deg);
          }
        }
      `}</style>
    </>
  )
}
