package com.giftregistry.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.giftregistry.R
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(viewModel: AuthViewModel = hiltViewModel()) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var signInPasswordVisible by remember { mutableStateOf(false) }
    var signUpPasswordVisible by remember { mutableStateOf(false) }
    var signUpConfirmPasswordVisible by remember { mutableStateOf(false) }

    // Clear error on tab switch
    LaunchedEffect(selectedTabIndex) {
        viewModel.clearError()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App title
            Spacer(modifier = Modifier.height(64.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Tab row
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text(stringResource(R.string.auth_sign_in_title)) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text(stringResource(R.string.auth_sign_up_title)) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tab content
            AnimatedContent(targetState = selectedTabIndex, label = "auth_tab_content") { tabIndex ->
                when (tabIndex) {
                    0 -> {
                        // Sign In tab
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Email field
                            OutlinedTextField(
                                value = formState.email,
                                onValueChange = { viewModel.updateEmail(it) },
                                label = { Text(stringResource(R.string.auth_email_label)) },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Password field
                            OutlinedTextField(
                                value = formState.password,
                                onValueChange = { viewModel.updatePassword(it) },
                                label = { Text(stringResource(R.string.auth_password_label)) },
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = if (signInPasswordVisible) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                trailingIcon = {
                                    IconButton(onClick = { signInPasswordVisible = !signInPasswordVisible }) {
                                        Icon(
                                            imageVector = if (signInPasswordVisible) {
                                                Icons.Default.VisibilityOff
                                            } else {
                                                Icons.Default.Visibility
                                            },
                                            contentDescription = stringResource(
                                                if (signInPasswordVisible) R.string.auth_password_hide
                                                else R.string.auth_password_show
                                            )
                                        )
                                    }
                                },
                                supportingText = if (formState.errorMessage != null) {
                                    {
                                        Text(
                                            text = formState.errorMessage!!,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                } else null,
                                isError = formState.errorMessage != null,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Sign In button
                            Button(
                                onClick = { viewModel.signIn() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !formState.isLoading
                            ) {
                                if (formState.isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                } else {
                                    Text(
                                        text = stringResource(R.string.auth_sign_in_button),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // OR divider
                            OrDivider()

                            Spacer(modifier = Modifier.height(16.dp))

                            // Google Sign In button
                            GoogleSignInButton(
                                isLoading = formState.isLoading,
                                onGoogleSignIn = { idToken -> viewModel.signInWithGoogle(idToken) },
                                onError = { message ->
                                    scope.launch { snackbarHostState.showSnackbar(message) }
                                },
                                context = context
                            )
                        }
                    }
                    1 -> {
                        // Create Account tab
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Email field
                            OutlinedTextField(
                                value = formState.email,
                                onValueChange = { viewModel.updateEmail(it) },
                                label = { Text(stringResource(R.string.auth_email_label)) },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Password field
                            OutlinedTextField(
                                value = formState.password,
                                onValueChange = { viewModel.updatePassword(it) },
                                label = { Text(stringResource(R.string.auth_password_label)) },
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = if (signUpPasswordVisible) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                trailingIcon = {
                                    IconButton(onClick = { signUpPasswordVisible = !signUpPasswordVisible }) {
                                        Icon(
                                            imageVector = if (signUpPasswordVisible) {
                                                Icons.Default.VisibilityOff
                                            } else {
                                                Icons.Default.Visibility
                                            },
                                            contentDescription = stringResource(
                                                if (signUpPasswordVisible) R.string.auth_password_hide
                                                else R.string.auth_password_show
                                            )
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Next
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Confirm Password field
                            OutlinedTextField(
                                value = formState.confirmPassword,
                                onValueChange = { viewModel.updateConfirmPassword(it) },
                                label = { Text(stringResource(R.string.auth_confirm_password_label)) },
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = if (signUpConfirmPasswordVisible) {
                                    VisualTransformation.None
                                } else {
                                    PasswordVisualTransformation()
                                },
                                trailingIcon = {
                                    IconButton(onClick = { signUpConfirmPasswordVisible = !signUpConfirmPasswordVisible }) {
                                        Icon(
                                            imageVector = if (signUpConfirmPasswordVisible) {
                                                Icons.Default.VisibilityOff
                                            } else {
                                                Icons.Default.Visibility
                                            },
                                            contentDescription = stringResource(
                                                if (signUpConfirmPasswordVisible) R.string.auth_password_hide
                                                else R.string.auth_password_show
                                            )
                                        )
                                    }
                                },
                                supportingText = if (formState.errorMessage != null) {
                                    {
                                        Text(
                                            text = formState.errorMessage!!,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                } else null,
                                isError = formState.errorMessage != null,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Create Account button
                            Button(
                                onClick = { viewModel.signUp() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !formState.isLoading
                            ) {
                                if (formState.isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                } else {
                                    Text(
                                        text = stringResource(R.string.auth_sign_up_button),
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // OR divider
                            OrDivider()

                            Spacer(modifier = Modifier.height(16.dp))

                            // Google Sign In button
                            GoogleSignInButton(
                                isLoading = formState.isLoading,
                                onGoogleSignIn = { idToken -> viewModel.signInWithGoogle(idToken) },
                                onError = { message ->
                                    scope.launch { snackbarHostState.showSnackbar(message) }
                                },
                                context = context
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Continue as Guest link
            TextButton(
                onClick = { viewModel.continueAsGuest() },
                modifier = Modifier.defaultMinSize(minHeight = 44.dp)
            ) {
                Text(
                    text = stringResource(R.string.auth_continue_as_guest),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun OrDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(R.string.auth_or_divider),
            modifier = Modifier.padding(horizontal = 8.dp),
            style = MaterialTheme.typography.bodyLarge
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun GoogleSignInButton(
    isLoading: Boolean,
    onGoogleSignIn: (String) -> Unit,
    onError: (String) -> Unit,
    context: android.content.Context
) {
    val scope = rememberCoroutineScope()

    OutlinedButton(
        onClick = {
            scope.launch {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(context.getString(R.string.default_web_client_id))
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                try {
                    val result = credentialManager.getCredential(context = context, request = request)
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                    onGoogleSignIn(googleIdTokenCredential.idToken)
                } catch (e: GetCredentialException) {
                    onError(e.message ?: "Google sign in failed.")
                } catch (e: Exception) {
                    onError(e.message ?: "Google sign in failed.")
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading
    ) {
        Text(
            text = stringResource(R.string.auth_google_sign_in_button),
            style = MaterialTheme.typography.labelLarge
        )
    }
}
