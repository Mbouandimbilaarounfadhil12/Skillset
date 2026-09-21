import axiosInstance from '../api/AxiosInstance';

export const screeningService = {
  getScreeningQuestions: (jobListingId) => 
    axiosInstance.get(`/screening-questions/job/${jobListingId}`),
  createQuestion: (questionData) => 
    axiosInstance.post('/screening-questions', questionData),
  updateQuestion: (questionId, questionData) => 
    axiosInstance.put(`/screening-questions/${questionId}`, questionData),
  deleteQuestion: (questionId) => 
    axiosInstance.delete(`/screening-questions/${questionId}`),
};

export default screeningService;
