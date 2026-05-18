import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useParams, useSearchParams, useNavigate, useLocation } from 'react-router'
import { useTranslation } from 'react-i18next'
import { useRegistryQuery } from '../features/registry/useRegistryQuery'
import { useItemsQuery } from '../features/registry/useItemsQuery'
import RegistryHeader from '../features/registry/RegistryHeader'
import ItemGrid from '../features/registry/ItemGrid'
import SkeletonCard from '../features/registry/SkeletonCard'
import ProgressStrip from '../features/registry/ProgressStrip'
import FilterChips, { type ItemFilter } from '../features/registry/FilterChips'
import NotFoundPage from './NotFoundPage'
import StickyReserveBanner from '../features/reservation/StickyReserveBanner'
import ReserveDetailSection from '../features/reservation/ReserveDetailSection'
import AuthModal from '../features/auth/AuthModal'
import GuestIdentityModal from '../features/auth/GuestIdentityModal'
import { useAuth } from '../features/auth/useAuth'
import { useGuestIdentity, type GuestIdentity } from '../features/auth/useGuestIdentity'
import { useCreateReservation } from '../features/reservation/useCreateReservation'
import { useActiveReservation } from '../features/reservation/useActiveReservation'
import { useActiveReservationHydration } from '../features/reservation/useActiveReservationHydration'
import { useToast } from '../components/ToastProvider'
import { mapHttpsErrorToI18nKey } from '../lib/error-mapping'
import type { Item } from '../lib/firestore-mapping'
import { TopNav, Footer, MonoCaption } from '../components/giftmaison'

const SKELETON_COUNT = 6

export default function RegistryPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { t } = useTranslation()
  const [searchParams, setSearchParams] = useSearchParams()
  const { user, isReady: authReady } = useAuth()
  const { identity } = useGuestIdentity()
  const { active, set: setActive } = useActiveReservation()
  const { showToast } = useToast()
  const registryQ = useRegistryQuery(id)
  const itemsQ = useItemsQuery(id)

  // j5j: read navigation state on first render (NOT inside a useEffect — must be visible
  // to the FIRST render to suppress the post-release races). location.state is the value
  // passed via navigate(path, { state }) from ItemReservePage's release-success handler.
  // When absent (refresh, deep link, navigate without state), the values are undefined
  // and all j5j logic becomes a no-op identity — pre-j5j behavior is preserved exactly.
  const location = useLocation()
  const navState = location.state as
    | { recentReleasedReservationId?: string; recentReleasedItemId?: string }
    | null
  const recentReleasedReservationId = navState?.recentReleasedReservationId
  const recentReleasedItemId = navState?.recentReleasedItemId

  // Hydrate active reservation from Firestore on page load (refresh, new tab, other device for signed-in).
  // The hook bails when active is already set to avoid clobbering a fresh in-session reservation.
  // j5j: when location.state carries recentReleasedReservationId (post-release from
  // ItemReservePage), pass it as ignoreReservationId so the hook treats a brief
  // composite-index-lag echo of the just-released reservation as null instead of
  // re-seeding ActiveReservationContext.
  useActiveReservationHydration(id, { ignoreReservationId: recentReleasedReservationId })

  // Compute effective email for reserved-by-me detection (D-06: never render reserver name).
  const effectiveEmail = user?.email ?? identity?.email ?? null

  // Returns a navigate handler ONLY when the item belongs to the current viewer.
  // Clicking a reserved-by-me item card navigates to /registry/:id/item/:itemId —
  // the per-item reserve-detail page where the specific reservation is fully manageable.
  const renderReservedByMeClick = useCallback((item: Item) => {
    if (!effectiveEmail) return undefined
    if (item.status !== 'reserved') return undefined
    if (item.reservedBy !== effectiveEmail) return undefined
    return () => {
      navigate(`/registry/${id}/item/${item.id}`)
    }
  }, [effectiveEmail, id, navigate])

  const [authModalOpen, setAuthModalOpen] = useState(false)
  const [guestModalOpen, setGuestModalOpen] = useState(false)
  const [pendingAutoReserveItemId, setPendingAutoReserveItemId] = useState<string | null>(null)
  const [filter, setFilter] = useState<ItemFilter>('all')
  // Ref guard: prevents auto-reserve from firing more than once per page mount.
  // Also protects against React 18 StrictMode double-effect invocation.
  const autoReserveFiredRef = useRef(false)

  const autoReserveMutation = useCreateReservation({
    onSuccess: (data, vars) => {
      const item = itemsQ.data?.find(i => i.id === vars.itemId)
      setActive({
        reservationId: data.reservationId,
        itemId: vars.itemId,
        itemName: item?.title ?? '',
        affiliateUrl: data.affiliateUrl,
        merchantDomain: item?.merchantDomain ?? null,
        expiresAtMs: data.expiresAtMs,
      })
      showToast(t('reservation.success'), 'success')
      // Auto-navigate to the per-item reserve-detail page after a successful reserve.
      navigate(`/registry/${id}/item/${vars.itemId}`)
    },
    onError: (err) => {
      const e = err as { code?: string; message?: string }
      showToast(t(mapHttpsErrorToI18nKey(e?.code, e?.message)), 'error')
    },
  })

  // Read the autoReserveItemId query param — set by ReReservePage after resolveReservation succeeds.
  const autoReserveItemId = searchParams.get('autoReserveItemId')

  useEffect(() => {
    // Idempotency guard: only fire once per page mount.
    if (autoReserveFiredRef.current) return
    // Param not present — nothing to do.
    if (!autoReserveItemId) return
    // Gate: Firebase Auth must have resolved before we attempt the mutation.
    if (!authReady) return
    // Gate: Items list must be loaded to determine item availability.
    if (!itemsQ.data) return

    const item = itemsQ.data.find(i => i.id === autoReserveItemId)

    // Case 1: item not found OR no longer available → show conflict toast, clear param.
    if (!item || item.status !== 'available') {
      autoReserveFiredRef.current = true
      showToast(t('reservation.conflict'), 'error')
      const next = new URLSearchParams(searchParams)
      next.delete('autoReserveItemId')
      setSearchParams(next, { replace: true })
      return
    }

    // Case 2: authenticated user → fire mutation directly.
    if (user) {
      autoReserveFiredRef.current = true
      const next = new URLSearchParams(searchParams)
      next.delete('autoReserveItemId')
      setSearchParams(next, { replace: true })
      autoReserveMutation.mutate({
        registryId: id!,
        itemId: item.id,
        giverName: user.displayName || (user.email?.split('@')[0] ?? 'Guest'),
        giverEmail: user.email ?? '',
        giverId: user.uid,
      })
      return
    }

    // Case 3: anonymous WITH stored guest identity → fire mutation with stored identity.
    if (identity) {
      autoReserveFiredRef.current = true
      const next = new URLSearchParams(searchParams)
      next.delete('autoReserveItemId')
      setSearchParams(next, { replace: true })
      autoReserveMutation.mutate({
        registryId: id!,
        itemId: item.id,
        giverName: `${identity.firstName} ${identity.lastName}`.trim(),
        giverEmail: identity.email,
        giverId: null,
      })
      return
    }

    // Case 4: anonymous WITHOUT stored guest identity → open GuestIdentityModal.
    // Remember the item id so the modal's onSubmit can complete the reservation.
    autoReserveFiredRef.current = true
    setPendingAutoReserveItemId(item.id)
    setGuestModalOpen(true)
    // Don't clear the param yet — handleGuestSubmitForAutoReserve will clear it on submit.
  }, [
    autoReserveItemId,
    authReady,
    itemsQ.data,
    user,
    identity,
    id,
    searchParams,
    setSearchParams,
    autoReserveMutation,
    showToast,
    t,
  ])

  function handleGuestSubmitForAutoReserve(g: GuestIdentity) {
    if (!pendingAutoReserveItemId || !id) return
    const itemId = pendingAutoReserveItemId
    setPendingAutoReserveItemId(null)
    const next = new URLSearchParams(searchParams)
    next.delete('autoReserveItemId')
    setSearchParams(next, { replace: true })
    autoReserveMutation.mutate({
      registryId: id,
      itemId,
      giverName: `${g.firstName} ${g.lastName}`.trim(),
      giverEmail: g.email,
      giverId: null,
    })
  }

  // j5j: render-shape override for the snapshot-race window after release-from-ItemReservePage.
  // When location.state carries recentReleasedItemId, override that item's status to
  // 'available' (clearing reservedBy/reservedAt/expiresAt) IF AND ONLY IF the cache still
  // shows it as 'reserved'. This neutralizes the case where useItemsQuery's new onSnapshot
  // listener fires first with stale Firestore client-cache data (item.status === 'reserved'),
  // overwriting oiy's optimistic patch. Once the user navigates away (or the real server
  // snapshot fires with fresh data), the override never re-engages — location.state is
  // consumed once per mount.
  //
  // Constraints:
  //   - itemsQ.data === undefined ⇒ itemsForRender === undefined (preserves loading-state render).
  //   - recentReleasedItemId absent ⇒ return itemsQ.data unchanged (referential identity).
  //   - released item not 'reserved' anymore ⇒ no override (identity). NEVER downgrades 'purchased'.
  //   - released item not present (deleted) ⇒ no override (identity).
  const itemsForRender = useMemo<Item[] | undefined>(() => {
    if (!itemsQ.data) return undefined
    if (!recentReleasedItemId) return itemsQ.data
    const target = itemsQ.data.find(it => it.id === recentReleasedItemId)
    if (!target) return itemsQ.data
    if (target.status !== 'reserved') return itemsQ.data
    return itemsQ.data.map(it =>
      it.id === recentReleasedItemId
        ? { ...it, status: 'available' as const, reservedBy: null, reservedAt: null, expiresAt: null }
        : it,
    )
  }, [itemsQ.data, recentReleasedItemId])

  // Counts per status — drives FilterChips count badges (optional UX).
  // j5j: consume itemsForRender so the progress strip + filter chip counts reflect the
  // override (no flash of stale counts contradicting the grid).
  const counts = useMemo(() => {
    const all = itemsForRender?.length ?? 0
    const reserved = itemsForRender?.filter(i => i.status === 'reserved').length ?? 0
    const purchased = itemsForRender?.filter(i => i.status === 'purchased').length ?? 0
    const available = all - reserved - purchased
    return { all, available, reserved, purchased } as Record<ItemFilter, number>
  }, [itemsForRender])

  const totalChosen = (counts.reserved ?? 0) + (counts.purchased ?? 0)
  const total = counts.all ?? 0
  const isOwner = !!user && registryQ.data?.ownerId === user.uid

  // WEB-D-13 + WEB-D-14: registry === null (not-found OR permission-denied) → 404.
  // No distinction between cases — prevents private registry enumeration.
  if (registryQ.data === null) {
    return <NotFoundPage />
  }

  const isInitialLoading = registryQ.data === undefined

  return (
    <div className="min-h-screen flex flex-col bg-gm-paper">
      <TopNav onSignInClick={() => setAuthModalOpen(true)} />

      <StickyReserveBanner />

      <main className="flex-1">
        {active && <ReserveDetailSection registryId={id!} />}
        {isInitialLoading ? (
          <>
            <section className="px-4 sm:px-7 lg:px-10 pt-10 pb-8 max-w-7xl mx-auto w-full">
              <div className="h-8 w-2/3 rounded bg-gm-paperDeep animate-pulse" />
              <div className="h-4 w-1/2 rounded bg-gm-paperDeep animate-pulse mt-3" />
            </section>
            <section className="px-4 sm:px-7 lg:px-10 pb-12 max-w-7xl mx-auto w-full grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 lg:gap-5">
              {Array.from({ length: SKELETON_COUNT }).map((_, i) => (
                <SkeletonCard key={i} />
              ))}
            </section>
          </>
        ) : (
          <>
            {/* Hero + progress strip — stacked on mobile, side-by-side from lg: */}
            <section className="px-4 sm:px-7 lg:px-10 pt-8 sm:pt-10 lg:pt-12 pb-8 lg:pb-9 border-b border-gm-line max-w-7xl mx-auto w-full">
              <div className="flex flex-col lg:flex-row lg:gap-10 lg:items-end lg:justify-between gap-6">
                <RegistryHeader registry={registryQ.data} />
                <ProgressStrip totalChosen={totalChosen} total={total} isOwner={isOwner} />
              </div>
            </section>

            {/* Section title + filter chips */}
            <section id="registry-list-section" className="px-4 sm:px-7 lg:px-10 pt-8 max-w-7xl mx-auto w-full">
              <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between mb-5">
                <h2 className="font-display text-[24px] lg:text-[34px] text-gm-ink leading-[1.05] tracking-[-0.5px] m-0">
                  {t('web_hero.section_title')}{' '}
                  <MonoCaption size="sm" tone="faint">
                    {t('web_hero.section_item_count', { n: total })}
                  </MonoCaption>
                </h2>
                <FilterChips active={filter} onChange={setFilter} counts={counts} />
              </div>

              {/* Item grid OR empty state */}
              {itemsForRender && itemsForRender.length > 0 ? (
                <ItemGrid
                  items={itemsForRender}
                  registryId={registryQ.data!.id}
                  filter={filter}
                  renderReservedByMeClick={renderReservedByMeClick}
                />
              ) : (
                <div className="text-center py-16">
                  <h3 className="font-display text-[24px] lg:text-[34px] text-gm-ink leading-[1.05] tracking-[-0.5px]">
                    {t('registry.empty_title')}
                  </h3>
                  <p className="mt-3 font-body text-[15px] sm:text-[16px] text-gm-inkSoft leading-[1.55] max-w-md mx-auto">
                    {t('registry.empty_body')}
                  </p>
                </div>
              )}
            </section>

            {/* Bottom spacing */}
            <div className="h-12 sm:h-16" aria-hidden="true" />
          </>
        )}
      </main>

      <Footer />

      <AuthModal
        open={authModalOpen}
        onOpenChange={setAuthModalOpen}
        onContinueAsGuest={() => setGuestModalOpen(true)}
      />
      <GuestIdentityModal
        open={guestModalOpen}
        onOpenChange={(o) => {
          setGuestModalOpen(o)
          if (!o) setPendingAutoReserveItemId(null)
        }}
        onSubmit={(g) => {
          if (pendingAutoReserveItemId) {
            handleGuestSubmitForAutoReserve(g)
          }
        }}
      />
    </div>
  )
}
