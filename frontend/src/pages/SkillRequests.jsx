import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { Clock, HandHelping, MapPin, Plus, Search, Video } from 'lucide-react'
import Avatar from '../components/ui/Avatar'
import Badge from '../components/ui/Badge'
import Button from '../components/ui/Button'
import { Input } from '../components/ui/Field'
import { PageHeader, Pagination, Tabs } from '../components/ui/Layout'
import { EmptyState, ErrorState, GridSkeleton } from '../components/ui/Feedback'
import RequestFormModal from '../components/RequestFormModal'
import { LevelBadge, SkillChip } from '../components/SkillBits'
import { useAsync } from '../hooks/useAsync'
import { requestService } from '../services'
import { OFFER_STATUS, REQUEST_STATUS } from '../lib/constants'
import { formatCredits, timeAgo } from '../lib/format'

function RequestCard({ request, offerStatus }) {
  return (
    <Link to={`/requests/${request.id}`} className="card card-hover flex h-full flex-col p-5">
      <div className="flex items-start justify-between gap-3">
        <SkillChip name={request.skill.name} color={request.skill.categoryColor} />
        {offerStatus ? <Badge meta={OFFER_STATUS[offerStatus]} /> : <Badge meta={REQUEST_STATUS[request.status]} />}
      </div>
      <h3 className="mt-3 line-clamp-2 font-semibold text-slate-900">{request.title}</h3>
      <p className="mt-1 line-clamp-3 text-sm text-slate-600">{request.description}</p>
      <div className="mt-3 flex flex-wrap items-center gap-2 text-xs text-slate-500">
        <LevelBadge level={request.desiredLevel} />
        <span className="inline-flex items-center gap-1">{request.mode === 'ONLINE' ? <Video size={12} /> : <MapPin size={12} />}{request.mode === 'ONLINE' ? 'Online' : 'Offline'}</span>
        <span className="inline-flex items-center gap-1"><Clock size={12} /> {formatCredits(request.durationHours)} hr</span>
      </div>
      <div className="mt-auto flex items-center gap-2 border-t border-slate-100 pt-3 text-xs text-slate-500">
        <Avatar user={request.learner} size="xs" />
        <span className="truncate">{request.mine ? 'You' : request.learner.fullName}</span>
        <span className="ml-auto whitespace-nowrap">{timeAgo(request.createdAt)} · {request.offerCount} offer{request.offerCount === 1 ? '' : 's'}</span>
      </div>
    </Link>
  )
}

export default function SkillRequests() {
  const [params, setParams] = useSearchParams()
  const tab = params.get('tab') || 'browse'
  const [page, setPage] = useState(0)
  const [teachableOnly, setTeachableOnly] = useState(true)
  const [query, setQuery] = useState('')
  const [q, setQ] = useState('')
  const creating = params.get('new') === '1'

  const { data, loading, error, reload } = useAsync(() => {
    if (tab === 'mine') return requestService.mine({ page, size: 12 })
    if (tab === 'offers') return requestService.myOffers({ page, size: 12 })
    return requestService.browse({ page, size: 12, teachableOnly, q: q || undefined })
  }, [tab, page, teachableOnly, q])

  const setTab = (value) => {
    setPage(0)
    setParams(value === 'browse' ? {} : { tab: value })
  }
  const closeCreate = () => {
    const next = new URLSearchParams(params)
    next.delete('new')
    setParams(next, { replace: true })
  }

  return (
    <div>
      <PageHeader
        title="Learning requests"
        subtitle="Post what you want to learn, or help classmates who need a teacher."
        actions={<Button icon={Plus} onClick={() => setParams({ ...Object.fromEntries(params), new: '1' })}>New request</Button>}
      />
      <Tabs
        tabs={[
          { value: 'browse', label: 'Help others' },
          { value: 'mine', label: 'My requests' },
          { value: 'offers', label: 'My offers' },
        ]}
        value={tab}
        onChange={setTab}
        className="mb-4"
      />

      {tab === 'browse' && (
        <div className="mb-5 flex flex-col gap-3 sm:flex-row sm:items-center">
          <form
            className="relative flex-1"
            onSubmit={(e) => {
              e.preventDefault()
              setPage(0)
              setQ(query.trim())
            }}
          >
            <Search size={16} className="absolute top-1/2 left-3.5 -translate-y-1/2 text-slate-400" />
            <Input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search requests" className="pl-10" />
          </form>
          <label className="inline-flex cursor-pointer items-center gap-2 text-sm text-slate-600">
            <input
              type="checkbox"
              className="h-4 w-4 rounded border-slate-300 accent-brand-600"
              checked={teachableOnly}
              onChange={(e) => {
                setPage(0)
                setTeachableOnly(e.target.checked)
              }}
            />
            Only skills I teach
          </label>
        </div>
      )}

      {loading ? (
        <GridSkeleton />
      ) : error ? (
        <ErrorState message={error} onRetry={reload} />
      ) : data.content.length === 0 ? (
        <EmptyState
          icon={HandHelping}
          title={tab === 'browse' ? 'No open requests right now' : tab === 'mine' ? "You haven't posted any requests" : "You haven't offered help yet"}
          message={
            tab === 'browse'
              ? teachableOnly
                ? 'Nobody needs help with the skills you teach yet. Try showing all requests.'
                : 'Check back soon — new requests show up here.'
              : tab === 'mine'
                ? 'Tell the community what you want to learn and let teachers come to you.'
                : 'Browse requests and offer to teach a skill you know.'
          }
          action={tab === 'mine' ? <Button icon={Plus} onClick={() => setParams({ tab: 'mine', new: '1' })}>Post a request</Button> : null}
        />
      ) : (
        <>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
            {tab === 'offers'
              ? data.content.map((o) => <RequestCard key={o.offer.id} request={o.request} offerStatus={o.offer.status} />)
              : data.content.map((r) => <RequestCard key={r.id} request={r} offerStatus={r.myOfferStatus} />)}
          </div>
          <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
        </>
      )}

      <RequestFormModal open={creating} onClose={closeCreate} onSaved={() => (tab === 'mine' ? reload() : setTab('mine'))} />
    </div>
  )
}
