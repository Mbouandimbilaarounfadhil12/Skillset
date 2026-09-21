import axiosInstance from './AxiosInstance'

export const getReviews = (applicationId) =>
  axiosInstance.get(`/applications/${applicationId}/reviews`).then(r => r.data)

export const addReview = (applicationId, rating, comments, status) =>
  axiosInstance.post(`/applications/${applicationId}/reviews`, { rating, comments, status }).then(r => r.data)
