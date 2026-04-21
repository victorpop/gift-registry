---
phase: 09-shared-chrome-status-ui
plan: 01
subsystem: ui
tags: [jetpack-compose, junit4, tdd, red-stubs, status-chips, bottom-nav, wave-0]

# Dependency graph
requires:
  - phase: 08-giftmaison-design-foundation
    provides: GiftMaisonColors/Typography/Shapes/Shadows tokens — consumed by status chip and chrome composables in Plans 02/03
provides:
  - 5 Wave 0 RED unit test stubs pinning the Phase 9 API surface contract
  - CHROME-01 visibility predicate test (14 assertions across all nav keys)
  - STAT-01 countdown logic test (6 boundary-case assertions for computeMinutesLeft)
  - STAT-01 animation spec test (6 assertions for PulsingDot constants)
  - STAT-01/02/03 dispatcher routing test (4 assertions including asymmetric PURCHASED→GIVEN mapping)
  - STAT-04 row alpha modifier test (3 assertions including regression guards)
affects: [09-02-status-pkg, 09-03-chrome-pkg]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Wave 0 RED TDD: pure-Kotlin JUnit 4 unit tests created before implementation exists; fail with unresolved-reference errors until Plans 02/03 ship named symbols"
    - "Public const val pattern: Plan 02 MUST expose PULSING_DOT_DEFAULT_PERIOD_MS, PULSING_DOT_ALPHA_START/END, PULSING_DOT_SCALE_START/END as top-level const vals in PulsingDot.kt to keep unit tests Compose-framework-free"
    - "StatusChipType enum pattern: Plan 02 MUST expose StatusChipType { OPEN, RESERVED, GIVEN } + statusChipTypeOf(ItemStatus) top-level fn in StatusChip.kt to allow pure-Kotlin dispatch test"

key-files:
  created:
    - app/src/test/java/com/giftregistry/ui/common/chrome/BottomNavVisibilityTest.kt
    - app/src/test/java/com/giftregistry/ui/common/status/ReservedChipTest.kt
    - app/src/test/java/com/giftregistry/ui/common/status/PulsingDotTest.kt
    - app/src/test/java/com/giftregistry/ui/common/status/StatusChipDispatcherTest.kt
    - app/src/test/java/com/giftregistry/ui/common/status/PurchasedRowModifierTest.kt
  modified: []

key-decisions:
  - "Wave 0 pure-Kotlin unit tests only: Compose UI test scaffolding (androidTest dir + ui-test-junit4 dep) deferred — unit tests cover the logic contract; visual verification happens via StyleGuidePreview sections added in Plan 04"
  - "PulsingDot constants exposed as top-level const vals: required for Compose-framework-free unit testing; Plan 02 contract is to declare these in PulsingDot.kt file scope"
  - "StatusChipType as top-level pure-Kotlin enum + statusChipTypeOf() function: allows dispatcher routing test without invoking @Composable; Plan 02 must implement both"
  - "Asymmetric domain→display mapping pinned: ItemStatus.AVAILABLE→OPEN, ItemStatus.RESERVED→RESERVED, ItemStatus.PURCHASED→GIVEN (the PURCHASED→GIVEN rename is the highest-risk pitfall from RESEARCH.md Pitfall 6)"

patterns-established:
  - "Pattern: Wave 0 RED stubs reference exact public API surface — test file package matches the implementation package so Plan 02/03 symbols are found by compiler automatically when placed in correct package"
  - "Pattern: PurchasedRowModifier test uses const val PURCHASED_ROW_ALPHA (not Modifier.toString() inspection) — more stable, less brittle, still covers the spec"

requirements-completed: [CHROME-01, STAT-01, STAT-02, STAT-03, STAT-04]

# Metrics
duration: 2min
completed: 2026-04-21
---

# Phase 9 Plan 01: Wave 0 RED Unit Test Stubs Summary

**5 pure-Kotlin JUnit 4 RED stubs locking the Phase 9 API surface: showsBottomNav() predicate, computeMinutesLeft() countdown, PulsingDot animation constants, StatusChipType dispatcher, and PURCHASED_ROW_ALPHA modifier**

## Performance

- **Duration:** 2 min
- **Started:** 2026-04-21T09:25:02Z
- **Completed:** 2026-04-21T09:27:19Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments

- Shipped 5 Wave 0 RED unit test files that compile-fail with unresolved-reference errors confirming the tests are genuinely RED (not vacuously passing)
- Pinned the critical asymmetric domain→display rename: `ItemStatus.PURCHASED → StatusChipType.GIVEN` — this is the highest-risk API contract per RESEARCH.md Pitfall 6, now locked by test
- Pinned `computeMinutesLeft(Long?, Long)` contract with 6 boundary cases including null-safety, integer truncation, 30-minute reservation window, and clamped-at-zero expiry
- Pinned PulsingDot animation constants (period=1400ms, alpha 1f↔0.5f, scale 1f↔0.85f) as top-level `const val` contract for Plan 02
- Pinned CHROME-01 nav visibility rule across all 14 nav keys (2 visible, 12 hidden, plus null key)

## Task Commits

1. **Task 1: CHROME-01 + STAT-01 countdown/pulse RED stubs (3 files)** - `13970aa` (test)
2. **Task 2: STAT-01/02/03/04 dispatcher + modifier RED stubs (2 files)** - `3de6865` (test)

## Files Created/Modified

- `app/src/test/java/com/giftregistry/ui/common/chrome/BottomNavVisibilityTest.kt` — 14 @Test methods for showsBottomNav() predicate across all AppNavKeys (RED: `showsBottomNav` unresolved)
- `app/src/test/java/com/giftregistry/ui/common/status/ReservedChipTest.kt` — 6 @Test methods for computeMinutesLeft(Long?, Long) boundary cases (RED: `computeMinutesLeft` unresolved)
- `app/src/test/java/com/giftregistry/ui/common/status/PulsingDotTest.kt` — 6 @Test methods for PULSING_DOT_* const val animation parameters (RED: all consts unresolved)
- `app/src/test/java/com/giftregistry/ui/common/status/StatusChipDispatcherTest.kt` — 4 @Test methods for statusChipTypeOf(ItemStatus) dispatch mapping (RED: StatusChipType + statusChipTypeOf unresolved)
- `app/src/test/java/com/giftregistry/ui/common/status/PurchasedRowModifierTest.kt` — 3 @Test methods for PURCHASED_ROW_ALPHA=0.55f with regression guards (RED: PURCHASED_ROW_ALPHA unresolved)

## Decisions Made

- Used `const val` pattern for PulsingDot constants instead of testing via Compose runtime — eliminates Compose test framework dependency and makes tests runnable in pure JVM unit test mode
- Used `StatusChipType` pure-Kotlin enum + `statusChipTypeOf()` top-level function instead of testing `@Composable StatusChip` directly — same reason
- Deferred androidTest scaffolding (Compose UI tests for CHROME-02/03 and STAT-02/03) per RESEARCH.md pragmatic note — unit tests cover all testable logic; visual verification handled by StyleGuidePreview in Plan 04

## Public API Surface Plans 02/03 MUST Expose

To flip these tests from RED to GREEN, Plan 02 (status package) and Plan 03 (chrome package) must expose the following symbols:

### Plan 02 — `com.giftregistry.ui.common.status`

| Symbol | File | Type |
|--------|------|------|
| `fun computeMinutesLeft(expiresAt: Long?, now: Long = System.currentTimeMillis()): Int` | StatusChip.kt or ReservedChip.kt | top-level function |
| `const val PULSING_DOT_DEFAULT_PERIOD_MS: Long = 1_400L` | PulsingDot.kt | top-level const |
| `const val PULSING_DOT_ALPHA_START: Float = 1f` | PulsingDot.kt | top-level const |
| `const val PULSING_DOT_ALPHA_END: Float = 0.5f` | PulsingDot.kt | top-level const |
| `const val PULSING_DOT_SCALE_START: Float = 1f` | PulsingDot.kt | top-level const |
| `const val PULSING_DOT_SCALE_END: Float = 0.85f` | PulsingDot.kt | top-level const |
| `enum class StatusChipType { OPEN, RESERVED, GIVEN }` | StatusChip.kt | top-level enum |
| `fun statusChipTypeOf(status: ItemStatus): StatusChipType` | StatusChip.kt | top-level function |
| `const val PURCHASED_ROW_ALPHA: Float = 0.55f` | PurchasedRowModifier.kt | top-level const |
| `fun Modifier.purchasedVisualTreatment(): Modifier` | PurchasedRowModifier.kt | extension function |

### Plan 03 — `com.giftregistry.ui.common.chrome`

| Symbol | File | Type |
|--------|------|------|
| `fun Any?.showsBottomNav(): Boolean` | GiftMaisonBottomNav.kt (or NavVisibility.kt) | top-level extension function — must return true ONLY for `HomeKey` and `RegistryDetailKey` |

## Deviations from Plan

None — plan executed exactly as written.

## Issues Encountered

None — compilation failures were expected and confirm correct RED state.

## Known Stubs

None — plan 01 is entirely test stubs. No implementation code was written.

## Next Phase Readiness

- Wave 0 complete: all 5 RED unit test files exist
- Plan 02 can begin immediately: `ui/common/status/` implementation (StatusChip.kt, PulsingDot.kt, PurchasedRowModifier.kt) — flips 5 of 5 files GREEN
- Plan 03 follows: `ui/common/chrome/` implementation — flips BottomNavVisibilityTest.kt GREEN
- Total: 33 @Test methods across 5 files, all currently failing with unresolved-reference errors

---
*Phase: 09-shared-chrome-status-ui*
*Completed: 2026-04-21*
