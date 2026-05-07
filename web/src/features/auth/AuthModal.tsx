import * as Dialog from '@radix-ui/react-dialog'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useTranslation } from 'react-i18next'
import { signInEmail, signUpEmail, signInWithGoogle } from './authProviders'
import { Btn, Field, MonoCaption } from '../../components/giftmaison'

const schema = z.object({
  email: z.string().min(1, 'required').email('email'),
  password: z.string().min(6, 'password-min-6'),
})
type FormValues = z.infer<typeof schema>

type Mode = 'signin' | 'signup'

interface Props {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** Called when the user picks "Continue as guest" — Plan 06 will open GuestIdentityModal next. */
  onContinueAsGuest: () => void
}

/**
 * In-page auth modal (Phase 13 restyle of Phase 5 AuthModal).
 *
 * Visual shell: Radix Dialog with rounded-gm-modal (20 px), shadow-gm-modal,
 * backdrop bg-gm-ink/40. Same atom shapes as <AuthScreen /> but contained
 * in a centred 480 px card that overlays the current page (RegistryPage).
 *
 * API contract preserved verbatim from Phase 5 (open / onOpenChange /
 * onContinueAsGuest props; signInEmail/signUpEmail/signInWithGoogle wiring;
 * react-hook-form + zod validation).
 */
export default function AuthModal({ open, onOpenChange, onContinueAsGuest }: Props) {
  const { t } = useTranslation()
  const [mode, setMode] = useState<Mode>('signin')
  const [serverError, setServerError] = useState<string | null>(null)
  const [showPassword, setShowPassword] = useState(false)
  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: '', password: '' },
  })

  async function handleGoogle() {
    setServerError(null)
    try {
      const u = await signInWithGoogle()
      if (u) onOpenChange(false)
    } catch {
      setServerError(t('common.error_generic'))
    }
  }

  async function handleSubmitEmail(values: FormValues) {
    setServerError(null)
    try {
      if (mode === 'signin') {
        await signInEmail(values.email, values.password)
      } else {
        await signUpEmail(values.email, values.password)
      }
      onOpenChange(false)
    } catch {
      setServerError(t('common.error_generic'))
    }
  }

  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 bg-gm-ink/40 z-40 backdrop-blur-[2px]" />
        <Dialog.Content className="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 w-[min(480px,90vw)] z-50 bg-gm-paper rounded-gm-modal p-6 shadow-gm-modal">
          <Dialog.Title className="font-display text-[24px] text-gm-ink leading-[1.1] tracking-[-0.5px] font-normal">
            {mode === 'signin' ? t('auth.sign_in_title') : t('auth.sign_up_title')}
          </Dialog.Title>
          <Dialog.Description className="sr-only">
            {mode === 'signin' ? t('auth.sign_in_title') : t('auth.sign_up_title')}
          </Dialog.Description>

          {/* Tabs */}
          <div role="tablist" className="flex gap-6 border-b border-gm-line mt-4">
            <button
              role="tab"
              type="button"
              aria-selected={mode === 'signin'}
              onClick={() => setMode('signin')}
              className={[
                'pb-3 -mb-px font-body text-[14px] tracking-[-0.1px] focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gm-accent',
                mode === 'signin' ? 'text-gm-ink font-medium border-b-2 border-gm-ink' : 'text-gm-inkFaint font-normal',
              ].join(' ')}
            >
              {t('auth.sign_in_title')}
            </button>
            <button
              role="tab"
              type="button"
              aria-selected={mode === 'signup'}
              onClick={() => setMode('signup')}
              className={[
                'pb-3 -mb-px font-body text-[14px] tracking-[-0.1px] focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gm-accent',
                mode === 'signup' ? 'text-gm-ink font-medium border-b-2 border-gm-ink' : 'text-gm-inkFaint font-normal',
              ].join(' ')}
            >
              {t('auth.sign_up_title')}
            </button>
          </div>

          {/* Google CTA — top placement preserved from Phase 5 modal layout */}
          <Btn variant="ghost" size="lg" onClick={handleGoogle} className="w-full mt-4" icon={
            <svg width="18" height="18" viewBox="0 0 18 18" aria-hidden="true">
              <path fill="#4285F4" d="M17.64 9.2c0-.64-.06-1.25-.17-1.84H9v3.48h4.84a4.14 4.14 0 0 1-1.8 2.72v2.25h2.91c1.7-1.57 2.69-3.88 2.69-6.6z" />
              <path fill="#34A853" d="M9 18c2.43 0 4.47-.8 5.96-2.18l-2.91-2.26c-.8.54-1.84.87-3.05.87-2.35 0-4.33-1.58-5.04-3.71H.96v2.33A9 9 0 0 0 9 18z" />
              <path fill="#FBBC05" d="M3.96 10.71A5.4 5.4 0 0 1 3.68 9c0-.59.1-1.17.28-1.71V4.96H.96A9 9 0 0 0 0 9c0 1.45.35 2.82.96 4.04l3-2.33z" />
              <path fill="#EA4335" d="M9 3.58c1.32 0 2.5.45 3.44 1.35l2.58-2.58A9 9 0 0 0 9 0 9 9 0 0 0 .96 4.96l3 2.33C4.67 5.16 6.65 3.58 9 3.58z" />
            </svg>
          }>
            {t('auth.sign_in_google')}
          </Btn>

          {/* Divider */}
          <div className="flex items-center gap-3 mt-4">
            <hr className="flex-1 border-gm-line" />
            <MonoCaption size="micro" tone="faint">{t('auth.or_separator')}</MonoCaption>
            <hr className="flex-1 border-gm-line" />
          </div>

          {/* Form */}
          <form onSubmit={form.handleSubmit(handleSubmitEmail)} className="flex flex-col gap-4 mt-4" noValidate>
            <Field
              label={t('auth.email_label')}
              type="email"
              autoComplete="email"
              aria-invalid={Boolean(form.formState.errors.email)}
              error={form.formState.errors.email ? String(form.formState.errors.email.message) : undefined}
              {...form.register('email')}
            />
            <Field
              label={t('auth.password_label')}
              type={showPassword ? 'text' : 'password'}
              autoComplete={mode === 'signin' ? 'current-password' : 'new-password'}
              aria-invalid={Boolean(form.formState.errors.password)}
              error={form.formState.errors.password ? String(form.formState.errors.password.message) : undefined}
              suffix={
                <button
                  type="button"
                  onClick={() => setShowPassword(s => !s)}
                  className="font-mono text-[10px] uppercase tracking-[1.3px] text-gm-inkFaint hover:text-gm-ink"
                >
                  {showPassword ? t('web_auth.hide_password') : t('web_auth.show_password')}
                </button>
              }
              {...form.register('password')}
            />
            {serverError && (
              <span role="alert" className="font-body text-[13px] text-gm-warn">
                {serverError}
              </span>
            )}
            <Btn type="submit" variant="primary" size="lg" disabled={form.formState.isSubmitting} className="w-full">
              {mode === 'signin' ? t('auth.sign_in_title') : t('auth.sign_up_title')}
            </Btn>
          </form>

          {/* Continue as guest */}
          <button
            type="button"
            onClick={() => { onOpenChange(false); onContinueAsGuest() }}
            className="mt-4 w-full font-body text-[13px] text-gm-inkSoft underline hover:text-gm-ink focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gm-accent"
          >
            {t('auth.continue_as_guest_link')}
          </button>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  )
}
