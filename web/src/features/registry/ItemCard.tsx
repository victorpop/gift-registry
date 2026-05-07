import { useTranslation } from 'react-i18next'
import type { Item, ItemStatus } from '../../lib/firestore-mapping'
import { Pill, PulseDot, MonoCaption } from '../../components/giftmaison'
import { useCountdown } from '../reservation/useCountdown'

interface Props {
  item: Item
  /**
   * Plan 05 injects the real ReserveButton via render-prop (existing Phase 5 contract).
   * When omitted, ItemCard renders a disabled placeholder for available items.
   */
  reserveSlot?: React.ReactNode
}

/**
 * Item card (UI-SPEC Screen 01 item-card anatomy + CONTEXT D-06 / D-10 / D-17).
 *
 * Container: bg-gm-paper, border-gm-line, rounded-gm-card (14 px), overflow-hidden.
 * Image: aspect-[4/3] mobile / aspect-[16/10] from sm:; objectFit cover; bg paperDeep
 * fallback for loading state. Purchased rows: opacity 0.55, image grayscale.
 *
 * Status pill top-left over image (12 px from edges).
 *
 * Body anatomy (vertical stack, 14/16 px padding):
 *   - Title (15 px Inter 500, -0.2 LS, ink)
 *   - Price + retailer row (Inter 500 / Mono 10 px, baseline aligned)
 *   - Status-conditional CTA / banner / nothing
 *
 * D-06 — NO RESERVER OR GIVER NAME EVER. Reserved card pill copy is "RESERVED"
 * (no name suffix). The in-card reserved banner shows
 * "{n} MIN LEFT — auto-releases if not purchased" — no name attribution line.
 * Purchased pill copy is "✓ PURCHASED" (no name attribution).
 */
function statusToPillTone(status: ItemStatus): 'neutral' | 'accent' | 'ok' {
  if (status === 'reserved') return 'accent'
  if (status === 'purchased') return 'ok'
  return 'neutral'
}

function statusPillKey(status: ItemStatus): string {
  if (status === 'reserved') return 'web_pill.reserved'
  if (status === 'purchased') return 'web_pill.purchased'
  return 'web_pill.available'
}

export default function ItemCard({ item, reserveSlot }: Props) {
  const { t } = useTranslation()
  const isPurchased = item.status === 'purchased'
  const isReserved = item.status === 'reserved'
  const isAvailable = item.status === 'available'

  // Minute-granularity countdown for in-card reserved banner (UI-SPEC: card banners
  // update every 60s; sticky banner — Plan 05 — updates every 1s).
  const countdown = useCountdown(item.expiresAt?.getTime() ?? null)
  const minutesLeft = countdown?.minutes ?? 0

  const priceText = item.price != null && item.currency
    ? `${item.price} ${item.currency}`
    : item.price != null ? String(item.price) : null

  const retailerText = item.merchantDomain ?? ''

  return (
    <article
      className={[
        'flex flex-col rounded-gm-card overflow-hidden border border-gm-line bg-gm-paper',
        isPurchased ? 'opacity-[0.55]' : '',
      ].join(' ')}
      data-testid="item-card"
      data-status={item.status}
    >
      {/* Image area — aspect 4:3 mobile / 16:10 desktop+ (D-10) */}
      <div className="relative aspect-[4/3] sm:aspect-[16/10] bg-gm-paperDeep overflow-hidden">
        {item.imageUrl ? (
          <img
            src={item.imageUrl}
            alt={item.title}
            className={`w-full h-full object-cover ${isPurchased ? 'grayscale' : ''}`}
            loading="lazy"
          />
        ) : (
          <div className="w-full h-full" aria-hidden="true" />
        )}
        {/* Status pill top-left */}
        <div className="absolute top-3 left-3">
          <Pill tone={statusToPillTone(item.status)} size="sm">
            {isReserved && <PulseDot size={8} className="mr-[2px]" />}
            {t(statusPillKey(item.status))}
          </Pill>
        </div>
      </div>

      {/* Body */}
      <div className="flex flex-col gap-3 p-4 flex-1">
        <div>
          <h3 className="font-body text-[15px] font-medium text-gm-ink leading-[1.25] tracking-[-0.2px] m-0">
            {item.title}
          </h3>
          {(priceText || retailerText) && (
            <div className="flex justify-between items-baseline mt-[6px]">
              {priceText && (
                <span className="font-body text-[14px] text-gm-ink font-medium" data-testid="price">
                  {item.price}
                  {item.currency && (
                    <span className="ml-1 font-mono text-[11px] text-gm-inkFaint">
                      {item.currency}
                    </span>
                  )}
                </span>
              )}
              {retailerText && (
                <MonoCaption size="micro" tone="faint">{retailerText}</MonoCaption>
              )}
            </div>
          )}
        </div>

        {/* Status-conditional region */}
        {isAvailable && (
          <div data-testid="reserve-slot" className="self-stretch">
            {reserveSlot ?? (
              <button
                type="button"
                disabled
                className="inline-flex items-center justify-center gap-2 w-full rounded-full border border-gm-ink bg-gm-ink text-gm-paper font-body text-[12px] font-medium tracking-[-0.1px] leading-none px-3 py-[7px] opacity-50 cursor-not-allowed"
              >
                {t('web_hero.reserve_cta')}
              </button>
            )}
          </div>
        )}

        {isReserved && (
          <div className="flex items-center gap-[10px] px-3 py-[9px] bg-gm-accentSoft rounded-lg">
            <PulseDot size={8} />
            <span className="font-mono text-[10.5px] text-gm-accent uppercase tracking-[0.4px] flex-1">
              {t('web_pill.reserved_banner', { minutes: minutesLeft })}
            </span>
          </div>
        )}

        {/* Purchased: no body CTA — image already shows opacity + grayscale.
            UI-SPEC ASCII contract says "(no body CTA; row is opacity 0.55, image
            grayscale, status pill bottom-left ✓ Purchased per D-16)".
            We keep the status pill at top-left (consistency across statuses); the
            opacity + grayscale carry the "purchased" signal sufficiently. */}
      </div>
    </article>
  )
}
