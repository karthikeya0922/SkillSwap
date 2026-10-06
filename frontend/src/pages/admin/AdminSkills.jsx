import { useState } from 'react'
import { Layers, Pencil, Plus, Search, Trash } from 'lucide-react'
import Badge from '../../components/ui/Badge'
import Button from '../../components/ui/Button'
import ConfirmDialog from '../../components/ui/ConfirmDialog'
import Modal from '../../components/ui/Modal'
import { Field, Input, Select, Textarea } from '../../components/ui/Field'
import { PageHeader, Pagination, Tabs } from '../../components/ui/Layout'
import { EmptyState, ErrorState, Skeleton } from '../../components/ui/Feedback'
import DynamicIcon from '../../components/DynamicIcon'
import { loadSkillCatalog } from '../../components/SkillPicker'
import { useAsync } from '../../hooks/useAsync'
import { adminService, skillService } from '../../services'
import { useToast } from '../../context/ToastContext'

const ACTIVE = { label: 'Active', className: 'bg-emerald-50 text-emerald-700 ring-emerald-200' }
const INACTIVE = { label: 'Inactive', className: 'bg-slate-100 text-slate-600 ring-slate-200' }

function SkillModal({ skill, categories, onClose, onSaved }) {
  const toast = useToast()
  const [form, setForm] = useState({ name: skill?.name || '', description: skill?.description || '', categoryId: skill?.categoryId || categories[0]?.id || '', active: skill ? skill.active : true })
  const [busy, setBusy] = useState(false)
  const save = async () => {
    setBusy(true)
    try {
      const payload = { ...form, categoryId: Number(form.categoryId) }
      if (skill) await adminService.updateSkill(skill.id, payload)
      else await adminService.createSkill(payload)
      toast.success(skill ? 'Skill updated' : 'Skill created')
      loadSkillCatalog({ refresh: true }).catch(() => {})
      onSaved()
      onClose()
    } catch (e) {
      toast.error('Could not save skill', e.message)
    } finally {
      setBusy(false)
    }
  }
  return (
    <Modal open onClose={onClose} title={skill ? 'Edit skill' : 'New skill'} footer={<><Button variant="secondary" onClick={onClose}>Cancel</Button><Button loading={busy} onClick={save}>Save</Button></>}>
      <div className="space-y-4">
        <Field label="Name" required><Input value={form.name} maxLength={80} onChange={(e) => setForm({ ...form, name: e.target.value })} /></Field>
        <Field label="Category" required>
          <Select value={form.categoryId} onChange={(e) => setForm({ ...form, categoryId: e.target.value })}>{categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}</Select>
        </Field>
        <Field label="Description"><Textarea rows={3} maxLength={500} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></Field>
        <label className="flex items-center gap-2 text-sm text-slate-700"><input type="checkbox" className="h-4 w-4 accent-brand-600" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} /> Active (students can add and book it)</label>
      </div>
    </Modal>
  )
}

function CategoryModal({ category, onClose, onSaved }) {
  const toast = useToast()
  const [form, setForm] = useState({ name: category?.name || '', description: category?.description || '', icon: category?.icon || 'Sparkles', color: category?.color || '#6366f1' })
  const [busy, setBusy] = useState(false)
  const save = async () => {
    setBusy(true)
    try {
      if (category) await adminService.updateCategory(category.id, form)
      else await adminService.createCategory(form)
      toast.success(category ? 'Category updated' : 'Category created')
      onSaved()
      onClose()
    } catch (e) {
      toast.error('Could not save category', e.message)
    } finally {
      setBusy(false)
    }
  }
  return (
    <Modal open onClose={onClose} title={category ? 'Edit category' : 'New category'} footer={<><Button variant="secondary" onClick={onClose}>Cancel</Button><Button loading={busy} onClick={save}>Save</Button></>}>
      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Name" className="sm:col-span-2" required><Input value={form.name} maxLength={60} onChange={(e) => setForm({ ...form, name: e.target.value })} /></Field>
        <Field label="Icon" hint="Lucide icon name"><Input value={form.icon} onChange={(e) => setForm({ ...form, icon: e.target.value })} /></Field>
        <Field label="Colour"><div className="flex gap-2"><input type="color" value={form.color} onChange={(e) => setForm({ ...form, color: e.target.value })} className="h-10 w-12 cursor-pointer rounded-lg border border-slate-200" /><Input value={form.color} onChange={(e) => setForm({ ...form, color: e.target.value })} /></div></Field>
        <Field label="Description" className="sm:col-span-2"><Textarea rows={2} maxLength={300} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} /></Field>
      </div>
    </Modal>
  )
}

export default function AdminSkills() {
  const toast = useToast()
  const [tab, setTab] = useState('skills')
  const [query, setQuery] = useState('')
  const [q, setQ] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [page, setPage] = useState(0)
  const [editing, setEditing] = useState(null)
  const [deleting, setDeleting] = useState(null)
  const categories = useAsync(() => skillService.categories(), [])
  const skills = useAsync(() => adminService.skills({ q: q || undefined, categoryId: categoryId || undefined, page, size: 20 }), [q, categoryId, page])

  const remove = async () => {
    try {
      if (deleting.kind === 'skill') await adminService.deleteSkill(deleting.item.id)
      else await adminService.deleteCategory(deleting.item.id)
      toast.success(deleting.kind === 'skill' ? 'Skill removed (deactivated if in use)' : 'Category deleted')
      skills.reload({ silent: true })
      categories.reload({ silent: true })
      loadSkillCatalog({ refresh: true }).catch(() => {})
    } catch (e) {
      toast.error('Could not delete', e.message)
      throw e
    }
  }

  return (
    <div>
      <PageHeader eyebrow="Admin" title="Skills & categories" subtitle="Curate the central skill catalogue." actions={<Button icon={Plus} onClick={() => setEditing({ kind: tab === 'skills' ? 'skill' : 'category' })}>{tab === 'skills' ? 'New skill' : 'New category'}</Button>} />
      <Tabs tabs={[{ value: 'skills', label: 'Skills' }, { value: 'categories', label: 'Categories', count: categories.data?.length }]} value={tab} onChange={setTab} className="mb-4" />

      {tab === 'skills' ? (
        <>
          <div className="card mb-4 flex flex-col gap-3 p-4 sm:flex-row">
            <form className="relative flex-1" onSubmit={(e) => { e.preventDefault(); setPage(0); setQ(query.trim()) }}>
              <Search size={16} className="absolute top-1/2 left-3.5 -translate-y-1/2 text-slate-400" />
              <Input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search skills" className="pl-10" />
            </form>
            <Select value={categoryId} onChange={(e) => { setCategoryId(e.target.value); setPage(0) }} className="sm:w-56" aria-label="Category">
              <option value="">All categories</option>
              {(categories.data || []).map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </Select>
          </div>
          {skills.loading ? <Skeleton className="h-96 w-full rounded-2xl" /> : skills.error ? <ErrorState message={skills.error} onRetry={skills.reload} /> : skills.data.content.length === 0 ? (
            <EmptyState icon={Layers} title="No skills found" />
          ) : (
            <>
              <div className="card overflow-x-auto">
                <table className="w-full min-w-[640px] text-left text-sm">
                  <thead className="border-b border-slate-100 text-xs text-slate-500 uppercase">
                    <tr><th className="px-4 py-3 font-semibold">Skill</th><th className="px-4 py-3 font-semibold">Category</th><th className="px-4 py-3 font-semibold">Teachers</th><th className="px-4 py-3 font-semibold">Learners</th><th className="px-4 py-3 font-semibold">Status</th><th className="px-4 py-3" /></tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {skills.data.content.map((s) => (
                      <tr key={s.id} className="hover:bg-slate-50/60">
                        <td className="px-4 py-3 font-medium text-slate-900">{s.name}</td>
                        <td className="px-4 py-3"><span className="inline-flex items-center gap-2 text-slate-600"><span className="h-2.5 w-2.5 rounded-full" style={{ background: s.categoryColor }} />{s.categoryName}</span></td>
                        <td className="px-4 py-3 text-slate-600">{s.teacherCount}</td>
                        <td className="px-4 py-3 text-slate-600">{s.learnerCount}</td>
                        <td className="px-4 py-3"><Badge meta={s.active ? ACTIVE : INACTIVE} /></td>
                        <td className="px-4 py-3 text-right whitespace-nowrap">
                          <Button size="sm" variant="ghost" icon={Pencil} onClick={() => setEditing({ kind: 'skill', item: s })} aria-label="Edit" />
                          <Button size="sm" variant="ghost" icon={Trash} onClick={() => setDeleting({ kind: 'skill', item: s })} aria-label="Delete" />
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <Pagination page={skills.data.page} totalPages={skills.data.totalPages} onChange={setPage} />
            </>
          )}
        </>
      ) : categories.loading ? <Skeleton className="h-96 w-full rounded-2xl" /> : (
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {categories.data.map((c) => (
            <div key={c.id} className="card flex items-center gap-3 p-4">
              <div className="grid h-11 w-11 place-items-center rounded-xl text-white" style={{ background: c.color }}><DynamicIcon name={c.icon} size={20} /></div>
              <div className="min-w-0 flex-1"><p className="truncate font-semibold text-slate-900">{c.name}</p><p className="text-xs text-slate-500">{c.skillCount} active skills</p></div>
              <Button size="sm" variant="ghost" icon={Pencil} onClick={() => setEditing({ kind: 'category', item: c })} aria-label="Edit" />
              <Button size="sm" variant="ghost" icon={Trash} onClick={() => setDeleting({ kind: 'category', item: c })} aria-label="Delete" />
            </div>
          ))}
        </div>
      )}

      {editing?.kind === 'skill' && <SkillModal skill={editing.item} categories={categories.data || []} onClose={() => setEditing(null)} onSaved={() => skills.reload({ silent: true })} />}
      {editing?.kind === 'category' && <CategoryModal category={editing.item} onClose={() => setEditing(null)} onSaved={() => categories.reload({ silent: true })} />}
      <ConfirmDialog
        open={Boolean(deleting)}
        onClose={() => setDeleting(null)}
        onConfirm={remove}
        title={`Delete ${deleting?.item.name}?`}
        message={deleting?.kind === 'skill' ? 'Skills used on profiles or in history are deactivated instead of deleted.' : 'Only empty categories can be deleted.'}
        confirmLabel="Delete"
      />
    </div>
  )
}
