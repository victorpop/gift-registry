package com.giftregistry.ui.item.edit

import androidx.lifecycle.SavedStateHandle
import com.giftregistry.MainDispatcherRule
import com.giftregistry.domain.auth.AuthRepository
import com.giftregistry.domain.auth.AuthStateEvent
import com.giftregistry.domain.model.Registry
import com.giftregistry.domain.model.User
import com.giftregistry.domain.preferences.GuestPreferencesRepository
import com.giftregistry.domain.usecase.ConfirmPurchaseUseCase
import com.giftregistry.domain.usecase.FetchOgMetadataUseCase
import com.giftregistry.domain.usecase.ObserveItemsUseCase
import com.giftregistry.domain.usecase.ObserveRegistryUseCase
import com.giftregistry.domain.usecase.ReserveItemUseCase
import com.giftregistry.domain.usecase.UpdateItemUseCase
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
 * Quick task 260507-vrp — pins the `isOwner` StateFlow contract on
 * [EditItemViewModel]. Mirrors [com.giftregistry.ui.registry.detail.RegistryDetailViewModelIsOwnerTest]
 * verbatim — same 4 cases, same Eagerly + initial-false default, same
 * `.catch { emit(false) }` safety net.
 *
 * The flow drives the dual-mode UI on EditItemScreen: owner mode = full
 * edit form (Save / Delete reachable from per-item kebab on the Detail
 * row); invitee mode = read-only fields + Reserve / Mark-as-purchased
 * actions reusing the same use cases the giver flow already uses on
 * RegistryDetailScreen.
 *
 * Defaults to `false` while loading so owner-only edit affordances never
 * flash for an invitee — no Save button glimpse during the brief load
 * before the first ownership emission resolves.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EditItemViewModelIsOwnerTest {

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
            reserveItemUseCase = mockk<ReserveItemUseCase>(relaxed = true),
            confirmPurchaseUseCase = mockk<ConfirmPurchaseUseCase>(relaxed = true),
            guestPreferencesRepository = mockk<GuestPreferencesRepository>(relaxed = true),
        )
    }

    @Test
    fun `isOwner is true when registry ownerId matches auth uid`() = runTest {
        every { observeRegistry("reg-1") } returns flowOf(fakeRegistry(ownerId = "user-1"))
        every { observeItems("reg-1") } returns flowOf(emptyList())
        every { authRepository.authState } returns flowOf(AuthStateEvent.Changed(fakeUser(uid = "user-1")))

        val viewModel = vm()
        advanceUntilIdle()

        assertTrue(
            "Owner viewing their own registry must see isOwner == true",
            viewModel.isOwner.value,
        )
    }

    @Test
    fun `isOwner is false when registry ownerId differs from auth uid`() = runTest {
        // Invitee case — the bug being fixed by quick-260507-vrp. A different
        // signed-in user opens an item on a registry they do not own; isOwner
        // must be false so the form fields render read-only and the Save +
        // Delete affordances disappear.
        every { observeRegistry("reg-1") } returns flowOf(fakeRegistry(ownerId = "user-1"))
        every { observeItems("reg-1") } returns flowOf(emptyList())
        every { authRepository.authState } returns flowOf(AuthStateEvent.Changed(fakeUser(uid = "user-2")))

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
        every { authRepository.authState } returns flowOf(AuthStateEvent.Changed(fakeUser(uid = "user-1")))

        val viewModel = vm()
        advanceUntilIdle()

        assertFalse(
            "Null registry (loading / not-found) must yield isOwner == false",
            viewModel.isOwner.value,
        )
    }

    @Test
    fun `isOwner is false when authState is null`() = runTest {
        // Signed-out / unauthenticated viewer — even if the registry loads,
        // no user UID to compare against, so isOwner must be false.
        every { observeRegistry("reg-1") } returns flowOf(fakeRegistry(ownerId = "user-1"))
        every { observeItems("reg-1") } returns flowOf(emptyList())
        every { authRepository.authState } returns flowOf(AuthStateEvent.Changed(null))

        val viewModel = vm()
        advanceUntilIdle()

        assertFalse(
            "Null auth user (signed-out) must yield isOwner == false",
            viewModel.isOwner.value,
        )
    }
}
