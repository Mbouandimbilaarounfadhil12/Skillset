import axiosInstance from './AxiosInstance'
import { SALARY_BENCHMARKS, STELLA_RESPONSES } from '../data/mockData'

export const sendChatMessage = async (message, history = []) => {
  try {
    const res = await axiosInstance.post('/ai/chat', { message, history })
    return res.data.reply
  } catch {
    return generateLocalReply(message)
  }
}

export const estimateSalary = async (jobTitle, location, experience) => {
  try {
    const res = await axiosInstance.post('/ai/salary-estimate', { jobTitle, location, experience })
    return res.data
  } catch {
    return localSalaryEstimate(jobTitle, location, experience)
  }
}

function generateLocalReply(message) {
  const m = message.toLowerCase()
  if (m.match(/salut|bonjour|hello|hi|bjr/)) return STELLA_RESPONSES.salut
  if (m.match(/aide|help|quoi|comment|que/)) return STELLA_RESPONSES.aide
  if (m.match(/salaire|rémunér|paye|combien|estimation/)) return STELLA_RESPONSES.salaire
  if (m.match(/emploi|travail|job|offre|poste|cherche/)) return STELLA_RESPONSES.emploi
  if (m.match(/entretien|interview|prépar/)) return STELLA_RESPONSES.entretien
  if (m.match(/cv|curriculum|candidature|lettre/)) return STELLA_RESPONSES.cv
  if (m.match(/merci|super|top|parfait|genial/)) return "Avec plaisir ! 😊 N'hésitez pas si vous avez d'autres questions."
  if (m.match(/quel.*salaire.*(\w+)/i)) {
    const titleMatch = m.match(/pour (?:un|une) (.+?)(?:\s+à|\s*$)/i)
    if (titleMatch) {
      const est = localSalaryEstimate(titleMatch[1], '')
      return `💰 Estimation STELLA pour **${titleMatch[1]}** :\n• Min : ${fmt(est.min)} FCFA\n• Médiane : ${fmt(est.median)} FCFA\n• Max : ${fmt(est.max)} FCFA\n\n_Basé sur les offres actuelles du marché camerounais._`
    }
  }
  return STELLA_RESPONSES.fallback
}

function localSalaryEstimate(jobTitle = '', location = '', experience = '') {
  const title = jobTitle.toLowerCase()
  const key = Object.keys(SALARY_BENCHMARKS).find(k => title.includes(k.toLowerCase()))
  const base = key ? SALARY_BENCHMARKS[key] : { min: 150_000, max: 500_000, median: 300_000 }

  let expMultiplier = 1
  if (experience === 'senior') expMultiplier = 1.25
  else if (experience === 'junior') expMultiplier = 0.8
  const locMultiplier = location?.toLowerCase().includes('douala') ? 1.05 : 1

  return {
    title: jobTitle,
    location,
    min: Math.round(base.min * expMultiplier * locMultiplier / 1000) * 1000,
    median: Math.round(base.median * expMultiplier * locMultiplier / 1000) * 1000,
    max: Math.round(base.max * expMultiplier * locMultiplier / 1000) * 1000,
    currency: 'FCFA',
    confidence: key ? 'élevé' : 'estimé',
    source: 'Analyse STELLA des offres SkillSet',
  }
}

function fmt(n) { return n.toLocaleString('fr-FR') }
