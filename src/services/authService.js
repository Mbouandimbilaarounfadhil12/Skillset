import axiosInstance from '../api/AxiosInstance';

export const authService = {
  register: (userData) => axiosInstance.post('/auth/register', userData),
  login: (email) => axiosInstance.post('/auth/login', null, { params: { email } }),
  getProfile: (userId) => axiosInstance.get(`/auth/profile/${userId}`),
  updateProfile: (userId, userData) => axiosInstance.put(`/auth/profile/${userId}`, userData),
};

export default authService;
