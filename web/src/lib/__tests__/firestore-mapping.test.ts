import { describe, expect, it } from 'vitest'
import { Timestamp, type DocumentData, type DocumentSnapshot } from 'firebase/firestore'
import { mapRegistrySnapshot } from '../firestore-mapping'

/**
 * Regression tests for the Phase 5 → Phase 13 schema-mismatch bug:
 * the web mapper was reading `name`, `occasionType`, `eventDate` (web domain
 * naming) directly off the Firestore document, but the Android app
 * (RegistryDto.kt — the source of truth) writes the canonical fields
 * `title`, `occasion`, `eventDateMs`. Result: empty Display XL headline,
 * missing occasion accent pill, no date mono caption.
 *
 * These tests assert the mapper consumes the Android-canonical schema.
 */

const makeSnap = (id: string, data: Record<string, unknown> | null) =>
  ({
    id,
    exists: () => data !== null,
    data: () => data ?? undefined,
  }) as unknown as DocumentSnapshot<DocumentData>

describe('mapRegistrySnapshot', () => {
  it('reads Android-canonical fields (title, occasion, eventDateMs) onto the web Registry domain', () => {
    const snap = makeSnap('reg1', {
      ownerId: 'u1',
      title: 'Sara birthday',
      occasion: 'birthday',
      eventDateMs: 1746662400000,
      visibility: 'public',
    })

    const result = mapRegistrySnapshot(snap)

    expect(result).not.toBeNull()
    expect(result!.id).toBe('reg1')
    expect(result!.ownerId).toBe('u1')
    expect(result!.name).toBe('Sara birthday')
    expect(result!.occasionType).toBe('birthday')
    expect(result!.eventDate).toBeInstanceOf(Date)
    expect(result!.eventDate!.getTime()).toBe(1746662400000)
    expect(result!.visibility).toBe('public')
  })

  it('maps null/missing eventDateMs to null eventDate while preserving other fields', () => {
    const snap = makeSnap('reg2', {
      ownerId: 'u1',
      title: 'Sara birthday',
      occasion: 'birthday',
      eventDateMs: null,
      visibility: 'public',
    })

    const result = mapRegistrySnapshot(snap)

    expect(result).not.toBeNull()
    expect(result!.name).toBe('Sara birthday')
    expect(result!.occasionType).toBe('birthday')
    expect(result!.eventDate).toBeNull()
  })

  it('returns null when snapshot does not exist', () => {
    const snap = makeSnap('regGone', null)
    const result = mapRegistrySnapshot(snap)
    expect(result).toBeNull()
  })

  it('does not fall back to legacy web-domain field names (name, occasionType, eventDate)', () => {
    // Document carries ONLY the legacy/wrong names — mapper must NOT read these.
    // This documents that the mapper is Android-canonical only (per diagnosis).
    const snap = makeSnap('regLegacy', {
      ownerId: 'u1',
      name: 'Old Legacy Name',
      occasionType: 'old-occasion',
      eventDate: Timestamp.fromMillis(1746662400000),
      visibility: 'public',
    })

    const result = mapRegistrySnapshot(snap)

    expect(result).not.toBeNull()
    expect(result!.name).toBe('')
    expect(result!.occasionType).toBe('')
    expect(result!.eventDate).toBeNull()
  })
})
