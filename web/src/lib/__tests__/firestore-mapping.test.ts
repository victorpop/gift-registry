import { describe, expect, it } from 'vitest'
import { Timestamp, type DocumentData, type DocumentSnapshot, type QueryDocumentSnapshot } from 'firebase/firestore'
import {
  deriveMerchantDomain,
  mapItemSnapshot,
  mapRegistrySnapshot,
  parsePriceString,
} from '../firestore-mapping'

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

// QueryDocumentSnapshot is non-null by contract — no `exists()` branch needed.
const makeItemSnap = (id: string, data: Record<string, unknown>) =>
  ({ id, data: () => data }) as unknown as QueryDocumentSnapshot<DocumentData>

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

/**
 * Regression tests for quick-260516-lbf: Android ItemDto writes `price` as a
 * free-form String? (e.g. "459,00 RON") and does NOT write `currency` or
 * `merchantDomain` at all. The web mapper must parse the string price and
 * derive the merchant domain from `originalUrl` — same fix philosophy as
 * quick-260510-o7w for RegistryDto (web mapper adapts to Android-canonical
 * schema, never the other way around).
 */

describe('parsePriceString', () => {
  it('parses "459,00 RON" (RO locale, comma decimal, explicit ISO code)', () => {
    expect(parsePriceString('459,00 RON')).toEqual({ amount: 459, currency: 'RON' })
  })

  it('parses "€19.99" (symbol prefix, dot decimal)', () => {
    expect(parsePriceString('€19.99')).toEqual({ amount: 19.99, currency: 'EUR' })
  })

  it('parses "$1,299.99" (US locale — comma thousands, dot decimal)', () => {
    expect(parsePriceString('$1,299.99')).toEqual({ amount: 1299.99, currency: 'USD' })
  })

  it('parses "299" (bare numeric, no currency)', () => {
    expect(parsePriceString('299')).toEqual({ amount: 299, currency: null })
  })

  it('returns nulls for empty input', () => {
    expect(parsePriceString('')).toEqual({ amount: null, currency: null })
  })

  it('parses "lei 1.299,50" (RO locale — dot thousands, comma decimal, alias currency)', () => {
    expect(parsePriceString('lei 1.299,50')).toEqual({ amount: 1299.5, currency: 'RON' })
  })

  it('returns nulls for non-numeric input "abc"', () => {
    expect(parsePriceString('abc')).toEqual({ amount: null, currency: null })
  })

  it('treats "1,234" (3-digit comma tail) as thousands, not decimal', () => {
    expect(parsePriceString('1,234')).toEqual({ amount: 1234, currency: null })
  })
})

describe('deriveMerchantDomain', () => {
  it('prefers explicit merchantDomain over derivation from originalUrl', () => {
    expect(
      deriveMerchantDomain({
        merchantDomain: 'override.com',
        originalUrl: 'https://emag.ro/x',
      }),
    ).toBe('override.com')
  })

  it('strips leading "www." when deriving from originalUrl', () => {
    expect(deriveMerchantDomain({ originalUrl: 'https://www.emag.ro/foo' })).toBe('emag.ro')
  })

  it('returns bare host for non-www originalUrl', () => {
    expect(deriveMerchantDomain({ originalUrl: 'https://ikea.com' })).toBe('ikea.com')
  })

  it('returns null when neither merchantDomain nor originalUrl is provided', () => {
    expect(deriveMerchantDomain({})).toBeNull()
  })

  it('returns null when originalUrl is malformed', () => {
    expect(deriveMerchantDomain({ originalUrl: 'not-a-url' })).toBeNull()
  })

  it('treats empty-string merchantDomain as missing and falls through to derivation', () => {
    expect(
      deriveMerchantDomain({
        merchantDomain: '',
        originalUrl: 'https://emag.ro/x',
      }),
    ).toBe('emag.ro')
  })
})

describe('mapItemSnapshot — Android schema', () => {
  it('parses string price + derives merchantDomain when Android doc omits currency/merchantDomain', () => {
    const snap = makeItemSnap('itemA', {
      title: 'Smart kettle',
      originalUrl: 'https://www.emag.ro/p',
      affiliateUrl: 'https://aff.example/p',
      imageUrl: null,
      price: '459,00 RON',
      notes: null,
      status: 'available',
    })

    const result = mapItemSnapshot(snap)

    expect(result.price).toBe(459)
    expect(result.currency).toBe('RON')
    expect(result.merchantDomain).toBe('emag.ro')
  })

  it('bare numeric string price → numeric amount, null currency, derived host', () => {
    const snap = makeItemSnap('itemB', {
      title: 'Lamp',
      originalUrl: 'https://ikea.com/p',
      affiliateUrl: 'https://aff.example/p',
      imageUrl: null,
      price: '299',
      notes: null,
      status: 'available',
    })

    const result = mapItemSnapshot(snap)

    expect(result.price).toBe(299)
    expect(result.currency).toBeNull()
    expect(result.merchantDomain).toBe('ikea.com')
  })

  it('null price → null price + null currency; merchantDomain still derived from originalUrl', () => {
    const snap = makeItemSnap('itemC', {
      title: 'Cookbook',
      originalUrl: 'https://www.emag.ro/cookbook',
      affiliateUrl: 'https://aff.example/c',
      imageUrl: null,
      price: null,
      notes: null,
      status: 'available',
    })

    const result = mapItemSnapshot(snap)

    expect(result.price).toBeNull()
    expect(result.currency).toBeNull()
    expect(result.merchantDomain).toBe('emag.ro')
  })

  it('legacy/future structured doc (numeric price, explicit currency, explicit merchantDomain) wins unchanged', () => {
    const snap = makeItemSnap('itemD', {
      title: 'Watch',
      originalUrl: 'https://emag.ro/watch',
      affiliateUrl: 'https://aff.example/w',
      imageUrl: null,
      price: 199.99,
      currency: 'EUR',
      merchantDomain: 'shop.com',
      notes: null,
      status: 'available',
    })

    const result = mapItemSnapshot(snap)

    expect(result.price).toBe(199.99)
    expect(result.currency).toBe('EUR')
    expect(result.merchantDomain).toBe('shop.com')
  })

  it('numeric price but no currency field → keeps numeric amount, currency stays null', () => {
    const snap = makeItemSnap('itemE', {
      title: 'Speaker',
      originalUrl: 'https://shop.example/s',
      affiliateUrl: 'https://aff.example/s',
      imageUrl: null,
      price: 199.99,
      notes: null,
      status: 'available',
    })

    const result = mapItemSnapshot(snap)

    expect(result.price).toBe(199.99)
    expect(result.currency).toBeNull()
    expect(result.merchantDomain).toBe('shop.example')
  })
})
