---
phase: 17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini
plan: 03
subsystem: api
tags: [firebase-functions, callable, gemini, rate-limit, firestore, ttl, jest, typescript]

# Dependency graph
requires:
  - phase: 17-discover (plan 17-02)
    provides: secrets.GEMINI_API_KEY, normalizeCacheKey, selectSitesForQuery, buildPrompt, callGemini, parseGeminiResponse
provides:
  - "discoverPopular onCall Callable (top-20 popularItems + L1 in-memory cache, 1h TTL)"
  - "discoverSearch onCall Callable (validate -> rate-limit -> cache -> Gemini -> conditional cache write)"
  - "checkAndIncrementRateLimit transactional 20/hr rolling-window helper (re-usable)"
  - "Discover Callable response contract { products, cached_at } (D-31)"
affects: [17-04-triggers-and-backfill, 17-05-android-discover, 17-06-deploy-and-uat]

# Tech tracking
tech-stack:
  added: []  # No new dependencies — uses Node 22 built-in fetch, firebase-admin, firebase-functions v2 (already in package.json)
  patterns:
    - "TTL-deadline write (Timestamp.fromDate(now + TTL_MS)) — matches Firestore TTL delete-when-field-value-<-now semantics"
    - "Callable handler split: testable inner handler (e.g. discoverPopularHandler) + thin onCall wrapper for region/secrets/AppCheck config"
    - "Transactional rate-limit (read+filter+write inside runTransaction) — prevents concurrent-call race past the cap"
    - "Module-scope L1 cache for cold-path Callables (1h TTL within Function instance)"

key-files:
  created:
    - "functions/src/discover/rateLimit.ts"
    - "functions/src/discover/getPopular.ts"
    - "functions/src/discover/search.ts"
    - "functions/src/__tests__/discover/rateLimit.test.ts"
  modified:
    - "functions/src/index.ts (added discoverPopular + discoverSearch named exports)"

key-decisions:
  - "Kept enforceAppCheck: true on both Callables (Phase 14/16 precedent — acceptInvite.ts/declineInvite.ts)"
  - "Rate-limit constants: WINDOW_MS=3600000 (1h), MAX_CALLS=20, RATE_LIMIT_TTL_MS=604800000 (7d) — matches D-13/D-14 verbatim"
  - "cachedAt is written as the TTL deadline (now + 30d), NOT serverTimestamp() — D-45 TTL semantics"
  - "Recover client-facing cached_at on hit by subtracting CACHE_TTL_MS from the stored deadline"
  - "Do not cache empty/failed Gemini responses (Claude's Discretion in CONTEXT.md — avoid permanently caching transient failures)"

patterns-established:
  - "Pattern: TTL deadline write — every TTL-managed Firestore field stores (now + TTL_MS) so the doc survives the full window"
  - "Pattern: Callable handler split — export both `xHandler(request)` (testable) and `x = onCall(opts, xHandler)` (deploy artifact)"
  - "Pattern: Discover Callable response shape — { products, cached_at } where cached_at is an ISO timestamp string"

requirements-completed:
  - D-10
  - D-11
  - D-12
  - D-13
  - D-15
  - D-20
  - D-21
  - D-23
  - D-25
  - D-26
  - D-31
  - D-47
  - D-48

# Metrics
duration: 3min
completed: 2026-05-27
---

# Phase 17 Plan 03: Discover Callables Summary

**discoverPopular + discoverSearch Callables (europe-west3, App Check on) with transactional 20/hr rate-limit, 30-day Firestore cache (TTL-deadline writes), and 1h in-memory L1 cache for popular items.**

## Performance

- **Duration:** 3 min
- **Started:** 2026-05-27T14:44:24Z
- **Completed:** 2026-05-27T14:47:53Z
- **Tasks:** 2 (Task 1 TDD → 2 commits RED+GREEN; Task 2 → 1 commit)
- **Files modified:** 4 created + 1 modified (`functions/src/index.ts`)

## Accomplishments

- `checkAndIncrementRateLimit(db, uid)` — Firestore-transactional rolling-window 20/hr counter with 7-day TTL on `lastWriteAt` (stored as a deadline, not a creation time). 5 unit-test cases cover new-uid, under-limit, at-cap reject, expired-filter cleanup, and HttpsError code.
- `discoverPopular` Callable — queries `popularItems orderBy(registryCount desc, updatedAt desc).limit(20)`, returns spec shape `{ products: [{ id, title, description, image_url, price, currency, retailer_url }] }`. Module-scope L1 cache with 1h TTL inside the Function instance (D-21). No L2 Firestore cache — `popularItems` is authoritative.
- `discoverSearch` Callable — full orchestration: auth gate → query validation (1–200 chars) → transactional rate-limit → cache read → Gemini call (with `[GEMINI_API_KEY]` secret) → defensive parse → conditional cache write (only when products non-empty). Returns `{ products, cached_at }` per D-31.
- Both Callables on `europe-west3` with `enforceAppCheck: true`, rejecting unauthenticated and anonymous-provider Auth per D-12.
- Both Callables exported from `functions/src/index.ts` (lines 25–26).

## Task Commits

1. **Task 1 RED — failing rate-limit test** — `a537e4d` (test)
2. **Task 1 GREEN — `checkAndIncrementRateLimit` impl** — `de001a9` (feat)
3. **Task 2 — `discoverPopular` + `discoverSearch` Callables** — `eaf61b2` (feat)

`functions/src/index.ts` named exports were committed by the parallel plan 17-04 agent (`aef1d98 feat(17-04): implement popularItems triggers (GREEN)`) — the parallel agent included my two `discover/getPopular` + `discover/search` named exports in their own commit when they added their three trigger exports to the same file. Verified the exports are present.

**Plan metadata:** [appended in final commit]

## Files Created/Modified

- `functions/src/discover/rateLimit.ts` (created) — transactional 20/hr per-uid counter
- `functions/src/discover/getPopular.ts` (created) — `discoverPopular` Callable + `discoverPopularHandler`
- `functions/src/discover/search.ts` (created) — `discoverSearch` Callable + `discoverSearchHandler`
- `functions/src/__tests__/discover/rateLimit.test.ts` (created) — 5 jest cases for the rate-limit helper
- `functions/src/index.ts` (modified) — added 2 named exports (discoverPopular, discoverSearch); the parallel 17-04 agent landed the commit with both my exports and their 3 trigger exports

## Decisions Made

- **`enforceAppCheck: true` retained on both Callables.** Confirmed via `acceptInvite.ts` (Phase 16) which uses the same flag. Plan 17-06 deploy task assumes App Check is enabled at deploy.
- **Rate-limit constants: WINDOW_MS=3,600,000 ms, MAX_CALLS=20, RATE_LIMIT_TTL_MS=604,800,000 ms.** Matches D-13 (20/1h) and D-14 (7-day TTL) verbatim.
- **`cachedAt` and `lastWriteAt` written as deadlines, NOT `FieldValue.serverTimestamp()`.** Per D-45 + Firestore TTL semantics (delete when field_value < current_time), storing the creation time would mark every doc eligible for deletion immediately on the next TTL sweep.
- **Recover cached_at on hit by subtracting CACHE_TTL_MS from the stored deadline.** The client sees the original creation time (ISO string), matching the D-31 contract, while Firestore TTL still uses the deadline to retain the doc for the full 30-day window.
- **Do not cache empty results or Gemini errors.** Claude's Discretion in CONTEXT.md — avoids permanently caching transient failures. Empty-result responses still return `{ products: [], cached_at: now.toISOString() }` so the client gets a fresh timestamp on each retry.

## Deviations from Plan

None — plan executed exactly as written.

## Issues Encountered

- **Parallel agent timing:** When I ran my first `npm run build` after writing Task 2 files, the build failed on `functions/src/__tests__/discover/triggers.test.ts` (RED-phase test from plan 17-04 which hadn't yet had its implementation landed). On retry seconds later, plan 17-04 had committed `triggers.ts` (`aef1d98`) and the build passed. This is plan 17-04's RED→GREEN cycle, not a deviation in this plan. The parallel agent also rewrote `functions/src/index.ts` between my Edit call and my commit, but kept both my exports in place (lines 25–26), so the file ended with all 5 new exports (2 Callables + 3 triggers).
- **No conflicts on `functions/src/index.ts`.** Both agents ended up writing identical content in the discoverPopular/discoverSearch lines, so git resolved cleanly. No rebase required.

## User Setup Required

**Pre-deploy (plan 17-06 explicitly owns this step) — `GEMINI_API_KEY` must be set:**

```bash
firebase functions:secrets:set GEMINI_API_KEY
# Paste the API key when prompted. Get a key from
# https://aistudio.google.com/app/apikey (free tier OK for testing).
```

Verify the secret is bound:

```bash
firebase functions:secrets:get GEMINI_API_KEY
```

Without this, `discoverSearch` will fail at runtime when it calls `GEMINI_API_KEY.value()`.

**Firestore TTL policies** (also plan 17-06):

```bash
gcloud firestore fields ttls update cachedAt \
  --collection-group=discoverCache --enable-ttl
gcloud firestore fields ttls update lastWriteAt \
  --collection-group=discoverRateLimits --enable-ttl
```

Both fields are written as deadlines (now + TTL_MS), so once TTL is enabled in Firestore the docs will be cleaned up automatically when their windows expire.

## Next Phase Readiness

- Plan 17-04 (popularItems triggers + backfill): both Callables read from `popularItems` and `discoverRateLimits`; the L1 cache in `discoverPopular` means a one-hour warm-up window where new trigger writes won't surface immediately. Acceptable per D-21.
- Plan 17-05 (Android Discover screen): two Callables `discoverPopular` + `discoverSearch` are ready to invoke via `FirebaseFunctions.getInstance("europe-west3").getHttpsCallable(name)`. Response shape:
  - `discoverPopular`: `{ products: [{ id, title, description, image_url, price, currency, retailer_url }] }`
  - `discoverSearch`: `{ products: [{ title, description, image_url, price, currency, retailer_url, retailer_name }], cached_at: ISO string }`
- Plan 17-06 (deploy + UAT): set `GEMINI_API_KEY` secret + enable Firestore TTL policies before deploy. Verify App Check is enforced on both Callables (it is, per code).

## Self-Check: PASSED

- FOUND: functions/src/discover/rateLimit.ts
- FOUND: functions/src/discover/getPopular.ts
- FOUND: functions/src/discover/search.ts
- FOUND: functions/src/__tests__/discover/rateLimit.test.ts
- FOUND commit a537e4d (test: failing rate-limit tests)
- FOUND commit de001a9 (feat: rateLimit impl)
- FOUND commit eaf61b2 (feat: discoverPopular + discoverSearch)
- FOUND named exports `discoverPopular` + `discoverSearch` in `functions/src/index.ts` (lines 25-26)
- All 7 discover test suites pass (77 tests, including the 5 new rate-limit cases)
- `npm run build` (tsc) clean

---
*Phase: 17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini*
*Completed: 2026-05-27*
