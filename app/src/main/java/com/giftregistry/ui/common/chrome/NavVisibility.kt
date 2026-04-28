package com.giftregistry.ui.common.chrome

import com.giftregistry.ui.navigation.AuthKey
import com.giftregistry.ui.navigation.OnboardingKey
import com.giftregistry.ui.navigation.ReReserveDeepLink

/**
 * CHROME-01 (quick-260428-v0q): Returns true on every authenticated destination.
 * Bottom nav is persistent across the app; only pre-auth flow + the transitional
 * re-reserve resolver + the null/loading fallback hide it.
 *
 * Hidden on:
 *  - null              — pre-resolution / loading state (backStack.lastOrNull() == null)
 *  - AuthKey           — pre-auth login/sign-up screen
 *  - OnboardingKey     — pre-auth onboarding carousel
 *  - ReReserveDeepLink — transitional resolver that auto-routes to a real destination
 *
 * Visible on: every other nav key (HomeKey, RegistryDetailKey, SettingsKey,
 * NotificationsKey, CreateRegistryKey, EditRegistryKey, AddItemKey, EditItemKey,
 * StoreListKey, StoreBrowserKey, and any future post-auth key by default).
 *
 * History:
 *  - Plan 09-02: introduced predicate as visible-whitelist HomeKey+RegistryDetailKey only.
 *  - quick-260428-v0q: inverted to hidden-whitelist per user request — Settings,
 *    Notifications, Stores, and all forms keep the nav bar.
 */
fun Any?.showsBottomNav(): Boolean = when (this) {
    null                 -> false
    is AuthKey           -> false
    is OnboardingKey     -> false
    is ReReserveDeepLink -> false
    else                 -> true
}
