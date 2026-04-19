import { useEffect, useRef, useState } from 'react'
import { useParams, useSearchParams } from 'react-router'
import { useTranslation } from 'react-i18next'
import { useRegistryQuery } from '../features/registry/useRegistryQuery'
import { useItemsQuery } from '../features/registry/useItemsQuery'
import RegistryHeader from '../features/registry/RegistryHeader'
import ItemGrid from '../features/registry/ItemGrid'
import SkeletonCard from '../features/registry/SkeletonCard'
import NotFoundPage from './NotFoundPage'
import LanguageSwitcher from '../components/LanguageSwitcher'
import ReserveButton from '../features/reservation/ReserveButton'
import ReservationBanner from '../features/reservation/ReservationBanner'
import { ConfirmPurchaseBanner } from '../features/reservation/ConfirmPurchaseBanner'
import AuthModal from '../features/auth/AuthModal'
import GuestIdentityModal from '../features/auth/GuestIdentityModal'
import { useAuth } from '../features/auth/useAuth'
import { useGuestIdentity, type GuestIdentity } from '../features/auth/useGuestIdentity'
import { useCreateReservation } from '../features/reservation/useCreateReservation'
import { useActiveReservation } from '../features/reservation/useActiveReservation'
import { useToast } from '../components/ToastProvider'
import { mapHttpsErrorToI18nKey } from '../lib/error-mapping'

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

  // WEB-D-13 + WEB-D-14: registry === null (not-found OR permission-denied) → 404.
  // No distinction between cases — prevents private registry enumeration.
  if (registryQ.data === null) {
    return <NotFoundPage />
  }

  const isInitialLoading = registryQ.data === undefined

  return (
    <div className="min-h-screen bg-surface">
      <nav className="h-12 bg-surface-variant flex items-center justify-between px-4 sticky top-0 z-20">
        <span className="text-base font-semibold text-surface-on">{t('app.name')}</span>
        <div className="flex items-center gap-4">
          {!user && (
            <button
              type="button"
              onClick={() => setAuthModalOpen(true)}
              className="text-sm font-normal text-surface-on underline hover:text-primary"
            >
              {t('auth.sign_in_link')}
            </button>
          )}
          <LanguageSwitcher />
        </div>
      </nav>

      <ReservationBanner />
      {active && (
        <ConfirmPurchaseBanner reservationId={active.reservationId} />
      )}

      {isInitialLoading ? (
        <>
          <div className="max-w-2xl mx-auto px-4 pt-16 pb-8">
            <div className="h-8 w-2/3 rounded bg-surface-variant animate-pulse" />
            <div className="h-4 w-1/2 rounded bg-surface-variant animate-pulse mt-3" />
          </div>
          <div className="max-w-2xl mx-auto px-4 pb-16 grid grid-cols-1 md:grid-cols-2 gap-4">
            {Array.from({ length: SKELETON_COUNT }).map((_, i) => (
              <SkeletonCard key={i} />
            ))}
          </div>
        </>
      ) : (
        <>
          <RegistryHeader registry={registryQ.data} />
          {itemsQ.data && itemsQ.data.length > 0 ? (
            <ItemGrid
              items={itemsQ.data}
              renderReserve={(item) => (
                <ReserveButton registryId={registryQ.data!.id} item={item} />
              )}
            />
          ) : (
            <div className="max-w-2xl mx-auto px-4 pb-16 text-center">
              <h2 className="text-xl font-semibold text-surface-on leading-tight">{t('registry.empty_title')}</h2>
              <p className="mt-2 text-base font-normal text-surface-onVariant leading-relaxed">{t('registry.empty_body')}</p>
            </div>
          )}
        </>
      )}

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
