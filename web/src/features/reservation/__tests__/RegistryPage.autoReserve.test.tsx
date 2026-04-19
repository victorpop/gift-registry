import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import '../../../i18n'
import type { Registry, Item } from '../../../lib/firestore-mapping'

const queryHooksMock = vi.hoisted(() => ({
  useRegistryQuery: vi.fn(),
  useItemsQuery: vi.fn(),
}))
vi.mock('../../registry/useRegistryQuery', () => ({ useRegistryQuery: queryHooksMock.useRegistryQuery }))
vi.mock('../../registry/useItemsQuery', () => ({ useItemsQuery: queryHooksMock.useItemsQuery }))

const authMock = vi.hoisted(() => ({ useAuth: vi.fn() }))
vi.mock('../../auth/useAuth', () => authMock)

const guestIdMock = vi.hoisted(() => ({ useGuestIdentity: vi.fn() }))
vi.mock('../../auth/useGuestIdentity', () => guestIdMock)

const autoMutate = vi.hoisted(() => vi.fn())
const useCreateResMock = vi.hoisted(() => vi.fn())
vi.mock('../useCreateReservation', () => ({ useCreateReservation: useCreateResMock }))

const toastMock = vi.hoisted(() => ({ showToast: vi.fn() }))
vi.mock('../../../components/ToastProvider', () => ({
  useToast: () => toastMock,
  ToastProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}))

const activeMock = vi.hoisted(() => ({ active: null as unknown, set: vi.fn(), clear: vi.fn() }))
vi.mock('../useActiveReservation', () => ({
  useActiveReservation: () => activeMock,
  ActiveReservationProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}))

// Mock sub-components that import firebase
vi.mock('../../reservation/ReserveButton', () => ({
  default: () => <button type="button">Reserve</button>,
}))
vi.mock('../../reservation/ReservationBanner', () => ({
  default: () => null,
}))
vi.mock('../../reservation/ConfirmPurchaseBanner', () => ({
  ConfirmPurchaseBanner: () => null,
}))
vi.mock('../../auth/AuthModal', () => ({
  default: ({ open }: { open: boolean }) => open ? <div data-testid="auth-modal" /> : null,
}))
vi.mock('../../auth/GuestIdentityModal', () => ({
  default: ({ open, onSubmit }: { open: boolean; onSubmit?: (g: unknown) => void }) =>
    open ? (
      <div data-testid="guest-modal">
        <span>Who are you?</span>
        <button
          type="button"
          data-testid="guest-submit"
          onClick={() => onSubmit?.({ firstName: 'Ion', lastName: 'Pop', email: 'ion@x.com' })}
        >
          Submit
        </button>
      </div>
    ) : null,
}))

import RegistryPage from '../../../pages/RegistryPage'

const registry: Registry = {
  id: 'reg-1',
  ownerId: 'owner-1',
  name: 'Test',
  occasionType: 'Wedding',
  eventDate: null,
  eventLocation: null,
  description: null,
  visibility: 'public',
  createdAt: null,
  updatedAt: null,
}

function makeItem(overrides: Partial<Item> = {}): Item {
  return {
    id: 'item-1',
    title: 'Coffee',
    imageUrl: null,
    price: null,
    currency: null,
    notes: null,
    status: 'available',
    reservedBy: null,
    reservedAt: null,
    expiresAt: null,
    affiliateUrl: 'https://x/?aff=1',
    originalUrl: 'https://x',
    merchantDomain: 'x',
    ...overrides,
  }
}

function renderAt(url: string) {
  const client = new QueryClient()
  const router = createMemoryRouter(
    [{ path: '/registry/:id', element: <RegistryPage /> }],
    { initialEntries: [url] },
  )
  return render(
    <QueryClientProvider client={client}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
}

describe('RegistryPage auto-reserve via ?autoReserveItemId=', () => {
  beforeEach(() => {
    autoMutate.mockReset()
    toastMock.showToast.mockReset()
    activeMock.set.mockReset()
    useCreateResMock.mockReset()
    useCreateResMock.mockReturnValue({ mutate: autoMutate, isPending: false })
    queryHooksMock.useRegistryQuery.mockReset()
    queryHooksMock.useItemsQuery.mockReset()
    authMock.useAuth.mockReset()
    guestIdMock.useGuestIdentity.mockReset()
    guestIdMock.useGuestIdentity.mockReturnValue({ identity: null, save: vi.fn(), clear: vi.fn() })
  })

  it('auto-fires createReservation for authenticated user when item is available', async () => {
    queryHooksMock.useRegistryQuery.mockReturnValue({ data: registry })
    queryHooksMock.useItemsQuery.mockReturnValue({ data: [makeItem()] })
    authMock.useAuth.mockReturnValue({ user: { uid: 'u1', displayName: 'Ana', email: 'ana@x.com' }, isReady: true })
    renderAt('/registry/reg-1?autoReserveItemId=item-1')
    await waitFor(() => {
      expect(autoMutate).toHaveBeenCalledWith(
        expect.objectContaining({
          registryId: 'reg-1',
          itemId: 'item-1',
          giverId: 'u1',
          giverEmail: 'ana@x.com',
        }),
      )
    })
  })

  it('shows conflict toast and does NOT mutate when item is not available', async () => {
    queryHooksMock.useRegistryQuery.mockReturnValue({ data: registry })
    queryHooksMock.useItemsQuery.mockReturnValue({ data: [makeItem({ status: 'reserved' })] })
    authMock.useAuth.mockReturnValue({ user: { uid: 'u1', email: 'a@b.com' }, isReady: true })
    renderAt('/registry/reg-1?autoReserveItemId=item-1')
    await waitFor(() => expect(toastMock.showToast).toHaveBeenCalled())
    const [title] = toastMock.showToast.mock.calls[0]
    expect(title).toBe('Someone just reserved this gift. Try another.')
    expect(autoMutate).not.toHaveBeenCalled()
  })

  it('shows conflict toast when referenced item does not exist', async () => {
    queryHooksMock.useRegistryQuery.mockReturnValue({ data: registry })
    queryHooksMock.useItemsQuery.mockReturnValue({ data: [makeItem({ id: 'other' })] })
    authMock.useAuth.mockReturnValue({ user: { uid: 'u1', email: 'a@b.com' }, isReady: true })
    renderAt('/registry/reg-1?autoReserveItemId=ghost')
    await waitFor(() => {
      expect(toastMock.showToast).toHaveBeenCalledWith('Someone just reserved this gift. Try another.', 'error')
    })
    expect(autoMutate).not.toHaveBeenCalled()
  })

  it('auto-fires using stored guest identity for anonymous user', async () => {
    guestIdMock.useGuestIdentity.mockReturnValue({
      identity: { firstName: 'Ion', lastName: 'Pop', email: 'ion@x.com' },
      save: vi.fn(),
      clear: vi.fn(),
    })
    queryHooksMock.useRegistryQuery.mockReturnValue({ data: registry })
    queryHooksMock.useItemsQuery.mockReturnValue({ data: [makeItem()] })
    authMock.useAuth.mockReturnValue({ user: null, isReady: true })
    renderAt('/registry/reg-1?autoReserveItemId=item-1')
    await waitFor(() => {
      expect(autoMutate).toHaveBeenCalledWith(
        expect.objectContaining({
          giverId: null,
          giverName: 'Ion Pop',
          giverEmail: 'ion@x.com',
        }),
      )
    })
  })

  it('opens GuestIdentityModal for anonymous user without stored identity', async () => {
    queryHooksMock.useRegistryQuery.mockReturnValue({ data: registry })
    queryHooksMock.useItemsQuery.mockReturnValue({ data: [makeItem()] })
    authMock.useAuth.mockReturnValue({ user: null, isReady: true })
    renderAt('/registry/reg-1?autoReserveItemId=item-1')
    await waitFor(() => {
      expect(screen.getByText('Who are you?')).toBeInTheDocument()
    })
    expect(autoMutate).not.toHaveBeenCalled()
  })

  it('auto-fires only once across multiple re-renders', async () => {
    queryHooksMock.useRegistryQuery.mockReturnValue({ data: registry })
    queryHooksMock.useItemsQuery.mockReturnValue({ data: [makeItem()] })
    authMock.useAuth.mockReturnValue({ user: { uid: 'u1', email: 'a@b.com' }, isReady: true })
    renderAt('/registry/reg-1?autoReserveItemId=item-1')
    await waitFor(() => expect(autoMutate).toHaveBeenCalledTimes(1))
    // Trigger a re-render by returning a new array reference from the items query
    queryHooksMock.useItemsQuery.mockReturnValue({ data: [makeItem()] })
    // Wait a tick; autoMutate should still have been called exactly once (the ref gates it)
    await new Promise(resolve => setTimeout(resolve, 50))
    expect(autoMutate).toHaveBeenCalledTimes(1)
  })
})
