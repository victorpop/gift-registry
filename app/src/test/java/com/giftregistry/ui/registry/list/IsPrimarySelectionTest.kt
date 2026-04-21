package com.giftregistry.ui.registry.list

import com.giftregistry.domain.model.Registry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * SCR-07: Primary card selection rule — `registries.maxByOrNull { it.updatedAt }?.id`.
 * CONTEXT.md locks most-recently-updated as the primary card; owner-controlled
 * pinning is out of v1.1 scope. This mirrors AppNavigation.kt Phase 9 resolver
 * (primaryRegistryId = registries.maxByOrNull { it.updatedAt }?.id).
 *
 * Kotlin stdlib contract: on a tie, `maxByOrNull` returns the FIRST matching
 * element. Tests pin that ordering so the UI stays deterministic.
 *
 * RED in Wave 0 — flips GREEN when Plan 02 ships primaryRegistryIdOf(List<Registry>).
 */
class IsPrimarySelectionTest {
    @Test fun empty_returnsNull() =
        assertNull(primaryRegistryIdOf(emptyList()))

    @Test fun single_returnsOnly() {
        val result = primaryRegistryIdOf(listOf(Registry(id = "r1", updatedAt = 100L)))
        assertEquals("r1", result)
    }

    @Test fun picksMaxUpdatedAt() {
        val result = primaryRegistryIdOf(
            listOf(
                Registry(id = "r1", updatedAt = 100L),
                Registry(id = "r2", updatedAt = 200L),
                Registry(id = "r3", updatedAt = 150L),
            )
        )
        assertEquals("r2", result)
    }

    @Test fun tieBreaks_consistentlyPicksFirst() {
        val result = primaryRegistryIdOf(
            listOf(
                Registry(id = "r1", updatedAt = 100L),
                Registry(id = "r2", updatedAt = 100L),
            )
        )
        assertEquals("r1", result)
    }

    @Test fun allZeroUpdatedAt_stillReturnsFirst() {
        val result = primaryRegistryIdOf(
            listOf(
                Registry(id = "r1", updatedAt = 0L),
                Registry(id = "r2", updatedAt = 0L),
            )
        )
        assertEquals("r1", result)
    }
}
