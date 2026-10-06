import { Area, AreaChart, Bar, BarChart, CartesianGrid, Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { Activity, CalendarCheck, CalendarDays, Coins, Flag, HandHelping, Layers, Star, Users, UsersRound } from 'lucide-react'
import { PageHeader, StatCard } from '../../components/ui/Layout'
import { ErrorState, Skeleton } from '../../components/ui/Feedback'
import Button from '../../components/ui/Button'
import { useAsync } from '../../hooks/useAsync'
import { adminService } from '../../services'
import { formatCredits } from '../../lib/format'

const BRAND = '#4f46e5'
const OUTCOME_COLORS = { Completed: '#10b981', Cancelled: '#f43f5e', Declined: '#f59e0b', Upcoming: '#6366f1' }
const AXIS = { fontSize: 12, fill: '#64748b' }
const tooltipStyle = { borderRadius: 12, border: '1px solid #e2e8f0', boxShadow: '0 8px 24px -8px rgb(15 23 42 / 0.15)', fontSize: 13 }

function ChartCard({ title, subtitle, children, className = '' }) {
  return (
    <section className={`card p-5 ${className}`}>
      <h2 className="font-semibold text-slate-900">{title}</h2>
      {subtitle && <p className="text-xs text-slate-500">{subtitle}</p>}
      <div className="mt-4 h-64">{children}</div>
    </section>
  )
}

export default function AdminDashboard() {
  const { data, loading, error, reload } = useAsync(() => adminService.analytics(), [])

  if (loading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-10 w-64" />
        <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">{Array.from({ length: 8 }).map((_, i) => <Skeleton key={i} className="h-28 rounded-2xl" />)}</div>
        <div className="grid gap-4 lg:grid-cols-2"><Skeleton className="h-80 rounded-2xl" /><Skeleton className="h-80 rounded-2xl" /></div>
      </div>
    )
  }
  if (error) return <ErrorState message={error} onRetry={reload} />
  const t = data.totals

  return (
    <div className="space-y-6">
      <PageHeader eyebrow="Admin" title="Platform analytics" subtitle="Health of the SkillSwap community at a glance." actions={t.openReports > 0 && <Button variant="danger" icon={Flag} to="/admin/reports">{t.openReports} open report{t.openReports > 1 ? 's' : ''}</Button>} />

      <section className="grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-4">
        <StatCard icon={Users} label="Total users" value={t.totalUsers} hint="Registered students" />
        <StatCard icon={Activity} tone="emerald" label="Active users" value={t.activeUsers} hint="Active in the last 30 days" />
        <StatCard icon={Layers} tone="violet" label="Total skills" value={t.totalSkills} hint="Active in the catalogue" />
        <StatCard icon={CalendarDays} tone="sky" label="Total sessions" value={t.totalSessions} hint="All statuses" />
        <StatCard icon={CalendarCheck} tone="emerald" label="Completed sessions" value={t.completedSessions} />
        <StatCard icon={HandHelping} tone="amber" label="Active requests" value={t.activeRequests} hint="Open, with offers or accepted" />
        <StatCard icon={Coins} tone="amber" label="Credits exchanged" value={formatCredits(t.creditsExchanged)} hint="Paid to teachers" />
        <StatCard icon={Star} tone="rose" label="Average rating" value={t.averageRating ? t.averageRating.toFixed(1) : '—'} hint={`${t.totalConnections} connections made`} />
      </section>

      <div className="grid gap-4 lg:grid-cols-2">
        <ChartCard title="User growth" subtitle="Total students and new sign-ups per month">
          <ResponsiveContainer>
            <AreaChart data={data.userGrowth} margin={{ left: -20, right: 8, top: 4 }}>
              <defs>
                <linearGradient id="growth" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor={BRAND} stopOpacity={0.3} />
                  <stop offset="100%" stopColor={BRAND} stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid stroke="#f1f5f9" vertical={false} />
              <XAxis dataKey="month" tick={AXIS} axisLine={false} tickLine={false} />
              <YAxis tick={AXIS} axisLine={false} tickLine={false} allowDecimals={false} />
              <Tooltip contentStyle={tooltipStyle} />
              <Area type="monotone" dataKey="totalUsers" name="Total students" stroke={BRAND} strokeWidth={2.5} fill="url(#growth)" />
              <Area type="monotone" dataKey="newUsers" name="New this month" stroke="#a78bfa" strokeWidth={2} fill="transparent" />
            </AreaChart>
          </ResponsiveContainer>
        </ChartCard>

        <ChartCard title="Sessions per month" subtitle="Completed versus cancelled or declined">
          <ResponsiveContainer>
            <BarChart data={data.sessionsPerMonth} margin={{ left: -20, right: 8, top: 4 }}>
              <CartesianGrid stroke="#f1f5f9" vertical={false} />
              <XAxis dataKey="month" tick={AXIS} axisLine={false} tickLine={false} />
              <YAxis tick={AXIS} axisLine={false} tickLine={false} allowDecimals={false} />
              <Tooltip contentStyle={tooltipStyle} cursor={{ fill: '#f8fafc' }} />
              <Legend iconType="circle" wrapperStyle={{ fontSize: 12 }} />
              <Bar dataKey="completed" name="Completed" stackId="s" fill="#10b981" radius={[0, 0, 0, 0]} />
              <Bar dataKey="cancelled" name="Cancelled / declined" stackId="s" fill="#fda4af" radius={[6, 6, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </ChartCard>

        <ChartCard title="Most popular skills" subtitle="Profiles listing the skill (teach or learn)">
          <ResponsiveContainer>
            <BarChart data={data.popularSkills} layout="vertical" margin={{ left: 10, right: 16 }}>
              <CartesianGrid stroke="#f1f5f9" horizontal={false} />
              <XAxis type="number" tick={AXIS} axisLine={false} tickLine={false} allowDecimals={false} />
              <YAxis type="category" dataKey="name" tick={AXIS} axisLine={false} tickLine={false} width={120} />
              <Tooltip contentStyle={tooltipStyle} cursor={{ fill: '#f8fafc' }} />
              <Bar dataKey="value" name="Profiles" fill={BRAND} radius={[0, 6, 6, 0]} barSize={16} />
            </BarChart>
          </ResponsiveContainer>
        </ChartCard>

        <ChartCard title="Most active categories" subtitle="Sessions booked per skill category">
          <ResponsiveContainer>
            <BarChart data={data.activeCategories} margin={{ left: -20, right: 8, top: 4 }}>
              <CartesianGrid stroke="#f1f5f9" vertical={false} />
              <XAxis dataKey="name" tick={{ ...AXIS, fontSize: 11 }} axisLine={false} tickLine={false} interval={0} angle={-20} textAnchor="end" height={50} />
              <YAxis tick={AXIS} axisLine={false} tickLine={false} allowDecimals={false} />
              <Tooltip contentStyle={tooltipStyle} cursor={{ fill: '#f8fafc' }} />
              <Bar dataKey="value" name="Sessions" fill="#8b5cf6" radius={[6, 6, 0, 0]} barSize={28} />
            </BarChart>
          </ResponsiveContainer>
        </ChartCard>

        <ChartCard title="Session outcomes" subtitle="Completed vs cancelled, all time" className="lg:col-span-2">
          <div className="flex h-full flex-col items-center gap-6 sm:flex-row">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={data.sessionOutcomes} dataKey="value" nameKey="name" innerRadius="55%" outerRadius="85%" paddingAngle={3} stroke="none">
                  {data.sessionOutcomes.map((o) => <Cell key={o.name} fill={OUTCOME_COLORS[o.name]} />)}
                </Pie>
                <Tooltip contentStyle={tooltipStyle} />
              </PieChart>
            </ResponsiveContainer>
            <ul className="w-full space-y-2 sm:w-64">
              {data.sessionOutcomes.map((o) => (
                <li key={o.name} className="flex items-center gap-2 text-sm">
                  <span className="h-3 w-3 rounded-full" style={{ background: OUTCOME_COLORS[o.name] }} />
                  <span className="flex-1 text-slate-600">{o.name}</span>
                  <span className="font-semibold text-slate-900">{o.value}</span>
                </li>
              ))}
            </ul>
          </div>
        </ChartCard>
      </div>
      <p className="text-center text-xs text-slate-400"><UsersRound size={12} className="inline" /> Figures update live from the database.</p>
    </div>
  )
}
