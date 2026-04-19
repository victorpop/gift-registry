import { createContext, useCallback, useContext, useMemo, useState, type ReactNode, createElement } from 'react'

export interface ActiveReservation {
  reservationId: string
  itemId: string
  itemName: string
  affiliateUrl: string
  merchantDomain: string | null
  expiresAtMs: number
}

interface Ctx {
  active: ActiveReservation | null
  set: (r: ActiveReservation | null) => void
  clear: () => void
}

const ActiveReservationContext = createContext<Ctx | null>(null)

export function ActiveReservationProvider({ children }: { children: ReactNode }) {
  const [active, setActive] = useState<ActiveReservation | null>(null)
  const set = useCallback((r: ActiveReservation | null) => setActive(r), [])
  const clear = useCallback(() => setActive(null), [])
  const value = useMemo(() => ({ active, set, clear }), [active, set, clear])
  return createElement(ActiveReservationContext.Provider, { value }, children)
}

export function useActiveReservation(): Ctx {
  const ctx = useContext(ActiveReservationContext)
  if (!ctx) {
    throw new Error('useActiveReservation must be used within ActiveReservationProvider')
  }
  return ctx
}
