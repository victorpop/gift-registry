import * as Dialog from '@radix-ui/react-dialog'
import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useTranslation } from 'react-i18next'
import type { User } from 'firebase/auth'
import { Btn, Field, MonoCaption } from '../../components/giftmaison'
import { signUpEmail } from './authProviders'
import { useGuestIdentity } from './useGuestIdentity'

const schema = z.object({
  firstName: z.string().min(1, 'required'),
  lastName: z.string().min(1, 'required'),
  email: z.string().min(1, 'required').email('email'),
  password: z.string().min(8, 'weak'),
})

const guestSchema = schema.pick({ firstName: true, lastName: true, email: true })

type FormValues = z.infer<typeof schema>

export interface SaveYourSpotModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** Item being reserved — shown in the subtitle row. */
  itemName: string
  /** Called after Firebase account creation succeeds. Parent should reserve under the new user. */
  onAccountCreated: (user: User, firstName: string, lastName: string) => void
  /** Called when the user picks "Continue as guest". Parent should reserve as guest. */
  onContinueAsGuest: (firstName: string, lastName: string, email: string) => void
}

/**
 * Save-your-spot pre-reservation modal. Replaces the older GuestIdentityModal
 * in the Reserve button flow: a guest who clicks Reserve sees this modal first.
 *
 * Two paths:
 *  - Primary "Create account & reserve" → validates all fields including password,
 *    calls signUpEmail (Firebase createUserWithEmailAndPassword), then hands the new
 *    User to the parent so it can reserve under that account.
 *  - Secondary "Continue as guest" → validates first/last name + email only (no
 *    password), then hands those values to the parent so it can reserve as a guest.
 *
 * Persists the entered first/last/email into useGuestIdentity (localStorage) on
 * either path so the values pre-fill on next reserve.
 */
export default function SaveYourSpotModal({
  open,
  onOpenChange,
  itemName,
  onAccountCreated,
  onContinueAsGuest,
}: SaveYourSpotModalProps) {
  const { t } = useTranslation()
  const { identity, save: saveIdentity } = useGuestIdentity()
  const [serverError, setServerError] = useState<string | null>(null)
  const [guestPending, setGuestPending] = useState(false)

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { firstName: '', lastName: '', email: '', password: '' },
  })

  useEffect(() => {
    if (open) {
      form.reset({
        firstName: identity?.firstName ?? '',
        lastName: identity?.lastName ?? '',
        email: identity?.email ?? '',
        password: '',
      })
      setServerError(null)
    }
  }, [open, identity, form])

  function translateFieldError(message: string | undefined): string | undefined {
    if (!message) return undefined
    if (message === 'required') return t('save_your_spot.field_required')
    if (message === 'email') return t('save_your_spot.field_invalid_email')
    if (message === 'weak') return t('save_your_spot.error_weak_password')
    return message
  }

  function mapFirebaseError(code: string | undefined): string {
    switch (code) {
      case 'auth/email-already-in-use':
        return t('save_your_spot.error_email_in_use')
      case 'auth/weak-password':
        return t('save_your_spot.error_weak_password')
      case 'auth/invalid-email':
        return t('save_your_spot.error_invalid_email')
      default:
        return t('save_your_spot.error_generic')
    }
  }

  async function handleCreateAccount(values: FormValues) {
    setServerError(null)
    try {
      const user = await signUpEmail(values.email, values.password)
      saveIdentity({
        firstName: values.firstName,
        lastName: values.lastName,
        email: values.email,
      })
      onAccountCreated(user, values.firstName, values.lastName)
      onOpenChange(false)
    } catch (err: unknown) {
      const code = (err as { code?: string })?.code
      setServerError(mapFirebaseError(code))
    }
  }

  async function handleContinueAsGuestClick() {
    setServerError(null)
    const values = form.getValues()
    const result = guestSchema.safeParse(values)
    if (!result.success) {
      // Surface validation errors on the name/email fields only.
      const fieldErrors = result.error.flatten().fieldErrors
      ;(['firstName', 'lastName', 'email'] as const).forEach((key) => {
        const msg = fieldErrors[key]?.[0]
        if (msg) {
          form.setError(key, { type: 'manual', message: msg })
        }
      })
      return
    }
    setGuestPending(true)
    saveIdentity({
      firstName: result.data.firstName,
      lastName: result.data.lastName,
      email: result.data.email,
    })
    onContinueAsGuest(result.data.firstName, result.data.lastName, result.data.email)
    onOpenChange(false)
    setGuestPending(false)
  }

  const errors = form.formState.errors

  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 bg-gm-ink/40 z-40 backdrop-blur-[2px]" />
        <Dialog.Content
          className="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 w-[min(480px,90vw)] z-50 bg-gm-paper rounded-gm-modal p-6 shadow-gm-modal max-h-[90vh] overflow-y-auto"
          aria-describedby="save-your-spot-desc"
        >
          <Dialog.Title className="font-display text-[24px] text-gm-ink leading-[1.1] tracking-[-0.5px] font-normal">
            {t('save_your_spot.title')}
          </Dialog.Title>

          <div className="mt-2">
            <MonoCaption size="micro" tone="faint">
              {t('save_your_spot.subtitle_item', { itemName })}
            </MonoCaption>
          </div>

          <Dialog.Description
            id="save-your-spot-desc"
            className="mt-2 font-body text-[14.5px] text-gm-inkSoft leading-[1.55]"
          >
            {t('save_your_spot.body')}
          </Dialog.Description>

          <ul className="mt-4 flex flex-col gap-3 font-body text-[14.5px] text-gm-ink">
            <li className="flex items-start gap-3">
              <span aria-hidden="true" className="text-[16px] leading-[1.4]">🕒</span>
              <span className="leading-[1.4]">{t('save_your_spot.benefit_timer')}</span>
            </li>
            <li className="flex items-start gap-3">
              <span aria-hidden="true" className="text-[16px] leading-[1.4]">✉️</span>
              <span className="leading-[1.4]">{t('save_your_spot.benefit_email')}</span>
            </li>
            <li className="flex items-start gap-3">
              <span aria-hidden="true" className="text-[16px] leading-[1.4]">🎁</span>
              <span className="leading-[1.4]">{t('save_your_spot.benefit_registries')}</span>
            </li>
          </ul>

          <form
            onSubmit={form.handleSubmit(handleCreateAccount)}
            className="mt-5 flex flex-col gap-4"
            noValidate
          >
            <Field
              label={t('save_your_spot.first_name_label')}
              type="text"
              autoComplete="given-name"
              aria-invalid={Boolean(errors.firstName)}
              error={translateFieldError(errors.firstName ? String(errors.firstName.message) : undefined)}
              {...form.register('firstName')}
            />
            <Field
              label={t('save_your_spot.last_name_label')}
              type="text"
              autoComplete="family-name"
              aria-invalid={Boolean(errors.lastName)}
              error={translateFieldError(errors.lastName ? String(errors.lastName.message) : undefined)}
              {...form.register('lastName')}
            />
            <Field
              label={t('save_your_spot.email_label')}
              type="email"
              autoComplete="email"
              aria-invalid={Boolean(errors.email)}
              error={translateFieldError(errors.email ? String(errors.email.message) : undefined)}
              {...form.register('email')}
            />

            <div className="flex flex-col gap-2 mt-1">
              <MonoCaption size="micro" tone="faint">
                {t('save_your_spot.password_section_label')}
              </MonoCaption>
              <Field
                label={t('save_your_spot.password_label')}
                type="password"
                autoComplete="new-password"
                placeholder={t('save_your_spot.password_placeholder')}
                aria-invalid={Boolean(errors.password)}
                error={translateFieldError(errors.password ? String(errors.password.message) : undefined)}
                {...form.register('password')}
              />
            </div>

            {serverError && (
              <span role="alert" className="font-body text-[13px] text-gm-warn">
                {serverError}
              </span>
            )}

            <div className="flex flex-col-reverse sm:flex-row sm:justify-between sm:items-center gap-3 mt-2">
              <button
                type="button"
                onClick={handleContinueAsGuestClick}
                disabled={form.formState.isSubmitting || guestPending}
                className="font-body text-[13px] text-gm-inkSoft underline hover:text-gm-ink focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gm-accent disabled:opacity-50 disabled:cursor-not-allowed self-start sm:self-auto"
              >
                {t('save_your_spot.secondary_cta')}
              </button>
              <Btn
                type="submit"
                variant="primary"
                size="lg"
                disabled={form.formState.isSubmitting || guestPending}
              >
                {t('save_your_spot.primary_cta')}
              </Btn>
            </div>
          </form>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  )
}
