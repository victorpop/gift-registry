---
phase: 17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini
plan: 02
subsystem: backend
tags: [firebase-functions, firestore-rules, firestore-indexes, gemini, secrets, typescript, jest, tdd]

requires:
  - phase: 17-discover…/17-01
    provides: "firestore.rules clean of config/{configId}, tests/rules/firestore.rules.test.ts clean of config/stores describe — both prerequisites for adding three new collection rules without conflict."
  - phase: 03-registry-item-management
    provides: "Items at registries/{id}/items/{itemId} schema — basis for the productId derivation that popularItems will key on."

provides:
  - "Six TypeScript source files under functions/src/discover/: urlNormalization, retailers, promptTemplate, parseGeminiResponse, cacheKey (5 from Task 1) + secrets, geminiClient (2 from Task 2)."
  - "Five Jest test files under functions/src/__tests__/discover/ exercising 57 unit cases covering D-16/D-17/D-24/D-28/D-29/D-30 contracts."
  - "Firestore rules for three new collections: popularItems (auth + non-anonymous read; no client write), discoverCache (server-only), discoverRateLimits (server-only, even self-read denied)."
  - "Composite Firestore index on popularItems (registryCount DESC, updatedAt DESC) required by the discoverPopular() Callable in plan 17-03."
  - "10 new rules tests (3 describe blocks) — full rules suite 46/46 green."
  - "Gemini secret (GEMINI_API_KEY) declared via 2nd-gen defineSecret — ready for plans 17-03/17-04 to import without re-declaring."

affects: ["17-03-callables", "17-04-triggers-and-backfill", "17-06-deploy-and-uat"]

tech-stack:
  added:
    - "Node 22 native fetch (no node-fetch dep) for the Gemini HTTP call."
    - "AbortSignal.timeout(20000) bound for Gemini google_search grounding latency."
  patterns:
    - "Pure-function module per concern under functions/src/discover/* — each file is independently testable without Firebase initialization."
    - "TDD RED→GREEN commits on pure-function tasks (test commit precedes feat commit for Task 1)."
    - "Single defineSecret declaration shared across callers via `import { GEMINI_API_KEY } from './secrets'` — avoids drift if a 2nd Gemini caller is added."
    - "Verbatim spec preservation: RETAILERS arrays + Gemini prompt are exact copies of CONTEXT.md D-28/D-29 — drift here would be a contract violation."
    - "Firestore rules for server-only collections: `allow read, write: if false;` even for self-reads when tamper-enumeration is a concern (discoverRateLimits)."

key-files:
  created:
    - "functions/src/discover/urlNormalization.ts"
    - "functions/src/discover/retailers.ts"
    - "functions/src/discover/promptTemplate.ts"
    - "functions/src/discover/parseGeminiResponse.ts"
    - "functions/src/discover/cacheKey.ts"
    - "functions/src/discover/secrets.ts"
    - "functions/src/discover/geminiClient.ts"
    - "functions/src/__tests__/discover/urlNormalization.test.ts"
    - "functions/src/__tests__/discover/retailers.test.ts"
    - "functions/src/__tests__/discover/promptTemplate.test.ts"
    - "functions/src/__tests__/discover/parseGeminiResponse.test.ts"
    - "functions/src/__tests__/discover/cacheKeyNormalization.test.ts"
  modified:
    - "firestore.rules — three new match blocks for popularItems / discoverCache / discoverRateLimits."
    - "firestore.indexes.json — one new composite index on popularItems(registryCount DESC, updatedAt DESC)."
    - "tests/rules/firestore.rules.test.ts — three new describe blocks (10 cases)."

key-decisions:
  - "geminiClient.ts is intentionally NOT unit-tested (D-50 precedent — pure-function tests cover the parse/normalize contracts; the network call is exercised end-to-end via Plan 17-06 smoke test). Adding a network mock would test our own mock, not the API surface."
  - "cacheKey.ts (the cacheKeyNormalization helper) was placed in its own file rather than inline in search.ts — separating concerns keeps the rate-limit/cache key derivation independently testable + reusable from triggers if needed."
  - "Removed the unused `err` binding in the parseGeminiResponse catch clause (TypeScript noUnusedLocals=true would have failed the build). Catch is bare `catch {}` — the diagnostic console.error already includes raw + query context."
  - "Used parsed.host (not parsed.hostname) in normalizeUrl so non-standard ports survive into the canonical form (defensive — almost no retailer uses non-443 ports, but the URL spec is preserved)."

patterns-established:
  - "Discover feature module organization (functions/src/discover/) — Callables, triggers, helpers, and tests all under one feature folder, mirroring Phase 6 `notifications/` and Phase 3 `registry/`."
  - "Test fence-stripping helper pattern in parseGeminiResponse — defensive parser strips both ```json and plain ``` fences before JSON.parse."
  - "Rules-test forge of anonymous provider context via `testEnv.authenticatedContext(uid, { firebase: { sign_in_provider: 'anonymous' } })` — verified against @firebase/rules-unit-testing TokenOptions surface in node_modules."

requirements-completed:
  - D-16
  - D-17
  - D-23
  - D-24
  - D-27
  - D-28
  - D-29
  - D-30
  - D-43
  - D-45
  - D-46
  - D-48

duration: 4min
completed: 2026-05-27
---

# Phase 17 Plan 02: Backend Foundations Summary

**Discover backend utility layer wired: URL canonicalization + SHA-256 productId, verbatim Romanian retailer catalog, Gemini prompt template + defensive response parser, cache-key normalizer, GEMINI_API_KEY 2nd-gen secret, gemini-2.5-flash HTTP client with google_search grounding, three Firestore-rules locked-down collections, and composite popularItems index — all backed by 57 unit tests + 10 rules tests.**

## Performance

- **Duration:** 4 min (wall-clock between RED commit `774041a` and Task 2 commit `61d230b`)
- **Tasks:** 2 (Task 1 with TDD split into RED + GREEN commits)
- **Source files created:** 7 (5 in Task 1 + 2 in Task 2)
- **Test files created:** 5
- **Files modified:** 3 (firestore.rules, firestore.indexes.json, tests/rules/firestore.rules.test.ts)
- **Unit tests added:** 57 (5 new suites)
- **Rules tests added:** 10 (3 new describe blocks)

## Accomplishments

- Six pure TypeScript modules under `functions/src/discover/` (urlNormalization, retailers, promptTemplate, parseGeminiResponse, cacheKey) — all exposing the documented exports, all backed by 57 Jest tests covering the D-16/D-17/D-24/D-28/D-29/D-30 verbatim cases.
- One Gemini secret declaration (`secrets.ts`) + HTTP client (`geminiClient.ts`) calling the verbatim D-27 endpoint `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent` with `tools: [{ google_search: {} }]`.
- Three new Firestore rules blocks (D-43): popularItems (auth + non-anonymous read; no client write), discoverCache (server-only), discoverRateLimits (server-only, even self-read denied to prevent tamper enumeration).
- One composite Firestore index on popularItems(registryCount DESC, updatedAt DESC) — declared per Claude's-Discretion in CONTEXT.md for the discoverPopular() Callable in plan 17-03.
- Full rules test suite remains green at 46/46 (36 pre-existing + 10 new across three D-43 describe blocks).
- Functions TypeScript build passes cleanly (tsc, strict mode).

## Task Commits

Each task was committed atomically:

1. **Task 1 RED: failing tests for discover pure-utility modules** — `774041a` (test)
2. **Task 1 GREEN: implement discover pure-utility modules** — `84f4086` (feat)
3. **Task 2: secrets + geminiClient + rules + indexes + rules tests** — `61d230b` (feat)

(Task 1 used TDD per `tdd="true"` flag — RED commit added 57 failing tests; GREEN commit added the implementations and all tests passed. No refactor commit needed — implementation already matched the documented exports.)

## Files Created/Modified

### Created

- `functions/src/discover/urlNormalization.ts` — `normalizeUrl(url) → { productId, canonicalUrl }` with the TRACKING_PARAMS set, sha256 hex productId, https forcing, fragment drop, trailing-slash strip, query-param alphabetic sort.
- `functions/src/discover/retailers.ts` — `RETAILERS` (universal, birthday, wedding, housewarming, baby_shower, christmas) verbatim from D-28 + `selectSitesForQuery(query)` heuristic (Romanian + English keyword match → universal⧺category, default universal-only).
- `functions/src/discover/promptTemplate.ts` — `buildPrompt(query, sites) → { systemPrompt, userPrompt }` verbatim D-29 (includes the "Return ONLY a strict JSON array", "Return between 5 and 15 items", schema list, "Drop items missing title|price|retailer_url" directives).
- `functions/src/discover/parseGeminiResponse.ts` — Defensive JSON parser per D-30 (strips ```json and ``` fences, drops items missing title|price|retailer_url, coerces string prices via parseFloat, truncates descriptions over 200 chars, never throws).
- `functions/src/discover/cacheKey.ts` — `normalizeCacheKey(query)` D-24 (lowercase + trim + collapse whitespace + URL-encode for Firestore doc-ID safety; Romanian diacritics preserved through encode/decode round-trip).
- `functions/src/discover/secrets.ts` — `GEMINI_API_KEY` via `defineSecret("GEMINI_API_KEY")` per D-27 (2nd-gen secret, never logged).
- `functions/src/discover/geminiClient.ts` — `callGemini(prompt, apiKey) → Promise<string>` HTTP wrapper around the D-27 verbatim endpoint with `google_search` tool, 20s AbortSignal.timeout, throws on network/non-2xx.
- 5 Jest test files under `functions/src/__tests__/discover/` (57 cases total: urlNormalization 13, retailers 16, promptTemplate 8, parseGeminiResponse 13, cacheKeyNormalization 7).

### Modified

- `firestore.rules` — three new `match` blocks appended after the deleted `config/{configId}` placeholder comment. popularItems gates on `request.auth.token.firebase.sign_in_provider != "anonymous"`; discoverCache and discoverRateLimits are server-only (`if false` for all client ops).
- `firestore.indexes.json` — new composite index entry on `popularItems` for `(registryCount DESC, updatedAt DESC)`.
- `tests/rules/firestore.rules.test.ts` — three new `describe` blocks (popularItems / discoverCache / discoverRateLimits D-43); uses the `firebase: { sign_in_provider: 'anonymous' }` TokenOptions to forge the anonymous provider context.

## Decisions Made

- **geminiClient.ts intentionally has no unit test.** Following D-50 precedent (no UI tests; pure-function and integration tests suffice), the network call is exercised end-to-end via plan 17-06 smoke test rather than a mocked-fetch unit test that would only verify our own mock.
- **cacheKey lives in its own file** rather than inline in search.ts. It's a pure helper, independently testable, and will be reused from the search.ts callable + potentially from any future cache-warming utility.
- **Removed the unused `err` binding** in `parseGeminiResponse`'s catch clause (tsconfig `noUnusedLocals: true` would have failed the build). Bare `catch {}` works because the diagnostic `console.error` already includes raw response + query context.
- **Used `parsed.host` not `parsed.hostname`** in normalizeUrl so non-standard ports survive into the canonical form (defensive — almost no Romanian retailer uses non-443 ports, but this preserves the URL spec).
- **TRACKING_PARAMS membership check is case-insensitive on the key** (lowercased before lookup) so callers passing `UTM_SOURCE` are normalized the same as `utm_source`.

## Verbatim Spec Constants (for audit + drift watch)

### TRACKING_PARAMS list (D-16 step 3, verbatim from CONTEXT.md)

```
utm_source, utm_medium, utm_campaign, utm_term, utm_content,
fbclid, gclid, mc_cid, mc_eid,
ref, affiliate_id, cmpid
```

### RETAILERS arrays committed (D-28, verbatim match to CONTEXT.md `<specifics>`)

- **universal:** emag.ro, altex.ro, carrefour.ro, vivre.eu, elefant.ro, flanco.ro
- **birthday:** mindblower.ro, funfox.ro, borealy.ro, douglas.ro, sephora.ro, libris.ro, carturesti.ro
- **wedding:** 23h.ro, crisiashop.ro, wedday.ro, happycards.ro, magazinulmireselor.ro
- **housewarming:** jysk.ro, mobexpert.ro, ikea.com/ro, dedeman.ro, leroymerlin.ro, vivre.eu, insignis.ro, kika.ro, somproduct.ro
- **baby_shower:** bekid.ro, babyneeds.ro, bebelul.ro, bebebliss.ro, bebenou.ro, chicco.ro, erfi.ro, babymatters.ro, noriel.ro
- **christmas:** borealy.ro, mindblower.ro, funfox.ro, gourmetgift.ro, douglas.ro, sephora.ro, kaufland.ro, lidl.ro

(All six categories cross-checked against the spec block in CONTEXT.md `<specifics>` — exact match.)

### Gemini prompt directives (D-29, verbatim)

System prompt embeds (in order):
1. "You are a product-discovery assistant for a Romanian gift-registry app."
2. "Search the Romanian web for products matching the user's query."
3. `Prioritize results from these Romanian retailers (highest priority first): ${sitesList}.`
4. "Return prices in RON (Romanian lei)."
5. "Return ONLY a strict JSON array — no prose, no markdown code fences, no explanation."
6. "Schema per item: {title, description, image_url, price, currency, retailer_url, retailer_name}."
7. "Return between 5 and 15 items; if fewer than 5 confident matches exist, return what's available — never pad with low-quality matches."
8. "Drop items missing title, price, or retailer_url."

## TTL Commands Intended for Plan 17-06 Deploy (D-45)

These TTL policies are configured at deploy time and live in Plan 17-06's task action verbatim. Recorded here so the deploy plan does not have to re-derive them:

```bash
# 30-day cache TTL on the Gemini results cache
gcloud firestore fields ttls update cachedAt \
  --collection-group=discoverCache --enable-ttl

# 7-day TTL on the rate-limit counter docs (D-14)
gcloud firestore fields ttls update lastWriteAt \
  --collection-group=discoverRateLimits --enable-ttl
```

## Composite Index Deploy Reminder (Plan 17-06)

The new `popularItems(registryCount DESC, updatedAt DESC)` index is in
`firestore.indexes.json` but only takes effect after a deploy:

```bash
firebase deploy --only firestore:indexes
```

Per CONTEXT.md "Claude's Discretion", this index is required for the
`discoverPopular()` Callable in Plan 17-03 to order results without
PERMISSION_DENIED / unsupported-query errors. Without the index deployed,
the Callable in Plan 17-03 will succeed only against the emulator (no
composite-index enforcement in emulator).

## Deviations from Plan

None — plan executed exactly as written. Three minor implementation-level
adjustments documented under "Decisions Made" above (parseGeminiResponse
catch binding removed for strict-mode compile; cacheKey placed in its own
file per planned spec; `parsed.host` used over `parsed.hostname`); none of
these change documented behavior or plan scope.

## Issues Encountered

- **Pre-existing test failures in `functions/src/__tests__/createReservation.test.ts`** (3 cases in the "emulator-only setTimeout fallback" describe). Failure: `The default Firebase app does not exist. Make sure you call initializeApp() before using any of the Firebase services.` Origin commit `8672900` (pre-Phase 17). Out of scope for this plan per the SCOPE BOUNDARY rule. Logged to `.planning/phases/17-discover…/deferred-items.md`. The Phase 17 work itself adds zero failures: 57/57 discover tests pass, 164/164 non-createReservation Functions tests pass, 46/46 rules tests pass.

## User Setup Required

None — secret `GEMINI_API_KEY` is declared in code but the actual value is set by the user in Plan 17-06 deploy via `firebase functions:secrets:set GEMINI_API_KEY`. No env vars or dashboard changes required for this plan.

## Next Phase Readiness

- **Plan 17-03 (Callables) unblocked.** `secrets.ts` exports the `GEMINI_API_KEY` reference that `discoverPopular` and `discoverSearch` need in their `onCall({ secrets: [GEMINI_API_KEY], ... })` option. `geminiClient.callGemini`, `parseGeminiResponse`, `buildPrompt`, `selectSitesForQuery`, `normalizeCacheKey` are all in place and tested.
- **Plan 17-04 (triggers + backfill) unblocked.** `urlNormalization.normalizeUrl` is the productId/canonicalUrl source of truth for both the onCreate/onDelete triggers and the one-shot backfill script. Composite index for the popular ordering is declared.
- **Plan 17-06 (deploy + UAT) absorbs the TTL gcloud commands** documented above and the `firebase deploy --only firestore:indexes` step.

## Self-Check: PASSED

Files verified to exist:
- functions/src/discover/urlNormalization.ts ✓
- functions/src/discover/retailers.ts ✓
- functions/src/discover/promptTemplate.ts ✓
- functions/src/discover/parseGeminiResponse.ts ✓
- functions/src/discover/cacheKey.ts ✓
- functions/src/discover/secrets.ts ✓
- functions/src/discover/geminiClient.ts ✓
- functions/src/__tests__/discover/urlNormalization.test.ts ✓
- functions/src/__tests__/discover/retailers.test.ts ✓
- functions/src/__tests__/discover/promptTemplate.test.ts ✓
- functions/src/__tests__/discover/parseGeminiResponse.test.ts ✓
- functions/src/__tests__/discover/cacheKeyNormalization.test.ts ✓
- firestore.rules (modified) ✓
- firestore.indexes.json (modified) ✓
- tests/rules/firestore.rules.test.ts (modified) ✓

Commits verified in git log:
- 774041a (Task 1 RED) ✓
- 84f4086 (Task 1 GREEN) ✓
- 61d230b (Task 2) ✓

---
*Phase: 17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini*
*Completed: 2026-05-27*
