import { useState, useRef, useEffect, useCallback } from 'react'
import { X, Send, ChevronDown, Sparkles, WifiOff, Zap, Search, ChevronRight } from 'lucide-react'
import { stellaChat, runStellaTask, appSearch } from '../../api/StellaApi'
import { useOnlineStatus } from '../../hooks/useOnlineStatus'
import { useAuthStore } from '../../store/AuthStore'
import { useTranslation } from '../../i18n/translations'
import './ChatbotWidget.css'

function TypingDots() {
  return <div className="cw-typing"><span /><span /><span /></div>
}

function renderText(text) {
  return text.split(/\*\*(.+?)\*\*/g).map((part, i) =>
    i % 2 === 1 ? <strong key={`b-${part}-${i}`}>{part}</strong> : part
  )
}

function buildGreeting(user, online, t) {
  const name   = user?.firstName ? `, ${user.firstName}` : ''
  const status = online ? '' : t.greetingOfflineNote
  const body = user?.role === 'EMPLOYER' ? t.greetingEmployerBody : t.greetingCandidateBody
  return `${t.greetingHello}${name} ! ${t.greetingIntro}\n\n${body}${status}`
}

export default function ChatbotWidget() {
  const { user }  = useAuthStore()
  const { isOnline, wasOffline } = useOnlineStatus()
  const t = useTranslation().chatbot

  const isEmployer = user?.role === 'EMPLOYER'
  const QUICK_ACTIONS = isEmployer ? t.quickActionsEmployer : t.quickActionsCandidate

  // Always include role + firstName in the profile context sent to the backend
  const profile = (() => {
    try {
      const stored = JSON.parse(localStorage.getItem('ss_profile') || 'null')
      const base = stored ?? user
      return { ...base, role: user?.role, firstName: user?.firstName, lastName: user?.lastName }
    } catch { return user }
  })()

  const [open, setOpen]           = useState(false)
  const [tab, setTab]             = useState('chat')
  const [messages, setMessages]   = useState([
    { id: 0, role: 'stella', text: buildGreeting(user, isOnline, t), action: null },
  ])
  const [input, setInput]         = useState('')
  const [loading, setLoading]     = useState(false)
  const [taskRunning, setTaskRunning] = useState(null)
  const [searchQ, setSearchQ]     = useState('')
  const [searchRes, setSearchRes] = useState(null)

  const bottomRef = useRef(null)
  const inputRef  = useRef(null)

  const pushStella = useCallback((text, action = null) => {
    setMessages(prev => [...prev, { id: Date.now(), role: 'stella', text, action }])
  }, [])

  useEffect(() => {
    if (!open) return
    let msg = null
    if (!isOnline) {
      msg = t.connectionLost
    } else if (wasOffline) {
      msg = t.connectionRestored
    }
    if (msg) {
      const timer = setTimeout(() => pushStella(msg, null), 0)
      return () => clearTimeout(timer)
    }
    return undefined
  }, [isOnline]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (open) {
      setTimeout(() => {
        bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
        inputRef.current?.focus()
      }, 80)
    }
  }, [open, messages])

  const send = async (text = input) => {
    const msg = text.trim()
    if (!msg || loading) return
    setInput('')

    setMessages(prev => [...prev, { id: Date.now(), role: 'user', text: msg, action: null }])
    setLoading(true)

    const history = messages.map(m => ({ role: m.role === 'stella' ? 'assistant' : 'user', content: m.text }))
    const { text: reply, action } = await stellaChat(msg, { history, profile, online: isOnline })

    setMessages(prev => [...prev, { id: Date.now() + 1, role: 'stella', text: reply, action: action ?? null }])
    setLoading(false)
  }

  const runTask = async (taskId) => {
    setTaskRunning(taskId)
    setTab('chat')
    setMessages(prev => [...prev, { id: Date.now(), role: 'user', text: `${t.autonomousModeTag} ${t.tasks.find(task => task.id === taskId)?.label}`, action: null }])
    setLoading(true)

    const result = await runStellaTask(taskId, profile)

    setMessages(prev => [...prev, { id: Date.now() + 1, role: 'stella', text: result, action: null }])
    setLoading(false)
    setTaskRunning(null)
  }

  const doSearch = (q = searchQ) => {
    if (!q.trim()) return
    const res = appSearch(q)
    setSearchRes({ query: q, ...res })
  }

  const handleKey = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send() }
  }

  return (
    <>
      {/* FAB */}
      <button
        className={`cw-fab ${open ? 'cw-fab--open' : ''}`}
        onClick={() => setOpen(v => !v)}
        aria-label={t.openStella}
      >
        {open ? <ChevronDown size={22} /> : <Sparkles size={22} />}
        {!open && <span className="cw-fab-label">STELLA</span>}
        {!open && !isOnline && <span className="cw-fab-offline-dot" title={t.offline} />}
      </button>

      {open && (
        <div className="cw-window">

          {/* Header */}
          <div className="cw-header">
            <div className="cw-header-info">
              <div className="cw-avatar"><Sparkles size={18} /></div>
              <div>
                <p className="cw-header-name">STELLA <span className="cw-header-badge">IA</span></p>
                <p className="cw-header-status">
                  {isOnline
                    ? <><span className="cw-dot cw-dot--green" />{t.online}</>
                    : <><span className="cw-dot cw-dot--red" /><WifiOff size={11} style={{ marginRight: 3 }} />{t.offline}</>
                  }
                </p>
              </div>
            </div>
            <div className="cw-header-actions">
              <button
                className={`cw-mode-btn ${tab === 'tasks' ? 'active' : ''}`}
                onClick={() => setTab(tb => tb === 'tasks' ? 'chat' : 'tasks')}
                title={t.autonomousMode}
              >
                <Zap size={15} />
              </button>
              <button
                className={`cw-mode-btn ${tab === 'search' ? 'active' : ''}`}
                onClick={() => setTab(tb => tb === 'search' ? 'chat' : 'search')}
                title={t.globalSearch}
              >
                <Search size={15} />
              </button>
              <button className="cw-close" onClick={() => setOpen(false)}><X size={18} /></button>
            </div>
          </div>

          {/* Offline banner */}
          {!isOnline && (
            <div className="cw-offline-banner">
              <WifiOff size={13} />
              {t.offlineBanner}
            </div>
          )}

          {/* ── TAB: AUTONOMOUS TASKS ── */}
          {tab === 'tasks' && (
            <div className="cw-tasks-panel">
              <p className="cw-tasks-title"><Zap size={14} /> {t.autonomousModeTitle}</p>
              <p className="cw-tasks-sub">{t.autonomousModeSub}</p>
              {t.tasks.map(task => (
                <button
                  key={task.id}
                  className={`cw-task-btn ${taskRunning === task.id ? 'running' : ''}`}
                  onClick={() => runTask(task.id)}
                  disabled={!!taskRunning}
                >
                  <span className="cw-task-icon">{task.icon}</span>
                  <span className="cw-task-info">
                    <span className="cw-task-label">{task.label}</span>
                    <span className="cw-task-desc">{task.desc}</span>
                  </span>
                  <ChevronRight size={14} className="cw-task-arrow" />
                </button>
              ))}
            </div>
          )}

          {/* ── TAB: GLOBAL SEARCH ── */}
          {tab === 'search' && (
            <div className="cw-search-panel">
              <p className="cw-tasks-title"><Search size={14} /> {t.globalSearchTitle}</p>
              <div className="cw-search-input-row">
                <input
                  className="cw-search-input"
                  placeholder={t.searchPlaceholder}
                  value={searchQ}
                  onChange={e => setSearchQ(e.target.value)}
                  onKeyDown={e => e.key === 'Enter' && doSearch()}
                />
                <button className="cw-search-go" onClick={() => doSearch()}>
                  <Search size={15} />
                </button>
              </div>
              {!isOnline && (
                <p className="cw-search-offline">{t.searchOfflineHint}</p>
              )}
              {searchRes && (
                <div className="cw-search-results">
                  {searchRes.jobs.length > 0 && (
                    <>
                      <p className="cw-search-section">{t.jobsSection} ({searchRes.jobs.length})</p>
                      {searchRes.jobs.map(j => (
                        <div key={j.id} className="cw-search-item">
                          <span>{j.logo}</span>
                          <div>
                            <p className="cw-search-item-title">{j.title}</p>
                            <p className="cw-search-item-sub">{j.company} · {j.location}</p>
                          </div>
                        </div>
                      ))}
                    </>
                  )}
                  {searchRes.companies.length > 0 && (
                    <>
                      <p className="cw-search-section">{t.companiesSection} ({searchRes.companies.length})</p>
                      {searchRes.companies.map(c => (
                        <div key={c.id} className="cw-search-item">
                          <span>{c.logo}</span>
                          <div>
                            <p className="cw-search-item-title">{c.name}</p>
                            <p className="cw-search-item-sub">⭐ {c.rating} · {c.sector}</p>
                          </div>
                        </div>
                      ))}
                    </>
                  )}
                  {searchRes.jobs.length === 0 && searchRes.companies.length === 0 && (
                    <p className="cw-search-empty">{t.noResultsFor} {searchRes.query} »</p>
                  )}
                </div>
              )}
            </div>
          )}

          {/* ── TAB: CHAT ── */}
          {tab === 'chat' && (
            <>
              <div className="cw-messages">
                {messages.map(m => (
                  <div key={m.id} className={`cw-msg cw-msg--${m.role}`}>
                    {m.role === 'stella' && <div className="cw-msg-avatar"><Sparkles size={13} /></div>}
                    <div className="cw-bubble">
                      {m.text.split('\n').map((line, i) => (
                        <span key={`${m.id}-${i}`}>
                          {renderText(line)}{i < m.text.split('\n').length - 1 && <br />}
                        </span>
                      ))}
                    </div>
                  </div>
                ))}
                {loading && (
                  <div className="cw-msg cw-msg--stella">
                    <div className="cw-msg-avatar"><Sparkles size={13} /></div>
                    <div className="cw-bubble"><TypingDots /></div>
                  </div>
                )}
                <div ref={bottomRef} />
              </div>

              {messages.length <= 1 && (
                <div className="cw-quick-actions">
                  {QUICK_ACTIONS.map(qa => (
                    <button key={qa.label} className="cw-quick-chip" onClick={() => send(qa.msg)}>
                      {qa.label}
                    </button>
                  ))}
                </div>
              )}

              <div className="cw-input-row">
                <textarea
                  ref={inputRef}
                  className="cw-input"
                  placeholder={isOnline ? t.inputPlaceholderOnline : t.inputPlaceholderOffline}
                  value={input}
                  onChange={e => setInput(e.target.value)}
                  onKeyDown={handleKey}
                  rows={1}
                />
                <button className="cw-send" onClick={() => send()} disabled={!input.trim() || loading}>
                  <Send size={17} />
                </button>
              </div>
            </>
          )}
        </div>
      )}
    </>
  )
}
