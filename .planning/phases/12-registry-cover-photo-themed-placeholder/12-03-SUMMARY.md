---
phase: 12-registry-cover-photo-themed-placeholder
plan: 03
subsystem: ui
tags: [compose, material3, modal-bottom-sheet, photo-picker, coil3, placeholder, cover-photo, kotlin]

# Dependency graph
requires:
  - phase: 12-01 (this phase)
    provides: "CoverPhotoPickerEnabled.kt + ResolveImageModel.kt + PresetCatalog.kt + CoverPhotoSelection.kt stubs replaced with real bodies"
  - phase: 12-02 (this phase, parallel)
    provides: "PresetCatalog.presetsFor / resolve real bodies + 36 drawable assets — consumed by CoverPhotoPickerSheet at runtime"
  - phase: 11-registry-detail-create-add-item-redesign
    provides: "OccasionCatalog.glyphFor + RegistryDetailHero placeholder pattern (180dp accentSoft→accent + 40sp glyph) — extracted to HeroImageOrPlaceholder"
  - phase: 10-onboarding-home-redesign
    provides: "RegistryCardPrimary + RegistryCardSecondary + Registry.imageUrl: String? — refactored to consume HeroImageOrPlaceholder"
  - phase: 09-shared-chrome-status-ui
    provides: "GiftMaisonTheme tokens (paper, paperDeep, ink, inkSoft, inkFaint, accent, accentSoft, line, radius14, pill, gap8/16/20, monoCaps, bodyM, bodyMEmphasis, displayXL) — call-site palette"
provides:
  - "HeroImageOrPlaceholder shared composable — single source of gradient+glyph placeholder used by RegistryDetailHero, RegistryCardPrimary, RegistryCardSecondary"
  - "isCoverPickerEnabled real predicate (`!occasion.isNullOrBlank()`) — flips D-12 RED tests GREEN"
  - "CoverPhotoPickerInline composable — 16:9 inline preview with three states (disabled / placeholder / selected); string-resource-agnostic"
  - "CoverPhotoPickerSheet composable — Material3 ModalBottomSheet with 3x2 LazyVerticalGrid (userScrollEnabled = false), full-width gallery pill, conditional remove button"
  - "PresetThumbnail internal composable — 16:9 selectable tile with accent vs line border on selected state"
  - "ColorFilter parameter on HeroImageOrPlaceholder — applied ONLY to AsyncImage branch (RegistryCardPrimary 70% brightness preserved); placeholder gradient unaffected"
  - "Visible bug fix (D-15): RegistryCardPrimary + RegistryCardSecondary now render gradient+glyph for `imageUrl == null` instead of empty box"
  - "Pitfall 6 guard preserved: 3-stop dark overlay in RegistryDetailHero stays gated on `imageUrl != null` after the HeroImageOrPlaceholder swap"
affects:
  - "12-04 (Plan 04): wires CoverPhotoPickerInline above OccasionTileGrid in CreateRegistryScreen, hosts CoverPhotoPickerSheet in CreateRegistryScreen state, supplies the 3 string-resource-keyed parameters (cover_photo_pick_occasion_first, cover_photo_pick_from_gallery, cover_photo_remove)"
  - "12-05 (Plan 05): adds StyleGuidePreview entries for the new composables; on-device UAT validates 40sp hero / 32sp card pixel contracts"

# Tech tracking
tech-stack:
  added:
    - "androidx.activity.compose rememberLauncherForActivityResult (already on classpath; first use for PickVisualMedia in this codebase)"
    - "androidx.compose.material3.ModalBottomSheet + rememberModalBottomSheetState (skipPartiallyExpanded = true)"
    - "androidx.compose.foundation.lazy.grid.LazyVerticalGrid + GridCells.Fixed(3) + itemsIndexed (first use in this codebase)"
  patterns:
    - "Shared image surface with optional ColorFilter — gradient placeholder branch intentionally bypasses the filter so themed gradients pop on dark surfaces"
    - "ModalBottomSheet dismiss pattern: `scope.launch { sheetState.hide() }.invokeOnCompletion { if (!sheetState.isVisible) onDismiss() }` (RESEARCH.md Pattern 7)"
    - "String-resource-agnostic UI primitives — caller (Plan 04) passes stringResource(...) values via parameters; primitives stay reusable in @Preview without R-class binding"
    - "Pitfall 7 guard: `remember(imageUrl) { resolveImageModel(imageUrl) }` ensures Coil tears down + recomposes on null↔non-null transitions; structural if/else (separate composable subtrees) instead of painterResource-style ternary"
    - "Pitfall 6 guard preserved: dark overlay (3-stop ink44 / transparent / inkAA gradient) lives at the call site of HeroImageOrPlaceholder, NOT inside the shared composable — only paints when imageUrl != null"

key-files:
  created:
    - "app/src/main/java/com/giftregistry/ui/registry/cover/HeroImageOrPlaceholder.kt — shared composable consumed by 3 call sites"
    - "app/src/main/java/com/giftregistry/ui/registry/cover/CoverPhotoPickerInline.kt — 16:9 inline preview block (D-09 + D-12)"
    - "app/src/main/java/com/giftregistry/ui/registry/cover/CoverPhotoPickerSheet.kt — ModalBottomSheet with 3x2 grid + gallery pill + conditional remove (D-10)"
    - "app/src/main/java/com/giftregistry/ui/registry/cover/PresetThumbnail.kt — internal 16:9 selectable tile"
  modified:
    - "app/src/main/java/com/giftregistry/ui/registry/cover/CoverPhotoPickerEnabled.kt — stub body replaced with `!occasion.isNullOrBlank()` (D-12)"
    - "app/src/main/java/com/giftregistry/ui/registry/list/RegistryCard.kt — bare AsyncImage replaced with HeroImageOrPlaceholder in both Primary and Secondary variants; colorFilter passed only to Primary (D-15)"
    - "app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailHero.kt — inline placeholder block replaced with HeroImageOrPlaceholder(glyphSize=40.sp); 3-stop dark overlay kept at call site, gated on imageUrl != null (Pitfall 6)"

key-decisions:
  - "HeroImageOrPlaceholder accepts optional ColorFilter parameter — applied ONLY to AsyncImage branch so the gradient placeholder renders at full brightness on the dark-ink primary card"
  - "CoverPhotoPickerInline + CoverPhotoPickerSheet are string-resource-agnostic — caller passes stringResource(...) values via parameters, keeping the primitives reusable for future @Preview surfaces"
  - "LazyVerticalGrid given a fixed 220.dp height + userScrollEnabled = false — 6 items in 2 rows of 16:9 tiles fit within the sheet width without overflow; locked-grid is correct for D-10 (no scroll desired)"
  - "Sheet preset selection wraps `scope.launch { sheetState.hide() }.invokeOnCompletion { if (!sheetState.isVisible) onDismiss() }` — closes sheet smoothly after each selection (preset / gallery / remove) without losing the dismiss animation"
  - "Sheet is reactive to occasion via `remember(occasion) { PresetCatalog.presetsFor(occasion) }` — D-11 belt-and-suspenders even though Plan 04's ViewModel will already reset selection to None on occasion change"
  - "Disabled-state caption sits over the gradient placeholder via a translucent ink scrim (ink @ 0.25 alpha) — keeps the gradient visible underneath while making the caption legible"

patterns-established:
  - "Shared image-or-placeholder composable with optional ColorFilter for selective image-branch processing — pattern reusable for any future image surfaces that need brightness/saturation tuning while preserving themed fallbacks"
  - "ModalBottomSheet sheet-hide-then-callback dismissal pattern — works around the gotcha where calling onDismissRequest directly skips the slide-out animation"
  - "String-resource-agnostic UI primitive — accept user-facing strings as parameters so the composable compiles without R-class context (key for testing + previews)"

requirements-completed:
  - D-09
  - D-10
  - D-11
  - D-12
  - D-14
  - D-15
  - D-16

# Metrics
duration: 10min
completed: 2026-04-28
---

# Phase 12 Plan 03: Cover-Photo Picker UI Primitives + HeroImageOrPlaceholder Refactor Summary

**Shared HeroImageOrPlaceholder + CoverPhotoPickerInline + CoverPhotoPickerSheet + PresetThumbnail composables shipped; both registry card variants now render the themed gradient+glyph placeholder when imageUrl == null (visible bug fixed); Phase 11 hero pixel contract preserved.**

## Performance

- **Duration:** 10 min
- **Started:** 2026-04-28T11:29:55Z
- **Completed:** 2026-04-28T11:40:10Z
- **Tasks:** 3
- **Files created:** 4 (HeroImageOrPlaceholder, CoverPhotoPickerInline, CoverPhotoPickerSheet, PresetThumbnail)
- **Files modified:** 3 (CoverPhotoPickerEnabled, RegistryCard, RegistryDetailHero)

## Accomplishments

- **HeroImageOrPlaceholder shared composable** is the single source of truth for the `accentSoft → accent` gradient + Instrument-Serif italic occasion glyph fallback (D-16). Three call sites now consume it: `RegistryDetailHero`, `RegistryCardPrimary`, `RegistryCardSecondary`.
- **Visible bug fixed (D-15):** Both `RegistryCardPrimary` and `RegistryCardSecondary` now render the themed placeholder when `Registry.imageUrl == null`. Previously bare `AsyncImage(model = null, ...)` calls left an empty box — the user-reported issue from the Home screen screenshot.
- **Phase 11 hero pixel contract preserved:** `RegistryDetailHero` passes `glyphSize = 40.sp` (cards use 32.sp). The 3-stop dark overlay (ink @ 0x44 top → transparent at 40% → ink @ 0xAA bottom) stays at the hero call site, gated on `registry?.imageUrl != null` so it only paints over real images (Pitfall 6 honored).
- **`isCoverPickerEnabled` real predicate** ships: `!occasion.isNullOrBlank()`. Wave 0 RED tests in `CoverPhotoPickerEnabledTest` (3 false-when-blank/null/whitespace cases) flip GREEN. The valid-occasion case stays GREEN.
- **CoverPhotoPickerInline** ships with all three D-09/D-12 states: disabled (no occasion → translucent ink scrim + caller-provided caption), enabled+None (gradient placeholder + tap opens sheet), enabled+Preset/Gallery (selection rendered via HeroImageOrPlaceholder + tap re-opens sheet).
- **CoverPhotoPickerSheet** ships per RESEARCH.md Pattern 7: `ModalBottomSheet(skipPartiallyExpanded = true)`, `LazyVerticalGrid(GridCells.Fixed(3), userScrollEnabled = false)` with 6 PresetThumbnails, full-width "Pick from gallery" pill (`PickVisualMedia.ImageOnly`), conditional "Remove cover photo" `TextButton` (only when `currentSelection !is None`).
- **PresetThumbnail** ships as an internal 16:9 selectable tile with 14 dp radius (D-04 token), 1.5 dp accent border on selected / 1 dp line border otherwise — matching the focused-input visual language from Phase 10/11.
- **String-resource-agnostic primitives:** Plan 04 will wire `R.string.cover_photo_pick_occasion_first`, `R.string.cover_photo_pick_from_gallery`, `R.string.cover_photo_remove` via parameters — sheet and inline composables compile and preview without R-class binding.

## HeroImageOrPlaceholder API Surface

```kotlin
@Composable
fun HeroImageOrPlaceholder(
    imageUrl: String?,
    occasion: String?,
    glyphSize: TextUnit = 32.sp,                // 40.sp at hero, 32.sp at cards (D-14)
    colorFilter: ColorFilter? = null,           // applied ONLY to AsyncImage branch
    modifier: Modifier = Modifier,
)
```

**colorFilter rationale:** `RegistryCardPrimary` darkens its real-image branch to 70% brightness via `ColorFilter.colorMatrix(ColorMatrix().apply { setToScale(0.7f, 0.7f, 0.7f, 1f) })`. The colorFilter is intentionally NOT applied to the gradient placeholder branch — the dark-ink card is already dark; the accent gradient must render at full brightness so it pops against ink. RegistryCardSecondary and RegistryDetailHero pass `null` (default) so they get full-brightness images.

## RegistryCard Refactor Diff Summary

| Call site | Before (bare) | After (shared) |
|-----------|---------------|----------------|
| `RegistryCardPrimary` (lines 60-68) | `AsyncImage(model = registry.imageUrl, contentScale = Crop, colorFilter = ColorFilter.colorMatrix(...))` — null URL renders empty box | `HeroImageOrPlaceholder(imageUrl = registry.imageUrl, occasion = registry.occasion, glyphSize = 32.sp, colorFilter = ColorFilter.colorMatrix(...))` — null URL renders gradient + glyph at full brightness |
| `RegistryCardSecondary` (lines 126-132) | `AsyncImage(model = registry.imageUrl, contentScale = Crop)` — null URL renders empty box | `HeroImageOrPlaceholder(imageUrl = registry.imageUrl, occasion = registry.occasion, glyphSize = 32.sp)` — null URL renders gradient + glyph |
| `RegistryDetailHero` (lines 84-129) | `if (imageUrl != null) AsyncImage(...) + 3-stop overlay; else Box(gradient) + Text(glyph)` — inline duplication of the gradient pattern | `HeroImageOrPlaceholder(glyphSize = 40.sp)` + `if (registry?.imageUrl != null) Box(3-stop overlay)` — placeholder pattern moved to shared composable; dark overlay stays at call site |

`grep -c 'AsyncImage' RegistryCard.kt` returns 0 (no bare AsyncImage calls remaining).

## Pitfall 6 Verification

The 3-stop dark overlay in `RegistryDetailHero.kt` is wrapped in:

```kotlin
// 3-stop dark overlay (ink@0x44 top, transparent 40%, ink@0xAA bottom) — ONLY when imageUrl != null (Pitfall 6 guard)
if (registry?.imageUrl != null) {
    val inkTop = Color(0xFF2A2420).copy(alpha = 0.27f)
    val inkBottom = Color(0xFF2A2420).copy(alpha = 0.67f)
    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(...)))
}
```

`grep -B1 -A4 'inkTop\|inkBottom\|Color(0xFF2A2420)' RegistryDetailHero.kt | grep -c 'imageUrl != null'` returns 1 — overlay block is enclosed by the imageUrl-non-null gate. The shared `HeroImageOrPlaceholder` composable does NOT contain any dark overlay logic — it only renders image XOR gradient+glyph.

## Phase 11 Pixel Contract Regression Check

`grep -c 'glyphSize = 40.sp' RegistryDetailHero.kt` returns 2 (one in KDoc explanation, one in the `HeroImageOrPlaceholder` call site). The hero glyph still renders at 40.sp — Phase 11 contract preserved. Cards use `glyphSize = 32.sp` (D-14 — 2 occurrences in RegistryCard.kt for Primary + Secondary).

## Task Commits

1. **Task 1: HeroImageOrPlaceholder + isCoverPickerEnabled GREEN** — `801150b` (feat)
2. **Task 2: RegistryCard + RegistryDetailHero consume HeroImageOrPlaceholder** — `d230a81` (refactor)
3. **Task 3: CoverPhotoPickerInline + Sheet + PresetThumbnail** — `51ec8ad` (feat)

**Plan metadata commit:** _(this commit, see final commit log)_

## Decision-ID Coverage

| Decision | Surface | This Plan's Contribution |
|----------|---------|--------------------------|
| D-09 | Inline 16:9 preview at top of CreateRegistryScreen | `CoverPhotoPickerInline.kt` shipped (Plan 04 wires in form) |
| D-10 | ModalBottomSheet 3x2 grid + gallery pill + remove | `CoverPhotoPickerSheet.kt` + `PresetThumbnail.kt` shipped |
| D-11 | Sheet content reactive to occasion | `remember(occasion) { PresetCatalog.presetsFor(occasion) }` in sheet |
| D-12 | Order gate (occasion required) | `isCoverPickerEnabled` real predicate + caption layered over disabled inline preview |
| D-14 | Themed placeholder (40 sp hero, 32 sp cards) | HeroImageOrPlaceholder accepts `glyphSize: TextUnit = 32.sp`; hero passes 40.sp |
| D-15 | Both card variants render placeholder when imageUrl null | RegistryCardPrimary + RegistryCardSecondary refactored |
| D-16 | Shared HeroImageOrPlaceholder | Single source of truth; 3 call sites |

## Files Created/Modified

**Created (4):**

- `app/src/main/java/com/giftregistry/ui/registry/cover/HeroImageOrPlaceholder.kt` — Shared image-or-placeholder composable
- `app/src/main/java/com/giftregistry/ui/registry/cover/CoverPhotoPickerInline.kt` — 16:9 inline preview block
- `app/src/main/java/com/giftregistry/ui/registry/cover/CoverPhotoPickerSheet.kt` — ModalBottomSheet picker
- `app/src/main/java/com/giftregistry/ui/registry/cover/PresetThumbnail.kt` — Selectable preset tile

**Modified (3):**

- `app/src/main/java/com/giftregistry/ui/registry/cover/CoverPhotoPickerEnabled.kt` — Stub body replaced with `!occasion.isNullOrBlank()`
- `app/src/main/java/com/giftregistry/ui/registry/list/RegistryCard.kt` — Both card variants now consume HeroImageOrPlaceholder
- `app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailHero.kt` — Hero refactored; 3-stop dark overlay preserved as conditional sibling

## Decisions Made

See `key-decisions` in frontmatter. Highlights:

- ColorFilter parameter on HeroImageOrPlaceholder applies ONLY to AsyncImage branch — keeps gradient placeholder at full brightness on dark-ink card (regression guard for Phase 10 dark-card pixel contract).
- Sheet height fixed at 220.dp to fit 6 tiles in 2 rows without scroll (D-10 explicit no-scroll requirement).
- String-resource-agnostic primitives — caller passes `stringResource(...)` values; primitives stay reusable in @Preview surfaces.

## Deviations from Plan

None — plan executed exactly as written. The plan's Task 1 specified adding `colorFilter` parameter "in this task" (Task 2 wording), but I added it during Task 1 since it was part of the API surface anyway and avoided a churn round-trip. This is consistent with the plan's intent (the colorFilter is referenced in Task 2 acceptance criteria as already present on HeroImageOrPlaceholder).

## Issues Encountered

- **Stale Hilt-generated factory after parallel Plan 12-02 commit** — Plan 12-02 added a `FirebaseFirestore` constructor parameter to `RegistryRepositoryImpl` between my reads. The KSP-generated `RegistryRepositoryImpl_Factory.java` had two-arg constructor cached; clearing `app/build/generated/ksp` and rebuilding regenerated it correctly. No code change needed — purely a parallel-execution build artifact issue. Subsequent builds passed cleanly.
- **Test-suite RED count tracking parallel progress:** When the plan started, 8 RED tests were failing (3 StorageRepositoryImplTest + 5 CreateRegistryViewModelCoverTest). After Plan 12-02 committed `2ac5910` + `dd66640`, the StorageRepositoryImplTest tests flipped GREEN; only 5 CreateRegistryViewModelCoverTest tests remain RED — those are explicitly Plan 04's territory. No regression introduced by Plan 12-03.

## User Setup Required

None — no external service configuration required. The new composables are Plan 04's job to wire into screens; Plan 04 will surface a `string` resource update if any new strings need user-translation (Romanian).

## Outstanding Wave 0 RED Suites Awaiting Plan 04

`CreateRegistryViewModelCoverTest` (5 tests):

1. `onSave_uploadsBeforeSavingRegistry` — D-07 + Pitfall 2 (`coVerifyOrder { uploadCover; createRegistry }`)
2. `uploadFailure_surfacesError_no_navigation` — D-07 (failure surfaces in `error: StateFlow<String?>`)
3. `presetSelection_skipsUpload_encodesAsString` — D-02 / D-05 (preset path skips StorageRepository.uploadCover)
4. `noneSelection_emitsNullImageUrl` — D-14 (None → imageUrl null)
5. `occasionChange_clearsPresetSelection` — D-11 (occasion change resets selection to None)

These will flip GREEN when Plan 04 ships the `CreateRegistryViewModel.onSave()` changes (upload-before-save ordering, error propagation, occasion-change collector).

## Next Phase Readiness

- **Plan 04 can now wire the picker UI.** All 5 picker-related composables are ready to drop into `CreateRegistryScreen` and `RegistryDetailScreen`. Plan 04 is responsible for:
  - Adding `coverPhotoSelection: MutableStateFlow<CoverPhotoSelection>` to `CreateRegistryViewModel`
  - Adding `init { occasion.collect { ... } }` to clear selection on occasion change (D-11)
  - Wiring `CoverPhotoPickerInline` above `OccasionTileGrid` in `CreateRegistryScreen` and hosting `CoverPhotoPickerSheet` state
  - Adding R.string.cover_photo_pick_occasion_first / cover_photo_pick_from_gallery / cover_photo_remove (and Romanian translations) to `strings.xml` + `values-ro/strings.xml`
  - Implementing the `viewModel.onSave()` upload-before-save ordering (D-07 + Pitfall 2)
  - Owner-only tap target on `RegistryDetailScreen.RegistryDetailHero` (D-13)
- **Plan 05 can begin StyleGuidePreview entries.** `HeroImageOrPlaceholder`, `CoverPhotoPickerInline`, `CoverPhotoPickerSheet`, `PresetThumbnail`, `RegistryCardPrimaryWithPlaceholder`, `RegistryCardSecondaryWithPlaceholder` are all callable from `@Preview` surfaces (Plan 04 supplies real strings, but @Preview can pass hard-coded English).
- **No blockers for Plan 04 / 05.** All composables compile cleanly; `:app:assembleDebug -x lint` exits 0.

## Self-Check: PASSED

All 4 created files exist:
- `app/src/main/java/com/giftregistry/ui/registry/cover/HeroImageOrPlaceholder.kt` — FOUND
- `app/src/main/java/com/giftregistry/ui/registry/cover/CoverPhotoPickerInline.kt` — FOUND
- `app/src/main/java/com/giftregistry/ui/registry/cover/CoverPhotoPickerSheet.kt` — FOUND
- `app/src/main/java/com/giftregistry/ui/registry/cover/PresetThumbnail.kt` — FOUND

All 3 task commit hashes verified in `git log`:
- `801150b` — FOUND
- `d230a81` — FOUND
- `51ec8ad` — FOUND

`./gradlew :app:assembleDebug -x lint` exits 0.
`./gradlew :app:testDebugUnitTest --tests 'com.giftregistry.ui.registry.cover.CoverPhotoPickerEnabledTest'` reports 4/4 GREEN.
Only Phase 12 Plan-04-territory RED tests remain failing (5 in CreateRegistryViewModelCoverTest); 0 regressions in pre-Phase-12 tests.

---
*Phase: 12-registry-cover-photo-themed-placeholder*
*Plan: 03*
*Completed: 2026-04-28*
