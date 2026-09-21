import axios from 'axios';
import { useAuthStore } from '../store/AuthStore';

const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
  headers: { 'Content-Type': 'application/json' },
});

axiosInstance.interceptors.request.use((config) => {
  const token = localStorage.getItem('authToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    // Only a real auth failure while we believed we were logged in warrants a logout.
    // A soft logout (store update) lets React Router redirect via ProtectedRoute
    // instead of a hard page reload that wipes in-flight SPA state.
    if (error.response?.status === 401 && localStorage.getItem('authToken')) {
      useAuthStore.getState().logout();
    }
    return Promise.reject(error);
  }
);

export default axiosInstance;
