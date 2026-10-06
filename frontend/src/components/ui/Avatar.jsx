import { assetUrl } from '../../api/client'
import { initials } from '../../lib/format'

const GRADIENTS = [
  'from-indigo-500 to-violet-500',
  'from-sky-500 to-cyan-400',
  'from-emerald-500 to-teal-400',
  'from-amber-500 to-orange-400',
  'from-rose-500 to-pink-400',
  'from-fuchsia-500 to-purple-500',
  'from-blue-600 to-indigo-400',
  'from-teal-600 to-emerald-400',
]

const SIZES = {
  xs: 'h-7 w-7 text-[10px]',
  sm: 'h-9 w-9 text-xs',
  md: 'h-11 w-11 text-sm',
  lg: 'h-14 w-14 text-base',
  xl: 'h-24 w-24 text-2xl',
}
const DOT = { xs: 'h-2 w-2', sm: 'h-2.5 w-2.5', md: 'h-3 w-3', lg: 'h-3.5 w-3.5', xl: 'h-5 w-5' }

export default function Avatar({ user, size = 'md', online, className = '' }) {
  const name = user?.fullName || '?'
  const gradient = GRADIENTS[(user?.id || name.length) % GRADIENTS.length]
  const src = assetUrl(user?.avatarUrl)
  return (
    <div className={`relative shrink-0 ${className}`}>
      {src ? (
        <img src={src} alt={name} className={`${SIZES[size]} rounded-full object-cover ring-2 ring-white`} />
      ) : (
        <div className={`${SIZES[size]} grid place-items-center rounded-full bg-gradient-to-br ${gradient} font-semibold text-white ring-2 ring-white`} aria-label={name}>
          {initials(name)}
        </div>
      )}
      {online !== undefined && (
        <span
          className={`absolute right-0 bottom-0 ${DOT[size]} rounded-full ring-2 ring-white ${online ? 'bg-emerald-500' : 'bg-slate-300'}`}
          title={online ? 'Online' : 'Offline'}
        />
      )}
    </div>
  )
}
