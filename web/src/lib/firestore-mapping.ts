import { Timestamp, type DocumentData, type QueryDocumentSnapshot, type DocumentSnapshot } from 'firebase/firestore'

// -------- Registry --------

export type RegistryVisibility = 'public' | 'private'

export interface Registry {
  id: string
  ownerId: string
  name: string
  occasionType: string
  eventDate: Date | null
  eventLocation: string | null
  description: string | null
  visibility: RegistryVisibility
  createdAt: Date | null
  updatedAt: Date | null
}

function timestampToDate(v: unknown): Date | null {
  if (!v) return null
  if (v instanceof Timestamp) return v.toDate()
  if (v instanceof Date) return v
  return null
}

// -------- Price + merchant helpers (Android schema bridge) --------
//
// The Android app (canonical writer per app/.../ItemDto.kt) writes `price`
// as a free-form String? (e.g. "459,00 RON", "€19.99", "299") and does NOT
// write `currency` or `merchantDomain`. The helpers below parse the string
// price + derive the merchant host so the web Item type stays populated.
// Mirrors functions/src/registry/fetchOgMetadata.ts (lines 26-108).

const CURRENCY_ALIASES: Record<string, string> = {
  '€': 'EUR',
  '$': 'USD',
  '£': 'GBP',
  lei: 'RON',
  ron: 'RON',
  eur: 'EUR',
  usd: 'USD',
}

function normalizeCurrency(raw: string | null | undefined): string | null {
  if (!raw) return null
  const trimmed = raw.trim()
  if (trimmed === '') return null
  if (/^[A-Z]{3}$/.test(trimmed)) return trimmed
  return CURRENCY_ALIASES[trimmed.toLowerCase()] ?? null
}

/**
 * Parses a free-form price string (as written by Android ItemDto.price, e.g.
 * "459,00 RON", "€19.99", "1.299,50 lei") into a numeric amount and ISO 4217
 * currency.
 *
 * Decimal-separator heuristic (mirrors functions/src/registry/fetchOgMetadata.ts):
 *  - If both '.' and ',' are present, the right-most is the decimal separator.
 *  - If only ',' is present, it is the decimal separator UNLESS the comma tail
 *    is exactly 3 digits (then it is a thousands separator: "1,234" → 1234).
 *  - If only '.' is present, it is the decimal separator (mirror behavior).
 *
 * Returns { amount: null, currency: null } when no numeric run is found.
 */
export function parsePriceString(raw: string): { amount: number | null; currency: string | null } {
  if (!raw) return { amount: null, currency: null }
  const match = raw.match(/\d[\d.,]*/)
  if (!match) return { amount: null, currency: null }
  const numericRun = match[0]

  // Normalize decimal separator → produce a JS-parsable Number string.
  let normalized: string
  const hasDot = numericRun.includes('.')
  const hasComma = numericRun.includes(',')
  if (hasDot && hasComma) {
    const lastDot = numericRun.lastIndexOf('.')
    const lastComma = numericRun.lastIndexOf(',')
    if (lastComma > lastDot) {
      // Comma is decimal → strip dots, replace comma with dot.
      normalized = numericRun.replace(/\./g, '').replace(',', '.')
    } else {
      // Dot is decimal → strip commas.
      normalized = numericRun.replace(/,/g, '')
    }
  } else if (hasComma) {
    // Only comma: decimal unless 3-digit tail (then thousands).
    const lastComma = numericRun.lastIndexOf(',')
    const tail = numericRun.slice(lastComma + 1)
    if (tail.length === 3 && /^\d{3}$/.test(tail)) {
      normalized = numericRun.replace(/,/g, '')
    } else {
      normalized = numericRun.replace(',', '.')
    }
  } else {
    normalized = numericRun
  }

  const amount = Number(normalized)
  if (!Number.isFinite(amount)) return { amount: null, currency: null }

  // Currency extraction from the remainder (mirrors functions parsePriceString).
  const remainder = raw.replace(numericRun, '').replace(/[\s ]+/g, ' ').trim()
  let currency: string | null = null
  if (remainder) {
    currency = normalizeCurrency(remainder)
    if (!currency) {
      for (const token of remainder.split(/\s+/)) {
        currency = normalizeCurrency(token)
        if (currency) break
      }
    }
    if (!currency && remainder.length > 0) {
      currency = normalizeCurrency(remainder[0])
    }
  }

  return { amount, currency }
}

/**
 * Derives the merchant domain for an Item. Priority:
 *  1. Explicit `merchantDomain` field on the doc (legacy/future writer wins;
 *     empty string is treated as missing and falls through).
 *  2. Hostname of `originalUrl` with leading "www." stripped.
 *  3. null when neither is usable (invalid URL, missing originalUrl).
 */
export function deriveMerchantDomain(d: { merchantDomain?: unknown; originalUrl?: unknown }): string | null {
  if (typeof d.merchantDomain === 'string' && d.merchantDomain.trim() !== '') {
    return d.merchantDomain
  }
  if (typeof d.originalUrl !== 'string' || d.originalUrl.trim() === '') return null
  try {
    return new URL(d.originalUrl).hostname.replace(/^www\./, '')
  } catch {
    return null
  }
}

export function mapRegistrySnapshot(snap: DocumentSnapshot<DocumentData>): Registry | null {
  if (!snap.exists()) return null
  const d = snap.data()!
  return {
    id: snap.id,
    ownerId: (d.ownerId as string) ?? '',
    // Android writes title/occasion/eventDateMs (RegistryDto.kt) — must match canonical schema
    name: (d.title as string) ?? '',
    occasionType: (d.occasion as string) ?? '',
    eventDate: typeof d.eventDateMs === 'number' ? new Date(d.eventDateMs) : null,
    eventLocation: (d.eventLocation as string | null) ?? null,
    description: (d.description as string | null) ?? null,
    // NEVER read invitedUsers — rules enforce access; client reads the doc or gets denied (WEB-D-10)
    visibility: ((d.visibility as string) === 'private' ? 'private' : 'public'),
    createdAt: timestampToDate(d.createdAt),
    updatedAt: timestampToDate(d.updatedAt),
  }
}

// -------- Item --------

export type ItemStatus = 'available' | 'reserved' | 'purchased'

export interface Item {
  id: string
  title: string
  imageUrl: string | null
  price: number | null
  currency: string | null
  notes: string | null
  status: ItemStatus
  reservedBy: string | null
  reservedAt: Date | null
  expiresAt: Date | null
  affiliateUrl: string
  originalUrl: string
  merchantDomain: string | null
}

function coerceStatus(s: unknown): ItemStatus {
  if (s === 'reserved' || s === 'purchased' || s === 'available') return s
  return 'available'
}

/**
 * Maps a Firestore item document to the web Item domain.
 *
 * Schema mismatch (surfaced during k37 verification; same class as
 * quick-260510-o7w for RegistryDto): the Android app — the canonical writer
 * per app/src/main/java/com/giftregistry/data/model/ItemDto.kt — writes:
 *   - `price` as a free-form String? (e.g. "459,00 RON", "€19.99", "299").
 *   - NO `currency` field.
 *   - NO `merchantDomain` field.
 * The web Item type expects numeric `price`, ISO `currency`, and
 * `merchantDomain`. To bridge: `parsePriceString` extracts amount + currency
 * from the string, and `deriveMerchantDomain` derives the host from
 * `originalUrl`. Explicit structured fields on the doc (numeric `price`,
 * `currency`, `merchantDomain`) still take precedence — this keeps a clean
 * path open for a future writer that normalizes price server-side without
 * breaking existing Android-written docs.
 */
export function mapItemSnapshot(snap: QueryDocumentSnapshot<DocumentData>): Item {
  const d = snap.data()

  // Price + currency: accept either a number (legacy/future structured shape)
  // or a string (Android-canonical). String shape may embed currency.
  let price: number | null = null
  let parsedCurrency: string | null = null
  if (typeof d.price === 'number' && Number.isFinite(d.price)) {
    price = d.price
  } else if (typeof d.price === 'string' && d.price.trim() !== '') {
    const parsed = parsePriceString(d.price)
    price = parsed.amount
    parsedCurrency = parsed.currency
  }
  // Explicit `currency` on the doc (legacy/future) wins; else fall back to
  // the currency parsed out of the price string.
  const currency =
    (typeof d.currency === 'string' && d.currency.trim() !== '' ? d.currency : null) ??
    parsedCurrency

  return {
    id: snap.id,
    title: (d.title as string) ?? '',
    imageUrl: (d.imageUrl as string | null) ?? null,
    price,
    currency,
    notes: (d.notes as string | null) ?? null,
    status: coerceStatus(d.status),
    reservedBy: (d.reservedBy as string | null) ?? null,
    reservedAt: timestampToDate(d.reservedAt),
    expiresAt: timestampToDate(d.expiresAt),
    affiliateUrl: (d.affiliateUrl as string) ?? '',
    originalUrl: (d.originalUrl as string) ?? '',
    merchantDomain: deriveMerchantDomain(d),
  }
}
