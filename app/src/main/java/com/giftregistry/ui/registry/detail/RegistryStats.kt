package com.giftregistry.ui.registry.detail

import com.giftregistry.domain.model.Item
import com.giftregistry.domain.model.ItemStatus

/**
 * SCR-08 4-stat strip (items / reserved / given / views).
 *
 * `views` is always 0 in v1.1 per CONTEXT.md § Deferred — `Registry.viewCount`
 * is deferred to v1.2. The field stays in this data class so the strip layout
 * is render-ready when the backing field ships without touching the composable.
 */
data class RegistryStats(
    val items: Int,
    val reserved: Int,
    val given: Int,
    val views: Int,
)

/**
 * Derives stats from the current items list.
 * Pure Kotlin — unit-tested by RegistryStatsTest (Wave 0).
 */
fun registryStatsOf(items: List<Item>): RegistryStats = RegistryStats(
    items = items.size,
    reserved = items.count { it.status == ItemStatus.RESERVED },
    given = items.count { it.status == ItemStatus.PURCHASED },
    views = 0,  // TODO v1.2: Registry.viewCount
)
