import { useState, useEffect } from 'react'
import { Loader2, AlertCircle, Check, Link as LinkIcon } from 'lucide-react'
import { getGoogleAuthUrl, getGoogleStatus, disconnectGoogle, testFranceTravailConnection } from '../../api/IntegrationsApi'
import AppNavbar from '../../components/common/AppNavbar'
import { useTranslation } from '../../i18n/translations'
import './IntegrationsPage.css'

export default function IntegrationsPage() {
  const t = useTranslation().integrations
  // Google Calendar state
  const [googleConnected, setGoogleConnected] = useState(false)
  const [googleEmail, setGoogleEmail] = useState('')
  const [googleLoading, setGoogleLoading] = useState(true)
  const [googleDisconnecting, setGoogleDisconnecting] = useState(false)

  // France Travail state
  const [clientId, setClientId] = useState('')
  const [clientSecret, setClientSecret] = useState('')
  const [franceTravailConnected, setFranceTravailConnected] = useState(false)
  const [testingConnection, setTestingConnection] = useState(false)
  const [franceTravailError, setFranceTravailError] = useState('')
  const [franceTravailSuccess, setFranceTravailSuccess] = useState(false)

  // Toast
  const [toastMessage, setToastMessage] = useState('')
  const [toastIsError, setToastIsError] = useState(false)

  // Load Google status on mount
  useEffect(() => {
    loadGoogleStatus()
  }, [])

  const loadGoogleStatus = async () => {
    try {
      setGoogleLoading(true)
      const data = await getGoogleStatus()
      setGoogleConnected(data.connected ?? false)
      setGoogleEmail(data.email ?? '')
    } catch (err) {
      console.error('Error loading Google status:', err)
    } finally {
      setGoogleLoading(false)
    }
  }

  const handleConnectGoogle = async () => {
    try {
      const data = await getGoogleAuthUrl()
      if (data.authUrl) {
        window.location.href = data.authUrl
      }
    } catch (err) {
      console.error('Error getting Google auth URL:', err)
      showToast(t.googleConnectError, true)
    }
  }

  const handleDisconnectGoogle = async () => {
    if (!window.confirm(t.confirmDisconnectGoogle)) return

    try {
      setGoogleDisconnecting(true)
      await disconnectGoogle()
      setGoogleConnected(false)
      setGoogleEmail('')
      showToast(t.googleDisconnected, false)
    } catch (err) {
      console.error('Error disconnecting Google:', err)
      showToast(t.disconnectError, true)
    } finally {
      setGoogleDisconnecting(false)
    }
  }

  const handleTestFranceTravail = async () => {
    if (!clientId || !clientSecret) {
      setFranceTravailError(t.fillClientFields)
      return
    }

    try {
      setTestingConnection(true)
      setFranceTravailError('')
      setFranceTravailSuccess(false)

      await testFranceTravailConnection(clientId, clientSecret)
      setFranceTravailConnected(true)
      setFranceTravailSuccess(true)
      showToast(t.franceTravailSuccessToast, false)
      setTimeout(() => setFranceTravailSuccess(false), 3000)
    } catch (err) {
      console.error('Error testing France Travail connection:', err)
      setFranceTravailConnected(false)
      setFranceTravailError(err.response?.data?.message || t.franceTravailError)
    } finally {
      setTestingConnection(false)
    }
  }

  const showToast = (msg, isError) => {
    setToastIsError(isError)
    setToastMessage(msg)
    setTimeout(() => setToastMessage(''), 3000)
  }

  return (
    <div className="st-shell">
      <AppNavbar />

      <div className="st-body">
        <main className="integrations-main">
          <h1 className="integrations-title">{t.title}</h1>
          <p className="integrations-subtitle">{t.subtitle}</p>

          {/* Toast */}
          {toastMessage && (
            <div className={`integrations-toast ${toastIsError ? 'integrations-toast--error' : ''}`}>
              {toastMessage}
            </div>
          )}

          <div className="integrations-container">
            {/* ── Google Calendar Section ── */}
            <section className="integration-card">
              <div className="integration-header">
                <div>
                  <h2 className="integration-title">{t.googleCalendar}</h2>
                  <p className="integration-description">
                    {t.googleCalendarDesc}
                  </p>
                </div>
                {googleLoading ? (
                  <Loader2 size={20} className="integration-spin" />
                ) : googleConnected ? (
                  <div className="integration-badge integration-badge--success">
                    <Check size={14} />
                    {t.connected}
                  </div>
                ) : (
                  <div className="integration-badge integration-badge--inactive">
                    {t.notConnected}
                  </div>
                )}
              </div>

              <p className="integration-explain">
                {t.googleExplain}
              </p>

              {googleLoading ? (
                <div className="integration-loading">
                  <Loader2 size={20} className="integration-spin" />
                  {t.loading}
                </div>
              ) : googleConnected ? (
                <div className="integration-connected">
                  <div className="integration-info">
                    <p className="integration-info-label">{t.connectedAccount}</p>
                    <p className="integration-info-value">{googleEmail}</p>
                  </div>
                  <button
                    className="integration-btn integration-btn--disconnect"
                    onClick={handleDisconnectGoogle}
                    disabled={googleDisconnecting}
                  >
                    {googleDisconnecting ? <Loader2 size={14} className="integration-spin" /> : null}
                    {t.disconnect}
                  </button>
                </div>
              ) : (
                <button
                  className="integration-btn integration-btn--primary"
                  onClick={handleConnectGoogle}
                >
                  <LinkIcon size={16} />
                  {t.connectGoogleCalendar}
                </button>
              )}
            </section>

            {/* ── France Travail Section ── */}
            <section className="integration-card">
              <div className="integration-header">
                <div>
                  <h2 className="integration-title">{t.franceTravail}</h2>
                  <p className="integration-description">
                    {t.franceTravailDesc}
                  </p>
                </div>
                {franceTravailConnected ? (
                  <div className="integration-badge integration-badge--success">
                    <Check size={14} />
                    {t.connected}
                  </div>
                ) : (
                  <div className="integration-badge integration-badge--inactive">
                    {t.notConnected}
                  </div>
                )}
              </div>

              <p className="integration-explain">
                {t.franceTravailExplain}
              </p>

              {franceTravailError && (
                <div className="integration-error">
                  <AlertCircle size={16} />
                  {franceTravailError}
                </div>
              )}

              {franceTravailSuccess && (
                <div className="integration-success">
                  <Check size={16} />
                  {t.connectionSuccess}
                </div>
              )}

              <div className="integration-form">
                <div className="form-group">
                  <label className="form-label">{t.clientId}</label>
                  <input
                    type="password"
                    className="form-input"
                    placeholder={t.clientIdPlaceholder}
                    value={clientId}
                    onChange={(e) => setClientId(e.target.value)}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">{t.clientSecret}</label>
                  <input
                    type="password"
                    className="form-input"
                    placeholder={t.clientSecretPlaceholder}
                    value={clientSecret}
                    onChange={(e) => setClientSecret(e.target.value)}
                  />
                </div>

                <button
                  className="integration-btn integration-btn--primary"
                  onClick={handleTestFranceTravail}
                  disabled={testingConnection}
                >
                  {testingConnection ? <Loader2 size={14} className="integration-spin" /> : null}
                  {t.saveAndTest}
                </button>
              </div>
            </section>
          </div>
        </main>
      </div>
    </div>
  )
}
