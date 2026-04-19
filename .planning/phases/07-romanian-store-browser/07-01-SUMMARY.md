---
phase: 07-romanian-store-browser
plan: 01
subsystem: database, android
tags: [firestore, datastore, hilt, kotlin, clean-architecture, tdd, mockk]

# Dependency graph
requires:
  - phase: 07-romanian-store-browser
    plan: 00
    provides: "config/stores Firestore path seeded, logo WebPs in drawable-nodpi, stores_* string keys"

provides:
  - "Store domain model (data class) and StoreRepository interface — zero Firebase imports in domain"
  - "StoreRepositoryImpl: one-shot Firestore get().await() on config/stores, manual map cast, sortedBy displayOrder, runCatching-wrapped"
  - "GetStoresUseCase wrapping StoreRepository"
  - "LastRegistryPreferencesRepository interface (pure Kotlin)"
  - "LastRegistryPreferencesDataStore: DataStore name 'last_registry_prefs' (unique), mirrors GuestPreferencesDataStore pattern"
  - "ObserveLastRegistryIdUseCase and SetLastRegistryIdUseCase"
  - "StoresModule: new @Module binding StoreRepository + LastRegistryPreferencesRepository @Singleton"
  - "8 unit tests (4 per repository) — all green"

affects: [07-02, 07-03, phase-07-store-list, phase-07-webview]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "One-shot Firestore read: get().await() with runCatching for config documents (not callbackFlow)"
    - "Manual map cast from Firestore snapshot: snapshot.get('stores') as? List<Map<String,Any?>> — avoids toObject unreliability on top-level arrays"
    - "DataStore unique name enforcement: 'last_registry_prefs' confirmed unique (used: guest_prefs, language_prefs, onboarding_prefs)"
    - "In-memory fake test pattern: inner class InMemoryXxx implements repository interface via MutableStateFlow"
    - "Separate StoresModule (@Module) for Phase 7 bindings — keeps DataModule free of phase-specific additions"

key-files:
  created:
    - app/src/main/java/com/giftregistry/domain/model/Store.kt
    - app/src/main/java/com/giftregistry/domain/store/StoreRepository.kt
    - app/src/main/java/com/giftregistry/domain/usecase/GetStoresUseCase.kt
    - app/src/main/java/com/giftregistry/data/model/StoreDto.kt
    - app/src/main/java/com/giftregistry/data/store/StoreRepositoryImpl.kt
    - app/src/main/java/com/giftregistry/domain/preferences/LastRegistryPreferencesRepository.kt
    - app/src/main/java/com/giftregistry/domain/usecase/ObserveLastRegistryIdUseCase.kt
    - app/src/main/java/com/giftregistry/domain/usecase/SetLastRegistryIdUseCase.kt
    - app/src/main/java/com/giftregistry/data/preferences/LastRegistryPreferencesDataStore.kt
    - app/src/main/java/com/giftregistry/di/StoresModule.kt
    - app/src/test/java/com/giftregistry/data/store/StoreRepositoryImplTest.kt
    - app/src/test/java/com/giftregistry/data/preferences/LastRegistryPreferencesDataStoreTest.kt
  modified: []

key-decisions:
  - "StoresModule created as a separate @Module rather than adding to DataModule — keeps Phase 7 additions discoverable and avoids merge conflicts with Phase 8+"
  - "Manual snapshot.get('stores') cast over toObject — Firestore POJO mapper does not reliably map top-level arrays; confirmed by Research Pattern 2"
  - "DataStore name 'last_registry_prefs' verified unique against existing names (guest_prefs, language_prefs, onboarding_prefs) per Research Pitfall 3"

patterns-established:
  - "One-shot Firestore config read: get().await() + runCatching (not snapshotFlow/callbackFlow)"
  - "DataStore mirror pattern: each DataStore uses a unique Context extension property name"
  - "In-memory fake unit test pattern for repository contracts (no Robolectric needed)"

requirements-completed:
  - STORE-01
  - STORE-03

# Metrics
duration: 8min
completed: 2026-04-19
---

# Phase 07 Plan 01: Domain + Data Layer for Store Browser Summary

**Store domain model + StoreRepository (Firestore one-shot read) + LastRegistryPreferencesDataStore + StoresModule DI binding with 8 green unit tests — zero Firebase imports in domain layer**

## Performance

- **Duration:** ~8 min
- **Started:** 2026-04-19T21:09:45Z
- **Completed:** 2026-04-19T21:17:00Z
- **Tasks:** 2 (both complete, TDD)
- **Files modified:** 12

## Accomplishments

- `Store` data class and `StoreRepository` interface in `domain/` with confirmed zero Firebase imports
- `StoreRepositoryImpl` reads `config/stores` via one-shot `firestore.collection("config").document("stores").get().await()`, maps via manual map cast (not `toObject`), sorts by `displayOrder` ascending, wrapped in `runCatching`
- `LastRegistryPreferencesDataStore` using DataStore name `"last_registry_prefs"` (verified unique); mirrors `GuestPreferencesDataStore` shape exactly
- `StoresModule` binds both new repositories `@Singleton` via `@Binds` — separate from `DataModule` for discoverability
- 8 unit tests: 4 for `StoreRepositoryImpl` (sort, missing logoAsset fallback, failure, empty array) + 4 for `LastRegistryPreferencesDataStore` contract via in-memory fake
- `./gradlew :app:compileDebugKotlin` exits 0 — full DI graph resolves

## Task Commits

1. **Task 1: Store domain model, interface, DTO, impl, use case + 4 tests** - `192d959` (feat)
2. **Task 2: LastRegistryPreferencesDataStore, use cases + StoresModule DI wiring** - `1e8f8b3` (feat)

## Files Created/Modified

- `app/src/main/java/com/giftregistry/domain/model/Store.kt` — Store data class: id, name, homepageUrl, displayOrder, logoAsset
- `app/src/main/java/com/giftregistry/domain/store/StoreRepository.kt` — interface: suspend getStores(): Result<List<Store>>
- `app/src/main/java/com/giftregistry/domain/usecase/GetStoresUseCase.kt` — use case wrapping StoreRepository
- `app/src/main/java/com/giftregistry/data/model/StoreDto.kt` — Firestore mapping DTO with toDomain()
- `app/src/main/java/com/giftregistry/data/store/StoreRepositoryImpl.kt` — Firestore-backed impl, one-shot read, manual map cast
- `app/src/main/java/com/giftregistry/domain/preferences/LastRegistryPreferencesRepository.kt` — pure Kotlin interface: observe/get/set/clear lastRegistryId
- `app/src/main/java/com/giftregistry/domain/usecase/ObserveLastRegistryIdUseCase.kt` — use case returning Flow<String?>
- `app/src/main/java/com/giftregistry/domain/usecase/SetLastRegistryIdUseCase.kt` — suspend use case setting registry id
- `app/src/main/java/com/giftregistry/data/preferences/LastRegistryPreferencesDataStore.kt` — DataStore name "last_registry_prefs", mirrors GuestPreferencesDataStore
- `app/src/main/java/com/giftregistry/di/StoresModule.kt` — new @Module binding both repos @Singleton
- `app/src/test/java/com/giftregistry/data/store/StoreRepositoryImplTest.kt` — 4 tests: sort, fallback, failure, empty
- `app/src/test/java/com/giftregistry/data/preferences/LastRegistryPreferencesDataStoreTest.kt` — 4 tests via in-memory fake

## Decisions Made

- Created `StoresModule` as a separate `@Module` instead of adding to `DataModule` — keeps Phase 7 additions in one discoverable location and avoids touching Phase 2's `DataModule` which other phases may also modify
- Used manual `snapshot.get("stores") as? List<Map<String, Any?>>` instead of `toObject()` — Firestore POJO mapper does not reliably map top-level arrays per Research Pattern 2
- DataStore name `"last_registry_prefs"` verified unique (existing: `guest_prefs`, `language_prefs`, `onboarding_prefs`) per Research Pitfall 3

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None

## Known Stubs

None — all domain interfaces are fully implemented; the Firestore read path is live (not mocked in production code). Store logos remain placeholder WebPs from Plan 00 (tracked there).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plans 07-02 (Store List screen) and 07-03 (Store Browser screen) can proceed immediately
- `StoreListViewModel` and `StoreBrowserViewModel` consume `GetStoresUseCase` directly
- `LastRegistryPreferencesDataStore` is ready for registry picker use in the add-item sheet

---
*Phase: 07-romanian-store-browser*
*Completed: 2026-04-19*
