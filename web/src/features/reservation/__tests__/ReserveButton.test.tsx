import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import '../../../i18n'
import React from 'react'

// Mock react-router navigate
const mockNavigate = vi.hoisted(() => vi.fn())
vi.mock('react-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router')>()
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  }
})

// Mock dependencies
const authMock = vi.hoisted(() => ({ useAuth: vi.fn() }))
vi.mock('../../auth/useAuth', () => authMock)

const mutateMock = vi.hoisted(() => vi.fn())
const useCreateReservationMock = vi.hoisted(() => vi.fn())
vi.mock('../useCreateReservation', () => ({
  useCreateReservation: useCreateReservationMock,
}))

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

import ReserveButton from '../ReserveButton'
import type { Item } from '../../../lib/firestore-mapping'

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
    affiliateUrl: 'https://emag.ro/?aff=1',
    originalUrl: 'https://emag.ro',
    merchantDomain: 'emag.ro',
    ...overrides,
  }
}

function renderBtn(item: Item = makeItem()) {
  const client = new QueryClient()
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <ReserveButton registryId="reg-1" item={item} />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('ReserveButton', () => {
  beforeEach(() => {
    mutateMock.mockReset()
    mockNavigate.mockReset()
    authMock.useAuth.mockReset()
    useCreateReservationMock.mockReset()
    useCreateReservationMock.mockReturnValue({ mutate: mutateMock, isPending: false })
    toastMock.showToast.mockReset()
    localStorage.clear()
  })

  it('when authenticated, clicking mutates with giverId=uid and skips guest modal', async () => {
    const user = userEvent.setup()
    authMock.useAuth.mockReturnValue({ user: { uid: 'u1', displayName: 'Ana Pop', email: 'ana@x.com' }, isReady: true })
    renderBtn()
    await user.click(screen.getByRole('button', { name: 'Reserve Gift' }))
    expect(mutateMock).toHaveBeenCalledWith({
      registryId: 'reg-1',
      itemId: 'item-1',
      giverName: 'Ana Pop',
      giverEmail: 'ana@x.com',
      giverId: 'u1',
    })
    // No guest modal shown
    expect(screen.queryByText('Who are you?')).not.toBeInTheDocument()
  })

  it('when anonymous, clicking opens GuestIdentityModal and does NOT mutate', async () => {
    const user = userEvent.setup()
    authMock.useAuth.mockReturnValue({ user: null, isReady: true })
    renderBtn()
    await user.click(screen.getByRole('button', { name: 'Reserve Gift' }))
    expect(screen.getByText('Who are you?')).toBeInTheDocument()
    expect(mutateMock).not.toHaveBeenCalled()
  })

  it('after guest modal submits, mutates with giverId=null and concatenated giverName', async () => {
    const user = userEvent.setup()
    authMock.useAuth.mockReturnValue({ user: null, isReady: true })
    renderBtn()
    await user.click(screen.getByRole('button', { name: 'Reserve Gift' }))
    await user.type(screen.getByLabelText('First Name'), 'Ion')
    await user.type(screen.getByLabelText('Last Name'), 'Popescu')
    await user.type(screen.getByLabelText('Email'), 'ion@x.com')
    // The submit button INSIDE the modal is also labelled "Reserve Gift"; it's the second one
    const reserveButtons = screen.getAllByRole('button', { name: 'Reserve Gift' })
    await user.click(reserveButtons[reserveButtons.length - 1])
    await waitFor(() => {
      expect(mutateMock).toHaveBeenCalledWith({
        registryId: 'reg-1',
        itemId: 'item-1',
        giverName: 'Ion Popescu',
        giverEmail: 'ion@x.com',
        giverId: null,
      })
    })
  })

  it('disables the button while mutation is pending', () => {
    authMock.useAuth.mockReturnValue({ user: { uid: 'u1', displayName: 'A B', email: 'a@b.com' }, isReady: true })
    useCreateReservationMock.mockReturnValue({ mutate: mutateMock, isPending: true })
    renderBtn()
    const btn = screen.getByRole('button')
    expect(btn).toBeDisabled()
    expect(btn).toHaveAttribute('aria-busy', 'true')
  })

  it('on successful reserve, navigate is called with /registry/{registryId}/item/{itemId}', async () => {
    authMock.useAuth.mockReturnValue({ user: { uid: 'u1', displayName: 'Ana Pop', email: 'ana@x.com' }, isReady: true })
    // Simulate useCreateReservation calling onSuccess
    useCreateReservationMock.mockImplementation(({ onSuccess }: { onSuccess: (data: unknown) => void }) => ({
      mutate: (payload: unknown) => {
        mutateMock(payload)
        onSuccess({ reservationId: 'res-1', affiliateUrl: 'https://emag.ro/?aff=1', expiresAtMs: 9999999000 })
      },
      isPending: false,
    }))
    const user = userEvent.setup()
    renderBtn()
    await user.click(screen.getByRole('button', { name: 'Reserve Gift' }))
    expect(mockNavigate).toHaveBeenCalledWith('/registry/reg-1/item/item-1')
  })
})
