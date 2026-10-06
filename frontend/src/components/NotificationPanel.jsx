import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  Award,
  Bell,
  CalendarCheck,
  CalendarClock,
  CalendarX,
  CheckCheck,
  Handshake,
  Info,
  MessageCircle,
  Sparkles,
  Star,
  Trophy,
  UserPlus,
  Users,
} from 'lucide-react'
import { notificationService } from '../services'
import { useRealtime } from '../context/NotificationContext'
import { timeAgo } from '../lib/format'
import { Spinner } from './ui/Feedback'

export const NOTIFICATION_ICONS = {
  CONNECTION_REQUEST: [UserPlus, 'bg-sky-50 text-sky-600'],
  CONNECTION_ACCEPTED: [Handshake, 'bg-emerald-50 text-emerald-600'],
  NEW_MESSAGE: [MessageCircle, 'bg-brand-50 text-brand-600'],
  SKILL_MATCH: [Sparkles, 'bg-orange-50 text-orange-600'],
  SESSION_REQUEST: [CalendarClock, 'bg-amber-50 text-amber-600'],
  SESSION_ACCEPTED: [CalendarCheck, 'bg-emerald-50 text-emerald-600'],
  SESSION_REJECTED: [CalendarX, 'bg-rose-50 text-rose-600'],
  SESSION_CANCELLED: [CalendarX, 'bg-rose-50 text-rose-600'],
  SESSION_COMPLETED: [CalendarCheck, 'bg-emerald-50 text-emerald-600'],
  UPCOMING_SESSION: [CalendarClock, 'bg-brand-50 text-brand-600'],
  RATING_RECEIVED: [Star, 'bg-amber-50 text-amber-600'],
  REQUEST_RESPONSE: [Handshake, 'bg-violet-50 text-violet-600'],
  REQUEST_ACCEPTED: [Handshake, 'bg-emerald-50 text-emerald-600'],
  GROUP_INVITATION: [Users, 'bg-sky-50 text-sky-600'],
  GROUP_EVENT: [Users, 'bg-brand-50 text-brand-600'],
  CHALLENGE_REVIEW: [Trophy, 'bg-emerald-50 text-emerald-600'],
  BADGE_EARNED: [Award, 'bg-amber-50 text-amber-600'],
  CREDITS: [Sparkles, 'bg-emerald-50 text-emerald-600'],
  SYSTEM: [Info, 'bg-slate-100 text-slate-600'],
}

export function NotificationItem({ n, onOpen }) {
  const [Icon, tone] = NOTIFICATION_ICONS[n.type] || NOTIFICATION_ICONS.SYSTEM
  return (
    <button
      onClick={() => onOpen(n)}
      className={`flex w-full gap-3 rounded-xl px-3 py-3 text-left transition hover:bg-slate-50 ${n.read ? '' : 'bg-brand-50/40'}`}
    >
      <div className={`grid h-9 w-9 shrink-0 place-items-center rounded-full ${tone}`}>
        <Icon size={16} />
      </div>
      <div className="min-w-0 flex-1">
        <p className="text-sm font-semibold text-slate-900">{n.title}</p>
        <p className="mt-0.5 line-clamp-2 text-xs text-slate-600">{n.message}</p>
        <p className="mt-1 text-[11px] text-slate-400">{timeAgo(n.createdAt)}</p>
      </div>
      {!n.read && <span className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-brand-500" />}
    </button>
  )
}

/** Bell icon with a dropdown of the latest notifications. */
export default function NotificationPanel() {
  const { unreadNotifications, setUnreadNotifications, subscribe } = useRealtime()
  const [open, setOpen] = useState(false)
  const [items, setItems] = useState(null)
  const ref = useRef(null)
  const navigate = useNavigate()

  useEffect(() => {
    if (!open) return undefined
    notificationService
      .list({ size: 8 })
      .then((page) => setItems(page.content))
      .catch(() => setItems([]))
    const close = (e) => ref.current && !ref.current.contains(e.target) && setOpen(false)
    document.addEventListener('mousedown', close)
    return () => document.removeEventListener('mousedown', close)
  }, [open])

  useEffect(() => subscribe('notifications', (n) => setItems((list) => (list ? [n, ...list].slice(0, 8) : list))), [subscribe])

  const openItem = (n) => {
    if (!n.read) {
      notificationService.markRead(n.id).catch(() => {})
      setUnreadNotifications((c) => Math.max(0, c - 1))
      setItems((list) => list?.map((x) => (x.id === n.id ? { ...x, read: true } : x)))
    }
    setOpen(false)
    if (n.link) navigate(n.link)
  }

  const markAll = () => {
    notificationService.markAllRead().catch(() => {})
    setUnreadNotifications(0)
    setItems((list) => list?.map((x) => ({ ...x, read: true })))
  }

  return (
    <div className="relative" ref={ref}>
      <button
        onClick={() => setOpen((o) => !o)}
        className="btn-ghost relative grid h-10 w-10 place-items-center rounded-xl"
        aria-label={`Notifications${unreadNotifications ? `, ${unreadNotifications} unread` : ''}`}
      >
        <Bell size={19} />
        {unreadNotifications > 0 && (
          <span className="absolute top-1.5 right-1.5 grid h-4 min-w-4 place-items-center rounded-full bg-rose-500 px-1 text-[10px] font-bold text-white">
            {unreadNotifications > 9 ? '9+' : unreadNotifications}
          </span>
        )}
      </button>
      {open && (
        <div className="fixed inset-x-3 top-16 z-40 animate-slide-up rounded-2xl border border-slate-200 bg-white shadow-lift sm:absolute sm:inset-x-auto sm:top-12 sm:right-0 sm:w-96">
          <div className="flex items-center justify-between border-b border-slate-100 px-4 py-3">
            <p className="font-semibold text-slate-900">Notifications</p>
            <button onClick={markAll} className="inline-flex items-center gap-1 text-xs font-medium text-brand-600 hover:text-brand-700">
              <CheckCheck size={14} /> Mark all read
            </button>
          </div>
          <div className="scrollbar-thin max-h-[60vh] overflow-y-auto p-2">
            {items === null ? (
              <div className="grid place-items-center py-8">
                <Spinner />
              </div>
            ) : items.length === 0 ? (
              <p className="py-8 text-center text-sm text-slate-500">You're all caught up ✨</p>
            ) : (
              items.map((n) => <NotificationItem key={n.id} n={n} onOpen={openItem} />)
            )}
          </div>
          <Link
            to="/notifications"
            onClick={() => setOpen(false)}
            className="block border-t border-slate-100 py-3 text-center text-sm font-medium text-brand-600 hover:bg-slate-50"
          >
            View all notifications
          </Link>
        </div>
      )}
    </div>
  )
}
