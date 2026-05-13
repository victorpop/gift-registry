import { useMutation } from '@tanstack/react-query'
import { httpsCallable, type HttpsCallableResult } from 'firebase/functions'
import { functions } from '../../firebase'

export interface CreateReservationRequest {
  registryId: string
  itemId: string
  giverName: string
  giverEmail: string
  giverId: string | null
}

export interface CreateReservationResponse {
  reservationId: string
  affiliateUrl: string
  expiresAtMs: number
}

// NOTE: functions instance is region-pinned to europe-west3 in firebase.ts.
// The callable name must match exactly the Cloud Function export name.
const createReservationFn = httpsCallable<CreateReservationRequest, CreateReservationResponse>(
  functions,
  'createReservation',
)

export interface UseCreateReservationOptions {
  /** Called with the successful response — use to raise toast + set active reservation. */
  onSuccess?: (data: CreateReservationResponse, variables: CreateReservationRequest) => void
  /** Called with a Firebase FunctionsError; use error-mapping.ts to choose the toast. */
  onError?: (err: unknown, variables: CreateReservationRequest) => void
}

export function useCreateReservation(options: UseCreateReservationOptions = {}) {
  return useMutation<CreateReservationResponse, unknown, CreateReservationRequest>({
    mutationFn: async (req) => {
      const result: HttpsCallableResult<CreateReservationResponse> = await createReservationFn(req)
      return result.data
    },
    onSuccess: (data, variables) => {
      options.onSuccess?.(data, variables)
    },
    onError: (err, variables) => {
      options.onError?.(err, variables)
    },
  })
}
