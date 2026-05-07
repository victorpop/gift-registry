import { useTranslation } from 'react-i18next'

/**
 * Shared footer (CONTEXT D-15 + UI-SPEC).
 *
 * Single line of mono-caps 10 px gm.inkFaint copy with letter-spacing 0.5 px.
 * The "en / ro" portion of the line is interactive — clicking toggles
 * i18n.changeLanguage. "terms" / "privacy" are stub anchors (#) for v1.1.
 *
 * Padding 16/16 (mobile), 16/40 (desktop) per UI-SPEC.
 *
 * Reads `web_footer.line` from i18n with three sub-tokens substituted:
 *   {terms}   → linked to #
 *   {privacy} → linked to #
 *   {locales} → "en / ro" with locale-toggle button
 *
 * Plan 03 ships the i18n keys; until then the t() calls fall back to the
 * key string and Plan 03's regression sweep validates the rendered copy.
 */
export function Footer() {
  const { t, i18n } = useTranslation()
  const otherLang = i18n.resolvedLanguage === 'ro' ? 'en' : 'ro'
  const localeLabel = i18n.resolvedLanguage === 'ro' ? 'ro / en' : 'en / ro'

  return (
    <footer className="px-4 py-6 sm:px-10 sm:py-8 border-t border-gm-line bg-gm-paper">
      <div className="flex flex-wrap items-center gap-x-3 gap-y-1 font-mono text-[10px] uppercase tracking-[0.5px] text-gm-inkFaint">
        <span>{t('web_footer.copyright', { defaultValue: '© giftmaison 2026' })}</span>
        <span aria-hidden="true">·</span>
        <a href="#" className="hover:text-gm-ink transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gm-accent">
          {t('web_footer.terms', { defaultValue: 'terms' })}
        </a>
        <span aria-hidden="true">·</span>
        <a href="#" className="hover:text-gm-ink transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gm-accent">
          {t('web_footer.privacy', { defaultValue: 'privacy' })}
        </a>
        <span aria-hidden="true">·</span>
        <button
          type="button"
          onClick={() => { void i18n.changeLanguage(otherLang) }}
          aria-label={`Switch language to ${otherLang === 'en' ? 'English' : 'Română'}`}
          className="hover:text-gm-ink transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gm-accent uppercase tracking-[0.5px]"
        >
          {localeLabel}
        </button>
      </div>
    </footer>
  )
}

export default Footer
