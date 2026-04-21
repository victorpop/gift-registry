package com.giftregistry.ui.item.add

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * SCR-10: AddItemMode — 3 modes exposed as an enum for the Paste URL / Browse
 * stores / Manual segmented control on the Add item screen.
 *
 * CONTEXT.md § Add Item URL locks the order: PasteUrl (default), BrowseStores, Manual.
 * Phase 10 precedent (STATE.md): "Tab index uses Int via rememberSaveable
 * mutableIntStateOf(0), not sealed class" — so ADD_ITEM_MODE_DEFAULT_ORDINAL is
 * exposed as a top-level const val so the Compose screen can initialise its
 * rememberSaveable state without importing the enum.
 *
 * RED in Wave 0 — flips GREEN when Plan 02 ships AddItemMode.kt + the
 * ADD_ITEM_MODE_DEFAULT_ORDINAL const val in com.giftregistry.ui.item.add.
 */
class AddItemModeTest {

    @Test fun allMembersExist() {
        val entries = AddItemMode.entries
        assertEquals(3, entries.size)
        assertEquals(AddItemMode.PasteUrl, entries[0])
        assertEquals(AddItemMode.BrowseStores, entries[1])
        assertEquals(AddItemMode.Manual, entries[2])
    }

    @Test fun pasteUrl_isOrdinalZero() =
        assertEquals(0, AddItemMode.PasteUrl.ordinal)

    @Test fun browseStores_isOrdinalOne() =
        assertEquals(1, AddItemMode.BrowseStores.ordinal)

    @Test fun manual_isOrdinalTwo() =
        assertEquals(2, AddItemMode.Manual.ordinal)

    @Test fun defaultOrdinal_isPasteUrl() {
        assertEquals(
            "Default mode ordinal must be 0 (PasteUrl) per CONTEXT.md § Paste URL mode (default)",
            0, ADD_ITEM_MODE_DEFAULT_ORDINAL,
        )
        assertEquals(AddItemMode.PasteUrl, AddItemMode.entries[ADD_ITEM_MODE_DEFAULT_ORDINAL])
    }
}
