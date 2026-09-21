import { JOBS, SALARY_BENCHMARKS } from '../data/mockData'

// ─── Score a single job vs candidate profile ──────────────────────────────────
export function matchScore(job, profile) {
  if (!profile) return 0
  let score = 0

  const profileSkills = (profile.skills || []).map(s => s.toLowerCase())
  const jobSkills     = (job.skills || []).map(s => s.toLowerCase())

  // Skills (max 45 pts)
  const matched = jobSkills.filter(js => profileSkills.some(ps => ps.includes(js) || js.includes(ps)))
  score += Math.round((matched.length / Math.max(jobSkills.length, 1)) * 45)

  // Location (max 20 pts)
  const city = (profile.city || profile.location || '').toLowerCase()
  if (city && job.location.toLowerCase().includes(city)) score += 20
  else if (city && job.country.toLowerCase().includes((profile.country || '').toLowerCase())) score += 8

  // Remote preference (max 15 pts)
  if (profile.remotePreference === 'remote' && job.remote) score += 15
  else if (profile.remotePreference === 'hybrid' && job.remote) score += 8

  // Sector (max 10 pts)
  if (profile.sector && job.sector === profile.sector) score += 10

  // Salary expectation (max 10 pts)
  if (profile.salaryExpectation) {
    const exp = Number(profile.salaryExpectation)
    if (exp && job.salary.max >= exp) score += 10
    else if (exp && job.salary.max >= exp * 0.8) score += 5
  }

  return Math.min(score, 100)
}

// ─── Get ranked jobs for a candidate profile ─────────────────────────────────
export function getRecommendedJobs(profile, limit = 5, jobsPool = JOBS) {
  return jobsPool
    .map(job => ({ ...job, matchPct: matchScore(job, profile) }))
    .sort((a, b) => b.matchPct - a.matchPct)
    .slice(0, limit)
}

// ─── Estimate salary for a profile ───────────────────────────────────────────
export function estimateProfileSalary(profile) {
  const title  = (profile.jobTitle || profile.title || '').toLowerCase()
  const exp    = Number(profile.yearsOfExperience || 0)
  const key    = Object.keys(SALARY_BENCHMARKS).find(k => title.includes(k.toLowerCase()))
  const base   = key ? SALARY_BENCHMARKS[key] : { min: 150_000, median: 300_000, max: 600_000 }

  const mult = exp >= 7 ? 1.4 : exp >= 4 ? 1.2 : exp >= 2 ? 1.05 : 0.85

  return {
    min:    Math.round(base.min    * mult / 10_000) * 10_000,
    median: Math.round(base.median * mult / 10_000) * 10_000,
    max:    Math.round(base.max    * mult / 10_000) * 10_000,
    currency: 'FCFA',
    confidence: key ? 'élevé' : 'estimé',
  }
}

// ─── Profile completeness ────────────────────────────────────────────────────
export function profileCompleteness(profile) {
  const fields = ['firstName', 'lastName', 'email', 'city', 'jobTitle', 'skills', 'bio', 'phone']
  const filled = fields.filter(f => {
    const v = profile?.[f]
    return v && (Array.isArray(v) ? v.length > 0 : String(v).trim().length > 0)
  })
  return Math.round((filled.length / fields.length) * 100)
}
