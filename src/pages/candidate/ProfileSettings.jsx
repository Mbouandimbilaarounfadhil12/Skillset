import { useState, useEffect, useRef } from 'react'
import { NavLink } from 'react-router-dom'
import {
  Mail, Phone, MapPin, ChevronRight, ChevronDown,
  Save, CheckCircle, CircleUser, Camera,
} from 'lucide-react'
import { useAuthStore }  from '../../store/AuthStore'
import { usePreferencesStore } from '../../store/PreferencesStore'
import { updateProfile, getProfile, uploadProfilePhoto } from '../../api/AuthApi'
import AppNavbar         from '../../components/common/AppNavbar'
import { useTranslation } from '../../i18n/translations'
import './ProfileSettings.css'

export default function ProfileSettings() {
  const { user, updateUser } = useAuthStore()
  const { language } = usePreferencesStore()
  const t = useTranslation().candidate.profile
  const [saved, setSaved] = useState(false)
  const [openAccordion, setOpenAccordion] = useState(null)
  const [visible, setVisible]       = useState(true)
  const [photoUploading, setPhotoUploading] = useState(false)
  const [photoError, setPhotoError] = useState('')
  const fileInputRef = useRef(null)

  const [form, setForm] = useState({
    firstName:       user?.firstName || '',
    lastName:        user?.lastName  || '',
    email:           user?.email     || '',
    phone:           user?.phoneNumber || '',
    location:        '',
    jobDomain:       '',
    desiredRole:     '',
    experienceLevel: '',
    contractType:    '',
    skills:          '',
    bio:             '',
    profilePictureUrl: user?.profilePictureUrl || '',
  })

  useEffect(() => {
    if (!user?.id) return
    getProfile(user.id).then(profile => {
      setForm(f => ({
        ...f,
        firstName:       profile.firstName || f.firstName,
        lastName:        profile.lastName  || f.lastName,
        phone:           profile.phoneNumber || f.phone,
        location:        profile.location || '',
        jobDomain:       profile.jobDomain || '',
        desiredRole:     profile.desiredRole || '',
        experienceLevel: profile.experienceLevel || '',
        contractType:    profile.contractType || '',
        skills:          profile.skills || '',
        bio:             profile.bio || '',
        profilePictureUrl: profile.profilePictureUrl || '',
      }))
    }).catch(() => {})
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.id])

  const handleChange = e => setForm({ ...form, [e.target.name]: e.target.value })

  const handleSave = (e) => {
    e.preventDefault()
    updateUser({ firstName: form.firstName, lastName: form.lastName, phoneNumber: form.phone })
    if (user?.id) {
      updateProfile(user.id, {
        firstName: form.firstName,
        lastName:  form.lastName,
        phoneNumber: form.phone,
        location: form.location,
        jobDomain: form.jobDomain,
        desiredRole: form.desiredRole,
        experienceLevel: form.experienceLevel,
        contractType: form.contractType,
        skills: form.skills,
        bio: form.bio,
      }).catch(() => {})
    }
    setSaved(true)
    setTimeout(() => setSaved(false), 3000)
  }

  const handlePhotoClick = () => fileInputRef.current?.click()

  const handlePhotoChange = async (e) => {
    const file = e.target.files?.[0]
    if (!file || !user?.id) return
    setPhotoError('')
    setPhotoUploading(true)
    try {
      const updated = await uploadProfilePhoto(user.id, file)
      setForm(f => ({ ...f, profilePictureUrl: updated.profilePictureUrl }))
      updateUser({ profilePictureUrl: updated.profilePictureUrl })
    } catch {
      setPhotoError(language === 'fr' ? "Échec de l'envoi de la photo." : 'Photo upload failed.')
    } finally {
      setPhotoUploading(false)
      e.target.value = ''
    }
  }

  const initials = `${(form.firstName[0] || '').toUpperCase()}${(form.lastName[0] || '').toUpperCase()}`
  const cvDate = new Date().toLocaleDateString(language === 'fr' ? 'fr-FR' : 'en-US', { day: 'numeric', month: 'long', year: 'numeric' })

  return (
    <div className="ps-shell">

      <AppNavbar />

      <main className="ps-main">
        <div className="ps-content">

          {/* ── Profile header — GRACE DIVINE style ── */}
          <div className="ps-profile-header">
            <div className="ps-profile-left">
              <h1 className="ps-full-name">{form.firstName.toUpperCase()} {form.lastName.toUpperCase()}</h1>
              <div className="ps-contact-rows">
                <div className="ps-contact-row">
                  <Mail size={15} className="ps-contact-icon" />
                  <span>{form.email}</span>
                </div>
                <div className="ps-contact-row">
                  <Phone size={15} className="ps-contact-icon" />
                  <span>{form.phone || t.addPhone}</span>
                </div>
                <div className="ps-contact-row">
                  <MapPin size={15} className="ps-contact-icon" />
                  <span>{form.location || t.addLocation}</span>
                </div>
              </div>
              {/* Visibility toggle */}
              <div className={`ps-visibility-banner ${visible ? 'ps-visibility-banner--on' : 'ps-visibility-banner--off'}`}>
                <span className="ps-vis-dot" />
                <span>{visible ? t.visibleOn : t.visibleOff}</span>
                <button className="ps-vis-toggle" onClick={() => setVisible(v => !v)}>
                  <ChevronDown size={16} />
                </button>
              </div>
            </div>
            <div className="ps-avatar-wrap">
              <button
                type="button"
                className="ps-avatar-circle ps-avatar-circle--clickable"
                onClick={handlePhotoClick}
                disabled={photoUploading}
                title={t.changePhoto}
              >
                {form.profilePictureUrl
                  ? <img src={form.profilePictureUrl} alt="" className="ps-avatar-img" />
                  : (initials || <CircleUser size={32} />)}
                <span className="ps-avatar-overlay">
                  <Camera size={16} />
                </span>
              </button>
              <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                className="ps-avatar-input"
                onChange={handlePhotoChange}
              />
              {photoUploading && <span className="ps-avatar-status">{language === 'fr' ? 'Envoi…' : 'Uploading…'}</span>}
              {photoError && <span className="ps-avatar-status ps-avatar-status--error">{photoError}</span>}
            </div>
          </div>

          {/* ── CV section ── */}
          <section className="ps-section">
            <h2 className="ps-section-title">{t.cvTitle}</h2>
            <div className="ps-cv-card">
              <div className="ps-cv-icon">PDF</div>
              <div className="ps-cv-info">
                <p className="ps-cv-name">{t.cvFileName}</p>
                <p className="ps-cv-date">{t.cvAddedOn} {cvDate}</p>
              </div>
            </div>
          </section>

          <hr className="ps-divider" />

          {/* ── Improve job suggestions ── */}
          <section className="ps-section">
            <h2 className="ps-section-title">{t.suggestionsTitle}</h2>
            <div className="ps-accordion-list">
              {t.accordions.map(acc => (
                <div key={acc.id} className="ps-accordion-item">
                  <button
                    className="ps-accordion-btn"
                    onClick={() => setOpenAccordion(openAccordion === acc.id ? null : acc.id)}
                  >
                    <div className="ps-accordion-text">
                      <span className="ps-accordion-label">{acc.label}</span>
                      <span className="ps-accordion-sub">{acc.sub}</span>
                    </div>
                    <ChevronRight size={18} className={`ps-accordion-arrow ${openAccordion === acc.id ? 'ps-accordion-arrow--open' : ''}`} />
                  </button>
                  {openAccordion === acc.id && (
                    <div className="ps-accordion-body">
                      {acc.id === 'qualifications' && (
                        <form onSubmit={handleSave} className="ps-form-group">
                          <div className="ps-field">
                            <label>{t.jobDomainLabel}</label>
                            <select name="jobDomain" value={form.jobDomain} onChange={handleChange}>
                              <option value="">{t.jobDomainPlaceholder}</option>
                              {t.jobDomains.map(d => <option key={d}>{d}</option>)}
                            </select>
                          </div>
                          <div className="ps-field">
                            <label>{t.experienceLabel}</label>
                            <select name="experienceLevel" value={form.experienceLevel} onChange={handleChange}>
                              <option value="">{t.selectPlaceholder}</option>
                              {t.experienceLevels.map(l => <option key={l}>{l}</option>)}
                            </select>
                          </div>
                          <div className="ps-field">
                            <label>{t.skillsLabel}</label>
                            <input name="skills" value={form.skills} onChange={handleChange} placeholder={t.skillsPlaceholder} />
                          </div>
                          <div className="ps-field">
                            <label>{t.bioLabel}</label>
                            <textarea name="bio" value={form.bio} onChange={handleChange} rows={3} placeholder={t.bioPlaceholder} />
                          </div>
                          <button type="submit" className="ps-save-btn">
                            <Save size={14} /> {t.save}
                          </button>
                          {saved && <span className="ps-saved-msg"><CheckCircle size={14} /> {t.saved}</span>}
                        </form>
                      )}
                      {acc.id === 'preferences' && (
                        <form onSubmit={handleSave} className="ps-form-group">
                          <div className="ps-field">
                            <label>{t.desiredRoleLabel}</label>
                            <input name="desiredRole" value={form.desiredRole} onChange={handleChange} placeholder={t.desiredRolePlaceholder} />
                          </div>
                          <div className="ps-field">
                            <label>{t.contractTypeLabel}</label>
                            <select name="contractType" value={form.contractType} onChange={handleChange}>
                              <option value="">{t.selectPlaceholder}</option>
                              {t.contractTypes.map(ct => <option key={ct}>{ct}</option>)}
                            </select>
                          </div>
                          <button type="submit" className="ps-save-btn">
                            <Save size={14} /> {t.save}
                          </button>
                          {saved && <span className="ps-saved-msg"><CheckCircle size={14} /> {t.saved}</span>}
                        </form>
                      )}
                      {acc.id === 'exclude' && (
                        <form onSubmit={handleSave} className="ps-form-group">
                          <div className="ps-field">
                            <label>{t.desiredLocationLabel}</label>
                            <input name="location" value={form.location} onChange={handleChange} placeholder={t.desiredLocationPlaceholder} />
                          </div>
                          <button type="submit" className="ps-save-btn">
                            <Save size={14} /> {t.save}
                          </button>
                          {saved && <span className="ps-saved-msg"><CheckCircle size={14} /> {t.saved}</span>}
                        </form>
                      )}
                      {acc.id === 'available' && (
                        <div className="ps-form-group">
                          <p className="ps-accordion-hint">{t.availableHint}</p>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              ))}
            </div>
          </section>

          <hr className="ps-divider" />

          {/* ── Personal info (inline edit) ── */}
          <section className="ps-section">
            <h2 className="ps-section-title">{t.reviewsSectionTitle}</h2>
            <NavLink to="/my-jobs" className="ps-review-link">
              <div>
                <p className="ps-review-label">{t.myReviews}</p>
                <p className="ps-review-sub">{t.reviewsSub}</p>
              </div>
              <ChevronRight size={18} />
            </NavLink>
          </section>

        </div>
      </main>
    </div>
  )
}
