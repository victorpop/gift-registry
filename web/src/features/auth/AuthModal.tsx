import * as Dialog from '@radix-ui/react-dialog'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useTranslation } from 'react-i18next'
import { signInEmail, signUpEmail, signInWithGoogle } from './authProviders'

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

export default function AuthModal({ open, onOpenChange, onContinueAsGuest }: Props) {
  const { t } = useTranslation()
  const [mode, setMode] = useState<Mode>('signin')
  const [serverError, setServerError] = useState<string | null>(null)
  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: '', password: '' },
  })

  async function handleGoogle() {
    setServerError(null)
    try {
      const user = await signInWithGoogle()
      if (user) {
        onOpenChange(false)
      }
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
        <Dialog.Overlay className="fixed inset-0 bg-surface-on/40 z-40" />
        <Dialog.Content className="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 w-[min(480px,90vw)] z-50 bg-surface rounded-lg p-6 shadow-xl">
          <Dialog.Title className="text-xl font-semibold text-surface-on leading-tight">
            {mode === 'signin' ? t('auth.sign_in_title') : t('auth.sign_up_title')}
          </Dialog.Title>
          <Dialog.Description className="sr-only">
            {mode === 'signin' ? t('auth.sign_in_title') : t('auth.sign_up_title')}
          </Dialog.Description>

          <div className="flex gap-2 mt-4" role="tablist">
            <button
              type="button"
              role="tab"
              aria-selected={mode === 'signin'}
              onClick={() => setMode('signin')}
              className={`flex-1 h-11 rounded text-sm font-normal ${mode === 'signin' ? 'bg-primary text-primary-on' : 'bg-surface-variant text-surface-on'}`}
            >
              {t('auth.sign_in_title')}
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={mode === 'signup'}
              onClick={() => setMode('signup')}
              className={`flex-1 h-11 rounded text-sm font-normal ${mode === 'signup' ? 'bg-primary text-primary-on' : 'bg-surface-variant text-surface-on'}`}
            >
              {t('auth.sign_up_title')}
            </button>
          </div>

          <button
            type="button"
            onClick={handleGoogle}
            className="mt-4 w-full h-12 rounded border border-outline flex items-center justify-center gap-2 text-base font-normal text-surface-on hover:bg-surface-variant focus:ring-2 focus:ring-primary focus:outline-none"
          >
            <svg width="18" height="18" viewBox="0 0 18 18" aria-hidden="true">
              <path fill="#4285F4" d="M17.64 9.2c0-.64-.06-1.25-.17-1.84H9v3.48h4.84a4.14 4.14 0 0 1-1.8 2.72v2.25h2.91c1.7-1.57 2.69-3.88 2.69-6.6z" />
              <path fill="#34A853" d="M9 18c2.43 0 4.47-.8 5.96-2.18l-2.91-2.26c-.8.54-1.84.87-3.05.87-2.35 0-4.33-1.58-5.04-3.71H.96v2.33A9 9 0 0 0 9 18z" />
              <path fill="#FBBC05" d="M3.96 10.71A5.4 5.4 0 0 1 3.68 9c0-.59.1-1.17.28-1.71V4.96H.96A9 9 0 0 0 0 9c0 1.45.35 2.82.96 4.04l3-2.33z" />
              <path fill="#EA4335" d="M9 3.58c1.32 0 2.5.45 3.44 1.35l2.58-2.58A9 9 0 0 0 9 0 9 9 0 0 0 .96 4.96l3 2.33C4.67 5.16 6.65 3.58 9 3.58z" />
            </svg>
            {t('auth.sign_in_google')}
          </button>

          <div className="mt-4 flex items-center gap-2" aria-hidden="true">
            <hr className="flex-1 border-outline" />
            <span className="text-sm font-normal text-surface-onVariant">{t('auth.or_separator')}</span>
            <hr className="flex-1 border-outline" />
          </div>

          <form onSubmit={form.handleSubmit(handleSubmitEmail)} className="mt-4 flex flex-col gap-4" noValidate>
            <label className="flex flex-col gap-1">
              <span className="text-sm font-normal text-surface-on">{t('auth.email_label')}</span>
              <input
                type="email"
                autoComplete="email"
                className="h-12 px-3 rounded border border-outline text-base focus:ring-2 focus:ring-primary focus:outline-none bg-surface text-surface-on"
                {...form.register('email')}
              />
              {form.formState.errors.email && <span role="alert" className="text-sm text-destructive">{String(form.formState.errors.email.message)}</span>}
            </label>
            <label className="flex flex-col gap-1">
              <span className="text-sm font-normal text-surface-on">{t('auth.password_label')}</span>
              <input
                type="password"
                autoComplete={mode === 'signin' ? 'current-password' : 'new-password'}
                className="h-12 px-3 rounded border border-outline text-base focus:ring-2 focus:ring-primary focus:outline-none bg-surface text-surface-on"
                {...form.register('password')}
              />
              {form.formState.errors.password && <span role="alert" className="text-sm text-destructive">{String(form.formState.errors.password.message)}</span>}
            </label>
            {serverError && <span role="alert" className="text-sm text-destructive">{serverError}</span>}
            <button
              type="submit"
              disabled={form.formState.isSubmitting}
              className="mt-2 min-h-[48px] px-6 rounded-full bg-primary text-primary-on font-semibold text-base hover:bg-[#5B4397] focus:ring-2 focus:ring-primary focus:ring-offset-2 focus:outline-none disabled:opacity-50"
            >
              {mode === 'signin' ? t('auth.sign_in_title') : t('auth.sign_up_title')}
            </button>
          </form>

          <button
            type="button"
            onClick={() => { onOpenChange(false); onContinueAsGuest() }}
            className="mt-4 w-full text-sm font-normal text-surface-onVariant underline hover:text-surface-on"
          >
            {t('auth.continue_as_guest_link')}
          </button>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  )
}
