import { useState } from 'react'
import { Ban, RotateCcw, Search, UsersRound } from 'lucide-react'
import Avatar from '../../components/ui/Avatar'
import Badge from '../../components/ui/Badge'
import Button from '../../components/ui/Button'
import ConfirmDialog from '../../components/ui/ConfirmDialog'
import { Input, Select } from '../../components/ui/Field'
import { PageHeader, Pagination } from '../../components/ui/Layout'
import { EmptyState, ErrorState, Skeleton } from '../../components/ui/Feedback'
import { RankBadge } from '../../components/SkillBits'
import { useAsync } from '../../hooks/useAsync'
import { adminService } from '../../services'
import { useToast } from '../../context/ToastContext'
import { formatDate, timeAgo } from '../../lib/format'

const STATUS = {
  ACTIVE: { label: 'Active', className: 'bg-emerald-50 text-emerald-700 ring-emerald-200' },
  SUSPENDED: { label: 'Suspended', className: 'bg-rose-50 text-rose-700 ring-rose-200' },
}

export default function AdminUsers() {
  const toast = useToast()
  const [query, setQuery] = useState('')
  const [q, setQ] = useState('')
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const [target, setTarget] = useState(null)
  const { data, loading, error, reload } = useAsync(() => adminService.users({ q: q || undefined, status: status || undefined, page, size: 20 }), [q, status, page])

  const act = async (fn, message) => {
    try {
      await fn()
      toast.success(message)
      reload({ silent: true })
    } catch (e) {
      toast.error('Action failed', e.message)
      throw e
    }
  }

  return (
    <div>
      <PageHeader eyebrow="Admin" title="Users" subtitle="Search, review and moderate accounts." />
      <div className="card mb-4 flex flex-col gap-3 p-4 sm:flex-row">
        <form className="relative flex-1" onSubmit={(e) => { e.preventDefault(); setPage(0); setQ(query.trim()) }}>
          <Search size={16} className="absolute top-1/2 left-3.5 -translate-y-1/2 text-slate-400" />
          <Input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search name, email or college" className="pl-10" />
        </form>
        <Select value={status} onChange={(e) => { setStatus(e.target.value); setPage(0) }} className="sm:w-48" aria-label="Status">
          <option value="">All statuses</option><option value="ACTIVE">Active</option><option value="SUSPENDED">Suspended</option>
        </Select>
      </div>
      {loading ? <Skeleton className="h-96 w-full rounded-2xl" /> : error ? <ErrorState message={error} onRetry={reload} /> : data.content.length === 0 ? (
        <EmptyState icon={UsersRound} title="No users found" />
      ) : (
        <>
          <div className="card overflow-x-auto">
            <table className="w-full min-w-[760px] text-left text-sm">
              <thead className="border-b border-slate-100 text-xs text-slate-500 uppercase">
                <tr><th className="px-4 py-3 font-semibold">User</th><th className="px-4 py-3 font-semibold">College</th><th className="px-4 py-3 font-semibold">Reputation</th><th className="px-4 py-3 font-semibold">Joined</th><th className="px-4 py-3 font-semibold">Status</th><th className="px-4 py-3" /></tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {data.content.map((u) => (
                  <tr key={u.id} className="hover:bg-slate-50/60">
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-3">
                        <Avatar user={u} size="sm" />
                        <div className="min-w-0">
                          <p className="font-medium text-slate-900">{u.fullName} {u.role === 'ADMIN' && <span className="chip ml-1 bg-slate-900 text-white">Admin</span>}</p>
                          <p className="text-xs text-slate-500">{u.email}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-slate-600"><p>{u.college}</p><p className="text-xs text-slate-400">{u.department} · Y{u.yearOfStudy}</p></td>
                    <td className="px-4 py-3"><RankBadge rank={u.rank} /><p className="mt-1 text-xs text-slate-500">{u.xp} XP · {u.ratingCount ? `${u.ratingAverage}★` : 'unrated'}</p></td>
                    <td className="px-4 py-3 text-slate-600"><p>{formatDate(u.createdAt)}</p><p className="text-xs text-slate-400">{u.lastActiveAt ? `seen ${timeAgo(u.lastActiveAt)}` : '—'}</p></td>
                    <td className="px-4 py-3"><Badge meta={STATUS[u.status]} />{u.suspensionReason && <p className="mt-1 max-w-[12rem] truncate text-xs text-slate-500" title={u.suspensionReason}>{u.suspensionReason}</p>}</td>
                    <td className="px-4 py-3 text-right">
                      {u.role !== 'ADMIN' && (u.status === 'ACTIVE'
                        ? <Button size="sm" variant="ghost" icon={Ban} className="text-rose-600 hover:bg-rose-50" onClick={() => setTarget(u)}>Suspend</Button>
                        : <Button size="sm" variant="soft" icon={RotateCcw} onClick={() => act(() => adminService.reactivate(u.id), `${u.fullName} reactivated`).catch(() => {})}>Reactivate</Button>)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
        </>
      )}
      <ConfirmDialog
        open={Boolean(target)}
        onClose={() => setTarget(null)}
        onConfirm={(reason) => act(() => adminService.suspend(target.id, reason), `${target.fullName} suspended`)}
        title={`Suspend ${target?.fullName}?`}
        message="They will be signed out immediately and their upcoming sessions will be cancelled with refunds."
        confirmLabel="Suspend user"
        withReason
        reasonPlaceholder="Reason shown to the user (optional)"
      />
    </div>
  )
}
