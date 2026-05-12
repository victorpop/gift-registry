import { useEffect, useMemo, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { useActiveReservation } from './useActiveReservation'
import { useCountdown } from './useCountdown'
import { useReleaseReservation } from './useReleaseReservation'
import { useAuth } from '../auth/useAuth'
import { useGuestIdentity } from '../auth/useGuestIdentity'
import { useToast } from '../../components/ToastProvider'
import { Btn, PulseDot } from '../../components/giftmaison'

/**
 * Sticky reservation banner (CONTEXT D-04 + UI-SPEC Sticky reserve banner anatomy).
 *
 * Position: `sticky top-0 z-30` so it sits above the TopNav while a reservation
 * is held. Visible when useActiveReservation.active is non-null AND the timer
 * has not expired. Auto-clears when countdown.expired flips true (preserves
 * Phase 5 behaviour from ReservationBanner).
 *
 * Layout:
 *   - Mobile: 2 rows. Message + pulsing dot above; full-width [Release] [Continue] stacked.
 *   - Tablet+: 1 row. Pulsing dot + message left; [Release] + [Continue] right.
 *
 * Background gm.ink, foreground gm.paper. Btn `quiet` (Release) overrides text-gm-paper
 * + 1 px paper/20 border on the dark surface. Btn `accent` (Continue) is rendered as
 * an <a> styled identically — Continue must open the affiliate URL in a new tab.
 *
 * aria-live="polite" announces minute changes only (UI-SPEC Accessibility):
 * a hidden span whose text re-renders only when `Math.floor(seconds/60)` changes.
 * The visible MM:SS digits are NOT inside the live region.
 *
 * Implementation note (revised v1.1): the rendered string instance is cached in a
 * ref keyed by Math.floor(totalSeconds/60). useMemo returns the SAME string instance
 * between minute ticks — text-node identity is preserved between minute boundaries,
 * so screen readers only re-announce when minutes flip, not on every 1 s countdown
 * re-render.
 */
export default function StickyReserveBanner() {
  const { t } = useTranslation()
  const { active, clear } = useActiveReservation()
  const countdown = useCountdown(active?.expiresAtMs ?? null)
  const { release, status: releaseStatus, error: releaseError } = useReleaseReservation()
  const { user } = useAuth()
  const { identity } = useGuestIdentity()
  const { showToast } = useToast()

  // Signed-in path: send undefined (backend uses auth.uid). Guest: send identity.email.
  const giverEmailToSend = user ? undefined : (identity?.email ?? undefined)

  // Refs to prevent duplicate toasts on re-render.
  const releaseSuccessToastedRef = useRef(false)
  const releaseErrorToastedForRef = useRef<string | null>(null)

  // Auto-dismiss on expiry (Phase 5 contract preserved).
  useEffect(() => {
    if (countdown?.expired) {
      clear()
    }
  }, [countdown?.expired, clear])

  // Show success toast and clear local state after a successful release.
  useEffect(() => {
    if (releaseStatus === 'success' && !releaseSuccessToastedRef.current) {
      releaseSuccessToastedRef.current = true
      showToast(t('reservation.release_success'), 'success')
      clear()
    }
  }, [releaseStatus, showToast, t, clear])

  // Show error toast when release fails (once per error message).
  useEffect(() => {
    if (releaseStatus === 'error' && releaseError && releaseErrorToastedForRef.current !== releaseError) {
      releaseErrorToastedForRef.current = releaseError
      showToast(t('reservation.release_error'), 'error')
    }
  }, [releaseStatus, releaseError, showToast, t])

  // Minute-only aria-live announcement (UI-SPEC: announce once per minute, NOT every second).
  // Strategy: cache the rendered string in a ref keyed by Math.floor(seconds/60). The
  // useMemo returns the SAME string instance between minute ticks, so the rendered
  // text node identity does not change — screen readers only re-announce when minutes flip.
  const lastMinutesRef = useRef<number | null>(null)
  const cachedAriaTextRef = useRef<string>('')

  const ariaText = useMemo(() => {
    const m = countdown ? Math.max(0, Math.floor(countdown.totalSeconds / 60)) : 0
    if (lastMinutesRef.current !== m) {
      lastMinutesRef.current = m
      cachedAriaTextRef.current = t('web_reserve.banner_aria_live', { minutes: m })
    }
    return cachedAriaTextRef.current
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [countdown?.totalSeconds, t])

  if (!active || !countdown || countdown.expired) return null

  const mm = String(countdown.minutes).padStart(2, '0')
  const ss = String(countdown.seconds).padStart(2, '0')
  const mmss = `${mm}:${ss}`
  const retailerLabel = active.merchantDomain ?? 'retailer'

  return (
    <div
      role="status"
      className="sticky top-0 z-30 bg-gm-ink text-gm-paper"
      data-testid="sticky-reserve-banner"
    >
      {/* Visible content */}
      <div className="flex flex-col gap-3 px-4 py-[14px] sm:flex-row sm:items-center sm:justify-between sm:gap-5 sm:px-7 lg:px-10">
        {/* Left: pulse dot + message stack */}
        <div className="flex items-start sm:items-center gap-[14px]">
          <PulseDot size={10} className="mt-1 sm:mt-0 flex-shrink-0" />
          <div className="flex flex-col gap-[2px]">
            <div className="font-body text-[14px] font-medium leading-tight">
              {t('web_reserve.banner_heading_pre')}
              <span className="font-display italic font-normal">
                {t('web_reserve.banner_heading_emphasis', { itemName: active.itemName })}
              </span>
              {t('web_reserve.banner_heading_post')}
            </div>
            <div className="font-mono text-[11px] tracking-[0.5px] opacity-65">
              {t('web_reserve.banner_subline_pre')}
              <span className="text-gm-paper/90">{t('web_reserve.banner_subline_retailer', { retailer: retailerLabel })}</span>
              {t('web_reserve.banner_subline_separator')}
              <span data-testid="banner-mmss">{t('web_reserve.banner_subline_countdown', { mmss })}</span>
            </div>
          </div>
        </div>

        {/* Right: action buttons. Mobile: full-width stacked. Tablet+: row. */}
        <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:gap-[10px]">
          <Btn
            variant="quiet"
            size="sm"
            onClick={async () => {
              if (!active) return
              await release(active.reservationId, giverEmailToSend)
            }}
            disabled={releaseStatus === 'pending'}
            aria-busy={releaseStatus === 'pending'}
            className="text-gm-paper border border-gm-paper/20 hover:bg-gm-paper/10 w-full sm:w-auto"
          >
            {t('web_reserve.release_cta')}
          </Btn>
          <a
            href={active.affiliateUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center justify-center gap-2 rounded-full border border-gm-accent bg-gm-accent text-gm-accentInk font-body font-medium tracking-[-0.1px] leading-none cursor-pointer px-3 py-[7px] text-[12px] focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gm-paper w-full sm:w-auto"
          >
            {t('web_reserve.continue_cta', { retailer: retailerLabel })}
          </a>
        </div>
      </div>

      {/* aria-live minute announcer (visually hidden, polite) */}
      <span aria-live="polite" className="sr-only" data-testid="aria-minute-announcer">
        {ariaText}
      </span>
    </div>
  )
}
