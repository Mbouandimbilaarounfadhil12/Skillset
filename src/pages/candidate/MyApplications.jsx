import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  CircleUser,
  MapPin, Clock, ChevronRight, Video, Building2, AlertCircle,
  ArrowRight, MessageCircle,
} from 'lucide-react'
import { useUserDataStore } from '../../store/UserDataStore'
import { useAuthStore }     from '../../store/AuthStore'
import { getCandidateApplications } from '../../api/ApplicationApi'
import { buildWhatsAppLink } from '../../utils/whatsapp'
import AppNavbar            from '../../components/common/AppNavbar'
import { useTranslation }   from '../../i18n/translations'
import './MyApplications.css'

const STATUS_STYLE = {
  'En cours':   { background: '#dbeafe', color: '#1d4ed8' },
  'En attente': { background: '#fef3c7', color: '#92400e' },
  'Refusé':     { background: '#fde8ea', color: '#c42033' },
  'Confirmé':   { background: '#dcfce7', color: '#166534' },
}

const STATUS_MAP = {
  SUBMITTED: { status: 'En attente', stage: 'CV transmis',      stageIndex: 0 },
  SCREENING: { status: 'En cours',   stage: 'Test technique',   stageIndex: 1 },
  INTERVIEW: { status: 'En cours',   stage: 'Entretien RH',     stageIndex: 2 },
  OFFER:     { status: 'En cours',   stage: 'Entretien final',  stageIndex: 3 },
  APPROVED:  { status: 'Confirmé',   stage: 'Décision',         stageIndex: 4 },
  REJECTED:  { status: 'Refusé',     stage: 'Candidature non retenue', stageIndex: 0 },
  WITHDRAWN: { status: 'Refusé',     stage: 'Candidature retirée', stageIndex: 0 },
}

const WHATSAPP_ELIGIBLE_STATUSES = new Set(['SCREENING', 'INTERVIEW', 'OFFER', 'APPROVED'])

function mapRealApplications(dtos) {
  return dtos.map(dto => {
    const meta = STATUS_MAP[dto.status] || STATUS_MAP.SUBMITTED
    return {
      id: dto.id,
      jobTitle: dto.jobTitle,
      company: dto.employerName || '',
      location: dto.jobLocation || '',
      type: dto.jobType || '',
      appliedDate: '',
      ...meta,
      employerPhone: dto.employerPhone,
      employerName: dto.employerName,
      rawStatus: dto.status,
    }
  })
}

export default function MyApplications() {
  const navigate = useNavigate()
  const { user } = useAuthStore()
  const { savedJobs, applications: mockApplications, interviews, archives, unsaveJob } = useUserDataStore()
  const t = useTranslation().candidate.myApplications

  const [realApplications, setRealApplications] = useState(null)
  useEffect(() => {
    if (!user?.id) return
    getCandidateApplications(user.id)
      .then(dtos => setRealApplications(mapRealApplications(dtos)))
      .catch(() => {})
  }, [user?.id])

  const applications = realApplications ?? mockApplications

  const [activeTab, setActiveTab] = useState('saved')

  const TABS = [
    { id: 'saved',        count: savedJobs.length,    label: t.tabs.saved },
    { id: 'applications', count: applications.length, label: t.tabs.applications },
    { id: 'interviews',   count: interviews.length,   label: t.tabs.interviews },
    { id: 'archives',     count: archives.length,     label: t.tabs.archives },
  ]

  return (
    <div className="ma-shell">
      <div className="ma-blob ma-blob--main" />
      <div className="ma-blob ma-blob--accent" />

      <AppNavbar />

      {/* Page header */}
      <div className="ma-page-header">
        <h1 className="ma-page-title">{t.pageTitle}</h1>
      </div>

      {/* Tab bar */}
      <div className="ma-tabs-wrap">
        <div className="ma-tabs">
          {TABS.map(tab => (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`ma-tab${activeTab === tab.id ? ' ma-tab--active' : ''}`}
            >
              <span className="ma-tab-count">{tab.count}</span>
              <span className="ma-tab-label">{tab.label}</span>
            </button>
          ))}
        </div>
        <div className="ma-tabs-line" />
      </div>

      {/* Content */}
      <div className="ma-content">

        {/* ── SAVED JOBS ── */}
        {activeTab === 'saved' && (
          <div className="ma-list">
            {savedJobs.length === 0 && (
              <div className="ma-empty">
                <div className="ma-empty-illo">🔖</div>
                <p className="ma-empty-title">{t.savedEmptyTitle}</p>
                <p className="ma-empty-hint">{t.savedEmptyHint}</p>
                <button className="ma-cta-btn" onClick={() => navigate('/jobs')}>
                  {t.findJob} <ArrowRight size={15} />
                </button>
              </div>
            )}
            {savedJobs.map(job => (
              <div key={job.id} className="ma-job-row">
                <span className="ma-job-logo">{job.logo}</span>
                <div className="ma-job-info">
                  <p className="ma-job-title">{job.title}</p>
                  <p className="ma-job-company">{job.company}</p>
                  <div className="ma-job-meta">
                    <span><MapPin size={11} />{job.location}</span>
                    <span><Clock size={11} />{t.savedOn} {job.savedAt?.split('T')[0]}</span>
                    {job.type && <span className="ma-badge">{job.type}</span>}
                  </div>
                </div>
                <div className="ma-job-actions">
                  <button className="ma-btn-primary" onClick={() => navigate('/jobs')}>{t.viewOffer}</button>
                  <button className="ma-btn-ghost" onClick={() => unsaveJob(job.id)}>{t.remove}</button>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* ── APPLICATIONS ── */}
        {activeTab === 'applications' && (
          <div className="ma-list">
            {applications.length === 0 && (
              <div className="ma-empty">
                <div className="ma-empty-illo">📄</div>
                <p className="ma-empty-title">{t.applicationsEmptyTitle}</p>
                <p className="ma-empty-hint">{t.applicationsEmptyHint}</p>
                <button className="ma-cta-btn" onClick={() => navigate('/jobs')}>
                  {t.searchJob} <ArrowRight size={15} />
                </button>
              </div>
            )}
            {applications.map(app => (
              <div key={app.id} className="ma-app-row">
                <div className="ma-app-head">
                  <span className="ma-job-logo">{app.logo || '💼'}</span>
                  <div className="ma-job-info">
                    <p className="ma-job-title">{app.jobTitle}</p>
                    <p className="ma-job-company">{app.company}{app.location ? ` · ${app.location}` : ''}</p>
                  </div>
                  <span
                    className="ma-status-badge"
                    style={STATUS_STYLE[app.status] ?? { background: '#f3f2f1', color: '#595959' }}
                  >
                    {app.status}
                  </span>
                </div>
                <div className="ma-app-meta">
                  <span><Clock size={11} />{t.appliedOn} {app.appliedDate}</span>
                  {app.type   && <span className="ma-badge">{app.type}</span>}
                  {app.salary && <span className="ma-salary-text">{app.salary}</span>}
                </div>
                {app.status !== 'Refusé' && (
                  <div className="ma-pipeline">
                    {t.stageSteps.map((s, i) => {
                      const done    = i < (app.stageIndex ?? 0)
                      const current = i === (app.stageIndex ?? 0)
                      return (
                        <div key={s} className="ma-pipeline-step">
                          {i < t.stageSteps.length - 1 && (
                            <div className={`ma-pipeline-line${done ? ' done' : ''}`} />
                          )}
                          <div className={`ma-pipeline-dot${done ? ' done' : current ? ' current' : ''}`} />
                          <span className={`ma-pipeline-label${current ? ' current' : ''}`}>{s}</span>
                        </div>
                      )
                    })}
                  </div>
                )}
                {app.status === 'Refusé' && (
                  <div className="ma-refused-note">
                    <AlertCircle size={13} /> {app.stage || t.notRetained}
                  </div>
                )}
                {WHATSAPP_ELIGIBLE_STATUSES.has(app.rawStatus) && app.employerPhone && (
                  <a
                    className="ma-whatsapp-btn"
                    href={buildWhatsAppLink(app.employerPhone, t.whatsappMessage?.replace('{job}', app.jobTitle))}
                    target="_blank"
                    rel="noreferrer"
                  >
                    <MessageCircle size={13} /> {t.contactWhatsapp}
                  </a>
                )}
              </div>
            ))}
          </div>
        )}

        {/* ── INTERVIEWS ── */}
        {activeTab === 'interviews' && (
          <div className="ma-list">
            {interviews.length === 0 && (
              <div className="ma-empty">
                <div className="ma-empty-illo">📅</div>
                <p className="ma-empty-title">{t.interviewsEmptyTitle}</p>
                <p className="ma-empty-hint">{t.interviewsEmptyHint}</p>
              </div>
            )}
            {interviews.map(itv => (
              <div key={itv.id} className="ma-interview-row">
                <div className="ma-itv-head">
                  <span className="ma-job-logo">{itv.logo || '🏢'}</span>
                  <div className="ma-job-info">
                    <p className="ma-job-title">{itv.jobTitle}</p>
                    <p className="ma-job-company">{itv.company}</p>
                  </div>
                  <span
                    className="ma-status-badge"
                    style={{ background: '#dcfce7', color: '#166534' }}
                  >
                    {itv.status}
                  </span>
                </div>
                <div className="ma-itv-details">
                  <div className="ma-itv-detail-item">
                    <Clock size={14} className="ma-itv-icon" />
                    <div>
                      <p className="ma-itv-label">{t.dateTime}</p>
                      <p className="ma-itv-value">{itv.date} {t.at} {itv.time}</p>
                    </div>
                  </div>
                  <div className="ma-itv-detail-item">
                    {itv.type === 'Visioconférence'
                      ? <Video size={14} className="ma-itv-icon" />
                      : <Building2 size={14} className="ma-itv-icon" />}
                    <div>
                      <p className="ma-itv-label">{t.format}</p>
                      <p className="ma-itv-value">{itv.type}{itv.platform ? ` · ${itv.platform}` : ''}</p>
                    </div>
                  </div>
                  <div className="ma-itv-detail-item">
                    <CircleUser size={14} className="ma-itv-icon" />
                    <div>
                      <p className="ma-itv-label">{t.contact}</p>
                      <p className="ma-itv-value">{itv.contact}</p>
                    </div>
                  </div>
                </div>
                {itv.calendlyLink && (
                  <div className="ma-calendly-wrap">
                    <a
                      href={itv.calendlyLink}
                      target="_blank"
                      rel="noreferrer"
                      className="ma-calendly-link"
                    >
                      📅 {t.confirmCalendly}
                    </a>
                  </div>
                )}
                {itv.notes && (
                  <div className="ma-itv-notes">
                    <p className="ma-itv-notes-label">{t.prepNotes}</p>
                    <p className="ma-itv-notes-body">{itv.notes}</p>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}

        {/* ── ARCHIVES ── */}
        {activeTab === 'archives' && (
          <div className="ma-list">
            {archives.length === 0 && (
              <div className="ma-empty">
                <div className="ma-empty-illo">📁</div>
                <p className="ma-empty-title">{t.archivesEmptyTitle}</p>
                <p className="ma-empty-hint">{t.archivesEmptyHint}</p>
              </div>
            )}
            {archives.map(arc => (
              <div key={arc.id} className="ma-job-row ma-job-row--muted">
                <span className="ma-job-logo">{arc.logo || '📁'}</span>
                <div className="ma-job-info">
                  <p className="ma-job-title">{arc.jobTitle}</p>
                  <p className="ma-job-company">{arc.company}</p>
                  <div className="ma-job-meta">
                    <span><Clock size={11} />{t.appliedOn} {arc.appliedDate}</span>
                    {arc.closedDate && <span><Clock size={11} />{t.closedOn} {arc.closedDate}</span>}
                  </div>
                  {arc.reason && <p className="ma-archive-reason">{arc.reason}</p>}
                </div>
                <ChevronRight size={16} className="ma-row-arrow" />
              </div>
            ))}
          </div>
        )}

      </div>
    </div>
  )
}
