import { forwardRef, type InputHTMLAttributes, type ReactNode } from 'react'

export interface FieldProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'prefix'> {
  /** Mono-caps label text. Required (a11y). */
  label: string
  /** Optional helper text below the input. */
  hint?: string
  /** Renders the "✓ auto-filled" tag next to the label (D-form auto-fill UX). */
  autofilled?: boolean
  /** Optional prefix node (e.g., currency, lock icon). */
  prefix?: ReactNode
  /** Optional suffix node (e.g., "show" toggle on password fields). */
  suffix?: ReactNode
  /** Optional error message under the field; uses gm.warn or destructive copy. */
  error?: string
}

/**
 * Form field wrapper (UI-SPEC Component Inventory > Atoms > Field).
 *
 * Mono-caps label 10 px in gm.inkFaint, then a bordered input row:
 *   - 11/14 px padding (web-screens.jsx Field literal — kept verbatim)
 *   - rounded-[10px], 1 px gm.line border, 1.5 px gm.accent on :focus-within
 *   - 14 px Inter body, gm.ink text, gm.inkFaint placeholder
 *
 * Compatible with react-hook-form via `{...register('email')}` spread → forwarded ref.
 *
 * Plan 06 (Auth screen restyle) is the primary consumer: email + password fields.
 */
export const Field = forwardRef<HTMLInputElement, FieldProps>(function Field(
  { label, hint, autofilled, prefix, suffix, error, className = '', ...inputProps },
  ref,
) {
  return (
    <label className={`flex flex-col gap-[6px] ${className}`.trim()}>
      <span className="font-mono text-[10px] uppercase tracking-[1.3px] text-gm-inkFaint font-medium">
        {label}
        {autofilled && <span className="text-gm-ok ml-2">✓ auto-filled</span>}
      </span>
      <div className="flex items-center gap-2 px-[14px] py-[11px] rounded-[10px] bg-gm-paper border border-gm-line focus-within:border-[1.5px] focus-within:border-gm-accent transition-colors">
        {prefix && <span className="text-gm-inkFaint text-[13px]">{prefix}</span>}
        <input
          ref={ref}
          className="flex-1 font-body text-[14px] text-gm-ink placeholder:text-gm-inkFaint bg-transparent outline-none"
          {...inputProps}
        />
        {suffix}
      </div>
      {hint && <span className="font-body text-[11.5px] text-gm-inkFaint">{hint}</span>}
      {error && (
        <span role="alert" className="font-body text-[12px] text-gm-warn">
          {error}
        </span>
      )}
    </label>
  )
})

export default Field
