import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Loader2 } from 'lucide-react'
import { useAuth } from '../auth/useAuth'
import { useGuestIdentity, type GuestIdentity } from '../auth/useGuestIdentity'
import GuestIdentityModal from '../auth/GuestIdentityModal'
import { useCreateReservation } from './useCreateReservation'
import { useActiveReservation } from './useActiveReservation'
import { useToast } from '../../components/ToastProvider'
import { mapHttpsErrorToI18nKey } from '../../lib/error-mapping'
import type { Item } from '../../lib/firestore-mapping'

interface Props {
  registryId: string
  item: Item
}

export default function ReserveButton({ registryId, item }: Props) {
  const { t } = useTranslation()
  const { user } = useAuth()
  const { identity } = useGuestIdentity()
  const { set: setActive } = useActiveReservation()
  const { showToast } = useToast()
  const [guestModalOpen, setGuestModalOpen] = useState(false)

  const mutation = useCreateReservation({
    onSuccess: (data) => {
      setActive({
        reservationId: data.reservationId,
        itemId: item.id,
        itemName: item.title,
        affiliateUrl: data.affiliateUrl,
        merchantDomain: item.merchantDomain,
        expiresAtMs: data.expiresAtMs,
      })
      showToast(t('reservation.success'), 'success')
    },
    onError: (err) => {
      const e = err as { code?: string; message?: string }
      const key = mapHttpsErrorToI18nKey(e?.code, e?.message)
      showToast(t(key), 'error')
    },
  })

  function doReserve(giverName: string, giverEmail: string, giverId: string | null) {
    mutation.mutate({
      registryId,
      itemId: item.id,
      giverName,
      giverEmail,
      giverId,
    })
  }

  function handleClick() {
    if (mutation.isPending) return
    if (user) {
      // Authenticated: skip guest modal
      const name = user.displayName || (user.email ? user.email.split('@')[0] : 'Guest')
      const email = user.email ?? ''
      doReserve(name, email, user.uid)
      return
    }
    // Anonymous — open GuestIdentityModal (pre-fills from localStorage automatically)
    setGuestModalOpen(true)
  }

  function handleGuestSubmit(id: GuestIdentity) {
    doReserve(`${id.firstName} ${id.lastName}`.trim(), id.email, null)
  }

  return (
    <>
      <button
        type="button"
        onClick={handleClick}
        disabled={mutation.isPending}
        aria-busy={mutation.isPending}
        aria-label={mutation.isPending ? 'Reserving gift…' : undefined}
        className="min-h-[48px] px-6 rounded-full bg-primary text-primary-on font-semibold text-base hover:bg-[#5B4397] focus:ring-2 focus:ring-primary focus:ring-offset-2 focus:outline-none disabled:opacity-50 disabled:cursor-not-allowed inline-flex items-center justify-center gap-2"
      >
        {mutation.isPending ? (
          <Loader2 className="w-4 h-4 animate-spin" aria-hidden="true" />
        ) : (
          t('reservation.reserve_item')
        )}
      </button>
      <GuestIdentityModal
        open={guestModalOpen}
        onOpenChange={setGuestModalOpen}
        onSubmit={handleGuestSubmit}
      />
      {/* Reference identity to keep lint happy and pre-fill behaviour explicit */}
      <span className="sr-only" data-testid="guest-identity-loaded">{identity ? 'yes' : 'no'}</span>
    </>
  )
}
