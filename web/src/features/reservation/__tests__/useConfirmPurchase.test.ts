import { describe, it, expect, vi, beforeEach } from "vitest"
import { act, renderHook } from "@testing-library/react"

const httpsCallableMock = vi.fn()

vi.mock("firebase/functions", () => ({
  httpsCallable: (...args: unknown[]) => httpsCallableMock(...args),
}))

vi.mock("../../../firebase", () => ({
  functions: { __mock: true },
}))

import { useConfirmPurchase } from "../useConfirmPurchase"

describe("useConfirmPurchase", () => {
  beforeEach(() => {
    httpsCallableMock.mockReset()
  })

  it("starts in idle status with no error", () => {
    const { result } = renderHook(() => useConfirmPurchase())
    expect(result.current.status).toBe("idle")
    expect(result.current.error).toBeNull()
  })

  it("transitions idle -> pending -> success on happy path", async () => {
    const callableSpy = vi.fn(async ({ reservationId }: { reservationId: string }) => {
      expect(reservationId).toBe("res1")
      return { data: { success: true } }
    })
    httpsCallableMock.mockReturnValue(callableSpy)

    const { result } = renderHook(() => useConfirmPurchase())

    await act(async () => {
      await result.current.confirm("res1")
    })

    expect(callableSpy).toHaveBeenCalledTimes(1)
    expect(result.current.status).toBe("success")
    expect(result.current.error).toBeNull()
  })

  it("surfaces RESERVATION_EXPIRED message on failed-precondition error", async () => {
    const err = Object.assign(new Error("RESERVATION_EXPIRED"), { code: "functions/failed-precondition" })
    const callableSpy = vi.fn().mockRejectedValue(err)
    httpsCallableMock.mockReturnValue(callableSpy)

    const { result } = renderHook(() => useConfirmPurchase())

    await act(async () => {
      await result.current.confirm("res1")
    })

    expect(result.current.status).toBe("error")
    expect(result.current.error).toBe("RESERVATION_EXPIRED")
  })

  it("surfaces generic error message on other failures", async () => {
    const callableSpy = vi.fn().mockRejectedValue(new Error("network timeout"))
    httpsCallableMock.mockReturnValue(callableSpy)

    const { result } = renderHook(() => useConfirmPurchase())

    await act(async () => {
      await result.current.confirm("res1")
    })

    expect(result.current.status).toBe("error")
    expect(result.current.error).toBe("network timeout")
  })

  it("passes reservationId in the callable payload", async () => {
    const callableSpy = vi.fn(async (_payload: { reservationId: string }) => {
      return { data: { success: true } }
    })
    httpsCallableMock.mockReturnValue(callableSpy)

    const { result } = renderHook(() => useConfirmPurchase())

    await act(async () => {
      await result.current.confirm("res-xyz")
    })

    expect(callableSpy).toHaveBeenCalledWith({ reservationId: "res-xyz" })
  })

  it("calls httpsCallable with 'confirmPurchase' as the function name", async () => {
    const callableSpy = vi.fn(async () => ({ data: { success: true } }))
    httpsCallableMock.mockReturnValue(callableSpy)

    const { result } = renderHook(() => useConfirmPurchase())

    await act(async () => {
      await result.current.confirm("r")
    })

    expect(httpsCallableMock).toHaveBeenCalledWith(expect.anything(), "confirmPurchase")
  })

  it("resets error to null when confirm is retried after failure", async () => {
    const callableSpy = vi.fn()
      .mockRejectedValueOnce(new Error("first fail"))
      .mockResolvedValueOnce({ data: { success: true } })
    httpsCallableMock.mockReturnValue(callableSpy)

    const { result } = renderHook(() => useConfirmPurchase())

    await act(async () => {
      await result.current.confirm("r")
    })
    expect(result.current.error).toBe("first fail")

    await act(async () => {
      await result.current.confirm("r")
    })
    expect(result.current.error).toBeNull()
    expect(result.current.status).toBe("success")
  })
})
