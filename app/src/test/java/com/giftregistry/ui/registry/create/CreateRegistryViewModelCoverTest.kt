package com.giftregistry.ui.registry.create

import android.net.Uri
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
import com.giftregistry.ui.registry.cover.CoverPhotoSelection
import com.giftregistry.ui.registry.cover.PresetCatalog
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/**
 * Phase 12 — RED → GREEN tests for the cover-photo upload + save contract
 * inside [CreateRegistryViewModel].
 *
 * Pins:
 * - D-07 / Pitfall 2: `uploadCover` MUST suspend-complete BEFORE
 *   `createRegistryUseCase.invoke(...)`. The `coVerifyOrder { ... }` block
 *   below names the upload first; the VM implementation must satisfy this
 *   contract by ordering the suspend calls inside `onSave()` (Plan 04).
 * - D-07 failure path: when `uploadCover` returns `Result.failure`, the VM
 *   must surface the error AND keep `savedRegistryId` null (no navigation
 *   to a registry that has no cover) AND clear `isSaving`.
 * - D-02 / D-05: Preset selection encodes via `PresetCatalog.encode` and
 *   never invokes `uploadCover` (only Gallery selections upload).
 * - D-14: None selection emits null imageUrl.
 * - D-11: occasion change clears any picked Preset (Wedding registry must
 *   never display a Birthday preset).
 *
 * Test method names mirror 12-RESEARCH.md Validation Architecture. Plan 04
 * keeps the contract assertions (`coVerifyOrder`, failure assertions) verbatim;
 * the only change vs. Wave 0 is removing the `fail("Wave 0 stub")` markers and
 * actually instantiating the VM so the contract is exercised end-to-end.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CreateRegistryViewModelCoverTest {

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
     * D-07 + Pitfall 2 — uploadCover MUST run BEFORE createRegistryUseCase.
     *
     * In the Gallery selection branch of `onSave()`, the download URL must
     * roundtrip via `storageRepository.uploadCover(...)` BEFORE the registry
     * document is written via `createRegistryUseCase(...)`. `coVerifyOrder`
     * pins the call sequence. If the VM reverses the order (writes the doc
     * first with `imageUrl=null`, then uploads), this test fails — the
     * navigation race in Pitfall 2 would leak.
     */
    @Test
    fun onSave_uploadsBeforeSavingRegistry() = runTest {
        val storageRepository = mockk<StorageRepository>(relaxed = true)
        val coverImageProcessor = mockk<CoverImageProcessor>(relaxed = true)
        val createRegistryUseCase = mockk<CreateRegistryUseCase>(relaxed = true)
        val uri = mockk<Uri>(relaxed = true)
        val capturedRegistry = slot<Registry>()

        coEvery { coverImageProcessor.compress(uri) } returns ByteArray(1024)
        coEvery { storageRepository.uploadCover(any(), any(), any()) } returns
            Result.success("https://download/x.jpg")
        coEvery { createRegistryUseCase.invoke(capture(capturedRegistry)) } returns
            Result.success("reg-1")

        val vm = buildViewModel(
            storageRepository = storageRepository,
            coverImageProcessor = coverImageProcessor,
            createRegistryUseCase = createRegistryUseCase,
        )
        vm.title.value = "Wedding bash"
        vm.occasion.value = "Wedding"
        vm.coverPhotoSelection.value = CoverPhotoSelection.Gallery(uri)
        vm.onSave()

        // CONTRACT (the VM must satisfy):
        //   1. processor.compress(uri)
        //   2. storageRepository.uploadCover(uid, registryId, jpegBytes)
        //   3. createRegistryUseCase(registry.copy(imageUrl = downloadUrl))
        coVerifyOrder {
            storageRepository.uploadCover(any(), any(), any())
            createRegistryUseCase.invoke(any())
        }
        assertEquals(
            "D-07: createRegistry must receive the download URL on imageUrl",
            "https://download/x.jpg",
            capturedRegistry.captured.imageUrl,
        )
    }

    /**
     * D-07 — failure path: uploadCover returns Result.failure → error surfaces,
     * savedRegistryId stays null, isSaving clears, NO Firestore write.
     */
    @Test
    fun uploadFailure_surfacesError_no_navigation() = runTest {
        val storageRepository = mockk<StorageRepository>(relaxed = true)
        val coverImageProcessor = mockk<CoverImageProcessor>(relaxed = true)
        val createRegistryUseCase = mockk<CreateRegistryUseCase>(relaxed = true)
        val uri = mockk<Uri>(relaxed = true)

        coEvery { coverImageProcessor.compress(uri) } returns ByteArray(1024)
        coEvery { storageRepository.uploadCover(any(), any(), any()) } returns
            Result.failure(RuntimeException("Network error"))

        val vm = buildViewModel(
            storageRepository = storageRepository,
            coverImageProcessor = coverImageProcessor,
            createRegistryUseCase = createRegistryUseCase,
        )
        vm.title.value = "Wedding bash"
        vm.occasion.value = "Wedding"
        vm.coverPhotoSelection.value = CoverPhotoSelection.Gallery(uri)
        vm.onSave()

        // CONTRACT (after upload fails):
        //   - error.value != null
        //   - savedRegistryId.value == null (no navigation)
        //   - isSaving.value == false (button re-enabled for retry)
        //   - createRegistryUseCase NEVER called
        coVerify(exactly = 0) { createRegistryUseCase.invoke(any()) }
        assertNotNull("D-07: error.value must be non-null after upload failure", vm.error.value)
        assertNull("D-07: savedRegistryId must stay null on failure (no navigation)", vm.savedRegistryId.value)
        assertEquals("D-07: isSaving must clear on failure", false, vm.isSaving.value)
    }

    /**
     * D-02 / D-05 — Preset selection skips upload and encodes as
     * `preset:{occasion}:{index}` sentinel via PresetCatalog.encode.
     */
    @Test
    fun presetSelection_skipsUpload_encodesAsString() = runTest {
        val storageRepository = mockk<StorageRepository>(relaxed = true)
        val createRegistryUseCase = mockk<CreateRegistryUseCase>(relaxed = true)
        val capturedRegistry = slot<Registry>()
        coEvery { createRegistryUseCase.invoke(capture(capturedRegistry)) } returns
            Result.success("reg-1")

        // Selection that bypasses upload entirely (D-02 — bundled drawable).
        val selection: CoverPhotoSelection = CoverPhotoSelection.Preset("Wedding", 3)
        // PresetCatalog.encode is final-shape; resolve happens client-side via Coil.
        val expectedSentinel = PresetCatalog.encode(
            (selection as CoverPhotoSelection.Preset).occasion,
            selection.index,
        )
        assertEquals("preset:Wedding:3", expectedSentinel)

        val vm = buildViewModel(
            storageRepository = storageRepository,
            createRegistryUseCase = createRegistryUseCase,
        )
        vm.title.value = "Wedding bash"
        vm.occasion.value = "Wedding"
        vm.coverPhotoSelection.value = selection
        vm.onSave()

        // CONTRACT:
        //   - storageRepository.uploadCover NEVER called
        //   - capturedRegistry.imageUrl == "preset:Wedding:3"
        coVerify(exactly = 0) { storageRepository.uploadCover(any(), any(), any()) }
        assertEquals(
            "D-02 / D-05: Preset selection must encode to canonical sentinel string",
            "preset:Wedding:3",
            capturedRegistry.captured.imageUrl,
        )
    }

    /** D-14 — None selection emits null imageUrl AND never invokes uploadCover. */
    @Test
    fun noneSelection_emitsNullImageUrl() = runTest {
        val storageRepository = mockk<StorageRepository>(relaxed = true)
        val createRegistryUseCase = mockk<CreateRegistryUseCase>(relaxed = true)
        val capturedRegistry = slot<Registry>()
        coEvery { createRegistryUseCase.invoke(capture(capturedRegistry)) } returns
            Result.success("reg-1")

        val vm = buildViewModel(
            storageRepository = storageRepository,
            createRegistryUseCase = createRegistryUseCase,
        )
        vm.title.value = "No-cover registry"
        vm.occasion.value = "Wedding"
        vm.coverPhotoSelection.value = CoverPhotoSelection.None
        vm.onSave()

        // CONTRACT:
        //   - capturedRegistry.imageUrl == null
        //   - storageRepository.uploadCover NEVER called
        coVerify(exactly = 0) { storageRepository.uploadCover(any(), any(), any()) }
        assertNull(
            "D-14: None selection must emit null imageUrl (placeholder branch)",
            capturedRegistry.captured.imageUrl,
        )
    }

    /**
     * D-11 — occasion change clears Preset selection.
     *
     * Currently selected: Preset("Wedding", 3). User switches occasion to
     * "Birthday" — coverPhotoSelection.value must reset to None so a Wedding
     * preset never displays on a Birthday registry.
     *
     * The VM has an `init { occasion.collect { ... } }` collector that fires
     * `coverPhotoSelection.value = None` when the picked Preset's occasion
     * no longer matches.
     */
    @Test
    fun occasionChange_clearsPresetSelection() = runTest {
        val vm = buildViewModel()
        vm.occasion.value = "Wedding"
        vm.coverPhotoSelection.value = CoverPhotoSelection.Preset("Wedding", 3)

        vm.occasion.value = "Birthday"

        // CONTRACT: switching occasion to a different value must reset the
        // picked preset to None (Wedding preset must never show on Birthday).
        assertEquals(
            "D-11: switching occasion must reset coverPhotoSelection to None",
            CoverPhotoSelection.None,
            vm.coverPhotoSelection.value,
        )
    }
}
