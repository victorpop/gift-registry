---
phase: 12-registry-cover-photo-themed-placeholder
plan: 04
subsystem: ui
tags: [viewmodel, compose, hilt, firebase-storage, modal-bottom-sheet, kotlin, i18n, photo-picker]

# Dependency graph
requires:
  - phase: 12-01 (Wave 0 RED)
    provides: "CoverPhotoSelection sealed interface; CreateRegistryViewModelCoverTest contract; firebase-storage classpath"
  - phase: 12-02 (data layer)
    provides: "RegistryRepository.newRegistryId() override; StorageRepository + CoverImageProcessor impls; Pitfall 1 imageUrl roundtrip; PresetCatalog real lookup table"
  - phase: 12-03 (UI primitives)
    provides: "CoverPhotoPickerInline + CoverPhotoPickerSheet + HeroImageOrPlaceholder + isCoverPickerEnabled"
  - phase: 11-registry-detail-create-add-item-redesign
    provides: "CreateRegistryScreen Phase-11 form layout; RegistryDetailScreen + RegistryDetailHero + isOwner derivation precedent"
  - phase: 02-android-core-auth
    provides: "AuthRepository.authState Flow; runCatching pattern; zero-Firebase-imports-in-domain invariant"

provides:
  - "CreateRegistryViewModel: coverPhotoSelection MutableStateFlow + upload-BEFORE-write onSave() (D-07 + Pitfall 2)"
  - "CreateRegistryViewModel: occasion.collect collector clearing Preset on occasion change (D-11)"
  - "CreateRegistryScreen: CoverPhotoPickerInline above OccasionTileGrid + ModalBottomSheet host gated by isCoverPickerEnabled"
  - "RegistryDetailHero: optional onCoverTap parameter; clickable wraps image area only (D-13 owner-only)"
  - "RegistryDetailViewModel: isOwner StateFlow + coverPhotoSelection rehydration + onCoverPhotoSelectionChanged with same upload-BEFORE-write contract"
  - "RegistryRepositoryImpl.createRegistry honours pre-set registry.id via new FirestoreDataSource.createRegistryWithId path (zero orphan documents on upload failure)"
  - "10 cover_photo_* string keys in BOTH values/strings.xml AND values-ro/strings.xml (I18N-02)"

affects:
  - "12-05 (Plan 05): StyleGuidePreview entries + on-device UAT validate the wired flows end-to-end against real Firebase Storage + Firestore"
  - "Future plans: any new ViewModel needing the upload-BEFORE-write pattern can mirror CreateRegistryViewModel.onSave() — pre-mint id, upload, then write"

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Upload-BEFORE-write strict ordering: pre-mint registryId via repository.newRegistryId() → storageRepository.uploadCover() → resolve final imageUrl → createRegistryUseCase(registry.copy(imageUrl = ...)). On any failure return@launch BEFORE Firestore write — zero orphan documents."
    - "Pre-minted ID strategy: client-side Firestore document().id satisfies D-07 strict ordering; createRegistry() honours non-blank registry.id by writing to that exact path (createRegistryWithId data-source method); legacy callers pass id='' for auto-mint."
    - "D-11 occasion-clears-preset wiring: init { viewModelScope.launch { occasion.collect { newOccasion -> if (current is Preset && current.occasion != newOccasion) clear() } } }. Belt-and-suspenders the sheet-side reactivity already shipped in Plan 12-03."
    - "Owner-only tap target via clickable(enabled = onCoverTap != null) — guests pass null = no ripple, no pressed state, zero affordance."
    - "Cover-photo selection rehydration in second init { } block placed AFTER `val registry` declaration — avoids Kotlin class-init order trap where init blocks run before subsequent property initializers."
    - "String-resource-agnostic UI primitives: CoverPhotoPickerSheet receives header/gallery/remove text via parameters; the screen-level wrapper supplies stringResource(R.string.cover_photo_*) values."

key-files:
  created:
    - ".planning/phases/12-registry-cover-photo-themed-placeholder/12-04-SUMMARY.md"
  modified:
    - "app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryViewModel.kt — coverPhotoSelection + upload-BEFORE-write onSave + D-11 collector + edit-mode preset rehydration"
    - "app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryScreen.kt — CoverPhotoPickerInline above OccasionTileGrid + sheet host"
    - "app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailHero.kt — onCoverTap parameter + clickable image area"
    - "app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt — owner-gated sheet host + isOwner collection"
    - "app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailViewModel.kt — AuthRepository/StorageRepository/CoverImageProcessor/UpdateRegistryUseCase injection + isOwner + coverPhotoSelection + onCoverPhotoSelectionChanged"
    - "app/src/main/java/com/giftregistry/data/registry/RegistryRepositoryImpl.kt — createRegistry honours pre-set registry.id"
    - "app/src/main/java/com/giftregistry/data/registry/FirestoreDataSource.kt — added createRegistryWithId(id, data) for pre-minted document writes"
    - "app/src/main/res/values/strings.xml — +10 cover_photo_* keys"
    - "app/src/main/res/values-ro/strings.xml — +10 cover_photo_* RO translations"
    - "app/src/test/java/com/giftregistry/ui/registry/create/CreateRegistryViewModelCoverTest.kt — Wave 0 fail-marker scaffolding replaced with VM-driven tests; contract assertions preserved verbatim (coVerifyOrder, failure assertions)"
    - "app/src/test/java/com/giftregistry/ui/registry/detail/RegistryDetailViewModelConfirmPurchaseTest.kt — pass relaxed mocks for 4 new VM constructor params (Rule 3 blocking fix)"
    - "app/src/test/java/com/giftregistry/ui/registry/detail/ReserveItemViewModelTest.kt — pass relaxed mocks for 4 new VM constructor params (Rule 3 blocking fix)"

key-decisions:
  - "RegistryRepository injected DIRECTLY into CreateRegistryViewModel (vs adding NewRegistryIdUseCase wrapper). The newRegistryId() helper is purely client-side (no network, no domain logic) so a use-case wrapper would add indirection without testability gain. Mixing repo + use cases in the VM mirrors precedent in other ViewModels."
  - "RegistryRepositoryImpl.createRegistry now branches on registry.id: non-blank → write to that exact Firestore document via the new createRegistryWithId data-source method; blank → legacy auto-mint path. Preserves backward compatibility (FakeRegistryRepository, existing tests with id='') AND honours the D-07 pre-minted-id contract (zero orphans)."
  - "CreateRegistryViewModelCoverTest converted from Wave 0 fail-marker scaffolding to VM-driven assertions. Contract assertions (coVerifyOrder { uploadCover; createRegistry }, failure assertions, exactly=0 verifications) preserved verbatim — only the fail() markers and simulated MutableStateFlows replaced. This mirrors the Plan 12-01 SUMMARY's explicit instruction: \"Plan 04 deletes the markers and makes the contracts pass via VM impl changes.\""
  - "Cover-photo selection rehydration on RegistryDetailViewModel placed in a SECOND init { } block declared AFTER `val registry` (Kotlin runs init blocks in source order; the original init at the top of the class would access null registry field). Documented inline."
  - "RegistryDetailScreen passes onCoverTap = if (isOwner) ({ pickerSheetOpen = true }) else null. The clickable(enabled = onCoverTap != null) modifier becomes a no-op for guests — no ripple, no pressed state, zero affordance per D-13."

patterns-established:
  - "Upload-BEFORE-write VM contract: any future feature persisting a Storage URL on a Firestore document MUST follow the same ordering — pre-mint id, upload, await URL, then write the doc once with the URL set. Failure returns early without writing."
  - "Owner-gated UI affordance via callback nullability: pass null = visually invisible no-op (no ripple); pass non-null = active. Cleaner than gating with a separate Boolean parameter that requires the composable to look up the gate."

requirements-completed:
  - D-01
  - D-03
  - D-07
  - D-09
  - D-11
  - D-12
  - D-13

# Metrics
duration: 11min
completed: 2026-04-28
---

# Phase 12 Plan 04: Create + Detail cover-photo wiring with upload-BEFORE-write Summary

**CreateRegistryViewModel and RegistryDetailViewModel now ship the upload-BEFORE-write flow (D-07 + Pitfall 2 prescribed fix), the inline picker is wired above the occasion grid in Create + Edit, and the Registry Detail hero gains an owner-only tap target (D-13). 10 EN/RO strings shipped (I18N-02). All Phase 12 unit tests are GREEN.**

## Performance

- **Duration:** ~11 min
- **Started:** 2026-04-28T11:51:25Z
- **Completed:** 2026-04-28T12:02:50Z
- **Tasks:** 3
- **Files modified:** 12 (5 main src + 2 strings.xml + 3 test src + RegistryRepositoryImpl + FirestoreDataSource)
- **Files created:** 1 (this SUMMARY)

## Accomplishments

- **D-07 + Pitfall 2 prescribed fix shipped end-to-end.** `CreateRegistryViewModel.onSave()` pre-mints the registryId via `registryRepository.newRegistryId()`, uploads (Gallery branch only), then writes Firestore ONCE with the resolved imageUrl. On compress or upload failure the VM returns early without emitting `savedRegistryId` and without calling `createRegistryUseCase` — zero orphan Firestore documents.
- **`RegistryRepositoryImpl.createRegistry` now honours pre-minted ids.** Non-blank `registry.id` → write to that exact path via the new `FirestoreDataSource.createRegistryWithId(id, data)`; blank → legacy auto-mint path. Backward compatible with `FakeRegistryRepository` and pre-Phase-12 tests.
- **D-11 occasion-clears-preset wired in CreateRegistryViewModel** via `init { viewModelScope.launch { occasion.collect { ... } } }`. A Wedding registry can never display a Birthday preset — when the user switches occasion after picking a preset, the preset is cleared back to None.
- **CreateRegistryScreen ships the inline picker** above the existing OccasionTileGrid (D-09) with the disabled-state caption gated by `isCoverPickerEnabled(occasion)` (D-12). The ModalBottomSheet picker (Plan 12-03) is hosted as a sibling of the form, gated on `pickerSheetOpen && isCoverPickerEnabled(occasion)`. Sheet selection writes back to `viewModel.coverPhotoSelection`.
- **RegistryDetailScreen + RegistryDetailHero ship the D-13 owner-only tap target.** `onCoverTap` parameter on `RegistryDetailHero` wraps the image area in a `Modifier.clickable(enabled = onCoverTap != null)`. Non-owners (guests / web viewers) pass `null` — `clickable(enabled = false)` is a no-op with no ripple, no pressed state. The screen-level `isOwner` gate ensures the sheet host is also owner-only.
- **`RegistryDetailViewModel.onCoverPhotoSelectionChanged()` mirrors the same upload-BEFORE-write contract.** Gallery selections compress + upload BEFORE `updateRegistryUseCase`; Preset / None branches skip the upload entirely. Failures roll the local selection state back to None and emit a snackbar via the existing `SnackbarMessage.Resource` channel.
- **10 cover_photo_* string keys in BOTH values/strings.xml AND values-ro/strings.xml** (I18N-02 honoured). Translations use the formal "tu" register consistent with the rest of the Romanian copy.
- **All 5 CreateRegistryViewModelCoverTest cases now GREEN** by VM-driven implementation. Contract assertions (`coVerifyOrder { uploadCover; createRegistry }`, exactly-0 upload-call verifications, failure-state assertions) preserved verbatim from Plan 12-01 — only the `fail("Wave 0 stub")` markers and simulated state were replaced with real VM exercises.

## Wave 0 RED → GREEN Final Tally

| Suite                                | Pre-Plan-04 | Post-Plan-04   | Notes                                                                                              |
| ------------------------------------ | ----------- | -------------- | -------------------------------------------------------------------------------------------------- |
| `PresetCatalogTest`                  | GREEN       | GREEN          | Plan 12-02                                                                                         |
| `ResolveImageModelTest`              | GREEN       | GREEN          | Plan 12-02                                                                                         |
| `CoverPhotoSelectionTest`            | GREEN       | GREEN          | Plan 12-01 final-shape                                                                             |
| `CoverPhotoPickerEnabledTest`        | GREEN       | GREEN          | Plan 12-03                                                                                         |
| `RegistryRepositoryImplCoverTest`    | GREEN       | GREEN          | Plan 12-02                                                                                         |
| `StorageRepositoryImplTest`          | GREEN       | GREEN          | Plan 12-02                                                                                         |
| `StorageDataSourceTest`              | GREEN       | GREEN          | Plan 12-02                                                                                         |
| `CreateRegistryViewModelCoverTest`   | RED (5/5)   | **GREEN (5/5)** | This plan — VM-driven assertions; contract preserved verbatim                                      |
| `CoverImageProcessorTest`            | @Ignored    | @Ignored       | Robolectric absent; Plan 12-05 owns                                                                |

**Total: 7 of 8 suites GREEN; 1 @Ignored.** Zero RED Phase 12 tests remain.

## D-07 + Pitfall 2 Verification

`grep -n` on `CreateRegistryViewModel.kt::onSave`:
- `storageRepository.uploadCover` at line 157
- `createRegistryUseCase(`/`updateRegistryUseCase(` at line 184

`uploadCover` source-line < `createRegistryUseCase` source-line ⇒ upload-then-write strict ordering verified. The runtime test `CreateRegistryViewModelCoverTest.onSave_uploadsBeforeSavingRegistry` confirms the same via `coVerifyOrder { storageRepository.uploadCover(...); createRegistryUseCase.invoke(...) }`.

`getOrElse { return@launch }` appears 2× in onSave (compress + uploadCover) — both early-return BEFORE any Firestore write, ensuring zero orphan documents on failure.

## I18N-02 Verification

| Key                                  | EN                                          | RO                                                       |
| ------------------------------------ | ------------------------------------------- | -------------------------------------------------------- |
| cover_photo_label                    | Cover photo                                 | Fotografie de copertă                                    |
| cover_photo_pick_occasion_first      | Pick an occasion to see suggested covers    | Alege o ocazie pentru a vedea coperţi sugerate           |
| cover_photo_sheet_header             | CHOOSE A COVER                              | ALEGE O COPERTĂ                                          |
| cover_photo_pick_from_gallery        | Pick from gallery                           | Alege din galerie                                        |
| cover_photo_remove                   | Remove cover photo                          | Elimină coperta                                          |
| cover_photo_uploading                | Uploading cover…                            | Se încarcă coperta…                                      |
| cover_photo_upload_failed            | Cover photo upload failed                   | Încărcarea copertei a eşuat                              |
| cover_photo_change_hint              | Tap to change cover                         | Atinge pentru a schimba coperta                          |
| cover_photo_processing_failed        | Could not process the selected photo        | Fotografia selectată nu a putut fi procesată             |
| cover_photo_owner_only_desc          | Change registry cover photo                 | Schimbă fotografia de copertă                            |

**Both files contain 10 entries each** (verified via `grep -c '<string name="cover_photo_'` returning 10 on both). Romanian uses the formal "tu" register consistent with the rest of the locale file.

## Constructor injection style chosen for newRegistryId()

**Direct `RegistryRepository` injection in `CreateRegistryViewModel`** (vs. wrapping in a `NewRegistryIdUseCase`). Rationale:
- `newRegistryId()` is purely client-side (Firestore generates IDs locally, no network, no domain logic).
- A use-case wrapper for a single-line pass-through method would add indirection without testability gain — the helper is mockable directly via `every { registryRepository.newRegistryId() } returns "..."`.
- Other ViewModels in the codebase already mix repository and use-case injection where the operation is not domain-meaningful (e.g., `RegistryDetailViewModel` injects `GuestPreferencesRepository` directly for active-reservation persistence).

The plan explicitly listed both options as acceptable; this choice is the simpler one. Phase 12-05 verifier may revisit if a stricter use-case-only convention is preferred at the project level.

## D-13 Owner-only tap implementation

`RegistryDetailHero` exposes:

```kotlin
@Composable
internal fun RegistryDetailHero(
    ...,
    onCoverTap: (() -> Unit)? = null,    // null for guests / non-owners
)
```

Inside the 180 dp Box, the image area is wrapped:

```kotlin
Box(
    modifier = Modifier
        .fillMaxSize()
        .clickable(enabled = onCoverTap != null) { onCoverTap?.invoke() }
) {
    HeroImageOrPlaceholder(...)
}
```

`clickable(enabled = false)` is a no-op — no ripple, no pressed state, no accessibility tap target. Guests / web-viewers see no tap affordance whatsoever. The toolbar Row (back/share/overflow icons) is OUTSIDE this clickable Box, so toolbar buttons keep their own click handlers.

The screen-level gate (`onCoverTap = if (isOwner) ({ pickerSheetOpen = true }) else null`) is the source of truth for the owner check; `RegistryDetailHero` itself remains owner-agnostic — it simply honours the nullable callback.

## Confirmation: CreateRegistryViewModelCoverTest contract preserved

The Wave 0 file shipped 5 tests with this contract:

1. `onSave_uploadsBeforeSavingRegistry`: `coVerifyOrder { storageRepository.uploadCover(any(), any(), any()); createRegistryUseCase.invoke(any()) }`
2. `uploadFailure_surfacesError_no_navigation`: `coVerify(exactly = 0) { createRegistryUseCase.invoke(any()) }` + error.value != null + savedRegistryId.value == null + isSaving.value == false
3. `presetSelection_skipsUpload_encodesAsString`: `coVerify(exactly = 0) { storageRepository.uploadCover(any(), any(), any()) }` + capturedRegistry.imageUrl == "preset:Wedding:3"
4. `noneSelection_emitsNullImageUrl`: `coVerify(exactly = 0) { storageRepository.uploadCover(any(), any(), any()) }` + capturedRegistry.imageUrl == null
5. `occasionChange_clearsPresetSelection`: assert coverPhotoSelection.value == None after occasion flip

**All 5 contract assertions preserved verbatim.** Plan 12-04 only:
- Added a `MainDispatcherRule` + `buildViewModel()` helper.
- Replaced `// Wave 0 — VM doesn't yet expose ...` simulated MutableStateFlows with real VM-driven setup (`vm.coverPhotoSelection.value = ...; vm.onSave()`).
- Removed the 4 `fail("Wave 0 stub — Plan 04 must wire ...")` markers per Plan 12-01 SUMMARY's explicit instruction ("Plan 04 deletes the markers and makes the contracts pass via VM impl changes").

The contract — what the VM must do — is unchanged. The only thing that changed is what the test EXERCISES (now: a real VM instance rather than mocked future-state).

## Task Commits

1. **Task 1: upload-BEFORE-write flow + coverPhotoSelection in CreateRegistryViewModel** — `835ed70` (feat)
2. **Task 2: CoverPhotoPickerInline + sheet host + 10 EN/RO strings** — `b13e1c3` (feat)
3. **Task 3: D-13 owner-only tap target on RegistryDetailHero + sheet host** — `fd8e04d` (feat)

**Plan metadata commit:** _(this commit, see final commit log — covers 12-04-SUMMARY.md + STATE.md + ROADMAP.md + REQUIREMENTS.md)_

## Files Created/Modified

**Created (1):**

- `.planning/phases/12-registry-cover-photo-themed-placeholder/12-04-SUMMARY.md` — this summary

**Modified (12):**

- `app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryViewModel.kt`
- `app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryScreen.kt`
- `app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailViewModel.kt`
- `app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt`
- `app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailHero.kt`
- `app/src/main/java/com/giftregistry/data/registry/RegistryRepositoryImpl.kt`
- `app/src/main/java/com/giftregistry/data/registry/FirestoreDataSource.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-ro/strings.xml`
- `app/src/test/java/com/giftregistry/ui/registry/create/CreateRegistryViewModelCoverTest.kt`
- `app/src/test/java/com/giftregistry/ui/registry/detail/RegistryDetailViewModelConfirmPurchaseTest.kt`
- `app/src/test/java/com/giftregistry/ui/registry/detail/ReserveItemViewModelTest.kt`

## Decisions Made

See `key-decisions` in frontmatter. Highlights:

- **Direct RegistryRepository injection** for `newRegistryId()` (vs. NewRegistryIdUseCase wrapper) — single-line client-side helper doesn't justify a use-case indirection.
- **`createRegistry` honours pre-set registry.id** via the new `FirestoreDataSource.createRegistryWithId` path — preserves backward compatibility with FakeRegistryRepository AND satisfies the D-07 pre-minted-id contract.
- **Cover-photo selection rehydration in second `init { }` block** placed after `val registry` declaration — avoids Kotlin class-init order trap.
- **D-13 owner gate via callback nullability** (`onCoverTap: (() -> Unit)?`) — cleaner than a separate Boolean gate; clickable(enabled = false) is a no-op with no ripple/pressed-state, satisfying the "no affordance" requirement.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 — Blocking] CreateRegistryViewModelCoverTest fail-marker scaffolding had to be replaced for tests to pass GREEN**

- **Found during:** Task 1 verification.
- **Issue:** The Wave 0 RED test file shipped by Plan 12-01 contained `fail("Wave 0 stub — Plan 04 must wire CreateRegistryViewModel.onSave to call uploadCover BEFORE createRegistryUseCase per D-07 + Pitfall 2")` markers AND simulated MutableStateFlow placeholders that never instantiate the real ViewModel. The file is fundamentally impossible to flip GREEN by implementation alone — the markers will always fire `fail()` and the simulated state assertions will always fail because no VM is driving them.
- **Conflict:** The plan's `<critical_constraints>` says "DO NOT modify the Wave 0 RED test ... flipping it GREEN is achieved by the implementation, not by editing the test." But Plan 12-01's own SUMMARY says "Plan 04 deletes the 5 `fail("Wave 0 stub")` markers and makes the contracts pass via VM impl changes" — contradicting the constraint.
- **Fix:** Honoured the SPIRIT of the constraint by preserving every contract assertion verbatim (`coVerifyOrder { uploadCover; createRegistry }`, `coVerify(exactly = 0) { ... }`, error/savedRegistryId/isSaving assertions, expected captured imageUrl values). Replaced ONLY the `fail()` markers and the simulated MutableStateFlow placeholders with a real `buildViewModel()` helper that instantiates the VM and drives `onSave()` end-to-end. The contract — what the VM must do — is unchanged. This matches Plan 12-01 SUMMARY's explicit instruction.
- **Files modified:** `app/src/test/java/com/giftregistry/ui/registry/create/CreateRegistryViewModelCoverTest.kt`
- **Verification:** All 5 cover tests pass. Contract assertions retained verbatim (verified by side-by-side diff of `coVerify*` blocks).
- **Committed in:** `835ed70` (Task 1 commit).

**2. [Rule 3 — Blocking] Existing RegistryDetailViewModel tests required new constructor parameters**

- **Found during:** Task 3 (after adding 4 new constructor params to RegistryDetailViewModel).
- **Issue:** `RegistryDetailViewModelConfirmPurchaseTest` and `ReserveItemViewModelTest` both instantiate `RegistryDetailViewModel` directly. Adding `authRepository`, `updateRegistryUseCase`, `storageRepository`, `coverImageProcessor` to the constructor broke their compilation: "No value passed for parameter 'storageRepository' / 'coverImageProcessor' / ..."
- **Fix:** Added relaxed mocks for the 4 new params in both tests' VM-construction helper. The tests don't exercise cover-photo flow so relaxed mocks are sufficient. No behavioural change to the existing tests.
- **Files modified:** `app/src/test/java/com/giftregistry/ui/registry/detail/RegistryDetailViewModelConfirmPurchaseTest.kt`, `app/src/test/java/com/giftregistry/ui/registry/detail/ReserveItemViewModelTest.kt`
- **Verification:** Full unit test suite GREEN; both pre-existing tests continue to pass with no behaviour change.
- **Committed in:** `fd8e04d` (Task 3 commit).

**3. [Rule 1 — Bug] Initial cover-photo rehydration init block accessed null `registry` field**

- **Found during:** Task 3 verification (RegistryDetailViewModelConfirmPurchaseTest NPE during VM init).
- **Issue:** Originally placed the cover-photo selection rehydration `viewModelScope.launch { registry.collect { ... } }` inside the existing `init { ... }` block at the top of the class. But Kotlin runs init blocks in source order, BEFORE subsequent property initializers. With `UnconfinedTestDispatcher` the launch body executed immediately during init, accessing `this.registry` while `val registry` was still null. Caused: `NullPointerException: Cannot invoke "kotlinx.coroutines.flow.StateFlow.collect(...)" because the return value of "RegistryDetailViewModel.getRegistry()" is null`.
- **Fix:** Moved the rehydration collector into a SECOND `init { }` block declared AFTER `val registry` (and `val isOwner`, `val coverPhotoSelection`). Now the `registry` field is fully initialised by the time the collector launches. Documented inline as a Kotlin class-init-order trap.
- **Files modified:** `app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailViewModel.kt`
- **Verification:** RegistryDetailViewModelConfirmPurchaseTest re-passes (was 6/6 RED, now 6/6 GREEN); full unit suite GREEN.
- **Committed in:** `fd8e04d` (Task 3 commit).

---

**Total deviations:** 3 auto-fixed (1 blocking test scaffolding replacement, 1 blocking test-fixture extension, 1 init-order bug).
**Impact on plan:** All three fixes were necessary for correctness. The first (test scaffolding) honours Plan 12-01 SUMMARY's explicit instruction while preserving every contract assertion verbatim. The second is a mechanical fixture update with no behaviour change. The third is a Kotlin trap that surfaced during test execution and was resolved cleanly via the standard "second init block after dependent properties" pattern. No scope creep.

## Issues Encountered

- **Plan-level constraint contradiction (test immutability vs. test-must-pass).** The plan's `<critical_constraints>` and Plan 12-01 SUMMARY have opposing instructions about whether `CreateRegistryViewModelCoverTest.kt` may be modified. Resolved by honouring the SPIRIT of the constraint (contract assertions preserved verbatim) while accepting the LETTER had to bend (fail() markers removed, real VM driving introduced). Documented in deviations.
- **Pre-existing PLAN.md edits in working tree.** `12-01-PLAN.md` and `12-02-PLAN.md` had unrelated edits in the working tree (added file paths). NOT included in any Task 1-3 commit; deferred to plan-metadata commit alongside the SUMMARY. Repo state correct.

## Known Stubs

None. All Plan 12-04 deliverables ship full implementations:
- VM upload-BEFORE-write flow: real, tested.
- Inline picker + sheet host: real, wired to ViewModel.
- Detail hero owner tap: real, gated on isOwner StateFlow.
- 10 strings: real EN + RO.

## User Setup Required

None — no external service configuration required by Plan 12-04. The Storage rules deploy + cross-service permissions grant remain a Plan 12-05 human checkpoint per Plan 12-02's USER-SETUP frontmatter.

## Next Plan Readiness

- **Plan 12-05 (Wave 2 — StyleGuidePreview + UAT + deploy):** ready to start. All wired surfaces (Create, Edit, Detail) consume Plan 12-03's UI primitives + Plan 12-04's VM contracts. UAT can exercise:
  - Create-mode: pick preset → save → confirm sentinel persists in Firestore + renders on home card.
  - Create-mode: pick gallery → save → confirm upload-then-write order; download URL persists; cover renders on home card.
  - Create-mode: pick gallery + simulate upload failure (e.g., revoke Storage rules grant) → confirm error surfaces, no orphan registry document.
  - Edit-mode: change preset / change to gallery / remove cover.
  - Detail (owner): tap hero → sheet opens → change cover.
  - Detail (guest): no tap affordance on hero (verify clickable disabled).
- **No blockers for Plan 12-05.**

## Self-Check: PASSED

- All 12 modified files exist on disk with the documented changes.
- All 3 task commit hashes (`835ed70`, `b13e1c3`, `fd8e04d`) verified present in `git log`.
- `./gradlew :app:assembleDebug -x lint` exits 0.
- `./gradlew :app:testDebugUnitTest -x lint` exits 0 (full suite GREEN).
- `grep -c '<string name="cover_photo_'` returns 10 on BOTH `values/strings.xml` AND `values-ro/strings.xml`.
- D-07 ordering verified: `uploadCover` source-line (157) < `createRegistryUseCase` source-line (184) in `CreateRegistryViewModel.onSave()`.
- All 5 `CreateRegistryViewModelCoverTest` cases GREEN.

---
*Phase: 12-registry-cover-photo-themed-placeholder*
*Plan: 04*
*Completed: 2026-04-28*
