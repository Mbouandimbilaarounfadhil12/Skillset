import { create } from 'zustand'

const MOCK_NOTIFS = [
  { id: 'n1', type: 'application', title: 'Candidature vue', body: 'Deloitte a consulté votre profil pour le poste d\'Auditeur Interne.', date: '2026-06-08T09:30:00', read: false, link: '/my-jobs' },
  { id: 'n2', type: 'interview',   title: 'Entretien confirmé', body: 'Votre entretien avec StartupCMR est confirmé pour le 15 jan.', date: '2026-06-07T14:10:00', read: false, link: '/my-jobs' },
  { id: 'n3', type: 'message',     title: 'Nouveau message', body: 'Mme. Enow (RH StartupCMR) vous a envoyé un message.', date: '2026-06-07T11:45:00', read: true, link: '/messages' },
  { id: 'n4', type: 'match',       title: 'Nouvelle offre correspondante', body: 'Une offre DevOps chez StartupCMR correspond à 92% à votre profil.', date: '2026-06-06T16:20:00', read: true, link: '/jobs' },
  { id: 'n5', type: 'stella',      title: 'STELLA a trouvé 3 offres', body: 'Basé sur votre profil, STELLA recommande 3 nouvelles opportunités.', date: '2026-06-05T08:00:00', read: true, link: '/jobs' },
]

export const useNotificationStore = create((set) => ({
  notifications: MOCK_NOTIFS,
  unreadCount: MOCK_NOTIFS.filter(n => !n.read).length,

  addNotification: (notif) => {
    const n = { id: `n-${Date.now()}`, date: new Date().toISOString(), read: false, ...notif }
    set(s => ({
      notifications: [n, ...s.notifications],
      unreadCount: s.unreadCount + 1,
    }))
  },

  markRead: (id) => set(s => ({
    notifications: s.notifications.map(n => n.id === id ? { ...n, read: true } : n),
    unreadCount: Math.max(0, s.unreadCount - (s.notifications.find(n => n.id === id)?.read ? 0 : 1)),
  })),

  markAllRead: () => set(s => ({
    notifications: s.notifications.map(n => ({ ...n, read: true })),
    unreadCount: 0,
  })),

  removeNotification: (id) => set(s => {
    const notif = s.notifications.find(n => n.id === id)
    return {
      notifications: s.notifications.filter(n => n.id !== id),
      unreadCount: Math.max(0, s.unreadCount - (notif?.read ? 0 : 1)),
    }
  }),
}))
