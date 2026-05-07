import { useTranslation } from 'react-i18next'
import type { Registry } from '../../lib/firestore-mapping'
import { Pill, MonoCaption } from '../../components/giftmaison'

interface Props {
  registry: Registry
}

/**
 * Hero block (UI-SPEC Screen 01 hero).
 * Layout (mobile = stacked, desktop ≥ 1024 px = side-by-side via parent grid):
 *   [occasion pill] [public/private pill] [· event date mono caps]
 *   <h1 Display XL italic-accent headline>
 *   <p Body L subline>
 *
 * The italic-accent emphasis on the headline is determined by the registry's
 * existing `name` field. Phase 13 doesn't introduce a separate emphasis-phrase
 * field — instead, this implementation italicises the entire registry name
 * unless a specific marker convention emerges. Final approach: the entire
 * Display XL renders in the upright body weight; if the owner-supplied name
 * contains a colon or em-dash, the suffix can be italic-accent. For v1.1 we
 * keep it simple — the whole headline is upright, and the accent period
 * is reserved to the wordmark. Italic-emphasis spans live on the reserve-detail
 * (Plan 05) and auth (Plan 06) headlines instead.
 *
 * D-06: NEVER render reserver/giver names anywhere in this header.
 */
export default function RegistryHeader({ registry }: Props) {
  const { t, i18n } = useTranslation()
  const dateFormatter = new Intl.DateTimeFormat(i18n.resolvedLanguage ?? 'en', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  })
  const formattedDate = registry.eventDate ? dateFormatter.format(registry.eventDate) : null

  // Map raw occasionType to a known Phase 13 occasion key. Housewarming-only for v1.1
  // (CONTEXT D-02); other occasions fall through to a generic copy.
  const occasionPillCopy =
    registry.occasionType?.toLowerCase() === 'housewarming'
      ? t('web_hero.occasion_housewarming')
      : (registry.occasionType?.toUpperCase() ?? '')

  const visibilityPill = registry.visibility === 'private'
    ? t('web_hero.private_pill')
    : t('web_hero.public_pill')

  return (
    <header className="flex flex-col gap-[18px] max-w-[640px]">
      <div className="flex flex-wrap items-center gap-[10px]">
        {occasionPillCopy && <Pill tone="accent" size="sm">{occasionPillCopy}</Pill>}
        <Pill tone="neutral" size="sm">{visibilityPill}</Pill>
        {formattedDate && (
          <MonoCaption size="sm" tone="faint">· {formattedDate}</MonoCaption>
        )}
      </div>
      <h1 className="font-display text-[36px] sm:text-[44px] lg:text-[56px] text-gm-ink leading-[1] sm:leading-[1.0] tracking-[-1.4px]">
        {registry.name}
      </h1>
      {registry.description && (
        <p className="font-body text-[15px] sm:text-[16px] text-gm-inkSoft leading-[1.55] [text-wrap:pretty] max-w-[540px]">
          {registry.description}
        </p>
      )}
    </header>
  )
}
