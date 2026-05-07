package com.giftregistry.ui.item.edit

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.giftregistry.MainDispatcherRule
import com.giftregistry.R
import com.giftregistry.domain.auth.AuthRepository
import com.giftregistry.domain.model.GuestUser
import com.giftregistry.domain.model.Registry
import com.giftregistry.domain.model.ReservationResult
import com.giftregistry.domain.model.User
import com.giftregistry.domain.preferences.GuestPreferencesRepository
import com.giftregistry.domain.usecase.ConfirmPurchaseUseCase
import com.giftregistry.domain.usecase.FetchOgMetadataUseCase
import com.giftregistry.domain.usecase.ObserveItemsUseCase
import com.giftregistry.domain.usecase.ObserveRegistryUseCase
import com.giftregistry.domain.usecase.ReserveItemUseCase
import com.giftregistry.domain.usecase.UpdateItemUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Quick task 260507-vrp — pins the reservation orchestration plumbing on
 * [EditItemViewModel] used by the invitee branch of EditItemScreen.
 *
 * The orchestration mirrors [com.giftregistry.ui.registry.detail.RegistryDetailViewModel]'s
 * performReservation + onConfirmPurchase line-for-line, with two adjustments:
 *  - snackbar payload is a raw Int resId (no SnackbarMessage sealed interface)
 *  - giverId passed to ReserveItemUseCase is `authRepository.currentUser?.uid`
 *    (signed-in invitees have a UID; the public web giver flow always passes null)
 *
 * The 5 tests:
 *  1. reserve success path -> setActiveReservationId + OpenRetailer
 *  2. no guest identity -> ShowGuestSheet, use case NOT called
 *  3. reserve failure -> ShowConflictError + setActiveReservationId NOT called
 *  4. confirmPurchase success -> success snackbar + setActiveReservationId(null)
 *  5. confirmPurchase failure -> error snackbar, setActiveReservationId NOT called
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EditItemViewModelReservationTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observeRegistry: ObserveRegistryUseCase = mockk(relaxed = true)
    private val observeItems: ObserveItemsUseCase = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val reserveItemUseCase: ReserveItemUseCase = mockk()
    private val confirmPurchaseUseCase: ConfirmPurchaseUseCase = mockk()
    private val guestPrefs: GuestPreferencesRepository = mockk(relaxed = true)

    private fun fakeRegistry(ownerId: String = "owner-1") = Registry(
        id = "reg-1",
        ownerId = ownerId,
        title = "Test",
        occasion = "Birthday",
    )

    private fun fakeGuest() = GuestUser(
        firstName = "Ana",
        lastName = "Pop",
        email = "ana@example.com",
    )

    private fun fakeUser(uid: String) = User(
        uid = uid,
        email = null,
        displayName = null,
        isAnonymous = false,
    )

    /** Wires every constructor param. observeRegistry / observeItems / authState
     *  must be stubbed in the test body BEFORE calling vm() so the StateFlow
     *  Eagerly-init reads them on construction. */
    private fun vm(): EditItemViewModel {
        val ssh = SavedStateHandle(
            mapOf("registryId" to "reg-1", "itemId" to "item-1"),
        )
        return EditItemViewModel(
            savedStateHandle = ssh,
            updateItem = mockk<UpdateItemUseCase>(relaxed = true),
            observeItems = observeItems,
            fetchOgMetadata = mockk<FetchOgMetadataUseCase>(relaxed = true),
            authRepository = authRepository,
            observeRegistry = observeRegistry,
            reserveItemUseCase = reserveItemUseCase,
            confirmPurchaseUseCase = confirmPurchaseUseCase,
            guestPreferencesRepository = guestPrefs,
        )
    }

    @Test
    fun `onReserveClicked success persists activeReservationId and emits OpenRetailer`() = runTest {
        every { observeRegistry("reg-1") } returns flowOf(fakeRegistry())
        every { observeItems("reg-1") } returns flowOf(emptyList())
        every { authRepository.authState } returns flowOf(fakeUser(uid = "user-2"))
        every { authRepository.currentUser } returns fakeUser(uid = "user-2")
        coEvery { guestPrefs.getGuestIdentity() } returns fakeGuest()
        coEvery {
            reserveItemUseCase("reg-1", "item-1", fakeGuest(), giverId = "user-2")
        } returns Result.success(
            ReservationResult(
                reservationId = "res-1",
                affiliateUrl = "https://aff.example/abc",
                expiresAtMs = 12345L,
            ),
        )

        val viewModel = vm()
        advanceUntilIdle()

        viewModel.reservationEvents.test {
            viewModel.onReserveClicked("item-1")
            advanceUntilIdle()

            val event = awaitItem()
            assertEquals(
                EditItemViewModel.ReservationEvent.OpenRetailer("https://aff.example/abc"),
                event,
            )
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) {
            reserveItemUseCase("reg-1", "item-1", fakeGuest(), giverId = "user-2")
        }
        coVerify(exactly = 1) { guestPrefs.setActiveReservationId("res-1") }
    }

    @Test
    fun `onReserveClicked with no guest identity emits ShowGuestSheet and skips reserve`() = runTest {
        every { observeRegistry("reg-1") } returns flowOf(fakeRegistry())
        every { observeItems("reg-1") } returns flowOf(emptyList())
        every { authRepository.authState } returns flowOf(fakeUser(uid = "user-2"))
        every { authRepository.currentUser } returns fakeUser(uid = "user-2")
        coEvery { guestPrefs.getGuestIdentity() } returns null

        val viewModel = vm()
        advanceUntilIdle()

        viewModel.reservationEvents.test {
            viewModel.onReserveClicked("item-1")
            advanceUntilIdle()

            val event = awaitItem()
            assertEquals(EditItemViewModel.ReservationEvent.ShowGuestSheet, event)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) {
            reserveItemUseCase(any(), any(), any(), any())
        }
    }

    @Test
    fun `onReserveClicked failure emits ShowConflictError and does not persist reservation id`() = runTest {
        every { observeRegistry("reg-1") } returns flowOf(fakeRegistry())
        every { observeItems("reg-1") } returns flowOf(emptyList())
        every { authRepository.authState } returns flowOf(fakeUser(uid = "user-2"))
        every { authRepository.currentUser } returns fakeUser(uid = "user-2")
        coEvery { guestPrefs.getGuestIdentity() } returns fakeGuest()
        coEvery {
            reserveItemUseCase("reg-1", "item-1", fakeGuest(), giverId = "user-2")
        } returns Result.failure(RuntimeException("ITEM_UNAVAILABLE"))

        val viewModel = vm()
        advanceUntilIdle()

        viewModel.reservationEvents.test {
            viewModel.onReserveClicked("item-1")
            advanceUntilIdle()

            val event = awaitItem()
            assertEquals(
                EditItemViewModel.ReservationEvent.ShowConflictError("ITEM_UNAVAILABLE"),
                event,
            )
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { guestPrefs.setActiveReservationId(any()) }
    }

    @Test
    fun `onConfirmPurchase success emits success resId and clears activeReservationId`() = runTest {
        every { observeRegistry("reg-1") } returns flowOf(fakeRegistry())
        every { observeItems("reg-1") } returns flowOf(emptyList())
        every { authRepository.authState } returns flowOf(fakeUser(uid = "user-2"))
        coEvery { confirmPurchaseUseCase("res-1") } returns Result.success(Unit)

        val viewModel = vm()
        advanceUntilIdle()

        viewModel.snackbarMessages.test {
            viewModel.onConfirmPurchase("res-1")
            advanceUntilIdle()

            val resId = awaitItem()
            assertEquals(R.string.reservation_confirm_purchase_success, resId)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { confirmPurchaseUseCase("res-1") }
        coVerify(exactly = 1) { guestPrefs.setActiveReservationId(null) }
    }

    @Test
    fun `onConfirmPurchase failure emits error resId and does not clear activeReservationId`() = runTest {
        every { observeRegistry("reg-1") } returns flowOf(fakeRegistry())
        every { observeItems("reg-1") } returns flowOf(emptyList())
        every { authRepository.authState } returns flowOf(fakeUser(uid = "user-2"))
        coEvery { confirmPurchaseUseCase("res-1") } returns Result.failure(RuntimeException("RESERVATION_EXPIRED"))

        val viewModel = vm()
        advanceUntilIdle()

        viewModel.snackbarMessages.test {
            viewModel.onConfirmPurchase("res-1")
            advanceUntilIdle()

            val resId = awaitItem()
            assertEquals(R.string.reservation_confirm_purchase_error, resId)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 0) { guestPrefs.setActiveReservationId(null) }
    }
}
