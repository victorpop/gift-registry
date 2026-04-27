# Phase 12: Registry Cover Photo & Themed Placeholder - Research

**Researched:** 2026-04-27
**Domain:** Android image picker + upload (Firebase Storage), Coil 3 image rendering, Compose Material3 modal sheet, themed gradient placeholder
**Confidence:** HIGH

## Summary

Phase 12 layers a cover-photo capability on top of the existing Phase 10/11 registry surfaces without changing data models or repositories. The technical work is well bounded: (a) add the `firebase-storage:22.0.1` main module to the Firebase BoM 34.11.0 bundle, (b) wire the AndroidX Photo Picker (`ActivityResultContracts.PickVisualMedia` with `ImageOnly`), (c) downscale and JPEG-compress the picked URI before `putFile().await()`, (d) extract a single `HeroImageOrPlaceholder` composable consumed by `RegistryDetailHero` + `RegistryCardPrimary` + `RegistryCardSecondary`, (e) ship 36 bundled drawables addressed by stable resource IDs (no Coil mapper needed), and (f) author a fresh `storage.rules` file deployed alongside the existing Firestore rules.

Two existing-codebase issues surfaced during research and must be fixed in the phase plan: `RegistryRepositoryImpl.toMap()` and `toUpdateMap()` do **not** include `imageUrl`, so even a fully wired upload path would silently drop the URL on save. `RegistryDto` is also missing the `imageUrl` field. Both are schema completion bugs left over from Phase 10's domain-model addition.

**Primary recommendation:** Encode presets as plain `Int` Android resource IDs passed directly to Coil 3's `AsyncImage(model = R.drawable.preset_wedding_3, …)`. Persist preset selection on `Registry.imageUrl` using a `preset:wedding:3` sentinel string and resolve it client-side via a tiny `PresetCatalog.resolve(sentinel): Int?` lookup table. This avoids `android.resource://` URIs (deprecated by Coil 3 because they block resource shrinking), avoids a custom `Mapper` registration, and keeps the door open for a follow-up phase that uploads the same 36 PNGs to public Firebase Storage for cross-client (web fallback) parity.

## User Constraints (from CONTEXT.md)

### Locked Decisions

**Photo Source & Catalog**
- **D-01:** Two cover-photo sources: (a) bundled per-occasion presets, (b) Android Photo Picker (gallery). **No camera capture, no URL paste.**
- **D-02:** 6 presets per occasion × 6 occasions = 36 bundled `drawable-xxhdpi` JPEGs. Curation list is part of the Phase 12 plan deliverable.
- **D-03:** Each registry has exactly one cover photo (no carousel / rotation).

**Storage Backend**
- **D-04:** Gallery uploads → Firebase Storage. Add `firebase-storage` to the BoM 34.11.0 bundle. Use the **main** module — not `firebase-storage-ktx`.
- **D-05:** Storage path: `/users/{uid}/registries/{registryId}/cover.jpg`. Single canonical filename; new uploads overwrite. Download URL persisted on `Registry.imageUrl`.
- **D-06:** Pre-upload processing: auto-resize to **1280×720 max (16:9)**, JPEG quality 85, no crop UI. Both hero (180 dp) and cards (16:9) use `ContentScale.Crop`.
- **D-07:** Upload triggered during `viewModel.onSave()` (not at pick time). Pick stages a content URI; VM uploads, awaits download URL, then writes the registry doc with `imageUrl`. Failure surfaces in existing `error: StateFlow<String?>`.
- **D-08:** New `storage.rules` file wired in `firebase.json`. Baseline: write = registry owner, read = registry members or public when `registry.visibility == "public"`. Cross-check with Phase 1 Firestore rules patterns.

**Picker UI & Placement**
- **D-09:** Inline 16:9 preview block at the top of the Create form (above the existing occasion tile grid). Renders current pick (preset thumb / remote URL via Coil 3) or themed placeholder.
- **D-10:** Tapping the preview opens a Material3 `ModalBottomSheet`: 3×2 grid (6 thumbnails) of the **currently selected** occasion's presets + full-width "Pick from gallery" pill below. Optional "Remove cover photo" — placement at Claude's discretion.
- **D-11:** Sheet content reactive to occasion. Switching occasion clears any picked preset (back to placeholder) so a Wedding never displays a Birthday preset.
- **D-12:** **Order gate** — picker disabled until an occasion is selected. Disabled-state preview shows gradient placeholder + caption `cover_photo_pick_occasion_first`. Gallery picking also gated.
- **D-13:** Picker reachable on three surfaces: (a) `CreateRegistryScreen` create flow, (b) same screen in edit mode, (c) `RegistryDetailScreen` — owner-only tap on the 180 dp hero. Guests / web see no tap affordance.

**Card & Hero Placeholder**
- **D-14:** When `Registry.imageUrl == null`, render `Brush.verticalGradient(accentSoft → accent)` + `OccasionCatalog.glyphFor(occasion)` in Instrument Serif italic, `colors.paper`. Glyph 40 sp on 180 dp hero, ~32 sp on cards.
- **D-15:** Apply to BOTH `RegistryCardPrimary` (dark-ink card) AND `RegistryCardSecondary` (paperDeep card). Placeholder gradient inside the 16:9 image area only — title/stats row keeps current treatment.
- **D-16:** Extract a shared `HeroImageOrPlaceholder(imageUrl, occasion, glyphSize, modifier)` composable from existing `RegistryDetailHero` placeholder block. Existing pixel contract on hero must not regress.

### Claude's Discretion

- **Preset selection encoding** — sentinel scheme vs `android.resource://` URI vs Storage URLs.
- **Web fallback rendering** of preset-backed registries — accept gradient placeholder OR copy 36 PNGs to public Storage.
- **"Remove cover photo" UX placement** — sheet menu item / long-press / etc.
- **Exact 6-image curation per occasion** — placeholder filler acceptable for first wave.
- **Picker thumbnail aspect** — 3×2 grid; each tile 16:9.
- **Image-resize implementation** — Coil 3 transformation API vs `BitmapFactory.Options.inSampleSize` + `Bitmap.compress`.
- **Upload progress UI in form** — minimum: existing `isSaving` indicator. Optional: dedicated upload progress bar.
- **Storage rules wording** — baseline given in D-08.

### Deferred Ideas (OUT OF SCOPE)

- **Per-occasion theme cascade** (THEME-01/02/03) — v1.2.
- **Camera capture** — explicit no.
- **URL-paste source for cover photos** — explicit no.
- **Image cropping UI** — auto-resize covers the need; deferred indefinitely.
- **Web fallback rendering of preset-backed registries** — may be a follow-up phase.
- **Per-item image upload** — Phase 11 deferred this; remains deferred.
- **Multi-image carousel / cover photo rotation** — out of scope.
- **"Remove cover photo" advanced UX** — basic remove in scope; richer trash/undo deferred.

## Project Constraints (from CLAUDE.md)

| Directive | Implication for Phase 12 |
|-----------|--------------------------|
| Firebase Android BoM **34.11.0**, no KTX modules | Add `firebase-storage` (main, version 22.0.1 from BoM 34.11.0). Do **not** add `firebase-storage-ktx`. |
| Compose BOM **2026.03.00** | `ModalBottomSheet` is `@ExperimentalMaterial3Api` and lives in `androidx.compose.material3`. |
| Kotlin **2.3.20**, AGP **8.13.0** (note: project is on AGP 8.x, not 9.x as the CLAUDE.md aspirational text suggests) | KSP 2.3.6 is the annotation processor. No `kapt`. |
| Hilt **2.59.2** (verified in `libs.versions.toml`; CLAUDE.md says 2.51.x — verify before committing the version) | Use `@HiltViewModel` + `@Inject` for the new `StorageRepository`. |
| Coil **3.4.0** | Pass `R.drawable.*` Int IDs to `AsyncImage` directly. `android.resource://` URIs are unsupported in Coil 3 unless `ResourceUriMapper` is manually registered. |
| `kotlinx-coroutines-tasks` is implicitly available via the Firebase main modules | `uploadTask.await()` and `downloadUrl.await()` work directly — same pattern used in `FirebaseAuthDataSource.kt`, `ItemRepositoryImpl.kt`, `ReservationRepositoryImpl.kt`. |
| **I18N-02** — every label in `app/src/main/res/values/strings.xml` + `values-ro/strings.xml` | ~10 new keys with Romanian translations. |
| **GSD workflow enforcement** — start through a GSD command before file edits. | Implementation must run via `/gsd:execute-phase 12`. |
| Domain layer must contain zero Firebase imports (Phase 02 decision) | `StorageRepository` interface in `domain/storage/`; `StorageRepositoryImpl` + `StorageDataSource` in `data/storage/`. |
| `runCatching { … }` wraps Firebase suspends to convert exceptions to `Result.failure` (Phase 02 decision) | Same pattern in the new upload path. |

## Phase Requirements

CONTEXT.md tags this phase as a feature add with **no explicit REQ-IDs** in REQUIREMENTS.md (the additional context block confirms `null` for IDs). Treat the locked `<decisions>` block as the requirement set.

Mapping from decisions to research support:

| Decision ID | Behavior | Research Support |
|-------------|----------|------------------|
| D-01 | Two photo sources, no camera/URL | Photo Picker (Pattern 1) + bundled drawables (Pattern 4) |
| D-02 | 36 bundled JPEGs per `OccasionCatalog` order | `drawable-xxhdpi/` placement + `PresetCatalog` lookup table (Pattern 5) |
| D-03 | One cover photo per registry | Single `imageUrl: String?` field (already exists) |
| D-04 | `firebase-storage` main module on BoM 34.11.0 | Maven verification: 22.0.1 in BoM 34.11.0 |
| D-05 | Path `/users/{uid}/registries/{registryId}/cover.jpg` | `StorageReference.child(...)` (Pattern 2) |
| D-06 | 1280×720 JPEG q=85 pre-upload | `BitmapFactory.Options.inSampleSize` + `Bitmap.compress` (Pattern 3) |
| D-07 | Upload during `onSave()`, surface error in `error` flow | `runCatching` + `await()` suspend pattern (Pattern 2) |
| D-08 | `storage.rules` file with cross-service Firestore lookup | `firestore.get()` cross-service rules (Pattern 6) |
| D-09 / D-10 / D-11 / D-12 | Inline preview + ModalBottomSheet picker, occasion-gated | `ModalBottomSheet` + `LazyVerticalGrid` (Pattern 7) |
| D-13 | Tap-to-change on Detail hero, owner-only | `isOwner` derived in `RegistryDetailViewModel`; gate the `onTap` callback |
| D-14 / D-15 / D-16 | Themed placeholder + shared `HeroImageOrPlaceholder` | Extract from existing `RegistryDetailHero` (line 108–129) (Pattern 8) |

## Standard Stack

### Core (verified)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `com.google.firebase:firebase-storage` | **22.0.1** (via Firebase BoM 34.11.0) | Cloud Storage upload/download | Main module. KTX module removed in BoM v34 (July 2025). Same SDK family already in app for Auth/Firestore/Functions/Messaging. |
| `androidx.activity:activity-compose` | already on classpath | `rememberLauncherForActivityResult` host for Photo Picker | Standard Compose-side Activity Result API; no extra dep needed. |
| `androidx.compose.material3:material3` | via Compose BOM 2026.03.00 | `ModalBottomSheet` | Phase 8 already integrated M3. `ModalBottomSheet` has been stable (despite the `@ExperimentalMaterial3Api` annotation) since 2024. |
| `io.coil-kt.coil3:coil-compose` | **3.4.0** | `AsyncImage(model = …)` for both URL and `Int` resource ID | Already in project. Confirmed Feb 2026 release; no upgrade needed. |
| `kotlinx-coroutines-tasks` | bundled | `Task.await()` extension | Used throughout existing data layer (`FirebaseAuthDataSource`, `ItemRepositoryImpl`, etc.). No new dep. |

**No new test dependencies required** — existing `mockk 1.13.17` + `turbine 1.2.0` + `kotlinx-coroutines-test 1.9.0` cover the upload-state ViewModel tests.

### Version Verification

`firebase-storage` 22.0.1 mapping confirmed via Firebase Android SDK Release Notes (BoM 34.11.0 published 2026-03-19). Coil 3.4.0 confirmed via official changelog (released 2026-02-24).

**Action for plan:** add to `libs.versions.toml`:

```toml
[libraries]
firebase-storage = { group = "com.google.firebase", name = "firebase-storage" }
```

Then in `app/build.gradle.kts`, append `implementation(libs.firebase.storage)` next to the other Firebase main modules.

### Alternatives Considered (and rejected)

| Instead of | Could Use | Why Rejected |
|------------|-----------|--------------|
| `BitmapFactory.Options.inSampleSize` for resize | Coil 3 transformations | **Coil transformations apply at display time, not before upload.** They cannot be used to produce a pre-upload byte stream. Confirmed via the Coil image pipeline docs — transformations run inside `decode()` and feed the in-memory `Image`, not a serialised file. The `BitmapFactory` path is the standard pre-upload reduction technique on Android. |
| `R.drawable.*` Int passed via a `Mapper` | Direct `Int` to `AsyncImage(model = ...)` | Coil 3 supports `Int` resource IDs natively (see Pattern 4 below). A custom mapper is unnecessary overhead. |
| `android.resource://` URI scheme | Direct `Int` resource ID | Coil 3 explicitly removed default support to allow R8 resource shrinking. Re-enabling via `ResourceUriMapper` is possible but disables resource shrinking — bad tradeoff for 36 bundled assets. |
| Bundle 36 PNGs to public Storage | Bundled drawables | (Decided by user — bundled was the explicit Q1 choice. Storage approach lives only as a follow-up for web fallback parity per Open Question 2.) |
| `androidx.activity:activity-ktx` `PickVisualMedia` | `ActivityResultContracts.PickVisualMedia` | Same API. The Compose-side `rememberLauncherForActivityResult` already wraps it; no separate KTX dep. |
| `cropImage` library (uCrop, ImageCropper) | No-crop + auto-resize 16:9 | User D-06 explicitly excludes a crop UI. `ContentScale.Crop` at display time is good enough. |
| Custom URL parser (regex on `imageUrl`) for sentinel detection | `String.startsWith("preset:")` + small `when` | A tiny lookup table is simpler, testable in pure Kotlin (Wave 0), and zero additional libraries. |

## Architecture Patterns

### Recommended Project Structure

```
app/src/main/java/com/giftregistry/
├── domain/storage/
│   └── StorageRepository.kt              # interface { suspend fun uploadCover(uid, registryId, jpegBytes): Result<String> }
├── data/storage/
│   ├── StorageDataSource.kt              # FirebaseStorage wrapper — putBytes + downloadUrl + cancellation token
│   └── StorageRepositoryImpl.kt          # @Singleton @Inject; runCatching { … }
├── ui/registry/cover/                    # NEW package
│   ├── CoverPhotoSelection.kt            # sealed interface { None, Preset(occasion: String, index: Int), Gallery(uri: Uri) }
│   ├── PresetCatalog.kt                  # data class PresetEntry(@DrawableRes id: Int) per (occasion, index)
│   ├── CoverPhotoPicker.kt               # 16:9 inline preview block (Composable)
│   ├── CoverPhotoPickerSheet.kt          # ModalBottomSheet (3×2 grid + gallery pill + remove)
│   ├── HeroImageOrPlaceholder.kt         # shared (consumed by RegistryDetailHero, RegistryCardPrimary/Secondary)
│   └── ResolveImageModel.kt              # imageUrl: String? → Any model: Int | String | null
├── ui/registry/create/CreateRegistryScreen.kt   # MUTATED — add CoverPhotoPicker above OccasionTileGrid
├── ui/registry/create/CreateRegistryViewModel.kt # MUTATED — add coverPhotoSelection + uploadCover invocation
├── ui/registry/detail/RegistryDetailScreen.kt   # MUTATED — owner-only tap on hero opens sheet
├── ui/registry/detail/RegistryDetailHero.kt     # REFACTOR — replace inline placeholder block with HeroImageOrPlaceholder
├── ui/registry/list/RegistryCard.kt             # MUTATED — replace bare AsyncImage with HeroImageOrPlaceholder
├── data/registry/RegistryRepositoryImpl.kt      # FIX — add imageUrl to toMap() AND toUpdateMap()
├── data/model/RegistryDto.kt                    # FIX — add `val imageUrl: String? = null` field
└── res/
    ├── drawable-xxhdpi/preset_housewarming_1.jpg ... preset_custom_6.jpg     # 36 NEW assets
    ├── values/strings.xml                       # +10 new keys
    └── values-ro/strings.xml                    # +10 RO translations
```

```
storage.rules                                      # NEW (root-level, mirrors firestore.rules)
firebase.json                                      # MUTATED — add { "storage": { "rules": "storage.rules" } }
```

### Pattern 1: Compose Photo Picker (single image, gated)

```kotlin
// Source: https://developer.android.com/training/data-storage/shared/photo-picker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia

@Composable
fun rememberPickImageLauncher(onPicked: (Uri) -> Unit) =
    rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) onPicked(uri)
        // Null = user dismissed. No-op; preview keeps current selection.
    }

// Inside CoverPhotoPickerSheet:
val launcher = rememberPickImageLauncher { uri ->
    onSelectionChanged(CoverPhotoSelection.Gallery(uri))
    onDismiss()
}
// Click handler:
launcher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
```

**No runtime permissions needed.** The Photo Picker handles READ_MEDIA_IMAGES internally and works back to API 19 via the embedded fallback. Returns a content URI Coil 3 reads natively (`AsyncImage(model = uri)`).

### Pattern 2: Pre-upload bitmap downscale + JPEG compress

```kotlin
// Source: https://developer.android.com/topic/performance/graphics/load-bitmap
//         + Bitmap.CompressFormat.JPEG quality=85 (D-06)
//
// Two-pass decode: bounds first (cheap), then sample-size-clamped decode.
// Power-of-2 inSampleSize because non-power-of-2 values are rounded down by BitmapFactory anyway.

internal fun ContentResolver.decodeAndCompressForCover(
    uri: Uri,
    targetMaxWidth: Int = 1280,
    targetMaxHeight: Int = 720,
    quality: Int = 85,
): ByteArray {
    // Pass 1: bounds-only decode
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        ?: throw IOException("Cannot read URI: $uri")

    // Compute power-of-2 sample size
    var sample = 1
    while (
        bounds.outWidth / (sample * 2) >= targetMaxWidth ||
        bounds.outHeight / (sample * 2) >= targetMaxHeight
    ) sample *= 2

    // Pass 2: sampled decode
    val decoded = openInputStream(uri)?.use {
        BitmapFactory.decodeStream(
            it, null,
            BitmapFactory.Options().apply { inSampleSize = sample },
        )
    } ?: throw IOException("Cannot decode URI: $uri")

    // Final fit to 1280×720 box (preserve aspect)
    val scaled = decoded.scaleToFit(targetMaxWidth, targetMaxHeight)
    if (scaled !== decoded) decoded.recycle()

    return ByteArrayOutputStream().also {
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, it)
        scaled.recycle()
    }.toByteArray()
}

private fun Bitmap.scaleToFit(maxW: Int, maxH: Int): Bitmap {
    if (width <= maxW && height <= maxH) return this
    val scale = minOf(maxW.toFloat() / width, maxH.toFloat() / height)
    return Bitmap.createScaledBitmap(this, (width * scale).toInt(), (height * scale).toInt(), true)
}
```

**Where it runs:** inside `StorageRepositoryImpl.uploadCover(...)` on `Dispatchers.IO` — never on the main thread. The function returns a `ByteArray` that feeds `StorageReference.putBytes(...)` (preferred over `putFile` here because we already have bytes in memory and don't want to spill to a temp file).

### Pattern 3: Firebase Storage upload + downloadUrl roundtrip (suspend)

```kotlin
// Source: https://firebase.google.com/docs/storage/android/upload-files
//         + project pattern: runCatching wraps Firebase suspend (Phase 02 D-08)

@Singleton
class StorageRepositoryImpl @Inject constructor(
    private val storage: FirebaseStorage,           // wired in DataModule (Hilt)
) : StorageRepository {

    override suspend fun uploadCover(
        uid: String,
        registryId: String,
        jpegBytes: ByteArray,
    ): Result<String> = runCatching {
        val ref = storage.reference.child("users/$uid/registries/$registryId/cover.jpg")
        // putBytes returns UploadTask; await() suspends until success/failure.
        ref.putBytes(jpegBytes).await()
        ref.downloadUrl.await().toString()           // public download URL string
    }
}
```

`FirebaseStorage` is provided as a singleton in a new (or existing) Hilt `DataModule`:

```kotlin
@Provides
@Singleton
fun provideFirebaseStorage(): FirebaseStorage {
    val instance = FirebaseStorage.getInstance()
    // Emulator support — mirrors existing Firestore wiring for use_emulator=true.
    if (BuildConfig.USE_FIREBASE_EMULATOR) {
        instance.useEmulator("10.0.2.2", 9199)
    }
    return instance
}
```

**Cancellation semantics:** if the user backs out of `CreateRegistryScreen` mid-upload, the `viewModelScope` is cancelled by Hilt's lifecycle handling. `await()` will throw `CancellationException`, propagating cleanly. The `UploadTask` itself does **not** auto-cancel on coroutine cancellation — but in this phase the only practical effect is a small wasted upload; no data corruption because Storage rules + `cover.jpg` overwrite semantics make orphan handling moot. **Do not** call `uploadTask.cancel()` from `onCleared()` — there's a known group thread reporting an unhandled exception when calling `cancel()` on an in-progress upload (FlutterFire #12385 mirror of an Android-SDK issue).

### Pattern 4: Coil 3 with `Int` resource ID OR remote URL via the same `AsyncImage` call

```kotlin
// Source: https://coil-kt.github.io/coil/upgrading_to_coil3/
//         "Passing the resource ID instead of the resource name will still work"

@Composable
fun HeroImageOrPlaceholder(
    imageUrl: String?,
    occasion: String?,
    glyphSize: TextUnit = 32.sp,
    modifier: Modifier = Modifier,
) {
    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    // Resolve the imageUrl to a Coil-friendly model (Int for preset, String for URL, null for placeholder).
    val model: Any? = remember(imageUrl) { resolveImageModel(imageUrl) }

    if (model != null) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier.background(
                Brush.verticalGradient(0f to colors.accentSoft, 1f to colors.accent)
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = OccasionCatalog.glyphFor(occasion),
                style = typography.displayXL.copy(fontSize = glyphSize, fontStyle = FontStyle.Italic),
                color = colors.paper,
            )
        }
    }
}

// Pure-Kotlin resolver — testable in Wave 0.
fun resolveImageModel(imageUrl: String?): Any? = when {
    imageUrl == null -> null
    imageUrl.startsWith("preset:") -> PresetCatalog.resolve(imageUrl)        // Int? (R.drawable.*)
    else -> imageUrl                                                         // String URL
}
```

### Pattern 5: `PresetCatalog` — sentinel <-> drawable resource ID lookup

```kotlin
// Sentinel format: "preset:{occasionStorageKey}:{index}" where index is 1..6.
// e.g., "preset:Wedding:3"

object PresetCatalog {
    /** Per-occasion preset bundle. Preserves OccasionCatalog order. */
    private val byOccasion: Map<String, List<Int>> = mapOf(
        "Housewarming" to listOf(
            R.drawable.preset_housewarming_1, R.drawable.preset_housewarming_2,
            R.drawable.preset_housewarming_3, R.drawable.preset_housewarming_4,
            R.drawable.preset_housewarming_5, R.drawable.preset_housewarming_6,
        ),
        "Wedding" to listOf(
            R.drawable.preset_wedding_1, R.drawable.preset_wedding_2,
            R.drawable.preset_wedding_3, R.drawable.preset_wedding_4,
            R.drawable.preset_wedding_5, R.drawable.preset_wedding_6,
        ),
        // ... Baby / Birthday / Christmas / Custom each map to 6 R.drawable IDs
    )

    /** Returns the 6 preset drawables for the given occasion (already canonicalised by OccasionCatalog). */
    fun presetsFor(occasion: String?): List<Int> =
        byOccasion[OccasionCatalog.storageKeyFor(occasion)] ?: emptyList()

    /** Encodes a (occasion, 1-based index) selection as the canonical sentinel. */
    fun encode(occasion: String, index1Based: Int): String = "preset:$occasion:$index1Based"

    /** Resolves a sentinel string back to its `R.drawable.*` ID. Returns null if malformed. */
    fun resolve(sentinel: String): Int? {
        if (!sentinel.startsWith("preset:")) return null
        val parts = sentinel.removePrefix("preset:").split(":")
        if (parts.size != 2) return null
        val (occasion, indexStr) = parts
        val idx = indexStr.toIntOrNull() ?: return null
        val list = byOccasion[OccasionCatalog.storageKeyFor(occasion)] ?: return null
        return list.getOrNull(idx - 1)
    }
}
```

**Wave 0 testable.** Pure Kotlin — no Compose, no Android framework — except `R.drawable.*` references which are `Int` constants. Tests assert encode/decode roundtrip and graceful nulls for malformed strings.

### Pattern 6: `storage.rules` with cross-service Firestore lookup

```
// Source: https://firebase.google.com/docs/storage/security/rules-conditions (cross-service rules)
//         + Phase 1 firestore.rules canReadRegistry pattern

rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {

    // Helper: look up the registry document via the cross-service Firestore extension.
    // firestore.get() and firestore.exists() read against the project's default Firestore database.
    // Each call counts toward Firestore read quota — keep usage minimal.
    function registryDoc(registryId) {
      return firestore.get(/databases/(default)/documents/registries/$(registryId));
    }

    function isOwnerOfRegistry(registryId) {
      return request.auth != null
          && registryDoc(registryId).data.ownerId == request.auth.uid;
    }

    function isPublicOrInvited(registryId) {
      let data = registryDoc(registryId).data;
      return data.get('visibility', 'public') == 'public'
          || (request.auth != null
              && data.get('invitedUsers', {})[request.auth.uid] == true);
    }

    // Cover photos: write = owner, read = anyone-who-can-read-the-registry.
    // Path schema: /users/{uid}/registries/{registryId}/cover.jpg (D-05)
    match /users/{uid}/registries/{registryId}/cover.jpg {
      allow write: if request.auth != null
                   && request.auth.uid == uid
                   && isOwnerOfRegistry(registryId);
      allow read:  if isOwnerOfRegistry(registryId)
                   || isPublicOrInvited(registryId);
    }

    // Default-deny everything else.
    match /{allPaths=**} {
      allow read, write: if false;
    }
  }
}
```

**Wire-up in `firebase.json`:**

```json
"storage": {
  "rules": "storage.rules"
}
```

**Important constraints (verified in cross-service rules announcement):**

1. **Maximum 2 Firestore reads per Storage rule evaluation.** This rule uses 1 (the `registryDoc` lookup is one document; `let` caches it). Adding more reads pushes us over the budget.
2. **First-time setup prompts a permissions grant** to connect Storage Rules with Firestore. Plan must include a manual Firebase Console checkpoint (or a documented `firebase deploy` instruction that surfaces the prompt).
3. **Each `firestore.get()` is billed as a Firestore read.** For a busy registry with many guest viewers, Storage reads will translate to Firestore reads. This is an accepted cost; there's no caching layer.
4. **Emulator coverage is incomplete.** Cross-service rules unit tests have known gaps in the Firebase JS SDK emulator (issue 6803 in `firebase/firebase-js-sdk`). Plan validation should rely on a manual deploy-to-staging UAT, not emulator-only testing.

### Pattern 7: ModalBottomSheet for the picker

```kotlin
// Source: https://developer.android.com/develop/ui/compose/components/bottom-sheets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverPhotoPickerSheet(
    occasion: String,
    currentSelection: CoverPhotoSelection,
    onSelectionChanged: (CoverPhotoSelection) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val launcher = rememberPickImageLauncher { uri ->
        onSelectionChanged(CoverPhotoSelection.Gallery(uri))
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GiftMaisonTheme.colors.paper,
        // scrimColor defaults to colors.scrim — keep default per handoff
    ) {
        Column(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            Text(
                text = stringResource(R.string.cover_photo_sheet_header),
                style = GiftMaisonTheme.typography.monoCaps,
                color = GiftMaisonTheme.colors.inkFaint,
            )

            // 3×2 grid of presets — non-scrolling LazyVerticalGrid wrapped in heightIn so the sheet sizes naturally
            val presets = remember(occasion) { PresetCatalog.presetsFor(occasion) }
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
                userScrollEnabled = false,
            ) {
                itemsIndexed(presets) { index, drawableId ->
                    PresetThumbnail(
                        drawableId = drawableId,
                        selected = (currentSelection as? CoverPhotoSelection.Preset)?.let {
                            it.occasion == occasion && it.index == index + 1
                        } == true,
                        onClick = {
                            onSelectionChanged(CoverPhotoSelection.Preset(occasion, index + 1))
                            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                        },
                    )
                }
            }

            // "Pick from gallery" pill
            Button(
                onClick = { launcher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
                modifier = Modifier.fillMaxWidth(),
                shape = GiftMaisonTheme.shapes.pill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GiftMaisonTheme.colors.ink,
                    contentColor = GiftMaisonTheme.colors.paper,
                ),
            ) {
                Text(stringResource(R.string.cover_photo_pick_from_gallery))
            }

            // Optional "Remove cover photo" — only shown when a selection exists
            if (currentSelection !is CoverPhotoSelection.None) {
                TextButton(
                    onClick = {
                        onSelectionChanged(CoverPhotoSelection.None)
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(
                        stringResource(R.string.cover_photo_remove),
                        color = GiftMaisonTheme.colors.inkSoft,
                    )
                }
            }
        }
    }
}
```

**Sheet structure rationale:** A 3×2 non-scrolling `LazyVerticalGrid` is the right primitive — `FlowRow` would also work but `LazyVerticalGrid` gives stable column count and matches the rest of Compose's grid idioms. With only 6 thumbnails, scrolling is unwanted (`userScrollEnabled = false`).

### Pattern 8: Refactor `RegistryDetailHero` to consume `HeroImageOrPlaceholder`

The existing `RegistryDetailHero.kt` (lines 86–129) has the inline placeholder block. Replace with:

```kotlin
// Inside the Box(modifier = modifier.fillMaxWidth().height(180.dp)) { … }
Box(modifier = Modifier.fillMaxSize()) {
    HeroImageOrPlaceholder(
        imageUrl = registry?.imageUrl,
        occasion = registry?.occasion,
        glyphSize = 40.sp,                    // hero pixel contract (matches existing 40.sp)
        modifier = Modifier.fillMaxSize(),
    )
    // 3-stop gradient overlay applied ONLY when imageUrl is non-null,
    // so the gradient placeholder isn't double-darkened.
    if (registry?.imageUrl != null) {
        val inkTop = Color(0xFF2A2420).copy(alpha = 0.27f)
        val inkBottom = Color(0xFF2A2420).copy(alpha = 0.67f)
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to inkTop, 0.4f to Color.Transparent, 1f to inkBottom)
            )
        )
    }
}
```

**Pixel contract guard (Pitfall 6 below):** the existing `RegistryDetailHero.kt` lines 94–107 apply the 3-stop dark overlay only when there's a real image. The placeholder gradient was rendered without the dark overlay — that's the existing visual. Phase 12's refactor must keep this asymmetry, otherwise the hero placeholder will visually regress.

### Anti-Patterns to Avoid

- **Don't open `InputStream` once and reuse it across `decodeStream` calls.** `BitmapFactory.decodeStream` consumes the stream; the bounds-only pass and the sampled decode each need a fresh `openInputStream(uri)`.
- **Don't run `BitmapFactory.decodeStream` on the main thread.** It can block for hundreds of ms on large images. The whole `decodeAndCompressForCover` body runs on `Dispatchers.IO`.
- **Don't pass a `Uri` directly to `putFile` after compression.** Once you've compressed in memory, use `putBytes(...)` — converting bytes back to a Uri is wasted I/O.
- **Don't `await()` `uploadTask` inside a transaction or other suspend that retries.** `await()` is one-shot. If you need progress callbacks AND coroutine semantics, use `addOnProgressListener` for progress and `await()` for completion in the same call site.
- **Don't write `imageUrl` to Firestore via the existing `Registry.toMap()` until you fix the bug.** The current `toMap()` (line 75) and `toUpdateMap()` (line 84) in `RegistryRepositoryImpl.kt` omit the `imageUrl` key. Without the fix, every save silently strips `imageUrl`. **This must be Wave 1 task #1.**
- **Don't store the `Gallery(uri)` selection across process death without a persistence strategy.** The content URI may be revoked. For Phase 12 the upload happens within the same lifecycle (during `onSave()`), so this is fine — but don't, for example, save the URI string to DataStore for "resume later" without re-requesting permissions.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Image picker UI / permission flow | Custom gallery navigator with `READ_MEDIA_IMAGES` runtime permission requests | `ActivityResultContracts.PickVisualMedia` | Photo Picker handles permissions internally; works back to API 19 via embedded fallback; future-proof against Android 14+ scoped storage tightening. |
| Bottom sheet overlay + drag handle + scrim | Custom `Popup` + manual dismissal logic | M3 `ModalBottomSheet` + `rememberModalBottomSheetState` | Solves accessibility, scrim, drag-to-dismiss, and back-press handling for free. |
| Image compression / sample-size math | Manual `Bitmap.createScaledBitmap` chain at full resolution | Two-pass `BitmapFactory` with `inJustDecodeBounds` | Single-pass decoding allocates the full original bitmap in memory first — easy OOM on 5+ MP camera images. The two-pass pattern is the documented Android approach. |
| Firebase Storage upload progress + retry | Manual chunked upload with `OkHttp` | `UploadTask` from `firebase-storage` | Exponential backoff, network change resumption, and metadata roundtrip are built in. |
| Storage path resolution / download URL | Hand-built REST URL with admin-issued tokens | `StorageReference.downloadUrl.await()` | Tokens are scoped, rotatable, and compatible with both Android Coil and the React web fallback. |
| Cross-service rule logic (Storage permissions reading Firestore docs) | Cloud Function callable that pre-validates and returns a signed upload URL | Native cross-service rules with `firestore.get()` | Saves a function deploy + roundtrip latency. Phase 1's Firestore rules already encode the same `isOwner` / `canRead` model — duplicating it in Storage rules is consistent and well-supported. |
| Drawable URI mapper for `R.drawable.*` | Custom Coil `Mapper<String, Int>` registered in a `SingletonImageLoader.Factory` | Pass `Int` directly to `AsyncImage(model = …)` | Coil 3 already supports `Int` resource IDs natively. Custom mappers are a code-shrinker hazard. |

**Key insight:** every part of this phase except the resize step has a first-class Google-supplied API. The resize step is the only place we write code that handles the actual image data — and even there, we use the documented two-pass `BitmapFactory` pattern.

## Runtime State Inventory

This is a feature-add phase, not a rename or migration. No existing runtime state is renamed or moved. However, there are still cross-system effects to enumerate:

| Category | Items Found | Action Required |
|----------|-------------|------------------|
| Stored data | Existing `registries` Firestore documents have `imageUrl: null` (Phase 10 added the field but no UI populates it). Phase 12 starts populating. **No migration needed** — `null` and absent fields both render the placeholder via `HeroImageOrPlaceholder`. | None |
| Stored data | New Firebase Storage objects at `/users/{uid}/registries/{registryId}/cover.jpg`. First-write only; no migration. | None |
| Live service config | New Firebase Storage bucket usage. The default bucket already exists (Storage is auto-provisioned with the project). Plan must verify the bucket is enabled in the Firebase Console. | Manual checkpoint: confirm Storage enabled in Firebase Console for `gift-registry-ro` |
| Live service config | Firebase Storage Rules deployment via `firebase deploy --only storage`. | Plan adds a deploy task (or human checkpoint) |
| Live service config | Cross-service rules permissions grant (Firestore ↔ Storage). The Firebase CLI / Console will prompt on first deploy. | Manual: accept the cross-service permissions prompt at deploy time |
| OS-registered state | None — purely application code. | None |
| Secrets and env vars | None — Storage uses the existing Firebase project's API key (already in `google-services.json`). No new secrets. | None |
| Build artifacts / installed packages | New `firebase-storage` dependency triggers a Gradle resolution refresh. Old build cache will rebuild on next `./gradlew :app:assembleDebug`. | None — automatic |
| Build artifacts / installed packages | 36 new JPEGs in `drawable-xxhdpi/` will increase APK size by ~4–10 MB depending on JPEG quality of source assets. | Plan should call out APK-size impact in human checkpoint |

## Common Pitfalls

### Pitfall 1: `RegistryRepositoryImpl.toMap()` and `toUpdateMap()` silently drop `imageUrl`

**What goes wrong:** the upload succeeds, the download URL roundtrips, the VM updates, the Firestore write fires — and `imageUrl` never appears on the document. The placeholder keeps rendering.

**Why it happens:** lines 75–82 (`toMap()`) and lines 84–89 (`toUpdateMap()`) in `RegistryRepositoryImpl.kt` (verified read on 2026-04-27) hardcode the fields to write. `imageUrl` is on `Registry` since Phase 10 but was never wired into either map. `RegistryDto.kt` is also missing the `imageUrl` field, so even reads will not populate it from Firestore docs that already have it.

**How to avoid:** Wave 0 of the plan must add a failing test that creates a registry with `imageUrl = "https://example.com/x.jpg"`, calls `createRegistry`, observes the resulting registry, and asserts `imageUrl` survives the roundtrip. Wave 1 fixes both `RegistryDto` (add `val imageUrl: String? = null`) and the two map functions.

**Warning signs:** unit test for `Registry.toMap()` would fail if it existed. Manually inspect the saved Firestore document — the `imageUrl` field will be missing.

### Pitfall 2: `LaunchedEffect(savedRegistryId)` fires before upload completes

**What goes wrong:** the existing `LaunchedEffect(savedRegistryId)` in `CreateRegistryScreen.kt` (line 104) routes navigation as soon as `savedRegistryId` becomes non-null. If `onSave()` writes the registry doc *before* the upload completes, navigation fires with `imageUrl = null` and the cover photo never persists on the freshly created registry.

**Why it happens:** the upload is an async I/O step that must complete before `createRegistryUseCase(...)` is called (so the URL can be passed in the registry payload). If the plan accidentally ships `viewModelScope.launch { upload(); savedRegistryId = ... }` instead of fully awaiting upload first, the race is invisible until production.

**How to avoid:** strict suspend ordering inside `onSave()`. The `Gallery(uri)` branch must compute `val downloadUrl = storageRepository.uploadCover(...).getOrThrow()` (or `getOrElse { return@launch with error }`) BEFORE constructing `registry.copy(imageUrl = downloadUrl)` and calling `createRegistryUseCase(registry)`. The `Preset` branch sets `imageUrl = PresetCatalog.encode(occasion, index)` synchronously — no upload step.

**Warning signs:** Saved registry document has `imageUrl = null`; the navigated-to detail screen shows the placeholder; subsequent edit mode loads `imageUrl = null` because Firestore was written before the upload roundtripped.

### Pitfall 3: Photo Picker URI is revoked after process death

**What goes wrong:** user picks from gallery → a content URI is staged in `coverPhotoSelection.value` → process dies → user reopens the app → URI is no longer readable; upload fails with `SecurityException`.

**Why it happens:** the Photo Picker grants a URI permission scoped to the calling process. If the process is killed and restored from saved-state, the permission is gone unless explicitly persisted via `ContentResolver.takePersistableUriPermission(...)`.

**How to avoid:** Phase 12's flow explicitly does **not** persist `Gallery(uri)` across process death. The picker → upload happens within a single screen lifecycle (during `onSave()`). If the user backgrounds the app between picking and saving, the worst case is they re-pick. Document this as accepted behaviour. **Do not** call `takePersistableUriPermission` — the URI is consumed once and forgotten.

**Warning signs:** intermittent `SecurityException: Permission Denial` from `ContentResolver.openInputStream` after a process death.

### Pitfall 4: ModalBottomSheet flicker on configuration change

**What goes wrong:** rotating the device while the sheet is open causes the sheet to dismiss-and-reopen, losing the user's preset selection state.

**Why it happens:** `rememberModalBottomSheetState` does not survive configuration change by default; the calling Compose hierarchy may also tear down and re-instantiate the sheet, dropping `currentSelection` if it lives in `remember { … }` instead of `rememberSaveable { … }`.

**How to avoid:** the picker's `coverPhotoSelection` lives in `CreateRegistryViewModel` (a `MutableStateFlow`), which survives rotation by Hilt's `viewModelScope`. The sheet's open/closed flag (`var sheetOpen by rememberSaveable { mutableStateOf(false) }`) survives via `rememberSaveable`. The selection is hoisted up to the VM, not held in the sheet itself.

**Warning signs:** UAT shows a brief flicker on rotation; preset selection visually resets.

### Pitfall 5: Cross-service Storage rules cost surprises

**What goes wrong:** every read of a cover photo (e.g., guest viewing a registry on the web fallback) triggers a Firestore read against the registry doc to evaluate the rule. For a popular registry with many viewers and many image-loads (Coil retries, browser cache misses), Firestore quota burn outpaces expectations.

**Why it happens:** cross-service rules charge each `firestore.get()` as a Firestore read.

**How to avoid:** keep the cross-service path narrow — only the `cover.jpg` match invokes Firestore. The default-deny `match /{allPaths=**}` does not. Coil 3's HTTP cache (added in 3.4.0 — see changelog) reduces repeat Storage reads from the same client. For very high traffic, a follow-up phase could move presets to a public-read Storage path so they don't invoke the rule at all.

**Warning signs:** Firestore read bill spikes after Phase 12 ships. Mitigation lives in the v1.2 phase that deploys public-read presets (Open Question 2 path b).

### Pitfall 6: Hero gradient overlay double-applied to placeholder

**What goes wrong:** during the refactor, `HeroImageOrPlaceholder` paints the placeholder gradient AND the existing `RegistryDetailHero` paints its 3-stop dark overlay on top. The result: the hero placeholder darkens visibly compared to v1.1.

**Why it happens:** the existing `RegistryDetailHero.kt` lines 94–107 only paint the dark overlay inside the `if (imageUrl != null)` branch. The naive refactor extracts the image-or-gradient into the shared composable but leaves the overlay above both — a visual regression.

**How to avoid:** keep the dark overlay's `if (registry?.imageUrl != null)` guard intact (see Pattern 8 above). Wave 0 ships a regression test that snapshots the hero placeholder pixel-for-pixel against the existing visual. Even without snapshot tooling, the existing `StyleGuidePreview.kt` Phase 11 hero preview is the manual UAT reference.

**Warning signs:** the hero placeholder looks darker after Phase 12 than before. The card placeholders (no overlay) look correct; only the hero looks off.

### Pitfall 7: Coil 3 caches the wrong model after `imageUrl` flips from URL to null

**What goes wrong:** owner uploads a cover, then taps "Remove cover photo". The placeholder should appear. Instead, the previous image lingers because Coil caches by `model` and the `Modifier`-keyed transition doesn't know about the model change.

**Why it happens:** in Coil 3.4.0, `useExistingImageAsPlaceholder` keeps a stale image visible during transitions. If the new model is `null` (placeholder branch in `HeroImageOrPlaceholder`), the composable swaps from `AsyncImage` to a `Box` — but if the call site passes a memoized `model` reference, the swap can be skipped.

**How to avoid:** use `key(imageUrl) { HeroImageOrPlaceholder(...) }` on the consumer side OR ensure the `if/else` in `HeroImageOrPlaceholder` is structural (not `painterResource`-based) so Compose tears down the `AsyncImage` node when `model` becomes null. Pattern 4 above is correctly structural.

**Warning signs:** UAT shows the old image briefly visible after Remove. Inspection in Layout Inspector reveals the `AsyncImage` node is still in the tree with `model = null`.

## Code Examples

### Hilt module: provide FirebaseStorage

```kotlin
// app/src/main/java/com/giftregistry/di/StorageModule.kt — NEW

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        val instance = FirebaseStorage.getInstance()
        if (BuildConfig.USE_FIREBASE_EMULATOR) {
            instance.useEmulator("10.0.2.2", 9199)
        }
        return instance
    }

    @Provides
    @Singleton
    fun provideStorageRepository(impl: StorageRepositoryImpl): StorageRepository = impl
}
```

### CreateRegistryViewModel: cover-photo state + onSave wiring

```kotlin
// app/src/main/java/com/giftregistry/ui/registry/create/CreateRegistryViewModel.kt — MUTATED

@HiltViewModel
class CreateRegistryViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val createRegistryUseCase: CreateRegistryUseCase,
    private val updateRegistryUseCase: UpdateRegistryUseCase,
    private val observeRegistryUseCase: ObserveRegistryUseCase,
    private val storageRepository: StorageRepository,                  // NEW
    private val coverImageProcessor: CoverImageProcessor,              // NEW — wraps decodeAndCompressForCover
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // ... existing 11 StateFlows preserved ...

    val coverPhotoSelection = MutableStateFlow<CoverPhotoSelection>(CoverPhotoSelection.None)  // NEW

    // Clear preset on occasion change (D-11)
    init {
        viewModelScope.launch {
            occasion.collect { newOccasion ->
                val current = coverPhotoSelection.value
                if (current is CoverPhotoSelection.Preset && current.occasion != newOccasion) {
                    coverPhotoSelection.value = CoverPhotoSelection.None
                }
            }
        }
    }

    fun onSave() {
        // ... existing validation preserved ...
        val ownerId = authRepository.currentUser?.uid ?: run { /* error */ return }

        viewModelScope.launch {
            isSaving.value = true; error.value = null

            // === NEW: resolve imageUrl from coverPhotoSelection ===
            val imageUrlForRegistry: String? = when (val sel = coverPhotoSelection.value) {
                CoverPhotoSelection.None -> null
                is CoverPhotoSelection.Preset -> PresetCatalog.encode(sel.occasion, sel.index)
                is CoverPhotoSelection.Gallery -> {
                    // For NEW registries we don't have a registryId yet → derive a stable temp id from
                    // currentUser + System.currentTimeMillis(); for EDIT registries use the existing id.
                    val targetRegistryId = registryId ?: "pending-${System.nanoTime()}"
                    val jpeg = runCatching { coverImageProcessor.compress(sel.uri) }
                        .getOrElse {
                            error.value = "Cover photo could not be processed: ${it.message}"
                            isSaving.value = false; return@launch
                        }
                    storageRepository.uploadCover(ownerId, targetRegistryId, jpeg)
                        .getOrElse {
                            error.value = "Cover photo upload failed: ${it.message}"
                            isSaving.value = false; return@launch
                        }
                }
            }

            val registry = Registry(
                // ... existing fields ...
                imageUrl = imageUrlForRegistry,                            // NEW
            )
            // ... existing create/update branching ...
            isSaving.value = false
        }
    }
}
```

**Note on `targetRegistryId` for new registries:** there's a known race here — uploading to `/users/{uid}/registries/pending-{nanoTime}/cover.jpg` and then writing the doc with that imageUrl creates an orphan if the create later fails. Two acceptable mitigations: (a) write the registry doc first with `imageUrl = null`, then upload to `/users/{uid}/registries/{realId}/cover.jpg`, then update the doc with the URL — two writes, one orphan-free path; (b) accept the orphan and rely on a periodic cleanup function (deferred to v1.2). Recommend (a) for v1.1 cleanliness.

### `CoverPhotoSelection` sealed interface (Wave 0 testable)

```kotlin
// app/src/main/java/com/giftregistry/ui/registry/cover/CoverPhotoSelection.kt — NEW

sealed interface CoverPhotoSelection {
    data object None : CoverPhotoSelection
    data class Preset(val occasion: String, val index: Int) : CoverPhotoSelection  // index 1..6
    data class Gallery(val uri: Uri) : CoverPhotoSelection
}
```

Wave 0 tests assert: equality semantics, default state is `None`, copy/mutation produces new instances (data class behaviour). The `Gallery(uri)` arm requires Android `Uri` — that test runs as a local unit test using `mockk { every { uri.toString() } returns "content://..." }`.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `READ_EXTERNAL_STORAGE` runtime permission + custom gallery picker | AndroidX `PickVisualMedia` Photo Picker | Android 13 (API 33) introduced; backported to API 19 via SystemUI in Photo Picker module | Zero permissions; works on every supported version (`minSdk = 23`); UI is system-supplied so users trust it. |
| Firebase KTX modules (`firebase-storage-ktx`) | Main module (`firebase-storage`) with merged Kotlin APIs | BoM v34.0.0 (July 2025) | No code changes needed (Kotlin extension functions live in the main module now). Plan must use the main artifact. |
| `android.resource://` URI scheme for drawables in Coil | Pass `Int` resource ID directly | Coil 3.0 (Oct 2024) | Faster, supports R8 resource shrinking, no custom `Mapper` needed. |
| `Glide` / `Picasso` for Compose | Coil 3 native Compose `AsyncImage` | Already migrated | Already standard in this project. |
| One-set-of-rules-per-service (Firestore-only OR Storage-only) | Cross-service rules with `firestore.get()` from Storage | Sept 2022 | Storage rules can read Firestore docs to validate ownership/visibility — eliminates the need for a Cloud Function pre-validation step. |
| `BitmapFactory.decodeStream(stream)` single-pass | Two-pass with `inJustDecodeBounds` for OOM safety | Android-canonical pattern | Memory safety on high-resolution camera images. |

**Deprecated/outdated:**
- **`firebase-storage-ktx`** — removed from BoM v34.0.0; using it pins the project to BoM ≤ 33.x.
- **`android.resource://` URIs in Coil** — works only with `ResourceUriMapper` opt-in; default unsupported in 3.x.
- **`READ_MEDIA_IMAGES` runtime permission** — still needed for *direct* `MediaStore` access, but unnecessary when using the Photo Picker.

## Open Questions

1. **Web fallback rendering of preset-backed registries.**
   - What we know: Android drawables don't exist on the web. The recommended sentinel-based encoding (`preset:wedding:3`) means the web fallback will see a `String` that doesn't parse as a URL and will need to fall back to the gradient-glyph placeholder.
   - What's unclear: whether the user wants visual parity (same image on web) or whether degradation to placeholder is acceptable.
   - Recommendation: ship Phase 12 with degradation accepted (web shows gradient placeholder for preset-backed registries). Add a **deferred** todo for v1.2: "Copy 36 PNGs to public Firebase Storage path; rewrite `PresetCatalog.encode` to emit Storage URLs instead of sentinels." This is a low-risk follow-up: changing the encoding to a URL only requires re-creating registries (which the user will do as they re-upload covers), and the Android client's `resolveImageModel` already routes URLs through Coil's HTTP path.

2. **Storage emulator wiring for development / CI.**
   - What we know: the Firebase Emulator Suite supports a Storage emulator on port 9199. The project's `firebase.json` does not currently include a `"storage"` emulator config.
   - What's unclear: whether the team wants to gate Phase 12 dev work behind emulator support (i.e., upload to emulator in `BuildConfig.USE_FIREBASE_EMULATOR`) or hit the real bucket.
   - Recommendation: add `"storage": { "port": 9199 }` to `firebase.json` emulators block. Wire `instance.useEmulator("10.0.2.2", 9199)` in the new `StorageModule` (mirrors Firestore). Real-bucket dev still possible via `-Puse_emulator=false`.

3. **Retry / backoff strategy for upload failure.**
   - What we know: `UploadTask` has built-in retry on transient network failures (exponential backoff, 6 retries) but NOT on permission denials or quota errors.
   - What's unclear: whether to surface a "Retry" button in the form on upload failure or auto-retry once.
   - Recommendation: surface a single "Retry" button via the existing `error: StateFlow<String?>` channel + a re-enabled save button. Don't auto-retry — keep behaviour predictable. Document in the plan as part of D-07.

4. **Hilt version verification.**
   - What we know: `libs.versions.toml` declares `hilt = "2.59.2"`; CLAUDE.md says 2.51.x. The Phase 02 KSP entry says "Hilt 2.59.2" so the project is genuinely on 2.59.2. CLAUDE.md is stale.
   - What's unclear: whether the planner should bump CLAUDE.md.
   - Recommendation: out of scope for Phase 12. Plan can simply use the version in `libs.versions.toml` (2.59.2). A docs-only quick task can fix CLAUDE.md.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Firebase Android BoM | Storage SDK | ✓ | 34.11.0 | — |
| Firebase Storage main module artefact | Cover photo upload | ✓ (Maven Central) | 22.0.1 (resolved by BoM) | — |
| Coil 3 | `AsyncImage` rendering | ✓ | 3.4.0 (already integrated) | — |
| Compose Material3 | `ModalBottomSheet` | ✓ | via BOM 2026.03.00 | — |
| AndroidX activity-compose `rememberLauncherForActivityResult` | Photo Picker host | ✓ (already on classpath via `activity-compose`) | bundled | — |
| Firebase CLI | Storage rules deploy | ✓ (project already deploys Functions/Firestore) | 13+ | Manual deploy from Firebase Console |
| Firestore + Storage cross-service permissions | `storage.rules` evaluating registry doc | ✓ (auto-prompted on first deploy) | n/a | If user declines: replace `firestore.get()` with a static-allow rule (less secure) — NOT recommended. |
| 36 stock-licensed JPEGs (1280×720) | Bundled presets | ✗ | — | Use placeholder filler images in Wave 1; replace with curated set in a follow-up commit before v1.1 release. CONTEXT.md explicitly accepts this. |
| Firebase Storage Emulator | Local dev | ✗ (not in `firebase.json`) | — | Wire in plan; or use real bucket via `-Puse_emulator=false`. |

**Missing dependencies with no fallback:** none (the missing JPEGs and emulator config have viable fallbacks).

**Missing dependencies with fallback:**
- 36 preset JPEGs — placeholder set acceptable for Wave 1 per D-02.
- Storage Emulator wiring — real bucket usable as fallback.

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 4.13.2 + MockK 1.13.17 + Turbine 1.2.0 + kotlinx-coroutines-test 1.9.0 |
| Config file | none — JUnit auto-discovery from `app/src/test/java/...` |
| Quick run command | `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.registry.cover.*" -x lint -x lintAnalyzeDebug` |
| Full suite command | `./gradlew :app:testDebugUnitTest -x lint -x lintAnalyzeDebug` |

### Phase Requirements → Test Map

| Decision ID | Behavior | Test Type | Automated Command | File Exists? |
|-------------|----------|-----------|-------------------|-------------|
| D-02 | `PresetCatalog.presetsFor(occasion)` returns 6 drawable IDs in stable order | unit | `./gradlew :app:testDebugUnitTest --tests "*PresetCatalogTest"` | ❌ Wave 0 |
| D-02 / D-05 | `PresetCatalog.encode("Wedding", 3)` → `"preset:Wedding:3"` and `resolve` roundtrips | unit | `./gradlew :app:testDebugUnitTest --tests "*PresetCatalogTest.encode_decode_roundtrip"` | ❌ Wave 0 |
| D-05 | `resolveImageModel(null) == null`, `resolveImageModel("preset:Wedding:3") is Int`, `resolveImageModel("https://x") is String` | unit | `./gradlew :app:testDebugUnitTest --tests "*ResolveImageModelTest"` | ❌ Wave 0 |
| D-11 | `CoverPhotoSelection.Preset` cleared when `occasion` flow emits a different value | unit | `./gradlew :app:testDebugUnitTest --tests "*CreateRegistryViewModelCoverTest.occasionChange_clearsPresetSelection"` | ❌ Wave 0 |
| D-07 / Pitfall 1 | `Registry.toMap()` includes `imageUrl` key | unit | `./gradlew :app:testDebugUnitTest --tests "*RegistryRepositoryImplTest.toMap_includesImageUrl"` | ❌ Wave 0 |
| D-07 | Successful upload sets `Registry.imageUrl` to download URL before `savedRegistryId` emits | unit | `./gradlew :app:testDebugUnitTest --tests "*CreateRegistryViewModelCoverTest.onSave_uploadsBeforeSavingRegistry"` | ❌ Wave 0 |
| D-07 | Upload failure sets `error: StateFlow<String?>` and does NOT emit `savedRegistryId` | unit | `./gradlew :app:testDebugUnitTest --tests "*CreateRegistryViewModelCoverTest.uploadFailure_surfacesError_no_navigation"` | ❌ Wave 0 |
| D-12 | Picker disabled state when `occasion.value.isBlank()` | unit (state-only, Compose-free) | `./gradlew :app:testDebugUnitTest --tests "*CoverPhotoPickerEnabledTest"` | ❌ Wave 0 |
| D-13 | Tap target on hero exposed only when `isOwner == true` | unit (state-only) | `./gradlew :app:testDebugUnitTest --tests "*RegistryDetailViewModelTest.coverTapTarget_ownerOnly"` | partial — RegistryDetailViewModelTest exists for confirm-purchase only; new `*CoverTest` file in Wave 0 |
| D-14 / D-16 | `HeroImageOrPlaceholder` chooses placeholder when `imageUrl == null`, AsyncImage when non-null (logic-only, via the resolveImageModel test) | unit | already covered by `ResolveImageModelTest` | (covered) |
| D-08 | Storage rules: owner can write, public registry readable by signed-in user, private registry readable only by invited user | manual / human checkpoint | `firebase emulators:exec --only firestore,storage 'pytest tests/storage_rules_test.py'` (note: cross-service emulation has gaps per Pitfall 5; treat as smoke test) | ❌ Plan adds a manual UAT in human checkpoint task |
| D-06 | `decodeAndCompressForCover` produces JPEG ≤ ~300 KB for a 4032×3024 input | unit (with synthetic Bitmap or test fixture) | `./gradlew :app:testDebugUnitTest --tests "*CoverImageProcessorTest"` | ❌ Wave 0 |

### Sampling Rate

- **Per task commit:** `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.registry.cover.*" --tests "com.giftregistry.data.storage.*" --tests "com.giftregistry.data.registry.*" -x lint -x lintAnalyzeDebug` (≈ < 30 s)
- **Per wave merge:** `./gradlew :app:testDebugUnitTest -x lint -x lintAnalyzeDebug` (full unit suite)
- **Phase gate:** full suite green + manual UAT (35-check checklist covering D-09/10/11/12/13/14/15 visual and behavioural requirements + locale + regression guards on Phase 10/11 hero pixel contract + emulator-vs-real-bucket smoke)

### Wave 0 Gaps

- [ ] `app/src/test/java/com/giftregistry/ui/registry/cover/PresetCatalogTest.kt` — covers D-02, D-05 sentinel encoding
- [ ] `app/src/test/java/com/giftregistry/ui/registry/cover/ResolveImageModelTest.kt` — covers `resolveImageModel` String/Int/null branching
- [ ] `app/src/test/java/com/giftregistry/ui/registry/cover/CoverPhotoSelectionTest.kt` — covers sealed interface equality + default state
- [ ] `app/src/test/java/com/giftregistry/ui/registry/cover/CoverPhotoPickerEnabledTest.kt` — covers D-12 picker-disabled-when-no-occasion logic
- [ ] `app/src/test/java/com/giftregistry/ui/registry/create/CreateRegistryViewModelCoverTest.kt` — covers D-07 upload-before-save ordering + D-11 occasion-change-clears-preset + Pitfall 2 race
- [ ] `app/src/test/java/com/giftregistry/data/registry/RegistryRepositoryImplCoverTest.kt` — covers Pitfall 1 (`toMap`/`toUpdateMap` include `imageUrl`)
- [ ] `app/src/test/java/com/giftregistry/data/storage/StorageRepositoryImplTest.kt` — covers `uploadCover` happy path + failure path with `runCatching`
- [ ] `app/src/test/java/com/giftregistry/data/storage/CoverImageProcessorTest.kt` — covers `decodeAndCompressForCover` size invariant (input dimensions in via fixture asset, output bytes ≤ ~300 KB and decodable as JPEG)
- [ ] Stub: `CoverPhotoPicker.kt`, `CoverPhotoPickerSheet.kt`, `HeroImageOrPlaceholder.kt`, `PresetCatalog.kt`, `CoverPhotoSelection.kt`, `ResolveImageModel.kt` — empty implementations that just satisfy compilation so test files reference real symbols (Phase 9 Plan 02 stub pattern)
- [ ] Stub: `StorageRepository.kt` interface in `domain/storage/` so VM tests can mock it
- [ ] Stub: `CoverImageProcessor.kt` interface so VM tests can mock the bitmap pipeline (concrete implementation arrives in Wave 1 with the actual `BitmapFactory` code)

The Wave 0 plan installs **no new dependencies** — every test relies on the already-present mockk + turbine + coroutines-test toolchain.

## Sources

### Primary (HIGH confidence)

- [Firebase Android SDK Release Notes](https://firebase.google.com/support/release-notes/android) — confirmed BoM 34.11.0 (March 2026); confirmed `firebase-storage` mapped to **22.0.1**; confirmed KTX modules removed in v34.0.0
- [Firebase Migrate to KTX in main modules](https://firebase.google.com/docs/android/kotlin-migration) — confirmed Kotlin extensions merged into main modules; `firebase-storage-ktx` deprecated
- [Firebase Cloud Storage upload files (Android)](https://firebase.google.com/docs/storage/android/upload-files) — `putFile()` / `putBytes()` / `downloadUrl` / progress / cancel API (the canonical Pattern 3 reference)
- [Firebase Storage Security Rules conditions](https://firebase.google.com/docs/storage/security/rules-conditions) — cross-service `firestore.get()` API, the 2-doc-per-evaluation cap, billing implications
- [Firebase cross-service rules announcement (blog, Sept 2022)](https://firebase.blog/posts/2022/09/announcing-cross-service-security-rules/) — cross-service rules availability + setup permissions prompt
- [Coil 3 changelog](https://coil-kt.github.io/coil/changelog/) — confirmed 3.4.0 release Feb 2026 with HTTP cache improvements
- [Coil 3 upgrade guide](https://coil-kt.github.io/coil/upgrading_to_coil3/) — documented the `android.resource://` removal + `Int` resource ID native support + `ResourceUriMapper` opt-in
- [Coil 3 image pipeline](https://coil-kt.github.io/coil/image_pipeline/) — confirmed Mapper interface and `ImageLoader.Builder.components` registration; clarified that transformations apply at decode/display time, not pre-upload
- [Android Photo Picker documentation](https://developer.android.com/training/data-storage/shared/photo-picker) — `ActivityResultContracts.PickVisualMedia` + `PickVisualMediaRequest` + `ImageOnly` filter
- [Android Loading Large Bitmaps Efficiently](https://developer.android.com/topic/performance/graphics/load-bitmap) — canonical two-pass `BitmapFactory` pattern with `inJustDecodeBounds`
- [Bitmap.CompressFormat](https://developer.android.com/reference/android/graphics/Bitmap.CompressFormat) — JPEG quality semantics
- [Compose Material3 Bottom Sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets) — `ModalBottomSheet` + `rememberModalBottomSheetState`
- [kotlinx-coroutines-play-services Task.await](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-play-services/kotlinx.coroutines.tasks/await.html) — already used throughout the project; canonical suspend bridge for Firebase Tasks
- Project source files (HIGH — direct read on 2026-04-27): `RegistryRepositoryImpl.kt`, `RegistryDto.kt`, `Registry.kt`, `RegistryDetailHero.kt`, `RegistryCard.kt`, `CreateRegistryScreen.kt`, `CreateRegistryViewModel.kt`, `OccasionCatalog.kt`, `firestore.rules`, `firebase.json`, `libs.versions.toml`, `app/build.gradle.kts`, Phase 10/11 CONTEXT.md, REQUIREMENTS.md, STATE.md

### Secondary (MEDIUM confidence)

- [UploadTask API reference](https://firebase.google.com/docs/reference/android/com/google/firebase/storage/UploadTask) — pause/resume/cancel semantics + `addOnProgressListener`
- [Coil 3 Mappers tutorial — Eclectic Engineer](https://theeclecticengineer.com/coil-image-pipeline-mapper/) — third-party but consistent with official pipeline docs
- [ProAndroidDev — Photo Picker on Compose tutorial](https://proandroiddev.com/implementing-photo-picker-on-android-kotlin-jetpack-compose-326e33e83b85) — third-party tutorial; confirms the `rememberLauncherForActivityResult(PickVisualMedia())` idiom
- [composables.com — ModalBottomSheet docs](https://composables.com/material3/modalbottomsheet) — third-party but accurate API surface for sheet state + scrim

### Tertiary (LOW confidence — flagged for validation)

- Coil 3 cache-versus-`null`-model interaction (Pitfall 7) — inferred from the 3.4.0 changelog's `useExistingImageAsPlaceholder` mention; manual UAT during Wave 2 will confirm.
- Cross-service Storage rules emulator gaps (Pitfall 5) — based on `firebase/firebase-js-sdk` issue 6803, which is a JS SDK report; behaviour on the Android emulator path may differ. Treat manual deploy-to-staging as the authoritative validation.

## Metadata

**Confidence breakdown:**
- Standard Stack: **HIGH** — every library version verified against official release notes (Firebase BoM 34.11.0, Coil 3.4.0) within April 2026.
- Architecture: **HIGH** — patterns verified against official Android + Firebase + Coil docs; existing project source confirms the established conventions (runCatching, callbackFlow, suspend tasks.await).
- Pitfalls: **HIGH** for #1 (verified by direct file read), **HIGH** for #2/#6 (logical from project structure), **MEDIUM** for #3/#4 (best-practice patterns), **MEDIUM** for #5 (cross-service rules cost is documented but exact billing depends on usage), **MEDIUM** for #7 (Coil cache behaviour inferred).
- Validation Architecture: **HIGH** — test framework already proven across Phases 1–11; new test files follow the same package-mirrors-impl pattern Phase 11 RESEARCH.md established.

**Research date:** 2026-04-27
**Valid until:** 2026-05-27 (30 days — stack is stable; recheck if Compose BOM, Firebase BoM, or Coil release between now and execution)
