import { Link } from 'react-router-dom'
import { Medal, Trophy } from 'lucide-react'
import Avatar from '../components/ui/Avatar'
import { PageHeader } from '../components/ui/Layout'
import { ErrorState, GridSkeleton } from '../components/ui/Feedback'
import { AchievementBadge, RankBadge, RatingInline } from '../components/SkillBits'
import { useAsync } from '../hooks/useAsync'
import { userService } from '../services'
import { useAuth } from '../context/AuthContext'

const PODIUM = ['text-amber-500', 'text-slate-400', 'text-orange-700']
const RANKS = [
  ['Beginner', 0],
  ['Contributor', 200],
  ['Mentor', 600],
  ['Expert', 1500],
  ['Community Master', 3000],
]

export default function Leaderboard() {
  const { user } = useAuth()
  const { data, loading, error, reload } = useAsync(() => Promise.all([userService.leaderboard(20), userService.badgeCatalog()]), [])

  return (
    <div>
      <PageHeader title="Leaderboard" subtitle="XP comes from teaching, learning, great ratings, helping on requests and challenges." />
      {loading ? <GridSkeleton count={3} /> : error ? <ErrorState message={error} onRetry={reload} /> : (
        <div className="grid gap-6 lg:grid-cols-3">
          <section className="card divide-y divide-slate-100 lg:col-span-2">
            {data[0].map((e) => (
              <Link key={e.userId} to={`/users/${e.userId}`} className={`flex items-center gap-4 p-4 hover:bg-slate-50 ${e.userId === user?.id ? 'bg-brand-50/50' : ''}`}>
                <span className="w-8 text-center text-lg font-bold text-slate-400">
                  {e.position <= 3 ? <Medal className={`mx-auto ${PODIUM[e.position - 1]}`} size={22} /> : e.position}
                </span>
                <Avatar user={{ id: e.userId, fullName: e.fullName, avatarUrl: e.avatarUrl }} size="md" />
                <div className="min-w-0 flex-1">
                  <p className="truncate font-semibold text-slate-900">{e.fullName}{e.userId === user?.id && ' (you)'}</p>
                  <p className="truncate text-xs text-slate-500">{e.department}</p>
                </div>
                <div className="hidden sm:block"><RatingInline value={e.ratingAverage} count={e.ratingAverage ? 1 : 0} size={12} /></div>
                <RankBadge rank={e.rank} />
                <span className="w-20 text-right font-bold text-brand-700">{e.xp} XP</span>
              </Link>
            ))}
          </section>
          <aside className="space-y-6">
            <section className="card p-5">
              <h2 className="section-title mb-3 flex items-center gap-2"><Trophy size={18} className="text-amber-500" /> Ranks</h2>
              <ul className="space-y-2">
                {RANKS.map(([name, xp]) => (
                  <li key={name} className="flex items-center justify-between"><RankBadge rank={name} /><span className="text-sm text-slate-500">{xp}+ XP</span></li>
                ))}
              </ul>
            </section>
            <section className="card p-5">
              <h2 className="section-title mb-3">Badges to earn</h2>
              <div className="space-y-2">{data[1].map((b) => <AchievementBadge key={b.code} badge={b} />)}</div>
            </section>
          </aside>
        </div>
      )}
    </div>
  )
}
