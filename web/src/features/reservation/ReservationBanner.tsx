import { useTranslation } from 'react-i18next'
import { useEffect } from 'react'
import { useActiveReservation } from './useActiveReservation'
import { useCountdown } from './useCountdown'

export default function ReservationBanner() {
  const { t } = useTranslation()
  const { active, clear } = useActiveReservation()
  const countdown = useCountdown(active?.expiresAtMs ?? null)

  // Dismiss automatically when countdown hits 0 (UI-SPEC: "no manual dismiss")
  useEffect(() => {
    if (countdown?.expired) {
      clear()
    }
  }, [countdown?.expired, clear])

  if (!active || !countdown || countdown.expired) return null

  const retailerLabel = active.merchantDomain ?? 'retailer'

  return (
    <div
      role="status"
      aria-live="polite"
      className="h-12 bg-primary text-primary-on flex items-center justify-between px-4 sticky top-12 z-10"
    >
      <span className="text-sm font-semibold">
        {t('reservation.banner_text', { itemName: active.itemName, minutes: countdown.minutes })}
      </span>
      <a
        href={active.affiliateUrl}
        target="_blank"
        rel="noopener noreferrer"
        className="text-sm font-semibold underline"
      >
        {t('reservation.banner_view', { retailer: retailerLabel })}
      </a>
    </div>
  )
}
