"use client"

import {
  createContext,
  useContext,
  useEffect,
  useSyncExternalStore,
  type ReactNode,
} from "react"
import { useRouter } from "next/navigation"

import type { RegionCode } from "@/constants/regions"
import { setUnauthorizedHandler } from "@/lib/auth-events"
import {
  clearSession,
  getSessionSnapshot,
  saveSession,
  subscribeToSessionChanges,
  type StoredSession,
} from "@/lib/auth-storage"
import type { AuthResponse } from "@/types/domain"

import { getUserRegion } from "./region"

interface AuthContextValue {
  user: StoredSession["user"] | null
  token: string | null
  region: RegionCode | null
  isAuthenticated: boolean
  isAuthReady: boolean
  login: (auth: AuthResponse) => void
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

function subscribeToClientReady() {
  return () => {}
}

function getClientReadySnapshot() {
  return true
}

function getClientReadyServerSnapshot() {
  return false
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const router = useRouter()
  const isAuthReady = useSyncExternalStore(
    subscribeToClientReady,
    getClientReadySnapshot,
    getClientReadyServerSnapshot
  )
  const session = useSyncExternalStore<StoredSession | null>(
    subscribeToSessionChanges,
    getSessionSnapshot,
    () => null
  )

  useEffect(() => {
    setUnauthorizedHandler(() => {
      clearSession()
      router.replace("/login")
    })

    return () => {
      setUnauthorizedHandler(null)
    }
  }, [router])

  function login(auth: AuthResponse) {
    saveSession(auth)
  }

  function logout() {
    clearSession()
    router.replace("/login")
  }

  const value: AuthContextValue = {
    user: session?.user ?? null,
    token: session?.token ?? null,
    region: getUserRegion(session?.user.maKhuVuc),
    isAuthenticated: Boolean(session?.token),
    isAuthReady,
    login,
    logout,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)

  if (!context) {
    throw new Error("useAuth must be used inside <AuthProvider>")
  }

  return context
}
