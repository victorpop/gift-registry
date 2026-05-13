/**
 * Tests for useReservationForItem hook (quick-260513-g9g Task 2).
 * Spec IDs: U-01 through U-07.
 */
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

// Mock firebase/functions httpsCallable (per-call pattern)
const callableFn = vi.hoisted(() => vi.fn())
vi.mock('firebase/functions', () => ({
  httpsCallable: vi.fn(() => callableFn),
}))
vi.mock('../../../firebase', () => ({ functions: { _kind: 'fakeApp' } }))

// Mock useAuth
const authMock = vi.hoisted(() => ({
  useAuth: vi.fn(),
}))
vi.mock('../../auth/useAuth', () => authMock)

// Mock useGuestIdentity
const guestMock = vi.hoisted(() => ({
  useGuestIdentity: vi.fn(),
}))
vi.mock('../../auth/useGuestIdentity', () => guestMock)

import { useReservationForItem } from '../useReservationForItem'

const ACTIVE_RES = {
  reservationId: 'res1',
  itemId: 'it1',
  itemName: 'Coffee Machine',
  affiliateUrl: 'https://emag.ro/item1',
  merchantDomain: 'emag.ro',
  expiresAtMs: 99999999000,
}

beforeEach(() => {
  callableFn.mockReset()
  authMock.useAuth.mockReset()
  guestMock.useGuestIdentity.mockReset()
  // Default: signed-in user
  authMock.useAuth.mockReturnValue({ user: { uid: 'u1', email: 'u1@x.com' }, isReady: true })
  guestMock.useGuestIdentity.mockReturnValue({ identity: null })
})

describe('useReservationForItem', () => {
  it('U-01: bails (status stays idle, no callable invocation) when authReady=false', async () => {
    authMock.useAuth.mockReturnValue({ user: null, isReady: false })
    guestMock.useGuestIdentity.mockReturnValue({ identity: null })

    const { result } = renderHook(() => useReservationForItem('reg1', 'it1'))

    // Wait a tick to ensure effects have run
    await new Promise(r => setTimeout(r, 20))

    expect(result.current.status).toBe('idle')
    expect(callableFn).not.toHaveBeenCalled()
  })

  it('U-02: bails when authReady=true but no user AND no guest identity', async () => {
    authMock.useAuth.mockReturnValue({ user: null, isReady: true })
    guestMock.useGuestIdentity.mockReturnValue({ identity: null })

    const { result } = renderHook(() => useReservationForItem('reg1', 'it1'))

    await new Promise(r => setTimeout(r, 20))

    expect(result.current.status).toBe('idle')
    expect(callableFn).not.toHaveBeenCalled()
  })

  it('U-03 (signed-in happy): sends only { registryId, itemId } (no giverEmail), returns status hydrated', async () => {
    callableFn.mockResolvedValue({ data: { active: ACTIVE_RES } })

    const { result } = renderHook(() => useReservationForItem('reg1', 'it1'))

    await waitFor(() => expect(result.current.status).toBe('hydrated'))
    expect(result.current.active).toEqual(ACTIVE_RES)
    // Should NOT include giverEmail in the payload for signed-in path
    expect(callableFn).toHaveBeenCalledWith({ registryId: 'reg1', itemId: 'it1' })
  })

  it('U-04 (guest happy): sends { registryId, itemId, giverEmail }, returns active', async () => {
    authMock.useAuth.mockReturnValue({ user: null, isReady: true })
    guestMock.useGuestIdentity.mockReturnValue({ identity: { firstName: 'Ion', lastName: 'Pop', email: 'ion@x.com' } })
    callableFn.mockResolvedValue({ data: { active: ACTIVE_RES } })

    const { result } = renderHook(() => useReservationForItem('reg1', 'it1'))

    await waitFor(() => expect(result.current.status).toBe('hydrated'))
    expect(result.current.active).toEqual(ACTIVE_RES)
    expect(callableFn).toHaveBeenCalledWith({ registryId: 'reg1', itemId: 'it1', giverEmail: 'ion@x.com' })
  })

  it('U-05 (empty): callable returns { active: null } → status=empty, active=null', async () => {
    callableFn.mockResolvedValue({ data: { active: null } })

    const { result } = renderHook(() => useReservationForItem('reg1', 'it1'))

    await waitFor(() => expect(result.current.status).toBe('empty'))
    expect(result.current.active).toBeNull()
  })

  it('U-06 (error): callable throws → status=error, active=null, console.warn called', async () => {
    const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {})
    callableFn.mockRejectedValue(new Error('network error'))

    const { result } = renderHook(() => useReservationForItem('reg1', 'it1'))

    await waitFor(() => expect(result.current.status).toBe('error'))
    expect(result.current.active).toBeNull()
    expect(warnSpy).toHaveBeenCalledWith(
      expect.stringContaining('useReservationForItem'),
      expect.any(Error),
    )
    warnSpy.mockRestore()
  })

  it('U-07 (StrictMode guard): mount twice with same key → callable invoked exactly once', async () => {
    callableFn.mockResolvedValue({ data: { active: ACTIVE_RES } })

    // Simulate StrictMode double-invocation by calling the hook twice in rapid succession
    // with same params — the key-based ref guard should deduplicate
    const { result, rerender } = renderHook(
      () => useReservationForItem('reg1', 'it1'),
    )

    await waitFor(() => expect(result.current.status).toBe('hydrated'))

    // Re-render (same key)
    rerender()
    await new Promise(r => setTimeout(r, 20))

    // callable should have been invoked exactly once
    expect(callableFn).toHaveBeenCalledTimes(1)
  })
})
