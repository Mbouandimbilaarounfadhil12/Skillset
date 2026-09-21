import { useState, useCallback, useEffect } from 'react'
import { Search, X, RotateCcw, ChevronDown } from 'lucide-react'
import { getSuggestions } from '../../api/SearchApi'

const CONTRACT_TYPES = [
  { value: 'CDI', label: 'CDI' },
  { value: 'CDD', label: 'CDD' },
  { value: 'Stage', label: 'Stage' },
  { value: 'Freelance', label: 'Freelance' },
]

const EXPERIENCE_LEVELS = [
  { value: '0', label: 'Junior (0-2 ans)' },
  { value: '2', label: 'Confirmé (2-5 ans)' },
  { value: '5', label: 'Senior (5+ ans)' },
]

export default function AdvancedSearchPanel({ onSearch, onReset }) {
  const [keywords, setKeywords] = useState('')
  const [location, setLocation] = useState('')
  const [contractType, setContractType] = useState('')
  const [minSalary, setMinSalary] = useState('')
  const [maxSalary, setMaxSalary] = useState('')
  const [experienceLevel, setExperienceLevel] = useState('')
  const [skills, setSkills] = useState([])
  const [skillInput, setSkillInput] = useState('')
  const [postedWithinDays, setPostedWithinDays] = useState('')
  const [suggestions, setSuggestions] = useState([])
  const [showSuggestions, setShowSuggestions] = useState(false)

  useEffect(() => {
    if (keywords.length > 2) {
      getSuggestions(keywords)
        .then(res => setSuggestions(res.data || []))
        .catch(() => setSuggestions([]))
    } else {
      setSuggestions([])
    }
  }, [keywords])

  const handleAddSkill = () => {
    if (skillInput.trim() && !skills.includes(skillInput.trim())) {
      setSkills([...skills, skillInput.trim()])
      setSkillInput('')
    }
  }

  const handleRemoveSkill = (skill) => {
    setSkills(skills.filter(s => s !== skill))
  }

  const handleSearch = () => {
    const criteria = {
      keywords: keywords || null,
      location: location || null,
      contractType: contractType || null,
      minSalary: minSalary ? parseInt(minSalary) : null,
      maxSalary: maxSalary ? parseInt(maxSalary) : null,
      experienceLevel: experienceLevel || null,
      skills: skills.length > 0 ? skills : null,
      postedWithinDays: postedWithinDays ? parseInt(postedWithinDays) : null,
      page: 0,
      size: 20,
      sortBy: 'RELEVANCE',
    }
    onSearch(criteria)
  }

  const handleReset = () => {
    setKeywords('')
    setLocation('')
    setContractType('')
    setMinSalary('')
    setMaxSalary('')
    setExperienceLevel('')
    setSkills([])
    setSkillInput('')
    setPostedWithinDays('')
    if (onReset) onReset()
  }

  return (
    <div className="asp-container">
      <div className="asp-header">
        <h2 className="asp-title">Recherche avancée</h2>
        <button className="asp-reset-btn" onClick={handleReset} title="Réinitialiser">
          <RotateCcw size={14} />
        </button>
      </div>

      <div className="asp-grid">
        {/* Keywords with autocomplete */}
        <div className="asp-field">
          <label className="asp-label">Mots-clés</label>
          <div className="asp-input-wrap">
            <Search size={14} className="asp-icon" />
            <input
              type="text"
              className="asp-input"
              placeholder="Titre, domaine, compétence…"
              value={keywords}
              onChange={e => setKeywords(e.target.value)}
              onFocus={() => setShowSuggestions(true)}
              onBlur={() => setTimeout(() => setShowSuggestions(false), 200)}
            />
            {showSuggestions && suggestions.length > 0 && (
              <div className="asp-suggestions">
                {suggestions.map((sugg, i) => (
                  <div
                    key={i}
                    className="asp-suggestion-item"
                    onClick={() => { setKeywords(sugg); setShowSuggestions(false) }}
                  >
                    {sugg}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Location */}
        <div className="asp-field">
          <label className="asp-label">Lieu</label>
          <input
            type="text"
            className="asp-input"
            placeholder="Ville, région, pays…"
            value={location}
            onChange={e => setLocation(e.target.value)}
          />
        </div>

        {/* Contract Type */}
        <div className="asp-field">
          <label className="asp-label">Type de contrat</label>
          <div className="asp-select-wrap">
            <select
              className="asp-select"
              value={contractType}
              onChange={e => setContractType(e.target.value)}
            >
              <option value="">— Tous —</option>
              {CONTRACT_TYPES.map(ct => (
                <option key={ct.value} value={ct.value}>{ct.label}</option>
              ))}
            </select>
            <ChevronDown size={13} className="asp-select-icon" />
          </div>
        </div>

        {/* Experience Level */}
        <div className="asp-field">
          <label className="asp-label">Expérience</label>
          <div className="asp-select-wrap">
            <select
              className="asp-select"
              value={experienceLevel}
              onChange={e => setExperienceLevel(e.target.value)}
            >
              <option value="">— Tous niveaux —</option>
              {EXPERIENCE_LEVELS.map(el => (
                <option key={el.value} value={el.value}>{el.label}</option>
              ))}
            </select>
            <ChevronDown size={13} className="asp-select-icon" />
          </div>
        </div>

        {/* Salary Range */}
        <div className="asp-field">
          <label className="asp-label">Salaire min</label>
          <input
            type="number"
            className="asp-input"
            placeholder="Ex: 30000"
            value={minSalary}
            onChange={e => setMinSalary(e.target.value)}
          />
        </div>

        <div className="asp-field">
          <label className="asp-label">Salaire max</label>
          <input
            type="number"
            className="asp-input"
            placeholder="Ex: 60000"
            value={maxSalary}
            onChange={e => setMaxSalary(e.target.value)}
          />
        </div>

        {/* Posted within days */}
        <div className="asp-field">
          <label className="asp-label">Posté depuis (jours)</label>
          <input
            type="number"
            className="asp-input"
            placeholder="Ex: 7"
            value={postedWithinDays}
            onChange={e => setPostedWithinDays(e.target.value)}
          />
        </div>
      </div>

      {/* Skills tags */}
      <div className="asp-skills-section">
        <label className="asp-label">Compétences</label>
        <div className="asp-skills-input">
          <input
            type="text"
            className="asp-input"
            placeholder="Ajouter une compétence…"
            value={skillInput}
            onChange={e => setSkillInput(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && (e.preventDefault(), handleAddSkill())}
          />
          <button className="asp-skill-add-btn" onClick={handleAddSkill}>
            Ajouter
          </button>
        </div>
        <div className="asp-skills-tags">
          {skills.map(skill => (
            <span key={skill} className="asp-skill-tag">
              {skill}
              <button
                className="asp-skill-remove"
                onClick={() => handleRemoveSkill(skill)}
              >
                <X size={12} />
              </button>
            </span>
          ))}
        </div>
      </div>

      {/* Action buttons */}
      <div className="asp-actions">
        <button className="asp-search-btn" onClick={handleSearch}>
          <Search size={15} /> Rechercher
        </button>
      </div>

      <style>{`
        .asp-container {
          background: var(--color-surface, #fff);
          border: 1px solid var(--color-border, #e5e7eb);
          border-radius: 12px;
          padding: 20px;
          margin-bottom: 24px;
        }
        .asp-header {
          display: flex;
          align-items: center;
          justify-content: space-between;
          margin-bottom: 16px;
        }
        .asp-title {
          font-size: 1rem;
          font-weight: 600;
          margin: 0;
          color: var(--color-text, #222);
        }
        .asp-reset-btn {
          width: 32px;
          height: 32px;
          display: flex;
          align-items: center;
          justify-content: center;
          border-radius: 8px;
          border: none;
          background: transparent;
          color: #888;
          cursor: pointer;
          transition: background 0.2s, color 0.2s;
        }
        .asp-reset-btn:hover {
          background: #f5f5f5;
          color: var(--color-text, #222);
        }
        .asp-grid {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
          gap: 12px;
          margin-bottom: 16px;
        }
        .asp-field {
          display: flex;
          flex-direction: column;
          gap: 6px;
        }
        .asp-label {
          font-size: 0.8rem;
          font-weight: 500;
          color: var(--color-text-muted, #666);
          text-transform: uppercase;
          letter-spacing: 0.5px;
        }
        .asp-input-wrap {
          position: relative;
          display: flex;
          align-items: center;
        }
        .asp-icon {
          position: absolute;
          left: 10px;
          color: #aaa;
          pointer-events: none;
        }
        .asp-input {
          padding: 9px 12px 9px 32px;
          border: 1.5px solid #ddd;
          border-radius: 8px;
          font-size: 0.9rem;
          background: var(--color-surface, #fff);
          color: var(--color-text, #222);
          width: 100%;
          box-sizing: border-box;
          outline: none;
          transition: border-color 0.2s;
        }
        .asp-input:focus {
          border-color: var(--color-primary, #6366f1);
        }
        .asp-suggestions {
          position: absolute;
          top: 100%;
          left: 0;
          right: 0;
          background: #fff;
          border: 1px solid #ddd;
          border-top: none;
          border-radius: 0 0 8px 8px;
          max-height: 120px;
          overflow-y: auto;
          z-index: 10;
        }
        .asp-suggestion-item {
          padding: 8px 12px;
          font-size: 0.85rem;
          color: #555;
          cursor: pointer;
          transition: background 0.14s;
          border-bottom: 1px solid #f0f0f0;
        }
        .asp-suggestion-item:hover {
          background: #f5f5f5;
        }
        .asp-suggestion-item:last-child {
          border-bottom: none;
        }
        .asp-select-wrap {
          position: relative;
          display: flex;
          align-items: center;
        }
        .asp-select {
          padding: 9px 12px;
          border: 1.5px solid #ddd;
          border-radius: 8px;
          font-size: 0.9rem;
          background: var(--color-surface, #fff);
          color: var(--color-text, #222);
          cursor: pointer;
          width: 100%;
          appearance: none;
          outline: none;
          transition: border-color 0.2s;
        }
        .asp-select:focus {
          border-color: var(--color-primary, #6366f1);
        }
        .asp-select-icon {
          position: absolute;
          right: 10px;
          color: #aaa;
          pointer-events: none;
        }
        .asp-skills-section {
          margin-bottom: 16px;
        }
        .asp-skills-input {
          display: flex;
          gap: 8px;
          margin-bottom: 8px;
        }
        .asp-skills-input .asp-input {
          flex: 1;
          padding-left: 12px;
        }
        .asp-skill-add-btn {
          padding: 8px 14px;
          border: 1.5px solid #ddd;
          border-radius: 8px;
          background: var(--color-surface, #fff);
          color: var(--color-text, #222);
          cursor: pointer;
          font-size: 0.85rem;
          font-weight: 500;
          transition: background 0.2s;
        }
        .asp-skill-add-btn:hover {
          background: #f5f5f5;
        }
        .asp-skills-tags {
          display: flex;
          flex-wrap: wrap;
          gap: 6px;
        }
        .asp-skill-tag {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          padding: 6px 10px;
          background: var(--color-primary, #6366f1);
          color: #fff;
          border-radius: 16px;
          font-size: 0.85rem;
          font-weight: 500;
        }
        .asp-skill-remove {
          background: none;
          border: none;
          color: #fff;
          cursor: pointer;
          padding: 0;
          display: flex;
          align-items: center;
          opacity: 0.8;
          transition: opacity 0.2s;
        }
        .asp-skill-remove:hover {
          opacity: 1;
        }
        .asp-actions {
          display: flex;
          gap: 10px;
          justify-content: flex-end;
        }
        .asp-search-btn {
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 10px 20px;
          border: none;
          border-radius: 8px;
          background: var(--color-primary, #6366f1);
          color: #fff;
          cursor: pointer;
          font-size: 0.9rem;
          font-weight: 600;
          transition: background 0.2s;
        }
        .asp-search-btn:hover {
          background: var(--color-primary-dark, #4f46e5);
        }
      `}</style>
    </div>
  )
}
