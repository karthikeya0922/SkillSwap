import { Link } from 'react-router-dom'
import { Repeat } from 'lucide-react'

export default function Logo({ to = '/', light = false, compact = false }) {
  return (
    <Link to={to} className="flex items-center gap-2" aria-label="SkillSwap home">
      <span className="grid h-9 w-9 place-items-center rounded-xl bg-gradient-to-br from-brand-500 to-violet-500 text-white shadow-md shadow-brand-500/30">
        <Repeat size={18} strokeWidth={2.5} />
      </span>
      {!compact && (
        <span className={`text-lg font-extrabold tracking-tight ${light ? 'text-white' : 'text-slate-900'}`}>
          Skill<span className={light ? 'text-brand-200' : 'text-brand-600'}>Swap</span>
        </span>
      )}
    </Link>
  )
}
