package com.giftregistry.ui.registry.detail

import com.giftregistry.domain.model.ItemStatus

/**
 * Filter chips on the Registry Detail screen — 3 states in fixed UI order.
 *
 * Domain → UI mapping:
 *   Open      → ItemStatus.AVAILABLE
 *   Reserved  → ItemStatus.RESERVED
 *   Completed → ItemStatus.PURCHASED   ← handoff asymmetric alias, matches Phase 9 StatusChip
 *
 * Unit-tested by FilterChipStateTest.
 */
enum class FilterChipState { Open, Reserved, Completed }

/** True when [status] should be included under the [this] filter. */
fun FilterChipState.matches(status: ItemStatus): Boolean = when (this) {
    FilterChipState.Open      -> status == ItemStatus.AVAILABLE
    FilterChipState.Reserved  -> status == ItemStatus.RESERVED
    FilterChipState.Completed -> status == ItemStatus.PURCHASED
}
