import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import LanguageDetector from 'i18next-browser-languagedetector'
import en from './en.json'
import ro from './ro.json'

// Key format: nested JSON, accessed via dot-notation (default i18next keySeparator '.')
// Example: t('reservation.reserve_item') resolves to "Reserve Gift" / "Rezervă Cadoul"
//
// Pitfall 5 (RESEARCH.md): do NOT set keySeparator: false — this would flatten access
// and break dot-notation lookups into the nested JSON.

void i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      en: { translation: en },
      ro: { translation: ro },
    },
    fallbackLng: 'en',
    supportedLngs: ['en', 'ro'],
    detection: {
      // Priority: explicit user choice (localStorage) > browser default
      order: ['localStorage', 'navigator'],
      caches: ['localStorage'],
      lookupLocalStorage: 'lang',
    },
    interpolation: {
      // React already escapes output, so i18next should not double-escape
      escapeValue: false,
    },
    returnNull: false,
  })

export default i18n
