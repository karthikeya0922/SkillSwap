import { Link } from 'react-router-dom'
import { CalendarCheck } from 'lucide-react'
import Avatar from './ui/Avatar'
import ConnectButton from './ConnectButton'
import { MatchMeter, RankBadge, RatingInline, SkillChip } from './SkillBits'

/** Student card for Discover and Matches. */
export default function UserCard({ user, reasons }) {
  const teaches = user.teaches || []
  const learns = user.learns || []
  return (
    <div className="card card-hover flex h-full flex-col p-5">
      <div className="flex items-start gap-3">
        <Link to={`/users/${user.id}`}>
          <Avatar user={user} size="lg" online={user.online} />
        </Link>
        <div className="min-w-0 flex-1">
          <Link to={`/users/${user.id}`} className="block truncate font-semibold text-slate-900 hover:text-brand-700">
            {user.fullName}
          </Link>
          <p className="truncate text-xs text-slate-500">
            {user.department} · Year {user.yearOfStudy}
          </p>
          <div className="mt-1.5 flex flex-wrap items-center gap-2">
            <RatingInline value={user.ratingAverage} count={user.ratingCount} size={13} />
            <RankBadge rank={user.rank} />
          </div>
        </div>
        {user.match && <MatchMeter score={user.match.score} label={user.match.score >= 80 ? 'Great match' : null} />}
      </div>

      {reasons?.length > 0 && (
        <p className="mt-3 rounded-xl bg-orange-50/70 px-3 py-2 text-xs leading-relaxed text-orange-900">{reasons[0]}</p>
      )}

      <div className="mt-4 space-y-3">
        <div>
          <p className="mb-1.5 text-[11px] font-semibold tracking-wide text-slate-400 uppercase">Can teach</p>
          <div className="flex flex-wrap gap-1.5">
            {teaches.length ? (
              teaches.slice(0, 4).map((s) => <SkillChip key={s.skillId} name={s.name} level={s.level} color={s.categoryColor} size="sm" />)
            ) : (
              <span className="text-xs text-slate-400">No teaching skills yet</span>
            )}
            {teaches.length > 4 && <span className="chip bg-slate-100 text-slate-500">+{teaches.length - 4}</span>}
          </div>
        </div>
        {learns.length > 0 && (
          <div>
            <p className="mb-1.5 text-[11px] font-semibold tracking-wide text-slate-400 uppercase">Wants to learn</p>
            <div className="flex flex-wrap gap-1.5">
              {learns.slice(0, 3).map((s) => (
                <SkillChip key={s.skillId} name={s.name} color={s.categoryColor} size="sm" />
              ))}
            </div>
          </div>
        )}
      </div>

      <div className="mt-auto flex items-center justify-between gap-3 border-t border-slate-100 pt-4">
        <span className="inline-flex items-center gap-1.5 text-xs text-slate-500">
          <CalendarCheck size={14} /> {user.sessionsCompleted} sessions
        </span>
        <ConnectButton userId={user.id} connection={user.connection} />
      </div>
    </div>
  )
}
