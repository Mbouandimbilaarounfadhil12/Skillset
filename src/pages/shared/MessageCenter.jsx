import { useEffect, useRef, useState, useCallback } from 'react'
import { useLocation } from 'react-router-dom'
import {
  Send, Search, MessageSquare, CheckCheck, Plus, X,
} from 'lucide-react'
import { useAuthStore }          from '../../store/AuthStore'
import { useMessageStore }       from '../../store/MessageStore'
import { useWebSocket }          from '../../hooks/useWebSocket'
import { markAsRead }            from '../../api/MessageApi'
import { getEmployerApplications } from '../../api/ApplicationApi'
import AppNavbar                 from '../../components/common/AppNavbar'
import { usePreferencesStore }   from '../../store/PreferencesStore'
import { useTranslation }        from '../../i18n/translations'
import './MessageCenter.css'

function fmtTime(iso, locale) {
  if (!iso) return ''
  const d   = new Date(iso)
  const now = new Date()
  if (d.toDateString() === now.toDateString()) {
    return d.toLocaleTimeString(locale, { hour: '2-digit', minute: '2-digit' })
  }
  return d.toLocaleDateString(locale, { day: '2-digit', month: 'short' })
}

function fmtMsgTime(iso, locale) {
  return new Date(iso).toLocaleTimeString(locale, { hour: '2-digit', minute: '2-digit' })
}

function initials(name = '') {
  return name.split(' ').filter(Boolean).map(w => w[0]).join('').slice(0, 2).toUpperCase() || '?'
}

export default function MessageCenter() {
  const { user } = useAuthStore()
  const {
    conversations, activeConvId, setActiveConv, sendMessage,
    startConversation, receiveMessage, loadConversations,
  } = useMessageStore()
  const { language } = usePreferencesStore()
  const t = useTranslation().messages
  const locale = language === 'fr' ? 'fr-FR' : 'en-US'
  const location = useLocation()

  const onWsMessage = useCallback((dto) => {
    if (!user?.id) return
    receiveMessage(dto, user.id)
    const otherUserId = dto.senderId === user.id ? dto.recipientId : dto.senderId
    if (otherUserId === activeConvId && dto.id) {
      markAsRead(dto.id).catch(() => {})
    }
  }, [user?.id, receiveMessage, activeConvId])
  useWebSocket(user?.id ?? null, onWsMessage)

  const [text, setText]           = useState('')
  const [search, setSearch]       = useState('')
  const [showModal, setShowModal] = useState(false)
  const [modalQ, setModalQ]       = useState('')
  const [modalContacts, setModalContacts] = useState([])
  const [modalLoading, setModalLoading]   = useState(false)

  const bottomRef = useRef(null)
  const inputRef  = useRef(null)
  const dialogRef = useRef(null)
  const startedFromLocationRef = useRef(false)

  const activeConv = conversations.find(c => c.otherUserId === activeConvId) || null
  const filtered   = conversations.filter(c =>
    (c.otherUserName || '').toLowerCase().includes(search.toLowerCase())
  )
  const modalResults = modalQ.trim()
    ? modalContacts.filter(c =>
        c.name.toLowerCase().includes(modalQ.toLowerCase()) ||
        (c.label || '').toLowerCase().includes(modalQ.toLowerCase())
      )
    : modalContacts

  useEffect(() => {
    loadConversations()
  }, [loadConversations])

  useEffect(() => {
    if (startedFromLocationRef.current) return
    if (location.state?.contactId) {
      startedFromLocationRef.current = true
      startConversation(location.state.contactId, location.state.contactName)
    }
  }, [location.state, startConversation])

  useEffect(() => {
    if (!showModal || user?.role !== 'EMPLOYER') {
      if (!showModal) setModalContacts([])
      return
    }
    setModalLoading(true)
    getEmployerApplications()
      .then(apps => {
        const byId = new Map()
        apps.forEach(a => {
          const id = a.candidateId || a.jobSeekerId
          if (id && !byId.has(id)) {
            byId.set(id, { id, name: a.candidateName || id, label: a.jobTitle || '' })
          }
        })
        setModalContacts([...byId.values()])
      })
      .catch(() => setModalContacts([]))
      .finally(() => setModalLoading(false))
  }, [showModal, user?.role])

  useEffect(() => {
    const d = dialogRef.current
    if (!d) return
    if (showModal) {
      d.showModal()
    } else {
      d.close()
    }
  }, [showModal])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [activeConv?.messages?.length])

  useEffect(() => {
    if (activeConvId) inputRef.current?.focus()
  }, [activeConvId])

  const handleSend = () => {
    const trimmed = text.trim()
    if (!trimmed || !activeConvId || !user?.id) return
    sendMessage(activeConvId, trimmed, user.id)
    setText('')
  }

  const handleKey = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleSend() }
  }

  const handleSelectContact = (contact) => {
    startConversation(contact.id, contact.name)
    setShowModal(false)
  }

  return (
    <div className="mc-shell">

      {/* New conversation modal — native <dialog> */}
      <dialog ref={dialogRef} className="mc-modal-dialog" onClose={() => setShowModal(false)}>
        <div className="mc-modal">
          <div className="mc-modal-header">
            <h3 className="mc-modal-title">{t.newConversation}</h3>
            <button className="mc-modal-close" onClick={() => setShowModal(false)}><X size={18} /></button>
          </div>
          <div className="mc-modal-search-wrap">
            <Search size={14} className="mc-search-icon" />
            <input
              className="mc-search-input"
              placeholder={t.searchContactPlaceholder}
              value={modalQ}
              onChange={e => setModalQ(e.target.value)}
            />
          </div>
          <div className="mc-modal-list">
            {modalLoading && <p className="mc-no-conv">…</p>}
            {!modalLoading && modalResults.map(c => (
              <button key={c.id} className="mc-modal-contact" onClick={() => handleSelectContact(c)}>
                <span className="mc-conv-avatar">{initials(c.name)}</span>
                <div className="mc-modal-contact-info">
                  <p className="mc-modal-contact-name">{c.name}</p>
                  <p className="mc-modal-contact-role">{c.label}</p>
                </div>
              </button>
            ))}
            {!modalLoading && modalResults.length === 0 && (
              <p className="mc-no-conv">{t.noContactFound}</p>
            )}
          </div>
        </div>
      </dialog>

      <AppNavbar />

      {/* Layout */}
      <div className="mc-layout">

        {/* Sidebar */}
        <aside className="mc-sidebar">
          <div className="mc-sidebar-header">
            <h2 className="mc-sidebar-title"><MessageSquare size={18} /> {t.title}</h2>
            <button
              className="mc-new-conv-btn"
              onClick={() => { setModalQ(''); setShowModal(true) }}
              aria-label={t.newConversation}
              title={t.newConversation}
            >
              <Plus size={17} />
            </button>
          </div>
          <div className="mc-search-wrap">
            <Search size={14} className="mc-search-icon" />
            <input
              className="mc-search-input"
              placeholder={t.searchConvPlaceholder}
              value={search}
              onChange={e => setSearch(e.target.value)}
            />
          </div>
          <div className="mc-conv-list">
            {filtered.length === 0 && <p className="mc-no-conv">{t.noConvFound}</p>}
            {filtered.map(c => (
              <button
                key={c.otherUserId}
                className={`mc-conv-item ${c.otherUserId === activeConvId ? 'mc-conv-item--active' : ''}`}
                onClick={() => setActiveConv(c.otherUserId, user?.id)}
              >
                <div className="mc-conv-avatar-wrap">
                  <span className="mc-conv-avatar">{initials(c.otherUserName)}</span>
                </div>
                <div className="mc-conv-info">
                  <div className="mc-conv-top">
                    <span className="mc-conv-name">{c.otherUserName}</span>
                    <span className="mc-conv-time">{fmtTime(c.lastMessageTime, locale)}</span>
                  </div>
                  <div className="mc-conv-bottom">
                    <span className={`mc-conv-preview ${c.unreadCount > 0 ? 'mc-conv-preview--bold' : ''}`}>
                      {c.lastMessage || t.startConversation}
                    </span>
                    {c.unreadCount > 0 && <span className="mc-unread-badge">{c.unreadCount}</span>}
                  </div>
                </div>
              </button>
            ))}
          </div>
        </aside>

        {/* Chat panel */}
        <main className="mc-chat">
          {activeConv ? (
            <>
              {/* Header */}
              <div className="mc-chat-header">
                <div className="mc-chat-avatar-wrap">
                  <span className="mc-chat-avatar">{initials(activeConv.otherUserName)}</span>
                </div>
                <div className="mc-chat-info">
                  <p className="mc-chat-name">{activeConv.otherUserName}</p>
                </div>
              </div>

              {/* Messages */}
              <div className="mc-messages">
                {(!activeConv.messages || activeConv.messages.length === 0) && (
                  <div className="mc-messages-empty">
                    <p>{t.noMessages}</p>
                  </div>
                )}
                {(activeConv.messages || []).map(m => {
                  const isUser = m.senderId === user?.id
                  return (
                    <div key={m.id} className={`mc-msg ${isUser ? 'mc-msg--user' : 'mc-msg--other'}`}>
                      {!isUser && (
                        <span className="mc-msg-avatar">{initials(activeConv.otherUserName)}</span>
                      )}
                      <div className="mc-bubble">
                        <p className="mc-bubble-text">{m.content}</p>
                        <span className="mc-bubble-meta">
                          {m.sentAt && fmtMsgTime(m.sentAt, locale)}
                          {isUser && <CheckCheck size={11} className={`mc-check ${m.isRead ? 'mc-check--read' : ''}`} />}
                        </span>
                      </div>
                    </div>
                  )
                })}
                <div ref={bottomRef} />
              </div>

              {/* Input */}
              <div className="mc-input-row">
                <textarea
                  ref={inputRef}
                  className="mc-input"
                  placeholder={`${t.writeToPrefix} ${activeConv.otherUserName}…`}
                  value={text}
                  onChange={e => setText(e.target.value)}
                  onKeyDown={handleKey}
                  rows={1}
                />
                <button
                  className="mc-send-btn"
                  onClick={handleSend}
                  disabled={!text.trim()}
                  aria-label={t.send}
                >
                  <Send size={18} />
                </button>
              </div>
            </>
          ) : (
            <div className="mc-empty-state">
              <MessageSquare size={52} strokeWidth={1.1} className="mc-empty-icon" />
              <h3>{t.selectConversation}</h3>
              <p>{t.selectConversationSub}</p>
              <button className="mc-start-btn" onClick={() => { setModalQ(''); setShowModal(true) }}>
                <Plus size={15} /> {t.newConversation}
              </button>
            </div>
          )}
        </main>
      </div>
    </div>
  )
}
