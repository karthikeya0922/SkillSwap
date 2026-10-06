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

api.interceptors.response.use(
  // Every successful response is wrapped in { success, data, message }; unwrap to the payload.
  (response) => response.data?.data,
  (error) => {
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
