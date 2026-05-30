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
     * row (quick-260428-iny) OR via the Discover small + button (quick-260530-nx5).
     * In both cases AddItemScreen renders the registry picker as the first field
     * and gates Save until a registry is picked.
     * Defaults to false for the existing CreateRegistry → AddItem chained path
     * and any other call site that already supplies a concrete registryId.
     */
    val fromAddSheet: Boolean = false,
    // quick-260530-nx5: optional pre-fill from a trusted upstream source
    // (e.g. Discover's Serper-supplied product data). When ANY of these
    // is non-blank, AddItemViewModel hydrates form state DIRECTLY and
    // SKIPS the OG-metadata Cloud Function (the upstream data is the
    // source of truth — re-fetching could overwrite it with worse data).
    val prefillTitle: String? = null,
    val prefillUrl: String? = null,
    val prefillImageUrl: String? = null,
    val prefillPrice: String? = null,
    val prefillRetailerName: String? = null,
    val prefillCurrency: String? = null,
)
@Serializable data class EditItemKey(val registryId: String, val itemId: String)
@Serializable data class EditRegistryKey(val registryId: String)
@Serializable data class ReReserveDeepLink(val reservationId: String)
@Serializable data object NotificationsKey
@Serializable data object DiscoverKey
