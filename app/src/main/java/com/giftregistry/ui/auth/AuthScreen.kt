package com.giftregistry.ui.auth

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giftregistry.R
import com.giftregistry.ui.common.FocusedFieldCaret
import com.giftregistry.ui.theme.GiftMaisonColors
import com.giftregistry.ui.theme.GiftMaisonTheme
import com.giftregistry.ui.theme.GiftMaisonWordmark
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(viewModel: AuthViewModel = hiltViewModel()) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    val shapes = GiftMaisonTheme.shapes
    val spacing = GiftMaisonTheme.spacing
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var passwordVisible by remember { mutableStateOf(false) }
    var isSignUpMode by remember { mutableStateOf(AUTH_SCREEN_DEFAULT_IS_SIGN_UP_MODE) }

    val emailFocusInteractionSource = remember { MutableInteractionSource() }
    val emailIsFocused by emailFocusInteractionSource.collectIsFocusedAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.paper)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.edge),
    ) {
        // Top bar: wordmark
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.gap16, bottom = spacing.gap20),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GiftMaisonWordmark()
        }

        // Headline + subline
        AuthHeadline()
        Spacer(modifier = Modifier.height(spacing.gap8))
        Text(
            text = stringResource(R.string.auth_subline),
            style = typography.bodyM,
            color = colors.inkSoft,
        )

        Spacer(modifier = Modifier.height(spacing.gap20))

        // Google banner — CredentialManager coroutine inside onClick
        GoogleBanner(
            onClick = {
                scope.launch {
                    try {
                        val credentialManager = CredentialManager.create(context)
                        val googleIdOption = GetGoogleIdOption.Builder()
                            .setFilterByAuthorizedAccounts(false)
                            .setServerClientId(context.getString(R.string.default_web_client_id))
                            .build()
                        val request = GetCredentialRequest.Builder()
                            .addCredentialOption(googleIdOption)
                            .build()
                        val result = credentialManager.getCredential(context = context, request = request)
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                        viewModel.signInWithGoogle(googleIdTokenCredential.idToken)
                    } catch (e: GetCredentialException) {
                        // Error surfaces via formState.errorMessage if propagated; silent otherwise
                    } catch (e: Exception) {
                        // Silent — Google sign-in failures don't need a toast in redesigned flow
                    }
                }
            },
        )

        Spacer(modifier = Modifier.height(spacing.gap20))

        // Divider with mode-aware label
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = colors.line)
            Text(
                text = stringResource(
                    if (isSignUpMode) R.string.auth_or_sign_up_email_divider
                    else R.string.auth_or_sign_in_email_divider
                ),
                modifier = Modifier.padding(horizontal = spacing.gap12),
                style = typography.bodyS,
                color = colors.inkFaint,
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = colors.line)
        }

        Spacer(modifier = Modifier.height(spacing.gap16))

        if (isSignUpMode) {
            // First name + Last name row (sign-up only)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.gap8),
            ) {
                OutlinedTextField(
                    value = formState.firstName,
                    onValueChange = viewModel::updateFirstName,
                    label = { Text(stringResource(R.string.auth_first_name_label)) },
                    placeholder = { Text(stringResource(R.string.auth_first_name_placeholder)) },
                    modifier = Modifier.weight(1f),
                    shape = shapes.radius12,
                    colors = fieldColors(colors),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                OutlinedTextField(
                    value = formState.lastName,
                    onValueChange = viewModel::updateLastName,
                    label = { Text(stringResource(R.string.auth_last_name_label)) },
                    placeholder = { Text(stringResource(R.string.auth_last_name_placeholder)) },
                    modifier = Modifier.weight(1f),
                    shape = shapes.radius12,
                    colors = fieldColors(colors),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
            }
            Spacer(modifier = Modifier.height(spacing.gap12))
        }

        // Email field with FocusedFieldCaret in trailingIcon slot
        OutlinedTextField(
            value = formState.email,
            onValueChange = viewModel::updateEmail,
            label = { Text(stringResource(R.string.auth_email_label)) },
            placeholder = { Text(stringResource(R.string.auth_email_placeholder_new)) },
            modifier = Modifier.fillMaxWidth(),
            shape = shapes.radius12,
            colors = fieldColors(colors),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            interactionSource = emailFocusInteractionSource,
            trailingIcon = {
                FocusedFieldCaret(
                    isFocused = emailIsFocused && formState.email.isEmpty(),
                    color = colors.accent,
                )
            },
        )

        Spacer(modifier = Modifier.height(spacing.gap12))

        // Password field with eye toggle
        OutlinedTextField(
            value = formState.password,
            onValueChange = viewModel::updatePassword,
            label = { Text(stringResource(R.string.auth_password_label)) },
            modifier = Modifier.fillMaxWidth(),
            shape = shapes.radius12,
            colors = fieldColors(colors),
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = if (isSignUpMode) ImeAction.Next else ImeAction.Done,
            ),
            supportingText = if (isSignUpMode) {
                @Composable { Text(stringResource(R.string.auth_password_helper), style = typography.bodyXS, color = colors.inkFaint) }
            } else null,
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = stringResource(
                            if (passwordVisible) R.string.auth_password_hide else R.string.auth_password_show
                        ),
                        tint = colors.inkFaint,
                    )
                }
            },
        )

        if (isSignUpMode) {
            Spacer(modifier = Modifier.height(spacing.gap12))
            OutlinedTextField(
                value = formState.confirmPassword,
                onValueChange = viewModel::updateConfirmPassword,
                label = { Text(stringResource(R.string.auth_confirm_password_label)) },
                modifier = Modifier.fillMaxWidth(),
                shape = shapes.radius12,
                colors = fieldColors(colors),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            )
        }

        // Inline error banner
        formState.errorMessage?.let { msg ->
            Spacer(modifier = Modifier.height(spacing.gap12))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shapes.radius12)
                    .background(colors.warn.copy(alpha = 0.15f))
                    .padding(spacing.gap12),
            ) {
                Text(text = msg, style = typography.bodyM, color = colors.inkSoft)
            }
        }

        Spacer(modifier = Modifier.height(spacing.gap20))

        // Sign up / Log in primary CTA (ink pill)
        Button(
            onClick = {
                if (isSignUpMode) viewModel.signUp() else viewModel.signIn()
            },
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp),
            shape = shapes.pill,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.ink,
                contentColor = colors.paper,
                disabledContainerColor = colors.ink.copy(alpha = 0.38f),
                disabledContentColor = colors.paper.copy(alpha = 0.38f),
            ),
            enabled = !formState.isLoading,
        ) {
            if (formState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = colors.paper, strokeWidth = 2.dp)
            } else {
                Text(
                    text = stringResource(if (isSignUpMode) R.string.auth_signup_cta else R.string.auth_login_cta),
                    style = typography.bodyL,
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.gap12))

        // Terms line — tappable Terms + Privacy spans
        val termsLabel = stringResource(R.string.auth_terms_link)
        val privacyLabel = stringResource(R.string.auth_privacy_link)
        val termsLine = buildAnnotatedString {
            val template = context.getString(R.string.auth_terms_line, termsLabel, privacyLabel)
            append(template)
            val termsStart = template.indexOf(termsLabel)
            if (termsStart >= 0) {
                addStyle(SpanStyle(color = colors.accent, textDecoration = TextDecoration.Underline), termsStart, termsStart + termsLabel.length)
                addStringAnnotation("url", "terms", termsStart, termsStart + termsLabel.length)
            }
            val privStart = template.indexOf(privacyLabel)
            if (privStart >= 0) {
                addStyle(SpanStyle(color = colors.accent, textDecoration = TextDecoration.Underline), privStart, privStart + privacyLabel.length)
                addStringAnnotation("url", "privacy", privStart, privStart + privacyLabel.length)
            }
        }
        Text(
            text = termsLine,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val termsUrl = context.getString(R.string.auth_terms_url)
                    if (termsUrl.isNotBlank()) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(termsUrl)))
                    }
                },
            style = typography.bodyXS,
            color = colors.inkFaint,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(spacing.gap16))

        // Footer ghost pill — mode toggle
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            TextButton(
                onClick = {
                    isSignUpMode = !isSignUpMode
                    viewModel.clearError()
                },
                modifier = Modifier
                    .defaultMinSize(minHeight = 44.dp)
                    .clip(shapes.pill),
            ) {
                Text(
                    text = stringResource(if (isSignUpMode) R.string.auth_login_footer else R.string.auth_signup_footer),
                    style = typography.bodyMEmphasis,
                    color = colors.ink,
                )
            }
        }

        // Tertiary guest link — preserves AUTH-05 (CONTEXT.md constraint)
        TextButton(
            onClick = { viewModel.continueAsGuest() },
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 44.dp),
        ) {
            Text(
                text = stringResource(R.string.auth_guest_tertiary_link),
                style = typography.bodyXS,
                color = colors.inkFaint,
            )
        }

        Spacer(modifier = Modifier.height(spacing.gap20))
    }
}

@Composable
private fun fieldColors(colors: GiftMaisonColors) =
    OutlinedTextFieldDefaults.colors(
        focusedContainerColor = colors.paperDeep,
        unfocusedContainerColor = colors.paperDeep,
        disabledContainerColor = colors.paperDeep,
        focusedBorderColor = colors.accent,
        unfocusedBorderColor = colors.line,
        cursorColor = colors.accent,
        focusedLabelColor = colors.accent,
        unfocusedLabelColor = colors.inkFaint,
        focusedTextColor = colors.ink,
        unfocusedTextColor = colors.ink,
        focusedPlaceholderColor = colors.inkFaint,
        unfocusedPlaceholderColor = colors.inkFaint,
    )
