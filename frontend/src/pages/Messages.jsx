import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { MessagesSquare, Search } from 'lucide-react'
import Avatar from '../components/ui/Avatar'
import Button from '../components/ui/Button'
import { Input } from '../components/ui/Field'
import { Skeleton } from '../components/ui/Feedback'
import ChatWindow from '../components/ChatWindow'
import { useAuth } from '../context/AuthContext'
import { useRealtime } from '../context/NotificationContext'
import { connectionService, messageService } from '../services'
import { timeAgo } from '../lib/format'

export default function Messages() {
  const { userId } = useParams()
  const navigate = useNavigate()
  const { user: me } = useAuth()
  const { subscribe, presence } = useRealtime()
  const [conversations, setConversations] = useState(null)
  const [connections, setConnections] = useState([])
  const [filter, setFilter] = useState('')

  const load = useCallback(() => {
    messageService.conversations().then(setConversations).catch(() => setConversations([]))
  }, [])

  useEffect(() => {
    load()
    connectionService.overview().then((o) => setConnections(o.connections)).catch(() => {})
  }, [load])

  // Keep the list fresh as messages arrive anywhere.
  useEffect(() => subscribe('messages', load), [subscribe, load])

  const activeId = userId ? Number(userId) : null
  const people = useMemo(() => {
    const byId = new Map()
    ;(conversations || []).forEach((c) => byId.set(c.partner.id, { ...c, id: c.partner.id }))
    connections.forEach((c) => {
      if (!byId.has(c.user.id)) byId.set(c.user.id, { id: c.user.id, partner: c.user, lastMessage: null, unreadCount: 0, online: c.online, connected: true })
    })
    const q = filter.trim().toLowerCase()
    return [...byId.values()].filter((p) => !q || p.partner.fullName.toLowerCase().includes(q))
  }, [conversations, connections, filter])

  const active = people.find((p) => p.id === activeId)
  const isOnline = (p) => presence[p.id] ?? p.online

  return (
    <div className="card -mx-4 flex h-[calc(100dvh-7.5rem)] overflow-hidden rounded-none sm:mx-0 sm:h-[calc(100dvh-9rem)] sm:rounded-2xl">
      <aside className={`w-full shrink-0 flex-col border-r border-slate-100 md:flex md:w-80 ${activeId ? 'hidden' : 'flex'}`}>
        <div className="border-b border-slate-100 p-4">
          <h1 className="text-lg font-bold text-slate-900">Messages</h1>
          <div className="relative mt-3">
            <Search size={15} className="absolute top-1/2 left-3 -translate-y-1/2 text-slate-400" />
            <Input value={filter} onChange={(e) => setFilter(e.target.value)} placeholder="Search conversations" className="py-2 pl-9" />
          </div>
        </div>
        <div className="scrollbar-thin flex-1 overflow-y-auto p-2">
          {conversations === null ? (
            Array.from({ length: 5 }).map((_, i) => (
              <div key={i} className="flex items-center gap-3 p-3">
                <Skeleton className="h-11 w-11 rounded-full" />
                <div className="flex-1 space-y-2"><Skeleton className="h-3 w-2/3" /><Skeleton className="h-3 w-1/2" /></div>
              </div>
            ))
          ) : people.length === 0 ? (
            <div className="px-4 py-10 text-center">
              <MessagesSquare className="mx-auto text-slate-300" size={32} />
              <p className="mt-2 text-sm text-slate-500">Connect with students to start chatting.</p>
              <Button size="sm" className="mt-4" to="/discover">Find students</Button>
            </div>
          ) : (
            people.map((p) => (
              <button
                key={p.id}
                onClick={() => navigate(`/messages/${p.id}`)}
                className={`flex w-full items-center gap-3 rounded-xl p-3 text-left transition ${p.id === activeId ? 'bg-brand-50' : 'hover:bg-slate-50'}`}
              >
                <Avatar user={p.partner} size="md" online={isOnline(p)} />
                <div className="min-w-0 flex-1">
                  <div className="flex items-center justify-between gap-2">
                    <p className="truncate text-sm font-semibold text-slate-900">{p.partner.fullName}</p>
                    {p.lastMessage && <span className="shrink-0 text-[11px] text-slate-400">{timeAgo(p.lastMessage.createdAt)}</span>}
                  </div>
                  <div className="flex items-center justify-between gap-2">
                    <p className={`truncate text-xs ${p.unreadCount ? 'font-semibold text-slate-800' : 'text-slate-500'}`}>
                      {p.lastMessage ? `${p.lastMessage.senderId === me.id ? 'You: ' : ''}${p.lastMessage.content}` : 'Start a conversation'}
                    </p>
                    {p.unreadCount > 0 && p.id !== activeId && (
                      <span className="grid h-5 min-w-5 place-items-center rounded-full bg-brand-600 px-1.5 text-[10px] font-bold text-white">{p.unreadCount}</span>
                    )}
                  </div>
                </div>
              </button>
            ))
          )}
        </div>
      </aside>

      <section className={`min-w-0 flex-1 ${activeId ? 'flex' : 'hidden md:flex'}`}>
        {active && active.connected ? (
          <div className="flex min-h-0 w-full flex-col">
            <ChatWindow me={me} partner={active.partner} online={isOnline(active)} onBack={() => navigate('/messages')} onActivity={load} />
          </div>
        ) : active ? (
          <div className="grid w-full place-items-center p-8 text-center text-sm text-slate-500">
            You're no longer connected with {active.partner.fullName}. Reconnect to keep chatting.
          </div>
        ) : activeId && conversations !== null ? (
          <div className="grid w-full place-items-center p-8 text-center">
            <div>
              <p className="text-sm text-slate-500">You can only message your connections.</p>
              <Button size="sm" className="mt-3" to={`/users/${activeId}`}>View profile</Button>
            </div>
          </div>
        ) : (
          <div className="grid w-full place-items-center p-8 text-center">
            <div>
              <div className="mx-auto grid h-16 w-16 place-items-center rounded-2xl bg-brand-50 text-brand-600"><MessagesSquare size={28} /></div>
              <p className="mt-4 font-semibold text-slate-900">Your conversations</p>
              <p className="mt-1 text-sm text-slate-500">Pick a conversation to start chatting in real time.</p>
            </div>
          </div>
        )}
      </section>
    </div>
  )
}
