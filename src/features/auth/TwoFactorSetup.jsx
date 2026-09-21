import { useState } from 'react'
import { ShieldCheck, ShieldOff, ShieldAlert, MailCheck } from 'lucide-react'
import { setup2fa, confirm2fa, disable2fa } from '../../api/AuthApi'
import { useAuthStore } from '../../store/AuthStore'
import { useTranslation } from '../../i18n/translations'

/**
 * Composant de gestion du 2FA dans les paramètres de sécurité.
 * Un code à 6 chiffres est envoyé par email, aussi bien pour activer que
 * pour désactiver la vérification en deux étapes.
 */
export default function TwoFactorSetup() {
  const t = useTranslation().twoFactorSetup
  const { user, updateUser } = useAuthStore()
  const isEnabled = Boolean(user?.twoFactorEnabled)

  const [phase, setPhase]         = useState('idle')   // 'idle' | 'verify-enable' | 'verify-disable'
  const [code, setCode]           = useState('')
  const [error, setError]         = useState('')
  const [loading, setLoading]     = useState(false)

  // ── Demander l'envoi d'un code par email ──────────────────────────────────
  const requestCode = async (nextPhase) => {
    setLoading(true)
    setError('')
    try {
      await setup2fa()
      setPhase(nextPhase)
      setCode('')
    } catch (err) {
      setError(err.response?.data?.message || t.qrError)
    } finally {
      setLoading(false)
    }
  }

  // ── Confirmer l'activation (code reçu par email) ──────────────────────────
  const handleConfirm = async (e) => {
    e.preventDefault()
    const cleaned = code.replace(/\s/g, '')
    if (cleaned.length !== 6) { setError(t.codeLength); return }
    setLoading(true)
    setError('')
    try {
      await confirm2fa(cleaned)
      updateUser({ twoFactorEnabled: true })
      setPhase('idle')
      setCode('')
    } catch (err) {
      setError(err.response?.data?.message || t.invalidCode)
    } finally {
      setLoading(false)
    }
  }

  // ── Confirmer la désactivation (code reçu par email) ──────────────────────
  const handleDisable = async (e) => {
    e.preventDefault()
    const cleaned = code.replace(/\s/g, '')
    if (cleaned.length !== 6) { setError(t.codeLength); return }
    setLoading(true)
    setError('')
    try {
      await disable2fa(cleaned)
      updateUser({ twoFactorEnabled: false })
      setPhase('idle')
      setCode('')
    } catch (err) {
      setError(err.response?.data?.message || t.invalidCode)
    } finally {
      setLoading(false)
    }
  }

  // ── Rendu : 2FA désactivée ────────────────────────────────────────────────
  if (!isEnabled && phase === 'idle') {
    return (
      <div className="tfa-card">
        <div className="tfa-status tfa-status--off">
          <ShieldOff size={20} />
          <span>{t.disabledStatus} <strong>{t.disabledLabel}</strong></span>
        </div>
        <p className="tfa-desc">
          {t.protectAccount}
        </p>
        <button className="tfa-btn tfa-btn--primary" onClick={() => requestCode('verify-enable')} disabled={loading}>
          {loading ? <span className="auth-spinner" /> : t.enable2fa}
        </button>
        {error && <p className="tfa-error">{error}</p>}
      </div>
    )
  }

  // ── Rendu : code envoyé, en attente de confirmation (activation) ──────────
  if (phase === 'verify-enable') {
    return (
      <div className="tfa-card">
        <div className="tfa-status tfa-status--pending">
          <ShieldAlert size={20} />
          <span>{t.settingUp}</span>
        </div>

        <div className="tfa-qr-wrap" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <MailCheck size={20} />
          <p className="tfa-desc" style={{ margin: 0 }}>{t.emailSentDesc}</p>
        </div>

        {error && <p className="tfa-error">{error}</p>}

        <form onSubmit={handleConfirm} className="tfa-verify-form">
          <input
            type="text"
            inputMode="numeric"
            pattern="[0-9 ]*"
            maxLength={7}
            placeholder="000 000"
            value={code}
            onChange={(e) => { setCode(e.target.value); setError('') }}
            autoComplete="one-time-code"
            className="tfa-code-input"
            autoFocus
          />
          <button type="submit" className="tfa-btn tfa-btn--primary" disabled={loading}>
            {loading ? <span className="auth-spinner" /> : t.confirmAndEnable}
          </button>
        </form>

        <button
          type="button"
          className="tfa-btn tfa-btn--ghost"
          onClick={() => { setPhase('idle'); setCode(''); setError('') }}
        >
          {t.cancel}
        </button>
      </div>
    )
  }

  // ── Rendu : 2FA activée ───────────────────────────────────────────────────
  if (isEnabled && phase === 'idle') {
    return (
      <div className="tfa-card">
        <div className="tfa-status tfa-status--on">
          <ShieldCheck size={20} />
          <span>{t.disabledStatus} <strong>{t.enabledLabel}</strong></span>
        </div>
        <p className="tfa-desc">
          {t.accountProtected}
        </p>
        <button
          className="tfa-btn tfa-btn--danger"
          onClick={() => requestCode('verify-disable')}
          disabled={loading}
        >
          {loading ? <span className="auth-spinner" /> : t.disable2fa}
        </button>
        {error && <p className="tfa-error">{error}</p>}
      </div>
    )
  }

  // ── Rendu : code envoyé, en attente de confirmation (désactivation) ───────
  if (phase === 'verify-disable') {
    return (
      <div className="tfa-card">
        <div className="tfa-status tfa-status--on">
          <ShieldCheck size={20} />
          <span>{t.confirmDisableTitle}</span>
        </div>
        <p className="tfa-desc">
          {t.confirmDisableDesc}
        </p>

        {error && <p className="tfa-error">{error}</p>}

        <form onSubmit={handleDisable} className="tfa-verify-form">
          <input
            type="text"
            inputMode="numeric"
            pattern="[0-9 ]*"
            maxLength={7}
            placeholder="000 000"
            value={code}
            onChange={(e) => { setCode(e.target.value); setError('') }}
            autoComplete="one-time-code"
            className="tfa-code-input"
            autoFocus
          />
          <button type="submit" className="tfa-btn tfa-btn--danger" disabled={loading}>
            {loading ? <span className="auth-spinner" /> : t.confirmDisable}
          </button>
        </form>

        <button
          type="button"
          className="tfa-btn tfa-btn--ghost"
          onClick={() => { setPhase('idle'); setCode(''); setError('') }}
        >
          {t.cancel}
        </button>
      </div>
    )
  }

  return null
}
