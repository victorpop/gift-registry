package com.giftregistry.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giftregistry.domain.preferences.OnboardingPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface OnboardingSeenState {
    data object Loading : OnboardingSeenState
    data object NotSeen : OnboardingSeenState
    data object Seen : OnboardingSeenState
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repo: OnboardingPreferencesRepository
) : ViewModel() {

    // SharingStarted.Eagerly is intentional (mirrors Phase 02 SettingsViewModel.currentLocale
    // decision): the flag must resolve before the first auth-routing frame to avoid a
    // carousel-flash on session restore / app relaunch.
    val state: StateFlow<OnboardingSeenState> = repo.observeOnboardingSeen()
        .map { seen -> if (seen) OnboardingSeenState.Seen else OnboardingSeenState.NotSeen }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = OnboardingSeenState.Loading,
        )

    fun markSeen() {
        viewModelScope.launch { repo.setOnboardingSeen() }
    }
}
