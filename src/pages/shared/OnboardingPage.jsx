import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../../store/AuthStore'
import { generateQuestions, generateCv, completeOnboarding } from '../../api/OnboardingApi'
import { ChevronRight, ChevronLeft, CheckCircle2, X } from 'lucide-react'
import MapPicker from '../../components/MapPicker'
import StellaLoader from '../../components/StellaLoader'
import PhaseIndicator from '../../features/onboarding/PhaseIndicator'
import CvGenerationScreen from '../../features/onboarding/CvGenerationScreen'
import { getJobsForDomain } from '../../data/domainJobs'
import { useTranslation } from '../../i18n/translations'
import './OnboardingPage.css'

/* ─── Static data (reference lists — kept in French) ─────────────── */
const INDUSTRIES = [
  'Informatique & Développement logiciel', 'Intelligence Artificielle & Machine Learning',
  'Cybersécurité', 'Cloud Computing', 'E-commerce & Marketplaces',
  'Fintech & Paiements digitaux', 'Télécommunications', 'Banque & Services financiers',
  'Assurance', 'Comptabilité & Audit', "Gestion d'actifs & Investissements",
  'Santé & Médecine', 'Pharmacie & Biotechnologies', 'Matériel médical',
  'Éducation & Formation', 'E-learning & EdTech', 'Recherche & Développement',
  'Commerce de détail & Distribution', 'Vente & Marketing', 'Publicité & Communication',
  'Industrie manufacturière', 'Automobile & Mobilité', 'Aéronautique & Spatial',
  'Chimie & Matériaux', 'Textile & Mode', 'Construction & BTP', 'Immobilier',
  'Architecture & Design', 'Ingénierie civile', 'Conseil & Management',
  'Ressources Humaines', 'Juridique & Droit', 'Agriculture & Agroalimentaire',
  'Pêche & Aquaculture', 'Énergie & Utilities', 'Énergies renouvelables',
  'Pétrole & Gaz', 'Mines & Ressources naturelles', 'Transport & Logistique',
  'Maritime & Ports', 'Aviation', 'Tourisme & Hôtellerie', 'Restauration',
  'Événementiel', 'Médias & Presse', 'Production audiovisuelle', 'Arts & Culture',
  'Sport & Loisirs', 'ONG & Associations', 'Administration publique',
  'Sécurité & Défense', 'Artisanat & Métiers manuels', 'Beauté & Bien-être',
  'Boulangerie, Pâtisserie & Métiers de bouche', 'Nettoyage & Services à la personne',
  'Autre',
]

const COUNTRIES = [
  { name: 'Cameroun', code: 'cm' }, { name: 'Sénégal', code: 'sn' },
  { name: "Côte d'Ivoire", code: 'ci' }, { name: 'Mali', code: 'ml' },
  { name: 'Guinée', code: 'gn' }, { name: 'Burkina Faso', code: 'bf' },
  { name: 'Niger', code: 'ne' }, { name: 'Bénin', code: 'bj' },
  { name: 'Togo', code: 'tg' }, { name: 'Congo (RDC)', code: 'cd' },
  { name: 'Congo (Brazzaville)', code: 'cg' }, { name: 'Gabon', code: 'ga' },
  { name: 'Madagascar', code: 'mg' }, { name: 'Maroc', code: 'ma' },
  { name: 'Algérie', code: 'dz' }, { name: 'Tunisie', code: 'tn' },
  { name: 'Mauritanie', code: 'mr' }, { name: 'Nigeria', code: 'ng' },
  { name: 'Ghana', code: 'gh' }, { name: 'Kenya', code: 'ke' },
  { name: 'Tanzanie', code: 'tz' }, { name: 'Afrique du Sud', code: 'za' },
  { name: 'Éthiopie', code: 'et' }, { name: 'Rwanda', code: 'rw' },
  { name: 'France', code: 'fr' }, { name: 'Belgique', code: 'be' },
  { name: 'Suisse', code: 'ch' }, { name: 'Luxembourg', code: 'lu' },
  { name: 'Espagne', code: 'es' }, { name: 'Portugal', code: 'pt' },
  { name: 'Italie', code: 'it' }, { name: 'Allemagne', code: 'de' },
  { name: 'Royaume-Uni', code: 'gb' }, { name: 'Pays-Bas', code: 'nl' },
  { name: 'Canada', code: 'ca' }, { name: 'États-Unis', code: 'us' },
  { name: 'Brésil', code: 'br' }, { name: 'Émirats Arabes Unis', code: 'ae' },
  { name: 'Qatar', code: 'qa' }, { name: 'Inde', code: 'in' },
  { name: 'Chine', code: 'cn' }, { name: 'Japon', code: 'jp' },
  { name: 'Australie', code: 'au' }, { name: 'Autre', code: '' },
]

function getCountryCode(name) {
  return COUNTRIES.find(c => c.name === name)?.code || ''
}

const OPTIONAL_IDS = [
  'companyLinkedIn', 'companyDescription', 'companyLocation', 'companyAddress',
  'candidateLinkedIn', 'workAuthorization', 'hasReferences',
]

/* ─── Question builders (initial, role-specific) ────────────────── */
function getCandidateInitialQuestions(answers, t) {
  const domain = answers.domain || ''
  const domainJobs = getJobsForDomain(domain)

  return [
    {
      id: 'domain',
      text: t.questions.domain,
      type: 'searchable_choice',
      options: INDUSTRIES,
      placeholder: t.questions.domainPlaceholder,
    },
    {
      id: 'desiredRole',
      text: domain
        ? `${t.questions.desiredRoleFor} ${domain.split('&')[0].trim()} »`
        : t.questions.desiredRole,
      type: domainJobs.length > 0 ? 'searchable_choice' : 'text',
      options: domainJobs,
      placeholder: t.questions.desiredRolePlaceholder,
    },
    {
      id: 'experienceLevel',
      text: t.questions.experienceLevel,
      type: 'single_choice',
      options: t.questions.experienceLevels,
    },
    {
      id: 'contractType',
      text: t.questions.contractTypeCandidate,
      type: 'multi_choice',
      options: t.questions.contractTypes,
    },
    {
      id: 'candidateLocation',
      type: 'country_city',
      text: t.questions.candidateLocation,
      countryId: 'candidateCountry',
      cityId: 'candidateCity',
    },
    {
      id: 'candidateLinkedIn',
      text: t.questions.candidateLinkedIn,
      type: 'text',
      placeholder: 'https://linkedin.com/in/votre-profil',
    },
    {
      id: 'workAuthorization',
      text: `${t.questions.workAuthorization}${answers.candidateCountry ? t.questions.workAuthorizationIn + answers.candidateCountry : t.questions.workAuthorizationFallback} ?`,
      type: 'single_choice',
      options: t.questions.workAuthorizationOptions,
    },
    {
      id: 'hasReferences',
      text: t.questions.hasReferences,
      type: 'single_choice',
      options: t.questions.hasReferencesOptions,
    },
  ]
}

function getEmployerInitialQuestions(answers, t) {
  const industry = answers.industry || ''
  const countryName = answers.companyCountry || ''

  return [
    {
      id: 'companyName',
      text: t.questions.companyName,
      type: 'text',
      placeholder: t.questions.companyNamePlaceholder,
    },
    {
      id: 'industry',
      text: t.questions.industry,
      type: 'searchable_choice',
      options: INDUSTRIES,
      placeholder: t.questions.industryPlaceholder,
    },
    {
      id: 'companyLocation',
      type: 'country_city',
      text: t.questions.companyLocation,
      countryId: 'companyCountry',
      cityId: 'companyCity',
    },
    {
      id: 'companySize',
      text: t.questions.companySize,
      type: 'single_choice',
      options: t.questions.companySizeOptions,
    },
    {
      id: 'hiringRole',
      text: industry
        ? `${t.questions.hiringRoleFor} ${industry.split('&')[0].trim()} »`
        : t.questions.hiringRole,
      type: 'text',
      placeholder: t.questions.hiringRolePlaceholder,
    },
    {
      id: 'contractType',
      text: t.questions.contractTypeEmployer,
      type: 'multi_choice',
      options: t.questions.contractTypes,
    },
    {
      id: 'companyDescription',
      text: t.questions.companyDescription,
      type: 'text',
      placeholder: t.questions.companyDescriptionPlaceholder,
    },
    {
      id: 'companyRegistrationNumber',
      text: countryName === 'France' ? t.questions.registrationNumberFrance : t.questions.registrationNumberOther,
      type: 'text',
      placeholder: countryName === 'France' ? t.questions.registrationPlaceholderFrance : t.questions.registrationPlaceholderOther,
    },
    {
      id: 'companyWebsite',
      text: t.questions.companyWebsite,
      type: 'text',
      placeholder: 'https://monentreprise.com',
    },
    {
      id: 'companyLinkedIn',
      text: t.questions.companyLinkedIn,
      type: 'text',
      placeholder: 'https://linkedin.com/company/monentreprise',
    },
    {
      id: 'companyAddress',
      text: t.questions.companyAddress,
      type: 'text',
      placeholder: t.questions.companyAddressPlaceholder,
    },
    {
      id: 'companyMap',
      text: t.questions.companyMap,
      type: 'map',
    },
  ]
}

/* ─── Searchable choice ─────────────────────────────────────────── */
function SearchableChoiceCard({ question, value, onChange, t }) {
  const [search, setSearch] = useState('')
  const [open, setOpen] = useState(false)
  const options = question.options || []

  const filtered = search.length > 0
    ? options.filter(o => o.toLowerCase().includes(search.toLowerCase()))
    : options

  const handleSelect = (opt) => { onChange(question.id, opt); setSearch(''); setOpen(false) }
  const handleClear  = () => onChange(question.id, '')

  return (
    <div className="ob-question ob-dropdown-wrapper">
      <p className="ob-question-text">{question.text}</p>
      {value ? (
        <div className="ob-selected-chip">
          <span>{value}</span>
          <button type="button" onClick={handleClear} aria-label={t.remove}><X size={14} /></button>
        </div>
      ) : (
        <div className="ob-dropdown-container">
          <input
            className="ob-input"
            value={search}
            onChange={e => { setSearch(e.target.value); setOpen(true) }}
            onFocus={() => setOpen(true)}
            onBlur={() => setTimeout(() => setOpen(false), 200)}
            placeholder={question.placeholder || t.searchPlaceholder}
          />
          {open && (
            <div className="ob-dropdown">
              {filtered.slice(0, 14).map((opt, i) => (
                <div key={i} className="ob-dropdown-item" onMouseDown={() => handleSelect(opt)}>{opt}</div>
              ))}
              {search.length > 1 && !options.find(o => o.toLowerCase() === search.toLowerCase()) && (
                <div className="ob-dropdown-item ob-dropdown-item--custom" onMouseDown={() => handleSelect(search)}>
                  {t.useCustom} {search} »
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  )
}

/* ─── Country + City combined ────────────────────────────────────── */
function CountryCityCard({ question, answers, onChange, t }) {
  const countryVal = answers[question.countryId] || ''
  const cityVal    = answers[question.cityId]    || ''
  const countryCode = getCountryCode(countryVal)

  const [cSearch, setCSearch] = useState('')
  const [cOpen, setCOpen]     = useState(false)
  const filteredCountries = COUNTRIES.filter(c =>
    c.name.toLowerCase().includes(cSearch.toLowerCase())
  )

  const [cityInput, setCityInput]   = useState(cityVal || '')
  const [citySugs, setCitySugs]     = useState([])
  const [cityOpen, setCityOpen]     = useState(false)
  const debounceRef = useRef(null)

  useEffect(() => {
    const timer = setTimeout(() => {
      if (!cityVal) { setCityInput(''); setCitySugs([]) }
      else setCityInput(cityVal)
    }, 0)
    return () => clearTimeout(timer)
  }, [cityVal, countryCode])

  const fetchCities = async (q) => {
    if (q.length < 2) { setCitySugs([]); return }
    const url = `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(q)}&countrycodes=${countryCode}&format=json&limit=8&accept-language=fr`
    try {
      const data = await (await fetch(url)).json()
      const cities = data.map(d => d.display_name.split(',')[0].trim())
        .filter((v, i, a) => a.indexOf(v) === i).slice(0, 7)
      setCitySugs(cities)
      setCityOpen(true)
    } catch { setCitySugs([]) }
  }

  const handleCountrySelect = (c) => {
    const countryName = c.code === '' ? (cSearch || t.other) : c.name
    onChange(question.countryId, countryName)
    onChange(question.cityId, '')
    setCSearch('');
    setCOpen(false)
    setCityInput('')
    setCitySugs([])
  }

  const handleCityInput = (e) => {
    const val = e.target.value
    setCityInput(val)
    onChange(question.cityId, val)
    clearTimeout(debounceRef.current)
    if (val.length >= 2) {
      debounceRef.current = setTimeout(() => fetchCities(val), 380)
    } else {
      setCitySugs([])
    }
  }

  const handleCitySelect = (city) => {
    setCityInput(city)
    onChange(question.cityId, city)
    setCitySugs([]);
    setCityOpen(false)
  }

  return (
    <div className="ob-question">
      <p className="ob-question-text">{question.text}</p>

      {/* Country row */}
      <div className="ob-location-row">
        <span className="ob-location-label">{t.country}</span>
        {countryVal ? (
          <div className="ob-selected-chip ob-selected-chip--inline">
            <span>{countryVal}</span>
            <button type="button" onClick={() => { onChange(question.countryId, ''); onChange(question.cityId, ''); setCityInput(''); setCitySugs([]); }} aria-label={t.changeCountry}><X size={13} /></button>
          </div>
        ) : (
          <div className="ob-dropdown-container ob-dropdown-container--flex">
            <input
              className="ob-input"
              value={cSearch}
              onChange={e => { setCSearch(e.target.value); setCOpen(true) }}
              onFocus={() => setCOpen(true)}
              onBlur={() => setTimeout(() => setCOpen(false), 200)}
              placeholder={t.choosCountry}
            />
            {cOpen && (
              <div className="ob-dropdown">
                {filteredCountries.slice(0, 10).map((c, i) => (
                  <div key={i} className="ob-dropdown-item" onMouseDown={() => handleCountrySelect(c)}>{c.name}</div>
                ))}
                {cSearch.length > 1 && !COUNTRIES.find(c => c.name.toLowerCase() === cSearch.toLowerCase()) && (
                  <div className="ob-dropdown-item ob-dropdown-item--custom" onMouseDown={() => handleCountrySelect({ name: cSearch, code: '' })}>
                    {t.useCustom} {cSearch} »
                  </div>
                )}
              </div>
            )}
          </div>
        )}
      </div>

      {/* City row — only shown when country is selected */}
      {countryVal && (
        <div className="ob-location-row ob-location-row--city">
          <span className="ob-location-label">{t.city}</span>
          <div className="ob-dropdown-container ob-dropdown-container--flex">
            <input
              className="ob-input"
              value={cityInput}
              onChange={handleCityInput}
              onBlur={() => setTimeout(() => setCityOpen(false), 200)}
              onFocus={() => citySugs.length > 0 && setCityOpen(true)}
              placeholder={t.cityPlaceholder}
            />
            {cityOpen && citySugs.length > 0 && (
              <div className="ob-dropdown">
                {citySugs.map((city, i) => (
                  <div key={i} className="ob-dropdown-item" onMouseDown={() => handleCitySelect(city)}>{city}</div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}

/* ─── Single choice ─────────────────────────────────────────────── */
function SingleChoiceCard({ question, value, onChange, t }) {
  const options = question.options || []
  const hasOther = options.includes(t.other)
  const stdOpts  = hasOther ? options.filter(o => o !== t.other) : options
  const isCustom = value && !options.includes(value)
  const isOtherActive = value === t.other || isCustom
  const otherText = isCustom ? value : ''

  return (
    <div className="ob-question">
      <p className="ob-question-text">{question.text}</p>
      <div className="ob-choices">
        {stdOpts.map(opt => (
          <button key={opt} type="button"
            className={`ob-choice ${value === opt ? 'ob-choice--active' : ''}`}
            onClick={() => onChange(question.id, opt)}>{opt}</button>
        ))}
        {hasOther && (
          <button type="button"
            className={`ob-choice ${isOtherActive ? 'ob-choice--active' : ''}`}
            onClick={() => onChange(question.id, isOtherActive ? '' : t.other)}>{t.other}</button>
        )}
      </div>
      {isOtherActive && (
        <input className="ob-input ob-other-input" placeholder={t.specify}
          value={otherText} autoFocus
          onChange={e => onChange(question.id, e.target.value || t.other)} />
      )}
    </div>
  )
}

/* ─── Multi choice ──────────────────────────────────────────────── */
function MultiChoiceCard({ question, value, onChange, t }) {
  const options  = question.options || []
  const hasOther = options.includes(t.other)
  const stdOpts  = hasOther ? options.filter(o => o !== t.other) : options
  const selected = value ? value.split(',').filter(Boolean) : []
  const stdSel   = selected.filter(v => stdOpts.includes(v))
  const customVals = selected.filter(v => !options.includes(v))
  const isOtherActive = selected.includes(t.other) || customVals.length > 0
  const customText = customVals[0] || ''

  const build = (std, otherOn, text) => {
    if (otherOn && text) return [...std, text].join(',')
    if (otherOn) return [...std, t.other].join(',')
    return std.join(',')
  }

  const toggleChip = (opt) => {
    const newStd = stdSel.includes(opt) ? stdSel.filter(v => v !== opt) : [...stdSel, opt]
    onChange(question.id, build(newStd, isOtherActive, customText))
  }

  return (
    <div className="ob-question">
      <p className="ob-question-text">{question.text}</p>
      <p className="ob-question-hint">{t.multiChoiceHint}</p>
      <div className="ob-choices ob-choices--multi">
        {stdOpts.map(opt => (
          <button key={opt} type="button"
            className={`ob-choice ${stdSel.includes(opt) ? 'ob-choice--active' : ''}`}
            onClick={() => toggleChip(opt)}>{opt}</button>
        ))}
        {hasOther && (
          <button type="button"
            className={`ob-choice ${isOtherActive ? 'ob-choice--active' : ''}`}
            onClick={() => onChange(question.id, build(stdSel, !isOtherActive, ''))}>{t.other}</button>
        )}
      </div>
      {isOtherActive && (
        <input className="ob-input ob-other-input" placeholder={t.specify}
          value={customText} autoFocus
          onChange={e => onChange(question.id, build(stdSel, true, e.target.value))} />
      )}
    </div>
  )
}

/* ─── Question dispatcher — shared by initial AND AI-generated questions ── */
function QuestionCard({ question, value, answers, onChange, onSideEffect, t }) {
  if (question.type === 'country_city') {
    return (
      <CountryCityCard
        question={question}
        answers={answers}
        onChange={(id, val) => onChange(id, val)}
        t={t}
      />
    )
  }
  if (question.type === 'searchable_choice') {
    return <SearchableChoiceCard question={question} value={value} onChange={onChange} t={t} />
  }
  if (question.type === 'single_choice') {
    return <SingleChoiceCard question={question} value={value} onChange={onChange} t={t} />
  }
  if (question.type === 'multi_choice') {
    return <MultiChoiceCard question={question} value={value} onChange={onChange} t={t} />
  }
  if (question.type === 'text' || question.type === 'number') {
    return (
      <div className="ob-question">
        <p className="ob-question-text">{question.text}</p>
        <input
          className="ob-input"
          type={question.type === 'number' ? 'number' : 'text'}
          placeholder={question.placeholder || ''}
          value={value || ''}
          onChange={e => onChange(question.id, e.target.value)}
        />
      </div>
    )
  }
  if (question.type === 'map') {
    return (
      <div className="ob-question">
        <p className="ob-question-text">{question.text}</p>
        <MapPicker
          value={value || ''}
          onChange={val => onChange(question.id, val)}
          onAddressFound={address => onSideEffect && onSideEffect('companyAddress', address)}
        />
      </div>
    )
  }
  return null
}

/* ─── Constants ─────────────────────────────────────────────────── */
const ROLE_ROUTES = { CANDIDATE: '/dashboard/candidate', EMPLOYER: '/dashboard/employer' }

/**
 * Builds the PreviousAnswerDTO-shaped list the backend expects for CV
 * generation, from the combined initial + AI answers. Country/city are
 * normalized to fieldKey "country"/"city" (regardless of the role-specific
 * question id they came from) since that's the literal key the backend's
 * buildContext() looks for to drive country-aware CV formatting.
 */
function buildAnswerList(initialQuestions, initialAnswers, aiQuestions, aiAnswers) {
  const fromInitial = initialQuestions.flatMap(q => {
    if (q.type === 'country_city') {
      const entries = []
      if (initialAnswers[q.countryId]) {
        entries.push({ fieldKey: 'country', question: q.text, answer: initialAnswers[q.countryId], phase: 'INITIAL' })
      }
      if (initialAnswers[q.cityId]) {
        entries.push({ fieldKey: 'city', question: q.text, answer: initialAnswers[q.cityId], phase: 'INITIAL' })
      }
      return entries
    }
    const val = initialAnswers[q.id]
    return (val !== undefined && val !== '')
      ? [{ fieldKey: q.id, question: q.text, answer: val, phase: 'INITIAL' }]
      : []
  })

  const fromAi = aiQuestions.flatMap(q => {
    const val = aiAnswers[q.id]
    return (val !== undefined && val !== '')
      ? [{ fieldKey: q.id, question: q.text, answer: val, phase: 'AI' }]
      : []
  })

  return [...fromInitial, ...fromAi]
}

/* ─── Main component ────────────────────────────────────────────── */
export default function OnboardingPage() {
  const navigate = useNavigate()
  const { user, setAuth } = useAuthStore()
  const role = user?.role || 'CANDIDATE'
  const t = useTranslation().onboarding

  useEffect(() => {
    if (user?.onboardingCompleted) {
      navigate(ROLE_ROUTES[role] ?? '/dashboard/candidate', { replace: true })
    }
  }, [user, role, navigate])

  // 'initial' | 'ai-init' | 'ai' | 'cv-gen' | 'cv' | 'done'
  const [phase, setPhase] = useState('initial')
  const [initialAnswers, setInitialAnswers] = useState({})
  const [initialIndex, setInitialIndex] = useState(0)
  const [aiQuestions, setAiQuestions] = useState([])
  const [aiIndex, setAiIndex] = useState(0)
  const [aiAnswers, setAiAnswers] = useState({})
  const [cvUrl, setCvUrl] = useState(null)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const initialQuestions = role === 'EMPLOYER'
    ? getEmployerInitialQuestions(initialAnswers, t)
    : getCandidateInitialQuestions(initialAnswers, t)

  const setInitialAnswer = (id, val) => {
    setInitialAnswers(prev => {
      const next = { ...prev, [id]: val }
      if (id === 'companyCountry' && prev.companyCountry !== val) next.companyCity = ''
      if (id === 'candidateCountry' && prev.candidateCountry !== val) next.candidateCity = ''
      if (id === 'domain' && prev.domain !== val) next.desiredRole = ''
      return next
    })
  }

  const handleChange = (id, val) => {
    if (phase === 'ai') {
      setAiAnswers(prev => ({ ...prev, [id]: val }))
    } else {
      setInitialAnswer(id, val)
    }
  }

  const isInitialQuestionAnswered = (question) => {
    if (!question) return false
    if (OPTIONAL_IDS.includes(question.id)) return true
    if (question.type === 'country_city') {
      return Boolean(initialAnswers[question.countryId] && String(initialAnswers[question.countryId]).trim())
    }
    const val = initialAnswers[question.id]
    return Boolean(val && String(val).trim() !== '')
  }

  const isAiQuestionAnswered = (question) => {
    if (!question) return false
    const val = aiAnswers[question.id]
    return Boolean(val && String(val).trim() !== '')
  }

  const currentInitialQuestion = initialQuestions[initialIndex]
  const currentAiQuestion = aiQuestions[aiIndex]
  const currentQuestion = phase === 'ai' ? currentAiQuestion : currentInitialQuestion

  const currentValue = phase === 'ai'
    ? (aiAnswers[currentAiQuestion?.id] || '')
    : (initialAnswers[currentInitialQuestion?.id] || '')

  const totalSteps = initialQuestions.length + aiQuestions.length
  const currentStepNum = phase === 'ai'
    ? initialQuestions.length + aiIndex + 1
    : initialIndex + 1

  const startAiPhase = async () => {
    setError('')
    setPhase('ai-init')
    try {
      const data = await generateQuestions(role, initialAnswers)
      const questions = data.questions || []
      setAiQuestions(questions)
      setAiIndex(0)
      if (questions.length === 0) {
        await finishFlow(questions)
        return
      }
      setPhase('ai')
    } catch (err) {
      console.error(err)
      setError(t.genericError)
      setPhase('initial')
    }
  }

  const handleComplete = async (withCv = false) => {
    setSubmitting(true)
    setError('')
    try {
      const data = await completeOnboarding(role, initialAnswers, aiAnswers, withCv)
      setAuth(data)
      setPhase('done')
      setTimeout(() => navigate(ROLE_ROUTES[role] ?? '/dashboard/candidate'), 1800)
    } catch (err) {
      setError(t.genericError)
      console.error(err)
    } finally {
      setSubmitting(false)
    }
  }

  const finishFlow = async (questionsOverride) => {
    const questions = questionsOverride ?? aiQuestions
    if (role === 'CANDIDATE') {
      setPhase('cv-gen')
      try {
        const jobTitle = initialAnswers.desiredRole || 'Non spécifié'
        const answers = buildAnswerList(initialQuestions, initialAnswers, questions, aiAnswers)
        const cvData = await generateCv(jobTitle, answers)
        setCvUrl(cvData.cvUrl)
        setPhase('cv')
      } catch (err) {
        console.error(err)
        setError(t.cvGenError)
        setPhase('ai')
      }
    } else {
      await handleComplete(false)
    }
  }

  const handleNext = async () => {
    setError('')
    if (phase === 'initial') {
      if (!isInitialQuestionAnswered(currentInitialQuestion)) {
        setError(t.mandatoryFields)
        return
      }
      if (initialIndex < initialQuestions.length - 1) {
        setInitialIndex(i => i + 1)
      } else {
        await startAiPhase()
      }
      return
    }
    if (phase === 'ai') {
      if (!isAiQuestionAnswered(currentAiQuestion)) {
        setError(t.answerRequired)
        return
      }
      if (aiIndex < aiQuestions.length - 1) {
        setAiIndex(i => i + 1)
      } else {
        await finishFlow()
      }
    }
  }

  const handleBack = () => {
    setError('')
    if (phase === 'ai') {
      if (aiIndex > 0) { setAiIndex(i => i - 1); return }
      setPhase('initial')
      setInitialIndex(initialQuestions.length - 1)
      return
    }
    if (phase === 'initial' && initialIndex > 0) {
      setInitialIndex(i => i - 1)
    }
  }

  const handleCvComplete = async () => {
    await handleComplete(true)
  }

  /* ── Loading ── */
  if (phase === 'ai-init') return (
    <StellaLoader message={t.aiInitMessage} sub={t.aiInitSub} />
  )

  if (phase === 'cv-gen') return (
    <StellaLoader message={t.cvGenMessage} sub={t.cvGenSub} />
  )

  /* ── Done ── */
  if (phase === 'done') {
    return (
      <div className="ob-shell">
        <div className="ob-done-card">
          <CheckCircle2 size={56} className="ob-done-icon" />
          <h2>{t.profileDoneTitle}</h2>
          <p>{t.profileDoneSub}</p>
          <button
            type="button"
            className="ob-btn-primary"
            onClick={() => navigate(ROLE_ROUTES[role] ?? '/dashboard/candidate', { replace: true })}
          >
            {t.goToDashboard} <ChevronRight size={18} />
          </button>
        </div>
      </div>
    )
  }

  /* ── CV Generation success screen ── */
  if (phase === 'cv') {
    return (
      <CvGenerationScreen
        cvUrl={cvUrl}
        country={initialAnswers.candidateCountry}
        onDownload={() => { /* track if needed */ }}
        onClose={() => handleCvComplete()}
      />
    )
  }

  /* ── One continuous question-at-a-time flow (initial + AI-generated) ── */
  const isLastAiQuestion = phase === 'ai' && aiIndex === aiQuestions.length - 1
  const nextLabel = isLastAiQuestion
    ? (role === 'CANDIDATE' ? t.generateCv : t.finish)
    : t.continue
  const canGoBack = phase === 'ai' ? true : initialIndex > 0

  return (
    <div className="ob-shell">
      <div className="ob-card">
        <div className="ob-card-header">
          <div className="ob-logo">
            <span className="ob-logo-icon">S</span>
            <span className="ob-logo-text">SkillSet</span>
          </div>
          <PhaseIndicator current={currentStepNum} total={totalSteps} />
        </div>

        <div className="ob-card-body">
          {phase === 'initial' && initialIndex === 0 && (
            <>
              <h2 className="ob-section-title">
                {role === 'CANDIDATE' ? t.candidateTitle : t.employerTitle}
              </h2>
              <p className="ob-section-sub">{t.sectionSub}</p>
            </>
          )}

          {error && <div className="ob-error">{error}</div>}

          {currentQuestion && (
            <div className="ob-questions">
              <QuestionCard
                question={currentQuestion}
                value={currentValue}
                answers={phase === 'ai' ? aiAnswers : initialAnswers}
                onChange={handleChange}
                onSideEffect={handleChange}
                t={t}
              />
            </div>
          )}
        </div>

        <div className="ob-card-footer">
          <button className="ob-btn-ghost" onClick={handleBack} disabled={!canGoBack}>
            <ChevronLeft size={16} /> {t.back}
          </button>
          <button className="ob-btn-primary" onClick={handleNext} disabled={submitting}>
            {nextLabel}
            <ChevronRight size={16} />
          </button>
        </div>
      </div>
    </div>
  )
}
