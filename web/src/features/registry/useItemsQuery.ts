import { useEffect } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { collection, onSnapshot, query, type FirestoreError } from 'firebase/firestore'
import { db } from '../../firebase'
import { mapItemSnapshot, type Item } from '../../lib/firestore-mapping'

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
    queryFn: () =>
      queryClient.getQueryData<Item[]>(queryKey as unknown as readonly unknown[]) ?? [],
    enabled: Boolean(registryId),
  })
}
