import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowLeft, Check, CheckCheck, Flag, Send } from 'lucide-react'
import Avatar from './ui/Avatar'
import { Spinner } from './ui/Feedback'
import ReportModal from './ReportModal'
import { messageService } from '../services'
import { useRealtime } from '../context/NotificationContext'
import { useToast } from '../context/ToastContext'
import { formatTime, parseDate } from '../lib/format'

const PAGE = 40

function dayLabel(value) {
  const d = parseDate(value)
  const today = new Date()
  const yesterday = new Date()
  yesterday.setDate(today.getDate() - 1)
  if (d.toDateString() === today.toDateString()) return 'Today'
  if (d.toDateString() === yesterday.toDateString()) return 'Yesterday'
  return d.toLocaleDateString('en-IN', { weekday: 'long', day: 'numeric', month: 'short' })
}

/** One-to-one conversation with live delivery, typing indicator and read receipts. */
export default function ChatWindow({ me, partner, online, onBack, onActivity }) {
  const { subscribe, publish, setActiveChat, refreshCounts } = useRealtime()
  const toast = useToast()
  const [messages, setMessages] = useState([])
  const [loading, setLoading] = useState(true)
  const [hasMore, setHasMore] = useState(false)
  const [loadingMore, setLoadingMore] = useState(false)
  const [error, setError] = useState(null)
  const [text, setText] = useState('')
  const [sending, setSending] = useState(false)
  const [partnerTyping, setPartnerTyping] = useState(false)
  const [reportTarget, setReportTarget] = useState(null)
  const scrollRef = useRef(null)
  const stickToBottom = useRef(true)
  const typingSent = useRef(0)
  const typingTimer = useRef(null)

  const markRead = useCallback(() => {
    messageService.markRead(partner.id).then(() => refreshCounts()).catch(() => {})
  }, [partner.id, refreshCounts])

  useEffect(() => {
    setActiveChat(partner.id)
    setLoading(true)
    setError(null)
    setMessages([])
    stickToBottom.current = true
    messageService
      .history(partner.id, { size: PAGE })
      .then((list) => {
        setMessages(list)
        setHasMore(list.length === PAGE)
        markRead()
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
    return () => setActiveChat(null)
  }, [partner.id, setActiveChat, markRead])

  useEffect(() => {
    const offMessages = subscribe('messages', (m) => {
      const inThread = (m.senderId === partner.id && m.recipientId === me.id) || (m.senderId === me.id && m.recipientId === partner.id)
      if (!inThread) return
      stickToBottom.current = true
      setMessages((list) => (list.some((x) => x.id === m.id) ? list : [...list, m]))
      if (m.senderId === partner.id) {
        setPartnerTyping(false)
        markRead()
      }
      onActivity?.()
    })
    const offReceipts = subscribe('read-receipts', (r) => {
      if (r.readerId !== partner.id) return
      setMessages((list) => list.map((m) => (m.senderId === me.id && !m.readAt ? { ...m, readAt: r.readAt } : m)))
    })
    const offTyping = subscribe('typing', (t) => {
      if (t.userId !== partner.id) return
      setPartnerTyping(t.typing)
      clearTimeout(typingTimer.current)
      if (t.typing) typingTimer.current = setTimeout(() => setPartnerTyping(false), 5000)
    })
    return () => {
      offMessages()
      offReceipts()
      offTyping()
      clearTimeout(typingTimer.current)
    }
  }, [subscribe, partner.id, me.id, markRead, onActivity])

  useLayoutEffect(() => {
    const el = scrollRef.current
    if (el && stickToBottom.current) el.scrollTop = el.scrollHeight
  }, [messages, partnerTyping])

  const loadOlder = async () => {
    if (!messages.length) return
    const el = scrollRef.current
    const previousHeight = el.scrollHeight
    setLoadingMore(true)
    stickToBottom.current = false
    try {
      const older = await messageService.history(partner.id, { before: messages[0].id, size: PAGE })
      setHasMore(older.length === PAGE)
      setMessages((list) => [...older, ...list])
      requestAnimationFrame(() => {
        el.scrollTop = el.scrollHeight - previousHeight
      })
    } catch (e) {
      toast.error('Could not load older messages', e.message)
    } finally {
      setLoadingMore(false)
    }
  }

  const notifyTyping = (typing) => {
    const now = Date.now()
    if (typing && now - typingSent.current < 2500) return
    typingSent.current = typing ? now : 0
    publish('/app/chat.typing', { recipientId: partner.id, typing })
  }

  const send = async (e) => {
    e.preventDefault()
    const content = text.trim()
    if (!content || sending) return
    setSending(true)
    notifyTyping(false)
    try {
      const message = await messageService.send(partner.id, content)
      stickToBottom.current = true
      setMessages((list) => (list.some((x) => x.id === message.id) ? list : [...list, message]))
      setText('')
      onActivity?.()
    } catch (err) {
      toast.error('Message not sent', err.message)
    } finally {
      setSending(false)
    }
  }

  const dayBreaks = new Set()
  messages.reduce((prev, m) => {
    const day = dayLabel(m.createdAt)
    if (day !== prev) dayBreaks.add(m.id)
    return day
  }, null)

  return (
    <div className="flex h-full min-h-0 flex-col">
      <div className="flex items-center gap-3 border-b border-slate-100 px-4 py-3">
        {onBack && (
          <button onClick={onBack} className="btn-ghost -ml-1 rounded-lg p-1.5 md:hidden" aria-label="Back to conversations">
            <ArrowLeft size={18} />
          </button>
        )}
        <Link to={`/users/${partner.id}`} className="flex min-w-0 flex-1 items-center gap-3">
          <Avatar user={partner} size="md" online={online} />
          <div className="min-w-0">
            <p className="truncate font-semibold text-slate-900">{partner.fullName}</p>
            <p className={`text-xs ${partnerTyping ? 'text-brand-600' : online ? 'text-emerald-600' : 'text-slate-400'}`}>
              {partnerTyping ? 'typing…' : online ? 'Online' : 'Offline'}
            </p>
          </div>
        </Link>
      </div>

      <div ref={scrollRef} className="scrollbar-thin flex-1 space-y-1 overflow-y-auto bg-slate-50/60 px-4 py-4" onScroll={(e) => {
        const el = e.currentTarget
        stickToBottom.current = el.scrollHeight - el.scrollTop - el.clientHeight < 80
      }}>
        {loading ? (
          <div className="grid h-full place-items-center"><Spinner /></div>
        ) : error ? (
          <p className="py-10 text-center text-sm text-rose-600">{error}</p>
        ) : (
          <>
            {hasMore && (
              <div className="pb-2 text-center">
                <button onClick={loadOlder} disabled={loadingMore} className="text-xs font-medium text-brand-600 hover:underline">
                  {loadingMore ? 'Loading…' : 'Load earlier messages'}
                </button>
              </div>
            )}
            {messages.length === 0 && (
              <div className="py-16 text-center">
                <p className="text-3xl">👋</p>
                <p className="mt-2 text-sm text-slate-500">Say hello to {partner.fullName.split(' ')[0]} and plan your first swap!</p>
              </div>
            )}
            {messages.map((m) => {
              const mine = m.senderId === me.id
              const showDay = dayBreaks.has(m.id)
              return (
                <div key={m.id}>
                  {showDay && (
                    <div className="my-3 text-center">
                      <span className="rounded-full bg-white px-3 py-1 text-[11px] font-medium text-slate-500 shadow-sm">{dayLabel(m.createdAt)}</span>
                    </div>
                  )}
                  <div className={`group flex ${mine ? 'justify-end' : 'justify-start'}`}>
                    {!mine && !m.removed && (
                      <button onClick={() => setReportTarget(m)} className="mr-1 self-center text-slate-300 opacity-0 transition group-hover:opacity-100 hover:text-rose-500" aria-label="Report message">
                        <Flag size={12} />
                      </button>
                    )}
                    <div className={`max-w-[80%] rounded-2xl px-3.5 py-2 text-sm shadow-sm sm:max-w-[65%] ${
                      m.removed ? 'bg-slate-100 text-slate-400 italic' : mine ? 'rounded-br-md bg-brand-600 text-white' : 'rounded-bl-md bg-white text-slate-800'
                    }`}>
                      <p className="break-words whitespace-pre-wrap">{m.content}</p>
                      <p className={`mt-0.5 flex items-center justify-end gap-1 text-[10px] ${mine ? 'text-brand-100' : 'text-slate-400'}`}>
                        {formatTime(m.createdAt)}
                        {mine && (m.readAt ? <CheckCheck size={12} aria-label="Read" /> : <Check size={12} aria-label="Sent" />)}
                      </p>
                    </div>
                  </div>
                </div>
              )
            })}
            {partnerTyping && (
              <div className="flex justify-start">
                <div className="flex gap-1 rounded-2xl rounded-bl-md bg-white px-4 py-3 shadow-sm">
                  {[0, 150, 300].map((d) => <span key={d} className="h-1.5 w-1.5 animate-bounce rounded-full bg-slate-400" style={{ animationDelay: `${d}ms` }} />)}
                </div>
              </div>
            )}
          </>
        )}
      </div>

      <form onSubmit={send} className="flex items-end gap-2 border-t border-slate-100 bg-white p-3">
        <textarea
          rows={1}
          value={text}
          maxLength={2000}
          onChange={(e) => {
            setText(e.target.value)
            notifyTyping(Boolean(e.target.value))
          }}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey) send(e)
          }}
          placeholder="Write a message…"
          className="input max-h-32 min-h-[44px] flex-1 resize-none"
          aria-label="Message"
        />
        <button type="submit" disabled={!text.trim() || sending} className="btn btn-primary h-11 w-11 shrink-0 p-0" aria-label="Send">
          <Send size={17} />
        </button>
      </form>

      <ReportModal
        open={Boolean(reportTarget)}
        onClose={() => setReportTarget(null)}
        targetType="MESSAGE"
        targetId={reportTarget?.id}
        targetLabel={`a message from ${partner.fullName}`}
      />
    </div>
  )
}
