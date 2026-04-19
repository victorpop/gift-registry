import { useEffect, useRef } from "react"
import { useTranslation } from "react-i18next"
import { useConfirmPurchase } from "./useConfirmPurchase"
import { useToast } from "../../components/ToastProvider"

export interface ConfirmPurchaseBannerProps {
  reservationId: string
}

/**
 * Phase 6 (UI-SPEC Contract 1 web): shown on RegistryPage for a giver who holds an
 * active reservation on this registry. Tapping the CTA invokes the `confirmPurchase`
 * callable (Plan 06-02). Success triggers a success toast; parent unmounts this banner
 * once useActiveReservation clears (Firestore snapshot flips status to "purchased").
 *
 * Typography contract: font-bold (700) and font-normal (400) only — no font-semibold.
 */
export function ConfirmPurchaseBanner({ reservationId }: ConfirmPurchaseBannerProps) {
  const { t } = useTranslation()
  const { confirm, status, error } = useConfirmPurchase()
  const { showToast } = useToast()

  const successToastedRef = useRef(false)
  const errorToastedForRef = useRef<string | null>(null)

  useEffect(() => {
    if (status === "success" && !successToastedRef.current) {
      successToastedRef.current = true
      showToast(t("reservation.confirm_purchase_success"), "success")
    }
  }, [status, showToast, t])

  useEffect(() => {
    if (status === "error" && error && errorToastedForRef.current !== error) {
      errorToastedForRef.current = error
      showToast(t("reservation.confirm_purchase_error"), "error")
    }
  }, [status, error, showToast, t])

  const isPending = status === "pending"

  return (
    <div
      role="status"
      aria-live="polite"
      className="flex flex-col gap-3 bg-surface-variant px-4 py-3 min-h-[64px]"
    >
      <div className="flex items-center gap-3">
        <svg
          aria-hidden="true"
          width="20"
          height="20"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
          className="text-primary"
        >
          <circle cx="9" cy="21" r="1" />
          <circle cx="20" cy="21" r="1" />
          <path d="M1 1h4l2.7 13.4a2 2 0 0 0 2 1.6h9.7a2 2 0 0 0 2-1.6L23 6H6" />
        </svg>
        <span className="text-sm font-bold text-surface-on">
          {t("reservation.confirm_purchase_heading")}
        </span>
      </div>

      <button
        type="button"
        onClick={() => { void confirm(reservationId) }}
        disabled={isPending}
        aria-busy={isPending}
        className={[
          "bg-primary text-primary-on rounded-md px-4 py-2 text-sm font-bold",
          "min-h-[48px]",
          "focus:ring-2 focus:ring-primary focus:ring-offset-2",
          "disabled:opacity-60 disabled:cursor-not-allowed",
        ].join(" ")}
      >
        {isPending
          ? t("reservation.confirm_purchase_loading")
          : t("reservation.confirm_purchase_cta")}
      </button>
    </div>
  )
}
