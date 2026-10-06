import { useState } from 'react'
import { Flag } from 'lucide-react'
import Modal from './ui/Modal'
import Button from './ui/Button'
import { Field, Textarea } from './ui/Field'
import { REPORT_REASONS } from '../lib/constants'
import { reportService } from '../services'
import { useToast } from '../context/ToastContext'

/** Report a user or a piece of content to the moderators. */
export default function ReportModal({ open, onClose, targetType, targetId, targetLabel }) {
  const toast = useToast()
  const [reason, setReason] = useState('SPAM')
  const [details, setDetails] = useState('')
  const [busy, setBusy] = useState(false)

  const submit = async () => {
    setBusy(true)
    try {
      await reportService.report({ targetType, targetId, reason, details: details.trim() || null })
      toast.success('Report submitted', 'Thanks — our moderators will review it.')
      setDetails('')
      onClose()
    } catch (e) {
      toast.error('Could not submit report', e.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal
      open={open}
      onClose={onClose}
      title="Report"
      description={targetLabel ? `Reporting ${targetLabel}` : 'Help us keep SkillSwap safe.'}
      size="sm"
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button variant="danger" icon={Flag} loading={busy} onClick={submit}>
            Submit report
          </Button>
        </>
      }
    >
      <div className="space-y-4">
        <Field label="Reason">
          <div className="grid grid-cols-2 gap-2">
            {REPORT_REASONS.map((r) => (
              <button
                key={r.value}
                type="button"
                onClick={() => setReason(r.value)}
                className={`rounded-xl border px-3 py-2 text-left text-sm transition ${
                  reason === r.value ? 'border-rose-300 bg-rose-50 font-medium text-rose-700' : 'border-slate-200 hover:border-slate-300'
                }`}
              >
                {r.label}
              </button>
            ))}
          </div>
        </Field>
        <Field label="Details" hint="What happened? Specifics help moderators act faster.">
          <Textarea rows={3} maxLength={1000} value={details} onChange={(e) => setDetails(e.target.value)} />
        </Field>
      </div>
    </Modal>
  )
}
