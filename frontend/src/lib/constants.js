export const LEVELS = ['BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT']

export const LEVEL_META = {
  BEGINNER: { label: 'Beginner', className: 'bg-sky-50 text-sky-700', dots: 1 },
  INTERMEDIATE: { label: 'Intermediate', className: 'bg-emerald-50 text-emerald-700', dots: 2 },
  ADVANCED: { label: 'Advanced', className: 'bg-violet-50 text-violet-700', dots: 3 },
  EXPERT: { label: 'Expert', className: 'bg-amber-50 text-amber-700', dots: 4 },
}

export const AVAILABILITY = [
  { value: 'WEEKDAY_MORNING', label: 'Weekday mornings', short: 'Wkday AM' },
  { value: 'WEEKDAY_AFTERNOON', label: 'Weekday afternoons', short: 'Wkday PM' },
  { value: 'WEEKDAY_EVENING', label: 'Weekday evenings', short: 'Wkday Eve' },
  { value: 'WEEKEND_MORNING', label: 'Weekend mornings', short: 'Wkend AM' },
  { value: 'WEEKEND_AFTERNOON', label: 'Weekend afternoons', short: 'Wkend PM' },
  { value: 'WEEKEND_EVENING', label: 'Weekend evenings', short: 'Wkend Eve' },
]

export const SESSION_STATUS = {
  REQUESTED: { label: 'Requested', className: 'bg-amber-50 text-amber-700 ring-amber-200' },
  ACCEPTED: { label: 'Accepted', className: 'bg-sky-50 text-sky-700 ring-sky-200' },
  SCHEDULED: { label: 'Scheduled', className: 'bg-brand-50 text-brand-700 ring-brand-200' },
  ONGOING: { label: 'Live now', className: 'bg-emerald-50 text-emerald-700 ring-emerald-200' },
  COMPLETED: { label: 'Completed', className: 'bg-slate-100 text-slate-700 ring-slate-200' },
  REJECTED: { label: 'Declined', className: 'bg-rose-50 text-rose-700 ring-rose-200' },
  CANCELLED: { label: 'Cancelled', className: 'bg-rose-50 text-rose-700 ring-rose-200' },
}

export const REQUEST_STATUS = {
  OPEN: { label: 'Open', className: 'bg-emerald-50 text-emerald-700 ring-emerald-200' },
  PENDING: { label: 'Offers received', className: 'bg-amber-50 text-amber-700 ring-amber-200' },
  ACCEPTED: { label: 'Teacher chosen', className: 'bg-brand-50 text-brand-700 ring-brand-200' },
  COMPLETED: { label: 'Completed', className: 'bg-slate-100 text-slate-700 ring-slate-200' },
  CANCELLED: { label: 'Cancelled', className: 'bg-rose-50 text-rose-700 ring-rose-200' },
}

export const OFFER_STATUS = {
  PENDING: { label: 'Offer pending', className: 'bg-amber-50 text-amber-700 ring-amber-200' },
  ACCEPTED: { label: 'Offer accepted', className: 'bg-emerald-50 text-emerald-700 ring-emerald-200' },
  DECLINED: { label: 'Not selected', className: 'bg-slate-100 text-slate-600 ring-slate-200' },
  WITHDRAWN: { label: 'Withdrawn', className: 'bg-slate-100 text-slate-600 ring-slate-200' },
}

export const DIFFICULTY = {
  EASY: { label: 'Easy', className: 'bg-emerald-50 text-emerald-700 ring-emerald-200' },
  MEDIUM: { label: 'Medium', className: 'bg-amber-50 text-amber-700 ring-amber-200' },
  HARD: { label: 'Hard', className: 'bg-rose-50 text-rose-700 ring-rose-200' },
}

export const REPORT_REASONS = [
  { value: 'SPAM', label: 'Spam' },
  { value: 'HARASSMENT', label: 'Harassment' },
  { value: 'FAKE_PROFILE', label: 'Fake profile' },
  { value: 'INAPPROPRIATE_CONTENT', label: 'Inappropriate content' },
  { value: 'OTHER', label: 'Other' },
]

export const RANK_STYLE = {
  Beginner: 'bg-slate-100 text-slate-700',
  Contributor: 'bg-sky-50 text-sky-700',
  Mentor: 'bg-violet-50 text-violet-700',
  Expert: 'bg-amber-50 text-amber-800',
  'Community Master': 'bg-gradient-to-r from-amber-100 to-rose-100 text-rose-800',
}
