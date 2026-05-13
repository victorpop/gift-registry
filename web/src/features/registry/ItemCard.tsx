import { Link } from 'react-router'
import { useTranslation } from 'react-i18next'
import type { Item, ItemStatus } from '../../lib/firestore-mapping'
// Import atoms directly (not via barrel) to avoid pulling TopNav→useAuth→firebase
// into ItemCard's import graph — keeps ItemCard.test.tsx (jsdom, no firebase mock) green.
import { Pill } from '../../components/giftmaison/Pill'
import { PulseDot } from '../../components/giftmaison/PulseDot'
import { MonoCaption } from '../../components/giftmaison/MonoCaption'
import { useCountdown } from '../reservation/useCountdown'

interface Props {
  item: Item
  /**
   * Required: used to construct the per-item detail page Link href.
   * /registry/{registryId}/item/{item.id}
   */
  registryId: string
  /**
   * When provided for a reserved-by-me item, the reserved banner row becomes a button
   * that fires this callback (expected: smooth-scroll to ReserveDetailSection anchor or
   * navigate to the per-item detail page). When omitted, reserved banner renders as a
   * non-interactive div (unchanged behaviour for items reserved by someone else —
   * D-06: never reveal reserver identity).
   */
  onReservedByMeClick?: () => void
}

/**
 * Item card (k37 redesign — vertical stack: image / shop / truncated name / price).
 *
 * The entire tile is wrapped in a react-router <Link> to /registry/:id/item/:itemId
 * for every status (available, reserved, purchased). The detail page handles all
 * status-specific UI (Reserve CTA, reserved-by-other view-only, purchased view-only,
 * reserved-by-me detail with countdown/release/confirm).
 *
 * Container: bg-gm-paper, border-gm-line, rounded-gm-card (14 px), overflow-hidden.
 * Image: aspect-[4/3] mobile / aspect-[16/10] from sm:; objectFit cover; bg paperDeep
 * fallback for loading state. Purchased rows: opacity 0.55, image grayscale.
 *
 * Status pill top-left over image (12 px from edges).
 *
 * Body anatomy (vertical stack, 16 px padding, gap-2):
 *   - Shop line: MonoCaption faint micro = merchantDomain raw (uppercase via mono caps)
 *   - Title h3 (15 px Inter 500, -0.2 LS, ink) — truncated to 60 chars + "..." when longer
 *   - Price (font-semibold, currency rendered as small mono on the side)
 *   - Status-conditional in-card reserved banner (only when reserved-by-me)
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

/**
 * Truncates a title to `limit` characters (default 60), trimming whitespace
 * (leading/trailing) before measuring and trimming again at the cut point so
 * the rendered ellipsis never has whitespace immediately preceding it.
 */
export function truncateTitle(title: string, limit = 60): string {
  const trimmed = title.trim()
  if (trimmed.length <= limit) return trimmed
  return trimmed.slice(0, limit).trimEnd() + '...'
}

export default function ItemCard({ item, registryId, onReservedByMeClick }: Props) {
  const { t } = useTranslation()
  const isPurchased = item.status === 'purchased'
  const isReserved = item.status === 'reserved'

  // Minute-granularity countdown for in-card reserved banner (UI-SPEC: card banners
  // update every 60s; sticky banner updates every 1s).
  const countdown = useCountdown(item.expiresAt?.getTime() ?? null)
  const minutesLeft = countdown?.minutes ?? 0

  const titleDisplay = truncateTitle(item.title)
  const retailerText = item.merchantDomain ?? ''

  return (
    <Link
      to={`/registry/${registryId}/item/${item.id}`}
      aria-label={t('web_pill.tile_aria', { title: titleDisplay })}
      data-testid="item-card"
      data-status={item.status}
      className={[
        'flex flex-col rounded-gm-card overflow-hidden border border-gm-line bg-gm-paper no-underline text-gm-ink',
        'focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gm-accent',
        isPurchased ? 'opacity-[0.55]' : '',
      ].join(' ')}
    >
      {/* Image area — aspect 4:3 mobile / 16:10 desktop+ */}
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

      {/* Body — vertical stack: shop / name / price */}
      <div className="flex flex-col gap-2 p-4 flex-1">
        {retailerText && (
          <MonoCaption size="micro" tone="faint">{retailerText}</MonoCaption>
        )}
        <h3 className="font-body text-[15px] font-medium text-gm-ink leading-[1.25] tracking-[-0.2px] m-0">
          {titleDisplay}
        </h3>
        {item.price != null && (
          <span className="font-body text-[15px] text-gm-ink font-semibold" data-testid="price">
            {item.price}
            {item.currency && (
              <span className="ml-1 font-mono text-[11px] text-gm-inkFaint font-normal">
                {item.currency}
              </span>
            )}
          </span>
        )}

        {/* Reserved-by-me in-card banner. Renders as a button when onReservedByMeClick
            is provided so the viewer can jump to detail page; otherwise non-interactive
            div (D-06: same banner regardless of who reserved — no identity reveal). */}
        {isReserved && onReservedByMeClick ? (
          <button
            type="button"
            onClick={(e) => {
              // Prevent the surrounding <Link> from also navigating — the banner's
              // onReservedByMeClick callback handles navigation itself.
              e.preventDefault()
              e.stopPropagation()
              onReservedByMeClick()
            }}
            aria-label={t('web_pill.reserved_by_me_navigate_aria')}
            className="w-full text-left flex items-center gap-[10px] px-3 py-[9px] bg-gm-accentSoft rounded-lg focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gm-accent"
          >
            <PulseDot size={8} />
            <span className="font-mono text-[10.5px] text-gm-accent uppercase tracking-[0.4px] flex-1">
              {t('web_pill.reserved_banner', { minutes: minutesLeft })}
            </span>
          </button>
        ) : isReserved ? (
          <div className="flex items-center gap-[10px] px-3 py-[9px] bg-gm-accentSoft rounded-lg">
            <PulseDot size={8} />
            <span className="font-mono text-[10.5px] text-gm-accent uppercase tracking-[0.4px] flex-1">
              {t('web_pill.reserved_banner', { minutes: minutesLeft })}
            </span>
          </div>
        ) : null}
      </div>
    </Link>
  )
}
