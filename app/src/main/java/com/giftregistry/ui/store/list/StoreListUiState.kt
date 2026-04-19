package com.giftregistry.ui.store.list

import com.giftregistry.domain.model.Store

sealed interface StoreListUiState {
    data object Loading : StoreListUiState
    data class Success(val stores: List<Store>) : StoreListUiState
    data class Error(val message: String) : StoreListUiState
}
