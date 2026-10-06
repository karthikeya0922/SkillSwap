import { useState } from 'react'
import { CalendarDays, Compass } from 'lucide-react'
import Button from '../components/ui/Button'
import { Select } from '../components/ui/Field'
import { PageHeader, Pagination, Tabs } from '../components/ui/Layout'
import { EmptyState, ErrorState, GridSkeleton } from '../components/ui/Feedback'
import SessionCard from '../components/SessionCard'
import { useAsync } from '../hooks/useAsync'
import { sessionService } from '../services'

const SCOPES = [
  { value: 'upcoming', label: 'Upcoming' },
  { value: 'pending', label: 'Requests' },
  { value: 'past', label: 'Past' },
]

const EMPTY = {
  upcoming: ['No upcoming sessions', 'Accepted and scheduled sessions will show up here.'],
  pending: ['No pending requests', 'Session requests waiting for a teacher’s answer appear here.'],
  past: ['No past sessions yet', 'Completed, cancelled and declined sessions are kept here.'],
}

export default function Sessions() {
  const [scope, setScope] = useState('upcoming')
  const [role, setRole] = useState('ALL')
  const [page, setPage] = useState(0)
  const { data, loading, error, reload } = useAsync(() => sessionService.list({ scope, role, page, size: 10 }), [scope, role, page])

  return (
    <div>
      <PageHeader
        title="Sessions"
        subtitle="Your teaching and learning sessions in one place."
        actions={<Button icon={Compass} to="/discover">Find a teacher</Button>}
      />
      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <Tabs tabs={SCOPES} value={scope} onChange={(v) => { setScope(v); setPage(0) }} />
        <Select value={role} onChange={(e) => { setRole(e.target.value); setPage(0) }} className="sm:w-48" aria-label="Role">
          <option value="ALL">Teaching & learning</option>
          <option value="TEACHING">Teaching only</option>
          <option value="LEARNING">Learning only</option>
        </Select>
      </div>
      {loading ? (
        <GridSkeleton count={4} className="space-y-3" />
      ) : error ? (
        <ErrorState message={error} onRetry={reload} />
      ) : data.content.length === 0 ? (
        <EmptyState icon={CalendarDays} title={EMPTY[scope][0]} message={EMPTY[scope][1]} action={<Button to="/matches" variant="secondary">See your matches</Button>} />
      ) : (
        <>
          <div className="space-y-3">{data.content.map((s) => <SessionCard key={s.id} session={s} />)}</div>
          <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  )
}
