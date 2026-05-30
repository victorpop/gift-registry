package com.giftregistry.ui.item.add

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import com.giftregistry.MainDispatcherRule
import com.giftregistry.domain.auth.AuthRepository
import com.giftregistry.domain.auth.AuthStateEvent
import com.giftregistry.domain.usecase.AddItemUseCase
import com.giftregistry.domain.usecase.FetchOgMetadataUseCase
import com.giftregistry.domain.usecase.ObserveRegistriesUseCase
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * quick-260530-nx5: Pins the prefill contract for AddItemViewModel.
 *
 * Behaviour locked in:
 *   - When any prefill field is non-blank, VM hydrates form state directly from
 *     SavedStateHandle and does NOT invoke FetchOgMetadataUseCase (the Cloud Function).
 *   - When no prefill fields are present, the existing fromAddSheet baseline is unchanged:
 *     form is blank, no OG fetch fires.
 *   - The critical case: prefillUrl is set but OG fetch must NOT be called — the
 *     Serper-supplied data is the source of truth.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AddItemViewModelPrefillTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Before fun stubAndroidLog() {
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
        prefillTitle: String = "",
        prefillUrl: String = "",
        prefillImageUrl: String = "",
        prefillPrice: String = "",
        prefillRetailerName: String = "",
        prefillCurrency: String = "",
        fromAddSheet: Boolean = true,
        fetchUseCase: FetchOgMetadataUseCase = mockk(relaxed = true),
    ): Pair<AddItemViewModel, FetchOgMetadataUseCase> {
        val authRepo = mockk<AuthRepository>(relaxed = true) {
            every { authState } returns flowOf(AuthStateEvent.Initial(user = null))
        }
        val observeRegs = mockk<ObserveRegistriesUseCase>(relaxed = true)
        val addItem = mockk<AddItemUseCase>(relaxed = true)
        val ssh = SavedStateHandle(mapOf(
            "fromAddSheet" to fromAddSheet,
            "prefillTitle" to prefillTitle,
            "prefillUrl" to prefillUrl,
            "prefillImageUrl" to prefillImageUrl,
            "prefillPrice" to prefillPrice,
            "prefillRetailerName" to prefillRetailerName,
            "prefillCurrency" to prefillCurrency,
        ))
        val vm = AddItemViewModel(ssh, authRepo, observeRegs, addItem, fetchUseCase)
        return vm to fetchUseCase
    }

    @Test fun `prefill values hydrate form state and do not trigger OG fetch`() = runTest {
        val fetch = mockk<FetchOgMetadataUseCase>(relaxed = true)
        val (vm, _) = buildVm(
            prefillTitle = "Toy",
            prefillUrl = "https://emag.ro/p/abc",
            prefillImageUrl = "https://img/x.jpg",
            prefillPrice = "199.99",
            prefillRetailerName = "eMAG",
            prefillCurrency = "RON",
            fromAddSheet = true,
            fetchUseCase = fetch,
        )
        // Drain the auto-fetch debounce pipeline to prove it doesn't fire.
        advanceTimeBy(1500)
        advanceUntilIdle()

        assertEquals("Toy", vm.title.value)
        assertEquals("https://emag.ro/p/abc", vm.url.value)
        assertEquals("https://img/x.jpg", vm.imageUrl.value)
        assertEquals("199.99", vm.price.value)
        assertEquals(true, vm.fromAddSheet)
        assertNull("selectedRegistryId should be null — no registry pre-selected", vm.selectedRegistryId.value)

        // Critical assertion: OG fetch Cloud Function must NOT have been called.
        coVerify(exactly = 0) { fetch.invoke(any()) }
    }

    @Test fun `no prefill values leaves form blank and does not trigger OG fetch`() = runTest {
        val fetch = mockk<FetchOgMetadataUseCase>(relaxed = true)
        val (vm, _) = buildVm(
            fromAddSheet = true,
            fetchUseCase = fetch,
        )
        advanceTimeBy(1500)
        advanceUntilIdle()

        assertEquals("", vm.title.value)
        assertEquals("", vm.url.value)
        assertEquals("", vm.imageUrl.value)
        assertEquals("", vm.price.value)
        assertNull(vm.selectedRegistryId.value)

        coVerify(exactly = 0) { fetch.invoke(any()) }
    }

    @Test fun `prefill takes precedence - even with prefillUrl set fetchOgMetadata is NOT called`() = runTest {
        // This is the critical contract: Serper-supplied product data is the source
        // of truth. Even though prefillUrl is a valid https URL that would normally
        // trigger the auto-fetch debounce pipeline, the prefill branch must have
        // already primed lastFetchedUrl to suppress it.
        val fetch = mockk<FetchOgMetadataUseCase>(relaxed = true)
        val (vm, _) = buildVm(
            prefillTitle = "Esprit T-Shirt",
            prefillUrl = "https://www.emag.ro/tricou-barbati/pd/DKTOY/",
            prefillImageUrl = "https://cdn.emag.ro/img.jpg",
            prefillPrice = "89.99",
            fetchUseCase = fetch,
        )
        // Advance well past the 700ms debounce to ensure nothing sneaks through.
        advanceTimeBy(2000)
        advanceUntilIdle()

        assertEquals("Esprit T-Shirt", vm.title.value)
        assertEquals("https://www.emag.ro/tricou-barbati/pd/DKTOY/", vm.url.value)

        // THE critical assertion — no Cloud Function call.
        coVerify(exactly = 0) { fetch.invoke(any()) }
    }
}
