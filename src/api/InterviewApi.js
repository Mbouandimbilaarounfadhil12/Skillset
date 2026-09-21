import axiosInstance from './AxiosInstance'

export const scheduleInterview = (interview) =>
  axiosInstance.post('/interviews', interview).then(r => r.data)

export const getCandidateInterviews = (candidateId) =>
  axiosInstance.get(`/interviews/candidate/${candidateId}`).then(r => r.data)

export const updateInterviewStatus = (interviewId, status) =>
  axiosInstance.put(`/interviews/${interviewId}/status`, null, { params: { status } }).then(r => r.data)
