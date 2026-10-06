import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowDownLeft, ArrowUpRight, Coins, Gift, Hourglass, RotateCcw, TrendingUp, Wallet as WalletIcon } from 'lucide-react'
import { PageHeader, Pagination, StatCard } from '../components/ui/Layout'
import { EmptyState, ErrorState, Skeleton } from '../components/ui/Feedback'
import { useAsync } from '../hooks/useAsync'
import { walletService } from '../services'
import { formatCredits, formatDateTime } from '../lib/format'

const TYPE_META = {
  SIGNUP_BONUS: [Gift, 'bg-violet-50 text-violet-600', 'Welcome bonus'],
  SESSION_PAYMENT: [ArrowUpRight, 'bg-rose-50 text-rose-600', 'Learning session'],
  SESSION_REFUND: [RotateCcw, 'bg-sky-50 text-sky-600', 'Refund'],
  TEACHING_EARNING: [ArrowDownLeft, 'bg-emerald-50 text-emerald-600', 'Teaching session'],
  ADMIN_ADJUSTMENT: [Coins, 'bg-slate-100 text-slate-600', 'Adjustment'],
}

export default function Wallet() {
  const [page, setPage] = useState(0)
  const summary = useAsync(() => walletService.balance(), [])
  const txs = useAsync(() => walletService.transactions({ page, size: 15 }), [page])

  if (summary.error) return <ErrorState message={summary.error} onRetry={summary.reload} />
  const w = summary.data

  return (
    <div className="space-y-6">
      <PageHeader title="Time wallet" subtitle="1 completed teaching hour = 1 Skill Credit." />

      {summary.loading ? (
        <Skeleton className="h-40 w-full rounded-3xl" />
      ) : (
        <>
          <section className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-amber-400 via-orange-400 to-rose-400 p-6 text-white sm:p-8">
            <div className="absolute -right-8 -bottom-12 h-48 w-48 rounded-full bg-white/15 blur-xl" />
            <p className="inline-flex items-center gap-2 text-sm font-medium text-white/90"><WalletIcon size={16} /> Balance</p>
            <p className="mt-2 text-5xl font-extrabold tracking-tight">{formatCredits(w.balance)}<span className="ml-2 text-xl font-semibold text-white/80">credits</span></p>
            <p className="mt-2 text-sm text-white/90">
              {formatCredits(w.available)} available for new requests
              {Number(w.pending) > 0 && ` · ${formatCredits(w.pending)} committed to pending requests`}
            </p>
          </section>
          <section className="grid gap-4 sm:grid-cols-3">
            <StatCard icon={TrendingUp} tone="emerald" label="Total earned" value={formatCredits(w.totalEarned)} hint="From teaching sessions" />
            <StatCard icon={ArrowUpRight} tone="rose" label="Total spent" value={formatCredits(w.totalSpent)} hint="On learning sessions (net of refunds)" />
            <StatCard icon={Hourglass} tone="sky" label="Held in escrow" value={formatCredits(w.held)} hint="For accepted upcoming sessions" />
          </section>
        </>
      )}

      <section>
        <h2 className="section-title mb-3">Transaction history</h2>
        {txs.loading ? (
          <Skeleton className="h-64 w-full rounded-2xl" />
        ) : txs.error ? (
          <ErrorState message={txs.error} onRetry={txs.reload} />
        ) : txs.data.content.length === 0 ? (
          <EmptyState icon={Coins} title="No transactions yet" message="Teach or learn a session and your credits will show up here." />
        ) : (
          <>
            <div className="card divide-y divide-slate-100">
              {txs.data.content.map((tx) => {
                const [Icon, tone, kind] = TYPE_META[tx.type] || TYPE_META.ADMIN_ADJUSTMENT
                const positive = Number(tx.amount) >= 0
                const row = (
                  <>
                    <div className={`grid h-10 w-10 shrink-0 place-items-center rounded-full ${tone}`}><Icon size={17} /></div>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium text-slate-900">{tx.description}</p>
                      <p className="text-xs text-slate-500">{kind} · {formatDateTime(tx.createdAt)}</p>
                    </div>
                    <div className="text-right">
                      <p className={`font-bold ${positive ? 'text-emerald-600' : 'text-rose-600'}`}>{formatCredits(tx.amount, { sign: true })}</p>
                      <p className="text-[11px] text-slate-400">bal. {formatCredits(tx.balanceAfter)}</p>
                    </div>
                  </>
                )
                return tx.sessionId ? (
                  <Link key={tx.id} to={`/sessions/${tx.sessionId}`} className="flex items-center gap-3 p-4 hover:bg-slate-50">{row}</Link>
                ) : (
                  <div key={tx.id} className="flex items-center gap-3 p-4">{row}</div>
                )
              })}
            </div>
            <Pagination page={txs.data.page} totalPages={txs.data.totalPages} onChange={setPage} />
          </>
        )}
      </section>
    </div>
  )
}
