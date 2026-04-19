package com.giftregistry.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.giftregistry.domain.preferences.LastRegistryPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// CRITICAL: name MUST be "last_registry_prefs" (unique). Research Pitfall 3 —
// duplicate DataStore names cause runtime IllegalStateException. Confirmed
// used names: guest_prefs, language_prefs, onboarding_prefs.
private val Context.lastRegistryDataStore: DataStore<Preferences> by preferencesDataStore(name = "last_registry_prefs")

@Singleton
class LastRegistryPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) : LastRegistryPreferencesRepository {

    private val lastRegistryIdKey = stringPreferencesKey("last_registry_id")

    override fun observeLastRegistryId(): Flow<String?> =
        context.lastRegistryDataStore.data.map { it[lastRegistryIdKey] }

    override suspend fun getLastRegistryId(): String? =
        context.lastRegistryDataStore.data.first()[lastRegistryIdKey]

    override suspend fun setLastRegistryId(registryId: String) {
        context.lastRegistryDataStore.edit { it[lastRegistryIdKey] = registryId }
    }

    override suspend fun clearLastRegistryId() {
        context.lastRegistryDataStore.edit { it.remove(lastRegistryIdKey) }
    }
}
