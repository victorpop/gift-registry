/**
 * Phase 17-07: Serper.dev /shopping endpoint HTTP wrapper.
 *
 * Replaces cseClient.ts (CSE 403 → Serper pivot, 2026-05-28).
 *
 * Pure function — no state, no Firestore. Each call:
 *   POST https://google.serper.dev/shopping
 *   Headers: { X-API-KEY: <secret>, Content-Type: application/json }
 *   Body: { q, gl, hl, location, num }
 *
 * Cost note: num=10 costs 1 credit per call; num > 10 costs 2 credits (Pitfall S-6).
 * Keep num at 10 (default) for cost control.
 *
 * Uses Node 22 built-in fetch — no external HTTP library needed.
 */

/**
 * A single product result from Serper /shopping.
 * Source: transitive-bullshit/serper.ts TypeScript SDK gist.
 */
export interface SerperShoppingItem {
  title: string;        // product title
  source: string;       // merchant label, e.g. "eMAG", "Altex"
  link: string;         // product URL — may be a google.com/shopping redirect (Pitfall S-2)
  price: string;        // formatted price, e.g. "179 RON" or "1.299,00 lei"
  imageUrl: string;     // product image URL (https encrypted-tbn CDN from Google)
  delivery?: string | Record<string, string>;
  rating?: number;      // 0–5 float
  ratingCount?: number; // review count
  offers?: string;      // "10+" sellers
  productId?: string;   // Google product ID
  position?: number;    // rank in results
}

interface SerperShoppingResponse {
  searchParameters?: Record<string, unknown>;
  shopping?: SerperShoppingItem[];
}

/**
 * Call Serper.dev /shopping for Romanian product results.
 *
 * @param query - The search query string (may contain Romanian diacritics — JSON.stringify handles UTF-8)
 * @param apiKey - The SERPER_API_KEY secret value
 * @param options - Optional overrides for gl, hl, location, num
 * @returns SerperShoppingItem[] from the response, or [] if shopping array is absent
 * @throws Error on non-2xx HTTP responses or network errors
 */
export async function callSerper(
  query: string,
  apiKey: string,
  options: { gl?: string; hl?: string; location?: string; num?: number } = {},
): Promise<SerperShoppingItem[]> {
  const body = {
    q: query,
    gl: options.gl ?? "ro",        // country code — Romania
    hl: options.hl ?? "ro",        // language — Romanian
    location: options.location ?? "Romania", // geo context (belt-and-suspenders with gl — Pitfall S-4)
    num: options.num ?? 10,        // num:10 — keeps cost at 1 credit/call; >10 costs 2 credits (Pitfall S-6)
    autocorrect: true,             // handles Romanian diacritic variants gracefully
  };

  const response = await fetch("https://google.serper.dev/shopping", {
    method: "POST",
    headers: {
      "X-API-KEY": apiKey,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
    signal: AbortSignal.timeout(10000), // 10s per call (Pitfall S-7: Serper ~1-2s avg)
  });

  if (!response.ok) {
    const errText = await response.text().catch(() => "");
    throw new Error(`Serper HTTP ${response.status}: ${errText.slice(0, 500)}`);
  }

  const json = (await response.json()) as SerperShoppingResponse;
  return json.shopping ?? [];
}
