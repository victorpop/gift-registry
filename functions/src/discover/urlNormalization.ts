/**
 * Phase 17 D-16/D-17: canonical product-URL normalization + SHA-256 productId.
 *
 * Used by:
 *   - popularItems counter triggers (functions/src/discover/triggers.ts — Plan 17-04)
 *   - One-shot backfill (functions/scripts/backfillPopularItems.ts — Plan 17-04)
 *
 * Two products at the same retailer with different utm/affiliate suffix
 * params MUST resolve to the same productId — that is the entire purpose of
 * this module.
 */

import { createHash } from "crypto";

/**
 * D-16: query params dropped before canonicalization.
 *
 * Comparison is case-insensitive (we lowercase the param key before checking
 * membership) so callers passing `UTM_SOURCE` are normalized the same as
 * `utm_source`.
 */
const TRACKING_PARAMS = new Set<string>([
  // Standard UTM
  "utm_source",
  "utm_medium",
  "utm_campaign",
  "utm_term",
  "utm_content",
  // Social click identifiers
  "fbclid",
  "gclid",
  "mc_cid",
  "mc_eid",
  // EMAG affiliate suffix params (per CONTEXT.md D-16 step 3)
  "ref",
  "affiliate_id",
  "cmpid",
]);

export interface NormalizedUrl {
  /** SHA-256 hex digest of `canonicalUrl` — 64 chars [a-f0-9]. */
  productId: string;
  /** Canonical form: `https://{host-lc}{path}{?sortedQuery}` (no fragment). */
  canonicalUrl: string;
}

/**
 * D-16 normalization pipeline (in order):
 *   1. Parse URL.
 *   2. Lowercase host.
 *   3. Strip tracking params from TRACKING_PARAMS (case-insensitive on key).
 *   4. Sort remaining params alphabetically by key.
 *   5. Drop URL fragment.
 *   6. Strip trailing slash from path (unless path is exactly "/").
 *   7. Reconstruct as `https://{host}{path}{?sortedQuery}` (force https).
 *
 * Returns the canonical URL string AND a sha256-hex productId of that string.
 */
export function normalizeUrl(url: string): NormalizedUrl {
  const parsed = new URL(url);
  const host = parsed.host.toLowerCase();

  // Strip tracking params (key compared lowercased), collect survivors.
  const remaining: Array<[string, string]> = [];
  for (const [key, value] of parsed.searchParams.entries()) {
    if (!TRACKING_PARAMS.has(key.toLowerCase())) {
      remaining.push([key, value]);
    }
  }
  // Step 4: alphabetic sort by key (stable for equal keys via slice index).
  remaining.sort(([a], [b]) => (a < b ? -1 : a > b ? 1 : 0));

  // Step 6: strip trailing slash from path unless path is just "/".
  let path = parsed.pathname;
  if (path !== "/" && path.endsWith("/")) {
    path = path.slice(0, -1);
  }

  // Step 4 continued + Step 7: reconstruct.
  const queryString = remaining.length
    ? "?" +
      remaining
        .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
        .join("&")
    : "";

  const canonicalUrl = `https://${host}${path}${queryString}`;
  const productId = createHash("sha256").update(canonicalUrl).digest("hex");
  return { productId, canonicalUrl };
}
