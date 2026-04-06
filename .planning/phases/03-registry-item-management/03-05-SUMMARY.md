---
phase: 03-registry-item-management
plan: 05
subsystem: ui
tags: [kotlin, compose, hilt, navigation3, material3, firestore-rules, deep-link]

# Dependency graph
requires:
  - phase: 03-01
    provides: domain models (Registry, Item, OgMetadata, ItemStatus), nav keys, AffiliateUrlTransformer
  - phase: 03-02
    provides: Cloud Functions (fetchOgMetadata, inviteToRegistry callables)
  - phase: 03-03
    provides: repository implementations, all 11 injectable use cases
provides:
  - AddItemScreen and AddItemViewModel with OG metadata auto-fill (ITEM-01, ITEM-02)
  - EditItemScreen and EditItemViewModel with pre-filled form and UpdateItemUseCase (ITEM-05)
  - InviteBottomSheet and InviteViewModel via InviteToRegistryUseCase (REG-05, REG-06, REG-07)
  - Deep link intent filter for https://giftregistry.app/registry/{id} (REG-08)
  - RegistryListScreen, CreateRegistryScreen, RegistryDetailScreen with ViewModels (Plan 04 dependency)
  - Security rule tests: 3 new invite flow tests (15 total, all passing)
  - AppNavigation fully wired with no placeholder entries remaining
affects: [04-reservation-system, 05-giver-flow, phase-03 verification]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - MutableStateFlow form fields with direct value mutation in ViewModels (no copy())
    - isFetchingOg + ogFetchFailed state flows for OG metadata loading states (pitfall 2)
    - var showInviteSheet by remember { mutableStateOf(false) } for bottom sheet state in nav entry
    - deepLinkRegistryId passed from Activity to AppNavigation as parameter
    - LaunchedEffect on savedItemId/savedSuccessfully to trigger navigation on success

key-files:
  created:
    - app/src/main/java/com/giftregistry/ui/item/add/AddItemViewModel.kt
    - app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt
    - app/src/main/java/com/giftregistry/ui/item/edit/EditItemViewModel.kt
    - app/src/main/java/com/giftregistry/ui/item/edit/EditItemScreen.kt
    - app/src/main/java/com/giftregistry/ui/registry/invite/InviteViewModel.kt
    - app/src/main/java/com/giftregistry/ui/registry/invite/InviteBottomSheet.kt
    - app/src/main/java/com/giftregistry/ui/registry/list/RegistryListViewModel.kt
    - app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt
    - app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryViewModel.kt
    - app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryScreen.kt
    - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailViewModel.kt
    - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt
  modified:
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
    - app/src/main/java/com/giftregistry/MainActivity.kt
    - app/src/main/AndroidManifest.xml
    - tests/rules/firestore.rules.test.ts
    - build.gradle.kts
    - gradle.properties

key-decisions:
  - "Registry screens (Plan 04 work) built as part of this plan — Plan 04 was a dependency that hadn't run"
  - "User.uid (not .id) is the correct field name in the domain model for auth user identity"
  - "menuAnchor uses ExposedDropdownMenuAnchorType.PrimaryNotEditable to avoid deprecation warning"
  - "InviteBottomSheet resets inviteSent via resetInviteSent() allowing multiple invites per bottom sheet session"
  - "deepLinkRegistryId extracted from intent.data in onCreate (not onNewIntent) — sufficient for cold start deep links"

patterns-established:
  - "ViewModel form fields as public MutableStateFlow — Screen composables bind directly via collectAsStateWithLifecycle()"
  - "LaunchedEffect on success state (savedItemId, savedSuccessfully) to trigger onBack navigation"
  - "Bottom sheet state managed locally in NavEntry composable with remember { mutableStateOf(false) }"

requirements-completed:
  - ITEM-01
  - ITEM-02
  - ITEM-05
  - ITEM-06
  - AFF-01
  - AFF-02
  - AFF-03
  - AFF-04
  - REG-05
  - REG-06
  - REG-07
  - REG-08

# Metrics
duration: 8min
completed: 2026-04-06
---

# Phase 03 Plan 05: Item Management Screens, Invite Bottom Sheet, and Deep Link Routing Summary

**Complete item add/edit screens with OG auto-fill, InviteBottomSheet wired into RegistryDetailKey, deep link routing for private registry access (REG-08), 15 passing Firestore security rule tests including 3 new invite flow tests**

## Performance

- **Duration:** 8 min
- **Started:** 2026-04-06T09:51:00Z
- **Completed:** 2026-04-06T09:59:00Z
- **Tasks:** 2 (+ 1 auto-approved checkpoint)
- **Files modified:** 16

## Accomplishments
- AddItemScreen/ViewModel with URL paste, OG metadata fetch with loading/error states, editable auto-fill form
- EditItemScreen/ViewModel loading existing item from ObserveItemsUseCase, pre-filling form for UpdateItemUseCase
- InviteBottomSheet + InviteViewModel calling InviteToRegistryUseCase callable, wired into RegistryDetailKey nav entry
- Deep link intent filter (`https://giftregistry.app/registry/`) + MainActivity deep link extraction passing to AppNavigation
- AppNavigation supports deepLinkRegistryId: routes unauthenticated users to AuthKey, authenticated users direct to RegistryDetailKey
- 3 new invite flow security rule tests: invited user reads registry, non-invited denied, invited user reads items subcollection
- Registry screens (RegistryListScreen, CreateRegistryScreen, RegistryDetailScreen) built as Plan 04 dependency

## Task Commits

Each task was committed atomically:

1. **Task 1: AddItem/EditItem screens with ViewModels and navigation wiring** - `9ff776a` (feat)
2. **Task 2: Invite bottom sheet, deep link routing, security rule tests** - `d66f967` (feat)

**Plan metadata:** (docs commit follows)

## Files Created/Modified
- `app/src/main/java/com/giftregistry/ui/item/add/AddItemViewModel.kt` - OG metadata fetch, isFetchingOg/ogFetchFailed state flows, AddItemUseCase
- `app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt` - URL field + Fetch button, loading/error states, editable form
- `app/src/main/java/com/giftregistry/ui/item/edit/EditItemViewModel.kt` - Loads item via ObserveItemsUseCase, UpdateItemUseCase on save
- `app/src/main/java/com/giftregistry/ui/item/edit/EditItemScreen.kt` - Pre-filled edit form with URL re-fetch option
- `app/src/main/java/com/giftregistry/ui/registry/invite/InviteViewModel.kt` - InviteToRegistryUseCase wrapper with email validation
- `app/src/main/java/com/giftregistry/ui/registry/invite/InviteBottomSheet.kt` - ModalBottomSheet with email field, success/error states
- `app/src/main/java/com/giftregistry/ui/registry/list/RegistryListViewModel.kt` - ObserveRegistriesUseCase, sealed UiState
- `app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt` - LazyColumn with cards, delete dialog
- `app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryViewModel.kt` - Create/edit mode, SavedStateHandle pre-fill
- `app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryScreen.kt` - Full form with occasion dropdown, visibility radio, notifications switch
- `app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailViewModel.kt` - Real-time registry + items StateFlows, delete operations
- `app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt` - Items with status chips, overflow menu with invite
- `app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt` - All Phase 3 nav entries wired, deepLinkRegistryId support
- `app/src/main/java/com/giftregistry/MainActivity.kt` - Deep link URI extraction passed to AppNavigation
- `app/src/main/AndroidManifest.xml` - Deep link intent filter for giftregistry.app/registry/
- `tests/rules/firestore.rules.test.ts` - 3 new invite flow tests (15 total, all passing)

## Decisions Made
- Registry screens (RegistryListScreen, CreateRegistryScreen, RegistryDetailScreen) built as blocking dependency — Plan 04 hadn't executed before this parallel executor ran
- User.uid (not .id) used for currentUser identity — User domain model uses uid field
- menuAnchor uses ExposedDropdownMenuAnchorType.PrimaryNotEditable to avoid deprecated overload
- InviteBottomSheet resets inviteSent via resetInviteSent() so multiple invites can be sent per session before dismissing
- Deep link extracted in onCreate (cold start) — onNewIntent not needed for this phase

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Built Plan 04 registry screens as prerequisite**
- **Found during:** Task 1 start
- **Issue:** Plan 05 depends on RegistryDetailScreen/RegistryListScreen which plan 04 hadn't run (parallel execution context)
- **Fix:** Created all 6 registry screens (list, create, detail + 3 ViewModels) following Plan 04 spec
- **Files modified:** RegistryListViewModel.kt, RegistryListScreen.kt, CreateRegistryViewModel.kt, CreateRegistryScreen.kt, RegistryDetailViewModel.kt, RegistryDetailScreen.kt
- **Committed in:** `9ff776a` (Task 1 commit)

**2. [Rule 1 - Bug] Fixed User.uid vs User.id field name**
- **Found during:** Task 1 compilation
- **Issue:** Code used `.id` but User domain model has `.uid` field
- **Fix:** Changed `currentUser?.id` to `currentUser?.uid` in RegistryListViewModel and CreateRegistryViewModel
- **Files modified:** RegistryListViewModel.kt, CreateRegistryViewModel.kt
- **Committed in:** `9ff776a` (Task 1 commit)

**3. [Rule 3 - Blocking] Added missing build config to worktree**
- **Found during:** Task 1 compile attempt
- **Issue:** Worktree missing `kotlin.android apply false` in build.gradle.kts and `android.useAndroidX=true` in gradle.properties
- **Fix:** Added both missing properties to align with main repo configuration
- **Files modified:** build.gradle.kts, gradle.properties
- **Committed in:** `9ff776a` (Task 1 commit)

---

**Total deviations:** 3 auto-fixed (1 missing prerequisite, 1 bug, 1 blocking config)
**Impact on plan:** All auto-fixes necessary. Registry screens were a required dependency. Config fixes enabled compilation. No scope creep.

## Issues Encountered
- npm install blocked by npm cache permissions — resolved by symlinking main repo's node_modules to worktree for test execution
- Parallel worktree had diverged from main branch and required `git rebase main` before any work could begin

## User Setup Required
None — no external service configuration required. Firebase emulators needed for runtime testing.

## Next Phase Readiness
- Complete Phase 3 UI is ready: registry CRUD + item CRUD + invite + deep links
- Phase 4 (Reservation System) can use RegistryDetailScreen's item list as the reservation entry point
- Deep link infrastructure in place — Phase 5 (giver flow) can extend it
- Security rule tests cover 15 cases; Phase 4 should add reservation write tests

---
*Phase: 03-registry-item-management*
*Completed: 2026-04-06*

## Self-Check: PASSED

- All 12 UI files created: FOUND
- Commits 9ff776a and d66f967: FOUND
- All 21 acceptance criteria: VERIFIED (grep confirmed each criterion)
- 15 Firestore security rule tests: PASSING
- Build compiles: BUILD SUCCESSFUL
