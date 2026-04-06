package com.giftregistry.ui.registry.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giftregistry.domain.auth.AuthRepository
import com.giftregistry.domain.model.Registry
import com.giftregistry.domain.usecase.CreateRegistryUseCase
import com.giftregistry.domain.usecase.ObserveRegistryUseCase
import com.giftregistry.domain.usecase.UpdateRegistryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateRegistryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val createRegistry: CreateRegistryUseCase,
    private val updateRegistry: UpdateRegistryUseCase,
    private val observeRegistry: ObserveRegistryUseCase
) : ViewModel() {

    val registryId: String? = savedStateHandle["registryId"]
    val isEditMode: Boolean get() = registryId != null

    val title = MutableStateFlow("")
    val occasion = MutableStateFlow("birthday")
    val eventDateMs = MutableStateFlow<Long?>(null)
    val eventLocation = MutableStateFlow("")
    val description = MutableStateFlow("")
    val visibility = MutableStateFlow("public")
    val notificationsEnabled = MutableStateFlow(true)

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _savedRegistryId = MutableStateFlow<String?>(null)
    val savedRegistryId: StateFlow<String?> = _savedRegistryId.asStateFlow()

    init {
        if (registryId != null) {
            viewModelScope.launch {
                observeRegistry(registryId).firstOrNull()?.let { registry ->
                    title.value = registry.title
                    occasion.value = registry.occasion
                    eventDateMs.value = registry.eventDateMs
                    eventLocation.value = registry.eventLocation ?: ""
                    description.value = registry.description ?: ""
                    visibility.value = registry.visibility
                    notificationsEnabled.value = registry.notificationsEnabled
                }
            }
        }
    }

    fun onSave() {
        val currentTitle = title.value.trim()
        if (currentTitle.isBlank()) {
            _error.value = "Registry name is required"
            return
        }
        if (currentTitle.length < 3) {
            _error.value = "Registry name must be at least 3 characters"
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null

            val ownerId = authRepository.currentUser?.uid ?: ""
            val registry = Registry(
                id = registryId ?: "",
                ownerId = ownerId,
                title = currentTitle,
                occasion = occasion.value,
                eventDateMs = eventDateMs.value,
                eventLocation = eventLocation.value.trim().ifBlank { null },
                description = description.value.trim().ifBlank { null },
                visibility = visibility.value,
                notificationsEnabled = notificationsEnabled.value
            )

            if (isEditMode) {
                updateRegistry(registry)
                    .onSuccess { _savedRegistryId.value = registryId }
                    .onFailure { e -> _error.value = e.message ?: "Failed to update registry" }
            } else {
                createRegistry(registry)
                    .onSuccess { id -> _savedRegistryId.value = id }
                    .onFailure { e -> _error.value = e.message ?: "Failed to create registry" }
            }

            _isSaving.value = false
        }
    }

    fun clearError() { _error.value = null }
}
