import type { HTMLAttributes, ReactNode } from 'react'

export interface StickyMobileBarProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode
}

/**
 * Mobile-only sticky-bottom container (CONTEXT D-12 + UI-SPEC "Mobile-only patterns").
 *
 * Pinned to the bottom of the viewport on mobile (< 640 px), hidden on tablet+.
 * Backdrop: bg-gm-paper at 85% opacity + backdrop-blur-sm.
 * Top border: 1 px gm.line.
 * Bottom padding: env(safe-area-inset-bottom) so the CTA never sits under the
 * iOS home indicator. Inner padding 12/16 px.
 *
 * Consumed by:
 *   - Plan 06 Auth screen — guest-skip card (most important affordance, must be 1-tap reachable)
 *   - Plan 04 sticky reserve banner stacks above this if both are active (z-index ordering)
 *
 * Requires `viewport-fit=cover` in <meta name="viewport"> (shipped in Plan 00 Task 3).
 */
export function StickyMobileBar({ children, className = '', ...rest }: StickyMobileBarProps) {
  return (
    <div
      className={[
        'fixed bottom-0 left-0 right-0 z-30 sm:hidden',
        'bg-gm-paper/85 backdrop-blur-sm border-t border-gm-line',
        'px-4 py-3',
        'pb-[calc(env(safe-area-inset-bottom)+12px)]',
        className,
      ].join(' ')}
      {...rest}
    >
      {children}
    </div>
  )
}

export default StickyMobileBar
