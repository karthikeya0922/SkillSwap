/** Small status pill. `meta` is an entry from lib/constants (label + className). */
export default function Badge({ meta, children, className = '' }) {
  return (
    <span className={`chip ring-1 ring-inset ${meta?.className || 'bg-slate-100 text-slate-700 ring-slate-200'} ${className}`}>
      {children || meta?.label}
    </span>
  )
}
