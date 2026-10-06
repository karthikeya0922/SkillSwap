import { useEffect } from 'react'
import { Controller, useForm } from 'react-hook-form'
import Modal from './ui/Modal'
import Button from './ui/Button'
import { Field, Input, Select, Textarea } from './ui/Field'
import SkillPicker from './SkillPicker'
import { groupService } from '../services'
import { useToast } from '../context/ToastContext'

const DEFAULTS = { name: '', description: '', skillId: null, maxMembers: 25, privacy: 'PUBLIC' }

export default function GroupFormModal({ open, onClose, group, onSaved }) {
  const toast = useToast()
  const { register, control, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm({ defaultValues: DEFAULTS })

  useEffect(() => {
    if (open) {
      reset(group ? { name: group.name, description: group.description, skillId: group.skill.id, maxMembers: group.maxMembers, privacy: group.privacy } : DEFAULTS)
    }
  }, [open, group, reset])

  const onSubmit = async (data) => {
    try {
      const payload = { ...data, maxMembers: Number(data.maxMembers) }
      const saved = group ? await groupService.update(group.id, payload) : await groupService.create(payload)
      toast.success(group ? 'Group updated' : 'Group created', group ? null : 'Invite classmates to get it going.')
      onSaved?.(saved)
      onClose()
    } catch (e) {
      toast.error('Could not save group', e.message)
    }
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={group ? 'Edit group' : 'Create a learning group'}
      size="lg"
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>Cancel</Button>
          <Button loading={isSubmitting} onClick={handleSubmit(onSubmit)}>{group ? 'Save' : 'Create group'}</Button>
        </>
      }
    >
      <form className="grid gap-4 sm:grid-cols-2" noValidate onSubmit={handleSubmit(onSubmit)}>
        <Field label="Group name" className="sm:col-span-2" error={errors.name?.message} required>
          <Input placeholder="React Study Group" error={errors.name} maxLength={80} {...register('name', { required: 'Name is required', minLength: { value: 3, message: 'At least 3 characters' } })} />
        </Field>
        <Field label="Description" className="sm:col-span-2" error={errors.description?.message} required>
          <Textarea rows={3} maxLength={1000} error={errors.description} placeholder="What will you learn together? How often do you meet?" {...register('description', { required: 'Add a description' })} />
        </Field>
        <Field label="Skill focus" error={errors.skillId?.message} required>
          <Controller name="skillId" control={control} rules={{ required: 'Choose a skill' }} render={({ field }) => <SkillPicker value={field.value} onChange={field.onChange} error={errors.skillId} />} />
        </Field>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Max members" error={errors.maxMembers?.message}>
            <Input type="number" min={2} max={200} {...register('maxMembers', { required: true, min: { value: 2, message: 'Min 2' }, max: { value: 200, message: 'Max 200' } })} />
          </Field>
          <Field label="Privacy">
            <Select {...register('privacy')}>
              <option value="PUBLIC">Public</option>
              <option value="PRIVATE">Private (invite only)</option>
            </Select>
          </Field>
        </div>
      </form>
    </Modal>
  )
}
