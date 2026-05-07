package com.giftregistry.ui.registry.detail

import androidx.lifecycle.SavedStateHandle
import com.giftregistry.MainDispatcherRule
import com.giftregistry.domain.auth.AuthRepository
import com.giftregistry.domain.model.Registry
import com.giftregistry.domain.model.User
import com.giftregistry.domain.preferences.GuestPreferencesRepository
import com.giftregistry.domain.usecase.ConfirmPurchaseUseCase
import com.giftregistry.domain.usecase.DeleteItemUseCase
import com.giftregistry.domain.usecase.DeleteRegistryUseCase
import com.giftregistry.domain.usecase.ObserveItemsUseCase
import com.giftregistry.domain.usecase.ObserveRegistryUseCase
import com.giftregistry.domain.usecase.ReserveItemUseCase
import com.giftregistry.ui.notifications.NotificationBus
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Quick task 260507-uzv — pins the `isOwner` StateFlow contract on
 * [RegistryDetailViewModel]. The flow drives both the existing D-13
 * cover-photo tap target AND the QUICK-260507-UZV overflow-menu gate
 * (kebab + DropdownMenu hidden when uid != registry.ownerId).
 *
 * Mirrors the canonical server-side ownership check at
 * `functions/src/registry/inviteToRegistry.ts:50` —
 * `registryData.ownerId !== request.auth.uid`.
 *
 * Defaults to `false` while loading (Eagerly initial value + .catch { emit(false) })
 * so non-owner UI is the safe default until the first emission resolves —
 * no flash of owner-only items.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RegistryDetailViewModelIsOwnerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val observeRegistry: ObserveRegistryUseCase = mockk(relaxed = true)
    private val observeItems: ObserveItemsUseCase = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk(relaxed = true)

    private fun fakeRegistry(ownerId: String) = Registry(
        id = "reg-1",
        ownerId = ownerId,
        title = "Test Registry",
        occasion = "Birthday",
    )

    private fun fakeUser(uid: String) = User(
        uid = uid,
        email = null,
        displayName = null,
        isAnonymous = false,
    )

    private fun vm(): RegistryDetailViewModel {
        val ssh = SavedStateHandle(mapOf("registryId" to "reg-1"))
        return RegistryDetailViewModel(
            observeRegistryUseCase = observeRegistry,
            observeItemsUseCase = observeItems,
            deleteRegistryUseCase = mockk<DeleteRegistryUseCase>(relaxed = true),
            deleteItemUseCase = mockk<DeleteItemUseCase>(relaxed = true),
            reserveItemUseCase = mockk<ReserveItemUseCase>(relaxed = true),
            guestPreferencesRepository = mockk<GuestPreferencesRepository>(relaxed = true),
            deepLinkBus = ReservationDeepLinkBus(),
            confirmPurchaseUseCase = mockk<ConfirmPurchaseUseCase>(relaxed = true),
            notificationBus = NotificationBus(),
            authRepository = authRepository,
            updateRegistryUseCase = mockk(relaxed = true),
            storageRepository = mockk(relaxed = true),
            coverImageProcessor = mockk(relaxed = true),
            savedStateHandle = ssh,
        )
    }

    @Test
    fun `isOwner is true when registry ownerId matches auth uid`() = runTest {
        every { observeRegistry("reg-1") } returns flowOf(fakeRegistry(ownerId = "user-1"))
        every { observeItems("reg-1") } returns flowOf(emptyList())
        every { authRepository.authState } returns flowOf(fakeUser(uid = "user-1"))

        val viewModel = vm()
        advanceUntilIdle()

        assertTrue(
            "Owner viewing their own registry must see isOwner == true",
            viewModel.isOwner.value,
        )
    }

    @Test
    fun `isOwner is false when registry ownerId differs from auth uid`() = runTest {
        // Invitee case — the bug being fixed by QUICK-260507-UZV. A different
        // signed-in user opens a registry they do not own; isOwner must be false
        // so the overflow menu and kebab disappear.
        every { observeRegistry("reg-1") } returns flowOf(fakeRegistry(ownerId = "user-1"))
        every { observeItems("reg-1") } returns flowOf(emptyList())
        every { authRepository.authState } returns flowOf(fakeUser(uid = "user-2"))

        val viewModel = vm()
        advanceUntilIdle()

        assertFalse(
            "Non-owner (uid != ownerId) must see isOwner == false",
            viewModel.isOwner.value,
        )
    }

    @Test
    fun `isOwner is false when registry is null`() = runTest {
        // Loading or not-found — must default to false so owner-only UI never
        // flashes during the initial load before the first Firestore snapshot.
        every { observeRegistry("reg-1") } returns flowOf(null)
        every { observeItems("reg-1") } returns flowOf(emptyList())
        every { authRepository.authState } returns flowOf(fakeUser(uid = "user-1"))

        val viewModel = vm()
        advanceUntilIdle()

        assertFalse(
            "Null registry (loading / not-found) must yield isOwner == false",
            viewModel.isOwner.value,
        )
    }

    @Test
    fun `isOwner is false when authState is null`() = runTest {
        // Signed-out / unauthenticated viewer (e.g. guest from a share link in
        // the Android app context) — even if the registry loads, no user UID to
        // compare against, so isOwner must be false.
        every { observeRegistry("reg-1") } returns flowOf(fakeRegistry(ownerId = "user-1"))
        every { observeItems("reg-1") } returns flowOf(emptyList())
        every { authRepository.authState } returns flowOf(null)

        val viewModel = vm()
        advanceUntilIdle()

        assertFalse(
            "Null auth user (signed-out) must yield isOwner == false",
            viewModel.isOwner.value,
        )
    }
}
