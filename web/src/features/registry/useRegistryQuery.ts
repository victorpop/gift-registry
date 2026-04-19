import { useEffect } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { doc, onSnapshot, type FirestoreError } from 'firebase/firestore'
import { db } from '../../firebase'
import { mapRegistrySnapshot, type Registry } from '../../lib/firestore-mapping'

/**
 * Real-time registry subscription wrapped in TanStack Query cache.
 *
 * - onSnapshot lives in useEffect (unsubscribes on unmount; single subscription per registry).
 * - Successful snapshots call queryClient.setQueryData — the queryFn is a passive reader.
 * - permission-denied AND not-found both set data to null (WEB-D-13 + WEB-D-14): client does not distinguish.
 * - staleTime: Infinity + refetchOn*: false are inherited from the global QueryClient defaults (queryClient.ts).
 *
 * Returns:
 *   - data === undefined while the first snapshot has not arrived (initial loading)
 *   - data === null when the registry is not accessible (not-found OR permission-denied)
 *   - data === Registry when readable
 */
export function useRegistryQuery(registryId: string | undefined) {
  const queryClient = useQueryClient()
  const queryKey = ['registry', registryId ?? 'undefined'] as const

  useEffect(() => {
    if (!registryId) return

    const docRef = doc(db, 'registries', registryId)
    const unsub = onSnapshot(
      docRef,
      (snap) => {
        queryClient.setQueryData<Registry | null>(
          queryKey as unknown as readonly unknown[],
          mapRegistrySnapshot(snap),
        )
      },
      (err: FirestoreError) => {
        // WEB-D-13 + WEB-D-14: both permission-denied and not-found → null (NotFoundPage).
        // Any other error (unavailable, internal, etc.) also maps to null — the giver sees
        // the generic 404 instead of a partial UI. Errors are not surfaced separately.
        // eslint-disable-next-line no-console
        console.warn('[useRegistryQuery] onSnapshot error', err.code)
        queryClient.setQueryData<Registry | null>(
          queryKey as unknown as readonly unknown[],
          null,
        )
      },
    )
    return unsub
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [registryId, queryClient])

  return useQuery<Registry | null>({
    queryKey: queryKey as unknown as readonly unknown[],
    // Passive reader — the real source of truth is the onSnapshot callback above.
    queryFn: () =>
      queryClient.getQueryData<Registry | null>(queryKey as unknown as readonly unknown[]) ?? null,
    enabled: Boolean(registryId),
  })
}
