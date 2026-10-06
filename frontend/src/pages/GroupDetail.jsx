import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, CalendarPlus, Crown, ExternalLink, Flag, Lock, LogOut, MapPin, Pencil, Send, Trash, UserPlus, Video } from 'lucide-react'
import Avatar from '../components/ui/Avatar'
import Button from '../components/ui/Button'
import ConfirmDialog from '../components/ui/ConfirmDialog'
import Modal from '../components/ui/Modal'
import { Field, Input, Select, Textarea } from '../components/ui/Field'
import { Tabs } from '../components/ui/Layout'
import { CardSkeleton, EmptyState, ErrorState } from '../components/ui/Feedback'
import GroupFormModal from '../components/GroupFormModal'
import ReportModal from '../components/ReportModal'
import { SkillChip } from '../components/SkillBits'
import { useAsync } from '../hooks/useAsync'
import { connectionService, groupService } from '../services'
import { useToast } from '../context/ToastContext'
import { formatDateTime, timeAgo, toDateInput } from '../lib/format'

function EventModal({ open, onClose, groupId, onSaved }) {
  const toast = useToast()
  const tomorrow = new Date(Date.now() + 86400000)
  const [form, setForm] = useState({ title: '', description: '', type: 'SESSION', date: toDateInput(tomorrow), startTime: '18:00', endTime: '19:00', mode: 'ONLINE', location: '', meetingLink: '' })
  const [busy, setBusy] = useState(false)
  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))
  const save = async () => {
    setBusy(true)
    try {
      await groupService.addEvent(groupId, form)
      toast.success('Event scheduled', 'Members have been notified.')
      onSaved()
      onClose()
    } catch (e) {
      toast.error('Could not schedule event', e.message)
    } finally {
      setBusy(false)
    }
  }
  return (
    <Modal open={open} onClose={onClose} title="Schedule a group session or event" size="lg" footer={<><Button variant="secondary" onClick={onClose}>Cancel</Button><Button loading={busy} onClick={save}>Schedule</Button></>}>
      <div className="grid gap-4 sm:grid-cols-2">
        <Field label="Title" className="sm:col-span-2" required><Input value={form.title} onChange={set('title')} maxLength={120} placeholder="Hooks deep-dive" /></Field>
        <Field label="Type"><Select value={form.type} onChange={set('type')}><option value="SESSION">Group session</option><option value="EVENT">Event / meetup</option></Select></Field>
        <Field label="Date"><Input type="date" min={toDateInput(new Date())} value={form.date} onChange={set('date')} /></Field>
        <Field label="Start"><Input type="time" value={form.startTime} onChange={set('startTime')} /></Field>
        <Field label="End"><Input type="time" value={form.endTime} onChange={set('endTime')} /></Field>
        <Field label="Mode"><Select value={form.mode} onChange={set('mode')}><option value="ONLINE">Online</option><option value="OFFLINE">Offline</option></Select></Field>
        {form.mode === 'ONLINE'
          ? <Field label="Meeting link"><Input value={form.meetingLink} onChange={set('meetingLink')} placeholder="https://…" /></Field>
          : <Field label="Location"><Input value={form.location} onChange={set('location')} /></Field>}
        <Field label="Description" className="sm:col-span-2"><Textarea rows={3} value={form.description} onChange={set('description')} maxLength={1000} /></Field>
      </div>
    </Modal>
  )
}

function InviteModal({ open, onClose, groupId, memberIds }) {
  const toast = useToast()
  const { data } = useAsync(() => (open ? connectionService.overview() : Promise.resolve(null)), [open])
  const [sent, setSent] = useState([])
  const candidates = (data?.connections || []).filter((c) => !memberIds.includes(c.user.id))
  const invite = async (user) => {
    try {
      await groupService.invite(groupId, user.id)
      setSent((s) => [...s, user.id])
      toast.success(`Invitation sent to ${user.fullName}`)
    } catch (e) {
      toast.error('Could not invite', e.message)
    }
  }
  return (
    <Modal open={open} onClose={onClose} title="Invite connections" size="sm">
      {candidates.length === 0 ? (
        <p className="text-sm text-slate-500">All your connections are already here — or you have none yet.</p>
      ) : (
        <ul className="space-y-2">
          {candidates.map((c) => (
            <li key={c.user.id} className="flex items-center gap-3">
              <Avatar user={c.user} size="sm" />
              <span className="flex-1 truncate text-sm font-medium">{c.user.fullName}</span>
              <Button size="sm" variant={sent.includes(c.user.id) ? 'secondary' : 'soft'} disabled={sent.includes(c.user.id)} onClick={() => invite(c.user)}>
                {sent.includes(c.user.id) ? 'Invited' : 'Invite'}
              </Button>
            </li>
          ))}
        </ul>
      )}
    </Modal>
  )
}

function Discussion({ groupId, canPost }) {
  const toast = useToast()
  const { data, loading, reload } = useAsync(() => groupService.posts(groupId, { size: 30 }), [groupId])
  const [text, setText] = useState('')
  const [busy, setBusy] = useState(false)
  const [reportPost, setReportPost] = useState(null)
  const post = async () => {
    if (!text.trim()) return
    setBusy(true)
    try {
      await groupService.addPost(groupId, text.trim())
      setText('')
      reload({ silent: true })
    } catch (e) {
      toast.error('Could not post', e.message)
    } finally {
      setBusy(false)
    }
  }
  const remove = async (p) => {
    try {
      await groupService.deletePost(groupId, p.id)
      reload({ silent: true })
    } catch (e) {
      toast.error('Could not delete', e.message)
    }
  }
  return (
    <div className="space-y-4">
      {canPost && (
        <div className="card p-4">
          <Textarea rows={2} value={text} onChange={(e) => setText(e.target.value)} maxLength={2000} placeholder="Share a resource, ask a question, or start a discussion…" />
          <div className="mt-2 flex justify-end"><Button size="sm" icon={Send} loading={busy} disabled={!text.trim()} onClick={post}>Post</Button></div>
        </div>
      )}
      {loading ? <CardSkeleton /> : data.content.length === 0 ? (
        <p className="card p-6 text-center text-sm text-slate-500">No posts yet. Start the conversation!</p>
      ) : (
        data.content.map((p) => (
          <div key={p.id} className="card flex gap-3 p-4">
            <Avatar user={p.author} size="sm" />
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2">
                <Link to={`/users/${p.author.id}`} className="text-sm font-semibold text-slate-900 hover:text-brand-700">{p.author.fullName}</Link>
                <span className="text-xs text-slate-400">{timeAgo(p.createdAt)}</span>
                <div className="ml-auto flex gap-1">
                  {!p.canDelete && <button onClick={() => setReportPost(p)} className="p-1 text-slate-300 hover:text-rose-500" aria-label="Report post"><Flag size={13} /></button>}
                  {p.canDelete && <button onClick={() => remove(p)} className="p-1 text-slate-300 hover:text-rose-500" aria-label="Delete post"><Trash size={13} /></button>}
                </div>
              </div>
              <p className="mt-1 text-sm whitespace-pre-line text-slate-700">{p.content}</p>
            </div>
          </div>
        ))
      )}
      <ReportModal open={Boolean(reportPost)} onClose={() => setReportPost(null)} targetType="GROUP_POST" targetId={reportPost?.id} targetLabel={`a post by ${reportPost?.author.fullName}`} />
    </div>
  )
}

function EventItem({ e, onDelete }) {
  return (
    <div className="card flex gap-4 p-4">
      <div className="grid w-14 shrink-0 place-items-center rounded-xl bg-violet-50 py-2 text-center">
        <span className="text-[11px] font-semibold text-violet-600 uppercase">{new Date(e.startTime).toLocaleDateString('en-IN', { month: 'short' })}</span>
        <span className="text-xl leading-none font-bold text-violet-800">{new Date(e.startTime).getDate()}</span>
      </div>
      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-center gap-2">
          <p className="font-semibold text-slate-900">{e.title}</p>
          <span className="chip bg-slate-100 text-slate-600">{e.type === 'SESSION' ? 'Group session' : 'Event'}</span>
        </div>
        <p className="mt-0.5 text-xs text-slate-500">{formatDateTime(e.startTime)} · by {e.createdBy.fullName}</p>
        {e.description && <p className="mt-1 text-sm text-slate-600">{e.description}</p>}
        <p className="mt-1 inline-flex items-center gap-1 text-xs text-slate-500">
          {e.mode === 'ONLINE' ? <Video size={12} /> : <MapPin size={12} />}
          {e.mode === 'ONLINE' ? (e.meetingLink ? <a href={e.meetingLink} target="_blank" rel="noopener noreferrer" className="link inline-flex items-center gap-1">Join link <ExternalLink size={11} /></a> : 'Online') : e.location}
        </p>
      </div>
      {e.canDelete && onDelete && <button onClick={() => onDelete(e)} className="self-start p-1 text-slate-300 hover:text-rose-500" aria-label="Delete event"><Trash size={15} /></button>}
    </div>
  )
}

export default function GroupDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const toast = useToast()
  const { data, loading, error, reload } = useAsync(() => groupService.get(id), [id])
  const [tab, setTab] = useState('discussion')
  const [modal, setModal] = useState(null)
  const [busy, setBusy] = useState(false)

  if (loading) return <CardSkeleton lines={6} />
  if (error) return <ErrorState message={error} onRetry={reload} />

  const { group: g, members, invited, upcomingEvents, pastEvents, canManage, canPost } = data
  const isOwner = g.myRole === 'OWNER'
  const run = async (fn, message, after) => {
    setBusy(true)
    try {
      await fn()
      toast.success(message)
      if (after) after()
      else await reload({ silent: true })
    } catch (e) {
      toast.error('Something went wrong', e.message)
      throw e
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="space-y-6">
      <Link to="/groups" className="inline-flex items-center gap-1 text-sm text-slate-500 hover:text-slate-800"><ArrowLeft size={14} /> Groups</Link>

      <section className="card overflow-hidden">
        <div className="h-24" style={{ background: `linear-gradient(120deg, ${g.skill.categoryColor}, ${g.skill.categoryColor}88)` }} />
        <div className="p-5 sm:p-7">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
            <div>
              <div className="flex flex-wrap items-center gap-2">
                <h1 className="text-2xl font-bold text-slate-900">{g.name}</h1>
                {g.privacy === 'PRIVATE' && <span className="chip bg-slate-100 text-slate-600"><Lock size={11} /> Private</span>}
              </div>
              <div className="mt-2 flex flex-wrap items-center gap-2 text-sm text-slate-500">
                <SkillChip name={g.skill.name} color={g.skill.categoryColor} size="sm" />
                <span>{g.memberCount}/{g.maxMembers} members</span>
                <span>· Created by {g.creator.fullName}</span>
              </div>
              <p className="mt-3 max-w-3xl text-slate-700">{g.description}</p>
            </div>
            <div className="flex flex-wrap gap-2">
              {g.myStatus === 'ACTIVE' ? (
                <>
                  <Button size="sm" variant="secondary" icon={CalendarPlus} onClick={() => setModal('event')}>Schedule</Button>
                  {(g.privacy === 'PUBLIC' || isOwner) && <Button size="sm" variant="secondary" icon={UserPlus} onClick={() => setModal('invite')}>Invite</Button>}
                  {isOwner && <Button size="sm" variant="secondary" icon={Pencil} onClick={() => setModal('edit')}>Edit</Button>}
                  {!isOwner && <Button size="sm" variant="ghost" icon={LogOut} onClick={() => setModal('leave')}>Leave</Button>}
                </>
              ) : (
                <Button icon={UserPlus} loading={busy} disabled={g.memberCount >= g.maxMembers} onClick={() => run(() => groupService.join(g.id), 'Welcome to the group!').catch(() => {})}>
                  {g.memberCount >= g.maxMembers ? 'Group is full' : g.myStatus === 'INVITED' ? 'Accept invitation' : 'Join group'}
                </Button>
              )}
              {canManage && <Button size="sm" variant="ghost" icon={Trash} onClick={() => setModal('delete')} aria-label="Delete group" />}
              {!isOwner && <Button size="sm" variant="ghost" icon={Flag} onClick={() => setModal('report')} aria-label="Report group" />}
            </div>
          </div>
        </div>
      </section>

      <div className="grid gap-6 lg:grid-cols-3">
        <div className="lg:col-span-2">
          <Tabs tabs={[{ value: 'discussion', label: 'Discussion' }, { value: 'events', label: 'Sessions & events', count: upcomingEvents.length }]} value={tab} onChange={setTab} className="mb-4" />
          {tab === 'discussion' ? (
            <Discussion groupId={g.id} canPost={canPost} />
          ) : (
            <div className="space-y-3">
              {upcomingEvents.length === 0 && <EmptyState icon={CalendarPlus} title="Nothing scheduled" message="Members can schedule group sessions and events." />}
              {upcomingEvents.map((e) => <EventItem key={e.id} e={e} onDelete={(ev) => run(() => groupService.deleteEvent(g.id, ev.id), 'Event deleted').catch(() => {})} />)}
              {pastEvents.length > 0 && <h3 className="pt-4 text-sm font-semibold text-slate-500">Past</h3>}
              {pastEvents.map((e) => <div key={e.id} className="opacity-70"><EventItem e={e} /></div>)}
            </div>
          )}
        </div>
        <aside className="card h-fit p-5">
          <h2 className="section-title mb-3">Members ({members.length})</h2>
          <ul className="space-y-3">
            {members.map((m) => (
              <li key={m.user.id} className="flex items-center gap-3">
                <Link to={`/users/${m.user.id}`}><Avatar user={m.user} size="sm" /></Link>
                <Link to={`/users/${m.user.id}`} className="min-w-0 flex-1 truncate text-sm font-medium text-slate-800 hover:text-brand-700">{m.user.fullName}</Link>
                {m.role === 'OWNER' ? <Crown size={15} className="text-amber-500" aria-label="Owner" /> : isOwner && (
                  <button onClick={() => run(() => groupService.removeMember(g.id, m.user.id), 'Member removed').catch(() => {})} className="text-xs text-slate-400 hover:text-rose-600">Remove</button>
                )}
              </li>
            ))}
          </ul>
          {invited.length > 0 && (
            <>
              <p className="mt-5 mb-2 text-xs font-semibold text-slate-400 uppercase">Invited</p>
              <ul className="space-y-2">{invited.map((m) => <li key={m.user.id} className="flex items-center gap-2 text-sm text-slate-500"><Avatar user={m.user} size="xs" /> {m.user.fullName}</li>)}</ul>
            </>
          )}
        </aside>
      </div>

      <GroupFormModal open={modal === 'edit'} onClose={() => setModal(null)} group={g} onSaved={() => reload({ silent: true })} />
      {modal === 'event' && <EventModal open onClose={() => setModal(null)} groupId={g.id} onSaved={() => { setTab('events'); reload({ silent: true }) }} />}
      <InviteModal open={modal === 'invite'} onClose={() => setModal(null)} groupId={g.id} memberIds={[...members, ...invited].map((m) => m.user.id)} />
      <ConfirmDialog open={modal === 'leave'} onClose={() => setModal(null)} onConfirm={() => run(() => groupService.leave(g.id), 'You left the group', () => navigate('/groups'))} title="Leave this group?" confirmLabel="Leave group" />
      <ConfirmDialog open={modal === 'delete'} onClose={() => setModal(null)} onConfirm={() => run(() => groupService.remove(g.id), 'Group deleted', () => navigate('/groups'))} title="Delete this group?" message="Posts, events and memberships will be removed permanently." confirmLabel="Delete group" />
      <ReportModal open={modal === 'report'} onClose={() => setModal(null)} targetType="GROUP" targetId={g.id} targetLabel={g.name} />
    </div>
  )
}
