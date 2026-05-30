package com.giftregistry.ui.registry.detail

import com.giftregistry.domain.model.ItemStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Filter chip enum + domain → filter predicate mapping.
 *
 *   Open      → ItemStatus.AVAILABLE
 *   Reserved  → ItemStatus.RESERVED
 *   Completed → ItemStatus.PURCHASED
 *
 * The Completed filter aliases the PURCHASED domain value (matches Phase 9
 * StatusChip dispatcher: ItemStatus.PURCHASED → "Given/Completed" UI label).
 */
class FilterChipStateTest {
    @Test fun allMembersExist() {
        val entries = FilterChipState.entries
        assertEquals(3, entries.size)
        assertEquals(FilterChipState.Open, entries[0])
        assertEquals(FilterChipState.Reserved, entries[1])
        assertEquals(FilterChipState.Completed, entries[2])
    }

    @Test fun open_matchesOnlyAvailable() {
        assertTrue(FilterChipState.Open.matches(ItemStatus.AVAILABLE))
        assertFalse(FilterChipState.Open.matches(ItemStatus.RESERVED))
        assertFalse(FilterChipState.Open.matches(ItemStatus.PURCHASED))
    }

    @Test fun reserved_matchesOnlyReserved() {
        assertTrue(FilterChipState.Reserved.matches(ItemStatus.RESERVED))
        assertFalse(FilterChipState.Reserved.matches(ItemStatus.AVAILABLE))
        assertFalse(FilterChipState.Reserved.matches(ItemStatus.PURCHASED))
    }

    @Test fun completed_matchesOnlyPurchased() {
        assertTrue(FilterChipState.Completed.matches(ItemStatus.PURCHASED))
        assertFalse(FilterChipState.Completed.matches(ItemStatus.AVAILABLE))
        assertFalse(FilterChipState.Completed.matches(ItemStatus.RESERVED))
    }
}
