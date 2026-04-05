package com.giftregistry.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.giftregistry.domain.preferences.LanguagePreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class LanguagePreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) : LanguagePreferencesRepository {

    private val languageTagKey = stringPreferencesKey("language_tag")

    override fun observeLanguageTag(): Flow<String?> =
        context.dataStore.data.map { prefs -> prefs[languageTagKey] }

    override suspend fun setLanguageTag(tag: String) {
        context.dataStore.edit { prefs -> prefs[languageTagKey] = tag }
    }

    override suspend fun getLanguageTag(): String? =
        context.dataStore.data.first()[languageTagKey]
}
