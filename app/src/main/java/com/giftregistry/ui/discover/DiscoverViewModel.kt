package com.giftregistry.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giftregistry.domain.discover.DiscoverRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Phase 17 D-41 — Hilt ViewModel coordinating both Discover sections.
 *
 * - `popular`: PopularState for the always-visible community section.
 *   Auto-loads in init {} (no user action required).
 * - `search`: SearchState for the FROM-THE-WEB section. Starts Idle (hidden)
 *   until the user submits a non-blank query via the Search IME action.
 * - `searchQuery`: text-field state, updated by `onQueryChange`. Typing does
 *   NOT auto-fire — only the IME Search action calls `search(query)`.
 *
 * Retry handlers: `loadPopular()` (community), `retrySearch()` (re-issues
 * the last submitted query).
 */
@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val repository: DiscoverRepository,
) : ViewModel() {

    private val _popular = MutableStateFlow<PopularState>(PopularState.Loading)
    val popular: StateFlow<PopularState> = _popular.asStateFlow()

    private val _search = MutableStateFlow<SearchState>(SearchState.Idle)
    val search: StateFlow<SearchState> = _search.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // 260530-ncw: session-local layout toggle; not persisted.
    private val _layoutMode = MutableStateFlow<DiscoverLayoutMode>(DiscoverLayoutMode.TwoColumns)
    val layoutMode: StateFlow<DiscoverLayoutMode> = _layoutMode.asStateFlow()

    fun toggleLayoutMode() {
        _layoutMode.value = when (_layoutMode.value) {
            DiscoverLayoutMode.OneColumn -> DiscoverLayoutMode.TwoColumns
            DiscoverLayoutMode.TwoColumns -> DiscoverLayoutMode.OneColumn
        }
    }

    init {
        loadPopular()
    }

    fun loadPopular() {
        _popular.value = PopularState.Loading
        viewModelScope.launch {
            repository.getPopular().fold(
                onSuccess = { products ->
                    _popular.value = if (products.isEmpty()) PopularState.Empty
                    else PopularState.Loaded(products)
                },
                onFailure = { err ->
                    _popular.value = PopularState.Error(err.message ?: "Unknown error")
                },
            )
        }
    }

    fun onQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun search(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _search.value = SearchState.Idle
            return
        }
        _search.value = SearchState.Loading
        viewModelScope.launch {
            repository.search(trimmed).fold(
                onSuccess = { products ->
                    _search.value = if (products.isEmpty()) SearchState.Empty
                    else SearchState.Loaded(products)
                },
                onFailure = { err ->
                    _search.value = SearchState.Error(err.message ?: "Unknown error")
                },
            )
        }
    }

    fun retrySearch() {
        search(_searchQuery.value)
    }
}
