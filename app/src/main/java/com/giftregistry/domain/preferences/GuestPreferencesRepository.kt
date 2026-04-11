package com.giftregistry.domain.preferences

import com.giftregistry.domain.model.GuestUser
import kotlinx.coroutines.flow.Flow

interface GuestPreferencesRepository {
    fun observeGuestIdentity(): Flow<GuestUser?>
    suspend fun getGuestIdentity(): GuestUser?
    suspend fun saveGuestIdentity(guest: GuestUser)
    suspend fun clearGuestIdentity()
}
