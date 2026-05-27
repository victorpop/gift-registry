# Phase 17: Discover Feature - Context

**Gathered:** 2026-05-27
**Status:** Ready for planning

<domain>
## Phase Boundary

Add a Discover bottom-nav surface to the Android app with two product-discovery sections, plus the Firebase Cloud Functions and Firestore data structures that power them:

1. **"From the community"** — top-N (default 20) products aggregated across all gift registries, ranked by how many distinct registries each unique product appears in (descending, all-time).
2. **"From the web"** — natural-language product search via Google Gemini 2.5 Flash with `google_search` grounding, biased toward Romanian retailers (occasion-aware site lists), backed by a 30-day Firestore-backed query cache.

This phase **decommissions the Phase 7 Stores capability entirely** (Stores nav slot, StoreListScreen, StoreBrowserScreen, StoreRepository, `config/stores` Firestore doc, store-logo drawables, `seedStores.ts` seed script, FAB sheet's "Browse stores" row).

**In scope:**
- Discover nav slot (replaces Stores slot 2 in `GiftMaisonBottomNav.kt`)
- DiscoverScreen + DiscoverViewModel + DiscoverRepository (Android, Compose + Hilt)
- Two Firebase Callable Functions: `discoverPopular`, `discoverSearch`
- Firestore-triggered `popularItems` counter with onCreate/onDelete maintenance + one-shot backfill script
- 30-day query cache for Gemini results (`discoverCache` collection)
- Per-user rolling-window rate limit for `discoverSearch` (20/hr) backed by `discoverRateLimits` collection
- Full Stores decommissioning (code + Firestore doc + drawables + strings + FAB sheet row)
- en + ro strings for all new UI surfaces (`discover_*` namespace)

**Out of scope:**
- Analytics events (user explicit: "I'll add those later")
- "Add to my registry" action from Discover cards (user explicit: "just open the URL")
- Pagination / infinite scroll (user explicit: "fixed list sizes")
- Per-user personalization (user explicit: "global popular for everyone")
- Price-refresh logic for cached search results (user explicit: "30-day cache is fine as-is")
- Affiliate URL transformation on Discover taps (user explicit choice to honor spec literally — see D-21)
- Web fallback variant (spec doesn't mention; Android-only)

</domain>

<decisions>
## Implementation Decisions

### Nav Slot Replacement (Bottom Nav Wiring)

- **D-01:** Slot 2 of `GiftMaisonBottomNav.kt` (currently "Stores" / `Icons.Outlined.Storefront` → `StoreListKey`) is rewired to Discover. Icon: `Icons.Outlined.Search`. Label string keys: `R.string.nav_discover_tab` = "DISCOVER" (en) / "DESCOPERĂ" (ro). The `NavSlotId.STORES` enum value is renamed to `NavSlotId.DISCOVER` and the `onStores` callback parameter is renamed to `onDiscover`.
- **D-02:** New Navigation3 key `@Serializable data object DiscoverKey` added to `AppNavKeys.kt`. `NavVisibility.kt`'s "shows bottom nav" predicate is extended to include `DiscoverKey` (Discover is a nav root, must show the bar). `AppNavigation.kt` adds an `entry<DiscoverKey> { DiscoverScreen(...) }` entry, and the bottom-nav `onDiscover` callback pushes `DiscoverKey` onto the back stack (clearing duplicates per nav-root convention).

### Stores Capability Decommissioning (Full Removal)

- **D-03:** Delete all Phase 7 code: `app/src/main/java/com/giftregistry/ui/store/list/` (StoreListScreen, StoreListViewModel, StoreListUiState, StoreListErrorState), `app/src/main/java/com/giftregistry/ui/store/browser/` (StoreBrowserScreen, StoreBrowserViewModel, all WebView wiring), `app/src/main/java/com/giftregistry/domain/store/` (StoreRepository interface), `app/src/main/java/com/giftregistry/data/store/` (StoreRepositoryImpl, store DTO mappers), `app/src/main/java/com/giftregistry/di/StoresModule.kt`, `app/src/main/java/com/giftregistry/data/preferences/LastRegistryPreferencesDataStore.kt` (used only by store browser per Phase 7 D-10).
- **D-04:** Delete `StoreListKey` and `StoreBrowserKey` from `AppNavKeys.kt`. Remove their `entry<...> { ... }` blocks from `AppNavigation.kt`. Remove them from `NavVisibility.kt`'s hide-list (they're being deleted, so they can't be referenced).
- **D-05:** Delete all `store_*.webp` drawables in `app/src/main/res/drawable-nodpi/` (store_emag, store_altex, store_flanco, store_libris, store_carturesti, store_ikea, store_dedeman, store_elefant, store_generic).
- **D-06:** Delete all `stores_*` and `nav_stores_tab` string keys from `app/src/main/res/values/strings.xml` and `values-ro/strings.xml`.
- **D-07:** Delete the `config/stores` Firestore document via a one-shot cleanup step (Firebase CLI `firestore:delete` or an Admin SDK script). Remove the `match /config/{configId}` rule from `firestore.rules` (and its test from `tests/rules/firestore.rules.test.ts`).
- **D-08:** Delete `functions/scripts/seedStores.ts` and any reference to it in `functions/data/stores.seed.json` (if present).
- **D-09:** Remove the "Browse stores" row from the FAB Add-action bottom sheet (Phase 9 D-09 — `add_sheet_browse_stores` action). The Add-action sheet retains "Create registry" / "Item from URL" / "Add manually" only.

### Backend Transport + Access Policy

- **D-10:** Two Firebase Callable Functions in `functions/src/discover/`: `discoverPopular()` and `discoverSearch({ query })`. **NOT** REST `onRequest`. **NOT** Retrofit on Android. Android calls via `FirebaseFunctions.getInstance("europe-west3").getHttpsCallable(name)`, matching every other Function in the project (`fetchOgMetadata`, `inviteToRegistry`, etc.). Spec's "GET /discover/popular" and "POST /discover/search" are translated to Callable invocations.
- **D-11:** Region: `europe-west3` (matches all other Functions per Phase 1 / Phase 7 convention).
- **D-12:** Both Callables reject unauthenticated requests: throw `HttpsError("unauthenticated", "Sign in required")` if `request.auth` is null. Both Callables also reject anonymous-provider Auth: if `request.auth.token.firebase.sign_in_provider === "anonymous"`, throw `HttpsError("permission-denied", "Registered account required")`. Discover is for signed-in registered users only — anonymous (web giver path, Phase 15 territory) is explicitly excluded.

### Rate Limiting

- **D-13:** Per-user rolling-window rate limit on `discoverSearch`: 20 calls per 1 hour per `request.auth.uid`. Storage: `discoverRateLimits/{uid}` Firestore doc holding `{ timestamps: number[] }` (array of recent epoch-ms call timestamps). On each call: read doc, filter to entries within last 3600000 ms, if length >= 20 throw `HttpsError("resource-exhausted", "Rate limit exceeded")`, else append `Date.now()` to filtered list and `set` the doc atomically (use Firestore transaction).
- **D-14:** Firestore TTL policy on `discoverRateLimits.lastWriteAt` field with 7-day expiry — auto-cleans abandoned counters. (Note: `lastWriteAt` is a top-level Timestamp on the doc, distinct from `timestamps[]` which is array of numbers.)
- **D-15:** `discoverPopular` has NO rate limit. It is heavily cached (in-memory L1 inside Function instance, 1hr TTL) and cheap to serve from cache. Cache miss still hits Firestore `popularItems` which is O(1) ordered read.

### Popular-Item Identity (Dedupe Rule)

- **D-16:** Product identity = SHA-256 hex digest of normalized `originalUrl`. Normalization steps, in order:
  1. Parse URL.
  2. Lowercase the host.
  3. Strip these query params: `utm_source`, `utm_medium`, `utm_campaign`, `utm_term`, `utm_content`, `fbclid`, `gclid`, `mc_cid`, `mc_eid`, plus any EMAG affiliate suffix params (`ref=`, `affiliate_id=`, `cmpid=`).
  4. Sort remaining query params alphabetically by key.
  5. Drop URL fragment (`#...`).
  6. Strip trailing slash from path (unless path is just `/`).
  7. Reconstruct as `https://{host}{path}{?sortedQuery}` (force `https` scheme).
- **D-17:** Both the SHA-256 hex digest (productId) AND the canonical normalized URL string are stored on the `popularItems/{productId}` doc — agents reading the doc can show the canonical URL without recomputing.

### Popular-Item Aggregation (Counter Strategy)

- **D-18:** Firestore-triggered counter maintenance. Two new Functions in `functions/src/discover/triggers.ts`:
  - `onItemCreatePopular` (trigger: `onDocumentCreated("registries/{registryId}/items/{itemId}")`): normalize the new item's `originalUrl` → productId. Read `popularItems/{productId}`; if missing, create with `{ canonicalUrl, title, imageUrl, price, registryIds: [registryId], registryCount: 1, updatedAt: serverTimestamp() }`. If present and `registryIds` does not contain `registryId`, add it via `arrayUnion` and increment `registryCount` via `increment(1)`. If `registryId` is already in `registryIds`, no-op (idempotent).
  - `onItemDeletePopular` (trigger: `onDocumentDeleted("registries/{registryId}/items/{itemId}")`): same logic in reverse — remove `registryId` from `registryIds` via `arrayRemove`, decrement `registryCount` via `increment(-1)`. If `registryCount` falls to 0, delete the `popularItems` doc.
- **D-19:** Item edits that change `originalUrl` are handled by an `onItemUpdatePopular` trigger (`onDocumentUpdated`) only when the URL field changed: treat as delete (old productId) + create (new productId). For other field changes (title, price, image), no counter update needed.
- **D-20:** `discoverPopular()` Callable queries `popularItems` ordered by `registryCount desc`, then `updatedAt desc` as tiebreaker. `limit(20)`. Returns response shape per spec: `{ products: [{ id, title, description, image_url, price, currency, retailer_url }] }`. Fields populated from `popularItems` doc: `id` = productId, `title` = doc.title, `description` = "" (Item schema has no description — empty string), `image_url` = doc.imageUrl ?? "", `price` = parseFloat(doc.price) if numeric else 0, `currency` = "RON", `retailer_url` = doc.canonicalUrl.
- **D-21:** In-memory L1 cache inside Function instance for `discoverPopular`: module-scope `let cache: { data, expiresAt } | null = null`; on call, return cached if `Date.now() < cache.expiresAt`, else query Firestore + refresh with `expiresAt = Date.now() + 3600000`. No Firestore L2 cache — `popularItems` IS the authoritative store and reads are fast.
- **D-22:** One-shot backfill script `functions/scripts/backfillPopularItems.ts` (same pattern as Phase 7's `seedStores.ts`). Scans existing items via `db.collectionGroup("items").get()`, normalizes URLs, aggregates by productId, writes `popularItems` docs in 500-doc batched writes. Idempotent (uses `set` with merge semantics). Run via `npx tsx functions/scripts/backfillPopularItems.ts` BEFORE deploying the onCreate/onDelete triggers (otherwise live triggers race with batched writes). Without this, `popularItems` is empty until enough new items accumulate post-deploy.

### Gemini Search Integration

- **D-23:** `discoverSearch({ query: string })` Callable. Validation:
  - `typeof query !== "string"` or `query.trim().length === 0` → throw `HttpsError("invalid-argument", "Query required")` (Callable equivalent of HTTP 400)
  - `query.length > 200` → throw `HttpsError("invalid-argument", "Query too long (max 200 chars)")`
- **D-24:** Cache key normalization: `normalizedQuery = query.toLowerCase().trim().replace(/\s+/g, " ")`. Use this as the Firestore doc ID (no SHA — short queries fit in doc-ID byte limit; URL-encode if path-segment chars present).
- **D-25:** Cache layer: `discoverCache/{normalizedQuery}` Firestore doc, shape: `{ query: string, normalizedQuery: string, results: object[], cachedAt: Timestamp }`. TTL: 30 days via Firestore TTL policy on `cachedAt` field. Stale-by-design — never refresh prices, accept the 30-day staleness.
- **D-26:** Flow: validate → rate-limit check → cache check (`discoverCache/{normalizedQuery}`) → if hit, return `{ products: cached.results, cached_at: cached.cachedAt.toDate().toISOString() }`. If miss → call Gemini → parse → store in cache → return.
- **D-27:** Gemini call: model `gemini-2.5-flash`, endpoint `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent`, tool `google_search` enabled in the `tools` field. API key from env var `GEMINI_API_KEY` via 2nd-gen `defineSecret("GEMINI_API_KEY")` (NOT `functions.config()` — deprecated in 2nd gen). NEVER exposed to Android. HTTP client: `node-fetch` or native `fetch` (Node 22 has built-in `fetch`).
- **D-28:** Retailer category lists live in `functions/src/discover/retailers.ts` as the exact verbatim object from the spec. Categories: `universal`, `birthday`, `wedding`, `housewarming`, `baby_shower`, `christmas`. Site-list selection at prompt-build time: heuristic occasion detection on the query (Romanian + English keyword match: "ziua de naștere"/"birthday" → birthday list; "nuntă"/"wedding" → wedding; "casă nouă"/"warming"/"mutare" → housewarming; "bebeluș"/"baby"/"shower" → baby_shower; "crăciun"/"christmas" → christmas; default → universal only). The chosen category's sites are concatenated with the universal list and embedded in the prompt as "prioritize results from: [comma-separated hosts]".
- **D-29:** Gemini prompt template (`functions/src/discover/promptTemplate.ts`):
  > System: "You are a product-discovery assistant for a Romanian gift-registry app. Search the Romanian web for products matching the user's query. Prioritize results from these Romanian retailers (highest priority first): {sitesList}. Return prices in RON (Romanian lei). Return ONLY a strict JSON array — no prose, no markdown code fences, no explanation. Schema per item: {title, description, image_url, price, currency, retailer_url, retailer_name}. Return between 5 and 15 items; if fewer than 5 confident matches exist, return what's available — never pad with low-quality matches. Drop items missing title, price, or retailer_url."
  >
  > User: "{user-query}"
- **D-30:** Defensive Gemini response parsing (`functions/src/discover/parseGeminiResponse.ts`):
  1. Strip markdown code fences if present (regex: `^\s*```(?:json)?\s*\n?` and `\n?\s*```\s*$`).
  2. `JSON.parse` inside try/catch.
  3. Verify result is array (else return `[]`).
  4. For each item: validate shape against schema (title, description, image_url, price, currency, retailer_url, retailer_name). Drop items missing `title` | `price` | `retailer_url`. Coerce types: `price` must be number (parseFloat if string, drop if NaN). `description` truncate to 200 chars. `image_url` default to empty string if missing.
  5. If entire response fails to parse: return `[]` and log `console.error("Gemini parse failed", { rawResponse, query })` to Cloud Logging.
  6. NEVER throw to client — graceful empty array on parse failure.
- **D-31:** Response shape from `discoverSearch` (Callable returns to client):
  ```
  { products: DiscoverProduct[], cached_at: string (ISO timestamp) }
  ```
  `cached_at` is the original `cachedAt` from the cache doc on hit, or fresh `new Date().toISOString()` on miss.

### Affiliate URL Behavior on Discover Taps (Intentional Spec Honoring)

- **D-32:** **Tapping a Discover card opens the raw `retailer_url` via `Intent(Intent.ACTION_VIEW, Uri.parse(retailer_url))` with NO affiliate transformation.** User explicitly chose this option after being shown the trade-off ("lose affiliate revenue on Discover taps"). Rationale: honor spec wording literally. The project's `AffiliateUrlTransformer` (Phase 3 D-05) is NOT invoked from Discover.
- **D-33:** Intent dispatch wrapped in try/catch for `ActivityNotFoundException` (no browser installed). On catch: show a Material3 Snackbar via `SnackbarHostState` with `R.string.discover_no_browser_toast` = "Could not open browser" (en) / "Nu s-a putut deschide browserul" (ro).

### Android UI (DiscoverScreen)

- **D-34:** New package `app/src/main/java/com/giftregistry/ui/discover/`. Files: `DiscoverScreen.kt` (Compose), `DiscoverViewModel.kt`, `DiscoverUiState.kt` (sealed UI states), `DiscoverProductCard.kt`.
- **D-35:** Layout: `Scaffold(topBar = …, snackbarHost = …)` with the Discover content in body. Top of body: `OutlinedTextField` with leading `Icons.Outlined.Search` icon, placeholder `R.string.discover_search_placeholder` = "Search for any product..." / "Caută orice produs...", `singleLine = true`, `keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)`, `keyboardActions = KeyboardActions(onSearch = { viewModel.search(query) })`. Body below = `LazyColumn` with two sections.
- **D-36:** Section ordering rule: "From the web" appears ONLY after the user has submitted a search (hidden in idle state). "From the community" is always visible. When both are present, web section is on top, community below. Section headers use `GiftMaisonTypography.monoCaps`:
  - `R.string.discover_section_web` = "FROM THE WEB" / "DE PE WEB"
  - `R.string.discover_section_community` = "FROM THE COMMUNITY" / "DIN COMUNITATE"
- **D-37:** `DiscoverProductCard` composable: Material3 `Card`, full-width, vertical layout. Top: `AsyncImage` (Coil 3) from `imageUrl`, aspect ratio 16:9, `R.drawable.discover_card_placeholder` as `placeholder` + `error` (neutral gradient + product-box glyph, matches GiftMaison style — Claude's discretion on exact look). Below: `Text(title, maxLines = 2, overflow = Ellipsis)`, `Text(description, maxLines = 2, overflow = Ellipsis, color = GiftMaisonColors.inkFaint)`, `Text(priceFormatted)`. Price formatted via `NumberFormat.getCurrencyInstance(Locale("ro", "RO"))` → "199,99 RON". On click: trigger Intent per D-32/D-33.
- **D-38:** `DiscoverViewModel` (Hilt) state:
  - `popular: StateFlow<PopularState>` — sealed: `Loading | Loaded(products) | Error(message)`
  - `searchQuery: StateFlow<String>` — current TextField value (two-way binding)
  - `search: StateFlow<SearchState>` — sealed: `Idle | Loading | Loaded(products) | Empty | Error(message)`
  - Loading state for popular = full-card shimmer skeletons (3-5 shimmer cards). Loading state for web = same skeleton style.
  - Error states render inline `Text(error) + Button(R.string.discover_retry)` that calls `viewModel.loadPopular()` or `viewModel.search(currentQuery)`.
  - `init {}` block in ViewModel calls `loadPopular()` immediately.
- **D-39:** `DiscoverRepository` interface (domain layer) in `app/src/main/java/com/giftregistry/domain/discover/DiscoverRepository.kt`:
  ```kotlin
  interface DiscoverRepository {
      suspend fun getPopular(): Result<List<DiscoverProduct>>
      suspend fun search(query: String): Result<List<DiscoverProduct>>
  }
  ```
  Domain model `DiscoverProduct(id, title, description, imageUrl, price, currency, retailerUrl)`.
- **D-40:** `DiscoverRepositoryImpl` in `data/discover/`. Calls `FirebaseFunctions.getInstance("europe-west3").getHttpsCallable("discoverPopular")` and `.getHttpsCallable("discoverSearch")`. Wraps in `runCatching`. Maps `HashMap`/`Map<String, Any>` response data to `DiscoverProduct` domain models. Hilt module `DiscoverModule.kt` binds `DiscoverRepository`.

### Localization

- **D-41:** All new strings under `discover_*` prefix (Phase 1 D-01 feature-namespace convention). Both `values/strings.xml` and `values-ro/strings.xml` updated in the SAME commit. Required keys:
  - `nav_discover_tab` ("DISCOVER" / "DESCOPERĂ")
  - `discover_search_placeholder` ("Search for any product..." / "Caută orice produs...")
  - `discover_section_web` ("FROM THE WEB" / "DE PE WEB")
  - `discover_section_community` ("FROM THE COMMUNITY" / "DIN COMUNITATE")
  - `discover_empty_search` ("No matches found. Try a different search." / "Niciun rezultat. Încearcă o altă căutare.")
  - `discover_empty_popular` ("Popular items will appear here once people add gifts." / "Articolele populare vor apărea aici pe măsură ce oamenii adaugă cadouri.")
  - `discover_error_load` ("Could not load. Try again." / "Nu s-a putut încărca. Încearcă din nou.")
  - `discover_error_search` ("Search failed. Try again." / "Căutarea a eșuat. Încearcă din nou.")
  - `discover_retry` ("Retry" / "Reîncearcă")
  - `discover_no_browser_toast` ("Could not open browser" / "Nu s-a putut deschide browserul")
- **D-42:** Existing `nav_stores_tab` and all `stores_*` keys DELETED in same commit (en + ro).

### Firestore Security Rules

- **D-43:** New rules in `firestore.rules`:
  - `match /popularItems/{productId}` — `allow read: if request.auth != null && request.auth.token.firebase.sign_in_provider != "anonymous"; allow write: if false;` (Functions Admin SDK bypasses rules)
  - `match /discoverCache/{normalizedQuery}` — `allow read, write: if false;` (server-only, no client access)
  - `match /discoverRateLimits/{uid}` — `allow read, write: if false;` (server-only, prevents tampering)
- **D-44:** Removed: `match /config/{configId}` rule (Stores doc no longer exists). Tests in `tests/rules/firestore.rules.test.ts` updated accordingly.

### Firestore TTL Configuration

- **D-45:** TTL policies configured at deploy time via Firebase CLI or console (NOT in rules — TTL is a collection-level config):
  - `discoverCache` collection, field `cachedAt`, TTL 30 days
  - `discoverRateLimits` collection, field `lastWriteAt`, TTL 7 days
  - These are configured via `gcloud firestore fields ttls update --collection-group=discoverCache --field=cachedAt` or equivalent CLI invocation. Document the exact commands in the plan.

### Cloud Functions Module Structure

- **D-46:** New folder `functions/src/discover/`:
  - `getPopular.ts` — Callable `discoverPopular`
  - `search.ts` — Callable `discoverSearch` (includes rate-limit + cache check + Gemini call)
  - `triggers.ts` — `onItemCreatePopular`, `onItemDeletePopular`, `onItemUpdatePopular`
  - `urlNormalization.ts` — pure function `normalizeUrl(url): { productId: string, canonicalUrl: string }`
  - `retailers.ts` — verbatim retailer category lists from spec, plus `selectSitesForQuery(query): string[]`
  - `geminiClient.ts` — wraps the HTTP call to Gemini, accepts (prompt, sites) returns raw response string
  - `parseGeminiResponse.ts` — defensive JSON parser per D-30
  - `promptTemplate.ts` — `buildPrompt(query, sites): { systemPrompt, userPrompt }`
- **D-47:** `functions/src/index.ts` exports updated: add `discoverPopular`, `discoverSearch`, `onItemCreatePopular`, `onItemDeletePopular`, `onItemUpdatePopular`. Remove nothing (Phase 7's `seedStores` was a script, not an export).

### Testing

- **D-48:** Backend unit tests (`functions/__tests__/discover/`):
  - `parseGeminiResponse.test.ts`: valid JSON array; JSON wrapped in ```json fences; JSON wrapped in plain ``` fences; malformed JSON (returns `[]` + logs); missing required fields (drops bad items, keeps valid); price as string vs number; description over 200 chars (truncated).
  - `urlNormalization.test.ts`: same product via different utm params → same productId; query param order independence; trailing slash insensitivity; fragment stripping; host case insensitivity; http vs https forced to https; EMAG affiliate suffix stripped.
  - `cacheKeyNormalization.test.ts`: lowercase + trim + collapse whitespace; non-ASCII (Romanian diacritics) preserved.
  - `rateLimiting.test.ts`: under-limit allows; at-limit rejects; expired timestamps (> 1hr old) cleaned out.
- **D-49:** Android tests:
  - `DiscoverViewModelTest`: idle → loadPopular() → loaded; loadPopular() → error → retry → loaded; search() from idle → loading → loaded; search() → loading → error → retry → loaded; search() → loading → empty.
  - `DiscoverRepositoryTest`: mock `FirebaseFunctions.getHttpsCallable`; valid response data → domain mapping; Callable failure → Result.failure propagation; empty `products` array → empty list (not error).
- **D-50:** Composable / UI tests NOT required (Phase 7 D-20 precedent — ViewModel StateFlow assertions suffice; visual verification via emulator + style guide preview).
- **D-51:** Add a `DiscoverPreview` section to `StyleGuidePreview.kt` (Phase 8 / 9 convention) showing: idle state, loading state, loaded state with 3 cards, empty search state, error state.

### Claude's Discretion

- Exact shimmer/skeleton component — use Material3 placeholder modifier, custom shimmer brush, or Compose's `Modifier.placeholder` from accompanist if added
- `R.drawable.discover_card_placeholder` exact visual (neutral gradient + product-box glyph in GiftMaison style)
- Snackbar positioning + timing for "no browser" toast (Material3 defaults fine)
- Exact denormalized field shape inside `popularItems` doc (current spec covers required fields; planner can add helpful denormalized fields)
- Firestore composite index needed for `popularItems orderBy(registryCount desc, updatedAt desc)` — yes, composite index required; declare in `firestore.indexes.json`
- Empty state for popular section when `popularItems` is empty post-backfill (use `discover_empty_popular` string + same friendly empty UI)
- Whether occasion detection uses simple keyword matching or regex word-boundary matching — keyword sufficient for v1
- Locale handling for the Gemini prompt when user query is Romanian vs English — Gemini handles multilingual queries natively; pass query through verbatim
- Whether `discoverSearch` results are cached on response failure (e.g., empty parse result) — no, only cache successful non-empty results to avoid permanently caching transient failures
- Test framework: keep Vitest/Jest convention from existing `functions/__tests__/`; keep JUnit 4 + Truth + Turbine on Android per existing Phase 2-7 pattern

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project constraints + tech stack
- `CLAUDE.md` — Firebase-only constraint, Kotlin/Compose/Hilt/KSP/Nav3 lock, Cloud Functions 2nd gen Node 22 TypeScript, Coil 3, no Retrofit
- `.planning/PROJECT.md` — Affiliate-commission revenue model, out-of-scope items (no SQLite), Romanian market focus
- `.planning/ROADMAP.md` §"Phase 17" — current phase entry

### Item schema + storage
- `app/src/main/java/com/giftregistry/domain/model/Item.kt` — Item domain model (title, originalUrl, affiliateUrl, imageUrl, price as String, no description, no currency)
- `.planning/phases/03-registry-item-management/03-CONTEXT.md` §"Firestore Data Access Layer" — Items at `registries/{id}/items/{itemId}` subcollection (D-01), affiliate transform at add-time (D-05), Firestore listener pattern (D-02)
- `firestore.rules` — Current rules to extend for `popularItems`, `discoverCache`, `discoverRateLimits`; current rule for `config/stores` to remove
- `tests/rules/firestore.rules.test.ts` — 12+ existing rule tests; update for new + removed collections

### Bottom nav contract (where Discover replaces Stores)
- `app/src/main/java/com/giftregistry/ui/common/chrome/GiftMaisonBottomNav.kt` — 5-slot nav, slot 2 = Stores currently, must be rewired
- `app/src/main/java/com/giftregistry/ui/common/chrome/NavVisibility.kt` — nav visibility predicate; `DiscoverKey` must be in shows-nav list, `StoreListKey`/`StoreBrowserKey` removed
- `app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt` — entry-provider map; add DiscoverKey, remove StoreListKey + StoreBrowserKey entries
- `app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt` — nav key declarations; add `DiscoverKey`, delete `StoreListKey` + `StoreBrowserKey`
- `.planning/phases/09-shared-chrome-status-ui/09-CONTEXT.md` §"Navigation restructure" + §"FAB Add-action bottom sheet" — bottom nav contract (CHROME-01) + FAB sheet rows including "Browse stores" (D-09) to remove

### Stores capability (being decommissioned)
- `.planning/phases/07-romanian-store-browser/07-CONTEXT.md` — entire Stores feature surface: code locations, Firestore doc, drawables, seed script — all to be deleted per D-03 through D-09
- `app/src/main/java/com/giftregistry/ui/store/list/StoreListScreen.kt` — delete
- `app/src/main/java/com/giftregistry/ui/store/browser/` (whole folder) — delete
- `app/src/main/java/com/giftregistry/di/StoresModule.kt` — delete
- `app/src/main/java/com/giftregistry/data/preferences/LastRegistryPreferencesDataStore.kt` — delete (used only by store browser)
- `app/src/main/res/drawable-nodpi/store_*.webp` (9 files) — delete
- `functions/scripts/seedStores.ts` + `functions/data/stores.seed.json` — delete

### Cloud Functions pattern + region
- `functions/src/index.ts` — exports + Functions 2nd-gen pattern, region setup
- `functions/src/registry/fetchOgMetadata.ts` — existing onCall Callable to mirror for `discoverPopular`/`discoverSearch`
- `functions/src/registry/inviteToRegistry.ts` — auth-context pattern (`request.auth` checks) to mirror
- `functions/src/reservation/createReservation.ts` — Firestore transaction pattern (relevant for atomic rate-limit update)

### Localization (Phase 1 conventions)
- `app/src/main/res/values/strings.xml` — add `discover_*` and `nav_discover_tab`, remove `stores_*` + `nav_stores_tab`
- `app/src/main/res/values-ro/strings.xml` — same, in Romanian
- `.planning/phases/01-firebase-foundation/01-CONTEXT.md` §"Localization" — feature-namespaced key prefix convention (D-01)

### Coil + image loading pattern (precedent: Phase 12 cover photos)
- `.planning/phases/12-registry-cover-photo-themed-placeholder/12-CONTEXT.md` — AsyncImage placeholder + error fallback pattern

### Style guide harness
- `app/src/main/java/com/giftregistry/ui/theme/preview/StyleGuidePreview.kt` — appended each phase; Discover should add a `DiscoverPreview` section

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **Firebase Callable invocation pattern** — every Function in the project is a Callable; mature mature wrapper around `FirebaseFunctions.getInstance("europe-west3").getHttpsCallable(name).call(payload)`. No new HTTP plumbing needed on Android.
- **`GiftMaisonTheme`** — typography (monoCaps for section headers), colors (paper, ink, inkFaint, accent, accentSoft, line), shapes (pill for buttons, radii for cards) — all production-ready.
- **Coil 3 `AsyncImage`** — Phase 12 established the pattern for placeholder + error fallback; Discover reuses with `discover_card_placeholder` drawable.
- **`callbackFlow` listener pattern** — NOT used here (Discover is request/response, not realtime). Mentioned to avoid the planner reaching for it.
- **`runCatching` for Firebase calls** — established pattern in `AuthRepositoryImpl`, `RegistryRepositoryImpl`, etc.; mirror in `DiscoverRepositoryImpl`.
- **Phase 7 `seedStores.ts`** — template for `backfillPopularItems.ts` one-shot script (idempotent Admin SDK batched writes).
- **`hydrateActiveReservation` Callable** — example of a Callable that reads Firestore + returns shaped data; structurally similar to `discoverPopular`.
- **`StyleGuidePreview.kt`** — debug harness; append `DiscoverPreview` section per existing per-phase convention.

### Established Patterns
- **3-layer clean architecture** — domain interfaces (`domain/discover/DiscoverRepository.kt`), data impls (`data/discover/DiscoverRepositoryImpl.kt`), Hilt module (`di/DiscoverModule.kt`).
- **KSP for Hilt annotation processing** — NOT KAPT (Phase 2 D-01 lock).
- **`StateFlow` + `collectAsStateWithLifecycle()`** — universal ViewModel↔Compose pattern.
- **Feature-namespaced string keys with en + ro from first commit** — Phase 1 D-01 lock.
- **Cloud Functions on `europe-west3`** — all 2nd-gen Functions in the project use this region.
- **Cloud Functions organized by feature folder** — `functions/src/registry/`, `reservation/`, `notifications/`, `email/`, `config/` — Discover adds `functions/src/discover/`.
- **Firestore TTL policies configured via gcloud CLI**, NOT in rules — out-of-band collection-level config.
- **Sealed UI state types** in Compose — established by Phase 2-7 ViewModels (Loading | Success | Error).
- **Idempotent seed/backfill scripts** — Phase 7 `seedStores.ts` is the precedent; backfill must use `set` not `add`.

### Integration Points
- **`GiftMaisonBottomNav.kt`** slot 2 enum value rename, icon swap, callback rename, label key swap
- **`AppNavigation.kt`** — add `entry<DiscoverKey> { … }` block, remove StoreListKey + StoreBrowserKey blocks, update nav-callback wiring (`onStores` → `onDiscover`, pushes `DiscoverKey`)
- **`NavVisibility.kt`** — add `DiscoverKey` to shows-nav predicate, remove Stores keys
- **`AppNavKeys.kt`** — add `@Serializable data object DiscoverKey`, delete `StoreListKey` + `StoreBrowserKey`
- **`strings.xml` + `values-ro/strings.xml`** — add `discover_*` (10 keys + `nav_discover_tab`), delete `stores_*` and `nav_stores_tab`
- **`firestore.rules`** — add rules for `popularItems`, `discoverCache`, `discoverRateLimits`; remove `config/stores` rule
- **`tests/rules/firestore.rules.test.ts`** — add tests for new collections, remove tests for `config/stores`
- **`functions/src/index.ts`** — export `discoverPopular`, `discoverSearch`, `onItemCreatePopular`, `onItemDeletePopular`, `onItemUpdatePopular`
- **`functions/src/discover/` (new)** — entire feature module
- **`functions/scripts/backfillPopularItems.ts` (new)** — one-shot script, idempotent
- **`AddActionSheet` (Phase 9, in `ui/common/chrome/` or wherever the FAB sheet lives)** — remove "Browse stores" row entry
- **`StyleGuidePreview.kt`** — append `DiscoverPreview` composable
- **`gradle/libs.versions.toml` + `app/build.gradle.kts`** — verify Firebase Functions SDK dependency present (probably already there from Phase 3 `fetchOgMetadata`); no Retrofit/OkHttp additions needed

</code_context>

<specifics>
## Specific Ideas

- **Discover icon:** `Icons.Outlined.Search` (matches spec hint). Alternative `Icons.Default.Explore` was offered in the user spec; `Search` reads more directly as "find products" given the prominent search field.
- **Romanian price formatting:** `NumberFormat.getCurrencyInstance(Locale("ro", "RO")).format(price.toDouble())` → "199,99 RON". User's spec is explicit about Romanian locale ("199,99 RON" decimal comma).
- **Gemini retailer category constants:** stored verbatim per spec at `functions/src/discover/retailers.ts`:
  ```typescript
  export const RETAILERS = {
    universal: ["emag.ro", "altex.ro", "carrefour.ro", "vivre.eu", "elefant.ro", "flanco.ro"],
    birthday: ["mindblower.ro", "funfox.ro", "borealy.ro", "douglas.ro", "sephora.ro", "libris.ro", "carturesti.ro"],
    wedding: ["23h.ro", "crisiashop.ro", "wedday.ro", "happycards.ro", "magazinulmireselor.ro"],
    housewarming: ["jysk.ro", "mobexpert.ro", "ikea.com/ro", "dedeman.ro", "leroymerlin.ro", "vivre.eu", "insignis.ro", "kika.ro", "somproduct.ro"],
    baby_shower: ["bekid.ro", "babyneeds.ro", "bebelul.ro", "bebebliss.ro", "bebenou.ro", "chicco.ro", "erfi.ro", "babymatters.ro", "noriel.ro"],
    christmas: ["borealy.ro", "mindblower.ro", "funfox.ro", "gourmetgift.ro", "douglas.ro", "sephora.ro", "kaufland.ro", "lidl.ro"]
  };
  ```
- **Section ordering (web on top after search):** matches the user mental model of "I just searched, show me what I asked for first."
- **Verbatim spec constants preserved:** 20 calls/hour rate limit, 200-char max query, 5-15 result range from Gemini, 30-day cache TTL, top-20 popular default, `gemini-2.5-flash` model.

</specifics>

<deferred>
## Deferred Ideas

- **Analytics events** — user explicitly out-of-scope ("I'll add those later")
- **"Add to my registry" action from Discover cards** — user explicitly out-of-scope; Discover taps only open the URL
- **Infinite scroll / pagination on either section** — user explicitly out-of-scope; fixed list sizes
- **Per-user personalization (popular ranked per user)** — user explicitly out-of-scope; global popular for everyone
- **Time-windowed popularity (last 7 days, last month)** — user explicit: "all-time popular for now"
- **Price-refresh logic on cached search results** — user explicit: 30-day staleness accepted
- **Affiliate URL transformation on Discover taps** — user explicit decision to honor spec, accept revenue loss (D-32). Future revisit: add a `affiliateUrl` field to popular response, transform server-side from `popularItems.canonicalUrl` via TS port of `AffiliateUrlTransformer`, return both `retailer_url` (raw, for display) and `affiliate_url` (for Intent dispatch). Estimated effort: 1 plan, ~80 LOC.
- **Web fallback variant of Discover** — spec doesn't mention; v1 Android-only
- **Hallucinated-URL validation on Gemini results** — Gemini's `google_search` grounding tool typically returns real URLs, but no HEAD request validation is performed before responding. If users start reporting 404s, add a HEAD-check pass with a parallel `Promise.all` capped at 200ms per URL. Acceptable risk for v1.
- **Image hotlink-protection fallback for retailers** — Romanian retailers (Altex, Flanco) sometimes block hotlinking. Coil placeholder covers this UX-wise. Server-side image proxying via Firebase Storage is deferred — would require new Storage rules + a new Function.
- **Backfill orchestration safety** — running `backfillPopularItems.ts` while triggers are deployed can double-count if a triggered write fires mid-scan. Mitigation: deploy backfill script FIRST (triggers OFF), run it, THEN deploy triggers. Document this in execution plan.
- **Hint at Gemini free-tier project quota exhaustion** — per-user 20/hr rate limit caps abuse, but if the user base grows the project-level Gemini quota could exhaust before any individual user hits 20/hr. Add Cloud Logging alert on Gemini 429 responses. Deferred to ops.
- **Empty state copy variants** — different empty for "popular has nothing yet" vs "your search returned nothing" — already split into `discover_empty_popular` and `discover_empty_search`.
- **Phase 7 Stores capability** — fully decommissioned per user's instruction; no rollback path planned.

</deferred>

---

*Phase: 17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini*
*Context gathered: 2026-05-27*
