import api from '../api/client'

/*
 * One small service object per API area. Every call resolves to the unwrapped `data` payload
 * (see api/client.js) or rejects with an Error carrying a friendly `message`.
 */

export const authService = {
  register: (data) => api.post('/auth/register', data),
  login: (data) => api.post('/auth/login', data),
  me: () => api.get('/auth/me'),
  logout: () => api.post('/auth/logout').catch(() => null),
}

export const userService = {
  myProfile: () => api.get('/users/profile'),
  updateProfile: (data) => api.put('/users/profile', data),
  uploadAvatar: (file) => {
    const form = new FormData()
    form.append('file', file)
    return api.post('/users/profile/avatar', form)
  },
  removeAvatar: () => api.delete('/users/profile/avatar'),
  getProfile: (id) => api.get('/users/' + id),
  discover: (params) => api.get('/users/discover', { params }),
  discoverMeta: () => api.get('/users/discover/meta'),
  dashboard: () => api.get('/dashboard'),
  leaderboard: (limit = 10) => api.get('/reputation/leaderboard', { params: { limit } }),
  badgeCatalog: () => api.get('/reputation/badges'),
}

export const skillService = {
  categories: () => api.get('/categories'),
  skills: (params) => api.get('/skills', { params }),
  mySkills: () => api.get('/user-skills'),
  addSkill: (data) => api.post('/user-skills', data),
  updateSkill: (id, data) => api.put('/user-skills/' + id, data),
  removeSkill: (id) => api.delete('/user-skills/' + id),
}

export const matchService = {
  list: (params) => api.get('/matches', { params }),
  recommended: (limit = 5) => api.get('/matches/recommended', { params: { limit } }),
  withUser: (userId) => api.get('/matches/' + userId),
}

export const connectionService = {
  overview: () => api.get('/connections'),
  send: (userId, message) => api.post('/connections', { userId, message }),
  respond: (id, action) => api.put('/connections/' + id, { action }),
  remove: (id) => api.delete('/connections/' + id),
}

export const requestService = {
  browse: (params) => api.get('/requests', { params: { scope: 'browse', ...params } }),
  mine: (params) => api.get('/requests', { params: { scope: 'mine', ...params } }),
  myOffers: (params) => api.get('/requests/offers/mine', { params }),
  get: (id) => api.get('/requests/' + id),
  create: (data) => api.post('/requests', data),
  update: (id, data) => api.put('/requests/' + id, data),
  cancel: (id) => api.post('/requests/' + id + '/cancel'),
  complete: (id) => api.post('/requests/' + id + '/complete'),
  offer: (id, message) => api.post('/requests/' + id + '/offers', { message }),
  withdrawOffer: (id) => api.delete('/requests/' + id + '/offers/mine'),
  acceptOffer: (id, offerId) => api.post('/requests/' + id + '/offers/' + offerId + '/accept'),
}

export const sessionService = {
  list: (params) => api.get('/sessions', { params }),
  get: (id) => api.get('/sessions/' + id),
  book: (data) => api.post('/sessions', data),
  accept: (id, details) => api.post('/sessions/' + id + '/accept', details || {}),
  reject: (id, reason) => api.post('/sessions/' + id + '/reject', { reason }),
  cancel: (id, reason) => api.post('/sessions/' + id + '/cancel', { reason }),
  complete: (id) => api.post('/sessions/' + id + '/complete'),
  updateDetails: (id, data) => api.put('/sessions/' + id, data),
}

export const walletService = {
  balance: () => api.get('/wallet/balance'),
  transactions: (params) => api.get('/wallet/transactions', { params }),
}

export const ratingService = {
  rate: (data) => api.post('/ratings', data),
  forUser: (userId, params) => api.get('/ratings/user/' + userId, { params }),
}

export const messageService = {
  conversations: () => api.get('/messages/conversations'),
  unreadCount: () => api.get('/messages/unread-count'),
  history: (userId, params) => api.get('/messages/' + userId, { params }),
  send: (userId, content) => api.post('/messages/' + userId, { content }),
  markRead: (userId) => api.put('/messages/' + userId + '/read'),
}

export const notificationService = {
  list: (params) => api.get('/notifications', { params }),
  unreadCount: () => api.get('/notifications/unread-count'),
  markRead: (id) => api.put('/notifications/' + id + '/read'),
  markAllRead: () => api.put('/notifications/read-all'),
  remove: (id) => api.delete('/notifications/' + id),
}

export const groupService = {
  list: (params) => api.get('/groups', { params }),
  invitations: () => api.get('/groups/invitations'),
  get: (id) => api.get('/groups/' + id),
  create: (data) => api.post('/groups', data),
  update: (id, data) => api.put('/groups/' + id, data),
  remove: (id) => api.delete('/groups/' + id),
  join: (id) => api.post('/groups/' + id + '/join'),
  leave: (id) => api.post('/groups/' + id + '/leave'),
  invite: (id, userId) => api.post('/groups/' + id + '/invitations', { userId }),
  declineInvitation: (id) => api.delete('/groups/' + id + '/invitations/mine'),
  removeMember: (id, userId) => api.delete('/groups/' + id + '/members/' + userId),
  posts: (id, params) => api.get('/groups/' + id + '/posts', { params }),
  addPost: (id, content) => api.post('/groups/' + id + '/posts', { content }),
  deletePost: (id, postId) => api.delete('/groups/' + id + '/posts/' + postId),
  addEvent: (id, data) => api.post('/groups/' + id + '/events', data),
  deleteEvent: (id, eventId) => api.delete('/groups/' + id + '/events/' + eventId),
}

export const challengeService = {
  list: (params) => api.get('/challenges', { params }),
  get: (id) => api.get('/challenges/' + id),
  create: (data) => api.post('/challenges', data),
  update: (id, data) => api.put('/challenges/' + id, data),
  remove: (id) => api.delete('/challenges/' + id),
  submissions: (id) => api.get('/challenges/' + id + '/submissions'),
  submit: (id, data) => api.post('/challenges/' + id + '/submissions', data),
  updateSubmission: (id, data) => api.put('/challenges/' + id + '/submissions/mine', data),
  reviews: (submissionId) => api.get('/challenges/submissions/' + submissionId + '/reviews'),
  review: (submissionId, data) => api.post('/challenges/submissions/' + submissionId + '/reviews', data),
}

export const reportService = {
  report: (data) => api.post('/reports', data),
}

export const adminService = {
  analytics: () => api.get('/admin/analytics'),
  users: (params) => api.get('/admin/users', { params }),
  suspend: (id, reason) => api.put('/admin/users/' + id + '/suspend', { reason }),
  reactivate: (id) => api.put('/admin/users/' + id + '/reactivate'),
  skills: (params) => api.get('/admin/skills', { params }),
  createSkill: (data) => api.post('/skills', data),
  updateSkill: (id, data) => api.put('/skills/' + id, data),
  deleteSkill: (id) => api.delete('/skills/' + id),
  createCategory: (data) => api.post('/categories', data),
  updateCategory: (id, data) => api.put('/categories/' + id, data),
  deleteCategory: (id) => api.delete('/categories/' + id),
  reports: (params) => api.get('/admin/reports', { params }),
  reviewReport: (id, data) => api.put('/admin/reports/' + id, data),
}
