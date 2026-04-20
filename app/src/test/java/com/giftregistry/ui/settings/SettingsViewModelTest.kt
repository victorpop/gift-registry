package com.giftregistry.ui.settings

import com.giftregistry.MainDispatcherRule
import com.giftregistry.domain.auth.SignOutUseCase
import com.giftregistry.domain.preferences.LanguagePreferencesRepository
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeLanguagePreferencesRepository : LanguagePreferencesRepository {
        private val flow = MutableStateFlow<String?>(null)
        var lastSyncedTag: String? = null
        var syncCallCount: Int = 0

        override fun observeLanguageTag(): Flow<String?> = flow

        override suspend fun setLanguageTag(tag: String) {
            flow.value = tag
        }

        override suspend fun getLanguageTag(): String? = flow.first()

        override suspend fun syncRemoteLocale(tag: String) {
            lastSyncedTag = tag
            syncCallCount++
        }

        fun emitLanguageTag(tag: String?) {
            flow.value = tag
        }
    }

    private val fakeSignOutUseCase: SignOutUseCase = mockk(relaxed = true)

    @Test
    fun `currentLocale defaults to en when DataStore has no stored value`() = runTest {
        val fakeRepo = FakeLanguagePreferencesRepository()
        val viewModel = SettingsViewModel(fakeRepo, fakeSignOutUseCase)
        advanceUntilIdle()

        assertEquals("en", viewModel.currentLocale.value)
    }

    @Test
    fun `currentLocale emits ro when DataStore has ro stored`() = runTest {
        val fakeRepo = FakeLanguagePreferencesRepository()
        fakeRepo.emitLanguageTag("ro")
        val viewModel = SettingsViewModel(fakeRepo, fakeSignOutUseCase)
        advanceUntilIdle()

        assertEquals("ro", viewModel.currentLocale.value)
    }

    @Test
    fun `changeLocale calls repository setLanguageTag with provided tag`() = runTest {
        val fakeRepo = FakeLanguagePreferencesRepository()
        val viewModel = SettingsViewModel(fakeRepo, fakeSignOutUseCase)
        advanceUntilIdle()

        viewModel.changeLocale("ro")
        advanceUntilIdle()

        assertEquals("ro", fakeRepo.getLanguageTag())
    }

    @Test
    fun `changeLocale syncs remote locale via repository`() = runTest {
        val fakeRepo = FakeLanguagePreferencesRepository()
        val viewModel = SettingsViewModel(fakeRepo, fakeSignOutUseCase)
        advanceUntilIdle()

        viewModel.changeLocale("ro")
        advanceUntilIdle()

        assertEquals("ro", fakeRepo.lastSyncedTag)
        assertEquals(1, fakeRepo.syncCallCount)
    }

    @Test
    fun `changeLocale syncs en locale via repository`() = runTest {
        val fakeRepo = FakeLanguagePreferencesRepository()
        val viewModel = SettingsViewModel(fakeRepo, fakeSignOutUseCase)
        advanceUntilIdle()

        viewModel.changeLocale("en")
        advanceUntilIdle()

        assertEquals("en", fakeRepo.lastSyncedTag)
        assertEquals(1, fakeRepo.syncCallCount)
    }
}
