---
phase: quick/260510-noi
plan: 01
subsystem: ui
tags: [android, jetpack-compose, registry, create-form, edit-form, i18n, tdd, regression-pin, viewmodel]

requires:
  - phase: 03-registry-item-management
    provides: "CreateRegistryViewModel.description StateFlow + onSave's ifBlank{null} mapping + Registry.description domain field + RegistryDto.description + RegistryRepositoryImpl.toMap/toUpdateMap wiring"
  - phase: 05-web-fallback
    provides: "Web mapper firestore-mapping.ts + RegistryHeader.tsx already read and render description (no web changes needed)"
provides:
  - "Multi-line `Description` OutlinedTextField on Android Add/Edit Registry form (minLines=3, maxLines=5), bound to viewModel.description with a 500-char soft cap at input"
  - "CreateRegistryViewModelDescriptionTest (3 tests) pinning round-trip contract: typed → onSave, blank → null, edit-mode hydration from observed Registry"
affects: [registry-create-flow, registry-edit-flow, future-description-display-on-detail-screen]

tech-stack:
  added: []
  patterns:
    - "UI-only wiring fix: when end-to-end persistence + i18n strings already exist from prior phases, the missing piece is sometimes just the composable. Identify via grep pass before re-implementing the wire."
    - "Regression-pin TDD: tests that PASS immediately because the contract already works — locking it so future refactors can't silently break the round-trip"
    - "Soft client-side char cap via `onValueChange = { vm.field.value = it.take(N) }` — no Firestore schema limit needed; UX-only guardrail"

key-files:
  created:
    - "app/src/test/java/com/giftregistry/ui/registry/create/CreateRegistryViewModelDescriptionTest.kt — 3-test regression pin (159 lines)"
  modified:
    - "app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryScreen.kt — added Description OutlinedTextField below the Place field"

key-decisions:
  - "Multi-line input sized larger than AddItemScreen notes precedent (3-5 vs 2-4) per the brief's call for a roomier description input"
  - "500-char soft cap at INPUT (take(500)) rather than VM/repository validation — the field is optional and Firestore has no schema limit; this is purely a UX guardrail to prevent runaway paste"
  - "No DTO/mapper/strings/VM/web changes — pre-flight grep confirmed every layer below the composable already shipped. The field was silently dropped only because no UI surfaced it."
  - "Regression-pin tests passed immediately on RED gate — intentional and documented. Tests lock the existing contract; without them, a future refactor could silently regress the round-trip."

patterns-established:
  - "Pre-flight investigation matters: before adding a 'missing' field, grep DTO → mapper → repository → VM → strings → web. The simplest cases are 'just wire the composable.'"
  - "Regression-pin tests are valid TDD: a test that passes on RED locks a contract; without it, the contract is implicit and can silently regress."

requirements-completed:
  - QUICK-NOI-01

duration: ~15min
completed: 2026-05-12
---

# Quick Task 260510-noi: Android Add Registry Description Field Summary

**Wired the existing `Registry.description` field through to the Android Add/Edit Registry form's UI — added a multi-line `Description` OutlinedTextField below the Place field plus a 3-test regression pin on the round-trip contract.**

## Performance

- **Duration:** ~15 min
- **Completed:** 2026-05-12
- **Tasks:** 2 (1 TDD auto + 1 human-verify checkpoint)
- **Files modified:** 2 (1 created test, 1 modified screen)

## Accomplishments

- **Owner can now capture an occasion description.** Multi-line text input (3-5 lines visible) labeled "Description" / "Descriere" renders below the Place field on both Add and Edit Registry forms. Persists to `registries/{id}.description` in Firestore. Edit mode hydrates the existing description into the field on screen open.
- **Localized.** Uses pre-existing `R.string.registry_description_label` and `R.string.registry_description_hint` resources (EN + RO already shipped in prior phases).
- **Optional field, no validation gate.** Empty submission still creates the registry; existing `onSave()` `ifBlank { null }` mapping preserved.
- **500-char soft cap.** Input clipped at 500 chars via `it.take(500)` — runaway paste protection without a Firestore schema limit.
- **Regression-pin test added.** `CreateRegistryViewModelDescriptionTest` (3 tests, all GREEN) locks the existing VM contract so a future refactor cannot silently break the round-trip, the blank→null mapping, or the edit-mode hydration.

## Task Commits

TDD cycle (RED → GREEN → human-verify):

1. **Task 1a: RED — pin description round-trip contract (3 tests)** — `f78baa6` (test)
2. **Task 1b: GREEN — wire description OutlinedTextField on Add/Edit Registry form** — `44f8fb8` (feat)
3. **Task 2: Human-verify checkpoint** — no commit (device walkthrough)

**Plan metadata:** committed alongside this SUMMARY.

## Files Created/Modified

- `app/src/test/java/com/giftregistry/ui/registry/create/CreateRegistryViewModelDescriptionTest.kt` (created, 159 lines) — 3 tests using MockK + StateFlow + slot capture, mirroring `CreateRegistryViewModelEventTimeTest` structure: (1) typed description round-trips through `onSave()` to `CreateRegistryUseCase`, (2) blank description maps to `null` (existing `ifBlank` contract), (3) edit-mode hydration populates `description.value` from observed Registry.
- `app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryScreen.kt` (modified, +22 lines) — added `val description by viewModel.description.collectAsStateWithLifecycle()` alongside the sibling state hoists; appended a multi-line `OutlinedTextField` (`minLines = 3`, `maxLines = 5`) below the eventLocation field, using `R.string.registry_description_label` / `_hint`, `giftMaisonFieldColors()`, `shapes.radius12`, and `onValueChange = { viewModel.description.value = it.take(500) }`.

## Automated Gates

| Gate | Command | Result |
| ---- | ------- | ------ |
| Description round-trip regression suite | `./gradlew :app:testDebugUnitTest --tests "...CreateRegistryViewModelDescriptionTest"` | 3/3 GREEN |
| Kotlin compile (proves new collectAsStateWithLifecycle binding compiles) | `./gradlew :app:compileDebugKotlin` | OK |
| No regressions in sibling VM tests | `CreateRegistryViewModelCoverTest` + `CreateRegistryViewModelEventTimeTest` | still GREEN |

## Why no DTO/mapper/strings work was needed

Pre-flight grep across the codebase confirmed every layer below the composable was already wired from prior phases:

1. **Domain:** `Registry.description: String?` in `domain/model/Registry.kt:11` — present
2. **DTO:** `RegistryDto.description: String?` in `data/model/RegistryDto.kt:11` — present
3. **Repository read path:** `RegistryRepositoryImpl.toDomain` propagates description — present
4. **Repository write paths:** `toMap` (create) and `toUpdateMap` (update) both write `"description"` to Firestore — present
5. **ViewModel state:** `CreateRegistryViewModel.description: MutableStateFlow<String>` — present
6. **ViewModel save:** `onSave()` maps `description.value.ifBlank { null }` — present
7. **ViewModel edit-mode hydration:** `description.value = registry.description ?: ""` — present
8. **i18n strings:** `registry_description_label` + `registry_description_hint` in BOTH `values/strings.xml` + `values-ro/strings.xml` — present
9. **Web fallback:** `web/src/lib/firestore-mapping.ts` reads description; `web/src/features/registry/RegistryHeader.tsx:60` renders it — present

The ONLY missing piece was the OutlinedTextField in `CreateRegistryScreen.kt`. Field was being silently dropped because no UI rendered it.

## RED → GREEN Cycle

- **RED (Task 1a, commit `f78baa6`):** Wrote the 3-test regression pin. Because the VM contract already existed end-to-end, all 3 tests passed immediately. This is intentional and is the regression-pin pattern: the test locks an existing contract before a UI change ships, so a future refactor (e.g. someone replacing `ifBlank { null }` with `.trim()`) fails fast in `:app:testDebugUnitTest`.
- **GREEN (Task 1b, commit `44f8fb8`):** Added the composable. Re-ran the test suite; 3/3 still GREEN. `:app:compileDebugKotlin` clean.

## Human Verification

**Status:** approved.

User confirmed via real device usage:
- Description multi-line field renders below the Place field on Add Registry.
- Typed text persists to Firestore (visible via Firestore console / refresh).
- Edit mode hydrates the saved description back into the field.
- Empty submission is valid (description is optional).
- 500-char soft cap holds at input.

## Decisions Made

- **3-5 lines visible (not the AddItemScreen 2-4 precedent).** The brief explicitly called for a roomier description input. The sibling `notes` field on AddItemScreen uses `minLines=2, maxLines=4` — this one is one line larger on each end to give the description more vertical breathing room.
- **No length-validation gate in `onSave()`.** Description must remain optional — adding `description.length > 0` validation would break the empty-submission UX. The 500-char cap is INPUT-only (`take(500)`), not a save-gate.
- **`collectAsStateWithLifecycle` hoisted alongside siblings, not inline.** Consistent with the existing pattern (lines 109-117 of `CreateRegistryScreen.kt` already hoist `title`, `occasion`, `eventLocation`, etc.).

## Deviations from Plan

None — plan executed exactly as written. The pre-flight investigation accurately scoped the work to "two files: one new test, one composable edit."

## Issues Encountered

None.

## User Setup Required

None — no external service configuration required.

## Known Stubs

None. End-to-end path is wired: input → ViewModel.description StateFlow → onSave() → ifBlank{null} → RegistryDto.description → Firestore. Edit mode round-trips correctly. Web fallback already renders it (confirmed via grep in pre-flight).

## Next Phase Readiness

- **Description visible on owner forms.** Owner can capture occasion details ("Sunday brunch reception, smart casual dress code") on registry creation/edit.
- **Detail screen rendering on Android:** Out of scope for this task, but worth flagging — `RegistryDetailHero` does not currently render description on the Android side. Web already does. A future phase or quick task may want to surface description on the Android detail screen for parity.
- **Latent: paste of >500 chars silently truncates.** No UX feedback (toast/snackbar) on truncation. If a user pastes a 2000-char block, it silently becomes 500 chars. Probably fine for this field (it's optional anyway and the truncation is visible inline), but flagged here for future polish.

## Self-Check: PASSED

Verified after writing this SUMMARY:
- `app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryScreen.kt` — exists.
- `app/src/test/java/com/giftregistry/ui/registry/create/CreateRegistryViewModelDescriptionTest.kt` — exists.
- Commit `f78baa6` (RED, Task 1a, test) — present in `git log`.
- Commit `44f8fb8` (GREEN, Task 1b, feat) — present in `git log`.

---

*Phase: quick/260510-noi*
*Completed: 2026-05-12*
