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

export function mapRegistrySnapshot(snap: DocumentSnapshot<DocumentData>): Registry | null {
  if (!snap.exists()) return null
  const d = snap.data()!
  return {
    id: snap.id,
    ownerId: (d.ownerId as string) ?? '',
    name: (d.name as string) ?? '',
    occasionType: (d.occasionType as string) ?? '',
    eventDate: timestampToDate(d.eventDate),
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

export function mapItemSnapshot(snap: QueryDocumentSnapshot<DocumentData>): Item {
  const d = snap.data()
  return {
    id: snap.id,
    title: (d.title as string) ?? '',
    imageUrl: (d.imageUrl as string | null) ?? null,
    price: typeof d.price === 'number' ? d.price : null,
    currency: (d.currency as string | null) ?? null,
    notes: (d.notes as string | null) ?? null,
    status: coerceStatus(d.status),
    reservedBy: (d.reservedBy as string | null) ?? null,
    reservedAt: timestampToDate(d.reservedAt),
    expiresAt: timestampToDate(d.expiresAt),
    affiliateUrl: (d.affiliateUrl as string) ?? '',
    originalUrl: (d.originalUrl as string) ?? '',
    merchantDomain: (d.merchantDomain as string | null) ?? null,
  }
}
