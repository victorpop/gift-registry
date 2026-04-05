package com.giftregistry.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giftregistry.domain.preferences.LanguagePreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val languagePrefsRepo: LanguagePreferencesRepository
) : ViewModel() {

    val currentLocale: StateFlow<String> = languagePrefsRepo.observeLanguageTag()
        .map { it ?: "en" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "en")

    fun changeLocale(languageTag: String) {
        viewModelScope.launch {
            languagePrefsRepo.setLanguageTag(languageTag)
        }
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(languageTag)
        )
    }
}
