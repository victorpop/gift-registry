---
phase: 03-registry-item-management
plan: 01
subsystem: domain
tags: [kotlin, firebase, firestore, cloud-functions, affiliate-url, 2performant, emag, navigation3, domain-model]

# Dependency graph
requires:
  - phase: 02-android-core-auth
    provides: AuthRepository pattern, AppNavKeys, User domain model, Gradle build setup

provides:
  - Registry and Item domain models with full Firestore schema fields
  - RegistryRepository and ItemRepository interfaces (CRUD + observe contracts)
  - ItemStatus enum with fromString factory
  - OgMetadata data class for URL metadata fetch results
  - AffiliateUrlTransformer: EMAG to 2Performant tracking URL transformation
  - Navigation keys for all Phase 3 screens (CreateRegistry, RegistryDetail, AddItem, EditItem, EditRegistry)
  - Phase 3 string resources in EN and RO (50+ keys each)
  - Firebase Firestore and Functions Gradle dependencies

affects:
  - 03-02 (Firestore data layer implements RegistryRepository and ItemRepository)
  - 03-03 (UI screens use nav keys and string resources)
  - 04-reservation (uses Item.status and ItemStatus enum)

# Tech tracking
tech-stack:
  added:
    - firebase-firestore (via BoM 34.11.0)
    - firebase-functions (via BoM 34.11.0)
  patterns:
    - Pure Kotlin domain models: no Firebase imports in domain layer
    - Repository interfaces with Flow for observation and suspend fun for mutations
    - AffiliateUrlTransformer as object with merchantRules map for extensibility
    - URLEncoder.encode for affiliate redirect URL encoding

key-files:
  created:
    - app/src/main/java/com/giftregistry/domain/model/Registry.kt
    - app/src/main/java/com/giftregistry/domain/model/Item.kt
    - app/src/main/java/com/giftregistry/domain/model/ItemStatus.kt
    - app/src/main/java/com/giftregistry/domain/model/OgMetadata.kt
    - app/src/main/java/com/giftregistry/domain/registry/RegistryRepository.kt
    - app/src/main/java/com/giftregistry/domain/item/ItemRepository.kt
    - app/src/main/java/com/giftregistry/util/AffiliateUrlTransformer.kt
    - app/src/test/java/com/giftregistry/util/AffiliateUrlTransformerTest.kt
  modified:
    - gradle/libs.versions.toml (added firebase-firestore, firebase-functions)
    - app/build.gradle.kts (added firestore and functions deps)
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt (added Phase 3 nav keys)
    - app/src/main/res/values/strings.xml (added Phase 3 EN strings)
    - app/src/main/res/values-ro/strings.xml (added Phase 3 RO strings)
    - app/src/test/java/com/giftregistry/ui/settings/SettingsViewModelTest.kt (bug fix)

key-decisions:
  - "AffiliateUrlTransformer uses placeholder affiliate IDs — must be replaced with BuildConfig fields from local.properties before production"
  - "merchantRules map pattern enables adding new merchants without changing core transform logic"
  - "EMAG affiliate URL uses 2Performant event.2performant.com/events/click endpoint per D-08 research"

patterns-established:
  - "Domain models: pure Kotlin data classes with no Firebase imports (zero Android/Firebase dependencies in domain layer)"
  - "Repository interfaces: Flow for observation, suspend fun for mutations, Result wrapper for error handling"
  - "AffiliateUrlTransformer: object singleton with map of merchant domain suffix -> transform function"

requirements-completed: [REG-01, REG-02, REG-03, REG-04, REG-10, ITEM-01, ITEM-05, ITEM-06, ITEM-07, AFF-01, AFF-02, AFF-03, AFF-04]

# Metrics
duration: 4min
completed: 2026-04-06
---

# Phase 3 Plan 01: Foundation — Domain Models, Repository Interfaces, Affiliate URL Transformer Summary

**Firebase Firestore/Functions deps, Registry/Item/OgMetadata domain models, repository interfaces, AffiliateUrlTransformer with EMAG-to-2Performant transformation, Phase 3 nav keys and 50+ localized string resources**

## Performance

- **Duration:** 4 min
- **Started:** 2026-04-06T09:36:12Z
- **Completed:** 2026-04-06T09:40:18Z
- **Tasks:** 2 (+ 1 TDD RED commit)
- **Files modified:** 13

## Accomplishments

- Registry, Item, ItemStatus, and OgMetadata domain models created with all fields matching Firestore schema (zero Firebase imports)
- RegistryRepository and ItemRepository interfaces define full CRUD + observe contracts using Flow and suspend functions
- AffiliateUrlTransformer transforms EMAG URLs to 2Performant tracking URLs; passes through unknown merchants unchanged; 5 tests passing
- All Phase 3 navigation keys added (CreateRegistryKey, RegistryDetailKey, AddItemKey, EditItemKey, EditRegistryKey)
- 50+ Phase 3 string resources in both English and Romanian covering all registry and item management UI labels

## Task Commits

1. **Task 1 (RED): AffiliateUrlTransformerTest** - `fd9de07` (test)
2. **Task 1: Gradle deps, domain models, repository interfaces, nav keys** - `7dce2fd` (feat)
3. **Task 2: AffiliateUrlTransformer + string resources + SettingsViewModelTest fix** - `ce5d4ec` (feat)

## Files Created/Modified

- `gradle/libs.versions.toml` - Added firebase-firestore and firebase-functions library aliases
- `app/build.gradle.kts` - Added firestore and functions implementation deps
- `app/src/main/java/com/giftregistry/domain/model/Registry.kt` - Registry domain model with all Firestore schema fields
- `app/src/main/java/com/giftregistry/domain/model/Item.kt` - Item domain model with status, URLs, metadata fields
- `app/src/main/java/com/giftregistry/domain/model/ItemStatus.kt` - AVAILABLE/RESERVED/PURCHASED enum with fromString
- `app/src/main/java/com/giftregistry/domain/model/OgMetadata.kt` - OG metadata data class for URL fetch results
- `app/src/main/java/com/giftregistry/domain/registry/RegistryRepository.kt` - Registry CRUD + observe interface
- `app/src/main/java/com/giftregistry/domain/item/ItemRepository.kt` - Item CRUD + observe + fetchOgMetadata interface
- `app/src/main/java/com/giftregistry/util/AffiliateUrlTransformer.kt` - EMAG affiliate URL transformer with 2Performant
- `app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt` - Added 5 Phase 3 nav keys
- `app/src/main/res/values/strings.xml` - Phase 3 EN string resources (registry + item management)
- `app/src/main/res/values-ro/strings.xml` - Phase 3 RO string resources (registry + item management)
- `app/src/test/java/com/giftregistry/util/AffiliateUrlTransformerTest.kt` - 5 AffiliateUrlTransformer tests

## Decisions Made

- AffiliateUrlTransformer uses placeholder affiliate IDs (`PLACEHOLDER_UNIQUE_ID`, `PLACEHOLDER_AFF_CODE`, `PLACEHOLDER_CAMPAIGN_ID`) that must be replaced with BuildConfig fields from local.properties before production
- merchantRules map pattern enables extensibility — adding new merchants only requires adding a new map entry and transform function
- EMAG affiliate URL structure uses 2Performant `event.2performant.com/events/click` with URL-encoded redirect

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed SettingsViewModelTest missing signOutUseCase parameter**
- **Found during:** Task 2 (AffiliateUrlTransformerTest execution blocked by test compilation error)
- **Issue:** `SettingsViewModel` constructor added `signOutUseCase: SignOutUseCase` in Phase 2 Plan 4 but `SettingsViewModelTest` was never updated, causing `compileDebugUnitTestKotlin` to fail
- **Fix:** Added `mockk<SignOutUseCase>(relaxed = true)` mock and passed it to all three `SettingsViewModel(...)` instantiations in the test
- **Files modified:** `app/src/test/java/com/giftregistry/ui/settings/SettingsViewModelTest.kt`
- **Verification:** `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.util.AffiliateUrlTransformerTest"` now exits 0
- **Committed in:** `ce5d4ec` (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (Rule 1 - Bug)
**Impact on plan:** Pre-existing test compilation error blocked task execution. Fix required and necessary for any test run to succeed. No scope creep.

## Issues Encountered

None beyond the SettingsViewModelTest bug documented above.

## User Setup Required

None — no external service configuration required for this plan.

## Next Phase Readiness

- All domain contracts established; Phase 3 Plan 2 (Firestore data layer) can implement RegistryRepository and ItemRepository
- AffiliateUrlTransformer is functional with placeholder IDs — production affiliate IDs (2Performant unique ID, affiliate code, EMAG campaign ID) must be added to local.properties and wired via BuildConfig before Phase 3 Plan 5 or 6

---
*Phase: 03-registry-item-management*
*Completed: 2026-04-06*
