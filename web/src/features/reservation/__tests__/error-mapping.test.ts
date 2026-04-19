import { describe, expect, it } from 'vitest'
import { mapHttpsErrorToI18nKey } from '../../../lib/error-mapping'

describe('mapHttpsErrorToI18nKey', () => {
  it('maps failed-precondition code to reservation.conflict', () => {
    expect(mapHttpsErrorToI18nKey('failed-precondition')).toBe('reservation.conflict')
  })

  it('maps ITEM_UNAVAILABLE message to reservation.conflict', () => {
    expect(mapHttpsErrorToI18nKey('internal', 'ITEM_UNAVAILABLE')).toBe('reservation.conflict')
  })

  it('maps not-found to common.error_generic', () => {
    expect(mapHttpsErrorToI18nKey('not-found')).toBe('common.error_generic')
  })

  it('maps internal to common.error_generic', () => {
    expect(mapHttpsErrorToI18nKey('internal')).toBe('common.error_generic')
  })

  it('maps unknown code to common.error_generic', () => {
    expect(mapHttpsErrorToI18nKey('random-code')).toBe('common.error_generic')
  })

  it('maps undefined to common.error_generic', () => {
    expect(mapHttpsErrorToI18nKey(undefined)).toBe('common.error_generic')
  })
})
