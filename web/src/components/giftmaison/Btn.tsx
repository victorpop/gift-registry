import type { ButtonHTMLAttributes, ReactNode } from 'react'

export type BtnVariant = 'primary' | 'accent' | 'ghost' | 'quiet'
export type BtnSize = 'sm' | 'md' | 'lg'

export interface BtnProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: BtnVariant
  size?: BtnSize
  /** Optional leading icon node (lucide-react glyph). */
  icon?: ReactNode
  children: ReactNode
}

/**
 * Pill-shaped button (UI-SPEC Component Inventory > Atoms > Btn).
 *
 * Variants:
 *   primary = ink bg / paper fg (default page primary CTA)
 *   accent  = accent bg / accentInk fg (sticky banner Continue, confirm-back CTA)
 *   ghost   = transparent bg / ink fg / line border (Sign in, Google, Skip)
 *   quiet   = transparent bg / inkSoft fg / no border (Release reservation on dark banner — pass className to override fg/border for the dark surface)
 *
 * Sizes (vertical/horizontal padding · font size):
 *   sm = 7/12 px · 12 px   md = 11/18 px · 13.5 px   lg = 14/22 px · 15 px
 * All sizes lineHeight: 1, letter-spacing -0.1 px, font-medium Inter.
 *
 * Focus ring: 2 px gm-accent outline + 2 px offset on :focus-visible (UI-SPEC a11y).
 *
 * Always renders <button>. For link buttons (e.g. "Continue to {retailer}"),
 * the consuming screen uses a sibling <a> styled identically — Plan 04 inlines
 * the same Btn classes on its <a target="_blank"> element.
 */
export function Btn({
  variant = 'primary',
  size = 'md',
  icon,
  className = '',
  children,
  type = 'button',
  ...rest
}: BtnProps) {
  const variantClasses: Record<BtnVariant, string> = {
    primary: 'bg-gm-ink    text-gm-paper    border-gm-ink',
    accent:  'bg-gm-accent text-gm-accentInk border-gm-accent',
    ghost:   'bg-transparent text-gm-ink     border-gm-line',
    quiet:   'bg-transparent text-gm-inkSoft border-transparent',
  }
  const sizeClasses: Record<BtnSize, string> = {
    sm: 'px-3 py-[7px] text-[12px]',     // 12 / 7 px (web-screens.jsx Btn `sm`)
    md: 'px-[18px] py-[11px] text-[13.5px]', // 18 / 11 px
    lg: 'px-[22px] py-[14px] text-[15px]',   // 22 / 14 px
  }
  return (
    <button
      type={type}
      className={[
        'inline-flex items-center justify-center gap-2 rounded-full border font-body font-medium tracking-[-0.1px] leading-none cursor-pointer',
        'focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gm-accent',
        'disabled:opacity-50 disabled:cursor-not-allowed',
        variantClasses[variant],
        sizeClasses[size],
        className,
      ].join(' ')}
      {...rest}
    >
      {icon}
      {children}
    </button>
  )
}

export default Btn
