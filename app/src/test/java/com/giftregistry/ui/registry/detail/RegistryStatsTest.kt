package com.giftregistry.ui.registry.detail

import com.giftregistry.domain.model.Item
import com.giftregistry.domain.model.ItemStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * SCR-08: 4-stat strip derivation (items / reserved / given / views).
 *
 *   items    = items.size
 *   reserved = items.count { status == RESERVED }
 *   given    = items.count { status == PURCHASED }   // 'given' is the Phase 11 UI label
 *   views    = 0                                      // CONTEXT.md Deferred — v1.2 Registry.viewCount
 *
 * Locked by CONTEXT.md § Stats strip ("items, reserved, given derive from the
 * existing items: Flow<List<Item>> — no new query; views renders as 0 with a
 * Phase 10-era todo").
 *
 * RED in Wave 0 — flips GREEN when Plan 02 ships RegistryStats.kt with
 * `data class RegistryStats(...)` + `fun registryStatsOf(items: List<Item>): RegistryStats`.
 */
class RegistryStatsTest {

    private fun item(status: ItemStatus = ItemStatus.AVAILABLE) = Item(
        id = "i${status.name.lowercase()}",
        registryId = "r1",
        title = "stub",
        status = status,
    )

    @Test fun emptyList_returnsAllZeros() =
        assertEquals(RegistryStats(items = 0, reserved = 0, given = 0, views = 0), registryStatsOf(emptyList()))

    @Test fun singleAvailable_itemsIsOne_reservedZero_givenZero() =
        assertEquals(
            RegistryStats(items = 1, reserved = 0, given = 0, views = 0),
            registryStatsOf(listOf(item(ItemStatus.AVAILABLE))),
        )

    @Test fun singleReserved_reservedIsOne() =
        assertEquals(
            RegistryStats(items = 1, reserved = 1, given = 0, views = 0),
            registryStatsOf(listOf(item(ItemStatus.RESERVED))),
        )

    @Test fun singlePurchased_givenIsOne() =
        assertEquals(
            RegistryStats(items = 1, reserved = 0, given = 1, views = 0),
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
            RegistryStats(items = 5, reserved = 1, given = 2, views = 0),
            registryStatsOf(items),
        )
    }

    @Test fun viewsAlwaysZero_untilV12() {
        // Regression guard — Phase 11 does NOT ship a real view count
        // (CONTEXT.md § Deferred Ideas: Registry.viewCount → v1.2).
        assertEquals(0, registryStatsOf(emptyList()).views)
        assertEquals(0, registryStatsOf(listOf(item(ItemStatus.AVAILABLE))).views)
        assertEquals(0, registryStatsOf(List(100) { item(ItemStatus.AVAILABLE) }).views)
    }
}
