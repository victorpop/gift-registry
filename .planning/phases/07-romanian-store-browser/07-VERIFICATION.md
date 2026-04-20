---
phase: 07-romanian-store-browser
verified: 2026-04-20T06:00:00Z
status: human_needed
score: 8/8 must-haves verified (automated); human UAT pending
re_verification: false
human_verification:
  - test: "STORE-01 — Home FAB menu expands with 'Browse stores' + 'Create registry'; Store List shows 8 stores in 2-col grid with logos and names; back works"
    expected: "Extended FAB expands menu; Store List renders LazyVerticalGrid with 8 entries; logos visible (placeholder WebPs in dev); back arrow returns to Home"
    why_human: "Compose UI rendering, AnimatedVisibility behavior, and logo display require a running device or emulator"
  - test: "STORE-01 — Registry Detail FAB opens DropdownMenu with 'Browse stores' option; navigates to Store List pre-selecting current registry"
    expected: "DropdownMenu appears with two items; 'Browse stores' pushes StoreListKey(preSelectedRegistryId = currentRegistryId)"
    why_human: "UI interaction and navigation flow require a running device"
  - test: "STORE-02 — Tapping a store card opens in-app WebView at store homepage with JS enabled"
    expected: "StoreBrowserScreen opens; store homepage loads with JS-rendered content (search bar, product tiles); TopAppBar shows store name; bottom 56dp Add-to-list button visible while scrolling"
    why_human: "WebView rendering and JS execution require a real device or emulator; network required"
  - test: "STORE-03 — Tapping 'Add to list' opens AddItemScreen pre-filled with current URL; OG fetch auto-triggers; save adds item with affiliate tag"
    expected: "AddItemScreen opens with URL pre-filled; title/image/price populate via FetchOgMetadataUseCase; save creates Firestore item with affiliateUrl populated"
    why_human: "End-to-end funnel spans WebView → AddItem sheet → OG fetch → Firestore write; requires network + real Firestore instance"
  - test: "STORE-04 — Airplane mode triggers error overlay; 'Add to list' disabled; retry restores WebView"
    expected: "WifiOff icon + error heading + body + Retry + Back shown; button greyed out; disabling airplane + Retry loads homepage and re-enables button"
    why_human: "Error state requires network failure simulation on a real device"
  - test: "D-08 external scheme blocking — tel:/mailto: links show Toast and do not open external app"
    expected: "Toast 'This link opens in an external app' shown; WebView stays on current page; external app does not launch"
    why_human: "Requires a store page with tel: or mailto: links and physical device interaction"
  - test: "D-06 cookie persistence — login/cookie state survives backing out and re-opening the same store"
    expected: "Cookie banner or login state from previous session remains when WebView is re-opened"
    why_human: "Cookie persistence requires multiple WebView sessions with real network"
  - test: "Romanian locale — all stores_* strings display in Romanian when device language is Romanian"
    expected: "FAB label 'Nou'; menu 'Răsfoiește magazine'; CTA 'Adaugă la listă'; errors in Romanian"
    why_human: "Locale switching requires changing device system language and visual inspection"
  - test: "FAB menu dismiss scrim — tapping outside the expanded FAB menu collapses it without navigating"
    expected: "Menu collapses; no navigation occurs; re-tapping FAB (now showing Close icon) also collapses"
    why_human: "Scrim dismiss requires UI interaction testing"
---

# Phase 7: Romanian Store Browser Verification Report

**Phase Goal:** Registry owners can browse a curated list of popular Romanian retailers, open any store in an in-app WebView, and add products to a registry via a persistent bottom "Add to list" CTA that pipes the current URL into the existing affiliate-tagging add-item flow
**Verified:** 2026-04-20T06:00:00Z
**Status:** human_needed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Owner can open "Browse stores" from Home screen and Registry Detail and see a curated list of popular Romanian retailers with logos and names | ✓ VERIFIED | `RegistryListScreen.kt` has `onNavigateToBrowseStores` + `AnimatedVisibility` menu with `ShoppingBag` icon; `RegistryDetailScreen.kt` has `DropdownMenu` with "Browse stores" entry; `StoreListScreen.kt` has `LazyVerticalGrid(GridCells.Fixed(2))`; 8 stores in seed JSON |
| 2 | Tapping a store opens an in-app WebView at the retailer's homepage with a persistent bottom "Add to list" bar | ✓ VERIFIED | `StoreBrowserScreen.kt` has `AndroidView(WebView)` in Scaffold with `bottomBar` 56dp Button; `javaScriptEnabled=true`, `domStorageEnabled=true`, `CookieManager.setAcceptCookie(true)` |
| 3 | Tapping "Add to list" opens the existing add-item sheet pre-filled with the current WebView URL; confirming adds the item with affiliate tag | ✓ VERIFIED | `onPageFinished` → `viewModel.onUrlChanged(url)`; `StoreBrowserScreen` dispatches `AddItemKey(registryId, initialUrl, initialRegistryId)` on tap; `AddItemViewModel` reads `initialUrl` from `SavedStateHandle` and calls `onFetchMetadata()` in `init`; `AffiliateUrlTransformer` already runs in `ItemRepositoryImpl.addItem` (unmodified Phase 3 pipeline) |
| 4 | If store page fails to load, WebView shows error state and "Add to list" is disabled; retry works without breaking nav stack | ✓ VERIFIED | `isForMainFrame` guard in `onReceivedError` calls `viewModel.onPageLoadFailed()`; `alpha(0f)` hides WebView; `WebViewErrorOverlay` shows `WifiOff` icon + Retry + Back; `enabled = addEnabled && viewModel.registryId != null` disables button; `LaunchedEffect(pageLoadFailed)` triggers `webViewRef?.reload()` on retry |
| 5 | Non-http/https URL schemes are blocked inside the WebView | ✓ VERIFIED | `shouldOverrideUrlLoading` checks `scheme != "http" && scheme != "https"` and returns `true`; Toast shown inside `view.post { }` |
| 6 | Domain layer (Store model, StoreRepository interface) has zero Firebase imports | ✓ VERIFIED | `grep "com.google.firebase"` returns no matches in `domain/model/Store.kt` or `domain/store/StoreRepository.kt` |
| 7 | Firestore `config/stores` is world-readable and client-write-denied | ✓ VERIFIED | `firestore.rules` has `match /config/{configId}` with `allow read: if true; allow write: if false;`; 4 rules tests exist in `firestore.rules.test.ts` |
| 8 | R8 keep rule prevents logo drawable stripping in release builds | ✓ VERIFIED | `app/proguard-rules.pro` contains `-keep class **.R$drawable { *; }` |

**Score:** 8/8 truths verified (automated)

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `functions/data/stores.seed.json` | 8 Romanian retailers in Firestore shape | ✓ VERIFIED | 8 entries, displayOrder 10–80, all have logoAsset |
| `functions/scripts/seedStores.ts` | Idempotent Admin SDK seed script | ✓ VERIFIED | Uses `readFileSync` + `.set()` pattern; `seed:stores` npm script present |
| `firestore.rules` | config/{configId} world-read + write-denied | ✓ VERIFIED | Block present with exact `allow read: if true; allow write: if false;` |
| `tests/rules/firestore.rules.test.ts` | 4 config/stores rule tests | ✓ VERIFIED | 4 tests: unauth read, auth read, unauth write denied, auth write denied |
| `app/proguard-rules.pro` | `-keep class **.R$drawable { *; }` | ✓ VERIFIED | Keep rule present |
| `app/src/main/res/values/strings.xml` | 14 stores_* English keys | ✓ VERIFIED | 14 matches including `stores_add_to_list_cta` |
| `app/src/main/res/values-ro/strings.xml` | 14 stores_* Romanian keys | ✓ VERIFIED | 14 matches including `Adaugă la listă` |
| `app/src/main/res/drawable-nodpi/store_generic.webp` | Fallback logo drawable | ✓ VERIFIED | All 9 WebP files present (placeholder 1x1 transparent) |
| `app/src/main/java/com/giftregistry/domain/model/Store.kt` | Store data class | ✓ VERIFIED | `data class Store(id, name, homepageUrl, displayOrder, logoAsset)` |
| `app/src/main/java/com/giftregistry/domain/store/StoreRepository.kt` | StoreRepository interface | ✓ VERIFIED | `interface StoreRepository { suspend fun getStores(): Result<List<Store>> }` |
| `app/src/main/java/com/giftregistry/data/store/StoreRepositoryImpl.kt` | Firestore-backed impl with runCatching + sortedBy | ✓ VERIFIED | `collection("config").document("stores").get().await()`, `runCatching`, `sortedBy { it.displayOrder }` |
| `app/src/main/java/com/giftregistry/data/preferences/LastRegistryPreferencesDataStore.kt` | DataStore with unique name | ✓ VERIFIED | `preferencesDataStore(name = "last_registry_prefs")` |
| `app/src/main/java/com/giftregistry/di/StoresModule.kt` | @Module with 2 @Binds | ✓ VERIFIED | 2 `@Binds` functions binding `StoreRepository` + `LastRegistryPreferencesRepository` |
| `app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt` | StoreListKey, StoreBrowserKey, AddItemKey extended | ✓ VERIFIED | `StoreListKey(preSelectedRegistryId: String? = null)`, `StoreBrowserKey(storeId, registryId: String?)`, `AddItemKey(registryId, initialUrl: String? = null, initialRegistryId: String? = null)` |
| `app/src/main/java/com/giftregistry/ui/store/list/StoreListScreen.kt` | Composable with LazyVerticalGrid + error state | ✓ VERIFIED | `LazyVerticalGrid(GridCells.Fixed(2))`, `CloudOff` error state |
| `app/src/main/java/com/giftregistry/ui/store/list/StoreListViewModel.kt` | HiltViewModel with StateFlow + retry | ✓ VERIFIED | `@HiltViewModel`, `StateFlow<StoreListUiState>`, `loadStores()` retry |
| `app/src/main/java/com/giftregistry/ui/store/list/StoreListUiState.kt` | Sealed interface Loading/Success/Error | ✓ VERIFIED | `sealed interface StoreListUiState` with 3 states |
| `app/src/main/java/com/giftregistry/ui/store/list/StoreLogoResolver.kt` | getIdentifier wrapper | ✓ VERIFIED | `context.resources.getIdentifier(...)` with `store_generic` fallback |
| `app/src/main/java/com/giftregistry/ui/store/browser/StoreBrowserScreen.kt` | Scaffold + AndroidView(WebView) + bottom bar + error overlay | ✓ VERIFIED | `AndroidView`, `javaScriptEnabled=true`, `CookieManager`, `isForMainFrame`, `onPageFinished`, `shouldOverrideUrlLoading`, `WifiOff` error overlay, `alpha(0f)` pattern |
| `app/src/main/java/com/giftregistry/ui/store/browser/StoreBrowserViewModel.kt` | 5 StateFlows + addToListEnabled | ✓ VERIFIED | `currentUrl`, `pageLoadFailed`, `storeName`, `homepageUrl`, `addToListEnabled` all present |
| `app/src/main/java/com/giftregistry/ui/item/add/AddItemViewModel.kt` | Reads initialUrl; auto-triggers onFetchMetadata | ✓ VERIFIED | `savedStateHandle["initialUrl"]`; `init { if (initialUrl.isNotBlank()) { url.value = initialUrl; onFetchMetadata() } }` |
| `app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt` | Accepts initialUrl + initialRegistryId | ✓ VERIFIED | Signature updated; passed via `hiltViewModelWithNavArgs` |
| `app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt` | entry<StoreListKey>, entry<StoreBrowserKey>, updated entry<AddItemKey> | ✓ VERIFIED | All 3 entries present; Home→StoreList (null registry); RegistryDetail→StoreList (preSelectedRegistryId); AddItemKey unpacks `initialUrl` + `initialRegistryId` |
| `app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt` | FAB refactored with Browse stores entry | ✓ VERIFIED | `onNavigateToBrowseStores` callback; `menuExpanded`; `AnimatedVisibility`; `ShoppingBag` icon |
| `app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt` | DropdownMenu with Browse stores entry | ✓ VERIFIED | `onNavigateToBrowseStores: () -> Unit = {}`; `DropdownMenuItem` wired |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|----|--------|---------|
| `RegistryListScreen.kt` | `backStack.add(StoreListKey)` | `onNavigateToBrowseStores` callback | ✓ WIRED | AppNavigation passes `onNavigateToBrowseStores = { backStack.add(StoreListKey(preSelectedRegistryId = null)) }` |
| `RegistryDetailScreen.kt` | `backStack.add(StoreListKey(preSelectedRegistryId = key.registryId))` | `onNavigateToBrowseStores` callback | ✓ WIRED | AppNavigation passes `onNavigateToBrowseStores = { backStack.add(StoreListKey(preSelectedRegistryId = key.registryId)) }` |
| `StoreListScreen.kt` | `StoreBrowserKey(storeId, registryId)` | `onStoreSelected` → `AppNavigation` | ✓ WIRED | `entry<StoreListKey>` passes `onStoreSelected = { storeId -> backStack.add(StoreBrowserKey(storeId, key.preSelectedRegistryId)) }` |
| `StoreBrowserScreen.kt` | `AddItemKey(registryId, initialUrl, initialRegistryId)` | `onAddToList` button → AppNavigation | ✓ WIRED | `entry<StoreBrowserKey>` dispatches `backStack.add(AddItemKey(registryId = target, initialUrl = url, initialRegistryId = target))` |
| `StoreBrowserScreen.kt` | `StoreBrowserViewModel.currentUrl` | `WebViewClient.onPageFinished → viewModel.onUrlChanged(url)` | ✓ WIRED | `onPageFinished` callback wired directly to `viewModel.onUrlChanged(url)` |
| `StoreBrowserScreen.kt` | `StoreBrowserViewModel.onPageLoadFailed` | `WebViewClient.onReceivedError(isForMainFrame)` | ✓ WIRED | `if (request.isForMainFrame) { viewModel.onPageLoadFailed() }` |
| `AddItemViewModel.kt` | `FetchOgMetadataUseCase` | `init block calls onFetchMetadata() when initialUrl non-blank` | ✓ WIRED | `if (initialUrl.isNotBlank()) { url.value = initialUrl; onFetchMetadata() }` |
| `StoreRepositoryImpl.kt` | Firestore `config/stores` | `firestore.collection("config").document("stores").get().await()` | ✓ WIRED | Confirmed present in implementation |
| `functions/scripts/seedStores.ts` | `functions/data/stores.seed.json` | `fs.readFileSync` | ✓ WIRED | `const jsonPath = path.resolve(__dirname, "../data/stores.seed.json")` |
| `StoresModule.kt` | `StoreRepositoryImpl` + `LastRegistryPreferencesDataStore` | `@Binds abstract functions` | ✓ WIRED | 2 `@Binds` present |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|--------------------|--------|
| `StoreListScreen.kt` | `uiState: StoreListUiState` | `StoreListViewModel.uiState` ← `GetStoresUseCase` ← `StoreRepositoryImpl.getStores()` ← `firestore.collection("config").document("stores").get()` | Yes — live Firestore one-shot read | ✓ FLOWING |
| `StoreBrowserScreen.kt` | `storeName`, `homepageUrl` | `StoreBrowserViewModel` ← `GetStoresUseCase` ← `StoreRepositoryImpl` | Yes — same pipeline | ✓ FLOWING |
| `StoreBrowserScreen.kt` | `currentUrl` | `onPageFinished` → `viewModel.onUrlChanged(url)` | Yes — real URL from WebView callback | ✓ FLOWING |
| `AddItemScreen.kt` (via StoreBrowser flow) | `url` field | `AddItemViewModel.initialUrl` ← `SavedStateHandle["initialUrl"]` ← `AddItemKey.initialUrl` ← `StoreBrowserScreen.onAddToList(currentUrl, ...)` | Yes — real URL from WebView | ✓ FLOWING |

### Behavioral Spot-Checks

Step 7b: SKIPPED — verifying Android Compose UI and WebView behavior requires a running Android device or emulator. Unit tests serve as the programmatic proxy.

**Unit test coverage (as documented in SUMMARYs and verified by file existence):**

| Test Suite | Count | Coverage |
|-----------|-------|----------|
| `StoreRepositoryImplTest` | 4 | Sort by displayOrder, missing logoAsset fallback, Firestore failure, empty stores array |
| `LastRegistryPreferencesDataStoreTest` | 4 | Save/get round-trip, null before save, observe after set, clear resets to null |
| `StoreListViewModelTest` | 4 | Init emits Loading→Success, failure→Error, empty list→Error, retry transitions |
| `StoreBrowserViewModelTest` | 6 | Initial state, URL change enables CTA, load failure disables CTA, retry, store matching, unknown store |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|------------|------------|-------------|--------|----------|
| STORE-01 | 07-00, 07-01, 07-02 | Owner can open "Browse stores" and see curated list of Romanian retailers | ✓ SATISFIED | StoreListScreen + FAB refactors + nav wiring all present; 8 stores seeded |
| STORE-02 | 07-03 | Tapping a store opens retailer homepage in-app WebView | ✓ SATISFIED (automated) / ? HUMAN for rendering | StoreBrowserScreen with AndroidView(WebView) wired; human UAT needed to confirm actual rendering |
| STORE-03 | 07-03 | Persistent "Add to list" opens add-item sheet pre-filled with URL; affiliate tag applied | ✓ SATISFIED (automated) / ? HUMAN for end-to-end | AddItemKey extended; AddItemViewModel auto-triggers OG fetch; AffiliateUrlTransformer unchanged; human UAT needed |
| STORE-04 | 07-03 | Error state on load failure; "Add to list" disabled; retry works | ✓ SATISFIED (automated) / ? HUMAN for device test | isForMainFrame guard; alpha(0f) pattern; button disabled; retry LaunchedEffect — human UAT needed |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `app/src/main/res/drawable-nodpi/store_*.webp` | N/A | 9 placeholder 1x1 transparent WebP files | ⚠️ Warning | Store cards will show blank logos in all builds until real brand assets replace the placeholders. Accepted deviation — explicitly tracked in 07-00-SUMMARY.md. Not a code blocker. |
| `AddItemViewModel.kt` | ~line 28 | `// TODO(D-10 follow-up): When multi-registry picker ships...` | ℹ️ Info | `initialRegistryId` read but unused; infrastructure shipped. D-10 picker deferred to v2. Accepted deviation. |
| `StoreBrowserScreen.kt` | ~line 644 | Toast inside `shouldOverrideUrlLoading` instead of Snackbar | ℹ️ Info | MVP trade-off documented in 07-03-SUMMARY.md (D-08). Accepted. |
| `AppNavigation.kt` | StoreBrowserKey entry | `val target = registryId ?: return@StoreBrowserScreen` — "Add to list" silently disabled when entering Store Browser from Home FAB | ⚠️ Warning | Users entering from Home FAB cannot add items. Accepted D-10 deviation — documented in plan and summary. Single-registry users entering from Registry Detail are unaffected. |

**No blocker anti-patterns.** All flagged items are accepted deviations documented in plan and summary files.

### Human Verification Required

Tests A–H from `.planning/phases/07-romanian-store-browser/07-HUMAN-UAT.md` are all pending device validation. The file was created by the executor in Task 3 of Plan 03. Do not duplicate the tests here — refer to that file for the full test scripts.

**Summary of what needs device validation:**

#### 1. Store List Rendering (STORE-01)

**Test:** Open app → tap Home Extended FAB → verify menu expands with "Browse stores" and "Create registry" → tap "Browse stores" → verify 2-column grid of 8 stores appears with logos and names
**Expected:** 8 store cards visible; placeholder logos shown (1x1 transparent — will appear blank until real assets supplied); store names visible; scroll works
**Why human:** Compose UI rendering requires a running device; logo visual quality cannot be verified programmatically

#### 2. WebView Store Rendering (STORE-02)

**Test:** From Registry Detail → FAB → "Browse stores" → tap "eMAG" → verify store homepage loads with JS-rendered content
**Expected:** eMAG homepage visible with search bar, product tiles; "Add to list" button at bottom; store name in TopAppBar
**Why human:** WebView rendering, JS execution, and network require a real device or emulator

#### 3. Add-to-list Full Funnel (STORE-03)

**Test:** In WebView → navigate to product page → tap "Add to list" → verify AddItemScreen opens with URL pre-filled → save → verify item in Registry with affiliateUrl set
**Expected:** URL field pre-filled; OG metadata populates; item saved; Firestore item has affiliateUrl
**Why human:** End-to-end spans multiple screens, OG fetch network call, Firestore write — not unit-testable

#### 4. Error State + Retry (STORE-04)

**Test:** Enable airplane mode → open Store Browser → verify error overlay → disable airplane mode → tap "Try again" → verify homepage loads
**Expected:** WifiOff icon + headings shown; "Add to list" greyed out; retry restores WebView
**Why human:** Requires network failure simulation on physical device

#### 5. External Scheme Blocking (D-08)

**Test:** Navigate to a store contact page with tel: or mailto: links → tap one
**Expected:** Toast "This link opens in an external app" shown; WebView stays on current page; no external app opened
**Why human:** Requires store pages with external scheme links and physical tap interaction

#### 6. Cookie Persistence (D-06)

**Test:** Accept cookies / log in within WebView → back out → re-open same store
**Expected:** Cookie/session state preserved; no re-prompting
**Why human:** Cookie persistence spans multiple WebView sessions; requires real network

#### 7. Romanian Locale (D-18)

**Test:** Switch device to Romanian → run Tests A-D briefly
**Expected:** All stores_* strings in Romanian ("Nou", "Răsfoiește magazine", "Adaugă la listă", "Pagina nu s-a încărcat")
**Why human:** Locale switching requires device system settings change and visual inspection

#### 8. FAB Menu Scrim Dismiss (Component Contract 1)

**Test:** Expand Home FAB menu → tap outside the menu items → verify menu collapses without navigating
**Expected:** Menu collapses; no navigation; FAB icon returns to Add
**Why human:** Compose gesture interaction requires a running device

### Known Accepted Deviations (Do Not Flag as Gaps)

| Deviation | Context | Decision |
|-----------|---------|----------|
| Toast instead of Snackbar for external-scheme blocking | D-08 MVP trade-off; Toast is inside WebViewClient callback thread | Follow-up can swap to Snackbar via dedicated StateFlow in StoreBrowserViewModel |
| Home-FAB Store Browser entry disables "Add to list" (null registryId) | D-10 multi-registry picker deferred | Infrastructure shipped; picker UI is v2 scope; Registry Detail entry path works correctly |
| Placeholder logo .webp files (1x1 transparent) | Real retailer logos are trademarked; cannot be auto-generated | Replace 9 files in `drawable-nodpi/` with real brand assets before production; no code changes needed |
| 14 stores_* keys instead of stated 15 | Plan stated 15; UI-SPEC defines 14; `common_back` already existed | Correct count; UI-SPEC is authoritative |

### Gaps Summary

No gaps blocking goal achievement. All automated verifications pass:

- All 28+ artifacts exist and are substantive (not stubs)
- All key links are wired end-to-end
- Data flows from Firestore through domain through ViewModel to UI
- Unit tests (18 tests across 4 suites) cover the non-UI logic
- Debug APK builds (per 07-03-SUMMARY.md)
- Firestore rules tests pass (per 07-00-SUMMARY.md)

The phase is complete pending human UAT. Status is `human_needed` because:
1. WebView rendering cannot be verified programmatically
2. JS execution in a real WebView requires a device or emulator
3. The end-to-end affiliate round-trip spans a network-dependent OG fetch + Firestore write
4. Physical device interaction is required for error state, external scheme blocking, cookies, and locale

The 07-HUMAN-UAT.md file (committed at `2904753`) contains the full test scripts for Tests A–H.

---

_Verified: 2026-04-20T06:00:00Z_
_Verifier: Claude (gsd-verifier)_
