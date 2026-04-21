package com.giftregistry.ui.registry.detail

import com.giftregistry.domain.model.ItemStatus

/**
 * SCR-08: Filter chips on the Registry Detail screen — 4 states in fixed UI order.
 *
 * Domain → UI mapping (locked by CONTEXT.md § Registry Detail):
 *   All       → every ItemStatus
 *   Open      → ItemStatus.AVAILABLE
 *   Reserved  → ItemStatus.RESERVED
 *   Completed → ItemStatus.PURCHASED   ← handoff asymmetric alias, matches Phase 9 StatusChip
 *
 * Unit-tested by FilterChipStateTest (Wave 0).
 */
enum class FilterChipState { All, Open, Reserved, Completed }

/** True when [status] should be included under the [this] filter. */
fun FilterChipState.matches(status: ItemStatus): Boolean = when (this) {
    FilterChipState.All       -> true
    FilterChipState.Open      -> status == ItemStatus.AVAILABLE
    FilterChipState.Reserved  -> status == ItemStatus.RESERVED
    FilterChipState.Completed -> status == ItemStatus.PURCHASED
}
