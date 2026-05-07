import { useTranslation } from 'react-i18next'
import { TopNav, Footer, Btn } from '../components/giftmaison'
import { useNavigate } from 'react-router'

/**
 * Generic 404 page (UI-SPEC Error state row + Phase 5 D-13/D-14 enumeration safety).
 * Used for: deleted registries, bad/nonexistent registry IDs, private registries
 * the viewer cannot access. Intentionally no distinction (prevents private
 * registry ID enumeration).
 *
 * Phase 13 visual: full-page paper bg + TopNav/Footer chrome + Display L heading
 * + Body L body + ghost "Back" Btn returning to `/`.
 */
export default function NotFoundPage() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  return (
    <div className="min-h-screen flex flex-col bg-gm-paper">
      <TopNav />
      <main className="flex-1 flex items-center justify-center px-4 sm:px-7 lg:px-10">
        <div className="max-w-md text-center flex flex-col gap-5 items-center">
          <h1 className="font-display text-[28px] sm:text-[36px] lg:text-[44px] text-gm-ink leading-[1.05] tracking-[-1px] m-0">
            {t('registry.not_found_title')}
          </h1>
          <p className="font-body text-[15px] sm:text-[16px] text-gm-inkSoft leading-[1.55] m-0">
            {t('registry.not_found_body')}
          </p>
          <Btn variant="ghost" size="md" onClick={() => navigate('/')}>
            {t('common.back')}
          </Btn>
        </div>
      </main>
      <Footer />
    </div>
  )
}
