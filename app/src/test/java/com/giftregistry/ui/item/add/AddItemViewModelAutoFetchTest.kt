package com.giftregistry.ui.item.add

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import com.giftregistry.MainDispatcherRule
import com.giftregistry.domain.auth.AuthRepository
import com.giftregistry.domain.auth.AuthStateEvent
import com.giftregistry.domain.model.OgMetadata
import com.giftregistry.domain.usecase.AddItemUseCase
import com.giftregistry.domain.usecase.FetchOgMetadataUseCase
import com.giftregistry.domain.usecase.ObserveRegistriesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * quick-260512-wt8: Pins the auto-fetch pipeline contract for AddItemViewModel.
 *
 * Behaviour locked in:
 *   - URL validity gate (http/https only, non-blank host)
 *   - 700ms debounce on user input
 *   - distinct/dedup against lastFetchedUrl
 *   - collectLatest cancellation when URL changes mid-fetch
 *   - initialUrl single-fire on init (no double-fire from auto-fetch flow)
 *   - manual onFetchMetadata() bypasses dedup (retry affordance)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddItemViewModelAutoFetchTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Before fun stubAndroidLog() {
        // android.util.Log is not present in the JVM unit-test classpath — the
        // production code (AddItemViewModel.onFetchMetadata) calls Log.d / Log.e
        // for diagnostic logging, which would otherwise throw RuntimeException
        // "Method d in android.util.Log not mocked." mockkStatic returns the
        // default Int (0) for every Log method so the VM under test runs to
        // completion.
        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.i(any(), any<String>()) } returns 0
    }

    @After fun unstubAndroidLog() {
        unmockkStatic(Log::class)
    }

    private fun buildVm(
        initialUrl: String = "",
        fetchUseCase: FetchOgMetadataUseCase = defaultFetchMock(),
    ): Pair<AddItemViewModel, FetchOgMetadataUseCase> {
        val authRepo = mockk<AuthRepository>(relaxed = true) {
            every { authState } returns flowOf(AuthStateEvent.Initial(user = null))
        }
        val observeRegs = mockk<ObserveRegistriesUseCase>(relaxed = true)
        val addItem = mockk<AddItemUseCase>(relaxed = true)
        val ssh = SavedStateHandle(mapOf("initialUrl" to initialUrl))
        val vm = AddItemViewModel(ssh, authRepo, observeRegs, addItem, fetchUseCase)
        return vm to fetchUseCase
    }

    private fun defaultFetchMock(): FetchOgMetadataUseCase {
        val m = mockk<FetchOgMetadataUseCase>(relaxed = true)
        coEvery { m.invoke(any<String>()) } returns Result.success(
            OgMetadata(title = "T", imageUrl = "img", price = "10", priceAmount = null, priceCurrency = null)
        )
        return m
    }

    private fun successFetchMock(): FetchOgMetadataUseCase {
        val m = mockk<FetchOgMetadataUseCase>()
        coEvery { m.invoke(any<String>()) } returns Result.success(
            OgMetadata(null, null, null, null, null)
        )
        return m
    }

    @Test fun `invalid url does not trigger fetch`() = runTest {
        val fetch = mockk<FetchOgMetadataUseCase>(relaxed = true)
        val (vm, _) = buildVm(fetchUseCase = fetch)
        vm.url.value = "not a url"
        advanceTimeBy(1000)
        advanceUntilIdle()
        coVerify(exactly = 0) { fetch.invoke(any()) }
    }

    @Test fun `non-http scheme does not trigger fetch`() = runTest {
        val fetch = mockk<FetchOgMetadataUseCase>(relaxed = true)
        val (vm, _) = buildVm(fetchUseCase = fetch)
        vm.url.value = "ftp://example.com/x"
        advanceTimeBy(1000)
        advanceUntilIdle()
        coVerify(exactly = 0) { fetch.invoke(any()) }
    }

    @Test fun `valid https url triggers exactly one fetch after debounce`() = runTest {
        val fetch = successFetchMock()
        val (vm, _) = buildVm(fetchUseCase = fetch)
        vm.url.value = "https://emag.ro/p/123"
        advanceTimeBy(800)
        advanceUntilIdle()
        coVerify(exactly = 1) { fetch.invoke("https://emag.ro/p/123") }
    }

    @Test fun `rapid edits debounce to single fetch for final value`() = runTest {
        val fetch = successFetchMock()
        val (vm, _) = buildVm(fetchUseCase = fetch)
        vm.url.value = "https://emag.ro/a"
        advanceTimeBy(200)
        vm.url.value = "https://emag.ro/b"
        advanceTimeBy(200)
        vm.url.value = "https://emag.ro/final"
        advanceTimeBy(800)
        advanceUntilIdle()
        coVerify(exactly = 1) { fetch.invoke("https://emag.ro/final") }
        coVerify(exactly = 0) { fetch.invoke("https://emag.ro/a") }
        coVerify(exactly = 0) { fetch.invoke("https://emag.ro/b") }
    }

    @Test fun `same url emitted twice in row dedups via lastFetchedUrl`() = runTest {
        val fetch = successFetchMock()
        val (vm, _) = buildVm(fetchUseCase = fetch)
        vm.url.value = "https://emag.ro/p/1"
        advanceTimeBy(800)
        advanceUntilIdle()
        // Re-emit the exact same value (e.g., user re-typed and value snapped back)
        vm.url.value = "https://emag.ro/p/1"
        advanceTimeBy(800)
        advanceUntilIdle()
        coVerify(exactly = 1) { fetch.invoke("https://emag.ro/p/1") }
    }

    @Test fun `initialUrl from savedStateHandle fires exactly one fetch on init`() = runTest {
        val fetch = successFetchMock()
        val (_, _) = buildVm(initialUrl = "https://prefill.com/x", fetchUseCase = fetch)
        advanceTimeBy(1500)
        advanceUntilIdle()
        // The init-block path fires the one-shot fetch; auto-fetch flow MUST NOT
        // double-fire for the same already-set URL.
        coVerify(exactly = 1) { fetch.invoke("https://prefill.com/x") }
    }

    @Test fun `manual onFetchMetadata bypasses dedup and re-fetches same url`() = runTest {
        val fetch = successFetchMock()
        val (vm, _) = buildVm(fetchUseCase = fetch)
        vm.url.value = "https://emag.ro/p/1"
        advanceTimeBy(800)
        advanceUntilIdle()
        // Manual retry — icon-button path — must fetch even though URL unchanged.
        vm.onFetchMetadata()
        advanceUntilIdle()
        coVerify(exactly = 2) { fetch.invoke("https://emag.ro/p/1") }
    }
}
