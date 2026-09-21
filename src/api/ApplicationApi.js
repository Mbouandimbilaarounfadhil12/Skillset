import axiosInstance from './AxiosInstance'

/**
 * Soumet une candidature avec CV (PDF) et lettre de motivation.
 * Le backend attend un multipart/form-data avec les champs :
 *   - jobListingId (texte, obligatoire)
 *   - coverLetter  (texte, optionnel)
 *   - cv           (fichier PDF, optionnel)
 */
export const submitApplication = (jobListingId, coverLetter, cvFile) => {
  const form = new FormData()
  form.append('jobListingId', jobListingId)
  if (coverLetter) form.append('coverLetter', coverLetter)
  if (cvFile)      form.append('cv', cvFile)
  return axiosInstance
    .post('/applications', form, { headers: { 'Content-Type': 'multipart/form-data' } })
    .then(r => r.data)
}

export const getEmployerApplications = () =>
  axiosInstance.get('/applications/employer').then(r => r.data)

export const getCandidateApplications = (jobSeekerId) =>
  axiosInstance.get(`/applications/candidate/${jobSeekerId}`).then(r => r.data)

export const getJobApplications = (jobListingId) =>
  axiosInstance.get(`/applications/job/${jobListingId}`).then(r => r.data)

export const getApplicationById = (applicationId) =>
  axiosInstance.get(`/applications/${applicationId}`).then(r => r.data)

export const updateApplicationStatus = (applicationId, status) =>
  axiosInstance.patch(`/applications/${applicationId}/status`, null, { params: { status } }).then(r => r.data)
