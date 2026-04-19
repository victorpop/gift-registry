package com.giftregistry.ui.store.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giftregistry.domain.usecase.GetStoresUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoreListViewModel @Inject constructor(
    private val getStores: GetStoresUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<StoreListUiState>(StoreListUiState.Loading)
    val uiState: StateFlow<StoreListUiState> = _uiState.asStateFlow()

    init { loadStores() }

    fun loadStores() {
        viewModelScope.launch {
            _uiState.value = StoreListUiState.Loading
            getStores()
                .onSuccess { stores ->
                    _uiState.value = if (stores.isEmpty()) {
                        // D-17: Empty stores array surfaces as Error state (same UI as fetch failure)
                        StoreListUiState.Error("")
                    } else {
                        StoreListUiState.Success(stores)
                    }
                }
                .onFailure { t ->
                    _uiState.value = StoreListUiState.Error(t.message ?: "")
                }
        }
    }
}
