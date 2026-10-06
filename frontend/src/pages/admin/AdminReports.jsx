import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Flag, Gavel } from 'lucide-react'
import Avatar from '../../components/ui/Avatar'
import Badge from '../../components/ui/Badge'
import Button from '../../components/ui/Button'
import Modal from '../../components/ui/Modal'
import { Field, Select, Textarea } from '../../components/ui/Field'
import { PageHeader, Pagination, Tabs } from '../../components/ui/Layout'
import { EmptyState, ErrorState, GridSkeleton } from '../../components/ui/Feedback'
import { useAsync } from '../../hooks/useAsync'
import { adminService } from '../../services'
import { useToast } from '../../context/ToastContext'
import { timeAgo, titleCase } from '../../lib/format'

const STATUS = {
  OPEN: { label: 'Open', className: 'bg-rose-50 text-rose-700 ring-rose-200' },
  IN_REVIEW: { label: 'In review', className: 'bg-amber-50 text-amber-700 ring-amber-200' },
  RESOLVED: { label: 'Resolved', className: 'bg-emerald-50 text-emerald-700 ring-emerald-200' },
  DISMISSED: { label: 'Dismissed', className: 'bg-slate-100 text-slate-600 ring-slate-200' },
}
const ACTIONS = [
  ['NONE', 'No action'],
  ['WARNING', 'Send a warning'],
  ['CONTENT_REMOVED', 'Remove the content'],
  ['USER_SUSPENDED', 'Suspend the user'],
]

function ReviewModal({ report, onClose, onSaved }) {
  const toast = useToast()
  const [status, setStatus] = useState(report.status === 'OPEN' ? 'RESOLVED' : report.status)
  const [action, setAction] = useState('NONE')
  const [note, setNote] = useState(report.adminNote || '')
  const [busy, setBusy] = useState(false)
  const actions = ACTIONS.filter(([value]) => !(value === 'CONTENT_REMOVED' && report.targetType === 'USER'))
  const save = async () => {
    setBusy(true)
    try {
      await adminService.reviewReport(report.id, { status: action !== 'NONE' ? 'RESOLVED' : status, action, adminNote: note.trim() || null })
      toast.success('Report updated')
      onSaved()
      onClose()
    } catch (e) {
      toast.error('Could not update report', e.message)
    } finally {
      setBusy(false)
    }
  }
  return (
    <Modal open onClose={onClose} title="Review report" description={`${titleCase(report.reason)} · ${titleCase(report.targetType)} #${report.targetId}`}
      footer={<><Button variant="secondary" onClick={onClose}>Cancel</Button><Button icon={Gavel} loading={busy} onClick={save}>Save decision</Button></>}>
      <div className="space-y-4">
        <div className="rounded-xl bg-slate-50 p-3 text-sm">
          <p className="text-xs font-medium text-slate-500">Reported content</p>
          <p className="mt-1 whitespace-pre-line text-slate-800">{report.targetPreview || '—'}</p>
          {report.details && <><p className="mt-3 text-xs font-medium text-slate-500">Reporter's note</p><p className="mt-1 text-slate-700">{report.details}</p></>}
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Action">
            <Select value={action} onChange={(e) => setAction(e.target.value)}>{actions.map(([v, l]) => <option key={v} value={v}>{l}</option>)}</Select>
          </Field>
          <Field label="Status" hint={action !== 'NONE' ? 'Taking an action resolves the report.' : undefined}>
            <Select value={action !== 'NONE' ? 'RESOLVED' : status} disabled={action !== 'NONE'} onChange={(e) => setStatus(e.target.value)}>
              {Object.entries(STATUS).map(([v, m]) => <option key={v} value={v}>{m.label}</option>)}
            </Select>
          </Field>
        </div>
        <Field label="Admin note" hint={action === 'USER_SUSPENDED' ? 'Used as the suspension reason.' : 'Internal note for the moderation log.'}>
          <Textarea rows={3} maxLength={1000} value={note} onChange={(e) => setNote(e.target.value)} />
        </Field>
      </div>
    </Modal>
  )
}

export default function AdminReports() {
  const [status, setStatus] = useState('OPEN')
  const [page, setPage] = useState(0)
  const [reviewing, setReviewing] = useState(null)
  const { data, loading, error, reload } = useAsync(() => adminService.reports({ status: status || undefined, page, size: 20 }), [status, page])

  return (
    <div>
      <PageHeader eyebrow="Admin" title="Reports" subtitle="Review reports from the community and take action." />
      <Tabs tabs={[{ value: 'OPEN', label: 'Open' }, { value: 'IN_REVIEW', label: 'In review' }, { value: 'RESOLVED', label: 'Resolved' }, { value: 'DISMISSED', label: 'Dismissed' }, { value: '', label: 'All' }]} value={status} onChange={(v) => { setStatus(v); setPage(0) }} className="mb-4" />
      {loading ? <GridSkeleton count={3} className="space-y-3" /> : error ? <ErrorState message={error} onRetry={reload} /> : data.content.length === 0 ? (
        <EmptyState icon={Flag} title="No reports here" message="Nice — the community is behaving." />
      ) : (
        <>
          <div className="space-y-3">
            {data.content.map((r) => (
              <div key={r.id} className="card flex flex-col gap-4 p-4 sm:flex-row sm:items-start">
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <Badge meta={STATUS[r.status]} />
                    <span className="chip bg-rose-50 text-rose-700">{titleCase(r.reason)}</span>
                    <span className="chip bg-slate-100 text-slate-600">{titleCase(r.targetType)}</span>
                    {r.actionTaken !== 'NONE' && <span className="chip bg-slate-900 text-white">{titleCase(r.actionTaken)}</span>}
                    <span className="text-xs text-slate-400">{timeAgo(r.createdAt)}</span>
                  </div>
                  <p className="mt-2 line-clamp-2 text-sm text-slate-800">{r.targetPreview}</p>
                  {r.details && <p className="mt-1 text-sm text-slate-500">“{r.details}”</p>}
                  <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-500">
                    <span className="inline-flex items-center gap-1.5">Reported by <Avatar user={r.reporter} size="xs" /> {r.reporter.fullName}</span>
                    {r.reportedUser && <span>Against <b className="text-slate-700">{r.reportedUser.fullName}</b> ({r.reportedUserEmail})</span>}
                    {r.resolvedByName && <span>Handled by {r.resolvedByName}</span>}
                  </div>
                  {r.adminNote && <p className="mt-2 rounded-lg bg-slate-50 px-3 py-2 text-xs text-slate-600">Note: {r.adminNote}</p>}
                </div>
                <div className="flex gap-2">
                  {r.reportedUser && <Button size="sm" variant="secondary" to={`/admin/users`}>Users</Button>}
                  <Button size="sm" icon={Gavel} onClick={() => setReviewing(r)}>Review</Button>
                </div>
              </div>
            ))}
          </div>
          <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
        </>
      )}
      {reviewing && <ReviewModal report={reviewing} onClose={() => setReviewing(null)} onSaved={() => reload({ silent: true })} />}
      <p className="mt-6 text-center text-xs text-slate-400">Removed messages are blanked; removed posts, submissions, groups and challenges are deleted. <Link to="/admin" className="link">Back to analytics</Link></p>
    </div>
  )
}
