package com.giftregistry.ui.store.browser

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giftregistry.domain.usecase.GetStoresUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoreBrowserViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getStores: GetStoresUseCase,
) : ViewModel() {

    val storeId: String = savedStateHandle["storeId"] ?: ""
    val registryId: String? = savedStateHandle["registryId"]

    private val _currentUrl = MutableStateFlow("")
    val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    private val _pageLoadFailed = MutableStateFlow(false)
    val pageLoadFailed: StateFlow<Boolean> = _pageLoadFailed.asStateFlow()

    private val _storeName = MutableStateFlow("")
    val storeName: StateFlow<String> = _storeName.asStateFlow()

    private val _homepageUrl = MutableStateFlow("")
    val homepageUrl: StateFlow<String> = _homepageUrl.asStateFlow()

    // Derived — Add-to-list button enabled when: load hasn't failed AND a page has loaded
    val addToListEnabled: StateFlow<Boolean> = combine(_currentUrl, _pageLoadFailed) { url, failed ->
        !failed && url.isNotBlank()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        viewModelScope.launch {
            getStores().onSuccess { stores ->
                stores.find { it.id == storeId }?.let { store ->
                    _storeName.value = store.name
                    _homepageUrl.value = store.homepageUrl
                }
            }
        }
    }

    fun onUrlChanged(url: String) {
        _currentUrl.value = url
        _pageLoadFailed.value = false
    }

    fun onPageLoadFailed() {
        _pageLoadFailed.value = true
        _currentUrl.value = ""
    }

    fun onRetry() {
        _pageLoadFailed.value = false
        // Composable re-renders WebView.reload() via LaunchedEffect(pageLoadFailed)
    }
}
