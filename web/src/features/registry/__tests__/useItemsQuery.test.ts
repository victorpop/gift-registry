import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createElement, type PropsWithChildren } from 'react'

const itemHandles = vi.hoisted(() => ({
  onNext: null as ((snap: unknown) => void) | null,
  onError: null as ((err: unknown) => void) | null,
  unsubscribe: vi.fn(),
}))

vi.mock('firebase/firestore', async () => {
  const actual = await vi.importActual<typeof import('firebase/firestore')>('firebase/firestore')
  return {
    ...actual,
    collection: vi.fn(() => ({ _kind: 'collectionref' })),
    query: vi.fn((ref: unknown) => ref),
    onSnapshot: vi.fn((_ref: unknown, onNext: (s: unknown) => void, onError: (e: unknown) => void) => {
      itemHandles.onNext = onNext
      itemHandles.onError = onError
      return itemHandles.unsubscribe
    }),
  }
})

vi.mock('../../../firebase', () => ({ db: { _kind: 'fakeDb' } }))

import { useItemsQuery } from '../useItemsQuery'

function wrapper(client: QueryClient) {
  return function Wrapper({ children }: PropsWithChildren) {
    return createElement(QueryClientProvider, { client }, children)
  }
}

describe('useItemsQuery', () => {
  let client: QueryClient
  beforeEach(() => {
    client = new QueryClient({ defaultOptions: { queries: { retry: false, staleTime: Infinity, refetchOnMount: false, refetchOnWindowFocus: false } } })
    itemHandles.onNext = null
    itemHandles.onError = null
    itemHandles.unsubscribe.mockReset()
  })

  it('maps items from snapshot.docs', async () => {
    const { result } = renderHook(() => useItemsQuery('reg-1'), { wrapper: wrapper(client) })
    await waitFor(() => expect(itemHandles.onNext).not.toBeNull())
    itemHandles.onNext!({
      docs: [
        { id: 'i1', data: () => ({ title: 'Coffee grinder', status: 'available', affiliateUrl: 'https://x/?aff=1', originalUrl: 'https://x' }) },
        { id: 'i2', data: () => ({ title: 'Cutting board', status: 'reserved', affiliateUrl: 'https://x/?aff=2', originalUrl: 'https://x' }) },
      ],
      size: 2,
      empty: false,
    })
    await waitFor(() => expect(result.current.data?.length).toBe(2))
    expect(result.current.data?.[0]).toMatchObject({ id: 'i1', title: 'Coffee grinder', status: 'available' })
    expect(result.current.data?.[1]).toMatchObject({ id: 'i2', status: 'reserved' })
  })

  it('returns [] on empty collection (not undefined)', async () => {
    const { result } = renderHook(() => useItemsQuery('reg-2'), { wrapper: wrapper(client) })
    await waitFor(() => expect(itemHandles.onNext).not.toBeNull())
    itemHandles.onNext!({ docs: [], size: 0, empty: true })
    await waitFor(() => expect(result.current.data).toEqual([]))
  })

  it('does not double-subscribe when the hook re-renders with same registryId', async () => {
    const { rerender } = renderHook(({ id }: { id: string }) => useItemsQuery(id), {
      wrapper: wrapper(client),
      initialProps: { id: 'reg-3' },
    })
    await waitFor(() => expect(itemHandles.onNext).not.toBeNull())
    rerender({ id: 'reg-3' })
    // onSnapshot should only have been called once (stable registryId dep)
    const firestoreMock = await import('firebase/firestore') as unknown as { onSnapshot: { mock: { calls: unknown[] } } }
    expect(firestoreMock.onSnapshot.mock.calls.length).toBe(1)
  })
})
