package com.giftregistry.ui.registry.detail

import com.giftregistry.domain.model.Item
import com.giftregistry.domain.model.ItemStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Stats strip derivation (items / reserved / given).
 *
 *   items    = items.size
 *   reserved = items.count { status == RESERVED }
 *   given    = items.count { status == PURCHASED }
 */
class RegistryStatsTest {

    private fun item(status: ItemStatus = ItemStatus.AVAILABLE) = Item(
        id = "i${status.name.lowercase()}",
        registryId = "r1",
        title = "stub",
        status = status,
    )

    @Test fun emptyList_returnsAllZeros() =
        assertEquals(RegistryStats(items = 0, reserved = 0, given = 0), registryStatsOf(emptyList()))

    @Test fun singleAvailable_itemsIsOne_reservedZero_givenZero() =
        assertEquals(
            RegistryStats(items = 1, reserved = 0, given = 0),
            registryStatsOf(listOf(item(ItemStatus.AVAILABLE))),
        )

    @Test fun singleReserved_reservedIsOne() =
        assertEquals(
            RegistryStats(items = 1, reserved = 1, given = 0),
            registryStatsOf(listOf(item(ItemStatus.RESERVED))),
        )

    @Test fun singlePurchased_givenIsOne() =
        assertEquals(
            RegistryStats(items = 1, reserved = 0, given = 1),
            registryStatsOf(listOf(item(ItemStatus.PURCHASED))),
        )

    @Test fun mixedStatuses_eachCounted() {
        val items = listOf(
            item(ItemStatus.AVAILABLE),
            item(ItemStatus.AVAILABLE),
            item(ItemStatus.RESERVED),
            item(ItemStatus.PURCHASED),
            item(ItemStatus.PURCHASED),
        )
        assertEquals(
            RegistryStats(items = 5, reserved = 1, given = 2),
            registryStatsOf(items),
        )
    }

}
