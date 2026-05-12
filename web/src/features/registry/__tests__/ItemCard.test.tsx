import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
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

function renderCard(item: Item, reserveSlot?: React.ReactNode) {
  return render(
    <MemoryRouter>
      <ItemCard item={item} registryId="reg-1" reserveSlot={reserveSlot} />
    </MemoryRouter>
  )
}

describe('ItemCard', () => {
  it('renders title, price, currency, and image alt=title for available item', () => {
    renderCard(makeItem())
    expect(screen.getByText('Coffee Grinder')).toBeInTheDocument()
    // Phase 13: price + currency render in two spans (price body + currency mono).
    // Use the data-testid="price" span and assert its text content.
    const priceEl = screen.getByTestId('price')
    expect(priceEl.textContent).toContain('49.99')
    expect(priceEl.textContent).toContain('RON')
    expect(screen.getByAltText('Coffee Grinder')).toBeInTheDocument()
    // ItemCard body is wrapped in a Link to the detail page.
    expect(screen.getByRole('link', { name: /Coffee Grinder/i })).toHaveAttribute('href', '/registry/reg-1/item/item-1')
  })

  it('shows Available pill (neutral tone, gm-paperDeep bg) for available status', () => {
    renderCard(makeItem({ status: 'available' }))
    // Phase 13: status pill is a <Pill tone="neutral" size="sm"> atom; pill copy
    // is uppercase ("AVAILABLE") via i18n web_pill.available. Assert visible copy
    // + the data-status attribute on the article wrapper.
    expect(screen.getByText('AVAILABLE')).toBeInTheDocument()
    expect(screen.getByTestId('item-card')).toHaveAttribute('data-status', 'available')
  })

  it('shows Reserved pill (accent tone, gm-accentSoft bg) for reserved status', () => {
    renderCard(makeItem({ status: 'reserved' }))
    // Phase 13 D-06: pill copy is "RESERVED" uppercase (no name suffix); also
    // rendered as the in-card banner with "{n} MIN LEFT" copy when expiresAt set.
    expect(screen.getByText('RESERVED')).toBeInTheDocument()
    expect(screen.getByTestId('item-card')).toHaveAttribute('data-status', 'reserved')
  })

  it('shows Purchased pill (ok tone) + opacity-0.55 article for purchased status', () => {
    renderCard(makeItem({ status: 'purchased' }))
    // Phase 13 D-06: pill copy is "✓ PURCHASED" (no giver name).
    expect(screen.getByText(/PURCHASED/)).toBeInTheDocument()
    const card = screen.getByTestId('item-card')
    expect(card).toHaveAttribute('data-status', 'purchased')
    // Outer opacity carries the purchased signal per Phase 13 D-17.
    expect(card.className).toContain('opacity-[0.55]')
  })

  it('renders reserve-slot when status is available', () => {
    renderCard(makeItem({ status: 'available' }))
    expect(screen.getByTestId('reserve-slot')).toBeInTheDocument()
  })

  it('does NOT render reserve-slot when status is reserved', () => {
    renderCard(makeItem({ status: 'reserved' }))
    expect(screen.queryByTestId('reserve-slot')).not.toBeInTheDocument()
  })

  it('does NOT render reserve-slot when status is purchased', () => {
    renderCard(makeItem({ status: 'purchased' }))
    expect(screen.queryByTestId('reserve-slot')).not.toBeInTheDocument()
  })

  it('uses custom reserveSlot when provided (Plan 06 injection)', () => {
    renderCard(makeItem({ status: 'available' }), <button>CustomReserve</button>)
    expect(screen.getByText('CustomReserve')).toBeInTheDocument()
  })

  it('renders price without currency when currency is null', () => {
    renderCard(makeItem({ price: 25, currency: null }))
    const priceEl = screen.getByTestId('price')
    expect(priceEl.textContent).toContain('25')
  })

  it('omits price block when price is null', () => {
    renderCard(makeItem({ price: null, currency: null }))
    expect(screen.queryByText(/RON/)).not.toBeInTheDocument()
    expect(screen.queryByTestId('price')).not.toBeInTheDocument()
  })
})
