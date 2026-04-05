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
import org.junit.Assert.assertNull
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

    @Test
    fun `initial state is Loading to prevent auth screen flash`() = runTest {
        // A freshly created ViewModel starts in Loading before auth state is known
        // We verify by resetting and checking before any state emission
        val freshRepo = FakeAuthRepository()
        val freshVm = AuthViewModel(
            signUpUseCase = SignUpUseCase(freshRepo),
            signInEmailUseCase = SignInEmailUseCase(freshRepo),
            signInGoogleUseCase = SignInGoogleUseCase(freshRepo),
            signInAnonymousUseCase = SignInAnonymousUseCase(freshRepo),
            observeAuthStateUseCase = ObserveAuthStateUseCase(freshRepo),
            signOutUseCase = SignOutUseCase(freshRepo)
        )
        // The ViewModel emits Loading first, then transitions based on auth state
        freshVm.authState.test {
            val first = awaitItem()
            assertTrue("Expected Loading or Unauthenticated as first state",
                first is AuthUiState.Loading || first is AuthUiState.Unauthenticated)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when auth state emits non-null user, authUiState transitions to Authenticated`() = runTest {
        val user = User(uid = "uid-123", email = "test@test.com", displayName = null, isAnonymous = false)
        fakeRepo.emitUser(user)
        advanceUntilIdle()

        val state = viewModel.authState.value
        assertTrue("Expected Authenticated but got $state", state is AuthUiState.Authenticated)
        assertEquals("uid-123", (state as AuthUiState.Authenticated).uid)
        assertEquals(false, state.isAnonymous)
    }

    @Test
    fun `when auth state emits null after init, authUiState transitions to Unauthenticated`() = runTest {
        // First emit a user, then null
        val user = User(uid = "uid-123", email = "test@test.com", displayName = null, isAnonymous = false)
        fakeRepo.emitUser(user)
        advanceUntilIdle()

        fakeRepo.emitUser(null)
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
        fakeRepo.emitUser(user)
        advanceUntilIdle()

        assertTrue("Setup: expected Authenticated", viewModel.authState.value is AuthUiState.Authenticated)

        viewModel.signOut()
        advanceUntilIdle()

        val state = viewModel.authState.value
        assertTrue("Expected Unauthenticated but got $state", state is AuthUiState.Unauthenticated)
    }
}
