import { create } from 'zustand'

const KEY = (uid) => `ss_userdata_${uid}`

function load(uid) {
  try {
    const raw = localStorage.getItem(KEY(uid))
    return raw ? JSON.parse(raw) : null
  } catch { return null }
}

function persist(uid, data) {
  try { localStorage.setItem(KEY(uid), JSON.stringify(data)) } catch { /* ignore */ }
}

// ── Rich seed data shown on first login ──────────────────────────
const SEED_SAVED_JOBS = [
  {
    id: 'j9', title: 'DevOps Engineer', company: 'StartupCMR', logo: '🚀',
    location: 'Yaoundé', type: 'CDI', salary: { min: 500000, max: 900000, currency: 'FCFA' },
    remote: true, saved: true, savedAt: new Date(Date.now() - 2 * 86400000).toISOString(),
  },
  {
    id: 'j11', title: 'Analyste Financier', company: 'Afriland First Bank', logo: '🏦',
    location: 'Yaoundé', type: 'CDI', salary: { min: 350000, max: 600000, currency: 'FCFA' },
    remote: false, saved: true, savedAt: new Date(Date.now() - 5 * 86400000).toISOString(),
  },
  {
    id: 'j6', title: 'Community Manager', company: 'Jumia Cameroun', logo: '🟢',
    location: 'Douala', type: 'CDI', salary: { min: 150000, max: 280000, currency: 'FCFA' },
    remote: true, saved: true, savedAt: new Date(Date.now() - 8 * 86400000).toISOString(),
  },
]

const SEED_APPLICATIONS = [
  {
    id: 'app-1', jobId: 'j1', jobTitle: 'Développeur React Senior', company: 'StartupCMR',
    logo: '🚀', location: 'Yaoundé', type: 'CDI',
    salary: '600k – 900k FCFA', appliedDate: new Date(Date.now() - 12 * 86400000).toISOString().split('T')[0],
    status: 'En cours', stage: 'Test technique', stageIndex: 1,
  },
  {
    id: 'app-2', jobId: 'j2', jobTitle: 'Data Analyst', company: 'Orange Cameroun',
    logo: '🟠', location: 'Douala', type: 'CDI',
    salary: '450k – 700k FCFA', appliedDate: new Date(Date.now() - 20 * 86400000).toISOString().split('T')[0],
    status: 'En cours', stage: 'Entretien RH', stageIndex: 2,
  },
  {
    id: 'app-3', jobId: 'j7', jobTitle: 'Auditeur Interne', company: 'Deloitte Afrique Centrale',
    logo: '🔵', location: 'Douala', type: 'CDI',
    salary: '450k – 700k FCFA', appliedDate: new Date(Date.now() - 30 * 86400000).toISOString().split('T')[0],
    status: 'En attente', stage: 'CV transmis', stageIndex: 0,
  },
]

const SEED_INTERVIEWS = [
  {
    id: 'itv-1', jobId: 'j2', jobTitle: 'Data Analyst', company: 'Orange Cameroun',
    logo: '🟠', status: 'Confirmé',
    date: new Date(Date.now() + 3 * 86400000).toLocaleDateString('fr-FR', { day: '2-digit', month: 'long', year: 'numeric' }),
    time: '10h00',
    type: 'Visioconférence', platform: 'Google Meet',
    contact: 'Sarah Mballa — RH Orange',
    notes: 'Préparer cas pratique analyse de données télécom. Apporter 3 exemples de projets Python/SQL passés.',
  },
  {
    id: 'itv-2', jobId: 'j1', jobTitle: 'Développeur React Senior', company: 'StartupCMR',
    logo: '🚀', status: 'En attente de confirmation',
    date: new Date(Date.now() + 7 * 86400000).toLocaleDateString('fr-FR', { day: '2-digit', month: 'long', year: 'numeric' }),
    time: '14h30',
    type: 'Présentiel', platform: '',
    contact: 'Armel Fouda — CTO StartupCMR',
    notes: 'Entretien technique sur React/Node.js + présentation de projet portfolio.',
  },
]

const SEED_ARCHIVES = [
  {
    id: 'arc-1', jobId: 'j4', jobTitle: 'Commercial Grands Comptes', company: 'MTN Cameroun',
    logo: '🟡', location: 'Douala', type: 'CDI',
    appliedDate: new Date(Date.now() - 45 * 86400000).toISOString().split('T')[0],
    closedDate: new Date(Date.now() - 20 * 86400000).toISOString().split('T')[0],
    status: 'Refusé', reason: 'Profil ne correspond pas aux prérequis commerciaux définis.',
  },
  {
    id: 'arc-2', jobId: 'j8', jobTitle: 'Chargé RH & Recrutement', company: 'Orange Cameroun',
    logo: '🟠', location: 'Yaoundé', type: 'CDI',
    appliedDate: new Date(Date.now() - 60 * 86400000).toISOString().split('T')[0],
    closedDate: new Date(Date.now() - 35 * 86400000).toISOString().split('T')[0],
    status: 'Refusé', reason: 'Poste pourvu en interne.',
  },
]

const empty = () => ({ savedJobs: [], applications: [], interviews: [], archives: [] })
const seed  = () => ({ savedJobs: SEED_SAVED_JOBS, applications: SEED_APPLICATIONS, interviews: SEED_INTERVIEWS, archives: SEED_ARCHIVES })

export const useUserDataStore = create((set, get) => ({
  _uid: null,
  savedJobs:    [],
  applications: [],
  interviews:   [],
  archives:     [],

  loadUser: (uid) => {
    if (!uid) return
    const data = load(uid)
    if (!data) {
      const seeded = seed()
      persist(uid, seeded)
      set({ _uid: uid, ...seeded })
    } else {
      set({ _uid: uid, ...data })
    }
  },

  _save: () => {
    const { _uid, savedJobs, applications, interviews, archives } = get()
    if (_uid) persist(_uid, { savedJobs, applications, interviews, archives })
  },

  // ── Saved jobs ───────────────────────────────────────────────────
  saveJob: (job) => {
    set(s => {
      if (s.savedJobs.some(j => j.id === job.id)) return s
      return { savedJobs: [{ ...job, saved: true, savedAt: new Date().toISOString() }, ...s.savedJobs] }
    })
    get()._save()
  },

  unsaveJob: (jobId) => {
    set(s => ({ savedJobs: s.savedJobs.filter(j => j.id !== jobId) }))
    get()._save()
  },

  isJobSaved: (jobId) => get().savedJobs.some(j => j.id === jobId),

  // ── Applications ─────────────────────────────────────────────────
  applyToJob: (application) => {
    set(s => {
      if (s.applications.some(a => a.jobId === application.jobId)) return s
      const app = {
        id: `a-${Date.now()}`,
        appliedDate: new Date().toISOString().split('T')[0],
        status: 'En attente',
        stage: 'CV transmis',
        stageIndex: 0,
        ...application,
      }
      return { applications: [app, ...s.applications] }
    })
    get()._save()
  },

  hasApplied: (jobId) => get().applications.some(a => a.jobId === jobId),

  // ── Interviews ────────────────────────────────────────────────────
  addInterview: (interview) => {
    set(s => ({ interviews: [interview, ...s.interviews] }))
    get()._save()
  },

  // ── Archives ──────────────────────────────────────────────────────
  archiveApplication: (appId) => {
    const app = get().applications.find(a => a.id === appId)
    if (!app) return
    set(s => ({
      applications: s.applications.filter(a => a.id !== appId),
      archives: [{ ...app, closedDate: new Date().toISOString().split('T')[0], reason: 'Archivé manuellement' }, ...s.archives],
    }))
    get()._save()
  },

  reset: () => set({ _uid: null, ...empty() }),
}))
