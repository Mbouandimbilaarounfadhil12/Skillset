import axiosInstance from './AxiosInstance'

export const getAdminStats = () =>
  axiosInstance.get('/admin/stats').then(r => r.data)

export const getAdminUsers = (search = '') =>
  axiosInstance.get('/admin/users', { params: search ? { search } : {} }).then(r => r.data)

export const toggleUserStatus = (userId) =>
  axiosInstance.put(`/admin/users/${userId}/status`).then(r => r.data)

export const getAdminAnalytics = () =>
  axiosInstance.get('/admin/analytics').then(r => r.data)

export const exportApplicationsCsv = () =>
  axiosInstance.get('/admin/export/applications', { responseType: 'blob' }).then(r => r.data)

export const getAdminJobs = (status = '') =>
  axiosInstance.get('/admin/jobs', { params: status ? { status } : {} }).then(r => r.data)

export const changeJobStatus = (jobId, status) =>
  axiosInstance.put(`/admin/jobs/${jobId}/status`, null, { params: { status } }).then(r => r.data)
