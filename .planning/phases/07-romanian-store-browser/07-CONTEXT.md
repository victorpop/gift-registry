# Phase 7: Romanian Store Browser - Context

**Gathered:** 2026-04-19
**Status:** Ready for planning

<domain>
## Phase Boundary

Registry owners can browse a curated list of popular Romanian retailers inside the Android app and funnel products from those stores into the existing URL-based add-item flow via an in-app WebView with a persistent bottom "Add to list" CTA.

This phase replaces the retired EMAG Catalog Integration scope (ITEM-03/ITEM-04) after confirming no public EMAG product catalog API exists. Covers STORE-01, STORE-02, STORE-03, STORE-04.

**In scope:**
- New "Browse stores" entry point and store-list screen
- In-app WebView screen with persistent bottom CTA
- Add-to-list sheet pre-fill from WebView URL (reuses existing Phase 3 add-item + affiliate + OG fetch pipelines)
- Registry picker in the sheet for users with multiple registries

**Out of scope:**
- Web fallback variant (owners are Android-only per PROJECT.md)
- Store usage analytics / personalized ordering
- Admin UI for managing the store list (edits happen directly in Firestore)
- Deep product search within stores (tap through to retailer search UI)
- New affiliate programs beyond what AffiliateUrlTransformer already handles

</domain>

<decisions>
## Implementation Decisions

### Store List — Composition & Source
- **D-01 (Claude's Discretion on Q1):** Ship 8 curated Romanian retailers covering the main gift categories:
  1. **eMAG** (https://www.emag.ro) — general merchandise, #1 RO retailer
  2. **Altex** (https://altex.ro) — electronics, appliances
  3. **Flanco** (https://www.flanco.ro) — electronics, appliances
  4. **Libris** (https://www.libris.ro) — books
  5. **Cărturești** (https://carturesti.ro) — books, gifts, stationery
  6. **IKEA Romania** (https://www.ikea.com/ro/ro/) — home & furniture
  7. **Dedeman** (https://www.dedeman.ro) — home improvement
  8. **Elefant** (https://www.elefant.ro) — books, toys, kids
- **D-02:** Store list stored in Firestore at `config/stores` doc, shape: `{ stores: [{ id, name, homepageUrl, displayOrder, logoAsset }] }`. Editable without Play Store release; Android caches via Firestore offline persistence
- **D-03:** Store logos bundled in `app/src/main/res/drawable-nodpi/` (filenames like `store_emag.webp`, `store_altex.webp`, etc.); Firestore doc references them by `logoAsset: "store_emag"` and Android resolves via `context.resources.getIdentifier(...)` — falls back to a generic `store_generic.webp` if identifier not found
- **D-04:** Store order controlled by `displayOrder: int` field (ascending); the 8 stores seeded with 10, 20, 30... leaving room for future inserts

### WebView Behavior
- **D-05:** JavaScript enabled (`webSettings.javaScriptEnabled = true`) — modern retailer sites require JS; without it search/listing breaks
- **D-06:** Cookies persist across sessions using `CookieManager` default behavior — store's login/cart state survives between visits; reduces user friction
- **D-07:** Current URL captured via `WebViewClient.onPageFinished(view, url)` callback — pushes URL into a ViewModel `StateFlow<String>`; the "Add to list" button reads the latest URL on tap (no JavaScript bridge needed)
- **D-08:** External URL schemes (non-http/https: `mailto:`, `tel:`, `intent:`, app-store deep links) blocked by returning `true` from `shouldOverrideUrlLoading`; user sees a snackbar "Link requires external app" — keeps user in the store-browser flow

### Add-to-List Flow & Registry Selection
- **D-09:** "Add to list" tap opens the **existing Phase 3 add-item bottom sheet** pre-filled with `webView.url`; the existing `fetchOgMetadata` Cloud Function runs on the URL to auto-fill title/image/price; user can edit fields before confirming
- **D-10:** Registry picker shown at top of the add-item sheet when user has ≥2 registries; hidden when user has exactly 1 registry (auto-selects); remembers last selection in DataStore via new `LastRegistryPreferencesDataStore`
- **D-11:** Launch entry points (two):
  1. **Home screen FAB menu** — extended FAB opens a menu with "Create registry" (existing) + new "Browse stores" action
  2. **Registry Detail add-item flow** — existing "Add item" entry adds a third option alongside "Paste URL" / "Manual entry": "Browse stores" (opens the store list with current registry pre-selected so the picker is skipped)
- **D-12:** Bottom sheet overlays the WebView — closing the sheet returns to the same page at the same scroll position; device back button dismisses sheet first then exits browser; this means the user does NOT lose their browsing state when adding an item

### Affiliate Integration
- **D-13:** Existing `AffiliateUrlTransformer` (Phase 3) handles URL tagging — this phase does NOT introduce new merchant rules. EMAG URLs already transform via AFF-02; other retailers pass through per AFF-04 (logged, no affiliate). Future MERCH-01 (v2) may add transformers for the other 7 stores
- **D-14:** No affiliate-aware rewriting of the WebView URL itself during browsing — browsing is organic. Transform happens only at add-time, consistent with Phase 3 behavior

### Error Handling & Edge Cases
- **D-15:** WebView load failure (offline, 500, DNS fail) shows an error state with "Retry" and "Back" buttons; the "Add to list" button is disabled until a successful page load (tracked via the same `StateFlow<String>` — empty string when load failed)
- **D-16:** Network lost mid-browsing: WebView's native behavior shows the last-rendered page with stale indicator; Add button remains enabled (URL is still valid) — adding works from cache via existing fetchOgMetadata retry logic
- **D-17:** Store list load failure: if Firestore `config/stores` is empty/missing, show error state ("Could not load stores — try again") with retry; no hardcoded fallback list (keeps the Firestore config as the single source of truth — ops can fix by seeding the doc)

### Localization
- **D-18:** New strings added under `stores_*` prefix (feature-namespaced per Phase 1 D-01 convention) in `strings.xml` + `values-ro/strings.xml`: entry point label, screen title, error copy, CTA label. Store names themselves are NOT translated (proper nouns — displayed as-is from Firestore)

### Testing Strategy
- **D-19:** Unit tests for `StoreRepositoryImpl` (Firestore read via in-memory fake), `StoreListViewModel` (loading/success/error states), `StoreBrowserViewModel` (URL state flow transitions), `LastRegistryPreferencesDataStore` (persistence)
- **D-20:** UI / composable tests not required — follow Phase 2 pattern (ViewModels tested with StateFlow assertions; composables verified manually via emulator). Reasoning: WebView itself is framework-provided; our orchestration logic lives entirely in ViewModel
- **D-21:** Firestore security rules: `config/stores` is world-readable (app needs it during onboarding with any auth state), owner-writable only via admin/manual ops; extend `firestore.rules` + rules test

### Seed Data
- **D-22:** Include a one-shot Firebase CLI / Admin SDK seed script (`functions/scripts/seedStores.ts` or similar) run once during deploy to populate `config/stores` with the 8 curated entries — avoids manual Firestore console editing. Script is idempotent (uses `set` not `add`)

</decisions>

<code_context>
## Existing Code Insights

### Reusable Assets
- `AffiliateUrlTransformer` (Phase 3) — transforms URLs at add-time; no changes needed
- `fetchOgMetadata` Cloud Function (Phase 3) — auto-fills title/image/price from a URL; called from add-item sheet
- Existing **add-item bottom sheet** in `app/src/main/java/com/giftregistry/ui/registry/detail/` — pre-filled with URL; this phase pipes the WebView URL into it
- `RegistryRepository.observeRegistries(ownerId)` (Phase 3) — live list for the picker
- `LanguagePreferencesDataStore` / `GuestPreferencesDataStore` — pattern to mirror for `LastRegistryPreferencesDataStore`
- Material3 Compose + existing theme (seed color #6750A4) — store-list screen follows existing list-screen patterns
- Firestore offline persistence already enabled — `config/stores` cached transparently

### Established Patterns
- Clean architecture: domain interfaces, data impls, use cases, Hilt DI
- `callbackFlow` + `awaitClose` for Firestore listeners (Phase 2 auth, Phase 3 items)
- ViewModels expose `StateFlow<UiState>` with sealed UI-state types (loading/success/error)
- Feature-namespaced string keys with en/ro variants from the first commit of the phase
- `R.string.*` for all user-facing copy — never hardcoded in composables
- No Firebase imports in domain layer
- Cloud Functions on `europe-west3`; Firestore rules tested via `tests/rules` harness

### Integration Points
- **Navigation3:** New routes `StoreListKey`, `StoreBrowserKey(storeId: String, registryId: String?)`; Home → StoreList; StoreList → StoreBrowser; Browser → add-item sheet (inline overlay)
- **Hilt / DI:** New module `StoresModule` binds `StoreRepository`, `LastRegistryPreferencesDataStore`
- **Home screen:** Extended FAB with menu — requires small refactor of existing FAB (currently single-action "Create registry")
- **Registry Detail:** Existing add-item entry gets a third option — small menu extension
- **Add-item sheet:** Accept an optional `initialUrl` and `initialRegistryId` param; existing code already accepts URL for URL-paste flow
- **Firestore rules:** Add `match /config/{configId} { allow read: if true; allow write: if false; }` — client-read-only, admin-writes via seed script
- **Manifest:** WebView permissions already granted (INTERNET); no new permissions needed

</code_context>

<specifics>
## Specific Ideas

- Home screen FAB becomes `ExtendedFloatingActionButton` → opens small menu (MaterialComponents pattern) with "Create registry" / "Browse stores" actions
- Store-list screen is a `LazyVerticalGrid` of 2-column cards (logo + store name); 64dp logo, centered, tap-target 100dp card
- WebView screen is a `Scaffold` with `topBar` (title = store name, close button) and `bottomBar` (persistent "Add to list" CTA, full-width primary button 56dp per Material3)
- On sheet open from WebView: URL + registryId (if pre-selected) pass into sheet's ViewModel; sheet re-uses existing composable
- Seed script reads a JSON file `functions/data/stores.seed.json` and writes to `config/stores` — easy to update list without touching code
- `store_generic.webp` fallback logo: grey placeholder with a storefront icon
- Romanian string copy: "Răsfoiește magazine" (Browse stores), "Adaugă la listă" (Add to list), "Retrimite încercarea" (Retry)

</specifics>

<deferred>
## Deferred Ideas

- Admin UI for managing stores in-app — ops edits Firestore directly or runs seed script
- Per-store featured products / curated picks — would require hand-curation; not core value
- Usage analytics / most-tapped stores — no analytics pipeline yet
- Smart store suggestions based on registry occasion type — nice-to-have, v2
- Additional affiliate transformers for Altex/Flanco/Libris/Cărturești/IKEA/Dedeman/Elefant — covered by MERCH-01 in v2 REQUIREMENTS.md
- Desktop/web variant of store browser — Web fallback is giver-only (PROJECT.md constraint)
- Deep product search within a store via API — contradicts the whole premise of this rescope (no reliable catalog APIs)
- Saving browsing history within a store — simple back button behavior is enough for v1
- Login-wall detection / warnings — user sees the wall natively via WebView; no custom handling

</deferred>
