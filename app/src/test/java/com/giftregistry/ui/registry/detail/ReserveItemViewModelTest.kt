package com.giftregistry.ui.registry.detail

import androidx.lifecycle.SavedStateHandle
import com.giftregistry.domain.model.GuestUser
import com.giftregistry.domain.model.ReservationResult
import com.giftregistry.domain.preferences.GuestPreferencesRepository
import com.giftregistry.domain.usecase.ConfirmPurchaseUseCase
import com.giftregistry.domain.usecase.DeleteItemUseCase
import com.giftregistry.domain.usecase.DeleteRegistryUseCase
import com.giftregistry.domain.usecase.ObserveItemsUseCase
import com.giftregistry.domain.usecase.ObserveRegistryUseCase
import com.giftregistry.domain.usecase.ReserveItemUseCase
import com.giftregistry.ui.notifications.NotificationBus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReserveItemViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var reserveItemUseCase: ReserveItemUseCase
    private lateinit var guestPrefs: GuestPreferencesRepository
    private lateinit var observeRegistry: ObserveRegistryUseCase
    private lateinit var observeItems: ObserveItemsUseCase
    private lateinit var deleteRegistry: DeleteRegistryUseCase
    private lateinit var deleteItem: DeleteItemUseCase

    private val guest = GuestUser("Ana", "Pop", "ana@example.ro")

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        reserveItemUseCase = mockk()
        guestPrefs = mockk(relaxed = true)
        observeRegistry = mockk()
        observeItems = mockk()
        deleteRegistry = mockk()
        deleteItem = mockk()
        every { observeRegistry(any()) } returns emptyFlow()
        every { observeItems(any()) } returns emptyFlow()
    }

    @After fun tearDown() { Dispatchers.resetMain() }

    private fun vm(): RegistryDetailViewModel {
        val ssh = SavedStateHandle(mapOf("registryId" to "reg-1"))
        return RegistryDetailViewModel(
            observeRegistryUseCase = observeRegistry,
            observeItemsUseCase = observeItems,
            deleteRegistryUseCase = deleteRegistry,
            deleteItemUseCase = deleteItem,
            reserveItemUseCase = reserveItemUseCase,
            guestPreferencesRepository = guestPrefs,
            deepLinkBus = ReservationDeepLinkBus(),
            confirmPurchaseUseCase = mockk(relaxed = true),
            notificationBus = NotificationBus(),
            // Phase 12 — cover-photo edit deps; not exercised by this test, relaxed mocks suffice.
            authRepository = mockk(relaxed = true),
            updateRegistryUseCase = mockk(relaxed = true),
            storageRepository = mockk(relaxed = true),
            coverImageProcessor = mockk(relaxed = true),
            savedStateHandle = ssh,
        )
    }

    @Test
    fun `onReserveClicked with guest identity emits OpenRetailer`() = runTest(dispatcher) {
        coEvery { guestPrefs.getGuestIdentity() } returns guest
        coEvery { reserveItemUseCase("reg-1", "item-1", guest, null) } returns
            Result.success(ReservationResult("r1", "https://aff.example", 123L))

        val vm = vm()
        vm.onReserveClicked("item-1")
        advanceUntilIdle()

        val event = vm.reservationEvents.first()
        assertTrue(event is RegistryDetailViewModel.ReservationEvent.OpenRetailer)
        assertEquals("https://aff.example",
            (event as RegistryDetailViewModel.ReservationEvent.OpenRetailer).affiliateUrl)
    }

    @Test
    fun `successful reserve persists reservationId via GuestPreferencesRepository`() = runTest(dispatcher) {
        coEvery { guestPrefs.getGuestIdentity() } returns guest
        coEvery { reserveItemUseCase("reg-1", "item-1", guest, null) } returns
            Result.success(ReservationResult("res-server-42", "https://aff.example", 123L))

        val vm = vm()
        vm.onReserveClicked("item-1")
        advanceUntilIdle()

        // The server-returned reservationId (NOT the item id) must be persisted
        // so ConfirmPurchaseBanner can pass it to confirmPurchase callable later.
        coVerify(exactly = 1) { guestPrefs.setActiveReservationId("res-server-42") }
    }

    @Test
    fun `onReserveClicked with no guest identity emits ShowGuestSheet`() = runTest(dispatcher) {
        coEvery { guestPrefs.getGuestIdentity() } returns null

        val vm = vm()
        vm.onReserveClicked("item-1")
        advanceUntilIdle()

        val event = vm.reservationEvents.first()
        assertTrue(event is RegistryDetailViewModel.ReservationEvent.ShowGuestSheet)
        coVerify(exactly = 0) { reserveItemUseCase(any(), any(), any(), any()) }
    }

    @Test
    fun `onGuestIdentitySubmitted saves guest and reserves pending item`() = runTest(dispatcher) {
        coEvery { guestPrefs.getGuestIdentity() } returns null
        coEvery { reserveItemUseCase("reg-1", "item-5", guest, null) } returns
            Result.success(ReservationResult("r", "https://aff", 0L))

        val vm = vm()
        vm.onReserveClicked("item-5")
        advanceUntilIdle()
        vm.onGuestIdentitySubmitted(guest)
        advanceUntilIdle()

        coVerify { guestPrefs.saveGuestIdentity(guest) }
        coVerify { reserveItemUseCase("reg-1", "item-5", guest, null) }
    }

    @Test
    fun `ITEM_UNAVAILABLE failure emits ShowConflictError`() = runTest(dispatcher) {
        coEvery { guestPrefs.getGuestIdentity() } returns guest
        coEvery { reserveItemUseCase(any(), any(), any(), any()) } returns
            Result.failure(IllegalStateException("ITEM_UNAVAILABLE"))

        val vm = vm()
        vm.onReserveClicked("item-1")
        advanceUntilIdle()

        val event = vm.reservationEvents.first()
        assertTrue(event is RegistryDetailViewModel.ReservationEvent.ShowConflictError)
        assertEquals("ITEM_UNAVAILABLE",
            (event as RegistryDetailViewModel.ReservationEvent.ShowConflictError).code)
    }
}
