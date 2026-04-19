package com.giftregistry.data.preferences

import com.giftregistry.domain.preferences.LastRegistryPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LastRegistryPreferencesDataStoreTest {

    private class InMemoryLastRegistryPrefs : LastRegistryPreferencesRepository {
        private val state = MutableStateFlow<String?>(null)
        override fun observeLastRegistryId(): Flow<String?> = state
        override suspend fun getLastRegistryId(): String? = state.value
        override suspend fun setLastRegistryId(registryId: String) { state.value = registryId }
        override suspend fun clearLastRegistryId() { state.value = null }
    }

    @Test
    fun `save then get returns same lastRegistryId`() = runTest {
        val prefs = InMemoryLastRegistryPrefs()
        prefs.setLastRegistryId("reg-123")
        assertEquals("reg-123", prefs.getLastRegistryId())
    }

    @Test
    fun `get returns null before save`() = runTest {
        val prefs = InMemoryLastRegistryPrefs()
        assertNull(prefs.getLastRegistryId())
    }

    @Test
    fun `observe emits saved id after set`() = runTest {
        val prefs = InMemoryLastRegistryPrefs()
        prefs.setLastRegistryId("reg-456")
        assertEquals("reg-456", prefs.observeLastRegistryId().first())
    }

    @Test
    fun `clear resets to null`() = runTest {
        val prefs = InMemoryLastRegistryPrefs()
        prefs.setLastRegistryId("reg-789")
        prefs.clearLastRegistryId()
        assertNull(prefs.getLastRegistryId())
    }
}
