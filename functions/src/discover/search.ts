/**
 * Phase 17 D-12/D-13/D-23/D-25/D-26/D-27/D-31/D-45: `discoverSearch` Callable.
 *
 * Orchestrates the full Gemini-search pipeline:
 *   1. Auth gate (D-12) — reject unauthenticated and anonymous-provider.
 *   2. Validate query (D-23) — non-empty, ≤200 chars.
 *   3. Rate limit (D-13) — transactional 20/hr per uid via
 *      checkAndIncrementRateLimit; throws "resource-exhausted" at cap.
 *   4. Cache check (D-25, D-26) — read discoverCache/{normalizedQuery}; on
 *      hit, return cached.results immediately.
 *   5. Gemini call (D-27) — select retailer sites for query → build prompt
 *      → call Gemini 2.5 Flash with google_search grounding. Errors map to
 *      empty result + no cache write.
 *   6. Defensive parse (D-30) — parseGeminiResponse never throws.
 *   7. Cache write — only on non-empty results (Claude's Discretion in
 *      CONTEXT.md: avoid permanently caching transient failures).
 *      `cachedAt` is the TTL DEADLINE (now + 30 days) per D-45 semantics —
 *      Firestore TTL deletes when field_value < now, so storing
 *      `serverTimestamp()` would mark every cache doc eligible immediately.
 *
 * Response shape (D-31):
 *   { products: DiscoverProduct[], cached_at: string (ISO timestamp) }
 *   `cached_at` is the original cache creation time on hit; on miss it's
 *   the time we wrote the doc (or "now" when we did not cache).
 */
import { onCall, HttpsError, CallableRequest } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { Timestamp } from "firebase-admin/firestore";
import { GEMINI_API_KEY } from "./secrets";
import { normalizeCacheKey } from "./cacheKey";
import { selectSitesForQuery } from "./retailers";
import { buildPrompt } from "./promptTemplate";
import { callGemini } from "./geminiClient";
import { parseGeminiResponse, DiscoverProduct } from "./parseGeminiResponse";
import { checkAndIncrementRateLimit } from "./rateLimit";

// D-45 TTL semantics: cachedAt is stored as the DEADLINE (now + 30 days),
// NOT creation time. Firestore TTL deletes a doc when field_value < now;
// storing `now` would mark the doc eligible immediately on the next sweep.
const CACHE_TTL_MS = 30 * 24 * 60 * 60 * 1000;
const REGION = "europe-west3";
const QUERY_MAX_LEN = 200;

interface SearchRequest {
  query?: unknown;
}

interface SearchResponse {
  products: DiscoverProduct[];
  cached_at: string;
}

export async function discoverSearchHandler(
  request: CallableRequest<SearchRequest>,
): Promise<SearchResponse> {
  // D-12: auth gate
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Sign in required");
  }
  if (request.auth.token.firebase?.sign_in_provider === "anonymous") {
    throw new HttpsError("permission-denied", "Registered account required");
  }

  // D-23: query validation
  const raw = request.data?.query;
  if (typeof raw !== "string" || raw.trim().length === 0) {
    throw new HttpsError("invalid-argument", "Query required");
  }
  const query = raw;
  if (query.length > QUERY_MAX_LEN) {
    throw new HttpsError("invalid-argument", "Query too long (max 200 chars)");
  }

  const db = admin.firestore();

  // D-13: rate limit (throws resource-exhausted at cap)
  await checkAndIncrementRateLimit(db, request.auth.uid);

  // D-24, D-25, D-26: cache lookup
  const cacheKey = normalizeCacheKey(query);
  const cacheRef = db.collection("discoverCache").doc(cacheKey);
  const cacheSnap = await cacheRef.get();
  if (cacheSnap.exists) {
    const data = cacheSnap.data()!;
    // D-31 client-facing cached_at = creation time. Since cachedAt is stored
    // as the DEADLINE (now + 30 days) per D-45 TTL semantics, recover the
    // creation time by subtracting CACHE_TTL_MS from the deadline.
    const cachedAtDeadline = data.cachedAt as Timestamp | undefined;
    const cachedAtCreation = cachedAtDeadline
      ? new Date(cachedAtDeadline.toDate().getTime() - CACHE_TTL_MS)
      : new Date();
    return {
      products: (data.results as DiscoverProduct[]) ?? [],
      cached_at: cachedAtCreation.toISOString(),
    };
  }

  // D-26, D-27, D-28, D-29: cache miss → Gemini
  const sites = selectSitesForQuery(query);
  const prompt = buildPrompt(query, sites);
  const apiKey = GEMINI_API_KEY.value();
  let rawResponse: string;
  try {
    rawResponse = await callGemini(prompt, apiKey);
  } catch (err) {
    console.error("[discoverSearch] Gemini call failed", err);
    // Claude's Discretion ("do not cache failures") — return empty, no cache write
    return { products: [], cached_at: new Date().toISOString() };
  }

  const products = parseGeminiResponse(rawResponse, query);

  // Claude's Discretion ("only cache successful non-empty results")
  const now = new Date();
  if (products.length > 0) {
    // D-45 TTL semantics: cachedAt must be the DEADLINE (now + 30 days),
    // not creation time. Firestore TTL deletes when field_value < now —
    // writing serverTimestamp() would make every cache doc eligible immediately.
    await cacheRef.set({
      query,
      normalizedQuery: cacheKey,
      results: products,
      cachedAt: Timestamp.fromDate(new Date(Date.now() + CACHE_TTL_MS)),
    });
  }

  return { products, cached_at: now.toISOString() };
}

export const discoverSearch = onCall(
  {
    region: REGION,
    enforceAppCheck: true,
    secrets: [GEMINI_API_KEY],
  },
  discoverSearchHandler,
);
