import { useState } from 'react'
import { Plus, Trash } from 'lucide-react'
import Button from './ui/Button'
import { Field, Input, Select, Textarea } from './ui/Field'
import SkillPicker from './SkillPicker'
import { LevelBadge } from './SkillBits'
import { LEVELS, LEVEL_META } from '../lib/constants'
import { skillService } from '../services'
import { useToast } from '../context/ToastContext'

/**
 * Manages one list of profile skills (TEACH or LEARN) against the API.
 * `skills` is the current list; `onChange` receives the updated list.
 */
export default function SkillEditor({ type, skills, onChange }) {
  const toast = useToast()
  const teach = type === 'TEACH'
  const empty = { skillId: null, level: teach ? 'INTERMEDIATE' : 'BEGINNER', yearsExperience: '', description: '' }
  const [form, setForm] = useState(empty)
  const [busy, setBusy] = useState(false)
  const [removing, setRemoving] = useState(null)

  const add = async () => {
    if (!form.skillId) {
      toast.error('Choose a skill first')
      return
    }
    setBusy(true)
    try {
      const created = await skillService.addSkill({
        skillId: form.skillId,
        type,
        level: form.level,
        yearsExperience: teach && form.yearsExperience !== '' ? Number(form.yearsExperience) : null,
        description: form.description.trim() || null,
      })
      onChange([...skills, created])
      setForm(empty)
      toast.success(`${created.skillName} added`)
    } catch (e) {
      toast.error('Could not add skill', e.message)
    } finally {
      setBusy(false)
    }
  }

  const remove = async (skill) => {
    setRemoving(skill.id)
    try {
      await skillService.removeSkill(skill.id)
      onChange(skills.filter((s) => s.id !== skill.id))
    } catch (e) {
      toast.error('Could not remove skill', e.message)
    } finally {
      setRemoving(null)
    }
  }

  return (
    <div className="space-y-4">
      {skills.length > 0 ? (
        <ul className="space-y-2">
          {skills.map((s) => (
            <li key={s.id} className="flex items-start gap-3 rounded-xl border border-slate-200 p-3">
              <span className="mt-1.5 h-2.5 w-2.5 shrink-0 rounded-full" style={{ backgroundColor: s.categoryColor }} />
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <p className="font-medium text-slate-900">{s.skillName}</p>
                  <LevelBadge level={s.level} />
                  {s.yearsExperience != null && <span className="text-xs text-slate-500">{s.yearsExperience} yrs</span>}
                </div>
                {s.description && <p className="mt-0.5 line-clamp-2 text-xs text-slate-500">{s.description}</p>}
              </div>
              <button
                onClick={() => remove(s)}
                disabled={removing === s.id}
                className="rounded-lg p-1.5 text-slate-400 hover:bg-rose-50 hover:text-rose-600"
                aria-label={`Remove ${s.skillName}`}
              >
                <Trash size={16} />
              </button>
            </li>
          ))}
        </ul>
      ) : (
        <p className="rounded-xl border border-dashed border-slate-200 p-4 text-center text-sm text-slate-500">
          {teach ? 'Add the skills you can help others with.' : 'Add the skills you want to pick up.'}
        </p>
      )}

      <div className="rounded-2xl bg-slate-50 p-4">
        <div className="grid gap-3 sm:grid-cols-2">
          <Field label="Skill">
            <SkillPicker value={form.skillId} onChange={(skillId) => setForm((f) => ({ ...f, skillId }))} />
          </Field>
          <Field label={teach ? 'Your proficiency' : 'Level you want to reach'}>
            <Select value={form.level} onChange={(e) => setForm((f) => ({ ...f, level: e.target.value }))}>
              {LEVELS.map((l) => (
                <option key={l} value={l}>
                  {LEVEL_META[l].label}
                </option>
              ))}
            </Select>
          </Field>
          {teach && (
            <Field label="Years of experience">
              <Input type="number" min="0" max="50" step="0.5" placeholder="e.g. 2" value={form.yearsExperience} onChange={(e) => setForm((f) => ({ ...f, yearsExperience: e.target.value }))} />
            </Field>
          )}
          <Field label="Description" className={teach ? '' : 'sm:col-span-2'}>
            <Textarea
              rows={1}
              maxLength={500}
              placeholder={teach ? 'What can you cover?' : 'What do you want to achieve?'}
              value={form.description}
              onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
            />
          </Field>
        </div>
        <div className="mt-3 flex justify-end">
          <Button icon={Plus} size="sm" loading={busy} onClick={add}>
            Add skill
          </Button>
        </div>
      </div>
    </div>
  )
}
