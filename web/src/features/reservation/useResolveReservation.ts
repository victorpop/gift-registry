import { useMutation } from '@tanstack/react-query'
import { httpsCallable } from 'firebase/functions'
import { functions } from '../../firebase'

export interface ResolveReservationRequest {
  reservationId: string
}

export interface ResolveReservationResponse {
  registryId: string
  itemId: string
  status: string
}

// NOTE: functions instance is region-pinned to europe-west3 in firebase.ts.
// Called inside the hook (not at module level) so that test mocks of httpsCallable
// are applied at call time — unlike createReservation which must be module-level for singleton.
// resolveReservation is used on a single page and does not need the singleton pattern.

export function useResolveReservation() {
  const resolveReservationFn = httpsCallable<ResolveReservationRequest, ResolveReservationResponse>(
    functions,
    'resolveReservation',
  )

  return useMutation<ResolveReservationResponse, unknown, ResolveReservationRequest>({
    mutationFn: async (req) => {
      const result = await resolveReservationFn(req)
      return result.data
    },
  })
}
