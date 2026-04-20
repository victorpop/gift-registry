---
phase: quick
plan: 260420-hcw
type: execute
wave: 1
depends_on: []
files_modified:
  - gradle/libs.versions.toml
  - app/build.gradle.kts
  - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt
  - app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt
  - app/src/main/java/com/giftregistry/ui/item/edit/EditItemScreen.kt
  - app/src/main/res/values/strings.xml
  - app/src/main/res/values-ro/strings.xml
autonomous: true
requirements:
  - QUICK-260420-hcw
must_haves:
  truths:
    - "Opening a registry with items that have imageUrl shows the product thumbnail in each ItemCard instead of a plus-icon placeholder"
    - "Items with null imageUrl (or a URL that fails to load) still render the existing plus-icon placeholder — no broken image, no crash, no layout shift"
    - "Pasting a URL in AddItemScreen and tapping Fetch populates the imageUrl field and renders a visible preview above that field"
    - "EditItemScreen shows a preview for the current imageUrl so the user sees what image will be saved"
  artifacts:
    - path: "gradle/libs.versions.toml"
      provides: "Coil 3 version + library aliases (coil-compose, coil-network-okhttp)"
      contains: "coil = \"3.4.0\""
    - path: "app/build.gradle.kts"
      provides: "Runtime dependencies on coil-compose and coil-network-okhttp"
      contains: "libs.coil.compose"
    - path: "app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt"
      provides: "AsyncImage thumbnail in ItemCard with icon fallback"
      contains: "AsyncImage"
    - path: "app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt"
      provides: "AsyncImage URL preview above the imageUrl OutlinedTextField"
      contains: "AsyncImage"
    - path: "app/src/main/java/com/giftregistry/ui/item/edit/EditItemScreen.kt"
      provides: "AsyncImage URL preview mirroring AddItemScreen"
      contains: "AsyncImage"
    - path: "app/src/main/res/values/strings.xml"
      provides: "Content description for item thumbnail image"
      contains: "item_image_content_desc"
    - path: "app/src/main/res/values-ro/strings.xml"
      provides: "Romanian content description parity"
      contains: "item_image_content_desc"
  key_links:
    - from: "app/build.gradle.kts"
      to: "io.coil-kt.coil3:coil-network-okhttp"
      via: "explicit runtime dep (Coil 3 split networking into a separate module — without this, HTTP image URLs silently fail)"
      pattern: "coil-network-okhttp"
    - from: "ItemCard in RegistryDetailScreen.kt"
      to: "item.imageUrl"
      via: "AsyncImage model parameter — null/blank falls through to error painter"
      pattern: "AsyncImage\\([\\s\\S]*item\\.imageUrl"
    - from: "AddItemScreen.kt preview"
      to: "imageUrl state"
      via: "conditional render when imageUrl.isNotBlank()"
      pattern: "imageUrl\\.isNotBlank"
---

<objective>
Wire Coil 3 image rendering into two touch points the user sees every day:

1. **Registry detail ItemCard** — the plus-icon placeholder (currently at `RegistryDetailScreen.kt:510-518` with the comment "Placeholder icon for image (Coil deferred per plan spec)") becomes a real product thumbnail loaded from `item.imageUrl`. When the URL is null or the load fails, fall back to the existing placeholder icon.
2. **AddItemScreen / EditItemScreen** — after the user pastes a URL and taps Fetch, show a preview of the image that will be saved, so they can visually confirm before tapping Save.

Purpose: The OG-fetch Cloud Function already returns the correct image URL end-to-end (user confirmed with an IKEA product), and `item.imageUrl` is persisted in Firestore — but the image is never rendered. This closes that gap and delivers the first piece of visual product identity in the registry list.

Output:
- Coil 3.4.0 wired into the Gradle version catalog and app module
- `AsyncImage` thumbnails in registry detail item list with icon fallback
- `AsyncImage` URL preview in Add and Edit item screens
- Localized content description strings (EN + RO) for accessibility
- `./gradlew :app:assembleDebug` passes
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@/Users/victorpop/ai-projects/gift-registry/CLAUDE.md
@/Users/victorpop/ai-projects/gift-registry/gradle/libs.versions.toml
@/Users/victorpop/ai-projects/gift-registry/app/build.gradle.kts
@/Users/victorpop/ai-projects/gift-registry/app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt
@/Users/victorpop/ai-projects/gift-registry/app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt
@/Users/victorpop/ai-projects/gift-registry/app/src/main/java/com/giftregistry/ui/item/edit/EditItemScreen.kt
@/Users/victorpop/ai-projects/gift-registry/app/src/main/java/com/giftregistry/domain/model/Item.kt
@/Users/victorpop/ai-projects/gift-registry/app/src/main/res/values/strings.xml
@/Users/victorpop/ai-projects/gift-registry/app/src/main/res/values-ro/strings.xml

<interfaces>
<!-- Key shapes and API surface the executor needs. No codebase exploration required. -->

Domain model (app/src/main/java/com/giftregistry/domain/model/Item.kt):
```kotlin
data class Item(
    val id: String = "",
    val registryId: String = "",
    val title: String = "",
    val originalUrl: String = "",
    val affiliateUrl: String = "",
    val imageUrl: String? = null,   // <-- NULLABLE String; may be blank after migration
    val price: String? = null,
    val notes: String? = null,
    val status: ItemStatus = ItemStatus.AVAILABLE,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val expiresAt: Long? = null,
)
```

Coil 3 API surface used (from coil3 v3.4.0):
```kotlin
// Artifact: io.coil-kt.coil3:coil-compose:3.4.0
import coil3.compose.AsyncImage

// Artifact: io.coil-kt.coil3:coil-network-okhttp:3.4.0  (REQUIRED for HTTP fetching — Coil 3 split networking out)
// No direct imports needed; its presence on the classpath registers the OkHttp network fetcher.

AsyncImage(
    model: Any?,                          // pass item.imageUrl (String? is fine)
    contentDescription: String?,
    modifier: Modifier = Modifier,
    placeholder: Painter? = null,         // painted while loading
    error: Painter? = null,               // painted on State.Error OR when model is null
    fallback: Painter? = error,           // painted when model is null (defaults to error painter)
    contentScale: ContentScale = ContentScale.Fit,
)
```

Existing placeholder pattern in ItemCard (RegistryDetailScreen.kt:510-518 — the code we are replacing):
```kotlin
// Placeholder icon for image (Coil deferred per plan spec)
Icon(
    imageVector = Icons.Default.Add,
    contentDescription = null,
    modifier = Modifier
        .size(48.dp)
        .padding(end = 8.dp),
    tint = MaterialTheme.colorScheme.onSurfaceVariant
)
```

State flow pattern shared by AddItemScreen and EditItemScreen:
```kotlin
val imageUrl by viewModel.imageUrl.collectAsStateWithLifecycle()  // StateFlow<String>
// OutlinedTextField bound to imageUrl already exists in both screens.
// Add preview ABOVE the OutlinedTextField, conditional on imageUrl.isNotBlank().
```
</interfaces>
</context>

<tasks>

<task type="auto">
  <name>Task 1: Add Coil 3 dependency to version catalog and app module</name>
  <files>gradle/libs.versions.toml, app/build.gradle.kts</files>
  <action>
Add Coil 3.4.0 to the project. CLAUDE.md locks version 3.4.0 and package `coil3` (NOT coil 2).

1. Edit `gradle/libs.versions.toml`:

   Under `[versions]` (after `googleServices = "4.4.2"`, keeping alphabetic grouping is not required — existing file does not alphabetize):
   ```
   coil = "3.4.0"
   ```

   Under `[libraries]` (after `turbine = ...` on line 38):
   ```
   coil-compose = { group = "io.coil-kt.coil3", name = "coil-compose", version.ref = "coil" }
   coil-network-okhttp = { group = "io.coil-kt.coil3", name = "coil-network-okhttp", version.ref = "coil" }
   ```

2. Edit `app/build.gradle.kts` — add Coil deps in the `dependencies { ... }` block. Place them in their own section with a comment, after the `// AppCompat (for locale switching)` block and before `// Test` (i.e., insert between lines 93 and 95):
   ```
       // Coil 3 (image loading for Compose)
       // coil-network-okhttp is REQUIRED — Coil 3 split networking into a separate module.
       // Without it, HTTP image URLs silently fail to load.
       implementation(libs.coil.compose)
       implementation(libs.coil.network.okhttp)
   ```

Do NOT add any other Coil modules (coil-svg, coil-gif, coil-video) — not needed for JPEG/PNG/WebP product images from retailers.

Do NOT use `coil-kt:coil-compose` (that is Coil 2). The group id MUST be `io.coil-kt.coil3` and the Compose composable we will import is `coil3.compose.AsyncImage`.
  </action>
  <verify>
    <automated>cd /Users/victorpop/ai-projects/gift-registry && ./gradlew :app:dependencies --configuration debugRuntimeClasspath 2>&1 | grep -E "coil3" | head -5</automated>
  </verify>
  <done>
- `gradle/libs.versions.toml` has `coil = "3.4.0"` under `[versions]` and both `coil-compose` + `coil-network-okhttp` aliases under `[libraries]`
- `app/build.gradle.kts` has `implementation(libs.coil.compose)` and `implementation(libs.coil.network.okhttp)` in its dependencies block
- Gradle sync succeeds; `./gradlew :app:dependencies` lists `io.coil-kt.coil3:coil-compose:3.4.0` and `io.coil-kt.coil3:coil-network-okhttp:3.4.0` in the debug runtime classpath
  </done>
</task>

<task type="auto">
  <name>Task 2: Render thumbnails in ItemCard and URL previews in Add/Edit screens</name>
  <files>app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt, app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt, app/src/main/java/com/giftregistry/ui/item/edit/EditItemScreen.kt, app/src/main/res/values/strings.xml, app/src/main/res/values-ro/strings.xml</files>
  <action>
Wire `AsyncImage` into three Composables and add the content description string.

**A. Add content description strings.**

Edit `app/src/main/res/values/strings.xml` — add near other `item_*` keys (after line 109 which has `item_image_label`):
```xml
<string name="item_image_content_desc">Product image</string>
```

Edit `app/src/main/res/values-ro/strings.xml` — parity entry near line 109:
```xml
<string name="item_image_content_desc">Imagine produs</string>
```

**B. ItemCard thumbnail (RegistryDetailScreen.kt lines 510-518).**

Add imports at the top of the file (merge with existing import blocks — keep alphabetical where the existing file does, otherwise append):
```kotlin
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
```

Replace the entire placeholder `Icon(...)` block at lines 510-518 (including the `// Placeholder icon for image (Coil deferred per plan spec)` comment) with:
```kotlin
// Product thumbnail. Falls back to the plus-icon placeholder when imageUrl is null or the load fails.
val placeholderPainter = rememberVectorPainter(Icons.Default.Add)
AsyncImage(
    model = item.imageUrl,
    contentDescription = stringResource(R.string.item_image_content_desc),
    modifier = Modifier
        .size(48.dp)
        .clip(RoundedCornerShape(4.dp))
        .padding(end = 0.dp), // size-then-clip; horizontal gap handled by Spacer below
    contentScale = ContentScale.Crop,
    placeholder = placeholderPainter,
    error = placeholderPainter,
    fallback = placeholderPainter,
)
Spacer(modifier = Modifier.size(8.dp))
```

Layout notes:
- Original placeholder had `.size(48.dp).padding(end = 8.dp)` — the padding reserved horizontal gap from the Column on its right. We keep the same 48dp footprint so the layout does not shift, but use a `Spacer(8.dp)` for the gap because `.clip()` must hug the image (padding after clip would leave clipped-but-padded whitespace inside the rounded rect — visually the image would not fill its box).
- `ContentScale.Crop` matches product-tile convention: fills the 48dp square without distortion, crops excess.
- Same painter used for `placeholder` (during load), `error` (on failure), and `fallback` (when model is null) — one visual state for all non-success cases. The Icons.Default.Add painter is tinted by Coil's default rendering; if the tint looks off, acceptable — tint parity is not a goal, presence-of-icon-on-failure is.

Verify `androidx.compose.ui.res.stringResource` is already imported in RegistryDetailScreen.kt (it should be — other strings are read via `stringResource` in the file). If not, add it.

**C. AddItemScreen URL preview (AddItemScreen.kt).**

Add imports:
```kotlin
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
```

Insert a preview BLOCK immediately **before** the `OutlinedTextField` at line 150 (the one bound to `imageUrl`). It must be conditional on `imageUrl.isNotBlank()`:
```kotlin
if (imageUrl.isNotBlank()) {
    val previewFallback = rememberVectorPainter(Icons.Default.Image)
    AsyncImage(
        model = imageUrl,
        contentDescription = stringResource(R.string.item_image_content_desc),
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(8.dp)),
        contentScale = ContentScale.Fit,
        placeholder = previewFallback,
        error = previewFallback,
        fallback = previewFallback,
    )
}
```

Placement rationale: ABOVE the imageUrl text field so the preview reads as "this is what will be saved for this URL field." 120dp tall + `ContentScale.Fit` keeps tall product shots uncropped (e.g., a full-body portrait of furniture).

**D. EditItemScreen URL preview (EditItemScreen.kt).**

Mirror the AddItemScreen change exactly. Same imports, same preview block inserted immediately before the `OutlinedTextField` bound to `imageUrl` at line 162.

Layout parity with AddItemScreen is required — the user flows between these screens and inconsistency would be jarring.

**Nothing else changes.** Do not touch ViewModels, DTOs, Item.kt, network_security_config.xml, web i18n files. Do not add SubcomposeAsyncImage — the simpler `AsyncImage` with painter fallbacks is sufficient per constraints.
  </action>
  <verify>
    <automated>cd /Users/victorpop/ai-projects/gift-registry && ./gradlew :app:assembleDebug 2>&1 | tail -30</automated>
  </verify>
  <done>
- `./gradlew :app:assembleDebug` succeeds (BUILD SUCCESSFUL)
- `RegistryDetailScreen.kt` no longer contains the string `"Coil deferred per plan spec"`
- `RegistryDetailScreen.kt`, `AddItemScreen.kt`, `EditItemScreen.kt` each import `coil3.compose.AsyncImage`
- `AddItemScreen.kt` and `EditItemScreen.kt` each have an `if (imageUrl.isNotBlank())` block wrapping an `AsyncImage` placed before the imageUrl `OutlinedTextField`
- `item_image_content_desc` present in both `values/strings.xml` and `values-ro/strings.xml`
- No references to Coil 2 package `coil.compose` (singular `coil`) anywhere — must be `coil3.compose`
  </done>
</task>

</tasks>

<verification>
Build gate: `./gradlew :app:assembleDebug` passes.

Behavioral gates (smoke check via the running app, not required for plan completion but informs done-ness):
- Open a registry that has at least one item with an imageUrl (e.g., the IKEA item referenced in the problem statement) — verify the thumbnail replaces the plus-icon placeholder.
- Open a registry with an item whose imageUrl is null or points to a 404 — verify the plus-icon placeholder still renders (no broken-image glyph, no crash).
- In AddItemScreen, paste a product URL and tap Fetch — verify a preview appears above the imageUrl text field once the OG fetch populates `imageUrl`.
- In EditItemScreen, open an item with a saved imageUrl — verify the preview renders immediately with the saved URL.

No unit tests required — the codebase has no existing tests for ItemCard or these screens, and the change is visual. Skip per constraint.
</verification>

<success_criteria>
- `./gradlew :app:assembleDebug` succeeds
- Coil 3.4.0 (`io.coil-kt.coil3:coil-compose` + `io.coil-kt.coil3:coil-network-okhttp`) on the app's debug runtime classpath
- ItemCard in registry detail renders `AsyncImage(item.imageUrl)` with icon fallback, 48dp square, `RoundedCornerShape(4.dp)` clip, `ContentScale.Crop`
- AddItemScreen and EditItemScreen each render a conditional preview above the imageUrl text field: `fillMaxWidth()`, 120dp tall, `RoundedCornerShape(8.dp)` clip, `ContentScale.Fit`
- Fallback strategy: single painter (Icons.Default.Add for ItemCard, Icons.Default.Image for Add/Edit preview) used for placeholder + error + fallback — matches constraint decision to use `AsyncImage` with `error`/`placeholder`/`fallback` painters (not SubcomposeAsyncImage)
- Content description localized in EN and RO
- No changes to ViewModels, DTOs, domain models, network security config, or web i18n files
</success_criteria>

<output>
After completion, create `.planning/quick/260420-hcw-wire-coil-image-rendering-preview-in-add/260420-hcw-SUMMARY.md`.
</output>
