package com.giftregistry.domain.preferences

import kotlinx.coroutines.flow.Flow

interface OnboardingPreferencesRepository {
    fun observeOnboardingSeen(): Flow<Boolean>
    suspend fun isOnboardingSeen(): Boolean
    suspend fun setOnboardingSeen()
}
