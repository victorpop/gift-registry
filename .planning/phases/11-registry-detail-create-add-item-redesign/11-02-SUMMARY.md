---
phase: 11-registry-detail-create-add-item-redesign
plan: 02
subsystem: ui
tags: [kotlin, pure-kotlin, unit-tests, registry-detail, create-registry, add-item, affiliate]

# Dependency graph
requires:
  - phase: 11-01
    provides: Wave 0 test files (7 test classes, 51 @Test methods) pinning all helper contracts
  - phase: 03-registry-item-management
    provides: AffiliateUrlTransformer object + merchantRules pattern

provides:
  - AffiliateUrlTransformer.isAffiliateDomain(url): Boolean — public accessor for Plans 03/04/05
  - FilterChipState enum + matches(ItemStatus): Boolean — SCR-08 filter chip logic
  - heroToolbarAlpha() with Pitfall 1 guard — SCR-08 toolbar alpha derivation
  - RegistryStats data class + registryStatsOf() — SCR-08 4-stat strip
  - shareUrlOf(registryId): String — SCR-08 share banner URL
  - OccasionCatalog object (6 entries + legacy alias map) — SCR-09 tile grid
  - AddItemMode enum + ADD_ITEM_MODE_DEFAULT_ORDINAL — SCR-10 segmented control
  - shouldShowAffiliateRow() predicate — SCR-10 affiliate row visibility

affects: [11-03, 11-04, 11-05]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Pure-Kotlin helper files in UI package — no Compose imports, unit-testable without runtime"
    - "isAffiliateDomain as additive public method on existing object — backward-compatible extension of merchantRules check"
    - "Legacy alias map with lowercased keys for case-insensitive Firestore doc canonicalisation"
    - "ADD_ITEM_MODE_DEFAULT_ORDINAL top-level const val per Phase 10 rememberSaveable Int pattern"

key-files:
  created:
    - app/src/main/java/com/giftregistry/ui/registry/detail/FilterChipState.kt
    - app/src/main/java/com/giftregistry/ui/registry/detail/HeroToolbarAlpha.kt
    - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryStats.kt
    - app/src/main/java/com/giftregistry/ui/registry/detail/ShareUrl.kt
    - app/src/main/java/com/giftregistry/ui/registry/create/OccasionCatalog.kt
    - app/src/main/java/com/giftregistry/ui/item/add/AddItemMode.kt
    - app/src/main/java/com/giftregistry/ui/item/add/AffiliateRowVisibility.kt
  modified:
    - app/src/main/java/com/giftregistry/util/AffiliateUrlTransformer.kt

key-decisions:
  - "isAffiliateDomain inserted between extractDomain() and noMatch() in AffiliateUrlTransformer — additive, merchantRules stays private, transform() unchanged"
  - "Both tasks committed in single atomic commit (9b6da52) because Gradle compiles all test packages together — task-level split required all 7 helpers to exist before any test could compile"
  - "OccasionCatalog legacy aliases use lowercase keys so storageKeyFor() normalises via .lowercase() before map lookup — handles any casing variant from Firestore"

patterns-established:
  - "Phase 11 helper pattern: pure-Kotlin files in ui.{screen} packages, no Compose imports, immediately unit-testable"

requirements-completed: [SCR-08, SCR-09, SCR-10]

# Metrics
duration: 4min
completed: 2026-04-21
---

# Phase 11 Plan 02: Phase 11 Shared Helpers + AffiliateUrlTransformer.isAffiliateDomain

**8 pure-Kotlin helpers (7 new files + 1 edit) ship all SCR-08/09/10 logic; 51 Wave 0 tests flip GREEN; Plans 03/04/05 unblocked to run in parallel**

## Performance

- **Duration:** ~4 min
- **Started:** 2026-04-21T20:20:00Z
- **Completed:** 2026-04-21T20:24:01Z
- **Tasks:** 2 (executed as single commit — compiler constraint)
- **Files modified:** 8

## Accomplishments
- Added `AffiliateUrlTransformer.isAffiliateDomain(url: String): Boolean` — the only edit to existing production code; backward-compatible, `merchantRules` stays private
- Shipped 4 SCR-08 detail helpers: FilterChipState enum, heroToolbarAlpha() with Pitfall 1 guard, RegistryStats + registryStatsOf(), shareUrlOf()
- Shipped SCR-09 OccasionCatalog object with 6 occasions in fixed handoff order + legacy alias map (`"Baby shower"→"Baby"`, `"Anniversary"→"Housewarming"`)
- Shipped SCR-10 AddItemMode enum + ADD_ITEM_MODE_DEFAULT_ORDINAL const + shouldShowAffiliateRow() predicate
- All 51 Wave 0 @Test methods GREEN; full unit suite GREEN; no Phase 3-10 regressions

## Task Commits

Both tasks committed atomically (Gradle compiles all test packages in a single step — Task 1 tests cannot compile without Task 2 symbols):

1. **Tasks 1+2: All 8 helpers** - `9b6da52` (feat)

**Plan metadata:** _(docs commit follows)_

## Files Created/Modified
- `app/src/main/java/com/giftregistry/util/AffiliateUrlTransformer.kt` — Added `isAffiliateDomain()` between `extractDomain()` and `noMatch()`
- `app/src/main/java/com/giftregistry/ui/registry/detail/FilterChipState.kt` — FilterChipState enum (All/Open/Reserved/Completed) + matches() extension
- `app/src/main/java/com/giftregistry/ui/registry/detail/HeroToolbarAlpha.kt` — heroToolbarAlpha() with firstVisibleItemIndex>=1→1f Pitfall 1 guard
- `app/src/main/java/com/giftregistry/ui/registry/detail/RegistryStats.kt` — RegistryStats data class + registryStatsOf() aggregation (views=0 TODO v1.2)
- `app/src/main/java/com/giftregistry/ui/registry/detail/ShareUrl.kt` — shareUrlOf() returning `https://gift-registry-ro.web.app/r/{id}`
- `app/src/main/java/com/giftregistry/ui/registry/create/OccasionCatalog.kt` — OccasionCatalog singleton, 6 entries, glyphFor(), storageKeyFor(), legacyAliases map
- `app/src/main/java/com/giftregistry/ui/item/add/AddItemMode.kt` — AddItemMode enum (PasteUrl/BrowseStores/Manual) + ADD_ITEM_MODE_DEFAULT_ORDINAL=0
- `app/src/main/java/com/giftregistry/ui/item/add/AffiliateRowVisibility.kt` — shouldShowAffiliateRow() predicate (url non-blank AND ogFetchSucceeded AND isAffiliateDomain)

## Decisions Made
- Both tasks committed in a single atomic commit: Gradle compiles all test packages together in `:app:compileDebugUnitTestKotlin`, so OccasionCatalog/AddItemMode/AffiliateRowVisibility must exist before the Task 1 test run can compile. This is a compiler constraint, not a deviation — plan notes it as TDD with wave-level interdependence.
- `isAffiliateDomain` inserted between `extractDomain()` and `noMatch()` — natural placement next to the method it mirrors; no structural change to the object.
- OccasionCatalog legacy aliases use lowercase keys (`"baby shower"`, `"anniversary"`) so `storageKeyFor()` normalises input via `.lowercase()` before lookup — handles any casing variant from legacy Firestore docs.

## Deviations from Plan

None — plan executed exactly as written. The single-commit approach for tasks 1+2 is a pragmatic adaptation to the Kotlin compilation constraint (all test packages compile together), not a deviation from the plan's intent.

## Issues Encountered

- Initial Task 1 test run failed to compile because OccasionCatalogTest (a Task 2 test file) referenced `OccasionCatalog` which didn't exist yet. Resolution: created all Task 2 files before running the combined test run — as expected by the wave design.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

Plans 03, 04, and 05 can now run in parallel. Each touches a single screen:
- Plan 03: RegistryDetailScreen (imports FilterChipState, heroToolbarAlpha, RegistryStats, shareUrlOf, OccasionCatalog.glyphFor)
- Plan 04: CreateRegistryScreen (imports OccasionCatalog.entries + storageKeyFor)
- Plan 05: AddItemScreen (imports AddItemMode, ADD_ITEM_MODE_DEFAULT_ORDINAL, shouldShowAffiliateRow, AffiliateUrlTransformer.isAffiliateDomain)

Public API exposed for Plans 03/04/05:
- `AffiliateUrlTransformer.isAffiliateDomain(url: String): Boolean`
- `enum class FilterChipState { All, Open, Reserved, Completed }` + `fun FilterChipState.matches(status: ItemStatus): Boolean`
- `fun heroToolbarAlpha(firstVisibleItemIndex: Int, firstVisibleItemScrollOffsetPx: Int, heroThresholdPx: Float): Float`
- `data class RegistryStats(items, reserved, given, views)` + `fun registryStatsOf(items: List<Item>): RegistryStats`
- `fun shareUrlOf(registryId: String): String`
- `object OccasionCatalog { val entries; fun glyphFor(String?); fun storageKeyFor(String?) }`
- `enum class AddItemMode { PasteUrl, BrowseStores, Manual }` + `const val ADD_ITEM_MODE_DEFAULT_ORDINAL = 0`
- `fun shouldShowAffiliateRow(url: String?, isAffiliateDomain: Boolean, ogFetchSucceeded: Boolean): Boolean`

---
*Phase: 11-registry-detail-create-add-item-redesign*
*Completed: 2026-04-21*
