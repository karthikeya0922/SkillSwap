import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Bell, CheckCheck, Trash } from 'lucide-react'
import Button from '../components/ui/Button'
import { PageHeader, Pagination, Tabs } from '../components/ui/Layout'
import { EmptyState, ErrorState, GridSkeleton } from '../components/ui/Feedback'
import { NotificationItem } from '../components/NotificationPanel'
import { useAsync } from '../hooks/useAsync'
import { notificationService } from '../services'
import { useRealtime } from '../context/NotificationContext'
import { useToast } from '../context/ToastContext'

export default function Notifications() {
  const navigate = useNavigate()
  const toast = useToast()
  const { setUnreadNotifications, subscribe } = useRealtime()
  const [filter, setFilter] = useState('all')
  const [page, setPage] = useState(0)
  const { data, loading, error, reload, setData } = useAsync(() => notificationService.list({ unreadOnly: filter === 'unread', page, size: 20 }), [filter, page])

  useEffect(() => subscribe('notifications', () => reload({ silent: true })), [subscribe, reload])

  const open = (n) => {
    if (!n.read) {
      notificationService.markRead(n.id).catch(() => {})
      setUnreadNotifications((c) => Math.max(0, c - 1))
    }
    if (n.link) navigate(n.link)
    else setData((d) => ({ ...d, content: d.content.map((x) => (x.id === n.id ? { ...x, read: true } : x)) }))
  }

  const markAll = async () => {
    await notificationService.markAllRead()
    setUnreadNotifications(0)
    toast.success('All caught up!')
    reload({ silent: true })
  }

  const remove = async (n) => {
    try {
      await notificationService.remove(n.id)
      if (!n.read) setUnreadNotifications((c) => Math.max(0, c - 1))
      setData((d) => ({ ...d, content: d.content.filter((x) => x.id !== n.id) }))
    } catch (e) {
      toast.error('Could not delete', e.message)
    }
  }

  return (
    <div className="mx-auto max-w-3xl">
      <PageHeader title="Notifications" actions={<Button variant="secondary" icon={CheckCheck} onClick={markAll}>Mark all read</Button>} />
      <Tabs tabs={[{ value: 'all', label: 'All' }, { value: 'unread', label: 'Unread' }]} value={filter} onChange={(v) => { setFilter(v); setPage(0) }} className="mb-4" />
      {loading ? <GridSkeleton count={4} className="space-y-3" /> : error ? <ErrorState message={error} onRetry={reload} /> : data.content.length === 0 ? (
        <EmptyState icon={Bell} title="Nothing here" message="Connection requests, session updates and ratings will show up here." />
      ) : (
        <>
          <div className="card divide-y divide-slate-100 p-2">
            {data.content.map((n) => (
              <div key={n.id} className="group flex items-center">
                <div className="flex-1"><NotificationItem n={n} onOpen={open} /></div>
                <button onClick={() => remove(n)} className="mr-2 p-2 text-slate-300 opacity-0 transition group-hover:opacity-100 hover:text-rose-500" aria-label="Delete notification"><Trash size={15} /></button>
              </div>
            ))}
          </div>
          <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  )
}
