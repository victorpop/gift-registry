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
        // ----- D-20 / Phase 14 follow-up: App Check provider wiring -----
        // Phase 16 callables (acceptInvite, declineInvite) enforce App Check.
        // Phase 14 web side already uses reCAPTCHA v3; Android side is wired here.
        //
        // Debug builds use the DebugAppCheckProvider so the Functions emulator
        // and physical device debug builds work without Play Integrity tokens.
        // Release builds use PlayIntegrity (Play Integrity API auto-enrolls
        // installed builds — no Play Store presence required for sideload).
        //
        // MUST be installed BEFORE any other Firebase SDK call to ensure the
        // App Check token is attached to the FIRST request.
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
