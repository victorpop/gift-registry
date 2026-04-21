package com.giftregistry.ui.common.status

import com.giftregistry.domain.model.ItemStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * STAT-01/02/03 dispatcher: domain ItemStatus → display StatusChipType.
 *
 * CRITICAL mapping (per CONTEXT.md + RESEARCH.md Pattern 7):
 *   domain AVAILABLE  → display OPEN
 *   domain RESERVED   → display RESERVED
 *   domain PURCHASED  → display GIVEN   ← asymmetric rename, locked decision
 *
 * RED in Wave 0 — flips GREEN when Plan 02 ships `StatusChipType` enum and
 * `statusChipTypeOf(ItemStatus): StatusChipType` top-level fn in StatusChip.kt.
 */
class StatusChipDispatcherTest {
    @Test fun available_maps_to_open() =
        assertEquals(StatusChipType.OPEN, statusChipTypeOf(ItemStatus.AVAILABLE))

    @Test fun reserved_maps_to_reserved() =
        assertEquals(StatusChipType.RESERVED, statusChipTypeOf(ItemStatus.RESERVED))

    @Test fun purchased_maps_to_given() =
        assertEquals(StatusChipType.GIVEN, statusChipTypeOf(ItemStatus.PURCHASED))

    @Test fun allDomainStatuses_haveMapping() {
        ItemStatus.entries.forEach { status ->
            assertNotNull(
                "Missing StatusChipType mapping for ItemStatus.$status",
                statusChipTypeOf(status)
            )
        }
    }
}
