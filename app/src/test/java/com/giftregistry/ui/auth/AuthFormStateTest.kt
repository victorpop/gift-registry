package com.giftregistry.ui.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * SCR-06: AuthFormState must expose firstName/lastName fields for sign-up mode,
 * and the screen's default mode must be SIGN UP (isSignUpMode = true).
 *
 * CONTEXT.md locked decision: "Default mode: Sign up — per handoff."
 * Current AuthScreen.kt has `var isSignUpMode by remember { mutableStateOf(false) }`
 * — Plan 03 flips this to true and exposes the default via a public top-level
 * `const val AUTH_SCREEN_DEFAULT_IS_SIGN_UP_MODE = true` so this test can pin it.
 *
 * RED in Wave 0 — flips GREEN when Plan 03 extends AuthFormState and exposes
 * AUTH_SCREEN_DEFAULT_IS_SIGN_UP_MODE in com.giftregistry.ui.auth.
 */
class AuthFormStateTest {

    @Test fun defaultInstance_firstNameIsBlank() =
        assertEquals("", AuthFormState().firstName)

    @Test fun defaultInstance_lastNameIsBlank() =
        assertEquals("", AuthFormState().lastName)

    @Test fun copy_firstName_updatesOnlyFirstName() {
        val original = AuthFormState(email = "a@b.com")
        val updated = original.copy(firstName = "Ana")
        assertEquals("Ana", updated.firstName)
        assertEquals("a@b.com", updated.email)   // regression guard
    }

    @Test fun copy_lastName_updatesOnlyLastName() {
        val original = AuthFormState(email = "a@b.com", firstName = "Ana")
        val updated = original.copy(lastName = "Popescu")
        assertEquals("Popescu", updated.lastName)
        assertEquals("Ana", updated.firstName)  // regression guard
        assertEquals("a@b.com", updated.email)
    }

    @Test fun existingFieldsPreserved() {
        val s = AuthFormState()
        assertEquals("", s.email)
        assertEquals("", s.password)
        assertEquals("", s.confirmPassword)
        assertFalse(s.isLoading)
        assertNull(s.errorMessage)
    }

    @Test fun defaultIsSignUpMode_isTrue() =
        assertTrue(
            "Per CONTEXT.md D-02: Auth screen must default to sign-up mode on first launch",
            AUTH_SCREEN_DEFAULT_IS_SIGN_UP_MODE
        )
}
