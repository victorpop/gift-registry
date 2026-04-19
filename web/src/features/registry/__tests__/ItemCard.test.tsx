import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import '../../../i18n'
import ItemCard from '../ItemCard'
import type { Item } from '../../../lib/firestore-mapping'

function makeItem(overrides: Partial<Item> = {}): Item {
  return {
    id: 'item-1',
    title: 'Coffee Grinder',
    imageUrl: 'https://example.com/img.jpg',
    price: 49.99,
    currency: 'RON',
    notes: null,
    status: 'available',
    reservedBy: null,
    reservedAt: null,
    expiresAt: null,
    affiliateUrl: 'https://store/?aff=1',
    originalUrl: 'https://store',
    merchantDomain: 'store',
    ...overrides,
  }
}

describe('ItemCard', () => {
  it('renders title, price, and image alt=title for available item', () => {
    render(<ItemCard item={makeItem()} />)
    expect(screen.getByText('Coffee Grinder')).toBeInTheDocument()
    expect(screen.getByText('49.99 RON')).toBeInTheDocument()
    expect(screen.getByAltText('Coffee Grinder')).toBeInTheDocument()
  })

  it('shows Available badge with surface-variant bg for available status', () => {
    render(<ItemCard item={makeItem({ status: 'available' })} />)
    const badge = screen.getByTestId('status-badge')
    expect(badge).toHaveAttribute('data-status', 'available')
    expect(badge).toHaveTextContent('Available')
    expect(badge.className).toContain('bg-surface-variant')
  })

  it('shows Reserved badge with bg-primary for reserved status', () => {
    render(<ItemCard item={makeItem({ status: 'reserved' })} />)
    const badge = screen.getByTestId('status-badge')
    expect(badge).toHaveAttribute('data-status', 'reserved')
    expect(badge).toHaveTextContent('Reserved')
    expect(badge.className).toContain('bg-primary')
  })

  it('shows Purchased badge with bg-surface-on for purchased status', () => {
    render(<ItemCard item={makeItem({ status: 'purchased' })} />)
    const badge = screen.getByTestId('status-badge')
    expect(badge).toHaveAttribute('data-status', 'purchased')
    expect(badge).toHaveTextContent('Purchased')
    expect(badge.className).toContain('bg-surface-on')
  })

  it('renders reserve-slot when status is available', () => {
    render(<ItemCard item={makeItem({ status: 'available' })} />)
    expect(screen.getByTestId('reserve-slot')).toBeInTheDocument()
  })

  it('does NOT render reserve-slot when status is reserved', () => {
    render(<ItemCard item={makeItem({ status: 'reserved' })} />)
    expect(screen.queryByTestId('reserve-slot')).not.toBeInTheDocument()
  })

  it('does NOT render reserve-slot when status is purchased', () => {
    render(<ItemCard item={makeItem({ status: 'purchased' })} />)
    expect(screen.queryByTestId('reserve-slot')).not.toBeInTheDocument()
  })

  it('uses custom reserveSlot when provided (Plan 06 injection)', () => {
    render(<ItemCard item={makeItem({ status: 'available' })} reserveSlot={<button>CustomReserve</button>} />)
    expect(screen.getByText('CustomReserve')).toBeInTheDocument()
  })

  it('renders price without currency when currency is null', () => {
    render(<ItemCard item={makeItem({ price: 25, currency: null })} />)
    expect(screen.getByText('25')).toBeInTheDocument()
  })

  it('omits price block when price is null', () => {
    render(<ItemCard item={makeItem({ price: null, currency: null })} />)
    expect(screen.queryByText(/RON/)).not.toBeInTheDocument()
  })
})
