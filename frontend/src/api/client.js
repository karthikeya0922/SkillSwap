import axios from 'axios'

const TOKEN_KEY = 'skillswap.token'

export const tokenStore = {
  get() {
    try {
      return localStorage.getItem(TOKEN_KEY)
    } catch {
      return null
    }
  },
  set(token) {
    try {
      localStorage.setItem(TOKEN_KEY, token)
    } catch {
      /* storage unavailable */
    }
  },
  clear() {
    try {
      localStorage.removeItem(TOKEN_KEY)
    } catch {
      /* storage unavailable */
    }
  },
}

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  // Generous on purpose: a sleeping free-tier server can take about a minute to start.
  timeout: 90000,
})

// ---- "Server is starting" detection ----
// When any request has been pending for a few seconds we tell listeners (ServerWakeBanner), so a cold start
// shows a friendly notice instead of a frozen button.
const SLOW_AFTER_MS = 4000
const slowRequests = new Set()
const wakeListeners = new Set()
const notifyWake = () => wakeListeners.forEach((listener) => listener(slowRequests.size > 0))

/** Subscribe to "the server is slow to answer" changes. Returns an unsubscribe function. */
export function onServerWaking(listener) {
  wakeListeners.add(listener)
  return () => wakeListeners.delete(listener)
}

function trackSlow(config) {
  const id = Symbol('request')
  config.__slowId = id
  config.__slowTimer = setTimeout(() => {
    slowRequests.add(id)
    notifyWake()
  }, SLOW_AFTER_MS)
}

function untrackSlow(config) {
  if (!config) return
  clearTimeout(config.__slowTimer)
  if (slowRequests.delete(config.__slowId)) notifyWake()
}

api.interceptors.request.use((config) => {
  const token = tokenStore.get()
  if (token) config.headers.Authorization = `Bearer ${token}`
  trackSlow(config)
  return config
})

let onUnauthorized = null
/** AuthContext registers a handler so an expired token logs the user out everywhere. */
export function setUnauthorizedHandler(handler) {
  onUnauthorized = handler
}

// Reads are retried automatically when the network blips or the server is briefly unavailable (for example while a
// free-tier host wakes up). Writes are never retried, so a request can't create something twice.
const RETRYABLE_METHODS = ['get', 'head', 'options']
const RETRYABLE_STATUSES = [502, 503, 504]
const MAX_RETRIES = 2
const RETRY_BASE_DELAY_MS = 600

function shouldRetry(error) {
  const config = error.config
  if (!config || axios.isCancel(error) || config.retry === false) return false
  if (!RETRYABLE_METHODS.includes((config.method || 'get').toLowerCase())) return false
  if ((config.__retryCount || 0) >= MAX_RETRIES) return false
  if (!error.response) return error.code !== 'ECONNABORTED' // network error, but not our own timeout
  return RETRYABLE_STATUSES.includes(error.response.status)
}

const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

api.interceptors.response.use(
  // Every successful response is wrapped in { success, data, message }; unwrap to the payload.
  (response) => {
    untrackSlow(response.config)
    return response.data?.data
  },
  async (error) => {
    untrackSlow(error.config)
    if (shouldRetry(error)) {
      const config = error.config
      config.__retryCount = (config.__retryCount || 0) + 1
      // Exponential backoff with a little jitter: ~600 ms, then ~1.2 s.
      await wait(RETRY_BASE_DELAY_MS * 2 ** (config.__retryCount - 1) + Math.random() * 200)
      return api(config)
    }
    const status = error.response?.status
    const body = error.response?.data
    if (status === 401 && tokenStore.get() && onUnauthorized) onUnauthorized()
    const normalized = new Error(friendlyMessage(status, body, error))
    normalized.status = status
    normalized.fieldErrors = body?.errors || null
    return Promise.reject(normalized)
  },
)

function friendlyMessage(status, body, error) {
  if (body?.message) return body.message
  if (error.code === 'ECONNABORTED') return 'The server is still starting up. Please wait a few seconds and try again.'
  if (!error.response) return "Can't reach SkillSwap right now. Check your connection and try again."
  if (status >= 500) return 'Something went wrong on our side. Please try again.'
  return 'Something went wrong. Please try again.'
}

/**
 * Wakes a sleeping backend as soon as the site opens, so it is usually ready by the time someone has typed their
 * email and password. Uses a public, cached endpoint and ignores the result.
 */
export function wakeServer() {
  api.get('/categories').catch(() => {})
}

/** Absolute URL for files served by the backend (avatars). */
export function assetUrl(path) {
  if (!path) return null
  if (/^https?:\/\//.test(path)) return path
  const base = import.meta.env.VITE_BACKEND_URL_PUBLIC || ''
  return `${base}${path}`
}

export default api
