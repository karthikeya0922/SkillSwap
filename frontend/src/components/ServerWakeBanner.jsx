import { useEffect, useState } from 'react'
import { onServerWaking } from '../api/client'
import { Spinner } from './ui/Feedback'

/**
 * Shown only while a request has been waiting for several seconds, which on the free hosting plan almost always
 * means the server is waking up from sleep. Disappears by itself once the server answers.
 */
export default function ServerWakeBanner() {
  const [waking, setWaking] = useState(false)
  useEffect(() => onServerWaking(setWaking), [])
  if (!waking) return null
  return (
    <div role="status" aria-live="polite" className="fixed inset-x-0 bottom-4 z-[60] flex justify-center px-4">
      <div className="card flex max-w-md items-center gap-3 px-4 py-3 text-sm shadow-lg">
        <Spinner className="h-5 w-5 shrink-0" />
        <p className="text-slate-600">
          <span className="font-semibold text-slate-900">Starting the server…</span> It sleeps when nobody is using it,
          so the first request can take up to a minute. Your request will go through on its own.
        </p>
      </div>
    </div>
  )
}
