package com.giftregistry

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
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

        // Phase 6: register POST_NOTIFICATIONS launcher at Activity level (Pitfall 5).
        // Must be registered before setContent (before the activity is started).
        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* no-op: best-effort ask; server continues to register tokens regardless */ }

        setContent {
            GiftRegistryTheme {
                val context = LocalContext.current
                // Phase 6: request POST_NOTIFICATIONS on Android 13+ (TIRAMISU / API 33+).
                // Deferred ask — fires once when MainActivity root composable first enters
                // composition, which covers owner-surface entry per RESEARCH Pitfall 5.
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!granted) {
                            permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }
                AppNavigation(deepLinkRegistryId = deepLinkRegistryId)
            }
        }
    }
}
