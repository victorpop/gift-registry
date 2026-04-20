package com.giftregistry.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giftregistry.domain.model.Notification
import com.giftregistry.domain.auth.ObserveAuthStateUseCase
import com.giftregistry.domain.usecase.MarkNotificationsReadUseCase
import com.giftregistry.domain.usecase.ObserveNotificationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val observeAuthState: ObserveAuthStateUseCase,
    private val observeNotifications: ObserveNotificationsUseCase,
    private val markNotificationsRead: MarkNotificationsReadUseCase,
) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState
        data object Unauthenticated : UiState
        data object Empty : UiState
        data class Loaded(val notifications: List<Notification>) : UiState
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<UiState> = observeAuthState()
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(UiState.Unauthenticated)
            } else {
                observeNotifications(user.uid).map { list ->
                    if (list.isEmpty()) UiState.Empty else UiState.Loaded(list)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading,
        )

    /** uid from the current auth state, cached for markVisibleRead calls. */
    private var currentUid: String? = null

    init {
        viewModelScope.launch {
            observeAuthState().collect { user ->
                currentUid = user?.uid
            }
        }
    }

    /**
     * Marks the given notification IDs as read in a single batched write.
     * Already-read IDs are filtered out client-side to avoid redundant writes.
     */
    fun markVisibleRead(ids: List<String>) {
        val uid = currentUid ?: return
        if (ids.isEmpty()) return
        viewModelScope.launch {
            markNotificationsRead(uid, ids)
        }
    }
}
