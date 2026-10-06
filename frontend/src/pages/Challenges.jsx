import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Controller, useForm } from 'react-hook-form'
import { CalendarClock, CircleCheck, Plus, Swords, Zap } from 'lucide-react'
import Avatar from '../components/ui/Avatar'
import Badge from '../components/ui/Badge'
import Button from '../components/ui/Button'
import Modal from '../components/ui/Modal'
import { Field, Input, Select, Textarea } from '../components/ui/Field'
import { PageHeader, Pagination, Tabs } from '../components/ui/Layout'
import { EmptyState, ErrorState, GridSkeleton } from '../components/ui/Feedback'
import SkillPicker from '../components/SkillPicker'
import { SkillChip } from '../components/SkillBits'
import { useAsync } from '../hooks/useAsync'
import { challengeService } from '../services'
import { useToast } from '../context/ToastContext'
import { DIFFICULTY } from '../lib/constants'
import { formatDate, timeAgo, toDateInput } from '../lib/format'

export function ChallengeFormModal({ open, onClose, challenge, onSaved }) {
  const toast = useToast()
  const inTwoWeeks = toDateInput(new Date(Date.now() + 14 * 86400000))
  const { register, control, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm()

  useEffect(() => {
    if (!open) return
    reset(
      challenge
        ? { title: challenge.title, description: challenge.description, skillId: challenge.skill.id, difficulty: challenge.difficulty, deadlineDate: challenge.deadline.slice(0, 10) }
        : { title: '', description: '', skillId: null, difficulty: 'MEDIUM', deadlineDate: inTwoWeeks },
    )
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, challenge, reset])

  const onSubmit = async ({ deadlineDate, ...data }) => {
    try {
      const payload = { ...data, deadline: `${deadlineDate}T23:59:00` }
      const saved = challenge ? await challengeService.update(challenge.id, payload) : await challengeService.create(payload)
      toast.success(challenge ? 'Challenge updated' : 'Challenge published')
      onSaved?.(saved)
      onClose()
    } catch (e) {
      toast.error('Could not save challenge', e.message)
    }
  }

  return (
    <Modal open={open} onClose={onClose} title={challenge ? 'Edit challenge' : 'Create a skill challenge'} size="lg"
      footer={<><Button variant="secondary" onClick={onClose}>Cancel</Button><Button loading={isSubmitting} onClick={handleSubmit(onSubmit)}>{challenge ? 'Save' : 'Publish'}</Button></>}>
      <form className="grid gap-4 sm:grid-cols-3" noValidate onSubmit={handleSubmit(onSubmit)}>
        <Field label="Title" className="sm:col-span-3" error={errors.title?.message} required>
          <Input maxLength={120} placeholder="Build a REST API using Spring Boot" error={errors.title} {...register('title', { required: 'Title is required' })} />
        </Field>
        <Field label="Skill" error={errors.skillId?.message} required>
          <Controller name="skillId" control={control} rules={{ required: 'Choose a skill' }} render={({ field }) => <SkillPicker value={field.value} onChange={field.onChange} error={errors.skillId} />} />
        </Field>
        <Field label="Difficulty">
          <Select {...register('difficulty')}><option value="EASY">Easy · 50 XP</option><option value="MEDIUM">Medium · 100 XP</option><option value="HARD">Hard · 150 XP</option></Select>
        </Field>
        <Field label="Deadline" error={errors.deadlineDate?.message}>
          <Input type="date" min={toDateInput(new Date(Date.now() + 86400000))} {...register('deadlineDate', { required: 'Choose a deadline' })} />
        </Field>
        <Field label="Description & requirements" className="sm:col-span-3" error={errors.description?.message} required>
          <Textarea rows={7} maxLength={4000} error={errors.description} placeholder="Describe the task, requirements and any bonus goals." {...register('description', { required: 'Describe the challenge' })} />
        </Field>
      </form>
    </Modal>
  )
}

function ChallengeCard({ c }) {
  return (
    <Link to={`/challenges/${c.id}`} className="card card-hover flex h-full flex-col p-5">
      <div className="flex items-start justify-between gap-2">
        <SkillChip name={c.skill.name} color={c.skill.categoryColor} size="sm" />
        <Badge meta={DIFFICULTY[c.difficulty]} />
      </div>
      <h3 className="mt-3 font-semibold text-slate-900">{c.title}</h3>
      <p className="mt-1 line-clamp-3 text-sm text-slate-600">{c.description}</p>
      <div className="mt-4 flex flex-wrap items-center gap-2 text-xs text-slate-500">
        <span className="chip bg-amber-50 text-amber-700"><Zap size={11} /> {c.xpReward} XP</span>
        <span className="inline-flex items-center gap-1"><CalendarClock size={12} /> {c.open ? `Due ${formatDate(c.deadline)}` : `Closed ${timeAgo(c.deadline)}`}</span>
        {c.submitted && <span className="chip bg-emerald-50 text-emerald-700"><CircleCheck size={11} /> Submitted</span>}
      </div>
      <div className="mt-auto flex items-center justify-between border-t border-slate-100 pt-3 text-xs text-slate-500">
        <span className="inline-flex items-center gap-1.5"><Avatar user={c.creator} size="xs" /> {c.creator.fullName}</span>
        <span>{c.submissionCount} submission{c.submissionCount === 1 ? '' : 's'}</span>
      </div>
    </Link>
  )
}

export default function Challenges() {
  const [status, setStatus] = useState('active')
  const [difficulty, setDifficulty] = useState('')
  const [page, setPage] = useState(0)
  const [creating, setCreating] = useState(false)
  const { data, loading, error, reload } = useAsync(() => challengeService.list({ status, difficulty: difficulty || undefined, page, size: 12 }), [status, difficulty, page])

  return (
    <div>
      <PageHeader title="Skill challenges" subtitle="Practice by building. Submit solutions, review peers and earn XP." actions={<Button icon={Plus} onClick={() => setCreating(true)}>Create challenge</Button>} />
      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <Tabs tabs={[{ value: 'active', label: 'Open' }, { value: 'past', label: 'Closed' }]} value={status} onChange={(v) => { setStatus(v); setPage(0) }} />
        <Select value={difficulty} onChange={(e) => { setDifficulty(e.target.value); setPage(0) }} className="sm:w-44" aria-label="Difficulty">
          <option value="">Any difficulty</option><option value="EASY">Easy</option><option value="MEDIUM">Medium</option><option value="HARD">Hard</option>
        </Select>
      </div>
      {loading ? <GridSkeleton /> : error ? <ErrorState message={error} onRetry={reload} /> : data.content.length === 0 ? (
        <EmptyState icon={Swords} title="No challenges here" message="Create one to challenge your peers." action={<Button icon={Plus} onClick={() => setCreating(true)}>Create challenge</Button>} />
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">{data.content.map((c) => <ChallengeCard key={c.id} c={c} />)}</div>
          <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
        </>
      )}
      <ChallengeFormModal open={creating} onClose={() => setCreating(false)} onSaved={() => { setStatus('active'); reload() }} />
    </div>
  )
}
