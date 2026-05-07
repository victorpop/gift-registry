import { useTranslation } from 'react-i18next'
import { TopNav, Footer, MonoCaption } from '../components/giftmaison'

/**
 * Landing page at "/". A minimal app intro screen for the giver who somehow
 * arrives at the bare domain (most arrive via /registry/:id deep links).
 *
 * Phase 13 restyle: full-bleed paper bg, TopNav + Footer chrome, italic-accent
 * Display L headline, body subline. No interactive elements beyond the chrome.
 */
export default function AppRootPage() {
  const { t } = useTranslation()
  return (
    <div className="min-h-screen flex flex-col bg-gm-paper">
      <TopNav />
      <main className="flex-1 flex flex-col items-center justify-center px-4 sm:px-7 md:px-10 py-16 max-w-2xl mx-auto w-full">
        <MonoCaption size="md" tone="faint" className="mb-4">
          {t('app.subtitle')}
        </MonoCaption>
        <h1 className="font-display text-[28px] sm:text-[36px] md:text-[44px] text-gm-ink leading-[1.05] tracking-[-1px] text-center">
          {t('app.name', { defaultValue: 'GiftMaison' })}
          <span className="text-gm-accent">.</span>
        </h1>
      </main>
      <Footer />
    </div>
  )
}
