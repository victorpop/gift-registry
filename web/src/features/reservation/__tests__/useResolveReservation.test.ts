import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { createElement, type PropsWithChildren } from 'react'

const callableMock = vi.hoisted(() => vi.fn())
const httpsCallableMock = vi.hoisted(() => vi.fn(() => callableMock))
vi.mock('firebase/functions', () => ({ httpsCallable: httpsCallableMock }))
vi.mock('../../../firebase', () => ({ functions: { _kind: 'fakeFunctions' } }))

import { useResolveReservation } from '../useResolveReservation'

function wrapper(client: QueryClient) {
  return function Wrapper({ children }: PropsWithChildren) {
    return createElement(QueryClientProvider, { client }, children)
  }
}

describe('useResolveReservation', () => {
  let client: QueryClient
  beforeEach(() => {
    client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
    callableMock.mockReset()
    httpsCallableMock.mockReset()
    httpsCallableMock.mockReturnValue(callableMock)
  })

  it('registers callable with name "resolveReservation"', () => {
    renderHook(() => useResolveReservation(), { wrapper: wrapper(client) })
    expect(httpsCallableMock).toHaveBeenCalledWith(expect.anything(), 'resolveReservation')
  })

  it('passes { reservationId } payload to the callable', async () => {
    callableMock.mockResolvedValue({ data: { registryId: 'r1', itemId: 'i1', status: 'active' } })
    const { result } = renderHook(() => useResolveReservation(), { wrapper: wrapper(client) })
    result.current.mutate({ reservationId: 'res-xyz' })
    await waitFor(() => expect(callableMock).toHaveBeenCalledWith({ reservationId: 'res-xyz' }))
  })

  it('resolves with { registryId, itemId, status }', async () => {
    callableMock.mockResolvedValue({ data: { registryId: 'reg-1', itemId: 'item-7', status: 'expired' } })
    const { result } = renderHook(() => useResolveReservation(), { wrapper: wrapper(client) })
    result.current.mutate({ reservationId: 'res-a' })
    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(result.current.data).toEqual({ registryId: 'reg-1', itemId: 'item-7', status: 'expired' })
  })

  it('surfaces the error on failure', async () => {
    callableMock.mockRejectedValue({ code: 'not-found', message: 'RESERVATION_NOT_FOUND' })
    const { result } = renderHook(() => useResolveReservation(), { wrapper: wrapper(client) })
    result.current.mutate({ reservationId: 'missing' })
    await waitFor(() => expect(result.current.isError).toBe(true))
  })
})
