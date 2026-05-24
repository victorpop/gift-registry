package com.giftregistry

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.giftregistry.domain.preferences.LanguagePreferencesRepository
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

@HiltAndroidApp(Application::class)
class GiftRegistryApp : Hilt_GiftRegistryApp() {

    @Inject lateinit var languagePrefsRepo: LanguagePreferencesRepository

    override fun onCreate() {
        super.onCreate()
        // App Check provider wiring. Debug builds use the DebugAppCheckProvider;
        // each device's auto-generated debug token must be registered in Firebase
        // Console (App Check → Apps → Manage debug tokens). Release builds use
        // PlayIntegrity. MUST be installed before any other Firebase SDK call.
        val appCheck = FirebaseAppCheck.getInstance()
        if (BuildConfig.DEBUG) {
            appCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance(),
            )
        } else {
            appCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance(),
            )
        }

        // Restore persisted locale before any activity is launched to prevent flicker.
        // Hilt field injection completes during super.onCreate() above, so the repo is
        // available here (unlike in MainActivity.onCreate before its super call).
        val savedLocale = runBlocking { languagePrefsRepo.getLanguageTag() }
        if (savedLocale != null) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(savedLocale)
            )
        }
    }
}
