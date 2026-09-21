import { useState, useMemo, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Search, MapPin, CircleUser,
  Clock, X, Briefcase, FileText, Sparkles,
  BookmarkCheck, CalendarCheck,
} from 'lucide-react'
import { useAuthStore }     from '../../store/AuthStore'
import { useUserDataStore } from '../../store/UserDataStore'
import { getRecommendedJobs, profileCompleteness } from '../../utils/matchingUtils'
import { getSuggestedJobs } from '../../api/JobApi'
import AppNavbar            from '../../components/common/AppNavbar'
import { useTranslation }   from '../../i18n/translations'
import './CandidateDashboard.css'

const QUICK_LINK_ICONS = {
  '/jobs': <Briefcase size={22} />,
  '/profile': <CircleUser size={22} />,
  '/applications': <FileText size={22} />,
}

// ScoredJobDTO has no company name/logo — substitute jobType/a neutral emoji for those display-only fields.
function mapScoredJobsToCardShape(jobs) {
  return jobs.map(job => ({
    id: job.id,
    title: job.title,
    company: job.jobType || '',
    logo: '🏢',
    location: job.location,
    matchPct: Math.round(job.score),
  }))
}

export default function CandidateDashboard() {
  const navigate = useNavigate()
  const { user }                                = useAuthStore()
  const { savedJobs, applications, interviews } = useUserDataStore()
  const t = useTranslation().candidate.dashboard
  const firstName = user?.firstName || t.defaultName

  const profile = useMemo(() => {
    try { return JSON.parse(localStorage.getItem('ss_profile') || 'null') ?? user } catch { return user }
  }, [user])

  const mockRecommendedJobs = useMemo(() => getRecommendedJobs(profile, 6), [profile])
  const [apiRecommendedJobs, setApiRecommendedJobs] = useState(null)
  const recommendedJobs = apiRecommendedJobs ?? mockRecommendedJobs
  const completeness = useMemo(() => profileCompleteness(profile), [profile])

  useEffect(() => {
    getSuggestedJobs()
      .then(jobs => { if (jobs?.length) setApiRecommendedJobs(mapScoredJobsToCardShape(jobs).slice(0, 6)) })
      .catch(() => {})
  }, [])

  const [query, setQuery]       = useState('')
  const [location, setLocation] = useState('')
  const [recentSearches, setRecentSearches] = useState(() => {
    try { return JSON.parse(localStorage.getItem(`ss_searches_${user?.id}`) || '[]') } catch { return [] }
  })

  const doSearch = (q = query, l = location) => {
    const trimQ = q.trim()
    const trimL = l.trim()
    if (!trimQ && !trimL) return
    const entry = { id: Date.now(), q: trimQ, l: trimL }
    const updated = [entry, ...recentSearches.filter(s => !(s.q === trimQ && s.l === trimL))].slice(0, 6)
    setRecentSearches(updated)
    try { localStorage.setItem(`ss_searches_${user?.id}`, JSON.stringify(updated)) } catch { /* ignore */ }
    navigate(`/jobs?q=${encodeURIComponent(trimQ)}&location=${encodeURIComponent(trimL)}`)
  }

  const removeSearch = (id) => {
    const updated = recentSearches.filter(s => s.id !== id)
    setRecentSearches(updated)
    try { localStorage.setItem(`ss_searches_${user?.id}`, JSON.stringify(updated)) } catch { /* ignore */ }
  }

  const countLabel = (count, { singular, plural }) => `${count} ${count !== 1 ? plural : singular}`

  return (
    <div className="cd-shell">
      <div className="cd-blob cd-blob--main" />
      <div className="cd-blob cd-blob--accent" />

      <AppNavbar />

      {/* ── Zone de recherche (dégradé rosé) ── */}
      <div className="cd-search-wrap">
        <div className="cd-search-bar">
          <div className="cd-search-field">
            <Search size={16} className="cd-search-icon" />
            <input
              className="cd-search-input"
              placeholder={t.searchQueryPlaceholder}
              value={query}
              onChange={e => setQuery(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && doSearch()}
            />
          </div>
          <div className="cd-search-sep" />
          <div className="cd-search-field">
            <MapPin size={16} className="cd-search-icon" />
            <input
              className="cd-search-input"
              placeholder={t.searchLocationPlaceholder}
              value={location}
              onChange={e => setLocation(e.target.value)}
              onKeyDown={e => e.key === 'Enter' && doSearch()}
            />
          </div>
          <button className="cd-search-btn" onClick={() => doSearch()}>
            {t.searchButton}
          </button>
        </div>
      </div>

      {/* ── Contenu principal ── */}
      <main className="cd-main">
        <div className="cd-content">

          <p className="cd-welcome">
            {t.welcomePrefix} <span className="cd-welcome-name">{firstName}</span> 👋
          </p>

          {/* Bannière croisée — bascule vers le parcours employeur */}
          <div className="cd-switch-banner">
            <div>
              <h2>{t.switchRoleTitle}</h2>
              <p>{t.switchRoleSub}</p>
            </div>
            <button className="cd-switch-btn" onClick={() => navigate('/register?role=EMPLOYER')}>
              <Briefcase size={16} /> {t.switchRoleButton}
            </button>
          </div>

          {/* Complétude du profil */}
          {completeness < 100 && (
            <div className="cd-profile-bar-wrap">
              <div className="cd-profile-bar-info">
                <span>{t.profileComplete.replace('{pct}', completeness)}</span>
              </div>
              <div className="cd-profile-bar-track">
                <div className="cd-profile-bar-fill" style={{ width: `${completeness}%` }} />
              </div>
              <button className="cd-profile-bar-btn" onClick={() => navigate('/profile')}>
                {t.completeProfileCta}
              </button>
            </div>
          )}

          {/* Stats */}
          <div className="cd-stats-row">
            <div className="cd-stat-chip">
              <Briefcase size={14} />
              {countLabel(applications.length, t.stats.applications)}
            </div>
            <div className="cd-stat-chip">
              <BookmarkCheck size={14} />
              {countLabel(savedJobs.length, t.stats.savedJobs)}
            </div>
            <div className="cd-stat-chip">
              <CalendarCheck size={14} />
              {countLabel(interviews.length, t.stats.interviews)}
            </div>
          </div>

          {/* Recommandations STELLA */}
          {recommendedJobs.length > 0 && (
            <div className="cd-section">
              <div className="cd-section-header">
                <p className="cd-section-title">
                  <Sparkles size={14} /> {t.recommendationsTitle}
                </p>
                <button className="cd-section-link" onClick={() => navigate('/jobs')}>
                  {t.seeAllOpportunities}
                </button>
              </div>
              <div className="cd-recommended-list">
                {recommendedJobs.map(job => (
                  <button
                    key={job.id}
                    className="cd-recommended-card"
                    onClick={() => navigate(`/jobs?q=${encodeURIComponent(job.title)}`)}
                  >
                    <span className="cd-rec-logo">{job.logo}</span>
                    <div className="cd-rec-info">
                      <p className="cd-rec-title">{job.title}</p>
                      <p className="cd-rec-company">{job.company} · {job.location}</p>
                    </div>
                    <span className="cd-rec-match">{job.matchPct}%</span>
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Recherches récentes */}
          <div className="cd-section">
            <p className="cd-section-title">
              <Clock size={14} /> {t.recentSearchesTitle}
            </p>
            {recentSearches.length === 0 ? (
              <p className="cd-empty-hint">{t.noRecentSearches}</p>
            ) : (
              <div className="cd-recent-list">
                {recentSearches.map(s => (
                  <div key={s.id} className="cd-recent-chip">
                    <button
                      className="cd-recent-chip-btn"
                      onClick={() => { setQuery(s.q); setLocation(s.l); doSearch(s.q, s.l) }}
                    >
                      {s.q || t.allOffers}
                      {s.l && <span className="cd-recent-loc">· {s.l}</span>}
                    </button>
                    <button
                      className="cd-recent-del"
                      onClick={() => removeSearch(s.id)}
                      aria-label={t.removeSearch}
                    >
                      <X size={11} />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Suggestions */}
          <div className="cd-section">
            <p className="cd-section-title">{t.suggestionsTitle}</p>
            <div className="cd-suggestions">
              {t.suggestions.map(s => (
                <button
                  key={s}
                  className="cd-suggestion"
                  onClick={() => { setQuery(s); doSearch(s, location) }}
                >
                  {s}
                </button>
              ))}
            </div>
          </div>

          {/* Accès rapides */}
          <div className="cd-section">
            <p className="cd-section-title">{t.quickLinksTitle}</p>
            <div className="cd-quick-links">
              {t.quickLinks.map(({ label, path }) => (
                <button key={path} className="cd-quick-card" onClick={() => navigate(path)}>
                  {QUICK_LINK_ICONS[path]}
                  {label}
                </button>
              ))}
            </div>
          </div>

        </div>
      </main>

    </div>
  )
}
