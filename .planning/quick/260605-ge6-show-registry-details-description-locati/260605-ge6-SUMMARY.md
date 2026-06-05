---
phase: quick-260605-ge6
plan: 01
subsystem: android-notifications
tags: [compose, invite-sheet, registry-details, reuse]
requires:
  - "InviteResponseViewModel (existing state machine)"
  - "ObserveRegistryUseCase (live registry Flow)"
  - "RegistryDetailsSection (registry-page details component)"
provides:
  - "InviteResponseViewModel.registry: StateFlow<Registry?> + observeRegistry(registryId)"
  - "Invite sheet renders shared RegistryDetailsSection (description/location/date) above CTAs"
affects:
  - "app/src/main/java/com/giftregistry/ui/notifications/InviteResponseViewModel.kt"
  - "app/src/main/java/com/giftregistry/ui/notifications/InviteResponseSheet.kt"
tech-stack:
  added: []
  patterns:
    - "Per-call driver MutableStateFlow<String?> + flatMapLatest into use case (vs SavedStateHandle arg)"
    - ".catch { emit(null) }.stateIn(Eagerly, null) degrade-to-null on denied/failed read"
    - "Live-data-with-payload-fallback to avoid empty-flash before the live read resolves"
key-files:
  created: []
  modified:
    - "app/src/main/java/com/giftregistry/ui/notifications/InviteResponseViewModel.kt"
    - "app/src/main/java/com/giftregistry/ui/notifications/InviteResponseSheet.kt"
decisions:
  - "D1: data source = live registry (client fetch), not payload-only — gives full event context incl. location"
  - "D4: eventLocation is live-registry-only (payload carries no location field)"
  - "D5: accept/decline/retry/reset/State/Action kept byte-identical"
metrics:
  duration: "3min"
  tasks: 2
  files: 2
  completed: 2026-06-05
---

# Quick 260605-ge6: Show Registry Details in Invite Sheet Summary

Surfaced the registry's description, location, and date/time inside the invite-response bottom sheet — above the Accept/Decline buttons — by reusing the registry-page `RegistryDetailsSection`, fed by the live registry (client fetch) with a payload fallback so the block never flashes empty.

## What Changed

**Task 1 — `InviteResponseViewModel` exposes the live registry (`d8e8e88`)**
- Injected `ObserveRegistryUseCase`.
- Added a per-call driver `_registryId: MutableStateFlow<String?>` plus a derived `registry: StateFlow<Registry?>` built with `filterNotNull().flatMapLatest { observeRegistryUseCase(it) }.catch { emit(null) }.stateIn(viewModelScope, SharingStarted.Eagerly, null)`. The `registryId` arrives per-call (not via SavedStateHandle), so the driver flow + `flatMapLatest` is used instead of the direct `observeRegistryUseCase(registryId)` form from `RegistryDetailViewModel`.
- Added `observeRegistry(registryId)` to start/refresh observation from the sheet.
- `accept()`, `decline()`, `retry()`, `reset()`, the `State` sealed interface, and the `Action` enum are byte-identical (D5).

**Task 2 — `InviteResponseSheet` reuses `RegistryDetailsSection` (`99f8bfb`)**
- Collected `viewModel.registry` and added `LaunchedEffect(registryId) { viewModel.observeRegistry(registryId) }`.
- Deleted the two ad-hoc payload blocks (the `payload["description"]` text and the `DateUtils.formatDateTime` event-date label).
- Replaced both with a single `RegistryDetailsSection(description = registry?.description ?: payload["description"], eventLocation = registry?.eventLocation, eventDateMs = registry?.eventDateMs ?: payload["eventDateMs"]?.toLongOrNull())`.
- Kept the trailing `Spacer(height = spacing.gap20)` for breathing room above the error banner / buttons (no leading Spacer — the component has its own internal vertical padding).
- Removed the now-unused `android.text.format.DateUtils`, `androidx.compose.ui.platform.LocalContext`, and bare `androidx.compose.runtime.remember` imports; added `com.giftregistry.ui.registry.detail.RegistryDetailsSection`.

## Behavior Trace

- **Registry loads** → `registry` non-null → `RegistryDetailsSection` receives live description/location/date → all three render via the shared component, matching the registry page.
- **Read denied/slow** → `.catch { emit(null) }` keeps `registry == null` → fallback feeds `payload["description"]` + parsed `payload["eventDateMs"]`, `eventLocation == null` → component renders description + date pill (no location), no crash, no emptier-than-today flash.
- **No registry details AND empty payload** → all three args null/blank → component's early `return` renders nothing (no DETAILS header, no empty-space artifacts).

## Verification

- `./gradlew :app:compileDebugKotlin` → **BUILD SUCCESSFUL** (after both tasks).
- Exactly two source files changed: the VM and the sheet (`git diff --name-only d8e8e88~1 HEAD -- app/src/**`).
- No `strings.xml` edits (`git diff -- app/src/main/res/values*/strings.xml` empty).
- `RegistryDetailsSection.kt` untouched.
- Exactly one `RegistryDetailsSection(...)` call in the sheet; `grep "DateUtils|eventDateLabel"` returns nothing; `LaunchedEffect(registryId)` + `viewModel.observeRegistry` present.
- Hero, "X is inviting you" title, registry-name text, error banner, Accept/Decline buttons, and decline dialog all remain.

## Build Prerequisite Note

This ran in an isolated git worktree. The gitignored `local.properties` and `app/google-services.json` were copied in from the main checkout `/Users/victorpop/ai-projects/gift-registry/` to satisfy Gradle. They are gitignored and were NOT committed (clean `git status`).

## Deviations from Plan

None — plan executed exactly as written.

## Known Stubs

None. The `eventLocation` being live-registry-only (null on fallback) is intentional per LOCKED decision D4, not a stub — the payload has no location field to fall back to.

## Self-Check: PASSED

- FOUND: app/src/main/java/com/giftregistry/ui/notifications/InviteResponseViewModel.kt
- FOUND: app/src/main/java/com/giftregistry/ui/notifications/InviteResponseSheet.kt
- FOUND commit: d8e8e88 (Task 1)
- FOUND commit: 99f8bfb (Task 2)
