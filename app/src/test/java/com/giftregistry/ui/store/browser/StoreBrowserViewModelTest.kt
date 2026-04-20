package com.giftregistry.ui.store.browser

import androidx.lifecycle.SavedStateHandle
import com.giftregistry.MainDispatcherRule
import com.giftregistry.domain.model.Store
import com.giftregistry.domain.usecase.GetStoresUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StoreBrowserViewModelTest {

    @get:Rule val mainDispatcher = MainDispatcherRule()

    private fun store(id: String, name: String, url: String) = Store(id, name, url, 10, "store_$id")

    private fun vm(storeId: String = "emag", registryId: String? = null, stores: List<Store> = emptyList()): StoreBrowserViewModel {
        val getStores = mockk<GetStoresUseCase>()
        coEvery { getStores() } returns Result.success(stores)
        val handle = SavedStateHandle(mapOf("storeId" to storeId, "registryId" to registryId))
        return StoreBrowserViewModel(handle, getStores)
    }

    @Test
    fun `initial state has empty url, not failed, disabled`() = runTest {
        val v = vm()
        advanceUntilIdle()
        assertEquals("", v.currentUrl.value)
        assertFalse(v.pageLoadFailed.value)
        assertFalse(v.addToListEnabled.value)
    }

    @Test
    fun `onUrlChanged sets currentUrl and enables add-to-list`() = runTest {
        val v = vm()
        advanceUntilIdle()
        v.onUrlChanged("https://www.emag.ro/product/1")
        advanceUntilIdle()
        assertEquals("https://www.emag.ro/product/1", v.currentUrl.value)
        assertTrue(v.addToListEnabled.value)
    }

    @Test
    fun `onPageLoadFailed sets pageLoadFailed, clears url, disables add-to-list`() = runTest {
        val v = vm()
        advanceUntilIdle()
        v.onUrlChanged("https://www.emag.ro")
        v.onPageLoadFailed()
        advanceUntilIdle()
        assertTrue(v.pageLoadFailed.value)
        assertEquals("", v.currentUrl.value)
        assertFalse(v.addToListEnabled.value)
    }

    @Test
    fun `onRetry clears pageLoadFailed`() = runTest {
        val v = vm()
        advanceUntilIdle()
        v.onPageLoadFailed()
        v.onRetry()
        advanceUntilIdle()
        assertFalse(v.pageLoadFailed.value)
    }

    @Test
    fun `matching storeId populates storeName and homepageUrl`() = runTest {
        val v = vm(
            storeId = "emag",
            stores = listOf(store("emag", "eMAG", "https://www.emag.ro"), store("altex", "Altex", "https://altex.ro")),
        )
        advanceUntilIdle()
        assertEquals("eMAG", v.storeName.value)
        assertEquals("https://www.emag.ro", v.homepageUrl.value)
    }

    @Test
    fun `unknown storeId leaves storeName empty`() = runTest {
        val v = vm(storeId = "unknown", stores = listOf(store("emag", "eMAG", "https://www.emag.ro")))
        advanceUntilIdle()
        assertEquals("", v.storeName.value)
    }
}
