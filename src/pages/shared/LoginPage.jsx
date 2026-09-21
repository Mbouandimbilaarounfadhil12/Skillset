import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Eye, EyeOff, Briefcase, Users, Sparkles, ShieldCheck } from 'lucide-react'
import { loginUser, verify2faLogin } from '../../api/AuthApi'
import { useAuthStore } from '../../store/AuthStore'
import PublicNavbar from '../../components/common/PublicNavbar'
import { useTranslation } from '../../i18n/translations'
import './AuthPage.css'

const ROLE_ROUTES = {
  CANDIDATE: '/dashboard/candidate',
  EMPLOYER:  '/dashboard/employer',
  ADMIN:     '/dashboard/admin',
}

const resolveRedirect = (data) =>
  data.onboardingCompleted ? (ROLE_ROUTES[data.role] ?? '/dashboard/candidate') : '/onboarding'

export default function LoginPage() {
  const navigate = useNavigate()
  const setAuth  = useAuthStore((s) => s.setAuth)
  const t = useTranslation().login

  const [form, setForm]           = useState({ email: '', password: '' })
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError]         = useState('')
  const [loading, setLoading]     = useState(false)

  // ── État 2FA ─────────────────────────────────────────────────────────────
  const [step, setStep]               = useState('credentials') // 'credentials' | 'totp'
  const [preAuthToken, setPreAuth]    = useState('')
  const [totpCode, setTotpCode]       = useState('')

  const handleChange = (e) => { setForm({ ...form, [e.target.name]: e.target.value }); setError('') }

  // ── Étape 1 : email + mot de passe ───────────────────────────────────────
  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!form.email || !form.password) { setError(t.fillAllFields); return }
    setLoading(true)
    try {
      const data = await loginUser({ email: form.email, password: form.password })
      if (data.twoFactorRequired) {
        setPreAuth(data.preAuthToken)
        setStep('totp')
        return
      }
      setAuth(data)
      navigate(resolveRedirect(data))
    } catch (err) {
      setError(err.response?.data?.message || t.invalidCredentials)
    } finally {
      setLoading(false)
    }
  }

  // ── Étape 2 : code TOTP ───────────────────────────────────────────────────
  const handleTotpSubmit = async (e) => {
    e.preventDefault()
    const cleaned = totpCode.replace(/\s/g, '')
    if (cleaned.length !== 6) { setError(t.twoFaInvalidLength); return }
    setLoading(true)
    try {
      const data = await verify2faLogin(preAuthToken, cleaned)
      setAuth(data)
      navigate(resolveRedirect(data))
    } catch (err) {
      setError(err.response?.data?.message || t.twoFaInvalid)
    } finally {
      setLoading(false)
    }
  }

  const [brandLine1, brandPrefix, brandAccent] = t.brandTitle

  return (
    <div className="auth-page">
      <PublicNavbar />
      <div className="auth-shell">

      {/* ── Panneau gauche — brand ── */}
      <div className="auth-brand">
        <div className="auth-brand-decoration">
          <div className="auth-orb auth-orb-1" />
          <div className="auth-orb auth-orb-2" />
        </div>

        <div className="auth-brand-content">
          <div className="auth-logo">
            <div className="auth-logo-icon">S</div>
            <span className="auth-logo-text">SkillSet</span>
          </div>

          <h1 className="auth-brand-title">
            {brandLine1}<br />{brandPrefix}<span className="auth-brand-accent">{brandAccent}</span>
          </h1>

          <p className="auth-brand-subtitle">{t.brandSubtitle}</p>

          <ul className="auth-features">
            <li>
              <span className="auth-feature-icon"><Briefcase size={14} /></span>{' '}
              {t.features[0]}
            </li>
            <li>
              <span className="auth-feature-icon"><Users size={14} /></span>{' '}
              {t.features[1]}
            </li>
            <li>
              <span className="auth-feature-icon"><Sparkles size={14} /></span>{' '}
              {t.features[2]}
            </li>
          </ul>
        </div>
      </div>

      {/* ── Panneau droit — formulaire ── */}
      <div className="auth-form-panel">
        <div className="auth-card">

          {/* ── Étape 1 : identifiants ── */}
          {step === 'credentials' && (
            <>
              <div className="auth-card-header">
                <h2>{t.title}</h2>
                <p>{t.subtitle}</p>
              </div>

              {error && <div className="auth-error">{error}</div>}

              <form onSubmit={handleSubmit} noValidate className="auth-form">
                <div className="auth-field">
                  <label htmlFor="email">{t.emailLabel}</label>
                  <input
                    id="email" name="email" type="email"
                    placeholder={t.emailPlaceholder}
                    value={form.email} onChange={handleChange} autoComplete="email"
                  />
                </div>

                <div className="auth-field">
                  <div className="auth-field-row">
                    <label htmlFor="password">{t.passwordLabel}</label>
                    <Link to="#" className="auth-forgot">{t.forgotPassword}</Link>
                  </div>
                  <div className="auth-input-wrap">
                    <input
                      id="password" name="password" type={showPassword ? 'text' : 'password'}
                      placeholder="••••••••"
                      value={form.password} onChange={handleChange} autoComplete="current-password"
                    />
                    <button
                      type="button" className="auth-eye"
                      onClick={() => setShowPassword(v => !v)}
                      aria-label={showPassword ? t.hidePassword : t.showPassword}
                    >
                      {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                    </button>
                  </div>
                </div>

                <button type="submit" disabled={loading} className="auth-btn-primary">
                  {loading ? <span className="auth-spinner" /> : t.submit}
                </button>
              </form>

              <p className="auth-legal">
                {t.legalNotice.prefix}
                <a href="/privacy" target="_blank" rel="noopener noreferrer">{t.legalNotice.privacy}</a>
                {t.legalNotice.and}
                <a href="/terms" target="_blank" rel="noopener noreferrer">{t.legalNotice.terms}</a>
                {t.legalNotice.suffix}
              </p>

              <p className="auth-switch" style={{ marginTop: '12px' }}>
                {t.noAccount}{' '}
                <Link to="/register">{t.signUp}</Link>
              </p>
            </>
          )}

          {/* ── Étape 2 : code TOTP ── */}
          {step === 'totp' && (
            <>
              <div className="auth-card-header">
                <span className="auth-2fa-icon"><ShieldCheck size={32} /></span>
                <h2>{t.twoFaTitle}</h2>
                <p>{t.twoFaSubtitle}</p>
              </div>

              {error && <div className="auth-error">{error}</div>}

              <form onSubmit={handleTotpSubmit} noValidate className="auth-form">
                <div className="auth-field">
                  <label htmlFor="totpCode">{t.twoFaLabel}</label>
                  <input
                    id="totpCode"
                    name="totpCode"
                    type="text"
                    inputMode="numeric"
                    pattern="[0-9 ]*"
                    maxLength={7}
                    placeholder="000 000"
                    value={totpCode}
                    onChange={(e) => { setTotpCode(e.target.value); setError('') }}
                    autoComplete="one-time-code"
                    autoFocus
                    style={{ letterSpacing: '0.25em', fontSize: '1.4rem', textAlign: 'center' }}
                  />
                </div>

                <button type="submit" disabled={loading} className="auth-btn-primary">
                  {loading ? <span className="auth-spinner" /> : t.verify}
                </button>
              </form>

              <p className="auth-switch" style={{ marginTop: '16px' }}>
                <button
                  type="button"
                  className="auth-forgot"
                  style={{ background: 'none', border: 'none', cursor: 'pointer', padding: 0 }}
                  onClick={() => { setStep('credentials'); setError(''); setTotpCode('') }}
                >
                  {t.backToLogin}
                </button>
              </p>
            </>
          )}

        </div>
      </div>

      </div>
    </div>
  )
}
