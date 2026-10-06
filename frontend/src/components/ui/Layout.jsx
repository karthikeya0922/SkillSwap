import { ChevronLeft, ChevronRight } from 'lucide-react'

export function PageHeader({ title, subtitle, actions, eyebrow }) {
  return (
    <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div>
        {eyebrow && <p className="mb-1 text-xs font-semibold tracking-wider text-brand-600 uppercase">{eyebrow}</p>}
        <h1 className="page-title">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-slate-500 sm:text-base">{subtitle}</p>}
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </div>
  )
}

export function Tabs({ tabs, value, onChange, className = '' }) {
  return (
    <div className={`scrollbar-thin -mx-1 flex gap-1 overflow-x-auto px-1 ${className}`}>
      <div className="inline-flex gap-1 rounded-xl bg-slate-100 p-1">
        {tabs.map((tab) => (
          <button
            key={tab.value}
            onClick={() => onChange(tab.value)}
            className={`flex items-center gap-1.5 rounded-lg px-3.5 py-1.5 text-sm font-medium whitespace-nowrap transition ${
              value === tab.value ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-500 hover:text-slate-800'
            }`}
          >
            {tab.label}
            {tab.count > 0 && (
              <span className={`rounded-full px-1.5 text-xs ${value === tab.value ? 'bg-brand-100 text-brand-700' : 'bg-slate-200 text-slate-600'}`}>
                {tab.count}
              </span>
            )}
          </button>
        ))}
      </div>
    </div>
  )
}

export function Pagination({ page, totalPages, onChange }) {
  if (!totalPages || totalPages <= 1) return null
  return (
    <div className="mt-6 flex items-center justify-center gap-3">
      <button className="btn btn-secondary btn-sm" disabled={page <= 0} onClick={() => onChange(page - 1)}>
        <ChevronLeft size={14} /> Previous
      </button>
      <span className="text-sm text-slate-500">
        Page {page + 1} of {totalPages}
      </span>
      <button className="btn btn-secondary btn-sm" disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>
        Next <ChevronRight size={14} />
      </button>
    </div>
  )
}

export function StatCard({ icon: Icon, label, value, hint, tone = 'brand' }) {
  const tones = {
    brand: 'bg-brand-50 text-brand-600',
    amber: 'bg-amber-50 text-amber-600',
    emerald: 'bg-emerald-50 text-emerald-600',
    sky: 'bg-sky-50 text-sky-600',
    rose: 'bg-rose-50 text-rose-600',
    violet: 'bg-violet-50 text-violet-600',
  }
  return (
    <div className="card p-4 sm:p-5">
      <div className="flex items-center justify-between">
        <p className="text-sm font-medium text-slate-500">{label}</p>
        {Icon && (
          <div className={`grid h-9 w-9 place-items-center rounded-xl ${tones[tone]}`}>
            <Icon size={18} />
          </div>
        )}
      </div>
      <p className="mt-2 text-2xl font-bold tracking-tight text-slate-900">{value}</p>
      {hint && <p className="mt-0.5 text-xs text-slate-500">{hint}</p>}
    </div>
  )
}
