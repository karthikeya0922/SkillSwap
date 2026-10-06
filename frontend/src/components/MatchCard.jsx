import { Link } from 'react-router-dom'
import { ArrowLeftRight, CalendarPlus } from 'lucide-react'
import Avatar from './ui/Avatar'
import Button from './ui/Button'
import ConnectButton from './ConnectButton'
import { MatchMeter, RatingInline, SkillChip } from './SkillBits'

/** Explains a match: who teaches what to whom, plus the score. */
export default function MatchCard({ match, compact = false }) {
  const { user, match: result } = match
  const colorFor = (name) =>
    [...(user.teaches || []), ...(user.learns || [])].find((s) => s.name === name)?.categoryColor || '#6366f1'
  return (
    <div className="card card-hover flex h-full flex-col p-5">
      <div className="flex items-start gap-3">
        <Link to={`/users/${user.id}`}>
          <Avatar user={user} size={compact ? 'md' : 'lg'} online={user.online} />
        </Link>
        <div className="min-w-0 flex-1">
          <Link to={`/users/${user.id}`} className="block truncate font-semibold text-slate-900 hover:text-brand-700">
            {user.fullName}
          </Link>
          <p className="truncate text-xs text-slate-500">{user.department}</p>
          <div className="mt-1">
            <RatingInline value={user.ratingAverage} count={user.ratingCount} size={13} />
          </div>
        </div>
        <MatchMeter score={result.score} label={compact ? null : result.label} />
      </div>

      <div className="mt-4 space-y-2">
        {result.theyCanTeach.length > 0 && (
          <div className="flex flex-wrap items-center gap-1.5 text-xs text-slate-500">
            <span className="font-medium text-slate-700">Teaches you</span>
            {result.theyCanTeach.map((s) => (
              <SkillChip key={s} name={s} color={colorFor(s)} size="sm" />
            ))}
          </div>
        )}
        {result.youCanTeach.length > 0 && (
          <div className="flex flex-wrap items-center gap-1.5 text-xs text-slate-500">
            <span className="font-medium text-slate-700">Learns from you</span>
            {result.youCanTeach.map((s) => (
              <SkillChip key={s} name={s} color={colorFor(s)} size="sm" />
            ))}
          </div>
        )}
      </div>

      {!compact && <p className="mt-3 text-sm leading-relaxed text-slate-600">{result.explanation}</p>}

      <div className="mt-auto flex items-center gap-2 pt-4">
        {result.mutual && (
          <span className="chip mr-auto bg-orange-50 whitespace-nowrap text-orange-700">
            <ArrowLeftRight size={12} /> Two-way swap
          </span>
        )}
        <div className={`flex gap-2 ${result.mutual ? '' : 'ml-auto'}`}>
          {result.theyCanTeach.length > 0 && !compact && (
            <Button size="sm" variant="secondary" icon={CalendarPlus} to={`/sessions/new?teacherId=${user.id}`}>
              Book
            </Button>
          )}
          <ConnectButton userId={user.id} connection={user.connection} />
        </div>
      </div>
    </div>
  )
}
