import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ChevronDown, Coins, LogOut, Menu, MessagesSquare, Search, UserRound, Wifi, WifiOff } from 'lucide-react'
import Avatar from './ui/Avatar'
import NotificationPanel from './NotificationPanel'
import { useAuth } from '../context/AuthContext'
import { useRealtime } from '../context/NotificationContext'
import { walletService } from '../services'
import { formatCredits } from '../lib/format'

export default function Navbar({ onMenu }) {
  const { user, isAdmin, logout } = useAuth()
  const { unreadMessages, connected, subscribe } = useRealtime()
  const [menuOpen, setMenuOpen] = useState(false)
  const [credits, setCredits] = useState(null)
  const [query, setQuery] = useState('')
  const menuRef = useRef(null)
  const navigate = useNavigate()

  useEffect(() => {
    if (isAdmin) return undefined
    const load = () => walletService.balance().then((w) => setCredits(w.balance)).catch(() => {})
    load()
    // Credit-affecting events arrive as notifications; refresh the pill when they do.
    return subscribe('notifications', (n) => /SESSION|CREDITS/.test(n.type) && load())
  }, [isAdmin, subscribe])

  useEffect(() => {
    const close = (e) => menuRef.current && !menuRef.current.contains(e.target) && setMenuOpen(false)
    document.addEventListener('mousedown', close)
    return () => document.removeEventListener('mousedown', close)
  }, [])

  const search = (e) => {
    e.preventDefault()
    navigate(`/discover${query.trim() ? `?q=${encodeURIComponent(query.trim())}` : ''}`)
  }

  return (
    <header className="sticky top-0 z-20 flex h-16 items-center gap-3 border-b border-slate-200/70 bg-white/80 px-4 backdrop-blur-md sm:px-6">
      <button onClick={onMenu} className="btn-ghost -ml-1 rounded-lg p-2 lg:hidden" aria-label="Open menu">
        <Menu size={20} />
      </button>

      {!isAdmin && (
        <form onSubmit={search} className="relative hidden max-w-md flex-1 md:block">
          <Search size={16} className="absolute top-1/2 left-3.5 -translate-y-1/2 text-slate-400" />
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search students or skills…"
            className="input bg-slate-50 pl-10"
            aria-label="Search"
          />
        </form>
      )}

      <div className="ml-auto flex items-center gap-1 sm:gap-2">
        {!isAdmin && (
          <>
            <span
              className={`hidden items-center gap-1 text-[11px] font-medium sm:inline-flex ${connected ? 'text-emerald-600' : 'text-slate-400'}`}
              title={connected ? 'Live updates connected' : 'Reconnecting to live updates…'}
            >
              {connected ? <Wifi size={13} /> : <WifiOff size={13} />}
            </span>
            {credits !== null && (
              <Link to="/wallet" className="chip mr-1 bg-amber-50 py-1.5 text-amber-800 ring-1 ring-amber-200 hover:bg-amber-100" title="Skill credits">
                <Coins size={14} /> {formatCredits(credits)} <span className="hidden sm:inline">credits</span>
              </Link>
            )}
            <Link to="/messages" className="btn-ghost relative grid h-10 w-10 place-items-center rounded-xl" aria-label="Messages">
              <MessagesSquare size={19} />
              {unreadMessages > 0 && (
                <span className="absolute top-1.5 right-1.5 grid h-4 min-w-4 place-items-center rounded-full bg-brand-600 px-1 text-[10px] font-bold text-white">
                  {unreadMessages > 9 ? '9+' : unreadMessages}
                </span>
              )}
            </Link>
            <NotificationPanel />
          </>
        )}

        <div className="relative ml-1" ref={menuRef}>
          <button onClick={() => setMenuOpen((o) => !o)} className="flex items-center gap-2 rounded-xl p-1 pr-2 hover:bg-slate-100">
            <Avatar user={user} size="sm" />
            <span className="hidden max-w-[8rem] truncate text-sm font-medium text-slate-700 sm:block">{user?.fullName}</span>
            <ChevronDown size={14} className="text-slate-400" />
          </button>
          {menuOpen && (
            <div className="absolute right-0 mt-2 w-56 animate-slide-up rounded-2xl border border-slate-200 bg-white p-1.5 shadow-lift">
              <div className="border-b border-slate-100 px-3 py-2.5">
                <p className="truncate text-sm font-semibold text-slate-900">{user?.fullName}</p>
                <p className="truncate text-xs text-slate-500">{user?.email}</p>
              </div>
              {!isAdmin && (
                <Link to="/profile" onClick={() => setMenuOpen(false)} className="mt-1 flex items-center gap-2 rounded-lg px-3 py-2 text-sm text-slate-700 hover:bg-slate-50">
                  <UserRound size={16} /> My profile
                </Link>
              )}
              <button
                onClick={() => {
                  logout()
                  navigate('/login')
                }}
                className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-sm text-rose-600 hover:bg-rose-50"
              >
                <LogOut size={16} /> Sign out
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  )
}
