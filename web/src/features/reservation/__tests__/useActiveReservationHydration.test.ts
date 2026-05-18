/**
 * Tests for useActiveReservationHydration's new optional `ignoreReservationId` option
 * (quick-260518-j5j). Spec IDs: H-NEW-01, H-NEW-02.
 */
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

// Mock firebase/functions: httpsCallable returns a function whose call returns
// a Promise the test controls per-case.
const callableMock = vi.hoisted(() => vi.fn())
vi.mock('firebase/functions', () => ({
  httpsCallable: () => callableMock,
}))
vi.mock('../../../firebase', () => ({
  functions: {},
}))

const authMock = vi.hoisted(() => ({ useAuth: vi.fn() }))
vi.mock('../../auth/useAuth', () => authMock)

const guestMock = vi.hoisted(() => ({ useGuestIdentity: vi.fn() }))
vi.mock('../../auth/useGuestIdentity', () => guestMock)

const activeMock = vi.hoisted(() => ({ active: null as unknown, set: vi.fn(), clear: vi.fn() }))
vi.mock('../useActiveReservation', () => ({
  useActiveReservation: () => activeMock,
}))

import { useActiveReservationHydration } from '../useActiveReservationHydration'

const RESERVATION = {
  reservationId: 'r1',
  itemId: 'it1',
  itemName: 'X',
  affiliateUrl: 'https://x',
  merchantDomain: null,
  expiresAtMs: Date.now() + 30 * 60 * 1000,
}

describe('useActiveReservationHydration (j5j ignoreReservationId)', () => {
  beforeEach(() => {
    callableMock.mockReset()
    activeMock.set = vi.fn()
    activeMock.active = null
    authMock.useAuth.mockReturnValue({ user: { uid: 'u1', email: 'u1@x.com' }, isReady: true })
    guestMock.useGuestIdentity.mockReturnValue({ identity: null })
  })

  it('H-NEW-01: (j5j) when options.ignoreReservationId matches the returned active reservationId, setStatus("empty") fires and set() is NOT called', async () => {
    callableMock.mockResolvedValueOnce({ data: { active: RESERVATION } })

    // @ts-expect-error j5j: 2nd arg not yet implemented in source — removed in GREEN
    const { result } = renderHook(() =>
      useActiveReservationHydration('reg1', { ignoreReservationId: 'r1' }),
    )

    await waitFor(() => {
      expect(result.current.status).toBe('empty')
    })
    expect(activeMock.set).not.toHaveBeenCalled()
  })

  it('H-NEW-02: (j5j) when options.ignoreReservationId does NOT match the returned active reservationId, set() IS called and status transitions to "hydrated"', async () => {
    callableMock.mockResolvedValueOnce({ data: { active: RESERVATION } })

    // @ts-expect-error j5j: 2nd arg not yet implemented in source — removed in GREEN
    const { result } = renderHook(() =>
      useActiveReservationHydration('reg1', { ignoreReservationId: 'OTHER-id' }),
    )

    await waitFor(() => {
      expect(result.current.status).toBe('hydrated')
    })
    expect(activeMock.set).toHaveBeenCalledWith(RESERVATION)
  })
})
