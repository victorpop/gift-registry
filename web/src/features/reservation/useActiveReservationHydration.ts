import { useEffect, useRef, useState } from "react"
import { httpsCallable } from "firebase/functions"
import { functions } from "../../firebase"
import { useAuth } from "../auth/useAuth"
import { useGuestIdentity } from "../auth/useGuestIdentity"
import { useActiveReservation, type ActiveReservation } from "./useActiveReservation"

type HydrationStatus = "idle" | "loading" | "hydrated" | "empty" | "error"

interface HydrationResponse {
  active: ActiveReservation | null
}

/**
 * useActiveReservationHydration — effect hook that queries the server for an active
 * reservation and populates ActiveReservationContext on registry page mount.
 *
 * Fires when:
 *   - authReady === true (Firebase Auth resolved — avoids false-guest reads)
 *   - registryId is non-empty
 *   - user (signed-in) OR identity (guest with email in localStorage) is present
 *   - active === null (DOES NOT clobber a fresh in-session reservation set by
 *     useCreateReservation.onSuccess — constraint from PLAN X5D)
 *
 * Key change detection (lastKeyRef): when registryId, user.uid, or email changes,
 * the key changes and a fresh fetch fires. Prevents StrictMode double-fetch.
 *
 * Signed-in path: sends only registryId — backend uses auth.uid (defence in depth).
 * Guest path: sends registryId + giverEmail — backend enforces giverId==null match.
 *
 * Hydration is best-effort: a failure logs a warning but does not block page render.
 */
export function useActiveReservationHydration(registryId: string | undefined) {
  const { user, isReady: authReady } = useAuth()
  const { identity } = useGuestIdentity()
  const { active, set } = useActiveReservation()
  const [status, setStatus] = useState<HydrationStatus>("idle")
  const lastKeyRef = useRef<string | null>(null)

  useEffect(() => {
    if (!registryId) return
    if (!authReady) return
    // Don't clobber an in-session reservation set by useCreateReservation.onSuccess.
    if (active) return

    const effectiveEmail = user?.email ?? identity?.email ?? null

    // Truly anonymous with no stored guest identity → cannot hydrate.
    if (!user && !identity) return
    if (!effectiveEmail) return

    const key = `${registryId}|${user?.uid ?? "guest"}|${effectiveEmail}`
    if (lastKeyRef.current === key) return
    lastKeyRef.current = key

    setStatus("loading")

    const callable = httpsCallable<
      { registryId: string; giverEmail?: string },
      HydrationResponse
    >(functions, "hydrateActiveReservation")

    // Signed-in path sends ONLY registryId — backend uses auth.uid (defence in depth).
    // Guest path sends registryId + giverEmail.
    const payload = user
      ? { registryId }
      : { registryId, giverEmail: effectiveEmail }

    let cancelled = false

    callable(payload)
      .then((r) => {
        if (cancelled) return
        if (r.data?.active) {
          set(r.data.active)
          setStatus("hydrated")
        } else {
          setStatus("empty")
        }
      })
      .catch((err) => {
        if (cancelled) return
        // Hydration is best-effort: a failure must not block page render.
        // Banner stays hidden until the user reserves again.
        // eslint-disable-next-line no-console
        console.warn("[useActiveReservationHydration] failed", err)
        setStatus("error")
      })

    return () => {
      cancelled = true
    }
  }, [registryId, authReady, user, identity, active, set])

  return { status }
}
