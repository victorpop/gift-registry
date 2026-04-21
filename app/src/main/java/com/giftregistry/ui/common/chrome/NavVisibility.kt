package com.giftregistry.ui.common.chrome

import com.giftregistry.ui.navigation.HomeKey
import com.giftregistry.ui.navigation.RegistryDetailKey

/**
 * CHROME-01: Returns true only for nav keys where the bottom nav bar should be visible.
 *
 * Visible on: HomeKey, RegistryDetailKey.
 * Hidden on: all other keys (AuthKey, OnboardingKey, CreateRegistryKey, EditRegistryKey,
 * AddItemKey, EditItemKey, StoreListKey, StoreBrowserKey, SettingsKey, NotificationsKey,
 * ReReserveDeepLink, null).
 *
 * Shipped as a stub in Plan 02 to unblock compilation of Wave 0 BottomNavVisibilityTest.
 * Plan 03 wires this into GiftMaisonBottomNav.kt.
 */
fun Any?.showsBottomNav(): Boolean = when (this) {
    is HomeKey           -> true
    is RegistryDetailKey -> true
    else                 -> false
}
