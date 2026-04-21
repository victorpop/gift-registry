package com.giftregistry.ui.registry.detail

import com.giftregistry.domain.model.ItemStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SCR-08: Filter chip enum + domain → filter predicate mapping.
 *
 *   All       → matches every status
 *   Open      → ItemStatus.AVAILABLE
 *   Reserved  → ItemStatus.RESERVED
 *   Completed → ItemStatus.PURCHASED
 *
 * Locked by CONTEXT.md § Registry Detail ("Open = AVAILABLE, Reserved = RESERVED,
 * Completed = PURCHASED") and UI-SPEC.md § Filter Chips Row.
 *
 * Note: this domain → UI label mapping is the same mapping used by Phase 9
 * StatusChip dispatcher (STATE.md: "ItemStatus.PURCHASED→GIVEN is highest-risk
 * API pitfall") — the Completed filter aliases the PURCHASED domain value.
 *
 * RED in Wave 0 — flips GREEN when Plan 02 ships FilterChipState.kt in
 * com.giftregistry.ui.registry.detail.
 */
class FilterChipStateTest {
    @Test fun allMembersExist() {
        val entries = FilterChipState.entries
        assertEquals(4, entries.size)
        assertEquals(FilterChipState.All, entries[0])
        assertEquals(FilterChipState.Open, entries[1])
        assertEquals(FilterChipState.Reserved, entries[2])
        assertEquals(FilterChipState.Completed, entries[3])
    }

    @Test fun all_matchesAnyStatus() {
        assertTrue(FilterChipState.All.matches(ItemStatus.AVAILABLE))
        assertTrue(FilterChipState.All.matches(ItemStatus.RESERVED))
        assertTrue(FilterChipState.All.matches(ItemStatus.PURCHASED))
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
