import { useCallback, useState } from "react"
import { httpsCallable, type HttpsCallableResult } from "firebase/functions"
import { functions } from "../../firebase"

export type ConfirmPurchaseStatus = "idle" | "pending" | "success" | "error"

export interface UseConfirmPurchaseResult {
  confirm: (reservationId: string) => Promise<void>
  status: ConfirmPurchaseStatus
  error: string | null
}

interface ConfirmPurchaseResponse {
  success: boolean
}

/**
 * Phase 6 (NOTF-01/NOTF-02): wraps the `confirmPurchase` Firebase callable.
 * Mirrors useResolveReservation — httpsCallable is created INSIDE confirm() so vitest
 * mocks of firebase/functions bind per-call.
 *
 * Errors: the server throws HttpsError("failed-precondition", "RESERVATION_EXPIRED")
 * when the reservation is no longer active. The hook surfaces err.message unchanged
 * so callers can toast a localized string based on the message.
 */
export function useConfirmPurchase(): UseConfirmPurchaseResult {
  const [status, setStatus] = useState<ConfirmPurchaseStatus>("idle")
  const [error, setError] = useState<string | null>(null)

  const confirm = useCallback(async (reservationId: string) => {
    setStatus("pending")
    setError(null)
    try {
      const callable = httpsCallable<{ reservationId: string }, ConfirmPurchaseResponse>(
        functions,
        "confirmPurchase",
      )
      const result: HttpsCallableResult<ConfirmPurchaseResponse> = await callable({ reservationId })
      if (result.data?.success !== true) {
        throw new Error("CONFIRM_PURCHASE_NO_SUCCESS")
      }
      setStatus("success")
    } catch (err) {
      const message = (err as { message?: string }).message ?? "unknown"
      setError(message)
      setStatus("error")
    }
  }, [])

  return { confirm, status, error }
}
