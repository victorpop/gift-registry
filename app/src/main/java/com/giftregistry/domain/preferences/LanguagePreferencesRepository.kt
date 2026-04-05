package com.giftregistry.domain.preferences

import kotlinx.coroutines.flow.Flow

interface LanguagePreferencesRepository {
    fun observeLanguageTag(): Flow<String?>
    suspend fun setLanguageTag(tag: String)
    suspend fun getLanguageTag(): String?
}
