import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { Building, CalendarCheck, CalendarPlus, Clock, Coins, Flag, GraduationCap, Pencil, Star, Users } from 'lucide-react'
import Avatar from '../components/ui/Avatar'
import Button from '../components/ui/Button'
import { Pagination } from '../components/ui/Layout'
import { EmptyState, ErrorState, Skeleton } from '../components/ui/Feedback'
import ConnectButton from '../components/ConnectButton'
import ReportModal from '../components/ReportModal'
import { AchievementBadge, LevelBadge, MatchMeter, RankBadge, RatingStars } from '../components/SkillBits'
import { useAsync } from '../hooks/useAsync'
import { ratingService, userService } from '../services'
import { formatCredits, formatDate, timeAgo } from '../lib/format'

function SkillCard({ skill, teach }) {
  return (
    <div className="rounded-xl border border-slate-200 p-3.5">
      <div className="flex items-center gap-2">
        <span className="h-2.5 w-2.5 rounded-full" style={{ backgroundColor: skill.categoryColor }} />
        <p className="flex-1 truncate font-semibold text-slate-900">{skill.skillName}</p>
        <LevelBadge level={skill.level} />
      </div>
      <p className="mt-1 text-xs text-slate-500">
        {skill.categoryName}
        {teach && skill.yearsExperience != null && ` · ${skill.yearsExperience} yrs experience`}
      </p>
      {skill.description && <p className="mt-2 text-sm text-slate-600">{skill.description}</p>}
    </div>
  )
}

function Reviews({ userId }) {
  const [page, setPage] = useState(0)
  const { data, loading } = useAsync(() => ratingService.forUser(userId, { page, size: 5 }), [userId, page])
  if (loading && !data) return <Skeleton className="h-40 w-full rounded-2xl" />
  if (!data || data.count === 0) return <EmptyState icon={Star} title="No reviews yet" message="Reviews from learners will appear here." />
  return (
    <div className="card p-5">
      <div className="grid gap-6 border-b border-slate-100 pb-5 sm:grid-cols-[auto_1fr]">
        <div className="text-center sm:pr-6">
          <p className="text-5xl font-extrabold text-slate-900">{data.average.toFixed(1)}</p>
          <RatingStars value={data.average} size={16} />
          <p className="mt-1 text-xs text-slate-500">{data.count} reviews</p>
        </div>
        <div className="space-y-1.5">
          {Object.entries(data.distribution).map(([stars, count]) => (
            <div key={stars} className="flex items-center gap-2 text-xs text-slate-500">
              <span className="w-3">{stars}</span>
              <div className="h-2 flex-1 overflow-hidden rounded-full bg-slate-100">
                <div className="h-full rounded-full bg-amber-400" style={{ width: `${(count / data.count) * 100}%` }} />
              </div>
              <span className="w-6 text-right">{count}</span>
            </div>
          ))}
          <div className="flex flex-wrap gap-x-4 gap-y-1 pt-2 text-xs text-slate-600">
            <span>Teaching <b>{data.teachingQuality}</b></span>
            <span>Communication <b>{data.communication}</b></span>
            <span>Knowledge <b>{data.knowledge}</b></span>
          </div>
        </div>
      </div>
      <ul className="divide-y divide-slate-100">
        {data.reviews.content.map((r) => (
          <li key={r.id} className="flex gap-3 py-4">
            <Avatar user={r.rater} size="sm" />
            <div className="min-w-0 flex-1">
              <div className="flex flex-wrap items-center gap-2">
                <p className="text-sm font-semibold text-slate-900">{r.rater.fullName}</p>
                <RatingStars value={r.stars} size={13} />
                <span className="text-xs text-slate-400">{timeAgo(r.createdAt)}</span>
              </div>
              <p className="text-xs text-slate-500">{r.skillName} session</p>
              {r.feedback && <p className="mt-1.5 text-sm text-slate-700">{r.feedback}</p>}
            </div>
          </li>
        ))}
      </ul>
      <Pagination page={data.reviews.page} totalPages={data.reviews.totalPages} onChange={setPage} />
    </div>
  )
}

export default function Profile({ self = false }) {
  const { id } = useParams()
  const [reporting, setReporting] = useState(false)
  const { data: p, loading, error, reload } = useAsync(() => (self ? userService.myProfile() : userService.getProfile(id)), [self, id])

  if (loading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-56 w-full rounded-3xl" />
        <Skeleton className="h-40 w-full rounded-2xl" />
      </div>
    )
  }
  if (error) return <ErrorState message={error} onRetry={reload} />

  const stats = [
    [CalendarCheck, 'Completed sessions', p.stats.completedSessions, 'bg-emerald-50 text-emerald-600'],
    [GraduationCap, 'Sessions taught', p.stats.sessionsTaught, 'bg-violet-50 text-violet-600'],
    [Coins, 'Credits earned', formatCredits(p.stats.creditsEarned), 'bg-amber-50 text-amber-600'],
    [Users, 'Connections', p.stats.connections, 'bg-sky-50 text-sky-600'],
  ]

  return (
    <div className="space-y-6">
      <section className="card overflow-hidden">
        <div className="h-28 bg-gradient-to-r from-brand-500 via-violet-500 to-fuchsia-400 sm:h-36" />
        <div className="px-5 pb-6 sm:px-8">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-start">
            <Avatar user={p} size="xl" online={p.self ? undefined : p.online} className="-mt-12 rounded-full ring-4 ring-white" />
            <div className="min-w-0 flex-1 sm:pt-3">
              <div className="flex flex-wrap items-center gap-2">
                <h1 className="text-2xl font-bold text-slate-900">{p.fullName}</h1>
                <RankBadge rank={p.rank.label} />
              </div>
              <p className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-slate-500">
                <span className="inline-flex items-center gap-1"><Building size={14} /> {p.college}</span>
                <span>{p.department} · Year {p.yearOfStudy}</span>
                <span className="inline-flex items-center gap-1"><Clock size={14} /> Joined {formatDate(p.memberSince)}</span>
              </p>
            </div>
            <div className="flex flex-wrap gap-2 sm:pt-3">
              {p.self ? (
                <Button icon={Pencil} to="/profile/edit">Edit profile</Button>
              ) : (
                <>
                  {p.teachSkills.length > 0 && (
                    <Button variant="secondary" icon={CalendarPlus} to={`/sessions/new?teacherId=${p.id}`}>Book session</Button>
                  )}
                  <ConnectButton userId={p.id} connection={p.connection} size="md" />
                  <Button variant="ghost" icon={Flag} onClick={() => setReporting(true)} aria-label="Report user" />
                </>
              )}
            </div>
          </div>

          <div className="mt-6 grid gap-6 lg:grid-cols-[1fr_auto]">
            <div>
              {p.bio ? <p className="max-w-2xl leading-relaxed text-slate-700">{p.bio}</p> : <p className="text-sm text-slate-400 italic">No bio yet.</p>}
              <div className="mt-4 flex flex-wrap items-center gap-3">
                <span className="inline-flex items-center gap-1.5 rounded-full bg-amber-50 px-3 py-1 text-sm font-semibold text-amber-800">
                  <Star size={15} className="fill-amber-400 text-amber-400" />
                  {p.ratingCount ? `${p.ratingAverage.toFixed(1)} rating` : 'No ratings yet'}
                  {p.ratingCount > 0 && <span className="font-normal text-amber-700">({p.ratingCount})</span>}
                </span>
                {p.availabilityLabels.map((a) => <span key={a} className="chip bg-slate-100 text-slate-600">{a}</span>)}
              </div>
              <div className="mt-4 max-w-md">
                <div className="flex justify-between text-xs text-slate-500">
                  <span>{p.rank.xp} XP</span>
                  <span>{p.rank.nextLabel ? `${p.rank.nextXp} XP → ${p.rank.nextLabel}` : 'Max rank'}</span>
                </div>
                <div className="mt-1 h-2 overflow-hidden rounded-full bg-slate-100">
                  <div className="h-full rounded-full bg-gradient-to-r from-brand-500 to-violet-500" style={{ width: `${p.rank.progress}%` }} />
                </div>
              </div>
            </div>
            {p.match && (
              <div className="rounded-2xl bg-orange-50/70 p-4 lg:w-80">
                <div className="flex items-start justify-between gap-3">
                  <p className="text-sm font-semibold text-orange-900">Why you match</p>
                  <MatchMeter score={p.match.score} label={p.match.label} />
                </div>
                <ul className="mt-2 space-y-1.5 text-sm text-orange-900/90">
                  {p.match.reasons.map((r) => <li key={r}>• {r}</li>)}
                </ul>
              </div>
            )}
          </div>
        </div>
      </section>

      <section className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        {stats.map(([Icon, label, value, tone]) => (
          <div key={label} className="card flex items-center gap-3 p-4">
            <div className={`grid h-10 w-10 place-items-center rounded-xl ${tone}`}><Icon size={18} /></div>
            <div>
              <p className="text-xl font-bold text-slate-900">{value}</p>
              <p className="text-xs text-slate-500">{label}</p>
            </div>
          </div>
        ))}
      </section>

      <div className="grid gap-6 lg:grid-cols-2">
        <section className="card p-5">
          <h2 className="section-title mb-4">Can teach</h2>
          {p.teachSkills.length ? (
            <div className="grid gap-3">{p.teachSkills.map((s) => <SkillCard key={s.id} skill={s} teach />)}</div>
          ) : (
            <p className="text-sm text-slate-500">No teaching skills listed yet.</p>
          )}
        </section>
        <section className="card p-5">
          <h2 className="section-title mb-4">Wants to learn</h2>
          {p.learnSkills.length ? (
            <div className="grid gap-3">{p.learnSkills.map((s) => <SkillCard key={s.id} skill={s} />)}</div>
          ) : (
            <p className="text-sm text-slate-500">No learning goals listed yet.</p>
          )}
        </section>
      </div>

      <section className="card p-5">
        <h2 className="section-title mb-4">Badges</h2>
        {p.badges.length ? (
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">{p.badges.map((b) => <AchievementBadge key={b.code} badge={b} />)}</div>
        ) : (
          <p className="text-sm text-slate-500">Complete sessions, help with requests and join challenges to earn badges.</p>
        )}
      </section>

      <section>
        <h2 className="section-title mb-4">Reviews from learners</h2>
        <Reviews userId={p.id} />
      </section>

      {!p.self && <ReportModal open={reporting} onClose={() => setReporting(false)} targetType="USER" targetId={p.id} targetLabel={p.fullName} />}
    </div>
  )
}
