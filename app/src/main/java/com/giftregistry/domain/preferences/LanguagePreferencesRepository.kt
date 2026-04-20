package com.giftregistry.domain.preferences

import kotlinx.coroutines.flow.Flow

interface LanguagePreferencesRepository {
    fun observeLanguageTag(): Flow<String?>
    suspend fun setLanguageTag(tag: String)
    suspend fun getLanguageTag(): String?

    /**
     * Best-effort sync of the chosen language to the user's Firestore profile so Cloud Functions
     * can render emails in the same language. No-op for unauthenticated users. Swallows failures.
     */
    suspend fun syncRemoteLocale(tag: String)
}
