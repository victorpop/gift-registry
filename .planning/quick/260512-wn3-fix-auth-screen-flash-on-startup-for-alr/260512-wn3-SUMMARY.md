---
phase: quick-260512-wn3
plan: 01
subsystem: android-auth
tags: [bug-fix, auth, cold-start, ux, regression-test]
dependency_graph:
  requires: []
  provides: [no-auth-flash-on-cold-start]
  affects: [AuthViewModel, FirebaseAuthDataSource, AuthRepositoryImpl, RegistryListViewModel, AddItemViewModel, EditItemViewModel, RegistryDetailViewModel, NotificationsViewModel, InboxBellViewModel]
tech_stack:
  added: []
  patterns: [AuthStateEvent-sealed-interface, callbackFlow-seenFirst-flag, Flow-filter-map-consumer-pattern]
key_files:
  created:
    - app/src/main/java/com/giftregistry/domain/auth/AuthStateEvent.kt
    - app/src/main/java/com/giftregistry/data/auth/UserMapping.kt
  modified:
    - app/src/main/java/com/giftregistry/domain/auth/AuthRepository.kt
    - app/src/main/java/com/giftregistry/domain/auth/ObserveAuthStateUseCase.kt
    - app/src/main/java/com/giftregistry/data/auth/FirebaseAuthDataSource.kt
    - app/src/main/java/com/giftregistry/data/auth/AuthRepositoryImpl.kt
    - app/src/main/java/com/giftregistry/ui/auth/AuthViewModel.kt
    - app/src/main/java/com/giftregistry/ui/item/add/AddItemViewModel.kt
    - app/src/main/java/com/giftregistry/ui/item/edit/EditItemViewModel.kt
    - app/src/main/java/com/giftregistry/ui/notifications/InboxBellViewModel.kt
    - app/src/main/java/com/giftregistry/ui/notifications/NotificationsViewModel.kt
    - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailViewModel.kt
    - app/src/main/java/com/giftregistry/ui/registry/list/RegistryListViewModel.kt
    - app/src/test/java/com/giftregistry/data/auth/FakeAuthRepository.kt
    - app/src/test/java/com/giftregistry/ui/auth/AuthViewModelTest.kt
    - app/src/test/java/com/giftregistry/ui/item/edit/EditItemViewModelIsOwnerTest.kt
    - app/src/test/java/com/giftregistry/ui/item/edit/EditItemViewModelReservationTest.kt
    - app/src/test/java/com/giftregistry/ui/registry/detail/RegistryDetailViewModelIsOwnerTest.kt
decisions:
  - "Introduce typed AuthStateEvent (Initial/Changed) at the dataSource layer rather than drop(1) at the use case level — drop(1) would hang the genuinely-signed-out path forever"
  - "Keep domain AuthRepository.authState as Flow<AuthStateEvent> rather than folding back to Flow<User?> with a second initialization signal — consumers that need User? apply filter/map inline"
  - "Extract FirebaseUser.toDomain() to shared UserMapping.kt so both FirebaseAuthDataSource and AuthRepositoryImpl use the same mapper"
  - "No artificial delays or timeouts — the Firebase AuthStateListener contract guarantees a post-attach callback once restoration completes"
metrics:
  duration: ~15min
  completed: 2026-05-12
  tasks_completed: 2
  tasks_total: 3
  files_changed: 17
---

# Quick 260512-wn3: Fix Auth Screen Flash on Cold Start — Summary

**One-liner:** Introduced `AuthStateEvent.Initial` vs `Changed` at the Firebase data-source layer so `AuthViewModel` stays in `Loading` during the cold-start gap where Firebase has not yet read the cached user from disk, eliminating the ~1-second `AuthScreen` flash for already-signed-in users.

## Root Cause

`FirebaseAuthDataSource.authStateFlow` used a `callbackFlow` with a `FirebaseAuth.AuthStateListener`. Firebase fires this listener **synchronously on attach** with `auth.currentUser`. On cold start, `auth.currentUser` is null at the moment of attach because Firebase has not yet finished reading the persisted user from disk. That synchronous null was `trySend`-ed into the flow, propagated through `AuthRepositoryImpl` and `ObserveAuthStateUseCase`, and collected in `AuthViewModel.init` which immediately wrote `_authState.value = AuthUiState.Unauthenticated`. About one second later, Firebase fired the listener again with the restored cached user and the state flipped to `Authenticated`.

The intermediate `Unauthenticated` state caused `AppNavigation`'s `LaunchedEffect` to push `AuthKey` onto the back stack, rendering `AuthScreen` for ~1 second before the cached user arrived.

## Why Alternative Approaches Were Rejected

- **`drop(1)` at `ObserveAuthStateUseCase`:** Unconditionally drops the first emission. For a genuinely signed-out user on cold start, the listener-attach callback is the only emission — `drop(1)` would leave the ViewModel stuck in `Loading` indefinitely, hanging the splash indicator.
- **Artificial `delay()` / timeout:** Brittle (device-dependent timing), and forbidden by task constraints.
- **Modifying `AuthScreen` to handle a loading flag:** Does not fix the `LaunchedEffect` in `AppNavigation` which pushes `AuthKey` onto the back stack, causing the visible navigation transition.
- **System SplashScreen alone:** Controls the system window chrome, not the Compose nav back stack. The auth content still renders and navigates.

## The Fix

**`AuthStateEvent` sealed interface** (`domain/auth/AuthStateEvent.kt`):
- `Initial(user: User?)` — the synchronous listener-attach emission
- `Changed(user: User?)` — any subsequent emission (restoration complete, sign-in, sign-out)

**`FirebaseAuthDataSource.authStateFlow`** uses a `seenFirst` flag inside `callbackFlow`:
- First listener callback → `trySend(AuthStateEvent.Initial(user))`
- Subsequent callbacks → `trySend(AuthStateEvent.Changed(user))`

**`AuthViewModel.init`** maps the event to UI state:
- `Initial(non-null user)` → `Authenticated` (fast path: Firebase had the user in-memory at attach)
- `Initial(null)` → stay in `Loading` (cold-start gap — wait for the Changed emission)
- `Changed(non-null user)` → `Authenticated`
- `Changed(null)` → `Unauthenticated`

## Consumer Updates (Task 1 Step 7)

The grep `grep -rn "ObserveAuthStateUseCase\|repository\.authState\|authRepository\.authState\|\.authState"` revealed 6 additional consumers beyond `AuthViewModel` that used `authRepository.authState` as `Flow<User?>`:

1. **`RegistryListViewModel`** — `currentUser` StateFlow and `uiState` flatMapLatest
2. **`AddItemViewModel`** — `registriesForPicker` flatMapLatest
3. **`EditItemViewModel`** — `isOwner` combine with registry
4. **`RegistryDetailViewModel`** — `isOwner` combine with registry
5. **`NotificationsViewModel`** — `uiState` flatMapLatest + `currentUid` collector
6. **`InboxBellViewModel`** — `unreadCount` flatMapLatest

Each consumer was updated with a `.filter { event -> event !is AuthStateEvent.Initial || event.user != null }.map { event -> when (event) { ... } }` transform before the consumer's existing pipeline. This skips `Initial(null)` (preserving the Loading/empty-list initial value) and emits the user for all other events — including `Changed(null)` which correctly signals sign-out.

## Unit Test Regression Coverage

`AuthViewModelTest` now includes:

| Test | Scenario | Assertion |
|------|----------|-----------|
| `initial authState is Loading before any emission` | No event yet | First state is Loading |
| `cold start with cached user — Initial(null) then Changed(user) never flashes Unauthenticated` | **BUG regression** | expectNoEvents() during null gap; next state is Authenticated |
| `cold start with cached user delivered as Initial fast-path` | Initial(user) | Loading → Authenticated in 2 emissions |
| `cold start with no cached user — Initial(null) then Changed(null)` | Genuinely signed out | Loading → Unauthenticated, never Authenticated |
| `runtime sign-out emits Unauthenticated` | Changed(null) after Authenticated | Unauthenticated |
| `signUp success`, `signIn success`, etc. (8 existing tests) | Form flows | All continue to pass via emitChanged |

All 12 tests pass (`./gradlew :app:testDebugUnitTest` BUILD SUCCESSFUL).

## On-Device Verification

Task 3 (checkpoint:human-verify) — awaiting physical device cold-start confirmation.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing] Updated 6 additional consumers beyond AuthViewModel**
- **Found during:** Task 1 step 7 (the codebase grep)
- **Issue:** `RegistryListViewModel`, `AddItemViewModel`, `EditItemViewModel`, `RegistryDetailViewModel`, `NotificationsViewModel`, and `InboxBellViewModel` all consumed `authRepository.authState` as `Flow<User?>`. Changing the type without updating these consumers would have caused compilation failures.
- **Fix:** Applied the `filter/map` pattern (skip `Initial(null)`, emit user for everything else) to each consumer. No behavioral regression — these consumers never needed the Initial/Changed distinction, only the settled user value.
- **Files modified:** All 6 ViewModel files above
- **Commit:** 845f147

**2. [Rule 2 - Missing] Updated 3 additional test files**
- **Found during:** Task 1 compilation of unit tests
- **Issue:** `EditItemViewModelIsOwnerTest`, `EditItemViewModelReservationTest`, and `RegistryDetailViewModelIsOwnerTest` used MockK to stub `authRepository.authState` with `flowOf(user)` or `flowOf(null)` — these no longer match `Flow<AuthStateEvent>`.
- **Fix:** Updated all stubs to `flowOf(AuthStateEvent.Changed(user))` / `flowOf(AuthStateEvent.Changed(null))`. Behavioral contract unchanged — using `Changed` (the settled-state event) is correct for these tests which simulate post-restoration states.
- **Files modified:** 3 test files listed above
- **Commit:** 845f147

## Known Stubs

None — all data flows are wired to real Firebase/fake implementations. No placeholder text or hardcoded empty values introduced.

## Self-Check: PASSED

- FOUND: `app/src/main/java/com/giftregistry/domain/auth/AuthStateEvent.kt`
- FOUND: `app/src/main/java/com/giftregistry/data/auth/UserMapping.kt`
- FOUND: commit `845f147` (feat: introduce AuthStateEvent and update auth pipeline)
- FOUND: commit `67b0251` (test: add AuthViewModel regression tests for cold-start flash)
