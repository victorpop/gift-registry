import { useState } from 'react'
import { useParams } from 'react-router'
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
import AuthModal from '../features/auth/AuthModal'
import GuestIdentityModal from '../features/auth/GuestIdentityModal'
import { useAuth } from '../features/auth/useAuth'

const SKELETON_COUNT = 6

export default function RegistryPage() {
  const { id } = useParams<{ id: string }>()
  const { t } = useTranslation()
  const { user } = useAuth()
  const registryQ = useRegistryQuery(id)
  const itemsQ = useItemsQuery(id)
  const [authModalOpen, setAuthModalOpen] = useState(false)
  const [guestModalOpen, setGuestModalOpen] = useState(false)

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
        onContinueAsGuest={() => {
          setGuestModalOpen(true)
        }}
      />
      <GuestIdentityModal
        open={guestModalOpen}
        onOpenChange={setGuestModalOpen}
        onSubmit={() => {
          // Identity is saved by the modal itself; nothing further to do here.
          // Reserve flow starts from ReserveButton click once modal closes.
        }}
      />
    </div>
  )
}
