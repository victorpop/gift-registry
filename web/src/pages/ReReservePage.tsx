import { useEffect, useRef } from 'react'
import { useNavigate, useParams } from 'react-router'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../features/auth/useAuth'
import { useResolveReservation } from '../features/reservation/useResolveReservation'

/**
 * Re-reserve email deep link landing page.
 *
 * Flow:
 *   1. Wait for Firebase Auth cold-start resolution (useAuth.isReady) — Pitfall 7.
 *   2. Call resolveReservation callable with reservationId from URL params.
 *   3. On success -> navigate to /registry/:registryId?autoReserveItemId=:itemId (replace).
 *   4. On error -> navigate to / (replace).
 *
 * Visible UI: centered "Checking your reservation…" text while the callable is in flight.
 * No other UI; this is a transient redirect page.
 *
 * Guard: hasFiredRef prevents React 18 StrictMode double-invocation from firing twice.
 */
export default function ReReservePage() {
  const { t } = useTranslation()
  const { id: reservationId } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { isReady } = useAuth()
  const resolve = useResolveReservation()
  const hasFiredRef = useRef(false)

  useEffect(() => {
    // Gate 1: wait for Firebase Auth to resolve its persisted session (Pitfall 7).
    if (!isReady) return

    // Gate 2: missing reservationId param → navigate away immediately.
    if (!reservationId) {
      navigate('/', { replace: true })
      return
    }

    // Gate 3: idempotency guard — fires exactly once per page mount.
    if (hasFiredRef.current) return
    // Also guard against mutation already being in a terminal state (StrictMode safety).
    if (resolve.isPending || resolve.isSuccess || resolve.isError) return

    hasFiredRef.current = true
    resolve.mutate(
      { reservationId },
      {
        onSuccess: (data) => {
          navigate(`/registry/${data.registryId}?autoReserveItemId=${data.itemId}`, { replace: true })
        },
        onError: () => {
          navigate('/', { replace: true })
        },
      },
    )
  }, [isReady, reservationId, navigate, resolve])

  return (
    <div className="min-h-screen flex items-center justify-center bg-surface px-6">
      <span
        role="status"
        aria-live="polite"
        className="text-base font-normal text-surface-on"
      >
        {t('reservation.resolving')}
      </span>
    </div>
  )
}
