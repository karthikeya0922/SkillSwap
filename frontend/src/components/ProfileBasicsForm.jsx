import { useRef, useState } from 'react'
import { useForm } from 'react-hook-form'
import { ImagePlus, Trash } from 'lucide-react'
import Avatar from './ui/Avatar'
import Button from './ui/Button'
import { Field, Input, Select, Textarea } from './ui/Field'
import { AVAILABILITY } from '../lib/constants'
import { userService } from '../services'
import { useAuth } from '../context/AuthContext'
import { useToast } from '../context/ToastContext'

/** Photo, personal details, bio and weekly availability. Calls onSaved(profile) after a successful save. */
export default function ProfileBasicsForm({ profile, onSaved, submitLabel = 'Save changes', completeOnboarding = false, extraActions }) {
  const toast = useToast()
  const { patchUser } = useAuth()
  const fileRef = useRef(null)
  const [avatarUrl, setAvatarUrl] = useState(profile.avatarUrl)
  const [uploading, setUploading] = useState(false)
  const [availability, setAvailability] = useState(profile.availability || [])
  const { register, handleSubmit, setError, formState: { errors, isSubmitting } } = useForm({
    defaultValues: {
      fullName: profile.fullName,
      college: profile.college,
      department: profile.department,
      yearOfStudy: profile.yearOfStudy,
      bio: profile.bio || '',
    },
  })

  const upload = async (file) => {
    if (!file) return
    if (file.size > 2 * 1024 * 1024) {
      toast.error('Image too large', 'Please choose an image under 2 MB.')
      return
    }
    setUploading(true)
    try {
      const { avatarUrl: url } = await userService.uploadAvatar(file)
      setAvatarUrl(url)
      patchUser({ avatarUrl: url })
      toast.success('Profile photo updated')
    } catch (e) {
      toast.error('Upload failed', e.message)
    } finally {
      setUploading(false)
    }
  }

  const removePhoto = async () => {
    try {
      await userService.removeAvatar()
      setAvatarUrl(null)
      patchUser({ avatarUrl: null })
    } catch (e) {
      toast.error('Could not remove photo', e.message)
    }
  }

  const toggleSlot = (slot) =>
    setAvailability((list) => (list.includes(slot) ? list.filter((s) => s !== slot) : [...list, slot]))

  const onSubmit = async (data) => {
    try {
      const saved = await userService.updateProfile({
        ...data,
        yearOfStudy: Number(data.yearOfStudy),
        availability,
        completeOnboarding,
      })
      patchUser({ fullName: saved.fullName, college: saved.college, department: saved.department, profileCompleted: saved.profileCompleted })
      onSaved?.(saved)
    } catch (e) {
      if (e.fieldErrors) Object.entries(e.fieldErrors).forEach(([f, message]) => setError(f, { message }))
      toast.error('Could not save profile', e.message)
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6" noValidate>
      <div className="flex flex-wrap items-center gap-4">
        <Avatar user={{ ...profile, avatarUrl }} size="xl" />
        <div className="space-y-2">
          <input ref={fileRef} type="file" accept="image/png,image/jpeg,image/webp,image/gif" className="hidden" onChange={(e) => upload(e.target.files?.[0])} />
          <div className="flex flex-wrap gap-2">
            <Button variant="secondary" size="sm" icon={ImagePlus} loading={uploading} onClick={() => fileRef.current?.click()}>
              {avatarUrl ? 'Change photo' : 'Upload photo'}
            </Button>
            {avatarUrl && (
              <Button variant="ghost" size="sm" icon={Trash} onClick={removePhoto}>
                Remove
              </Button>
            )}
          </div>
          <p className="text-xs text-slate-500">PNG, JPG, WebP or GIF up to 2 MB.</p>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Full name" error={errors.fullName?.message}>
          <Input error={errors.fullName} {...register('fullName', { required: 'Full name is required' })} />
        </Field>
        <Field label="College / University" error={errors.college?.message}>
          <Input error={errors.college} {...register('college', { required: 'College is required' })} />
        </Field>
        <Field label="Department" error={errors.department?.message}>
          <Input error={errors.department} {...register('department', { required: 'Department is required' })} />
        </Field>
        <Field label="Year of study" error={errors.yearOfStudy?.message}>
          <Select {...register('yearOfStudy', { required: true })}>
            {[1, 2, 3, 4, 5, 6].map((y) => (
              <option key={y} value={y}>
                Year {y}
              </option>
            ))}
          </Select>
        </Field>
        <Field label="Bio" className="sm:col-span-2" error={errors.bio?.message} hint="A couple of lines about you, what you love building or learning.">
          <Textarea rows={3} maxLength={1000} placeholder="Backend developer who loves explaining things with real projects…" {...register('bio', { maxLength: { value: 1000, message: 'Keep it under 1000 characters' } })} />
        </Field>
      </div>

      <Field label="When are you usually free?" hint="Used to find students whose schedule fits yours.">
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
          {AVAILABILITY.map((slot) => {
            const active = availability.includes(slot.value)
            return (
              <button
                key={slot.value}
                type="button"
                onClick={() => toggleSlot(slot.value)}
                className={`rounded-xl border px-3 py-2.5 text-sm font-medium transition ${
                  active ? 'border-brand-300 bg-brand-50 text-brand-700' : 'border-slate-200 text-slate-600 hover:border-slate-300'
                }`}
                aria-pressed={active}
              >
                {slot.label}
              </button>
            )
          })}
        </div>
      </Field>

      <div className="flex flex-wrap justify-end gap-2">
        {extraActions}
        <Button type="submit" loading={isSubmitting}>
          {submitLabel}
        </Button>
      </div>
    </form>
  )
}
