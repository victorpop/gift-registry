package com.giftregistry.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giftregistry.domain.auth.ObserveAuthStateUseCase
import com.giftregistry.domain.usecase.ObserveUnreadNotificationCountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class InboxBellViewModel @Inject constructor(
    private val observeAuthState: ObserveAuthStateUseCase,
    private val observeUnreadCount: ObserveUnreadNotificationCountUseCase,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val unreadCount: StateFlow<Int> = observeAuthState()
        .flatMapLatest { user ->
            if (user == null) flowOf(0) else observeUnreadCount(user.uid)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0,
        )
}
