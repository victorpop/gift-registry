import { describe, it, expect, vi, beforeEach } from "vitest"
import { render, screen, fireEvent, waitFor } from "@testing-library/react"
import React from "react"
import "../../../i18n"

const httpsCallableMock = vi.fn()
vi.mock("firebase/functions", () => ({
  httpsCallable: (...args: unknown[]) => httpsCallableMock(...args),
}))
vi.mock("../../../firebase", () => ({ functions: { __mock: true } }))

const showToastMock = vi.fn()
vi.mock("../../../components/ToastProvider", () => ({
  useToast: () => ({ showToast: showToastMock }),
  ToastProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}))

import { ConfirmPurchaseBanner } from "../ConfirmPurchaseBanner"

describe("ConfirmPurchaseBanner", () => {
  beforeEach(() => {
    httpsCallableMock.mockReset()
    showToastMock.mockReset()
  })

  it("renders heading and CTA with font-bold classes and 48px min-height", () => {
    httpsCallableMock.mockReturnValue(vi.fn())
    render(<ConfirmPurchaseBanner reservationId="res1" />)
    expect(screen.getByRole("status")).toBeInTheDocument()
    const button = screen.getByRole("button")
    // Class contract — exact tokens matter for the visual regression.
    expect(button.className).toContain("font-bold")
    expect(button.className).toContain("min-h-[48px]")
    expect(button.className).toContain("bg-primary")
    // Never use font-semibold on this project.
    expect(button.className).not.toContain("font-semibold")
  })

  it("calls confirmPurchase callable with the supplied reservationId when clicked", async () => {
    const callableSpy = vi.fn(async () => ({ data: { success: true } }))
    httpsCallableMock.mockReturnValue(callableSpy)

    render(<ConfirmPurchaseBanner reservationId="res-abc" />)
    fireEvent.click(screen.getByRole("button"))

    await waitFor(() => expect(callableSpy).toHaveBeenCalledWith({ reservationId: "res-abc" }))
  })

  it("shows success toast once on success", async () => {
    const callableSpy = vi.fn(async () => ({ data: { success: true } }))
    httpsCallableMock.mockReturnValue(callableSpy)

    render(<ConfirmPurchaseBanner reservationId="res-abc" />)
    fireEvent.click(screen.getByRole("button"))

    await waitFor(() => expect(showToastMock).toHaveBeenCalledTimes(1))
    const [message, variant] = showToastMock.mock.calls[0]
    expect(variant).toBe("success")
    // Message is i18n-rendered; at minimum it should NOT be the raw i18n key
    expect(String(message)).not.toBe("reservation.confirm_purchase_success")
  })

  it("shows error toast on RESERVATION_EXPIRED", async () => {
    const err = Object.assign(new Error("RESERVATION_EXPIRED"), { code: "functions/failed-precondition" })
    const callableSpy = vi.fn().mockRejectedValue(err)
    httpsCallableMock.mockReturnValue(callableSpy)

    render(<ConfirmPurchaseBanner reservationId="res1" />)
    fireEvent.click(screen.getByRole("button"))

    await waitFor(() => expect(showToastMock).toHaveBeenCalledTimes(1))
    const [, variant] = showToastMock.mock.calls[0]
    expect(variant).toBe("error")
  })

  it("disables button and sets aria-busy=true while pending", async () => {
    let resolve: (v: unknown) => void = () => {}
    const pending = new Promise((r) => { resolve = r })
    const callableSpy = vi.fn(() => pending)
    httpsCallableMock.mockReturnValue(callableSpy)

    render(<ConfirmPurchaseBanner reservationId="r" />)
    const button = screen.getByRole("button")
    fireEvent.click(button)

    await waitFor(() => {
      expect(button).toBeDisabled()
      expect(button.getAttribute("aria-busy")).toBe("true")
    })

    resolve({ data: { success: true } })
  })
})
