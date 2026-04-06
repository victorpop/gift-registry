---
phase: 03-registry-item-management
plan: 03
subsystem: database
tags: [firestore, hilt, kotlin, coroutines, callbackflow, repository-pattern, use-cases]

# Dependency graph
requires:
  - phase: 03-01
    provides: domain models (Registry, Item, OgMetadata, ItemStatus), repository interfaces (RegistryRepository, ItemRepository), AffiliateUrlTransformer
  - phase: 02-android-core-auth
    provides: Hilt DI setup (AppModule, DataModule), FirebaseAuthDataSource pattern, AuthRepositoryImpl pattern
provides:
  - FirestoreDataSource: callbackFlow-based real-time observation for registries and items subcollection
  - RegistryRepositoryImpl: full CRUD + invite via CloudFunctions callable
  - ItemRepositoryImpl: full CRUD + OG metadata fetch + AffiliateUrlTransformer applied at addItem
  - 11 use cases with @Inject constructor for Hilt injection
  - AppModule provides FirebaseFirestore and FirebaseFunctions (with emulator routing)
  - DataModule binds RegistryRepository and ItemRepository
affects: [04-reservation-system, 05-ui-screens, phase-03 UI plans]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - callbackFlow + awaitClose for Firestore SnapshotListener real-time observation
    - runCatching wrapping all Firestore suspend calls in repository implementations
    - items as subcollection (registries/{id}/items/{itemId}) per D-01
    - invitedUsers.ifEmpty guard on createRegistry to always include field
    - FirebaseFunctions.getHttpsCallable for server-side operations (invite, OG metadata)

key-files:
  created:
    - app/src/main/java/com/giftregistry/data/model/RegistryDto.kt
    - app/src/main/java/com/giftregistry/data/model/ItemDto.kt
    - app/src/main/java/com/giftregistry/data/registry/FirestoreDataSource.kt
    - app/src/main/java/com/giftregistry/data/registry/RegistryRepositoryImpl.kt
    - app/src/main/java/com/giftregistry/data/registry/ItemRepositoryImpl.kt
    - app/src/main/java/com/giftregistry/domain/usecase/CreateRegistryUseCase.kt (updated)
    - app/src/main/java/com/giftregistry/domain/usecase/UpdateRegistryUseCase.kt
    - app/src/main/java/com/giftregistry/domain/usecase/DeleteRegistryUseCase.kt
    - app/src/main/java/com/giftregistry/domain/usecase/ObserveRegistriesUseCase.kt
    - app/src/main/java/com/giftregistry/domain/usecase/ObserveRegistryUseCase.kt
    - app/src/main/java/com/giftregistry/domain/usecase/ObserveItemsUseCase.kt (updated)
    - app/src/main/java/com/giftregistry/domain/usecase/AddItemUseCase.kt
    - app/src/main/java/com/giftregistry/domain/usecase/UpdateItemUseCase.kt
    - app/src/main/java/com/giftregistry/domain/usecase/DeleteItemUseCase.kt
    - app/src/main/java/com/giftregistry/domain/usecase/FetchOgMetadataUseCase.kt (updated)
    - app/src/main/java/com/giftregistry/domain/usecase/InviteToRegistryUseCase.kt (updated)
  modified:
    - app/src/main/java/com/giftregistry/di/AppModule.kt
    - app/src/main/java/com/giftregistry/di/DataModule.kt

key-decisions:
  - "FirestoreDataSource mirrors FirebaseAuthDataSource pattern exactly: @Singleton @Inject constructor with callbackFlow + awaitClose"
  - "Items stored as subcollection registries/{id}/items/{itemId} for scoped real-time queries"
  - "invitedUsers.ifEmpty{} guard on createRegistry prevents Firestore security rules failure (invitedUsers field must always exist)"
  - "AffiliateUrlTransformer applied in ItemRepositoryImpl.addItem, not in use case — keeps affiliate logic in data layer"
  - "Unknown merchant URLs logged via android.util.Log.w (AFF-04) for operational review without blocking item creation"

patterns-established:
  - "DTO classes use all-default-value constructors (no-arg) for Firestore reflection deserialization"
  - "Repository implementations use private extension functions toDomain() and toMap()/toUpdateMap() for clean DTO/domain conversion"
  - "Use cases are thin @Inject wrappers with operator fun invoke delegating to repository — no business logic in use cases"
  - "All Firebase calls in data layer; zero Firebase imports in domain layer"

requirements-completed:
  - REG-01
  - REG-02
  - REG-03
  - REG-04
  - REG-05
  - REG-09
  - REG-10
  - ITEM-01
  - ITEM-02
  - ITEM-05
  - ITEM-06
  - ITEM-07
  - AFF-01
  - AFF-02
  - AFF-03
  - AFF-04

# Metrics
duration: 12min
completed: 2026-04-06
---

# Phase 03 Plan 03: Data Layer, Use Cases, and Hilt DI for Registry/Item Management Summary

**Firestore data pipeline wired from raw documents to injectable use cases: FirestoreDataSource with callbackFlow listeners, RegistryRepositoryImpl and ItemRepositoryImpl with runCatching, AffiliateUrlTransformer applied at write time, and 11 Hilt-injectable use cases with zero Firebase imports in the domain layer.**

## Performance

- **Duration:** 12 min
- **Started:** 2026-04-06T10:00:00Z
- **Completed:** 2026-04-06T10:12:00Z
- **Tasks:** 2
- **Files modified:** 18

## Accomplishments
- FirestoreDataSource with real-time callbackFlow observation for registries collection and items subcollection
- RegistryRepositoryImpl and ItemRepositoryImpl with consistent runCatching pattern, DTO-to-domain mapping, and callable function invocation
- All 11 use cases (registry CRUD + observe + invite, item CRUD + observe + OG metadata fetch) as thin @Inject wrappers
- AppModule extended with FirebaseFirestore (emulator 10.0.2.2:8080) and FirebaseFunctions (emulator 10.0.2.2:5001) providers
- DataModule extended with @Binds for RegistryRepository and ItemRepository
- Domain layer remains pure Kotlin with zero Firebase imports confirmed

## Task Commits

Each task was committed atomically:

1. **Task 1: DTOs, FirestoreDataSource, repository implementations** - `4d321ed` (feat)
2. **Task 2: Use cases and Hilt DI wiring** - `e1178ab` (feat)

**Plan metadata:** (docs commit to follow)

## Files Created/Modified
- `app/src/main/java/com/giftregistry/data/model/RegistryDto.kt` - Firestore DTO for registry documents with no-arg constructor
- `app/src/main/java/com/giftregistry/data/model/ItemDto.kt` - Firestore DTO for item documents with no-arg constructor
- `app/src/main/java/com/giftregistry/data/registry/FirestoreDataSource.kt` - Firestore access with callbackFlow listeners and .await() writes
- `app/src/main/java/com/giftregistry/data/registry/RegistryRepositoryImpl.kt` - Registry CRUD + invite via Functions callable
- `app/src/main/java/com/giftregistry/data/registry/ItemRepositoryImpl.kt` - Item CRUD + AffiliateUrlTransformer + OG metadata via Functions callable
- `app/src/main/java/com/giftregistry/domain/usecase/` - 11 use case files (7 new, 4 updated to add @Inject)
- `app/src/main/java/com/giftregistry/di/AppModule.kt` - Added provideFirebaseFirestore and provideFirebaseFunctions
- `app/src/main/java/com/giftregistry/di/DataModule.kt` - Added bindRegistryRepository and bindItemRepository

## Decisions Made
- DTO classes use all-default-value constructors required for Firestore's `toObject()` reflection deserialization
- Repository private extension functions (`toDomain()`, `toMap()`, `toUpdateMap()`) keep conversion logic co-located and out of use cases
- AffiliateUrlTransformer applied inside `ItemRepositoryImpl.addItem` (data layer) rather than in `AddItemUseCase` — keeps affiliate business logic out of the domain layer while still ensuring it runs on every item creation

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Added @Inject to 4 pre-existing use cases missing the annotation**
- **Found during:** Task 2 (use cases)
- **Issue:** CreateRegistryUseCase, FetchOgMetadataUseCase, InviteToRegistryUseCase, and ObserveItemsUseCase existed without `@Inject constructor`, making them non-injectable by Hilt
- **Fix:** Added `import javax.inject.Inject` and `@Inject` to all 4 existing use cases
- **Files modified:** CreateRegistryUseCase.kt, FetchOgMetadataUseCase.kt, InviteToRegistryUseCase.kt, ObserveItemsUseCase.kt
- **Verification:** `grep -r "@Inject constructor" domain/usecase/` shows all 11 use cases
- **Committed in:** `e1178ab` (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (Rule 1 - bug fix)
**Impact on plan:** Fix was necessary for Hilt to inject these use cases into ViewModels. No scope creep.

## Issues Encountered
None beyond the pre-existing missing @Inject annotations.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Full data pipeline from Firestore to injectable use cases is complete
- Phase 03-04 (registry list screen) can inject ObserveRegistriesUseCase and CreateRegistryUseCase directly into ViewModels
- Phase 03-05+ item screens can inject all item use cases
- Firebase emulators are configured for both Firestore and Functions — emulator suite must be running for debug builds

---
*Phase: 03-registry-item-management*
*Completed: 2026-04-06*

## Self-Check: PASSED

- All 5 data layer files: FOUND
- Commits 4d321ed and e1178ab: FOUND
- DataModule @Binds count: 4 (2 existing + 2 new)
- AppModule @Provides count: 3 (1 existing + 2 new)
