import { Client } from '@stomp/stompjs'

let client = null

export function connecterWebSocket(userId, onMessageRecu) {
  client = new Client({
    brokerURL: 'ws://localhost:8080/ws',
    connectHeaders: {
      Authorization: `Bearer ${localStorage.getItem('authToken')}`,
    },
    reconnectDelay: 5000,
    onConnect: () => {
      client.subscribe(`/topic/user/${userId}`, (frame) => {
        try {
          onMessageRecu(JSON.parse(frame.body))
        } catch {
          onMessageRecu(frame.body)
        }
      })
    },
    onStompError: (frame) => {
      console.error('STOMP error:', frame.headers['message'])
    },
  })
  client.activate()
}

export function envoyerMessage(message) {
  if (client?.connected) {
    client.publish({
      destination: '/app/chat',
      body: JSON.stringify(message),
    })
  }
}

export function deconnecterWebSocket() {
  if (client) {
    client.deactivate()
    client = null
  }
}
