import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, CalendarPlus, Check, CircleCheck, Clock, Flag, HandHelping, MapPin, Pencil, Undo2, Video, X } from 'lucide-react'
import Avatar from '../components/ui/Avatar'
import Badge from '../components/ui/Badge'
import Button from '../components/ui/Button'
import ConfirmDialog from '../components/ui/ConfirmDialog'
import { Textarea } from '../components/ui/Field'
import { CardSkeleton, ErrorState } from '../components/ui/Feedback'
import RequestFormModal from '../components/RequestFormModal'
import ReportModal from '../components/ReportModal'
import { LevelBadge, RatingInline, SkillChip } from '../components/SkillBits'
import { useAsync } from '../hooks/useAsync'
import { requestService } from '../services'
import { useToast } from '../context/ToastContext'
import { OFFER_STATUS, REQUEST_STATUS } from '../lib/constants'
import { formatCredits, timeAgo } from '../lib/format'

export default function RequestDetail() {
  const { id } = useParams()
  const toast = useToast()
  const { data, loading, error, reload } = useAsync(() => requestService.get(id), [id])
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)
  const [editing, setEditing] = useState(false)
  const [confirm, setConfirm] = useState(null)
  const [reporting, setReporting] = useState(false)

  if (loading) return <CardSkeleton lines={6} />
  if (error) return <ErrorState message={error} onRetry={reload} />

  const { request: r, offers, myOffer } = data
  const open = r.status === 'OPEN' || r.status === 'PENDING'

  const act = async (fn, success) => {
    setBusy(true)
    try {
      await fn()
      toast.success(success)
      await reload({ silent: true })
    } catch (e) {
      toast.error('Something went wrong', e.message)
      throw e
    } finally {
      setBusy(false)
    }
  }

  const sendOffer = () => {
    if (!message.trim()) {
      toast.error('Add a short message for the learner')
      return
    }
    act(() => requestService.offer(r.id, message.trim()), 'Offer sent!').then(() => setMessage('')).catch(() => {})
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <Link to="/requests" className="inline-flex items-center gap-1 text-sm text-slate-500 hover:text-slate-800">
        <ArrowLeft size={14} /> Learning requests
      </Link>

      <section className="card p-5 sm:p-7">
        <div className="flex flex-wrap items-center gap-2">
          <SkillChip name={r.skill.name} color={r.skill.categoryColor} />
          <Badge meta={REQUEST_STATUS[r.status]} />
          {!r.mine && (
            <button onClick={() => setReporting(true)} className="ml-auto inline-flex items-center gap-1 text-xs text-slate-400 hover:text-rose-600">
              <Flag size={12} /> Report
            </button>
          )}
        </div>
        <h1 className="mt-3 text-2xl font-bold text-slate-900">{r.title}</h1>
        <p className="mt-3 leading-relaxed whitespace-pre-line text-slate-700">{r.description}</p>
        <div className="mt-5 grid gap-3 rounded-2xl bg-slate-50 p-4 text-sm sm:grid-cols-4">
          <div><p className="text-xs text-slate-500">Target level</p><div className="mt-1"><LevelBadge level={r.desiredLevel} /></div></div>
          <div><p className="text-xs text-slate-500">Mode</p><p className="mt-1 inline-flex items-center gap-1 font-medium">{r.mode === 'ONLINE' ? <Video size={14} /> : <MapPin size={14} />}{r.mode === 'ONLINE' ? 'Online' : 'Offline'}</p></div>
          <div><p className="text-xs text-slate-500">Duration</p><p className="mt-1 inline-flex items-center gap-1 font-medium"><Clock size={14} /> {formatCredits(r.durationHours)} hours</p></div>
          <div><p className="text-xs text-slate-500">Schedule</p><p className="mt-1 font-medium">{r.preferredSchedule || 'Flexible'}</p></div>
        </div>
        <div className="mt-5 flex flex-wrap items-center justify-between gap-3">
          <Link to={`/users/${r.learner.id}`} className="flex items-center gap-2">
            <Avatar user={r.learner} size="sm" />
            <div>
              <p className="text-sm font-semibold text-slate-900">{r.mine ? 'You' : r.learner.fullName}</p>
              <p className="text-xs text-slate-500">Posted {timeAgo(r.createdAt)}</p>
            </div>
          </Link>
          {r.mine && (
            <div className="flex flex-wrap gap-2">
              {open && <Button variant="secondary" size="sm" icon={Pencil} onClick={() => setEditing(true)}>Edit</Button>}
              {r.status === 'ACCEPTED' && (
                <>
                  <Button size="sm" icon={CalendarPlus} to={`/sessions/new?teacherId=${r.acceptedTeacher.id}&skillId=${r.skill.id}&requestId=${r.id}`}>Book session</Button>
                  <Button size="sm" variant="secondary" icon={CircleCheck} onClick={() => setConfirm('complete')}>Mark completed</Button>
                </>
              )}
              {(open || r.status === 'ACCEPTED') && <Button size="sm" variant="ghost" icon={X} onClick={() => setConfirm('cancel')}>Cancel request</Button>}
            </div>
          )}
        </div>
      </section>

      {r.status === 'ACCEPTED' && r.acceptedTeacher && (
        <div className="card flex items-center gap-3 border-emerald-200 bg-emerald-50/50 p-4">
          <Avatar user={r.acceptedTeacher} size="md" />
          <div className="flex-1">
            <p className="text-sm text-emerald-800">{r.mine ? 'Your teacher' : 'Teacher'}</p>
            <p className="font-semibold text-slate-900">{r.acceptedTeacher.fullName}</p>
          </div>
          {r.mine && <Button size="sm" variant="soft" to={`/messages/${r.acceptedTeacher.id}`}>Message</Button>}
        </div>
      )}

      {r.mine ? (
        <section>
          <h2 className="section-title mb-3">Offers from teachers ({offers.length})</h2>
          {offers.length === 0 ? (
            <div className="card p-6 text-center text-sm text-slate-500">
              <HandHelping className="mx-auto mb-2 text-slate-300" size={28} />
              No offers yet. Teachers of {r.skill.name} will see your request and can respond here.
            </div>
          ) : (
            <div className="space-y-3">
              {offers.map((o) => (
                <div key={o.id} className="card flex flex-col gap-3 p-4 sm:flex-row sm:items-start">
                  <Link to={`/users/${o.teacher.id}`}><Avatar user={o.teacher} size="md" /></Link>
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <Link to={`/users/${o.teacher.id}`} className="font-semibold text-slate-900 hover:text-brand-700">{o.teacher.fullName}</Link>
                      <RatingInline value={o.teacher.ratingAverage} count={o.teacher.ratingCount} size={12} />
                      <Badge meta={OFFER_STATUS[o.status]} />
                    </div>
                    <p className="mt-1 text-sm text-slate-700">{o.message}</p>
                    <p className="mt-1 text-xs text-slate-400">{timeAgo(o.createdAt)}</p>
                  </div>
                  {o.status === 'PENDING' && open && (
                    <Button size="sm" variant="success" icon={Check} loading={busy} onClick={() => act(() => requestService.acceptOffer(r.id, o.id), `${o.teacher.fullName} is your teacher! Book a session next.`).catch(() => {})}>
                      Accept offer
                    </Button>
                  )}
                </div>
              ))}
            </div>
          )}
        </section>
      ) : myOffer ? (
        <section className="card p-5">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="text-sm font-semibold text-slate-900">Your offer</p>
              <p className="mt-1 text-sm text-slate-600">{myOffer.message}</p>
            </div>
            <div className="flex items-center gap-2">
              <Badge meta={OFFER_STATUS[myOffer.status]} />
              {myOffer.status === 'PENDING' && (
                <Button size="sm" variant="ghost" icon={Undo2} loading={busy} onClick={() => act(() => requestService.withdrawOffer(r.id), 'Offer withdrawn').catch(() => {})}>Withdraw</Button>
              )}
            </div>
          </div>
        </section>
      ) : open ? (
        <section className="card p-5">
          <h2 className="section-title">Offer to help</h2>
          {r.canOffer ? (
            <>
              <p className="muted mt-1 mb-3">Introduce yourself and explain how you'd teach this. You'll earn 1 credit per hour taught.</p>
              <Textarea rows={3} maxLength={800} value={message} onChange={(e) => setMessage(e.target.value)} placeholder={`Hi ${r.learner.fullName.split(' ')[0]}! I can help you with ${r.skill.name}…`} />
              <div className="mt-3 flex justify-end"><Button icon={HandHelping} loading={busy} onClick={sendOffer}>Send offer</Button></div>
            </>
          ) : (
            <p className="mt-1 text-sm text-slate-500">
              Add <b>{r.skill.name}</b> to the skills you teach to offer help. <Link to="/profile/edit" className="link">Update skills</Link>
            </p>
          )}
        </section>
      ) : null}

      <RequestFormModal open={editing} onClose={() => setEditing(false)} request={r} onSaved={() => reload({ silent: true })} />
      <ConfirmDialog
        open={confirm === 'cancel'}
        onClose={() => setConfirm(null)}
        onConfirm={() => act(() => requestService.cancel(r.id), 'Request cancelled')}
        title="Cancel this request?"
        message="Teachers will no longer be able to offer help."
        confirmLabel="Cancel request"
      />
      <ConfirmDialog
        open={confirm === 'complete'}
        onClose={() => setConfirm(null)}
        onConfirm={() => act(() => requestService.complete(r.id), 'Marked as completed')}
        title="Mark as completed?"
        message="Do this once you've learned what you needed."
        confirmLabel="Mark completed"
        tone="primary"
      />
      <ReportModal open={reporting} onClose={() => setReporting(false)} targetType="SKILL_REQUEST" targetId={r.id} targetLabel={`"${r.title}"`} />
    </div>
  )
}
