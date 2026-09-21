import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Plus, X, Sparkles } from 'lucide-react'
import { useAuthStore } from '../../store/AuthStore'
import AppNavbar        from '../../components/common/AppNavbar'
import { useTranslation } from '../../i18n/translations'
import './PostJobPage.css'

export default function PostJobPage() {
  const navigate = useNavigate()
  const { user } = useAuthStore()
  const t = useTranslation().employer.postJob

  const [form, setForm] = useState({
    title: '', company: user?.company || '', location: '', country: 'Cameroun',
    type: t.contractTypes[0], sector: t.sectors[0], experience: t.experienceLevels[2],
    salaryMin: '', salaryMax: '', currency: 'FCFA',
    remote: false, description: '', requirements: '', skills: [],
  })
  const [skillInput, setSkillInput] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [done, setDone] = useState(false)

  const set = (key, val) => setForm(prev => ({ ...prev, [key]: val }))

  const addSkill = () => {
    const s = skillInput.trim()
    if (s && !form.skills.includes(s)) {
      set('skills', [...form.skills, s])
      setSkillInput('')
    }
  }

  const removeSkill = (s) => set('skills', form.skills.filter(x => x !== s))

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSubmitting(true)
    await new Promise(r => setTimeout(r, 1200))
    setSubmitting(false)
    setDone(true)
  }

  if (done) {
    return (
      <div className="pj-shell">
        <div className="pj-success">
          <div className="pj-success-icon">✅</div>
          <h2 className="pj-success-title">{t.successTitle}</h2>
          <p className="pj-success-sub">{t.successSub}</p>
          <div className="pj-success-actions">
            <button className="pj-btn-primary" onClick={() => navigate('/dashboard/employer')}>{t.backToDashboard}</button>
            <button className="pj-btn-ghost" onClick={() => { setDone(false); setForm({ ...form, title: '', description: '' }) }}>
              {t.postAnother}
            </button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="pj-shell">
      <div className="pj-blob pj-blob--main" />

      <AppNavbar />

      <main className="pj-main">
        <div className="pj-header">
          <h1 className="pj-title">{t.pageTitle}</h1>
          <p className="pj-sub">{t.pageSub}</p>
        </div>

        <form className="pj-form" onSubmit={handleSubmit}>

          {/* Infos générales */}
          <div className="pj-card">
            <h2 className="pj-card-title">{t.generalInfo}</h2>
            <div className="pj-grid-2">
              <div className="pj-field">
                <label className="pj-label">{t.jobTitleLabel}</label>
                <input className="pj-input" required placeholder={t.jobTitlePlaceholder} value={form.title} onChange={e => set('title', e.target.value)} />
              </div>
              <div className="pj-field">
                <label className="pj-label">{t.companyLabel}</label>
                <input className="pj-input" required placeholder={t.companyPlaceholder} value={form.company} onChange={e => set('company', e.target.value)} />
              </div>
              <div className="pj-field">
                <label className="pj-label">{t.cityLabel}</label>
                <input className="pj-input" required placeholder={t.cityPlaceholder} value={form.location} onChange={e => set('location', e.target.value)} />
              </div>
              <div className="pj-field">
                <label className="pj-label">{t.countryLabel}</label>
                <input className="pj-input" value={form.country} onChange={e => set('country', e.target.value)} />
              </div>
            </div>

            <div className="pj-grid-3">
              <div className="pj-field">
                <label className="pj-label">{t.contractTypeLabel}</label>
                <select className="pj-select" value={form.type} onChange={e => set('type', e.target.value)}>
                  {t.contractTypes.map(ct => <option key={ct}>{ct}</option>)}
                </select>
              </div>
              <div className="pj-field">
                <label className="pj-label">{t.sectorLabel}</label>
                <select className="pj-select" value={form.sector} onChange={e => set('sector', e.target.value)}>
                  {t.sectors.map(s => <option key={s}>{s}</option>)}
                </select>
              </div>
              <div className="pj-field">
                <label className="pj-label">{t.experienceLabel}</label>
                <select className="pj-select" value={form.experience} onChange={e => set('experience', e.target.value)}>
                  {t.experienceLevels.map(l => <option key={l}>{l}</option>)}
                </select>
              </div>
            </div>

            <div className="pj-field">
              <label className="pj-toggle-label">
                <input type="checkbox" checked={form.remote} onChange={e => set('remote', e.target.checked)} />
                <span className="pj-toggle-track" />
                {t.remoteLabel}
              </label>
            </div>
          </div>

          {/* Salaire */}
          <div className="pj-card">
            <h2 className="pj-card-title">{t.compensation}</h2>
            <div className="pj-grid-3">
              <div className="pj-field">
                <label className="pj-label">{t.salaryMinLabel}</label>
                <input className="pj-input" type="number" placeholder="Ex: 300000" value={form.salaryMin} onChange={e => set('salaryMin', e.target.value)} />
              </div>
              <div className="pj-field">
                <label className="pj-label">{t.salaryMaxLabel}</label>
                <input className="pj-input" type="number" placeholder="Ex: 600000" value={form.salaryMax} onChange={e => set('salaryMax', e.target.value)} />
              </div>
              <div className="pj-field">
                <label className="pj-label">{t.currencyLabel}</label>
                <select className="pj-select" value={form.currency} onChange={e => set('currency', e.target.value)}>
                  <option>FCFA</option>
                  <option>EUR</option>
                  <option>XOF</option>
                  <option>MAD</option>
                </select>
              </div>
            </div>
            <div className="pj-stella-hint">
              <Sparkles size={14} />
              {t.stellaHint}
            </div>
          </div>

          {/* Description */}
          <div className="pj-card">
            <h2 className="pj-card-title">{t.descriptionSection}</h2>
            <div className="pj-field">
              <label className="pj-label">{t.descriptionLabel}</label>
              <textarea className="pj-textarea" required rows={5} placeholder={t.descriptionPlaceholder} value={form.description} onChange={e => set('description', e.target.value)} />
            </div>
            <div className="pj-field">
              <label className="pj-label">{t.profileLabel}</label>
              <textarea className="pj-textarea" rows={3} placeholder={t.profilePlaceholder} value={form.requirements} onChange={e => set('requirements', e.target.value)} />
            </div>
          </div>

          {/* Compétences */}
          <div className="pj-card">
            <h2 className="pj-card-title">{t.skillsSection}</h2>
            <div className="pj-skill-input-row">
              <input
                className="pj-input"
                placeholder={t.skillsPlaceholder}
                value={skillInput}
                onChange={e => setSkillInput(e.target.value)}
                onKeyDown={e => { if (e.key === 'Enter') { e.preventDefault(); addSkill() } }}
              />
              <button type="button" className="pj-add-skill-btn" onClick={addSkill}>
                <Plus size={16} />
              </button>
            </div>
            {form.skills.length > 0 && (
              <div className="pj-skills-list">
                {form.skills.map(s => (
                  <span key={s} className="pj-skill-tag">
                    {s}
                    <button type="button" onClick={() => removeSkill(s)} aria-label={`${t.removeSkill} ${s}`}><X size={12} /></button>
                  </span>
                ))}
              </div>
            )}
          </div>

          <div className="pj-form-footer">
            <button type="button" className="pj-btn-ghost" onClick={() => navigate('/dashboard/employer')}>{t.cancel}</button>
            <button type="submit" className="pj-btn-primary" disabled={submitting}>
              {submitting ? t.publishing : t.publish}
            </button>
          </div>
        </form>
      </main>
    </div>
  )
}
