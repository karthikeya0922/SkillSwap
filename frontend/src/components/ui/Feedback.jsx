import { CircleAlert, RotateCcw } from 'lucide-react'
import Button from './Button'

export function Skeleton({ className = '' }) {
  return <div className={`skeleton ${className}`} />
}

export function CardSkeleton({ lines = 3 }) {
  return (
    <div className="card space-y-3 p-5">
      <div className="flex items-center gap-3">
        <Skeleton className="h-11 w-11 rounded-full" />
        <div className="flex-1 space-y-2">
          <Skeleton className="h-3.5 w-1/2" />
          <Skeleton className="h-3 w-1/3" />
        </div>
      </div>
      {Array.from({ length: lines }).map((_, i) => (
        <Skeleton key={i} className={`h-3 ${i % 2 ? 'w-4/5' : 'w-full'}`} />
      ))}
    </div>
  )
}

export function GridSkeleton({ count = 6, className = 'grid gap-4 sm:grid-cols-2 xl:grid-cols-3' }) {
  return (
    <div className={className}>
      {Array.from({ length: count }).map((_, i) => (
        <CardSkeleton key={i} />
      ))}
    </div>
  )
}

export function EmptyState({ icon: Icon, title, message, action, className = '' }) {
  return (
    <div className={`card flex flex-col items-center px-6 py-12 text-center ${className}`}>
      {Icon && (
        <div className="mb-4 grid h-14 w-14 place-items-center rounded-2xl bg-brand-50 text-brand-600">
          <Icon size={26} />
        </div>
      )}
      <h3 className="text-base font-semibold text-slate-900">{title}</h3>
      {message && <p className="mt-1 max-w-sm text-sm text-slate-500">{message}</p>}
      {action && <div className="mt-5">{action}</div>}
    </div>
  )
}

export function ErrorState({ message, onRetry }) {
  return (
    <div className="card flex flex-col items-center px-6 py-10 text-center">
      <div className="mb-3 grid h-12 w-12 place-items-center rounded-full bg-rose-50 text-rose-600">
        <CircleAlert size={22} />
      </div>
      <p className="font-medium text-slate-800">We couldn't load this</p>
      <p className="mt-1 max-w-sm text-sm text-slate-500">{message}</p>
      {onRetry && (
        <Button variant="secondary" size="sm" icon={RotateCcw} className="mt-4" onClick={onRetry}>
          Try again
        </Button>
      )}
    </div>
  )
}

export function Spinner({ className = '' }) {
  return <div className={`h-6 w-6 animate-spin rounded-full border-2 border-brand-200 border-t-brand-600 ${className}`} />
}

export function FullPageSpinner() {
  return (
    <div className="grid min-h-[60vh] place-items-center">
      <Spinner className="h-8 w-8" />
    </div>
  )
}
