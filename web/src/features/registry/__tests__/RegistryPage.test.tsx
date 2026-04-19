import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import '../../../i18n'
import type { Registry, Item } from '../../../lib/firestore-mapping'

// Mock the two hooks used by RegistryPage
const mocks = vi.hoisted(() => ({
  useRegistryQuery: vi.fn(),
  useItemsQuery: vi.fn(),
}))
vi.mock('../useRegistryQuery', () => ({ useRegistryQuery: mocks.useRegistryQuery }))
vi.mock('../useItemsQuery', () => ({ useItemsQuery: mocks.useItemsQuery }))

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
    expect(screen.getByText(/Wedding · /)).toBeInTheDocument()
    expect(screen.getByText('Coffee Grinder')).toBeInTheDocument()
  })

  it('renders empty state when registry loads but items is empty array', () => {
    mocks.useRegistryQuery.mockReturnValue({ data: sampleRegistry, isLoading: false })
    mocks.useItemsQuery.mockReturnValue({ data: [], isLoading: false })
    renderPage()
    expect(screen.getByText('Nothing here yet')).toBeInTheDocument()
    expect(screen.getByText("The registry owner hasn't added any gifts yet. Check back later.")).toBeInTheDocument()
  })
})
