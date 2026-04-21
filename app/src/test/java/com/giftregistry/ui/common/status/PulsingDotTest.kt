package com.giftregistry.ui.common.status

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * STAT-01: PulsingDot default period is 1400 ms (handoff CSS pulse keyframe).
 * Animation endpoints: alpha 1f↔0.5f, scale 1f↔0.85f.
 * RED in Wave 0 — flips GREEN when Plan 02 exposes public consts in PulsingDot.kt.
 * Plan 02 MUST declare these as top-level `const val` in PulsingDot.kt so this
 * unit test can run without the Compose test framework.
 */
class PulsingDotTest {
    @Test fun defaultPeriod_is1400ms() =
        assertEquals(1_400L, PULSING_DOT_DEFAULT_PERIOD_MS)

    @Test fun halfPeriodMs_splits1400evenly() =
        assertEquals(700L, PULSING_DOT_DEFAULT_PERIOD_MS / 2L)

    @Test fun alphaStart_is1f() =
        assertEquals(1f, PULSING_DOT_ALPHA_START, 0.0001f)

    @Test fun alphaEnd_is0_5f() =
        assertEquals(0.5f, PULSING_DOT_ALPHA_END, 0.0001f)

    @Test fun scaleStart_is1f() =
        assertEquals(1f, PULSING_DOT_SCALE_START, 0.0001f)

    @Test fun scaleEnd_is0_85f() =
        assertEquals(0.85f, PULSING_DOT_SCALE_END, 0.0001f)
}
