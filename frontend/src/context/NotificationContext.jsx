import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Client } from '@stomp/stompjs'
import { useAuth } from './AuthContext'
import { useToast } from './ToastContext'
import { messageService, notificationService } from '../services'

const NotificationContext = createContext(null)

const QUEUES = ['notifications', 'messages', 'read-receipts', 'typing', 'presence']

function brokerUrl() {
  if (import.meta.env.VITE_WS_URL) return import.meta.env.VITE_WS_URL
  const scheme = window.location.protocol === 'https:' ? 'wss' : 'ws'
  return `${scheme}://${window.location.host}/ws`
}

/**
 * Owns the single STOMP connection for the signed-in user and fans incoming events out to listeners.
 * Also tracks unread notification and message counts for the navbar.
 */
export function NotificationProvider({ children }) {
  const { token, user } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const [connected, setConnected] = useState(false)
  const [unreadNotifications, setUnreadNotifications] = useState(0)
  const [unreadMessages, setUnreadMessages] = useState(0)
  const [presence, setPresence] = useState({})
  const listeners = useRef(Object.fromEntries(QUEUES.map((q) => [q, new Set()])))
  const clientRef = useRef(null)
  const activeChatRef = useRef(null)

  const refreshCounts = useCallback(() => {
    if (!user || user.role === 'ADMIN') return
    notificationService.unreadCount().then((r) => setUnreadNotifications(r.count)).catch(() => {})
    messageService.unreadCount().then((r) => setUnreadMessages(r.count)).catch(() => {})
  }, [user])

  useEffect(() => {
    refreshCounts()
  }, [refreshCounts])

  useEffect(() => {
    if (!token || !user || user.role === 'ADMIN') return undefined
    const client = new Client({
      brokerURL: brokerUrl(),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 4000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    })
    client.onConnect = () => {
      setConnected(true)
      QUEUES.forEach((queue) =>
        client.subscribe(`/user/queue/${queue}`, (frame) => {
          let payload
          try {
            payload = JSON.parse(frame.body)
          } catch {
            return
          }
          listeners.current[queue].forEach((fn) => fn(payload))
        }),
      )
    }
    client.onWebSocketClose = () => setConnected(false)
    client.onStompError = () => setConnected(false)
    client.activate()
    clientRef.current = client
    return () => {
      client.deactivate()
      clientRef.current = null
      setConnected(false)
    }
  }, [token, user])

  const subscribe = useCallback((queue, handler) => {
    listeners.current[queue].add(handler)
    return () => listeners.current[queue].delete(handler)
  }, [])

  const publish = useCallback((destination, body) => {
    const client = clientRef.current
    if (client?.connected) client.publish({ destination, body: JSON.stringify(body) })
  }, [])

  // Built-in listeners: toasts for notifications, unread counters, presence map.
  useEffect(() => {
    const offNotification = subscribe('notifications', (n) => {
      setUnreadNotifications((c) => c + 1)
      toast.info(n.title, n.message, { onClick: n.link ? () => navigate(n.link) : undefined })
    })
    const offMessage = subscribe('messages', (m) => {
      if (m.senderId !== user?.id && activeChatRef.current !== m.senderId) setUnreadMessages((c) => c + 1)
    })
    const offPresence = subscribe('presence', (p) => setPresence((map) => ({ ...map, [p.userId]: p.online })))
    return () => {
      offNotification()
      offMessage()
      offPresence()
    }
  }, [subscribe, toast, navigate, user])

  const setActiveChat = useCallback((userId) => {
    activeChatRef.current = userId
  }, [])

  const value = useMemo(
    () => ({
      connected,
      unreadNotifications,
      unreadMessages,
      presence,
      setUnreadNotifications,
      refreshCounts,
      subscribe,
      publish,
      setActiveChat,
    }),
    [connected, unreadNotifications, unreadMessages, presence, refreshCounts, subscribe, publish, setActiveChat],
  )

  return <NotificationContext.Provider value={value}>{children}</NotificationContext.Provider>
}

export const useRealtime = () => useContext(NotificationContext)
