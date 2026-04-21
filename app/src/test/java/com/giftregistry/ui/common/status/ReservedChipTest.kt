package com.giftregistry.ui.common.status

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * STAT-01: `computeMinutesLeft(expiresAt: Long?, now: Long)` returns clamped
 * whole-minute remainder. expiresAt is epoch millis per domain Item.expiresAt.
 * RED in Wave 0 — flips GREEN when Plan 02 ships `computeMinutesLeft` in
 * `com.giftregistry.ui.common.status`.
 */
class ReservedChipTest {
    @Test fun computeMinutesLeft_null_returnsZero() =
        assertEquals(0, computeMinutesLeft(expiresAt = null, now = 0L))

    @Test fun computeMinutesLeft_expired_returnsZero() =
        assertEquals(0, computeMinutesLeft(expiresAt = 500L, now = 1_000L))

    @Test fun computeMinutesLeft_oneMinuteExact_returnsOne() =
        assertEquals(1, computeMinutesLeft(expiresAt = 60_000L, now = 0L))

    @Test fun computeMinutesLeft_twentyThreeMinutes_returns23() =
        assertEquals(23, computeMinutesLeft(expiresAt = 23L * 60_000L, now = 0L))

    @Test fun computeMinutesLeft_almostOneMinute_returnsZero() =
        assertEquals(0, computeMinutesLeft(expiresAt = 59_999L, now = 0L))

    @Test fun computeMinutesLeft_thirtyMinutesExact_returns30() =
        assertEquals(30, computeMinutesLeft(expiresAt = 30L * 60_000L, now = 0L))
}
