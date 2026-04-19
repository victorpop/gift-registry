import { useTranslation } from 'react-i18next'
import type { Registry } from '../../lib/firestore-mapping'

interface Props {
  registry: Registry
}

export default function RegistryHeader({ registry }: Props) {
  const { i18n } = useTranslation()
  const dateFormatter = new Intl.DateTimeFormat(i18n.resolvedLanguage ?? 'en', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
  const formattedDate = registry.eventDate ? dateFormatter.format(registry.eventDate) : null

  return (
    <header className="max-w-2xl mx-auto px-4 pt-16 pb-8">
      <h1 className="text-[28px] font-semibold text-surface-on leading-tight">
        {registry.name}
      </h1>
      <p className="mt-2 text-base font-normal text-surface-onVariant leading-relaxed">
        {[registry.occasionType, formattedDate, registry.eventLocation].filter(Boolean).join(' · ')}
      </p>
      {registry.description && (
        <p className="mt-4 text-base font-normal text-surface-on leading-relaxed">
          {registry.description}
        </p>
      )}
    </header>
  )
}
