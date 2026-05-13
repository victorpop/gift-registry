import * as Dialog from '@radix-ui/react-dialog'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useTranslation } from 'react-i18next'
import { Btn, Field, MonoCaption } from '../../components/giftmaison'
import { signUpEmail } from './authProviders'

const schema = z.object({
  password: z.string().min(8, 'weak'),
})

type FormValues = z.infer<typeof schema>

export interface SaveYourSpotModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** Pre-filled, read-only, displayed in footer hint. Sourced from the active reservation. */
  email: string
  /** null → use title_no_name variant; otherwise interpolated into title_with_name. */
  firstName: string | null
  /** Item name shown in the subtitle row (e.g. "✓ RESERVED · {itemName} · 12:34 left"). */
  itemName: string
  /** Live "MM:SS" string pulled from the parent's useCountdown — updates each tick. */
  mmss: string
  /** Called by parent when "Not now, thanks" is clicked. Parent persists dismissal. */
  onDismiss: () => void
  /** Called by parent on success so it can show a toast / persist dismissal. */
  onSuccess: () => void
}

/**
 * Save-your-spot upsell modal shown on ItemReservePage after a guest reservation.
 *
 * Triggers `signUpEmail` (Firebase createUserWithEmailAndPassword) to create a real
 * Firebase account using the email already on the reservation. The reservation
 * continues to hydrate by email after sign-up because giver matching is email-based
 * for guest reservations (giverEmail key in backend, giverId stays null until linked).
 *
 * Design note: this project has NO anonymous-auth path, so we do NOT call
 * linkWithCredential. The simpler flow is just "create a real account with the
 * guest's email"; useReservationForItem re-runs after user flips from null to the
 * new User, but its key is effectiveEmail (= user.email ?? identity.email), which
 * is unchanged — so the same reservation continues to resolve.
 *
 * All visible strings are sourced from i18n (`save_your_spot.*`).
 */
export default function SaveYourSpotModal({
  open,
  onOpenChange,
  email,
  firstName,
  itemName,
  mmss,
  onDismiss,
  onSuccess,
}: SaveYourSpotModalProps) {
  const { t } = useTranslation()
  const [serverError, setServerError] = useState<string | null>(null)

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { password: '' },
  })

  function translateFieldError(message: string | undefined): string | undefined {
    if (!message) return undefined
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

  async function handleValid(values: FormValues) {
    setServerError(null)
    try {
      await signUpEmail(email, values.password)
      onSuccess()
      onOpenChange(false)
    } catch (err: unknown) {
      const code = (err as { code?: string })?.code
      setServerError(mapFirebaseError(code))
    }
  }

  function handleDismissClick() {
    onDismiss()
    onOpenChange(false)
  }

  const title = firstName
    ? t('save_your_spot.title_with_name', { firstName })
    : t('save_your_spot.title_no_name')

  const separator = t('save_your_spot.subtitle_separator')
  const subtitleStatus = t('save_your_spot.subtitle_status')
  const subtitleCountdown = t('save_your_spot.subtitle_countdown', { mmss })

  const passwordError = translateFieldError(
    form.formState.errors.password ? String(form.formState.errors.password.message) : undefined,
  )

  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 bg-gm-ink/40 z-40 backdrop-blur-[2px]" />
        <Dialog.Content
          className="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 w-[min(480px,90vw)] z-50 bg-gm-paper rounded-gm-modal p-6 shadow-gm-modal"
          aria-describedby="save-your-spot-desc"
        >
          <Dialog.Title className="font-display text-[24px] text-gm-ink leading-[1.1] tracking-[-0.5px] font-normal">
            {title}
          </Dialog.Title>

          <div className="mt-2">
            <MonoCaption size="micro" tone="faint">
              {subtitleStatus}
              {separator}
              {itemName}
              {separator}
              {subtitleCountdown}
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
            onSubmit={form.handleSubmit(handleValid)}
            className="mt-5 flex flex-col gap-3"
            noValidate
          >
            <div className="flex flex-col gap-2">
              <MonoCaption size="micro" tone="faint">
                {t('save_your_spot.password_section_label')}
              </MonoCaption>
              <Field
                label={t('save_your_spot.password_label')}
                type="password"
                autoComplete="new-password"
                placeholder={t('save_your_spot.password_placeholder')}
                aria-invalid={Boolean(form.formState.errors.password)}
                error={passwordError}
                {...form.register('password')}
              />
            </div>

            {serverError && (
              <span role="alert" className="font-body text-[13px] text-gm-warn">
                {serverError}
              </span>
            )}

            <div className="flex justify-between items-center mt-4">
              <button
                type="button"
                onClick={handleDismissClick}
                className="font-body text-[13px] text-gm-inkSoft underline hover:text-gm-ink focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-gm-accent"
              >
                {t('save_your_spot.secondary_cta')}
              </button>
              <Btn
                type="submit"
                variant="primary"
                size="lg"
                disabled={form.formState.isSubmitting}
              >
                {t('save_your_spot.primary_cta')}
              </Btn>
            </div>
          </form>

          <p className="mt-4 font-body text-[12px] text-gm-inkFaint text-center">
            {t('save_your_spot.footer_email_hint', { email })}
          </p>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  )
}
