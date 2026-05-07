import type { HTMLAttributes, ReactNode } from 'react'

export interface MonoCaptionProps extends HTMLAttributes<HTMLSpanElement> {
  tone?: 'soft' | 'faint'
  size?: 'micro' | 'sm' | 'md'
  children: ReactNode
}

/**
 * Mono-caps caption span (UI-SPEC Atoms table).
 * Sizes: micro (10 px) / sm (11 px, default) / md (12 px).
 * Tones: soft = inkSoft, faint = inkFaint (default).
 * Letter-spacing scales with size: 1.2 / 1.3 / 1.5 px.
 */
export function MonoCaption({
  tone = 'faint',
  size = 'sm',
  className = '',
  children,
  ...rest
}: MonoCaptionProps) {
  const toneClass = tone === 'soft' ? 'text-gm-inkSoft' : 'text-gm-inkFaint'
  const sizeClass =
    size === 'micro' ? 'text-[10px] tracking-[1.2px]'
    : size === 'md' ? 'text-[12px] tracking-[1.5px]'
    : 'text-[11px] tracking-[1.3px]'
  return (
    <span
      className={`font-mono uppercase font-medium ${toneClass} ${sizeClass} ${className}`.trim()}
      {...rest}
    >
      {children}
    </span>
  )
}

export default MonoCaption
