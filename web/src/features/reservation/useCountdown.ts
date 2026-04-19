import { useEffect, useState } from 'react'

export interface Countdown {
  minutes: number
  seconds: number
  totalSeconds: number
  expired: boolean
}

function compute(expiresAtMs: number, nowMs: number): Countdown {
  const diff = Math.max(0, expiresAtMs - nowMs)
  const totalSeconds = Math.floor(diff / 1000)
  return {
    minutes: Math.floor(totalSeconds / 60),
    seconds: totalSeconds % 60,
    totalSeconds,
    expired: totalSeconds === 0,
  }
}

/**
 * Emits a new countdown value every 1000ms from now until expiresAt.
 * Tolerates past timestamps (returns expired=true immediately).
 *
 * Timer authority is server-side (Cloud Function writes expiresAt; Cloud Tasks releases at that instant).
 * This hook is display-only — it never enforces anything.
 */
export function useCountdown(expiresAtMs: number | null | undefined): Countdown | null {
  const [countdown, setCountdown] = useState<Countdown | null>(() =>
    expiresAtMs != null ? compute(expiresAtMs, Date.now()) : null,
  )

  useEffect(() => {
    if (expiresAtMs == null) {
      setCountdown(null)
      return
    }
    setCountdown(compute(expiresAtMs, Date.now()))
    const interval = setInterval(() => {
      setCountdown(compute(expiresAtMs, Date.now()))
    }, 1000)
    return () => clearInterval(interval)
  }, [expiresAtMs])

  return countdown
}
