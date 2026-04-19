package com.giftregistry.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giftregistry.domain.auth.SignOutUseCase
import com.giftregistry.domain.preferences.LanguagePreferencesRepository
import com.giftregistry.domain.usecase.ObserveEmailLocaleUseCase
import com.giftregistry.domain.usecase.SetEmailLocaleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val languagePrefsRepo: LanguagePreferencesRepository,
    private val signOutUseCase: SignOutUseCase,
    private val observeEmailLocaleUseCase: ObserveEmailLocaleUseCase,
    private val setEmailLocaleUseCase: SetEmailLocaleUseCase,
) : ViewModel() {

    val currentLocale: StateFlow<String> = languagePrefsRepo.observeLanguageTag()
        .map { it ?: "en" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "en")

    /**
     * Phase 6 (UI-SPEC Contract 5): reactive view of users/{uid}.preferredLocale.
     * null until the Firestore snapshot arrives or if the user is not signed in.
     * Default "en" is applied in the UI display layer, not here.
     */
    val emailLocale: StateFlow<String?> = observeEmailLocaleUseCase()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun signOut() {
        signOutUseCase()
    }

    fun changeLocale(languageTag: String) {
        viewModelScope.launch {
            languagePrefsRepo.setLanguageTag(languageTag)
        }
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(languageTag)
        )
    }

    /**
     * Phase 6 (UI-SPEC Contract 5): write users/{uid}.preferredLocale.
     * Fire-and-forget; UI state reflects via emailLocale StateFlow.
     */
    fun onEmailLocaleChange(locale: String) {
        viewModelScope.launch {
            setEmailLocaleUseCase(locale)
        }
    }
}
