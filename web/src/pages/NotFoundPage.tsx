import { useTranslation } from 'react-i18next'

/**
 * Generic 404 page. Used for:
 *   - deleted registries (Firestore not-found)
 *   - bad/nonexistent registry IDs (Firestore not-found)
 *   - private registries the viewer cannot access (Firestore permission-denied)
 * Intentionally no distinction between these cases per WEB-D-13/WEB-D-14 — prevents
 * enumeration of private registry IDs.
 */
export default function NotFoundPage() {
  const { t } = useTranslation()
  return (
    <div className="min-h-screen flex items-center justify-center bg-surface px-6">
      <div className="max-w-md text-center">
        <h1 className="text-xl font-semibold text-surface-on leading-tight mb-4">
          {t('registry.not_found_title')}
        </h1>
        <p className="text-base font-normal text-surface-onVariant leading-relaxed">
          {t('registry.not_found_body')}
        </p>
      </div>
    </div>
  )
}
