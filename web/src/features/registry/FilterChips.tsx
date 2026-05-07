import { useTranslation } from 'react-i18next'

export type ItemFilter = 'all' | 'available' | 'reserved' | 'purchased'

export interface FilterChipsProps {
  active: ItemFilter
  onChange: (next: ItemFilter) => void
  /** Item counts per status — when provided, chips show "Available 3" etc. */
  counts?: Partial<Record<ItemFilter, number>>
}

/**
 * Horizontally scrollable filter chips (UI-SPEC mobile-only patterns).
 * Track: bg-gm-paperDeep rounded-full p-1.
 * Chips: Body 12.5 px Inter weight 500. Active chip = bg-gm-paper +
 * shadow-[0_1px_2px_rgba(0,0,0,0.06)] + text-gm-ink. Inactive = text-gm-inkFaint.
 *
 * On mobile the row scrolls horizontally with `overflow-x-auto` and momentum
 * (no fade overlay — handoff explicit). On desktop the chips sit inline with
 * the section title in the parent layout.
 */
export function FilterChips({ active, onChange, counts }: FilterChipsProps) {
  const { t } = useTranslation()
  const filters: { key: ItemFilter; labelKey: string }[] = [
    { key: 'all',        labelKey: 'web_hero.filter_all' },
    { key: 'available',  labelKey: 'reservation.status_available' },
    { key: 'reserved',   labelKey: 'reservation.status_reserved' },
    { key: 'purchased',  labelKey: 'reservation.status_purchased' },
  ]
  return (
    <div
      role="tablist"
      aria-label="Filter items by status"
      className="flex gap-[6px] p-1 bg-gm-paperDeep rounded-full overflow-x-auto whitespace-nowrap [scrollbar-width:none] [-ms-overflow-style:none] [&::-webkit-scrollbar]:hidden"
    >
      {filters.map(({ key, labelKey }) => {
        const isActive = active === key
        const count = counts?.[key]
        return (
          <button
            key={key}
            role="tab"
            type="button"
            aria-selected={isActive}
            onClick={() => onChange(key)}
            className={[
              'px-[14px] py-[6px] rounded-full font-body text-[12.5px] font-medium leading-none transition-colors whitespace-nowrap',
              'focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gm-accent',
              isActive
                ? 'bg-gm-paper text-gm-ink shadow-[0_1px_2px_rgba(0,0,0,0.06)]'
                : 'bg-transparent text-gm-inkFaint hover:text-gm-ink',
            ].join(' ')}
          >
            {t(labelKey)}{count !== undefined ? ` ${count}` : ''}
          </button>
        )
      })}
    </div>
  )
}

export default FilterChips
