import type { HTMLAttributes } from 'react'

export interface WordmarkProps extends HTMLAttributes<HTMLDivElement> {
  /** 22 px (default, body usage) or 24 px (top nav). */
  size?: 22 | 24
  /** Render the adjacent mono-caps "gift registry" tagline. Default false. */
  withTag?: boolean
}

/**
 * GiftMaison wordmark (CONTEXT D-13). Italic Instrument Serif `giftmaison.` with
 * the terminal period in `gm.accent`. Mirrors Android Phase 8 wordmark visually.
 *
 * Size 22 = body-level (footer, auth caption); size 24 = top nav.
 *
 * `withTag` adds " gift registry" mono-caps 10 px in `gm.inkFaint` (web-screens.jsx Logo).
 */
export function Wordmark({ size = 22, withTag = false, className = '', ...rest }: WordmarkProps) {
  const sizeClass = size === 24 ? 'text-[24px]' : 'text-[22px]'
  return (
    <div
      className={`flex items-baseline gap-2 ${className}`.trim()}
      {...rest}
    >
      <span
        className={`font-display italic text-gm-ink leading-none tracking-[-0.2px] ${sizeClass}`}
      >
        giftmaison<span className="text-gm-accent">.</span>
      </span>
      {withTag && (
        <span className="font-mono text-[10px] text-gm-inkFaint uppercase tracking-[1.5px] font-medium">
          gift registry
        </span>
      )}
    </div>
  )
}

export default Wordmark
