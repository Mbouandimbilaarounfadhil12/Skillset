import { useEffect } from 'react'
import { connecterWebSocket, deconnecterWebSocket } from '../services/websocketService'

/**
 * Hook React qui connecte/déconnecte automatiquement le WebSocket
 * selon le cycle de vie du composant.
 *
 * @param {string|null}  userId      ID de l'utilisateur connecté
 * @param {function}     onMessage   callback appelé à chaque message reçu
 */
export function useWebSocket(userId, onMessage) {
  useEffect(() => {
    if (!userId) return
    connecterWebSocket(userId, onMessage)
    return () => deconnecterWebSocket()
  }, [userId]) // eslint-disable-line react-hooks/exhaustive-deps

  // onMessage est intentionnellement exclu des deps : le service garde
  // la dernière référence sans provoquer de reconnexion à chaque render.
}

export default useWebSocket
