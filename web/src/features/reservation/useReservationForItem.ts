import { useEffect, useRef, useState } from "react"
import { httpsCallable } from "firebase/functions"
import { functions } from "../../firebase"
import { useAuth } from "../auth/useAuth"
import { useGuestIdentity } from "../auth/useGuestIdentity"
import type { ActiveReservation } from "./useActiveReservation"

type HydrationStatus = "idle" | "loading" | "hydrated" | "empty" | "error"

interface HydrationResponse {
  active: ActiveReservation | null
}

/**
 * useReservationForItem — fetches the caller's active reservation for a specific item.
 *
 * Used by ItemReservePage to resolve the per-item reservation independently of the
 * shared useActiveReservation context (which only holds the most-recent reservation).
 * This hook owns its own local state and does NOT write to the shared context.
 *
 * Gating mirrors useActiveReservationHydration:
 *   - authReady must be true (avoids false-guest reads on initial load)
 *   - registryId and itemId must be non-empty
 *   - user (signed-in) OR identity (guest with email in localStorage) must be present
 *
 * Key-based StrictMode guard: prevents double-fetch in React 18 StrictMode.
 * Key: `${registryId}|${itemId}|${uid|'guest'}|${effectiveEmail}`.
 *
 * Signed-in path: sends only { registryId, itemId } — backend uses auth.uid (defence in depth).
 * Guest path: sends { registryId, itemId, giverEmail } — backend enforces giverId==null match.
 *
 * Best-effort: a failure logs a warning but does not throw.
 */
export function useReservationForItem(
  registryId: string | undefined,
  itemId: string | undefined,
): { status: HydrationStatus; active: ActiveReservation | null } {
  const { user, isReady: authReady } = useAuth()
  const { identity } = useGuestIdentity()
  const [status, setStatus] = useState<HydrationStatus>("idle")
  const [active, setActive] = useState<ActiveReservation | null>(null)
  const lastKeyRef = useRef<string | null>(null)

  useEffect(() => {
    if (!registryId) return
    if (!itemId) return
    if (!authReady) return

    const effectiveEmail = user?.email ?? identity?.email ?? null

    // Truly anonymous with no stored guest identity → cannot hydrate.
    if (!user && !identity) return
    if (!effectiveEmail) return

    const key = `${registryId}|${itemId}|${user?.uid ?? "guest"}|${effectiveEmail}`
    if (lastKeyRef.current === key) return
    lastKeyRef.current = key

    setStatus("loading")

    const callable = httpsCallable<
      { registryId: string; itemId: string; giverEmail?: string },
      HydrationResponse
    >(functions, "getReservationForItem")

    // Signed-in path sends ONLY registryId + itemId — backend uses auth.uid (defence in depth).
    // Guest path sends registryId + itemId + giverEmail.
    const payload = user
      ? { registryId, itemId }
      : { registryId, itemId, giverEmail: effectiveEmail }

    let cancelled = false

    callable(payload)
      .then((r) => {
        if (cancelled) return
        if (r.data?.active) {
          setActive(r.data.active)
          setStatus("hydrated")
        } else {
          setActive(null)
          setStatus("empty")
        }
      })
      .catch((err) => {
        if (cancelled) return
        // Best-effort: a failure must not block page render.
        // eslint-disable-next-line no-console
        console.warn("[useReservationForItem] failed", err)
        setActive(null)
        setStatus("error")
      })

    return () => {
      cancelled = true
    }
  }, [registryId, itemId, authReady, user, identity])

  return { status, active }
}
