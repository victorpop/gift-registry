package com.giftregistry.ui.navigation

import kotlinx.serialization.Serializable

@Serializable data object AuthKey
@Serializable data object OnboardingKey
@Serializable data object HomeKey
@Serializable data object SettingsKey
@Serializable data object CreateRegistryKey
@Serializable data class RegistryDetailKey(val registryId: String)
@Serializable data class AddItemKey(val registryId: String)
@Serializable data class EditItemKey(val registryId: String, val itemId: String)
@Serializable data class EditRegistryKey(val registryId: String)
@Serializable data class ReReserveDeepLink(val reservationId: String)
@Serializable data class StoreListKey(val preSelectedRegistryId: String? = null)
@Serializable data class StoreBrowserKey(val storeId: String, val registryId: String?)
