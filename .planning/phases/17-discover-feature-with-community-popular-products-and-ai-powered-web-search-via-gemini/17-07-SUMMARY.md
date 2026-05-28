---
phase: 17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini
plan: 07
subsystem: api
tags: [firebase-functions, gemini, serper, typescript, tdd, search, discover]

# Dependency graph
requires:
  - phase: 17-02-backend-foundations
    provides: discoverCache, rateLimit, cacheKey, urlNormalization infrastructure
  - phase: 17-03-callables
    provides: discoverSearch Callable wrapper (auth gate, App Check, rate-limit, cache)
  - phase: 17-06-deploy-and-uat
    provides: GEMINI_API_KEY set in Secret Manager; confirmed UAT-6 hallucination root cause

provides:
  - "Serper.dev /shopping API client (serperClient.ts) — POST with X-API-KEY header"
  - "SerperShoppingItem[] normalizer with Romanian price parsing (serperNormalizer.ts)"
  - "43-store hostname allowlist post-filter with graceful padding"
  - "Gemini intent extraction (JSON mode, no grounding tools) in callGeminiIntent"
  - "buildIntentPrompt — intent-extraction prompt (replaces product-listing prompt)"
  - "parseIntentResponse — IntentResult parser with searchQuery field (renamed from cseQuery)"
  - "runSearchPipeline — testable pure function for orchestration unit tests"
  - "SERPER_API_KEY secret declared in secrets.ts"
  - "discoverSearch deployed to europe-west3 with Gemini intent → Serper /shopping pipeline"

affects:
  - 17-08-android-price-guard-reuat  # re-UAT relies on real prices now populated by Serper

# Tech tracking
tech-stack:
  added:
    - "Serper.dev /shopping API (POST, X-API-KEY header) — 1 credit per 10-result call"
    - "SERPER_API_KEY Secret Manager secret (bound to discoverSearch Callable)"
  patterns:
    - "Gemini JSON-mode intent extraction (response_mime_type + response_schema, NO tools)"
    - "Promise.allSettled fan-out (max 3 Serper calls, partial-failure tolerant)"
    - "Hostname post-filter allowlist (43 stores) instead of PSE pre-filter"
    - "Injectable dependency pattern (runSearchPipeline PipelineDeps) for unit-testing without Firestore"
    - "Romanian price parsing: period=thousands, comma=decimal (parsePrice)"

key-files:
  created:
    - functions/src/discover/serperClient.ts
    - functions/src/discover/serperNormalizer.ts
    - functions/src/__tests__/discover/parseIntentResponse.test.ts
    - functions/src/__tests__/discover/serperNormalizer.test.ts
    - functions/src/__tests__/discover/serperOrchestration.test.ts
  modified:
    - functions/src/discover/secrets.ts         # added SERPER_API_KEY
    - functions/src/discover/geminiClient.ts    # callGemini → callGeminiIntent (JSON mode, no tools)
    - functions/src/discover/promptTemplate.ts  # buildPrompt → buildIntentPrompt
    - functions/src/discover/parseGeminiResponse.ts  # parseGeminiResponse → parseIntentResponse; DiscoverProduct kept
    - functions/src/discover/search.ts          # full pipeline re-arch; runSearchPipeline exported
    - functions/src/__tests__/discover/promptTemplate.test.ts   # rewritten for buildIntentPrompt
    - functions/src/__tests__/discover/parseGeminiResponse.test.ts  # updated for new exports
  deleted:
    - functions/src/discover/enrichImages.ts    # OG scraping eliminated; Serper /shopping provides imageUrl

key-decisions:
  - "Serper.dev /shopping chosen as CSE fallback after CSE returned HTTP 403 (new customer block); 5-7x cheaper than CSE paid tier ($0.001-0.003 vs $0.015 per user search)"
  - "cseQuery renamed to searchQuery in INTENT_SCHEMA, geminiClient, promptTemplate, parseGeminiResponse — 3-file rename for provider-agnosticism"
  - "applyAllowlistFilter operates on DiscoverProduct[] (after normalize) not SerperShoppingItem[] — cleaner test surface; in-allowlist first, pad with out-of-allowlist when < 3"
  - "runSearchPipeline exported as a pure-ish injectable function to enable serperOrchestration.test.ts without Firestore"
  - "enrichImages.ts deleted after rewriteToHttps() copied into serperNormalizer.ts; node-html-parser left in package.json (fetchOgMetadata.ts still imports it)"
  - "parseGeminiResponse.test.ts updated to test parseIntentResponse + DiscoverProduct export (old product-array tests removed — they tested a removed function)"

patterns-established:
  - "Intent-only Gemini: use JSON mode (response_mime_type + response_schema) without tools; do NOT mix tools with JSON mode (Gemini 2.5 Flash incompatibility)"
  - "Fan-out pattern: slice categories to 3, Promise.allSettled, flatten fulfilled, log rejected"
  - "Romanian price parsing: detect RO format (/\\d\\.\\d{3}[,\\s]/ or /,\\d{2}$/) before applying US format stripping"

requirements-completed:
  - V2-D1
  - V2-ARCH
  - V2-RULE
  - V2-STORES
  - V2-COST
  - D-12
  - D-13
  - D-23
  - D-24
  - D-25
  - D-26
  - D-31

# Metrics
duration: 38min
completed: 2026-05-29
---

# Phase 17 Plan 07: Backend Search Re-Scope (Serper.dev Pivot) Summary

**discoverSearch re-architected from Gemini grounding (UAT-6 hallucinations) to Gemini JSON-mode intent + Serper.dev /shopping fan-out with 43-store hostname post-filter; deployed to europe-west3**

## Performance

- **Duration:** 38 min
- **Started:** 2026-05-28T21:11:16Z
- **Completed:** 2026-05-29T00:00:00Z
- **Tasks:** 4 executed (Task 1 was already complete per checkpoint state; Tasks 2-5 executed)
- **Files modified:** 12 files (5 created, 5 modified, 1 rewritten test, 1 deleted)

## Accomplishments

- Serper gate outcome: **PASSED** (Task 1 was completed before this agent — SERPER_API_KEY set in Secret Manager, proof curl returned HTTP 200 with shopping[] array)
- Wave 0 RED test suite committed first (97116f4) covering 4 files: parseIntentResponse, serperNormalizer, serperOrchestration, promptTemplate
- GREEN implemented in 3 sequential commits: Gemini intent layer (3ac88a3) → Serper client + normalizer (16b6af1) → search.ts orchestration + enrichImages.ts deletion (c964bbd)
- Full discover test suite: **117 tests across 10 suites, all GREEN**
- TypeScript build: **exits 0, zero errors**
- `firebase deploy --only functions:discoverSearch` completed successfully; Secret Manager auto-granted SERPER_API_KEY access to the compute service account

## Final Orchestration Flow

```
discoverSearch Callable (UNCHANGED wrapper: auth gate → rate-limit → cache check)
    ├── [cache hit] → return immediately
    └── [cache miss]
         ├── selectSitesForQuery(query) → contextSites
         ├── buildIntentPrompt(query, sites) → BuiltPrompt
         ├── callGeminiIntent(prompt, GEMINI_API_KEY) → IntentResult {giftCategories[].searchQuery}
         │   (JSON mode, NO tools, 30s timeout — UAT-6 hallucination fix)
         ├── categories.slice(0, 3) → fan-out cap
         ├── Promise.allSettled(categories.map(c → callSerper(c.searchQuery, SERPER_API_KEY)))
         │   (POST https://google.serper.dev/shopping, X-API-KEY, gl=ro, hl=ro, location=Romania, num=10)
         ├── per settled result: applyAllowlistFilter(normalizeSerperItems(items))
         │   (43-store allowlist, graceful padding when < 3 in-allowlist)
         ├── dedupeByUrl(allProducts) → flat, de-duped DiscoverProduct[]
         └── cache write (products.length > 0) + return { products, cached_at }
```

## DiscoverProduct Contract Confirmation

The `DiscoverProduct` interface is **unchanged** — Android requires zero changes:

```typescript
export interface DiscoverProduct {
  title: string;       // from item.title
  description: string; // "" (Serper shopping has no snippet)
  image_url: string;   // item.imageUrl (always https from Google encrypted-tbn CDN)
  price: number;       // parsePrice(item.price) — NOW POPULATED with real prices (vs price=0 always with CSE plan)
  currency: string;    // "RON" (or "EUR" if detected)
  retailer_url: string; // item.link
  retailer_name: string; // item.source ?? DOMAIN_TO_RETAILER[rootDomain] ?? rootDomain
}
```

**Key UX improvement vs. CSE plan:** The CSE plan always had `price = 0` (CSE pagemap doesn't reliably surface prices for Romanian retailers). Serper /shopping carries `price` as a string for most results. After `parsePrice()`, most product cards will show a real price in RON. The Android `price > 0` guard (Plan 17-08) is still recommended for results where Serper has no price.

## cseQuery → searchQuery Rename

Applied in 3 files:
- `geminiClient.ts` — INTENT_SCHEMA properties: `cseQuery` → `searchQuery`
- `parseGeminiResponse.ts` — IntentResult.giftCategories[].searchQuery
- `promptTemplate.ts` — buildIntentPrompt describes the field as a "product search query" (no "cse" wording)

Search now returns `searchQuery` as the per-category field name, making the schema provider-agnostic. No `cseQuery` remains in any source file (only in doc comments explaining the rename history).

## Google.com/Shopping Redirect Rate (Pitfall S-2)

Not measurable until the first production queries populate discoverCache. The allowlist filter correctly handles redirect links: `extractRootDomain("https://www.google.com/shopping/product/...") → "google.com"` → not in ALLOWED_DOMAINS → lands in the out-of-allowlist padding set (still works via browser redirect on Android). Monitor via Firestore discoverCache: if > 20% of `retailer_url` values contain `google.com/shopping`, consider async redirect-resolution in a future plan.

## Task Commits

1. **Task 2: Wave 0 RED test suite** - `97116f4` (test) — 4 test files, all failing RED
2. **Task 3: Gemini intent layer** - `3ac88a3` (feat) — secrets.ts, geminiClient.ts, promptTemplate.ts, parseGeminiResponse.ts
3. **Task 4: Serper client + normalizer** - `16b6af1` (feat) — serperClient.ts, serperNormalizer.ts
4. **Task 5: search.ts re-arch + delete enrichImages** - `c964bbd` (feat) — search.ts, parseGeminiResponse.test.ts updated, enrichImages.ts deleted

## Files Created/Modified

- `functions/src/discover/serperClient.ts` — NEW: Serper.dev /shopping HTTP POST wrapper
- `functions/src/discover/serperNormalizer.ts` — NEW: parsePrice + normalizeSerperItems + applyAllowlistFilter + dedupeByUrl + ALLOWED_DOMAINS + DOMAIN_TO_RETAILER
- `functions/src/__tests__/discover/parseIntentResponse.test.ts` — NEW: 16 tests for parseIntentResponse
- `functions/src/__tests__/discover/serperNormalizer.test.ts` — NEW: 30 tests for normalizer + parsePrice + allowlist
- `functions/src/__tests__/discover/serperOrchestration.test.ts` — NEW: 8 tests for runSearchPipeline (fan-out cap, allSettled, empty-intent)
- `functions/src/discover/secrets.ts` — EXTENDED: added SERPER_API_KEY
- `functions/src/discover/geminiClient.ts` — REPLACED: callGemini → callGeminiIntent (JSON mode, no tools)
- `functions/src/discover/promptTemplate.ts` — REPLACED: buildPrompt → buildIntentPrompt
- `functions/src/discover/parseGeminiResponse.ts` — REPLACED parser: parseGeminiResponse → parseIntentResponse; DiscoverProduct kept
- `functions/src/discover/search.ts` — RE-ARCHITECTED: full Serper pipeline + runSearchPipeline export
- `functions/src/__tests__/discover/promptTemplate.test.ts` — REWRITTEN: buildPrompt → buildIntentPrompt tests
- `functions/src/__tests__/discover/parseGeminiResponse.test.ts` — UPDATED: tests parseIntentResponse + DiscoverProduct export
- `functions/src/discover/enrichImages.ts` — DELETED: OG scraping eliminated; Serper provides imageUrl directly

## Decisions Made

- Serper.dev /shopping chosen after CSE 403 (new customer block). No PSE config needed — site restriction is a code-level hostname post-filter.
- `applyAllowlistFilter` accepts `DiscoverProduct[]` (post-normalize) rather than raw `SerperShoppingItem[]` — cleaner API boundary, better testability.
- `runSearchPipeline` exported with injectable deps (PipelineDeps interface) so unit tests can drive it without Firestore — satisfies serperOrchestration.test.ts requirement from the plan.
- `parseGeminiResponse.test.ts` updated (not preserved) since it imported a now-removed function — auto-fix Rule 1 (blocking TypeScript compile error).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Updated parseGeminiResponse.test.ts to remove import of deleted function**
- **Found during:** Task 5 (npm run build failed)
- **Issue:** `parseGeminiResponse.test.ts` imported `parseGeminiResponse` which was replaced by `parseIntentResponse`; TS2305 compile error
- **Fix:** Rewrote the file to test `parseIntentResponse` + `DiscoverProduct` type export; the new `parseIntentResponse.test.ts` already covers the full intent parser
- **Files modified:** `functions/src/__tests__/discover/parseGeminiResponse.test.ts`
- **Verification:** `npm run build` exits 0; 117 tests green
- **Committed in:** c964bbd (Task 5 commit)

---

**Total deviations:** 1 auto-fixed (Rule 1 — blocking compile error from removed function)
**Impact on plan:** Zero scope creep. The fix was required for TypeScript to compile.

## Issues Encountered

None beyond the auto-fixed compile error.

## Known Stubs

None — all data paths are wired:
- `price` is now populated from Serper price strings via `parsePrice()` (no longer always 0)
- `image_url` is populated from Serper `imageUrl` field (always https from Google CDN)
- `retailer_name` is populated from `item.source` or DOMAIN_TO_RETAILER fallback
- `description` is intentionally `""` — Serper shopping endpoint carries no snippet

## Next Phase Readiness

- **Plan 17-08 (Android price guard + re-UAT):** Ready to proceed. Serper now returns real prices, so the Android `DiscoverProductCard.kt` `price > 0` guard is now meaningful (most cards will show a real price). Re-UAT of UAT-6 scenarios ("cadou copil 2 ani", "gift for coffee lover", etc.) can proceed on-device once 17-08 is deployed.
- The community-popular path (`getPopular.ts`, `triggers.ts`) was untouched — confirmed by triggers.test.ts passing in the full suite.

---
*Phase: 17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini*
*Completed: 2026-05-29*
