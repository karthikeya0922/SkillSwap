import { useState } from 'react'
import Modal from './ui/Modal'
import Button from './ui/Button'
import { Field, Textarea } from './ui/Field'
import { RatingStars } from './SkillBits'
import { ratingService } from '../services'
import { useToast } from '../context/ToastContext'

const ASPECTS = [
  ['teachingQuality', 'Teaching quality'],
  ['communication', 'Communication'],
  ['knowledge', 'Knowledge'],
]

/** Learner rates the teacher of a completed session. */
export default function RatingModal({ open, onClose, session, onRated }) {
  const toast = useToast()
  const [values, setValues] = useState({ stars: 0, teachingQuality: 0, communication: 0, knowledge: 0 })
  const [feedback, setFeedback] = useState('')
  const [busy, setBusy] = useState(false)
  const complete = Object.values(values).every((v) => v > 0)

  const submit = async () => {
    setBusy(true)
    try {
      await ratingService.rate({ sessionId: session.id, ...values, feedback: feedback.trim() || null })
      toast.success('Thanks for your rating!', `${session.teacher.fullName} will be notified.`)
      onRated?.()
      onClose()
    } catch (e) {
      toast.error('Could not save rating', e.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Rate your session"
      description={session ? `${session.skill.name} with ${session.teacher.fullName}` : ''}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Later
          </Button>
          <Button loading={busy} disabled={!complete} onClick={submit}>
            Submit rating
          </Button>
        </>
      }
    >
      <div className="space-y-5">
        <div className="rounded-2xl bg-amber-50/60 p-4 text-center">
          <p className="mb-2 text-sm font-medium text-slate-700">Overall experience</p>
          <div className="flex justify-center">
            <RatingStars value={values.stars} onChange={(stars) => setValues((v) => ({ ...v, stars }))} size={32} label="Overall rating" />
          </div>
        </div>
        <div className="space-y-3">
          {ASPECTS.map(([key, label]) => (
            <div key={key} className="flex items-center justify-between gap-3">
              <span className="text-sm text-slate-700">{label}</span>
              <RatingStars value={values[key]} onChange={(n) => setValues((v) => ({ ...v, [key]: n }))} size={22} label={label} />
            </div>
          ))}
        </div>
        <Field label="Written feedback" hint="Optional, shown on their profile.">
          <Textarea rows={3} maxLength={1000} value={feedback} onChange={(e) => setFeedback(e.target.value)} placeholder="What made this session great?" />
        </Field>
      </div>
    </Modal>
  )
}
