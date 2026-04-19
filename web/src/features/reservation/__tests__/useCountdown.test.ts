import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { act, renderHook } from '@testing-library/react'
import { useCountdown } from '../useCountdown'

describe('useCountdown', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-04-19T12:00:00Z'))
  })
  afterEach(() => {
    vi.useRealTimers()
  })

  it('returns minutes/seconds for future expiry', () => {
    const expiresAt = Date.now() + 5 * 60 * 1000 // +5 min
    const { result } = renderHook(() => useCountdown(expiresAt))
    expect(result.current).toEqual({ minutes: 5, seconds: 0, totalSeconds: 300, expired: false })
  })

  it('advances when time elapses', () => {
    const expiresAt = Date.now() + 30 * 1000
    const { result } = renderHook(() => useCountdown(expiresAt))
    expect(result.current?.seconds).toBe(30)
    act(() => {
      vi.advanceTimersByTime(1000)
    })
    expect(result.current?.seconds).toBe(29)
    act(() => {
      vi.advanceTimersByTime(29_000)
    })
    expect(result.current?.expired).toBe(true)
  })

  it('returns expired=true for past expiry', () => {
    const expiresAt = Date.now() - 10 * 1000
    const { result } = renderHook(() => useCountdown(expiresAt))
    expect(result.current).toEqual({ minutes: 0, seconds: 0, totalSeconds: 0, expired: true })
  })

  it('returns null when expiresAtMs is null', () => {
    const { result } = renderHook(() => useCountdown(null))
    expect(result.current).toBeNull()
  })

  it('returns null when expiresAtMs is undefined', () => {
    const { result } = renderHook(() => useCountdown(undefined))
    expect(result.current).toBeNull()
  })
})
