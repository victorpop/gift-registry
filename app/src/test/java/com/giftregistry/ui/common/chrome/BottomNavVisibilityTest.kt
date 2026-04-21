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
 * CHROME-01: bottom nav visible ONLY on HomeKey + RegistryDetailKey.
 * Hidden on all 11 other keys (per CONTEXT.md locked decision).
 * RED in Wave 0 — flips GREEN when Plan 03 ships `Any?.showsBottomNav()` in
 * `com.giftregistry.ui.common.chrome`.
 */
class BottomNavVisibilityTest {
    @Test fun homeKey_showsNav() = assertTrue(HomeKey.showsBottomNav())
    @Test fun registryDetailKey_showsNav() =
        assertTrue(RegistryDetailKey("r1").showsBottomNav())

    @Test fun authKey_hidesNav() = assertFalse(AuthKey.showsBottomNav())
    @Test fun onboardingKey_hidesNav() = assertFalse(OnboardingKey.showsBottomNav())
    @Test fun createRegistryKey_hidesNav() = assertFalse(CreateRegistryKey.showsBottomNav())
    @Test fun editRegistryKey_hidesNav() =
        assertFalse(EditRegistryKey("r1").showsBottomNav())
    @Test fun addItemKey_hidesNav() = assertFalse(AddItemKey("r1").showsBottomNav())
    @Test fun editItemKey_hidesNav() =
        assertFalse(EditItemKey("r1", "i1").showsBottomNav())
    @Test fun storeListKey_hidesNav() =
        assertFalse(StoreListKey(preSelectedRegistryId = null).showsBottomNav())
    @Test fun storeBrowserKey_hidesNav() =
        assertFalse(StoreBrowserKey(storeId = "s1", registryId = null).showsBottomNav())
    @Test fun settingsKey_hidesNav() = assertFalse(SettingsKey.showsBottomNav())
    @Test fun notificationsKey_hidesNav() = assertFalse(NotificationsKey.showsBottomNav())
    @Test fun reReserveDeepLink_hidesNav() =
        assertFalse(ReReserveDeepLink("rs1").showsBottomNav())
    @Test fun nullKey_hidesNav() = assertFalse((null as Any?).showsBottomNav())
}
