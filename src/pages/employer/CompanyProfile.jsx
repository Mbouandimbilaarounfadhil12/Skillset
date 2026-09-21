import { useState, useMemo } from 'react'
import {
  Building2, MapPin, Globe, Users, Star, Save, Edit3, CheckCircle, Link2, Copy,
} from 'lucide-react'
import { useAuthStore } from '../../store/AuthStore'
import { COMPANIES }    from '../../data/mockData'
import AppNavbar        from '../../components/common/AppNavbar'
import { useTranslation } from '../../i18n/translations'
import './CompanyProfile.css'

const SIZES = ['1-10','10-50','50-200','200-500','500-1000','1000-5000','5000+']

export default function CompanyProfile() {
  const { user } = useAuthStore()
  const t = useTranslation().employer.company

  const companyName = user?.companyName || user?.company || null
  const baseCompany = useMemo(() => {
    if (companyName) {
      return COMPANIES.find(c => c.name.toLowerCase().includes(companyName.toLowerCase())) || null
    }
    return null
  }, [companyName])

  const [form, setForm] = useState({
    name:        companyName || baseCompany?.name || '',
    sector:      baseCompany?.sector || t.sectors[0],
    size:        baseCompany?.size   || '10-50',
    city:        baseCompany?.city   || 'Douala',
    country:     baseCompany?.country || 'Cameroun',
    website:     '',
    description: baseCompany?.description || '',
    tagline:     '',
    email:       user?.email || '',
    phone:       '',
  })
  const [saved,    setSaved]    = useState(false)
  const [editing,  setEditing]  = useState(false)
  const [copied,   setCopied]   = useState(false)

  const toSlug = (name) => (name || '').trim().toLowerCase()
    .replace(/[^a-z0-9\s-]/g, '').replace(/\s+/g, '-').replace(/-{2,}/g, '-') || 'company'

  const publicUrl = `${window.location.origin}/careers/${toSlug(form.name)}`

  const handleCopy = () => {
    navigator.clipboard.writeText(publicUrl).then(() => {
      setCopied(true); setTimeout(() => setCopied(false), 2000)
    })
  }

  const set = (key, val) => { setForm(prev => ({ ...prev, [key]: val })); setSaved(false) }

  const handleSave = () => {
    setSaved(true)
    setEditing(false)
    setTimeout(() => setSaved(false), 3000)
  }

  return (
    <div className="cp-shell">
      <div className="cp-blob cp-blob--main" />
      <div className="cp-blob cp-blob--accent" />

      <AppNavbar />

      <main className="cp-main">
        <div className="cp-page-header">
          <div>
            <h1 className="cp-title"><Building2 size={22} /> {t.pageTitle}</h1>
            <p className="cp-subtitle">{t.pageSubtitle}</p>
          </div>
          <div className="cp-header-actions">
            {saved && (
              <span className="cp-saved-msg"><CheckCircle size={15} /> {t.saved}</span>
            )}
            {editing ? (
              <>
                <button className="cp-cancel-btn" onClick={() => setEditing(false)}>{t.cancel}</button>
                <button className="cp-save-btn" onClick={handleSave}><Save size={15} /> {t.save}</button>
              </>
            ) : (
              <button className="cp-edit-btn" onClick={() => setEditing(true)}><Edit3 size={15} /> {t.edit}</button>
            )}
          </div>
        </div>

        {/* ── Public careers URL ── */}
        <div className="cp-careers-banner">
          <Link2 size={15} />
          <span className="cp-careers-label">{t.careersLabel}</span>
          <a href={publicUrl} target="_blank" rel="noreferrer" className="cp-careers-url">{publicUrl}</a>
          <button className="cp-careers-copy" onClick={handleCopy} title={t.copyTitle}>
            {copied ? <CheckCircle size={14} /> : <Copy size={14} />}
            {copied ? t.copied : t.copy}
          </button>
        </div>

        <div className="cp-layout">

          {/* ── Form panel ── */}
          <section className="cp-form-panel">

            {/* Cover / Logo preview */}
            <div className="cp-cover">
              <div className="cp-company-logo">{baseCompany?.logo || '🏢'}</div>
            </div>

            <div className="cp-form-body">

              <div className="cp-field-group">
                <label className="cp-label">{t.companyName}</label>
                {editing
                  ? <input className="cp-input" value={form.name} onChange={e => set('name', e.target.value)} placeholder={t.companyNamePlaceholder} />
                  : <p className="cp-value">{form.name || '—'}</p>
                }
              </div>

              <div className="cp-field-group">
                <label className="cp-label">{t.tagline}</label>
                {editing
                  ? <input className="cp-input" value={form.tagline} onChange={e => set('tagline', e.target.value)} placeholder={t.taglinePlaceholder} />
                  : <p className="cp-value">{form.tagline || '—'}</p>
                }
              </div>

              <div className="cp-two-cols">
                <div className="cp-field-group">
                  <label className="cp-label">{t.sector}</label>
                  {editing
                    ? (
                      <select className="cp-select" value={form.sector} onChange={e => set('sector', e.target.value)}>
                        {t.sectors.map(s => <option key={s} value={s}>{s}</option>)}
                      </select>
                    )
                    : <p className="cp-value">{form.sector}</p>
                  }
                </div>
                <div className="cp-field-group">
                  <label className="cp-label">{t.size}</label>
                  {editing
                    ? (
                      <select className="cp-select" value={form.size} onChange={e => set('size', e.target.value)}>
                        {SIZES.map(s => <option key={s} value={s}>{s} {t.employees}</option>)}
                      </select>
                    )
                    : <p className="cp-value">{form.size} {t.employees}</p>
                  }
                </div>
              </div>

              <div className="cp-two-cols">
                <div className="cp-field-group">
                  <label className="cp-label"><MapPin size={12} /> {t.city}</label>
                  {editing
                    ? <input className="cp-input" value={form.city} onChange={e => set('city', e.target.value)} placeholder={t.cityPlaceholder} />
                    : <p className="cp-value">{form.city || '—'}</p>
                  }
                </div>
                <div className="cp-field-group">
                  <label className="cp-label">{t.country}</label>
                  {editing
                    ? <input className="cp-input" value={form.country} onChange={e => set('country', e.target.value)} placeholder={t.countryPlaceholder} />
                    : <p className="cp-value">{form.country || '—'}</p>
                  }
                </div>
              </div>

              <div className="cp-two-cols">
                <div className="cp-field-group">
                  <label className="cp-label"><Globe size={12} /> {t.website}</label>
                  {editing
                    ? <input className="cp-input" value={form.website} onChange={e => set('website', e.target.value)} placeholder={t.websitePlaceholder} />
                    : <p className="cp-value">{form.website || '—'}</p>
                  }
                </div>
                <div className="cp-field-group">
                  <label className="cp-label">{t.contactEmail}</label>
                  {editing
                    ? <input className="cp-input" value={form.email} onChange={e => set('email', e.target.value)} placeholder={t.contactEmailPlaceholder} />
                    : <p className="cp-value">{form.email || '—'}</p>
                  }
                </div>
              </div>

              <div className="cp-field-group">
                <label className="cp-label">{t.description}</label>
                {editing
                  ? (
                    <textarea
                      className="cp-textarea"
                      rows={4}
                      value={form.description}
                      onChange={e => set('description', e.target.value)}
                      placeholder={t.descriptionPlaceholder}
                    />
                  )
                  : <p className="cp-value cp-value--multi">{form.description || '—'}</p>
                }
              </div>

            </div>
          </section>

          {/* ── Preview panel ── */}
          <aside className="cp-preview-panel">
            <p className="cp-preview-title">{t.previewTitle}</p>
            <div className="cp-preview-card">
              <div className="cp-preview-cover">
                <div className="cp-preview-logo">{baseCompany?.logo || '🏢'}</div>
              </div>
              <div className="cp-preview-body">
                <h3 className="cp-preview-name">{form.name || t.previewDefaultName}</h3>
                {form.tagline && <p className="cp-preview-tagline">{form.tagline}</p>}
                <div className="cp-preview-meta">
                  <span><Building2 size={12} /> {form.sector}</span>
                  <span><Users size={12} /> {form.size} {t.employees}</span>
                  <span><MapPin size={12} /> {form.city}, {form.country}</span>
                </div>
                {baseCompany && (
                  <div className="cp-preview-rating">
                    {[1,2,3,4,5].map(n => (
                      <Star
                        key={n}
                        size={13}
                        fill={n <= Math.round(baseCompany.rating) ? '#f5a623' : 'none'}
                        color={n <= Math.round(baseCompany.rating) ? '#f5a623' : '#ddd'}
                      />
                    ))}
                    <span className="cp-preview-rating-val">{baseCompany.rating}</span>
                    <span className="cp-preview-rating-count">({baseCompany.reviewCount} {t.reviews})</span>
                  </div>
                )}
                {form.description && (
                  <p className="cp-preview-desc">{form.description}</p>
                )}
              </div>
            </div>
          </aside>
        </div>
      </main>
    </div>
  )
}
