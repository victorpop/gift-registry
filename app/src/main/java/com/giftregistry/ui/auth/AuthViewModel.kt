package com.giftregistry.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.giftregistry.domain.auth.ObserveAuthStateUseCase
import com.giftregistry.domain.auth.SignInAnonymousUseCase
import com.giftregistry.domain.auth.SignInEmailUseCase
import com.giftregistry.domain.auth.SignInGoogleUseCase
import com.giftregistry.domain.auth.SignOutUseCase
import com.giftregistry.domain.auth.SignUpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val signUpUseCase: SignUpUseCase,
    private val signInEmailUseCase: SignInEmailUseCase,
    private val signInGoogleUseCase: SignInGoogleUseCase,
    private val signInAnonymousUseCase: SignInAnonymousUseCase,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val signOutUseCase: SignOutUseCase
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.Loading)
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    private val _formState = MutableStateFlow(AuthFormState())
    val formState: StateFlow<AuthFormState> = _formState.asStateFlow()

    init {
        viewModelScope.launch {
            // drop(1) skips the initial null from Firebase before session is restored.
            // This prevents a brief flash of the auth screen on app restart.
            observeAuthStateUseCase()
                .drop(1)
                .collect { user ->
                    _authState.value = if (user != null) {
                        AuthUiState.Authenticated(uid = user.uid, isAnonymous = user.isAnonymous)
                    } else {
                        AuthUiState.Unauthenticated
                    }
                }
        }
    }

    fun updateEmail(email: String) {
        _formState.value = _formState.value.copy(email = email, errorMessage = null)
    }

    fun updatePassword(password: String) {
        _formState.value = _formState.value.copy(password = password, errorMessage = null)
    }

    fun updateConfirmPassword(confirmPassword: String) {
        _formState.value = _formState.value.copy(confirmPassword = confirmPassword)
    }

    fun clearError() {
        _formState.value = _formState.value.copy(errorMessage = null)
    }

    fun signUp() {
        val state = _formState.value
        when {
            state.email.isBlank() -> {
                _formState.value = state.copy(errorMessage = "Email cannot be empty.")
                return
            }
            state.password.length < 8 -> {
                _formState.value = state.copy(errorMessage = "Password must be at least 8 characters.")
                return
            }
            state.password != state.confirmPassword -> {
                _formState.value = state.copy(errorMessage = "Passwords do not match.")
                return
            }
        }

        viewModelScope.launch {
            _formState.value = _formState.value.copy(isLoading = true, errorMessage = null)
            val result = signUpUseCase(state.email, state.password)
            result.fold(
                onSuccess = { _formState.value = _formState.value.copy(isLoading = false) },
                onFailure = { throwable ->
                    _formState.value = _formState.value.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "Sign up failed."
                    )
                }
            )
        }
    }

    fun signIn() {
        val state = _formState.value
        when {
            state.email.isBlank() -> {
                _formState.value = state.copy(errorMessage = "Email cannot be empty.")
                return
            }
            state.password.isBlank() -> {
                _formState.value = state.copy(errorMessage = "Password cannot be empty.")
                return
            }
        }

        viewModelScope.launch {
            _formState.value = _formState.value.copy(isLoading = true, errorMessage = null)
            val result = signInEmailUseCase(state.email, state.password)
            result.fold(
                onSuccess = { _formState.value = _formState.value.copy(isLoading = false) },
                onFailure = { throwable ->
                    _formState.value = _formState.value.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "Sign in failed."
                    )
                }
            )
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _formState.value = _formState.value.copy(isLoading = true, errorMessage = null)
            val result = signInGoogleUseCase(idToken)
            result.fold(
                onSuccess = { _formState.value = _formState.value.copy(isLoading = false) },
                onFailure = { throwable ->
                    _formState.value = _formState.value.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "Google sign in failed."
                    )
                }
            )
        }
    }

    fun continueAsGuest() {
        viewModelScope.launch {
            _formState.value = _formState.value.copy(isLoading = true, errorMessage = null)
            val result = signInAnonymousUseCase()
            result.fold(
                onSuccess = { _formState.value = _formState.value.copy(isLoading = false) },
                onFailure = { throwable ->
                    _formState.value = _formState.value.copy(
                        isLoading = false,
                        errorMessage = throwable.message ?: "Guest sign in failed."
                    )
                }
            )
        }
    }

    fun signOut() {
        signOutUseCase()
    }
}
