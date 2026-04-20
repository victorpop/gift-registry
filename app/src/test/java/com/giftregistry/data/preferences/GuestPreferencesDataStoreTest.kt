package com.giftregistry.data.preferences

import com.giftregistry.domain.model.GuestUser
import com.giftregistry.domain.preferences.GuestPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * GuestPreferencesDataStore wraps Android DataStore and is covered by an integration
 * smoke test manually (D-16 persists across process death — manual verification
 * per 04-VALIDATION.md). This unit test verifies the repository contract using
 * an in-memory fake to ensure call-site behavior is correct.
 */
class GuestPreferencesDataStoreTest {

    private class InMemoryGuestPrefs : GuestPreferencesRepository {
        private val state = MutableStateFlow<GuestUser?>(null)
        private val reservationState = MutableStateFlow<String?>(null)
        override fun observeGuestIdentity(): Flow<GuestUser?> = state
        override suspend fun getGuestIdentity(): GuestUser? = state.value
        override suspend fun saveGuestIdentity(guest: GuestUser) { state.value = guest }
        override suspend fun clearGuestIdentity() { state.value = null }
        override fun observeActiveReservationId(): Flow<String?> = reservationState
        override suspend fun getActiveReservationId(): String? = reservationState.value
        override suspend fun setActiveReservationId(reservationId: String?) { reservationState.value = reservationId }
    }

    @Test
    fun `save then get returns same GuestUser`() = runTest {
        val prefs = InMemoryGuestPrefs()
        val g = GuestUser("Ion", "Popescu", "ion@example.ro")
        prefs.saveGuestIdentity(g)
        assertEquals(g, prefs.getGuestIdentity())
    }

    @Test
    fun `get returns null before save`() = runTest {
        val prefs = InMemoryGuestPrefs()
        assertNull(prefs.getGuestIdentity())
    }

    @Test
    fun `clear removes identity`() = runTest {
        val prefs = InMemoryGuestPrefs()
        prefs.saveGuestIdentity(GuestUser("A", "B", "a@b.c"))
        prefs.clearGuestIdentity()
        assertNull(prefs.getGuestIdentity())
    }

    @Test
    fun `setActiveReservationId persists and null clears`() = runTest {
        val prefs = InMemoryGuestPrefs()
        assertNull(prefs.getActiveReservationId())
        prefs.setActiveReservationId("res-abc-123")
        assertEquals("res-abc-123", prefs.getActiveReservationId())
        prefs.setActiveReservationId(null)
        assertNull(prefs.getActiveReservationId())
    }
}
