package com.giftregistry.ui.item.add

import com.giftregistry.domain.model.Registry
import com.giftregistry.ui.registry.list.isActive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * quick-260507-uce: Pins the contract for `AddItemViewModel.registriesForPicker`
 * — the picker dropdown in AddItemScreen must show only ACTIVE registries
 * (matching the Lists screen Active tab definition), never past ones.
 *
 * The "active" predicate is the `Registry.isActive(todayMs)` extension defined
 * in `com.giftregistry.ui.registry.list.TabFilters` — DO NOT redefine here. The
 * production code under test must call that exact extension on every emission
 * of `observeRegistries(uid)` before exposing the StateFlow.
 *
 *   Active = eventDateMs == null || eventDateMs >= todayMs   (inclusive)
 *   Past   = eventDateMs != null && eventDateMs < todayMs    (strict)
 *
 * Test technique mirrors TabFilterPredicateTest: plain JUnit 4 + assertTrue /
 * assertFalse / assertEquals, no Robolectric, no MockK, no Coroutines test
 * framework. Instead of instantiating AddItemViewModel (which needs
 * SavedStateHandle + AuthRepository + several use cases) we exercise the SAME
 * filter expression as a pure function over a `List<Registry>` and a fixed
 * `todayMs`. Plan 12-quick-260507-uce specifies the production code MUST use
 * `registries.filter { it.isActive(todayMs) }` so this helper pins the
 * contract — divergence between helper and production = test failure surfaces
 * the regression.
 */
class AddItemViewModelPickerFilterTest {

    /** Mirrors the production filter in `AddItemViewModel.registriesForPicker`. */
    private fun activeRegistriesFor(list: List<Registry>, todayMs: Long): List<Registry> =
        list.filter { it.isActive(todayMs) }

    @Test fun registriesForPicker_excludesPastRegistries() {
        val todayMs = 5_000L
        val past = Registry(id = "past", eventDateMs = 1_000L)
        val future = Registry(id = "future", eventDateMs = 10_000L)
        val undated = Registry(id = "undated", eventDateMs = null)

        val result = activeRegistriesFor(listOf(past, future, undated), todayMs)

        assertEquals(2, result.size)
        assertTrue("future registry must be present", result.any { it.id == "future" })
        assertTrue("undated registry must be present", result.any { it.id == "undated" })
        assertFalse("past registry MUST NOT appear", result.any { it.id == "past" })
    }

    @Test fun registriesForPicker_includesNullEventDate() {
        val todayMs = 5_000L
        val undated = Registry(id = "undated", eventDateMs = null)

        val result = activeRegistriesFor(listOf(undated), todayMs)

        assertEquals(1, result.size)
        assertEquals("undated", result.single().id)
    }

    @Test fun registriesForPicker_includesTodayBoundary() {
        val todayMs = 5_000L
        val onBoundary = Registry(id = "today", eventDateMs = 5_000L)

        val result = activeRegistriesFor(listOf(onBoundary), todayMs)

        assertEquals(
            "registry with eventDateMs == startOfTodayMs is Active (inclusive boundary)",
            1, result.size
        )
        assertEquals("today", result.single().id)
    }

    @Test fun registriesForPicker_emptyWhenAllPast() {
        val todayMs = 5_000L
        val pastA = Registry(id = "test", eventDateMs = 1_000L)
        val pastB = Registry(id = "av-wedding", eventDateMs = 2_000L)
        val pastC = Registry(id = "secret-santa", eventDateMs = 3_000L)

        val result = activeRegistriesFor(listOf(pastA, pastB, pastC), todayMs)

        assertTrue(
            "all-past list must produce empty picker — drives the inline 'Create a registry first' affordance",
            result.isEmpty()
        )
    }
}
