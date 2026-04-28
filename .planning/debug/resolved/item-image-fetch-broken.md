---
status: awaiting_human_verify
trigger: "item-image-fetch-broken — image not displayed after URL fetch; no manual upload option"
created: 2026-04-28T00:00:00Z
updated: 2026-04-28T00:02:00Z
---

## Current Focus
<!-- OVERWRITE on each update - reflects NOW -->

hypothesis: H1-CONFIRMED and fixed. h2 deferred (future phase).
test: TypeScript build passes (npm run build — no errors). Kotlin edits verified by read (no new imports needed, correct types).
expecting: Re-fetch of mobexpert.ro URL now returns https:// image URL; Coil loads image successfully.
next_action: AWAITING_HUMAN_VERIFY — user to re-test URL fetch with same mobexpert.ro URL.

## Symptoms
<!-- Written during gathering, then IMMUTABLE -->

expected: After URL fetch, product photo loads in Edit Item screen. If fetch fails or user wants override, manual image pick (gallery/camera) is available.
actual: Image URL field populated with http://mobexpert.ro/... (http not https). Image preview shows broken-image placeholder. No manual upload UI visible.
errors: None reported. Silent Coil load failure on cleartext HTTP. Missing upload UI is a feature gap.
reproduction: 1. Add/Edit Item screen. 2. Paste mobexpert.ro URL. 3. Tap Fetch. 4. Name+price populated, Image URL http://, preview broken. 5. No manual upload control found.
started: 2026-04-28 during testing of URL import flow

## Eliminated
<!-- APPEND only - prevents re-investigating -->

- hypothesis: Coil misconfiguration (wrong model type, missing OkHttp client)
  evidence: AsyncImage is called with a plain String imageUrl — standard Coil 3 usage. No Coil config touched in the item screens. The issue is the URL scheme, not Coil setup.
  timestamp: 2026-04-28

- hypothesis: CloudFunction returns null imageUrl (function error)
  evidence: User reported Image URL field was populated with http://mobexpert.ro/... — function returned a value. The image URL field in EditItemScreen IS populated (line 59, viewModel.imageUrl). The problem is the scheme.
  timestamp: 2026-04-28

- hypothesis: Missing upload UI is a regression (was present, now removed)
  evidence: Grep found PickVisualMedia only in CoverPhotoPickerSheet.kt (registry covers). No commented-out item image picker code anywhere. Feature gap, not regression.
  timestamp: 2026-04-28

## Evidence
<!-- APPEND only - facts discovered -->

- timestamp: 2026-04-28
  checked: functions/src/registry/fetchOgMetadata.ts line 282
  found: imageUrl returned as `og("image")` — verbatim value of the og:image meta tag. No scheme normalization. Mobexpert's og:image starts with http://.
  implication: Cloud Function hands back whatever scheme the retailer uses. If the page says http://, the client gets http://.

- timestamp: 2026-04-28
  checked: app/src/main/AndroidManifest.xml
  found: No `android:usesCleartextTraffic` attribute. No `android:networkSecurityConfig` attribute on <application> in the main manifest.
  implication: Android platform default applies — cleartext HTTP blocked for targetSdk >= 28.

- timestamp: 2026-04-28
  checked: app/build.gradle.kts
  found: targetSdk = 36, minSdk = 23
  implication: targetSdk 36 >> 28. Cleartext HTTP traffic is blocked by the platform for all http:// image URLs. Coil silently falls back to the error painter.

- timestamp: 2026-04-28
  checked: app/src/debug/res/xml/network_security_config.xml (referenced from debug AndroidManifest.xml)
  found: cleartextTrafficPermitted only for domain 10.0.2.2 (emulator localhost alias). No wildcard or retailer domain exception.
  implication: Even in debug builds, http://mobexpert.ro is blocked. No production network_security_config.xml exists at all (only a debug one).

- timestamp: 2026-04-28
  checked: ItemRepositoryImpl.kt fetchOgMetadata (lines 44-58)
  found: data["imageUrl"] as? String — no normalization applied on the Android client side before storing in OgMetadata.imageUrl.
  implication: The http:// URL flows straight from Cloud Function response to viewModel.imageUrl to AsyncImage.

- timestamp: 2026-04-28
  checked: EditItemScreen.kt (lines 168-181), AddItemScreen.kt ItemPreviewCard.kt
  found: AsyncImage receives imageUrl string as-is. error painter = Icons.Default.Image (the broken placeholder the user sees). No scheme correction at render time.
  implication: The broken-image icon IS the Coil error fallback, not a custom placeholder.

- timestamp: 2026-04-28
  checked: grep for PickVisualMedia, ActivityResultContracts.GetContent, rememberLauncherForActivityResult, camera, gallery across all app/src/main/java/**/*.kt
  found: Only match in CoverPhotoPickerSheet.kt — scoped to registry cover photo flow. Zero matches in EditItemScreen, AddItemScreen, or any item-related file.
  implication: No image upload UI exists for items. Feature gap confirmed.

## Resolution
<!-- OVERWRITE as understanding evolves -->

root_cause:
  SUB-ISSUE 1 (broken image preview):
    The fetchOgMetadata Cloud Function returns og:image verbatim (no scheme normalization).
    Mobexpert's og:image is served as http://. Android targetSdk=36 blocks cleartext HTTP by
    default (no usesCleartextTraffic, no network_security_config that whitelists external domains).
    Coil receives an http:// URL it cannot load and silently renders the error painter
    (Icons.Default.Image), which the user sees as a broken-image icon.

  SUB-ISSUE 2 (no manual image upload):
    No image picker (PickVisualMedia / gallery launcher) exists anywhere in the item-add or
    item-edit flow. The only gallery picker in the codebase is CoverPhotoPickerSheet.kt for
    registry covers. This is a feature gap — the capability was never implemented for items.

fix:
  FIX 1 — Normalize image URL scheme in Cloud Function (functions/src/registry/fetchOgMetadata.ts):
    After resolving imageUrl from og("image"), apply:
      function normalizeImageUrl(raw: string | null): string | null {
        if (!raw) return null;
        if (raw.startsWith("http://")) return "https://" + raw.slice(7);
        return raw;
      }
    Change: imageUrl: og("image") → imageUrl: normalizeImageUrl(og("image"))

  FIX 1b — Belt-and-suspenders: also normalize in Android client (ItemRepositoryImpl.kt):
    In fetchOgMetadata, before assigning imageUrl:
      imageUrl = (data["imageUrl"] as? String)?.let { if (it.startsWith("http://")) "https://" + it.drop(7) else it }

  FIX 2 — Add image picker to Edit Item screen (EditItemScreen.kt + EditItemViewModel.kt):
    Scope: EditItemScreen only (the screen the user was on). AddItemScreen already has a
    "Manual" tab with an imageUrl text field.
    Add a "Pick from gallery" button below the Image URL text field in EditItemScreen.
    Wire via rememberLauncherForActivityResult(PickVisualMedia()) — same pattern as CoverPhotoPickerSheet.
    On URI pick, update viewModel.imageUrl.value with the content:// URI string so Coil can load it locally.
    Note: local URIs don't persist across app installs — if user saves, imageUrl will be a content:// URI
    that won't survive being loaded on another device. Proper fix would upload to Firebase Storage.
    For now, the image URL text field already accepts a custom URL — the minimal fix is just the
    gallery picker that writes a local URI into imageUrl for preview + save purposes.

verification: TypeScript build clean (npm run build). Kotlin edits reviewed — correct types, no new imports needed. Runtime verification pending user re-test.
files_changed:
  - functions/src/registry/fetchOgMetadata.ts (added normalizeImageUrl helper, applied at imageUrl return site)
  - app/src/main/java/com/giftregistry/data/registry/ItemRepositoryImpl.kt (client-side http→https normalization in fetchOgMetadata)
