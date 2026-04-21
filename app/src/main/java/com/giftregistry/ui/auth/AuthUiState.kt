package com.giftregistry.ui.auth

sealed interface AuthUiState {
    data object Loading : AuthUiState
    data object Unauthenticated : AuthUiState
    data class Authenticated(val uid: String, val isAnonymous: Boolean) : AuthUiState
}

data class AuthFormState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    // Plan 03: firstName / lastName fields for sign-up mode — stubs here unblock Wave 0
    // test compilation; Plan 03 wires these into the AuthScreen sign-up form.
    val firstName: String = "",
    val lastName: String = "",
)

/**
 * Default mode for the Auth screen. CONTEXT.md D-02 locks this to true (sign-up first).
 * Wave 0 stub: Plan 03 integrates this into AuthScreen.kt.
 */
const val AUTH_SCREEN_DEFAULT_IS_SIGN_UP_MODE: Boolean = true
