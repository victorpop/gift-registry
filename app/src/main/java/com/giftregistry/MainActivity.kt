package com.giftregistry

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.giftregistry.domain.preferences.LanguagePreferencesRepository
import com.giftregistry.ui.navigation.AppNavigation
import com.giftregistry.ui.theme.GiftRegistryTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint(AppCompatActivity::class)
class MainActivity : Hilt_MainActivity() {

    @Inject lateinit var languagePrefsRepo: LanguagePreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        // Restore persisted locale synchronously BEFORE super to prevent flicker
        val savedLocale = runBlocking {
            languagePrefsRepo.getLanguageTag()
        }
        if (savedLocale != null) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(savedLocale)
            )
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GiftRegistryTheme {
                AppNavigation()
            }
        }
    }
}
