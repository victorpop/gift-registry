---
phase: 09-shared-chrome-status-ui
plan: 02
subsystem: ui
tags: [jetpack-compose, junit4, tdd, wave-1, status-chips, pulsing-dot, purchased-row]

# Dependency graph
requires:
  - phase: 08-giftmaison-design-foundation
    provides: GiftMaisonColors/Typography/Shapes tokens — consumed by all 3 new composables
  - phase: 09-01
    provides: Wave 0 RED test stubs locking the public API surface Plan 02 must satisfy
provides:
  - PulsingDot.kt — reusable infinite-animation dot + 5 public animation const vals
  - StatusChip.kt — StatusChip dispatcher + ReservedChip/GivenChip/OpenChip + computeMinutesLeft + statusChipTypeOf
  - PurchasedRowModifier.kt — PURCHASED_ROW_ALPHA=0.55f + Modifier.purchasedVisualTreatment()
  - NavVisibility.kt stub — showsBottomNav() function unblocking BottomNavVisibilityTest compilation (Plan 03 will expand into GiftMaisonBottomNav)
affects: [09-03-chrome-pkg, 09-04-registry-detail-rewire, phase-11-add-item-url-field]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "PulsingDot uses rememberInfiniteTransition + tween(halfPeriod, FastOutSlowInEasing) + RepeatMode.Reverse — half-period split ensures one full alpha/scale cycle = [period]"
    - "computeMinutesLeft() is pure Kotlin (no Compose dependency) — enables unit testing without Compose runtime"
    - "LaunchedEffect(expiresAt) + delay(60_000L) countdown — minute-cadence, no ViewModel ticker, display-only per CONTEXT.md decision D-06"
    - "statusChipTypeOf() top-level pure Kotlin function — authoritative AVAILABLE→OPEN/PURCHASED→GIVEN asymmetric rename translator"
    - "NavVisibility.kt stub pattern — created before GiftMaisonBottomNav.kt to unblock Wave 0 chrome test compilation without implementing full chrome package"

key-files:
  created:
    - app/src/main/java/com/giftregistry/ui/common/status/PulsingDot.kt
    - app/src/main/java/com/giftregistry/ui/common/status/StatusChip.kt
    - app/src/main/java/com/giftregistry/ui/common/status/PurchasedRowModifier.kt
    - app/src/main/java/com/giftregistry/ui/common/chrome/NavVisibility.kt
  modified:
    - app/src/main/res/values/strings.xml
    - app/src/main/res/values-ro/strings.xml

key-decisions:
  - "NavVisibility.kt stub created in Plan 02 (not Plan 03): all unit tests compile in a single step; BottomNavVisibilityTest.kt blocked compilation of status tests until showsBottomNav() existed; stub implements the correct logic so Plan 03 can build on it"
  - "Text import at top-level (not fully qualified): StatusChip.kt uses top-level import for androidx.compose.material3.Text; plan specified FQN but top-level import is idiomatic Kotlin and equivalent"
  - "No MaterialTheme.colorScheme usage: all colour tokens consumed via GiftMaisonTheme.colors.* enforcing Phase 8 design system throughout"

patterns-established:
  - "Pattern: Wave 1 GREEN implementation — create the implementation package, expose exact public API surface locked by Wave 0 tests, run tests to confirm RED→GREEN flip"
  - "Pattern: Cross-package compile unblocking — when Wave 0 RED tests in package B reference symbols from package A that isn't being implemented yet, create a minimal correct stub in A to unblock B's compilation"

requirements-completed: [STAT-01, STAT-02, STAT-03, STAT-04]

# Metrics
duration: 3min
completed: 2026-04-21
---

# Phase 9 Plan 02: ui/common/status/ — Wave 1 Implementation Summary

**PulsingDot + StatusChip dispatcher + PurchasedRowModifier — flips all 4 Wave 0 status test classes (19 @Test methods) from RED to GREEN using Phase 8 GiftMaison design tokens**

## Performance

- **Duration:** 3 min
- **Started:** 2026-04-21T09:29:52Z
- **Completed:** 2026-04-21
- **Tasks:** 2
- **Files modified:** 6 (4 created, 2 modified)

## Accomplishments

- Created `ui/common/status/` package with 3 Kotlin files implementing all STAT-01..04 requirements
- Flipped 4 Wave 0 test classes RED→GREEN: PulsingDotTest (6/6), ReservedChipTest (6/6), StatusChipDispatcherTest (4/4), PurchasedRowModifierTest (3/3) = 19/19 total
- Enforced asymmetric domain→display mapping: `ItemStatus.PURCHASED → StatusChipType.GIVEN` (highest-risk rename locked by test)
- Added 5 status chip string keys to both EN (`values/strings.xml`) and RO (`values-ro/strings.xml`) locales = 10 new string entries
- Created `NavVisibility.kt` stub in `ui/common/chrome/` — implements `showsBottomNav()` correctly to unblock BottomNavVisibilityTest compilation (bonus: CHROME-01 also goes GREEN)
- Full unit suite passes with 0 regressions (`./gradlew :app:testDebugUnitTest` BUILD SUCCESSFUL)

## Task Commits

1. **Task 1: PulsingDot.kt + 5 status string keys (EN + RO)** — `5ca3240`
2. **Task 2: StatusChip.kt + PurchasedRowModifier.kt (Wave 0 RED→GREEN)** — `ac35ee9`

## Files Created/Modified

### Created

| File | Purpose | LOC |
|------|---------|-----|
| `app/src/main/java/com/giftregistry/ui/common/status/PulsingDot.kt` | 5 animation const vals + `PulsingDot` @Composable | ~80 |
| `app/src/main/java/com/giftregistry/ui/common/status/StatusChip.kt` | `StatusChipType` enum, `statusChipTypeOf()`, `computeMinutesLeft()`, `StatusChip`/`ReservedChip`/`GivenChip`/`OpenChip` | ~130 |
| `app/src/main/java/com/giftregistry/ui/common/status/PurchasedRowModifier.kt` | `PURCHASED_ROW_ALPHA=0.55f`, `Modifier.purchasedVisualTreatment()` | ~35 |
| `app/src/main/java/com/giftregistry/ui/common/chrome/NavVisibility.kt` | `Any?.showsBottomNav()` stub (Rule 3 deviation) | ~20 |

### Modified

| File | Change |
|------|--------|
| `app/src/main/res/values/strings.xml` | +5 status chip keys: `status_chip_reserved/given/open/countdown_template/countdown_zero` |
| `app/src/main/res/values-ro/strings.xml` | +5 Romanian equivalents: REZERVAT/OFERIT/DISPONIBIL/%1$d min/<1 min |

## Wave 0 Test Flip Count

| Test Class | Methods | Before | After |
|-----------|---------|--------|-------|
| PulsingDotTest | 6 | RED | GREEN |
| ReservedChipTest | 6 | RED | GREEN |
| StatusChipDispatcherTest | 4 | RED | GREEN |
| PurchasedRowModifierTest | 3 | RED | GREEN |
| **Total** | **19** | **RED** | **GREEN** |

Additionally: BottomNavVisibilityTest (14 methods) also flipped GREEN via the NavVisibility.kt stub — this is Plan 03's requirement, but the stub implements the correct logic.

## Token Consumption Verified

- `GiftMaisonTheme.colors.*` used throughout StatusChip.kt (3 occurrences — ReservedChip, GivenChip, OpenChip each access `colors`)
- `GiftMaisonTheme.typography.*` and `GiftMaisonTheme.shapes.*` consumed in all 3 chip composables
- **0 hex literals** (`0xFF...`) in any of the 3 new status files
- **0 `MaterialTheme.colorScheme` references** in new files (Phase 8 tokens only)

## String Resource Additions

7 status chip string keys × 2 locales = 14 new string entries (plan stated 7 keys but the EN string `status_chip_given` uses `✓ GIVEN` — only 5 status chip keys were added per the plan spec):

| Key | English | Romanian |
|-----|---------|---------|
| `status_chip_reserved` | RESERVED | REZERVAT |
| `status_chip_given` | ✓ GIVEN | ✓ OFERIT |
| `status_chip_open` | OPEN | DISPONIBIL |
| `status_chip_countdown_template` | %1$dm | %1$d min |
| `status_chip_countdown_zero` | &lt;1m | &lt;1 min |

Note: Plan spec said "7 keys" but the Copywriting Contract in UI-SPEC lists 5 status chip keys (status_chip_*). The 7 mentioned in the plan likely counted the nav tab keys that are Plan 03's responsibility. All 5 status chip keys are present in both locales.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] NavVisibility.kt stub created to unblock status test compilation**

- **Found during:** Task 1 verification — `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.common.status.*"` failed because Gradle compiles ALL unit tests before running any subset. `BottomNavVisibilityTest.kt` referenced `showsBottomNav()` which was unresolved.
- **Fix:** Created `app/src/main/java/com/giftregistry/ui/common/chrome/NavVisibility.kt` implementing `Any?.showsBottomNav()` with the correct `HomeKey`/`RegistryDetailKey` → true, everything else → false logic
- **Impact:** Bonus — BottomNavVisibilityTest (14 methods, Plan 03 requirement) also flips GREEN. Plan 03 will expand this stub into the full `GiftMaisonBottomNav.kt` composable.
- **Files modified:** `app/src/main/java/com/giftregistry/ui/common/chrome/NavVisibility.kt` (new)
- **Commit:** `ac35ee9`

## Known Follow-ups

- **Plan 04** (RegistryDetailScreen rewire): Delete inline `ItemStatusChip` + `ReservationCountdown.kt` and replace with `StatusChip(item.status, item.expiresAt)` calls
- **Plan 03** (Chrome package): Expand `NavVisibility.kt` stub into `GiftMaisonBottomNav.kt` composable with the full bottom nav implementation
- **Phase 11** (Add-item URL field): Consume `PulsingDot(color = colors.accent, period = 1000.milliseconds)` for the "Fetching from…" indicator

## Known Stubs

None — all exported symbols are fully implemented. The `NavVisibility.kt` file is a complete correct implementation (not a stub returning hardcoded values), so it does not qualify as a stub.

## Self-Check

- [x] PulsingDot.kt exists at correct path
- [x] StatusChip.kt exists at correct path
- [x] PurchasedRowModifier.kt exists at correct path
- [x] NavVisibility.kt exists at correct path
- [x] Task 1 commit `5ca3240` exists
- [x] Task 2 commit `ac35ee9` exists
- [x] `./gradlew :app:testDebugUnitTest` BUILD SUCCESSFUL (0 failures)
- [x] 19/19 Wave 0 status @Test methods GREEN

## Self-Check: PASSED

---
*Phase: 09-shared-chrome-status-ui*
*Completed: 2026-04-21*
