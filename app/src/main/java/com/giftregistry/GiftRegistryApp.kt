package com.giftregistry

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.giftregistry.domain.preferences.LanguagePreferencesRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

@HiltAndroidApp(Application::class)
class GiftRegistryApp : Hilt_GiftRegistryApp() {

    @Inject lateinit var languagePrefsRepo: LanguagePreferencesRepository

    override fun onCreate() {
        super.onCreate()
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
