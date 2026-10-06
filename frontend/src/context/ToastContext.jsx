import { createContext, useCallback, useContext, useMemo, useRef, useState } from 'react'
import { CircleCheck, CircleAlert, Info, X } from 'lucide-react'

const ToastContext = createContext(null)

const STYLES = {
  success: { icon: CircleCheck, className: 'text-emerald-600' },
  error: { icon: CircleAlert, className: 'text-rose-600' },
  info: { icon: Info, className: 'text-brand-600' },
}

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])
  const nextId = useRef(1)

  const dismiss = useCallback((id) => setToasts((list) => list.filter((t) => t.id !== id)), [])

  const show = useCallback(
    (type, title, message, { duration = 4500, onClick } = {}) => {
      const id = nextId.current++
      setToasts((list) => [...list.slice(-3), { id, type, title, message, onClick }])
      setTimeout(() => dismiss(id), duration)
    },
    [dismiss],
  )

  const api = useMemo(
    () => ({
      success: (title, message, opts) => show('success', title, message, opts),
      error: (title, message, opts) => show('error', title, message, opts),
      info: (title, message, opts) => show('info', title, message, opts),
    }),
    [show],
  )

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div className="pointer-events-none fixed inset-x-0 top-3 z-[60] flex flex-col items-center gap-2 px-3 sm:top-auto sm:right-4 sm:bottom-4 sm:left-auto sm:items-end">
        {toasts.map((toast) => {
          const { icon: Icon, className } = STYLES[toast.type]
          return (
            <div
              key={toast.id}
              role="status"
              onClick={() => {
                toast.onClick?.()
                dismiss(toast.id)
              }}
              className={`pointer-events-auto flex w-full max-w-sm animate-slide-up items-start gap-3 rounded-xl border border-slate-200 bg-white p-3.5 shadow-lift ${toast.onClick ? 'cursor-pointer' : ''}`}
            >
              <Icon size={18} className={`mt-0.5 shrink-0 ${className}`} />
              <div className="min-w-0 flex-1">
                <p className="text-sm font-semibold text-slate-900">{toast.title}</p>
                {toast.message && <p className="mt-0.5 text-sm text-slate-600">{toast.message}</p>}
              </div>
              <button
                onClick={(e) => {
                  e.stopPropagation()
                  dismiss(toast.id)
                }}
                className="text-slate-400 hover:text-slate-600"
                aria-label="Dismiss"
              >
                <X size={16} />
              </button>
            </div>
          )
        })}
      </div>
    </ToastContext.Provider>
  )
}

export const useToast = () => useContext(ToastContext)
