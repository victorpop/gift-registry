---
phase: 03-registry-item-management
plan: "04"
subsystem: android-ui
tags: [compose, navigation3, hilt, viewmodel, registry-crud]
dependency_graph:
  requires: ["03-03"]
  provides: ["registry-list-screen", "create-edit-registry-screen", "registry-detail-screen", "phase3-navigation"]
  affects: ["03-05", "04-reservation"]
tech_stack:
  added: []
  patterns: ["sealed-ui-state", "savedstatehandle-edit-mode", "observeuse-case-stateflow", "lazy-column-cards", "combinedclickable-overflow-menu"]
key_files:
  created:
    - app/src/main/java/com/giftregistry/ui/registry/list/RegistryListViewModel.kt
    - app/src/main/java/com/giftregistry/ui/registry/list/RegistryListScreen.kt
    - app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryViewModel.kt
    - app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryScreen.kt
    - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailViewModel.kt
    - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt
  modified:
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
decisions:
  - "User.uid (not .id) — User domain model uses uid field matching Firebase auth UID"
  - "combinedClickable for registry cards — enables both tap-to-open and long-press-for-menu without extra UI"
  - "MutableStateFlow fields in CreateRegistryViewModel — allows direct mutation from Compose without extra setter boilerplate"
metrics:
  duration: "4min"
  completed_date: "2026-04-06"
  tasks_completed: 3
  files_changed: 7
---

# Phase 03 Plan 04: Registry UI Screens Summary

Registry management screens (list as home, create/edit, detail with items) wired into Navigation3 with real-time Firestore observation via StateFlow ViewModels.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Registry list, create/edit, detail screens + ViewModels | 743e086 | 6 new files |
| 2 | Navigation3 wiring — replace HomeKey placeholder | 3c8a4c8 | AppNavigation.kt |
| 3 | Verify registry CRUD flow (auto-approved checkpoint) | — | — |

## What Was Built

**RegistryListViewModel** — Hilt ViewModel observing user registries via `ObserveRegistriesUseCase`. Sealed `RegistryListUiState` (Loading/Success/Error). Delete with error propagation via `MutableStateFlow<String?>`.

**RegistryListScreen** — Material3 Scaffold with TopAppBar (settings icon), ExtendedFAB (create), LazyColumn of registry cards. Each card uses `combinedClickable` for tap-to-detail and long-press overflow menu (edit/delete). Delete confirmation AlertDialog. All strings from `R.string.registry_*`.

**CreateRegistryViewModel** — `SavedStateHandle["registryId"]` for edit mode detection. Pre-fills form from `ObserveRegistryUseCase` when editing. Validates title (3–50 chars). Calls `CreateRegistryUseCase` or `UpdateRegistryUseCase` based on mode.

**CreateRegistryScreen** — Full form: OutlinedTextField for title, `ExposedDropdownMenuBox` for occasion, `DatePickerDialog` for event date, location/description fields, RadioButton row for visibility (public/private), Switch for notifications (REG-09). All strings externalized.

**RegistryDetailViewModel** — Dual StateFlow observation: `ObserveRegistryUseCase` for registry metadata, `ObserveItemsUseCase` for real-time item list (ITEM-07). `DeleteItemUseCase` and `DeleteRegistryUseCase` both injected.

**RegistryDetailScreen** — Registry info card (occasion chip, visibility badge, date, location, description). Items LazyColumn with `ItemStatusChip` composable showing Available (primaryContainer), Reserved (tertiaryContainer), Purchased (surfaceVariant). FAB for add item. TopAppBar overflow: Edit, Share (stub), Invite, Delete. Item overflow: Edit, Delete with confirmation dialogs.

**AppNavigation** — HomeKey now renders `RegistryListScreen`. Entries added for `CreateRegistryKey`, `EditRegistryKey`, `RegistryDetailKey`. Placeholder entries for `AddItemKey` and `EditItemKey` (Plan 05 will replace).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed `User.id` reference — domain model uses `uid`**
- **Found during:** Task 1 (first compile)
- **Issue:** Plan code samples used `authRepository.currentUser?.id` but the `User` domain model has `uid` field (matching Firebase auth UID)
- **Fix:** Changed `.id` to `.uid` in both `RegistryListViewModel` and `CreateRegistryViewModel`
- **Files modified:** RegistryListViewModel.kt, CreateRegistryViewModel.kt
- **Commit:** 743e086 (fix applied before commit)

## Known Stubs

- `AddItemKey` entry in AppNavigation renders `Text("Add Item — Plan 05")` — intentional placeholder, Plan 05 wires the real AddItemScreen
- `EditItemKey` entry renders `Text("Edit Item — Plan 05")` — intentional placeholder, Plan 05 wires the real EditItemScreen
- Share registry button in RegistryDetailScreen overflow has no action — deep link sharing is Phase 5 scope
- Coil image loading deferred: ItemCard uses placeholder icon instead of `AsyncImagePainter` — wire in Plan 05 when AddItemScreen brings image URLs

## Self-Check

### Files Created
- [x] RegistryListViewModel.kt — exists
- [x] RegistryListScreen.kt — exists
- [x] CreateRegistryViewModel.kt — exists
- [x] CreateRegistryScreen.kt — exists
- [x] RegistryDetailViewModel.kt — exists
- [x] RegistryDetailScreen.kt — exists
- [x] AppNavigation.kt — modified

### Commits
- [x] 743e086 — Task 1 (6 new screen/viewmodel files)
- [x] 3c8a4c8 — Task 2 (AppNavigation wired)

### Build
- [x] `./gradlew :app:compileDebugKotlin` exits 0 (verified twice)

## Self-Check: PASSED
