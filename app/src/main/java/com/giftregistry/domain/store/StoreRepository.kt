package com.giftregistry.domain.store

import com.giftregistry.domain.model.Store

interface StoreRepository {
    /**
     * One-shot read of the `config/stores` Firestore document. Returns
     * `Result.success(stores)` sorted by displayOrder ascending, or
     * `Result.failure(...)` if the read fails. Empty list is a successful
     * state (doc exists but `stores` array absent or empty) — callers
     * surface an "empty/error" UI state per D-17.
     */
    suspend fun getStores(): Result<List<Store>>
}
