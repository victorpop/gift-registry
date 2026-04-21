package com.giftregistry.ui.registry.detail

/**
 * SCR-08: Pinned-toolbar alpha fade derivation.
 *
 * Contract (pure Kotlin — Compose call site converts `120.dp` to px via
 * `with(LocalDensity.current) { 120.dp.toPx() }` outside `derivedStateOf`,
 * per RESEARCH.md Pitfall 2):
 *
 *   if (firstVisibleItemIndex >= 1) 1f                               // PITFALL 1 GUARD
 *   else (firstVisibleItemScrollOffsetPx / heroThresholdPx).coerceIn(0f, 1f)
 *
 * The guard short-circuits to 1f once the hero (index 0) has scrolled off the
 * screen — `LazyListState.firstVisibleItemScrollOffset` resets to 0 when the
 * first visible item changes, which would otherwise flash the toolbar back to
 * transparent (RESEARCH.md Pitfall 1).
 *
 * Unit-tested by HeroToolbarAlphaTest (Wave 0).
 */
fun heroToolbarAlpha(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffsetPx: Int,
    heroThresholdPx: Float,
): Float {
    if (firstVisibleItemIndex >= 1) return 1f
    return (firstVisibleItemScrollOffsetPx.toFloat() / heroThresholdPx).coerceIn(0f, 1f)
}
