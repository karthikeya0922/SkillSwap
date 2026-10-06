import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, CalendarClock, ExternalLink, Flag, GitBranch, MessageSquare, Pencil, Send, Star, Trash, Zap } from 'lucide-react'
import Avatar from '../components/ui/Avatar'
import Badge from '../components/ui/Badge'
import Button from '../components/ui/Button'
import ConfirmDialog from '../components/ui/ConfirmDialog'
import Modal from '../components/ui/Modal'
import { Field, Input, Textarea } from '../components/ui/Field'
import { CardSkeleton, ErrorState } from '../components/ui/Feedback'
import ReportModal from '../components/ReportModal'
import { RatingStars, SkillChip } from '../components/SkillBits'
import { ChallengeFormModal } from './Challenges'
import { useAsync } from '../hooks/useAsync'
import { challengeService } from '../services'
import { useToast } from '../context/ToastContext'
import { DIFFICULTY } from '../lib/constants'
import { formatDateTime, timeAgo } from '../lib/format'

function SubmissionForm({ challengeId, existing, onSaved }) {
  const toast = useToast()
  const [form, setForm] = useState({ projectTitle: '', description: '', githubUrl: '', demoUrl: '', submissionText: '' })
  const [busy, setBusy] = useState(false)
  useEffect(() => {
    if (existing) setForm({ projectTitle: existing.projectTitle, description: existing.description, githubUrl: existing.githubUrl || '', demoUrl: existing.demoUrl || '', submissionText: existing.submissionText || '' })
  }, [existing])
  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))
  const save = async () => {
    setBusy(true)
    try {
      if (existing) await challengeService.updateSubmission(challengeId, form)
      else await challengeService.submit(challengeId, form)
      toast.success(existing ? 'Submission updated' : 'Submitted! XP added to your profile.')
      onSaved()
    } catch (e) {
      toast.error('Could not submit', e.message)
    } finally {
      setBusy(false)
    }
  }
  return (
    <div className="card space-y-4 p-5">
      <h2 className="section-title">{existing ? 'Edit your submission' : 'Submit your solution'}</h2>
      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Project title" required><Input value={form.projectTitle} onChange={set('projectTitle')} maxLength={120} /></Field>
        <Field label="GitHub URL" hint="https://github.com/…"><Input value={form.githubUrl} onChange={set('githubUrl')} placeholder="https://github.com/you/project" /></Field>
        <Field label="Short description" className="sm:col-span-2" required><Textarea rows={2} value={form.description} onChange={set('description')} maxLength={1500} /></Field>
        <Field label="Demo URL"><Input value={form.demoUrl} onChange={set('demoUrl')} placeholder="https://…" /></Field>
        <Field label="Written answer / notes"><Textarea rows={2} value={form.submissionText} onChange={set('submissionText')} maxLength={4000} /></Field>
      </div>
      <div className="flex justify-end"><Button icon={Send} loading={busy} onClick={save}>{existing ? 'Save changes' : 'Submit'}</Button></div>
    </div>
  )
}

function ReviewModal({ submission, onClose, onSaved }) {
  const toast = useToast()
  const [rating, setRating] = useState(0)
  const [comment, setComment] = useState('')
  const [busy, setBusy] = useState(false)
  const { data: reviews } = useAsync(() => challengeService.reviews(submission.id), [submission.id])
  const save = async () => {
    setBusy(true)
    try {
      await challengeService.review(submission.id, { rating, comment: comment.trim() || null })
      toast.success('Review posted')
      onSaved()
      onClose()
    } catch (e) {
      toast.error('Could not post review', e.message)
    } finally {
      setBusy(false)
    }
  }
  const canReview = !submission.mine && !submission.reviewedByMe
  return (
    <Modal open onClose={onClose} title={submission.projectTitle} description={`by ${submission.user.fullName}`}
      footer={canReview ? <><Button variant="secondary" onClick={onClose}>Close</Button><Button loading={busy} disabled={!rating} onClick={save}>Post review</Button></> : null}>
      <div className="space-y-5">
        {canReview && (
          <div className="space-y-3 rounded-2xl bg-slate-50 p-4">
            <RatingStars value={rating} onChange={setRating} size={26} label="Your rating" />
            <Textarea rows={2} value={comment} onChange={(e) => setComment(e.target.value)} maxLength={1000} placeholder="What's great? What could be improved?" />
          </div>
        )}
        <div>
          <p className="mb-2 text-sm font-semibold text-slate-700">Peer reviews</p>
          {!reviews ? <CardSkeleton lines={1} /> : reviews.length === 0 ? <p className="text-sm text-slate-500">No reviews yet.</p> : (
            <ul className="space-y-3">
              {reviews.map((r) => (
                <li key={r.id} className="flex gap-3">
                  <Avatar user={r.reviewer} size="xs" />
                  <div className="flex-1">
                    <div className="flex items-center gap-2"><span className="text-sm font-medium">{r.reviewer.fullName}</span><RatingStars value={r.rating} size={12} /></div>
                    {r.comment && <p className="text-sm text-slate-600">{r.comment}</p>}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </Modal>
  )
}

export default function ChallengeDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const toast = useToast()
  const challenge = useAsync(() => challengeService.get(id), [id])
  const submissions = useAsync(() => challengeService.submissions(id), [id])
  const [reviewing, setReviewing] = useState(null)
  const [reporting, setReporting] = useState(null)
  const [modal, setModal] = useState(null)

  if (challenge.loading) return <CardSkeleton lines={8} />
  if (challenge.error) return <ErrorState message={challenge.error} onRetry={challenge.reload} />
  const c = challenge.data
  const mine = submissions.data?.find((s) => s.mine)
  const refresh = () => {
    challenge.reload({ silent: true })
    submissions.reload({ silent: true })
  }

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <Link to="/challenges" className="inline-flex items-center gap-1 text-sm text-slate-500 hover:text-slate-800"><ArrowLeft size={14} /> Challenges</Link>
      <section className="card p-5 sm:p-7">
        <div className="flex flex-wrap items-center gap-2">
          <SkillChip name={c.skill.name} color={c.skill.categoryColor} />
          <Badge meta={DIFFICULTY[c.difficulty]} />
          <span className="chip bg-amber-50 text-amber-700"><Zap size={12} /> {c.xpReward} XP</span>
          <span className={`chip ${c.open ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-600'}`}><CalendarClock size={12} /> {c.open ? `Due ${formatDateTime(c.deadline)}` : 'Closed'}</span>
          <div className="ml-auto flex gap-1">
            {c.mine && <Button size="sm" variant="ghost" icon={Pencil} onClick={() => setModal('edit')}>Edit</Button>}
            {c.mine && <Button size="sm" variant="ghost" icon={Trash} onClick={() => setModal('delete')} aria-label="Delete challenge" />}
            {!c.mine && <Button size="sm" variant="ghost" icon={Flag} onClick={() => setReporting({ type: 'CHALLENGE', id: c.id, label: c.title })} aria-label="Report" />}
          </div>
        </div>
        <h1 className="mt-3 text-2xl font-bold text-slate-900 sm:text-3xl">{c.title}</h1>
        <p className="mt-1 text-sm text-slate-500">Posted by {c.creator.fullName} · {timeAgo(c.createdAt)}</p>
        <p className="mt-4 leading-relaxed whitespace-pre-line text-slate-700">{c.description}</p>
      </section>

      {c.open && <SubmissionForm challengeId={c.id} existing={mine} onSaved={refresh} />}

      <section>
        <h2 className="section-title mb-3">Submissions ({submissions.data?.length ?? 0})</h2>
        {submissions.loading ? <CardSkeleton /> : submissions.data.length === 0 ? (
          <p className="card p-6 text-center text-sm text-slate-500">No submissions yet — be the first!</p>
        ) : (
          <div className="grid gap-4 md:grid-cols-2">
            {submissions.data.map((s) => (
              <div key={s.id} className="card flex flex-col p-5">
                <div className="flex items-center gap-3">
                  <Avatar user={s.user} size="sm" />
                  <div className="min-w-0 flex-1">
                    <p className="truncate font-semibold text-slate-900">{s.projectTitle}</p>
                    <p className="text-xs text-slate-500">{s.mine ? 'You' : s.user.fullName} · {timeAgo(s.createdAt)}</p>
                  </div>
                  {s.reviewCount > 0 && <span className="chip bg-amber-50 text-amber-700"><Star size={11} className="fill-amber-400" /> {s.averageRating} ({s.reviewCount})</span>}
                </div>
                <p className="mt-3 text-sm text-slate-600">{s.description}</p>
                {s.submissionText && <p className="mt-2 line-clamp-3 rounded-xl bg-slate-50 p-3 text-sm text-slate-600">{s.submissionText}</p>}
                <div className="mt-auto flex flex-wrap items-center gap-2 pt-4">
                  {s.githubUrl && <Button size="sm" variant="secondary" icon={GitBranch} href={s.githubUrl}>Code</Button>}
                  {s.demoUrl && <Button size="sm" variant="secondary" icon={ExternalLink} href={s.demoUrl}>Demo</Button>}
                  <Button size="sm" variant={!s.mine && !s.reviewedByMe ? 'soft' : 'ghost'} icon={MessageSquare} onClick={() => setReviewing(s)} className="ml-auto">
                    {!s.mine && !s.reviewedByMe ? 'Review' : 'Reviews'}
                  </Button>
                  {!s.mine && <button onClick={() => setReporting({ type: 'SUBMISSION', id: s.id, label: s.projectTitle })} className="p-1 text-slate-300 hover:text-rose-500" aria-label="Report submission"><Flag size={13} /></button>}
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      {reviewing && <ReviewModal submission={reviewing} onClose={() => setReviewing(null)} onSaved={() => submissions.reload({ silent: true })} />}
      <ReportModal open={Boolean(reporting)} onClose={() => setReporting(null)} targetType={reporting?.type} targetId={reporting?.id} targetLabel={reporting?.label} />
      <ChallengeFormModal open={modal === 'edit'} onClose={() => setModal(null)} challenge={c} onSaved={refresh} />
      <ConfirmDialog
        open={modal === 'delete'}
        onClose={() => setModal(null)}
        onConfirm={async () => {
          try {
            await challengeService.remove(c.id)
            toast.success('Challenge deleted')
            navigate('/challenges')
          } catch (e) {
            toast.error('Could not delete', e.message)
            throw e
          }
        }}
        title="Delete this challenge?"
        message="All submissions and reviews will be removed."
        confirmLabel="Delete"
      />
    </div>
  )
}
