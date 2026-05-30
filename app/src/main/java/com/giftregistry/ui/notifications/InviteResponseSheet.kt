package com.giftregistry.ui.notifications

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giftregistry.R
import com.giftregistry.domain.model.Notification
import com.giftregistry.domain.model.NotificationType
import com.giftregistry.ui.registry.cover.HeroImageOrPlaceholder
import com.giftregistry.ui.theme.GiftMaisonTheme

/**
 * D-11 — Branching predicate for inbox card tap.
 *
 * Returns true iff the notification is a post-Phase-16 INVITE carrying a
 * pendingEntryKey payload field. Legacy INVITE notifications (pre-Phase-16)
 * lack this field and fall back to the navigate-to-registry behaviour.
 *
 * Extracted as a top-level pure-Kotlin fun for testability (see
 * NotificationCardBranchingTest).
 */
fun shouldOpenInviteSheet(notification: Notification): Boolean =
    notification.type == NotificationType.INVITE &&
        notification.payload["pendingEntryKey"] != null

/**
 * D-01 — ModalBottomSheet hosting the registry hero + Accept/Decline CTAs.
 *
 * Renders from the notification payload alone (zero registry-doc read on the
 * client — D-10). Payload fields consumed: coverUrl (nullable), occasion
 * (nullable), registryName, actorName, eventDateMs (optional, String of millis).
 *
 * D-05: On AcceptedSuccess, parent navigates to RegistryDetailKey via
 * onAcceptSuccess(registryId).
 * D-07: Sheet stays open during in-flight callable (confirmValueChange guard)
 * and on Error (warn-banner + Retry); swipe-dismiss is allowed in Idle and
 * Error states.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteResponseSheet(
    registryId: String,
    payload: Map<String, String?>,
    onAcceptSuccess: (registryId: String) -> Unit,
    onDismiss: () -> Unit,
    viewModel: InviteResponseViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isLoading = state is InviteResponseViewModel.State.Submitting

    // D-07: block swipe-dismiss + onDismissRequest while a callable is in-flight.
    // skipPartiallyExpanded=true: sheet goes straight to its content-sized resting
    // height in one animation rather than landing in the half-expanded state and
    // requiring the user to drag up to reveal content.
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { state !is InviteResponseViewModel.State.Submitting },
    )

    LaunchedEffect(state) {
        when (state) {
            is InviteResponseViewModel.State.AcceptedSuccess -> {
                onAcceptSuccess(registryId)
            }
            is InviteResponseViewModel.State.DeclinedSuccess -> {
                onDismiss()
            }
            else -> Unit
        }
    }

    // Reset to Idle when the sheet leaves composition mid-error so the next
    // open starts fresh.
    DisposableEffect(Unit) {
        onDispose {
            if (state is InviteResponseViewModel.State.Error) viewModel.reset()
        }
    }

    val colors = GiftMaisonTheme.colors
    val typography = GiftMaisonTheme.typography
    val spacing = GiftMaisonTheme.spacing

    ModalBottomSheet(
        onDismissRequest = { if (!isLoading) onDismiss() },
        sheetState = sheetState,
        containerColor = colors.paper,
    ) {
        var showDeclineDialog by rememberSaveable { mutableStateOf(false) }

        Column(modifier = Modifier.fillMaxWidth()) {
            // Hero — full-bleed, 180 dp tall (matches Registry Detail hero contract)
            HeroImageOrPlaceholder(
                imageUrl = payload["coverUrl"],
                occasion = payload["occasion"],
                glyphSize = 40.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            )

            Spacer(modifier = Modifier.height(spacing.gap20))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.edgeWide),
            ) {
                val actorName = payload["actorName"]
                    ?: stringResource(R.string.invite_sheet_default_actor)
                val registryName = payload["registryName"]
                    ?: stringResource(R.string.invite_sheet_default_registry)

                Text(
                    text = stringResource(R.string.invite_sheet_title_template, actorName),
                    style = typography.displayS,
                    color = colors.ink,
                )

                Spacer(modifier = Modifier.height(spacing.gap4))

                Text(
                    text = registryName,
                    style = typography.displayL,
                    color = colors.accent,
                )

                // Optional event-date metadata (only if eventDateMs present and parseable)
                val context = LocalContext.current
                val eventDateLabel = remember(payload["eventDateMs"]) {
                    val raw = payload["eventDateMs"] ?: return@remember null
                    val millis = raw.toLongOrNull() ?: return@remember null
                    DateUtils.formatDateTime(
                        context,
                        millis,
                        DateUtils.FORMAT_SHOW_DATE
                            or DateUtils.FORMAT_SHOW_TIME
                            or DateUtils.FORMAT_SHOW_WEEKDAY
                            or DateUtils.FORMAT_ABBREV_ALL,
                    ).uppercase()
                }
                if (eventDateLabel != null) {
                    Spacer(modifier = Modifier.height(spacing.gap16))
                    Text(
                        text = eventDateLabel,
                        style = typography.monoCaps,
                        color = colors.inkSoft,
                    )
                }

                Spacer(modifier = Modifier.height(spacing.gap20))

                // D-07 — inline warn-banner on Error state (verbatim AuthScreen pattern)
                val currentState = state
                if (currentState is InviteResponseViewModel.State.Error) {
                    val messageRes = when (currentState.messageKey) {
                        "invite_sheet_error_accept" -> R.string.invite_sheet_error_accept
                        else -> R.string.invite_sheet_error_decline
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(GiftMaisonTheme.shapes.radius12)
                            .background(colors.warn.copy(alpha = 0.15f))
                            .padding(spacing.gap12),
                    ) {
                        Column {
                            Text(
                                text = stringResource(messageRes),
                                style = typography.bodyM,
                                color = colors.ink,
                            )
                            Spacer(modifier = Modifier.height(spacing.gap8))
                            TextButton(onClick = { viewModel.retry() }) {
                                Text(
                                    text = stringResource(R.string.invite_sheet_error_retry),
                                    color = colors.accent,
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(spacing.gap20))
                }

                // Accept (primary)
                Button(
                    onClick = { viewModel.accept(registryId) },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.accentInk,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    val s = state
                    if (s is InviteResponseViewModel.State.Submitting &&
                        s.action == InviteResponseViewModel.Action.Accept
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = colors.accentInk,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(stringResource(R.string.invite_sheet_accept_cta))
                    }
                }

                Spacer(modifier = Modifier.height(spacing.gap8))

                // Decline (secondary — outlined; border = ink @ 40% per UI-SPEC color disambiguation)
                OutlinedButton(
                    onClick = { showDeclineDialog = true },
                    enabled = !isLoading,
                    border = BorderStroke(1.dp, colors.ink.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    val s = state
                    if (s is InviteResponseViewModel.State.Submitting &&
                        s.action == InviteResponseViewModel.Action.Decline
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = colors.ink,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.invite_sheet_decline_cta),
                            color = colors.ink,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(spacing.gap20))
            }
        }

        // D-03 — Decline confirmation dialog
        if (showDeclineDialog) {
            DeclineConfirmDialog(
                registryName = payload["registryName"]
                    ?: stringResource(R.string.invite_sheet_default_registry),
                onCancel = { showDeclineDialog = false },
                onConfirm = {
                    showDeclineDialog = false
                    viewModel.decline(registryId)
                },
            )
        }
    }
}

@Composable
private fun DeclineConfirmDialog(
    registryName: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = GiftMaisonTheme.colors
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = stringResource(R.string.invite_sheet_decline_confirm_title, registryName),
                style = GiftMaisonTheme.typography.bodyMEmphasis,
                color = colors.ink,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.invite_sheet_decline_confirm_decline),
                    color = colors.accent,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(text = stringResource(R.string.invite_sheet_decline_confirm_cancel))
            }
        },
    )
}
