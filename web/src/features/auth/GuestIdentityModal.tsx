import * as Dialog from '@radix-ui/react-dialog'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useTranslation } from 'react-i18next'
import { useGuestIdentity, type GuestIdentity } from './useGuestIdentity'
import { useEffect } from 'react'
import { Btn, Field } from '../../components/giftmaison'

const schema = z.object({
  firstName: z.string().min(1, 'required'),
  lastName: z.string().min(1, 'required'),
  email: z.string().min(1, 'required').email('email'),
})

type FormValues = z.infer<typeof schema>

interface Props {
  open: boolean
  onOpenChange: (open: boolean) => void
  /** Called when submit is valid; Plan 06 uses this to proceed with createReservation. */
  onSubmit: (identity: GuestIdentity) => void
}

/**
 * Guest identity modal (Phase 13 visual: bg-gm-paper modal shell, rounded-gm-modal,
 * shadow-gm-modal; 3 Field atoms + primary Btn submit). Behavioural contract is
 * preserved verbatim from Phase 5: useGuestIdentity hook (identity / save), zod
 * schema for validation, form.reset on open, onSubmit prop call after save+close.
 */
export default function GuestIdentityModal({ open, onOpenChange, onSubmit }: Props) {
  const { t } = useTranslation()
  const { identity, save } = useGuestIdentity()

  const form = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: identity ?? { firstName: '', lastName: '', email: '' },
  })

  // Keep form values in sync when identity loads asynchronously or modal re-opens
  useEffect(() => {
    if (open) {
      form.reset(identity ?? { firstName: '', lastName: '', email: '' })
    }
  }, [open, identity, form])

  function handleValid(values: FormValues) {
    save(values)
    onSubmit(values)
    onOpenChange(false)
  }

  const errors = form.formState.errors

  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 bg-gm-ink/40 z-40 backdrop-blur-[2px]" />
        <Dialog.Content
          className="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 w-[min(480px,90vw)] z-50 bg-gm-paper rounded-gm-modal p-6 shadow-gm-modal"
          aria-describedby="guest-modal-desc"
        >
          <Dialog.Title className="font-display text-[24px] text-gm-ink leading-[1.1] tracking-[-0.5px] font-normal">
            {t('auth.guest_modal_title')}
          </Dialog.Title>
          <Dialog.Description id="guest-modal-desc" className="mt-2 font-body text-[14.5px] text-gm-inkSoft leading-[1.55]">
            {t('auth.guest_modal_body')}
          </Dialog.Description>

          <form onSubmit={form.handleSubmit(handleValid)} className="mt-6 flex flex-col gap-4" noValidate>
            <Field
              label={t('auth.guest_first_name')}
              type="text"
              autoComplete="given-name"
              aria-invalid={Boolean(errors.firstName)}
              error={errors.firstName ? String(errors.firstName.message) : undefined}
              {...form.register('firstName')}
            />
            <Field
              label={t('auth.guest_last_name')}
              type="text"
              autoComplete="family-name"
              aria-invalid={Boolean(errors.lastName)}
              error={errors.lastName ? String(errors.lastName.message) : undefined}
              {...form.register('lastName')}
            />
            <Field
              label={t('auth.email_label')}
              type="email"
              autoComplete="email"
              aria-invalid={Boolean(errors.email)}
              error={errors.email ? String(errors.email.message) : undefined}
              {...form.register('email')}
            />
            <Btn type="submit" variant="primary" size="lg" disabled={form.formState.isSubmitting} className="w-full mt-2">
              {t('reservation.reserve_item')}
            </Btn>
          </form>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  )
}
