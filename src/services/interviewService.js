import axiosInstance from '../api/AxiosInstance';

export const interviewService = {
  scheduleInterview: (interviewData) => axiosInstance.post('/interviews', interviewData),
  getCandidateInterviews: (candidateId) => 
    axiosInstance.get(`/interviews/candidate/${candidateId}`),
  getInterviewerSchedule: (interviewerId) => 
    axiosInstance.get(`/interviews/interviewer/${interviewerId}`),
  getInterviewById: (interviewId) => 
    axiosInstance.get(`/interviews/${interviewId}`),
  updateInterviewStatus: (interviewId, status) => 
    axiosInstance.put(`/interviews/${interviewId}/status`, null, { params: { status } }),
  addFeedback: (interviewId, notes, rating) => 
    axiosInstance.put(`/interviews/${interviewId}/feedback`, null, { params: { notes, rating } }),
};

export default interviewService;
