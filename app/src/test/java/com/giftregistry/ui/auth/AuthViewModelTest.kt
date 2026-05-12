package com.giftregistry.ui.auth

import app.cash.turbine.test
import com.giftregistry.MainDispatcherRule
import com.giftregistry.data.auth.FakeAuthRepository
import com.giftregistry.domain.auth.ObserveAuthStateUseCase
import com.giftregistry.domain.auth.SignInAnonymousUseCase
import com.giftregistry.domain.auth.SignInEmailUseCase
import com.giftregistry.domain.auth.SignInGoogleUseCase
import com.giftregistry.domain.auth.SignOutUseCase
import com.giftregistry.domain.auth.SignUpUseCase
import com.giftregistry.domain.model.User
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepo: FakeAuthRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        fakeRepo = FakeAuthRepository()
        viewModel = AuthViewModel(
            signUpUseCase = SignUpUseCase(fakeRepo),
            signInEmailUseCase = SignInEmailUseCase(fakeRepo),
            signInGoogleUseCase = SignInGoogleUseCase(fakeRepo),
            signInAnonymousUseCase = SignInAnonymousUseCase(fakeRepo),
            observeAuthStateUseCase = ObserveAuthStateUseCase(fakeRepo),
            signOutUseCase = SignOutUseCase(fakeRepo)
        )
    }

    // -----------------------------------------------------------------------
    // Regression tests for BUG-AUTH-FLASH-260512 (cold-start AuthScreen flash)
    // -----------------------------------------------------------------------

    @Test
    fun `initial authState is Loading before any emission`() = runTest {
        val freshRepo = FakeAuthRepository()
        val freshVm = AuthViewModel(
            signUpUseCase = SignUpUseCase(freshRepo),
            signInEmailUseCase = SignInEmailUseCase(freshRepo),
            signInGoogleUseCase = SignInGoogleUseCase(freshRepo),
            signInAnonymousUseCase = SignInAnonymousUseCase(freshRepo),
            observeAuthStateUseCase = ObserveAuthStateUseCase(freshRepo),
            signOutUseCase = SignOutUseCase(freshRepo)
        )
        freshVm.authState.test {
            assertTrue(awaitItem() is AuthUiState.Loading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cold start with cached user — Initial(null) then Changed(user) never flashes Unauthenticated`() = runTest {
        // This is the BUG-AUTH-FLASH-260512 regression test.
        // Firebase Auth on cold start fires the listener synchronously with null (cache not
        // yet read), then fires again with the restored cached user. The ViewModel MUST stay
        // in Loading during the null gap — flipping to Unauthenticated would render AuthScreen
        // for ~1 second before the user emission arrives.
        val freshRepo = FakeAuthRepository()
        val freshVm = AuthViewModel(
            signUpUseCase = SignUpUseCase(freshRepo),
            signInEmailUseCase = SignInEmailUseCase(freshRepo),
            signInGoogleUseCase = SignInGoogleUseCase(freshRepo),
            signInAnonymousUseCase = SignInAnonymousUseCase(freshRepo),
            observeAuthStateUseCase = ObserveAuthStateUseCase(freshRepo),
            signOutUseCase = SignOutUseCase(freshRepo)
        )
        freshVm.authState.test {
            assertTrue("Initial state must be Loading", awaitItem() is AuthUiState.Loading)

            freshRepo.emitInitial(null)
            advanceUntilIdle()
            // CRITICAL: no Unauthenticated emission here — stays in Loading
            expectNoEvents()

            val cachedUser = User(uid = "cached-uid", email = "u@test.com", displayName = null, isAnonymous = false)
            freshRepo.emitChanged(cachedUser)
            advanceUntilIdle()
            val next = awaitItem()
            assertTrue("Expected Authenticated, got $next", next is AuthUiState.Authenticated)
            assertEquals("cached-uid", (next as AuthUiState.Authenticated).uid)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cold start with cached user delivered as Initial fast-path resolves immediately to Authenticated`() = runTest {
        val freshRepo = FakeAuthRepository()
        val freshVm = AuthViewModel(
            signUpUseCase = SignUpUseCase(freshRepo),
            signInEmailUseCase = SignInEmailUseCase(freshRepo),
            signInGoogleUseCase = SignInGoogleUseCase(freshRepo),
            signInAnonymousUseCase = SignInAnonymousUseCase(freshRepo),
            observeAuthStateUseCase = ObserveAuthStateUseCase(freshRepo),
            signOutUseCase = SignOutUseCase(freshRepo)
        )
        freshVm.authState.test {
            assertTrue(awaitItem() is AuthUiState.Loading)
            freshRepo.emitInitial(User(uid = "u1", email = null, displayName = null, isAnonymous = false))
            advanceUntilIdle()
            assertTrue(awaitItem() is AuthUiState.Authenticated)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cold start with no cached user — Initial(null) then Changed(null) settles to Unauthenticated`() = runTest {
        val freshRepo = FakeAuthRepository()
        val freshVm = AuthViewModel(
            signUpUseCase = SignUpUseCase(freshRepo),
            signInEmailUseCase = SignInEmailUseCase(freshRepo),
            signInGoogleUseCase = SignInGoogleUseCase(freshRepo),
            signInAnonymousUseCase = SignInAnonymousUseCase(freshRepo),
            observeAuthStateUseCase = ObserveAuthStateUseCase(freshRepo),
            signOutUseCase = SignOutUseCase(freshRepo)
        )
        freshVm.authState.test {
            assertTrue(awaitItem() is AuthUiState.Loading)
            freshRepo.emitInitial(null)
            advanceUntilIdle()
            expectNoEvents() // stays Loading — no Unauthenticated flash
            freshRepo.emitChanged(null)
            advanceUntilIdle()
            assertTrue(awaitItem() is AuthUiState.Unauthenticated)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // -----------------------------------------------------------------------
    // Existing tests (updated to use explicit emitChanged API where intent
    // is "Firebase reports a user post-attachment")
    // -----------------------------------------------------------------------

    @Test
    fun `when auth state emits non-null user, authUiState transitions to Authenticated`() = runTest {
        val user = User(uid = "uid-123", email = "test@test.com", displayName = null, isAnonymous = false)
        fakeRepo.emitChanged(user)
        advanceUntilIdle()

        val state = viewModel.authState.value
        assertTrue("Expected Authenticated but got $state", state is AuthUiState.Authenticated)
        assertEquals("uid-123", (state as AuthUiState.Authenticated).uid)
        assertEquals(false, state.isAnonymous)
    }

    @Test
    fun `runtime sign-out emits Unauthenticated`() = runTest {
        // First establish an authenticated state via Changed(user), then sign out
        val user = User(uid = "uid-123", email = "test@test.com", displayName = null, isAnonymous = false)
        fakeRepo.emitChanged(user)
        advanceUntilIdle()

        fakeRepo.emitChanged(null)
        advanceUntilIdle()

        val state = viewModel.authState.value
        assertTrue("Expected Unauthenticated but got $state", state is AuthUiState.Unauthenticated)
    }

    @Test
    fun `signUp success sets state to Authenticated`() = runTest {
        viewModel.updateEmail("user@example.com")
        viewModel.updatePassword("password123")
        viewModel.updateConfirmPassword("password123")

        viewModel.signUp()
        advanceUntilIdle()

        val state = viewModel.authState.value
        assertTrue("Expected Authenticated but got $state", state is AuthUiState.Authenticated)
    }

    @Test
    fun `signUp failure sets error message in formState`() = runTest {
        fakeRepo.shouldFail = true
        fakeRepo.failureException = Exception("Auth failed")

        viewModel.updateEmail("user@example.com")
        viewModel.updatePassword("password123")
        viewModel.updateConfirmPassword("password123")

        viewModel.signUp()
        advanceUntilIdle()

        val formState = viewModel.formState.value
        assertNotNull("Expected error message but got null", formState.errorMessage)
        assertEquals(false, formState.isLoading)
    }

    @Test
    fun `signIn success sets state to Authenticated`() = runTest {
        viewModel.updateEmail("user@example.com")
        viewModel.updatePassword("password123")

        viewModel.signIn()
        advanceUntilIdle()

        val state = viewModel.authState.value
        assertTrue("Expected Authenticated but got $state", state is AuthUiState.Authenticated)
    }

    @Test
    fun `signIn failure sets error message in formState`() = runTest {
        fakeRepo.shouldFail = true
        fakeRepo.failureException = Exception("Auth failed")

        viewModel.updateEmail("user@example.com")
        viewModel.updatePassword("password123")

        viewModel.signIn()
        advanceUntilIdle()

        val formState = viewModel.formState.value
        assertNotNull("Expected error message but got null", formState.errorMessage)
        assertEquals(false, formState.isLoading)
    }

    @Test
    fun `continueAsGuest success sets state to Authenticated with isAnonymous true`() = runTest {
        viewModel.continueAsGuest()
        advanceUntilIdle()

        val state = viewModel.authState.value
        assertTrue("Expected Authenticated but got $state", state is AuthUiState.Authenticated)
        assertTrue("Expected isAnonymous=true", (state as AuthUiState.Authenticated).isAnonymous)
    }

    @Test
    fun `signOut resets state to Unauthenticated`() = runTest {
        // First sign in to set authenticated state
        val user = User(uid = "uid-123", email = "test@test.com", displayName = null, isAnonymous = false)
        fakeRepo.emitChanged(user)
        advanceUntilIdle()

        assertTrue("Setup: expected Authenticated", viewModel.authState.value is AuthUiState.Authenticated)

        viewModel.signOut()
        advanceUntilIdle()

        val state = viewModel.authState.value
        assertTrue("Expected Unauthenticated but got $state", state is AuthUiState.Unauthenticated)
    }
}
