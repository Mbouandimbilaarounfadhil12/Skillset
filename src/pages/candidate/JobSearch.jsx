import { useState, useEffect, useCallback, useRef } from 'react'
import { useSearchParams } from 'react-router-dom'
import {
  Search, MapPin, SlidersHorizontal, X, ChevronDown,
  Bookmark, BookmarkCheck, CheckCircle2, Send, Upload, FileText,
} from 'lucide-react'
import { useUserDataStore }           from '../../store/UserDataStore'
import { useAuthStore }               from '../../store/AuthStore'
import AppNavbar                      from '../../components/common/AppNavbar'
import JobCard                        from '../../features/job-board/JobCard'
import { getJobs }                    from '../../api/JobApi'
import { submitApplication, getCandidateApplications } from '../../api/ApplicationApi'
import { useTranslation }             from '../../i18n/translations'
import './JobSearch.css'

export default function JobSearch() {
  const [searchParams, setSearchParams] = useSearchParams()
  const t = useTranslation().candidate.jobSearch

  const { saveJob, unsaveJob, isJobSaved, applyToJob } = useUserDataStore()
  const { user } = useAuthStore()

  // Applied-job state comes from the real backend, not the local mock store,
  // so it stays correct across reloads/devices instead of only within this session.
  const [appliedJobIds, setAppliedJobIds] = useState(new Set())

  useEffect(() => {
    if (!user?.id) return
    getCandidateApplications(user.id)
      .then(apps => setAppliedJobIds(new Set(apps.map(a => a.jobListingId))))
      .catch(() => {})
  }, [user?.id])

  const hasApplied = (jobId) => appliedJobIds.has(jobId)

  const [query,       setQuery]       = useState(searchParams.get('q') || '')
  const [location,    setLocation]    = useState(searchParams.get('location') || '')
  const [filters,     setFilters]     = useState({ type: '', sector: '', salaryMin: 0, remote: false, sort: 'relevance' })
  const [showFilters, setShowFilters] = useState(false)
  const [results,     setResults]     = useState([])
  const [selected,    setSelected]    = useState(null)
  const [applyModal,  setApplyModal]  = useState(false)
  const [applied,     setApplied]     = useState(false)
  const [coverLetter, setCoverLetter] = useState('')
  const [cvFile,      setCvFile]      = useState(null)
  const [submitting,  setSubmitting]  = useState(false)
  const [submitError, setSubmitError] = useState(null)
  const fileInputRef = useRef(null)

  const doSearch = useCallback((q = query, loc = location, f = filters) => {
    getJobs({ q, location: loc, type: f.type, sector: f.sector, salaryMin: f.salaryMin, remote: f.remote, sort: f.sort })
      .then(setResults)
    setSearchParams({ q, location: loc }, { replace: true })
  }, [query, location, filters, setSearchParams])

  useEffect(() => {
    doSearch(searchParams.get('q') || '', searchParams.get('location') || '', filters)
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const handleFilter = (key, val) => {
    const next = { ...filters, [key]: val }
    setFilters(next)
    doSearch(query, location, next)
  }

  const clearFilters = () => {
    const reset = { type: '', sector: '', salaryMin: 0, remote: false, sort: 'relevance' }
    setFilters(reset)
    doSearch(query, location, reset)
  }

  const activeFilterCount = [filters.type, filters.sector, filters.remote, filters.salaryMin > 0].filter(Boolean).length

  // Save / unsave a job
  const handleSave = (job) => {
    if (isJobSaved(job.id)) {
      unsaveJob(job.id)
    } else {
      saveJob(job)
    }
  }

  // Open apply modal
  const handleApply = () => {
    if (!selected) return
    if (hasApplied(selected.id)) return
    setApplied(false)
    setCoverLetter('')
    setCvFile(null)
    setSubmitError(null)
    setApplyModal(true)
  }

  // Postuler directement depuis la carte, sans passer par le panneau de détail
  const handleQuickApply = (job) => {
    if (hasApplied(job.id)) return
    setSelected(job)
    setApplied(false)
    setCoverLetter('')
    setCvFile(null)
    setSubmitError(null)
    setApplyModal(true)
  }

  // Confirm application — appel API réel
  const confirmApply = async () => {
    if (!selected || submitting) return
    setSubmitting(true)
    setSubmitError(null)
    try {
      await submitApplication(selected.id, coverLetter, cvFile)
      setAppliedJobIds(prev => new Set(prev).add(selected.id))
      applyToJob({
        jobId:    selected.id,
        jobTitle: selected.title,
        company:  selected.company,
        logo:     selected.logo,
        location: selected.location,
        type:     selected.type,
        salary:   `${(selected.salary.min / 1000).toFixed(0)}k – ${(selected.salary.max / 1000).toFixed(0)}k ${selected.salary.currency}`,
      })
      setApplied(true)
      setTimeout(() => setApplyModal(false), 2500)
    } catch (err) {
      setSubmitError(err?.response?.data?.message || t.applyFailed)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="js-shell">
      <div className="js-blob js-blob--main" />
      <div className="js-blob js-blob--accent" />

      <AppNavbar />

      {/* ── Search bar ── */}
      <div className="js-search-wrap">
        <div className="js-search-bar">
          <div className="js-search-field">
            <Search size={17} className="js-search-icon" />
            <input
              className="js-search-input"
              placeholder={t.searchPlaceholder}
              value={query}
              onChange={e => setQuery(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && doSearch()}
            />
            {query && (
              <button className="js-clear-btn" onClick={() => { setQuery(''); doSearch('', location) }}>
                <X size={14} />
              </button>
            )}
          </div>
          <div className="js-search-sep" />
          <div className="js-search-field">
            <MapPin size={17} className="js-search-icon" />
            <input
              className="js-search-input"
              placeholder={t.locationPlaceholder}
              value={location}
              onChange={e => setLocation(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && doSearch()}
            />
          </div>
          <button className="js-search-btn" onClick={() => doSearch()}>{t.searchButton}</button>
        </div>
      </div>

      {/* ── Body ── */}
      <div className="js-body">

        {/* ── Sidebar filters ── */}
        <aside className={`js-filters ${showFilters ? 'js-filters--open' : ''}`}>
          <div className="js-filters-header">
            <span className="js-filters-title">
              {t.filters} {activeFilterCount > 0 && <span className="js-filter-count">{activeFilterCount}</span>}
            </span>
            {activeFilterCount > 0 && (
              <button className="js-clear-all" onClick={clearFilters}>{t.clearAll}</button>
            )}
          </div>

          <div className="js-filter-group">
            <p className="js-filter-label">{t.sortBy}</p>
            <select className="js-select" value={filters.sort} onChange={e => handleFilter('sort', e.target.value)}>
              <option value="relevance">{t.sortRelevance}</option>
              <option value="date">{t.sortDate}</option>
              <option value="salary">{t.sortSalary}</option>
            </select>
          </div>

          <div className="js-filter-group">
            <p className="js-filter-label">{t.contractType}</p>
            <div className="js-filter-chips">
              {t.contractTypes.map(ct => (
                <button key={ct} className={`js-chip ${filters.type === ct ? 'js-chip--active' : ''}`}
                  onClick={() => handleFilter('type', filters.type === ct ? '' : ct)}>
                  {ct}
                </button>
              ))}
            </div>
          </div>

          <div className="js-filter-group">
            <p className="js-filter-label">{t.sector}</p>
            <div className="js-filter-chips">
              {t.sectors.map(s => (
                <button key={s} className={`js-chip ${filters.sector === s ? 'js-chip--active' : ''}`}
                  onClick={() => handleFilter('sector', filters.sector === s ? '' : s)}>
                  {s}
                </button>
              ))}
            </div>
          </div>

          <div className="js-filter-group">
            <p className="js-filter-label">{t.minSalary}</p>
            {t.salaryRanges.map(r => (
              <label key={r.value} className="js-radio">
                <input type="radio" name="salaryMin" checked={filters.salaryMin === r.value}
                  onChange={() => handleFilter('salaryMin', r.value)} />
                {r.label}
              </label>
            ))}
          </div>

          <div className="js-filter-group">
            <label className="js-toggle">
              <input type="checkbox" checked={filters.remote}
                onChange={e => handleFilter('remote', e.target.checked)} />
              <span className="js-toggle-track" />
              {t.remoteOnly}
            </label>
          </div>
        </aside>

        {/* ── Results + detail ── */}
        <div className="js-results-wrap">
          <div className="js-results-header">
            <p className="js-results-count">
              <strong>{results.length}</strong> {results.length !== 1 ? t.resultsFound.plural : t.resultsFound.singular}
              {query && <> {t.resultsFor.replace('{q}', query)}</>}
            </p>
            <button className="js-filter-toggle-btn" onClick={() => setShowFilters(v => !v)}>
              <SlidersHorizontal size={15} />
              {t.filters}
              {activeFilterCount > 0 && <span className="js-filter-count">{activeFilterCount}</span>}
              <ChevronDown size={14} className={showFilters ? 'js-chevron-up' : ''} />
            </button>
          </div>

          <div className="js-layout">
            {/* List */}
            <div className="js-list">
              {results.length === 0 ? (
                <div className="js-empty">
                  <p className="js-empty-title">{t.noResultsTitle}</p>
                  <p className="js-empty-hint">{t.noResultsHint}</p>
                  <button className="js-empty-reset" onClick={clearFilters}>{t.resetFilters}</button>
                </div>
              ) : (
                results.map(job => (
                  <JobCard
                    key={job.id}
                    job={{ ...job, saved: isJobSaved(job.id) }}
                    onClick={setSelected}
                    onSave={() => handleSave(job)}
                    onApply={handleQuickApply}
                    applied={hasApplied(job.id)}
                  />
                ))
              )}
            </div>

            {/* Detail panel */}
            {selected && (
              <div className="js-detail">
                <button className="js-detail-close" onClick={() => setSelected(null)} aria-label={t.close}>
                  <X size={18} />
                </button>

                <div className="js-detail-head">
                  <span className="js-detail-logo">{selected.logo}</span>
                  <div>
                    <h2 className="js-detail-title">{selected.title}</h2>
                    <p className="js-detail-company">{selected.company} · {selected.location}</p>
                  </div>
                </div>

                <div className="js-detail-badges">
                  <span className="jc-badge">{selected.type}</span>
                  {selected.remote && <span className="jc-badge jc-badge--remote">{t.remote}</span>}
                  {selected.experience && <span className="jc-badge">{selected.experience}</span>}
                  {selected.education  && <span className="jc-badge">{selected.education}</span>}
                </div>

                <p className="js-detail-salary">
                  💰 {(selected.salary.min / 1000).toFixed(0)}k – {(selected.salary.max / 1000).toFixed(0)}k {selected.salary.currency}
                </p>

                <p className="js-detail-desc">{selected.description}</p>

                {selected.skills?.length > 0 && (
                  <div className="js-detail-skills">
                    <p className="js-detail-sub">{t.requiredSkills}</p>
                    <div className="js-detail-skill-list">
                      {selected.skills.map(s => <span key={s} className="jc-skill">{s}</span>)}
                    </div>
                  </div>
                )}

                <div className="js-detail-actions">
                  {hasApplied(selected.id) ? (
                    <div className="js-applied-state">
                      <CheckCircle2 size={18} /> {t.applied}
                    </div>
                  ) : (
                    <button className="js-apply-btn" onClick={handleApply}>
                      <Send size={16} /> {t.applyNow}
                    </button>
                  )}
                  <button
                    className={`js-save-btn ${isJobSaved(selected.id) ? 'js-save-btn--active' : ''}`}
                    onClick={() => handleSave(selected)}
                  >
                    {isJobSaved(selected.id) ? <BookmarkCheck size={16} /> : <Bookmark size={16} />}
                    {isJobSaved(selected.id) ? t.saved : t.save}
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* ── Apply Modal ── */}
      {applyModal && selected && (
        <div className="js-modal-overlay" onClick={e => { if (e.target === e.currentTarget && !applied) setApplyModal(false) }}>
          <div className="js-modal">
            {applied ? (
              <div className="js-modal-success">
                <CheckCircle2 size={48} className="js-modal-success-icon" />
                <h3>{t.appliedModalTitle}</h3>
                <p>{t.appliedModalBody} <strong>{selected.company}</strong>.</p>
                <p>{t.appliedModalTrack} <strong>{t.myJobs}</strong>.</p>
              </div>
            ) : (
              <>
                <div className="js-modal-head">
                  <span className="js-modal-logo">{selected.logo}</span>
                  <div>
                    <h3 className="js-modal-title">{selected.title}</h3>
                    <p className="js-modal-company">{selected.company} · {selected.location}</p>
                  </div>
                  <button className="js-modal-close" onClick={() => setApplyModal(false)}><X size={18} /></button>
                </div>

                {/* Lettre de motivation */}
                <div className="js-modal-field">
                  <label className="js-modal-label">{t.coverLetter}</label>
                  <textarea
                    className="js-modal-textarea"
                    rows={4}
                    placeholder={t.coverLetterPlaceholder}
                    value={coverLetter}
                    onChange={e => setCoverLetter(e.target.value)}
                  />
                </div>

                {/* Upload CV */}
                <div className="js-modal-field">
                  <label className="js-modal-label">{t.cvLabel}</label>
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept=".pdf,.doc,.docx"
                    style={{ display: 'none' }}
                    onChange={e => setCvFile(e.target.files[0] || null)}
                  />
                  <button
                    className="js-upload-btn"
                    onClick={() => fileInputRef.current?.click()}
                  >
                    {cvFile
                      ? <><FileText size={15} /> {cvFile.name}</>
                      : <><Upload size={15} /> {t.chooseFile}</>}
                  </button>
                  {cvFile && (
                    <button className="js-remove-file" onClick={() => setCvFile(null)}>
                      <X size={13} /> {t.removeFile}
                    </button>
                  )}
                </div>

                {submitError && <p className="js-modal-error">{submitError}</p>}

                <div className="js-modal-actions">
                  <button
                    className="js-apply-btn"
                    onClick={confirmApply}
                    disabled={submitting}
                  >
                    {submitting
                      ? t.sending
                      : <><Send size={15} /> {t.sendApplication}</>}
                  </button>
                  <button className="js-modal-cancel" onClick={() => setApplyModal(false)}>{t.cancel}</button>
                </div>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
