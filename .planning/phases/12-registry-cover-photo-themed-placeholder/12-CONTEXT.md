# Phase 12: Registry Cover Photo & Themed Placeholder - Context

**Gathered:** 2026-04-27
**Status:** Ready for planning

<domain>
## Phase Boundary

Add registry cover-photo support across the three owner surfaces:

1. **Inline cover-photo picker** on `CreateRegistryScreen` (also reachable in edit mode and via tap-to-change on `RegistryDetailScreen`'s 180 dp hero) offering two sources:
   - **Bundled occasion presets** — 6 curated photos per occasion × 6 occasions (Housewarming, Wedding, Baby, Birthday, Christmas, Custom) = 36 drawable-xxhdpi JPEGs shipped in the APK.
   - **Gallery picker** — Android Photo Picker → Firebase Storage upload at `/users/{uid}/registries/{registryId}/cover.jpg`. Adds `firebase-storage` to the Firebase BoM 34.11.0 bundle (currently absent).

2. **Themed placeholder rendering** on `RegistryCardPrimary` and `RegistryCardSecondary` for the `Registry.imageUrl == null` case. Currently both cards call `AsyncImage(model = registry.imageUrl, …)` with no null fallback — the visible bug from the user's screenshot. Extend the Phase 11 hero placeholder (`accentSoft → accent` vertical gradient + `OccasionCatalog.glyphFor(occasion)` in Instrument Serif italic, paper colour) to both card variants. `RegistryDetailHero` already implements this pattern — extract to a shared composable.

Out of scope: per-occasion theme cascade (THEME-01/02/03 still deferred to v1.2), per-item image upload (Phase 11's Manual-mode imageUrl input remains URL-only), camera capture, URL-paste source for cover photos, multi-image carousel, image-cropping UI, web-fallback rendering of preset-backed registries (Claude's discretion at planning time).

</domain>

<decisions>
## Implementation Decisions

### Photo Source & Catalog

- **D-01:** Two cover-photo sources: (a) bundled per-occasion presets, (b) Android Photo Picker (gallery). **No camera capture, no URL paste.**
- **D-02:** 6 presets per occasion × 6 occasions in `OccasionCatalog` = 36 bundled drawable-xxhdpi JPEGs. Curation list (36 stock-licensed images, owner-flow appropriate) is part of the Phase 12 plan deliverable.
- **D-03:** Each registry has exactly one cover photo (no carousel / rotation).

### Storage Backend

- **D-04:** Gallery uploads → Firebase Storage. Adds `firebase-storage` to the Firebase Android BoM bundle (BoM 34.11.0). Use the main module — **not** `firebase-storage-ktx` (KTX modules removed in BoM v34, per CLAUDE.md).
- **D-05:** Storage path schema: `/users/{uid}/registries/{registryId}/cover.jpg` — single canonical filename so a new upload overwrites the prior cover (no orphan storage). Download URL persisted on `Registry.imageUrl`.
- **D-06:** Image processing before upload: auto-resize to **1280×720 max (16:9)**, JPEG quality 85, no crop UI. Hero (180 dp) and cards (16:9) both use `ContentScale.Crop`, so the image fits without an explicit crop screen. Predictable upload size ~150–300 KB.
- **D-07:** Upload is triggered during `viewModel.onSave()`, not at pick time. Pick stages a content URI; the ViewModel uploads, awaits the download URL, then writes the registry document with `imageUrl` set. Failure surfaces in the existing `error: StateFlow<String?>` channel.
- **D-08:** New Firebase Storage rules file (`storage.rules` + wired in `firebase.json`). Baseline: write = registry owner only, read = registry members or public when registry visibility is `"public"`. Researcher should consult Firebase Storage rules docs and existing Firestore rules patterns (Phase 1) for consistency.

### Picker UI & Placement

- **D-09:** Inline 16:9 preview block at the **top of the form** in `CreateRegistryScreen` (above the existing occasion tile grid). Renders the current pick (preset thumbnail or remote URL via Coil 3 `AsyncImage`) or, when nothing is chosen, the themed placeholder.
- **D-10:** Tapping the preview opens a Material3 `ModalBottomSheet` containing: a 3×2 grid (6 thumbnails) of the **currently-selected occasion's** presets, plus a full-width "Pick from gallery" pill button below. Optional "Remove cover photo" action — implementation placement (sheet menu? long-press?) is Claude's discretion.
- **D-11:** Sheet content is reactive to occasion: changing occasion in the form updates the preset grid live. If the user has already picked a preset and then switches occasion, the picked preset is **cleared** (back to placeholder) so a Wedding registry never displays a Birthday preset.
- **D-12:** **Order gate** — the picker is disabled until an occasion is selected. Disabled-state preview shows the gradient placeholder + caption "Pick an occasion to see suggested covers" (new string key `cover_photo_pick_occasion_first`). Gallery picking is also gated until occasion exists (the upload path needs `registryId`/`occasion` context).
- **D-13:** Picker reachable on three surfaces:
   - (a) `CreateRegistryScreen` — create flow.
   - (b) Same screen in **edit mode** (`registryId != null` branch already exists).
   - (c) `RegistryDetailScreen` — owner taps the 180 dp hero to open the same `ModalBottomSheet`. Tap target is **owner-only** (gate via existing `isOwner` check); guests / web viewers see no tap affordance.

### Card & Hero Placeholder

- **D-14:** When `Registry.imageUrl == null`, render `Brush.verticalGradient(accentSoft → accent)` + `OccasionCatalog.glyphFor(occasion)` in Instrument Serif italic, `colors.paper`. Glyph size: **40 sp** on the 180 dp hero (existing — keep), **~32 sp** on cards (16:9 area, smaller surface).
- **D-15:** Apply to `RegistryCardPrimary` (dark-ink card) AND `RegistryCardSecondary` (paperDeep card). Placeholder gradient renders **only inside the 16:9 image area** — the rest of each card (title, stats row) keeps its current ink/paperDeep surface treatment. The two cards differ in body styling, not in placeholder behaviour.
- **D-16:** Extract a shared `HeroImageOrPlaceholder(imageUrl, occasion, glyphSize, modifier)` composable from the existing `RegistryDetailHero` placeholder block; consume it from `RegistryDetailHero`, `RegistryCardPrimary`, and `RegistryCardSecondary`. Existing pixel contract on the hero must not regress.

### Claude's Discretion

- **Preset selection encoding on `Registry.imageUrl`** — `Registry.imageUrl` is `String?`. Bundled drawables aren't HTTP URLs. Options: (a) sentinel scheme like `preset:wedding:3` + a Coil 3 mapper that resolves to `R.drawable.preset_wedding_3`; (b) `android.resource://com.giftregistry/drawable/preset_wedding_3` URIs (Coil handles natively); (c) bundle the 36 PNGs to a public Firebase Storage path so presets are also URLs. Planner picks based on Coil 3 fetcher capabilities and web-fallback impact (see next bullet).
- **Web fallback rendering of preset-backed registries** — Android drawables don't exist on the React web fallback. Either (a) accept that web shows the gradient+glyph placeholder for preset-backed registries (degradation), or (b) copy the 36 PNGs to a public Storage path so the same `imageUrl` works on both clients. Decide at planning time; lean toward (b) only if curation/cost is acceptable.
- **"Remove cover photo" UX placement** — sheet menu item? long-press on the preview? Recommended: include the action inside the picker bottom sheet as a low-emphasis text button at the bottom.
- **Exact 6-image curation per occasion** — 36 stock-licensed JPEG files (1280×720 each, owner-flow appropriate). Plan should call out the asset-acquisition task explicitly; placeholder filler images are acceptable for the first execution wave if real curation slips.
- **Picker thumbnail aspect** — 3×2 grid in the bottom sheet; each tile is itself 16:9 (matches card ratio so previews are honest).
- **Image-resize implementation** — Coil 3's transformation API vs. Android's `BitmapFactory.Options.inSampleSize` + `Bitmap.compress`. Planner researches; prefer Coil 3 if it can run pre-upload.
- **Upload progress UI in form** — minimum: existing `isSaving` `CircularProgressIndicator` covers it. Optional: dedicated upload progress bar inside the preview block.
- **Storage rules wording** — baseline above; planner produces final rule text.

### Folded Todos

None — `gsd-tools todo match-phase 12` returned 0 matches against Phase 12 keywords.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase 11 contract (most important — locks the placeholder pattern)
- `.planning/phases/11-registry-detail-create-add-item-redesign/11-CONTEXT.md` — defines the `accentSoft → accent` gradient + occasion glyph hero placeholder we're extending to cards. Also notes `Registry.imageUrl` field origin and that "Manual mode image upload is deferred" (item-image upload — separate from this phase's registry-cover upload).

### Phase 10 contract (Registry.imageUrl + card variants)
- `.planning/phases/10-onboarding-home-redesign/10-CONTEXT.md` — introduces `Registry.imageUrl: String?`, `RegistryCardPrimary` vs `RegistryCardSecondary` visual contract, and the `isPrimary = maxByOrNull(updatedAt)` selection rule.

### Existing source files (must read before modifying)
- `app/src/main/java/com/giftregistry/ui/registry/list/RegistryCard.kt` — current `AsyncImage` calls with no null fallback (the visible bug); reuse the private `OccasionPill` composable inside the Box overlay; extract gradient placeholder.
- `app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailHero.kt` — **reference implementation of the gradient + glyph placeholder** (lines 86–129). Extract to shared `HeroImageOrPlaceholder` composable; do not regress the 3-stop overlay (`ink44` top → transparent 40% → `inkAA` bottom) or pinned-toolbar alpha behaviour.
- `app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryScreen.kt` — picker insertion point (above `OccasionTileGrid`). **Preserve all existing form/save behaviour, `LaunchedEffect(savedRegistryId)`, edit-mode branching, error surface, custom Row top bar.**
- `app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryViewModel.kt` — add `coverPhotoSelection` state + upload trigger inside `onSave()`. Preserve all existing 11 StateFlows + the date picker.
- `app/src/main/java/com/giftregistry/ui/registry/create/OccasionCatalog.kt` — `glyphFor(occasion: String?)` is the single source of truth for occasion → glyph; reuse, don't fork.
- `app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt` — owner-only tap target on the 180 dp hero; existing `isOwner` check.
- `app/src/main/java/com/giftregistry/domain/model/Registry.kt` — `imageUrl: String?` field already present (Phase 10). No schema change.
- `app/src/main/java/com/giftregistry/data/registry/RegistryRepositoryImpl.kt` (or equivalent — researcher to confirm exact path) — current Firestore document write path; extend to set `imageUrl` post-upload.

### Build / config
- `CLAUDE.md` — Firebase Android BoM 34.11.0 (no KTX modules — use `firebase-storage` main module). I18N-02 strings.xml + values-ro/strings.xml requirement. Hilt 2.51.x for DI.
- `app/build.gradle.kts` (or `libs.versions.toml`) — add `firebase-storage` to the Firebase bundle.
- `firebase.json` + new `storage.rules` — Firebase Storage rules wiring.

### External design handoff (user-supplied)
- `/Users/victorpop/Downloads/design_handoff_android_owner_flow 2/README.md` — visual contract for the 5 owner-flow screens (Onboarding, Home, Registry Detail, Create Registry, Add Item). The handoff does NOT include an explicit cover-photo picker — Phase 12's picker visual is Claude's discretion within the handoff's design system (radii: 14 for tiles, 12 for inputs, 10 for chips; spacing tokens; mono-caps labels; accent/accentSoft/paper/paperDeep/line palette).
- `/Users/victorpop/Downloads/design_handoff_android_owner_flow 2/screens/09-create-registry.png` — the Create-registry artboard (no cover-photo block in the original; Phase 12 inserts it above the occasion grid).
- `/Users/victorpop/Downloads/design_handoff_android_owner_flow 2/screens/07-home-registries.png` — Home cards artboard; the gradient + glyph placeholder is the new visual on these cards.
- `/Users/victorpop/Downloads/design_handoff_android_owner_flow 2/reference/theme.jsx` — colour tokens (accent, accentSoft, accentInk, paper, paperDeep, ink, line) — already converted to sRGB hex in `GiftMaisonTheme`.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets (Phase 8–11 shipped)
- `GiftMaisonTheme.colors / typography / shapes / spacing` — accent, accentSoft, accentInk, paper, paperDeep, ink, line, plus Instrument Serif italic for glyphs.
- `OccasionCatalog.glyphFor(occasion: String?): String` — already used by `RegistryDetailHero` placeholder.
- `Brush.verticalGradient(accentSoft → accent)` — pattern locked in `RegistryDetailHero` lines 109–117; extract to shared composable.
- Coil 3 `AsyncImage` — the rendering primitive for both URLs and (with a custom mapper/fetcher) for `preset:` sentinels, depending on the encoding chosen.
- `Registry.imageUrl: String?` — domain model field; no migration needed.
- `RegistryDetailScreen` `isOwner` check — gate the new tap target.
- `CreateRegistryScreen` skip / save / edit-mode branching — preserve verbatim; just add the cover-photo input.
- Existing 1.5 dp accent border on focused inputs and 14 dp tile radius — apply consistently to the cover-photo preview / sheet thumbnails so the picker harmonises with the rest of the form.

### Established Patterns
- **Single-screen create + edit** — `CreateRegistryScreen` handles both via `registryId == null` branch.
- **Repository-driven save flow** — VM exposes `savedRegistryId: StateFlow<String?>`; `LaunchedEffect(savedRegistryId)` routes to `onSaved` / `onSkip`. Cover-photo upload must complete before `savedRegistryId` is emitted (otherwise navigation races the upload).
- **I18N-02** — every new label lives in `app/src/main/res/values/strings.xml` + `app/src/main/res/values-ro/strings.xml`. Phase 12 adds ~6–10 new keys: `cover_photo_label`, `cover_photo_pick_occasion_first`, `cover_photo_pick_from_gallery`, `cover_photo_remove`, `cover_photo_uploading`, `cover_photo_upload_failed`, etc.
- **Hilt DI** — wire a new `StorageRepository` (or extend `RegistryRepositoryImpl`) for cover uploads.
- **Firebase data-layer pattern** — `callbackFlow + awaitClose` for listeners (not relevant here — uploads are one-shot suspend), `runCatching` to convert Firebase exceptions to `Result.failure` keeping FirebaseExceptions out of the domain layer (Phase 02 decision).

### Integration Points
- `app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryScreen.kt` — insert `CoverPhotoPicker` composable above the existing `OccasionTileGrid` call (line ~240).
- `app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryViewModel.kt` — add `coverPhotoSelection: MutableStateFlow<CoverPhotoSelection>` (sealed class: `None`, `Preset(occasion: String, index: Int)`, `Gallery(uri: Uri)`); upload Gallery selections during `onSave()`.
- `app/src/main/java/com/giftregistry/ui/registry/list/RegistryCard.kt` — replace the bare `AsyncImage(model = registry.imageUrl)` calls in BOTH `RegistryCardPrimary` and `RegistryCardSecondary` with the new `HeroImageOrPlaceholder` shared composable.
- `app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailHero.kt` — replace inline image+placeholder block with `HeroImageOrPlaceholder` (preserve the 3-stop overlay on the URL path).
- `app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt` — add owner-only tap target on the hero; route to the new `CoverPhotoPickerSheet` (potentially via a shared callback hoisted to the screen).
- `app/src/main/java/com/giftregistry/data/storage/` — **new package** for `StorageDataSource` (Firebase Storage wrapper) + `StorageRepository`.
- `app/src/main/java/com/giftregistry/ui/registry/cover/` — **new package** for `CoverPhotoPicker`, `CoverPhotoPickerSheet`, `HeroImageOrPlaceholder`, `CoverPhotoSelection` sealed class, `PresetCatalog`.
- `app/src/main/res/drawable-xxhdpi/preset_*.jpg` — **36 new bundled assets** (placeholder set acceptable for first wave; production curation may follow).
- `app/build.gradle.kts` (and/or `libs.versions.toml`) — add `firebase-storage` to the Firebase BoM bundle.
- `storage.rules` (new) + `firebase.json` — wire Storage rules.
- `app/src/main/res/values/strings.xml` + `values-ro/strings.xml` — new keys.
- `StyleGuidePreview.kt` — append Phase 12 preview sections: `CoverPhotoPickerInline`, `CoverPhotoPickerSheet (occasion: Wedding)`, `RegistryCardPrimaryWithPlaceholder`, `RegistryCardSecondaryWithPlaceholder`.

</code_context>

<specifics>
## Specific Ideas

- **Phase 11 hero placeholder pattern is the locked source of truth** for the gradient + glyph rendering. Cards must look harmonious with the hero — same gradient direction (vertical, accentSoft top → accent bottom), same glyph (Instrument Serif italic, paper colour), same sizing logic.
- **1280×720 (16:9)** is chosen because Hero (180 dp height, ~360 dp wide on standard phones) and cards (16:9 aspect) both render at that or smaller sizes — no upscaling, no oversized uploads.
- **"Pick an occasion to see suggested covers"** is the recommended wording for the disabled-state hint when no occasion is selected. Lock during planning.
- **Tap-to-change on the Detail hero is owner-only.** Guests / web fallback never see a tap affordance. Phase 1 security rules already restrict write to owners — this is a UI gate, not the security boundary.
- **Single-canonical Storage filename** (`cover.jpg`) means a re-upload overwrites cleanly — no orphans, no garbage-collection job needed.

</specifics>

<deferred>
## Deferred Ideas

- **Per-occasion theme cascade** (THEME-01/02/03) — accent/accentSoft hue shifts per registry occasion. Still deferred to v1.2; Phase 12 uses the Housewarming palette regardless of registry occasion.
- **Camera capture** — explicit no for Phase 12 (user decision).
- **URL-paste source** for cover photos — explicit no for Phase 12 (user decision).
- **Image cropping UI** — auto-resize to 1280×720 covers the need; explicit crop tool deferred indefinitely.
- **Web fallback rendering of preset-backed registries** — see Claude's Discretion. May be a follow-up phase if drawable-only encoding is chosen.
- **Per-item image upload** — Phase 11 deferred this for items; remains deferred. Phase 12 is registry-cover only.
- **Multi-image carousel / cover photo rotation** — out of scope.
- **"Remove cover photo" advanced UX** — basic remove action is in scope (Claude's discretion on placement); a more elaborate trash/undo flow is deferred.

### Reviewed Todos (not folded)

None — no pending todos matched Phase 12.

</deferred>

---

*Phase: 12-registry-cover-photo-themed-placeholder*
*Context gathered: 2026-04-27*
