import { Link } from 'react-router-dom'
import { ArrowRight, BookOpen, CalendarCheck, CalendarDays, Coins, Flame, GraduationCap, HandHelping, Plus, Star, TrendingUp } from 'lucide-react'
import Button from '../components/ui/Button'
import Badge from '../components/ui/Badge'
import { StatCard } from '../components/ui/Layout'
import { CardSkeleton, EmptyState, ErrorState, Skeleton } from '../components/ui/Feedback'
import MatchCard from '../components/MatchCard'
import SessionCard from '../components/SessionCard'
import DynamicIcon from '../components/DynamicIcon'
import { RankBadge } from '../components/SkillBits'
import { useAsync } from '../hooks/useAsync'
import { userService } from '../services'
import { REQUEST_STATUS } from '../lib/constants'
import { formatCredits, greeting, timeAgo } from '../lib/format'

function DashboardSkeleton() {
  return (
    <div className="space-y-6">
      <Skeleton className="h-28 w-full rounded-2xl" />
      <div className="grid grid-cols-2 gap-4 lg:grid-cols-5">
        {Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-28 rounded-2xl" />)}
      </div>
      <div className="grid gap-4 lg:grid-cols-2">
        <CardSkeleton />
        <CardSkeleton />
      </div>
    </div>
  )
}

export default function Dashboard() {
  const { data, loading, error, reload } = useAsync(() => userService.dashboard(), [])
  if (loading) return <DashboardSkeleton />
  if (error) return <ErrorState message={error} onRetry={reload} />

  const { stats, rank, recommendedMatches, upcomingSessions, activeRequests, recentActivity, profileCompletion } = data

  return (
    <div className="space-y-8">
      <section className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-brand-600 via-brand-700 to-violet-700 p-6 text-white sm:p-8">
        <div className="absolute -top-16 -right-10 h-56 w-56 rounded-full bg-white/10 blur-2xl" />
        <div className="relative flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h1 className="text-2xl font-bold sm:text-3xl">
              {greeting()}, {data.firstName} 👋
            </h1>
            <p className="mt-1 text-brand-100">
              {data.pendingSessionRequests > 0
                ? `You have ${data.pendingSessionRequests} session request${data.pendingSessionRequests > 1 ? 's' : ''} waiting for your answer.`
                : 'Ready to learn something new or share what you know?'}
            </p>
            <div className="mt-4 flex flex-wrap gap-2">
              <Button to="/discover" className="bg-white text-brand-700 hover:bg-brand-50">Find a teacher</Button>
              <Button to="/requests?new=1" icon={Plus} className="bg-white/15 text-white hover:bg-white/25">New learning request</Button>
            </div>
          </div>
          <div className="w-full max-w-xs rounded-2xl bg-white/10 p-4 backdrop-blur">
            <div className="flex items-center justify-between">
              <RankBadge rank={rank.label} className="bg-white text-brand-700" />
              <span className="text-sm font-semibold">{rank.xp} XP</span>
            </div>
            <div className="mt-3 h-2 overflow-hidden rounded-full bg-white/20">
              <div className="h-full rounded-full bg-gradient-to-r from-amber-300 to-orange-400" style={{ width: `${rank.progress}%` }} />
            </div>
            <p className="mt-2 text-xs text-brand-100">
              {rank.nextLabel ? `${rank.nextXp - rank.xp} XP to ${rank.nextLabel}` : 'Top rank reached — legend!'}
            </p>
          </div>
        </div>
      </section>

      {profileCompletion.percent < 100 && (
        <div className="card flex flex-col gap-3 p-4 sm:flex-row sm:items-center">
          <div className="flex-1">
            <p className="text-sm font-semibold text-slate-900">Your profile is {profileCompletion.percent}% complete</p>
            <p className="text-xs text-slate-500">{profileCompletion.missing.join(' · ')}</p>
            <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-slate-100">
              <div className="h-full rounded-full bg-brand-500" style={{ width: `${profileCompletion.percent}%` }} />
            </div>
          </div>
          <Button size="sm" variant="soft" to="/profile/edit">Complete profile</Button>
        </div>
      )}

      <section className="grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-5">
        <StatCard icon={Coins} tone="amber" label="Skill Credits" value={formatCredits(stats.credits)} hint="Available to spend" />
        <StatCard icon={CalendarCheck} tone="emerald" label="Sessions" value={stats.sessionsCompleted} hint={`${stats.sessionsTaught} taught · ${stats.sessionsLearned} learned`} />
        <StatCard icon={GraduationCap} tone="violet" label="Skills taught" value={stats.skillsTaught} hint="Distinct skills" />
        <StatCard icon={BookOpen} tone="sky" label="Skills learned" value={stats.skillsLearned} hint="Distinct skills" />
        <StatCard icon={Star} tone="rose" label="Rating" value={stats.ratingCount ? stats.rating.toFixed(1) : '—'} hint={stats.ratingCount ? `${stats.ratingCount} reviews` : 'No reviews yet'} />
      </section>

      <section>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="section-title flex items-center gap-2"><Flame size={18} className="text-orange-500" /> Recommended matches</h2>
          <Link to="/matches" className="link inline-flex items-center gap-1 text-sm">See all <ArrowRight size={14} /></Link>
        </div>
        {recommendedMatches.length ? (
          <div className="grid gap-4 md:grid-cols-2 2xl:grid-cols-4">
            {recommendedMatches.map((m) => <MatchCard key={m.user.id} match={m} compact />)}
          </div>
        ) : (
          <EmptyState icon={Flame} title="No matches yet" message="Add skills you teach and want to learn to unlock matches." action={<Button to="/profile/edit" size="sm">Add skills</Button>} />
        )}
      </section>

      <div className="grid gap-6 lg:grid-cols-5">
        <section className="lg:col-span-3">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="section-title flex items-center gap-2"><CalendarDays size={18} className="text-brand-500" /> Upcoming sessions</h2>
            <Link to="/sessions" className="link text-sm">All sessions</Link>
          </div>
          {upcomingSessions.length ? (
            <div className="space-y-3">{upcomingSessions.map((s) => <SessionCard key={s.id} session={s} compact />)}</div>
          ) : (
            <EmptyState icon={CalendarDays} title="Nothing scheduled" message="Book a session with one of your matches to get started." action={<Button to="/discover" size="sm">Discover teachers</Button>} />
          )}
        </section>

        <section className="lg:col-span-2">
          <div className="mb-4 flex items-center justify-between">
            <h2 className="section-title flex items-center gap-2"><HandHelping size={18} className="text-violet-500" /> My learning requests</h2>
            <Link to="/requests?tab=mine" className="link text-sm">Manage</Link>
          </div>
          {activeRequests.length ? (
            <div className="card divide-y divide-slate-100">
              {activeRequests.map((r) => (
                <Link key={r.id} to={`/requests/${r.id}`} className="flex items-center gap-3 p-4 hover:bg-slate-50">
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-semibold text-slate-900">{r.title}</p>
                    <p className="text-xs text-slate-500">{r.skill.name} · {r.offerCount} offer{r.offerCount === 1 ? '' : 's'}</p>
                  </div>
                  <Badge meta={REQUEST_STATUS[r.status]} />
                </Link>
              ))}
            </div>
          ) : (
            <EmptyState icon={HandHelping} title="No active requests" message="Post what you want to learn and let teachers come to you." action={<Button to="/requests?new=1" size="sm" icon={Plus}>New request</Button>} />
          )}
        </section>
      </div>

      <section>
        <h2 className="section-title mb-4 flex items-center gap-2"><TrendingUp size={18} className="text-emerald-500" /> Recent activity</h2>
        {recentActivity.length ? (
          <div className="card divide-y divide-slate-100">
            {recentActivity.map((a, i) => (
              <div key={i} className="flex items-center gap-3 p-4">
                <div className="grid h-9 w-9 shrink-0 place-items-center rounded-full" style={{ background: `${a.color}1a`, color: a.color }}>
                  {a.kind === 'BADGE' ? <DynamicIcon name={a.icon} size={16} /> : <Coins size={16} />}
                </div>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-slate-800">{a.title}</p>
                  <p className="text-xs text-slate-500">{timeAgo(a.at)}</p>
                </div>
                {a.amount != null && (
                  <span className={`text-sm font-bold ${a.amount >= 0 ? 'text-emerald-600' : 'text-rose-600'}`}>
                    {formatCredits(a.amount, { sign: true })}
                  </span>
                )}
              </div>
            ))}
          </div>
        ) : (
          <EmptyState icon={TrendingUp} title="No activity yet" message="Your credits and achievements will show up here." />
        )}
      </section>
    </div>
  )
}
