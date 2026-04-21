---
phase: 10-onboarding-home-redesign
plan: "01"
subsystem: testing
tags: [junit4, tdd, red-green, wave0, unit-tests, kotlin]

requires:
  - phase: 09-shared-chrome-status-ui
    provides: Wave 0 TDD RED pattern established (BottomNavVisibilityTest precedent)
  - phase: 08-giftmaison-design-foundation
    provides: wordmarkAnnotatedString pure-Kotlin factory pattern for AnnotatedString unit testing

provides:
  - "6 Wave 0 RED JUnit 4 unit test files pinning Phase 10 public API contract"
  - "SCR-07 tab filter predicates: startOfTodayMs (Calendar), isActive, isPast, isDraft, primaryRegistryIdOf"
  - "SCR-06+SCR-07 avatar initials: toAvatarInitials(displayName, email) 3-tier fallback chain"
  - "SCR-06 auth headline: authHeadlineAnnotatedString ink/inkSoft/accent 3-colour split + trailing period"
  - "SCR-06 AuthFormState contract: firstName + lastName fields, AUTH_SCREEN_DEFAULT_IS_SIGN_UP_MODE=true"

affects:
  - 10-02-PLAN (Plan 02 must ship TabFilters.kt + AvatarInitials.kt to flip 28 RED tests GREEN)
  - 10-03-PLAN (Plan 03 must ship AuthHeadline.kt + extend AuthFormState to flip 12 RED tests GREEN)

tech-stack:
  added: []
  patterns:
    - "Wave 0 TDD RED pattern: 6 failing test files written before any implementation, referencing symbols from future plans"
    - "Pure-JVM JUnit 4 test pattern: no MockK, no Robolectric, no Compose test runtime in wave-0 stubs"
    - "AnnotatedString unit testing without Compose UI runtime: androidx.compose.ui.graphics.Color + spanStyles inspection"

key-files:
  created:
    - app/src/test/java/com/giftregistry/ui/registry/list/TabFilterPredicateTest.kt
    - app/src/test/java/com/giftregistry/ui/registry/list/DraftHeuristicTest.kt
    - app/src/test/java/com/giftregistry/ui/registry/list/IsPrimarySelectionTest.kt
    - app/src/test/java/com/giftregistry/ui/common/AvatarInitialsTest.kt
    - app/src/test/java/com/giftregistry/ui/auth/AuthHeadlineTest.kt
    - app/src/test/java/com/giftregistry/ui/auth/AuthFormStateTest.kt
  modified: []

key-decisions:
  - "Calendar-based startOfTodayMs (not LocalDate) for minSdk 23 compat — LocalDate.atStartOfDay() requires API 26"
  - "eventDateMs null = Active (not Past) per CONTEXT.md Tab filter definitions; isActive is inclusive (>=), isPast is strict (<)"
  - "Draft heuristic = title.isBlank() || itemCount == 0 — client-only, no Registry.status field until v1.2"
  - "primaryRegistryIdOf uses maxByOrNull{updatedAt} with first-on-tie pin — mirrors AppNavigation.kt Phase 9 resolver"
  - "toAvatarInitials fallback chain: displayName split(2 words) → email firstChar → '?' literal"
  - "AUTH_SCREEN_DEFAULT_IS_SIGN_UP_MODE = true — flip from current false per CONTEXT.md locked decision D-02"
  - "AuthFormState gains firstName + lastName without removing existing 5 fields (regression guard test ships)"

patterns-established:
  - "Wave 0 RED stub pattern matches Phase 8/9 precedent: pure-JVM JUnit 4, no Compose test, unresolved-reference compile-fail"
  - "AnnotatedString factory tested via .text string inspection + .spanStyles color verification — no Compose runtime needed"

requirements-completed:
  - SCR-06
  - SCR-07

duration: 2min
completed: 2026-04-21
---

# Phase 10 Plan 01: Wave 0 RED Stubs — SCR-06 + SCR-07 Contract Summary

**6 JUnit 4 RED stub test files (40 @Test methods) pinning the Phase 10 API contract: Calendar-based tab predicates, draft heuristic, maxByOrNull primary resolver, avatar initials fallback chain, 3-colour headline AnnotatedString, and AuthFormState firstName/lastName + sign-up-mode default**

## Performance

- **Duration:** 2 min
- **Started:** 2026-04-21T15:49:05Z
- **Completed:** 2026-04-21T15:51:37Z
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments

- 4 RED test files for SCR-07 registry-list logic: tab filter predicates (9 tests), draft heuristic (5 tests), isPrimary selector (5 tests), avatar initials (9 tests)
- 2 RED test files for SCR-06 auth screen: headline AnnotatedString (6 tests), AuthFormState contract (6 tests)
- All 40 tests compile-fail with unresolved-reference errors confirming Wave 0 RED state
- No implementation files created — clean separation between Wave 0 (stubs) and Wave 1 (implementations)

## Task Commits

1. **Task 1: SCR-07 registry list predicates + avatar initials (4 files)** - `704ed12` (test)
2. **Task 2: SCR-06 auth headline + AuthFormState contract (2 files)** - `3d7331a` (test)

## Files Created/Modified

- `app/src/test/java/com/giftregistry/ui/registry/list/TabFilterPredicateTest.kt` — 9 tests pinning startOfTodayMs Calendar boundary, isActive/isPast predicates
- `app/src/test/java/com/giftregistry/ui/registry/list/DraftHeuristicTest.kt` — 5 tests pinning title.isBlank()||itemCount==0 heuristic
- `app/src/test/java/com/giftregistry/ui/registry/list/IsPrimarySelectionTest.kt` — 5 tests pinning maxByOrNull{updatedAt} primary resolver with tie-break ordering
- `app/src/test/java/com/giftregistry/ui/common/AvatarInitialsTest.kt` — 9 tests pinning displayName→email→"?" initials fallback chain
- `app/src/test/java/com/giftregistry/ui/auth/AuthHeadlineTest.kt` — 6 tests pinning authHeadlineAnnotatedString 3-colour split + trailing accent period
- `app/src/test/java/com/giftregistry/ui/auth/AuthFormStateTest.kt` — 6 tests pinning firstName/lastName fields + AUTH_SCREEN_DEFAULT_IS_SIGN_UP_MODE=true

## Decisions Made

- Calendar-based `startOfTodayMs` (not LocalDate) for minSdk 23 compatibility — `LocalDate.atStartOfDay()` requires API 26
- `isActive` is inclusive at boundary (eventDateMs == todayMs → Active), `isPast` is strict less-than
- Draft heuristic kept client-only (`title.isBlank() || itemCount == 0`) — no `Registry.status` field until v1.2
- `primaryRegistryIdOf` tie-breaks to first element per Kotlin stdlib `maxByOrNull` contract — tests pin "r1" to prevent indeterminate UI
- `AUTH_SCREEN_DEFAULT_IS_SIGN_UP_MODE = true` flips current `false` per CONTEXT.md D-02 (first-run → new user creates account)

## Deviations from Plan

None — plan executed exactly as written. All 6 test files created with exact @Test counts (9+5+5+9+6+6 = 40) and the expected compile-fail RED state.

## Issues Encountered

None. The additional unresolved-reference errors in AuthHeadlineTest.kt (`.start`, `.end`, `.item` on spanStyles) are cascade failures from the primary `authHeadlineAnnotatedString` being unresolved — expected and correct RED behavior.

## Public API Surface Plans 02/03 Must Ship

**Plan 02** (`app/src/main/java/com/giftregistry/ui/registry/list/TabFilters.kt`):
- `fun startOfTodayMs(now: Long = System.currentTimeMillis()): Long`
- `fun Registry.isActive(todayMs: Long): Boolean`
- `fun Registry.isPast(todayMs: Long): Boolean`
- `fun Registry.isDraft(itemCount: Int): Boolean`
- `fun primaryRegistryIdOf(registries: List<Registry>): String?`

**Plan 02** (`app/src/main/java/com/giftregistry/ui/common/AvatarInitials.kt`):
- `fun toAvatarInitials(displayName: String?, email: String?): String`

**Plan 03** (`app/src/main/java/com/giftregistry/ui/auth/AuthHeadline.kt`):
- `fun authHeadlineAnnotatedString(prefix: String, accent: String, ink: Color, accentColor: Color, inkSoft: Color): AnnotatedString`

**Plan 03** (`app/src/main/java/com/giftregistry/ui/auth/AuthUiState.kt` — extend in place):
- `data class AuthFormState(... val firstName: String = "", val lastName: String = "", ...)`
- `const val AUTH_SCREEN_DEFAULT_IS_SIGN_UP_MODE: Boolean = true`

## Confirmed Deferred (Not Introduced in Wave 0)

- Compose UI test scaffolding — pure-JVM JUnit 4 only, matching Phase 8/9 precedent
- Item-count aggregation — `isDraft(itemCount)` takes count as param so the heuristic is testable even though the ViewModel doesn't aggregate counts yet (CONTEXT.md deferred)
- `Registry.status: 'draft'` field — client-only heuristic until v1.2

## Known Stubs

None — this plan only creates test stubs (which are intentionally RED). No production code with stub data was introduced.

## Next Phase Readiness

- Wave 0 RED stubs in place — Plans 02 and 03 can proceed to implement the pinned API surface
- `./gradlew :app:compileDebugUnitTestKotlin` currently fails with 40+ unresolved-reference errors (expected RED state)
- When Plan 02 lands, 28 tests (TabFilterPredicate + DraftHeuristic + IsPrimary + AvatarInitials) should flip GREEN
- When Plan 03 lands, remaining 12 tests (AuthHeadline + AuthFormState) should flip GREEN

---
*Phase: 10-onboarding-home-redesign*
*Completed: 2026-04-21*
