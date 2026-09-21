import axiosInstance from './AxiosInstance'

// Google Calendar Integration
export const getGoogleAuthUrl = () =>
  axiosInstance.get('/integrations/google/auth').then(r => r.data)

export const getGoogleStatus = () =>
  axiosInstance.get('/integrations/google/status').then(r => r.data)

export const disconnectGoogle = () =>
  axiosInstance.delete('/integrations/google').then(r => r.data)

export const confirmGoogleAuth = (code) =>
  axiosInstance.post('/integrations/google/callback', { code }).then(r => r.data)

// Job Board Integration — Multi-Regional Publishing
export const publishToJobBoards = (jobId, targetCountries) =>
  axiosInstance.post(`/jobboards/publish/${jobId}`, { targetCountries }).then(r => r.data)

export const unpublishFromJobBoards = (jobId) =>
  axiosInstance.delete(`/jobboards/unpublish/${jobId}`).then(r => r.data)

// Legacy France Travail Integration (backward compatibility)
export const publishToFranceTravail = (jobId) =>
  publishToJobBoards(jobId, ['FR'])

export const unpublishFromFranceTravail = (jobId) =>
  unpublishFromJobBoards(jobId)

export const getFranceTravailStatus = (jobId) =>
  axiosInstance.get(`/jobboards/france-travail/status/${jobId}`).then(r => r.data)

export const testFranceTravailConnection = (clientId, clientSecret) =>
  axiosInstance.post('/jobboards/france-travail/test', { clientId, clientSecret }).then(r => r.data)
