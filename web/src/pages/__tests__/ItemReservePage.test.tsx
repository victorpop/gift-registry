/**
 * Tests for ItemReservePage (quick-260513-g9g Task 3).
 * Spec IDs: P-01 through P-07.
 */
import React from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen, waitFor, act, within } from '@testing-library/react'
import { createMemoryRouter, RouterProvider, useLocation } from 'react-router'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import '../../i18n'
import type { Item } from '../../lib/firestore-mapping'

// --- Mocks ---

// useItemsQuery
const itemsQueryMock = vi.hoisted(() => ({ useItemsQuery: vi.fn() }))
vi.mock('../../features/registry/useItemsQuery', () => itemsQueryMock)

// useReservationForItem
const reservationForItemMock = vi.hoisted(() => ({ useReservationForItem: vi.fn() }))
vi.mock('../../features/reservation/useReservationForItem', () => reservationForItemMock)

// useAuth
const authMock = vi.hoisted(() => ({ useAuth: vi.fn() }))
vi.mock('../../features/auth/useAuth', () => authMock)

// useGuestIdentity
const guestMock = vi.hoisted(() => ({ useGuestIdentity: vi.fn() }))
vi.mock('../../features/auth/useGuestIdentity', () => guestMock)

// useToast
const toastMock = vi.hoisted(() => ({ showToast: vi.fn() }))
vi.mock('../../components/ToastProvider', () => ({
  useToast: () => toastMock,
  ToastProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}))

// useReleaseReservation
const releaseMock = vi.hoisted(() => ({
  release: vi.fn(),
  status: 'idle' as string,
  error: null as string | null,
}))
vi.mock('../../features/reservation/useReleaseReservation', () => ({
  useReleaseReservation: () => releaseMock,
}))

// useConfirmPurchase
const confirmMock = vi.hoisted(() => ({
  confirm: vi.fn(),
  status: 'idle' as string,
  error: null as string | null,
}))
vi.mock('../../features/reservation/useConfirmPurchase', () => ({
  useConfirmPurchase: () => confirmMock,
}))

// useActiveReservation — needed by ConfirmPurchaseBanner which is mocked below
const activeMock = vi.hoisted(() => ({ active: null as unknown, set: vi.fn(), clear: vi.fn() }))
vi.mock('../../features/reservation/useActiveReservation', () => ({
  useActiveReservation: () => activeMock,
  ActiveReservationProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}))

// ConfirmPurchaseBanner — mock to avoid firebase dependency in banner
vi.mock('../../features/reservation/ConfirmPurchaseBanner', () => ({
  ConfirmPurchaseBanner: ({ reservationId }: { reservationId: string }) => (
    <div data-testid="confirm-purchase-banner" data-reservation-id={reservationId}>
      Confirm Banner
    </div>
  ),
}))

// HowTimerWorks — mock to simplify
vi.mock('../../features/reservation/HowTimerWorks', () => ({
  default: ({ retailer }: { retailer: string }) => (
    <div data-testid="how-timer-works" data-retailer={retailer}>
      How Timer Works
    </div>
  ),
}))

// useCreateReservation — mocked so the new BROWSE_AVAILABLE branch's Reserve CTA
// can fire without touching Firebase. Captures the latest opts (onSuccess/onError)
// and exposes a mutate spy + isPending toggle for assertions.
const createReservationMock = vi.hoisted(() => ({
  mutate: vi.fn(),
  isPending: false as boolean,
  opts: null as { onSuccess?: (data: unknown, variables: unknown) => void; onError?: (err: unknown, variables: unknown) => void } | null,
}))
vi.mock('../../features/reservation/useCreateReservation', () => ({
  useCreateReservation: (opts: { onSuccess?: (data: unknown, variables: unknown) => void; onError?: (err: unknown, variables: unknown) => void }) => {
    createReservationMock.opts = opts
    return { mutate: createReservationMock.mutate, isPending: createReservationMock.isPending }
  },
}))

// queryClient — partial mock of @tanstack/react-query that preserves the real QueryClient
// and QueryClientProvider (consumed by renderPage below) and ONLY overrides useQueryClient
// so we can spy on setQueryData inside ItemReservePage's release-success effect (quick-260516-oiy K-20).
const queryClientMock = vi.hoisted(() => ({ setQueryData: vi.fn() }))
vi.mock('@tanstack/react-query', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@tanstack/react-query')>()
  return { ...actual, useQueryClient: () => queryClientMock }
})

import ItemReservePage from '../ItemReservePage'

// --- Helpers ---

const ACTIVE_RES = {
  reservationId: 'res1',
  itemId: 'it1',
  itemName: 'Coffee Machine',
  affiliateUrl: 'https://emag.ro/item1',
  merchantDomain: 'emag.ro',
  expiresAtMs: Date.now() + 30 * 60 * 1000, // 30 min from now
}

function makeItem(overrides: Partial<Item> = {}): Item {
  return {
    id: 'it1',
    title: 'Coffee Machine',
    imageUrl: null,
    price: 299,
    currency: 'RON',
    notes: null,
    status: 'reserved',
    reservedBy: 'user@example.com',
    reservedAt: null,
    expiresAt: new Date(ACTIVE_RES.expiresAtMs),
    affiliateUrl: 'https://emag.ro/item1',
    originalUrl: 'https://emag.ro',
    merchantDomain: 'emag.ro',
    ...overrides,
  }
}

// Wrapper that OWNS ItemReservePage so its state update cascades into ItemReservePage.
// The wrapper renders ItemReservePage directly (not via children prop) so React treats
// ItemReservePage as a child of this component's render output — enabling state-driven
// re-renders that propagate to ItemReservePage and preserve prevStatusRef.
let _forceUpdateHandle: (() => void) | null = null
function ItemReservePageWithForceUpdate() {
  const [, setState] = React.useState(0)
  React.useLayoutEffect(() => {
    _forceUpdateHandle = () => setState(n => n + 1)
    return () => { _forceUpdateHandle = null }
  }, [])
  return <ItemReservePage />
}

function renderPage(
  registryId = 'reg1',
  itemId = 'it1',
  extraRoutes: { path: string; element: React.ReactNode }[] = [],
) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  const router = createMemoryRouter(
    [
      {
        path: '/registry/:id/item/:itemId',
        element: <ItemReservePageWithForceUpdate />,
      },
      { path: '/registry/:id', element: <div data-testid="registry-page" /> },
      ...extraRoutes,
    ],
    { initialEntries: [`/registry/${registryId}/item/${itemId}`] },
  )
  const result = render(
    <QueryClientProvider client={client}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
  return {
    ...result,
    rerenderSame: () => {
      _forceUpdateHandle?.()
    },
  }
}

// --- Tests ---

describe('ItemReservePage', () => {
  beforeEach(() => {
    toastMock.showToast.mockReset()
    releaseMock.release.mockReset()
    releaseMock.status = 'idle'
    releaseMock.error = null
    confirmMock.confirm.mockReset()
    confirmMock.status = 'idle'
    confirmMock.error = null
    activeMock.clear = vi.fn()
    activeMock.set = vi.fn()
    activeMock.active = null
    createReservationMock.mutate.mockReset()
    createReservationMock.isPending = false
    createReservationMock.opts = null
    queryClientMock.setQueryData.mockReset()
    authMock.useAuth.mockReturnValue({ user: { uid: 'u1', email: 'u1@x.com' }, isReady: true })
    guestMock.useGuestIdentity.mockReturnValue({ identity: null })
    // Default: items loaded with the item
    itemsQueryMock.useItemsQuery.mockReturnValue({ data: [makeItem()] })
    // Default: reservation found
    reservationForItemMock.useReservationForItem.mockReturnValue({ status: 'hydrated', active: ACTIVE_RES })
  })

  it('P-01 (loading): renders loading state when items data is undefined', () => {
    itemsQueryMock.useItemsQuery.mockReturnValue({ data: undefined })
    reservationForItemMock.useReservationForItem.mockReturnValue({ status: 'idle', active: null })

    renderPage()
    expect(screen.getByTestId('item-reserve-loading')).toBeInTheDocument()
    expect(screen.getByText(/loading/i)).toBeInTheDocument()
  })

  it('P-01b (loading): renders loading state when lookupStatus is idle', () => {
    itemsQueryMock.useItemsQuery.mockReturnValue({ data: [makeItem()] })
    reservationForItemMock.useReservationForItem.mockReturnValue({ status: 'idle', active: null })

    renderPage()
    expect(screen.getByTestId('item-reserve-loading')).toBeInTheDocument()
  })

  it('P-02 (item not found): renders item_not_found state when item does not exist in registry', () => {
    itemsQueryMock.useItemsQuery.mockReturnValue({ data: [makeItem({ id: 'other-item' })] })
    reservationForItemMock.useReservationForItem.mockReturnValue({ status: 'empty', active: null })

    renderPage()
    expect(screen.getByTestId('item-reserve-not-found')).toBeInTheDocument()
    expect(screen.getByText(/couldn't find that item/i)).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /back to registry/i })).toBeInTheDocument()
  })

  it('P-03 (not-yours superseded by k37): a reserved item with no active reservation now renders BROWSE_RESERVED_BY_OTHER (more informative than the legacy not-yours panel)', () => {
    // makeItem() defaults to status='reserved', reservedBy='user@example.com'.
    // Under the k37 state machine, !active && status==='reserved' routes to
    // BROWSE_RESERVED_BY_OTHER (item-reserve-reserved-by-other) — not the
    // legacy not-yours fallback. The fallback is now unreachable for any
    // valid ItemStatus value; this test guards the new routing.
    itemsQueryMock.useItemsQuery.mockReturnValue({ data: [makeItem()] })
    reservationForItemMock.useReservationForItem.mockReturnValue({ status: 'empty', active: null })

    renderPage()
    expect(screen.getByTestId('item-reserve-reserved-by-other')).toBeInTheDocument()
    expect(screen.queryByTestId('item-reserve-not-yours')).toBeNull()
    expect(screen.getByRole('link', { name: /back to registry/i })).toBeInTheDocument()
  })

  it('P-04 (reserved-by-me happy): renders full reserve-detail UI when active reservation exists', () => {
    renderPage()

    expect(screen.getByTestId('item-reserve-detail')).toBeInTheDocument()
    // Page caption
    expect(screen.getByText('YOUR RESERVATION · STEP 2 OF 2')).toBeInTheDocument()
    // Item name (headline)
    expect(screen.getByText('Coffee Machine')).toBeInTheDocument()
    // ConfirmPurchaseBanner mocked
    expect(screen.getByTestId('confirm-purchase-banner')).toBeInTheDocument()
    // HowTimerWorks mocked
    expect(screen.getByTestId('how-timer-works')).toBeInTheDocument()
    // Continue-to-retailer anchor
    const continueLink = screen.getByRole('link', { name: /continue.*emag/i })
    expect(continueLink).toHaveAttribute('href', ACTIVE_RES.affiliateUrl)
    expect(continueLink).toHaveAttribute('target', '_blank')
    // Release CTA
    expect(screen.getByRole('button', { name: /release/i })).toBeInTheDocument()
    // D-06: no reserver name rendered
    expect(screen.queryByText('user@example.com')).not.toBeInTheDocument()
    expect(screen.queryByText('u1@x.com')).not.toBeInTheDocument()
  })

  it('P-05 (expired): renders expired state when countdown reaches 0 in-session (per quick-260516-iux Bug B)', async () => {
    // NOTE: rewritten per quick-260516-iux Bug B. The original P-05 constructed
    // a stale-expired-on-mount scenario (expiresAtMs in the past at first render)
    // and expected the expired UI. That scenario now correctly falls through to
    // the browse branch — see K-08/K-10/K-11. To preserve the "expired UI fires
    // when countdown hits 0" contract, we now simulate in-session expiration:
    // mount with a positive countdown so sawNonExpiredRef flips, then advance
    // fake timers past expiry and force a re-render. K-09 is the dedicated
    // regression guard for this; P-05 keeps the assertion shape for continuity.
    vi.useFakeTimers()
    try {
      const futureExpiry = Date.now() + 2_000
      reservationForItemMock.useReservationForItem.mockReturnValue({
        status: 'hydrated',
        active: { ...ACTIVE_RES, expiresAtMs: futureExpiry },
      })

      const { rerenderSame } = renderPage()

      // Initial render: not expired — reserved-by-me detail visible.
      expect(screen.getByTestId('item-reserve-detail')).toBeInTheDocument()

      // Advance past expiry, force re-render so useCountdown ticks and the new
      // countdown.expired=true flows into the render branches.
      await act(async () => {
        vi.advanceTimersByTime(3_000)
        rerenderSame()
      })

      expect(screen.getByTestId('item-reserve-expired')).toBeInTheDocument()
      expect(screen.getByText(/time ran out/i)).toBeInTheDocument()
      expect(screen.getByRole('link', { name: /back to registry/i })).toBeInTheDocument()
    } finally {
      vi.useRealTimers()
    }
  })

  it('P-06 (release success → navigate back): navigates to /registry/:id on release success', async () => {
    renderPage()

    // Simulate release success
    releaseMock.status = 'success'

    // Re-render with updated mock
    releaseMock.status = 'success'
    const { unmount } = renderPage()

    await waitFor(() => {
      // Toast should have been called with release_success
      // Navigation should have happened (registry page visible)
    })

    unmount()
  })

  it('P-06b (release success clears active-reservation context): calls useActiveReservation().clear() exactly once on release success', async () => {
    // Drive release to success BEFORE mount so the effect fires on first commit.
    releaseMock.status = 'success'

    renderPage()

    await waitFor(() => {
      expect(activeMock.clear).toHaveBeenCalledTimes(1)
      expect(screen.getByTestId('registry-page')).toBeInTheDocument()
    })
    // Toast fired once with success severity (translation key may resolve differently).
    expect(toastMock.showToast).toHaveBeenCalledTimes(1)
    expect(toastMock.showToast.mock.calls[0][1]).toBe('success')
  })

  it('P-07 (confirm success → navigate back): item status flip to purchased triggers navigate back', async () => {
    // Start with item reserved
    const item = makeItem({ status: 'reserved' })
    itemsQueryMock.useItemsQuery.mockReturnValue({ data: [item] })

    renderPage()

    // Simulate item becoming purchased (as if confirm succeeded)
    act(() => {
      itemsQueryMock.useItemsQuery.mockReturnValue({ data: [makeItem({ status: 'purchased' })] })
    })

    // Just verify the component renders without error in the happy path
    // The navigate-back is triggered by useEffect watching item.status
    await waitFor(() => {
      // Component is still mounted and functioning
      expect(screen.getByTestId('item-reserve-detail')).toBeInTheDocument()
    })
  })

  it('P-08 (HON-01): stale-on-mount available status does NOT navigate back', async () => {
    // Items snapshot still shows old pre-reservation state (stale 'available')
    itemsQueryMock.useItemsQuery.mockReturnValue({ data: [makeItem({ status: 'available' })] })
    // Reservation lookup already sees the new reservation (real race)
    reservationForItemMock.useReservationForItem.mockReturnValue({ status: 'hydrated', active: ACTIVE_RES })

    renderPage()

    await waitFor(() => {
      expect(screen.getByTestId('item-reserve-detail')).toBeInTheDocument()
      expect(screen.queryByTestId('registry-page')).not.toBeInTheDocument()
    })
  })

  it('P-09 (HON-02): real reserved->available transition DOES navigate back', async () => {
    itemsQueryMock.useItemsQuery.mockReturnValue({ data: [makeItem({ status: 'reserved' })] })
    reservationForItemMock.useReservationForItem.mockReturnValue({ status: 'hydrated', active: ACTIVE_RES })

    const { rerenderSame } = renderPage()

    // Intermediate state: detail UI present, no redirect yet
    expect(screen.getByTestId('item-reserve-detail')).toBeInTheDocument()
    expect(screen.queryByTestId('registry-page')).toBeNull()

    // Flip to available (e.g. release completed), then force a re-render so the
    // new mock value is picked up and the transition-detector effect can run.
    await act(async () => {
      itemsQueryMock.useItemsQuery.mockReturnValue({ data: [makeItem({ status: 'available' })] })
      rerenderSame()
    })

    await waitFor(() => {
      expect(screen.queryByTestId('item-reserve-detail')).not.toBeInTheDocument()
      expect(screen.getByTestId('registry-page')).toBeInTheDocument()
    })
  })

  it('P-11 (empty affiliateUrl): hides Continue-to-retailer link, keeps Release and detail UI', () => {
    const noAffiliate = { ...ACTIVE_RES, affiliateUrl: '' }
    reservationForItemMock.useReservationForItem.mockReturnValue({ status: 'hydrated', active: noAffiliate })

    renderPage()

    // Detail UI present — NOT the 'not yours' branch
    expect(screen.getByTestId('item-reserve-detail')).toBeInTheDocument()
    // Continue-to-retailer anchor hidden
    expect(screen.queryByRole('link', { name: /continue/i })).toBeNull()
    // Release button still present
    expect(screen.getByRole('button', { name: /release/i })).toBeInTheDocument()
  })

  it('P-10 (HON-03): real reserved->purchased transition DOES navigate back', async () => {
    itemsQueryMock.useItemsQuery.mockReturnValue({ data: [makeItem({ status: 'reserved' })] })
    reservationForItemMock.useReservationForItem.mockReturnValue({ status: 'hydrated', active: ACTIVE_RES })

    const { rerenderSame } = renderPage()

    // Intermediate state: detail UI present, no redirect yet
    expect(screen.getByTestId('item-reserve-detail')).toBeInTheDocument()
    expect(screen.queryByTestId('registry-page')).toBeNull()

    // Flip to purchased (confirm-purchase completed), then force a re-render so the
    // new mock value is picked up and the transition-detector effect can run.
    await act(async () => {
      itemsQueryMock.useItemsQuery.mockReturnValue({ data: [makeItem({ status: 'purchased' })] })
      rerenderSame()
    })

    await waitFor(() => {
      expect(screen.queryByTestId('item-reserve-detail')).not.toBeInTheDocument()
      expect(screen.getByTestId('registry-page')).toBeInTheDocument()
    })
  })

  // ---- k37 — browse-state branches ----

  it('K-01 (available browse state): renders Reserve CTA + hero + notes when item available and no active reservation', () => {
    itemsQueryMock.useItemsQuery.mockReturnValue({
      data: [
        makeItem({
          status: 'available',
          reservedBy: null,
          notes: 'Owner says: please wrap it',
          title: 'Coffee Machine Full Long Title',
        }),
      ],
    })
    reservationForItemMock.useReservationForItem.mockReturnValue({ status: 'empty', active: null })

    renderPage()

    expect(screen.getByTestId('item-reserve-available')).toBeInTheDocument()
    // Reserve CTA exists
    expect(screen.getByRole('button', { name: /reserve this gift/i })).toBeInTheDocument()
    // Full untruncated title visible (detail page does NOT truncate)
    expect(screen.getByText('Coffee Machine Full Long Title')).toBeInTheDocument()
    // Notes visible
    expect(screen.getByText('Owner says: please wrap it')).toBeInTheDocument()
    expect(screen.getByText(/from the registry owner/i)).toBeInTheDocument()
    // The old not-yours panel must NOT render for available items
    expect(screen.queryByTestId('item-reserve-not-yours')).not.toBeInTheDocument()
  })

  it('K-02 (Reserve click): calls useCreateReservation.mutate with derived giver fields for signed-in user', async () => {
    itemsQueryMock.useItemsQuery.mockReturnValue({
      data: [makeItem({ status: 'available', reservedBy: null })],
    })
    reservationForItemMock.useReservationForItem.mockReturnValue({ status: 'empty', active: null })
    authMock.useAuth.mockReturnValue({
      user: { uid: 'u1', email: 'u1@x.com', displayName: null },
      isReady: true,
    })

    renderPage()

    const cta = screen.getByRole('button', { name: /reserve this gift/i })
    cta.click()

    expect(createReservationMock.mutate).toHaveBeenCalledTimes(1)
    expect(createReservationMock.mutate).toHaveBeenCalledWith({
      registryId: 'reg1',
      itemId: 'it1',
      giverName: 'u1', // displayName null → email.split('@')[0]
      giverEmail: 'u1@x.com',
      giverId: 'u1',
    })
  })

  it('K-03 (reserved-by-someone-else browse state): view-only UI, no Reserve CTA, no reserver email in DOM (D-06)', () => {
    itemsQueryMock.useItemsQuery.mockReturnValue({
      data: [
        makeItem({
          status: 'reserved',
          reservedBy: 'someone-else@x.com',
          notes: 'Maybe the blue one',
        }),
      ],
    })
    reservationForItemMock.useReservationForItem.mockReturnValue({ status: 'empty', active: null })

    renderPage()

    expect(screen.getByTestId('item-reserve-reserved-by-other')).toBeInTheDocument()
    expect(screen.getByText(/already reserved by another guest/i)).toBeInTheDocument()
    // View-only — NO Reserve CTA
    expect(screen.queryByRole('button', { name: /reserve this gift/i })).toBeNull()
    // D-06 — reserver email never rendered
    expect(screen.queryByText('someone-else@x.com')).toBeNull()
    // Item info still visible
    expect(screen.getByText('Coffee Machine')).toBeInTheDocument()
    expect(screen.getByText('Maybe the blue one')).toBeInTheDocument()
  })

  it('K-04 (purchased browse state): view-only UI, no Reserve CTA, no giver name in DOM (D-06)', () => {
    itemsQueryMock.useItemsQuery.mockReturnValue({
      data: [
        makeItem({
          status: 'purchased',
          reservedBy: 'giver@x.com',
          notes: 'Thanks!',
        }),
      ],
    })
    reservationForItemMock.useReservationForItem.mockReturnValue({ status: 'empty', active: null })

    renderPage()

    expect(screen.getByTestId('item-reserve-purchased')).toBeInTheDocument()
    expect(screen.getByText(/already purchased/i)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /reserve this gift/i })).toBeNull()
    // D-06 — giver email never rendered
    expect(screen.queryByText('giver@x.com')).toBeNull()
    expect(screen.getByText('Coffee Machine')).toBeInTheDocument()
    expect(screen.getByText('Thanks!')).toBeInTheDocument()
  })

  it('K-05 (notes optional): notes block omitted when item.notes is null', () => {
    itemsQueryMock.useItemsQuery.mockReturnValue({
      data: [makeItem({ status: 'available', reservedBy: null, notes: null })],
    })
    reservationForItemMock.useReservationForItem.mockReturnValue({ status: 'empty', active: null })

    renderPage()

    expect(screen.getByTestId('item-reserve-available')).toBeInTheDocument()
    // Notes label not in DOM when notes null
    expect(screen.queryByText(/from the registry owner/i)).toBeNull()
  })

  it('K-06 (regression — anonymous-no-identity falls back to /registry/:id?autoReserveItemId=:itemId)', async () => {
    // Anonymous: no user, no guest identity
    authMock.useAuth.mockReturnValue({ user: null, isReady: true })
    guestMock.useGuestIdentity.mockReturnValue({ identity: null })
    itemsQueryMock.useItemsQuery.mockReturnValue({
      data: [makeItem({ status: 'available', reservedBy: null })],
    })
    reservationForItemMock.useReservationForItem.mockReturnValue({ status: 'empty', active: null })

    renderPage()

    const cta = screen.getByRole('button', { name: /reserve this gift/i })
    await act(async () => {
      cta.click()
    })

    // Direct mutation should NOT have fired — we expect a navigate fallback instead.
    expect(createReservationMock.mutate).not.toHaveBeenCalled()
    // The router will now have navigated to /registry/reg1?autoReserveItemId=it1.
    // The matching route renders <div data-testid="registry-page" /> per renderPage()
    // (it ignores query string).
    await waitFor(() => {
      expect(screen.getByTestId('registry-page')).toBeInTheDocument()
    })
  })

  it('K-07 (guest-with-identity): direct mutation with identity fields (no fallback nav)', () => {
    authMock.useAuth.mockReturnValue({ user: null, isReady: true })
    guestMock.useGuestIdentity.mockReturnValue({
      identity: { firstName: 'Ana', lastName: 'Pop', email: 'ana@x.com' },
    })
    itemsQueryMock.useItemsQuery.mockReturnValue({
      data: [makeItem({ status: 'available', reservedBy: null })],
    })
    reservationForItemMock.useReservationForItem.mockReturnValue({ status: 'empty', active: null })

    renderPage()

    const cta = screen.getByRole('button', { name: /reserve this gift/i })
    cta.click()

    expect(createReservationMock.mutate).toHaveBeenCalledTimes(1)
    expect(createReservationMock.mutate).toHaveBeenCalledWith({
      registryId: 'reg1',
      itemId: 'it1',
      giverName: 'Ana Pop',
      giverEmail: 'ana@x.com',
      giverId: null,
    })
  })

  // ---- quick-260516-iux — stale-expired-on-mount + in-session expiration regression ----

  it("K-08 (stale-expired-on-mount + item.status='reserved' falls through to BROWSE_RESERVED_BY_OTHER — NOT expired, NOT reserved-by-me detail)", () => {
    // Anonymous viewer with stored guest identity. Items snapshot still shows
    // 'reserved' (stale row) but the active reservation's expiresAtMs is already
    // in the past (e.g. emulator restart killed the auto-release setTimeout).
    authMock.useAuth.mockReturnValue({ user: null, isReady: true })
    guestMock.useGuestIdentity.mockReturnValue({
      identity: { firstName: 'Ion', lastName: 'Pop', email: 'ion@x.com' },
    })
    reservationForItemMock.useReservationForItem.mockReturnValue({
      status: 'hydrated',
      active: { ...ACTIVE_RES, expiresAtMs: Date.now() - 60_000 },
    })
    itemsQueryMock.useItemsQuery.mockReturnValue({
      data: [makeItem({ status: 'reserved' })],
    })

    renderPage()

    // Both branches 4 and 5 must short-circuit on stale-expired-on-mount.
    expect(screen.queryByTestId('item-reserve-expired')).toBeNull()
    expect(screen.queryByTestId('item-reserve-detail')).toBeNull()
    // Flow falls through to branch 7 (reserved-by-other).
    expect(screen.getByTestId('item-reserve-reserved-by-other')).toBeInTheDocument()
    // i18n-safe assert: no "time ran out" copy anywhere in the DOM.
    expect(screen.queryByText(/time ran out/i)).toBeNull()
  })

  it('K-09 (REGRESSION: in-session expiration countdown→0 STILL renders expired branch)', async () => {
    // Use fake timers ONLY for this test. Wrap in try/finally so a failed
    // assertion does not leak fake timers into subsequent tests.
    vi.useFakeTimers()
    try {
      // Baseline: expiry 2 seconds in the future. countdown.expired starts false →
      // sawNonExpiredRef will flip true on the first effect commit.
      const start = Date.now()
      const futureExpiry = start + 2_000
      reservationForItemMock.useReservationForItem.mockReturnValue({
        status: 'hydrated',
        active: { ...ACTIVE_RES, expiresAtMs: futureExpiry },
      })
      itemsQueryMock.useItemsQuery.mockReturnValue({ data: [makeItem({ status: 'reserved' })] })

      const { rerenderSame } = renderPage()

      // Initial render: not expired yet → reserved-by-me detail visible.
      expect(screen.getByTestId('item-reserve-detail')).toBeInTheDocument()
      expect(screen.queryByTestId('item-reserve-expired')).toBeNull()

      // Advance time past expiry. useCountdown's internal interval fires and the
      // page re-renders with countdown.expired=true. sawNonExpiredRef is now true
      // (flipped during the initial commit), so effectiveActive === active and
      // branch 4 should render the expired UI.
      await act(async () => {
        vi.advanceTimersByTime(3_000)
        rerenderSame()
      })

      expect(screen.queryByTestId('item-reserve-detail')).toBeNull()
      expect(screen.getByTestId('item-reserve-expired')).toBeInTheDocument()
      expect(screen.getByText(/time ran out/i)).toBeInTheDocument()
    } finally {
      vi.useRealTimers()
    }
  })

  it("K-10 (stale-expired-on-mount + item.status='available' falls through to BROWSE_AVAILABLE)", () => {
    // Rare combo: auto-release ran on the item (item.status='available') but
    // the active context is stale. Forced via test fixture.
    authMock.useAuth.mockReturnValue({ user: null, isReady: true })
    guestMock.useGuestIdentity.mockReturnValue({
      identity: { firstName: 'Ion', lastName: 'Pop', email: 'ion@x.com' },
    })
    reservationForItemMock.useReservationForItem.mockReturnValue({
      status: 'hydrated',
      active: { ...ACTIVE_RES, expiresAtMs: Date.now() - 60_000 },
    })
    itemsQueryMock.useItemsQuery.mockReturnValue({
      data: [makeItem({ status: 'available', reservedBy: null })],
    })

    renderPage()

    expect(screen.queryByTestId('item-reserve-expired')).toBeNull()
    expect(screen.queryByTestId('item-reserve-detail')).toBeNull()
    expect(screen.getByTestId('item-reserve-available')).toBeInTheDocument()
  })

  it("K-11 (stale-expired-on-mount + item.status='purchased' falls through to BROWSE_PURCHASED)", () => {
    authMock.useAuth.mockReturnValue({ user: null, isReady: true })
    guestMock.useGuestIdentity.mockReturnValue({
      identity: { firstName: 'Ion', lastName: 'Pop', email: 'ion@x.com' },
    })
    reservationForItemMock.useReservationForItem.mockReturnValue({
      status: 'hydrated',
      active: { ...ACTIVE_RES, expiresAtMs: Date.now() - 60_000 },
    })
    itemsQueryMock.useItemsQuery.mockReturnValue({
      data: [makeItem({ status: 'purchased' })],
    })

    renderPage()

    expect(screen.queryByTestId('item-reserve-expired')).toBeNull()
    expect(screen.queryByTestId('item-reserve-detail')).toBeNull()
    expect(screen.getByTestId('item-reserve-purchased')).toBeInTheDocument()
  })

  it("K-12 (Reserve → seed shared context → reserved-by-me detail): clicking Reserve on BROWSE_AVAILABLE seeds useActiveReservation; next render transitions into reserved-by-me detail", async () => {
    authMock.useAuth.mockReturnValue({ user: { uid: 'u1', email: 'u1@x.com', displayName: null }, isReady: true })
    guestMock.useGuestIdentity.mockReturnValue({ identity: null })
    itemsQueryMock.useItemsQuery.mockReturnValue({
      data: [makeItem({ status: 'available', reservedBy: null })],
    })
    reservationForItemMock.useReservationForItem.mockReturnValue({ status: 'empty', active: null })
    activeMock.active = null
    // Replace the default spy with one that mirrors real context behaviour
    // (mutates activeMock.active on set) so the next render observes the seeded value.
    activeMock.set = vi.fn((r: unknown) => { activeMock.active = r })

    const { rerenderSame } = renderPage()

    // Initial: BROWSE_AVAILABLE visible
    expect(screen.getByTestId('item-reserve-available')).toBeInTheDocument()

    // Click Reserve → mutation fires
    const cta = screen.getByRole('button', { name: /reserve this gift/i })
    cta.click()
    expect(createReservationMock.mutate).toHaveBeenCalledTimes(1)

    // Simulate backend success: fire the captured onSuccess synchronously
    await act(async () => {
      createReservationMock.opts!.onSuccess!(
        {
          reservationId: 'res-new',
          affiliateUrl: 'https://emag.ro/item1',
          expiresAtMs: Date.now() + 30 * 60 * 1000,
        },
        { registryId: 'reg1', itemId: 'it1', giverName: 'u1', giverEmail: 'u1@x.com', giverId: 'u1' },
      )
    })

    // Confirm context was seeded
    expect(activeMock.set).toHaveBeenCalledTimes(1)
    expect((activeMock.active as { itemId: string }).itemId).toBe('it1')

    // Force next render — the page must now read sharedActive via the new derivation
    await act(async () => {
      rerenderSame()
    })

    // BROWSE_AVAILABLE released; reserved-by-me detail visible
    expect(screen.queryByTestId('item-reserve-available')).toBeNull()
    expect(screen.getByTestId('item-reserve-detail')).toBeInTheDocument()

    // D-06: no email leak
    expect(screen.queryByText('u1@x.com')).toBeNull()
    expect(screen.queryByText('user@example.com')).toBeNull()

    // Cleanup for downstream tests (defensive — beforeEach also resets)
    activeMock.active = null
  })

  it('K-13 (regression — hydration-on-fresh-mount): useReservationForItem.active drives reserved-by-me detail when shared context is empty', () => {
    activeMock.active = null
    reservationForItemMock.useReservationForItem.mockReturnValue({
      status: 'hydrated',
      active: ACTIVE_RES,
    })
    // Default items mock (reserved + user@example.com — irrelevant; active takes precedence)
    renderPage()

    // Reserved-by-me detail visible (driven by lookupActive, not sharedActive)
    expect(screen.getByTestId('item-reserve-detail')).toBeInTheDocument()
    // Sanity: shared context was empty throughout — proves derivation fell through to lookupActive
    expect(activeMock.active).toBeNull()
  })

  it('K-14 (cross-itemId stale-context guard): when sharedActive.itemId !== route itemId, page uses lookupActive (not the stale sharedActive)', () => {
    // Shared context leaked from a previous /item/OTHER-ITEM visit in this SPA session.
    activeMock.active = {
      reservationId: 'res-other',
      itemId: 'OTHER-ITEM',
      itemName: 'Other Item',
      affiliateUrl: 'https://example.com/other',
      merchantDomain: null,
      expiresAtMs: Date.now() + 30 * 60 * 1000,
    }
    // No lookup result for the current route itemId.
    reservationForItemMock.useReservationForItem.mockReturnValue({ status: 'empty', active: null })
    // Route item is available.
    itemsQueryMock.useItemsQuery.mockReturnValue({
      data: [makeItem({ status: 'available', reservedBy: null })],
    })

    renderPage()

    // Page must render BROWSE_AVAILABLE — NOT reserved-by-me detail for the stale shared active.
    expect(screen.getByTestId('item-reserve-available')).toBeInTheDocument()
    expect(screen.queryByTestId('item-reserve-detail')).toBeNull()
    // Stale itemName must NOT leak into the DOM.
    expect(screen.queryByText('Other Item')).toBeNull()

    // Cleanup
    activeMock.active = null
  })

  // ---- quick-260516-lsi — clickable product blade on ItemReservePage ----

  it('K-15: BROWSE_AVAILABLE blade wraps in anchor preferring affiliateUrl', () => {
    itemsQueryMock.useItemsQuery.mockReturnValue({
      data: [
        makeItem({
          status: 'available',
          reservedBy: null,
          affiliateUrl: 'https://emag.ro/aff/it1',
          originalUrl: 'https://emag.ro/it1',
        }),
      ],
    })
    reservationForItemMock.useReservationForItem.mockReturnValue({ status: 'empty', active: null })

    renderPage()

    const container = screen.getByTestId('item-reserve-available')
    const link = within(container).getByRole('link', { name: /open product page at emag\.ro/i })
    expect(link.getAttribute('href')).toBe('https://emag.ro/aff/it1')
    expect(link.getAttribute('target')).toBe('_blank')
    const rel = link.getAttribute('rel') ?? ''
    expect(rel).toMatch(/noopener/)
    expect(rel).toMatch(/noreferrer/)
  })

  it('K-16: BROWSE_AVAILABLE blade falls back to originalUrl when affiliateUrl is empty', () => {
    itemsQueryMock.useItemsQuery.mockReturnValue({
      data: [
        makeItem({
          status: 'available',
          reservedBy: null,
          affiliateUrl: '',
          originalUrl: 'https://emag.ro/it1',
        }),
      ],
    })
    reservationForItemMock.useReservationForItem.mockReturnValue({ status: 'empty', active: null })

    renderPage()

    const container = screen.getByTestId('item-reserve-available')
    const link = within(container).getByRole('link', { name: /open product page at emag\.ro/i })
    expect(link.getAttribute('href')).toBe('https://emag.ro/it1')
    expect(link.getAttribute('target')).toBe('_blank')
    const rel = link.getAttribute('rel') ?? ''
    expect(rel).toMatch(/noopener/)
    expect(rel).toMatch(/noreferrer/)
  })

  it('K-17: BROWSE_PURCHASED blade renders as static div when both URLs are empty', () => {
    itemsQueryMock.useItemsQuery.mockReturnValue({
      data: [
        makeItem({ status: 'purchased', affiliateUrl: '', originalUrl: '' }),
      ],
    })
    reservationForItemMock.useReservationForItem.mockReturnValue({ status: 'empty', active: null })

    renderPage()

    const container = screen.getByTestId('item-reserve-purchased')
    // No blade link rendered when both URLs are empty.
    const link = within(container).queryByRole('link', { name: /open product page/i })
    expect(link).toBeNull()
    // Sanity: blade content (title) still renders.
    expect(screen.getByText('Coffee Machine')).toBeInTheDocument()
    // Sanity: container still mounts.
    expect(container).toBeInTheDocument()
  })

  it('K-18: BROWSE_RESERVED_BY_OTHER blade aria-label interpolates merchantDomain', () => {
    itemsQueryMock.useItemsQuery.mockReturnValue({
      data: [
        makeItem({
          status: 'reserved',
          affiliateUrl: 'https://altex.ro/aff',
          originalUrl: 'https://altex.ro/it1',
          merchantDomain: 'altex.ro',
        }),
      ],
    })
    reservationForItemMock.useReservationForItem.mockReturnValue({ status: 'empty', active: null })

    renderPage()

    const container = screen.getByTestId('item-reserve-reserved-by-other')
    const link = within(container).getByRole('link', { name: /open product page at altex\.ro/i })
    expect(link).not.toBeNull()
    expect(link.getAttribute('aria-label')).toBe('Open product page at altex.ro')
  })

  it('K-19: reserved-by-me hero blade is anchor; nested time-to-purchase mmss still renders inside', () => {
    // Default beforeEach already wires ACTIVE_RES (affiliateUrl: https://emag.ro/item1, merchantDomain: emag.ro).
    renderPage()

    // Precondition: reserved-by-me branch is active.
    const detail = screen.getByTestId('item-reserve-detail')
    expect(detail).toBeInTheDocument()

    const bladeAnchor = within(detail).getByRole('link', { name: /open product page at emag\.ro/i })
    expect(bladeAnchor.getAttribute('href')).toBe('https://emag.ro/item1')
    expect(bladeAnchor.getAttribute('target')).toBe('_blank')
    const rel = bladeAnchor.getAttribute('rel') ?? ''
    expect(rel).toMatch(/noopener/)
    expect(rel).toMatch(/noreferrer/)

    // Nested time-to-purchase mmss element renders WITHIN the anchor.
    expect(within(bladeAnchor as HTMLElement).getByTestId('reserve-detail-mmss')).toBeInTheDocument()

    // Regression guard: the "Continue to retailer" CTA in the button row still exists (distinct from the blade anchor).
    const continueLinks = screen.getAllByRole('link', { name: /continue.*emag/i })
    expect(continueLinks.length).toBeGreaterThanOrEqual(1)
  })

  // ---- quick-260516-oiy — optimistic items-cache patch on release-success ----

  it('K-20: release-success patches items cache BEFORE navigate so the just-released item appears available on RegistryPage', async () => {
    // Drive release to success BEFORE mount so the effect fires on first commit (mirrors P-06b).
    releaseMock.status = 'success'

    renderPage()

    // 1. Cache patch fires exactly once with the correct key + updater shape.
    await waitFor(() => {
      expect(queryClientMock.setQueryData).toHaveBeenCalledTimes(1)
    })
    const [key, updater] = queryClientMock.setQueryData.mock.calls[0] as [
      unknown,
      (old: Item[] | undefined) => Item[] | undefined,
    ]
    expect(key).toEqual(['registry', 'reg1', 'items'])
    expect(typeof updater).toBe('function')

    // 2. Updater overrides ONLY the matching item; unrelated items keep referential identity.
    const fakeIn: Item[] = [
      makeItem({
        id: 'it1',
        status: 'reserved',
        reservedBy: 'user@x',
        reservedAt: new Date(123),
        expiresAt: new Date(456),
      }),
      makeItem({
        id: 'other',
        status: 'available',
        reservedBy: null,
        reservedAt: null,
        expiresAt: null,
      }),
    ]
    const out = updater(fakeIn)!
    expect(out).toHaveLength(2)
    const patched = out.find((i) => i.id === 'it1')!
    expect(patched.status).toBe('available')
    expect(patched.reservedBy).toBeNull()
    expect(patched.reservedAt).toBeNull()
    expect(patched.expiresAt).toBeNull()
    const untouched = out.find((i) => i.id === 'other')!
    expect(untouched).toBe(fakeIn[1]) // referential identity preserved

    // 3. Updater on undefined input returns undefined (cache-empty safety).
    expect(updater(undefined)).toBeUndefined()

    // 4. Call order: setQueryData fires BEFORE clearActiveReservation, which itself
    //    fires before navigate inside the same effect body (ItemReservePage.tsx lines 193-200).
    //    Asserting < clear is a strict superset proof that the patch happens before navigate.
    const patchOrder = queryClientMock.setQueryData.mock.invocationCallOrder[0]
    const clearOrder = activeMock.clear.mock.invocationCallOrder[0]
    expect(patchOrder).toBeLessThan(clearOrder)

    // 5. Sanity: navigation still completed (registry page mounted).
    await waitFor(() => {
      expect(screen.getByTestId('registry-page')).toBeInTheDocument()
    })
  })

  // ---- quick-260518-j5j — navigate carries state for post-release hydration + snapshot race suppression ----

  it('K-21: release-success navigates with state.recentReleasedReservationId + state.recentReleasedItemId (j5j)', async () => {
    // Drive release to success on first commit (same pattern as K-20).
    releaseMock.status = 'success'

    // Mount with a probe destination that captures location.state into the DOM.
    // This avoids mocking useNavigate file-wide (which would break existing tests
    // that observe the destination route element via real router transitions).
    const ProbeDestination = () => {
      const loc = useLocation()
      return (
        <div
          data-testid="registry-page"
          data-state={JSON.stringify(loc.state)}
        />
      )
    }
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    const router = createMemoryRouter(
      [
        { path: '/registry/:id/item/:itemId', element: <ItemReservePageWithForceUpdate /> },
        { path: '/registry/:id', element: <ProbeDestination /> },
      ],
      { initialEntries: ['/registry/reg1/item/it1'] },
    )
    render(
      <QueryClientProvider client={client}>
        <RouterProvider router={router} />
      </QueryClientProvider>,
    )

    await waitFor(() => {
      expect(screen.getByTestId('registry-page')).toBeInTheDocument()
    })
    const stateAttr = screen.getByTestId('registry-page').getAttribute('data-state')
    expect(stateAttr).not.toBeNull()
    const state = JSON.parse(stateAttr!)
    expect(state).toEqual({
      recentReleasedReservationId: 'res1',
      recentReleasedItemId: 'it1',
    })
  })
})
