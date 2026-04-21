---
phase: 10-onboarding-home-redesign
plan: 02
subsystem: ui
tags: [compose, jetpack-compose, giftmaison-theme, animation, kotlin, firebase]

# Dependency graph
requires:
  - phase: 10-01
    provides: Wave 0 RED stubs — AuthHeadlineTest, AuthFormStateTest, TabFilterPredicateTest, DraftHeuristicTest, IsPrimarySelectionTest, AvatarInitialsTest
  - phase: 08-design-system
    provides: GiftMaisonTheme, GiftMaisonColors, GiftMaisonTypography, GiftMaisonShapes, GiftMaisonSpacing, GiftMaisonShadows, InstrumentSerifFamily
provides:
  - Registry.imageUrl nullable field (backward-compat Firestore POJO extension)
  - startOfTodayMs(now) — Calendar-based midnight helper (minSdk 23 safe)
  - Registry.isActive(todayMs) / isPast(todayMs) / isDraft(itemCount) predicates
  - primaryRegistryIdOf(List<Registry>): String? — most-recently-updated ID
  - toAvatarInitials(displayName, email): String — fallback-chain initials derivation
  - AvatarButton — 30 dp olive circle, 44 dp tap target, bodyMEmphasis paper initials
  - FocusedFieldCaret — 1.1 s opacity-only pulse (const FOCUSED_FIELD_CARET_PERIOD_MS)
  - ConcentricRings — Canvas 3-ring corner decoration (alpha 0.08/0.12/0.18)
  - GoogleBanner — terracotta accent banner with G-circle, serif arrow, concentric rings, googleBannerShadow
  - SegmentedTabs — pill-track segmented control (paperDeep track, paper selected, animateColorAsState 200 ms)
affects:
  - 10-03 (Auth screen integration — consumes GoogleBanner, FocusedFieldCaret)
  - 10-04 (Home screen integration — consumes AvatarButton, SegmentedTabs, TabFilters, AvatarInitials)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "GiftMaisonTheme.* exclusively — no raw Color(0xFF) literals in composables (Color.White exception for Google circle)"
    - "Calendar.getInstance() for midnight epoch (minSdk 23 — no java.time.LocalDate)"
    - "Provisional string refs: auth_google_sign_in_button (rewired in Plan 03) + auth_settings_title (rewired in Plan 04)"
    - "FocusedFieldCaret distinct from PulsingDot: 1.1 s opacity-only vs 1.4 s opacity+scale"
    - "Canvas corner rings (Offset(width, 0)) clips naturally at composable bounds — no overflow clipping needed"

key-files:
  created:
    - app/src/main/java/com/giftregistry/domain/model/Registry.kt (imageUrl field added)
    - app/src/main/java/com/giftregistry/ui/registry/list/TabFilters.kt
    - app/src/main/java/com/giftregistry/ui/common/AvatarInitials.kt
    - app/src/main/java/com/giftregistry/ui/common/AvatarButton.kt
    - app/src/main/java/com/giftregistry/ui/common/FocusedFieldCaret.kt
    - app/src/main/java/com/giftregistry/ui/common/ConcentricRings.kt
    - app/src/main/java/com/giftregistry/ui/auth/GoogleBanner.kt
    - app/src/main/java/com/giftregistry/ui/registry/list/SegmentedTabs.kt
  modified: []

key-decisions:
  - "GoogleBanner package: ui/auth/ (per plan artifacts list) — not ui/common/ — because it is screen-specific to AuthScreen"
  - "SegmentedTabs package: ui/registry/list/ (per plan artifacts list) — not ui/common/ — because it is screen-specific to RegistryListScreen"
  - "Canvas over layered Box for ConcentricRings — rings anchor at corner (Offset(width, 0)) and natural bounds-clip achieves handoff overflow:hidden"
  - "Color.White used in GoogleBanner G-circle — not in GiftMaisonTheme token palette; explicitly approved by plan constraint"
  - "Provisional string refs: auth_google_sign_in_button + auth_settings_title used until Plans 03/04 add the final keys"
  - "FocusedFieldCaret uses 1.1 s InfiniteRepeatableSpec with RepeatMode.Reverse — distinctly shorter than PulsingDot 1.4 s and no scale modifier"

patterns-established:
  - "Composable state hoisting: SegmentedTabs caller owns selectedIndex + onTabSelected"
  - "Semantic accessibility: AvatarButton uses semantics { role = Role.Button; contentDescription = desc }"
  - "Provisional string refs documented in KDoc and rewired in follow-on plans"

requirements-completed:
  - SCR-06
  - SCR-07

# Metrics
duration: 45min
completed: 2026-04-21
---

# Phase 10 Plan 02: Domain model + TabFilters + AvatarInitials + 5 shared composables Summary

**Registry.imageUrl added; TabFilters + AvatarInitials flip 4 Wave 0 tests GREEN; 5 GiftMaisonTheme-exclusive Compose composables (AvatarButton, FocusedFieldCaret, ConcentricRings, GoogleBanner, SegmentedTabs) delivered for Plans 03 + 04 integration**

## Performance

- **Duration:** ~45 min (resumed from partial state — Task 1 was committed, Task 2 partially done)
- **Started:** 2026-04-21T00:00:00Z
- **Completed:** 2026-04-21
- **Tasks:** 2 (both complete)
- **Files modified:** 8 (1 domain model update, 2 pure-Kotlin helpers, 5 Compose composable files)

## Accomplishments

- Shipped `Registry.imageUrl: String? = null` — backward-compatible Firestore POJO extension, no migration needed
- Shipped `TabFilters.kt` (startOfTodayMs/isActive/isPast/isDraft/primaryRegistryIdOf) + `AvatarInitials.kt` (toAvatarInitials fallback chain) — flipped 4 Wave 0 tests RED → GREEN (28 @Test methods: TabFilterPredicateTest × 9, DraftHeuristicTest × 5, IsPrimarySelectionTest × 5, AvatarInitialsTest × 9)
- Shipped 5 new Compose composables consuming GiftMaisonTheme.* exclusively: AvatarButton, FocusedFieldCaret, ConcentricRings, GoogleBanner, SegmentedTabs
- FocusedFieldCaret is structurally distinct from PulsingDot (1.1 s opacity-only vs 1.4 s opacity+scale) with independent `const val FOCUSED_FIELD_CARET_PERIOD_MS`
- AuthHeadlineTest (3 tests) and AuthFormStateTest remain RED as expected — Plan 03 responsibility

## Task Commits

Each task was committed atomically:

1. **Task 1: Registry.imageUrl + TabFilters + AvatarInitials — flip 4 Wave 0 tests GREEN** - `82ec032` (feat)
2. **Task 2: ui/common composables (AvatarButton, ConcentricRings, FocusedFieldCaret, GoogleBanner, SegmentedTabs)** - `9ebdbc5` (feat)

**Plan metadata:** (docs commit — see below)

## Files Created/Modified

- `app/src/main/java/com/giftregistry/domain/model/Registry.kt` — added `imageUrl: String? = null` (backward-compat)
- `app/src/main/java/com/giftregistry/ui/registry/list/TabFilters.kt` — startOfTodayMs (Calendar), isActive/isPast/isDraft, primaryRegistryIdOf
- `app/src/main/java/com/giftregistry/ui/common/AvatarInitials.kt` — toAvatarInitials fallback chain (displayName → email → "?")
- `app/src/main/java/com/giftregistry/ui/common/AvatarButton.kt` — 30 dp olive circle, 44 dp tap target, semantic Role.Button
- `app/src/main/java/com/giftregistry/ui/common/FocusedFieldCaret.kt` — 1.1 s opacity-only InfiniteRepeatableSpec caret
- `app/src/main/java/com/giftregistry/ui/common/ConcentricRings.kt` — Canvas 3-ring corner decoration (alpha 0.08/0.12/0.18)
- `app/src/main/java/com/giftregistry/ui/auth/GoogleBanner.kt` — terracotta accent banner, G-circle placeholder, serif arrow, concentric rings, googleBannerShadow
- `app/src/main/java/com/giftregistry/ui/registry/list/SegmentedTabs.kt` — pill-track segmented control, animateColorAsState 200 ms

## Decisions Made

- GoogleBanner in `ui/auth/` package (screen-specific, not ui/common/) per plan artifacts spec
- SegmentedTabs in `ui/registry/list/` package (screen-specific) per plan artifacts spec
- Canvas for ConcentricRings: corner anchor at `Offset(width, 0)` naturally clips at bounds — achieves handoff `overflow:hidden` without explicit clipping
- `Color.White` used for Google G-circle background — not a GiftMaisonTheme token; explicitly permitted by plan constraints
- Provisional strings: `auth_google_sign_in_button` (GoogleBanner) + `auth_settings_title` (AvatarButton) — both exist in strings.xml; Plans 03/04 rewire to final keys

## Known Stubs

- **GoogleBanner G-circle**: `Text("G")` in accent-colored `bodyMEmphasis` is a placeholder for a branded Google G asset. File: `GoogleBanner.kt`, composable body. Intentional per plan ("acceptable placeholder until a branded G asset is added"). Plan 03 may or may not replace with a real Google icon SVG.
- **AvatarButton provisional string**: `R.string.auth_settings_title` → Plan 04 rewires to `R.string.home_avatar_content_desc` (string not yet in strings.xml)
- **GoogleBanner provisional string**: `R.string.auth_google_sign_in_button` → Plan 03 rewires to `R.string.auth_google_cta` (string not yet in strings.xml)

## Deviations from Plan

None — plan executed exactly as specified. The 3 AuthHeadlineTest failures are expected (Wave 0 RED stubs; Plan 03 flips them GREEN).

## Issues Encountered

None. Both provisional string resource references (`auth_google_sign_in_button`, `auth_settings_title`) existed in `values/strings.xml`, confirming no compile-time unresolved reference errors.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- **Plan 03 (Auth screen integration):** GoogleBanner + FocusedFieldCaret ready to consume. Needs: `auth_google_cta` string key (rewires GoogleBanner), AuthHeadline composable integration (flips 3 AuthHeadlineTest RED → GREEN).
- **Plan 04 (Home screen integration):** AvatarButton + SegmentedTabs + TabFilters.startOfTodayMs ready to consume. Needs: `home_avatar_content_desc` string key (rewires AvatarButton).
- Plans 03 and 04 can run in parallel (Wave 1) — no file conflicts with each other or with this plan's output.

---
*Phase: 10-onboarding-home-redesign*
*Completed: 2026-04-21*
