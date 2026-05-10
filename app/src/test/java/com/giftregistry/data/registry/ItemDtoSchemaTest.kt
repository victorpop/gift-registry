package com.giftregistry.data.registry

import com.giftregistry.data.model.ItemDto
import com.google.firebase.Timestamp
import com.google.firebase.functions.FirebaseFunctions
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Quick task 260510-oja — schema-shape regression tests for [ItemDto] and the
 * data→domain mapper in [ItemRepositoryImpl].
 *
 * Source of truth for the Firestore item schema is the Cloud Function
 * `createReservation` (functions/src/reservation/createReservation.ts:62-67):
 *   reservedBy: string  (giverEmail)
 *   reservedAt: Timestamp  (FieldValue.serverTimestamp())
 *   expiresAt:  Timestamp  (Timestamp.fromMillis(...))
 *
 * Before this task, [ItemDto] was missing `reservedBy` / `reservedAt` and typed
 * `expiresAt` as `Long?`, causing Firestore's CustomClassMapper to throw on
 * `doc.toObject(ItemDto::class.java)` whenever a registry contained a reserved
 * item. The crash killed the registry detail screen for any owner.
 *
 * These tests pin the JVM-testable contract:
 *  - DTO field types match the canonical schema (Tests 1-3).
 *  - Default-null reservation fields support the most-common case where an
 *    item is `available` and not reserved (Test 4).
 *  - The data→domain mapper converts the Firestore [Timestamp] `expiresAt` to
 *    the domain `Item.expiresAt: Long?` epoch-ms contract consumed by
 *    StatusChip / ReservedChip (Test 5), and treats null safely (Test 6).
 *
 * The actual `CustomClassMapper` happiness is verified at the device level in
 * Task 3 (live Firestore deserialization).
 */
class ItemDtoSchemaTest {

    // ------------------------------------------------------------------ Test 1
    @Test
    fun `ItemDto has reservedBy String field matching createReservation writer`() {
        val dto = ItemDto(reservedBy = "giver@example.com")
        assertEquals("giver@example.com", dto.reservedBy)
    }

    // ------------------------------------------------------------------ Test 2
    @Test
    fun `ItemDto has reservedAt Timestamp field matching createReservation writer`() {
        val ts = Timestamp(1_700_000_000L, 0)
        val dto = ItemDto(reservedAt = ts)
        assertEquals(1_700_000_000L, dto.reservedAt?.seconds)
    }

    // ------------------------------------------------------------------ Test 3
    @Test
    fun `ItemDto expiresAt is Timestamp not Long matching createReservation writer`() {
        val dto = ItemDto(expiresAt = Timestamp.fromMillis(1_700_000_500_000L))
        assertEquals(1_700_000_500_000L, dto.expiresAt?.toDate()?.time)
    }

    // ------------------------------------------------------------------ Test 4
    @Test
    fun `ItemDto default constructor has null reservation fields for available items`() {
        val dto = ItemDto()
        assertNull(dto.reservedBy)
        assertNull(dto.reservedAt)
        assertNull(dto.expiresAt)
    }

    // ------------------------------------------------------------------ Test 5
    @Test
    fun `toDomain converts non-null Timestamp expiresAt to Long epoch ms`() = runTest {
        val expectedMs = 1_700_000_500_000L
        val dto = ItemDto(
            id = "item1",
            title = "T",
            status = "reserved",
            expiresAt = Timestamp.fromMillis(expectedMs),
        )
        val dataSource: FirestoreDataSource = mockk(relaxed = true)
        every { dataSource.observeItems("reg1") } returns MutableStateFlow(listOf(dto))
        val functions: FirebaseFunctions = mockk(relaxed = true)
        val repo = ItemRepositoryImpl(dataSource, functions)

        val items = repo.observeItems("reg1").first()

        assertEquals(1, items.size)
        assertEquals(expectedMs, items.single().expiresAt)
    }

    // ------------------------------------------------------------------ Test 6
    @Test
    fun `toDomain maps null Timestamp expiresAt to null Long for available items`() = runTest {
        // Guards Edge case 1 (optimistic local snapshot — serverTimestamp not yet
        // resolved) and Edge case 2 (most items have all reservation fields null).
        val dto = ItemDto(id = "item2", title = "T2", status = "available")
        val dataSource: FirestoreDataSource = mockk(relaxed = true)
        every { dataSource.observeItems("reg1") } returns MutableStateFlow(listOf(dto))
        val functions: FirebaseFunctions = mockk(relaxed = true)
        val repo = ItemRepositoryImpl(dataSource, functions)

        val items = repo.observeItems("reg1").first()

        assertEquals(1, items.size)
        assertNull(items.single().expiresAt)
    }
}
