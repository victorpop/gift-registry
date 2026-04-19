import { beforeEach, describe, expect, it } from 'vitest'
import { act, renderHook } from '@testing-library/react'
import { useGuestIdentity, GUEST_IDENTITY_STORAGE_KEY } from '../useGuestIdentity'

describe('useGuestIdentity', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('returns identity === null on first mount with empty localStorage', () => {
    const { result } = renderHook(() => useGuestIdentity())
    expect(result.current.identity).toBeNull()
  })

  it('reads existing value from localStorage on mount (pre-fill)', () => {
    localStorage.setItem(
      GUEST_IDENTITY_STORAGE_KEY,
      JSON.stringify({ firstName: 'Ana', lastName: 'Pop', email: 'ana@x.com' }),
    )
    const { result } = renderHook(() => useGuestIdentity())
    expect(result.current.identity).toEqual({ firstName: 'Ana', lastName: 'Pop', email: 'ana@x.com' })
  })

  it('save() writes to localStorage and updates state', () => {
    const { result } = renderHook(() => useGuestIdentity())
    act(() => {
      result.current.save({ firstName: 'Ion', lastName: 'Popescu', email: 'ion@x.com' })
    })
    expect(result.current.identity).toEqual({ firstName: 'Ion', lastName: 'Popescu', email: 'ion@x.com' })
    expect(localStorage.getItem(GUEST_IDENTITY_STORAGE_KEY)).toBe(
      JSON.stringify({ firstName: 'Ion', lastName: 'Popescu', email: 'ion@x.com' }),
    )
  })

  it('returns null for malformed JSON (does not crash)', () => {
    localStorage.setItem(GUEST_IDENTITY_STORAGE_KEY, 'not-json')
    const { result } = renderHook(() => useGuestIdentity())
    expect(result.current.identity).toBeNull()
  })

  it('returns null for missing required fields', () => {
    localStorage.setItem(GUEST_IDENTITY_STORAGE_KEY, JSON.stringify({ firstName: 'Ana' }))
    const { result } = renderHook(() => useGuestIdentity())
    expect(result.current.identity).toBeNull()
  })

  it('clear() removes from localStorage and sets identity to null', () => {
    localStorage.setItem(
      GUEST_IDENTITY_STORAGE_KEY,
      JSON.stringify({ firstName: 'Ana', lastName: 'Pop', email: 'ana@x.com' }),
    )
    const { result } = renderHook(() => useGuestIdentity())
    act(() => {
      result.current.clear()
    })
    expect(result.current.identity).toBeNull()
    expect(localStorage.getItem(GUEST_IDENTITY_STORAGE_KEY)).toBeNull()
  })
})
