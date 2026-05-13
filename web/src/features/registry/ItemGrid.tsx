import type { Item } from '../../lib/firestore-mapping'
import ItemCard from './ItemCard'
import type { ItemFilter } from './FilterChips'

interface Props {
  items: Item[]
  /** Required: forwarded to each ItemCard so the tile Link can construct its href. */
  registryId: string
  /** Active status filter from FilterChips. Defaults to 'all'. */
  filter?: ItemFilter
  /**
   * Optional factory: given an item, returns a click handler to navigate to the
   * per-item reserve-detail page — or undefined if the item is not the current
   * viewer's reservation. When provided, reserved-by-me item cards' in-card
   * banner becomes an interactive button.
   */
  renderReservedByMeClick?: (item: Item) => (() => void) | undefined
}

function passes(item: Item, filter: ItemFilter): boolean {
  if (filter === 'all') return true
  return item.status === filter
}

export default function ItemGrid({ items, registryId, filter = 'all', renderReservedByMeClick }: Props) {
  const visible = items.filter(i => passes(i, filter))
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 lg:gap-5">
      {visible.map((item) => (
        <ItemCard
          key={item.id}
          item={item}
          registryId={registryId}
          onReservedByMeClick={renderReservedByMeClick?.(item)}
        />
      ))}
    </div>
  )
}
