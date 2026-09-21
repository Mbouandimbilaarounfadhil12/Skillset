import axiosInstance from './AxiosInstance'

export const getTalentPools = () =>
  axiosInstance.get('/talent-pools').then(r => r.data)

export const createTalentPool = (dto) =>
  axiosInstance.post('/talent-pools', dto).then(r => r.data)

export const getPoolMembers = (poolId, page = 0, size = 20) =>
  axiosInstance.get(`/talent-pools/${poolId}/members`, { params: { page, size } }).then(r => r.data)

export const addCandidateToPool = (poolId, candidateId, notes, source) =>
  axiosInstance.post(`/talent-pools/${poolId}/members`, { candidateId, notes, source }).then(r => r.data)

export const removeCandidateFromPool = (poolId, candidateId) =>
  axiosInstance.delete(`/talent-pools/${poolId}/members/${candidateId}`).then(r => r.data)

export const updateMemberStatus = (poolId, candidateId, status) =>
  axiosInstance.put(`/talent-pools/${poolId}/members/${candidateId}/status`, { status }).then(r => r.data)

export const addFromApplication = (poolId, applicationId) =>
  axiosInstance.post(`/talent-pools/${poolId}/members/from-application`, { applicationId }).then(r => r.data)

export const getRecommendedCandidates = (poolId) =>
  axiosInstance.get(`/talent-pools/${poolId}/recommendations`).then(r => r.data)
