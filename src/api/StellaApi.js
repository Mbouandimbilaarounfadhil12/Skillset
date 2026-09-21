import axiosInstance from './AxiosInstance'
import { STELLA_RESPONSES, searchJobs, COMPANIES } from '../data/mockData'
import { getRecommendedJobs, estimateProfileSalary, profileCompleteness } from '../utils/matchingUtils'

// ─── Online check ─────────────────────────────────────────────────────────────
export const isOnline = () => navigator.onLine

// ─── Internet search via DuckDuckGo (backend proxy) ──────────────────────────
export async function webSearch(query) {
  if (!isOnline()) return null
  try {
    const res = await axiosInstance.get('/stella/search', { params: { q: query }, timeout: 6000 })
    return res.data
  } catch {
    return null
  }
}

// ─── Global app search (jobs + companies) ─────────────────────────────────────
export function appSearch(query) {
  const q = query.toLowerCase()
  const jobs      = searchJobs(q, '', {}).slice(0, 4)
  const companies = COMPANIES.filter(c =>
    c.name.toLowerCase().includes(q) || c.sector.toLowerCase().includes(q)
  ).slice(0, 3)
  return { jobs, companies }
}

// ─── Main chat with STELLA ────────────────────────────────────────────────────
export async function stellaChat(message, { history = [], profile = null, online = true } = {}) {
  // Try backend
  try {
    const res = await axiosInstance.post('/stella/chat', {
      message,
      history: history.slice(-6),
      profile,
      online,
    }, { timeout: 8000 })
    return { text: res.data.reply, source: 'backend' }
  } catch {
    return { text: await generateLocalReply(message, { profile, online }), source: 'local' }
  }
}

// ─── Autonomous tasks ─────────────────────────────────────────────────────────
export async function runStellaTask(taskId, profile) {
  switch (taskId) {

    case 'find-best': {
      const jobs = getRecommendedJobs(profile, 3)
      if (jobs.length === 0) return "Je n'ai pas trouvé d'offres correspondant à votre profil. Complétez votre profil pour de meilleures recommandations."
      const list = jobs.map((j, i) => `${i + 1}. **${j.title}** — ${j.company} (${j.matchPct}% de correspondance) · ${fmtSalary(j.salary)}`).join('\n')
      return `🎯 Top ${jobs.length} offres pour votre profil :\n\n${list}\n\nSouhaitez-vous que je postule à l'une d'elles ?`
    }

    case 'salary-analysis': {
      const est = estimateProfileSalary(profile)
      return `💰 Estimation de salaire pour **${profile?.jobTitle || 'votre poste'}** :\n\n• Minimum : ${fmt(est.min)} ${est.currency}\n• Médiane : ${fmt(est.median)} ${est.currency}\n• Maximum : ${fmt(est.max)} ${est.currency}\n\n_Confiance : ${est.confidence} · Basé sur les offres du marché camerounais._`
    }

    case 'profile-analysis': {
      const score = profileCompleteness(profile)
      const skillsArr = Array.isArray(profile?.skills)
        ? profile.skills
        : (profile?.skills ? String(profile.skills).split(',').map(s => s.trim()).filter(Boolean) : [])

      const fieldLabels = {
        firstName: 'Prénom', lastName: 'Nom', email: 'Email', city: 'Ville',
        jobTitle: 'Poste recherché', skills: 'Compétences', bio: 'Bio', phone: 'Téléphone',
      }
      const missing = Object.entries(fieldLabels)
        .filter(([key]) => {
          const v = profile?.[key]
          return !(v && (Array.isArray(v) ? v.length > 0 : String(v).trim().length > 0))
        })
        .map(([, label]) => label)

      const advice = missing.length > 0
        ? `Complétez votre **${missing[0]}** pour augmenter la pertinence de vos recommandations.`
        : 'Votre profil est complet — continuez à le tenir à jour !'

      return `📊 Analyse de votre profil (score : **${score}/100**) :\n\n✅ Compétences renseignées : ${skillsArr.slice(0, 3).join(', ') || 'Aucune'}\n⚠️ Champs manquants : ${missing.join(', ') || 'Aucun'}\n\n💡 Conseil STELLA : ${advice}`
    }

    case 'prepare-interview': {
      const jobs    = getRecommendedJobs(profile, 1)
      const company = jobs[0]?.company || "l'entreprise"
      return `🎯 Préparation d'entretien pour ${company} :\n\n**Questions fréquentes :**\n1. Parlez-moi de vous\n2. Pourquoi ${company} ?\n3. Votre plus grande réussite ?\n4. Où vous voyez-vous dans 5 ans ?\n\n**Méthode STAR** (Situation, Tâche, Action, Résultat) pour structurer vos réponses.\n\n💡 Je peux faire une simulation d'entretien avec vous. Dites "Simule un entretien" !`
    }

    default:
      return "Tâche non reconnue. Essayez : trouver des offres, analyser mon profil, estimer mon salaire."
  }
}

// ─── Local reply generator ────────────────────────────────────────────────────
async function generateLocalReply(message, { profile, online }) {
  const m = message.toLowerCase()

  if (!online && m.match(/internet|web|cherche/)) {
    return "⚠️ Vous n'êtes pas connecté(e) à internet. Je ne peux pas effectuer de recherches en ligne pour le moment.\n\nJe peux néanmoins vous aider avec les données SkillSet disponibles hors ligne. Que souhaitez-vous ?"
  }

  if (m.match(/salut|bonjour|hello|bjr|bonsoir/)) return replyGreeting(profile)
  if (m.match(/salaire|rémunér|paye|combien gagn/)) return replySalaire(message, profile)
  if (m.match(/offre|emploi|job|travail|poste/))   return replyOffres(profile)
  if (m.match(/entreprise|company|avis|culture/))  return replyEntreprise(m)
  if (m.match(/entretien|interview|prépare/))       return STELLA_RESPONSES.entretien
  if (m.match(/cv|curriculum|candidature/))         return STELLA_RESPONSES.cv
  if (m.match(/aide|help|que peux|que sais/))       return STELLA_RESPONSES.aide
  if (m.match(/merci|super|top|parfait/))           return "Avec plaisir ! 😊 N'hésitez pas si vous avez d'autres questions."

  if (online) {
    const results = await webSearch(message)
    if (results?.abstractText) {
      return `${results.abstractText}\n\n_Source : ${results.abstractUrl || 'recherche web'}_`
    }
    if (results?.relatedTopics?.length) {
      const list = results.relatedTopics.slice(0, 3).map(t => `• ${t.text}`).join('\n')
      return `Voici ce que j'ai trouvé sur "${message}" :\n\n${list}`
    }
  }

  return STELLA_RESPONSES.fallback
}

function replyGreeting(profile) {
  const name = profile?.firstName ? `, ${profile.firstName}` : ''
  return `Bonjour${name} ! Je suis **STELLA**, votre assistante IA SkillSet 🌟\nComment puis-je vous aider aujourd'hui ?\n\n💡 Je peux chercher des offres, estimer votre salaire, préparer votre entretien, analyser votre profil ou vous mettre en contact avec un recruteur.`
}

function replySalaire(message, profile) {
  if (profile?.jobTitle) {
    const est = estimateProfileSalary(profile)
    return `💰 Pour un **${profile.jobTitle}**, voici les fourchettes actuelles :\n\n• Min : ${fmt(est.min)} FCFA\n• Médiane : ${fmt(est.median)} FCFA\n• Max : ${fmt(est.max)} FCFA\n\n_Basé sur les offres SkillSet · Confiance : ${est.confidence}_`
  }
  const m = message.toLowerCase()
  const titleMatch = m.match(/(?:quel.*salaire|combien.*gagne).*(?:pour |un |une )(.+?)(?:\s+à|\s+en|\s*$)/i)
  if (titleMatch) {
    const est = estimateProfileSalary({ jobTitle: titleMatch[1], yearsOfExperience: 3 })
    return `💰 Estimation pour **${titleMatch[1]}** :\n\n• Min : ${fmt(est.min)} FCFA\n• Médiane : ${fmt(est.median)} FCFA\n• Max : ${fmt(est.max)} FCFA`
  }
  return STELLA_RESPONSES.salaire
}

function replyOffres(profile) {
  if (profile) {
    const jobs = getRecommendedJobs(profile, 3)
    if (jobs.length > 0) {
      const list = jobs.map(j => `• **${j.title}** — ${j.company} (${j.matchPct}%)`).join('\n')
      return `🎯 Offres correspondant à votre profil :\n\n${list}\n\nConsultez la page **Offres** pour postuler !`
    }
  }
  return STELLA_RESPONSES.emploi
}

function replyEntreprise(m) {
  const company = COMPANIES.find(c => m.includes(c.name.toLowerCase()))
  if (company) {
    return `🏢 **${company.name}** — ${company.sector}\n⭐ Note : ${company.rating}/5 (${company.reviewCount} avis)\n📍 ${company.city}, ${company.country}\n\n_"${company.reviews[0]?.body}"_`
  }
  const top = [...COMPANIES].sort((a, b) => b.rating - a.rating).slice(0, 3)
  const lines = top.map(c => '• **' + c.name + '** ⭐ ' + c.rating + '/5').join('\n')
  return `🏆 Top entreprises SkillSet :\n${lines}`
}

function fmt(n) { return n.toLocaleString('fr-FR') }
function fmtSalary(s) { return `${(s.min / 1000).toFixed(0)}k–${(s.max / 1000).toFixed(0)}k ${s.currency}` }
