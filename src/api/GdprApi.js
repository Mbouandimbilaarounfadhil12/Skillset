import axiosInstance from './AxiosInstance'

export const exportMyData = () =>
  axiosInstance.get('/gdpr/export')

export const deleteMyAccount = () =>
  axiosInstance.delete('/gdpr/account')

export const getConsents = () =>
  axiosInstance.get('/gdpr/consents')

export const updateConsent = (type, accepted) =>
  axiosInstance.put(`/gdpr/consents/${type}`, { accepted })

export const revokeAllConsents = () =>
  axiosInstance.delete('/gdpr/consents')
