package com.giftregistry.ui.registry.detail

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * SCR-08: Pinned-toolbar alpha fade tied to LazyListState scroll.
 *
 * Contract (pure Kotlin — density conversion of 120.dp is done at the call site
 * in Compose and passed in as `heroThresholdPx: Float`):
 *
 *   if (firstVisibleItemIndex >= 1) return 1f                          // PITFALL 1 GUARD
 *   else (firstVisibleItemScrollOffsetPx / heroThresholdPx).coerceIn(0f, 1f)
 *
 * RESEARCH.md Pitfall 1: "Hero item (index 0) scrolls fully off screen →
 * firstVisibleItemIndex becomes 1 → firstVisibleItemScrollOffset resets to 0 →
 * toolbar alpha snaps back to 0 (transparent) even though list is scrolled down."
 * Without the `>= 1 → 1f` short-circuit the toolbar flashes transparent. This
 * test pins the guard so the pitfall cannot regress silently.
 *
 * RED in Wave 0 — flips GREEN when Plan 02 ships HeroToolbarAlpha.kt with
 * top-level `heroToolbarAlpha(firstVisibleItemIndex, firstVisibleItemScrollOffsetPx, heroThresholdPx)`
 * in com.giftregistry.ui.registry.detail.
 */
class HeroToolbarAlphaTest {
    @Test fun offsetZero_returnsZero() =
        assertEquals(0f, heroToolbarAlpha(firstVisibleItemIndex = 0, firstVisibleItemScrollOffsetPx = 0, heroThresholdPx = 120f), 0.001f)

    @Test fun halfThreshold_returnsHalf() =
        assertEquals(0.5f, heroToolbarAlpha(0, 60, 120f), 0.001f)

    @Test fun atThreshold_returnsOne() =
        assertEquals(1f, heroToolbarAlpha(0, 120, 120f), 0.001f)

    @Test fun aboveThreshold_clampsToOne() =
        assertEquals(1f, heroToolbarAlpha(0, 500, 120f), 0.001f)

    @Test fun secondItemVisible_returnsOne_evenWithSmallOffset() =
        assertEquals(
            "Pitfall 1 guard: firstVisibleItemIndex >= 1 must short-circuit to 1f " +
                "regardless of the stale firstVisibleItemScrollOffset value",
            1f,
            heroToolbarAlpha(firstVisibleItemIndex = 1, firstVisibleItemScrollOffsetPx = 0, heroThresholdPx = 120f),
            0.001f,
        )

    @Test fun thirdItemVisible_returnsOne() =
        assertEquals(1f, heroToolbarAlpha(5, 300, 120f), 0.001f)

    @Test fun negativeOffset_clampsToZero() =
        assertEquals(0f, heroToolbarAlpha(0, -10, 120f), 0.001f)
}
