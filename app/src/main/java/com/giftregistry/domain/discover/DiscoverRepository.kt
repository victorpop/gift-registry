package com.giftregistry.domain.discover

/**
 * Phase 17 D-39 — read-only Android-side contract over the two Discover
 * Cloud Functions Callables (`discoverPopular`, `discoverSearch`).
 *
 * Both methods return `Result<List<DiscoverProduct>>`:
 * - success(emptyList()) when the Callable returns an empty `products` array
 *   (NOT a failure — the UI maps this to Empty state).
 * - failure(<exception>) for Callable errors (HttpsError, network, etc.).
 *
 * The implementation lives in `data/discover/DiscoverRepositoryImpl.kt` and
 * is bound by Hilt via `di/DiscoverModule.kt`.
 */
interface DiscoverRepository {
    suspend fun getPopular(): Result<List<DiscoverProduct>>
    suspend fun search(query: String): Result<List<DiscoverProduct>>
}
