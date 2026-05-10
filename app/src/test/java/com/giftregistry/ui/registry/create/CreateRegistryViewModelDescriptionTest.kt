package com.giftregistry.ui.registry.create

import androidx.lifecycle.SavedStateHandle
import com.giftregistry.MainDispatcherRule
import com.giftregistry.data.storage.CoverImageProcessor
import com.giftregistry.domain.auth.AuthRepository
import com.giftregistry.domain.model.Registry
import com.giftregistry.domain.model.User
import com.giftregistry.domain.registry.RegistryRepository
import com.giftregistry.domain.storage.StorageRepository
import com.giftregistry.domain.usecase.CreateRegistryUseCase
import com.giftregistry.domain.usecase.ObserveRegistryUseCase
import com.giftregistry.domain.usecase.UpdateRegistryUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/**
 * quick-260510-noi — RED → GREEN regression pins for the Description field
 * round-trip on [CreateRegistryViewModel].
 *
 * The VM-side wiring (description StateFlow, edit-mode hydration, and
 * `description.value.ifBlank { null }` on save) was already in place from
 * earlier phases. These tests pin the existing contract so future refactors
 * cannot silently break it now that the field is finally surfaced in the UI.
 *
 * Pins:
 * - QUICK-NOI-01a: typed description round-trips through onSave() into the
 *   Registry passed to CreateRegistryUseCase.
 * - QUICK-NOI-01b: blank description → null on save (existing ifBlank contract).
 * - QUICK-NOI-01c: edit-mode hydration of description from observed Registry.
 *
 * Mirrors the 8-arg constructor + SavedStateHandle setup from
 * [CreateRegistryViewModelEventTimeTest].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CreateRegistryViewModelDescriptionTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun buildViewModel(
        authRepository: AuthRepository = mockk(relaxed = true) {
            every { currentUser } returns User(uid = "uid-1", email = null, displayName = null, isAnonymous = false)
        },
        createRegistryUseCase: CreateRegistryUseCase = mockk(relaxed = true),
        updateRegistryUseCase: UpdateRegistryUseCase = mockk(relaxed = true),
        observeRegistryUseCase: ObserveRegistryUseCase = mockk(relaxed = true),
        registryRepository: RegistryRepository = mockk(relaxed = true) {
            every { newRegistryId() } returns "minted-id-1"
        },
        storageRepository: StorageRepository = mockk(relaxed = true),
        coverImageProcessor: CoverImageProcessor = mockk(relaxed = true),
        registryId: String? = null,
    ): CreateRegistryViewModel {
        val savedStateHandle = SavedStateHandle().apply {
            if (registryId != null) set("registryId", registryId)
        }
        return CreateRegistryViewModel(
            authRepository = authRepository,
            createRegistryUseCase = createRegistryUseCase,
            updateRegistryUseCase = updateRegistryUseCase,
            observeRegistryUseCase = observeRegistryUseCase,
            registryRepository = registryRepository,
            storageRepository = storageRepository,
            coverImageProcessor = coverImageProcessor,
            savedStateHandle = savedStateHandle,
        )
    }

    /**
     * QUICK-NOI-01a — typed description must reach CreateRegistryUseCase verbatim.
     * Captures the Registry passed to the use case via a mockk slot.
     */
    @Test
    fun `onSave passes typed description through to CreateRegistryUseCase`() = runTest {
        val captured = slot<Registry>()
        val createUseCase = mockk<CreateRegistryUseCase>(relaxed = true)
        coEvery { createUseCase.invoke(capture(captured)) } returns Result.success("new-id")

        val vm = buildViewModel(createRegistryUseCase = createUseCase)
        vm.title.value = "My Wedding"
        vm.occasion.value = "Wedding"
        vm.description.value = "Sunday brunch at Grand Hotel"

        vm.onSave()
        advanceUntilIdle()

        assertEquals(
            "Typed description must round-trip into the Registry passed to CreateRegistryUseCase",
            "Sunday brunch at Grand Hotel",
            captured.captured.description,
        )
    }

    /**
     * QUICK-NOI-01b — blank description must map to null on save (ifBlank contract).
     * This is the pre-existing VM behaviour; the test pins it so a future
     * refactor cannot silently start writing empty strings to Firestore.
     */
    @Test
    fun `onSave maps blank description to null`() = runTest {
        val captured = slot<Registry>()
        val createUseCase = mockk<CreateRegistryUseCase>(relaxed = true)
        coEvery { createUseCase.invoke(capture(captured)) } returns Result.success("new-id")

        val vm = buildViewModel(createRegistryUseCase = createUseCase)
        vm.title.value = "Plain registry"
        vm.occasion.value = "Birthday"
        // description left at default ""

        vm.onSave()
        advanceUntilIdle()

        assertNull(
            "Blank description must map to null in the persisted Registry",
            captured.captured.description,
        )
    }

    /**
     * QUICK-NOI-01c — edit-mode hydration: when ObserveRegistryUseCase emits a
     * Registry with description=non-null, the VM must populate description.value
     * with that string so the OutlinedTextField renders it on first paint.
     */
    @Test
    fun `edit mode hydrates description from observed registry`() = runTest {
        val storedRegistry = Registry(
            id = "reg-1",
            ownerId = "uid-1",
            title = "Existing wedding",
            occasion = "Wedding",
            description = "Existing note",
        )
        val observeRegistryUseCase = mockk<ObserveRegistryUseCase>(relaxed = true)
        coEvery { observeRegistryUseCase.invoke("reg-1") } returns flowOf(storedRegistry)

        val vm = buildViewModel(
            observeRegistryUseCase = observeRegistryUseCase,
            registryId = "reg-1",
        )
        advanceUntilIdle() // drain init { viewModelScope.launch { ... } }

        assertEquals(
            "Edit-mode hydration must populate description.value from the observed Registry",
            "Existing note",
            vm.description.value,
        )
    }
}
