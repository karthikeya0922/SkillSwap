import { lazy, Suspense } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { AppLayout, GuestRoute, ProtectedRoute } from './components/AppLayout'
import { FullPageSpinner } from './components/ui/Feedback'
import Landing from './pages/Landing'
import { Login, Register } from './pages/Auth'
import NotFound from './pages/NotFound'

// Signed-in pages are code-split so the landing page stays light.
const Onboarding = lazy(() => import('./pages/Onboarding'))
const Dashboard = lazy(() => import('./pages/Dashboard'))
const Discover = lazy(() => import('./pages/Discover'))
const Matches = lazy(() => import('./pages/Matches'))
const Profile = lazy(() => import('./pages/Profile'))
const EditProfile = lazy(() => import('./pages/EditProfile'))
const Connections = lazy(() => import('./pages/Connections'))
const SkillRequests = lazy(() => import('./pages/SkillRequests'))
const RequestDetail = lazy(() => import('./pages/RequestDetail'))
const Sessions = lazy(() => import('./pages/Sessions'))
const BookSession = lazy(() => import('./pages/BookSession'))
const SessionDetail = lazy(() => import('./pages/SessionDetail'))
const Wallet = lazy(() => import('./pages/Wallet'))
const Messages = lazy(() => import('./pages/Messages'))
const Groups = lazy(() => import('./pages/Groups'))
const GroupDetail = lazy(() => import('./pages/GroupDetail'))
const Challenges = lazy(() => import('./pages/Challenges'))
const ChallengeDetail = lazy(() => import('./pages/ChallengeDetail'))
const Notifications = lazy(() => import('./pages/Notifications'))
const Leaderboard = lazy(() => import('./pages/Leaderboard'))
const AdminDashboard = lazy(() => import('./pages/admin/AdminDashboard'))
const AdminUsers = lazy(() => import('./pages/admin/AdminUsers'))
const AdminSkills = lazy(() => import('./pages/admin/AdminSkills'))
const AdminReports = lazy(() => import('./pages/admin/AdminReports'))

export default function App() {
  return (
    <Suspense fallback={<FullPageSpinner />}>
      <Routes>
        <Route path="/" element={<Landing />} />
        <Route path="/login" element={<GuestRoute><Login /></GuestRoute>} />
        <Route path="/register" element={<GuestRoute><Register /></GuestRoute>} />
        <Route path="/onboarding" element={<ProtectedRoute><Onboarding /></ProtectedRoute>} />

        <Route element={<ProtectedRoute><AppLayout /></ProtectedRoute>}>
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/discover" element={<Discover />} />
          <Route path="/matches" element={<Matches />} />
          <Route path="/profile" element={<Profile self />} />
          <Route path="/profile/edit" element={<EditProfile />} />
          <Route path="/users/:id" element={<Profile />} />
          <Route path="/connections" element={<Connections />} />
          <Route path="/requests" element={<SkillRequests />} />
          <Route path="/requests/:id" element={<RequestDetail />} />
          <Route path="/sessions" element={<Sessions />} />
          <Route path="/sessions/new" element={<BookSession />} />
          <Route path="/sessions/:id" element={<SessionDetail />} />
          <Route path="/wallet" element={<Wallet />} />
          <Route path="/messages" element={<Messages />} />
          <Route path="/messages/:userId" element={<Messages />} />
          <Route path="/groups" element={<Groups />} />
          <Route path="/groups/:id" element={<GroupDetail />} />
          <Route path="/challenges" element={<Challenges />} />
          <Route path="/challenges/:id" element={<ChallengeDetail />} />
          <Route path="/notifications" element={<Notifications />} />
          <Route path="/leaderboard" element={<Leaderboard />} />
        </Route>

        <Route element={<ProtectedRoute admin><AppLayout /></ProtectedRoute>}>
          <Route path="/admin" element={<AdminDashboard />} />
          <Route path="/admin/users" element={<AdminUsers />} />
          <Route path="/admin/skills" element={<AdminSkills />} />
          <Route path="/admin/reports" element={<AdminReports />} />
        </Route>

        <Route path="/home" element={<Navigate to="/dashboard" replace />} />
        <Route path="*" element={<NotFound />} />
      </Routes>
    </Suspense>
  )
}
