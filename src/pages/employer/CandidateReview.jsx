import { useState, useCallback, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import PropTypes from 'prop-types'
import { DragDropContext, Droppable, Draggable } from '@hello-pangea/dnd'
import { Calendar, Search, Filter, ChevronDown, Loader2, ListChecks, MessageSquare, MessageCircle } from 'lucide-react'
import { useAuthStore }            from '../../store/AuthStore'
import AppNavbar                   from '../../components/common/AppNavbar'
import MatchScoreBadge             from '../../features/matching/MatchScoreBadge'
import ReviewPanel                 from '../../features/reviews/ReviewPanel'
import BulkStatusModal             from '../../features/automation/BulkStatusModal'
import AddToPoolButton             from '../../features/talentpool/AddToPoolButton'
import { getEmployerApplications, updateApplicationStatus } from '../../api/ApplicationApi'
import { buildWhatsAppLink } from '../../utils/whatsapp'
import { useTranslation } from '../../i18n/translations'
import './CandidateReview.css'

const COLUMN_META = [
  { id: 'SUBMITTED', color: '#2b4fbf', bg: '#e8f0ff' },
  { id: 'SCREENING', color: '#a05a00', bg: '#fff3e0' },
  { id: 'INTERVIEW', color: '#7c3aed', bg: '#f3e8ff' },
  { id: 'OFFER',     color: '#0d7a5f', bg: '#e0f7f4' },
  { id: 'APPROVED',  color: '#1a6e44', bg: '#e8f8ee' },
  { id: 'REJECTED',  color: '#c42033', bg: '#fff0f0' },
]

function initials(name = '') {
  return name.split(' ').filter(Boolean).map(w => w[0]).join('').slice(0, 2).toUpperCase() || '?'
}

function KanbanCard({ c, idx, onSelect, isSelected, onToggle, onContact, anonymousLabel }) {
  return (
    <Draggable draggableId={c.id} index={idx}>
      {(prov, snap) => (
        <div
          ref={prov.innerRef}
          {...prov.draggableProps}
          {...prov.dragHandleProps}
          className={`kb-card${snap.isDragging ? ' kb-card--dragging' : ''}${isSelected ? ' kb-card--selected' : ''}`}
          onClick={() => onSelect(c)}
        >
          <div className="kb-card-top">
            <div className="kb-avatar">{c.initials}</div>
            <div className="kb-name">{c.name || anonymousLabel}</div>
            <div style={{ display: 'flex', gap: '6px', marginLeft: 'auto', alignItems: 'center' }}>
              {c.candidateId && (
                <AddToPoolButton candidateId={c.candidateId} onSuccess={() => {}} />
              )}
              {c.candidateId && (
                <button
                  className="kb-contact-btn"
                  title="Contacter"
                  aria-label="Contacter"
                  onClick={e => { e.stopPropagation(); onContact(c) }}
                >
                  <MessageSquare size={14} />
                </button>
              )}
              {c.candidatePhone && (
                <a
                  className="kb-contact-btn kb-contact-btn--wa"
                  title="Contacter via WhatsApp"
                  aria-label="Contacter via WhatsApp"
                  href={buildWhatsAppLink(c.candidatePhone, `Bonjour ${c.name || ''}, je vous contacte au sujet de votre candidature pour « ${c.jobTitle || ''} ».`)}
                  target="_blank"
                  rel="noreferrer"
                  onClick={e => e.stopPropagation()}
                >
                  <MessageCircle size={14} />
                </a>
              )}
              <input
                type="checkbox"
                className="kb-card-check"
                checked={isSelected}
                onClick={e => e.stopPropagation()}
                onChange={e => { e.stopPropagation(); onToggle(c.id) }}
              />
            </div>
          </div>
          <div className="kb-job" title={c.jobTitle}>{c.jobTitle}</div>
          <div className="kb-card-footer">
            <MatchScoreBadge score={c.match} explanation={c.explanation} size="sm" />
            <span className="kb-date"><Calendar size={10} />{c.appliedDate}</span>
          </div>
        </div>
      )}
    </Draggable>
  )
}

KanbanCard.propTypes = {
  c: PropTypes.shape({
    id:          PropTypes.string.isRequired,
    initials:    PropTypes.string.isRequired,
    name:        PropTypes.string,
    jobTitle:    PropTypes.string,
    match:       PropTypes.number,
    explanation: PropTypes.string,
    appliedDate: PropTypes.string,
    candidatePhone: PropTypes.string,
  }).isRequired,
  idx:        PropTypes.number.isRequired,
  onSelect:   PropTypes.func.isRequired,
  isSelected: PropTypes.bool.isRequired,
  onToggle:   PropTypes.func.isRequired,
  onContact:  PropTypes.func.isRequired,
  anonymousLabel: PropTypes.string.isRequired,
}

export default function CandidateReview() {
  const { user } = useAuthStore()
  const navigate = useNavigate()
  const t = useTranslation().employer.review
  const COLUMNS = COLUMN_META.map(col => ({ ...col, label: t.columns[col.id] }))

  const [cards,         setCards]         = useState([])
  const [statuses,      setStatuses]      = useState({})
  const [filterJob,     setFilterJob]     = useState('all')
  const [search,        setSearch]        = useState('')
  const [loading,       setLoading]       = useState(true)
  const [error,         setError]         = useState(null)
  const [selectedCard,  setSelectedCard]  = useState(null)
  const [selectedIds,   setSelectedIds]   = useState(new Set())
  const [bulkModalOpen, setBulkModalOpen] = useState(false)

  // ── Jobs distincts pour le filtre ──────────────────────────────────────────
  const jobs = [...new Map(cards.map(c => [c.jobId, { id: c.jobId, title: c.jobTitle }])).values()]

  // ── Filtrage ───────────────────────────────────────────────────────────────
  const visible = cards.filter(c => {
    const q = search.toLowerCase()
    return (filterJob === 'all' || c.jobId === filterJob) &&
      (q === '' || (c.name?.toLowerCase().includes(q)) || (c.jobTitle?.toLowerCase().includes(q)))
  })

  // ── Organisation par colonne ───────────────────────────────────────────────
  const byColumn = Object.fromEntries(COLUMNS.map(col => [col.id, []]))
  visible.forEach(c => {
    const st = statuses[c.id] ?? c.defaultStatus
    if (byColumn[st]) byColumn[st].push(c)
  })

  // ── Sélection en masse ────────────────────────────────────────────────────
  const toggleSelect = useCallback((id) => {
    setSelectedIds(prev => {
      const next = new Set(prev)
      next.has(id) ? next.delete(id) : next.add(id)
      return next
    })
  }, [])

  const handleContact = useCallback((c) => {
    navigate('/messages', { state: { contactId: c.candidateId, contactName: c.name } })
  }, [navigate])

  const loadCards = useCallback(() => {
    setLoading(true)
    getEmployerApplications()
      .then(dtos => {
        setCards(dtos.map(dto => ({
          id:            dto.id,
          candidateId:   dto.candidateId || dto.jobSeekerId,
          name:          dto.candidateName,
          candidatePhone: dto.candidatePhone,
          initials:      initials(dto.candidateName),
          jobTitle:      dto.jobTitle,
          jobId:         dto.jobListingId,
          match:         dto.matchScore,
          explanation:   dto.matchExplanation,
          defaultStatus: dto.status,
          appliedDate:   '',
        })))
      })
      .catch(err => setError(err?.response?.data?.message || t.loadError))
      .finally(() => setLoading(false))
  }, [t.loadError])

  // ── Chargement des données réelles ─────────────────────────────────────────
  useEffect(() => { loadCards() }, [user?.id, loadCards])

  // ── Drag & Drop → PATCH /applications/{id}/status ─────────────────────────
  const onDragEnd = useCallback(({ source, destination, draggableId }) => {
    if (!destination || destination.droppableId === source.droppableId) return
    const newStatus = destination.droppableId
    setStatuses(prev => ({ ...prev, [draggableId]: newStatus }))
    updateApplicationStatus(draggableId, newStatus).catch(() => {
      // Rollback visuel si l'appel échoue
      setStatuses(prev => ({ ...prev, [draggableId]: source.droppableId }))
    })
  }, [])

  return (
    <div className="cr-shell">
      <div className="cr-blob cr-blob--main" />
      <div className="cr-blob cr-blob--accent" />

      <AppNavbar />

      <main className="cr-main cr-main--board">
        <div className="cr-header">
          <h1 className="cr-title">{t.pipelineTitle}</h1>
          <p className="cr-subtitle">
            {loading ? t.loading : `${cards.length} ${cards.length !== 1 ? t.candidates.plural : t.candidates.singular} · ${t.dragHint}`}
          </p>
        </div>

        <div className="cr-filters cr-filters--board">
          <div className="cr-filter-wrap">
            <Search size={14} className="cr-filter-icon" />
            <input
              className="cr-select"
              style={{ paddingLeft: 32 }}
              placeholder={t.searchPlaceholder}
              value={search}
              onChange={e => setSearch(e.target.value)}
            />
          </div>
          <div className="cr-filter-wrap">
            <Filter size={13} className="cr-filter-icon" />
            <select className="cr-select" value={filterJob} onChange={e => setFilterJob(e.target.value)}>
              <option value="all">{t.allJobs}</option>
              {jobs.map(j => <option key={j.id} value={j.id}>{j.title}</option>)}
            </select>
            <ChevronDown size={13} className="cr-filter-arrow" />
          </div>
          {selectedIds.size > 0 && (
            <button className="cr-bulk-btn" onClick={() => setBulkModalOpen(true)}>
              <ListChecks size={15} />
              {t.bulkActions} ({selectedIds.size})
            </button>
          )}
        </div>

        {loading && (
          <div className="cr-loading">
            <Loader2 size={28} className="cr-loading-icon" />
            <p>{t.loadingCandidates}</p>
          </div>
        )}

        {error && !loading && (
          <div className="cr-error">{error}</div>
        )}

        {!loading && !error && (
          <DragDropContext onDragEnd={onDragEnd}>
            <div className="kb-board">
              {COLUMNS.map(col => {
                const colCards = byColumn[col.id] ?? []
                return (
                  <div key={col.id} className="kb-col">
                    <div className="kb-col-header" style={{ borderBottomColor: col.color }}>
                      <span className="kb-col-label" style={{ color: col.color }}>{col.label}</span>
                      <span className="kb-col-count" style={{ background: col.bg, color: col.color }}>{colCards.length}</span>
                    </div>
                    <Droppable droppableId={col.id}>
                      {(provided, snapshot) => (
                        <div
                          ref={provided.innerRef}
                          {...provided.droppableProps}
                          className={`kb-col-body${snapshot.isDraggingOver ? ' kb-col-body--over' : ''}`}
                        >
                          {colCards.map((c, idx) => (
                            <KanbanCard
                              key={c.id}
                              c={c}
                              idx={idx}
                              onSelect={setSelectedCard}
                              isSelected={selectedIds.has(c.id)}
                              onToggle={toggleSelect}
                              onContact={handleContact}
                              anonymousLabel={t.anonymousCandidate}
                            />
                          ))}
                          {provided.placeholder}
                        </div>
                      )}
                    </Droppable>
                  </div>
                )
              })}
            </div>
          </DragDropContext>
        )}
      </main>

      {selectedCard && (
        <ReviewPanel card={selectedCard} onClose={() => setSelectedCard(null)} />
      )}

      {bulkModalOpen && (
        <BulkStatusModal
          selectedIds={[...selectedIds]}
          onSuccess={() => {
            setSelectedIds(new Set())
            setBulkModalOpen(false)
            loadCards()
          }}
          onClose={() => setBulkModalOpen(false)}
        />
      )}
    </div>
  )
}
