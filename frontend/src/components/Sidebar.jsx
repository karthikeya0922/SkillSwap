import { NavLink } from 'react-router-dom'
import {
  CalendarDays,
  ChartBar,
  Compass,
  Flag,
  Flame,
  HandHelping,
  LayoutDashboard,
  Layers,
  MessagesSquare,
  Swords,
  Trophy,
  UserRound,
  Users,
  UsersRound,
  Wallet,
  X,
} from 'lucide-react'
import Logo from './Logo'
import { useAuth } from '../context/AuthContext'
import { useRealtime } from '../context/NotificationContext'

const STUDENT_NAV = [
  { section: 'Overview' },
  { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { to: '/discover', label: 'Discover', icon: Compass },
  { to: '/matches', label: 'Matches', icon: Flame },
  { section: 'Learn & teach' },
  { to: '/sessions', label: 'Sessions', icon: CalendarDays },
  { to: '/requests', label: 'Learning requests', icon: HandHelping },
  { to: '/wallet', label: 'Time wallet', icon: Wallet },
  { section: 'Community' },
  { to: '/messages', label: 'Messages', icon: MessagesSquare, badge: 'messages' },
  { to: '/connections', label: 'Connections', icon: UsersRound },
  { to: '/groups', label: 'Groups', icon: Users },
  { to: '/challenges', label: 'Challenges', icon: Swords },
  { to: '/leaderboard', label: 'Leaderboard', icon: Trophy },
  { to: '/profile', label: 'My profile', icon: UserRound },
]

const ADMIN_NAV = [
  { section: 'Admin' },
  { to: '/admin', label: 'Analytics', icon: ChartBar, end: true },
  { to: '/admin/users', label: 'Users', icon: UsersRound },
  { to: '/admin/skills', label: 'Skills & categories', icon: Layers },
  { to: '/admin/reports', label: 'Reports', icon: Flag },
]

export default function Sidebar({ open, onClose }) {
  const { isAdmin } = useAuth()
  const { unreadMessages } = useRealtime()
  const items = isAdmin ? ADMIN_NAV : STUDENT_NAV

  return (
    <>
      {open && <div className="fixed inset-0 z-30 bg-slate-900/30 backdrop-blur-sm lg:hidden" onClick={onClose} />}
      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-64 flex-col border-r border-slate-200/70 bg-white transition-transform duration-200 lg:translate-x-0 ${
          open ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <div className="flex h-16 items-center justify-between px-5">
          <Logo to={isAdmin ? '/admin' : '/dashboard'} />
          <button onClick={onClose} className="btn-ghost rounded-lg p-1.5 lg:hidden" aria-label="Close menu">
            <X size={18} />
          </button>
        </div>
        <nav className="scrollbar-thin flex-1 overflow-y-auto px-3 pb-6">
          {items.map((item) =>
            item.section ? (
              <p key={item.section} className="mt-5 mb-1.5 px-3 text-[11px] font-semibold tracking-wider text-slate-400 uppercase">
                {item.section}
              </p>
            ) : (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.end}
                onClick={onClose}
                className={({ isActive }) =>
                  `group mb-0.5 flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition ${
                    isActive ? 'bg-brand-50 text-brand-700' : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
                  }`
                }
              >
                <item.icon size={18} className="shrink-0" />
                <span className="flex-1">{item.label}</span>
                {item.badge === 'messages' && unreadMessages > 0 && (
                  <span className="rounded-full bg-brand-600 px-2 py-0.5 text-[10px] font-bold text-white">{unreadMessages}</span>
                )}
              </NavLink>
            ),
          )}
        </nav>
        {!isAdmin && (
          <div className="m-3 rounded-2xl bg-gradient-to-br from-brand-600 to-violet-600 p-4 text-white">
            <p className="text-sm font-semibold">Teach to earn credits</p>
            <p className="mt-1 text-xs text-brand-100">Every hour you teach earns 1 Skill Credit to spend on learning.</p>
            <NavLink to="/requests" onClick={onClose} className="mt-3 inline-block rounded-lg bg-white/20 px-3 py-1.5 text-xs font-semibold hover:bg-white/30">
              Browse requests →
            </NavLink>
          </div>
        )}
      </aside>
    </>
  )
}
