import { describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import '../i18n'

// Hoist mocks before any imports that transitively touch firebase.ts
const registryMocks = vi.hoisted(() => ({
  useRegistryQuery: vi.fn().mockReturnValue({ data: undefined, isLoading: true }),
  useItemsQuery: vi.fn().mockReturnValue({ data: undefined, isLoading: true }),
}))

vi.mock('../features/registry/useRegistryQuery', () => ({
  useRegistryQuery: registryMocks.useRegistryQuery,
}))
vi.mock('../features/registry/useItemsQuery', () => ({
  useItemsQuery: registryMocks.useItemsQuery,
}))

// Import pages after mocks are registered
import AppRootPage from '../pages/AppRootPage'
import RegistryPage from '../pages/RegistryPage'
import ReReservePage from '../pages/ReReservePage'
import NotFoundPage from '../pages/NotFoundPage'

function renderAt(path: string) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const router = createMemoryRouter(
    [
      { path: '/',                           element: <AppRootPage /> },
      { path: '/registry/:id',               element: <RegistryPage /> },
      { path: '/reservation/:id/re-reserve', element: <ReReservePage /> },
      { path: '*',                           element: <NotFoundPage /> },
    ],
    { initialEntries: [path] },
  )
  return render(
    <QueryClientProvider client={client}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
}

describe('App router', () => {
  it('renders AppRootPage at /', () => {
    renderAt('/')
    expect(screen.getByText('Gift Registry')).toBeInTheDocument()
    expect(screen.getByText('Download for Android')).toBeInTheDocument()
  })

  it('renders RegistryPage route at /registry/:id (route match smoke test)', () => {
    // Smoke test: the /registry/:id route is matched (no wildcard NotFoundPage)
    // RegistryPage renders nav + skeleton content (detailed skeleton test is in RegistryPage.test.tsx)
    renderAt('/registry/abc123')
    // NotFoundPage text should NOT appear (wrong route was NOT matched)
    expect(screen.queryByText('Registry not available')).not.toBeInTheDocument()
  })

  it('renders ReReservePage at /reservation/:id/re-reserve', () => {
    renderAt('/reservation/res-999/re-reserve')
    expect(screen.getByText('Checking your reservation\u2026')).toBeInTheDocument()
  })

  it('renders NotFoundPage at an unknown path', () => {
    renderAt('/totally-not-a-real-path')
    expect(screen.getByText('Registry not available')).toBeInTheDocument()
  })
})
