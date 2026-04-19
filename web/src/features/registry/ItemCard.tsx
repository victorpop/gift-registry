import { useTranslation } from 'react-i18next'
import type { Item, ItemStatus } from '../../lib/firestore-mapping'

interface Props {
  item: Item
  /** Plan 06 injects the real ReserveButton. When null, Plan 04 renders a disabled placeholder. */
  reserveSlot?: React.ReactNode
}

function statusClasses(status: ItemStatus): string {
  switch (status) {
    case 'reserved':
      return 'bg-primary text-primary-on'
    case 'purchased':
      return 'bg-surface-on text-surface'
    case 'available':
    default:
      return 'bg-surface-variant text-surface-on'
  }
}

function statusKey(status: ItemStatus): string {
  switch (status) {
    case 'reserved':  return 'reservation.status_reserved'
    case 'purchased': return 'reservation.status_purchased'
    case 'available':
    default:          return 'reservation.status_available'
  }
}

export default function ItemCard({ item, reserveSlot }: Props) {
  const { t } = useTranslation()
  const priceText = item.price != null && item.currency
    ? `${item.price} ${item.currency}`
    : item.price != null
      ? String(item.price)
      : null

  return (
    <article className="flex gap-4 p-4 rounded-lg bg-surface-variant border border-outline hover:shadow-md transition-shadow">
      {item.imageUrl ? (
        <img
          src={item.imageUrl}
          alt={item.title}
          className="w-24 h-24 rounded-md object-cover bg-outline flex-shrink-0"
        />
      ) : (
        <div className="w-24 h-24 rounded-md bg-outline flex-shrink-0" aria-hidden="true" />
      )}
      <div className="flex-1 flex flex-col gap-2 justify-between">
        <div>
          <div className="flex items-start justify-between gap-2">
            <h2 className="text-base font-semibold text-surface-on leading-tight">
              {item.title}
            </h2>
            <span
              className={`text-sm font-normal px-2 py-1 rounded ${statusClasses(item.status)} flex-shrink-0`}
              data-testid="status-badge"
              data-status={item.status}
            >
              {t(statusKey(item.status))}
            </span>
          </div>
          {priceText && (
            <p className="text-sm font-normal text-surface-onVariant mt-1">{priceText}</p>
          )}
        </div>
        {item.status === 'available' && (
          <div data-testid="reserve-slot">
            {reserveSlot ?? (
              <button
                type="button"
                disabled
                className="min-h-[48px] px-6 rounded-full bg-primary text-primary-on font-semibold text-base opacity-50 cursor-not-allowed"
              >
                {t('reservation.reserve_item')}
              </button>
            )}
          </div>
        )}
      </div>
    </article>
  )
}
