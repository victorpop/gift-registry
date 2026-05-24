package com.giftregistry.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giftregistry.domain.notifications.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * D-07 — Sheet-scoped state machine for InviteResponseSheet.
 *
 * Owns the in-flight callable for Accept / Decline; surfaces Submitting / Error /
 * Success terminal states. Parent composable consumes state + drives navigation
 * on AcceptedSuccess (D-05).
 *
 * State transitions:
 *   Idle → Submitting(action) → AcceptedSuccess | DeclinedSuccess | Error(action)
 *   Error(action) → Submitting(action) (via retry())
 *   Idle (via reset() — called when sheet dismissed mid-error)
 *
 * Plan 16-01 RED test contract:
 *   `(err as InviteResponseViewModel.State.Error).action` must compile, so the
 *   Error data class has `action` as its first parameter. The second parameter
 *   (`messageKey`) carries a string-resource key that the parent composable
 *   resolves to localized copy at render time.
 */
@HiltViewModel
class InviteResponseViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    enum class Action { Accept, Decline }

    sealed interface State {
        data object Idle : State
        data class Submitting(val action: Action) : State
        data class Error(val action: Action, val messageKey: String) : State
        data object AcceptedSuccess : State
        data object DeclinedSuccess : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    private var lastRegistryId: String? = null
    private var lastAction: Action? = null

    fun accept(registryId: String) {
        lastRegistryId = registryId
        lastAction = Action.Accept
        _state.value = State.Submitting(Action.Accept)
        viewModelScope.launch {
            notificationRepository.acceptInvite(registryId).fold(
                onSuccess = { _state.value = State.AcceptedSuccess },
                onFailure = {
                    _state.value = State.Error(Action.Accept, "invite_sheet_error_accept")
                },
            )
        }
    }

    fun decline(registryId: String) {
        lastRegistryId = registryId
        lastAction = Action.Decline
        _state.value = State.Submitting(Action.Decline)
        viewModelScope.launch {
            notificationRepository.declineInvite(registryId).fold(
                onSuccess = { _state.value = State.DeclinedSuccess },
                onFailure = {
                    _state.value = State.Error(Action.Decline, "invite_sheet_error_decline")
                },
            )
        }
    }

    /** Retry the last attempted action. No-op if no prior action. */
    fun retry() {
        val rid = lastRegistryId ?: return
        when (lastAction) {
            Action.Accept -> accept(rid)
            Action.Decline -> decline(rid)
            null -> Unit
        }
    }

    /** Force state back to Idle (e.g., sheet dismissed mid-error). */
    fun reset() {
        _state.value = State.Idle
    }
}
