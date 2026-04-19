import { useParams } from 'react-router'
import { useTranslation } from 'react-i18next'
import { useRegistryQuery } from '../features/registry/useRegistryQuery'
import { useItemsQuery } from '../features/registry/useItemsQuery'
import RegistryHeader from '../features/registry/RegistryHeader'
import ItemGrid from '../features/registry/ItemGrid'
import SkeletonCard from '../features/registry/SkeletonCard'
import NotFoundPage from './NotFoundPage'
import LanguageSwitcher from '../components/LanguageSwitcher'

const SKELETON_COUNT = 6

export default function RegistryPage() {
  const { id } = useParams<{ id: string }>()
  const { t } = useTranslation()
  const registryQ = useRegistryQuery(id)
  const itemsQ = useItemsQuery(id)

  // WEB-D-13 + WEB-D-14: registry === null (not-found OR permission-denied) → 404.
  // No distinction between cases — prevents private registry enumeration.
  if (registryQ.data === null) {
    return <NotFoundPage />
  }

  const isInitialLoading = registryQ.data === undefined

  return (
    <div className="min-h-screen bg-surface">
      <nav className="h-12 bg-surface-variant flex items-center justify-between px-4 sticky top-0 z-10">
        <span className="text-base font-semibold text-surface-on">{t('app.name')}</span>
        <div className="flex items-center gap-4">
          {/* Plan 05 adds AuthModal trigger here */}
          <LanguageSwitcher />
        </div>
      </nav>

      {/* Plan 06 inserts <ReservationBanner /> here (sticky below nav) when current giver has active reservation */}

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
            <ItemGrid items={itemsQ.data} />
          ) : (
            <div className="max-w-2xl mx-auto px-4 pb-16 text-center">
              <h2 className="text-xl font-semibold text-surface-on leading-tight">
                {t('registry.empty_title')}
              </h2>
              <p className="mt-2 text-base font-normal text-surface-onVariant leading-relaxed">
                {t('registry.empty_body')}
              </p>
            </div>
          )}
        </>
      )}
    </div>
  )
}
