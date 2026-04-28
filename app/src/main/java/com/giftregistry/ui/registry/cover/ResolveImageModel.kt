package com.giftregistry.ui.registry.cover

/**
 * Phase 12 — resolves a `Registry.imageUrl` into a Coil 3 model (D-05 / D-14).
 *
 * Coil 3 routes `model: Any?` based on its runtime type:
 *   - `Int` → resource ID (preset drawable)
 *   - `String` → URL (OkHttp fetcher)
 *   - `null` → no image; consumer renders the gradient + glyph placeholder
 *
 * Branches:
 *   - `null` → `null` (placeholder branch in HeroImageOrPlaceholder)
 *   - `"preset:..."` sentinel → `Int?` from [PresetCatalog.resolve]; `null`
 *     for malformed sentinels so the UI shows the placeholder instead of
 *     leaking the raw sentinel string into Coil (which would fail to load
 *     it as a URL and render a broken-image affordance).
 *   - any other String → the String unchanged (HTTP URL, Coil routes
 *     through OkHttp).
 *
 * RESEARCH.md Pattern 4. Plan 03 will consume this from the shared
 * `HeroImageOrPlaceholder` composable.
 */
fun resolveImageModel(imageUrl: String?): Any? = when {
    imageUrl == null -> null
    imageUrl.startsWith("preset:") -> PresetCatalog.resolve(imageUrl)
    else -> imageUrl
}
