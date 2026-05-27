/**
 * Phase 17 D-24: Firestore doc-ID-safe cache key for discoverSearch.
 *
 * Steps: lowercase, trim, collapse all whitespace (incl. tabs/newlines) to
 * single space. Romanian diacritics are preserved (they survive
 * encodeURIComponent and decode back to the same code points).
 *
 * encodeURIComponent ensures the result is a valid Firestore document ID
 * (Firestore rejects "/" and has a 1500-byte limit; short queries fit
 * comfortably).
 */

export function normalizeCacheKey(query: string): string {
  // \s collapses tab/newline/space; trim handles leading + trailing whitespace.
  const normalized = query.toLowerCase().trim().replace(/\s+/g, " ");
  return encodeURIComponent(normalized);
}
