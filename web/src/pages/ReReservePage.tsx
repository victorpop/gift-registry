import { useTranslation } from 'react-i18next'

/**
 * Plan 03 stub — Plan 07 replaces the body with:
 *   useEffect(() => {
 *     resolveReservation({ reservationId }).then(r =>
 *       navigate(`/registry/${r.registryId}?autoReserveItemId=${r.itemId}`)
 *     )
 *   }, [])
 */
export default function ReReservePage() {
  const { t } = useTranslation()
  return (
    <div className="min-h-screen flex items-center justify-center bg-surface">
      <span className="text-base font-normal text-surface-on">{t('reservation.resolving')}</span>
    </div>
  )
}
