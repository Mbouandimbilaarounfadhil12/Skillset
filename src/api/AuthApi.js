import axiosInstance from './AxiosInstance'

export const loginUser = (credentials) =>
  axiosInstance.post('/auth/login', credentials).then((r) => r.data)

export const registerUser = (userData) =>
  axiosInstance.post('/auth/register', userData).then((r) => r.data)

export const getProfile = (userId) =>
  axiosInstance.get(`/auth/profile/${userId}`).then((r) => r.data)

export const updateProfile = (userId, data) =>
  axiosInstance.put(`/auth/profile/${userId}`, data).then((r) => r.data)

export const uploadProfilePhoto = (userId, file) => {
  const form = new FormData()
  form.append('file', file)
  return axiosInstance
    .post(`/auth/profile/${userId}/photo`, form, { headers: { 'Content-Type': 'multipart/form-data' } })
    .then((r) => r.data)
}

// ── 2FA ──────────────────────────────────────────────────────────────────────

export const setup2fa = () =>
  axiosInstance.post('/auth/2fa/setup').then((r) => r.data)

export const confirm2fa = (code) =>
  axiosInstance.post('/auth/2fa/confirm', { code }).then((r) => r.data)

export const verify2faLogin = (preAuthToken, code) =>
  axiosInstance.post('/auth/2fa/verify-login', { preAuthToken, code }).then((r) => r.data)

export const disable2fa = (code) =>
  axiosInstance.delete('/auth/2fa/disable', { data: { code } }).then((r) => r.data)
