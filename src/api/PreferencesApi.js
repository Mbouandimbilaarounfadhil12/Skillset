import axiosInstance from './AxiosInstance'

export const getPreferences  = ()        => axiosInstance.get('/users/preferences').then(r => r.data)
export const savePreferences = (payload) => axiosInstance.put('/users/preferences', payload).then(r => r.data)
