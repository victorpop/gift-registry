import { useEffect, useMemo, useRef, useState } from 'react'
import { useParams, useSearchParams } from 'react-router'
import { useTranslation } from 'react-i18next'
import { useRegistryQuery } from '../features/registry/useRegistryQuery'
import { useItemsQuery } from '../features/registry/useItemsQuery'
import RegistryHeader from '../features/registry/RegistryHeader'
import ItemGrid from '../features/registry/ItemGrid'
import SkeletonCard from '../features/registry/SkeletonCard'
import ProgressStrip from '../features/registry/ProgressStrip'
import FilterChips, { type ItemFilter } from '../features/registry/FilterChips'
import NotFoundPage from './NotFoundPage'
import ReserveButton from '../features/reservation/ReserveButton'
import StickyReserveBanner from '../features/reservation/StickyReserveBanner'
import ReserveDetailSection from '../features/reservation/ReserveDetailSection'
import AuthModal from '../features/auth/AuthModal'
import GuestIdentityModal from '../features/auth/GuestIdentityModal'
import { useAuth } from '../features/auth/useAuth'
import { useGuestIdentity, type GuestIdentity } from '../features/auth/useGuestIdentity'
import { useCreateReservation } from '../features/reservation/useCreateReservation'
import { useActiveReservation } from '../features/reservation/useActiveReservation'
import { useToast } from '../components/ToastProvider'
import { mapHttpsErrorToI18nKey } from '../lib/error-mapping'
import { TopNav, Footer, MonoCaption } from '../components/giftmaison'

const SKELETON_COUNT = 6

export default function RegistryPage() {
  const { id } = useParams<{ id: string }>()
  const { t } = useTranslation()
  const [searchParams, setSearchParams] = useSearchParams()
  const { user, isReady: authReady } = useAuth()
  const { identity } = useGuestIdentity()
  const { active, set: setActive } = useActiveReservation()
  const { showToast } = useToast()
  const registryQ = useRegistryQuery(id)
  const itemsQ = useItemsQuery(id)

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

  // Counts per status — drives FilterChips count badges (optional UX).
  const counts = useMemo(() => {
    const all = itemsQ.data?.length ?? 0
    const reserved = itemsQ.data?.filter(i => i.status === 'reserved').length ?? 0
    const purchased = itemsQ.data?.filter(i => i.status === 'purchased').length ?? 0
    const available = all - reserved - purchased
    return { all, available, reserved, purchased } as Record<ItemFilter, number>
  }, [itemsQ.data])

  const totalChosen = (counts.reserved ?? 0) + (counts.purchased ?? 0)
  const total = counts.all ?? 0

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
                <ProgressStrip totalChosen={totalChosen} total={total} />
              </div>
            </section>

            {/* Section title + filter chips */}
            <section className="px-4 sm:px-7 lg:px-10 pt-8 max-w-7xl mx-auto w-full">
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
              {itemsQ.data && itemsQ.data.length > 0 ? (
                <ItemGrid
                  items={itemsQ.data}
                  registryId={registryQ.data!.id}
                  filter={filter}
                  renderReserve={(item) => (
                    <ReserveButton registryId={registryQ.data!.id} item={item} />
                  )}
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
