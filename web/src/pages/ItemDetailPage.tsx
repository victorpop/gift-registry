import { useEffect, useRef } from 'react'
import { useParams, Link } from 'react-router'
import { useTranslation } from 'react-i18next'
import { useItemsQuery } from '../features/registry/useItemsQuery'
import { useActiveReservation } from '../features/reservation/useActiveReservation'
import { useConfirmPurchase } from '../features/reservation/useConfirmPurchase'
import { useToast } from '../components/ToastProvider'
import ReserveButton from '../features/reservation/ReserveButton'
import NotFoundPage from './NotFoundPage'
// Import atoms directly (not via barrel) to avoid pulling TopNav→useAuth→firebase
// into the import graph unnecessarily — but TopNav/Footer are chrome so they're fine here.
import { TopNav, Footer, Pill, MonoCaption, Btn } from '../components/giftmaison'
import type { Item, ItemStatus } from '../lib/firestore-mapping'

/**
 * Per-item detail page — /registry/:id/item/:itemId
 *
 * Deep-linkable and refresh-survivable. Reuses useItemsQuery (same react-query cache
 * key as RegistryPage) so live Firestore updates flow through automatically.
 *
 * CTA surface:
 *   - available   → <ReserveButton> (same auth/guest gating as RegistryPage)
 *   - reserved, ours  → "Mark as purchased" CTA (gated on useActiveReservation.active)
 *   - reserved, theirs → read-only banner (D-06: no reserver name ever)
 *   - purchased   → read-only banner, no CTAs (other than Go-to-retailer + Back)
 *
 * "Go to retailer" always uses item.affiliateUrl (NOT originalUrl) — EMAG monetization.
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

// Btn primary lg classes inlined for use on a sibling <a> (per Btn JSDoc pattern).
const BTN_PRIMARY_LG_CLASSES =
  'inline-flex items-center justify-center gap-2 rounded-full border font-body font-medium tracking-[-0.1px] leading-none cursor-pointer focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gm-accent bg-gm-ink text-gm-paper border-gm-ink px-[22px] py-[14px] text-[15px]'

export default function ItemDetailPage() {
  const { id: registryId, itemId } = useParams<{ id: string; itemId: string }>()
  const { t } = useTranslation()
  const itemsQ = useItemsQuery(registryId)
  const { active } = useActiveReservation()
  const { confirm, status, error } = useConfirmPurchase()
  const { showToast } = useToast()

  // Toast side-effects for mark-as-purchased (mirror ConfirmPurchaseBanner pattern).
  const successToastedRef = useRef(false)
  const errorToastedForRef = useRef<string | null>(null)

  useEffect(() => {
    if (status === 'success' && !successToastedRef.current) {
      successToastedRef.current = true
      showToast(t('reservation.confirm_purchase_success'), 'success')
    }
  }, [status, showToast, t])

  useEffect(() => {
    if (status === 'error' && error && errorToastedForRef.current !== error) {
      errorToastedForRef.current = error
      showToast(t('reservation.confirm_purchase_error'), 'error')
    }
  }, [status, error, showToast, t])

  const isPending = status === 'pending'

  // Loading: data not yet populated by Firestore onSnapshot.
  if (itemsQ.data === undefined) {
    return (
      <div className="min-h-screen flex flex-col bg-gm-paper">
        <TopNav />
        <main className="flex-1 flex items-center justify-center" role="status" aria-label={t('web_item_detail.loading')}>
          <div className="w-16 h-16 rounded-full bg-gm-paperDeep animate-pulse" aria-hidden="true" />
        </main>
        <Footer />
      </div>
    )
  }

  // Not-found: data loaded but item absent (deleted or invalid id).
  const item: Item | undefined = itemsQ.data.find(i => i.id === itemId)
  if (!item) {
    return <NotFoundPage />
  }

  const isOwnReservation = item.status === 'reserved' && active?.itemId === item.id

  const priceText = item.price != null && item.currency
    ? `${item.price} ${item.currency}`
    : item.price != null ? String(item.price) : null

  return (
    <div className="min-h-screen flex flex-col bg-gm-paper">
      <TopNav />

      <main className="flex-1">
        <div className="max-w-3xl mx-auto px-4 sm:px-7 lg:px-10 py-8">
          {/* Caption + back link */}
          <div className="flex items-center justify-between mb-6">
            <MonoCaption size="micro" tone="faint">{t('web_item_detail.page_caption')}</MonoCaption>
            <Link
              to={`/registry/${registryId}`}
              className="inline-flex items-center gap-1 font-body text-[13.5px] font-medium text-gm-ink border border-gm-line rounded-full px-[18px] py-[11px] leading-none tracking-[-0.1px] focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gm-accent hover:bg-gm-paperDeep transition-colors"
            >
              {t('web_item_detail.back_to_registry')}
            </Link>
          </div>

          {/* Hero image */}
          <div className="aspect-[16/10] rounded-gm-card overflow-hidden bg-gm-paperDeep mb-6">
            {item.imageUrl ? (
              <img
                src={item.imageUrl}
                alt={item.title}
                className="w-full h-full object-cover"
              />
            ) : (
              <div className="w-full h-full" aria-hidden="true" />
            )}
          </div>

          {/* Status pill */}
          <div className="mb-3">
            <Pill tone={statusToPillTone(item.status)} size="sm">
              {t(statusPillKey(item.status))}
            </Pill>
          </div>

          {/* Title */}
          <h1 className="font-display text-[28px] sm:text-[36px] lg:text-[44px] text-gm-ink leading-[1.05] tracking-[-1px] m-0 mb-4">
            {item.title}
          </h1>

          {/* Price + currency + merchant */}
          {(priceText || item.merchantDomain) && (
            <div className="flex justify-between items-baseline mb-6">
              {priceText && (
                <span className="font-body text-[14px] text-gm-ink font-medium">
                  {item.price}
                  {item.currency && (
                    <span className="ml-1 font-mono text-[11px] text-gm-inkFaint">
                      {item.currency}
                    </span>
                  )}
                </span>
              )}
              {item.merchantDomain && (
                <MonoCaption size="micro" tone="faint">{item.merchantDomain}</MonoCaption>
              )}
            </div>
          )}

          {/* Notes (only when present) */}
          {item.notes && (
            <div className="mb-6 p-4 bg-gm-paperDeep rounded-lg">
              <MonoCaption size="micro" tone="faint" className="block mb-2">
                {t('web_item_detail.notes_label')}
              </MonoCaption>
              <p className="font-body text-[14px] text-gm-inkSoft leading-[1.55] m-0">
                {item.notes}
              </p>
            </div>
          )}

          {/* Go to retailer — primary CTA (sibling <a> pattern per Btn JSDoc) */}
          <div className="mb-6">
            <a
              href={item.affiliateUrl}
              target="_blank"
              rel="noopener noreferrer"
              className={BTN_PRIMARY_LG_CLASSES}
            >
              {t('web_item_detail.go_to_retailer')}
            </a>
          </div>

          {/* Status-driven action zone */}
          {item.status === 'available' && (
            <ReserveButton registryId={registryId!} item={item} />
          )}

          {item.status === 'reserved' && isOwnReservation && active && (
            <div className="bg-gm-accentSoft border border-[rgba(200,98,58,0.30)] rounded-gm-card p-5">
              <Btn
                variant="accent"
                size="md"
                disabled={isPending}
                aria-busy={isPending}
                onClick={() => void confirm(active.reservationId)}
              >
                {isPending
                  ? t('web_item_detail.mark_as_purchased_loading')
                  : t('web_item_detail.mark_as_purchased_cta')}
              </Btn>
            </div>
          )}

          {item.status === 'reserved' && !isOwnReservation && (
            <div className="bg-gm-paperDeep border border-gm-line rounded-lg p-4">
              <p className="font-body text-[14px] text-gm-inkSoft leading-[1.55] m-0">
                {t('web_item_detail.reserved_read_only')}
              </p>
            </div>
          )}

          {item.status === 'purchased' && (
            <div className="bg-gm-paperDeep border border-gm-line rounded-lg p-4">
              <p className="font-body text-[14px] text-gm-inkSoft leading-[1.55] m-0">
                {t('web_item_detail.purchased_label')}
              </p>
            </div>
          )}
        </div>
      </main>

      <Footer />
    </div>
  )
}
