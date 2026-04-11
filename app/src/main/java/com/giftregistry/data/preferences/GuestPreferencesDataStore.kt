package com.giftregistry.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.giftregistry.domain.model.GuestUser
import com.giftregistry.domain.preferences.GuestPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// CRITICAL: name MUST be "guest_prefs" (NOT "user_prefs"). Research Pitfall 6 —
// duplicate DataStore names cause runtime IllegalStateException.
private val Context.guestDataStore: DataStore<Preferences> by preferencesDataStore(name = "guest_prefs")

@Singleton
class GuestPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) : GuestPreferencesRepository {

    private val firstNameKey = stringPreferencesKey("guest_first_name")
    private val lastNameKey = stringPreferencesKey("guest_last_name")
    private val emailKey = stringPreferencesKey("guest_email")

    override fun observeGuestIdentity(): Flow<GuestUser?> =
        context.guestDataStore.data.map { prefs ->
            val email = prefs[emailKey] ?: return@map null
            GuestUser(
                firstName = prefs[firstNameKey].orEmpty(),
                lastName = prefs[lastNameKey].orEmpty(),
                email = email,
            )
        }

    override suspend fun getGuestIdentity(): GuestUser? {
        val prefs = context.guestDataStore.data.first()
        val email = prefs[emailKey] ?: return null
        return GuestUser(
            firstName = prefs[firstNameKey].orEmpty(),
            lastName = prefs[lastNameKey].orEmpty(),
            email = email,
        )
    }

    override suspend fun saveGuestIdentity(guest: GuestUser) {
        context.guestDataStore.edit { prefs ->
            prefs[firstNameKey] = guest.firstName
            prefs[lastNameKey] = guest.lastName
            prefs[emailKey] = guest.email
        }
    }

    override suspend fun clearGuestIdentity() {
        context.guestDataStore.edit { it.clear() }
    }
}
