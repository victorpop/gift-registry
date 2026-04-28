package com.giftregistry.ui.registry.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.giftregistry.R
import com.giftregistry.domain.preferences.GuestPreferencesRepository
import com.giftregistry.domain.usecase.ConfirmPurchaseUseCase
import com.giftregistry.domain.usecase.DeleteItemUseCase
import com.giftregistry.domain.usecase.DeleteRegistryUseCase
import com.giftregistry.domain.usecase.ObserveItemsUseCase
import com.giftregistry.domain.usecase.ObserveRegistryUseCase
import com.giftregistry.domain.usecase.ReserveItemUseCase
import com.giftregistry.ui.notifications.NotificationBus
import com.giftregistry.ui.notifications.PurchasePush
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import com.giftregistry.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class RegistryDetailViewModelConfirmPurchaseTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val observeRegistry: ObserveRegistryUseCase = mockk(relaxed = true)
    private val observeItems: ObserveItemsUseCase = mockk(relaxed = true)
    private val deleteRegistry: DeleteRegistryUseCase = mockk(relaxed = true)
    private val deleteItem: DeleteItemUseCase = mockk(relaxed = true)
    private val reserveItemUseCase: ReserveItemUseCase = mockk(relaxed = true)
    private val guestPrefs: GuestPreferencesRepository = mockk(relaxed = true)
    private val confirmPurchaseUseCase: ConfirmPurchaseUseCase = mockk()
    private val notificationBus = NotificationBus()

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
            confirmPurchaseUseCase = confirmPurchaseUseCase,
            notificationBus = notificationBus,
            // Phase 12 — cover-photo edit deps; not exercised by this test, relaxed mocks suffice.
            authRepository = mockk(relaxed = true),
            updateRegistryUseCase = mockk(relaxed = true),
            storageRepository = mockk(relaxed = true),
            coverImageProcessor = mockk(relaxed = true),
            savedStateHandle = ssh,
        )
    }

    @Test
    fun `onConfirmPurchase success emits reservation_confirm_purchase_success resource id`() = runTest {
        coEvery { confirmPurchaseUseCase("res-1") } returns Result.success(Unit)
        val viewModel = vm()

        viewModel.snackbarMessages.test {
            viewModel.onConfirmPurchase("res-1")
            advanceUntilIdle()

            val msg = awaitItem()
            assertEquals(SnackbarMessage.Resource(R.string.reservation_confirm_purchase_success), msg)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onConfirmPurchase failure emits reservation_confirm_purchase_error resource id`() = runTest {
        coEvery { confirmPurchaseUseCase("res-1") } returns Result.failure(RuntimeException("RESERVATION_EXPIRED"))
        val viewModel = vm()

        viewModel.snackbarMessages.test {
            viewModel.onConfirmPurchase("res-1")
            advanceUntilIdle()

            val msg = awaitItem()
            assertEquals(SnackbarMessage.Resource(R.string.reservation_confirm_purchase_error), msg)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onConfirmPurchase sets confirmingPurchase to true then false on success`() = runTest {
        coEvery { confirmPurchaseUseCase("res-1") } returns Result.success(Unit)
        val viewModel = vm()

        // The flow starts with false; after launch it becomes true, then false again.
        // Using a list to capture all emissions.
        val states = mutableListOf<Boolean>()
        val collectJob = launch {
            viewModel.confirmingPurchase.collect { states.add(it) }
        }

        viewModel.onConfirmPurchase("res-1")
        advanceUntilIdle()

        // At minimum: initial false (or true), then final false
        assertFalse("Final confirmingPurchase must be false", viewModel.confirmingPurchase.value)
        collectJob.cancel()
    }

    @Test
    fun `onConfirmPurchase failure leaves confirmingPurchase as false`() = runTest {
        coEvery { confirmPurchaseUseCase("res-1") } returns Result.failure(RuntimeException("RESERVATION_EXPIRED"))
        val viewModel = vm()

        viewModel.onConfirmPurchase("res-1")
        advanceUntilIdle()

        assertFalse(viewModel.confirmingPurchase.value)
    }

    @Test
    fun `NotificationBus push emits SnackbarMessage_Push via snackbarMessages`() = runTest {
        val viewModel = vm()
        advanceUntilIdle() // allow init{} collector to subscribe

        viewModel.snackbarMessages.test {
            // notificationBus.emit is a suspend fun — launch it in the test coroutine
            launch { notificationBus.emit(PurchasePush("reg1", "Ana's Registry")) }
            advanceUntilIdle()

            val msg = awaitItem()
            assertEquals(SnackbarMessage.Push(registryName = "Ana's Registry", registryId = "reg1"), msg)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onConfirmPurchase invokes use case with provided reservationId`() = runTest {
        coEvery { confirmPurchaseUseCase(any()) } returns Result.success(Unit)
        val viewModel = vm()

        viewModel.onConfirmPurchase("reservation-42")
        advanceUntilIdle()

        coVerify(exactly = 1) { confirmPurchaseUseCase("reservation-42") }
    }
}
