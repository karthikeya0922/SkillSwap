/** Form controls styled with the design system. Works with react-hook-form's register() (React 19 ref-as-prop). */

export function Field({ label, error, hint, children, className = '', required }) {
  return (
    <div className={className}>
      {label && (
        <label className="label">
          {label}
          {required && <span className="ml-0.5 text-rose-500">*</span>}
        </label>
      )}
      {children}
      {error ? (
        <p className="mt-1.5 text-xs font-medium text-rose-600">{error}</p>
      ) : (
        hint && <p className="mt-1.5 text-xs text-slate-500">{hint}</p>
      )}
    </div>
  )
}

export function Input({ error, className = '', ...props }) {
  return <input className={`input ${error ? 'input-error' : ''} ${className}`} {...props} />
}

export function Textarea({ error, className = '', rows = 4, ...props }) {
  return <textarea rows={rows} className={`input resize-y ${error ? 'input-error' : ''} ${className}`} {...props} />
}

export function Select({ error, className = '', children, ...props }) {
  return (
    <select className={`input cursor-pointer pr-8 ${error ? 'input-error' : ''} ${className}`} {...props}>
      {children}
    </select>
  )
}
