package com.giftregistry.ui.registry.invite

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giftregistry.domain.usecase.InviteToRegistryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InviteViewModel @Inject constructor(
    private val inviteToRegistry: InviteToRegistryUseCase
) : ViewModel() {

    val email = MutableStateFlow("")

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _inviteSent = MutableStateFlow(false)
    val inviteSent: StateFlow<Boolean> = _inviteSent.asStateFlow()

    fun onSendInvite(registryId: String) {
        val emailValue = email.value.trim()
        if (emailValue.isBlank() || !emailValue.contains("@")) {
            _error.value = "Please enter a valid email address"
            return
        }

        viewModelScope.launch {
            _isSending.value = true
            _error.value = null

            inviteToRegistry(registryId, emailValue)
                .onSuccess {
                    _inviteSent.value = true
                    email.value = ""
                }
                .onFailure { e ->
                    Log.e("InviteVM", "inviteToRegistry failed for $emailValue on $registryId", e)
                    _error.value = e.message ?: "Failed to send invitation"
                }

            _isSending.value = false
        }
    }

    fun clearError() { _error.value = null }
    fun resetInviteSent() { _inviteSent.value = false }
}
