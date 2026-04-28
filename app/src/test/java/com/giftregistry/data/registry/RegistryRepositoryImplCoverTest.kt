package com.giftregistry.data.registry

import com.giftregistry.data.model.RegistryDto
import com.giftregistry.domain.model.Registry
import com.google.firebase.functions.FirebaseFunctions
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Phase 12 — Wave 0 RED tests for the imageUrl roundtrip in
 * [RegistryRepositoryImpl] (Pitfall 1) + the `newRegistryId()` helper
 * required for D-07 upload-then-write.
 *
 * Pitfall 1 background: today the private `Registry.toMap()` (line 75-82)
 * and `Registry.toUpdateMap()` (line 84-89) hardcode the field set and
 * SILENTLY DROP `imageUrl`. Even a fully wired upload path would never
 * persist the cover URL. RegistryDto is also missing the field, so reads
 * never populate it from Firestore. Plan 02 fixes both files.
 *
 * Wave 0 RED: these tests fail because:
 *  - createRegistry/updateRegistry capture maps that lack the "imageUrl" key.
 *  - The RegistryDto-to-domain mapper drops imageUrl regardless of what
 *    Firestore returned (RegistryDto has no field for it yet).
 *  - newRegistryId() returns "" via the interface default.
 *
 * Plan 02 flips all five tests GREEN.
 */
class RegistryRepositoryImplCoverTest {

    private lateinit var dataSource: FirestoreDataSource
    private lateinit var functions: FirebaseFunctions
    private lateinit var repository: RegistryRepositoryImpl

    @Before
    fun setUp() {
        dataSource = mockk(relaxed = true)
        functions = mockk(relaxed = true)
        repository = RegistryRepositoryImpl(dataSource, functions)
    }

    /**
     * Pitfall 1 — toMap MUST include `imageUrl` for createRegistry to persist
     * the cover URL. Plan 02 adds `"imageUrl" to imageUrl` to the map.
     */
    @Test
    fun toMap_includesImageUrl() = runTest {
        val capturedMap = slot<Map<String, Any?>>()
        coEvery { dataSource.createRegistry(capture(capturedMap)) } returns "reg-id"

        val registry = sampleRegistry(imageUrl = "preset:Wedding:3")
        val result = repository.createRegistry(registry)

        assertTrue("createRegistry must succeed", result.isSuccess)
        assertTrue(
            "Pitfall 1: toMap MUST include 'imageUrl' key — currently dropped silently",
            capturedMap.captured.containsKey("imageUrl"),
        )
        assertEquals(
            "Pitfall 1: imageUrl value must roundtrip into the Firestore map",
            "preset:Wedding:3",
            capturedMap.captured["imageUrl"],
        )
    }

    /**
     * Pitfall 1 — toUpdateMap MUST include `imageUrl` for updateRegistry edits
     * (e.g., owner changes the cover from picker on Detail screen).
     */
    @Test
    fun toUpdateMap_includesImageUrl() = runTest {
        val capturedMap = slot<Map<String, Any?>>()
        coEvery { dataSource.updateRegistry(any(), capture(capturedMap)) } returns Unit

        val registry = sampleRegistry(id = "reg-1", imageUrl = "https://x.jpg")
        val result = repository.updateRegistry(registry)

        assertTrue("updateRegistry must succeed", result.isSuccess)
        assertTrue(
            "Pitfall 1: toUpdateMap MUST include 'imageUrl' key for cover edits",
            capturedMap.captured.containsKey("imageUrl"),
        )
        assertEquals(
            "Pitfall 1: imageUrl value must roundtrip into the Firestore update map",
            "https://x.jpg",
            capturedMap.captured["imageUrl"],
        )
    }

    /**
     * Pitfall 1 — RegistryDto MUST carry an `imageUrl` field so observed
     * registries reflect the value persisted in Firestore. Plan 02 adds
     * `val imageUrl: String? = null` to RegistryDto + propagates in toDomain().
     */
    @Test
    fun toDomain_propagatesImageUrl() = runTest {
        val flow = MutableStateFlow<RegistryDto?>(
            // Wave 0: this constructor argument doesn't exist yet (Pitfall 1).
            // We reflectively set it via a future-shape mock. For Wave 0 we
            // simulate by creating a dto with the expected imageUrl populated
            // through reflection-friendly means. Plan 02 adds the field.
            sampleDtoWithImageUrlSimulated(id = "reg-1", imageUrlValue = "preset:Birthday:1"),
        )
        every { dataSource.observeRegistry("reg-1") } returns flow

        val observed: Registry? = repository.observeRegistry("reg-1").first()

        assertNotNull("observed registry must be non-null", observed)
        assertEquals(
            "Pitfall 1: RegistryDto.imageUrl must propagate to Registry.imageUrl",
            "preset:Birthday:1",
            observed?.imageUrl,
        )
    }

    /** Pitfall 1 — when Firestore doc has no imageUrl field, domain Registry.imageUrl is null. */
    @Test
    fun toDomain_nullImageUrl_whenAbsent() = runTest {
        val flow = MutableStateFlow<RegistryDto?>(RegistryDto(id = "reg-1"))
        every { dataSource.observeRegistry("reg-1") } returns flow

        val observed: Registry? = repository.observeRegistry("reg-1").first()

        assertNotNull(observed)
        assertNull(
            "Pitfall 1: missing imageUrl field on RegistryDto must yield null on Registry",
            observed?.imageUrl,
        )
    }

    /**
     * D-07 enabler — `newRegistryId()` returns a non-blank Firestore-generated
     * client-side ID so the upload path `users/{uid}/registries/{registryId}/cover.jpg`
     * can be constructed BEFORE the registry doc is written (Pitfall 2 mitigation).
     *
     * Wave 0 RED: the interface default returns "" so the assertion fails.
     * Plan 02 wires `firestore.collection("registries").document().id` (no
     * server roundtrip — Firestore generates IDs locally).
     */
    @Test
    fun newRegistryId_returnsNonBlankString() {
        val id = repository.newRegistryId()
        assertTrue(
            "D-07: newRegistryId() must return a non-blank Firestore-generated ID " +
                "(Wave 0 RED — Plan 02 wires Firestore client-side ID generation)",
            id.isNotBlank(),
        )
    }

    // ------------------------------------------------------------------ Helpers

    private fun sampleRegistry(
        id: String = "",
        ownerId: String = "uid-1",
        imageUrl: String? = null,
    ) = Registry(
        id = id,
        ownerId = ownerId,
        title = "Test Registry",
        occasion = "Wedding",
        visibility = "public",
        imageUrl = imageUrl,
    )

    /**
     * Wave 0 helper: returns a RegistryDto representing what a Firestore doc
     * with an `imageUrl` field would deserialise to — once Plan 02 adds the
     * field to RegistryDto. Wave 0's RegistryDto has NO imageUrl field, so
     * this helper currently returns a vanilla DTO and the
     * `toDomain_propagatesImageUrl` test fails RED on the assertion.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun sampleDtoWithImageUrlSimulated(id: String, imageUrlValue: String) =
        RegistryDto(id = id)
}
