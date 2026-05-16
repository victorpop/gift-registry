import { useEffect, useRef } from 'react'
import { useParams, useNavigate, Link } from 'react-router'
import { useTranslation } from 'react-i18next'
import { Loader2 } from 'lucide-react'
import type { Item, ItemStatus } from '../lib/firestore-mapping'
import { useItemsQuery } from '../features/registry/useItemsQuery'
import { useReservationForItem } from '../features/reservation/useReservationForItem'
import { useCountdown } from '../features/reservation/useCountdown'
import { useReleaseReservation } from '../features/reservation/useReleaseReservation'
import { useActiveReservation } from '../features/reservation/useActiveReservation'
import { useCreateReservation } from '../features/reservation/useCreateReservation'
import { ConfirmPurchaseBanner } from '../features/reservation/ConfirmPurchaseBanner'
import HowTimerWorks from '../features/reservation/HowTimerWorks'
import { useAuth } from '../features/auth/useAuth'
import { useGuestIdentity } from '../features/auth/useGuestIdentity'
import { useToast } from '../components/ToastProvider'
import { mapHttpsErrorToI18nKey } from '../lib/error-mapping'
import { TopNav, Footer, MonoCaption, Btn, Pill } from '../components/giftmaison'

/**
 * ItemReservePage — dedicated per-item reserve-detail page at /registry/:id/item/:itemId.
 *
 * Enables each active reservation to be reachable by URL, not just the most-recent one.
 * Driven by useReservationForItem (NOT useActiveReservation context) so it works for
 * older concurrent reservations too.
 *
 * State priority (top → bottom — first match wins; branches 4 and 5 read
 * `effectiveActive` instead of `active`):
 *   1. !id || !itemId                                       → null (router safety)
 *   2. items undefined OR lookupStatus idle/loading          → loading
 *   3. !item                                                → item-not-found
 *   4. effectiveActive && countdown.expired                  → expired (in-session only)
 *   5. effectiveActive                                      → reserved-by-me detail (happy path)
 *   6. !effectiveActive && item.status === 'available'      → BROWSE_AVAILABLE (k37 — Reserve CTA)
 *   7. !effectiveActive && item.status === 'reserved'       → BROWSE_RESERVED_BY_OTHER (view-only, no CTA)
 *   8. !effectiveActive && item.status === 'purchased'      → BROWSE_PURCHASED (view-only, no CTA)
 *   9. (fallback, unreachable)                              → not-yours panel
 *
 * `effectiveActive` is derived as:
 *   effectiveActive = (active && countdown?.expired && !sawNonExpiredRef.current)
 *                       ? null
 *                       : active
 *
 * sawNonExpiredRef flips to true the first time we observe an active reservation
 * whose countdown is not yet expired during this mount. This distinguishes
 * legitimate in-session expiration (sawNonExpiredRef true → effectiveActive =
 * active → branch 4 renders "Your time ran out") from stale-expired-on-mount
 * (sawNonExpiredRef false → effectiveActive = null → branches 4 and 5 both skip
 * → flow falls through to item.status browse branches).
 *
 * IMPORTANT: useEffects that observe the reservation lifecycle (the
 * sawNonExpiredRef tracking effect AND the navigate-on-status-flip effect at
 * line ~131) continue to use the real `active`, NOT effectiveActive. The
 * reserve-mutation onSuccess also operates on the real shared
 * useActiveReservation context. effectiveActive is purely a render-branch gate.
 *
 * Stale-expired-on-mount handling (quick-260516-iux Bug B): when the page loads
 * with active.expiresAtMs already in the past (e.g. emulator restart killed the
 * auto-release setTimeout per quick-260510-pdp, or any other legacy stale-active
 * row), sawNonExpiredRef is false, so effectiveActive is null, so branches 4
 * and 5 are skipped. The viewer lands on the item.status browse branch —
 * typically BROWSE_RESERVED_BY_OTHER because the stale row keeps
 * item.status === 'reserved' until Cloud Tasks (or a manual refresh of items)
 * flips it.
 *
 * D-06 enforcement: no reserver/giver name/email is ever rendered on this page in any state.
 *
 * Reserve flow on the BROWSE_AVAILABLE branch (k37 user decision — DIRECT mutation):
 *   - Signed-in: derive giverName/email/uid from useAuth() and call useCreateReservation directly.
 *   - Guest with stored identity: derive from identity, giverId=null.
 *   - Anonymous-no-identity: fallback navigate to /registry/:id?autoReserveItemId=:itemId so
 *     the existing RegistryPage GuestIdentityModal handles identity capture (this is the
 *     ONLY path that round-trips through RegistryPage — all others reserve directly here).
 *   - On success: useActiveReservation.set() seeds the context; the next render naturally
 *     transitions into the reserved-by-me detail branch (no manual navigate).
 *
 * On release success: clear shared active-reservation context, show toast, navigate back to /registry/:id.
 * On confirm success: detected by item status flip to 'purchased' or 'available' →
 *   navigate back to /registry/:id.
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
  const { set: setActive, clear: clearActiveReservation } = useActiveReservation()

  // Direct Reserve mutation for the BROWSE_AVAILABLE branch (k37). On success:
  // seed the shared active-reservation context with the new reservation; the next
  // render of this page will pick it up via useReservationForItem and naturally
  // transition into the reserved-by-me detail branch — NO manual navigate needed
  // (the URL is already /registry/:id/item/:itemId). This intentionally differs
  // from RegistryPage's autoReserveMutation which DOES navigate, because that
  // mutation runs from the registry index and needs to push the user to detail.
  const reserveMutation = useCreateReservation({
    onSuccess: (data, vars) => {
      const target = itemsQ.data?.find(i => i.id === vars.itemId)
      setActive({
        reservationId: data.reservationId,
        itemId: vars.itemId,
        itemName: target?.title ?? '',
        affiliateUrl: data.affiliateUrl,
        merchantDomain: target?.merchantDomain ?? null,
        expiresAtMs: data.expiresAtMs,
      })
      showToast(t('reservation.success'), 'success')
    },
    onError: (err) => {
      const e = err as { code?: string; message?: string }
      showToast(t(mapHttpsErrorToI18nKey(e?.code, e?.message)), 'error')
    },
  })

  // Signed-in: send undefined; guest: send identity.email.
  const giverEmailToSend = user ? undefined : (identity?.email ?? undefined)

  // Ref guard: fire release success toast + navigate once.
  const releaseSuccessHandledRef = useRef(false)
  const releaseErrorHandledRef = useRef<string | null>(null)

  // Ref guard: navigate back once when item status flips (covers confirm success too).
  const itemStatusNavigatedRef = useRef(false)
  // Tracks the previously-observed item status to detect real transitions out of 'reserved'.
  const prevStatusRef = useRef<ItemStatus | undefined>(undefined)

  // sawNonExpiredRef: tracks whether we have EVER observed a non-expired
  // countdown for this active reservation during the current page mount.
  // - false on mount ⇒ if active+expired on first observation, the reservation
  //   was already stale before we arrived. We MUST short-circuit BOTH the
  //   expired branch (4) AND the reserved-by-me detail branch (5) — neither
  //   makes sense for a viewer who has no mental model of having reserved this
  //   item. Fall through to the item.status browse branches.
  // - true once observed non-expired ⇒ subsequent expiry (countdown ticking to
  //   0 while user is on the page) is an IN-SESSION expiration → render the
  //   legitimate "Your time ran out" UI (branch 4 fires normally).
  // Resets per mount (useRef in component scope) — desired behaviour: a user
  // who navigates AWAY and BACK to a now-expired reservation should not see the
  // expired page NOR the reserved-by-me detail if it had already expired before
  // they returned.
  const sawNonExpiredRef = useRef(false)

  // Tracking effect: flip sawNonExpiredRef to true the first time we observe the
  // REAL `active` reservation with a non-expired countdown during this mount.
  // MUST observe the real `active`, NOT effectiveActive — effectiveActive is the
  // gated render value; the ref is the gate's input.
  useEffect(() => {
    if (active && countdown && !countdown.expired) {
      sawNonExpiredRef.current = true
    }
  }, [active, countdown])

  // Release success: clear shared active-reservation context, show toast, navigate back.
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

  // --- Reserve CTA click handler (BROWSE_AVAILABLE branch) ---
  // Mirrors ReserveButton.tsx's handleClick logic so the derivation rules stay in sync.
  function handleReserveClick() {
    if (!id || !itemId) return
    if (reserveMutation.isPending) return

    if (user) {
      const giverName = user.displayName || (user.email ? user.email.split('@')[0] : 'Guest')
      const giverEmail = user.email ?? ''
      reserveMutation.mutate({
        registryId: id,
        itemId,
        giverName,
        giverEmail,
        giverId: user.uid,
      })
      return
    }

    if (identity) {
      const giverName = `${identity.firstName} ${identity.lastName}`.trim()
      reserveMutation.mutate({
        registryId: id,
        itemId,
        giverName,
        giverEmail: identity.email,
        giverId: null,
      })
      return
    }

    // Anonymous-no-identity: round-trip through RegistryPage so the existing
    // GuestIdentityModal can capture name + email. RegistryPage's auto-reserve
    // effect will then fire the mutation and on success navigate back here.
    navigate(`/registry/${id}?autoReserveItemId=${itemId}`)
  }

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

  // Gated `active` for render-branch decisions ONLY. When the page mounts with
  // an already-expired reservation (sawNonExpiredRef still false), treat active
  // as null so branches 4 and 5 both fall through to the item.status browse
  // branches (quick-260516-iux Bug B). The real `active` is still used by
  // useEffects (release-success, navigate-on-status-flip, sawNonExpiredRef
  // tracking) and by the reserve-mutation onSuccess. Only the render switches
  // care about effectiveActive.
  const effectiveActive =
    active && countdown?.expired && !sawNonExpiredRef.current
      ? null
      : active

  // Expired state: countdown has reached 0 (only checked when effectiveActive is set —
  // this means we observed a non-expired state earlier in this mount, so the
  // expiry happened in-session and the "Your time ran out" UI is legitimate).
  if (effectiveActive && countdown?.expired) {
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

  // --- Reserved-by-me detail (happy path) ---
  if (effectiveActive) {
    return renderReservedByMeDetail({
      id,
      item,
      active: effectiveActive,
      countdown,
      release,
      releaseStatus,
      giverEmailToSend,
      t,
    })
  }

  // --- Browse states (k37) ---

  if (item.status === 'available') {
    return (
      <BrowseShell id={id} t={t}>
        <div data-testid="item-reserve-available" className="flex flex-col gap-7">
          <div>
            <MonoCaption size="micro" tone="faint">
              {t('web_reserve.item_page.page_caption')}
            </MonoCaption>
            <h1 className="font-display text-[28px] sm:text-[38px] lg:text-[44px] text-gm-ink leading-[1.05] tracking-[-1px] mt-[10px] mb-2 max-w-[640px]">
              {t('web_reserve.item_page.available_title')}
            </h1>
            <p className="font-body text-[15px] text-gm-inkSoft leading-[1.55] m-0 max-w-[560px]">
              {t('web_reserve.item_page.available_subline')}
            </p>
          </div>
          <ItemDetailHero item={item} statusPillKey="web_pill.available" pillTone="neutral" t={t} />
          <NotesBlock notes={item.notes} t={t} />
          <div>
            <button
              type="button"
              onClick={handleReserveClick}
              disabled={reserveMutation.isPending}
              aria-busy={reserveMutation.isPending}
              className="min-h-[48px] px-6 rounded-full bg-gm-accent text-gm-accentInk font-body font-semibold text-base hover:opacity-90 focus:ring-2 focus:ring-gm-accent focus:ring-offset-2 focus:outline-none disabled:opacity-50 disabled:cursor-not-allowed inline-flex items-center justify-center gap-2"
            >
              {reserveMutation.isPending ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" aria-hidden="true" />
                  {t('web_reserve.item_page.reserve_cta_pending')}
                </>
              ) : (
                t('web_reserve.item_page.reserve_cta')
              )}
            </button>
          </div>
        </div>
      </BrowseShell>
    )
  }

  if (item.status === 'reserved') {
    return (
      <BrowseShell id={id} t={t}>
        <div data-testid="item-reserve-reserved-by-other" className="flex flex-col gap-7">
          <div>
            <MonoCaption size="micro" tone="faint">
              {t('web_reserve.item_page.page_caption')}
            </MonoCaption>
            <h1 className="font-display text-[28px] sm:text-[38px] lg:text-[44px] text-gm-ink leading-[1.05] tracking-[-1px] mt-[10px] mb-2 max-w-[640px]">
              {t('web_reserve.item_page.reserved_by_other_title')}
            </h1>
            <p className="font-body text-[15px] text-gm-inkSoft leading-[1.55] m-0 max-w-[560px]">
              {t('web_reserve.item_page.reserved_by_other_body')}
            </p>
          </div>
          <ItemDetailHero item={item} statusPillKey="web_pill.reserved_by_other" pillTone="accent" t={t} />
          <NotesBlock notes={item.notes} t={t} />
        </div>
      </BrowseShell>
    )
  }

  if (item.status === 'purchased') {
    return (
      <BrowseShell id={id} t={t}>
        <div data-testid="item-reserve-purchased" className="flex flex-col gap-7">
          <div>
            <MonoCaption size="micro" tone="faint">
              {t('web_reserve.item_page.page_caption')}
            </MonoCaption>
            <h1 className="font-display text-[28px] sm:text-[38px] lg:text-[44px] text-gm-ink leading-[1.05] tracking-[-1px] mt-[10px] mb-2 max-w-[640px]">
              {t('web_reserve.item_page.purchased_title')}
            </h1>
            <p className="font-body text-[15px] text-gm-inkSoft leading-[1.55] m-0 max-w-[560px]">
              {t('web_reserve.item_page.purchased_body')}
            </p>
          </div>
          <ItemDetailHero item={item} statusPillKey="web_pill.purchased" pillTone="ok" t={t} />
          <NotesBlock notes={item.notes} t={t} />
        </div>
      </BrowseShell>
    )
  }

  // Fallback (should not be reachable — every ItemStatus is handled above).
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

// ---- Browse-state helpers (inline — no new files per Task 2 spec step 2) ----

interface BrowseShellProps {
  id: string
  t: (key: string, opts?: Record<string, unknown>) => string
  children: React.ReactNode
}

/** Shared page chrome (TopNav + back link + Footer) used by all three browse branches. */
function BrowseShell({ id, t, children }: BrowseShellProps) {
  return (
    <div className="min-h-screen flex flex-col bg-gm-paper">
      <TopNav />
      <main className="flex-1">
        <div className="bg-gm-paperDeep border-b border-gm-line">
          <div className="px-4 sm:px-7 lg:px-10 pt-8 sm:pt-9 lg:pt-9 pb-10 max-w-7xl mx-auto w-full">
            <div className="mb-6">
              <Link
                to={`/registry/${id}`}
                aria-label={t('web_reserve.item_page.back_to_registry')}
                className="font-body text-[13px] text-gm-accent underline underline-offset-[3px] decoration-[1px] hover:decoration-2"
              >
                {t('web_reserve.item_page.back_to_registry')}
              </Link>
            </div>
            <div className="max-w-3xl">{children}</div>
          </div>
        </div>
      </main>
      <Footer />
    </div>
  )
}

interface ItemDetailHeroProps {
  item: Item
  statusPillKey: string
  pillTone: 'neutral' | 'accent' | 'ok'
  t: (key: string, opts?: Record<string, unknown>) => string
}

/**
 * Shared item-detail hero for the three browse branches (k37). Renders image / shop /
 * FULL untruncated name / price. NEVER renders reserver/giver name (D-06).
 */
function ItemDetailHero({ item, statusPillKey, pillTone, t }: ItemDetailHeroProps) {
  return (
    <div className="flex flex-col sm:flex-row gap-5 p-5 bg-gm-paper rounded-gm-card border border-gm-line">
      <div className="w-full aspect-[4/3] sm:w-[180px] sm:h-[180px] sm:flex-shrink-0 rounded-[10px] overflow-hidden bg-gm-line">
        {item.imageUrl && (
          <img
            src={item.imageUrl}
            alt={item.title}
            className="w-full h-full object-cover"
          />
        )}
      </div>
      <div className="flex-1 flex flex-col gap-[10px] min-w-0">
        <Pill tone={pillTone} size="sm">{t(statusPillKey)}</Pill>
        {item.merchantDomain && (
          <MonoCaption size="micro" tone="faint">{item.merchantDomain}</MonoCaption>
        )}
        <h2 className="m-0 font-body text-[20px] font-medium text-gm-ink leading-[1.2] tracking-[-0.3px] break-words">
          {item.title}
        </h2>
        {item.price != null && (
          <div className="font-body text-[16px] text-gm-ink font-semibold">
            {item.price}
            {item.currency && (
              <span className="ml-1 font-mono text-[12px] text-gm-inkFaint font-normal">
                {item.currency}
              </span>
            )}
          </div>
        )}
      </div>
    </div>
  )
}

interface NotesBlockProps {
  notes: string | null
  t: (key: string, opts?: Record<string, unknown>) => string
}

/** Owner notes block — only renders when notes is non-null. */
function NotesBlock({ notes, t }: NotesBlockProps) {
  if (!notes) return null
  return (
    <div className="flex flex-col gap-2 p-5 bg-gm-paper rounded-gm-card border border-gm-line">
      <MonoCaption size="micro" tone="faint">
        {t('web_reserve.item_page.notes_label')}
      </MonoCaption>
      <p className="font-body text-[15px] text-gm-inkSoft leading-[1.55] m-0 whitespace-pre-line">
        {notes}
      </p>
    </div>
  )
}

// ---- Reserved-by-me detail (existing happy path — extracted as a render fn so the
//      main component body stays linear and the new browse branches are easy to read) ----

interface ReservedByMeDetailParams {
  id: string
  item: Item
  active: { reservationId: string; itemName: string; affiliateUrl: string; merchantDomain: string | null; expiresAtMs: number; itemId: string }
  countdown: ReturnType<typeof useCountdown>
  release: (reservationId: string, giverEmail?: string) => Promise<unknown> | unknown
  releaseStatus: string
  giverEmailToSend: string | undefined
  t: (key: string, opts?: Record<string, unknown>) => string
}

function renderReservedByMeDetail({
  id,
  item,
  active,
  countdown,
  release,
  releaseStatus,
  giverEmailToSend,
  t,
}: ReservedByMeDetailParams) {
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
