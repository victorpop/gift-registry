---
phase: quick-260419-ubj
plan: 01
subsystem: ui
tags: [android, compose, horizontal-pager, datastore, navigation3, hilt, onboarding, i18n]

requires:
  - phase: 02-android-core-auth
    provides: AuthScreen composable, AuthUiState, AppNavigation, LanguagePreferencesDataStore pattern
  - phase: 04-reservation-system
    provides: GuestPreferencesDataStore (template for unique DataStore name convention)
provides:
  - OnboardingPreferencesRepository (domain) + OnboardingPreferencesDataStore (data) for onboarding_seen flag
  - OnboardingKey nav key + AppNavigation routing that inserts carousel before AuthScreen on first unauthenticated launch
  - OnboardingScreen (HorizontalPager 4 pages; pages 0..2 carousel slides, page 3 embeds existing AuthScreen)
  - EN + RO copy for all carousel strings
affects: [06-notifications-email-flows, any future auth-entrypoint changes]

tech-stack:
  added: []  # No new deps; Compose HorizontalPager + DataStore Preferences already on classpath
  patterns:
    - Third DataStore in the codebase — each requires a uniquely-named `Context.*DataStore` extension property AND a unique `preferencesDataStore(name=...)` string to avoid IllegalStateException
    - Unauthenticated routing now gated by two-dimensional state (AuthUiState × OnboardingSeenState); Loading on either dimension defers the first route
    - Pager-overlay-on-top-of-AuthScreen pattern: nested Scaffold (AuthScreen's) inside a HorizontalPager page is acceptable when overlay controls are hidden on that page

key-files:
  created:
    - app/src/main/java/com/giftregistry/domain/preferences/OnboardingPreferencesRepository.kt
    - app/src/main/java/com/giftregistry/data/preferences/OnboardingPreferencesDataStore.kt
    - app/src/main/java/com/giftregistry/ui/onboarding/OnboardingViewModel.kt
    - app/src/main/java/com/giftregistry/ui/onboarding/OnboardingScreen.kt
  modified:
    - app/src/main/java/com/giftregistry/di/DataModule.kt
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
    - app/src/main/res/values/strings.xml
    - app/src/main/res/values-ro/strings.xml

key-decisions:
  - "AuthScreen reused directly as HorizontalPager page 4 (option a) — no fallback 'Continue' button needed; AuthScreen's own Scaffold/SnackbarHost compiles and layouts inside a pager page slot without modification"
  - "LaunchedEffect(pagerState.settledPage) writes onboarding_seen — settledPage updates only after a swipe animation fully completes, avoiding premature write during partial drags"
  - "SharingStarted.Eagerly for OnboardingViewModel.state — mirrors Phase 02 SettingsViewModel.currentLocale decision so the flag resolves before the first auth-routing frame, preventing a carousel flash on session restore"
  - "No extra markSeen() in AuthScreen — swipe-to-auth already marks seen via settledPage==3; covering 'user authenticates on first launch without swiping' is unreachable (auth requires reaching page 4)"
  - "Unauthenticated routing in AppNavigation now uses a 'correct-entry' check (OnboardingKey vs AuthKey) instead of a single !is AuthKey guard — otherwise the LaunchedEffect would clear the onboarding stack and replace it with AuthKey on the second recomposition"

patterns-established:
  - "Write-once-true DataStore boolean: no setter for the false path; flag persists across sign-out (only reset by clearing app data)"
  - "Onboarding overlay rendered in a Box sibling to the HorizontalPager — overlay is hidden on the final pager page so the embedded screen (AuthScreen) owns the full viewport"
  - "Page indicator dot count == carousel slide count (3), NOT pager pageCount (4) — the auth page intentionally has no dot representation"

requirements-completed: [ONBOARD-01, ONBOARD-02, ONBOARD-03]

duration: 4min
completed: 2026-04-19
---

# Phase quick-260419-ubj Plan 01: Onboarding Carousel (3 Slides Before Auth) Summary

**Three-slide HorizontalPager onboarding carousel inserted before AuthScreen on first unauthenticated launch, with DataStore-persisted onboarding_seen flag so the carousel appears exactly once.**

## Performance

- **Duration:** ~4 minutes
- **Started:** 2026-04-19T18:54:53Z
- **Completed:** 2026-04-19T18:58:57Z
- **Tasks:** 2
- **Files modified/created:** 9 (4 created, 5 modified)

## Accomplishments

- New `OnboardingPreferencesRepository` (domain) and `OnboardingPreferencesDataStore` (data) — third DataStore in the codebase, using unique name `onboarding_prefs` and unique Context extension property `onboardingDataStore`
- `OnboardingScreen` with 4-page HorizontalPager: pages 0..2 are themed onboarding slides, page 3 directly embeds the existing `AuthScreen()` composable — no AuthScreen changes
- Top-right Skip button and bottom-center 3-dot page indicator rendered only on pages 0..2; page 3 shows AuthScreen with no overlay
- `AppNavigation` now waits for both `AuthUiState.Loading` AND `OnboardingSeenState.Loading` before first route, and routes `Unauthenticated` → `OnboardingKey` when the flag is `NotSeen`, otherwise → `AuthKey`
- Onboarding flag written from two paths: pager settles on page 4 (`settledPage == 3`) OR Skip tapped — both call `viewModel.markSeen()`
- EN default + RO (`values-ro`) translations for 9 new string keys, using XML numeric entities for Romanian diacritics (matches existing file convention)

## Task Commits

1. **Task 1: DataStore + domain repo + Hilt binding + nav key + strings** — `9ce398e` (feat)
2. **Task 2: OnboardingScreen + ViewModel + wire into AppNavigation** — `70bb1e3` (feat)

## Files Created/Modified

### Created
- `app/src/main/java/com/giftregistry/domain/preferences/OnboardingPreferencesRepository.kt` — domain interface with `observeOnboardingSeen()` / `isOnboardingSeen()` / `setOnboardingSeen()`
- `app/src/main/java/com/giftregistry/data/preferences/OnboardingPreferencesDataStore.kt` — `@Singleton` implementation backed by DataStore Preferences `onboarding_prefs` with `booleanPreferencesKey("onboarding_seen")`
- `app/src/main/java/com/giftregistry/ui/onboarding/OnboardingViewModel.kt` — Hilt ViewModel exposing `StateFlow<OnboardingSeenState>` (Loading/NotSeen/Seen) via `SharingStarted.Eagerly`; `markSeen()` delegates to repo
- `app/src/main/java/com/giftregistry/ui/onboarding/OnboardingScreen.kt` — `HorizontalPager(pageCount=4)`, private `OnboardingSlide` composable with Material icon illustrations (FormatListBulleted, Link, CardGiftcard), Box overlay with Skip button + dot indicator visible only when `currentPage < 3`

### Modified
- `app/src/main/java/com/giftregistry/di/DataModule.kt` — added `bindOnboardingPreferences` `@Binds @Singleton`
- `app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt` — added `@Serializable data object OnboardingKey`
- `app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt` — injects `OnboardingViewModel`, extends `LaunchedEffect` keys to `(authUiState, onboardingSeenState)`, routes Unauthenticated through `OnboardingKey` when NotSeen with a correct-entry check, extends Loading gate, adds `entry<OnboardingKey> { OnboardingScreen() }`
- `app/src/main/res/values/strings.xml` — 9 new `onboarding_*` keys (English)
- `app/src/main/res/values-ro/strings.xml` — 9 new `onboarding_*` keys (Romanian, XML numeric entities for diacritics)

## Decisions Made

- **AuthScreen reused directly as pager page 4 (option a)** — nested Scaffold/SnackbarHost/Credential Manager all work inside a HorizontalPager page slot. The plan's option (b) fallback ("Continue" button → OnboardingScreen parent `onNavigateToAuth` callback) was NOT needed. Manual smoke-test on device is still recommended to confirm the CredentialManager popup behaves well at page-4 composition.
- **Correct-entry guard in Unauthenticated branch** — a naive `current !is AuthKey` guard (the pre-existing pattern) would have repeatedly cleared the `OnboardingKey` stack and replaced it with `AuthKey`. Introduced an explicit `alreadyOnCorrect` check covering both valid entries.
- **Two-dimensional Loading gate** — `AppNavigation` now returns early when EITHER auth OR onboarding state is Loading. Prevents any partial-state route before the onboarding flag resolves.
- **Page indicator has 3 dots, not 4** — the auth page is intentionally not represented in the indicator; the carousel visually ends at slide 3.

## Deviations from Plan

None — plan executed exactly as written. The one structural tweak (the `alreadyOnCorrect` correct-entry check in `AppNavigation`) is an inline realization of the plan's intent ("reset only if we're not already on the correct unauthenticated entry"), not a scope change.

## Issues Encountered

- None. Two incremental Gradle builds (one after Task 1, one after Task 2) both passed with no new warnings or errors. The pre-existing `@ApplicationContext` parameter-only-annotation warning present in `Language`/`Guest` DataStores repeats for the new `Onboarding` DataStore — not in scope (deviation-rules scope boundary: "pre-existing warnings in unrelated files are out of scope", and the warning pattern is unchanged from the mirrored files).

## Manual Smoke-Test Status

The plan specifies 8 manual smoke-test steps requiring an Android emulator or device (install/uninstall/clear-data/locale-switch). These were **NOT executed** in this run — the executor has no emulator/device session. All 8 steps remain as acceptance criteria for the user to run when verifying on device.

**Automated verifications that PASSED:**
- `./gradlew :app:compileDebugKotlin` — succeeded after Task 1
- `./gradlew :app:assembleDebug` — succeeded after Task 2 (APK built)
- All plan-specified grep verifications (see `<verification>` block items 4-6) return expected matches
- Three distinct DataStore names present: `user_prefs`, `guest_prefs`, `onboarding_prefs`
- No hardcoded EN copy in `app/src/main/java/com/giftregistry/ui/onboarding/` (all via `stringResource`)

## User Setup Required

None — no external service configuration.

## Next Phase Readiness

- Onboarding flag surface (`OnboardingPreferencesRepository.setOnboardingSeen()`) is available should Phase 06 (notifications) want to tie onboarding completion to a first-run notification prompt
- `OnboardingKey` can be inserted anywhere in a future nav graph; the stack-reset logic in `AppNavigation` already handles it

## Self-Check: PASSED

**Files:**
- FOUND: app/src/main/java/com/giftregistry/domain/preferences/OnboardingPreferencesRepository.kt
- FOUND: app/src/main/java/com/giftregistry/data/preferences/OnboardingPreferencesDataStore.kt
- FOUND: app/src/main/java/com/giftregistry/ui/onboarding/OnboardingViewModel.kt
- FOUND: app/src/main/java/com/giftregistry/ui/onboarding/OnboardingScreen.kt
- FOUND: app/src/main/java/com/giftregistry/di/DataModule.kt (modified)
- FOUND: app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt (modified)
- FOUND: app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt (modified)
- FOUND: app/src/main/res/values/strings.xml (modified)
- FOUND: app/src/main/res/values-ro/strings.xml (modified)

**Commits:**
- FOUND: 9ce398e
- FOUND: 70bb1e3

---
*Quick task: 260419-ubj-onboarding-carousel-3-slides-before-auth*
*Completed: 2026-04-19*
