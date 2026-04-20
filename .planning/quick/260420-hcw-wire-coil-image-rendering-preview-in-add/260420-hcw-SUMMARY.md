---
phase: quick
plan: 260420-hcw
subsystem: ui

tags: [coil, coil3, async-image, image-loading, compose, item-rendering, url-preview]

requires:
  - phase: 03-registry-item-management
    provides: fetchOgMetadata persists imageUrl into Firestore Item docs (the data this plan finally renders)

provides:
  - Coil 3.4.0 wired into version catalog (coil-compose + coil-network-okhttp)
  - AsyncImage thumbnail in ItemCard with icon fallback (null/blank/error all fall through to Icons.Default.Add painter)
  - AsyncImage URL preview above the imageUrl field in AddItemScreen and EditItemScreen, 120dp tall with ContentScale.Fit
  - item_image_content_desc localized string (EN + RO)

affects: [future product-tile UI, browse-stores item cards, any screen rendering Item.imageUrl]

tech-stack:
  added:
    - io.coil-kt.coil3:coil-compose:3.4.0
    - io.coil-kt.coil3:coil-network-okhttp:3.4.0
  patterns:
    - "AsyncImage with single painter for placeholder/error/fallback — one visual state for all non-success cases (simpler than SubcomposeAsyncImage, sufficient for current needs)"
    - "coil3 package (NOT coil) import discipline — Coil 3 lives under io.coil-kt.coil3 with coil3.* imports"
    - "coil-network-okhttp mandatory companion — Coil 3 split networking into its own module; without it HTTP image URLs silently fail to load"

key-files:
  created: []
  modified:
    - gradle/libs.versions.toml
    - app/build.gradle.kts
    - app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt
    - app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt
    - app/src/main/java/com/giftregistry/ui/item/edit/EditItemScreen.kt
    - app/src/main/res/values/strings.xml
    - app/src/main/res/values-ro/strings.xml

key-decisions:
  - "Single painter for placeholder/error/fallback in AsyncImage — null imageUrl, loading, and load-failure all render the same icon, matching the plan's decision to keep it simple"
  - "Spacer(8.dp) after clipped thumbnail instead of .padding(end = 8.dp) — padding after clip would leave clipped-but-padded whitespace inside the rounded rect; Spacer keeps the 48dp image fully visible"
  - "Dropped Icons.Default.Image from RegistryDetailScreen imports — ItemCard body only uses Icons.Default.Add, so the Image import from the plan's import list was unused and would become a lint warning"

patterns-established:
  - "AsyncImage(model = maybeNullString, placeholder/error/fallback = samePainter) — idiomatic for optional-URL product imagery with guaranteed visual continuity"
  - "Conditional AsyncImage preview gated by imageUrl.isNotBlank() placed immediately above the corresponding OutlinedTextField — reads as 'here is what will be saved for this URL field'"

requirements-completed: [QUICK-260420-hcw]

duration: 3m 20s
completed: 2026-04-20
---

# Quick 260420-hcw: Wire Coil Image Rendering Summary

**Coil 3.4.0 wired end-to-end — ItemCard now renders product thumbnails from item.imageUrl, and Add/Edit item screens show a 120dp preview above the imageUrl field when a URL is present.**

## Performance

- **Duration:** 3m 20s
- **Started:** 2026-04-20T09:33:11Z
- **Completed:** 2026-04-20T09:36:31Z
- **Tasks:** 2
- **Files modified:** 7

## Accomplishments

- Coil 3.4.0 on the app's debug runtime classpath (both coil-compose and coil-network-okhttp verified via `./gradlew :app:dependencies`)
- Registry detail item list renders real product thumbnails — the "Coil deferred per plan spec" placeholder comment is gone from the codebase
- AddItemScreen: pasting a URL + tapping Fetch (which populates `imageUrl` via the OG fetch Cloud Function) now shows a live preview above the imageUrl text field
- EditItemScreen: preview renders immediately on open for any item with a saved imageUrl, giving the user visual confirmation of what will be saved
- Accessibility: content description strings added and localized (EN "Product image" / RO "Imagine produs")
- `./gradlew :app:assembleDebug` passes cleanly

## Task Commits

Each task was committed atomically:

1. **Task 1: Add Coil 3 dependency to version catalog and app module** — `76cc25c` (chore)
2. **Task 2: Render thumbnails in ItemCard and URL previews in Add/Edit screens** — `a7be3aa` (feat)

## Files Created/Modified

- `gradle/libs.versions.toml` — adds `coil = "3.4.0"` version + `coil-compose` and `coil-network-okhttp` library aliases
- `app/build.gradle.kts` — runtime dependencies on `libs.coil.compose` and `libs.coil.network.okhttp` with an inline comment explaining why the okhttp module is mandatory
- `app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt` — ItemCard placeholder Icon replaced with `AsyncImage(model = item.imageUrl)` at 48dp, `RoundedCornerShape(4.dp)` clip, `ContentScale.Crop`, single `Icons.Default.Add` painter for placeholder/error/fallback; 8dp Spacer added for gap to Column
- `app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt` — conditional `AsyncImage` preview block (`if (imageUrl.isNotBlank())`) inserted immediately before the imageUrl `OutlinedTextField`; 120dp tall with `ContentScale.Fit` and `Icons.Default.Image` fallback
- `app/src/main/java/com/giftregistry/ui/item/edit/EditItemScreen.kt` — same preview block as AddItemScreen (layout parity)
- `app/src/main/res/values/strings.xml` — `<string name="item_image_content_desc">Product image</string>`
- `app/src/main/res/values-ro/strings.xml` — `<string name="item_image_content_desc">Imagine produs</string>`

## Decisions Made

- **Single painter covers all non-success AsyncImage states.** Per plan constraint, we used one `rememberVectorPainter(Icons.Default.Add)` instance for `placeholder`, `error`, and `fallback` on the ItemCard — there's no need for loading-vs-error visual differentiation at the 48dp thumbnail size, and it keeps the code terse.
- **Spacer instead of padding after clip.** Plan prose noted this: `Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)).padding(end = 8.dp)` would clip the image to a 48dp rounded square, then add padding *inside* its 48dp box (shrinking the visible image). We use `.size(48.dp).clip(...)` on the AsyncImage and a separate `Spacer(8.dp)` for the horizontal gap — the thumbnail fills its 48dp box and the gap is preserved.
- **Dropped `Icons.Default.Image` import from RegistryDetailScreen.** The plan's import list for that file included `Icons.Default.Image`, but the ItemCard body only uses `Icons.Default.Add` as its fallback painter. Keeping the unused import would have produced a compiler/lint warning. Add/Edit screens still import `Icons.Default.Image` because they genuinely use it as the 120dp preview fallback.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Dropped unused `Icons.Default.Image` import from RegistryDetailScreen.kt**
- **Found during:** Task 2 (B — ItemCard thumbnail)
- **Issue:** Plan's import list for RegistryDetailScreen included `androidx.compose.material.icons.filled.Image`, but the ItemCard body only references `Icons.Default.Add` as its fallback painter. An unused import would trigger a lint warning and may be flagged by ktlint in CI.
- **Fix:** Removed the `Icons.Default.Image` import from RegistryDetailScreen.kt; kept it in AddItemScreen.kt and EditItemScreen.kt where it is actually used (120dp preview fallback).
- **Files modified:** `app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt`
- **Verification:** `./gradlew :app:assembleDebug` BUILD SUCCESSFUL
- **Committed in:** `a7be3aa` (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 blocking — import hygiene)
**Impact on plan:** Minor cleanup to match actual code usage. No scope change.

## Issues Encountered

None. The initial `./gradlew :app:assembleDebug` run surfaced an unrelated Gradle daemon stack trace in the log (not from our changes — tasks continued past it), but the build reported `BUILD SUCCESSFUL` and a re-run produced a clean output. No action required.

## User Setup Required

None — Coil 3 runs entirely inside the app; no external service configuration.

## Next Phase Readiness

- Every future screen that renders `Item.imageUrl` can now `import coil3.compose.AsyncImage` with zero additional setup — the Coil OkHttp network fetcher is registered on the classpath via `coil-network-okhttp`.
- Pattern reference for future product-tile UIs (browse stores, wishlist grids, etc.): use `AsyncImage(model = string?, placeholder = painter, error = painter, fallback = painter)` with `ContentScale.Crop` for square tiles and `ContentScale.Fit` for previews where aspect ratio matters.
- No blockers introduced.

## Self-Check: PASSED

**Files verified exist on disk:**
- FOUND: gradle/libs.versions.toml
- FOUND: app/build.gradle.kts
- FOUND: app/src/main/java/com/giftregistry/ui/registry/detail/RegistryDetailScreen.kt
- FOUND: app/src/main/java/com/giftregistry/ui/item/add/AddItemScreen.kt
- FOUND: app/src/main/java/com/giftregistry/ui/item/edit/EditItemScreen.kt
- FOUND: app/src/main/res/values/strings.xml
- FOUND: app/src/main/res/values-ro/strings.xml

**Commits verified in git log:**
- FOUND: 76cc25c (Task 1 — Coil dependency)
- FOUND: a7be3aa (Task 2 — AsyncImage wiring)

**Runtime classpath verified:**
- FOUND: io.coil-kt.coil3:coil-compose:3.4.0 in debugRuntimeClasspath
- FOUND: io.coil-kt.coil3:coil-network-okhttp:3.4.0 in debugRuntimeClasspath

**Build verified:**
- `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL

---
*Phase: quick*
*Completed: 2026-04-20*
