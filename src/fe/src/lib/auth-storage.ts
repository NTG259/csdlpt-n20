import type { AuthResponse } from "@/types/domain"

const AUTH_SESSION_KEY = "sitemain_session"
const SESSION_DURATION_MS = 365 * 24 * 60 * 60 * 1000
const sessionListeners = new Set<() => void>()
let cachedSession: StoredSession | null | undefined

export type StoredUser = Omit<AuthResponse, "token" | "tokenType" | "expiresIn">

export interface StoredSession {
  token: string
  expiresAt: number
  user: StoredUser
}

function getStorage() {
  if (typeof window === "undefined") {
    return null
  }

  return window.localStorage
}

function emitSessionChange() {
  for (const listener of sessionListeners) {
    listener()
  }
}

function removeStoredSession() {
  const storage = getStorage()
  storage?.removeItem(AUTH_SESSION_KEY)
}

function writeStoredSession(session: StoredSession) {
  const storage = getStorage()
  storage?.setItem(AUTH_SESSION_KEY, JSON.stringify(session))
}

function isStoredUser(value: unknown): value is StoredUser {
  if (!value || typeof value !== "object") {
    return false
  }

  const user = value as Record<string, unknown>

  return (
    typeof user.userId === "string" &&
    typeof user.hoTen === "string" &&
    typeof user.email === "string" &&
    typeof user.maKhuVuc === "string" &&
    typeof user.vaiTro === "string"
  )
}

function isStoredSession(value: unknown): value is StoredSession {
  if (!value || typeof value !== "object") {
    return false
  }

  const session = value as Record<string, unknown>

  return (
    typeof session.token === "string" &&
    typeof session.expiresAt === "number" &&
    isStoredUser(session.user)
  )
}

function readStoredSession(): StoredSession | null {
  const storage = getStorage()

  if (!storage) {
    return null
  }

  const rawSession = storage.getItem(AUTH_SESSION_KEY)

  if (!rawSession) {
    return null
  }

  try {
    const parsed = JSON.parse(rawSession) as unknown

    if (!isStoredSession(parsed)) {
      removeStoredSession()
      return null
    }

    if (parsed.expiresAt < Date.now()) {
      const migratedSession = {
        ...parsed,
        expiresAt: Date.now() + SESSION_DURATION_MS,
      }
      writeStoredSession(migratedSession)
      return migratedSession
    }

    return parsed
  } catch {
    removeStoredSession()
    return null
  }
}

function refreshSessionFromStorage() {
  cachedSession = readStoredSession()
  emitSessionChange()
}

function resolveExpiresInMs(expiresIn: number) {
  // Backend hiện trả giây, một số doc/client cũ ghi mili-giây.
  return expiresIn < 1_000_000 ? expiresIn * 1000 : expiresIn
}

export function saveSession(auth: AuthResponse): StoredSession {
  const session: StoredSession = {
    token: auth.token,
    expiresAt:
      Date.now() + Math.max(SESSION_DURATION_MS, resolveExpiresInMs(auth.expiresIn)),
    user: {
      userId: auth.userId,
      hoTen: auth.hoTen,
      email: auth.email,
      maKhuVuc: auth.maKhuVuc,
      vaiTro: auth.vaiTro,
    },
  }

  writeStoredSession(session)
  cachedSession = session
  emitSessionChange()

  return session
}

export function getSession(): StoredSession | null {
  if (cachedSession === undefined) {
    cachedSession = readStoredSession()
  }

  return cachedSession
}

export function getSessionSnapshot() {
  return getSession()
}

export function clearSession() {
  removeStoredSession()
  cachedSession = null
  emitSessionChange()
}

export function subscribeToSessionChanges(listener: () => void) {
  sessionListeners.add(listener)

  if (typeof window === "undefined") {
    return () => {
      sessionListeners.delete(listener)
    }
  }

  const handleStorage = (event: StorageEvent) => {
    if (event.key === AUTH_SESSION_KEY) {
      refreshSessionFromStorage()
    }
  }

  window.addEventListener("storage", handleStorage)

  return () => {
    sessionListeners.delete(listener)
    window.removeEventListener("storage", handleStorage)
  }
}
