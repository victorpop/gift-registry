---
phase: 11-registry-detail-create-add-item-redesign
plan: 03
subsystem: ui
tags: [kotlin, jetpack-compose, registry-detail, giftmaison, redesign, scr-08]

# Dependency graph
requires:
  - phase: 11-02
    provides: FilterChipState, heroToolbarAlpha, registryStatsOf, shareUrlOf, OccasionCatalog.glyphFor

provides:
  - RegistryDetailHero composable — 180 dp hero with AsyncImage/placeholder + pinned toolbar alpha-fade
  - StatsStrip composable — 4-stat strip with vertical dividers
  - ShareBanner composable — accentSoft pill with clipboard copy + share intent
  - FilterChipsRow composable — horizontally scrolling 4-chip filter row
  - RegistryItemRow composable — 58×58 thumbnail + StatusChip + always-visible overflow
  - RegistryDetailScreen re-skin — Box + LazyColumn wiring all 5 sub-composables

affects: [11-06]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Box-over-LazyColumn pattern (UI-SPEC Pitfall 4): Scaffold removed; SnackbarHost placed manually inside Box"
    - "heroToolbarAlpha Pitfall 1 guard: firstVisibleItemIndex >= 1 short-circuits to 1f"
    - "Pitfall 2 guard: LocalDensity captured via remember(density) outside derivedStateOf lambda"
    - "ShareBanner onShared callback: scope.launch required because showSnackbar is a suspend function"
    - "FilterChip animated state: animateColorAsState bg + textColor for 150 ms crossfade"
    - "RegistryItemRow drawBehind bottom border skips isLast row"

key-files:
  created:
    - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailHero.kt
    - app/src/main/java/com/giftregistry/ui/registry/detail/StatsStrip.kt
    - app/src/main/java/com/giftregistry/ui/registry/detail/ShareBanner.kt
    - app/src/main/java/com/giftregistry/ui/registry/detail/FilterChipsRow.kt
    - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryItemRow.kt
  modified:
    - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt
    - app/src/main/res/values/strings.xml
    - app/src/main/res/values-ro/strings.xml

key-decisions:
  - "ShareBanner.onShared is a non-suspend lambda; caller (RegistryDetailScreen) wraps with scope.launch to call showSnackbar — Rule 1 auto-fix applied during compile"
  - "RegistryItemRow uses MaterialTheme.colorScheme.error for destructive delete text (consistent with existing DropdownMenu pattern in the file being replaced)"
  - "Toolbar icon tint adapts: ink when toolbarAlpha > 0.5f (paper bg showing), paper when < 0.5f (hero visible) — improves contrast vs fixed ink tint"

patterns-established:
  - "Phase 11 SCR-08 5-composable split pattern: RegistryDetailScreen is a thin wiring shell; each visual section is a separate internal composable"

requirements-completed: [SCR-08]

# Metrics
duration: 8min
completed: 2026-04-21
---

# Phase 11 Plan 03: SCR-08 Registry Detail Re-skin

**5 new sub-composables + RegistryDetailScreen rewritten Scaffold→Box; 13 new string keys (EN + RO); legacy per-screen FAB removed; full unit suite GREEN**

## Performance

- **Duration:** ~8 min
- **Started:** 2026-04-21T20:27:29Z
- **Completed:** 2026-04-21T20:35:00Z
- **Tasks:** 2
- **Files modified:** 8

## Accomplishments

- Created 5 new `internal` Compose files under `ui/registry/detail/`:
  - `RegistryDetailHero.kt` — 180 dp hero (Coil 3 AsyncImage or accentSoft→accent placeholder with occasion glyph) + pinned toolbar with `heroToolbarAlpha` Pitfall 1+2 guards
  - `StatsStrip.kt` — 4-stat strip (items/reserved/given/views) with 1 dp `colors.line` vertical dividers, stats derived from `registryStatsOf(items)`
  - `ShareBanner.kt` — accentSoft 10-radius pill with clipboard copy + `Intent.ACTION_SEND` chooser, snackbar via `onShared` callback
  - `FilterChipsRow.kt` — `LazyRow` of 4 `FilterChipState` chips with animated ink-fill active state and inline counts
  - `RegistryItemRow.kt` — 58×58 10-radius thumbnail + title/price column + `StatusChip` + always-visible 26×26 pill overflow ⋯ button; `drawBehind` bottom border skips last item
- Rewrote `RegistryDetailScreen.kt`: `Scaffold`→`Box`, legacy per-screen FAB removed (Phase 9 global FAB covers add-item), `TopAppBar`→pinned toolbar inside `RegistryDetailHero`, inline `ItemCard`/`RegistryInfoSection`→5 new sub-composables
- Added 13 new string keys to both `values/strings.xml` and `values-ro/strings.xml`: `registry_detail_share_button_desc`, `registry_detail_overflow_desc`, `registry_detail_item_overflow_desc`, `registry_stat_items`, `registry_stat_reserved`, `registry_stat_given`, `registry_stat_views`, `registry_share_link_copied`, `registry_share_helper`, `registry_filter_all`, `registry_filter_open`, `registry_filter_reserved`, `registry_filter_completed`
- All Phase 4/6/9 behaviour preserved: reservationEvents dispatch, GuestIdentitySheet, ConfirmPurchaseBanner, snackbarMessages (Resource + Push with onNavigateToRegistry), delete AlertDialogs, DropdownMenu (Edit/Share/Invite/Delete)
- `RegistryDetailViewModel`, `AppNavigation.kt`, and all domain files untouched

## Task Commits

1. **Tasks 1 + 2: All SCR-08 files** — `f8e0e4b` (feat(11-03))

## Files Created/Modified

- `app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailHero.kt` — NEW: 180 dp hero + pinned toolbar
- `app/src/main/java/com/giftregistry/ui/registry/detail/StatsStrip.kt` — NEW: 4-stat strip
- `app/src/main/java/com/giftregistry/ui/registry/detail/ShareBanner.kt` — NEW: share pill
- `app/src/main/java/com/giftregistry/ui/registry/detail/FilterChipsRow.kt` — NEW: 4-chip filter row
- `app/src/main/java/com/giftregistry/ui/registry/detail/RegistryItemRow.kt` — NEW: item row with overflow
- `app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt` — REWRITTEN: 588→382 lines
- `app/src/main/res/values/strings.xml` — 13 new Phase 11 SCR-08 EN keys added
- `app/src/main/res/values-ro/strings.xml` — 13 new Phase 11 SCR-08 RO keys added

## Decisions Made

- `ShareBanner.onShared` is a non-suspend lambda but callers need to invoke `showSnackbar` (suspend). Caller wraps with `scope.launch { snackbarHostState.showSnackbar(...) }` — consistent with the existing `onShareTap` closure in RegistryDetailScreen. (Rule 1 auto-fix: compile error caught and corrected.)
- Toolbar icon tint uses conditional: `if (toolbarAlpha > 0.5f) colors.ink else colors.paper` — avoids invisible back arrow when hero is visible (pure white bg shows behind black icon when alpha is 0).
- `RegistryItemRow` uses `MaterialTheme.colorScheme.error` for the destructive delete label — matches the existing `DropdownMenuItem` pattern used in the file being replaced; no new colour literal introduced.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] showSnackbar suspend call outside coroutine in ShareBanner onShared**

- **Found during:** Task 2 compile
- **Issue:** ShareBanner.onShared lambda called `snackbarHostState.showSnackbar(linkCopiedMsg)` directly — suspend function requires coroutine context
- **Fix:** Wrapped with `scope.launch { snackbarHostState.showSnackbar(linkCopiedMsg) }` in the lambda passed from RegistryDetailScreen
- **Files modified:** `RegistryDetailScreen.kt` (onShared lambda)
- **Commit:** f8e0e4b

## Known Stubs

- `views` stat in StatsStrip always renders `"0"` — `Registry.viewCount` field deferred to v1.2 per CONTEXT.md (stub is intentional and documented)
- Item row sub-line ("Reserved by X" / "Given by Y") omitted — reserver/giver name not in `Item` domain model; deferred to v1.2 per plan spec

## Self-Check

Files created:
- [x] `app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailHero.kt` — FOUND
- [x] `app/src/main/java/com/giftregistry/ui/registry/detail/StatsStrip.kt` — FOUND
- [x] `app/src/main/java/com/giftregistry/ui/registry/detail/ShareBanner.kt` — FOUND
- [x] `app/src/main/java/com/giftregistry/ui/registry/detail/FilterChipsRow.kt` — FOUND
- [x] `app/src/main/java/com/giftregistry/ui/registry/detail/RegistryItemRow.kt` — FOUND

Commit exists:
- [x] f8e0e4b — FOUND

Build checks:
- [x] `./gradlew :app:compileDebugKotlin` — BUILD SUCCESSFUL
- [x] `./gradlew :app:assembleDebug` — BUILD SUCCESSFUL
- [x] `./gradlew :app:testDebugUnitTest` — BUILD SUCCESSFUL (all Phase 3-11 Wave 0 tests GREEN)
- [x] No Scaffold, FloatingActionButton, or TopAppBar in RegistryDetailScreen.kt
- [x] 13 EN + 13 RO string keys added

## Self-Check: PASSED

---
*Phase: 11-registry-detail-create-add-item-redesign*
*Completed: 2026-04-21*
