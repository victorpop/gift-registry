package com.giftregistry.ui.discover

import com.giftregistry.domain.discover.DiscoverProduct

/**
 * 260530-ncw — session-local layout mode for the Discover screen.
 * Default = TwoColumns. OneColumn is user-toggled via the icon button
 * in the top-right; not persisted.
 */
sealed interface DiscoverLayoutMode {
    val columnCount: Int
    data object OneColumn : DiscoverLayoutMode { override val columnCount: Int = 1 }
    data object TwoColumns : DiscoverLayoutMode { override val columnCount: Int = 2 }
}

/**
 * Phase 17 D-38 — sealed state machine for the popular (community) section.
 *
 * Lifecycle: Loading (init) → Loaded(products) | Empty | Error(message).
 * `loadPopular()` resets to Loading on every retry.
 */
sealed interface PopularState {
    data object Loading : PopularState
    data class Loaded(val products: List<DiscoverProduct>) : PopularState
    data object Empty : PopularState
    data class Error(val message: String) : PopularState
}

/**
 * Phase 17 D-38 — sealed state machine for the FROM-THE-WEB search section.
 *
 * Idle is the initial state and the state to which a blank/whitespace-only
 * query resets — the UI uses Idle to hide the entire FROM-THE-WEB section
 * (D-35). Loading appears after a Search IME action; Loaded/Empty/Error are
 * terminal.
 */
sealed interface SearchState {
    data object Idle : SearchState
    data object Loading : SearchState
    data class Loaded(val products: List<DiscoverProduct>) : SearchState
    data object Empty : SearchState
    data class Error(val message: String) : SearchState
}
