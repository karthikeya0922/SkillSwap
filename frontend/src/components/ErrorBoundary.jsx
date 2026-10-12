import { Component } from 'react'
import { CircleAlert, House, RotateCcw } from 'lucide-react'

/** Lazy-loaded pages fail this way when the app was redeployed while the tab was open. */
function isChunkLoadError(error) {
  return /Failed to fetch dynamically imported module|Importing a module script failed|error loading dynamically imported module/i
    .test(error?.message || '')
}

/**
 * Catches render errors below it and shows a recovery screen instead of a blank page. Pass `resetKey` (for example
 * the current path) so navigating elsewhere clears the error automatically.
 */
export default class ErrorBoundary extends Component {
  state = { error: null, resetKey: this.props.resetKey }

  static getDerivedStateFromError(error) {
    return { error }
  }

  static getDerivedStateFromProps(props, state) {
    if (props.resetKey !== state.resetKey) return { error: null, resetKey: props.resetKey }
    return null
  }

  componentDidCatch(error, info) {
    console.error('Unhandled UI error', error, info?.componentStack)
  }

  reset = () => {
    if (isChunkLoadError(this.state.error)) window.location.reload()
    else this.setState({ error: null })
  }

  render() {
    const { error } = this.state
    if (!error) return this.props.children
    const stale = isChunkLoadError(error)
    return (
      <div className={`grid place-items-center px-4 ${this.props.fullPage ? 'min-h-screen' : 'min-h-[60vh]'}`}>
        <div className="card flex max-w-md flex-col items-center px-6 py-10 text-center">
          <div className="mb-3 grid h-12 w-12 place-items-center rounded-full bg-rose-50 text-rose-600">
            <CircleAlert size={22} />
          </div>
          <p className="font-semibold text-slate-900">
            {stale ? 'A new version of SkillSwap is available' : 'Something went wrong on this page'}
          </p>
          <p className="mt-1 text-sm text-slate-500">
            {stale
              ? 'Reload to get the latest version. Nothing you saved has been lost.'
              : 'Your data is safe. Try again, or head back to your dashboard.'}
          </p>
          <div className="mt-5 flex gap-2">
            <button type="button" className="btn btn-primary btn-sm" onClick={this.reset}>
              <RotateCcw size={14} /> {stale ? 'Reload' : 'Try again'}
            </button>
            <a href="/" className="btn btn-secondary btn-sm">
              <House size={14} /> Home
            </a>
          </div>
        </div>
      </div>
    )
  }
}
