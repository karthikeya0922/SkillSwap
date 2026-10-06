import { useState } from 'react'
import { Navigate, Outlet, useLocation } from 'react-router-dom'
import Navbar from './Navbar'
import Sidebar from './Sidebar'
import { useAuth } from '../context/AuthContext'
import { FullPageSpinner } from './ui/Feedback'

/** Shell for every signed-in page: sidebar + top bar + routed content. */
export function AppLayout() {
  const [menuOpen, setMenuOpen] = useState(false)
  return (
    <div className="min-h-screen">
      <Sidebar open={menuOpen} onClose={() => setMenuOpen(false)} />
      <div className="lg:pl-64">
        <Navbar onMenu={() => setMenuOpen(true)} />
        <main className="mx-auto w-full max-w-7xl px-4 py-6 sm:px-6 sm:py-8 lg:px-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

/** Requires a signed-in user; admins are kept to admin pages and vice versa. */
export function ProtectedRoute({ admin = false, children }) {
  const { isAuthenticated, isAdmin, initializing } = useAuth()
  const location = useLocation()
  if (initializing) return <FullPageSpinner />
  if (!isAuthenticated) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  if (admin && !isAdmin) return <Navigate to="/dashboard" replace />
  if (!admin && isAdmin) return <Navigate to="/admin" replace />
  return children
}

/** Login/register pages bounce signed-in users to their home. */
export function GuestRoute({ children }) {
  const { isAuthenticated, isAdmin, initializing } = useAuth()
  if (initializing) return <FullPageSpinner />
  if (isAuthenticated) return <Navigate to={isAdmin ? '/admin' : '/dashboard'} replace />
  return children
}
