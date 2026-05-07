import { useTranslation } from 'react-i18next'
import { useActiveReservation } from './useActiveReservation'
import { useCountdown } from './useCountdown'
import { useItemsQuery } from '../registry/useItemsQuery'
import { ConfirmPurchaseBanner } from './ConfirmPurchaseBanner'
import HowTimerWorks from './HowTimerWorks'
import { Pill, MonoCaption } from '../../components/giftmaison'

export interface ReserveDetailSectionProps {
  /** Registry id from URL params — used to look up the active item from useItemsQuery. */
  registryId: string
}

/**
 * In-page reserve-detail section (CONTEXT D-04 + UI-SPEC).
 *
 * Renders BELOW the StickyReserveBanner and ABOVE RegistryHeader on
 * /registry/:id when useActiveReservation.active is non-null. Container is a
 * subdued bg-gm-paperDeep wrapper consistent with rest of page padding.
 *
 * Children (top to bottom):
 *   1. Mono caption "YOUR RESERVATION · STEP 2 OF 2"
 *   2. Display L italic-accent headline (split into pre/emphasis/post i18n keys)
 *   3. Reserved item card — paperDeep surface, 160 px square thumbnail (mobile:
 *      full-width 4:3), title + price + retailer + nested time-to-purchase
 *      progress bar (4 px, gm.line track, gm.accent fill, transition width 1s linear)
 *   4. Confirm-back card (delegated to <ConfirmPurchaseBanner>)
 *   5. "How the timer works" sidebar (delegated to <HowTimerWorks>) — desktop >= 1024 px
 *      grid-cols-[1fr_340px], mobile stacks below the confirm-back via collapse.
 */
export default function ReserveDetailSection({ registryId }: ReserveDetailSectionProps) {
  const { t } = useTranslation()
  const { active } = useActiveReservation()
  const itemsQ = useItemsQuery(registryId)
  const countdown = useCountdown(active?.expiresAtMs ?? null)

  if (!active || !countdown || countdown.expired) return null

  const item = itemsQ.data?.find(i => i.id === active.itemId) ?? null
  const retailer = active.merchantDomain ?? 'retailer'
  const minutesLeft = countdown.minutes
  const mm = String(countdown.minutes).padStart(2, '0')
  const ss = String(countdown.seconds).padStart(2, '0')
  const mmss = `${mm}:${ss}`
  // Progress bar: 30 minutes total, fill width = remaining / total.
  // Server set expiresAt = now + 30min on reserve. We approximate the elapsed
  // fraction from the countdown (no server "createdAt" exposed to the client).
  const totalSeconds = 30 * 60
  const remainingPct = Math.max(0, Math.min(100, (countdown.totalSeconds / totalSeconds) * 100))

  return (
    <section
      className="bg-gm-paperDeep border-b border-gm-line"
      data-testid="reserve-detail-section"
    >
      <div className="px-4 sm:px-7 lg:px-10 pt-8 sm:pt-9 lg:pt-9 pb-8 max-w-7xl mx-auto w-full">
        <MonoCaption size="micro" tone="faint">
          {t('web_reserve.detail_caption')}
        </MonoCaption>
        <h1 className="font-display text-[28px] sm:text-[38px] lg:text-[44px] text-gm-ink leading-[1.05] tracking-[-1px] mt-[10px] mb-7 max-w-[640px]">
          {t('web_reserve.detail_headline_pre')}
          <span className="italic text-gm-accent">{t('web_reserve.detail_headline_emphasis')}</span>
          {t('web_reserve.detail_headline_post')}
        </h1>

        <div className="grid grid-cols-1 lg:grid-cols-[1fr_340px] gap-8 items-start">
          {/* Left/main column */}
          <div className="flex flex-col gap-5">
            {/* Reserved item card */}
            <div className="flex flex-col sm:flex-row gap-5 p-5 bg-gm-paperDeep rounded-gm-card border border-gm-line">
              <div className="w-full aspect-[4/3] sm:w-[160px] sm:h-[160px] sm:flex-shrink-0 rounded-[10px] overflow-hidden bg-gm-line">
                {item?.imageUrl && (
                  <img src={item.imageUrl} alt={active.itemName} className="w-full h-full object-cover" />
                )}
              </div>
              <div className="flex-1 flex flex-col gap-[14px] justify-between min-w-0">
                <div>
                  <Pill tone="accent" size="sm">{t('web_reserve.item_pill')}</Pill>
                  <h2 className="m-0 mt-[10px] mb-1 font-body text-[20px] font-medium text-gm-ink leading-[1.2] tracking-[-0.3px]">
                    {active.itemName}
                  </h2>
                  {item && (item.price != null || item.merchantDomain) && (
                    <div className="font-body text-[14px] text-gm-inkSoft">
                      {item.price != null && (
                        <>
                          {item.price}{' '}
                          {item.currency && <span className="text-gm-inkFaint">{item.currency}</span>}
                          <span className="text-gm-inkFaint mx-2">·</span>
                        </>
                      )}
                      sold at <strong className="font-medium text-gm-ink">{retailer}</strong>
                    </div>
                  )}
                </div>
                {/* Time-to-purchase nested card */}
                <div className="p-3 px-[14px] bg-gm-paper rounded-lg border border-gm-line">
                  <div className="flex justify-between items-baseline mb-2">
                    <MonoCaption size="micro" tone="faint">{t('web_reserve.time_label')}</MonoCaption>
                    <span className="font-mono text-[12px] text-gm-accent font-medium" data-testid="reserve-detail-mmss">{mmss}</span>
                  </div>
                  <div className="h-[3px] bg-gm-line rounded-[2px] overflow-hidden">
                    <div className="h-full bg-gm-accent transition-[width] duration-1000 ease-linear" style={{ width: `${remainingPct}%` }} />
                  </div>
                </div>
              </div>
            </div>

            {/* Confirm-back card — re-styled component */}
            <ConfirmPurchaseBanner reservationId={active.reservationId} minutesLeft={minutesLeft} />
          </div>

          {/* Right/sidebar — desktop only via grid; mobile stacks below */}
          <HowTimerWorks retailer={retailer} />
        </div>
      </div>
    </section>
  )
}
