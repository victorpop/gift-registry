/**
 * Tests for StickyReserveBanner (quick-260513-i6l).
 * Asserts the Continue-to-retailer anchor is hidden when active.affiliateUrl is empty.
 */
import React from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import '../../../i18n'

const activeMock = vi.hoisted(() => ({
  active: null as null | {
    reservationId: string
    itemId: string
    itemName: string
    affiliateUrl: string
    merchantDomain: string | null
    expiresAtMs: number
  },
  set: vi.fn(),
  clear: vi.fn(),
}))
vi.mock('../useActiveReservation', () => ({
  useActiveReservation: () => activeMock,
  ActiveReservationProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}))

const countdownMock = vi.hoisted(() => ({
  totalSeconds: 30 * 60,
  minutes: 30,
  seconds: 0,
  expired: false,
}))
vi.mock('../useCountdown', () => ({
  useCountdown: () => countdownMock,
}))

const releaseMock = vi.hoisted(() => ({
  release: vi.fn(),
  status: 'idle' as string,
  error: null as string | null,
}))
vi.mock('../useReleaseReservation', () => ({
  useReleaseReservation: () => releaseMock,
}))

const authMock = vi.hoisted(() => ({ useAuth: vi.fn() }))
vi.mock('../../auth/useAuth', () => authMock)

const guestMock = vi.hoisted(() => ({ useGuestIdentity: vi.fn() }))
vi.mock('../../auth/useGuestIdentity', () => guestMock)

const toastMock = vi.hoisted(() => ({ showToast: vi.fn() }))
vi.mock('../../../components/ToastProvider', () => ({
  useToast: () => toastMock,
  ToastProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}))

import StickyReserveBanner from '../StickyReserveBanner'

describe('StickyReserveBanner — Continue-to-retailer visibility', () => {
  beforeEach(() => {
    toastMock.showToast.mockReset()
    releaseMock.release.mockReset()
    releaseMock.status = 'idle'
    releaseMock.error = null
    authMock.useAuth.mockReturnValue({ user: { uid: 'u1', email: 'u1@x.com' }, isReady: true })
    guestMock.useGuestIdentity.mockReturnValue({ identity: null })
    countdownMock.totalSeconds = 30 * 60
    countdownMock.minutes = 30
    countdownMock.seconds = 0
    countdownMock.expired = false
  })

  it('renders Continue-to-retailer link when active.affiliateUrl is non-empty', () => {
    activeMock.active = {
      reservationId: 'res1',
      itemId: 'it1',
      itemName: 'Coffee Machine',
      affiliateUrl: 'https://emag.ro/item1',
      merchantDomain: 'emag.ro',
      expiresAtMs: Date.now() + 30 * 60 * 1000,
    }
    render(<StickyReserveBanner />)
    const link = screen.getByRole('link', { name: /continue/i })
    expect(link).toHaveAttribute('href', 'https://emag.ro/item1')
    expect(screen.getByRole('button', { name: /release/i })).toBeInTheDocument()
  })

  it('hides Continue-to-retailer link when active.affiliateUrl is empty, keeps Release button', () => {
    activeMock.active = {
      reservationId: 'res1',
      itemId: 'it1',
      itemName: 'Coffee Machine',
      affiliateUrl: '',
      merchantDomain: 'ikea.com',
      expiresAtMs: Date.now() + 30 * 60 * 1000,
    }
    render(<StickyReserveBanner />)
    expect(screen.queryByRole('link', { name: /continue/i })).toBeNull()
    expect(screen.getByRole('button', { name: /release/i })).toBeInTheDocument()
    expect(screen.getByTestId('sticky-reserve-banner')).toBeInTheDocument()
  })
})
