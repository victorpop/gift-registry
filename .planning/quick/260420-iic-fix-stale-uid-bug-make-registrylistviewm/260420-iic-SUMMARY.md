---
phase: quick/260420-iic
plan: 01
subsystem: ui
tags: [android, kotlin, viewmodel, kotlin-flow, flatMapLatest, firebase-auth, firestore, coroutines]

# Dependency graph
requires:
  - phase: 02-android-core-auth
    provides: AuthRepository.authState Flow<User?> and Activity-scoped hiltViewModel() behavior
  - phase: 03-registry-item-management
    provides: ObserveRegistriesUseCase(ownerId) Flow<List<Registry>>
provides:
  - RegistryListViewModel.uiState rebinds the Firestore listener on every auth state change
  - Loading-state coverage during the signed-out transient (prevents empty-state flash)
affects: [registry-list-ui, auth-flow, sign-out/sign-in UX]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "authState.flatMapLatest { user -> observe(user.uid) } — canonical reactive-per-user upstream swap for Activity-scoped ViewModels"

key-files:
  created: []
  modified:
    - app/src/main/java/com/giftregistry/ui/registry/list/RegistryListViewModel.kt

key-decisions:
  - "@OptIn(ExperimentalCoroutinesApi::class) placed on the class, not the init block — Kotlin rejects @OptIn on initializer target"
  - "Emit RegistryListUiState.Loading (not Success(emptyList())) when user == null to avoid a brief empty-state flash during sign-out; the auth screen replaces the UI shortly after"
  - "flatMapLatest preserved downstream .catch so errors from both the auth flow and the per-user registries flow route to RegistryListUiState.Error"

patterns-established:
  - "Reactive per-user upstream swap: Activity-scoped ViewModels that depend on a UID must derive their StateFlow from AuthRepository.authState via flatMapLatest, never capture uid in init"

requirements-completed:
  - QUICK-260420-iic

# Metrics
duration: 1m 19s
completed: 2026-04-20
---

# Quick Task 260420-iic: Fix stale-UID bug in RegistryListViewModel

**RegistryListViewModel.uiState now rebinds the Firestore listener on every auth state change via authState.flatMapLatest { user -> observeRegistries(user.uid) }, eliminating stale-UID queries after sign-out/sign-in on the same Activity.**

## Performance

- **Duration:** 1m 19s
- **Started:** 2026-04-20T10:22:28Z
- **Completed:** 2026-04-20T10:23:47Z
- **Tasks:** 1
- **Files modified:** 1

## Accomplishments
- Replaced init-time `val uid = authRepository.currentUser?.uid ?: ""` capture with reactive `authRepository.authState.flatMapLatest { user -> ... }` chain
- Firestore listener now cancels the prior user's subscription and rebinds to the new user's uid on every auth emission
- Preserved `SharingStarted.Eagerly` so the StateFlow attaches on first auth emission (no first-emission delay regression)
- Preserved terminal `.catch` so errors from the auth flow and the inner registries flow route to `RegistryListUiState.Error`
- Emits `RegistryListUiState.Loading` when `user == null` to avoid an empty-state flash during sign-out transient

## Task Commits

1. **Task 1: Make RegistryListViewModel.uiState reactive to authState** - `42c1f6a` (fix)

## Files Created/Modified
- `app/src/main/java/com/giftregistry/ui/registry/list/RegistryListViewModel.kt` — Reactive upstream swap; added imports for `ExperimentalCoroutinesApi`, `flatMapLatest`, `flowOf`; `@OptIn(ExperimentalCoroutinesApi::class)` on class-level annotation

## Decisions Made
- **`@OptIn` placement:** The plan suggested annotating the `init` block. Kotlin rejects `@OptIn` on initializer target (compile error: "This annotation is not applicable to target 'initializer'"). Moved to the class-level annotation — still tighter than file-level and the smallest valid scope for this single experimental API usage.
- **Null-user emission:** Chose `flowOf(RegistryListUiState.Loading)` over `flowOf(Success(emptyList()))` for the signed-out branch per plan design notes — avoids showing "no registries" to a user who is only transiently signed-out.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Moved `@OptIn(ExperimentalCoroutinesApi::class)` from `init` block to class declaration**
- **Found during:** Task 1 (first `./gradlew :app:assembleDebug` run)
- **Issue:** Plan placed `@OptIn(ExperimentalCoroutinesApi::class)` on the `init` block. Kotlin compiler rejected this with `e: ... This annotation is not applicable to target 'initializer'. Applicable targets: class, property, local variable, value parameter, constructor, function, getter, setter, expression, file, typealias`. Without fixing it, `./gradlew :app:assembleDebug` fails — blocking verification and the Done criterion.
- **Fix:** Moved the `@OptIn(ExperimentalCoroutinesApi::class)` annotation onto the `RegistryListViewModel` class declaration (above `@HiltViewModel`). This is the tightest valid scope for this single `flatMapLatest` usage.
- **Files modified:** `app/src/main/java/com/giftregistry/ui/registry/list/RegistryListViewModel.kt`
- **Verification:** `./gradlew :app:assembleDebug` now exits 0; all plan sanity checks still pass (including `grep -q "@OptIn(ExperimentalCoroutinesApi::class)"`).
- **Committed in:** `42c1f6a` (Task 1 commit — rolled in with the reactive-upstream change)

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Semantically identical to the plan's intent — `@OptIn` still scopes the experimental-API acknowledgement to `RegistryListViewModel`. The only reason the plan's exact instruction did not compile is Kotlin's target rules for `@OptIn`. No scope creep; no other files touched.

## Issues Encountered
- First build failed on `@OptIn` target placement (see deviation #1). Resolved inline by moving the annotation to a valid target.

## User Setup Required
None.

## Manual Verification (optional, documented in plan)
The original user-visible bug (A's registries bleeding into B's list after sign-out/sign-in) can be reproduced per the plan's verification steps:
1. Sign in as user A → create a registry → verify list shows it.
2. Sign out.
3. Sign in as user B with existing registries.
4. Expected (post-fix): B's registries show. A's do not. The list correctly re-queries Firestore with B's uid.

Automated build gate (`./gradlew :app:assembleDebug`) passes, which is the required verification per plan constraints.

## Next Phase Readiness
- Registry list UI correctly reacts to auth changes — no further blocker for multi-account testing.
- Audit in the plan confirmed this is the only VM with the init-time UID anti-pattern. `CreateRegistryViewModel`, `SettingsViewModel`, `RegistryDetailViewModel`, `InviteViewModel`, `AddItemViewModel`, `EditItemViewModel` were reviewed and deemed safe.
- Non-VM UID sites (`FcmTokenRepositoryImpl`, `LanguagePreferencesDataStore.syncRemoteLocale()`) read UID inside suspend functions and are safe per call.

## Self-Check: PASSED

- `app/src/main/java/com/giftregistry/ui/registry/list/RegistryListViewModel.kt` — FOUND (modified)
- Commit `42c1f6a` — FOUND in `git log`
- All plan sanity checks — PASS (authState present, flatMapLatest present, `observeRegistries(user.uid)` present, stale `val uid = authRepository.currentUser?.uid` pattern removed, `@OptIn(ExperimentalCoroutinesApi::class)` present, `SharingStarted.Eagerly` preserved)
- `./gradlew :app:assembleDebug` — BUILD SUCCESSFUL

---
*Phase: quick/260420-iic*
*Completed: 2026-04-20*
