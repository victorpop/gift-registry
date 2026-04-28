package com.giftregistry.ui.registry.cover

/**
 * Phase 12 — Wave 0 STUB.
 *
 * Resolves a `Registry.imageUrl` value to a Coil 3 model:
 *   - `null` -> null (placeholder branch in HeroImageOrPlaceholder)
 *   - `"preset:..."` sentinel -> Int? (`R.drawable.*` from PresetCatalog.resolve)
 *   - any other String -> the String unchanged (HTTP URL, Coil routes through OkHttp)
 *
 * STUB returns null for everything so ResolveImageModelTest fails RED on
 * the URL passthrough + sentinel-to-Int branches. Plan 03 ships the real
 * three-branch `when` per RESEARCH.md Pattern 4.
 */
fun resolveImageModel(imageUrl: String?): Any? = null
