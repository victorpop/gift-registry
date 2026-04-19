import { beforeEach, describe, expect, it, vi } from "vitest"
import { render, screen, waitFor } from "@testing-library/react"
import { createMemoryRouter, RouterProvider } from "react-router"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import "../i18n"
import type { Registry, Item } from "../lib/firestore-mapping"

// Mock firebase/functions + firebase so httpsCallable is controllable.
const httpsCallableMock = vi.fn()
vi.mock("firebase/functions", () => ({
  httpsCallable: (...args: unknown[]) => httpsCallableMock(...args),
}))
vi.mock("../firebase", () => ({
  functions: { __mock: true },
  firestore: { __mock: true },
  auth: { __mock: true },
}))

// Mock data-loading hooks the RegistryPage depends on
const queryHooksMock = vi.hoisted(() => ({
  useRegistryQuery: vi.fn(),
  useItemsQuery: vi.fn(),
}))
vi.mock("../features/registry/useRegistryQuery", () => ({ useRegistryQuery: queryHooksMock.useRegistryQuery }))
vi.mock("../features/registry/useItemsQuery", () => ({ useItemsQuery: queryHooksMock.useItemsQuery }))

const authMock = vi.hoisted(() => ({ useAuth: vi.fn() }))
vi.mock("../features/auth/useAuth", () => authMock)

const guestIdMock = vi.hoisted(() => ({ useGuestIdentity: vi.fn() }))
vi.mock("../features/auth/useGuestIdentity", () => guestIdMock)

const useCreateResMock = vi.hoisted(() => vi.fn())
vi.mock("../features/reservation/useCreateReservation", () => ({ useCreateReservation: useCreateResMock }))

// Mock ToastProvider
const showToastMock = vi.fn()
vi.mock("../components/ToastProvider", () => ({
  useToast: () => ({ showToast: showToastMock }),
  ToastProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}))

// Mock useActiveReservation — controls whether banner shows
const activeMock = vi.hoisted(() => ({ active: null as unknown, set: vi.fn(), clear: vi.fn() }))
vi.mock("../features/reservation/useActiveReservation", () => ({
  useActiveReservation: () => activeMock,
  ActiveReservationProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}))

// Mock sub-components that import firebase
vi.mock("../features/reservation/ReserveButton", () => ({
  default: () => <button type="button">Reserve</button>,
}))
vi.mock("../features/reservation/ReservationBanner", () => ({
  default: () => null,
}))
vi.mock("../features/auth/AuthModal", () => ({
  default: ({ open }: { open: boolean }) => open ? <div data-testid="auth-modal" /> : null,
}))
vi.mock("../features/auth/GuestIdentityModal", () => ({
  default: ({ open, onSubmit }: { open: boolean; onSubmit?: (g: unknown) => void }) =>
    open ? (
      <div data-testid="guest-modal">
        <span>Who are you?</span>
        <button
          type="button"
          data-testid="guest-submit"
          onClick={() => onSubmit?.({ firstName: "Ion", lastName: "Pop", email: "ion@x.com" })}
        >
          Submit
        </button>
      </div>
    ) : null,
}))

import RegistryPage from "../pages/RegistryPage"

const registry: Registry = {
  id: "reg-1",
  ownerId: "owner-1",
  name: "Test Registry",
  occasionType: "Wedding",
  eventDate: null,
  eventLocation: null,
  description: null,
  visibility: "public",
  createdAt: null,
  updatedAt: null,
}

function makeItem(overrides: Partial<Item> = {}): Item {
  return {
    id: "item-1",
    title: "Coffee",
    imageUrl: null,
    price: null,
    currency: null,
    notes: null,
    status: "available",
    reservedBy: null,
    reservedAt: null,
    expiresAt: null,
    affiliateUrl: "https://x/?aff=1",
    originalUrl: "https://x",
    merchantDomain: "x",
    ...overrides,
  }
}

function renderAt(url = "/registry/reg-1") {
  const client = new QueryClient()
  const router = createMemoryRouter(
    [{ path: "/registry/:id", element: <RegistryPage /> }],
    { initialEntries: [url] },
  )
  return render(
    <QueryClientProvider client={client}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  )
}

describe("RegistryPage — ConfirmPurchaseBanner integration", () => {
  beforeEach(() => {
    httpsCallableMock.mockReset()
    showToastMock.mockReset()
    activeMock.active = null
    activeMock.set.mockReset()
    activeMock.clear.mockReset()
    useCreateResMock.mockReset()
    useCreateResMock.mockReturnValue({ mutate: vi.fn(), isPending: false })
    queryHooksMock.useRegistryQuery.mockReset()
    queryHooksMock.useItemsQuery.mockReset()
    authMock.useAuth.mockReset()
    guestIdMock.useGuestIdentity.mockReset()
    guestIdMock.useGuestIdentity.mockReturnValue({ identity: null, save: vi.fn(), clear: vi.fn() })
    queryHooksMock.useRegistryQuery.mockReturnValue({ data: registry })
    queryHooksMock.useItemsQuery.mockReturnValue({ data: [makeItem()] })
    authMock.useAuth.mockReturnValue({ user: null, isReady: true })
  })

  it("renders ConfirmPurchaseBanner when activeReservation is active", async () => {
    activeMock.active = {
      reservationId: "res1",
      itemId: "it1",
      itemName: "Coffee",
      affiliateUrl: "https://x/?aff=1",
      merchantDomain: "x",
      expiresAtMs: Date.now() + 30 * 60 * 1000,
    }
    httpsCallableMock.mockReturnValue(vi.fn(async () => ({ data: { success: true } })))

    renderAt()

    await waitFor(() => {
      // The confirm banner renders its CTA button with this i18n text
      expect(screen.getByText("I completed the purchase")).toBeInTheDocument()
    })
  })

  it("does NOT render ConfirmPurchaseBanner when activeReservation is null", async () => {
    activeMock.active = null

    renderAt()

    // Banner should not be in the DOM
    expect(screen.queryByText("I completed the purchase")).toBeNull()
  })

  it("does NOT render ConfirmPurchaseBanner when reservation is cleared (purchased)", async () => {
    // useActiveReservation returns null for non-active statuses per its contract
    activeMock.active = null

    renderAt()

    expect(screen.queryByText("I completed the purchase")).toBeNull()
  })
})
