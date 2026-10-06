import { useMemo } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { ArrowLeft, CalendarPlus, Coins, Compass, Info } from 'lucide-react'
import Avatar from '../components/ui/Avatar'
import Button from '../components/ui/Button'
import { Field, Input, Select, Textarea } from '../components/ui/Field'
import { CardSkeleton, EmptyState, ErrorState } from '../components/ui/Feedback'
import { LevelBadge, RatingInline } from '../components/SkillBits'
import { useAsync } from '../hooks/useAsync'
import { sessionService, userService, walletService } from '../services'
import { useToast } from '../context/ToastContext'
import { formatCredits, toDateInput } from '../lib/format'

const TIMES = Array.from({ length: (23 - 6) * 4 + 1 }, (_, i) => {
  const minutes = 6 * 60 + i * 15
  return `${String(Math.floor(minutes / 60)).padStart(2, '0')}:${String(minutes % 60).padStart(2, '0')}`
})
const toMinutes = (t) => (t ? Number(t.slice(0, 2)) * 60 + Number(t.slice(3)) : 0)
const label = (t) => new Date(`2000-01-01T${t}`).toLocaleTimeString('en-IN', { hour: 'numeric', minute: '2-digit' }).toUpperCase()

export default function BookSession() {
  const [params] = useSearchParams()
  const teacherId = params.get('teacherId')
  const requestId = params.get('requestId')
  const navigate = useNavigate()
  const toast = useToast()

  const { data, loading, error, reload } = useAsync(
    () => (teacherId ? Promise.all([userService.getProfile(teacherId), walletService.balance()]) : Promise.resolve(null)),
    [teacherId],
  )
  const tomorrow = useMemo(() => {
    const d = new Date()
    d.setDate(d.getDate() + 1)
    return toDateInput(d)
  }, [])

  const { register, handleSubmit, watch, setError, formState: { errors, isSubmitting } } = useForm({
    values: data
      ? { skillId: params.get('skillId') || data[0].teachSkills[0]?.skillId || '', date: tomorrow, startTime: '18:00', endTime: '19:00', mode: 'ONLINE', location: '', meetingLink: '', notes: '' }
      : undefined,
  })

  if (!teacherId) {
    return (
      <EmptyState
        icon={Compass}
        title="Choose who to learn from"
        message="Open a student's profile or one of your matches and tap “Book session”."
        action={<Button to="/matches">See my matches</Button>}
      />
    )
  }
  if (loading) return <CardSkeleton lines={8} />
  if (error) return <ErrorState message={error} onRetry={reload} />

  const [teacher, wallet] = data
  const minutes = toMinutes(watch('endTime')) - toMinutes(watch('startTime'))
  const credits = minutes > 0 ? Math.round((minutes / 60) * 100) / 100 : 0
  const mode = watch('mode')
  const insufficient = credits > Number(wallet.available)

  const onSubmit = async (form) => {
    try {
      const session = await sessionService.book({
        teacherId: Number(teacherId),
        skillId: Number(form.skillId || params.get('skillId')),
        date: form.date,
        startTime: form.startTime,
        endTime: form.endTime,
        mode: form.mode,
        location: form.mode === 'OFFLINE' ? form.location : null,
        meetingLink: form.mode === 'ONLINE' ? form.meetingLink : null,
        notes: form.notes,
        skillRequestId: requestId ? Number(requestId) : null,
      })
      toast.success('Session requested!', `${teacher.fullName.split(' ')[0]} will be notified to accept it.`)
      navigate(`/sessions/${session.id}`)
    } catch (e) {
      if (e.fieldErrors) Object.entries(e.fieldErrors).forEach(([f, message]) => setError(f, { message }))
      toast.error('Could not book session', e.message)
    }
  }

  if (!teacher.teachSkills.length) {
    return <EmptyState icon={Info} title={`${teacher.fullName} isn't teaching any skills yet`} action={<Button to="/discover">Find another teacher</Button>} />
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <Link to={`/users/${teacher.id}`} className="inline-flex items-center gap-1 text-sm text-slate-500 hover:text-slate-800">
        <ArrowLeft size={14} /> Back to profile
      </Link>
      <div>
        <h1 className="page-title">Book a session</h1>
        <p className="mt-1 text-slate-500">Pick a skill and a time. Credits are only held once the teacher accepts.</p>
      </div>

      <div className="card flex items-center gap-4 p-4">
        <Avatar user={teacher} size="lg" online={teacher.online} />
        <div className="flex-1">
          <p className="font-semibold text-slate-900">{teacher.fullName}</p>
          <p className="text-sm text-slate-500">{teacher.department} · {teacher.college}</p>
          <RatingInline value={teacher.ratingAverage} count={teacher.ratingCount} size={13} />
        </div>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="card space-y-5 p-5 sm:p-6" noValidate>
        <Field label="Skill" error={errors.skillId?.message} required>
          <Select {...register('skillId', { required: 'Choose a skill' })} disabled={Boolean(requestId)}>
            {teacher.teachSkills.map((s) => (
              <option key={s.skillId} value={s.skillId}>{s.skillName} — {s.level.toLowerCase()}</option>
            ))}
          </Select>
        </Field>
        <div className="flex flex-wrap gap-2">
          {teacher.teachSkills.map((s) => <span key={s.id} className="inline-flex items-center gap-1.5 text-xs text-slate-500">{s.skillName} <LevelBadge level={s.level} /></span>)}
        </div>

        <div className="grid gap-4 sm:grid-cols-3">
          <Field label="Date" error={errors.date?.message} required>
            <Input type="date" min={toDateInput(new Date())} error={errors.date} {...register('date', { required: 'Choose a date' })} />
          </Field>
          <Field label="Start time" error={errors.startTime?.message} required>
            <Select {...register('startTime', { required: true })}>{TIMES.map((t) => <option key={t} value={t}>{label(t)}</option>)}</Select>
          </Field>
          <Field label="End time" error={errors.endTime?.message} required>
            <Select {...register('endTime', { required: true })}>{TIMES.map((t) => <option key={t} value={t}>{label(t)}</option>)}</Select>
          </Field>
        </div>

        <div className="grid gap-4 sm:grid-cols-3">
          <Field label="Mode">
            <Select {...register('mode')}>
              <option value="ONLINE">Online</option>
              <option value="OFFLINE">Offline (in person)</option>
            </Select>
          </Field>
          {mode === 'ONLINE' ? (
            <Field label="Meeting link" className="sm:col-span-2" error={errors.meetingLink?.message} hint="Optional — the teacher can add one when accepting.">
              <Input placeholder="https://meet.google.com/…" error={errors.meetingLink} {...register('meetingLink', { pattern: { value: /^$|^https?:\/\/.+/, message: 'Link must start with http:// or https://' } })} />
            </Field>
          ) : (
            <Field label="Location" className="sm:col-span-2" hint="e.g. Central Library, Room 2">
              <Input maxLength={200} {...register('location')} />
            </Field>
          )}
        </div>

        <Field label="Notes for the teacher" hint="What would you like to cover?">
          <Textarea rows={3} maxLength={1000} {...register('notes')} />
        </Field>

        <div className={`flex flex-wrap items-center justify-between gap-3 rounded-2xl p-4 ${insufficient ? 'bg-rose-50' : 'bg-amber-50'}`}>
          <div className="flex items-center gap-3">
            <Coins className={insufficient ? 'text-rose-500' : 'text-amber-500'} />
            <div>
              <p className="font-semibold text-slate-900">
                {minutes > 0 ? `${formatCredits(credits)} credit${credits === 1 ? '' : 's'}` : 'Choose an end time after the start'}
              </p>
              <p className="text-xs text-slate-600">
                You have {formatCredits(wallet.available)} available
                {Number(wallet.pending) > 0 && ` (${formatCredits(wallet.pending)} committed to pending requests)`}
              </p>
            </div>
          </div>
          {insufficient && <Link to="/requests" className="text-sm font-medium text-rose-700 underline">Teach to earn more credits</Link>}
        </div>

        <div className="flex justify-end gap-2">
          <Button variant="secondary" onClick={() => navigate(-1)}>Cancel</Button>
          <Button type="submit" icon={CalendarPlus} loading={isSubmitting} disabled={minutes <= 0 || insufficient}>Request session</Button>
        </div>
      </form>
    </div>
  )
}
