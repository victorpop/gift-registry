package com.giftregistry.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.giftregistry.domain.preferences.OnboardingPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// CRITICAL: name MUST be "onboarding_prefs" (NOT "user_prefs", NOT "guest_prefs").
// Duplicate DataStore names cause runtime IllegalStateException. The Context
// extension property MUST also be uniquely named (onboardingDataStore) so it
// does not collide with the dataStore / guestDataStore properties on Context
// declared in the Language / Guest preferences DataStores.
private val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(name = "onboarding_prefs")

@Singleton
class OnboardingPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) : OnboardingPreferencesRepository {

    private val onboardingSeenKey = booleanPreferencesKey("onboarding_seen")

    override fun observeOnboardingSeen(): Flow<Boolean> =
        context.onboardingDataStore.data.map { prefs -> prefs[onboardingSeenKey] ?: false }

    override suspend fun isOnboardingSeen(): Boolean =
        context.onboardingDataStore.data.first()[onboardingSeenKey] ?: false

    override suspend fun setOnboardingSeen() {
        context.onboardingDataStore.edit { prefs -> prefs[onboardingSeenKey] = true }
    }
}
