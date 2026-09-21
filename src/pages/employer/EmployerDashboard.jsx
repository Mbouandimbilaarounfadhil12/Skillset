import { useMemo, useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  CircleUser, Plus,
  Briefcase, Users, CheckCircle, ChevronRight,
  BarChart2, Calendar,
} from 'lucide-react'
import { useAuthStore }       from '../../store/AuthStore'
import { usePreferencesStore } from '../../store/PreferencesStore'
import { JOBS }               from '../../data/mockData'
import { getCompanyJobs, getSuggestedCandidates } from '../../api/JobApi'
import AppNavbar              from '../../components/common/AppNavbar'
import AutomationRulesPanel   from '../../features/automation/AutomationRulesPanel'
import TalentPoolsPanel       from '../../features/talentpool/TalentPoolList'
import { useTranslation }     from '../../i18n/translations'
import './EmployerDashboard.css'

function DonutChart({ pct = 0 }) {
  const r = 40, cx = 60, cy = 60
  const circ = 2 * Math.PI * r
  const dash = (Math.min(100, Math.max(0, pct)) / 100) * circ
  return (
    <svg width="130" height="130" viewBox="0 0 120 120">
      <circle cx={cx} cy={cy} r={r} fill="none" stroke="var(--surface-border-soft)" strokeWidth="14" />
      <circle cx={cx} cy={cy} r={r} fill="none" stroke="#C42033" strokeWidth="14"
        strokeDasharray={`${dash} ${circ - dash}`} strokeLinecap="round"
        transform={`rotate(-90 ${cx} ${cy})`} />
      <text x={cx} y={cy + 4} textAnchor="middle" fontSize="20" fontWeight="bold" fill="currentColor" fontFamily="system-ui" style={{ color: 'var(--text-primary)' }}>{pct}</text>
    </svg>
  )
}

function MiniCalendar({ t, locale }) {
  const today = new Date()
  const year = today.getFullYear(), month = today.getMonth()
  const monthName = today.toLocaleDateString(locale, { month: 'long', year: 'numeric' })
  const firstDay = (new Date(year, month, 1).getDay() + 6) % 7
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  const cells = [
    ...Array.from({ length: firstDay }, () => null),
    ...Array.from({ length: daysInMonth }, (_, i) => i + 1),
  ]
  return (
    <div>
      <p style={{ fontSize: '13px', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'capitalize', marginBottom: '12px' }}>{monthName}</p>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: '2px', textAlign: 'center' }}>
        {t.dayLabels.map((d, i) => (
          <div key={i} style={{ fontSize: '11px', color: 'var(--text-muted)', padding: '4px 0' }}>{d}</div>
        ))}
        {cells.map((d, i) => (
          <div key={i} style={{
            fontSize: '11px', padding: '5px 0', borderRadius: '50%', lineHeight: 1,
            background: d === today.getDate() ? '#c42033' : 'transparent',
            color: d === today.getDate() ? '#fff' : d ? 'var(--text-secondary)' : 'transparent',
            fontWeight: d === today.getDate() ? 700 : 400,
          }}>{d ?? ''}</div>
        ))}
      </div>
    </div>
  )
}

const scoreColor = s =>
  s >= 75 ? { background: '#e7f4e0', color: '#2d7600' } :
  s >= 50 ? { background: '#fff4e0', color: '#9a5700' } :
             { background: '#fff0f2', color: '#c42033' }

const RECENT_CANDIDATES = [
  { name: 'Aisha Mbarga',      role: 'Développeur React',  score: 78 },
  { name: 'Jean-Paul Fofana',  role: 'Full Stack Engineer', score: 35 },
  { name: 'Christelle Ngo',    role: 'Product Designer',    score: 60 },
  { name: 'Bruno Kamdem',      role: 'Chef de projet',      score: 90 },
  { name: 'Sandra Ewome',      role: 'QA Engineer',         score: 90 },
  { name: 'Rodrigue Talla',    role: 'Data Analyst',        score: 55 },
]

// Fallback mock mapped to the same {initials, primary, secondary, score} shape used for real ScoredCandidateDTO results.
const MOCK_CANDIDATE_CARDS = RECENT_CANDIDATES.map(c => ({
  initials: c.name.split(' ').map(n => n[0]).join('').slice(0, 2),
  primary: c.name,
  secondary: c.role,
  score: c.score,
}))

const QUICK_ICONS = {
  '/employer/jobs/new':   { icon: Plus,       cls: 'ed-quick-icon--cherry' },
  '/employer/candidates': { icon: Users,      cls: 'ed-quick-icon--blue' },
  '/employer/jobs':       { icon: Briefcase,  cls: 'ed-quick-icon--green' },
  '/employer/company':    { icon: CircleUser, cls: 'ed-quick-icon--violet' },
}

export default function EmployerDashboard() {
  const navigate = useNavigate()
  const { user } = useAuthStore()
  const { language } = usePreferencesStore()
  const t = useTranslation().employer.dashboard
  const firstName = user?.firstName || t.defaultName
  const locale = language === 'fr' ? 'fr-FR' : 'en-US'

  const companyName = user?.companyName || user?.company || null
  const jobs = useMemo(() =>
    companyName
      ? JOBS.filter(j => j.company.toLowerCase().includes(companyName.toLowerCase()))
      : JOBS.slice(0, 4),
    [companyName]
  )

  const hiringStates = useMemo(() =>
    jobs.map(job => {
      const app = job.applicants ?? 10
      return {
        ...job,
        applied:   app,
        interview: Math.max(1, Math.floor(app * 0.38)),
        offer:     Math.max(0, Math.floor(app * 0.14)),
        hired:     Math.max(0, Math.floor(app * 0.06)),
      }
    }),
    [jobs]
  )

  const totalApplied = hiringStates.reduce((a, j) => a + j.applied, 0)
  const totalHired   = hiringStates.reduce((a, j) => a + j.hired, 0)
  const hiringRatio  = totalApplied ? Math.round((totalHired / totalApplied) * 100) : 0

  const ACQUISITIONS = t.acquisitionLabels.map((label, i) => ({ label, value: [84, 64, 54, 37][i] }))

  const [candidateCards, setCandidateCards] = useState(MOCK_CANDIDATE_CARDS)

  useEffect(() => {
    if (!user?.id) return
    getCompanyJobs(user.id)
      .then(companyJobs => {
        if (!companyJobs?.length) return null
        const job = companyJobs.find(j => j.status === 'OPEN') || companyJobs[0]
        return getSuggestedCandidates(job.id)
      })
      .then(candidates => {
        if (!candidates?.length) return
        setCandidateCards(candidates.slice(0, 6).map(c => {
          // ScoredCandidateDTO has no person name — show desiredRole/jobDomain instead of a fabricated name.
          const primary = c.desiredRole || c.jobDomain || 'Candidat'
          const secondary = c.desiredRole && c.jobDomain ? c.jobDomain : (c.location || '')
          return {
            initials: primary.slice(0, 2).toUpperCase(),
            primary,
            secondary,
            score: Math.round(c.score),
          }
        }))
      })
      .catch(() => {})
  }, [user?.id])

  return (
    <div className="ed-shell">
      {/* Blobs de fond rosés */}
      <div className="ed-blob ed-blob--main" />
      <div className="ed-blob ed-blob--accent" />

      <AppNavbar />

      {/* ── Contenu principal ── */}
      <main className="ed-main">
        <div className="ed-content">

          {/* Accueil */}
          <h1 className="ed-welcome">
            {t.welcomePrefix} <span className="ed-welcome-name">{firstName}</span>
          </h1>
          <p className="ed-welcome-sub">{t.welcomeSub}</p>

          {/* Bannière CTA */}
          <div className="ed-cta-banner">
            <div>
              <h2>{t.ctaTitle}</h2>
              <p>{t.ctaSub}</p>
            </div>
            <button className="ed-cta-btn" onClick={() => navigate('/employer/jobs/new')}>
              <Plus size={16} /> {t.ctaButton}
            </button>
          </div>

          {/* Bannière croisée — bascule vers le parcours candidat */}
          <div className="ed-switch-banner">
            <div>
              <h2>{t.switchRoleTitle}</h2>
              <p>{t.switchRoleSub}</p>
            </div>
            <button className="ed-switch-btn" onClick={() => navigate('/register?role=CANDIDATE')}>
              <CircleUser size={16} /> {t.switchRoleButton}
            </button>
          </div>

          {/* Statistiques */}
          <div className="ed-section">
            <p className="ed-section-title">{t.globalStats}</p>
            <div className="ed-stats-grid">
              {[
                { label: t.stats.activeJobs,   value: String(jobs.length).padStart(2, '0'),     icon: <Briefcase size={18} />,   color: '#c42033' },
                { label: t.stats.applications,  value: String(totalApplied).padStart(2, '0'),    icon: <Users size={18} />,       color: '#2b4fbf' },
                { label: t.stats.hired,         value: String(totalHired).padStart(2, '0'),      icon: <CheckCircle size={18} />, color: '#1a6e44' },
                { label: t.stats.hireRate,      value: `${hiringRatio}%`,                        icon: <BarChart2 size={18} />,   color: '#6629a6' },
              ].map(({ label, value, icon, color }) => (
                <div key={label} className="ed-stat-card">
                  <div style={{ color, marginBottom: '8px' }}>{icon}</div>
                  <p className="ed-stat-value" style={{ color }}>{value}</p>
                  <p className="ed-stat-label">{label}</p>
                </div>
              ))}
            </div>
          </div>

          {/* Accès rapides */}
          <div className="ed-section">
            <p className="ed-section-title">{t.quickLinksTitle}</p>
            <div className="ed-quick-grid">
              {t.quickLinks.map(({ label, desc, path }) => {
                const { icon: Icon, cls } = QUICK_ICONS[path]
                return (
                  <button key={path} className="ed-quick-card" onClick={() => navigate(path)}>
                    <div className={`ed-quick-icon ${cls}`}>
                      <Icon size={20} />
                    </div>
                    <div>
                      <p className="ed-quick-label">{label}</p>
                      <p className="ed-quick-desc">{desc}</p>
                    </div>
                    <ChevronRight size={16} className="ed-quick-arrow" />
                  </button>
                )
              })}
            </div>
          </div>

          {/* États du recrutement + Candidats récents */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr auto', gap: '20px', marginBottom: '32px', alignItems: 'start' }}>

            {/* Table de recrutement */}
            <div style={{ background: 'var(--surface-card)', border: '1.5px solid var(--surface-border)', borderRadius: '14px', overflow: 'hidden', boxShadow: '0 2px 8px rgba(0,0,0,0.05)' }}>
              <div style={{ padding: '16px 20px', borderBottom: '1px solid var(--surface-border-soft)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <strong style={{ fontSize: '14px', color: 'var(--text-primary)' }}>{t.hiringStatus}</strong>
                <button style={{ fontSize: '12px', color: '#c42033', background: 'none', border: 'none', cursor: 'pointer', fontWeight: 600 }} onClick={() => navigate('/employer/jobs')}>
                  {t.seeAll}
                </button>
              </div>
              <div style={{ overflowX: 'auto' }}>
                <table style={{ width: '100%', fontSize: '13px', borderCollapse: 'collapse' }}>
                  <thead>
                    <tr style={{ borderBottom: '1px solid var(--surface-border-soft)' }}>
                      {t.tableHeaders.map((h, i) => (
                        <th key={h + i} style={{ padding: '10px 16px', fontSize: '11px', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.5px', textAlign: i === 0 ? 'left' : 'center' }}>{h}</th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {hiringStates.length === 0 ? (
                      <tr>
                        <td colSpan={5} style={{ padding: '32px', textAlign: 'center', color: 'var(--text-muted)', fontSize: '13px' }}>
                          {t.noJobs}{' '}
                          <button style={{ color: '#c42033', background: 'none', border: 'none', cursor: 'pointer' }} onClick={() => navigate('/employer/jobs/new')}>
                            {t.publish}
                          </button>
                        </td>
                      </tr>
                    ) : hiringStates.map(job => (
                      <tr key={job.id} style={{ borderBottom: '1px solid var(--surface-border-soft)' }}>
                        <td style={{ padding: '12px 16px' }}>
                          <p style={{ fontWeight: 600, color: 'var(--text-primary)', marginBottom: '2px' }}>{job.title}</p>
                          <p style={{ fontSize: '11px', color: 'var(--text-muted)' }}>{job.type} · {job.experience}</p>
                        </td>
                        {[job.applied, job.interview, job.offer, job.hired].map((val, idx) => (
                          <td key={idx} style={{ padding: '12px 16px', textAlign: 'center' }}>
                            {val > 0 ? (
                              <button
                                style={{ fontWeight: 700, fontSize: '13px', color: idx === 3 ? '#c42033' : 'var(--text-primary)', background: 'none', border: 'none', cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: '2px' }}
                                onClick={() => navigate('/employer/candidates')}
                              >
                                {val} <ChevronRight size={11} style={{ color: 'var(--text-muted)' }} />
                              </button>
                            ) : <span style={{ color: 'var(--surface-border)', fontSize: '11px' }}>—</span>}
                          </td>
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Candidats récents */}
            <div style={{ width: '260px', background: 'var(--surface-card)', border: '1.5px solid var(--surface-border)', borderRadius: '14px', overflow: 'hidden', boxShadow: '0 2px 8px rgba(0,0,0,0.05)' }}>
              <div style={{ padding: '14px 18px', borderBottom: '1px solid var(--surface-border-soft)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <strong style={{ fontSize: '14px', color: 'var(--text-primary)' }}>{t.recentCandidates}</strong>
                <button style={{ fontSize: '12px', color: '#c42033', background: 'none', border: 'none', cursor: 'pointer', fontWeight: 600 }} onClick={() => navigate('/employer/candidates')}>
                  {t.see}
                </button>
              </div>
              {candidateCards.map((c, i) => (
                <div
                  key={i}
                  onClick={() => navigate('/employer/candidates')}
                  style={{ padding: '10px 18px', display: 'flex', alignItems: 'center', gap: '10px', borderBottom: '1px solid var(--surface-border-soft)', cursor: 'pointer' }}
                >
                  <div style={{ width: '32px', height: '32px', borderRadius: '50%', background: 'var(--surface-page-alt)', color: '#c42033', fontSize: '11px', fontWeight: 700, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                    {c.initials}
                  </div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <p style={{ fontSize: '13px', fontWeight: 600, color: 'var(--text-primary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{c.primary}</p>
                    <p style={{ fontSize: '11px', color: 'var(--text-muted)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{c.secondary}</p>
                  </div>
                  <span style={{ fontSize: '11px', fontWeight: 700, padding: '2px 7px', borderRadius: '10px', flexShrink: 0, ...scoreColor(c.score) }}>
                    {c.score}%
                  </span>
                </div>
              ))}
              <div style={{ padding: '12px 18px', borderTop: '1px solid var(--surface-border-soft)', textAlign: 'center' }}>
                <button style={{ fontSize: '12px', color: '#c42033', background: 'none', border: 'none', cursor: 'pointer', fontWeight: 600 }} onClick={() => navigate('/employer/candidates')}>
                  {t.seeAllApplications}
                </button>
              </div>
            </div>
          </div>

          {/* Acquisitions + Taux d'embauche + Planning */}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '20px' }}>

            {/* Acquisitions */}
            <div style={{ background: 'var(--surface-card)', border: '1.5px solid var(--surface-border)', borderRadius: '14px', padding: '24px', boxShadow: '0 2px 8px rgba(0,0,0,0.05)' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px' }}>
                <strong style={{ fontSize: '14px', color: 'var(--text-primary)' }}>{t.acquisitions}</strong>
                <BarChart2 size={15} style={{ color: 'var(--text-muted)' }} />
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                {ACQUISITIONS.map((a, i) => (
                  <div key={a.label}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '6px' }}>
                      <span style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>{a.label}</span>
                      <span style={{ fontSize: '12px', fontWeight: 700, color: 'var(--text-primary)' }}>{a.value}%</span>
                    </div>
                    <div style={{ height: '6px', background: 'var(--surface-border-soft)', borderRadius: '3px', overflow: 'hidden' }}>
                      <div style={{ height: '100%', borderRadius: '3px', background: '#c42033', width: `${a.value}%`, opacity: 1 - i * 0.18 }} />
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Taux d'embauche */}
            <div style={{ background: 'var(--surface-card)', border: '1.5px solid var(--surface-border)', borderRadius: '14px', padding: '24px', boxShadow: '0 2px 8px rgba(0,0,0,0.05)', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '16px' }}>
              <strong style={{ fontSize: '14px', color: 'var(--text-primary)', alignSelf: 'flex-start' }}>{t.hireRateTitle}</strong>
              <DonutChart pct={hiringRatio} />
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px', width: '100%' }}>
                <div style={{ background: 'var(--surface-page)', borderRadius: '10px', padding: '12px', textAlign: 'center' }}>
                  <p style={{ fontSize: '20px', fontWeight: 800, color: 'var(--text-primary)' }}>{totalApplied}</p>
                  <p style={{ fontSize: '11px', color: 'var(--text-muted)' }}>{t.stats.applications}</p>
                </div>
                <div style={{ background: 'var(--surface-page-alt)', borderRadius: '10px', padding: '12px', textAlign: 'center' }}>
                  <p style={{ fontSize: '20px', fontWeight: 800, color: '#c42033' }}>{totalHired}</p>
                  <p style={{ fontSize: '11px', color: 'var(--text-muted)' }}>{t.stats.hired}</p>
                </div>
              </div>
            </div>

            {/* Planning */}
            <div style={{ background: 'var(--surface-card)', border: '1.5px solid var(--surface-border)', borderRadius: '14px', padding: '24px', boxShadow: '0 2px 8px rgba(0,0,0,0.05)' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
                <strong style={{ fontSize: '14px', color: 'var(--text-primary)' }}>{t.planning}</strong>
                <Calendar size={15} style={{ color: 'var(--text-muted)' }} />
              </div>
              <MiniCalendar t={t} locale={locale} />
              <div style={{ marginTop: '16px', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', padding: '10px 12px', background: 'var(--surface-page-alt)', borderRadius: '10px' }}>
                  <div style={{ width: '6px', height: '6px', borderRadius: '50%', background: '#c42033', flexShrink: 0 }} />
                  <div>
                    <p style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-primary)' }}>{t.hrInterview}</p>
                    <p style={{ fontSize: '11px', color: 'var(--text-muted)' }}>{t.today} · 09:30</p>
                  </div>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', padding: '10px 12px', background: 'var(--surface-page)', borderRadius: '10px' }}>
                  <div style={{ width: '6px', height: '6px', borderRadius: '50%', background: 'var(--text-muted)', flexShrink: 0 }} />
                  <div>
                    <p style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-primary)' }}>{t.reviewApplications}</p>
                    <p style={{ fontSize: '11px', color: 'var(--text-muted)' }}>{t.tomorrow} · 14:00</p>
                  </div>
                </div>
              </div>
            </div>

          </div>

          {/* Règles d'automatisation */}
          <div className="ed-section">
            <AutomationRulesPanel />
          </div>

          {/* Viviers de talents */}
          <div className="ed-section">
            <p className="ed-section-title">{t.talentPoolsTitle}</p>
            <TalentPoolsPanel />
          </div>

        </div>
      </main>
    </div>
  )
}
