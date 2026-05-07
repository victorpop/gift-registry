import { useState } from 'react'
import { useNavigate } from 'react-router'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useTranslation } from 'react-i18next'
import { TopNav, Footer, Wordmark, Btn, Field, MonoCaption, StickyMobileBar } from '../../components/giftmaison'
import EditorialPhoto from './EditorialPhoto'
import GuestSkipCard from './GuestSkipCard'
import { signInEmail, signUpEmail, signInWithGoogle } from './authProviders'
import { useAuth } from './useAuth'

const schema = z.object({
  email: z.string().min(1, 'required').email('email'),
  password: z.string().min(6, 'password-min-6'),
})
type FormValues = z.infer<typeof schema>

type Mode = 'signin' | 'signup'

/**
 * Full-page auth screen at /sign-in (UI-SPEC Screen 03 + CONTEXT D-05).
 *
 * Layout:
 *   - Mobile: form column full-bleed; <EditorialPhoto /> hides itself; <StickyMobileBar>
 *     wraps GuestSkipCard pinned to viewport bottom.
 *   - Tablet (640–1024 px): form column full-width; photo still hidden.
 *   - Desktop (≥ 1024 px): split lg:grid-cols-[520px_1fr] — form left, photo right.
 *
 * Default tab: "Sign in" for returning users (the historic Phase 5 default —
 * the "default to sign-up on first visit" heuristic is Claude's-discretion
 * follow-up; v1.1 ships with the simpler default).
 */
export default function AuthScreen() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { user } = useAuth()
  const [mode, setMode] = useState<Mode>('signin')
  const [serverError, setServerError] = useState<string | null>(null)
  const [showPassword, setShowPassword] = useState(false)

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: '', password: '' },
  })

  // Already signed in? bounce back to /
  if (user) {
    navigate('/', { replace: true })
    return null
  }

  async function handleGoogle() {
    setServerError(null)
    try {
      const u = await signInWithGoogle()
      if (u) navigate(-1)
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
      navigate(-1)
    } catch {
      setServerError(t('common.error_generic'))
    }
  }

  function handleSkip() {
    // Continuing as guest just bounces back; the receiving page (e.g. /registry/:id)
    // already handles guest identity prompts.
    navigate(-1)
  }

  return (
    <div className="min-h-screen flex flex-col bg-gm-paper">
      <TopNav />
      <main className="flex-1 grid grid-cols-1 lg:grid-cols-[520px_1fr]">
        {/* Form column */}
        <div className="px-4 sm:px-7 lg:px-14 py-10 lg:py-14 flex flex-col">
          <Wordmark size={22} />
          <div className="mt-12 lg:mt-16 max-w-[400px] flex flex-col gap-5">
            <MonoCaption size="md" tone="faint">{t('web_auth.caption')}</MonoCaption>
            <h1 className="font-display text-[28px] sm:text-[36px] lg:text-[44px] text-gm-ink leading-[1.05] tracking-[-1px] m-0">
              {t('web_auth.headline_pre')}
              <span className="italic text-gm-ink">{t('web_auth.headline_emphasis')}</span>
              {t('web_auth.headline_post')}
            </h1>
            <p className="font-body text-[14.5px] text-gm-inkSoft leading-[1.55] m-0">
              {t('web_auth.subline')}
            </p>

            {/* Tabs */}
            <div role="tablist" aria-label="Auth mode" className="flex gap-6 border-b border-gm-line mt-2">
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

            <form onSubmit={form.handleSubmit(handleSubmitEmail)} className="flex flex-col gap-4 mt-2" noValidate>
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

            {/* Divider */}
            <div className="flex items-center gap-3 my-2">
              <hr className="flex-1 border-gm-line" />
              <MonoCaption size="micro" tone="faint">{t('auth.or_separator')}</MonoCaption>
              <hr className="flex-1 border-gm-line" />
            </div>

            {/* Google CTA */}
            <Btn variant="ghost" size="lg" onClick={handleGoogle} className="w-full" icon={
              <svg width="18" height="18" viewBox="0 0 18 18" aria-hidden="true">
                <path fill="#4285F4" d="M17.64 9.2c0-.64-.06-1.25-.17-1.84H9v3.48h4.84a4.14 4.14 0 0 1-1.8 2.72v2.25h2.91c1.7-1.57 2.69-3.88 2.69-6.6z" />
                <path fill="#34A853" d="M9 18c2.43 0 4.47-.8 5.96-2.18l-2.91-2.26c-.8.54-1.84.87-3.05.87-2.35 0-4.33-1.58-5.04-3.71H.96v2.33A9 9 0 0 0 9 18z" />
                <path fill="#FBBC05" d="M3.96 10.71A5.4 5.4 0 0 1 3.68 9c0-.59.1-1.17.28-1.71V4.96H.96A9 9 0 0 0 0 9c0 1.45.35 2.82.96 4.04l3-2.33z" />
                <path fill="#EA4335" d="M9 3.58c1.32 0 2.5.45 3.44 1.35l2.58-2.58A9 9 0 0 0 9 0 9 9 0 0 0 .96 4.96l3 2.33C4.67 5.16 6.65 3.58 9 3.58z" />
              </svg>
            }>
              {t('auth.sign_in_google')}
            </Btn>

            {/* Guest skip — desktop inline */}
            <div className="hidden lg:block mt-4">
              <GuestSkipCard onSkip={handleSkip} />
            </div>
          </div>
        </div>

        {/* Editorial photo column — desktop-only */}
        <EditorialPhoto />
      </main>

      {/* Footer hidden on mobile when sticky guest skip is showing — caller adjusts pb */}
      <Footer />

      {/* Mobile guest skip — pinned to viewport bottom */}
      <StickyMobileBar>
        <GuestSkipCard onSkip={handleSkip} />
      </StickyMobileBar>
    </div>
  )
}
