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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateRegistryViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val createRegistryUseCase: CreateRegistryUseCase,
    private val updateRegistryUseCase: UpdateRegistryUseCase,
    private val observeRegistryUseCase: ObserveRegistryUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val registryId: String? = savedStateHandle["registryId"]

    val title = MutableStateFlow("")
    val occasion = MutableStateFlow("")
    val eventDateMs = MutableStateFlow<Long?>(null)
    val eventLocation = MutableStateFlow("")
    val description = MutableStateFlow("")
    val visibility = MutableStateFlow("public")
    val notificationsEnabled = MutableStateFlow(true)

    val isSaving = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val savedRegistryId = MutableStateFlow<String?>(null)

    val isEditMode: Boolean get() = registryId != null

    init {
        if (registryId != null) {
            viewModelScope.launch {
                val registry = observeRegistryUseCase(registryId).first()
                if (registry != null) {
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
        val titleValue = title.value.trim()
        if (titleValue.length < 3 || titleValue.length > 50) {
            error.value = "Registry name must be between 3 and 50 characters."
            return
        }

        val ownerId = authRepository.currentUser?.uid ?: run {
            error.value = "Not authenticated."
            return
        }

        viewModelScope.launch {
            isSaving.value = true
            error.value = null

            val now = System.currentTimeMillis()
            val registry = Registry(
                id = registryId ?: "",
                ownerId = ownerId,
                title = titleValue,
                occasion = occasion.value,
                visibility = visibility.value,
                eventDateMs = eventDateMs.value,
                eventLocation = eventLocation.value.ifBlank { null },
                description = description.value.ifBlank { null },
                locale = "en",
                notificationsEnabled = notificationsEnabled.value,
                createdAt = if (registryId == null) now else 0L,
                updatedAt = now
            )

            if (registryId == null) {
                createRegistryUseCase(registry).fold(
                    onSuccess = { id -> savedRegistryId.value = id },
                    onFailure = { e -> error.value = e.message ?: "Failed to create registry." }
                )
            } else {
                updateRegistryUseCase(registry).fold(
                    onSuccess = { savedRegistryId.value = registryId },
                    onFailure = { e -> error.value = e.message ?: "Failed to update registry." }
                )
            }

            isSaving.value = false
        }
    }

    fun clearError() {
        error.value = null
    }
}
