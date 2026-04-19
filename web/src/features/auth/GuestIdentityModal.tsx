import * as Dialog from '@radix-ui/react-dialog'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useTranslation } from 'react-i18next'
import { useGuestIdentity, type GuestIdentity } from './useGuestIdentity'
import { useEffect } from 'react'

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
        <Dialog.Overlay className="fixed inset-0 bg-surface-on/40 z-40" />
        <Dialog.Content
          className="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 w-[min(480px,90vw)] z-50 bg-surface rounded-lg p-6 shadow-xl"
          aria-describedby="guest-modal-desc"
        >
          <Dialog.Title className="text-xl font-semibold text-surface-on leading-tight">
            {t('auth.guest_modal_title')}
          </Dialog.Title>
          <Dialog.Description id="guest-modal-desc" className="mt-2 text-base font-normal text-surface-onVariant leading-relaxed">
            {t('auth.guest_modal_body')}
          </Dialog.Description>

          <form onSubmit={form.handleSubmit(handleValid)} className="mt-6 flex flex-col gap-4" noValidate>
            <label className="flex flex-col gap-1">
              <span className="text-sm font-normal text-surface-on">{t('auth.guest_first_name')}</span>
              <input
                type="text"
                autoComplete="given-name"
                aria-invalid={Boolean(errors.firstName)}
                className="h-12 px-3 rounded border border-outline text-base focus:ring-2 focus:ring-primary focus:outline-none bg-surface text-surface-on"
                {...form.register('firstName')}
              />
              {errors.firstName && <span role="alert" className="text-sm text-destructive">{String(errors.firstName.message)}</span>}
            </label>

            <label className="flex flex-col gap-1">
              <span className="text-sm font-normal text-surface-on">{t('auth.guest_last_name')}</span>
              <input
                type="text"
                autoComplete="family-name"
                aria-invalid={Boolean(errors.lastName)}
                className="h-12 px-3 rounded border border-outline text-base focus:ring-2 focus:ring-primary focus:outline-none bg-surface text-surface-on"
                {...form.register('lastName')}
              />
              {errors.lastName && <span role="alert" className="text-sm text-destructive">{String(errors.lastName.message)}</span>}
            </label>

            <label className="flex flex-col gap-1">
              <span className="text-sm font-normal text-surface-on">{t('auth.email_label')}</span>
              <input
                type="email"
                autoComplete="email"
                aria-invalid={Boolean(errors.email)}
                className="h-12 px-3 rounded border border-outline text-base focus:ring-2 focus:ring-primary focus:outline-none bg-surface text-surface-on"
                {...form.register('email')}
              />
              {errors.email && <span role="alert" className="text-sm text-destructive">{String(errors.email.message)}</span>}
            </label>

            <button
              type="submit"
              className="mt-2 min-h-[48px] px-6 rounded-full bg-primary text-primary-on font-semibold text-base hover:bg-[#5B4397] focus:ring-2 focus:ring-primary focus:ring-offset-2 focus:outline-none disabled:opacity-50"
              disabled={form.formState.isSubmitting}
            >
              {t('reservation.reserve_item')}
            </button>
          </form>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  )
}
