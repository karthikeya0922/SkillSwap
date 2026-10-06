import { useEffect } from 'react'
import { useForm, Controller } from 'react-hook-form'
import Modal from './ui/Modal'
import Button from './ui/Button'
import { Field, Input, Select, Textarea } from './ui/Field'
import SkillPicker from './SkillPicker'
import { LEVELS, LEVEL_META } from '../lib/constants'
import { requestService } from '../services'
import { useToast } from '../context/ToastContext'

const DEFAULTS = { skillId: null, title: '', description: '', desiredLevel: 'BEGINNER', preferredSchedule: '', mode: 'ONLINE', durationHours: '1' }

/** Create or edit a learning request ("I want to learn …"). */
export default function RequestFormModal({ open, onClose, onSaved, request }) {
  const toast = useToast()
  const { register, control, handleSubmit, reset, setError, formState: { errors, isSubmitting } } = useForm({ defaultValues: DEFAULTS })

  useEffect(() => {
    if (open) {
      reset(
        request
          ? {
              skillId: request.skill.id,
              title: request.title,
              description: request.description,
              desiredLevel: request.desiredLevel,
              preferredSchedule: request.preferredSchedule || '',
              mode: request.mode,
              durationHours: String(request.durationHours),
            }
          : DEFAULTS,
      )
    }
  }, [open, request, reset])

  const onSubmit = async (data) => {
    try {
      const payload = { ...data, durationHours: Number(data.durationHours) }
      const saved = request ? await requestService.update(request.id, payload) : await requestService.create(payload)
      toast.success(request ? 'Request updated' : 'Request posted', request ? null : 'Teachers of this skill can now offer to help.')
      onSaved?.(saved)
      onClose()
    } catch (e) {
      if (e.fieldErrors) Object.entries(e.fieldErrors).forEach(([f, message]) => setError(f, { message }))
      toast.error('Could not save request', e.message)
    }
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={request ? 'Edit learning request' : 'What do you want to learn?'}
      description="Describe your goal and teachers who know the skill can respond."
      size="lg"
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>Cancel</Button>
          <Button loading={isSubmitting} onClick={handleSubmit(onSubmit)}>{request ? 'Save changes' : 'Post request'}</Button>
        </>
      }
    >
      <form className="grid gap-4 sm:grid-cols-2" onSubmit={handleSubmit(onSubmit)} noValidate>
        <Field label="Skill" error={errors.skillId?.message} required>
          <Controller
            name="skillId"
            control={control}
            rules={{ required: 'Choose a skill' }}
            render={({ field }) => <SkillPicker value={field.value} onChange={field.onChange} error={errors.skillId} />}
          />
        </Field>
        <Field label="Level you want to reach" required>
          <Select {...register('desiredLevel')}>
            {LEVELS.map((l) => <option key={l} value={l}>{LEVEL_META[l].label}</option>)}
          </Select>
        </Field>
        <Field label="Title" className="sm:col-span-2" error={errors.title?.message} required>
          <Input placeholder="e.g. I want to learn Spring Boot" error={errors.title} maxLength={120} {...register('title', { required: 'Give your request a title' })} />
        </Field>
        <Field label="Description" className="sm:col-span-2" error={errors.description?.message} required>
          <Textarea rows={4} maxLength={1500} placeholder="What do you already know? What would you like to build or understand?" error={errors.description} {...register('description', { required: 'Describe what you want to learn' })} />
        </Field>
        <Field label="Preferred schedule" hint="e.g. Weekday evenings after 6 pm">
          <Input maxLength={200} {...register('preferredSchedule')} />
        </Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Mode">
            <Select {...register('mode')}>
              <option value="ONLINE">Online</option>
              <option value="OFFLINE">Offline</option>
            </Select>
          </Field>
          <Field label="Duration (hrs)" error={errors.durationHours?.message}>
            <Select {...register('durationHours')}>
              {['0.5', '1', '1.5', '2', '3', '4', '6', '8'].map((h) => <option key={h} value={h}>{h}</option>)}
            </Select>
          </Field>
        </div>
      </form>
    </Modal>
  )
}
