package com.giftregistry.ui.store.list

import com.giftregistry.MainDispatcherRule
import com.giftregistry.domain.model.Store
import com.giftregistry.domain.usecase.GetStoresUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StoreListViewModelTest {

    @get:Rule val mainDispatcher = MainDispatcherRule()

    private fun store(id: String) = Store(id, "$id-name", "https://$id", 10, "store_$id")

    @Test
    fun `init loads stores and emits Success`() = runTest {
        val getStores = mockk<GetStoresUseCase>()
        coEvery { getStores() } returns Result.success(listOf(store("a"), store("b")))
        val vm = StoreListViewModel(getStores)
        advanceUntilIdle()
        val state = vm.uiState.value
        assertTrue(state is StoreListUiState.Success)
        assertEquals(2, (state as StoreListUiState.Success).stores.size)
    }

    @Test
    fun `failure emits Error state with message`() = runTest {
        val getStores = mockk<GetStoresUseCase>()
        coEvery { getStores() } returns Result.failure(RuntimeException("network"))
        val vm = StoreListViewModel(getStores)
        advanceUntilIdle()
        val state = vm.uiState.value
        assertTrue(state is StoreListUiState.Error)
        assertEquals("network", (state as StoreListUiState.Error).message)
    }

    @Test
    fun `empty stores list surfaces as Error state`() = runTest {
        val getStores = mockk<GetStoresUseCase>()
        coEvery { getStores() } returns Result.success(emptyList())
        val vm = StoreListViewModel(getStores)
        advanceUntilIdle()
        assertTrue(vm.uiState.value is StoreListUiState.Error)
    }

    @Test
    fun `loadStores retry transitions Error to Success`() = runTest {
        val getStores = mockk<GetStoresUseCase>()
        coEvery { getStores() } returnsMany listOf(
            Result.failure(RuntimeException("transient")),
            Result.success(listOf(store("a"))),
        )
        val vm = StoreListViewModel(getStores)
        advanceUntilIdle()
        assertTrue(vm.uiState.value is StoreListUiState.Error)
        vm.loadStores()
        advanceUntilIdle()
        assertTrue(vm.uiState.value is StoreListUiState.Success)
    }
}
