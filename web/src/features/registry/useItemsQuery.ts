import { useEffect } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { collection, onSnapshot, query, type FirestoreError } from 'firebase/firestore'
import { db } from '../../firebase'
import { mapItemSnapshot, type Item } from '../../lib/firestore-mapping'

/**
 * Real-time items subscription wrapped in TanStack Query cache.
 *
 * - onSnapshot lives in useEffect (unsubscribes on unmount; single subscription per registry).
 * - Successful snapshots call queryClient.setQueryData — the queryFn is a passive reader.
 * - On error (permission-denied, unavailable, etc.) data resolves to [] — clients see an empty
 *   list rather than a partial UI; the parent useRegistryQuery owns the not-found 404 branch.
 * - staleTime: Infinity + refetchOn*: false are inherited from the global QueryClient defaults.
 *
 * Returns:
 *   - data === undefined while the first snapshot has not arrived (initial loading)
 *   - data === [] when the snapshot is empty OR an error occurred
 *   - data === Item[] when items load successfully
 */
export function useItemsQuery(registryId: string | undefined) {
  const queryClient = useQueryClient()
  const queryKey = ['registry', registryId ?? 'undefined', 'items'] as const

  useEffect(() => {
    if (!registryId) return

    const col = collection(db, 'registries', registryId, 'items')
    // Phase 5: insertion order is fine (UI-SPEC Layout: single list, no sort specified)
    const q = query(col)
    const unsub = onSnapshot(
      q,
      (snap) => {
        const items = snap.docs.map(mapItemSnapshot)
        queryClient.setQueryData<Item[]>(queryKey as unknown as readonly unknown[], items)
      },
      (err: FirestoreError) => {
        // eslint-disable-next-line no-console
        console.warn('[useItemsQuery] onSnapshot error', err.code)
        queryClient.setQueryData<Item[]>(queryKey as unknown as readonly unknown[], [])
      },
    )
    return unsub
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [registryId, queryClient])

  return useQuery<Item[]>({
    queryKey: queryKey as unknown as readonly unknown[],
    // Passive reader — the real source of truth is the onSnapshot callback above.
    // When the cache has no value yet (initial mount, before onSnapshot fires),
    // return a never-resolving Promise so the query stays in 'pending' state
    // (data === undefined). Returning [] here would conflate "still loading" with
    // "registry has no items", breaking downstream loading-state branches.
    queryFn: () => {
      const cached = queryClient.getQueryData<Item[]>(
        queryKey as unknown as readonly unknown[],
      )
      if (cached === undefined) {
        return new Promise<Item[]>(() => {})
      }
      return cached
    },
    enabled: Boolean(registryId),
  })
}
