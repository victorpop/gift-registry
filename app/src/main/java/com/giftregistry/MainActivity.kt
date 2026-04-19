package com.giftregistry

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigationevent.setViewTreeNavigationEventDispatcherOwner
import com.giftregistry.ui.navigation.AppNavigation
import com.giftregistry.ui.theme.GiftRegistryTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint(AppCompatActivity::class)
class MainActivity : Hilt_MainActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Persisted locale is restored in GiftRegistryApp.onCreate() before any
        // activity launches, so MainActivity only needs standard setup here.
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // AppCompatActivity 1.7.0 overrides setContentView and sets the view tree
        // owners manually (lifecycle/viewmodel/savedstate/back-pressed), bypassing
        // ComponentActivity.initializeViewTreeOwners(). That means it never sets the
        // ViewTreeNavigationEventDispatcherOwner that Navigation3's NavDisplay needs,
        // so wire it explicitly here. ComponentActivity already implements
        // NavigationEventDispatcherOwner in androidx.activity 1.12+.
        window.decorView.setViewTreeNavigationEventDispatcherOwner(this)

        // Handle deep link for registry invites (REG-08)
        val deepLinkRegistryId = intent?.data?.let { uri ->
            if (uri.host == "giftregistry.app" && uri.pathSegments.firstOrNull() == "registry") {
                uri.pathSegments.getOrNull(1)
            } else null
        }

        setContent {
            GiftRegistryTheme {
                AppNavigation(deepLinkRegistryId = deepLinkRegistryId)
            }
        }
    }
}
