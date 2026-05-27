package com.giftregistry.ui.discover

import app.cash.turbine.test
import com.giftregistry.MainDispatcherRule
import com.giftregistry.domain.discover.DiscoverProduct
import com.giftregistry.domain.discover.DiscoverRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Phase 17 Plan 05 D-49 — ViewModel state-machine tests.
 *
 * Uses a hand-rolled FakeDiscoverRepository to control success/failure/empty
 * returns for each test; Turbine + MainDispatcherRule for StateFlow assertions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DiscoverViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun sampleProducts(n: Int = 2): List<DiscoverProduct> =
        (1..n).map {
            DiscoverProduct(
                id = "id-$it",
                title = "title-$it",
                description = "desc-$it",
                imageUrl = "https://img/$it.jpg",
                price = 100.0 * it,
                currency = "RON",
                retailerUrl = "https://emag.ro/$it",
            )
        }

    // ---------- init / loadPopular ----------

    @Test
    fun `init triggers loadPopular — Loading then Loaded on non-empty success`() = runTest {
        val repo = FakeDiscoverRepository(popularResult = Result.success(sampleProducts(3)))
        val vm = DiscoverViewModel(repo)

        vm.popular.test {
            // Initial value is Loading (or Loaded — depending on dispatcher timing). Accept either.
            val first = awaitItem()
            val loaded = if (first is PopularState.Loaded) first else awaitItem()
            assertTrue("expected Loaded final state, got $loaded", loaded is PopularState.Loaded)
            assertEquals(3, (loaded as PopularState.Loaded).products.size)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, repo.popularCalls)
    }

    @Test
    fun `loadPopular empty list emits Empty`() = runTest {
        val repo = FakeDiscoverRepository(popularResult = Result.success(emptyList()))
        val vm = DiscoverViewModel(repo)

        vm.popular.test {
            val first = awaitItem()
            val terminal = if (first is PopularState.Empty) first else awaitItem()
            assertTrue("expected Empty terminal state, got $terminal", terminal is PopularState.Empty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadPopular failure emits Error with exception message`() = runTest {
        val repo = FakeDiscoverRepository(popularResult = Result.failure(RuntimeException("OFFLINE")))
        val vm = DiscoverViewModel(repo)

        vm.popular.test {
            val first = awaitItem()
            val terminal = if (first is PopularState.Error) first else awaitItem()
            assertTrue("expected Error terminal state, got $terminal", terminal is PopularState.Error)
            assertEquals("OFFLINE", (terminal as PopularState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ---------- search ----------

    @Test
    fun `search with blank query resets to Idle without calling repo`() = runTest {
        val repo = FakeDiscoverRepository(
            popularResult = Result.success(emptyList()),
            searchResult = Result.success(sampleProducts(1)),
        )
        val vm = DiscoverViewModel(repo)

        vm.search("   ") // whitespace only — should reset to Idle, no Callable invocation

        assertEquals(SearchState.Idle, vm.search.value)
        assertEquals(0, repo.searchCalls)
    }

    @Test
    fun `search with non-empty query emits Loaded on success`() = runTest {
        val repo = FakeDiscoverRepository(
            popularResult = Result.success(emptyList()),
            searchResult = Result.success(sampleProducts(2)),
        )
        val vm = DiscoverViewModel(repo)

        vm.search("espresso")

        val state = vm.search.value
        assertTrue("expected Loaded, got $state", state is SearchState.Loaded)
        assertEquals(2, (state as SearchState.Loaded).products.size)
        assertEquals(1, repo.searchCalls)
        assertEquals("espresso", repo.lastQuery)
    }

    @Test
    fun `search empty list emits Empty`() = runTest {
        val repo = FakeDiscoverRepository(
            popularResult = Result.success(emptyList()),
            searchResult = Result.success(emptyList()),
        )
        val vm = DiscoverViewModel(repo)

        vm.search("nothing-here")

        assertTrue(vm.search.value is SearchState.Empty)
    }

    @Test
    fun `search failure emits Error`() = runTest {
        val repo = FakeDiscoverRepository(
            popularResult = Result.success(emptyList()),
            searchResult = Result.failure(RuntimeException("RATE_LIMITED")),
        )
        val vm = DiscoverViewModel(repo)

        vm.search("x")

        val state = vm.search.value
        assertTrue("expected Error, got $state", state is SearchState.Error)
        assertEquals("RATE_LIMITED", (state as SearchState.Error).message)
    }

    @Test
    fun `search trims query before calling repo`() = runTest {
        val repo = FakeDiscoverRepository(
            popularResult = Result.success(emptyList()),
            searchResult = Result.success(sampleProducts(1)),
        )
        val vm = DiscoverViewModel(repo)

        vm.search("  espresso  ")

        assertEquals("espresso", repo.lastQuery)
    }

    @Test
    fun `retrySearch re-invokes last query`() = runTest {
        val repo = FakeDiscoverRepository(
            popularResult = Result.success(emptyList()),
            searchResult = Result.success(sampleProducts(1)),
        )
        val vm = DiscoverViewModel(repo)

        vm.onQueryChange("moka pot")
        vm.search("moka pot")
        assertEquals(1, repo.searchCalls)

        vm.retrySearch()
        assertEquals(2, repo.searchCalls)
        assertEquals("moka pot", repo.lastQuery)
    }

    // ---------- onQueryChange ----------

    @Test
    fun `onQueryChange updates searchQuery but does NOT fire search`() = runTest {
        val repo = FakeDiscoverRepository(
            popularResult = Result.success(emptyList()),
            searchResult = Result.success(sampleProducts(1)),
        )
        val vm = DiscoverViewModel(repo)

        vm.onQueryChange("typing...")

        assertEquals("typing...", vm.searchQuery.value)
        assertEquals(SearchState.Idle, vm.search.value)
        assertEquals(0, repo.searchCalls)
    }

    /** Hand-rolled fake DiscoverRepository to avoid mock library overhead for VM-side tests. */
    private class FakeDiscoverRepository(
        var popularResult: Result<List<DiscoverProduct>> = Result.success(emptyList()),
        var searchResult: Result<List<DiscoverProduct>> = Result.success(emptyList()),
    ) : DiscoverRepository {
        var popularCalls = 0
        var searchCalls = 0
        var lastQuery: String? = null

        override suspend fun getPopular(): Result<List<DiscoverProduct>> {
            popularCalls++
            return popularResult
        }

        override suspend fun search(query: String): Result<List<DiscoverProduct>> {
            searchCalls++
            lastQuery = query
            return searchResult
        }
    }
}
