import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import '../../../i18n'

// Auth mock — controls isReady flag
const authMock = vi.hoisted(() => ({ useAuth: vi.fn() }))
vi.mock('../../auth/useAuth', () => authMock)

// Resolve mutation mock
const mutateMock = vi.hoisted(() => vi.fn())
const mutationMock = vi.hoisted(() => ({
  mutate: vi.fn(),
  isPending: false,
  isSuccess: false,
  isError: false,
}))
vi.mock('../useResolveReservation', () => ({
  useResolveReservation: () => mutationMock,
}))

import ReReservePage from '../../../pages/ReReservePage'

function renderPage(reservationId: string) {
  const client = new QueryClient()
  const router = createMemoryRouter(
    [
      { path: '/reservation/:id/re-reserve', element: <ReReservePage /> },
      { path: '/registry/:id', element: <div data-testid="registry-page" /> },
      { path: '/', element: <div data-testid="home-page" /> },
    ],
    { initialEntries: [`/reservation/${reservationId}/re-reserve`] },
  )
  return render(
    <QueryClientProvider client={client}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
}

describe('ReReservePage', () => {
  beforeEach(() => {
    mutateMock.mockReset()
    mutationMock.mutate = mutateMock
    mutationMock.isPending = false
    mutationMock.isSuccess = false
    mutationMock.isError = false
    authMock.useAuth.mockReset()
  })

  it('shows "Checking your reservation…" while waiting', () => {
    authMock.useAuth.mockReturnValue({ isReady: false, user: null })
    renderPage('res-1')
    expect(screen.getByText('Checking your reservation\u2026')).toBeInTheDocument()
  })

  it('does NOT call resolveReservation until auth is ready (Pitfall 7)', () => {
    authMock.useAuth.mockReturnValue({ isReady: false, user: null })
    renderPage('res-1')
    expect(mutateMock).not.toHaveBeenCalled()
  })

  it('calls resolveReservation once auth is ready with the reservationId from params', () => {
    authMock.useAuth.mockReturnValue({ isReady: true, user: null })
    renderPage('res-xyz')
    expect(mutateMock).toHaveBeenCalledWith(
      { reservationId: 'res-xyz' },
      expect.any(Object),
    )
  })

  it('navigates to /registry/:rid?autoReserveItemId=:iid on resolveReservation success', async () => {
    authMock.useAuth.mockReturnValue({ isReady: true, user: null })
    mutateMock.mockImplementation((_vars: unknown, opts?: { onSuccess?: (d: unknown) => void }) => {
      opts?.onSuccess?.({ registryId: 'reg-1', itemId: 'item-5', status: 'expired' })
    })
    renderPage('res-1')
    await waitFor(() => expect(screen.getByTestId('registry-page')).toBeInTheDocument())
  })

  it('navigates to / on resolveReservation error', async () => {
    authMock.useAuth.mockReturnValue({ isReady: true, user: null })
    mutateMock.mockImplementation((_vars: unknown, opts?: { onError?: (e: unknown) => void }) => {
      opts?.onError?.({ code: 'not-found' })
    })
    renderPage('res-1')
    await waitFor(() => expect(screen.getByTestId('home-page')).toBeInTheDocument())
  })
})
