import { useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { Eye, EyeOff, User, Briefcase, ChevronRight } from 'lucide-react'
import { registerUser } from '../../api/AuthApi'
import { useAuthStore } from '../../store/AuthStore'
import PublicNavbar from '../../components/common/PublicNavbar'
import { useTranslation } from '../../i18n/translations'
import './RegisterPage.css'
import './AuthPage.css'

const ROLE_ROUTES = {
  CANDIDATE: '/dashboard/candidate',
  EMPLOYER:  '/dashboard/employer',
}

const resolveRedirect = (data) =>
  data.onboardingCompleted ? (ROLE_ROUTES[data.role] ?? '/dashboard/candidate') : '/onboarding'

export default function RegisterPage() {
  const navigate = useNavigate()
  const setAuth  = useAuthStore((s) => s.setAuth)
  const t = useTranslation().register

  // Permet aux liens croisés (dashboard employeur → "chercher un emploi" et
  // inversement) d'ouvrir directement le formulaire sur le bon rôle, sans
  // repasser par l'étape de sélection.
  const [searchParams] = useSearchParams()
  const presetRole = searchParams.get('role')
  const initialRole = presetRole === 'CANDIDATE' || presetRole === 'EMPLOYER' ? presetRole : ''

  const [step, setStep]           = useState(initialRole ? 'form' : 'select')
  const [form, setForm]           = useState({ firstName: '', lastName: '', email: '', password: '', confirmPassword: '', role: initialRole })
  const [showPassword, setShowPw] = useState(false)
  const [showConfirm, setShowCf]  = useState(false)
  const [error, setError]         = useState('')
  const [loading, setLoading]     = useState(false)

  const selectRole = (role) => { setForm({ ...form, role }); setStep('form') }
  const handleChange = (e) => { setForm({ ...form, [e.target.name]: e.target.value }); setError('') }

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!form.firstName || !form.lastName || !form.email || !form.password)
      return setError(t.fillAllFields)
    if (form.password.length < 8)
      return setError(t.passwordTooShort)
    if (form.password !== form.confirmPassword)
      return setError(t.passwordMismatch)
    setLoading(true)
    try {
      const data = await registerUser({
        firstName: form.firstName, lastName: form.lastName,
        email: form.email, password: form.password, role: form.role,
      })
      setAuth(data)
      navigate(resolveRedirect(data))
    } catch (err) {
      setError(err.response?.data?.message || t.registerError)
    } finally {
      setLoading(false)
    }
  }

  const [candBrandPrefix, candBrandAccent] = t.candidateBrandTitle
  const [empBrandPrefix, empBrandAccent] = t.employerBrandTitle

  /* ── Étape 1 — Choix du profil ── */
  if (step === 'select') {
    return (
      <div className="auth-page">
        <PublicNavbar />
        <div className="reg-select-shell">

          <div className="reg-select-header">
            <h1>{t.joinTitle}</h1>
            <p>{t.joinSub}</p>
          </div>

          <div className="reg-select-cards">

            <button className="reg-role-card reg-role-card--candidate" onClick={() => selectRole('CANDIDATE')}>
              <div className="reg-role-card-body">
                <div className="reg-role-icon"><User size={24} /></div>
                <h2>{t.candidateTitle}</h2>
                <p>{t.candidateDesc}</p>
              </div>
              <div className="reg-role-card-footer">
                <span>{t.start}</span>
                <ChevronRight size={16} />
              </div>
              <div className="reg-role-card-deco" />
            </button>

            <button className="reg-role-card reg-role-card--employer" onClick={() => selectRole('EMPLOYER')}>
              <div className="reg-role-card-body">
                <div className="reg-role-icon"><Briefcase size={24} /></div>
                <h2>{t.employerTitle}</h2>
                <p>{t.employerDesc}</p>
              </div>
              <div className="reg-role-card-footer">
                <span>{t.start}</span>
                <ChevronRight size={16} />
              </div>
              <div className="reg-role-card-deco" />
            </button>

          </div>

          <p className="reg-select-login">
            {t.alreadyAccount} <Link to="/login">{t.signIn}</Link>
          </p>

        </div>
      </div>
    )
  }

  /* ── Étape 2 — Formulaire d'inscription ── */
  return (
    <div className="auth-page">
      <PublicNavbar />
      <div className="auth-shell">

      {/* Panneau gauche */}
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
            {form.role === 'CANDIDATE'
              ? <>{candBrandPrefix}<span className="auth-brand-accent">{candBrandAccent}</span></>
              : <>{empBrandPrefix}<span className="auth-brand-accent">{empBrandAccent}</span></>
            }
          </h1>
          <p className="auth-brand-subtitle">
            {form.role === 'CANDIDATE' ? t.candidateBrandSub : t.employerBrandSub}
          </p>
        </div>
      </div>

      {/* Panneau droit */}
      <div className="auth-form-panel">
        <div className="auth-card auth-card-register">

          <div className="auth-card-header">
            <button className="reg-back-btn" onClick={() => setStep('select')}>
              {t.back}
            </button>
            <h2>{t.formTitle}</h2>
            <p>{t.formSub}</p>
            <div className="reg-profile-badge">
              {form.role === 'CANDIDATE'
                ? <><User size={13} /> {t.candidateTitle}</>
                : <><Briefcase size={13} /> {t.employerTitle}</>
              }
            </div>
          </div>

          {error && <div className="auth-error">{error}</div>}

          <form onSubmit={handleSubmit} noValidate className="auth-form">

            <div className="auth-field-group">
              <div className="auth-field">
                <label htmlFor="firstName">{t.firstNameLabel}</label>
                <input
                  id="firstName" name="firstName" type="text" placeholder={t.firstNameLabel}
                  value={form.firstName} onChange={handleChange} autoComplete="given-name"
                />
              </div>
              <div className="auth-field">
                <label htmlFor="lastName">{t.lastNameLabel}</label>
                <input
                  id="lastName" name="lastName" type="text" placeholder={t.lastNameLabel}
                  value={form.lastName} onChange={handleChange} autoComplete="family-name"
                />
              </div>
            </div>

            <div className="auth-field">
              <label htmlFor="email">{t.emailLabel}</label>
              <input
                id="email" name="email" type="email" placeholder={t.emailPlaceholder}
                value={form.email} onChange={handleChange} autoComplete="email"
              />
            </div>

            <div className="auth-field">
              <label htmlFor="password">{t.passwordLabel}</label>
              <div className="auth-input-wrap">
                <input
                  id="password" name="password" type={showPassword ? 'text' : 'password'}
                  placeholder={t.passwordPlaceholder}
                  value={form.password} onChange={handleChange} autoComplete="new-password"
                />
                <button type="button" className="auth-eye" onClick={() => setShowPw(v => !v)}>
                  {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
            </div>

            <div className="auth-field">
              <label htmlFor="confirmPassword">{t.confirmPasswordLabel}</label>
              <div className="auth-input-wrap">
                <input
                  id="confirmPassword" name="confirmPassword" type={showConfirm ? 'text' : 'password'}
                  placeholder="••••••••"
                  value={form.confirmPassword} onChange={handleChange} autoComplete="new-password"
                />
                <button type="button" className="auth-eye" onClick={() => setShowCf(v => !v)}>
                  {showConfirm ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
            </div>

            <button type="submit" disabled={loading} className="auth-btn-primary">
              {loading ? <span className="auth-spinner" /> : t.createAccount}
            </button>

          </form>

          <p className="auth-switch" style={{ marginTop: '20px' }}>
            {t.alreadyAccount} <Link to="/login">{t.signIn}</Link>
          </p>

        </div>
      </div>

      </div>
    </div>
  )
}
