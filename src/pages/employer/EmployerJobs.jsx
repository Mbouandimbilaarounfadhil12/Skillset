import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Plus, Briefcase,
  MapPin, Users, Eye, ChevronRight, CheckCircle2, PauseCircle,
} from 'lucide-react'
import { useAuthStore } from '../../store/AuthStore'
import { JOBS }         from '../../data/mockData'
import AppNavbar        from '../../components/common/AppNavbar'
import { useTranslation } from '../../i18n/translations'
import './EmployerJobs.css'

function fmtSalary(s) {
  return `${(s.min / 1000).toFixed(0)}k – ${(s.max / 1000).toFixed(0)}k ${s.currency}`
}

export default function EmployerJobs() {
  const navigate = useNavigate()
  const { user } = useAuthStore()
  const t = useTranslation().employer.jobs
  const [closedIds, setClosedIds] = useState([])
  const [activeFilter, setActiveFilter] = useState('all')

  const companyName = user?.companyName || user?.company || null
  const allJobs = useMemo(() =>
    companyName
      ? JOBS.filter(j => j.company.toLowerCase().includes(companyName.toLowerCase()))
      : JOBS.slice(0, 6),
    [companyName]
  )

  const jobs = useMemo(() =>
    allJobs.filter(j => {
      const closed = closedIds.includes(j.id)
      if (activeFilter === 'active') return !closed
      if (activeFilter === 'closed') return closed
      return true
    }),
    [allJobs, closedIds, activeFilter]
  )

  const toggleClose = (id) => setClosedIds(prev => prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id])

  const FILTERS = [
    { id: 'all',    label: t.filters.all,    count: allJobs.length },
    { id: 'active', label: t.filters.active, count: allJobs.filter(j => !closedIds.includes(j.id)).length },
    { id: 'closed', label: t.filters.closed, count: closedIds.length },
  ]

  return (
    <div className="ej-shell">
      {/* Blobs de fond */}
      <div className="ej-blob ej-blob--main" />
      <div className="ej-blob ej-blob--accent" />

      <AppNavbar />

      {/* ── Contenu ── */}
      <main className="ej-main">

        {/* En-tête */}
        <div className="ej-header">
          <div>
            <h1 className="ej-title"><Briefcase size={22} /> {t.pageTitle}</h1>
            <p className="ej-subtitle">
              {allJobs.length} {allJobs.length !== 1 ? t.published.plural : t.published.singular}
            </p>
          </div>
          <button className="ej-new-btn" onClick={() => navigate('/employer/jobs/new')}>
            <Plus size={15} /> {t.newJob}
          </button>
        </div>

        {/* Filtres */}
        <div style={{ display: 'flex', gap: '8px', marginBottom: '24px' }}>
          {FILTERS.map(f => (
            <button
              key={f.id}
              onClick={() => setActiveFilter(f.id)}
              style={{
                padding: '8px 18px', borderRadius: '20px', fontSize: '13px', fontWeight: 600,
                cursor: 'pointer', border: 'none', transition: 'all 0.14s',
                background: activeFilter === f.id ? '#c42033' : 'var(--surface-card)',
                color: activeFilter === f.id ? '#fff' : 'var(--text-secondary)',
                boxShadow: activeFilter === f.id ? '0 2px 8px rgba(196,32,51,0.25)' : '0 0 0 1.5px var(--surface-border)',
              }}
            >
              {f.label} <span style={{ opacity: 0.75, marginLeft: '4px' }}>{f.count}</span>
            </button>
          ))}
        </div>

        {/* État vide */}
        {jobs.length === 0 ? (
          <div className="ej-empty">
            <Briefcase size={48} className="ej-empty-icon" />
            <h3>{t.emptyTitle}</h3>
            <p>{t.emptyHint}</p>
            <button className="ej-empty-btn" onClick={() => navigate('/employer/jobs/new')}>
              <Plus size={16} /> {t.newJob}
            </button>
          </div>
        ) : (
          <div className="ej-list">
            {jobs.map(job => {
              const isClosed = closedIds.includes(job.id)
              return (
                <div key={job.id} className={`ej-card${isClosed ? ' ej-card--closed' : ''}`}>

                  {/* Logo */}
                  <div className="ej-card-logo">{job.logo}</div>

                  {/* Infos */}
                  <div className="ej-card-body">
                    <div className="ej-card-top">
                      <h3 className="ej-card-title">{job.title}</h3>
                      <span className={`ej-status-badge ${isClosed ? 'ej-status-badge--closed' : 'ej-status-badge--active'}`}>
                        {isClosed ? <><PauseCircle size={11} /> {t.closedBadge}</> : <><CheckCircle2 size={11} /> {t.activeBadge}</>}
                      </span>
                    </div>
                    <div className="ej-card-meta">
                      <span>{job.type}</span>
                      <span>{job.experience}</span>
                      <span><MapPin size={11} /> {job.location?.split(',')[0] ?? job.location}</span>
                      {job.remote && <span>{t.remote}</span>}
                      <span style={{ color: '#c42033', fontWeight: 700 }}>{fmtSalary(job.salary)}</span>
                    </div>
                    {job.skills?.length > 0 && (
                      <div className="ej-card-skills">
                        {job.skills.slice(0, 4).map(s => (
                          <span key={s} className="ej-skill">{s}</span>
                        ))}
                      </div>
                    )}
                  </div>

                  {/* Stats */}
                  <div className="ej-card-stats">
                    <div className="ej-stat">
                      <span className="ej-stat-value"><Users size={14} style={{ display: 'inline', verticalAlign: 'middle' }} /> {job.applicants ?? 0}</span>
                      <span className="ej-stat-label">{(job.applicants ?? 0) !== 1 ? t.candidates.plural : t.candidates.singular}</span>
                    </div>
                  </div>

                  {/* Actions */}
                  <div className="ej-card-actions">
                    <button
                      className="ej-action-btn ej-action-btn--primary"
                      onClick={() => navigate('/employer/candidates')}
                    >
                      <Eye size={13} /> {t.viewCandidates} <ChevronRight size={12} />
                    </button>
                    <button
                      className={`ej-action-btn ${isClosed ? 'ej-action-btn--open' : 'ej-action-btn--ghost'}`}
                      onClick={() => toggleClose(job.id)}
                    >
                      {isClosed ? t.reopen : t.close}
                    </button>
                  </div>

                </div>
              )
            })}
          </div>
        )}
      </main>
    </div>
  )
}
