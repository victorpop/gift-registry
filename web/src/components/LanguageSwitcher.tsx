import { useTranslation } from 'react-i18next'

/**
 * Toggle between English and Romanian.
 * Persists to localStorage key 'lang' via i18next-browser-languagedetector (see i18n/index.ts).
 */
export default function LanguageSwitcher() {
  const { i18n } = useTranslation()
  const currentLang = i18n.resolvedLanguage ?? 'en'
  const nextLang = currentLang === 'en' ? 'ro' : 'en'

  return (
    <button
      type="button"
      onClick={() => { void i18n.changeLanguage(nextLang) }}
      className="text-sm font-normal text-surface-onVariant hover:text-surface-on underline"
      aria-label={`Switch language to ${nextLang === 'en' ? 'English' : 'Română'}`}
    >
      {nextLang === 'en' ? 'EN' : 'RO'}
    </button>
  )
}
