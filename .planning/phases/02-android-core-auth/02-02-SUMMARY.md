---
phase: 02-android-core-auth
plan: 02
subsystem: auth
tags: [firebase-auth, hilt, kotlin-flow, datastore, callbackflow, coroutines]

# Dependency graph
requires:
  - phase: 02-android-core-auth/02-01
    provides: Android project scaffold with Hilt, Firebase BoM 34.11.0, DataStore, KSP
provides:
  - AuthRepository interface (domain contract for all auth operations)
  - FirebaseAuthDataSource (Firebase SDK wrapper via callbackFlow)
  - AuthRepositoryImpl (Firebase-to-domain mapping via toDomain())
  - LanguagePreferencesDataStore (DataStore-backed locale persistence)
  - 6 use cases: SignUp, SignInEmail, SignInGoogle, SignInAnonymous, LinkAccount, ObserveAuthState
  - FakeAuthRepository (test double with configurable shouldFail and emitUser())
  - DI bindings: AuthRepository -> AuthRepositoryImpl, LanguagePreferencesRepository -> LanguagePreferencesDataStore
affects:
  - 02-03 (auth ViewModels depend on use cases and FakeAuthRepository)
  - 02-04 (auth UI consumes ViewModels built from these use cases)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Domain layer holds pure Kotlin interfaces/models with zero Firebase imports"
    - "FirebaseUser.toDomain() extension function for clean domain boundary mapping"
    - "callbackFlow for bridging Firebase AuthStateListener to Kotlin Flow"
    - "runCatching wrapping Firebase suspend calls for Result<User> error handling"
    - "FakeAuthRepository pattern: MutableStateFlow + shouldFail flag for ViewModel tests"

key-files:
  created:
    - app/src/main/java/com/giftregistry/domain/model/User.kt
    - app/src/main/java/com/giftregistry/domain/model/GuestUser.kt
    - app/src/main/java/com/giftregistry/domain/auth/AuthRepository.kt
    - app/src/main/java/com/giftregistry/domain/auth/SignUpUseCase.kt
    - app/src/main/java/com/giftregistry/domain/auth/SignInEmailUseCase.kt
    - app/src/main/java/com/giftregistry/domain/auth/SignInGoogleUseCase.kt
    - app/src/main/java/com/giftregistry/domain/auth/SignInAnonymousUseCase.kt
    - app/src/main/java/com/giftregistry/domain/auth/LinkAccountUseCase.kt
    - app/src/main/java/com/giftregistry/domain/auth/ObserveAuthStateUseCase.kt
    - app/src/main/java/com/giftregistry/domain/preferences/LanguagePreferencesRepository.kt
    - app/src/main/java/com/giftregistry/data/auth/FirebaseAuthDataSource.kt
    - app/src/main/java/com/giftregistry/data/auth/AuthRepositoryImpl.kt
    - app/src/main/java/com/giftregistry/data/preferences/LanguagePreferencesDataStore.kt
    - app/src/test/java/com/giftregistry/data/auth/FakeAuthRepository.kt
  modified:
    - app/src/main/java/com/giftregistry/di/DataModule.kt

key-decisions:
  - "Zero Firebase imports in domain layer enforced — pure Kotlin interfaces only"
  - "callbackFlow chosen for Firebase AuthStateListener bridging — proper lifecycle cleanup via awaitClose"
  - "runCatching used in AuthRepositoryImpl — converts Firebase exceptions to Result.failure without leaking Firebase types"
  - "FakeAuthRepository in test source set (not main) — injected via Hilt test components in future tests"

patterns-established:
  - "domain/ package: pure Kotlin only, no Android or Firebase imports"
  - "data/ package: Firebase SDK usage isolated here, never crosses to domain"
  - "Use cases are thin @Inject wrappers delegating to AuthRepository — single operator fun invoke"
  - "toDomain() private extension in AuthRepositoryImpl — FirebaseUser conversion stays in data layer"

requirements-completed: [AUTH-01, AUTH-02, AUTH-03, AUTH-05, AUTH-06]

# Metrics
duration: 7min
completed: 2026-04-05
---

# Phase 02 Plan 02: Auth Domain and Data Layer Summary

**AuthRepository interface + FirebaseAuthDataSource + 6 use cases + FakeAuthRepository — full auth contract with clean domain/data separation and zero Firebase imports in domain**

## Performance

- **Duration:** 7 min
- **Started:** 2026-04-05T07:07:02Z
- **Completed:** 2026-04-05T07:14:00Z
- **Tasks:** 2
- **Files modified:** 15 (14 created, 1 modified)

## Accomplishments
- Domain layer created with zero Firebase imports: User model, GuestUser model, AuthRepository interface, LanguagePreferencesRepository interface, and 6 single-responsibility use cases
- Data layer created: FirebaseAuthDataSource wraps Firebase SDK via callbackFlow, AuthRepositoryImpl maps FirebaseUser to domain User via toDomain() extension, LanguagePreferencesDataStore persists locale via DataStore
- DI bindings complete: DataModule @Binds both AuthRepository and LanguagePreferencesRepository interfaces to their implementations
- FakeAuthRepository test double in test source set with configurable shouldFail flag and emitUser() helper

## Task Commits

Each task was committed atomically:

1. **Task 1: Create auth domain layer** - `85a2ea4` (feat)
2. **Task 2: Create auth data layer and DI bindings** - `b495435` (feat)

**Plan metadata:** (assigned after SUMMARY commit)

## Files Created/Modified
- `domain/model/User.kt` - Pure Kotlin user model (uid, email, displayName, isAnonymous)
- `domain/model/GuestUser.kt` - Guest reservation model (firstName, lastName, email)
- `domain/auth/AuthRepository.kt` - Interface covering all auth operations with Flow<User?> for state
- `domain/auth/SignUpUseCase.kt` - Email/password sign-up delegate
- `domain/auth/SignInEmailUseCase.kt` - Email/password sign-in delegate
- `domain/auth/SignInGoogleUseCase.kt` - Google sign-in delegate (takes idToken)
- `domain/auth/SignInAnonymousUseCase.kt` - Anonymous sign-in delegate
- `domain/auth/LinkAccountUseCase.kt` - Anonymous-to-email account linking delegate
- `domain/auth/ObserveAuthStateUseCase.kt` - Flow<User?> auth state observer
- `domain/preferences/LanguagePreferencesRepository.kt` - Locale preference interface
- `data/auth/FirebaseAuthDataSource.kt` - Firebase SDK wrapper with callbackFlow for auth state
- `data/auth/AuthRepositoryImpl.kt` - Repository implementation with runCatching + toDomain()
- `data/preferences/LanguagePreferencesDataStore.kt` - DataStore implementation for locale
- `di/DataModule.kt` - @Binds both repository interfaces to implementations
- `test/.../FakeAuthRepository.kt` - Test double with MutableStateFlow, shouldFail, emitUser()

## Decisions Made
- Zero Firebase imports in domain enforced — pure Kotlin interfaces and data classes only, keeping domain testable without Android/Firebase dependencies
- callbackFlow with awaitClose for AuthStateListener — ensures listener is removed when collector cancels, preventing leaks
- runCatching for all Firebase suspend calls — converts exceptions to Result.failure keeping FirebaseExceptions out of domain

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Known Stubs
None - no hardcoded placeholder data; all implementations are wired to real Firebase Auth and DataStore. FakeAuthRepository in test sources is intentional and clearly marked.

## Next Phase Readiness
- Auth domain contract ready for ViewModel consumption in 02-03
- FakeAuthRepository ready for unit testing ViewModels
- DataModule bindings ready for Hilt injection throughout the app
- LanguagePreferencesDataStore ready for locale switching in AppCompatActivity

---
*Phase: 02-android-core-auth*
*Completed: 2026-04-05*

## Self-Check: PASSED

All files verified present. Both task commits confirmed in git log.
