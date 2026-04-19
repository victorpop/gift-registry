import { useParams } from 'react-router'
import { useTranslation } from 'react-i18next'
import LanguageSwitcher from '../components/LanguageSwitcher'

/**
 * Plan 03 stub — Plan 04 replaces the body with RegistryHeader + ItemGrid + ReservationBanner.
 * The stub exists so the router resolves and typecheck/tests pass before Plan 04 lands.
 */
export default function RegistryPage() {
  const { id } = useParams<{ id: string }>()
  const { t } = useTranslation()
  return (
    <div className="min-h-screen bg-surface">
      <nav className="h-12 bg-surface-variant flex items-center justify-between px-4 sticky top-0 z-10">
        <span className="text-base font-semibold text-surface-on">{t('app.name')}</span>
        <LanguageSwitcher />
      </nav>
      <main className="max-w-2xl mx-auto px-4 pt-16 pb-16">
        <h1 className="text-[28px] font-semibold text-surface-on leading-tight">
          Registry {id}
        </h1>
        <p className="text-sm font-normal text-surface-onVariant mt-2">
          {t('common.loading')}
        </p>
      </main>
    </div>
  )
}
