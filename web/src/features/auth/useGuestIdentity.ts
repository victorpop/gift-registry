import { useCallback, useEffect, useState } from 'react'

export const GUEST_IDENTITY_STORAGE_KEY = 'guestIdentity'

export interface GuestIdentity {
  firstName: string
  lastName: string
  email: string
}

function read(): GuestIdentity | null {
  try {
    const raw = localStorage.getItem(GUEST_IDENTITY_STORAGE_KEY)
    if (!raw) return null
    const parsed = JSON.parse(raw)
    // Shape-check: all three fields present and strings
    if (
      parsed &&
      typeof parsed === 'object' &&
      typeof parsed.firstName === 'string' &&
      typeof parsed.lastName === 'string' &&
      typeof parsed.email === 'string'
    ) {
      return { firstName: parsed.firstName, lastName: parsed.lastName, email: parsed.email }
    }
    return null
  } catch {
    return null
  }
}

export function useGuestIdentity() {
  const [identity, setIdentity] = useState<GuestIdentity | null>(() => read())

  // Sync across tabs (storage event fires when another tab writes)
  useEffect(() => {
    function onStorage(e: StorageEvent) {
      if (e.key === GUEST_IDENTITY_STORAGE_KEY) {
        setIdentity(read())
      }
    }
    window.addEventListener('storage', onStorage)
    return () => window.removeEventListener('storage', onStorage)
  }, [])

  const save = useCallback((next: GuestIdentity) => {
    localStorage.setItem(GUEST_IDENTITY_STORAGE_KEY, JSON.stringify(next))
    setIdentity(next)
  }, [])

  const clear = useCallback(() => {
    localStorage.removeItem(GUEST_IDENTITY_STORAGE_KEY)
    setIdentity(null)
  }, [])

  return { identity, save, clear }
}
