import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createElement, type PropsWithChildren } from 'react'

// Hoisted handle to capture the onSnapshot callbacks + return unsub
const snapshotHandles = vi.hoisted(() => ({
  onNext: null as ((snap: unknown) => void) | null,
  onError: null as ((err: unknown) => void) | null,
  unsubscribe: vi.fn(),
}))

vi.mock('firebase/firestore', async () => {
  const actual = await vi.importActual<typeof import('firebase/firestore')>('firebase/firestore')
  return {
    ...actual,
    doc: vi.fn(() => ({ _kind: 'docref' })),
    onSnapshot: vi.fn((_ref: unknown, onNext: (s: unknown) => void, onError: (e: unknown) => void) => {
      snapshotHandles.onNext = onNext
      snapshotHandles.onError = onError
      return snapshotHandles.unsubscribe
    }),
  }
})

vi.mock('../../../firebase', () => ({
  db: { _kind: 'fakeDb' },
}))

import { useRegistryQuery } from '../useRegistryQuery'

function wrapper(client: QueryClient) {
  return function Wrapper({ children }: PropsWithChildren) {
    return createElement(QueryClientProvider, { client }, children)
  }
}

describe('useRegistryQuery', () => {
  let client: QueryClient
  beforeEach(() => {
    client = new QueryClient({ defaultOptions: { queries: { retry: false, staleTime: Infinity, refetchOnMount: false, refetchOnWindowFocus: false } } })
    snapshotHandles.onNext = null
    snapshotHandles.onError = null
    snapshotHandles.unsubscribe.mockReset()
  })

  it('returns registry data when first snapshot arrives', async () => {
    const { result } = renderHook(() => useRegistryQuery('reg-1'), { wrapper: wrapper(client) })

    // Wait for the useEffect to register the onSnapshot callback
    await waitFor(() => expect(snapshotHandles.onNext).not.toBeNull())

    // Simulate Firestore emitting a document
    snapshotHandles.onNext!({
      id: 'reg-1',
      exists: () => true,
      data: () => ({
        ownerId: 'owner-1',
        name: 'Test Registry',
        occasionType: 'Wedding',
        eventDate: null,
        eventLocation: null,
        description: null,
        visibility: 'public',
        invitedUsers: {},
        createdAt: null,
        updatedAt: null,
      }),
    })

    await waitFor(() => {
      expect(result.current.data).toMatchObject({ id: 'reg-1', name: 'Test Registry', visibility: 'public' })
    })
  })

  it('returns null data when permission-denied error fires (WEB-D-14)', async () => {
    const { result } = renderHook(() => useRegistryQuery('reg-2'), { wrapper: wrapper(client) })
    await waitFor(() => expect(snapshotHandles.onError).not.toBeNull())
    snapshotHandles.onError!({ code: 'permission-denied', message: 'Denied' })
    await waitFor(() => expect(result.current.data).toBeNull())
  })

  it('returns null data when not-found error fires (WEB-D-13)', async () => {
    const { result } = renderHook(() => useRegistryQuery('reg-3'), { wrapper: wrapper(client) })
    await waitFor(() => expect(snapshotHandles.onError).not.toBeNull())
    snapshotHandles.onError!({ code: 'not-found', message: 'Missing' })
    await waitFor(() => expect(result.current.data).toBeNull())
  })

  it('calls unsubscribe exactly once on unmount', async () => {
    const { unmount } = renderHook(() => useRegistryQuery('reg-4'), { wrapper: wrapper(client) })
    // Wait for effect to run
    await waitFor(() => expect(snapshotHandles.unsubscribe).toBeDefined())
    expect(snapshotHandles.unsubscribe).not.toHaveBeenCalled()
    unmount()
    expect(snapshotHandles.unsubscribe).toHaveBeenCalledTimes(1)
  })

  it('maps snapshot with exists() false to null (deleted registry)', async () => {
    const { result } = renderHook(() => useRegistryQuery('reg-5'), { wrapper: wrapper(client) })
    await waitFor(() => expect(snapshotHandles.onNext).not.toBeNull())
    snapshotHandles.onNext!({
      id: 'reg-5',
      exists: () => false,
      data: () => undefined,
    })
    await waitFor(() => expect(result.current.data).toBeNull())
  })
})
