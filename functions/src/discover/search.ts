/**
 * Phase 17 D-12/D-13/D-23/D-25/D-26/D-27/D-31/D-45: `discoverSearch` Callable.
 *
 * Orchestrates the Gemini intent → Serper /shopping fan-out pipeline:
 *   1. Auth gate (D-12) — reject unauthenticated and anonymous-provider.
 *   2. Validate query (D-23) — non-empty, ≤200 chars.
 *   3. Rate limit (D-13) — transactional 20/hr per uid via
 *      checkAndIncrementRateLimit; throws "resource-exhausted" at cap.
 *   4. Cache check (D-25, D-26) — read discoverCache/{normalizedQuery}; on
 *      hit, return cached.results immediately.
 *   5. Cache miss → runSearchPipeline (Gemini intent → Serper fan-out →
 *      normalize → allowlist filter → de-dupe → flat products[]).
 *   6. Cache write — only on non-empty results (avoid caching failures).
 *      `cachedAt` is the TTL DEADLINE (now + 30 days) per D-45 semantics.
 *
 * Response shape (D-31):
 *   { products: DiscoverProduct[], cached_at: string (ISO timestamp) }
 *
 * // No google_search / grounding — products come from Serper /shopping (UAT-6 hallucination fix).
 *
 * The Callable wrapper (auth, App Check, validation, rate-limit, cache) is
 * UNCHANGED from the pre-v2 implementation. Only the cache-miss internals changed.
 * Android requires ZERO changes — DiscoverRepositoryImpl maps the same products[] contract.
 */
import { onCall, HttpsError, CallableRequest } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { Timestamp } from "firebase-admin/firestore";
import { GEMINI_API_KEY, SERPER_API_KEY } from "./secrets";
import { normalizeCacheKey } from "./cacheKey";
import { selectSitesForQuery } from "./retailers";
import { buildIntentPrompt } from "./promptTemplate";
import { callGeminiIntent } from "./geminiClient";
import type { IntentResult } from "./geminiClient";
import { callSerper } from "./serperClient";
import type { SerperShoppingItem } from "./serperClient";
import { normalizeSerperItems, applyAllowlistFilter, dedupeByUrl } from "./serperNormalizer";
import { DiscoverProduct } from "./parseGeminiResponse";
import { checkAndIncrementRateLimit } from "./rateLimit";
import type { BuiltPrompt } from "./promptTemplate";

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

// ---------------------------------------------------------------------------
// Pipeline dependency types — used for unit-testing with mocks
// ---------------------------------------------------------------------------

type IntentFn = (prompt: BuiltPrompt, apiKey: string) => Promise<IntentResult>;
type SerperFn = (query: string, apiKey: string) => Promise<SerperShoppingItem[]>;

interface PipelineDeps {
  intentFn: IntentFn;
  serperFn: SerperFn;
  apiKeyGemini: string;
  apiKeySerper: string;
}

// ---------------------------------------------------------------------------
// runSearchPipeline — exported pure-ish function for unit-testing
// ---------------------------------------------------------------------------

/**
 * Execute the Gemini intent → Serper fan-out → normalize → allowlist → de-dupe pipeline.
 *
 * Exported so serperOrchestration.test.ts can drive it with mocked callSerper + callGeminiIntent
 * without Firestore dependencies.
 *
 * @param query - Validated, non-empty user query string
 * @param deps  - Injected dependencies (intentFn, serperFn, apiKeys)
 * @returns Flat, de-duped DiscoverProduct[] (may be empty on total failure)
 */
export async function runSearchPipeline(
  query: string,
  deps: PipelineDeps,
): Promise<DiscoverProduct[]> {
  const { intentFn, serperFn, apiKeyGemini, apiKeySerper } = deps;

  // Step 1: Gemini intent extraction
  const sites = selectSitesForQuery(query);
  const prompt = buildIntentPrompt(query, sites);

  let intent: IntentResult;
  try {
    intent = await intentFn(prompt, apiKeyGemini);
  } catch (e) {
    console.error("[discoverSearch] intent failed", e);
    // Fallback: single Serper call with the raw query
    intent = { giftCategories: [{ name: "", reason: "", searchQuery: query }] };
  }

  // Step 2: Cap categories to 3 (fan-out cost control)
  let categories = (intent.giftCategories ?? []).slice(0, 3);
  if (categories.length === 0) {
    // Empty intent fallback: one Serper call with the raw query
    categories = [{ name: "", reason: "", searchQuery: query }];
  }

  // Step 3: Fan-out Serper /shopping calls (Promise.allSettled — partial failure tolerance)
  const settled = await Promise.allSettled(
    categories.map((cat) =>
      serperFn(cat.searchQuery, apiKeySerper).then((items) =>
        applyAllowlistFilter(normalizeSerperItems(items)),
      ),
    ),
  );

  // Step 4: Flatten fulfilled results + log rejected
  const allProducts: DiscoverProduct[] = [];
  for (const result of settled) {
    if (result.status === "rejected") {
      console.warn("[discoverSearch] Serper call failed", result.reason);
      continue;
    }
    allProducts.push(...result.value);
  }

  // Step 5: Cross-batch de-dupe by normalized URL
  return dedupeByUrl(allProducts);
}

// ---------------------------------------------------------------------------
// discoverSearchHandler — Callable handler
// ---------------------------------------------------------------------------

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

  // D-26: cache miss → run the Serper pipeline
  const products = await runSearchPipeline(query, {
    intentFn: callGeminiIntent,
    serperFn: callSerper,
    apiKeyGemini: GEMINI_API_KEY.value(),
    apiKeySerper: SERPER_API_KEY.value(),
  });

  console.log(
    `[discoverSearch] query="${query}" serper_products=${products.length}`,
  );

  // Cache write — only on non-empty results (avoid caching transient failures)
  const now = new Date();
  if (products.length > 0) {
    // D-45 TTL semantics: cachedAt must be the DEADLINE (now + 30 days)
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
    secrets: [GEMINI_API_KEY, SERPER_API_KEY],
    // ~2s Serper × 3 parallel + ~15s intent = well within 90s (Pitfall S-7).
    // Do NOT reduce — 90s was already set for the prior OG enrichment path.
    timeoutSeconds: 90,
  },
  discoverSearchHandler,
);
