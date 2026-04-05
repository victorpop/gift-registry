---
phase: 02-android-core-auth
plan: 03
subsystem: auth, ui
tags: [jetpack-compose, navigation3, hilt, credential-manager, material3, stateflow, turbine]

# Dependency graph
requires:
  - phase: 02-android-core-auth/02-02
    provides: AuthRepository interface, domain use cases (SignUp, SignIn, SignInGoogle, SignInAnonymous, ObserveAuthState), FakeAuthRepository test double, User domain model

provides:
  - AuthUiState sealed interface (Loading, Unauthenticated, Authenticated)
  - AuthFormState data class for form field tracking
  - AuthViewModel with HiltViewModel, StateFlow auth state, and all auth action methods
  - AuthScreen with tabbed sign-in/sign-up UI, Google Credential Manager integration, guest link
  - AppNavigation with Navigation3 NavDisplay and auth-state-driven back stack gating
  - AppNavKeys (@Serializable AuthKey, HomeKey, SettingsKey)
  - 17 auth string keys in English (values/strings.xml) and Romanian (values-ro/strings.xml)
  - 9 AuthViewModelTest cases using FakeAuthRepository (all pass)
  - MainDispatcherRule for coroutines testing
  - SignOutUseCase in domain layer

affects:
  - 02-android-core-auth/02-04 (settings screen — replaces placeholder SettingsKey entry)
  - Phase 3 (registry screens — extends HomeKey screen, uses authState for user context)
  - Phase 4 (reservation — AuthScreen.kt guest conversion sheet triggered from reservation flow)

# Tech tracking
tech-stack:
  added:
    - Navigation3 1.0.1 (NavDisplay with entryProvider)
    - Credential Manager (GetGoogleIdOption, GoogleIdTokenCredential)
    - app.cash.turbine 1.2.0 (Flow testing in AuthViewModelTest)
    - PrimaryTabRow (replaces deprecated TabRow)
  patterns:
    - drop(1) on ObserveAuthStateUseCase Flow to prevent auth screen flash on session restore
    - LaunchedEffect(authUiState) drives back stack mutations in AppNavigation
    - Loading state gates NavDisplay rendering (CircularProgressIndicator shown instead)
    - hiltViewModel() works via Activity ViewModelStoreOwner without rememberViewModelStoreNavEntryDecorator
    - FakeAuthRepository + real use cases pattern for ViewModel unit testing (no mocking frameworks needed)

key-files:
  created:
    - app/src/main/java/com/giftregistry/ui/auth/AuthUiState.kt
    - app/src/main/java/com/giftregistry/ui/auth/AuthViewModel.kt
    - app/src/main/java/com/giftregistry/ui/auth/AuthScreen.kt
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
    - app/src/main/java/com/giftregistry/domain/auth/SignOutUseCase.kt
    - app/src/test/java/com/giftregistry/MainDispatcherRule.kt
    - app/src/test/java/com/giftregistry/ui/auth/AuthViewModelTest.kt
  modified:
    - app/src/main/java/com/giftregistry/MainActivity.kt (wired AppNavigation)
    - app/src/main/res/values/strings.xml (+17 auth keys)
    - app/src/main/res/values-ro/strings.xml (+17 Romanian auth keys)

key-decisions:
  - "drop(1) on ObserveAuthStateUseCase Flow skips initial null before Firebase session restore, preventing auth screen flash"
  - "PrimaryTabRow used instead of deprecated TabRow per Material3 guidance"
  - "rememberViewModelStoreNavEntryDecorator absent from Navigation3 1.0.1 stable; hiltViewModel() uses Activity ViewModelStoreOwner instead"
  - "SignOutUseCase added to domain layer (plan mentioned it as option but omitted from use case list)"
  - "Google Sign-In error shows Snackbar (network/external errors); field validation errors show inline supportingText"

patterns-established:
  - "Pattern: TDD with FakeAuthRepository + real use cases avoids MockK overhead for ViewModel tests"
  - "Pattern: AuthUiState.Loading as initial ViewModel state prevents auth screen flash on restart"
  - "Pattern: LaunchedEffect(authUiState) for declarative back stack management in NavDisplay"

requirements-completed: [AUTH-01, AUTH-02, AUTH-03, AUTH-04, AUTH-05]

# Metrics
duration: 24min
completed: 2026-04-05
---

# Phase 2 Plan 03: Auth UI and Navigation Summary

**Tabbed auth screen (email/password + Google Credential Manager + guest), Navigation3 auth gating, and 9-test ViewModel suite using FakeAuthRepository**

## Performance

- **Duration:** 24 min
- **Started:** 2026-04-05T07:15:22Z
- **Completed:** 2026-04-05T07:38:58Z
- **Tasks:** 2
- **Files modified:** 11

## Accomplishments
- AuthViewModel with Loading/Unauthenticated/Authenticated states and all auth actions; all 9 tests pass
- AuthScreen with PrimaryTabRow, password visibility toggle, Google sign-in via Credential Manager, guest link (44dp touch target), Snackbar for network errors
- Navigation3 AppNavigation with auth-state-driven back stack; Loading state shows CircularProgressIndicator (no NavDisplay flash)
- 17 new string resource keys in English and Romanian per UI-SPEC Copywriting Contract

## Task Commits

Each task was committed atomically:

1. **Task 1: AuthViewModel, AuthUiState, MainDispatcherRule, AuthViewModelTest** - `5db4f65` (feat)
2. **Task 2: AuthScreen, AppNavigation, AppNavKeys, MainActivity, string resources** - `09d254f` (feat)

**Plan metadata:** (docs commit follows)

## Files Created/Modified
- `app/src/main/java/com/giftregistry/ui/auth/AuthUiState.kt` - Sealed interface (Loading, Unauthenticated, Authenticated) + AuthFormState
- `app/src/main/java/com/giftregistry/ui/auth/AuthViewModel.kt` - @HiltViewModel with StateFlow auth state and all auth actions
- `app/src/main/java/com/giftregistry/ui/auth/AuthScreen.kt` - Full auth UI: tabbed sign-in/sign-up, Credential Manager Google sign-in, guest link
- `app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt` - @Serializable AuthKey, HomeKey, SettingsKey
- `app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt` - NavDisplay with auth gating and Loading state guard
- `app/src/main/java/com/giftregistry/domain/auth/SignOutUseCase.kt` - New use case for sign out (was missing from Plan 02)
- `app/src/test/java/com/giftregistry/MainDispatcherRule.kt` - UnconfinedTestDispatcher JUnit rule
- `app/src/test/java/com/giftregistry/ui/auth/AuthViewModelTest.kt` - 9 test cases for AuthViewModel
- `app/src/main/java/com/giftregistry/MainActivity.kt` - Wired to AppNavigation
- `app/src/main/res/values/strings.xml` - Added 17 auth string keys
- `app/src/main/res/values-ro/strings.xml` - Added 17 Romanian auth string keys

## Decisions Made
- `drop(1)` on `ObserveAuthStateUseCase` Flow skips the initial null that Firebase emits before restoring the persisted session — prevents brief flash of the auth screen on app restart.
- `PrimaryTabRow` used instead of the deprecated `TabRow` per Material3 guidance.
- Google Sign-In errors (network failures, cancellation) show in Snackbar; field validation errors show inline via `supportingText`.
- `SignOutUseCase` added to domain layer — the plan mentioned it as an option but omitted from the use case inventory; required for `signOut()` in AuthViewModel.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Added SignOutUseCase to domain layer**
- **Found during:** Task 1 (AuthViewModel implementation)
- **Issue:** Plan's AuthViewModel `signOut()` referenced `observeAuthStateUseCase.repository.signOut()` — `ObserveAuthStateUseCase` doesn't expose the repository. The use case list in Plan 02 omitted `SignOutUseCase`.
- **Fix:** Created `SignOutUseCase.kt` in `domain/auth/` with `@Inject constructor(val repository: AuthRepository)` and `operator fun invoke() = repository.signOut()`; updated AuthViewModel to inject and use it.
- **Files modified:** `SignOutUseCase.kt` (new), `AuthViewModel.kt`, `AuthViewModelTest.kt`
- **Verification:** All 9 tests pass; build clean.
- **Committed in:** `5db4f65` (Task 1 commit)

**2. [Rule 1 - Bug] PrimaryTabRow replaces deprecated TabRow**
- **Found during:** Task 2 (AuthScreen build)
- **Issue:** `TabRow` is deprecated in Material3 via Compose BOM 2026.03.00; compiler emits deprecation warning.
- **Fix:** Changed import and usage to `PrimaryTabRow`.
- **Files modified:** `AuthScreen.kt`
- **Verification:** No deprecation warnings in build output.
- **Committed in:** `09d254f` (Task 2 commit)

**3. [Rule 3 - Blocking] rememberViewModelStoreNavEntryDecorator absent from Navigation3 1.0.1**
- **Found during:** Task 2 (AppNavigation build)
- **Issue:** Plan required `rememberViewModelStoreNavEntryDecorator()` for `hiltViewModel()` inside `NavDisplay`, but this function does not exist in the Navigation3 1.0.1 stable API. Build failed with unresolved reference.
- **Fix:** Removed the decorator from `entryDecorators` list. `hiltViewModel()` works via `Activity.viewModelStore` through `LocalViewModelStoreOwner.current` which the Activity provides; no extra decorator needed.
- **Files modified:** `AppNavigation.kt`
- **Verification:** Build passes; hiltViewModel() resolves correctly.
- **Committed in:** `09d254f` (Task 2 commit)

---

**Total deviations:** 3 auto-fixed (1 missing critical, 1 bug, 1 blocking)
**Impact on plan:** All auto-fixes necessary for correctness and compilability. No scope creep.

## Known Stubs

| Stub | File | Description | Resolved in |
|------|------|-------------|-------------|
| `Text("Home — Phase 3")` | `AppNavigation.kt:100` | Placeholder Home screen body | Phase 3 plan (registry screens) |
| `Text("Settings — Plan 04")` | `AppNavigation.kt:105` | Placeholder Settings screen | 02-04-PLAN.md |

These stubs are intentional per plan spec ("placeholder Scaffold ... Body shows centered `Text('Home -- Phase 3')`"). They do not block the plan's goals (auth flow is complete).

## Issues Encountered
None beyond the deviations documented above.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Auth flow is complete and testable end-to-end with FakeAuthRepository
- Navigation3 back stack is ready for Phase 3 registry screen additions (add entry for RegistryKey, handle auth state for registry-specific gating)
- Settings stub in SettingsKey ready for Plan 02-04 replacement
- Home stub in HomeKey ready for Phase 3 replacement

## Self-Check: PASSED

All 9 files created, 2 task commits verified, SUMMARY.md written.

---
*Phase: 02-android-core-auth*
*Completed: 2026-04-05*
