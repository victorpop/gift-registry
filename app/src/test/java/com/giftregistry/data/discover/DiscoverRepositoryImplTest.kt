package com.giftregistry.data.discover

import com.google.android.gms.tasks.Tasks
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableReference
import com.google.firebase.functions.HttpsCallableResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 17 Plan 05 D-49 — repository-layer tests covering both Callables
 * (`discoverPopular`, `discoverSearch`). Mirrors the existing
 * ReservationRepositoryConfirmPurchaseTest mocking pattern: stub
 * FirebaseFunctions + HttpsCallableReference with mockk; drive .call() with
 * Tasks.forResult / Tasks.forException; assert on the mapped domain output.
 */
class DiscoverRepositoryImplTest {

    // ---------- getPopular ----------

    @Test
    fun `getPopular maps products list with ids verbatim`() = runTest {
        val functions = mockk<FirebaseFunctions>()
        val callable = mockk<HttpsCallableReference>()
        val result = mockk<HttpsCallableResult>()
        val payload = mapOf(
            "products" to listOf(
                mapOf(
                    "id" to "doc-1",
                    "title" to "Espresso machine",
                    "description" to "15 bar",
                    "image_url" to "https://img/1.jpg",
                    "price" to 1299.0,
                    "currency" to "RON",
                    "retailer_url" to "https://emag.ro/1",
                ),
                mapOf(
                    "id" to "doc-2",
                    "title" to "Yeti tumbler",
                    "description" to "20oz",
                    "image_url" to "https://img/2.jpg",
                    "price" to 199L, // integer-form price
                    "currency" to "RON",
                    "retailer_url" to "https://yeti.com/2",
                ),
            ),
        )
        every { functions.getHttpsCallable("discoverPopular") } returns callable
        every { callable.call() } returns Tasks.forResult(result)
        every { result.getData() } returns payload

        val repo = DiscoverRepositoryImpl(functions)
        val actual = repo.getPopular()

        assertTrue("expected Result.success, got $actual", actual.isSuccess)
        val products = actual.getOrThrow()
        assertEquals(2, products.size)
        assertEquals("doc-1", products[0].id)
        assertEquals("Espresso machine", products[0].title)
        assertEquals(1299.0, products[0].price, 0.0001)
        assertEquals("RON", products[0].currency)
        assertEquals("https://emag.ro/1", products[0].retailerUrl)
        // Long price converts to Double.
        assertEquals(199.0, products[1].price, 0.0001)
    }

    @Test
    fun `getPopular with empty products returns success with empty list — NOT failure`() = runTest {
        val functions = mockk<FirebaseFunctions>()
        val callable = mockk<HttpsCallableReference>()
        val result = mockk<HttpsCallableResult>()
        every { functions.getHttpsCallable("discoverPopular") } returns callable
        every { callable.call() } returns Tasks.forResult(result)
        every { result.getData() } returns mapOf("products" to emptyList<Any>())

        val repo = DiscoverRepositoryImpl(functions)
        val actual = repo.getPopular()

        assertTrue("empty list must be Result.success", actual.isSuccess)
        assertTrue(actual.getOrThrow().isEmpty())
    }

    @Test
    fun `getPopular callable failure propagates as Result_failure`() = runTest {
        val functions = mockk<FirebaseFunctions>()
        val callable = mockk<HttpsCallableReference>()
        every { functions.getHttpsCallable("discoverPopular") } returns callable
        every { callable.call() } returns Tasks.forException(RuntimeException("INTERNAL"))

        val repo = DiscoverRepositoryImpl(functions)
        val actual = repo.getPopular()

        assertTrue(actual.isFailure)
        assertEquals("INTERNAL", actual.exceptionOrNull()?.message)
    }

    @Test
    fun `getPopular missing id field leaves id empty (popular mode)`() = runTest {
        val functions = mockk<FirebaseFunctions>()
        val callable = mockk<HttpsCallableReference>()
        val result = mockk<HttpsCallableResult>()
        val payload = mapOf(
            "products" to listOf(
                mapOf(
                    // no "id" key
                    "title" to "Untitled",
                    "image_url" to "",
                    "price" to 0,
                    "currency" to "RON",
                    "retailer_url" to "",
                ),
            ),
        )
        every { functions.getHttpsCallable("discoverPopular") } returns callable
        every { callable.call() } returns Tasks.forResult(result)
        every { result.getData() } returns payload

        val repo = DiscoverRepositoryImpl(functions)
        val actual = repo.getPopular()

        assertTrue(actual.isSuccess)
        assertEquals("", actual.getOrThrow()[0].id)
    }

    // ---------- search ----------

    @Test
    fun `search maps products and passes query payload`() = runTest {
        val functions = mockk<FirebaseFunctions>()
        val callable = mockk<HttpsCallableReference>()
        val result = mockk<HttpsCallableResult>()
        val captor = slot<Map<String, Any?>>()
        val payload = mapOf(
            "products" to listOf(
                mapOf(
                    // No id field — search responses do not include it (D-31).
                    "title" to "Bialetti Moka",
                    "description" to "Stovetop espresso",
                    "image_url" to "https://img/m.jpg",
                    "price" to 149.99,
                    "currency" to "RON",
                    "retailer_url" to "https://emag.ro/moka",
                    "retailer_name" to "eMAG",
                ),
            ),
            "cached_at" to "2026-05-27T10:00:00Z",
        )
        every { functions.getHttpsCallable("discoverSearch") } returns callable
        every { callable.call(capture(captor)) } returns Tasks.forResult(result)
        every { result.getData() } returns payload

        val repo = DiscoverRepositoryImpl(functions)
        val actual = repo.search("espresso")

        assertTrue(actual.isSuccess)
        assertEquals(mapOf("query" to "espresso"), captor.captured)
        val products = actual.getOrThrow()
        assertEquals(1, products.size)
        // Search mode: missing id should be filled with a generated UUID — non-empty.
        assertNotNull(products[0].id)
        assertFalse("expected generated id, got empty", products[0].id.isEmpty())
        assertEquals("Bialetti Moka", products[0].title)
        assertEquals(149.99, products[0].price, 0.0001)
        assertEquals("eMAG", products[0].retailerName)
    }

    @Test
    fun `getPopular maps retailer_name when present`() = runTest {
        val functions = mockk<FirebaseFunctions>()
        val callable = mockk<HttpsCallableReference>()
        val result = mockk<HttpsCallableResult>()
        val payload = mapOf(
            "products" to listOf(
                mapOf(
                    "id" to "doc-1",
                    "title" to "Espresso machine",
                    "description" to "",
                    "image_url" to "https://img/1.jpg",
                    "price" to 1299.0,
                    "currency" to "RON",
                    "retailer_url" to "https://emag.ro/1",
                    "retailer_name" to "emag.ro",
                ),
            ),
        )
        every { functions.getHttpsCallable("discoverPopular") } returns callable
        every { callable.call() } returns Tasks.forResult(result)
        every { result.getData() } returns payload

        val repo = DiscoverRepositoryImpl(functions)
        val actual = repo.getPopular()

        assertTrue(actual.isSuccess)
        assertEquals("emag.ro", actual.getOrThrow()[0].retailerName)
    }

    @Test
    fun `missing retailer_name defaults to empty string`() = runTest {
        val functions = mockk<FirebaseFunctions>()
        val callable = mockk<HttpsCallableReference>()
        val result = mockk<HttpsCallableResult>()
        every { functions.getHttpsCallable("discoverPopular") } returns callable
        every { callable.call() } returns Tasks.forResult(result)
        every { result.getData() } returns mapOf(
            "products" to listOf(
                mapOf(
                    "id" to "doc-1",
                    "title" to "x",
                    "image_url" to "",
                    "price" to 0,
                    "currency" to "RON",
                    "retailer_url" to "https://x/",
                ),
            ),
        )

        val repo = DiscoverRepositoryImpl(functions)
        val actual = repo.getPopular()

        assertTrue(actual.isSuccess)
        assertEquals("", actual.getOrThrow()[0].retailerName)
    }

    @Test
    fun `search empty list returns success with empty list — NOT failure`() = runTest {
        val functions = mockk<FirebaseFunctions>()
        val callable = mockk<HttpsCallableReference>()
        val result = mockk<HttpsCallableResult>()
        every { functions.getHttpsCallable("discoverSearch") } returns callable
        every { callable.call(any()) } returns Tasks.forResult(result)
        every { result.getData() } returns mapOf("products" to emptyList<Any>(), "cached_at" to "2026-05-27T10:00:00Z")

        val repo = DiscoverRepositoryImpl(functions)
        val actual = repo.search("nothing-here")

        assertTrue(actual.isSuccess)
        assertTrue(actual.getOrThrow().isEmpty())
    }

    @Test
    fun `search callable failure propagates as Result_failure`() = runTest {
        val functions = mockk<FirebaseFunctions>()
        val callable = mockk<HttpsCallableReference>()
        every { functions.getHttpsCallable("discoverSearch") } returns callable
        every { callable.call(any()) } returns Tasks.forException(RuntimeException("RATE_LIMITED"))

        val repo = DiscoverRepositoryImpl(functions)
        val actual = repo.search("x")

        assertTrue(actual.isFailure)
        assertEquals("RATE_LIMITED", actual.exceptionOrNull()?.message)
    }
}
