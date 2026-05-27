package com.giftregistry.ui.item.add

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * SCR-10: AddItemMode — 2 modes exposed as an enum for the Paste URL / Manual
 * segmented control on the Add item screen.
 *
 * CONTEXT.md § Add Item URL locks the order: PasteUrl (default), Manual.
 * Phase 10 precedent (STATE.md): "Tab index uses Int via rememberSaveable
 * mutableIntStateOf(0), not sealed class" — so ADD_ITEM_MODE_DEFAULT_ORDINAL is
 * exposed as a top-level const val so the Compose screen can initialise its
 * rememberSaveable state without importing the enum.
 *
 * History:
 *  - Wave 0 (Phase 11): originally 3 modes (PasteUrl, Browse stores, Manual).
 *  - Plan 17-01: middle mode removed with the Stores capability; enum now
 *    has 2 modes — PasteUrl (ordinal 0) and Manual (ordinal 1).
 */
class AddItemModeTest {

    @Test fun allMembersExist() {
        val entries = AddItemMode.entries
        assertEquals(2, entries.size)
        assertEquals(AddItemMode.PasteUrl, entries[0])
        assertEquals(AddItemMode.Manual, entries[1])
    }

    @Test fun pasteUrl_isOrdinalZero() =
        assertEquals(0, AddItemMode.PasteUrl.ordinal)

    @Test fun manual_isOrdinalOne() =
        assertEquals(1, AddItemMode.Manual.ordinal)

    @Test fun defaultOrdinal_isPasteUrl() {
        assertEquals(
            "Default mode ordinal must be 0 (PasteUrl) per CONTEXT.md § Paste URL mode (default)",
            0, ADD_ITEM_MODE_DEFAULT_ORDINAL,
        )
        assertEquals(AddItemMode.PasteUrl, AddItemMode.entries[ADD_ITEM_MODE_DEFAULT_ORDINAL])
    }
}
