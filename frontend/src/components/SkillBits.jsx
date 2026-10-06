import { Star } from 'lucide-react'
import { LEVEL_META, RANK_STYLE } from '../lib/constants'
import DynamicIcon from './DynamicIcon'

/** Skill name pill tinted with its category colour, with optional level dots. */
export function SkillChip({ name, level, color = '#6366f1', size = 'md' }) {
  const meta = level ? LEVEL_META[level] : null
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full border font-medium ${size === 'sm' ? 'px-2 py-0.5 text-[11px]' : 'px-2.5 py-1 text-xs'}`}
      style={{ borderColor: `${color}33`, backgroundColor: `${color}12`, color: '#1e293b' }}
      title={meta ? `${name} · ${meta.label}` : name}
    >
      <span className="h-1.5 w-1.5 rounded-full" style={{ backgroundColor: color }} />
      {name}
      {meta && (
        <span className="flex gap-0.5" aria-hidden>
          {[1, 2, 3, 4].map((i) => (
            <span key={i} className="h-1 w-1 rounded-full" style={{ backgroundColor: i <= meta.dots ? color : `${color}33` }} />
          ))}
        </span>
      )}
    </span>
  )
}

export function LevelBadge({ level }) {
  const meta = LEVEL_META[level]
  if (!meta) return null
  return <span className={`chip ${meta.className}`}>{meta.label}</span>
}

export function RankBadge({ rank, className = '' }) {
  if (!rank) return null
  return <span className={`chip font-semibold ${RANK_STYLE[rank] || RANK_STYLE.Beginner} ${className}`}>{rank}</span>
}

/** Read-only rating display, e.g. ★ 4.8 (23). */
export function RatingInline({ value, count, size = 14 }) {
  if (!count) return <span className="text-xs text-slate-400">New teacher</span>
  return (
    <span className="inline-flex items-center gap-1 text-sm font-semibold text-slate-800">
      <Star size={size} className="fill-amber-400 text-amber-400" />
      {Number(value).toFixed(1)}
      <span className="font-normal text-slate-400">({count})</span>
    </span>
  )
}

/** Five stars; interactive when `onChange` is given. */
export function RatingStars({ value = 0, onChange, size = 20, label }) {
  return (
    <div className="flex items-center gap-1" role={onChange ? 'radiogroup' : undefined} aria-label={label}>
      {[1, 2, 3, 4, 5].map((n) => {
        const filled = n <= Math.round(value)
        const star = <Star size={size} className={filled ? 'fill-amber-400 text-amber-400' : 'text-slate-300'} />
        return onChange ? (
          <button
            key={n}
            type="button"
            onClick={() => onChange(n)}
            className="rounded transition hover:scale-110"
            aria-label={`${n} star${n > 1 ? 's' : ''}`}
            aria-checked={n === value}
            role="radio"
          >
            {star}
          </button>
        ) : (
          <span key={n}>{star}</span>
        )
      })}
    </div>
  )
}

export function AchievementBadge({ badge, compact = false }) {
  return (
    <div className={`flex items-center gap-3 ${compact ? '' : 'rounded-xl border border-slate-100 bg-slate-50/60 p-3'}`} title={badge.description}>
      <div
        className="grid h-10 w-10 shrink-0 place-items-center rounded-xl text-white shadow-sm"
        style={{ background: `linear-gradient(135deg, ${badge.color}, ${badge.color}cc)` }}
      >
        <DynamicIcon name={badge.icon} size={18} />
      </div>
      {!compact && (
        <div className="min-w-0">
          <p className="truncate text-sm font-semibold text-slate-900">{badge.name}</p>
          <p className="truncate text-xs text-slate-500">{badge.description}</p>
        </div>
      )}
    </div>
  )
}

export function MatchMeter({ score, label, size = 'md' }) {
  const great = score >= 80
  const color = great ? 'text-orange-600' : score >= 60 ? 'text-brand-600' : 'text-slate-600'
  return (
    <div className="text-right">
      <p className={`${size === 'lg' ? 'text-3xl' : 'text-xl'} leading-none font-extrabold ${color}`}>{score}%</p>
      {label && (
        <p className={`mt-1 text-[11px] font-semibold whitespace-nowrap ${color}`}>
          {great && '🔥 '}
          {label}
        </p>
      )}
    </div>
  )
}
