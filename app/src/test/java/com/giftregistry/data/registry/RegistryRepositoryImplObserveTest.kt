package com.giftregistry.data.registry

import com.giftregistry.data.model.RegistryDto
import com.google.firebase.functions.FirebaseFunctions
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the merge/dedupe/sort pipeline in [RegistryRepositoryImpl.observeRegistries].
 *
 * Uses MockK to stub [FirestoreDataSource.observeOwnedRegistries] and
 * [FirestoreDataSource.observeInvitedRegistries] so tests run on the JVM without
 * Firebase SDK.
 */
class RegistryRepositoryImplObserveTest {

    private lateinit var ownedFlow: MutableStateFlow<List<RegistryDto>>
    private lateinit var invitedFlow: MutableStateFlow<List<RegistryDto>>
    private lateinit var dataSource: FirestoreDataSource
    private lateinit var functions: FirebaseFunctions
    private lateinit var repository: RegistryRepositoryImpl

    private val uid = "test-uid"

    @Before
    fun setUp() {
        ownedFlow = MutableStateFlow(emptyList())
        invitedFlow = MutableStateFlow(emptyList())

        dataSource = mockk(relaxed = true)
        functions = mockk(relaxed = true)

        every { dataSource.observeOwnedRegistries(uid) } returns ownedFlow
        every { dataSource.observeInvitedRegistries(uid) } returns invitedFlow

        repository = RegistryRepositoryImpl(dataSource, functions)
    }

    // ------------------------------------------------------------------ Test 1
    @Test
    fun `observeRegistries emits owned and invited merged into one list`() = runTest {
        val regA = dto("A", ownerId = uid, updatedAt = 100L)
        val regB = dto("B", ownerId = "other-uid", updatedAt = 50L)

        ownedFlow.value = listOf(regA)
        invitedFlow.value = listOf(regB)

        val result = repository.observeRegistries(uid).first()

        assertEquals(2, result.size)
        assertTrue(result.any { it.id == "A" })
        assertTrue(result.any { it.id == "B" })
    }

    // ------------------------------------------------------------------ Test 2
    @Test
    fun `dedupe — registry in both owned and invited appears exactly once, owned entry wins`() =
        runTest {
            // Edge case: owner self-invited their own registry
            val fromOwned = dto("X", ownerId = uid, updatedAt = 200L, title = "from-owned")
            val fromInvited = dto("X", ownerId = uid, updatedAt = 200L, title = "from-invited")

            ownedFlow.value = listOf(fromOwned)
            invitedFlow.value = listOf(fromInvited)

            val result = repository.observeRegistries(uid).first()

            assertEquals(1, result.size)
            // Owner entry wins — its title is "from-owned"
            assertEquals("from-owned", result.single().title)
        }

    // ------------------------------------------------------------------ Test 3
    @Test
    fun `sorted by updatedAt descending`() = runTest {
        val newer = dto("newer", ownerId = uid, updatedAt = 200L)
        val older = dto("older", ownerId = "other-uid", updatedAt = 100L)

        // Put the older one in owned and the newer one in invited to ensure
        // the sort crosses stream boundaries
        ownedFlow.value = listOf(older)
        invitedFlow.value = listOf(newer)

        val result = repository.observeRegistries(uid).first()

        assertEquals(listOf("newer", "older"), result.map { it.id })
    }

    // ------------------------------------------------------------------ Test 4a
    @Test
    fun `empty invited — returns owned only`() = runTest {
        val regA = dto("A", ownerId = uid, updatedAt = 200L)
        val regB = dto("B", ownerId = uid, updatedAt = 100L)

        ownedFlow.value = listOf(regA, regB)
        invitedFlow.value = emptyList()

        val result = repository.observeRegistries(uid).first()

        assertEquals(2, result.size)
        assertEquals(listOf("A", "B"), result.map { it.id })
    }

    // ------------------------------------------------------------------ Test 4b
    @Test
    fun `empty owned — returns invited only`() = runTest {
        val regC = dto("C", ownerId = "other-uid", updatedAt = 50L)

        ownedFlow.value = emptyList()
        invitedFlow.value = listOf(regC)

        val result = repository.observeRegistries(uid).first()

        assertEquals(1, result.size)
        assertEquals("C", result.single().id)
    }

    // ------------------------------------------------------------------ Test 4c
    @Test
    fun `both empty — returns empty list`() = runTest {
        ownedFlow.value = emptyList()
        invitedFlow.value = emptyList()

        val result = repository.observeRegistries(uid).first()

        assertTrue(result.isEmpty())
    }

    // ------------------------------------------------------------------ Test 5
    @Test
    fun `updates to invited flow propagate to merged output`() = runTest {
        val regA = dto("A", ownerId = uid, updatedAt = 200L)
        ownedFlow.value = listOf(regA)
        invitedFlow.value = emptyList()

        // Collect two emissions
        val emissions = mutableListOf<List<String>>()

        // Use a coroutine to collect while we mutate the flow
        val job = launch(Dispatchers.Unconfined) {
            repository.observeRegistries(uid).collect { list ->
                emissions.add(list.map { it.id })
            }
        }

        // Wait for first emission (only A)
        yield()

        // Now add an invited registry
        val regB = dto("B", ownerId = "other-uid", updatedAt = 50L)
        invitedFlow.value = listOf(regB)

        yield()

        job.cancel()

        // First emission: [A], second emission: [A, B]
        assertTrue("Expected at least 2 emissions, got: $emissions", emissions.size >= 2)
        assertTrue("First emission should contain A", emissions.first().contains("A"))
        val lastEmission = emissions.last()
        assertTrue("Last emission should contain A", lastEmission.contains("A"))
        assertTrue("Last emission should contain B", lastEmission.contains("B"))
    }

    // ------------------------------------------------------------------ Helpers

    private fun dto(
        id: String,
        ownerId: String = "",
        updatedAt: Long = 0L,
        title: String = "Registry $id"
    ) = RegistryDto(id = id, ownerId = ownerId, updatedAt = updatedAt, title = title)
}
