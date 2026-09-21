import { create } from 'zustand'
import {
  getConversations,
  getConversation,
  sendMessage as apiSendMessage,
  markAsRead,
} from '../api/MessageApi'

export const useMessageStore = create((set, get) => ({
  conversations: [],
  activeConvId:  null,
  totalUnread:   0,

  loadConversations: async () => {
    const dtos = await getConversations()
    const conversations = dtos.map(d => ({
      otherUserId:     d.otherUserId,
      otherUserName:   d.otherUserName,
      lastMessage:     d.lastMessage,
      lastMessageTime: d.lastMessageTime,
      unreadCount:     d.unreadCount,
      messages:        null,
    }))
    set({
      conversations,
      totalUnread: conversations.reduce((acc, c) => acc + c.unreadCount, 0),
    })
  },

  setActiveConv: async (otherUserId, myUserId) => {
    set({ activeConvId: otherUserId })
    if (!otherUserId) return
    const conv = get().conversations.find(c => c.otherUserId === otherUserId)
    if (!conv || conv.messages !== null) return

    const messages = await getConversation(myUserId, otherUserId)
    messages
      .filter(m => m.recipientId === myUserId && !m.isRead)
      .forEach(m => { markAsRead(m.id).catch(() => {}) })

    set(s => {
      const target = s.conversations.find(c => c.otherUserId === otherUserId)
      const unreadDelta = target ? target.unreadCount : 0
      return {
        conversations: s.conversations.map(c =>
          c.otherUserId === otherUserId ? { ...c, messages, unreadCount: 0 } : c
        ),
        totalUnread: Math.max(0, s.totalUnread - unreadDelta),
      }
    })
  },

  startConversation: (otherUserId, otherUserName) => {
    const existing = get().conversations.find(c => c.otherUserId === otherUserId)
    if (existing) {
      set({ activeConvId: otherUserId })
      return otherUserId
    }
    const newConv = {
      otherUserId,
      otherUserName,
      lastMessage:     '',
      lastMessageTime: null,
      unreadCount:     0,
      messages:        [],
    }
    set(s => ({ conversations: [newConv, ...s.conversations], activeConvId: otherUserId }))
    return otherUserId
  },

  sendMessage: (otherUserId, text, myUserId) => {
    const now = new Date().toISOString()
    const optimistic = {
      id: `local-${Date.now()}`,
      senderId: myUserId,
      recipientId: otherUserId,
      content: text,
      isRead: true,
      sentAt: now,
    }
    set(s => ({
      conversations: s.conversations.map(c =>
        c.otherUserId === otherUserId
          ? { ...c, messages: [...(c.messages ?? []), optimistic], lastMessage: text, lastMessageTime: now }
          : c
      ),
    }))

    apiSendMessage(otherUserId, text)
      .then(saved => {
        set(s => ({
          conversations: s.conversations.map(c =>
            c.otherUserId === otherUserId
              ? { ...c, messages: c.messages.map(m => m.id === optimistic.id ? { ...m, id: saved.id } : m) }
              : c
          ),
        }))
      })
      .catch(err => console.error('Failed to send message', err))
  },

  receiveMessage: (dto, myUserId) => {
    if (dto.senderId === myUserId) return
    const otherUserId = dto.senderId
    const isActive = get().activeConvId === otherUserId
    const now = new Date().toISOString()
    const incoming = { ...dto, sentAt: now }

    set(s => {
      const existing = s.conversations.find(c => c.otherUserId === otherUserId)
      if (existing) {
        return {
          conversations: s.conversations.map(c =>
            c.otherUserId === otherUserId
              ? {
                  ...c,
                  messages:        c.messages ? [...c.messages, incoming] : c.messages,
                  lastMessage:     dto.content,
                  lastMessageTime: now,
                  unreadCount:     isActive ? c.unreadCount : c.unreadCount + 1,
                }
              : c
          ),
          totalUnread: isActive ? s.totalUnread : s.totalUnread + 1,
        }
      }
      const newConv = {
        otherUserId,
        otherUserName:   otherUserId,
        lastMessage:     dto.content,
        lastMessageTime: now,
        unreadCount:     isActive ? 0 : 1,
        messages:        [incoming],
      }
      return {
        conversations: [newConv, ...s.conversations],
        totalUnread: isActive ? s.totalUnread : s.totalUnread + 1,
      }
    })
  },
}))
