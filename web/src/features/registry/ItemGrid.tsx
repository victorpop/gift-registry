import type { Item } from '../../lib/firestore-mapping'
import ItemCard from './ItemCard'

interface Props {
  items: Item[]
  /**
   * Optional render-prop for injecting the real ReserveButton per item (Plan 06 wires this).
   * When omitted, ItemCard renders a disabled placeholder for available items.
   */
  renderReserve?: (item: Item) => React.ReactNode
}

export default function ItemGrid({ items, renderReserve }: Props) {
  return (
    <div className="max-w-2xl mx-auto px-4 pb-16 grid grid-cols-1 md:grid-cols-2 gap-4">
      {items.map((item) => (
        <ItemCard
          key={item.id}
          item={item}
          reserveSlot={renderReserve?.(item)}
        />
      ))}
    </div>
  )
}
