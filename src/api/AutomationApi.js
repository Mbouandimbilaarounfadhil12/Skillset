import axiosInstance from './AxiosInstance'

export const createRule = (dto) =>
  axiosInstance.post('/automation/rules', dto)

export const getRules = () =>
  axiosInstance.get('/automation/rules')

export const toggleRule = (id) =>
  axiosInstance.put(`/automation/rules/${id}/toggle`)

export const bulkUpdateStatus = (applicationIds, newStatus) =>
  axiosInstance.post('/automation/bulk-status', { applicationIds, newStatus })
