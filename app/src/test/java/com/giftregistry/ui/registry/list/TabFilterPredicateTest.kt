package com.giftregistry.ui.registry.list

import com.giftregistry.domain.model.Registry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * SCR-07: Active / Past tab predicates over Registry.eventDateMs.
 *
 *   Active = eventDateMs == null || eventDateMs >= todayMs
 *   Past   = eventDateMs != null && eventDateMs < todayMs
 *
 * startOfTodayMs uses Calendar for minSdk 23 compat (LocalDate.atStartOfDay()
 * would require API 26). See 10-RESEARCH.md Pitfall 7.
 *
 * RED in Wave 0 — flips GREEN when Plan 02 ships TabFilters.kt with top-level
 * startOfTodayMs / Registry.isActive / Registry.isPast in
 * com.giftregistry.ui.registry.list.
 */
class TabFilterPredicateTest {
    @Test fun startOfTodayMs_normalizesToMidnight() {
        val now = 1_745_500_000_000L
        val expected = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        assertEquals(expected, startOfTodayMs(now))
    }

    @Test fun isActive_nullDate_isActive() =
        assertTrue(Registry(eventDateMs = null).isActive(todayMs = 1_000L))

    @Test fun isActive_futureDate_isActive() =
        assertTrue(Registry(eventDateMs = 10_000L).isActive(todayMs = 5_000L))

    @Test fun isActive_todayBoundary_isActive() =
        assertTrue(Registry(eventDateMs = 5_000L).isActive(todayMs = 5_000L))

    @Test fun isActive_pastDate_isNotActive() =
        assertFalse(Registry(eventDateMs = 1_000L).isActive(todayMs = 5_000L))

    @Test fun isPast_pastDate_isPast() =
        assertTrue(Registry(eventDateMs = 1_000L).isPast(todayMs = 5_000L))

    @Test fun isPast_nullDate_isNotPast() =
        assertFalse(Registry(eventDateMs = null).isPast(todayMs = 5_000L))

    @Test fun isPast_todayBoundary_isNotPast() =
        assertFalse(Registry(eventDateMs = 5_000L).isPast(todayMs = 5_000L))

    @Test fun isPast_futureDate_isNotPast() =
        assertFalse(Registry(eventDateMs = 10_000L).isPast(todayMs = 5_000L))
}
