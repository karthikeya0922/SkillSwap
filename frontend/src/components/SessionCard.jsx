import { Link } from 'react-router-dom'
import { Clock, Coins, MapPin, Video } from 'lucide-react'
import Avatar from './ui/Avatar'
import Badge from './ui/Badge'
import { SESSION_STATUS } from '../lib/constants'
import { formatCredits, formatDuration, formatShortDate, formatTime, parseDate } from '../lib/format'

export default function SessionCard({ session, compact = false }) {
  const teaching = session.myRole === 'TEACHER'
  const other = teaching ? session.learner : session.teacher
  const start = parseDate(session.startAt)
  return (
    <Link
      to={`/sessions/${session.id}`}
      className={`card card-hover flex items-center gap-4 ${compact ? 'p-3.5' : 'p-4'}`}
    >
      <div className="grid w-14 shrink-0 place-items-center rounded-xl bg-brand-50 py-2 text-center">
        <span className="text-[11px] font-semibold text-brand-600 uppercase">
          {start.toLocaleDateString('en-IN', { month: 'short' })}
        </span>
        <span className="text-xl leading-none font-bold text-brand-800">{start.getDate()}</span>
      </div>
      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-center gap-2">
          <p className="truncate font-semibold text-slate-900">{session.skill.name}</p>
          <Badge meta={SESSION_STATUS[session.status]} />
        </div>
        <div className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-slate-500">
          <span className="inline-flex items-center gap-1">
            <Clock size={12} /> {formatShortDate(session.startAt)}, {formatTime(session.startAt)} · {formatDuration(session.durationMinutes)}
          </span>
          {!compact && (
            <span className="inline-flex items-center gap-1">
              {session.mode === 'ONLINE' ? <Video size={12} /> : <MapPin size={12} />}
              {session.mode === 'ONLINE' ? 'Online' : session.location || 'Offline'}
            </span>
          )}
        </div>
      </div>
      <div className="hidden items-center gap-2 sm:flex">
        <div className="text-right">
          <p className="text-[11px] text-slate-400">{teaching ? 'Teaching' : 'Learning from'}</p>
          <p className="max-w-[9rem] truncate text-sm font-medium text-slate-700">{other.fullName}</p>
        </div>
        <Avatar user={other} size="sm" />
      </div>
      <span className={`chip shrink-0 ${teaching ? 'bg-emerald-50 text-emerald-700' : 'bg-amber-50 text-amber-700'}`}>
        <Coins size={12} />
        {teaching ? '+' : '-'}
        {formatCredits(session.credits)}
      </span>
    </Link>
  )
}
