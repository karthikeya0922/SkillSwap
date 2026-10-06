/** Parses backend LocalDateTime ("2026-10-06T18:00:00") or LocalDate ("2026-10-06") as local time. */
export function parseDate(value) {
  if (!value) return null
  if (value instanceof Date) return value
  if (/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    const [y, m, d] = value.split('-').map(Number)
    return new Date(y, m - 1, d)
  }
  return new Date(value)
}

const dateFmt = new Intl.DateTimeFormat('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
const shortDateFmt = new Intl.DateTimeFormat('en-IN', { weekday: 'short', day: 'numeric', month: 'short' })
const timeFmt = new Intl.DateTimeFormat('en-IN', { hour: 'numeric', minute: '2-digit', hour12: true })

export const formatDate = (v) => (v ? dateFmt.format(parseDate(v)) : '')
export const formatShortDate = (v) => (v ? shortDateFmt.format(parseDate(v)) : '')
export const formatTime = (v) => (v ? timeFmt.format(parseDate(v)).toUpperCase() : '')
export const formatDateTime = (v) => (v ? `${formatShortDate(v)} · ${formatTime(v)}` : '')

export function timeAgo(value) {
  const date = parseDate(value)
  if (!date) return ''
  const seconds = Math.round((Date.now() - date.getTime()) / 1000)
  const future = seconds < 0
  const abs = Math.abs(seconds)
  const units = [
    ['year', 31536000],
    ['month', 2592000],
    ['week', 604800],
    ['day', 86400],
    ['hour', 3600],
    ['minute', 60],
  ]
  if (abs < 45) return future ? 'in a moment' : 'just now'
  for (const [unit, size] of units) {
    if (abs >= size) {
      const n = Math.floor(abs / size)
      const label = `${n} ${unit}${n > 1 ? 's' : ''}`
      return future ? `in ${label}` : `${label} ago`
    }
  }
  return ''
}

export function formatCredits(value, { sign = false } = {}) {
  const n = Number(value || 0)
  const text = Number.isInteger(n) ? String(n) : n.toFixed(2).replace(/0$/, '')
  return sign && n > 0 ? `+${text}` : text
}

export function formatDuration(minutes) {
  if (!minutes) return ''
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  if (!h) return `${m} min`
  return m ? `${h} hr ${m} min` : `${h} hr${h > 1 ? 's' : ''}`
}

export function greeting(date = new Date()) {
  const h = date.getHours()
  if (h < 12) return 'Good morning'
  if (h < 17) return 'Good afternoon'
  return 'Good evening'
}

export const titleCase = (s) =>
  (s || '')
    .toLowerCase()
    .split('_')
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(' ')

export const firstName = (name) => (name || '').trim().split(/\s+/)[0]

export function initials(name) {
  const parts = (name || '?').trim().split(/\s+/)
  return ((parts[0]?.[0] || '') + (parts.length > 1 ? parts[parts.length - 1][0] : '')).toUpperCase()
}

/** yyyy-mm-dd for <input type="date"> in local time. */
export function toDateInput(date) {
  const d = date instanceof Date ? date : parseDate(date)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}
