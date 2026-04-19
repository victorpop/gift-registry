---
phase: 07-romanian-store-browser
plan: 02
subsystem: android, ui, navigation
tags: [compose, navigation3, hilt, material3, store-list, fab-menu, lazy-grid, mockk]

# Dependency graph
requires:
  - phase: 07-romanian-store-browser
    plan: 00
    provides: "stores_* string keys, store logo drawables, R8 keep rule"
  - phase: 07-romanian-store-browser
    plan: 01
    provides: "Store domain model, StoreRepository interface, GetStoresUseCase, StoresModule DI binding"

provides:
  - "StoreListKey(preSelectedRegistryId: String? = null) and StoreBrowserKey(storeId, registryId: String?) nav keys"
  - "StoreListUiState sealed interface: Loading / Success(stores) / Error(message)"
  - "StoreListViewModel @HiltViewModel with loadStores() retry, empty list surfaced as Error (D-17)"
  - "StoreLogoResolver: resolveStoreLogoResId(context, logoAsset) — getIdentifier with store_generic fallback"
  - "StoreListScreen: TopAppBar + LazyVerticalGrid(2 cols) + Loading/Success/Error states"
  - "RegistryListScreen FAB refactored to expandable menu (Browse stores + Create registry)"
  - "RegistryDetailScreen add-item FAB now opens DropdownMenu with Add item + Browse stores"
  - "AppNavigation: entry<StoreListKey> wired; Home→StoreList (null registryId); RegistryDetail→StoreList (current registryId)"
  - "4 StoreListViewModelTest unit tests (init success, failure, empty list, retry) — all green"

affects: [07-03]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "AnimatedVisibility + expandVertically/fadeIn for FAB menu expansion (M3 standard, no third-party library)"
    - "AnimatedContent(contentAlignment = Alignment.Center) for FAB icon swap (Add ↔ Close) per Research Pitfall 7"
    - "LazyVerticalGrid(GridCells.Fixed(2)) for 2-column store card grid"
    - "resolveStoreLogoResId: getIdentifier wrapper — isolated in StoreLogoResolver.kt to prevent scattered usages"
    - "DropdownMenu for Registry Detail add-item choice (simpler than FAB menu pattern since it has a single trigger)"
    - "StoreListKey as data class (not data object) — carries preSelectedRegistryId: String? = null for registry-aware navigation"

key-files:
  created:
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt (modified: +2 keys)
    - app/src/main/java/com/giftregistry/ui/store/list/StoreListUiState.kt
    - app/src/main/java/com/giftregistry/ui/store/list/StoreListViewModel.kt
    - app/src/main/java/com/giftregistry/ui/store/list/StoreLogoResolver.kt
    - app/src/main/java/com/giftregistry/ui/store/list/StoreListScreen.kt
    - app/src/test/java/com/giftregistry/ui/store/list/StoreListViewModelTest.kt
  modified:
    - app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt
    - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt

key-decisions:
  - "StoreListKey declared as data class (not data object) to carry optional preSelectedRegistryId — enables registry-aware navigation from RegistryDetail without a separate nav key or ViewModel-scoped mechanism"
  - "Nullable preSelectedRegistryId: String? = null is safe for Navigation3 serialization per Research Pitfall 6 and STATE.md Phase 4 decision (nullable defaults with null are clean)"
  - "entry<StoreBrowserKey> intentionally NOT added — Plan 03 owns this handler; tapping a store card is a no-op until Plan 03 ships"
  - "Used Icons.Default.ShoppingBag (confirmed in material-icons-extended) over Icons.Default.Storefront (LOW confidence per Research Tertiary note)"
  - "FilledTonalButton for FAB menu items per UI-SPEC Color Scheme: accent reserved for the FAB itself, not expanded menu items"
  - "RegistryDetailScreen.onNavigateToBrowseStores has default = {} to avoid breaking existing call sites that don't yet pass the callback"

# Metrics
duration: 3min
completed: 2026-04-19
---

# Phase 07 Plan 02: Store List Screen + FAB Refactor Summary

**Browse stores entry points live end-to-end: Home FAB menu + Registry Detail DropdownMenu both navigate to StoreListScreen (LazyVerticalGrid, 2-col, Loading/Success/Error states) with registry-aware StoreListKey(preSelectedRegistryId) threading through to Plan 03's StoreBrowserKey**

## Performance

- **Duration:** ~3 min
- **Started:** 2026-04-19T21:14:35Z
- **Completed:** 2026-04-19T21:17:45Z
- **Tasks:** 2 (both complete)
- **Files modified:** 9 (5 new, 4 modified)

## Accomplishments

- `StoreListKey(preSelectedRegistryId: String? = null)` and `StoreBrowserKey(storeId: String, registryId: String?)` appended to `AppNavKeys.kt` — both `@Serializable data class`
- `StoreListUiState` sealed interface: `Loading`, `Success(stores: List<Store>)`, `Error(message: String)`
- `StoreListViewModel` (`@HiltViewModel`): `StateFlow<StoreListUiState>`, `loadStores()` retry, empty stores → `Error` per D-17
- `StoreLogoResolver`: `resolveStoreLogoResId(context, logoAsset)` as the single call-site for `getIdentifier`; fallback to `R.drawable.store_generic`
- `StoreListScreen`: `TopAppBar` + `LazyVerticalGrid(GridCells.Fixed(2))` + three states (loading spinner, success grid, error overlay with CloudOff + retry)
- `RegistryListScreen` FAB refactored: `ExtendedFloatingActionButton` toggles `AnimatedVisibility` column with `FabMenuRow` items (Browse stores + Create registry); scrim `Box` dismisses on outside tap; icon animates Add ↔ Close via `AnimatedContent`
- `RegistryDetailScreen` add-item FAB upgraded to `DropdownMenu`: "Add item" + "Browse stores" options; new `onNavigateToBrowseStores` callback
- `AppNavigation`: `entry<StoreListKey>` wired to `StoreListScreen`; Home passes `preSelectedRegistryId = null`; RegistryDetail passes `preSelectedRegistryId = key.registryId`; `StoreBrowserKey` pushed on store card tap (no entry handler — Plan 03 pending)
- 4 `StoreListViewModelTest` tests all green: init success, failure, empty list, retry

## Task Commits

1. **Task 1: Nav keys + StoreListViewModel + StoreLogoResolver + 4 VM tests** - `6d42554` (feat)
2. **Task 2: StoreListScreen + FAB refactor + Registry Detail entry + nav wiring** - `531eeec` (feat)

## Nav Key Signatures (exact Kotlin)

```kotlin
@Serializable data class StoreListKey(val preSelectedRegistryId: String? = null)
@Serializable data class StoreBrowserKey(val storeId: String, val registryId: String?)
```

## Files Created / Modified

| File | Status | Key addition |
|------|--------|-------------|
| `AppNavKeys.kt` | Modified | +2 keys (StoreListKey, StoreBrowserKey) |
| `StoreListUiState.kt` | New | Sealed interface: Loading/Success/Error |
| `StoreListViewModel.kt` | New | @HiltViewModel, loadStores() retry, D-17 empty→Error |
| `StoreLogoResolver.kt` | New | resolveStoreLogoResId — single getIdentifier call-site |
| `StoreListScreen.kt` | New | LazyVerticalGrid(2), CloudOff error state, TopAppBar |
| `StoreListViewModelTest.kt` | New | 4 tests — all green |
| `RegistryListScreen.kt` | Modified | FAB → expandable AnimatedVisibility menu |
| `RegistryDetailScreen.kt` | Modified | FAB → DropdownMenu with Browse stores option |
| `AppNavigation.kt` | Modified | entry<StoreListKey> + both caller sites updated |

## Deviations from Plan

### Auto-fixed Issues

None — plan executed exactly as written with one minor pre-emptive alignment:

**Alignment note:** Task 1 declared `StoreListKey` as `data class` (not `data object`) from the start, because Task 2's action block explicitly corrects this. The plan's Task 1 action shows `data object StoreListKey` but its Task 2 action block says "Go back and update Step 1's Nav key declaration" to use `data class StoreListKey(val preSelectedRegistryId: String? = null)`. Applied the final correct form immediately in Task 1 to avoid a two-step correction.

**Icon deviation (UI-SPEC tertiary note):** `Icons.Default.ShoppingBag` used over `Icons.Default.Storefront` per UI-SPEC fallback instruction and Research Tertiary note (Storefront availability unconfirmed at Compose BOM 2026.03.00). Both icon names are noted in the plan; ShoppingBag is confirmed present in `material-icons-extended`.

## Known Stubs

None — all navigation callbacks wire to real screens. One intentional gap documented below.

## Intentional Known Gap

**Tapping a store card is a no-op until Plan 03**

- `StoreListScreen` pushes `StoreBrowserKey(storeId, registryId)` via `onStoreSelected` in `AppNavigation`
- `entry<StoreBrowserKey>` handler is NOT added in this plan — Plan 03 owns the WebView screen implementation
- In Navigation3, an unregistered back-stack entry renders no content (no crash), so the app is in a valid intermediate state
- This gap is expected, documented, and will be resolved in Plan 07-03

## Issues Encountered

None.

## Next Phase Readiness

- Plan 07-03 (Store Browser WebView screen) can proceed immediately — `StoreBrowserKey(storeId, registryId)` is already being pushed on store card tap; Plan 03 adds the matching `entry<StoreBrowserKey>` handler and `StoreBrowserScreen` composable

---
*Phase: 07-romanian-store-browser*
*Completed: 2026-04-19*
