package com.giftregistry.ui.registry.create

import android.net.Uri
import com.giftregistry.data.storage.CoverImageProcessor
import com.giftregistry.domain.model.Registry
import com.giftregistry.domain.storage.StorageRepository
import com.giftregistry.domain.usecase.CreateRegistryUseCase
import com.giftregistry.ui.registry.cover.CoverPhotoSelection
import com.giftregistry.ui.registry.cover.PresetCatalog
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Test

/**
 * Phase 12 — Wave 0 RED tests for the cover-photo upload + save contract
 * inside [CreateRegistryViewModel].
 *
 * Pins:
 * - D-07 / Pitfall 2: `uploadCover` MUST suspend-complete BEFORE
 *   `createRegistryUseCase.invoke(...)`. The `coVerifyOrder { ... }` block
 *   below names the upload first; Plan 04 must satisfy this contract by
 *   changing the ViewModel implementation order. **DOWNSTREAM PLANS MUST
 *   NEVER EDIT THIS TEST — flip RED → GREEN by changing the VM only.**
 * - D-07 failure path: when `uploadCover` returns `Result.failure`, the VM
 *   must surface the error AND keep `savedRegistryId` null (no navigation
 *   to a registry that has no cover) AND clear `isSaving`.
 * - D-02 / D-05: Preset selection encodes via `PresetCatalog.encode` and
 *   never invokes `uploadCover` (only Gallery selections upload).
 * - D-14: None selection emits null imageUrl.
 * - D-11: occasion change clears any picked Preset (Wedding registry must
 *   never display a Birthday preset).
 *
 * Wave 0 RED strategy: each test orchestrates the contract via mocked
 * dependencies + a `fail("Wave 0 stub — Plan 04 wires CreateRegistryViewModel")`
 * marker. The marker assertion fails until Plan 04 ships the real onSave
 * orchestration. Test method names mirror 12-RESEARCH.md Validation Architecture.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CreateRegistryViewModelCoverTest {

    /**
     * D-07 + Pitfall 2 — uploadCover MUST run BEFORE createRegistryUseCase.
     *
     * Plan 04 contract: in the Gallery selection branch of `onSave()`, the
     * download URL must roundtrip via `storageRepository.uploadCover(...)`
     * BEFORE the registry document is written via `createRegistryUseCase(...)`.
     * `coVerifyOrder` below pins the call sequence. If Plan 04 reverses the
     * order (writes the doc first with `imageUrl=null`, then uploads), this
     * test stays RED — the navigation race in Pitfall 2 would leak.
     */
    @Test
    fun onSave_uploadsBeforeSavingRegistry() = runTest {
        val storageRepository = mockk<StorageRepository>(relaxed = true)
        val coverImageProcessor = mockk<CoverImageProcessor>(relaxed = true)
        val createRegistryUseCase = mockk<CreateRegistryUseCase>(relaxed = true)
        val uri = mockk<Uri>(relaxed = true)

        coEvery { coverImageProcessor.compress(uri) } returns ByteArray(1024)
        coEvery { storageRepository.uploadCover(any(), any(), any()) } returns
            Result.success("https://download/x.jpg")
        coEvery { createRegistryUseCase.invoke(any()) } returns Result.success("reg-1")

        // CONTRACT (Plan 04 must satisfy):
        //   1. processor.compress(uri)
        //   2. storageRepository.uploadCover(uid, registryId, jpegBytes)
        //   3. createRegistryUseCase(registry.copy(imageUrl = downloadUrl))
        // Wave 0: no VM exists yet — fail until Plan 04 ships the orchestration.
        coVerifyOrder {
            storageRepository.uploadCover(any(), any(), any())
            createRegistryUseCase.invoke(any())
        }

        // Wave 0 RED marker — Plan 04 deletes this line when orchestration ships.
        fail("Wave 0 stub — Plan 04 must wire CreateRegistryViewModel.onSave to call uploadCover BEFORE createRegistryUseCase per D-07 + Pitfall 2")
    }

    /**
     * D-07 — failure path: uploadCover returns Result.failure → error surfaces,
     * savedRegistryId stays null, isSaving clears.
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

        // Wave 0 RED — simulated VM state placeholder. Plan 04 wires the real
        // error.value / savedRegistryId.value / isSaving.value flows.
        val error = MutableStateFlow<String?>(null)
        val savedRegistryId = MutableStateFlow<String?>(null)
        val isSaving = MutableStateFlow(false)

        // CONTRACT (Plan 04 must satisfy after upload fails):
        //   - error.value != null
        //   - savedRegistryId.value == null (no navigation)
        //   - isSaving.value == false (button re-enabled for retry)
        //   - createRegistryUseCase NEVER called
        coVerify(exactly = 0) { createRegistryUseCase.invoke(any()) }

        assertNotNull("D-07: error.value must be non-null after upload failure", error.value)
        assertNull("D-07: savedRegistryId must stay null on failure (no navigation)", savedRegistryId.value)
        assertEquals("D-07: isSaving must clear on failure", false, isSaving.value)
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

        // Plan 04 contract:
        //   - storageRepository.uploadCover NEVER called
        //   - capturedRegistry.imageUrl == "preset:Wedding:3"
        coVerify(exactly = 0) { storageRepository.uploadCover(any(), any(), any()) }

        // Wave 0 RED — VM doesn't yet capture the registry; mark fail.
        fail("Wave 0 stub — Plan 04 must wire onSave to call createRegistryUseCase with imageUrl=PresetCatalog.encode(...) for Preset selections, skipping uploadCover")
    }

    /** D-14 — None selection emits null imageUrl AND never invokes uploadCover. */
    @Test
    fun noneSelection_emitsNullImageUrl() = runTest {
        val storageRepository = mockk<StorageRepository>(relaxed = true)
        val createRegistryUseCase = mockk<CreateRegistryUseCase>(relaxed = true)
        val capturedRegistry = slot<Registry>()
        coEvery { createRegistryUseCase.invoke(capture(capturedRegistry)) } returns
            Result.success("reg-1")

        val selection: CoverPhotoSelection = CoverPhotoSelection.None
        assertEquals(CoverPhotoSelection.None, selection)

        // Plan 04 contract:
        //   - capturedRegistry.imageUrl == null
        //   - storageRepository.uploadCover NEVER called
        coVerify(exactly = 0) { storageRepository.uploadCover(any(), any(), any()) }

        fail("Wave 0 stub — Plan 04 must wire onSave to pass imageUrl=null for None selection")
    }

    /**
     * D-11 — occasion change clears Preset selection.
     *
     * Currently selected: Preset("Wedding", 3). User switches occasion to
     * "Birthday" — coverPhotoSelection.value must reset to None so a Wedding
     * preset never displays on a Birthday registry.
     *
     * Plan 04 contract: an `init { … }` collector on `occasion` flow that
     * fires `coverPhotoSelection.value = None` when the picked Preset's
     * occasion no longer matches.
     */
    @Test
    fun occasionChange_clearsPresetSelection() = runTest {
        // Wave 0 — VM doesn't expose `coverPhotoSelection` MutableStateFlow yet;
        // simulate the future contract via a local flow.
        val occasion = MutableStateFlow("Wedding")
        val coverPhotoSelection = MutableStateFlow<CoverPhotoSelection>(
            CoverPhotoSelection.Preset("Wedding", 3),
        )

        // Plan 04 must add this collector inside CreateRegistryViewModel.init { ... }
        // For Wave 0 we just describe the contract.
        occasion.value = "Birthday"

        // CONTRACT (Plan 04 must satisfy):
        //   coverPhotoSelection.value == CoverPhotoSelection.None after occasion flip.
        // Wave 0 — no collector exists, so the value stays as Preset.
        // We assert the FUTURE contract here so the test fails RED:
        assertEquals(
            "D-11: switching occasion must reset coverPhotoSelection to None " +
                "(Wave 0 RED — Plan 04 wires the init { occasion.collect { ... } } block)",
            CoverPhotoSelection.None,
            coverPhotoSelection.value,
        )
    }
}
