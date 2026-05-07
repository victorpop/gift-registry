import type { HTMLAttributes, ReactNode } from 'react'

export type PillTone = 'neutral' | 'accent' | 'second' | 'ok' | 'warn'
export type PillSize = 'sm' | 'md'

export interface PillProps extends HTMLAttributes<HTMLSpanElement> {
  tone?: PillTone
  size?: PillSize
  children: ReactNode
}

/**
 * Mono-caps pill (CONTEXT D-16). 11 px (sm) / 12 px (md), letter-spacing 0.3 px,
 * weight 500, rounded-full, 1 px border. Token mapping per UI-SPEC "Pill tone mapping".
 *
 * Status pill copy is uppercased and white-space: nowrap so chips don't wrap.
 *
 * The `ok` tone uses an oklch-derived green soft bg via arbitrary value
 * (`bg-[oklch(0.94_0.04_150)]`) since this colour is not in the gm.* palette
 * (UI-SPEC explicitly notes it as a computed value).
 */
export function Pill({ tone = 'neutral', size = 'sm', className = '', children, ...rest }: PillProps) {
  const toneClasses: Record<PillTone, string> = {
    neutral: 'bg-gm-paperDeep  text-gm-inkSoft  border-gm-line',
    accent:  'bg-gm-accentSoft text-gm-accent   border-transparent',
    second:  'bg-gm-secondSoft text-gm-second   border-transparent',
    ok:      'bg-[oklch(0.94_0.04_150)] text-gm-ok border-transparent',
    warn:    'bg-[oklch(0.95_0.04_70)]  text-gm-warn border-transparent',
  }
  const sizeClasses: Record<PillSize, string> = {
    sm: 'px-2 py-[3px] text-[11px]',
    md: 'px-[10px] py-[5px] text-[12px]',
  }
  return (
    <span
      className={[
        'inline-flex items-center gap-[5px] rounded-full border font-mono font-medium uppercase whitespace-nowrap tracking-[0.3px]',
        toneClasses[tone],
        sizeClasses[size],
        className,
      ].join(' ')}
      {...rest}
    >
      {children}
    </span>
  )
}

export default Pill
