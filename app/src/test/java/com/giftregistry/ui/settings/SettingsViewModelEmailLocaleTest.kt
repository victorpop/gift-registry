package com.giftregistry.ui.settings

import app.cash.turbine.test
import com.giftregistry.MainDispatcherRule
import com.giftregistry.domain.auth.SignOutUseCase
import com.giftregistry.domain.preferences.LanguagePreferencesRepository
import com.giftregistry.domain.usecase.ObserveEmailLocaleUseCase
import com.giftregistry.domain.usecase.SetEmailLocaleUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelEmailLocaleTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val fakeLanguagePrefsRepo: LanguagePreferencesRepository = mockk(relaxed = true) {
        every { observeLanguageTag() } returns flowOf("en")
    }
    private val fakeSignOutUseCase: SignOutUseCase = mockk(relaxed = true)

    private fun buildVm(
        observe: ObserveEmailLocaleUseCase,
        set: SetEmailLocaleUseCase,
    ) = SettingsViewModel(
        languagePrefsRepo = fakeLanguagePrefsRepo,
        signOutUseCase = fakeSignOutUseCase,
        observeEmailLocaleUseCase = observe,
        setEmailLocaleUseCase = set,
    )

    @Test
    fun `emailLocale emits null initially then ro when repository emits ro`() = runTest {
        val source = MutableStateFlow<String?>(null)
        val observe = mockk<ObserveEmailLocaleUseCase>()
        val set = mockk<SetEmailLocaleUseCase>(relaxed = true)
        every { observe.invoke() } returns source

        val vm = buildVm(observe, set)

        vm.emailLocale.test {
            assertEquals(null, awaitItem())
            source.value = "ro"
            assertEquals("ro", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onEmailLocaleChange invokes SetEmailLocaleUseCase with new value`() = runTest {
        val observe = mockk<ObserveEmailLocaleUseCase>()
        every { observe.invoke() } returns flowOf(null)
        val set = mockk<SetEmailLocaleUseCase>()
        coEvery { set.invoke("ro") } returns Result.success(Unit)

        val vm = buildVm(observe, set)
        vm.onEmailLocaleChange("ro")
        advanceUntilIdle()

        coVerify(exactly = 1) { set.invoke("ro") }
    }

    @Test
    fun `onEmailLocaleChange swallows failure from use case (no throw)`() = runTest {
        val observe = mockk<ObserveEmailLocaleUseCase>()
        every { observe.invoke() } returns flowOf("en")
        val set = mockk<SetEmailLocaleUseCase>()
        coEvery { set.invoke(any()) } returns Result.failure(IllegalArgumentException("Invalid locale"))

        val vm = buildVm(observe, set)
        vm.onEmailLocaleChange("zz") // intentionally invalid; VM must not throw
        advanceUntilIdle()

        coVerify(exactly = 1) { set.invoke("zz") }
    }
}
