import axios from 'axios'

const publicApi = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
  headers: { 'Content-Type': 'application/json' },
})

export const getCareerPage = (slug) =>
  publicApi.get(`/public/careers/${slug}`).then(r => r.data)
