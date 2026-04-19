/**
 * Maps Firebase HttpsError codes thrown by Cloud Functions callables to i18n keys.
 * Used by the reservation mutation to decide which toast to display.
 */
export function mapHttpsErrorToI18nKey(code: string | undefined, message?: string | undefined): string {
  // createReservation throws:
  //   - 'failed-precondition' / ITEM_UNAVAILABLE    -> RES-09 conflict toast
  //   - 'not-found'           / ITEM_NOT_FOUND      -> generic (item vanished)
  //   - 'invalid-argument'    / MISSING_REQUIRED_FIELDS -> generic (client bug)
  //   - any other (internal, unavailable, etc.)     -> generic
  if (code === 'failed-precondition' || message === 'ITEM_UNAVAILABLE') {
    return 'reservation.conflict'
  }
  return 'common.error_generic'
}
