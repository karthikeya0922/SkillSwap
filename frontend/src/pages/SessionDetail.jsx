import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import {
  ArrowLeft,
  CalendarCheck,
  CalendarX,
  Check,
  CircleCheck,
  Clock,
  Coins,
  ExternalLink,
  HandHelping,
  MapPin,
  MessageCircle,
  Pencil,
  Star,
  Video,
  X,
} from 'lucide-react'
import Avatar from '../components/ui/Avatar'
import Badge from '../components/ui/Badge'
import Button from '../components/ui/Button'
import ConfirmDialog from '../components/ui/ConfirmDialog'
import Modal from '../components/ui/Modal'
import { Field, Input, Textarea } from '../components/ui/Field'
import { CardSkeleton, ErrorState } from '../components/ui/Feedback'
import RatingModal from '../components/RatingModal'
import { useAsync } from '../hooks/useAsync'
import { sessionService } from '../services'
import { useToast } from '../context/ToastContext'
import { SESSION_STATUS } from '../lib/constants'
import { formatCredits, formatDate, formatDuration, formatTime, timeAgo } from '../lib/format'

const NEXT_STEP = {
  REQUESTED: { TEACHER: 'Accept or decline this request. Credits are held only once you accept.', LEARNER: 'Waiting for the teacher to accept. No credits have been held yet.' },
  ACCEPTED: { TEACHER: 'Add a meeting link or location so the session is fully scheduled.', LEARNER: 'Accepted! Credits are held. The teacher will confirm the meeting details.' },
  SCHEDULED: { TEACHER: 'All set. You can join 15 minutes before the start.', LEARNER: 'All set. You can join 15 minutes before the start.' },
  ONGOING: { TEACHER: 'Session in progress. The learner confirms completion to release your credits.', LEARNER: 'Once the session is done, mark it completed to release the credits to your teacher.' },
}

function DetailsModal({ open, onClose, session, onSave, accepting }) {
  const [link, setLink] = useState(session.meetingLink || '')
  const [location, setLocation] = useState(session.location || '')
  const [notes, setNotes] = useState(session.notes || '')
  const [busy, setBusy] = useState(false)
  const online = session.mode === 'ONLINE'
  const save = async () => {
    setBusy(true)
    try {
      await onSave({ meetingLink: online ? link.trim() : undefined, location: online ? undefined : location.trim(), notes: notes.trim() })
      onClose()
    } catch {
      /* toast already shown */
    } finally {
      setBusy(false)
    }
  }
  return (
    <Modal
      open={open}
      onClose={onClose}
      title={accepting ? 'Accept session' : 'Session details'}
      description={accepting ? `Credits (${formatCredits(session.credits)}) will be held from the learner's wallet.` : 'Shared with the other participant.'}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>Cancel</Button>
          <Button variant={accepting ? 'success' : 'primary'} icon={accepting ? Check : undefined} loading={busy} onClick={save}>
            {accepting ? 'Accept session' : 'Save details'}
          </Button>
        </>
      }
    >
      <div className="space-y-4">
        {online ? (
          <Field label="Meeting link" hint={accepting ? 'Optional now — you can add it later.' : undefined}>
            <Input value={link} onChange={(e) => setLink(e.target.value)} placeholder="https://meet.google.com/…" />
          </Field>
        ) : (
          <Field label="Location">
            <Input value={location} onChange={(e) => setLocation(e.target.value)} placeholder="Central Library, Room 2" />
          </Field>
        )}
        <Field label="Notes">
          <Textarea rows={3} value={notes} onChange={(e) => setNotes(e.target.value)} maxLength={1000} />
        </Field>
      </div>
    </Modal>
  )
}

export default function SessionDetail() {
  const { id } = useParams()
  const toast = useToast()
  const { data: s, loading, error, reload } = useAsync(() => sessionService.get(id), [id])
  const [dialog, setDialog] = useState(null)

  if (loading) return <CardSkeleton lines={8} />
  if (error) return <ErrorState message={error} onRetry={reload} />

  const teaching = s.myRole === 'TEACHER'
  const other = teaching ? s.learner : s.teacher
  const a = s.actions
  const hint = NEXT_STEP[s.status]?.[s.myRole]

  const run = async (fn, message) => {
    try {
      await fn()
      toast.success(message)
      await reload({ silent: true })
    } catch (e) {
      toast.error('Something went wrong', e.message)
      throw e
    }
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <Link to="/sessions" className="inline-flex items-center gap-1 text-sm text-slate-500 hover:text-slate-800">
        <ArrowLeft size={14} /> All sessions
      </Link>

      <section className="card overflow-hidden">
        <div className="flex flex-wrap items-start justify-between gap-4 bg-gradient-to-r from-brand-50 to-violet-50 p-5 sm:p-7">
          <div>
            <div className="flex flex-wrap items-center gap-2">
              <Badge meta={SESSION_STATUS[s.status]} />
              <span className="chip bg-white text-slate-600">{teaching ? 'You are teaching' : 'You are learning'}</span>
            </div>
            <h1 className="mt-3 text-2xl font-bold text-slate-900 sm:text-3xl">{s.skill.name} session</h1>
            <p className="mt-1 text-slate-600">{formatDate(s.startAt)} · {formatTime(s.startAt)} – {formatTime(s.endAt)}</p>
          </div>
          <div className={`rounded-2xl px-4 py-3 text-center ${teaching ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'}`}>
            <p className="inline-flex items-center gap-1 text-2xl font-bold"><Coins size={20} />{teaching ? '+' : '-'}{formatCredits(s.credits)}</p>
            <p className="text-xs">credits</p>
          </div>
        </div>

        {hint && <p className="border-b border-slate-100 bg-white px-5 py-3 text-sm text-slate-600 sm:px-7">{hint}</p>}

        <div className="grid gap-6 p-5 sm:p-7 md:grid-cols-2">
          <dl className="space-y-4 text-sm">
            {[
              [Clock, 'Duration', formatDuration(s.durationMinutes)],
              [s.mode === 'ONLINE' ? Video : MapPin, s.mode === 'ONLINE' ? 'Meeting link' : 'Location',
                s.mode === 'ONLINE'
                  ? s.meetingLink ? <a key="link" href={s.meetingLink} target="_blank" rel="noopener noreferrer" className="link break-all">{s.meetingLink}</a> : 'To be added'
                  : s.location || 'To be decided'],
              [CalendarCheck, 'Mode', s.mode === 'ONLINE' ? 'Online' : 'In person'],
            ].map(([Icon, label, value]) => (
              <div key={label} className="flex gap-3">
                <div className="grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-slate-100 text-slate-500"><Icon size={16} /></div>
                <div className="min-w-0">
                  <dt className="text-xs text-slate-500">{label}</dt>
                  <dd className="font-medium text-slate-800">{value}</dd>
                </div>
              </div>
            ))}
            {s.skillRequestId && (
              <Link to={`/requests/${s.skillRequestId}`} className="inline-flex items-center gap-1.5 text-sm link">
                <HandHelping size={14} /> Linked learning request
              </Link>
            )}
          </dl>

          <div className="space-y-4">
            {[['Teacher', s.teacher], ['Learner', s.learner]].map(([role, u]) => (
              <Link key={role} to={`/users/${u.id}`} className="flex items-center gap-3 rounded-xl border border-slate-100 p-3 hover:bg-slate-50">
                <Avatar user={u} size="md" />
                <div className="min-w-0 flex-1">
                  <p className="text-xs text-slate-500">{role}</p>
                  <p className="truncate font-semibold text-slate-900">{u.fullName}</p>
                </div>
              </Link>
            ))}
            {s.notes && (
              <div className="rounded-xl bg-slate-50 p-3">
                <p className="text-xs font-medium text-slate-500">Notes</p>
                <p className="mt-1 text-sm whitespace-pre-line text-slate-700">{s.notes}</p>
              </div>
            )}
            {s.cancelReason && (
              <div className="rounded-xl bg-rose-50 p-3 text-sm text-rose-800">
                <p className="text-xs font-medium">{s.status === 'REJECTED' ? 'Declined' : 'Cancelled'}{s.cancelledByName ? ` by ${s.cancelledByName}` : ''}</p>
                <p className="mt-1">{s.cancelReason}</p>
              </div>
            )}
            {s.completedAt && <p className="text-xs text-slate-500">Completed {timeAgo(s.completedAt)}</p>}
          </div>
        </div>

        <div className="flex flex-wrap justify-end gap-2 border-t border-slate-100 p-5 sm:px-7">
          <Button variant="ghost" icon={MessageCircle} to={`/messages/${other.id}`}>Message {other.fullName.split(' ')[0]}</Button>
          {a.canEditDetails && s.status !== 'REQUESTED' && <Button variant="secondary" icon={Pencil} onClick={() => setDialog('details')}>Edit details</Button>}
          {a.canCancel && <Button variant="secondary" icon={CalendarX} onClick={() => setDialog('cancel')}>Cancel</Button>}
          {a.canReject && <Button variant="secondary" icon={X} onClick={() => setDialog('reject')}>Decline</Button>}
          {a.canAccept && <Button variant="success" icon={Check} onClick={() => setDialog('accept')}>Accept</Button>}
          {a.canJoin && s.meetingLink && <Button icon={ExternalLink} href={s.meetingLink}>Join session</Button>}
          {a.canComplete && <Button variant="success" icon={CircleCheck} onClick={() => setDialog('complete')}>Mark completed</Button>}
          {a.canRate && <Button icon={Star} onClick={() => setDialog('rate')}>Rate session</Button>}
          {s.rated && s.myRole === 'LEARNER' && <span className="chip self-center bg-emerald-50 text-emerald-700"><Star size={12} /> Rated — thank you!</span>}
        </div>
      </section>

      {(dialog === 'accept' || dialog === 'details') && (
        <DetailsModal
          open
          accepting={dialog === 'accept'}
          session={s}
          onClose={() => setDialog(null)}
          onSave={(details) =>
            dialog === 'accept'
              ? run(() => sessionService.accept(s.id, details), 'Session accepted')
              : run(() => sessionService.updateDetails(s.id, details), 'Details updated')
          }
        />
      )}
      <ConfirmDialog
        open={dialog === 'cancel'}
        onClose={() => setDialog(null)}
        onConfirm={(reason) => run(() => sessionService.cancel(s.id, reason), 'Session cancelled')}
        title="Cancel this session?"
        message={['ACCEPTED', 'SCHEDULED'].includes(s.status) ? 'Held credits will be refunded to the learner.' : 'The other participant will be notified.'}
        confirmLabel="Cancel session"
        withReason
      />
      <ConfirmDialog
        open={dialog === 'reject'}
        onClose={() => setDialog(null)}
        onConfirm={(reason) => run(() => sessionService.reject(s.id, reason), 'Request declined')}
        title="Decline this request?"
        message="Let the learner know why, and maybe suggest another time."
        confirmLabel="Decline"
        withReason
      />
      <ConfirmDialog
        open={dialog === 'complete'}
        onClose={() => setDialog((d) => (d === 'complete' ? null : d))}
        onConfirm={() => run(() => sessionService.complete(s.id), 'Session completed!').then(() => setDialog('rate'))}
        title="Mark this session as completed?"
        message={`${formatCredits(s.credits)} credits will be released to ${s.teacher.fullName}.`}
        confirmLabel="Yes, it happened"
        tone="primary"
      />
      <RatingModal open={dialog === 'rate'} onClose={() => setDialog(null)} session={s} onRated={() => reload({ silent: true })} />
    </div>
  )
}
