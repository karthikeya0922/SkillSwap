import { useState } from 'react'
import { Flame } from 'lucide-react'
import { PageHeader, Pagination, Tabs } from '../components/ui/Layout'
import { EmptyState, ErrorState, GridSkeleton } from '../components/ui/Feedback'
import Button from '../components/ui/Button'
import MatchCard from '../components/MatchCard'
import { useAsync } from '../hooks/useAsync'
import { matchService } from '../services'

const TABS = [
  { value: 'all', label: 'All matches' },
  { value: 'mutual', label: 'Two-way swaps' },
  { value: 'great', label: '🔥 80%+' },
]

export default function Matches() {
  const [tab, setTab] = useState('all')
  const [page, setPage] = useState(0)
  const { data, loading, error, reload } = useAsync(
    () => matchService.list({ page, size: 12, mutualOnly: tab === 'mutual', minScore: tab === 'great' ? 80 : 0 }),
    [tab, page],
  )

  return (
    <div>
      <PageHeader
        title="Your skill matches"
        subtitle="Scored on what you want to learn, what you can teach, shared interests, availability and ratings."
      />
      <Tabs
        tabs={TABS}
        value={tab}
        onChange={(t) => {
          setTab(t)
          setPage(0)
        }}
        className="mb-6"
      />
      {loading ? (
        <GridSkeleton />
      ) : error ? (
        <ErrorState message={error} onRetry={reload} />
      ) : data.content.length === 0 ? (
        <EmptyState
          icon={Flame}
          title="No matches here yet"
          message="Matches appear when someone teaches what you want to learn, or wants to learn what you teach."
          action={<Button to="/profile/edit">Update my skills</Button>}
        />
      ) : (
        <>
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {data.content.map((m) => <MatchCard key={m.user.id} match={m} />)}
          </div>
          <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  )
}
