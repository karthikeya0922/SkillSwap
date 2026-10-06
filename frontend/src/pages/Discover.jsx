import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Compass, Search, SlidersHorizontal, X } from 'lucide-react'
import { PageHeader, Pagination } from '../components/ui/Layout'
import { EmptyState, ErrorState, GridSkeleton } from '../components/ui/Feedback'
import { Input, Select } from '../components/ui/Field'
import Button from '../components/ui/Button'
import UserCard from '../components/UserCard'
import SkillPicker from '../components/SkillPicker'
import { useAsync } from '../hooks/useAsync'
import { skillService, userService } from '../services'
import { AVAILABILITY, LEVELS, LEVEL_META } from '../lib/constants'

const FILTER_KEYS = ['q', 'skillId', 'categoryId', 'level', 'minRating', 'department', 'availability', 'sort']

export default function Discover() {
  const [params, setParams] = useSearchParams()
  const [page, setPage] = useState(0)
  const [showFilters, setShowFilters] = useState(false)
  const [query, setQuery] = useState(params.get('q') || '')
  const filters = Object.fromEntries(FILTER_KEYS.map((k) => [k, params.get(k) || '']))
  const filterKey = params.toString()

  const { data: meta } = useAsync(() => Promise.all([skillService.categories(), userService.discoverMeta()]), [])
  const [categories, departments] = meta ? [meta[0], meta[1].departments] : [[], []]

  useEffect(() => setPage(0), [filterKey])
  useEffect(() => setQuery(params.get('q') || ''), [params])

  const { data, loading, error, reload } = useAsync(() => {
    const clean = Object.fromEntries(Object.entries(filters).filter(([, v]) => v !== ''))
    return userService.discover({ ...clean, page, size: 12 })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filterKey, page])

  const setFilter = (key, value) => {
    const next = new URLSearchParams(params)
    if (value === '' || value == null) next.delete(key)
    else next.set(key, value)
    setParams(next, { replace: true })
  }
  const activeCount = FILTER_KEYS.filter((k) => k !== 'q' && k !== 'sort' && filters[k]).length

  return (
    <div>
      <PageHeader title="Discover students" subtitle="Browse classmates by skill, level, rating and availability." />

      <div className="card mb-6 p-4">
        <div className="flex flex-col gap-3 sm:flex-row">
          <form
            className="relative flex-1"
            onSubmit={(e) => {
              e.preventDefault()
              setFilter('q', query.trim())
            }}
          >
            <Search size={16} className="absolute top-1/2 left-3.5 -translate-y-1/2 text-slate-400" />
            <Input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search by name, skill or department" className="pl-10" />
          </form>
          <div className="flex gap-2">
            <Select value={filters.sort || 'match'} onChange={(e) => setFilter('sort', e.target.value === 'match' ? '' : e.target.value)} className="sm:w-44" aria-label="Sort by">
              <option value="match">Best match</option>
              <option value="rating">Highest rated</option>
              <option value="sessions">Most sessions</option>
              <option value="newest">Newest</option>
              <option value="name">Name A–Z</option>
            </Select>
            <Button variant={showFilters || activeCount ? 'soft' : 'secondary'} icon={SlidersHorizontal} onClick={() => setShowFilters((s) => !s)}>
              Filters{activeCount ? ` (${activeCount})` : ''}
            </Button>
          </div>
        </div>

        {showFilters && (
          <div className="mt-4 grid animate-fade-in gap-3 border-t border-slate-100 pt-4 sm:grid-cols-2 lg:grid-cols-3">
            <SkillPicker value={filters.skillId ? Number(filters.skillId) : null} onChange={(v) => setFilter('skillId', v ?? '')} placeholder="Any skill they teach" aria-label="Skill" />
            <Select value={filters.categoryId} onChange={(e) => setFilter('categoryId', e.target.value)} aria-label="Category">
              <option value="">Any category</option>
              {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
            </Select>
            <Select value={filters.level} onChange={(e) => setFilter('level', e.target.value)} aria-label="Proficiency">
              <option value="">Any proficiency</option>
              {LEVELS.map((l) => <option key={l} value={l}>{LEVEL_META[l].label} or above</option>)}
            </Select>
            <Select value={filters.minRating} onChange={(e) => setFilter('minRating', e.target.value)} aria-label="Minimum rating">
              <option value="">Any rating</option>
              <option value="4.5">4.5★ and up</option>
              <option value="4">4★ and up</option>
              <option value="3">3★ and up</option>
            </Select>
            <Select value={filters.department} onChange={(e) => setFilter('department', e.target.value)} aria-label="Department">
              <option value="">Any department</option>
              {departments.map((d) => <option key={d} value={d}>{d}</option>)}
            </Select>
            <Select value={filters.availability} onChange={(e) => setFilter('availability', e.target.value)} aria-label="Availability">
              <option value="">Any availability</option>
              {AVAILABILITY.map((a) => <option key={a.value} value={a.value}>{a.label}</option>)}
            </Select>
            {activeCount > 0 && (
              <button
                className="inline-flex items-center gap-1 justify-self-start text-sm font-medium text-slate-500 hover:text-slate-800"
                onClick={() => {
                  const next = new URLSearchParams()
                  if (filters.q) next.set('q', filters.q)
                  setParams(next, { replace: true })
                }}
              >
                <X size={14} /> Clear filters
              </button>
            )}
          </div>
        )}
      </div>

      {loading ? (
        <GridSkeleton />
      ) : error ? (
        <ErrorState message={error} onRetry={reload} />
      ) : data.content.length === 0 ? (
        <EmptyState icon={Compass} title="No students found" message="Try removing a filter or searching for a different skill." />
      ) : (
        <>
          <p className="mb-3 text-sm text-slate-500">{data.totalElements} student{data.totalElements === 1 ? '' : 's'} found</p>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
            {data.content.map((u) => <UserCard key={u.id} user={u} />)}
          </div>
          <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  )
}
