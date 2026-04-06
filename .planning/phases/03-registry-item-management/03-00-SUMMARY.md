---
phase: 03-registry-item-management
plan: "00"
subsystem: testing
tags: [mockk, turbine, coroutines-test, fake-repositories, unit-tests, domain-models]

# Dependency graph
requires:
  - phase: 02-android-core-auth
    provides: auth domain pattern (callbackFlow, runCatching, FakeAuthRepository) used as template for fakes

provides:
  - FakeRegistryRepository — in-memory MutableStateFlow fake implementing RegistryRepository
  - FakeItemRepository — in-memory MutableStateFlow fake implementing ItemRepository
  - 4 test stub files for use cases (CreateRegistry, ObserveItems, FetchOgMetadata, InviteToRegistry)
  - Domain model stubs: Registry, Item, ItemStatus, OgMetadata
  - Domain interface stubs: RegistryRepository, ItemRepository
  - Minimal use case stubs: CreateRegistryUseCase, ObserveItemsUseCase, FetchOgMetadataUseCase, InviteToRegistryUseCase

affects:
  - 03-01 (domain foundation — will flesh out Registry/Item models and RegistryRepository/ItemRepository)
  - 03-02 (item management — will flesh out use cases and AffiliateUrlTransformer)
  - 03-03 (invite flow — InviteToRegistryUseCaseTest is ready)
  - All subsequent Phase 3 plans that implement and turn tests GREEN

# Tech tracking
tech-stack:
  added:
    - MockK 1.13.17 (already in build.gradle.kts from prior setup)
    - Turbine 1.2.0 (already in build.gradle.kts from prior setup)
    - kotlinx-coroutines-test 1.9.0 (already in build.gradle.kts from prior setup)
  patterns:
    - MutableStateFlow-backed in-memory fake repository pattern (same as FakeAuthRepository)
    - shouldFail flag for simulating repository failures in tests
    - Turbine .test { awaitItem() } pattern for Flow assertions
    - seedItems() helper for pre-loading fake state before Flow observation

key-files:
  created:
    - app/src/main/java/com/giftregistry/domain/model/Registry.kt
    - app/src/main/java/com/giftregistry/domain/model/Item.kt
    - app/src/main/java/com/giftregistry/domain/model/ItemStatus.kt
    - app/src/main/java/com/giftregistry/domain/model/OgMetadata.kt
    - app/src/main/java/com/giftregistry/domain/registry/RegistryRepository.kt
    - app/src/main/java/com/giftregistry/domain/item/ItemRepository.kt
    - app/src/main/java/com/giftregistry/domain/usecase/CreateRegistryUseCase.kt
    - app/src/main/java/com/giftregistry/domain/usecase/ObserveItemsUseCase.kt
    - app/src/main/java/com/giftregistry/domain/usecase/FetchOgMetadataUseCase.kt
    - app/src/main/java/com/giftregistry/domain/usecase/InviteToRegistryUseCase.kt
    - app/src/test/java/com/giftregistry/data/registry/FakeRegistryRepository.kt
    - app/src/test/java/com/giftregistry/data/item/FakeItemRepository.kt
    - app/src/test/java/com/giftregistry/domain/usecase/CreateRegistryUseCaseTest.kt
    - app/src/test/java/com/giftregistry/domain/usecase/ObserveItemsUseCaseTest.kt
    - app/src/test/java/com/giftregistry/domain/usecase/FetchOgMetadataUseCaseTest.kt
    - app/src/test/java/com/giftregistry/domain/usecase/InviteToRegistryUseCaseTest.kt
  modified: []

key-decisions:
  - "Domain model stubs created in Wave 0 rather than waiting for Plan 01 — required for test stubs to compile"
  - "Minimal use case stubs (delegate directly to repository) created alongside tests — Plan 01 will replace these with full implementations"
  - "FakeItemRepository includes seedItems() helper to allow pre-loading state before Flow observation in tests"

patterns-established:
  - "Wave 0 test scaffolding pattern: create fakes + minimal stubs first so RED tests can be written before GREEN implementation"
  - "shouldFail boolean on fakes for simulating failure paths without MockK"
  - "Turbine cancelAndConsumeRemainingEvents() after Flow assertions in StateFlow-backed fakes"

requirements-completed:
  - REG-01
  - ITEM-07
  - AFF-01

# Metrics
duration: 8min
completed: 2026-04-06
---

# Phase 3 Plan 00: Wave 0 Test Scaffolding Summary

**In-memory fake repositories and compilable test stubs for Registry/Item domain using MutableStateFlow, MockK, and Turbine — establishing RED state for Phase 3 GREEN implementation**

## Performance

- **Duration:** ~8 min
- **Started:** 2026-04-06T09:31:00Z
- **Completed:** 2026-04-06T09:39:27Z
- **Tasks:** 2
- **Files modified:** 16

## Accomplishments
- Created FakeRegistryRepository and FakeItemRepository with MutableStateFlow internals, shouldFail flags, and seedItems() helper
- Created 4 test stub files covering CreateRegistry, ObserveItems (with Turbine Flow testing), FetchOgMetadata, and InviteToRegistry use cases
- Added domain model stubs (Registry, Item, ItemStatus, OgMetadata) and interface stubs (RegistryRepository, ItemRepository) needed for tests to compile before Plan 01 runs
- All 8 unit tests GREEN (testDebugUnitTest BUILD SUCCESSFUL)

## Task Commits

Each task was committed atomically:

1. **Task 1: Add test dependencies** - `6ca37f7` (chore) — verified MockK, Turbine, coroutines-test already present
2. **Task 2: Create fake repositories and test stubs** - `7ae58f6` (feat) — 16 files created

## Files Created/Modified

- `app/src/main/java/com/giftregistry/domain/model/Registry.kt` — Registry data class with all Firestore-schema fields
- `app/src/main/java/com/giftregistry/domain/model/Item.kt` — Item data class with status, affiliate URLs, and metadata fields
- `app/src/main/java/com/giftregistry/domain/model/ItemStatus.kt` — AVAILABLE/RESERVED/PURCHASED enum
- `app/src/main/java/com/giftregistry/domain/model/OgMetadata.kt` — title, imageUrl, price for OG extraction results
- `app/src/main/java/com/giftregistry/domain/registry/RegistryRepository.kt` — Observe + CRUD + invite interface
- `app/src/main/java/com/giftregistry/domain/item/ItemRepository.kt` — Observe + CRUD + fetchOgMetadata interface
- `app/src/main/java/com/giftregistry/domain/usecase/CreateRegistryUseCase.kt` — minimal stub (delegates to repo)
- `app/src/main/java/com/giftregistry/domain/usecase/ObserveItemsUseCase.kt` — minimal stub (delegates to repo)
- `app/src/main/java/com/giftregistry/domain/usecase/FetchOgMetadataUseCase.kt` — minimal stub (delegates to repo)
- `app/src/main/java/com/giftregistry/domain/usecase/InviteToRegistryUseCase.kt` — minimal stub (delegates to repo)
- `app/src/test/java/com/giftregistry/data/registry/FakeRegistryRepository.kt` — in-memory MutableStateFlow fake
- `app/src/test/java/com/giftregistry/data/item/FakeItemRepository.kt` — in-memory MutableStateFlow fake with seedItems()
- `app/src/test/java/com/giftregistry/domain/usecase/CreateRegistryUseCaseTest.kt` — success + failure paths
- `app/src/test/java/com/giftregistry/domain/usecase/ObserveItemsUseCaseTest.kt` — Turbine Flow assertions, registryId filtering
- `app/src/test/java/com/giftregistry/domain/usecase/FetchOgMetadataUseCaseTest.kt` — metadata success + failure
- `app/src/test/java/com/giftregistry/domain/usecase/InviteToRegistryUseCaseTest.kt` — inviteLog assertion + failure

## Decisions Made

- Domain model stubs and minimal use case stubs created in Wave 0 rather than waiting for Plan 01 — required for test compilation. Plan 01 will replace/extend these with full implementations including Firestore field mappings, navigation keys, and string resources.
- `shouldFail` flag on fakes preferred over MockK for simple repository failure simulation — keeps tests readable without mock setup overhead.
- `seedItems()` helper added to FakeItemRepository so tests can set up pre-existing state before subscribing to the Flow (needed for Turbine `awaitItem()` pattern).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Created domain model stubs and use case stubs to unblock test compilation**
- **Found during:** Task 2 (creating fake repositories and test stubs)
- **Issue:** Test stubs instantiate use cases (`CreateRegistryUseCase(fakeRepo)`) but no domain models or interfaces existed yet since Plan 01 hasn't run
- **Fix:** Created minimal domain models (Registry, Item, ItemStatus, OgMetadata), interfaces (RegistryRepository, ItemRepository), and use case stubs (CreateRegistryUseCase, ObserveItemsUseCase, FetchOgMetadataUseCase, InviteToRegistryUseCase) alongside the test files. Plan 01 will extend these with full field sets and implementations.
- **Files modified:** 10 main source files created
- **Verification:** `./gradlew :app:compileDebugUnitTestKotlin` and `:app:testDebugUnitTest` BUILD SUCCESSFUL
- **Committed in:** 7ae58f6 (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (blocking — test compilation prerequisite)
**Impact on plan:** Blocking fix necessary for Wave 0 to be meaningful. Domain model stubs match the exact interface specified in the plan's context block. No scope creep.

## Issues Encountered

- Worktree started on initial commit only — needed `git merge main` to bring app code into the worktree branch. Resolved immediately.
- Missing `local.properties` (Android SDK path) in worktree — copied from main repo to unblock Gradle. Not committed (gitignored).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Wave 0 scaffolding complete: FakeRegistryRepository, FakeItemRepository, and 4 test stubs are ready for GREEN implementation
- Plan 01 can extend Registry/Item models with additional Firestore fields, add navigation keys, and add string resources
- Plan 02 will implement use cases (AddItem, UpdateItem, DeleteItem, etc.) turning those tests GREEN
- ObserveItemsUseCaseTest with Turbine covers ITEM-07 real-time status requirement

---
*Phase: 03-registry-item-management*
*Completed: 2026-04-06*

## Self-Check: PASSED

- All 10 key files verified present on disk
- Task commits 6ca37f7 and 7ae58f6 verified in git log
- `./gradlew :app:testDebugUnitTest` BUILD SUCCESSFUL (8 tests green)
