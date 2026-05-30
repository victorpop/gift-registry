---
phase: 17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini
verified: 2026-05-30T00:00:00Z
status: passed
score: 10/10 must-haves verified
re_verification: false
gaps: []
human_verification:
  - test: "Verify Discover bottom nav slot 2 shows Search icon + 'DISCOVER' / 'DESCOPERĂ' label on physical device"
    expected: "Slot 2 icon is the Search icon, label reads DISCOVER (English) or DESCOPERĂ (Romanian)"
    why_human: "Icon rendering and locale switch are visual; automated grep confirms string keys and NavSlotId.DISCOVER enum but cannot verify pixel-level rendering on-device."
  - test: "Verify FROM THE WEB section hides in the Idle state and appears after search submission"
    expected: "Only FROM THE COMMUNITY section visible on Discover screen load; FROM THE WEB section appears only after user submits a query."
    why_human: "Conditional rendering of LazyColumn sections requires on-screen visual verification (already validated in 17-08 UAT, but documented here for completeness)."
---

# Phase 17: Discover Feature Verification Report

**Phase Goal:** Ship the Discover bottom-nav surface (Android Compose) with two product-discovery sections (community-popular + Gemini-powered web search), backed by Cloud Functions Callables + Firestore triggers + a 30-day query cache, and fully decommission the Phase 7 Stores capability in the same phase.

**Search v2 re-scope (2026-05-28):** Original Gemini `google_search` grounding path (plans 17-01..17-06) exposed hallucination during UAT-6. Plans 17-07/17-08 replaced search internals: Gemini JSON-mode intent extraction (no grounding tools) → Serper.dev `/shopping` fan-out (≤3 queries) → 43-store hostname allowlist post-filter → de-dupe → 20-result cap → cache. Plans 17-06 (deploy work shipped, UAT superseded) and 17-08 (on-device re-UAT, 8/8 PASS 2026-05-30) are the authoritative close-out.

**Verified:** 2026-05-30
**Status:** PASSED
**Re-verification:** No — initial verification

---

## Goal Achievement

### Observable Truths

| #  | Truth | Status | Evidence |
|----|-------|--------|----------|
| 1  | Discover bottom-nav surface (slot 2) exists on Android with DiscoverKey wired in Navigation3 | VERIFIED | `AppNavKeys.kt:28` — `@Serializable data object DiscoverKey`; `AppNavigation.kt:159-213` — `onDiscover` callback pushes `DiscoverKey`, `entry<DiscoverKey> { DiscoverScreen() }` registered |
| 2  | Community-popular section backed by real Firestore triggers and discoverPopular Callable with L1 cache | VERIFIED | `getPopular.ts` exports `discoverPopular` with module-scope L1 cache (D-21); `triggers.ts` exports `onItemCreatePopular`, `onItemDeletePopular`, `onItemUpdatePopular`; all exported from `functions/src/index.ts:25-30` |
| 3  | Search v2: Gemini JSON-mode intent (NO grounding) → Serper /shopping fan-out → allowlist → 20-result cap → cache | VERIFIED | `geminiClient.ts:70,85-86` — `callGeminiIntent`, `response_mime_type:"application/json"`, `response_schema`; `search.ts:28,34-36,50,109,135` — Serper fan-out, `MAX_SEARCH_RESULTS = 20`, `dedupeByUrl(…).slice(0,20)`; no `google_search` grounding in active code paths |
| 4  | Auth gate + anonymous rejection + query validation + rate limit + App Check + region + 90s timeout | VERIFIED | `search.ts:45,147,149,160,165,216-221` — `REGION="europe-west3"`, unauthenticated throws, anonymous throws, >200 chars throws, `checkAndIncrementRateLimit`, `enforceAppCheck:true`, `timeoutSeconds:90` |
| 5  | 30-day query cache in discoverCache collection with deadline TTL semantics | VERIFIED | `search.ts:41-44,170-207` — `CACHE_TTL_MS = 30*24*60*60*1000`, `cachedAt` stored as deadline (`now + CACHE_TTL_MS`); TTL policies deployed in 17-06 |
| 6  | Phase 7 Stores fully decommissioned (code, drawables, strings, rules, FAB sheet row, seed scripts) | VERIFIED | `ui/store/`, `domain/store/`, `data/store/`, `di/StoresModule.kt` — all absent; 9 store_*.webp drawables gone; `stores_*`/`add_item_tab_browse` strings: grep returns 0; `config/{configId}` rule removed from `firestore.rules`; `seedStores.ts` + `stores.seed.json` deleted; no `BrowseStores` in `AddItemMode` or `AddItemScreen` |
| 7  | Discover strings in both values/strings.xml (EN) and values-ro/strings.xml (RO) — 11 keys per locale | VERIFIED | EN: `nav_discover_tab="DISCOVER"`, all 10 `discover_*` keys at lines 231-240; RO: `nav_discover_tab="DESCOPERĂ"`, all 10 discover_* keys with proper Unicode escapes at lines 232-241 |
| 8  | No Gemini hallucination path in active code (google_search tool not used) | VERIFIED | `grep -r "google_search" functions/src/discover/` returns only doc comments in `geminiClient.ts` and `search.ts` explaining the removal — zero active code invocations; `callGeminiIntent` uses only `response_mime_type`/`response_schema` |
| 9  | enrichImages.ts deleted; node-html-parser retained (still used by fetchOgMetadata.ts) | VERIFIED | `ls functions/src/discover/enrichImages.ts` → not found; `functions/package.json` has `"node-html-parser": "^7.1.0"`; `functions/src/registry/fetchOgMetadata.ts:2` imports it |
| 10 | On-device UAT signed off 8/8 PASS (2026-05-30) including critical RE-UAT-6 hallucination driver | VERIFIED | `17-08-UAT-RESULTS.md` — `status: passed`, 8/8 PASS, signed off by victorpop 2026-05-30; hallucination structurally eliminated |

**Score:** 10/10 truths verified

---

## Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `functions/src/discover/search.ts` | discoverSearch Callable — Serper pipeline | VERIFIED | `MAX_SEARCH_RESULTS=20`, `REGION="europe-west3"`, `enforceAppCheck:true`, `timeoutSeconds:90`, Gemini intent → Serper fan-out → allowlist → de-dupe → cap → cache |
| `functions/src/discover/serperClient.ts` | Serper /shopping POST wrapper | VERIFIED | File exists; `serperOrchestration.test.ts` drives it via injectable PipelineDeps |
| `functions/src/discover/serperNormalizer.ts` | 43-store ALLOWED_DOMAINS + normalize + allowlist filter + de-dupe | VERIFIED | 315 lines; `ALLOWED_DOMAINS = new Set([...])` (43-store set); `applyAllowlistFilter`, `dedupeByUrl`, `parsePrice` exported |
| `functions/src/discover/geminiClient.ts` | callGeminiIntent — JSON mode, no grounding tools | VERIFIED | `callGeminiIntent` at line 70; `response_mime_type:"application/json"`, `response_schema:INTENT_SCHEMA`; google_search removed |
| `functions/src/discover/getPopular.ts` | discoverPopular Callable with L1 in-memory cache | VERIFIED | Module-scope `l1Cache` with `expiresAt = now + L1_TTL_MS`; `discoverPopularHandler` checks cache before Firestore query |
| `functions/src/discover/triggers.ts` | onItemCreate/Delete/Update triggers + testable handlers | VERIFIED | `handleItemCreate`, `handleItemDelete`, `handleItemUpdate` (delegates to delete+create); all three onDocument* triggers exported |
| `functions/src/discover/secrets.ts` | GEMINI_API_KEY + SERPER_API_KEY secrets | VERIFIED | `defineSecret("GEMINI_API_KEY")` at line 32, `defineSecret("SERPER_API_KEY")` at line 34 |
| `functions/src/discover/rateLimit.ts` | 20/hr rolling-window rate limit | VERIFIED | `MAX_CALLS = 20`, `WINDOW_MS = 3600000` (1 hr); throws `resource-exhausted` at cap |
| `functions/src/discover/urlNormalization.ts` | normalizeUrl → productId (SHA-256) + canonicalUrl | VERIFIED | Exported `normalizeUrl`, TRACKING_PARAMS set, sha256 hex, https forcing, fragment drop |
| `functions/src/discover/cacheKey.ts` | normalizeCacheKey(query) | VERIFIED | `export function normalizeCacheKey` — lowercase + trim + collapse whitespace + encodeURIComponent |
| `functions/src/discover/retailers.ts` | RETAILERS verbatim + selectSitesForQuery heuristic | VERIFIED | 6 categories matching D-28 spec; `selectSitesForQuery` with Romanian+English keyword match |
| `functions/src/discover/promptTemplate.ts` | buildIntentPrompt (renamed from buildPrompt) | VERIFIED | Replaced with `buildIntentPrompt` for intent extraction per V2 re-scope |
| `functions/src/discover/parseGeminiResponse.ts` | parseIntentResponse (renamed; DiscoverProduct kept) | VERIFIED | `parseIntentResponse` exports `IntentResult`; `DiscoverProduct` interface retained for Android contract |
| `functions/scripts/deleteConfigStores.ts` | One-shot Admin SDK config/stores deletion script | VERIFIED | File exists; idempotent on missing doc; deployed during 17-06 |
| `functions/scripts/backfillPopularItems.ts` | One-shot backfill for popularItems | VERIFIED | File exists; 12 docs backfilled per 17-06 SUMMARY |
| `firestore.rules` | popularItems + discoverCache + discoverRateLimits rules; config/{configId} removed | VERIFIED | `match /popularItems/{productId}` at line 127; `match /discoverCache/{normalizedQuery}` at 135; `match /discoverRateLimits/{uid}` at 142; no `config/{configId}` |
| `firestore.indexes.json` | popularItems composite index (registryCount DESC, updatedAt DESC) | VERIFIED | Lines 49-53: collectionGroup="popularItems", registryCount DESCENDING, updatedAt DESCENDING |
| `app/src/main/java/com/giftregistry/ui/discover/DiscoverScreen.kt` | Compose screen with OutlinedTextField + LazyColumn + two sections | VERIFIED | File exists; Plan 05 SUMMARY confirms scaffold, OutlinedTextField ImeAction.Search, LazyColumn, conditional FROM THE WEB, always-on FROM THE COMMUNITY |
| `app/src/main/java/com/giftregistry/ui/discover/DiscoverProductCard.kt` | Product card with Coil AsyncImage + price guard | VERIFIED | `AsyncImage` import at line 25; placeholder at line 85-86; `price > 0.0` guard at line 112 (confirmed by 17-08 Task 1 no-edit verification) |
| `app/src/main/java/com/giftregistry/ui/discover/DiscoverViewModel.kt` | HiltViewModel with popular/search StateFlows | VERIFIED | File exists; Plan 05 SUMMARY: `loadPopular` in `init {}`, `popular`/`search`/`searchQuery` StateFlows, `retrySearch` |
| `app/src/main/java/com/giftregistry/domain/discover/DiscoverRepository.kt` | Repository interface | VERIFIED | `suspend getPopular()` + `suspend search(query)` returning `Result<List<DiscoverProduct>>` |
| `app/src/main/java/com/giftregistry/data/discover/DiscoverRepositoryImpl.kt` | Callable wrapper impl | VERIFIED | Wraps `discoverPopular` + `discoverSearch` Callables; maps HashMap response to `DiscoverProduct` domain models |
| `app/src/main/java/com/giftregistry/di/DiscoverModule.kt` | Hilt @Binds module | VERIFIED | File exists; `@Binds DiscoverRepository -> DiscoverRepositoryImpl` |
| `app/src/main/res/drawable/discover_card_placeholder.xml` | Vector drawable for card placeholder | VERIFIED | File exists; gradient + gift-box glyph |
| `app/src/main/res/values/strings.xml` | 11 discover_* keys (EN) | VERIFIED | Lines 231-240: all 11 keys present with EN values |
| `app/src/main/res/values-ro/strings.xml` | 11 discover_* keys (RO) with diacritics | VERIFIED | Lines 232-241: all 11 keys with Romanian translations using &#NNNN; Unicode escapes; nav_discover_tab="DESCOPERĂ" |

---

## Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `GiftMaisonBottomNav.kt` | `DiscoverKey` | `NavSlotId.DISCOVER`, `onDiscover` callback, `nav_discover_tab` | WIRED | Slot 2 enum renamed, icon swapped to `Icons.Outlined.Search`, `nav_discover_tab` resolved from strings.xml |
| `AppNavigation.kt` | `DiscoverScreen()` | `entry<DiscoverKey> { DiscoverScreen() }` | WIRED | Entry block registered at line 213; onDiscover callback pushes DiscoverKey at lines 159-163 |
| `DiscoverRepositoryImpl` | `discoverPopular` + `discoverSearch` Callables | `FirebaseFunctions.getInstance("europe-west3").getHttpsCallable(name)` | WIRED | AppModule provides FirebaseFunctions pinned to europe-west3; DiscoverModule binds the impl |
| `search.ts` | Serper `/shopping` API | `callSerper(cat.searchQuery, SERPER_API_KEY)` | WIRED | `serperFn: callSerper` injected in prod path; SERPER_API_KEY bound to runtime SA |
| `search.ts` | Gemini intent extraction | `callGeminiIntent(prompt, GEMINI_API_KEY)` | WIRED | JSON mode, no tools; `intentFn: callGeminiIntent` injected in prod path |
| `search.ts` | discoverCache | `db.collection("discoverCache").doc(cacheKey)` | WIRED | Cache check before pipeline; cache write after successful pipeline (products.length > 0 guard) |
| `triggers.ts` | `popularItems` collection | `handleItemCreate`/`handleItemDelete`/`handleItemUpdate` + Admin SDK | WIRED | Three `onDocument*` triggers registered; functions exported in `index.ts:28-30` |
| `functions/src/index.ts` | all 5 Phase 17 exports | `export { discoverPopular, discoverSearch, onItemCreatePopular, onItemDeletePopular, onItemUpdatePopular }` | WIRED | Lines 25-30 in index.ts |
| `AddItemMode` | 2-tab enum (PasteUrl, Manual) | BrowseStores variant removed | WIRED | `AddItemMode.BrowseStores` absent from source; no `onNavigateToBrowseStores` in `AddItemScreen` |

---

## Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|--------------------|--------|
| `DiscoverScreen.kt` | `popular` StateFlow (PopularState.Loaded) | `discoverPopular` Callable → Firestore `popularItems` query → domain mapping | Yes — Firestore `popularItems` is populated by triggers on item create/delete/update + backfill script | FLOWING |
| `DiscoverScreen.kt` | `search` StateFlow (SearchState.Loaded) | `discoverSearch` Callable → Gemini intent → Serper /shopping → normalize → allowlist → cache | Yes — Serper returns real product titles/prices/images from Romanian retailers; UAT-6 driver confirmed no hallucination | FLOWING |
| `DiscoverProductCard.kt` | `product.price` (price guard) | `serperNormalizer.parsePrice(item.price)` | Yes — Serper /shopping carries price strings for most results; `parsePrice` produces real number; `price > 0.0` guard prevents "0,00 RON" display | FLOWING |

---

## Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| discoverSearch Callable exported from index.ts | `grep "discoverSearch" functions/src/index.ts` | `export { discoverSearch } from "./discover/search"` | PASS |
| discoverPopular Callable exported | `grep "discoverPopular" functions/src/index.ts` | `export { discoverPopular } from "./discover/getPopular"` | PASS |
| All 5 trigger exports present | `grep "onItem.*Popular" functions/src/index.ts` | Lines 28-30 with all 3 triggers | PASS |
| No google_search grounding in active paths | `grep -r "google_search" functions/src/discover/` | Only doc-comment mentions (UAT-6 fix noted) — zero active code invocations | PASS |
| 20-result cap enforced | `grep "MAX_SEARCH_RESULTS" functions/src/discover/search.ts` | `const MAX_SEARCH_RESULTS = 20` + `dedupeByUrl(allProducts).slice(0, MAX_SEARCH_RESULTS)` | PASS |
| enrichImages.ts deleted | `ls functions/src/discover/enrichImages.ts` | Not found | PASS |
| Stores decommission: no store code in app/src | `grep -r "StoreListKey\|StoreBrowserKey\|StoreRepository" app/src/` | Zero matches | PASS |
| Discover strings in both locales | `grep "nav_discover_tab" values/strings.xml values-ro/strings.xml` | Lines 231 (EN) + 232 (RO) with correct values | PASS |
| Price guard in DiscoverProductCard | `grep "price > 0" DiscoverProductCard.kt` | Line 112: `if (product.price > 0.0)` | PASS |
| On-device UAT | 17-08-UAT-RESULTS.md | 8/8 PASS including RE-UAT-6 driver; signed off 2026-05-30 | PASS |

---

## Requirements Coverage

Phase 17 uses CONTEXT.md decisions D-01..D-51 as the requirement set, plus V2-D1, V2-ARCH, V2-RULE, V2-STORES, V2-COST from 17-SEARCH-V2-SPEC.md. REQUIREMENTS.md has no formal REQ-IDs for Phase 17.

| Decision Set | Plans | Status | Evidence |
|--------------|-------|--------|----------|
| D-01..D-02 (Nav slot, DiscoverKey) | 17-05 | SATISFIED | NavSlotId.DISCOVER, DiscoverKey in AppNavKeys, AppNavigation entry wired |
| D-03..D-09 (Stores decommission) | 17-01 | SATISFIED | All store code/drawables/strings/rules/scripts deleted; AddItemMode 2-tab |
| D-10..D-12 (Callable transport, region, auth gate) | 17-03 | SATISFIED | europe-west3, unauthenticated + anonymous rejection in search.ts |
| D-13..D-14 (Rate limit 20/hr, TTL on rateLimits) | 17-03, 17-06 | SATISFIED | rateLimit.ts MAX_CALLS=20, WINDOW_MS=3600000; TTL deployed per 17-06 |
| D-15 (discoverPopular no rate limit) | 17-03 | SATISFIED | Only discoverSearch calls checkAndIncrementRateLimit |
| D-16..D-17 (URL normalization, SHA-256 productId) | 17-02 | SATISFIED | urlNormalization.ts with 7-step pipeline, sha256 hex productId |
| D-18..D-22 (Trigger + backfill + L1 cache + discoverPopular shape) | 17-04, 17-03 | SATISFIED | triggers.ts, backfillPopularItems.ts, getPopular.ts L1 cache |
| D-23..D-26 (Query validation, cache key, cache TTL, flow) | 17-03, 17-06 | SATISFIED | QUERY_MAX_LEN=200, normalizeCacheKey, discoverCache with 30-day deadline TTL |
| D-27..D-31 (Gemini client, retailers, prompt, parser, response shape) | 17-02, superseded by 17-07 | SATISFIED via v2 | callGeminiIntent replaces callGemini; promptTemplate → buildIntentPrompt; parseGeminiResponse → parseIntentResponse; DiscoverProduct contract unchanged |
| D-32..D-33 (Raw URL tap, ActivityNotFoundException Snackbar) | 17-05 | SATISFIED | `Intent.ACTION_VIEW` raw retailerUrl; Snackbar on ActivityNotFoundException |
| D-34..D-40 (Android UI structure: DiscoverScreen, ViewModel, Repository) | 17-05 | SATISFIED | All files exist with documented exports and behavior |
| D-41..D-42 (Discover strings, stores strings removed) | 17-05, 17-01 | SATISFIED | 11 discover_* keys × 2 locales; all stores_* keys absent |
| D-43..D-44 (Firestore rules: 3 collections, config removed) | 17-02, 17-01 | SATISFIED | popularItems/discoverCache/discoverRateLimits rules present; config/{configId} absent |
| D-45 (TTL policies) | 17-06 | SATISFIED | TTL policies deployed (confirmed in 17-06 SUMMARY) |
| D-46..D-47 (Module structure, index.ts exports) | 17-02..17-04 | SATISFIED | functions/src/discover/ has all documented files; index.ts exports all 5 functions |
| D-48..D-51 (Tests, Android tests, StyleGuidePreview) | 17-02..17-05 | SATISFIED | 10 backend test suites (118/118); 17 Android unit tests; DiscoverPreview in StyleGuidePreview |
| V2-D1, V2-ARCH, V2-RULE, V2-STORES, V2-COST (Search v2 re-scope) | 17-07 | SATISFIED | Gemini intent-only; Serper /shopping; 43-store allowlist; community-popular untouched; cost ~$0.003/search |

---

## Anti-Patterns Found

| File | Pattern | Severity | Assessment |
|------|---------|----------|------------|
| `functions/src/discover/serperNormalizer.ts:13,21` | Comment references deleted `enrichImages.ts` | Info | Doc comments only — not active code; function was correctly copied before deletion. No impact. |
| `functions/src/discover/search.ts:219-221` | `// ~2s Serper × 3 parallel + ~15s intent = well within 90s` | Info | Performance estimate comment; 90s timeout is correctly set. No impact. |

No blockers or warnings found. All data paths produce real values (no hardcoded empty arrays, no stub returns in the production path).

---

## Human Verification Required

### 1. Discover bottom nav icon and label rendering

**Test:** Switch device locale to Romanian (Settings → Language), open the app, observe bottom nav slot 2.
**Expected:** Slot 2 shows the Search icon and label "DESCOPERĂ" in Romanian locale; "DISCOVER" in English.
**Why human:** Automated checks confirm `NavSlotId.DISCOVER`, `Icons.Outlined.Search`, and string key presence. Pixel-level icon rendering and locale-switch behavior require on-device observation. (Partially validated by UAT RE-UAT-11 which confirmed DESCOPERĂ + DE PE WEB + DIN COMUNITATE in Romanian locale.)

### 2. FROM THE WEB / FROM THE COMMUNITY section ordering

**Test:** Open Discover tab; observe that only FROM THE COMMUNITY section is visible. Enter a search query and submit; observe that FROM THE WEB section appears above FROM THE COMMUNITY.
**Expected:** Section ordering matches D-36 (web on top after search, community always visible, web hidden in idle state).
**Why human:** Section ordering under real async state transitions is most reliably verified on-device. (Validated in 17-08 UAT RE-UAT-07, RE-UAT-08, RE-UAT-11.)

---

## Out-of-Scope Debt / Follow-ups

### Pre-existing RED test failures in `createReservation.test.ts` (NOT Phase 17 regressions)

The project-wide test suite reports 3 failing cases in `functions/src/__tests__/createReservation.test.ts` ("emulator-only setTimeout fallback" describe block). These are:

- **Origin commit:** `8672900` ("add RED test for emulator fallback") — predates Phase 17
- **Nature:** TDD RED tests awaiting a GREEN implementation; failing with `The default Firebase app does not exist` because the emulator-fallback path was never implemented
- **Phase 17 isolation:** `git log --grep="17-0" -- functions/src/reservation/` returns zero commits — no Phase 17 commit touched reservation code
- **Phase 17 test suite:** 118/118 discover tests green; 3 reservation failures are pre-existing tech debt
- **Tracking:** Logged in `.planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/deferred-items.md`

These failures are out of scope for Phase 17 verification and do not constitute a phase gap.

---

## Gaps Summary

No gaps. All 10 observable truths verified. All required artifacts exist, are substantive (not stubs), are wired into the live system, and produce real data (confirmed by 17-08 UAT). The Stores decommission is complete and irreversible. The Search v2 re-scope structurally eliminates the UAT-6 hallucination. On-device sign-off is logged in 17-08-UAT-RESULTS.md.

---

_Verified: 2026-05-30_
_Verifier: Claude (gsd-verifier)_
