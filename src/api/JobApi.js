import axiosInstance from './AxiosInstance'
import { searchJobs, JOBS, SAVED_JOBS } from '../data/mockData'

// JobListingDTO (backend) ne correspond pas 1:1 au format attendu par JobCard/JobSearch
// (salaire à plat au lieu d'un objet, compétences en chaîne CSV, etc.) — on normalise ici
// pour que tous les composants candidats reçoivent la même forme que les données mock.
const normalizeJob = (dto) => ({
  ...dto,
  company: dto.company || dto.companyId,
  logo: dto.logo || '🏢',
  type: dto.jobType,
  salary: {
    min: Number(dto.salaryMin) || 0,
    max: Number(dto.salaryMax) || 0,
    currency: dto.currency || 'FCFA',
  },
  skills: dto.skills ?? (dto.requiredSkills ? dto.requiredSkills.split(',').map(s => s.trim()) : []),
  remote: !!dto.remote,
  postedDaysAgo: dto.postedDaysAgo ?? 0,
  applicants: dto.applicants ?? 0,
  featured: !!dto.featured,
  matchPct: dto.matchScore != null ? Math.round(dto.matchScore) : null,
})

export const getJobs = (params = {}) =>
  axiosInstance.get('/jobs', { params }).then(r => r.data.map(normalizeJob)).catch(() => searchJobs(params.q, params.location, params))

export const getJobById = (id) =>
  axiosInstance.get(`/jobs/${id}`).then(r => normalizeJob(r.data)).catch(() => JOBS.find(j => j.id === id) ?? null)

export const searchJobsMock = (query, location, filters) =>
  Promise.resolve(searchJobs(query, location, filters))

export const getSavedJobs = (userId) =>
  axiosInstance.get(`/jobs/saved/${userId}`).then(r => r.data).catch(() => SAVED_JOBS)

export const saveJob = (userId, jobId) =>
  axiosInstance.post('/jobs/save', { userId, jobId }).then(r => r.data).catch(() => ({ saved: true }))

export const unsaveJob = (userId, jobId) =>
  axiosInstance.delete(`/jobs/save/${userId}/${jobId}`).then(r => r.data).catch(() => ({ saved: false }))

export const applyToJob = (jobId, applicationData) =>
  axiosInstance.post(`/jobs/${jobId}/apply`, applicationData).then(r => r.data)

export const getApplications = (userId) =>
  axiosInstance.get(`/applications?userId=${userId}`).then(r => r.data)

export const getInterviews = (userId) =>
  axiosInstance.get(`/interviews?userId=${userId}`).then(r => r.data)

export const getSuggestedJobs = () =>
  axiosInstance.get('/jobs/suggested').then(r => r.data)

export const getSuggestedCandidates = (jobId) =>
  axiosInstance.get(`/jobs/${jobId}/suggested-candidates`).then(r => r.data)

export const getCompanyJobs = (companyId) =>
  axiosInstance.get(`/jobs/company/${companyId}`).then(r => r.data)
