import { useState } from 'react'
import { Check, Clock, MessageCircle, UserPlus } from 'lucide-react'
import Button from './ui/Button'
import { connectionService } from '../services'
import { useToast } from '../context/ToastContext'

/** Connect / pending / accept / message button driven by the viewer's connection state. */
export default function ConnectButton({ userId, connection, onChange, size = 'sm', fullWidth = false }) {
  const toast = useToast()
  const [state, setState] = useState(connection?.state || 'NONE')
  const [connectionId, setConnectionId] = useState(connection?.connectionId || null)
  const [busy, setBusy] = useState(false)
  const width = fullWidth ? 'w-full' : ''

  const run = async (fn, nextState, message) => {
    setBusy(true)
    try {
      const result = await fn()
      setState(nextState)
      if (result?.id) setConnectionId(result.id)
      toast.success(message)
      onChange?.(nextState)
    } catch (e) {
      toast.error('Could not update connection', e.message)
    } finally {
      setBusy(false)
    }
  }

  if (state === 'SELF') return null
  if (state === 'CONNECTED') {
    return (
      <Button size={size} variant="soft" icon={MessageCircle} to={`/messages/${userId}`} className={width}>
        Message
      </Button>
    )
  }
  if (state === 'PENDING_SENT') {
    return (
      <Button size={size} variant="secondary" icon={Clock} disabled className={width}>
        Request sent
      </Button>
    )
  }
  if (state === 'PENDING_RECEIVED') {
    return (
      <Button
        size={size}
        variant="success"
        icon={Check}
        loading={busy}
        className={width}
        onClick={() => run(() => connectionService.respond(connectionId, 'ACCEPT'), 'CONNECTED', 'Connection accepted')}
      >
        Accept request
      </Button>
    )
  }
  return (
    <Button
      size={size}
      icon={UserPlus}
      loading={busy}
      className={width}
      onClick={() => run(() => connectionService.send(userId), 'PENDING_SENT', 'Connection request sent')}
    >
      Connect
    </Button>
  )
}
