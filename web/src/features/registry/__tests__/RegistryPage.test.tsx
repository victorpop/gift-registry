import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import '../../../i18n'
import type { Registry, Item } from '../../../lib/firestore-mapping'

// Mock the hooks used by RegistryPage (registry data + auth)
const mocks = vi.hoisted(() => ({
  useRegistryQuery: vi.fn(),
  useItemsQuery: vi.fn(),
}))
vi.mock('../useRegistryQuery', () => ({ useRegistryQuery: mocks.useRegistryQuery }))
vi.mock('../useItemsQuery', () => ({ useItemsQuery: mocks.useItemsQuery }))

// Mock useAuth to avoid firebase/auth initialization
vi.mock('../../auth/useAuth', () => ({
  useAuth: () => ({ user: null, isReady: true }),
}))

// Mock reservation components/hooks so no firebase calls happen in this test
vi.mock('../../reservation/ReserveButton', () => ({
  default: () => <button type="button">Reserve Gift</button>,
}))
vi.mock('../../reservation/ReservationBanner', () => ({
  default: () => null,
}))
vi.mock('../../reservation/ConfirmPurchaseBanner', () => ({
  ConfirmPurchaseBanner: () => null,
}))
vi.mock('../../reservation/useActiveReservation', () => ({
  useActiveReservation: () => ({ active: null, set: vi.fn(), clear: vi.fn() }),
  ActiveReservationProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}))
vi.mock('../../../components/ToastProvider', () => ({
  useToast: () => ({ showToast: vi.fn() }),
  ToastProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}))

// Mock auth modals to avoid firebase/auth dependency
vi.mock('../../auth/AuthModal', () => ({
  default: () => null,
}))
vi.mock('../../auth/GuestIdentityModal', () => ({
  default: () => null,
}))

// Mock useGuestIdentity and useCreateReservation (added by Plan 07 for autoReserve flow)
// These imports transitively load firebase.ts which fails in jsdom without a valid API key
vi.mock('../../auth/useGuestIdentity', () => ({
  useGuestIdentity: () => ({ identity: null, save: vi.fn(), clear: vi.fn() }),
}))
vi.mock('../../reservation/useCreateReservation', () => ({
  useCreateReservation: () => ({ mutate: vi.fn(), isPending: false }),
}))

// j5j: mock useActiveReservationHydration so we can assert what args RegistryPage passes.
// The real hook makes a callable network call when conditions are met — mocking is safer than
// relying on early-return guards. Returns the real hook's shape: { status }.
const hydrationMock = vi.hoisted(() => ({ useActiveReservationHydration: vi.fn(() => ({ status: 'idle' })) }))
vi.mock('../../reservation/useActiveReservationHydration', () => hydrationMock)

import RegistryPage from '../../../pages/RegistryPage'

function renderPage(id: string = 'reg-1') {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const router = createMemoryRouter(
    [{ path: '/registry/:id', element: <RegistryPage /> }],
    { initialEntries: [`/registry/${id}`] },
  )
  return render(
    <QueryClientProvider client={client}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
}

const sampleRegistry: Registry = {
  id: 'reg-1',
  ownerId: 'owner-1',
  name: 'Test Wedding Registry',
  occasionType: 'Wedding',
  eventDate: new Date('2026-06-01T12:00:00Z'),
  eventLocation: 'Bucharest',
  description: null,
  visibility: 'public',
  createdAt: null,
  updatedAt: null,
}

const availableItem: Item = {
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
}

describe('RegistryPage', () => {
  beforeEach(() => {
    hydrationMock.useActiveReservationHydration.mockClear()
  })

  it('renders 6 skeleton cards when registry data is undefined (initial loading)', () => {
    mocks.useRegistryQuery.mockReturnValue({ data: undefined, isLoading: true })
    mocks.useItemsQuery.mockReturnValue({ data: undefined, isLoading: true })
    const { container } = renderPage()
    // SkeletonCard renders as article with aria-hidden=true
    const skeletonCards = container.querySelectorAll('article[aria-hidden="true"]')
    expect(skeletonCards.length).toBe(6)
  })

  it('renders NotFoundPage when registry data is null (permission-denied or not-found)', () => {
    mocks.useRegistryQuery.mockReturnValue({ data: null, isLoading: false })
    mocks.useItemsQuery.mockReturnValue({ data: [], isLoading: false })
    renderPage()
    expect(screen.getByText('Registry not available')).toBeInTheDocument()
  })

  it('renders RegistryHeader + ItemGrid when registry and items load', () => {
    mocks.useRegistryQuery.mockReturnValue({ data: sampleRegistry, isLoading: false })
    mocks.useItemsQuery.mockReturnValue({ data: [availableItem], isLoading: false })
    renderPage()
    expect(screen.getByText('Test Wedding Registry')).toBeInTheDocument()
    // Phase 13: occasion + date split across pills + mono caption (separate DOM nodes).
    // Pill renders the occasion (uppercase) and a separate MonoCaption renders
    // "· {formatted date}". Assert each independently.
    expect(screen.getByText('WEDDING')).toBeInTheDocument()
    expect(screen.getByText(/June 1, 2026/)).toBeInTheDocument()
    expect(screen.getByText('Coffee Grinder')).toBeInTheDocument()
  })

  it('renders empty state when registry loads but items is empty array', () => {
    mocks.useRegistryQuery.mockReturnValue({ data: sampleRegistry, isLoading: false })
    mocks.useItemsQuery.mockReturnValue({ data: [], isLoading: false })
    renderPage()
    expect(screen.getByText('Nothing here yet')).toBeInTheDocument()
    expect(screen.getByText("The registry owner hasn't added any gifts yet. Check back later.")).toBeInTheDocument()
  })

  // ---- quick-260518-j5j: navigate-state propagation override for the post-release race ----

  it('R-NEW-01: (j5j) when location.state.recentReleasedItemId is set, the released item renders as available even if items cache shows reserved', () => {
    mocks.useRegistryQuery.mockReturnValue({ data: sampleRegistry, isLoading: false })
    const reservedItem: Item = {
      ...availableItem,
      status: 'reserved',
      reservedBy: 'someone@x',
      reservedAt: new Date(),
      expiresAt: new Date(Date.now() + 30 * 60 * 1000),
    }
    mocks.useItemsQuery.mockReturnValue({ data: [reservedItem], isLoading: false })

    // Render with location.state carrying the released item id.
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    const router = createMemoryRouter(
      [{ path: '/registry/:id', element: <RegistryPage /> }],
      { initialEntries: [{ pathname: '/registry/reg-1', state: { recentReleasedItemId: 'item-1' } }] },
    )
    render(
      <QueryClientProvider client={client}>
        <RouterProvider router={router} />
      </QueryClientProvider>,
    )

    // ItemCard exposes `data-status={item.status}` on its outermost Link — use that for a
    // robust assertion (avoids coupling to en.json literals).
    expect(screen.getByText('Coffee Grinder')).toBeInTheDocument()
    const card = screen.getByTestId('item-card')
    expect(card.getAttribute('data-status')).toBe('available')
  })

  it('R-NEW-02: (j5j) without location.state, items render with their cache shape unchanged', () => {
    mocks.useRegistryQuery.mockReturnValue({ data: sampleRegistry, isLoading: false })
    const reservedItem: Item = {
      ...availableItem,
      status: 'reserved',
      reservedBy: 'someone@x',
      reservedAt: new Date(),
      expiresAt: new Date(Date.now() + 30 * 60 * 1000),
    }
    mocks.useItemsQuery.mockReturnValue({ data: [reservedItem], isLoading: false })

    renderPage() // existing helper — no initial state

    // Reserved item should render with its real status (NOT overridden).
    expect(screen.getByText('Coffee Grinder')).toBeInTheDocument()
    const card = screen.getByTestId('item-card')
    expect(card.getAttribute('data-status')).toBe('reserved')
  })

  it('R-NEW-03: (j5j) RegistryPage passes ignoreReservationId to useActiveReservationHydration when nav state carries it', () => {
    mocks.useRegistryQuery.mockReturnValue({ data: sampleRegistry, isLoading: false })
    mocks.useItemsQuery.mockReturnValue({ data: [availableItem], isLoading: false })

    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    const router = createMemoryRouter(
      [{ path: '/registry/:id', element: <RegistryPage /> }],
      { initialEntries: [{ pathname: '/registry/reg-1', state: { recentReleasedReservationId: 'res-abc' } }] },
    )
    render(
      <QueryClientProvider client={client}>
        <RouterProvider router={router} />
      </QueryClientProvider>,
    )

    expect(hydrationMock.useActiveReservationHydration).toHaveBeenCalled()
    const callArgs = hydrationMock.useActiveReservationHydration.mock.calls[0]
    expect(callArgs[0]).toBe('reg-1')
    expect(callArgs[1]).toEqual({ ignoreReservationId: 'res-abc' })
  })
})
