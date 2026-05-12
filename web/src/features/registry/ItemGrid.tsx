import type { Item } from '../../lib/firestore-mapping'
import ItemCard from './ItemCard'
import type { ItemFilter } from './FilterChips'

interface Props {
  items: Item[]
  /** Optional render-prop for injecting the real ReserveButton per item. */
  renderReserve?: (item: Item) => React.ReactNode
  /** Active status filter from FilterChips. Defaults to 'all'. */
  filter?: ItemFilter
}

function passes(item: Item, filter: ItemFilter): boolean {
  if (filter === 'all') return true
  return item.status === filter
}

export default function ItemGrid({ items, renderReserve, filter = 'all' }: Props) {
  const visible = items.filter(i => passes(i, filter))
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 lg:gap-5">
      {visible.map((item) => (
        <ItemCard
          key={item.id}
          item={item}
          reserveSlot={renderReserve?.(item)}
        />
      ))}
    </div>
  )
}
