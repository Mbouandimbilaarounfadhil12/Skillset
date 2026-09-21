import axiosInstance from './AxiosInstance'

export const searchJobs = (criteria) =>
  axiosInstance.post('/search/jobs', criteria)

export const searchCandidates = (criteria) =>
  axiosInstance.post('/search/candidates', criteria)

export const getSuggestions = (query) =>
  axiosInstance.get('/search/suggestions', { params: { q: query } })
