import type { HTMLAttributes } from 'react'

export interface PulseDotProps extends HTMLAttributes<HTMLSpanElement> {
  /** Diameter in px. 8 (default — status pill, in-card banner) or 10 (sticky banner). */
  size?: 8 | 10
}

/**
 * Pulsing accent dot (CONTEXT D-11, UI-SPEC Animation).
 * Renders a circle in gm.accent with a soft accentSoft halo and the shared
 * `gm-pulse` keyframe animation (1.4 s alternate). Reduced-motion fallback
 * is applied at the @keyframes level in web/src/index.css.
 *
 * Size 8 px = Reserved item-card pill leading dot, in-card reserved banner.
 * Size 10 px = sticky reservation banner (Plan 04).
 */
export function PulseDot({ size = 8, className = '', ...rest }: PulseDotProps) {
  const dimClass = size === 10 ? 'w-[10px] h-[10px] shadow-[0_0_0_5px_var(--gm-accentSoft)]' : 'w-2 h-2 shadow-[0_0_0_4px_var(--gm-accentSoft)]'
  return (
    <span
      aria-hidden="true"
      className={`inline-block rounded-full bg-gm-accent animate-gm-pulse ${dimClass} ${className}`.trim()}
      {...rest}
    />
  )
}

export default PulseDot
