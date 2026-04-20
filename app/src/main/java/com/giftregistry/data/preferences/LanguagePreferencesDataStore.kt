package com.giftregistry.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.giftregistry.domain.preferences.LanguagePreferencesRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

private val ALLOWED_LOCALES = setOf("en", "ro")

@Singleton
class LanguagePreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : LanguagePreferencesRepository {

    private val languageTagKey = stringPreferencesKey("language_tag")

    override fun observeLanguageTag(): Flow<String?> =
        context.dataStore.data.map { prefs -> prefs[languageTagKey] }

    override suspend fun setLanguageTag(tag: String) {
        context.dataStore.edit { prefs -> prefs[languageTagKey] = tag }
    }

    override suspend fun getLanguageTag(): String? =
        context.dataStore.data.first()[languageTagKey]

    override suspend fun syncRemoteLocale(tag: String) {
        val uid = auth.currentUser?.uid ?: return
        if (tag !in ALLOWED_LOCALES) return
        runCatching {
            firestore.collection("users").document(uid)
                .set(mapOf("preferredLocale" to tag), SetOptions.merge())
                .await()
        }.onFailure { e -> android.util.Log.w("LanguagePrefs", "Remote locale sync failed", e) }
    }
}
