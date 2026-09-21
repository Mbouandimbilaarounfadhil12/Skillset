import { useEffect, useRef } from 'react'
import { Client } from '@stomp/stompjs'
import { useAuthStore }          from '../../store/AuthStore'
import { useNotificationStore }  from '../../store/NotificationStore'

const WS_URL = (import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws')
  .replace(/^http/, 'ws')

export default function NotificationListener() {
  const { user, token } = useAuthStore()
  const addNotification  = useNotificationStore(s => s.addNotification)
  const clientRef        = useRef(null)

  useEffect(() => {
    if (!user?.id || !token) return

    const client = new Client({
      brokerURL:        WS_URL,
      connectHeaders:   { Authorization: `Bearer ${token}` },
      reconnectDelay:   5000,
      onConnect: () => {
        client.subscribe(`/topic/notifications/${user.id}`, (frame) => {
          try {
            const payload = JSON.parse(frame.body)
            addNotification({
              type:  payload.type  || 'info',
              title: payload.title || 'Notification',
              body:  payload.body  || '',
              link:  payload.link  || '/',
            })
          } catch {
            // malformed frame — ignore
          }
        })
      },
    })

    client.activate()
    clientRef.current = client

    return () => { client.deactivate() }
  }, [user?.id, token, addNotification])

  return null
}
