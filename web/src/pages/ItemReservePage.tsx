import { useEffect, useRef } from 'react'
import { useParams, useNavigate, Link } from 'react-router'
import { useTranslation } from 'react-i18next'
import type { ItemStatus } from '../lib/firestore-mapping'
import { useItemsQuery } from '../features/registry/useItemsQuery'
import { useReservationForItem } from '../features/reservation/useReservationForItem'
import { useCountdown } from '../features/reservation/useCountdown'
import { useReleaseReservation } from '../features/reservation/useReleaseReservation'
import { useActiveReservation } from '../features/reservation/useActiveReservation'
import { ConfirmPurchaseBanner } from '../features/reservation/ConfirmPurchaseBanner'
import HowTimerWorks from '../features/reservation/HowTimerWorks'
import { useAuth } from '../features/auth/useAuth'
import { useGuestIdentity } from '../features/auth/useGuestIdentity'
import { useToast } from '../components/ToastProvider'
import { TopNav, Footer, MonoCaption, Btn, Pill } from '../components/giftmaison'

/**
 * ItemReservePage — dedicated per-item reserve-detail page at /registry/:id/item/:itemId.
 *
 * Enables each active reservation to be reachable by URL, not just the most-recent one.
 * Driven by useReservationForItem (NOT useActiveReservation context) so it works for
 * older concurrent reservations too.
 *
 * States (in priority order):
 *   loading   — items data undefined OR lookup status is idle/loading
 *   not-found — item not in registry items list
 *   not-yours — item exists but no active reservation for this viewer
 *   expired   — active reservation found but countdown.expired === true
 *   detail    — full reserve-detail UI
 *
 * D-06 enforcement: no reserver name or giver identity is ever rendered on this page.
 *
 * On release success: clear shared active-reservation context, show toast, navigate back to /registry/:id.
 * On confirm success: detected by item status flip to 'purchased' or 'available' →
 *   navigate back to /registry/:id. (ConfirmPurchaseBanner handles its own toast and
 *   clears the shared active context; this page navigates independently.)
 *
 * Note: ConfirmPurchaseBanner internally calls useActiveReservation().clear() on success.
 * If the viewer has another active reservation in the shared context, that clear() will
 * wipe it. This is accepted — when the user navigates back to /registry/:id,
 * useActiveReservationHydration will re-resolve the next most-recent active reservation.
 */
export default function ItemReservePage() {
  const { id, itemId } = useParams<{ id: string; itemId: string }>()
  const navigate = useNavigate()
  const { t } = useTranslation()
  const itemsQ = useItemsQuery(id)
  const { status: lookupStatus, active } = useReservationForItem(id, itemId)
  const countdown = useCountdown(active?.expiresAtMs ?? null)
  const { release, status: releaseStatus, error: releaseError } = useReleaseReservation()
  const { user } = useAuth()
  const { identity } = useGuestIdentity()
  const { showToast } = useToast()
  const { clear: clearActiveReservation } = useActiveReservation()

  // Signed-in: send undefined; guest: send identity.email.
  const giverEmailToSend = user ? undefined : (identity?.email ?? undefined)

  // Ref guard: fire release success toast + navigate once.
  const releaseSuccessHandledRef = useRef(false)
  const releaseErrorHandledRef = useRef<string | null>(null)

  // Ref guard: navigate back once when item status flips (covers confirm success too).
  const itemStatusNavigatedRef = useRef(false)
  // Tracks the previously-observed item status to detect real transitions out of 'reserved'.
  const prevStatusRef = useRef<ItemStatus | undefined>(undefined)

  // Release success: clear shared active-reservation context, show toast, navigate back.
  // Mirrors the pattern in StickyReserveBanner.tsx and ConfirmPurchaseBanner.tsx so the
  // viewer's RegistryPage no longer renders a phantom StickyReserveBanner for the released
  // reservation. The ref-guard ensures this fires exactly once per release-success transition.
  useEffect(() => {
    if (releaseStatus === 'success' && !releaseSuccessHandledRef.current) {
      releaseSuccessHandledRef.current = true
      showToast(t('reservation.release_success'), 'success')
      clearActiveReservation()
      navigate(`/registry/${id}`)
    }
  }, [releaseStatus, id, navigate, showToast, t, clearActiveReservation])

  // Release error: show toast once per error message.
  useEffect(() => {
    if (
      releaseStatus === 'error' &&
      releaseError &&
      releaseErrorHandledRef.current !== releaseError
    ) {
      releaseErrorHandledRef.current = releaseError
      showToast(t('reservation.release_error'), 'error')
    }
  }, [releaseStatus, releaseError, showToast, t])

  // Detect a real status TRANSITION out of 'reserved' (covers confirm-purchase success and
  // release fired from another tab / via Cloud Tasks). We must observe 'reserved' first —
  // otherwise a stale 'available' snapshot on initial mount would bounce the user back before
  // the reservation propagates. itemStatusNavigatedRef ensures we fire at most once per mount.
  const currentItemStatus = itemsQ.data?.find(i => i.id === itemId)?.status
  useEffect(() => {
    const prev = prevStatusRef.current
    prevStatusRef.current = currentItemStatus

    if (!active) return
    if (itemStatusNavigatedRef.current) return
    if (prev !== 'reserved') return
    if (currentItemStatus !== 'purchased' && currentItemStatus !== 'available') return

    itemStatusNavigatedRef.current = true
    navigate(`/registry/${id}`)
  }, [currentItemStatus, active, id, navigate])

  // --- State branches ---

  if (!id || !itemId) {
    // Fallback — shouldn't happen if route is mounted correctly.
    return null
  }

  // Loading state: items not yet loaded OR reservation lookup not yet complete.
  if (itemsQ.data === undefined || lookupStatus === 'idle' || lookupStatus === 'loading') {
    return (
      <div className="min-h-screen flex flex-col bg-gm-paper">
        <TopNav />
        <main className="flex-1 flex items-center justify-center px-4">
          <div data-testid="item-reserve-loading" className="text-center">
            <p className="font-body text-[15px] text-gm-inkSoft">
              {t('web_reserve.item_page.loading_label')}
            </p>
          </div>
        </main>
        <Footer />
      </div>
    )
  }

  const item = itemsQ.data.find(i => i.id === itemId) ?? null

  // Item not found in registry.
  if (!item) {
    return (
      <div className="min-h-screen flex flex-col bg-gm-paper">
        <TopNav />
        <main className="flex-1 flex items-center justify-center px-4">
          <div data-testid="item-reserve-not-found" className="max-w-md text-center flex flex-col gap-4 items-center">
            <h1 className="font-display text-[28px] sm:text-[36px] text-gm-ink leading-[1.05] tracking-[-1px] m-0">
              {t('web_reserve.item_page.item_not_found_title')}
            </h1>
            <p className="font-body text-[15px] text-gm-inkSoft leading-[1.55] m-0">
              {t('web_reserve.item_page.item_not_found_body')}
            </p>
            <Link
              to={`/registry/${id}`}
              aria-label={t('web_reserve.item_page.back_to_registry')}
              className="font-body text-[14px] text-gm-accent underline underline-offset-[3px]"
            >
              {t('web_reserve.item_page.back_to_registry')}
            </Link>
          </div>
        </main>
        <Footer />
      </div>
    )
  }

  // Not-yours state: item exists but no active reservation for this viewer.
  if (!active) {
    return (
      <div className="min-h-screen flex flex-col bg-gm-paper">
        <TopNav />
        <main className="flex-1 flex items-center justify-center px-4">
          <div data-testid="item-reserve-not-yours" className="max-w-md text-center flex flex-col gap-4 items-center">
            <h1 className="font-display text-[28px] sm:text-[36px] text-gm-ink leading-[1.05] tracking-[-1px] m-0">
              {t('web_reserve.item_page.not_yours_title')}
            </h1>
            <p className="font-body text-[15px] text-gm-inkSoft leading-[1.55] m-0">
              {t('web_reserve.item_page.not_yours_body')}
            </p>
            <Link
              to={`/registry/${id}`}
              aria-label={t('web_reserve.item_page.back_to_registry')}
              className="font-body text-[14px] text-gm-accent underline underline-offset-[3px]"
            >
              {t('web_reserve.item_page.back_to_registry')}
            </Link>
          </div>
        </main>
        <Footer />
      </div>
    )
  }

  // Expired state: countdown has reached 0.
  if (countdown?.expired) {
    return (
      <div className="min-h-screen flex flex-col bg-gm-paper">
        <TopNav />
        <main className="flex-1 flex items-center justify-center px-4">
          <div data-testid="item-reserve-expired" className="max-w-md text-center flex flex-col gap-4 items-center">
            <h1 className="font-display text-[28px] sm:text-[36px] text-gm-ink leading-[1.05] tracking-[-1px] m-0">
              {t('web_reserve.item_page.expired_title')}
            </h1>
            <p className="font-body text-[15px] text-gm-inkSoft leading-[1.55] m-0">
              {t('web_reserve.item_page.expired_body')}
            </p>
            <Link
              to={`/registry/${id}`}
              aria-label={t('web_reserve.item_page.back_to_registry')}
              className="font-body text-[14px] text-gm-accent underline underline-offset-[3px]"
            >
              {t('web_reserve.item_page.back_to_registry')}
            </Link>
          </div>
        </main>
        <Footer />
      </div>
    )
  }

  // --- Happy path: reserved-by-me detail UI ---

  const retailer = active.merchantDomain ?? 'retailer'
  const minutesLeft = countdown?.minutes ?? 0
  const mm = String(countdown?.minutes ?? 0).padStart(2, '0')
  const ss = String(countdown?.seconds ?? 0).padStart(2, '0')
  const mmss = `${mm}:${ss}`

  // Progress bar: 30 minutes total, fill width = remaining / total.
  const totalSeconds = 30 * 60
  const remainingPct = Math.max(
    0,
    Math.min(100, ((countdown?.totalSeconds ?? 0) / totalSeconds) * 100),
  )

  return (
    <div className="min-h-screen flex flex-col bg-gm-paper">
      <TopNav />
      <main className="flex-1" data-testid="item-reserve-detail">
        <div className="bg-gm-paperDeep border-b border-gm-line">
          <div className="px-4 sm:px-7 lg:px-10 pt-8 sm:pt-9 lg:pt-9 pb-8 max-w-7xl mx-auto w-full">
            {/* Back link */}
            <div className="mb-6">
              <Link
                to={`/registry/${id}`}
                aria-label={t('web_reserve.item_page.back_to_registry')}
                className="font-body text-[13px] text-gm-accent underline underline-offset-[3px] decoration-[1px] hover:decoration-2"
              >
                {t('web_reserve.item_page.back_to_registry')}
              </Link>
            </div>

            <MonoCaption size="micro" tone="faint">
              {t('web_reserve.item_page.page_caption')}
            </MonoCaption>
            <h1 className="font-display text-[28px] sm:text-[38px] lg:text-[44px] text-gm-ink leading-[1.05] tracking-[-1px] mt-[10px] mb-7 max-w-[640px]">
              {t('web_reserve.item_page.headline_pre')}
              <span className="italic text-gm-accent">{t('web_reserve.item_page.headline_emphasis')}</span>
              {t('web_reserve.item_page.headline_post')}
            </h1>

            <div className="grid grid-cols-1 lg:grid-cols-[1fr_340px] gap-8 items-start">
              {/* Left/main column */}
              <div className="flex flex-col gap-5">
                {/* Reserved item card (hero) */}
                <div className="flex flex-col sm:flex-row gap-5 p-5 bg-gm-paperDeep rounded-gm-card border border-gm-line">
                  <div className="w-full aspect-[4/3] sm:w-[160px] sm:h-[160px] sm:flex-shrink-0 rounded-[10px] overflow-hidden bg-gm-line">
                    {item.imageUrl && (
                      <img
                        src={item.imageUrl}
                        alt={active.itemName}
                        className="w-full h-full object-cover"
                      />
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
                        <span className="font-mono text-[12px] text-gm-accent font-medium" data-testid="reserve-detail-mmss">
                          {mmss}
                        </span>
                      </div>
                      <div className="h-[3px] bg-gm-line rounded-[2px] overflow-hidden">
                        <div
                          className="h-full bg-gm-accent transition-[width] duration-1000 ease-linear"
                          style={{ width: `${remainingPct}%` }}
                        />
                      </div>
                    </div>
                  </div>
                </div>

                {/* Confirm-back card */}
                <ConfirmPurchaseBanner reservationId={active.reservationId} minutesLeft={minutesLeft} />

                {/* Release CTA inline (no StickyReserveBanner on this page) */}
                <div className="flex flex-col sm:flex-row gap-3 sm:gap-[10px] sm:items-center">
                  <Btn
                    variant="quiet"
                    size="sm"
                    onClick={async () => {
                      await release(active.reservationId, giverEmailToSend)
                    }}
                    disabled={releaseStatus === 'pending'}
                    aria-busy={releaseStatus === 'pending'}
                  >
                    {t('web_reserve.release_cta')}
                  </Btn>
                  {active.affiliateUrl && (
                    <a
                      href={active.affiliateUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      aria-label={t('web_reserve.continue_cta', { retailer })}
                      className="inline-flex items-center justify-center gap-2 rounded-full border border-gm-accent bg-gm-accent text-gm-accentInk font-body font-medium tracking-[-0.1px] leading-none cursor-pointer px-3 py-[7px] text-[12px] focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gm-accent"
                    >
                      {t('web_reserve.continue_cta', { retailer })}
                    </a>
                  )}
                </div>
              </div>

              {/* Right/sidebar — desktop only via grid; mobile stacks below */}
              <HowTimerWorks retailer={retailer} />
            </div>
          </div>
        </div>
      </main>
      <Footer />
    </div>
  )
}
