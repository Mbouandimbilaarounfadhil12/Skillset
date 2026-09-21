import axiosInstance from '../api/AxiosInstance';

export const applicationService = {
  submitApplication: (appData) => axiosInstance.post('/applications', appData),
  getCandidateApplications: (jobSeekerId) => 
    axiosInstance.get(`/applications/candidate/${jobSeekerId}`),
  getJobApplications: (jobListingId) => 
    axiosInstance.get(`/applications/job/${jobListingId}`),
  getApplicationById: (applicationId) => 
    axiosInstance.get(`/applications/${applicationId}`),
  updateApplicationStatus: (applicationId, status) =>
    axiosInstance.patch(`/applications/${applicationId}/status`, null, { params: { status } }),
};

export default applicationService;
