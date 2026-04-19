/**
 * Shared Playwright fixtures: seeded registry + item data.
 * Plans 06 and 07 extend this with per-test seeding against the Firestore emulator.
 */

export interface SeededRegistry {
  id: string
  name: string
  visibility: 'public' | 'private'
  ownerId: string
}

export interface SeededItem {
  id: string
  title: string
  affiliateUrl: string
  status: 'available' | 'reserved' | 'purchased'
}

export const PUBLIC_REGISTRY: SeededRegistry = {
  id: 'test-public-registry-01',
  name: 'Test Wedding Registry',
  visibility: 'public',
  ownerId: 'test-owner-uid',
}

export const PRIVATE_REGISTRY: SeededRegistry = {
  id: 'test-private-registry-01',
  name: 'Private Baby Shower',
  visibility: 'private',
  ownerId: 'test-owner-uid',
}

export const AVAILABLE_ITEM: SeededItem = {
  id: 'test-item-available',
  title: 'Test Gift Available',
  affiliateUrl: 'https://example.com/product?aff=test',
  status: 'available',
}

export const RESERVED_ITEM: SeededItem = {
  id: 'test-item-reserved',
  title: 'Test Gift Reserved',
  affiliateUrl: 'https://example.com/product?aff=test',
  status: 'reserved',
}
