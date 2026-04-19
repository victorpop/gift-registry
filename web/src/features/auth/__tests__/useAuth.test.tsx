import { beforeEach, describe, expect, it, vi } from 'vitest'
import { renderHook, waitFor } from '@testing-library/react'

const handles = vi.hoisted(() => ({
  onNext: null as ((u: unknown) => void) | null,
  unsubscribe: vi.fn(),
}))

vi.mock('firebase/auth', async () => {
  const actual = await vi.importActual<typeof import('firebase/auth')>('firebase/auth')
  return {
    ...actual,
    onAuthStateChanged: vi.fn((_auth: unknown, cb: (u: unknown) => void) => {
      handles.onNext = cb
      return handles.unsubscribe
    }),
  }
})

vi.mock('../../../firebase', () => ({
  auth: { _kind: 'fakeAuth' },
}))

import { useAuth } from '../useAuth'

describe('useAuth', () => {
  beforeEach(() => {
    handles.onNext = null
    handles.unsubscribe.mockReset()
  })

  it('returns isReady=false before first auth emission', () => {
    const { result } = renderHook(() => useAuth())
    expect(result.current.isReady).toBe(false)
    expect(result.current.user).toBeNull()
  })

  it('returns user=null, isReady=true after auth emits null (signed out)', async () => {
    const { result } = renderHook(() => useAuth())
    handles.onNext!(null)
    await waitFor(() => expect(result.current.isReady).toBe(true))
    expect(result.current.user).toBeNull()
  })

  it('returns user=<User>, isReady=true after auth emits a user', async () => {
    const { result } = renderHook(() => useAuth())
    const fakeUser = { uid: 'u1', email: 'a@b.com' }
    handles.onNext!(fakeUser)
    await waitFor(() => expect(result.current.isReady).toBe(true))
    expect(result.current.user).toEqual(fakeUser)
  })

  it('calls unsubscribe on unmount', () => {
    const { unmount } = renderHook(() => useAuth())
    unmount()
    expect(handles.unsubscribe).toHaveBeenCalledTimes(1)
  })
})
