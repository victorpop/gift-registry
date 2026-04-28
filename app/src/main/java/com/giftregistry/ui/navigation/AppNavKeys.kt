package com.giftregistry.ui.navigation

import kotlinx.serialization.Serializable

@Serializable data object AuthKey
@Serializable data object OnboardingKey
@Serializable data object HomeKey
@Serializable data object SettingsKey
@Serializable data object CreateRegistryKey
@Serializable data class RegistryDetailKey(val registryId: String)
@Serializable data class AddItemKey(
    val registryId: String? = null,
    val initialUrl: String? = null,
    val initialRegistryId: String? = null,
    /**
     * True when AddItem was entered via the FAB Add-action sheet's "Add an item"
     * row (no registry chosen yet). When true, AddItemScreen renders a registry
     * picker as the first field and gates Save until a registry is picked.
     * Defaults to false for the existing CreateRegistry → AddItem chained path
     * and any other call site that already supplies a concrete registryId.
     */
    val fromAddSheet: Boolean = false,
)
@Serializable data class EditItemKey(val registryId: String, val itemId: String)
@Serializable data class EditRegistryKey(val registryId: String)
@Serializable data class ReReserveDeepLink(val reservationId: String)
@Serializable data class StoreListKey(val preSelectedRegistryId: String? = null)
@Serializable data class StoreBrowserKey(val storeId: String, val registryId: String?)
@Serializable data object NotificationsKey
