import { useTranslation } from 'react-i18next'

/**
 * EN / RO toggle (CONTEXT D-14, D-18 + UI-SPEC top nav atom shape).
 *
 * Renders as "EN / RO" mono-caps; the active locale is gm.ink, inactive is gm.inkFaint.
 * Click anywhere swaps locales (single-button toggle, simpler than the prototype's
 * two-button shape — same UX outcome, fewer DOM nodes, mirrors the existing
 * Phase 5 button semantics).
 *
 * Persistence: i18next-browser-languagedetector with localStorage 'lang' key
 * (Phase 5 D-15 lock — unchanged).
 */
export default function LanguageSwitcher() {
  const { i18n } = useTranslation()
  const isEn = (i18n.resolvedLanguage ?? 'en') === 'en'
  const otherLang = isEn ? 'ro' : 'en'

  return (
    <button
      type="button"
      onClick={() => { void i18n.changeLanguage(otherLang) }}
      aria-label={`Switch language to ${otherLang === 'en' ? 'English' : 'Română'}`}
      className="font-mono text-[11px] uppercase tracking-[0.5px] text-gm-inkFaint hover:text-gm-ink transition-colors focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gm-accent"
    >
      <span className={isEn ? 'text-gm-ink' : ''}>EN</span>
      <span className="mx-1 text-gm-inkFaint">/</span>
      <span className={!isEn ? 'text-gm-ink' : ''}>RO</span>
    </button>
  )
}
