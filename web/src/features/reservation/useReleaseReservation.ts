import { useCallback, useState } from "react"
import { httpsCallable, type HttpsCallableResult } from "firebase/functions"
import { functions } from "../../firebase"

export type ReleaseStatus = "idle" | "pending" | "success" | "error"

export interface UseReleaseReservationResult {
  release: (reservationId: string, giverEmail?: string) => Promise<void>
  status: ReleaseStatus
  error: string | null
}

interface ReleaseResponse {
  success: boolean
}

/**
 * useReleaseReservation — wraps the `releaseReservationCallable` Firebase callable.
 *
 * Mirrors useConfirmPurchase shape exactly:
 *   - httpsCallable is created INSIDE release() so vitest mocks bind per-call.
 *   - Signed-in callers: pass reservationId only (backend uses auth.uid for ownership).
 *   - Guest callers: pass reservationId + giverEmail (backend enforces giverEmail + null giverId).
 *
 * Status lifecycle: idle → pending → success | error.
 * On success, caller is responsible for calling clear() to hide the banner.
 */
export function useReleaseReservation(): UseReleaseReservationResult {
  const [status, setStatus] = useState<ReleaseStatus>("idle")
  const [error, setError] = useState<string | null>(null)

  const release = useCallback(async (reservationId: string, giverEmail?: string) => {
    setStatus("pending")
    setError(null)
    try {
      const callable = httpsCallable<
        { reservationId: string; giverEmail?: string },
        ReleaseResponse
      >(functions, "releaseReservationCallable")

      const payload: { reservationId: string; giverEmail?: string } = giverEmail
        ? { reservationId, giverEmail }
        : { reservationId }

      const result: HttpsCallableResult<ReleaseResponse> = await callable(payload)

      if (result.data?.success !== true) {
        throw new Error("RELEASE_NO_SUCCESS")
      }
      setStatus("success")
    } catch (err) {
      setError((err as { message?: string }).message ?? "unknown")
      setStatus("error")
    }
  }, [])

  return { release, status, error }
}
