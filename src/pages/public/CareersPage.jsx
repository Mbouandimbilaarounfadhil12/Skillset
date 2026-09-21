import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  MapPin, Users, Globe, ExternalLink,
  ChevronRight, X, Upload, Loader2, CheckCircle, AlertTriangle,
} from 'lucide-react'
import { getCareerPage } from '../../api/PublicApi'
import { submitApplication } from '../../api/ApplicationApi'
import { useAuthStore } from '../../store/AuthStore'
import './CareersPage.css'

function JobCard({ job, onApply }) {
  return (
    <div className="cp-job-card">
      <div className="cp-job-info">
        <p className="cp-job-title">{job.title}</p>
        <div className="cp-job-meta">
          {job.location && <span><MapPin size={13} /> {job.location}</span>}
          {job.jobType && <span><span size={13} /> {job.jobType}</span>}
          {(job.salaryMin || job.salaryMax) && (
            <span>{job.salaryMin} – {job.salaryMax}</span>
          )}
        </div>
        {job.requiredSkills && (
          <p className="cp-job-skills">{job.requiredSkills}</p>
        )}
      </div>
      <button className="cp-apply-btn" onClick={() => onApply(job)}>
        Postuler <ChevronRight size={15} />
      </button>
    </div>
  )
}

function ApplyModal({ job, onClose, isAuthenticated, onLoginRequired }) {
  const [coverLetter, setCoverLetter] = useState('')
  const [cvFile, setCvFile] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const [success, setSuccess] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()
    if (!isAuthenticated) { onLoginRequired(job); return }
    setSubmitting(true)
    setError('')
    try {
      await submitApplication(job.id, coverLetter, cvFile)
      setSuccess(true)
    } catch (err) {
      setError(err?.response?.data?.message || 'Une erreur est survenue.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="cp-modal-backdrop" onClick={onClose}>
      <div className="cp-modal" onClick={e => e.stopPropagation()}>
        <div className="cp-modal-header">
          <div>
            <p className="cp-modal-title">Postuler — {job.title}</p>
            <p className="cp-modal-sub">{job.location} · {job.jobType}</p>
          </div>
          <button className="cp-modal-close" onClick={onClose}><X size={18} /></button>
        </div>

        {success ? (
          <div className="cp-success">
            <CheckCircle size={36} className="cp-success-icon" />
            <p className="cp-success-title">Candidature envoyée !</p>
            <p className="cp-success-sub">Vous serez contacté(e) prochainement.</p>
            <button className="cp-apply-btn" onClick={onClose}>Fermer</button>
          </div>
        ) : (
          <form className="cp-form" onSubmit={handleSubmit}>
            {error && (
              <div className="cp-form-error">
                <AlertTriangle size={15} /> {error}
              </div>
            )}
            <label className="cp-label">
              Lettre de motivation
              <textarea
                className="cp-textarea"
                placeholder="Présentez-vous et expliquez votre intérêt pour ce poste…"
                value={coverLetter}
                onChange={e => setCoverLetter(e.target.value)}
                rows={5}
              />
            </label>
            <label className="cp-label cp-file-label">
              CV (PDF)
              <div className="cp-file-btn">
                <Upload size={15} />
                {cvFile ? cvFile.name : 'Choisir un fichier…'}
                <input
                  type="file"
                  accept=".pdf"
                  className="cp-file-input"
                  onChange={e => setCvFile(e.target.files[0] || null)}
                />
              </div>
            </label>
            <button className="cp-submit-btn" type="submit" disabled={submitting}>
              {submitting ? <Loader2 size={16} className="cp-spin" /> : null}
              {submitting ? 'Envoi…' : 'Envoyer ma candidature'}
            </button>
          </form>
        )}
      </div>
    </div>
  )
}

export default function CareersPage() {
  const { slug } = useParams()
  const navigate = useNavigate()
  const { user } = useAuthStore()

  const [company, setCompany] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [activeJob, setActiveJob] = useState(null)

  useEffect(() => {
    getCareerPage(slug)
      .then(setCompany)
      .catch(() => setError('Page introuvable ou aucune offre disponible.'))
      .finally(() => setLoading(false))
  }, [slug])

  const handleLoginRequired = (job) => {
    sessionStorage.setItem('postLoginRedirect', `/careers/${slug}`)
    sessionStorage.setItem('pendingJobId', job.id)
    navigate('/login')
  }

  if (loading) return (
    <div className="cp-loader">
      <Loader2 size={36} className="cp-spin" />
    </div>
  )

  if (error || !company) return (
    <div className="cp-error-page">
      <AlertTriangle size={40} />
      <p>{error || 'Page introuvable.'}</p>
      <button className="cp-apply-btn" onClick={() => navigate('/')}>Retour</button>
    </div>
  )

  return (
    <div className="cp-shell">
      {/* Company header */}
      <header className="cp-header">
        <div className="cp-header-inner">
          <div className="cp-company-logo">
            {(company.companyName || 'C').charAt(0).toUpperCase()}
          </div>
          <div>
            <h1 className="cp-company-name">{company.companyName}</h1>
            <div className="cp-company-meta">
              {company.industry && <span>{company.industry}</span>}
              {company.companySize && <span><Users size={13} /> {company.companySize}</span>}
              {(company.companyCity || company.companyCountry) && (
                <span><MapPin size={13} /> {[company.companyCity, company.companyCountry].filter(Boolean).join(', ')}</span>
              )}
              {company.companyWebsite && (
                <a href={company.companyWebsite} target="_blank" rel="noreferrer" className="cp-link">
                  <Globe size={13} /> Site web
                </a>
              )}
              {company.companyLinkedIn && (
                <a href={company.companyLinkedIn} target="_blank" rel="noreferrer" className="cp-link">
                  <ExternalLink size={13} /> LinkedIn
                </a>
              )}
            </div>
          </div>
        </div>
      </header>

      {/* Job listings */}
      <main className="cp-main">
        <div className="cp-section-head">
          <h2 className="cp-section-title">Offres ouvertes</h2>
          <span className="cp-job-count">{company.openJobs.length} poste{company.openJobs.length !== 1 ? 's' : ''}</span>
        </div>

        {company.openJobs.length === 0 ? (
          <div className="cp-empty">
            <span size={36} />
            <p>Aucun poste ouvert pour le moment.</p>
          </div>
        ) : (
          <div className="cp-jobs">
            {company.openJobs.map(job => (
              <JobCard key={job.id} job={job} onApply={setActiveJob} />
            ))}
          </div>
        )}
      </main>

      {activeJob && (
        <ApplyModal
          job={activeJob}
          onClose={() => setActiveJob(null)}
          isAuthenticated={!!user}
          onLoginRequired={handleLoginRequired}
        />
      )}
    </div>
  )
}
