package com.giftregistry.ui.registry.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giftregistry.data.storage.CoverImageProcessor
import com.giftregistry.domain.auth.AuthRepository
import com.giftregistry.domain.model.Registry
import com.giftregistry.domain.registry.RegistryRepository
import com.giftregistry.domain.storage.StorageRepository
import com.giftregistry.domain.usecase.CreateRegistryUseCase
import com.giftregistry.domain.usecase.ObserveRegistryUseCase
import com.giftregistry.domain.usecase.UpdateRegistryUseCase
import com.giftregistry.ui.registry.cover.CoverPhotoSelection
import com.giftregistry.ui.registry.cover.PresetCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateRegistryViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val createRegistryUseCase: CreateRegistryUseCase,
    private val updateRegistryUseCase: UpdateRegistryUseCase,
    private val observeRegistryUseCase: ObserveRegistryUseCase,
    // Phase 12 D-07 — direct repository injection for `newRegistryId()`. The
    // helper is a pure client-side Firestore ID generator (no network) and the
    // existing CreateRegistryUseCase already wraps the repo for create/update,
    // so adding a one-line use case wrapper would add indirection without
    // testability gain. Mixing repo + use cases here mirrors the precedent
    // already used by other ViewModels in the codebase.
    private val registryRepository: RegistryRepository,
    private val storageRepository: StorageRepository,
    private val coverImageProcessor: CoverImageProcessor,
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

    /**
     * Phase 12 — current cover-photo selection (D-09 / D-10).
     *
     * Drives [CoverPhotoPickerInline] preview state on the form and the
     * picker sheet's selected-state highlight. On `onSave()`:
     * - [CoverPhotoSelection.None] → `imageUrl = null`, no upload.
     * - [CoverPhotoSelection.Preset] → `imageUrl = PresetCatalog.encode(...)`, no upload.
     * - [CoverPhotoSelection.Gallery] → `imageUrl = downloadUrl` AFTER upload completes
     *   (D-07 + Pitfall 2 prescribed fix — upload-then-write strict ordering).
     *
     * Cleared automatically on occasion change when the picked preset belongs
     * to a different occasion (D-11) — see the `init { occasion.collect { ... } }`
     * collector below.
     */
    val coverPhotoSelection = MutableStateFlow<CoverPhotoSelection>(CoverPhotoSelection.None)

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
                    // Edit-mode preset rehydration (Wave 1 simplification): if the
                    // existing imageUrl is a `preset:OCC:N` sentinel, decode and
                    // reflect it in the picker so the right tile shows selected.
                    // Gallery-URL existing covers leave selection as None — the
                    // inline preview still renders the existing image via
                    // HeroImageOrPlaceholder; user can re-pick to replace.
                    val existingUrl = registry.imageUrl
                    if (existingUrl != null && existingUrl.startsWith("preset:")) {
                        val parts = existingUrl.removePrefix("preset:").split(":")
                        val presetIndex = parts.getOrNull(1)?.toIntOrNull()
                        val presetOccasion = parts.getOrNull(0)
                        if (presetOccasion != null && !presetOccasion.isBlank() && presetIndex != null && presetIndex >= 1) {
                            coverPhotoSelection.value =
                                CoverPhotoSelection.Preset(presetOccasion, presetIndex)
                        }
                    }
                }
            }
        }
        // D-11 — clear preset selection when occasion changes to a different
        // value. A Wedding registry must never display a Birthday preset.
        // CreateRegistryViewModelCoverTest.occasionChange_clearsPresetSelection
        // pins this contract.
        viewModelScope.launch {
            occasion.collect { newOccasion ->
                val current = coverPhotoSelection.value
                if (current is CoverPhotoSelection.Preset && current.occasion != newOccasion) {
                    coverPhotoSelection.value = CoverPhotoSelection.None
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

            // D-07 enabler — pre-mint the registryId BEFORE any upload so the
            // Storage path `users/{uid}/registries/{registryId}/cover.jpg` is
            // known and stable. In edit mode reuse the existing id; in create
            // mode mint a fresh client-side Firestore id (no network roundtrip).
            val effectiveRegistryId: String = registryId ?: registryRepository.newRegistryId()

            // D-07 + Pitfall 2 prescribed fix — RESOLVE THE FINAL imageUrl
            // BEFORE any Firestore write. Strict suspend ordering:
            //   1. Gallery → compress + uploadCover → use download URL.
            //   2. Preset  → encode sentinel synchronously.
            //   3. None    → null.
            // On Gallery upload failure we set error.value and return early,
            // emitting NOTHING to savedRegistryId — guarantees zero orphan
            // registry documents (CreateRegistryViewModelCoverTest pins this).
            val resolvedImageUrl: String? = when (val sel = coverPhotoSelection.value) {
                CoverPhotoSelection.None -> null
                is CoverPhotoSelection.Preset -> PresetCatalog.encode(sel.occasion, sel.index)
                is CoverPhotoSelection.Gallery -> {
                    val jpeg = runCatching { coverImageProcessor.compress(sel.uri) }
                        .getOrElse {
                            error.value = "Cover photo could not be processed: ${it.message}"
                            isSaving.value = false
                            return@launch
                        }
                    storageRepository.uploadCover(ownerId, effectiveRegistryId, jpeg)
                        .getOrElse {
                            error.value = "Cover photo upload failed: ${it.message}"
                            isSaving.value = false
                            return@launch
                        }
                }
            }

            val now = System.currentTimeMillis()
            val registry = Registry(
                id = effectiveRegistryId,
                ownerId = ownerId,
                title = titleValue,
                occasion = occasion.value,
                visibility = visibility.value,
                eventDateMs = eventDateMs.value,
                eventLocation = eventLocation.value.ifBlank { null },
                description = description.value.ifBlank { null },
                locale = "en",
                notificationsEnabled = notificationsEnabled.value,
                imageUrl = resolvedImageUrl,
                createdAt = if (registryId == null) now else 0L,
                updatedAt = now
            )

            if (registryId == null) {
                createRegistryUseCase(registry).fold(
                    onSuccess = { newId -> savedRegistryId.value = newId },
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
