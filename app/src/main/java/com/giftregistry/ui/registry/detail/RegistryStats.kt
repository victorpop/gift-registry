package com.giftregistry.ui.registry.detail

import com.giftregistry.domain.model.Item
import com.giftregistry.domain.model.ItemStatus

/**
 * SCR-08 stats strip (items / reserved / given).
 */
data class RegistryStats(
    val items: Int,
    val reserved: Int,
    val given: Int,
)

/**
 * Derives stats from the current items list.
 * Pure Kotlin — unit-tested by RegistryStatsTest.
 */
fun registryStatsOf(items: List<Item>): RegistryStats = RegistryStats(
    items = items.size,
    reserved = items.count { it.status == ItemStatus.RESERVED },
    given = items.count { it.status == ItemStatus.PURCHASED },
)
