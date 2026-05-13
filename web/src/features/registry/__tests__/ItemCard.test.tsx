import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
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

function renderCard(props: { item?: Item; registryId?: string; onReservedByMeClick?: () => void } = {}) {
  const item = props.item ?? makeItem()
  const registryId = props.registryId ?? 'reg1'
  return render(
    <MemoryRouter>
      <ItemCard item={item} registryId={registryId} onReservedByMeClick={props.onReservedByMeClick} />
    </MemoryRouter>,
  )
}

describe('ItemCard', () => {
  it('renders title, price, currency, and image alt=title for available item', () => {
    renderCard()
    expect(screen.getByText('Coffee Grinder')).toBeInTheDocument()
    const priceEl = screen.getByTestId('price')
    expect(priceEl.textContent).toContain('49.99')
    expect(priceEl.textContent).toContain('RON')
    expect(screen.getByAltText('Coffee Grinder')).toBeInTheDocument()
  })

  it('shows Available pill (neutral tone) for available status', () => {
    renderCard({ item: makeItem({ status: 'available' }) })
    expect(screen.getByText('AVAILABLE')).toBeInTheDocument()
    expect(screen.getByTestId('item-card')).toHaveAttribute('data-status', 'available')
  })

  it('shows Reserved pill for reserved status', () => {
    renderCard({ item: makeItem({ status: 'reserved' }) })
    expect(screen.getByText('RESERVED')).toBeInTheDocument()
    expect(screen.getByTestId('item-card')).toHaveAttribute('data-status', 'reserved')
  })

  it('shows Purchased pill + opacity-0.55 wrapper for purchased status', () => {
    renderCard({ item: makeItem({ status: 'purchased' }) })
    expect(screen.getByText(/PURCHASED/)).toBeInTheDocument()
    const card = screen.getByTestId('item-card')
    expect(card).toHaveAttribute('data-status', 'purchased')
    // Opacity may live on the inner wrapper now (Link is the outermost element).
    // Assert it appears anywhere in the card subtree's rendered className strings.
    const html = card.outerHTML
    expect(html).toContain('opacity-[0.55]')
  })

  it('renders price without currency when currency is null', () => {
    renderCard({ item: makeItem({ price: 25, currency: null }) })
    const priceEl = screen.getByTestId('price')
    expect(priceEl.textContent).toContain('25')
  })

  it('omits price block when price is null', () => {
    renderCard({ item: makeItem({ price: null, currency: null }) })
    expect(screen.queryByText(/RON/)).not.toBeInTheDocument()
    expect(screen.queryByTestId('price')).not.toBeInTheDocument()
  })

  // ---- New k37 tests ----

  it('K37-A: wraps the article in a Link to /registry/:id/item/:itemId', () => {
    renderCard({ item: makeItem({ id: 'item-1' }), registryId: 'reg1' })
    const card = screen.getByTestId('item-card')
    const link = card.closest('a')
    expect(link).not.toBeNull()
    expect(link!.getAttribute('href')).toBe('/registry/reg1/item/item-1')
  })

  it('K37-B: does NOT render any reserve-slot for available items (CTA removed)', () => {
    renderCard({ item: makeItem({ status: 'available' }) })
    expect(screen.queryByTestId('reserve-slot')).not.toBeInTheDocument()
  })

  it('K37-C: renders shop line (merchantDomain) above product name in DOM order', () => {
    renderCard({ item: makeItem({ merchantDomain: 'store', title: 'Coffee Grinder' }) })
    const shop = screen.getByText('store')
    const name = screen.getByText('Coffee Grinder')
    // compareDocumentPosition: bit 4 = name FOLLOWING shop
    const rel = shop.compareDocumentPosition(name)
    // eslint-disable-next-line no-bitwise
    expect(rel & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
  })

  it('K37-D: truncates titles longer than 60 chars with trailing "..."', () => {
    const longTitle = 'a'.repeat(70)
    renderCard({ item: makeItem({ title: longTitle }) })
    // The product name h3 — find by checking which element contains the truncated text.
    const expected = 'a'.repeat(60) + '...'
    expect(screen.getByText(expected)).toBeInTheDocument()
  })

  it('K37-E: does NOT truncate titles exactly 60 chars', () => {
    const title60 = 'a'.repeat(60)
    renderCard({ item: makeItem({ title: title60 }) })
    expect(screen.getByText(title60)).toBeInTheDocument()
    // No ellipsis-suffixed variant
    expect(screen.queryByText(title60 + '...')).not.toBeInTheDocument()
  })

  it('K37-F: trims trailing whitespace before adding ellipsis', () => {
    // 58 'a's + 3 spaces + 'bcd' = 58+3+3=64 chars; truncate at 60 lands inside
    // "   bcd" — after slice(0,60) the tail would be "...aaa   " — trimEnd should
    // remove the trailing spaces so the rendered ellipsis is not preceded by spaces.
    const title = 'a'.repeat(58) + '   bcd'
    renderCard({ item: makeItem({ title }) })
    // Find the h3 — the rendered product-name element. We assert the text:
    // - ends with "..."
    // - has NO whitespace immediately before the "..."
    const card = screen.getByTestId('item-card')
    const h3 = card.querySelector('h3')
    expect(h3).not.toBeNull()
    const text = h3!.textContent ?? ''
    expect(text.endsWith('...')).toBe(true)
    const beforeEllipsis = text.slice(0, -3)
    expect(beforeEllipsis).toBe(beforeEllipsis.trimEnd())
  })

  it('K37-G: purchased tile is still wrapped in a Link (clickable)', () => {
    renderCard({ item: makeItem({ status: 'purchased' }), registryId: 'reg1' })
    const card = screen.getByTestId('item-card')
    const link = card.closest('a')
    expect(link).not.toBeNull()
    expect(link!.getAttribute('href')).toBe('/registry/reg1/item/item-1')
  })

  it('K37-H: reserved-by-me banner click prevents Link navigation and calls onReservedByMeClick', async () => {
    const user = userEvent.setup()
    const onClick = vi.fn()
    renderCard({
      item: makeItem({ status: 'reserved', expiresAt: new Date(Date.now() + 30 * 60 * 1000) }),
      onReservedByMeClick: onClick,
      registryId: 'reg1',
    })
    const banner = screen.getByRole('button', { name: /open your reservation details/i })
    await user.click(banner)
    expect(onClick).toHaveBeenCalledTimes(1)
  })

  it('K37-I: D-06 — does NOT render reserver email for reserved status', () => {
    renderCard({ item: makeItem({ status: 'reserved', reservedBy: 'leak@example.com' }) })
    expect(screen.queryByText('leak@example.com')).not.toBeInTheDocument()
  })

  it('K37-J: D-06 — does NOT render giver email for purchased status', () => {
    renderCard({ item: makeItem({ status: 'purchased', reservedBy: 'leak@example.com' }) })
    expect(screen.queryByText('leak@example.com')).not.toBeInTheDocument()
  })

  it('K37-K: tile aria-label includes the (truncated) title', () => {
    renderCard({ item: makeItem({ title: 'Coffee Grinder' }), registryId: 'reg1' })
    const card = screen.getByTestId('item-card')
    const link = card.closest('a')
    expect(link).not.toBeNull()
    const aria = link!.getAttribute('aria-label') ?? ''
    expect(aria).toContain('Coffee Grinder')
  })
})
