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
  timeout: 20000,
})

api.interceptors.request.use((config) => {
  const token = tokenStore.get()
  if (token) config.headers.Authorization = `Bearer ${token}`
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
  (response) => response.data?.data,
  async (error) => {
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
  if (error.code === 'ECONNABORTED') return 'The server took too long to respond. Please try again.'
  if (!error.response) return "Can't reach SkillSwap right now. Check your connection and try again."
  if (status >= 500) return 'Something went wrong on our side. Please try again.'
  return 'Something went wrong. Please try again.'
}

/** Absolute URL for files served by the backend (avatars). */
export function assetUrl(path) {
  if (!path) return null
  if (/^https?:\/\//.test(path)) return path
  const base = import.meta.env.VITE_BACKEND_URL_PUBLIC || ''
  return `${base}${path}`
}

export default api
