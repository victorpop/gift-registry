package com.giftregistry.domain.preferences

import kotlinx.coroutines.flow.Flow

/**
 * Phase 6 (D-14): users/{uid}.preferredLocale read/write.
 * Values are "en" | "ro" only; null means "not yet set" (trigger defaults to "en").
 * This is the EMAIL/notification locale, NOT the app display locale (that is LanguagePreferencesRepository).
 */
interface EmailLocaleRepository {
    fun observeEmailLocale(): Flow<String?>
    suspend fun getEmailLocale(): String?
    suspend fun setEmailLocale(locale: String): Result<Unit>
}
