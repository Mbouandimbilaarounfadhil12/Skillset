import axiosInstance from './AxiosInstance'

export const generateQuestions = (role, initialAnswers) =>
  axiosInstance.post('/onboarding/generate-questions', { role, initialAnswers }).then(r => r.data)

export const completeOnboarding = (role, initialAnswers, aiAnswers, wantsCv = false) =>
  axiosInstance.post('/onboarding/complete', { role, initialAnswers, aiAnswers, wantsCv }).then(r => r.data)

export const generateCv = (jobTitle, answers) =>
  axiosInstance.post('/onboarding/generate-cv', { jobTitle, answers }).then(r => r.data)
