import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createElement, type PropsWithChildren } from 'react'

const callableMock = vi.hoisted(() => vi.fn())
const httpsCallableMock = vi.hoisted(() => vi.fn(() => callableMock))
vi.mock('firebase/functions', () => ({ httpsCallable: httpsCallableMock }))
vi.mock('../../../firebase', () => ({ functions: { _kind: 'fakeFunctions' } }))

import { useCreateReservation } from '../useCreateReservation'

function wrapper(client: QueryClient) {
  return function Wrapper({ children }: PropsWithChildren) {
    return createElement(QueryClientProvider, { client }, children)
  }
}

describe('useCreateReservation', () => {
  let client: QueryClient
  let windowOpenSpy: ReturnType<typeof vi.spyOn>

  beforeEach(() => {
    client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
    callableMock.mockReset()
    // NOTE: Do NOT reset httpsCallableMock — httpsCallable is called at module load time
    // (module-level const). The assertion below checks cumulative calls including the module-init call.
    windowOpenSpy = vi.spyOn(window, 'open').mockImplementation(() => null)
  })
  afterEach(() => {
    windowOpenSpy.mockRestore()
  })

  it('registers callable with name "createReservation"', () => {
    // httpsCallable is invoked at module initialization time to create the callable fn.
    // We verify the call happened with the correct arguments (checked against all calls made so far).
    expect(httpsCallableMock).toHaveBeenCalledWith(
      expect.anything(),
      'createReservation',
    )
  })

  it('passes the full payload shape to the callable', async () => {
    callableMock.mockResolvedValue({ data: { reservationId: 'r1', affiliateUrl: 'https://x/?aff=1', expiresAtMs: Date.now() + 1_800_000 } })
    const { result } = renderHook(() => useCreateReservation(), { wrapper: wrapper(client) })
    const payload = { registryId: 'reg-1', itemId: 'item-1', giverName: 'Ana Pop', giverEmail: 'ana@x.com', giverId: null }
    result.current.mutate(payload)
    await waitFor(() => expect(callableMock).toHaveBeenCalledWith(payload))
  })

  it('opens affiliateUrl in a new tab on success (WEB-D-07)', async () => {
    const url = 'https://emag.ro/?aff=123'
    callableMock.mockResolvedValue({ data: { reservationId: 'r1', affiliateUrl: url, expiresAtMs: Date.now() + 1_800_000 } })
    const { result } = renderHook(() => useCreateReservation(), { wrapper: wrapper(client) })
    result.current.mutate({ registryId: 'r', itemId: 'i', giverName: 'A B', giverEmail: 'a@b', giverId: 'u1' })
    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(windowOpenSpy).toHaveBeenCalledWith(url, '_blank', 'noopener,noreferrer')
  })

  it('does NOT open window when affiliateUrl is empty', async () => {
    callableMock.mockResolvedValue({ data: { reservationId: 'r1', affiliateUrl: '', expiresAtMs: Date.now() + 1_800_000 } })
    const { result } = renderHook(() => useCreateReservation(), { wrapper: wrapper(client) })
    result.current.mutate({ registryId: 'r', itemId: 'i', giverName: 'A B', giverEmail: 'a@b', giverId: null })
    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(windowOpenSpy).not.toHaveBeenCalled()
  })

  it('invokes onSuccess option with response data', async () => {
    const onSuccess = vi.fn()
    const response = { reservationId: 'r1', affiliateUrl: 'https://x/?aff=1', expiresAtMs: 123 }
    callableMock.mockResolvedValue({ data: response })
    const { result } = renderHook(() => useCreateReservation({ onSuccess }), { wrapper: wrapper(client) })
    const payload = { registryId: 'r', itemId: 'i', giverName: 'A B', giverEmail: 'a@b', giverId: null }
    result.current.mutate(payload)
    await waitFor(() => expect(onSuccess).toHaveBeenCalledWith(response, payload))
  })

  it('invokes onError option on callable failure', async () => {
    const onError = vi.fn()
    const err = { code: 'failed-precondition', message: 'ITEM_UNAVAILABLE' }
    callableMock.mockRejectedValue(err)
    const { result } = renderHook(() => useCreateReservation({ onError }), { wrapper: wrapper(client) })
    const payload = { registryId: 'r', itemId: 'i', giverName: 'A B', giverEmail: 'a@b', giverId: null }
    result.current.mutate(payload)
    await waitFor(() => expect(result.current.isError).toBe(true))
    expect(onError).toHaveBeenCalledWith(err, payload)
  })
})
