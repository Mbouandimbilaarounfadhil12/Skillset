import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { CheckCircle, XCircle, Clock, Search, ChevronDown, Briefcase } from 'lucide-react'
import { getAdminJobs, changeJobStatus } from '../../api/AdminApi'
import { useTranslation } from '../../i18n/translations'
import './ModerationPanel.css'

const STATUS_OPTIONS = ['', 'OPEN', 'CLOSED', 'DRAFT']

export default function ModerationPanel() {
  const t = useTranslation().admin.moderation
  const [jobs, setJobs] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [filterStatus, setFilterStatus] = useState('')
  const [search, setSearch] = useState('')
  const [changing, setChanging] = useState({})

  const load = (status = filterStatus) => {
    setLoading(true)
    setError('')
    getAdminJobs(status)
      .then(setJobs)
      .catch(() => setError(t.loadError))
      .finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [filterStatus]) // eslint-disable-line

  const handleStatus = async (jobId, newStatus) => {
    setChanging(c => ({ ...c, [jobId]: true }))
    try {
      const updated = await changeJobStatus(jobId, newStatus)
      setJobs(js => js.map(j => j.id === jobId ? updated : j))
    } catch {
      setError(t.statusChangeError)
    } finally {
      setChanging(c => ({ ...c, [jobId]: false }))
    }
  }

  const visible = jobs.filter(j =>
    !search || j.title.toLowerCase().includes(search.toLowerCase()) ||
    j.location?.toLowerCase().includes(search.toLowerCase())
  )

  const counts = { OPEN: 0, CLOSED: 0, DRAFT: 0 }
  jobs.forEach(j => { if (counts[j.status] !== undefined) counts[j.status]++ })

  return (
    <div className="mp-shell">
      <div className="mp-header">
        <h1 className="mp-title">{t.pageTitle}</h1>
        <p className="mp-sub">{t.pageSub}</p>
      </div>

      <div className="mp-kpis">
        <div className="mp-kpi mp-kpi--open">
          <CheckCircle size={20} />
          <div>
            <p className="mp-kpi-val">{counts.OPEN}</p>
            <p className="mp-kpi-label">{t.open}</p>
          </div>
        </div>
        <div className="mp-kpi mp-kpi--closed">
          <XCircle size={20} />
          <div>
            <p className="mp-kpi-val">{counts.CLOSED}</p>
            <p className="mp-kpi-label">{t.closed}</p>
          </div>
        </div>
        <div className="mp-kpi mp-kpi--draft">
          <Clock size={20} />
          <div>
            <p className="mp-kpi-val">{counts.DRAFT}</p>
            <p className="mp-kpi-label">{t.drafts}</p>
          </div>
        </div>
        <div className="mp-kpi mp-kpi--total">
          <Briefcase size={20} />
          <div>
            <p className="mp-kpi-val">{jobs.length}</p>
            <p className="mp-kpi-label">{t.total}</p>
          </div>
        </div>
      </div>

      <div className="mp-toolbar">
        <div className="mp-search-wrap">
          <Search size={15} className="mp-search-icon" />
          <input
            className="mp-search"
            placeholder={t.searchPlaceholder}
            value={search}
            onChange={e => setSearch(e.target.value)}
          />
        </div>
        <div className="mp-filter-wrap">
          <select
            className="mp-filter"
            value={filterStatus}
            onChange={e => setFilterStatus(e.target.value)}
          >
            {STATUS_OPTIONS.map(s => (
              <option key={s} value={s}>{s ? t.statusLabels[s] : t.allStatuses}</option>
            ))}
          </select>
          <ChevronDown size={14} className="mp-filter-arrow" />
        </div>
      </div>

      {error && <p className="mp-error">{error}</p>}

      {loading ? (
        <div className="mp-loading">{t.loading}</div>
      ) : visible.length === 0 ? (
        <div className="mp-empty">
          <Briefcase size={40} color="var(--text-muted)" />
          <p>{t.emptyResults}</p>
        </div>
      ) : (
        <div className="mp-table-wrap">
          <table className="mp-table">
            <thead>
              <tr>
                <th>{t.table.title}</th>
                <th>{t.table.location}</th>
                <th>{t.table.type}</th>
                <th>{t.table.applications}</th>
                <th>{t.table.postedOn}</th>
                <th>{t.table.expiresOn}</th>
                <th>{t.table.status}</th>
                <th>{t.table.actions}</th>
              </tr>
            </thead>
            <tbody>
              {visible.map(job => (
                <tr key={job.id}>
                  <td className="mp-td-title">
                    <Link to={`/jobs/${job.id}`} className="mp-job-link" target="_blank" rel="noopener noreferrer">
                      {job.title}
                    </Link>
                  </td>
                  <td>{job.location || '—'}</td>
                  <td>{job.jobType || '—'}</td>
                  <td className="mp-td-center">{job.applicationCount}</td>
                  <td>{job.postedAt}</td>
                  <td>{job.expiresAt || '—'}</td>
                  <td>
                    <span className={`mp-badge mp-badge--${job.status.toLowerCase()}`}>
                      {t.statusLabels[job.status] ?? job.status}
                    </span>
                  </td>
                  <td>
                    <div className="mp-actions">
                      {job.status !== 'OPEN' && (
                        <button
                          className="mp-action-btn mp-action-btn--open"
                          disabled={changing[job.id]}
                          onClick={() => handleStatus(job.id, 'OPEN')}
                        >
                          {t.openAction}
                        </button>
                      )}
                      {job.status !== 'CLOSED' && (
                        <button
                          className="mp-action-btn mp-action-btn--close"
                          disabled={changing[job.id]}
                          onClick={() => handleStatus(job.id, 'CLOSED')}
                        >
                          {t.closeAction}
                        </button>
                      )}
                      {job.status !== 'DRAFT' && (
                        <button
                          className="mp-action-btn mp-action-btn--draft"
                          disabled={changing[job.id]}
                          onClick={() => handleStatus(job.id, 'DRAFT')}
                        >
                          {t.draftAction}
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
