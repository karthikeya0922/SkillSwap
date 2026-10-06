import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Check, MessageCircle, UserMinus, UsersRound, X } from 'lucide-react'
import Avatar from '../components/ui/Avatar'
import Button from '../components/ui/Button'
import ConfirmDialog from '../components/ui/ConfirmDialog'
import { PageHeader, Tabs } from '../components/ui/Layout'
import { EmptyState, ErrorState, GridSkeleton } from '../components/ui/Feedback'
import { RankBadge } from '../components/SkillBits'
import { useAsync } from '../hooks/useAsync'
import { connectionService } from '../services'
import { useToast } from '../context/ToastContext'
import { useRealtime } from '../context/NotificationContext'
import { timeAgo } from '../lib/format'

export default function Connections() {
  const toast = useToast()
  const { presence } = useRealtime()
  const [tab, setTab] = useState('connections')
  const [removing, setRemoving] = useState(null)
  const [busyId, setBusyId] = useState(null)
  const { data, loading, error, reload } = useAsync(() => connectionService.overview(), [])

  const respond = async (c, action) => {
    setBusyId(c.id)
    try {
      await connectionService.respond(c.id, action)
      toast.success(action === 'ACCEPT' ? `You're now connected with ${c.user.fullName}` : 'Request declined')
      reload({ silent: true })
    } catch (e) {
      toast.error('Could not respond', e.message)
    } finally {
      setBusyId(null)
    }
  }

  const remove = async () => {
    try {
      await connectionService.remove(removing.id)
      toast.success(removing.status === 'PENDING' ? 'Request cancelled' : 'Connection removed')
      reload({ silent: true })
    } catch (e) {
      toast.error('Could not remove', e.message)
      throw e
    }
  }

  const list = data ? data[tab] : []
  const tabs = [
    { value: 'connections', label: 'Connections', count: data?.connections.length },
    { value: 'incoming', label: 'Requests', count: data?.incoming.length },
    { value: 'outgoing', label: 'Sent', count: data?.outgoing.length },
  ]

  return (
    <div>
      <PageHeader title="Connections" subtitle="Connected students can chat with you privately." />
      <Tabs tabs={tabs} value={tab} onChange={setTab} className="mb-6" />
      {loading ? (
        <GridSkeleton count={4} className="grid gap-3 md:grid-cols-2" />
      ) : error ? (
        <ErrorState message={error} onRetry={reload} />
      ) : list.length === 0 ? (
        <EmptyState
          icon={UsersRound}
          title={tab === 'connections' ? 'No connections yet' : tab === 'incoming' ? 'No pending requests' : 'No sent requests'}
          message="Find students on the Discover page and send them a connection request."
          action={<Button to="/discover">Discover students</Button>}
        />
      ) : (
        <div className="grid gap-3 md:grid-cols-2">
          {list.map((c) => (
            <div key={c.id} className="card flex flex-col gap-3 p-4">
              <div className="flex items-center gap-3">
                <Link to={`/users/${c.user.id}`}>
                  <Avatar user={c.user} size="md" online={tab === 'connections' ? (presence[c.user.id] ?? c.online) : undefined} />
                </Link>
                <div className="min-w-0 flex-1">
                  <Link to={`/users/${c.user.id}`} className="block truncate font-semibold text-slate-900 hover:text-brand-700">{c.user.fullName}</Link>
                  <p className="truncate text-xs text-slate-500">{c.user.department} · {c.user.college}</p>
                </div>
                <RankBadge rank={c.user.rank} />
              </div>
              {c.message && tab !== 'connections' && (
                <p className="rounded-xl bg-slate-50 px-3 py-2 text-sm text-slate-600">“{c.message}”</p>
              )}
              <div className="flex items-center justify-between gap-2">
                <span className="text-xs text-slate-400">
                  {tab === 'connections' ? `Connected ${timeAgo(c.respondedAt || c.createdAt)}` : `Sent ${timeAgo(c.createdAt)}`}
                </span>
                <div className="flex gap-2">
                  {tab === 'connections' && (
                    <>
                      <Button size="sm" variant="soft" icon={MessageCircle} to={`/messages/${c.user.id}`}>Message</Button>
                      <Button size="sm" variant="ghost" icon={UserMinus} onClick={() => setRemoving(c)} aria-label="Remove connection" />
                    </>
                  )}
                  {tab === 'incoming' && (
                    <>
                      <Button size="sm" variant="secondary" icon={X} disabled={busyId === c.id} onClick={() => respond(c, 'REJECT')}>Decline</Button>
                      <Button size="sm" variant="success" icon={Check} loading={busyId === c.id} onClick={() => respond(c, 'ACCEPT')}>Accept</Button>
                    </>
                  )}
                  {tab === 'outgoing' && (
                    <Button size="sm" variant="secondary" onClick={() => setRemoving(c)}>Cancel request</Button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
      <ConfirmDialog
        open={Boolean(removing)}
        onClose={() => setRemoving(null)}
        onConfirm={remove}
        title={removing?.status === 'PENDING' ? 'Cancel this request?' : `Remove ${removing?.user.fullName}?`}
        message={removing?.status === 'PENDING' ? 'They will no longer see your request.' : 'You will no longer be able to chat until you reconnect.'}
        confirmLabel={removing?.status === 'PENDING' ? 'Cancel request' : 'Remove'}
      />
    </div>
  )
}
