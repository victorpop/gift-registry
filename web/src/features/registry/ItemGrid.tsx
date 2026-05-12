import type { Item } from '../../lib/firestore-mapping'
import ItemCard from './ItemCard'
import type { ItemFilter } from './FilterChips'

interface Props {
  items: Item[]
  /** Optional render-prop for injecting the real ReserveButton per item. */
  renderReserve?: (item: Item) => React.ReactNode
  /** Active status filter from FilterChips. Defaults to 'all'. */
  filter?: ItemFilter
  /**
   * Optional factory: given an item, returns a click handler to scroll to the
   * ReserveDetailSection anchor — or undefined if the item is not the current viewer's
   * reservation. When provided, reserved-by-me item cards become interactive buttons.
   */
  renderReservedByMeClick?: (item: Item) => (() => void) | undefined
}

function passes(item: Item, filter: ItemFilter): boolean {
  if (filter === 'all') return true
  return item.status === filter
}

export default function ItemGrid({ items, renderReserve, filter = 'all', renderReservedByMeClick }: Props) {
  const visible = items.filter(i => passes(i, filter))
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 lg:gap-5">
      {visible.map((item) => (
        <ItemCard
          key={item.id}
          item={item}
          reserveSlot={renderReserve?.(item)}
          onReservedByMeClick={renderReservedByMeClick?.(item)}
        />
      ))}
    </div>
  )
}
