import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Check, Lock, Plus, Search, Users, X } from 'lucide-react'
import Avatar from '../components/ui/Avatar'
import Button from '../components/ui/Button'
import { Input } from '../components/ui/Field'
import { PageHeader, Pagination, Tabs } from '../components/ui/Layout'
import { EmptyState, ErrorState, GridSkeleton } from '../components/ui/Feedback'
import GroupFormModal from '../components/GroupFormModal'
import { SkillChip } from '../components/SkillBits'
import { useAsync } from '../hooks/useAsync'
import { groupService } from '../services'
import { useToast } from '../context/ToastContext'

function GroupCard({ group }) {
  const full = group.memberCount >= group.maxMembers
  return (
    <Link to={`/groups/${group.id}`} className="card card-hover flex h-full flex-col p-5">
      <div className="flex items-start justify-between gap-2">
        <div className="grid h-11 w-11 place-items-center rounded-xl text-white" style={{ background: group.skill.categoryColor }}>
          <Users size={20} />
        </div>
        <div className="flex gap-1.5">
          {group.privacy === 'PRIVATE' && <span className="chip bg-slate-100 text-slate-600"><Lock size={11} /> Private</span>}
          {group.myStatus === 'ACTIVE' && <span className="chip bg-emerald-50 text-emerald-700">{group.myRole === 'OWNER' ? 'Owner' : 'Member'}</span>}
          {group.myStatus === 'INVITED' && <span className="chip bg-sky-50 text-sky-700">Invited</span>}
        </div>
      </div>
      <h3 className="mt-3 font-semibold text-slate-900">{group.name}</h3>
      <p className="mt-1 line-clamp-2 text-sm text-slate-600">{group.description}</p>
      <div className="mt-3"><SkillChip name={group.skill.name} color={group.skill.categoryColor} size="sm" /></div>
      <div className="mt-auto flex items-center justify-between border-t border-slate-100 pt-3 text-xs text-slate-500">
        <span className="inline-flex items-center gap-1.5"><Avatar user={group.creator} size="xs" /> {group.creator.fullName}</span>
        <span className={full ? 'font-medium text-rose-600' : ''}>{group.memberCount}/{group.maxMembers} members</span>
      </div>
    </Link>
  )
}

export default function Groups() {
  const toast = useToast()
  const [tab, setTab] = useState('all')
  const [page, setPage] = useState(0)
  const [query, setQuery] = useState('')
  const [q, setQ] = useState('')
  const [creating, setCreating] = useState(false)
  const { data, loading, error, reload } = useAsync(() => groupService.list({ page, size: 12, mine: tab === 'mine', q: q || undefined }), [tab, page, q])
  const invitations = useAsync(() => groupService.invitations(), [])

  const answer = async (invite, accept) => {
    try {
      if (accept) await groupService.join(invite.groupId)
      else await groupService.declineInvitation(invite.groupId)
      toast.success(accept ? `You joined ${invite.groupName}` : 'Invitation declined')
      invitations.reload({ silent: true })
      reload({ silent: true })
    } catch (e) {
      toast.error('Could not respond', e.message)
    }
  }

  return (
    <div>
      <PageHeader title="Learning groups" subtitle="Study together, share resources and run group sessions." actions={<Button icon={Plus} onClick={() => setCreating(true)}>Create group</Button>} />

      {invitations.data?.length > 0 && (
        <div className="mb-6 space-y-2">
          {invitations.data.map((inv) => (
            <div key={inv.groupId} className="card flex flex-col gap-3 border-sky-200 bg-sky-50/50 p-4 sm:flex-row sm:items-center">
              <p className="flex-1 text-sm text-slate-700">
                <b>{inv.invitedBy.fullName}</b> invited you to join <b>{inv.groupName}</b>
              </p>
              <div className="flex gap-2">
                <Button size="sm" variant="secondary" icon={X} onClick={() => answer(inv, false)}>Decline</Button>
                <Button size="sm" variant="success" icon={Check} onClick={() => answer(inv, true)}>Join</Button>
              </div>
            </div>
          ))}
        </div>
      )}

      <div className="mb-6 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <Tabs tabs={[{ value: 'all', label: 'Explore' }, { value: 'mine', label: 'My groups' }]} value={tab} onChange={(v) => { setTab(v); setPage(0) }} />
        <form className="relative sm:w-72" onSubmit={(e) => { e.preventDefault(); setPage(0); setQ(query.trim()) }}>
          <Search size={15} className="absolute top-1/2 left-3 -translate-y-1/2 text-slate-400" />
          <Input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search groups or skills" className="pl-9" />
        </form>
      </div>

      {loading ? (
        <GridSkeleton />
      ) : error ? (
        <ErrorState message={error} onRetry={reload} />
      ) : data.content.length === 0 ? (
        <EmptyState icon={Users} title={tab === 'mine' ? "You haven't joined any groups" : 'No groups found'} message="Start one around a skill you care about." action={<Button icon={Plus} onClick={() => setCreating(true)}>Create group</Button>} />
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">{data.content.map((g) => <GroupCard key={g.id} group={g} />)}</div>
          <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
        </>
      )}
      <GroupFormModal open={creating} onClose={() => setCreating(false)} onSaved={() => { setTab('mine'); reload() }} />
    </div>
  )
}
