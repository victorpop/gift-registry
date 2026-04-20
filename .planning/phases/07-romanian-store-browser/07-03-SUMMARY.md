---
phase: 07-romanian-store-browser
plan: 03
subsystem: android, ui, navigation, webview
tags: [compose, navigation3, hilt, webview, material3, androidview, stateflow, mockk]

# Dependency graph
requires:
  - phase: 07-romanian-store-browser
    plan: 00
    provides: "stores_* string keys, store logo drawables, R8 keep rule, WebView permission"
  - phase: 07-romanian-store-browser
    plan: 01
    provides: "Store domain model, StoreRepository, GetStoresUseCase, StoresModule DI"
  - phase: 07-romanian-store-browser
    plan: 02
    provides: "StoreBrowserKey(storeId, registryId) nav key, entry<StoreBrowserKey> placeholder"
  - phase: 03-registry-item-management
    provides: "AddItemScreen, AddItemViewModel, FetchOgMetadataUseCase, AffiliateUrlTransformer"

provides:
  - "StoreBrowserScreen: Scaffold + AndroidView(WebView) + persistent 56dp Add-to-list bottom bar + error overlay"
  - "StoreBrowserViewModel: currentUrl/pageLoadFailed/storeName/homepageUrl/addToListEnabled StateFlows"
  - "AddItemKey extended with initialUrl: String? = null + initialRegistryId: String? = null"
  - "AddItemViewModel auto-triggers onFetchMetadata() when initialUrl non-blank"
  - "AddItemScreen signature updated to accept + forward initialUrl/initialRegistryId via hiltViewModelWithNavArgs"
  - "entry<StoreBrowserKey> in AppNavigation — closes the no-op gap from Plan 02"
  - "entry<AddItemKey> in AppNavigation updated to unpack new optional fields"
  - "StoreBrowserViewModelTest: 6 unit tests (initial state, URL change, load fail, retry, store match, unknown store)"
  - "07-HUMAN-UAT.md: 8 manual UAT tests (Tests A-H) pending device validation"

affects: []

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "AndroidView(::WebView) factory-once pattern: WebView created in factory lambda, load triggered via LaunchedEffect(homepageUrl) to avoid recomposition reloads (Research Pattern 1)"
    - "WebView ref as remember { mutableStateOf<WebView?>(null) } in composable — never in ViewModel (Pitfall 4 context leak prevention)"
    - "isForMainFrame guard in onReceivedError to avoid error overlay on subresource failures (Pitfall 5)"
    - "alpha(0f) to hide WebView without removing from composition during error state — preserves instance"
    - "LaunchedEffect(pageLoadFailed) for retry: ViewModel.onRetry() clears flag → LaunchedEffect triggers webViewRef?.reload()"
    - "hiltViewModelWithNavArgs seeding SavedStateHandle with initialUrl/initialRegistryId for cross-screen URL passing"
    - "Nullable defaults (String? = null) on new AddItemKey fields for safe Navigation3 serialization (Pitfall 6)"

key-files:
  created:
    - app/src/main/java/com/giftregistry/ui/store/browser/StoreBrowserViewModel.kt
    - app/src/main/java/com/giftregistry/ui/store/browser/StoreBrowserScreen.kt
    - app/src/test/java/com/giftregistry/ui/store/browser/StoreBrowserViewModelTest.kt
    - .planning/phases/07-romanian-store-browser/07-HUMAN-UAT.md
  modified:
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt
    - app/src/main/java/com/giftregistry/ui/item/add/AddItemViewModel.kt
    - app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt

key-decisions:
  - "WebView ref held in Composable (remember mutableStateOf) not ViewModel — prevents Activity context leak on rotation/nav pop"
  - "LaunchedEffect(homepageUrl, webViewRef) for initial load — avoids loadUrl in update lambda which re-runs on every recomposition"
  - "LaunchedEffect(pageLoadFailed) for retry — ViewModel state flip triggers WebView.reload() from composable scope, keeping WebView methods on main thread"
  - "External scheme (mailto:, tel:) blocking uses Toast inside shouldOverrideUrlLoading — MVP acceptable per plan note; follow-up can swap to Snackbar via dedicated StateFlow"
  - "Add-to-list button disabled when registryId is null (Home-FAB entry path) — guards the AddItemKey dispatch from null registryId; D-10 picker is the follow-up path"
  - "isForMainFrame guard prevents error overlay showing for subresource failures (ads, tracking pixels)"
  - "alpha(0f) keeps WebView in composition during error state — avoids re-creation of WebView instance on retry"

requirements-completed: [STORE-02, STORE-03, STORE-04]

# Metrics
duration: 12min
completed: 2026-04-20
---

# Phase 07 Plan 03: Store Browser WebView + Add-to-list Funnel Summary

**In-app WebView store browser (StoreBrowserScreen) with persistent Add-to-list CTA that pre-fills AddItemScreen via SavedStateHandle, completing the STORE-02/03/04 funnel through the existing Phase 3 OG-fetch + AffiliateUrlTransformer pipeline**

## Performance

- **Duration:** ~12 min
- **Started:** 2026-04-20T05:26:35Z
- **Completed:** 2026-04-20T05:38:00Z
- **Tasks:** 3 (Tasks 1 + 2 fully automated; Task 3 = human UAT file created)
- **Files modified:** 8 (4 new, 4 modified)

## Accomplishments

- `StoreBrowserViewModel` with `currentUrl`, `pageLoadFailed`, `storeName`, `homepageUrl`, `addToListEnabled` StateFlows — 6 unit tests green
- `StoreBrowserScreen` Scaffold with AndroidView-wrapped WebView: `javaScriptEnabled=true`, `domStorageEnabled=true`, `CookieManager.setAcceptCookie(true)`, `onPageFinished`/`onReceivedError(isForMainFrame)`/`shouldOverrideUrlLoading` wired; error overlay with WifiOff icon + Retry + Back; persistent 56dp Add-to-list bottom bar
- `AddItemKey` extended with `initialUrl: String? = null` and `initialRegistryId: String? = null`; `AddItemViewModel` auto-triggers `onFetchMetadata()` when `initialUrl` non-blank; `AddItemScreen` forwards new params via `hiltViewModelWithNavArgs`
- `entry<StoreBrowserKey>` added to `AppNavigation` — closes the no-op gap deferred from Plan 02; `entry<AddItemKey>` updated to unpack new optional fields
- `07-HUMAN-UAT.md` created with 8 Tests (A-H) covering STORE-01..04 + locale + external scheme + cookies + FAB dismiss — all pending device validation
- `./gradlew :app:assembleDebug` exits 0; full unit test suite green

## Task Commits

1. **Task 1: AddItemKey pre-fill + StoreBrowserViewModel + 6 unit tests** - `8f63601` (feat)
2. **Task 2: StoreBrowserScreen composable + AppNavigation entry<StoreBrowserKey> wiring** - `356ad2a` (feat)
3. **Task 3: Human UAT file (Tests A-H, all pending)** - `2904753` (test)

## WebView Settings

| Setting | Value | Decision |
|---------|-------|----------|
| `javaScriptEnabled` | `true` | D-05: required for modern Romanian e-commerce sites |
| `domStorageEnabled` | `true` | Required by most Romanian e-commerce sites (localStorage) |
| `CookieManager.setAcceptCookie` | `true` | D-06: persists login/cart state across sessions |
| `onPageFinished` | `→ viewModel.onUrlChanged(url)` | D-07: URL captured for Add-to-list CTA |
| `onReceivedError` | `if (isForMainFrame) viewModel.onPageLoadFailed()` | D-15 + Pitfall 5 |
| `shouldOverrideUrlLoading` | Blocks non-http/https schemes | D-08: keeps user in browser flow |

## Auto-fetch Behavior in AddItemViewModel

When `initialUrl` is non-blank (set via `hiltViewModelWithNavArgs` from `StoreBrowserScreen.onAddToList`):
1. `url.value = initialUrl` (pre-fills the URL field in `AddItemScreen`)
2. `onFetchMetadata()` is called in the `init` block — triggers `FetchOgMetadataUseCase` to auto-populate title, imageUrl, price
3. The existing `AffiliateUrlTransformer` (Phase 3, `ItemRepositoryImpl.addItem`) runs on save — no changes needed to the affiliate pipeline

The StateFlow that triggers the auto-fetch is the `initialUrl` field read from `SavedStateHandle` in the `init` block.

## UAT Results

| Test | Requirement | Result |
|------|-------------|--------|
| A — Store List + Home FAB menu | STORE-01 | pending |
| B — WebView opens store homepage | STORE-02 | pending |
| C — Add-to-list funnel + affiliate round-trip | STORE-03 | pending |
| D — WebView error state + retry | STORE-04 | pending |
| E — External scheme blocking | D-08 | pending |
| F — Cookie persistence | D-06 | pending |
| G — Romanian locale | D-18 | pending |
| H — FAB menu dismiss scrim | Component Contract 1 | pending |

## Files Created / Modified

| File | Status | Key addition |
|------|--------|-------------|
| `StoreBrowserViewModel.kt` | New | @HiltViewModel, currentUrl/pageLoadFailed/addToListEnabled StateFlows, 6 tests |
| `StoreBrowserScreen.kt` | New | Scaffold + AndroidView(WebView) + error overlay + 56dp bottom CTA |
| `StoreBrowserViewModelTest.kt` | New | 6 unit tests — all green |
| `07-HUMAN-UAT.md` | New | 8 manual UAT tests A-H, all pending |
| `AppNavKeys.kt` | Modified | AddItemKey extended with initialUrl + initialRegistryId |
| `AddItemViewModel.kt` | Modified | Reads initialUrl/initialRegistryId from SavedStateHandle; init auto-triggers OG fetch |
| `AddItemScreen.kt` | Modified | Accepts + forwards initialUrl/initialRegistryId via hiltViewModelWithNavArgs |
| `AppNavigation.kt` | Modified | entry<StoreBrowserKey> added; entry<AddItemKey> updated to pass new optional fields |

## Deviations from Plan

### Known Deviations (documented in plan)

**1. External scheme Toast instead of Snackbar (D-08)**
- **Context:** `shouldOverrideUrlLoading` callback runs on the WebView's main thread. Showing a Compose `Snackbar` from this callback requires a dedicated ViewModel StateFlow for the "blocked" event, which is additional complexity.
- **Implementation:** Toast used inside `view.post { }` — matches D-08's intent (user sees feedback), minimal code.
- **Follow-up:** A follow-up revision can swap to Snackbar via a dedicated `_externalSchemeBlocked: SharedFlow<Unit>` in `StoreBrowserViewModel` if UAT flags this.

**2. Home-FAB entry disables Add-to-list (D-10 follow-up)**
- **Context:** When the user opens Store Browser from the Home FAB (no pre-selected registry), `registryId` is null at the nav key level. `AddItemKey` requires a concrete `registryId`.
- **Implementation:** `Button(enabled = addEnabled && viewModel.registryId != null)` — Add-to-list is disabled for the Home-FAB entry path. Registry Detail entry path works correctly.
- **Follow-up:** D-10 picker + `LastRegistryPreferencesDataStore` auto-pick for single-registry users will resolve this in a future plan.

None — plan executed exactly as written for automated portions. The two deviations above were anticipated and documented in the plan objective.

## Known Stubs

None — all navigation callbacks wire to real screens. Human UAT tests are pending but the implementation is complete; stub behavior would have been caught by the acceptance criteria checks.

## Issues Encountered

None — compilation, unit tests, and debug APK build all passed on first attempt.

## Next Phase Readiness

- Phase 7 is complete — all 4 plans (00-03) executed and committed
- Human UAT (Tests A-H) in `.planning/phases/07-romanian-store-browser/07-HUMAN-UAT.md` pending device validation
- The add-to-list funnel is end-to-end: Store List → WebView → Add Item (pre-filled) → Save → AffiliateUrlTransformer → Firestore
- D-10 registry picker remains deferred — single-registry users are served correctly; multi-registry users need the picker

---
*Phase: 07-romanian-store-browser*
*Completed: 2026-04-20*
