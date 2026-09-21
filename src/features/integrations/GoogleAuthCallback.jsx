import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Loader2, CheckCircle, AlertCircle } from 'lucide-react'
import { confirmGoogleAuth } from '../../api/IntegrationsApi'
import { useTranslation } from '../../i18n/translations'

export default function GoogleAuthCallback() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const t = useTranslation().googleAuthCallback
  const [status, setStatus] = useState('loading') // 'loading', 'success', 'error'
  const [error, setError] = useState('')

  useEffect(() => {
    const handleCallback = async () => {
      try {
        const code = searchParams.get('code')

        if (!code) {
          setStatus('error')
          setError(t.missingCode)
          return
        }

        // Send code to backend to exchange for access token
        await confirmGoogleAuth(code)

        setStatus('success')

        // Redirect to integrations page after 2 seconds
        setTimeout(() => {
          navigate('/settings/integrations')
        }, 2000)
      } catch (err) {
        console.error('Error during Google auth callback:', err)
        setStatus('error')
        setError(
          err.response?.data?.message || t.connectError
        )
      }
    }

    handleCallback()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [searchParams, navigate])

  const handleRetry = () => {
    navigate('/settings/integrations')
  }

  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '100vh',
        background: 'var(--surface-page)',
        padding: '20px',
      }}
    >
      <div
        style={{
          background: 'var(--surface-card)',
          borderRadius: '12px',
          padding: '40px 32px',
          maxWidth: '400px',
          width: '100%',
          textAlign: 'center',
          boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
        }}
      >
        {status === 'loading' && (
          <>
            <Loader2
              size={48}
              style={{
                color: '#c42033',
                marginBottom: '20px',
                animation: 'spin 1s linear infinite',
              }}
            />
            <h1 style={{ fontSize: '20px', color: 'var(--text-primary)', margin: '0 0 8px 0' }}>
              {t.connecting}
            </h1>
            <p style={{ fontSize: '14px', color: 'var(--text-secondary)', margin: 0 }}>
              {t.pleaseWait}
            </p>
          </>
        )}

        {status === 'success' && (
          <>
            <CheckCircle
              size={48}
              style={{
                color: '#2e7d32',
                marginBottom: '20px',
              }}
            />
            <h1 style={{ fontSize: '20px', color: 'var(--text-primary)', margin: '0 0 8px 0' }}>
              {t.success}
            </h1>
            <p style={{ fontSize: '14px', color: 'var(--text-secondary)', margin: '0 0 20px 0' }}>
              {t.successBody}
            </p>
            <p style={{ fontSize: '12px', color: 'var(--text-muted)', margin: 0 }}>
              {t.redirecting}
            </p>
          </>
        )}

        {status === 'error' && (
          <>
            <AlertCircle
              size={48}
              style={{
                color: '#c42033',
                marginBottom: '20px',
              }}
            />
            <h1 style={{ fontSize: '20px', color: 'var(--text-primary)', margin: '0 0 8px 0' }}>
              {t.errorTitle}
            </h1>
            <p style={{ fontSize: '14px', color: 'var(--text-secondary)', margin: '0 0 20px 0' }}>
              {error}
            </p>
            <button
              onClick={handleRetry}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                padding: '10px 24px',
                borderRadius: '6px',
                fontSize: '13px',
                fontWeight: 600,
                background: '#c42033',
                color: '#fff',
                border: 'none',
                cursor: 'pointer',
                transition: 'all 0.2s ease',
                boxShadow: '0 2px 4px rgba(196,32,51,0.15)',
              }}
              onMouseEnter={(e) => {
                e.target.style.background = '#a81729'
                e.target.style.boxShadow = '0 4px 12px rgba(196,32,51,0.25)'
              }}
              onMouseLeave={(e) => {
                e.target.style.background = '#c42033'
                e.target.style.boxShadow = '0 2px 4px rgba(196,32,51,0.15)'
              }}
            >
              {t.backToSettings}
            </button>
          </>
        )}
      </div>

      <style>{`
        @keyframes spin {
          from {
            transform: rotate(0deg);
          }
          to {
            transform: rotate(360deg);
          }
        }
      `}</style>
    </div>
  )
}
