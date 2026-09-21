// ─── Companies ───────────────────────────────────────────────────────────────
export const COMPANIES = [
  {
    id: 'c1', name: 'Orange Cameroun', logo: '🟠', sector: 'Télécommunications',
    size: '1000-5000', country: 'Cameroun', city: 'Douala', rating: 4.1, reviewCount: 148,
    description: "Leader des télécoms en Afrique centrale.",
    reviews: [
      { id: 'r1', author: 'Marie K.', rating: 5, title: 'Excellente ambiance', body: "Super équipe, management bienveillant, formation continue.", date: '2025-11-12' },
      { id: 'r2', author: 'Paul M.', rating: 3, title: 'Correct mais bureaucratique', body: "Bon environnement mais les décisions prennent du temps.", date: '2025-09-05' },
      { id: 'r3', author: 'Sophie A.', rating: 5, title: 'Meilleure expérience pro', body: "Apprentissage rapide, responsabilités dès le départ.", date: '2025-07-22' },
    ],
  },
  {
    id: 'c2', name: 'MTN Cameroun', logo: '🟡', sector: 'Télécommunications',
    size: '1000-5000', country: 'Cameroun', city: 'Yaoundé', rating: 3.8, reviewCount: 214,
    description: "Deuxième opérateur télécom au Cameroun.",
    reviews: [
      { id: 'r4', author: 'Jean L.', rating: 4, title: 'Bonne expérience', body: "Projets intéressants et collègues compétents.", date: '2025-12-01' },
      { id: 'r5', author: 'Alice B.', rating: 3, title: 'Moyen', body: "Salaires en dessous du marché, mais avantages corrects.", date: '2025-10-18' },
    ],
  },
  {
    id: 'c3', name: 'Société Générale Cameroun', logo: '🔴', sector: 'Finance & Banque',
    size: '500-1000', country: 'Cameroun', city: 'Douala', rating: 4.3, reviewCount: 87,
    description: "Filiale africaine du groupe Société Générale.",
    reviews: [
      { id: 'r6', author: 'Eric D.', rating: 5, title: 'Cadre professionnel', body: "Standards internationaux, équipe de qualité.", date: '2025-08-15' },
      { id: 'r7', author: 'Nadia F.', rating: 4, title: 'Très bien', body: "Bonne culture d'entreprise, rémunération juste.", date: '2025-06-30' },
    ],
  },
  {
    id: 'c4', name: 'Deloitte Afrique Centrale', logo: '🔵', sector: 'Conseil & Audit',
    size: '200-500', country: 'Cameroun', city: 'Douala', rating: 4.5, reviewCount: 62,
    description: "Cabinet de conseil et d'audit leader en Afrique.",
    reviews: [
      { id: 'r8', author: 'Thomas R.', rating: 5, title: 'Tremplin de carrière', body: "Projets variés, clients internationaux, montée en compétences rapide.", date: '2025-11-20' },
    ],
  },
  {
    id: 'c5', name: 'TotalEnergies Cameroun', logo: '⚫', sector: 'Énergie',
    size: '500-1000', country: 'Cameroun', city: 'Douala', rating: 4.0, reviewCount: 95,
    description: "Acteur majeur du secteur pétrolier et gazier.",
    reviews: [
      { id: 'r9', author: 'Marc P.', rating: 4, title: 'Bons avantages', body: "Véhicule de fonction, assurance santé, bonne rémunération.", date: '2025-09-14' },
      { id: 'r10', author: 'Lucie N.', rating: 4, title: 'Environnement exigeant', body: "Haute performance attendue mais récompensée.", date: '2025-07-08' },
    ],
  },
  {
    id: 'c6', name: 'StartupCMR', logo: '🚀', sector: 'Tech & Startups',
    size: '10-50', country: 'Cameroun', city: 'Yaoundé', rating: 4.7, reviewCount: 28,
    description: "Hub de l'innovation tech camerounaise.",
    reviews: [
      { id: 'r11', author: 'Kevin A.', rating: 5, title: 'Passionnant !', body: "Liberté, créativité, impact direct sur le produit.", date: '2025-12-15' },
    ],
  },
  {
    id: 'c7', name: 'Jumia Cameroun', logo: '🟢', sector: 'E-commerce',
    size: '100-500', country: 'Cameroun', city: 'Douala', rating: 3.6, reviewCount: 103,
    description: "Leader de l'e-commerce en Afrique.",
    reviews: [
      { id: 'r12', author: 'Diana E.', rating: 4, title: 'Dynamique', body: "Rythme soutenu, bonne expérience de la croissance rapide.", date: '2025-10-02' },
      { id: 'r13', author: 'Franck T.', rating: 3, title: 'Instable', body: "Turnover élevé, objectifs changeants.", date: '2025-08-19' },
    ],
  },
  {
    id: 'c8', name: 'Afriland First Bank', logo: '🏦', sector: 'Finance & Banque',
    size: '500-1000', country: 'Cameroun', city: 'Yaoundé', rating: 3.9, reviewCount: 76,
    description: "Banque panafricaine fondée au Cameroun.",
    reviews: [
      { id: 'r14', author: 'Rose M.', rating: 4, title: 'Fier d\'y travailler', body: "Institution locale respectée, bons collègues.", date: '2025-11-08' },
    ],
  },
]

// ─── Salary benchmarks ────────────────────────────────────────────────────────
export const SALARY_BENCHMARKS = {
  'Développeur web':         { min: 350_000, max: 750_000, median: 500_000 },
  'Développeur React':       { min: 400_000, max: 900_000, median: 620_000 },
  'Développeur Full Stack':  { min: 450_000, max: 1_000_000, median: 680_000 },
  'Data analyst':            { min: 400_000, max: 850_000, median: 580_000 },
  'Data scientist':          { min: 500_000, max: 1_100_000, median: 750_000 },
  'DevOps':                  { min: 450_000, max: 1_000_000, median: 700_000 },
  'Chef de projet':          { min: 350_000, max: 700_000, median: 480_000 },
  'Community manager':       { min: 150_000, max: 350_000, median: 220_000 },
  'Comptable':               { min: 200_000, max: 500_000, median: 320_000 },
  'Commercial':              { min: 150_000, max: 450_000, median: 280_000 },
  'Ressources humaines':     { min: 200_000, max: 500_000, median: 340_000 },
  'Graphiste':               { min: 150_000, max: 400_000, median: 250_000 },
  'Infirmier':               { min: 120_000, max: 300_000, median: 190_000 },
  'Médecin':                 { min: 300_000, max: 800_000, median: 520_000 },
  'Chargé de communication': { min: 200_000, max: 450_000, median: 300_000 },
  'Consultant':              { min: 350_000, max: 800_000, median: 550_000 },
  'Analyste financier':      { min: 350_000, max: 750_000, median: 490_000 },
  'Responsable marketing':   { min: 280_000, max: 600_000, median: 400_000 },
}

// ─── Jobs ─────────────────────────────────────────────────────────────────────
export const JOBS = [
  {
    id: 'j1', title: 'Développeur React Senior', companyId: 'c6', company: 'StartupCMR',
    logo: '🚀', location: 'Yaoundé', country: 'Cameroun', type: 'CDI',
    salary: { min: 600_000, max: 900_000, currency: 'FCFA' }, remote: true,
    skills: ['React', 'Node.js', 'TypeScript', 'MongoDB'],
    description: 'Rejoignez notre équipe tech pour construire la prochaine génération de produits digitaux africains.',
    sector: 'Informatique & Développement logiciel', experience: '3-5 ans', education: 'Bac+3/5',
    postedDaysAgo: 2, applicants: 34, featured: true, saved: false,
  },
  {
    id: 'j2', title: 'Data Analyst', companyId: 'c1', company: 'Orange Cameroun',
    logo: '🟠', location: 'Douala', country: 'Cameroun', type: 'CDI',
    salary: { min: 450_000, max: 700_000, currency: 'FCFA' }, remote: false,
    skills: ['Python', 'SQL', 'Power BI', 'Excel'],
    description: 'Analysez les données de millions de clients pour améliorer nos services télécoms.',
    sector: 'Informatique & Développement logiciel', experience: '2-4 ans', education: 'Bac+4/5',
    postedDaysAgo: 5, applicants: 67, featured: true, saved: false,
  },
  {
    id: 'j3', title: 'Responsable Comptabilité', companyId: 'c3', company: 'Société Générale Cameroun',
    logo: '🔴', location: 'Douala', country: 'Cameroun', type: 'CDI',
    salary: { min: 350_000, max: 550_000, currency: 'FCFA' }, remote: false,
    skills: ['OHADA', 'SAGE', 'Fiscalité', 'Reporting'],
    description: 'Supervisez la comptabilité et les reportings financiers de la filiale.',
    sector: 'Comptabilité & Audit', experience: '5+ ans', education: 'Bac+4/5 Comptabilité',
    postedDaysAgo: 8, applicants: 23, featured: false, saved: false,
  },
  {
    id: 'j4', title: 'Commercial Grands Comptes', companyId: 'c2', company: 'MTN Cameroun',
    logo: '🟡', location: 'Douala', country: 'Cameroun', type: 'CDI',
    salary: { min: 250_000, max: 450_000, currency: 'FCFA' }, remote: false,
    skills: ['Négociation', 'CRM', 'B2B', 'Télécom'],
    description: 'Développez et gérez un portefeuille de clients grands comptes.',
    sector: 'Vente & Marketing', experience: '3-5 ans', education: 'Bac+3/4',
    postedDaysAgo: 3, applicants: 89, featured: false, saved: false,
  },
  {
    id: 'j5', title: 'Chef de Projet IT', companyId: 'c5', company: 'TotalEnergies Cameroun',
    logo: '⚫', location: 'Douala', country: 'Cameroun', type: 'CDI',
    salary: { min: 500_000, max: 800_000, currency: 'FCFA' }, remote: false,
    skills: ['PMBOK', 'Agile', 'ERP SAP', 'IT Infrastructure'],
    description: 'Pilotez les projets de transformation digitale du groupe en Afrique centrale.',
    sector: 'Informatique & Développement logiciel', experience: '5+ ans', education: 'Bac+5 Ingénieur',
    postedDaysAgo: 1, applicants: 41, featured: true, saved: false,
  },
  {
    id: 'j6', title: 'Community Manager', companyId: 'c7', company: 'Jumia Cameroun',
    logo: '🟢', location: 'Douala', country: 'Cameroun', type: 'CDI',
    salary: { min: 150_000, max: 280_000, currency: 'FCFA' }, remote: true,
    skills: ['Facebook Ads', 'Instagram', 'Canva', 'Copywriting'],
    description: 'Animez nos communautés en ligne et boostez notre présence digitale.',
    sector: 'Publicité & Communication', experience: '1-3 ans', education: 'Bac+2/3 Communication',
    postedDaysAgo: 10, applicants: 156, featured: false, saved: false,
  },
  {
    id: 'j7', title: 'Auditeur Interne', companyId: 'c4', company: 'Deloitte Afrique Centrale',
    logo: '🔵', location: 'Douala', country: 'Cameroun', type: 'CDI',
    salary: { min: 450_000, max: 700_000, currency: 'FCFA' }, remote: false,
    skills: ['IFRS', 'Audit', 'Risk management', 'Excel avancé'],
    description: "Menez des missions d'audit financier et conseil auprès de grandes entreprises.",
    sector: 'Comptabilité & Audit', experience: '2-5 ans', education: 'Bac+5 Finance/Audit',
    postedDaysAgo: 6, applicants: 38, featured: true, saved: false,
  },
  {
    id: 'j8', title: 'Chargé RH & Recrutement', companyId: 'c1', company: 'Orange Cameroun',
    logo: '🟠', location: 'Yaoundé', country: 'Cameroun', type: 'CDI',
    salary: { min: 250_000, max: 450_000, currency: 'FCFA' }, remote: false,
    skills: ['Recrutement', 'GPEC', 'Formation', 'ATS'],
    description: 'Gérez le cycle complet de recrutement et les plans de développement RH.',
    sector: 'Ressources Humaines', experience: '2-4 ans', education: 'Bac+4/5 RH',
    postedDaysAgo: 12, applicants: 72, featured: false, saved: false,
  },
  {
    id: 'j9', title: 'DevOps Engineer', companyId: 'c6', company: 'StartupCMR',
    logo: '🚀', location: 'Yaoundé', country: 'Cameroun', type: 'CDI',
    salary: { min: 500_000, max: 900_000, currency: 'FCFA' }, remote: true,
    skills: ['Docker', 'Kubernetes', 'AWS', 'CI/CD', 'Linux'],
    description: "Automatisez et fiabilisez notre infrastructure cloud pour une plateforme à l'échelle.",
    sector: 'Informatique & Développement logiciel', experience: '3-6 ans', education: 'Bac+4/5 Informatique',
    postedDaysAgo: 4, applicants: 19, featured: false, saved: false,
  },
  {
    id: 'j10', title: 'Graphiste / Motion Designer', companyId: 'c7', company: 'Jumia Cameroun',
    logo: '🟢', location: 'Douala', country: 'Cameroun', type: 'CDD',
    salary: { min: 180_000, max: 320_000, currency: 'FCFA' }, remote: true,
    skills: ['Adobe Illustrator', 'After Effects', 'Figma', 'Branding'],
    description: 'Créez des visuels engageants pour nos campagnes marketing digitales.',
    sector: 'Architecture & Design', experience: '1-3 ans', education: 'Bac+2/3 Design',
    postedDaysAgo: 7, applicants: 93, featured: false, saved: false,
  },
  {
    id: 'j11', title: 'Analyste Financier', companyId: 'c8', company: 'Afriland First Bank',
    logo: '🏦', location: 'Yaoundé', country: 'Cameroun', type: 'CDI',
    salary: { min: 350_000, max: 600_000, currency: 'FCFA' }, remote: false,
    skills: ['Modélisation financière', 'Bloomberg', 'Excel', 'IFRS'],
    description: 'Analysez la viabilité financière des projets de financement corporate.',
    sector: 'Banque & Services financiers', experience: '2-5 ans', education: 'Bac+5 Finance',
    postedDaysAgo: 9, applicants: 44, featured: false, saved: false,
  },
  {
    id: 'j12', title: 'Développeur Full Stack', companyId: 'c6', company: 'StartupCMR',
    logo: '🚀', location: 'Yaoundé', country: 'Cameroun', type: 'CDI',
    salary: { min: 450_000, max: 800_000, currency: 'FCFA' }, remote: true,
    skills: ['Vue.js', 'Java Spring Boot', 'PostgreSQL', 'REST API'],
    description: 'Développez des fonctionnalités end-to-end sur notre plateforme SaaS B2B.',
    sector: 'Informatique & Développement logiciel', experience: '2-4 ans', education: 'Bac+3/5 Informatique',
    postedDaysAgo: 1, applicants: 27, featured: true, saved: false,
  },
  {
    id: 'j13', title: 'Consultant SAP FI/CO', companyId: 'c4', company: 'Deloitte Afrique Centrale',
    logo: '🔵', location: 'Douala', country: 'Cameroun', type: 'CDI',
    salary: { min: 500_000, max: 900_000, currency: 'FCFA' }, remote: false,
    skills: ['SAP FI', 'SAP CO', 'ABAP', 'Gestion de projet'],
    description: 'Implémentez et optimisez les modules SAP Finance pour nos clients grands comptes.',
    sector: 'Conseil & Management', experience: '4-7 ans', education: 'Bac+5 Informatique/Finance',
    postedDaysAgo: 14, applicants: 12, featured: false, saved: false,
  },
  {
    id: 'j14', title: 'Responsable Marketing Digital', companyId: 'c2', company: 'MTN Cameroun',
    logo: '🟡', location: 'Douala', country: 'Cameroun', type: 'CDI',
    salary: { min: 350_000, max: 600_000, currency: 'FCFA' }, remote: false,
    skills: ['SEO', 'SEA', 'CRM', 'Analytics', 'Email marketing'],
    description: 'Définissez et exécutez la stratégie digitale de la marque MTN au Cameroun.',
    sector: 'Publicité & Communication', experience: '3-5 ans', education: 'Bac+4/5 Marketing',
    postedDaysAgo: 11, applicants: 58, featured: false, saved: false,
  },
  {
    id: 'j15', title: 'Ingénieur Réseau & Sécurité', companyId: 'c1', company: 'Orange Cameroun',
    logo: '🟠', location: 'Douala', country: 'Cameroun', type: 'CDI',
    salary: { min: 400_000, max: 700_000, currency: 'FCFA' }, remote: false,
    skills: ['Cisco', 'Firewall', 'VPN', 'MPLS', 'Cybersécurité'],
    description: "Administrez et sécurisez l'infrastructure réseau nationale d'Orange.",
    sector: 'Cybersécurité', experience: '3-5 ans', education: 'Bac+4/5 Réseaux',
    postedDaysAgo: 16, applicants: 30, featured: false, saved: false,
  },
]

// ─── Saved jobs (for candidate) ───────────────────────────────────────────────
export const SAVED_JOBS = [
  { ...JOBS[0], saved: true, savedAt: '2025-12-18' },
  { ...JOBS[4], saved: true, savedAt: '2025-12-15' },
  { ...JOBS[11], saved: true, savedAt: '2025-12-10' },
]

// ─── Applications ─────────────────────────────────────────────────────────────
export const APPLICATIONS = [
  {
    id: 'a1', jobId: 'j3', jobTitle: 'Responsable Comptabilité', company: 'Société Générale Cameroun',
    logo: '🔴', location: 'Douala', salary: '350k-550k FCFA', type: 'CDI',
    appliedDate: '2025-12-10', status: 'En cours', stage: 'Entretien RH',
    stageIndex: 2,
  },
  {
    id: 'a2', jobId: 'j7', jobTitle: 'Auditeur Interne', company: 'Deloitte Afrique Centrale',
    logo: '🔵', location: 'Douala', salary: '450k-700k FCFA', type: 'CDI',
    appliedDate: '2025-11-28', status: 'En attente', stage: 'CV transmis',
    stageIndex: 1,
  },
  {
    id: 'a3', jobId: 'j14', jobTitle: 'Responsable Marketing Digital', company: 'MTN Cameroun',
    logo: '🟡', location: 'Douala', salary: '350k-600k FCFA', type: 'CDI',
    appliedDate: '2025-11-15', status: 'Refusé', stage: 'Refus suite entretien',
    stageIndex: 0,
  },
  {
    id: 'a4', jobId: 'j1', jobTitle: 'Développeur React Senior', company: 'StartupCMR',
    logo: '🚀', location: 'Yaoundé', salary: '600k-900k FCFA', type: 'CDI',
    appliedDate: '2025-12-16', status: 'En cours', stage: 'Test technique',
    stageIndex: 2,
  },
]

// ─── Interviews ───────────────────────────────────────────────────────────────
export const INTERVIEWS = [
  {
    id: 'i1', jobTitle: 'Développeur React Senior', company: 'StartupCMR', logo: '🚀',
    date: '2026-01-15', time: '10:00', type: 'Visioconférence', platform: 'Google Meet',
    status: 'Confirmé', contact: 'Mme. Béatrice ENOW (RH)',
    notes: 'Préparer une présentation d\'un projet React personnel (10 min).',
  },
  {
    id: 'i2', jobTitle: 'Responsable Comptabilité', company: 'Société Générale Cameroun', logo: '🔴',
    date: '2026-01-20', time: '14:30', type: 'Présentiel', platform: null,
    status: 'Confirmé', contact: 'M. Pierre MVONDO (DG)',
    notes: 'Apporter CV imprimé + diplômes + attestations de travail.',
  },
]

// ─── Archives ─────────────────────────────────────────────────────────────────
export const ARCHIVES = [
  {
    id: 'ar1', jobTitle: 'Community Manager', company: 'Jumia Cameroun', logo: '🟢',
    appliedDate: '2025-09-10', closedDate: '2025-10-05', reason: 'Offre pourvue',
  },
  {
    id: 'ar2', jobTitle: 'Chargé RH & Recrutement', company: 'Orange Cameroun', logo: '🟠',
    appliedDate: '2025-08-22', closedDate: '2025-09-18', reason: 'Refus après test',
  },
]

// ─── Search results helper ────────────────────────────────────────────────────
export function searchJobs(query = '', location = '', filters = {}) {
  const q = query.toLowerCase().trim()
  const loc = location.toLowerCase().trim()

  return JOBS.filter(job => {
    const matchQ = !q || job.title.toLowerCase().includes(q)
      || job.company.toLowerCase().includes(q)
      || job.skills.some(s => s.toLowerCase().includes(q))
      || job.sector.toLowerCase().includes(q)
    const matchLoc = !loc || job.location.toLowerCase().includes(loc) || job.country.toLowerCase().includes(loc)
    const matchType = !filters.type || job.type === filters.type
    const matchRemote = !filters.remote || job.remote === true
    const matchSalaryMin = !filters.salaryMin || job.salary.max >= filters.salaryMin
    const matchSector = !filters.sector || job.sector === filters.sector
    return matchQ && matchLoc && matchType && matchRemote && matchSalaryMin && matchSector
  }).sort((a, b) => {
    if (filters.sort === 'salary') return b.salary.max - a.salary.max
    return a.postedDaysAgo - b.postedDaysAgo
  })
}

// ─── STELLA chatbot responses ─────────────────────────────────────────────────
export const STELLA_RESPONSES = {
  salut: "Bonjour ! Je suis STELLA, votre assistante IA SkillSet 🌟 Comment puis-je vous aider aujourd'hui ?",
  aide: "Je peux vous aider à :\n• Trouver des offres d'emploi adaptées\n• Estimer votre salaire\n• Préparer votre CV\n• Simuler des entretiens\n• Comparer des entreprises\n\nQue souhaitez-vous faire ?",
  salaire: "💰 Pour estimer votre salaire, dites-moi votre poste et votre ville. Ex: *\"Quel salaire pour un développeur web à Douala ?\"*",
  emploi: "🔍 Vous cherchez un emploi ? Dites-moi votre domaine et je vous ferai des recommandations personnalisées !",
  entretien: "🎯 Pour préparer votre entretien :\n1. Renseignez-vous sur l'entreprise\n2. Préparez vos exemples STAR\n3. Soignez votre tenue\n4. Posez des questions pertinentes\n\nVoulez-vous que je vous fasse une simulation ?",
  cv: "📄 Un bon CV doit avoir :\n• Un résumé percutant\n• Des expériences quantifiées\n• Les compétences clés\n• Une mise en page claire\n\nVoulez-vous des conseils personnalisés ?",
  fallback: "Je n'ai pas bien compris votre demande 😊 Essayez de me parler de votre recherche d'emploi, de votre salaire ou de votre entretien !",
}
