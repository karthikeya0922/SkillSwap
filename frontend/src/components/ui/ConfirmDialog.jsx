import { useState } from 'react'
import { TriangleAlert } from 'lucide-react'
import Modal from './Modal'
import Button from './Button'
import { Textarea } from './Field'

/**
 * Confirmation dialog. With `withReason`, shows an optional text box and passes its value to onConfirm.
 * onConfirm may return a promise; the dialog shows a spinner until it settles.
 */
export default function ConfirmDialog({
  open,
  onClose,
  onConfirm,
  title = 'Are you sure?',
  message,
  confirmLabel = 'Confirm',
  tone = 'danger',
  withReason = false,
  reasonPlaceholder = 'Add a short reason (optional)',
}) {
  const [busy, setBusy] = useState(false)
  const [reason, setReason] = useState('')

  const handleConfirm = async () => {
    setBusy(true)
    try {
      await onConfirm(reason.trim() || undefined)
      setReason('')
      onClose()
    } catch {
      /* caller shows the error toast */
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal
      open={open}
      onClose={busy ? undefined : onClose}
      title={title}
      size="sm"
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={busy}>
            Go back
          </Button>
          <Button variant={tone === 'danger' ? 'danger' : 'primary'} loading={busy} onClick={handleConfirm}>
            {confirmLabel}
          </Button>
        </>
      }
    >
      <div className="flex gap-3">
        {tone === 'danger' && (
          <div className="grid h-10 w-10 shrink-0 place-items-center rounded-full bg-rose-50 text-rose-600">
            <TriangleAlert size={18} />
          </div>
        )}
        <div className="flex-1 space-y-3">
          {message && <p className="text-sm text-slate-600">{message}</p>}
          {withReason && (
            <Textarea rows={3} value={reason} onChange={(e) => setReason(e.target.value)} placeholder={reasonPlaceholder} maxLength={300} />
          )}
        </div>
      </div>
    </Modal>
  )
}
