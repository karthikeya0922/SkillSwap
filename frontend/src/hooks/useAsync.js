import { useCallback, useEffect, useRef, useState } from 'react'

/**
 * Runs an async loader and tracks { data, loading, error }. Re-runs when `deps` change; stale responses from
 * earlier runs are ignored. `reload()` re-runs on demand and `setData` allows optimistic updates.
 */
export function useAsync(loader, deps = []) {
  const [state, setState] = useState({ data: null, loading: true, error: null })
  const runId = useRef(0)

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const run = useCallback(loader, deps)

  const reload = useCallback(
    ({ silent = false } = {}) => {
      const id = ++runId.current
      if (!silent) setState((s) => ({ ...s, loading: true, error: null }))
      return Promise.resolve()
        .then(run)
        .then((data) => {
          if (id === runId.current) setState({ data, loading: false, error: null })
          return data
        })
        .catch((error) => {
          if (id === runId.current) setState((s) => ({ ...s, loading: false, error: error.message }))
        })
    },
    [run],
  )

  useEffect(() => {
    reload()
  }, [reload])

  const setData = useCallback((updater) => {
    setState((s) => ({ ...s, data: typeof updater === 'function' ? updater(s.data) : updater }))
  }, [])

  return { ...state, reload, setData }
}

/** Wraps an action with a busy flag and toast feedback. */
export function useAction(toast) {
  const [busy, setBusy] = useState(false)
  const run = useCallback(
    async (fn, { success, error = 'Action failed' } = {}) => {
      setBusy(true)
      try {
        const result = await fn()
        if (success) toast.success(success)
        return result
      } catch (e) {
        toast.error(error, e.message)
        throw e
      } finally {
        setBusy(false)
      }
    },
    [toast],
  )
  return [run, busy]
}
