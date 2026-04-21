---
phase: 09-shared-chrome-status-ui
plan: 04
subsystem: ui
tags: [jetpack-compose, material3, bottom-nav, modal-bottom-sheet, status-chips, wave-2, chrome-integration, registry-detail-rewire]

# Dependency graph
requires:
  - phase: 09-02
    provides: StatusChip(status, expiresAt) at com.giftregistry.ui.common.status — drop-in for inline ItemStatusChip
  - phase: 09-03
    provides: GiftMaisonBottomNav + AddActionSheet + showsBottomNav() at com.giftregistry.ui.common.chrome
provides:
  - AppNavigation.kt — full chrome integration (GiftMaisonBottomNav + AddActionSheet + RegistryListViewModel scope + blur guard)
  - RegistryDetailScreen.kt — item rows now use shared StatusChip; inline ItemStatusChip + ReservationCountdown deleted
  - StyleGuidePreview.kt — 4 new @Preview composables for Chrome + Status verification
affects: [phase-10-home-redesign, phase-11-add-item-url-field]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "showsBottomNav() import replaces local predicate — semantic change from persistent-everywhere to HomeKey+RegistryDetailKey only (intentional Phase 9 cutover per CONTEXT.md D-03)"
    - "AddActionSheet hoisted outside Scaffold closure — sheet scrim overlays nav bar correctly; placement inside entry<HomeKey> would fail to overlay the bar"
    - "Modifier.blur(1.dp) gated by Build.VERSION.SDK_INT >= Build.VERSION_CODES.S — plain ink.copy(0.55f) scrim on API < 31"
    - "hiltViewModel<RegistryListViewModel>() at AppNavigation scope — Activity ViewModelStoreOwner shares instance with entry<HomeKey>, no duplicate Firebase listeners"
    - "maxByOrNull { it.updatedAt } as Phase 9 isPrimary resolver — Phase 10 refines with real isPrimary field"
    - "sheetContextRegistryId: RegistryDetailKey.registryId when on detail, else primaryRegistryId — pre-selects currently-viewed registry for sheet actions from RegistryDetail"

key-files:
  modified:
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
    - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt
    - app/src/main/java/com/giftregistry/ui/theme/preview/StyleGuidePreview.kt
  deleted:
    - app/src/main/java/com/giftregistry/ui/registry/detail/ReservationCountdown.kt

key-decisions:
  - "SuggestionChip imports kept in RegistryDetailScreen.kt — file still uses SuggestionChip for registry.occasion label in header; only the inline ItemStatusChip composable was deleted"
  - "hasRegistries && primaryRegistryId != null simplified to hasRegistries — compiler warned condition was always true; hasRegistries is already defined as primaryRegistryId != null"
  - "AddActionSheet placed after Scaffold block (not inside Scaffold) — ensures ModalBottomSheet scrim overlays the bottom nav bar itself"

# Metrics
duration: ~12min
completed: 2026-04-21
---

# Phase 9 Plan 04: Wave 2 — Chrome + Status Integration Summary

**AppNavigation cutover to GiftMaisonBottomNav + AddActionSheet; RegistryDetailScreen rewired to shared StatusChip; ReservationCountdown.kt deleted; StyleGuidePreview appended with 4 Phase 9 preview sections. Debug APK builds. Awaiting human on-device checkpoint.**

## Performance

- **Duration:** ~12 min
- **Started:** 2026-04-21T09:43:38Z
- **Completed:** 2026-04-21 (Tasks 1+2 complete; Task 3 awaiting human verification)
- **Tasks:** 2/3 automated complete; 1 human-verify checkpoint pending
- **Files modified:** 3 (modified) + 1 (deleted)

## Accomplishments

### Task 1 — AppNavigation.kt cutover

- Deleted old `showsBottomNav()` local predicate (persistent-everywhere semantics) and imported `showsBottomNav` from `com.giftregistry.ui.common.chrome` (HomeKey + RegistryDetailKey only — intentional Phase 9 semantic change per D-03)
- Deleted entire `NavigationBar { NavigationBarItem(...) x4 }` block (4-tab nav)
- Replaced with `GiftMaisonBottomNav` bound to 5-slot routing per UI-SPEC Interaction Contract table
- Hoisted `AddActionSheet` OUTSIDE `Scaffold` closure so scrim overlays the nav bar
- Injected `RegistryListViewModel` at AppNavigation scope; `primaryRegistryId = maxByOrNull { it.updatedAt }?.id`
- Lists tab: routes to `RegistryDetailKey(primaryRegistryId)` when hasRegistries; opens AddActionSheet when zero registries; no-op when already on RegistryDetailKey
- Blur on API 31+ via `Modifier.blur(1.dp)` gated by `Build.VERSION.SDK_INT >= Build.VERSION_CODES.S`
- `sheetContextRegistryId` derived from current key — pre-selects current registry when on RegistryDetail, falls back to primary for FAB from Home

### Task 2 — RegistryDetailScreen rewire + cleanup + StyleGuidePreview

- Added `import com.giftregistry.ui.common.status.StatusChip`
- Replaced `ItemStatusChip(status = item.status)` with `StatusChip(status = item.status, expiresAt = item.expiresAt)` — single call handles both status pill and countdown
- Deleted `if (item.status == ItemStatus.RESERVED) { ... ReservationCountdown(...) }` block (lines ~539-552) — countdown now rendered inside StatusChip
- Deleted private `ItemStatusChip` composable (lines ~601-625) — replaced by shared chip
- Deleted `ReservationCountdown.kt` file entirely (0 remaining references in codebase)
- Kept `SuggestionChip` / `SuggestionChipDefaults` imports — still used for `registry.occasion` pill on detail screen header
- Appended 4 new `@Preview` composables to StyleGuidePreview.kt:
  - `BottomNavHomeSelectedPreview` — HomeKey selected state
  - `BottomNavListsSelectedPreview` — RegistryDetailKey (Lists) selected state
  - `StatusChipsPreview` — AVAILABLE / RESERVED / PURCHASED chips side-by-side
  - `PulsingDotPreview` — 1400ms vs 1000ms periods side-by-side (size=8dp for visibility)

## Task Commits

1. **Task 1: AppNavigation cutover** — `621be0d`
2. **Task 2: RegistryDetailScreen rewire + StyleGuidePreview** — `8a55b8c`

## Files Created/Modified

### Modified

| File | Change | Key diff |
|------|--------|---------|
| `AppNavigation.kt` | 224 insertions, 182 deletions | Old NavigationBar replaced; chrome + addSheet integrated |
| `RegistryDetailScreen.kt` | ~20 lines removed, ~3 added | ItemStatusChip deleted; StatusChip import + usage added |
| `StyleGuidePreview.kt` | +71 lines | 4 new @Preview composables appended |

### Deleted

| File | Reason |
|------|--------|
| `ReservationCountdown.kt` | Only consumer (RegistryDetailScreen line ~548) deleted in this plan; no remaining references |

## Verification Results (Automated)

- `./gradlew :app:compileDebugKotlin` — BUILD SUCCESSFUL
- `./gradlew :app:testDebugUnitTest` — BUILD SUCCESSFUL (all 33+ unit tests GREEN)
- `./gradlew :app:assembleDebug` — BUILD SUCCESSFUL (debug APK produced)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] SuggestionChip imports incorrectly removed**

- **Found during:** Task 2 compile — `Unresolved reference 'SuggestionChipDefaults'` at line 417
- **Issue:** The plan specified removing `SuggestionChip` and `SuggestionChipDefaults` imports, but those are still used at line ~416 for the `registry.occasion` chip on the registry header (not just by the deleted `ItemStatusChip`)
- **Fix:** Kept `SuggestionChip` and `SuggestionChipDefaults` imports in `RegistryDetailScreen.kt`
- **Files modified:** `app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt`
- **Commit:** `8a55b8c`

**2. [Rule 1 - Bug] Redundant condition in Lists-tab handler simplified**

- **Found during:** Task 1 compile — Kotlin compiler warning `Condition is always 'true'` on `hasRegistries && primaryRegistryId != null`
- **Issue:** `hasRegistries` is defined as `primaryRegistryId != null`, making the compound condition tautological
- **Fix:** Simplified to `} else if (hasRegistries) {`
- **Files modified:** `app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt`
- **Commit:** `621be0d`

## Task 3 — Human Checkpoint

Task 3 is a `type="checkpoint:human-verify"` gate. The debug APK has been built and installed-ready. The continuation agent will run after the user confirms on-device verification of all 21 checks.

## Known Follow-ups

1. **Cleanup todo**: Old `nav_home`, `nav_add_list`, `nav_preferences` string keys remain in `strings.xml` and `values-ro/strings.xml`. No UI code references them (old NavigationBar deleted). Safe to remove in a follow-up quick task.
2. **Phase 11**: Apply element-level purchased treatments (image grayscale + ink tint + check overlay + title strikethrough) at item row call sites — `PurchasedRowModifier.purchasedVisualTreatment()` is Phase 9 row-alpha only; element-level treatments documented in KDoc as Phase 11 responsibility.
3. **Phase 10**: Refine `isPrimary` resolution from `maxByOrNull { it.updatedAt }` to the real `isPrimary` field when Home redesign lands.

## Known Stubs

None — all exported symbols are fully implemented. The `sheetContextRegistryId` falls back to `primaryRegistryId` for Home-FAB paths; the "zero-registry" defensive no-op on `onItemFromUrl` / `onAddManually` is intentional and documented inline.

## Self-Check

- [x] AppNavigation.kt exists and compiles
- [x] `GiftMaisonBottomNav` import present in AppNavigation.kt
- [x] `showsBottomNav` from chrome package imported (local predicate deleted)
- [x] `NavigationBar {` count = 0 in AppNavigation.kt
- [x] `AddActionSheet(` in AppNavigation.kt
- [x] `maxByOrNull { it.updatedAt }` in AppNavigation.kt
- [x] `Build.VERSION.SDK_INT >= Build.VERSION_CODES.S` in AppNavigation.kt
- [x] `StatusChip(status = item.status, expiresAt = item.expiresAt)` in RegistryDetailScreen.kt
- [x] `private fun ItemStatusChip` count = 0 in RegistryDetailScreen.kt
- [x] `ReservationCountdown` count = 0 in RegistryDetailScreen.kt
- [x] `ReservationCountdown.kt` file deleted (verified via git rm)
- [x] `grep -r "ReservationCountdown" app/src/main --include="*.kt"` = 0 matches
- [x] 4 new `@Preview` composables in StyleGuidePreview.kt (total = 9, was 5)
- [x] Task 1 commit `621be0d` exists
- [x] Task 2 commit `8a55b8c` exists
- [x] `./gradlew :app:testDebugUnitTest` BUILD SUCCESSFUL
- [x] `./gradlew :app:assembleDebug` BUILD SUCCESSFUL

## Self-Check: PASSED

---
*Phase: 09-shared-chrome-status-ui*
*Status: Tasks 1+2 complete — awaiting human checkpoint (Task 3)*
*Completed Tasks 1+2: 2026-04-21*
