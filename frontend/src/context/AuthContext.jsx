import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { setUnauthorizedHandler, tokenStore } from '../api/client'
import { authService } from '../services'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [token, setToken] = useState(() => tokenStore.get())
  const [initializing, setInitializing] = useState(Boolean(tokenStore.get()))

  const logout = useCallback(() => {
    authService.logout()
    tokenStore.clear()
    setToken(null)
    setUser(null)
  }, [])

  useEffect(() => {
    setUnauthorizedHandler(() => {
      tokenStore.clear()
      setToken(null)
      setUser(null)
    })
  }, [])

  // Restore the session from a stored token on first load.
  useEffect(() => {
    if (!token) {
      setInitializing(false)
      return
    }
    let cancelled = false
    authService
      .me()
      .then((me) => !cancelled && setUser(me))
      .catch(() => {
        if (!cancelled) {
          tokenStore.clear()
          setToken(null)
        }
      })
      .finally(() => !cancelled && setInitializing(false))
    return () => {
      cancelled = true
    }
    // Only on mount: later token changes come with a user object already.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const acceptAuth = useCallback((auth) => {
    tokenStore.set(auth.token)
    setToken(auth.token)
    setUser(auth.user)
    return auth.user
  }, [])

  const login = useCallback((credentials) => authService.login(credentials).then(acceptAuth), [acceptAuth])
  const register = useCallback((data) => authService.register(data).then(acceptAuth), [acceptAuth])
  const refreshUser = useCallback(() => authService.me().then((me) => (setUser(me), me)), [])
  const patchUser = useCallback((changes) => setUser((u) => (u ? { ...u, ...changes } : u)), [])

  const value = useMemo(
    () => ({
      user,
      token,
      isAuthenticated: Boolean(user),
      isAdmin: user?.role === 'ADMIN',
      initializing,
      login,
      register,
      logout,
      refreshUser,
      patchUser,
    }),
    [user, token, initializing, login, register, logout, refreshUser, patchUser],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)
