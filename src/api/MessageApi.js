import axiosInstance from './AxiosInstance'

export const sendMessage = (recipientId, content) =>
  axiosInstance.post('/messages', { recipientId, content }).then(r => r.data)

export const getConversations = () =>
  axiosInstance.get('/messages/conversations').then(r => r.data)

export const getConversation = (userId1, userId2) =>
  axiosInstance.get('/messages/conversation', { params: { userId1, userId2 } }).then(r => r.data)

export const getUnreadMessages = (userId) =>
  axiosInstance.get(`/messages/unread/${userId}`).then(r => r.data)

export const markAsRead = (messageId) =>
  axiosInstance.put(`/messages/${messageId}/read`).then(r => r.data)
