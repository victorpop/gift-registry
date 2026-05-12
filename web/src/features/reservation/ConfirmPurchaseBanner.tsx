import { useEffect, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { useConfirmPurchase } from './useConfirmPurchase'
import { useActiveReservation } from './useActiveReservation'
import { useToast } from '../../components/ToastProvider'
import { Btn } from '../../components/giftmaison'

export interface ConfirmPurchaseBannerProps {
  reservationId: string
  /** Optional minutes-left context for the body copy. Defaults to 30 if absent. */
  minutesLeft?: number
}

/**
 * Confirm-back card (UI-SPEC In-page reserve-detail section, item 4) — the
 * accentSoft surface with Display S headline + Body S body + accent CTA.
 *
 * Surface: bg-gm-accentSoft, border 1 px rgba(accent, 0.30), rounded-gm-card,
 * padding 20/22 px. Layout: copy block flex-1 + Btn accent right-aligned (mobile
 * stacks them).
 *
 * Behavioural contract preserved from Phase 5:
 *   - useConfirmPurchase().confirm(reservationId) on tap
 *   - aria-busy on the button while pending
 *   - success/error toasts via useToast (one-shot — successToastedRef + errorToastedForRef)
 *   - clears active reservation context via useActiveReservation().clear() on success (one-shot via successToastedRef)
 *   - role="status" + aria-live="polite" wrapper for screen readers
 */
export function ConfirmPurchaseBanner({ reservationId, minutesLeft = 30 }: ConfirmPurchaseBannerProps) {
  const { t } = useTranslation()
  const { confirm, status, error } = useConfirmPurchase()
  const { clear } = useActiveReservation()
  const { showToast } = useToast()

  const successToastedRef = useRef(false)
  const errorToastedForRef = useRef<string | null>(null)

  useEffect(() => {
    if (status === 'success' && !successToastedRef.current) {
      successToastedRef.current = true
      showToast(t('reservation.confirm_purchase_success'), 'success')
      clear()
    }
  }, [status, showToast, t, clear])

  useEffect(() => {
    if (status === 'error' && error && errorToastedForRef.current !== error) {
      errorToastedForRef.current = error
      showToast(t('reservation.confirm_purchase_error'), 'error')
    }
  }, [status, error, showToast, t])

  const isPending = status === 'pending'

  return (
    <div
      role="status"
      aria-live="polite"
      className="flex flex-col sm:flex-row sm:items-center gap-[18px] p-5 sm:p-[20px_22px] bg-gm-accentSoft rounded-gm-card border border-[rgba(200,98,58,0.30)]"
    >
      <div className="flex-1">
        <h3 className="m-0 font-display text-[20px] sm:text-[22px] text-gm-ink leading-[1.1] tracking-[-0.3px] font-normal">
          {t('web_reserve.confirm_heading')}
        </h3>
        <p className="m-0 mt-1 font-body text-[13.5px] text-gm-inkSoft leading-[1.45]">
          {t('web_reserve.confirm_body', { minutes: minutesLeft })}
        </p>
      </div>
      <Btn
        variant="accent"
        size="md"
        onClick={() => { void confirm(reservationId) }}
        disabled={isPending}
        aria-busy={isPending}
      >
        {isPending
          ? t('reservation.confirm_purchase_loading')
          : t('reservation.confirm_purchase_cta')}
      </Btn>
    </div>
  )
}
