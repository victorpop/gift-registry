import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { MemoryRouter, Routes, Route } from 'react-router'
import '../../../i18n'
import ItemDetailPage from '../../../pages/ItemDetailPage'
import type { Item } from '../../../lib/firestore-mapping'

// --- Mocks ---

const mockConfirm = vi.fn(() => Promise.resolve())
vi.mock('../../reservation/useConfirmPurchase', () => ({
  useConfirmPurchase: () => ({ confirm: mockConfirm, status: 'idle', error: null }),
}))

let mockActive: {
  reservationId: string
  itemId: string
  itemName: string
  affiliateUrl: string
  merchantDomain: string | null
  expiresAtMs: number
} | null = null

vi.mock('../../reservation/useActiveReservation', () => ({
  useActiveReservation: () => ({ active: mockActive, set: vi.fn(), clear: vi.fn() }),
}))

let mockItemsData: Item[] | undefined = []

vi.mock('../useItemsQuery', () => ({
  useItemsQuery: () => ({ data: mockItemsData }),
}))

vi.mock('../../reservation/ReserveButton', () => ({
  default: () => <div data-testid="reserve-button-mock" />,
}))

vi.mock('../../../components/ToastProvider', () => ({
  useToast: () => ({ showToast: vi.fn() }),
}))

// TopNav pulls in useAuth → firebase; mock it so no firebase init needed in jsdom
vi.mock('../../../components/giftmaison/TopNav', () => ({
  TopNav: () => <nav data-testid="top-nav" />,
}))

// --- Helpers ---

function makeItem(overrides: Partial<Item> = {}): Item {
  return {
    id: 'item-1',
    title: 'Coffee Grinder',
    imageUrl: null,
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

function renderAt(itemId = 'item-1') {
  return render(
    <MemoryRouter initialEntries={[`/registry/reg-1/item/${itemId}`]}>
      <Routes>
        <Route path="/registry/:id/item/:itemId" element={<ItemDetailPage />} />
        {/* NotFoundPage route so the not-found render works correctly in tests */}
        <Route path="*" element={<div>Not Found Fallback</div>} />
      </Routes>
    </MemoryRouter>,
  )
}

beforeEach(() => {
  mockConfirm.mockClear()
  mockActive = null
  mockItemsData = []
})

// --- Tests ---

describe('ItemDetailPage', () => {
  it('renders loading state when items query has no data yet', () => {
    mockItemsData = undefined
    const { container } = renderAt()
    // Loading placeholder has role="status"
    expect(container.querySelector('[role="status"]')).toBeInTheDocument()
  })

  it('renders NotFoundPage when item is not in the items list', () => {
    mockItemsData = []
    renderAt()
    // NotFoundPage renders the registry.not_found_title key
    expect(screen.getByText('Registry not available')).toBeInTheDocument()
  })

  it('renders title, status pill, affiliate-URL Go-to-retailer link for available item', () => {
    mockItemsData = [makeItem({ affiliateUrl: 'https://store/?aff=1' })]
    renderAt()
    expect(screen.getByText('Coffee Grinder')).toBeInTheDocument()
    expect(screen.getByText('AVAILABLE')).toBeInTheDocument()
    const retailerLink = screen.getByRole('link', { name: /Go to retailer/i })
    expect(retailerLink).toHaveAttribute('href', 'https://store/?aff=1')
    expect(retailerLink).toHaveAttribute('target', '_blank')
    expect(retailerLink.getAttribute('rel')).toContain('noopener')
    expect(retailerLink.getAttribute('rel')).toContain('noreferrer')
  })

  it('renders <ReserveButton> when item.status === "available"', () => {
    mockItemsData = [makeItem({ status: 'available' })]
    renderAt()
    expect(screen.getByTestId('reserve-button-mock')).toBeInTheDocument()
  })

  it('renders Mark-as-purchased CTA when active reservation matches this item', () => {
    mockItemsData = [makeItem({ status: 'reserved' })]
    mockActive = {
      reservationId: 'res-1',
      itemId: 'item-1',
      itemName: 'Coffee Grinder',
      affiliateUrl: 'https://store/?aff=1',
      merchantDomain: 'store',
      expiresAtMs: Date.now() + 1800000,
    }
    renderAt()
    expect(
      screen.getByRole('button', { name: /I completed the purchase/i }),
    ).toBeInTheDocument()
  })

  it('calls useConfirmPurchase.confirm(reservationId) when Mark-as-purchased clicked', () => {
    mockItemsData = [makeItem({ status: 'reserved' })]
    mockActive = {
      reservationId: 'res-1',
      itemId: 'item-1',
      itemName: 'Coffee Grinder',
      affiliateUrl: 'https://store/?aff=1',
      merchantDomain: 'store',
      expiresAtMs: Date.now() + 1800000,
    }
    renderAt()
    const btn = screen.getByRole('button', { name: /I completed the purchase/i })
    fireEvent.click(btn)
    expect(mockConfirm).toHaveBeenCalledOnce()
    expect(mockConfirm).toHaveBeenCalledWith('res-1')
  })

  it('renders read-only reserved banner (no CTA, no name) when item is reserved but active is null', () => {
    const item = makeItem({ status: 'reserved', reservedBy: 'uid-other' })
    mockItemsData = [item]
    mockActive = null
    const { container } = renderAt()
    // Read-only banner text present
    expect(
      screen.getByText(/This item is reserved/i),
    ).toBeInTheDocument()
    // No mark-as-purchased CTA
    expect(
      screen.queryByRole('button', { name: /I completed the purchase/i }),
    ).not.toBeInTheDocument()
    // No ReserveButton
    expect(screen.queryByTestId('reserve-button-mock')).not.toBeInTheDocument()
    // D-06: reservedBy uid must NOT appear anywhere in the DOM
    expect(container.textContent).not.toContain('uid-other')
  })

  it('renders read-only reserved banner when active.itemId !== this item\'s id', () => {
    mockItemsData = [makeItem({ id: 'item-1', status: 'reserved' })]
    mockActive = {
      reservationId: 'res-2',
      itemId: 'item-2',
      itemName: 'Other Item',
      affiliateUrl: 'https://store/?aff=2',
      merchantDomain: 'store',
      expiresAtMs: Date.now() + 1800000,
    }
    renderAt()
    expect(screen.getByText(/This item is reserved/i)).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: /I completed the purchase/i }),
    ).not.toBeInTheDocument()
    expect(screen.queryByTestId('reserve-button-mock')).not.toBeInTheDocument()
  })

  it('renders purchased label (no CTAs except Go-to-retailer + Back) when item.status === "purchased"', () => {
    mockItemsData = [makeItem({ status: 'purchased' })]
    renderAt()
    expect(screen.getByText(/This item has been purchased/i)).toBeInTheDocument()
    // No mark-as-purchased button
    expect(
      screen.queryByRole('button', { name: /I completed the purchase/i }),
    ).not.toBeInTheDocument()
    // No ReserveButton mock
    expect(screen.queryByTestId('reserve-button-mock')).not.toBeInTheDocument()
    // Go-to-retailer link still present
    expect(screen.getByRole('link', { name: /Go to retailer/i })).toBeInTheDocument()
  })

  it('renders Back-to-registry link pointing at /registry/{id}', () => {
    mockItemsData = [makeItem()]
    renderAt()
    const backLink = screen.getByRole('link', { name: /Back to registry/i })
    expect(backLink).toHaveAttribute('href', '/registry/reg-1')
  })

  it('renders item.notes when notes is non-null', () => {
    mockItemsData = [makeItem({ notes: 'Pick the blue one' })]
    renderAt()
    expect(screen.getByText('Pick the blue one')).toBeInTheDocument()
    expect(screen.getByText('Notes')).toBeInTheDocument()
  })

  it('does not render notes block when item.notes is null', () => {
    mockItemsData = [makeItem({ notes: null })]
    renderAt()
    expect(screen.queryByText('Notes')).not.toBeInTheDocument()
  })
})
