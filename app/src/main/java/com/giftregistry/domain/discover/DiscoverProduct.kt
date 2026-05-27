package com.giftregistry.domain.discover

/**
 * Phase 17 D-39 domain model — flat data class matching the Callable
 * response shape from both `discoverPopular` (D-20) and `discoverSearch`
 * (D-31).
 *
 * - `id` is the Firestore document ID for popular products; for search
 *   results (which have no doc ID) the repository synthesises a stable
 *   UUID at mapping time so LazyColumn keys remain unique and
 *   recomposition is stable.
 * - `price` is a Double because both Callables return numeric price.
 *   Display formatting via `NumberFormat.getCurrencyInstance(Locale("ro","RO"))`
 *   is applied at the UI layer (D-37).
 * - `imageUrl` may be empty — the UI renders `discover_card_placeholder.xml`
 *   in that case via Coil `placeholder`/`error` slots.
 */
data class DiscoverProduct(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String,
    val price: Double,
    val currency: String,
    val retailerUrl: String,
    val retailerName: String = "",
)
