---
phase: 11-registry-detail-create-add-item-redesign
plan: 01
subsystem: testing
tags: [junit4, tdd, red-stubs, kotlin, wave-0, registry-detail, create-registry, add-item]

# Dependency graph
requires: []
provides:
  - 7 Wave 0 RED JUnit 4 test files pinning the Phase 11 public API contract
  - SCR-08 FilterChipState enum + matches() predicate (5 tests)
  - SCR-08 heroToolbarAlpha Pitfall 1 guard (7 tests)
  - SCR-08 registryStatsOf derivation + views=0 guard (6 tests)
  - SCR-08 shareUrlOf format https://gift-registry-ro.web.app/r/{id} (5 tests)
  - SCR-09 OccasionCatalog 6 entries + glyph map + legacy aliases (17 tests)
  - SCR-10 AddItemMode enum + ADD_ITEM_MODE_DEFAULT_ORDINAL=0 (5 tests)
  - SCR-10 shouldShowAffiliateRow predicate (6 tests)
affects:
  - 11-02 (Plan 02 ships the symbols to flip these RED tests GREEN)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Wave 0 TDD RED: pure-Kotlin JUnit 4 stubs reference future public API; compile-fail at Wave 0 is the expected RED state; Plan 02 ships implementation to flip GREEN"
    - "Test package mirrors impl package: test in com.giftregistry.ui.registry.detail references symbols Plan 02 ships in same package — no FQN import required"

key-files:
  created:
    - app/src/test/java/com/giftregistry/ui/registry/detail/FilterChipStateTest.kt
    - app/src/test/java/com/giftregistry/ui/registry/detail/HeroToolbarAlphaTest.kt
    - app/src/test/java/com/giftregistry/ui/registry/detail/RegistryStatsTest.kt
    - app/src/test/java/com/giftregistry/ui/registry/detail/ShareUrlTest.kt
    - app/src/test/java/com/giftregistry/ui/registry/create/OccasionCatalogTest.kt
    - app/src/test/java/com/giftregistry/ui/item/add/AddItemModeTest.kt
    - app/src/test/java/com/giftregistry/ui/item/add/AffiliateRowVisibilityTest.kt
  modified: []

key-decisions:
  - "Wave 0 test package = impl package: tests in com.giftregistry.ui.registry.detail etc. so unqualified symbol references work once Plan 02 ships"
  - "51 total @Test methods across 7 files covering all Phase 11 SCR-08/09/10 pure-Kotlin contracts"
  - "HeroToolbarAlpha Pitfall 1 guard pinned: firstVisibleItemIndex >= 1 must short-circuit to 1f to prevent toolbar flash-to-transparent when hero scrolls off-screen"
  - "views=0 regression guard in RegistryStatsTest: 3 assertions all assert views==0 to prevent accidental v1.2 viewCount from landing in Phase 11"
  - "Legacy alias map pinned: Baby shower -> Baby, Anniversary -> Housewarming for backward compat with pre-Phase-11 Firestore docs"
  - "ADD_ITEM_MODE_DEFAULT_ORDINAL exposed as top-level const val (not just PasteUrl.ordinal) per Phase 10 rememberSaveable Int-backed state pattern"

patterns-established:
  - "Pattern: Wave 0 RED stubs reference exact public API from plan context interfaces section; test package = impl package; no FQN imports needed"
  - "Pattern: Pitfall guard test — dedicate one @Test specifically to the known pitfall scenario with a message argument naming the pitfall (e.g., 'Pitfall 1 guard: ...')"
  - "Pattern: Legacy alias coverage in catalog tests — test both canonical and all known legacy forms with message arguments quoting the source doc"

requirements-completed: [SCR-08, SCR-09, SCR-10]

# Metrics
duration: 8min
completed: 2026-04-21
---

# Phase 11 Plan 01: Wave 0 RED Stubs Summary

**51 pure-Kotlin JUnit 4 test stubs across 7 files pin the Phase 11 contract — FilterChipState, heroToolbarAlpha (with Pitfall 1 guard), registryStatsOf, shareUrlOf, OccasionCatalog, AddItemMode, and shouldShowAffiliateRow — all compile-failing until Plan 02 ships implementations**

## Performance

- **Duration:** ~8 min
- **Started:** 2026-04-21T19:18:53Z
- **Completed:** 2026-04-21T19:27:00Z
- **Tasks:** 2
- **Files modified:** 7

## Accomplishments
- 7 Wave 0 RED JUnit 4 test files created under `app/src/test/java/com/giftregistry/ui/{registry/detail,registry/create,item/add}/`
- 51 total @Test methods confirm the exact public API surface Plan 02 must ship to flip tests GREEN
- Pitfall 1 guard (firstVisibleItemIndex >= 1 → 1f) pinned by dedicated test with message explaining the toolbar-flash regression vector
- Legacy alias map for Firestore backward compat (Baby shower → Baby, Anniversary → Housewarming) locked in OccasionCatalogTest
- 80 unresolved-reference compile errors confirmed as expected RED state

## Task Commits

1. **Task 1: SCR-08 registry-detail RED stubs** - `19a3a01` (test)
2. **Task 2: SCR-09 + SCR-10 RED stubs** - `b1ccd09` (test)

## Files Created/Modified
- `app/src/test/java/com/giftregistry/ui/registry/detail/FilterChipStateTest.kt` — 5 tests for FilterChipState enum entries + matches() predicate mapping All/Open/Reserved/Completed
- `app/src/test/java/com/giftregistry/ui/registry/detail/HeroToolbarAlphaTest.kt` — 7 tests for heroToolbarAlpha; test 5 specifically pins the Pitfall 1 firstVisibleItemIndex >= 1 guard
- `app/src/test/java/com/giftregistry/ui/registry/detail/RegistryStatsTest.kt` — 6 tests for registryStatsOf; viewsAlwaysZero_untilV12 regression guard with 3 assertions
- `app/src/test/java/com/giftregistry/ui/registry/detail/ShareUrlTest.kt` — 5 tests pinning https://gift-registry-ro.web.app/r/{id} format + no URL encoding guard
- `app/src/test/java/com/giftregistry/ui/registry/create/OccasionCatalogTest.kt` — 17 tests for 6 occasions in fixed order, glyph map, case-insensitive lookup, legacy aliases
- `app/src/test/java/com/giftregistry/ui/item/add/AddItemModeTest.kt` — 5 tests for AddItemMode enum + ADD_ITEM_MODE_DEFAULT_ORDINAL=0 const val
- `app/src/test/java/com/giftregistry/ui/item/add/AffiliateRowVisibilityTest.kt` — 6 tests for shouldShowAffiliateRow three-condition AND predicate

## Decisions Made
- Test package = impl package so unqualified symbol references work in Wave 0 without FQN imports — same pattern as Phase 8-10 Wave 0 tests
- Pitfall 1 guard gets its own dedicated @Test (secondItemVisible_returnsOne_evenWithSmallOffset) with a message argument quoting the pitfall name — ensures the guard cannot regress silently
- Legacy aliases tested as separate @Test methods (not combined) to give clear failure messages when a specific alias mapping breaks

## Public API Plan 02 Must Ship

For each of the 7 test files to flip GREEN, Plan 02 must implement:

| Symbol | File | Package |
|--------|------|---------|
| `enum class FilterChipState { All, Open, Reserved, Completed }` + `fun FilterChipState.matches(status: ItemStatus): Boolean` | FilterChipState.kt | ui.registry.detail |
| `fun heroToolbarAlpha(firstVisibleItemIndex: Int, firstVisibleItemScrollOffsetPx: Int, heroThresholdPx: Float): Float` | HeroToolbarAlpha.kt | ui.registry.detail |
| `data class RegistryStats(items, reserved, given, views)` + `fun registryStatsOf(items: List<Item>): RegistryStats` | RegistryStats.kt | ui.registry.detail |
| `fun shareUrlOf(registryId: String): String` | ShareUrl.kt | ui.registry.detail |
| `data class OccasionEntry(storageKey, glyph)` + `object OccasionCatalog { val entries; fun glyphFor(String?); fun storageKeyFor(String?) }` | OccasionCatalog.kt | ui.registry.create |
| `enum class AddItemMode { PasteUrl, BrowseStores, Manual }` + `const val ADD_ITEM_MODE_DEFAULT_ORDINAL = 0` | AddItemMode.kt | ui.item.add |
| `fun shouldShowAffiliateRow(url: String?, isAffiliateDomain: Boolean, ogFetchSucceeded: Boolean): Boolean` | AffiliateRowVisibility.kt | ui.item.add |
| `fun AffiliateUrlTransformer.isAffiliateDomain(url: String): Boolean` | AffiliateUrlTransformer.kt | util (existing file, new method) |

## Deviations from Plan

None — plan executed exactly as written.

## Issues Encountered

None.

## Known Stubs

None — this plan creates test stubs only. The implementations (FilterChipState.kt, HeroToolbarAlpha.kt, etc.) are intentionally absent; they ship in Plan 02.

## Next Phase Readiness
- Plan 02 can now proceed to implement all 7 symbols in the exact API shape the tests require
- `./gradlew :app:compileDebugUnitTestKotlin` fails with 80 unresolved-reference errors — this is the expected RED state
- All 7 Wave 0 test files are in the Wave 0 Requirements checklist in 11-VALIDATION.md

---
*Phase: 11-registry-detail-create-add-item-redesign*
*Completed: 2026-04-21*
