package com.giftregistry.ui.common.chrome

import com.giftregistry.ui.navigation.AddItemKey
import com.giftregistry.ui.navigation.AuthKey
import com.giftregistry.ui.navigation.CreateRegistryKey
import com.giftregistry.ui.navigation.EditItemKey
import com.giftregistry.ui.navigation.EditRegistryKey
import com.giftregistry.ui.navigation.HomeKey
import com.giftregistry.ui.navigation.NotificationsKey
import com.giftregistry.ui.navigation.OnboardingKey
import com.giftregistry.ui.navigation.ReReserveDeepLink
import com.giftregistry.ui.navigation.RegistryDetailKey
import com.giftregistry.ui.navigation.SettingsKey
import com.giftregistry.ui.navigation.StoreBrowserKey
import com.giftregistry.ui.navigation.StoreListKey
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * CHROME-01 (quick-260428-v0q): bottom nav HIDDEN only on the 4-case whitelist
 * (null, AuthKey, OnboardingKey, ReReserveDeepLink). Every other nav key in
 * AppNavKeys.kt is post-auth and shows the nav bar.
 *
 * This test pins the new contract; flipping any of the assertTrue cases
 * regresses the user's reported bug (Settings/Notifications/Stores/forms hide
 * the nav).
 */
class BottomNavVisibilityTest {

    // ---- HIDDEN cases (4) ----

    @Test fun nullKey_hidesNav() = assertFalse((null as Any?).showsBottomNav())
    @Test fun authKey_hidesNav() = assertFalse(AuthKey.showsBottomNav())
    @Test fun onboardingKey_hidesNav() = assertFalse(OnboardingKey.showsBottomNav())
    @Test fun reReserveDeepLink_hidesNav() =
        assertFalse(ReReserveDeepLink("rs1").showsBottomNav())

    // ---- VISIBLE cases (9) — every post-auth key in AppNavKeys.kt ----

    @Test fun homeKey_showsNav() = assertTrue(HomeKey.showsBottomNav())
    @Test fun registryDetailKey_showsNav() =
        assertTrue(RegistryDetailKey("r1").showsBottomNav())
    @Test fun settingsKey_showsNav() = assertTrue(SettingsKey.showsBottomNav())
    @Test fun notificationsKey_showsNav() = assertTrue(NotificationsKey.showsBottomNav())
    @Test fun createRegistryKey_showsNav() = assertTrue(CreateRegistryKey.showsBottomNav())
    @Test fun editRegistryKey_showsNav() =
        assertTrue(EditRegistryKey("r1").showsBottomNav())
    @Test fun addItemKey_showsNav() = assertTrue(AddItemKey("r1").showsBottomNav())
    @Test fun editItemKey_showsNav() =
        assertTrue(EditItemKey("r1", "i1").showsBottomNav())
    @Test fun storeListKey_showsNav() =
        assertTrue(StoreListKey(preSelectedRegistryId = null).showsBottomNav())
    @Test fun storeBrowserKey_showsNav() =
        assertTrue(StoreBrowserKey(storeId = "s1", registryId = null).showsBottomNav())
}
