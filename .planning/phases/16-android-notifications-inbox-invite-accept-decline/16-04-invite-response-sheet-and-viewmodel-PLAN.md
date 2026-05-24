---
phase: 16-android-notifications-inbox-invite-accept-decline
plan: 04
type: execute
wave: 3
depends_on:
  - 16-01-wave-0-red-tests-and-index
  - 16-03-android-domain-data-layer
files_modified:
  - app/src/main/java/com/giftregistry/ui/notifications/InviteResponseViewModel.kt
  - app/src/main/java/com/giftregistry/ui/notifications/InviteResponseSheet.kt
  - app/src/main/java/com/giftregistry/ui/notifications/NotificationsViewModel.kt
  - app/src/main/java/com/giftregistry/ui/notifications/NotificationsScreen.kt
  - app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
autonomous: true
requirements:
  - D-01
  - D-03
  - D-05
  - D-07
  - D-11
user_setup: []

must_haves:
  truths:
    - "InviteResponseViewModel exists with State sealed interface (Idle, Submitting, Error, AcceptedSuccess, DeclinedSuccess)"
    - "InviteResponseViewModel exposes state: StateFlow<State>, accept(registryId), decline(registryId), retry(), reset() methods"
    - "InviteResponseSheet ModalBottomSheet renders payload-driven hero via HeroImageOrPlaceholder (D-01)"
    - "Sheet exposes Accept primary CTA + Decline secondary CTA (D-01)"
    - "Tapping Decline opens a Material3 AlertDialog with Cancel / Decline buttons (D-03)"
    - "Sheet dismissal is blocked during Submitting via confirmValueChange = { !isLoading } on rememberModalBottomSheetState"
    - "Sheet shows inline warn-banner on Error state with Retry button (D-07)"
    - "Auto-navigation to RegistryDetailKey on AcceptedSuccess (D-05)"
    - "Predicate shouldOpenInviteSheet(notification) exists as pure-Kotlin top-level fun (extracted for D-11 testability)"
    - "NotificationsScreen branches on shouldOpenInviteSheet at tap-time — sheet open vs registry navigate (D-11)"
    - "NotificationsViewModel exposes openInviteSheet(notification) and dismissInviteSheet() with inviteSheetState: StateFlow<Notification?>"
    - "All Plan 16-01 RED tests for NotificationCardBranchingTest + InviteResponseViewModelTest flip GREEN"
  artifacts:
    - path: "app/src/main/java/com/giftregistry/ui/notifications/InviteResponseViewModel.kt"
      provides: "Sheet state machine with Hilt VM"
      contains: "InviteResponseViewModel"
      min_lines: 70
    - path: "app/src/main/java/com/giftregistry/ui/notifications/InviteResponseSheet.kt"
      provides: "ModalBottomSheet composable + content + DeclineConfirmDialog + shouldOpenInviteSheet predicate"
      contains: "ModalBottomSheet"
      min_lines: 150
    - path: "app/src/main/java/com/giftregistry/ui/notifications/NotificationsViewModel.kt"
      provides: "Extended with openInviteSheet/dismissInviteSheet + inviteSheetState"
      contains: "openInviteSheet"
    - path: "app/src/main/java/com/giftregistry/ui/notifications/NotificationsScreen.kt"
      provides: "Branches tap behavior via shouldOpenInviteSheet; hosts InviteResponseSheet"
      contains: "shouldOpenInviteSheet"
    - path: "app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt"
      provides: "onAcceptSuccess callback wired to navigate to RegistryDetailKey"
      contains: "onAcceptSuccess"
  key_links:
    - from: "InviteResponseSheet"
      to: "HeroImageOrPlaceholder"
      via: "Phase 12 reusable composable; payload coverUrl + occasion → hero render"
      pattern: "HeroImageOrPlaceholder\\("
    - from: "InviteResponseViewModel"
      to: "NotificationRepository.acceptInvite / declineInvite"
      via: "Injected via Hilt; runCatching wraps to State.AcceptedSuccess or State.Error"
      pattern: "notificationRepository\\.(accept|decline)Invite"
    - from: "NotificationsScreen tap"
      to: "InviteResponseSheet (open) OR onNavigateToRegistry (legacy fallback)"
      via: "shouldOpenInviteSheet predicate (D-11)"
      pattern: "shouldOpenInviteSheet\\("
    - from: "InviteResponseSheet AcceptedSuccess"
      to: "AppNavigation RegistryDetailKey navigate"
      via: "onAcceptSuccess callback"
      pattern: "onAcceptSuccess"
---

<objective>
Ship the actionable invite UI: a ModalBottomSheet (InviteResponseSheet) that renders the registry hero from the notification payload (zero registry-doc read), with Accept / Decline CTAs, an inline warn-banner error state, and auto-navigation on accept. Add the supporting ViewModel state machine + a pure-Kotlin branching predicate that decides sheet-vs-navigate at NotificationCard tap-time (D-11). Wire it into NotificationsScreen + AppNavigation.

Purpose: Make the new accept-gate model interactive on Android. Legacy invites (no pendingEntryKey) gracefully degrade to old navigate behavior.
Output: 2 new Kotlin files + 3 modified.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/phases/16-android-notifications-inbox-invite-accept-decline/16-CONTEXT.md
@.planning/phases/16-android-notifications-inbox-invite-accept-decline/16-UI-SPEC.md
@.planning/phases/16-android-notifications-inbox-invite-accept-decline/16-RESEARCH.md
@app/src/main/java/com/giftregistry/ui/notifications/NotificationsScreen.kt
@app/src/main/java/com/giftregistry/ui/notifications/NotificationsViewModel.kt
@app/src/main/java/com/giftregistry/ui/registry/invite/InviteBottomSheet.kt
@app/src/main/java/com/giftregistry/ui/registry/cover/HeroImageOrPlaceholder.kt
@app/src/main/java/com/giftregistry/ui/auth/AuthScreen.kt
@app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt
@app/src/main/java/com/giftregistry/ui/theme/GiftMaisonTheme.kt
@app/src/test/java/com/giftregistry/ui/notifications/InviteResponseViewModelTest.kt
@app/src/test/java/com/giftregistry/ui/notifications/NotificationCardBranchingTest.kt

<interfaces>
<!-- NotificationRepository (from Plan 16-03): -->
```kotlin
interface NotificationRepository {
    suspend fun acceptInvite(registryId: String): Result<Unit>
    suspend fun declineInvite(registryId: String): Result<Unit>
    // ...
}
```

<!-- HeroImageOrPlaceholder API (Phase 12 — reused as-is): -->
```kotlin
@Composable
fun HeroImageOrPlaceholder(
    imageUrl: String?,
    occasion: String?,
    glyphSize: TextUnit = 40.sp,
    modifier: Modifier = Modifier,
)
```

<!-- GiftMaisonTheme tokens accessed via composition locals: -->
```kotlin
GiftMaisonTheme.colors.{paper, ink, inkSoft, accent, accentInk, line, warn}
GiftMaisonTheme.typography.{displayL, displayS, bodyL, bodyM, bodyMEmphasis, monoCaps}
GiftMaisonTheme.spacing.{gap4, gap8, gap12, gap14, gap16, gap20, edge, edgeWide}
GiftMaisonTheme.shapes.{radius12}  // for warn-banner clip
```

<!-- AuthScreen warn-banner pattern (verbatim copy target, lines 274-286): -->
```kotlin
Box(
    modifier = Modifier.fillMaxWidth().clip(shapes.radius12).background(colors.warn.copy(alpha = 0.15f)).padding(spacing.gap12),
) { Column { ... } }
```

<!-- NotificationsScreen current onClick (line 126-128): -->
```kotlin
NotificationCard(
    notification = notification,
    onClick = { notification.payload["registryId"]?.let { onNavigateToRegistry(it) } },
)
```
We BRANCH this on shouldOpenInviteSheet(notification).
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Create InviteResponseViewModel state machine</name>
  <read_first>
    - app/src/test/java/com/giftregistry/ui/notifications/InviteResponseViewModelTest.kt (Plan 16-01 RED tests — these define the exact contract)
    - app/src/main/java/com/giftregistry/domain/notifications/NotificationRepository.kt (post Plan 16-03)
    - app/src/main/java/com/giftregistry/ui/auth/AuthViewModel.kt (existing Hilt VM pattern — for @HiltViewModel + viewModelScope.launch usage)
  </read_first>
  <behavior>
    State machine (per UI-SPEC Interaction & State Contracts):
    - State.Idle (initial)
    - State.Submitting(action: Action) where Action is Accept or Decline
    - State.Error(action: Action, message: String) — message is a stringResource KEY (not localized string — caller resolves)
    - State.AcceptedSuccess (terminal — caller dismisses sheet + navigates)
    - State.DeclinedSuccess (terminal — caller dismisses sheet)

    Methods:
    - accept(registryId: String): launches viewModelScope; emits Submitting(Accept); on repo success emits AcceptedSuccess; on failure emits Error(Accept, "invite_sheet_error_accept").
    - decline(registryId: String): mirror of accept, with "invite_sheet_error_decline" key.
    - retry(): re-runs the last attempted action (cached as `private var lastRegistryId: String?` + `private var lastAction: Action?`). If no prior action, no-op.
    - reset(): returns state to Idle (called by parent when sheet dismissed in Error state, so reopening the sheet later starts fresh — defensive).

    Hilt-injected via @HiltViewModel and constructor inject of NotificationRepository.
    Uses MutableStateFlow<State>(Idle) exposed as StateFlow<State>.
  </behavior>
  <action>
    Create app/src/main/java/com/giftregistry/ui/notifications/InviteResponseViewModel.kt:
    ```kotlin
    package com.giftregistry.ui.notifications

    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.viewModelScope
    import com.giftregistry.domain.notifications.NotificationRepository
    import dagger.hilt.android.lifecycle.HiltViewModel
    import kotlinx.coroutines.flow.MutableStateFlow
    import kotlinx.coroutines.flow.StateFlow
    import kotlinx.coroutines.flow.asStateFlow
    import kotlinx.coroutines.launch
    import javax.inject.Inject

    /**
     * D-07 — Sheet-scoped state machine for InviteResponseSheet.
     *
     * Owns the in-flight callable for Accept / Decline; surfaces Submitting / Error /
     * Success terminal states. Parent composable consumes state + drives navigation
     * on AcceptedSuccess (D-05).
     *
     * State transitions:
     *   Idle → Submitting(action) → AcceptedSuccess | DeclinedSuccess | Error(action)
     *   Error(action) → Submitting(action) (via retry())
     *   Idle (via reset() — called when sheet dismissed mid-error)
     */
    @HiltViewModel
    class InviteResponseViewModel @Inject constructor(
        private val notificationRepository: NotificationRepository,
    ) : ViewModel() {

        enum class Action { Accept, Decline }

        sealed interface State {
            data object Idle : State
            data class Submitting(val action: Action) : State
            data class Error(val action: Action, val messageKey: String) : State
            data object AcceptedSuccess : State
            data object DeclinedSuccess : State
        }

        private val _state = MutableStateFlow<State>(State.Idle)
        val state: StateFlow<State> = _state.asStateFlow()

        private var lastRegistryId: String? = null
        private var lastAction: Action? = null

        fun accept(registryId: String) {
            lastRegistryId = registryId
            lastAction = Action.Accept
            _state.value = State.Submitting(Action.Accept)
            viewModelScope.launch {
                notificationRepository.acceptInvite(registryId).fold(
                    onSuccess = { _state.value = State.AcceptedSuccess },
                    onFailure = {
                        _state.value = State.Error(Action.Accept, "invite_sheet_error_accept")
                    },
                )
            }
        }

        fun decline(registryId: String) {
            lastRegistryId = registryId
            lastAction = Action.Decline
            _state.value = State.Submitting(Action.Decline)
            viewModelScope.launch {
                notificationRepository.declineInvite(registryId).fold(
                    onSuccess = { _state.value = State.DeclinedSuccess },
                    onFailure = {
                        _state.value = State.Error(Action.Decline, "invite_sheet_error_decline")
                    },
                )
            }
        }

        /** Retry the last attempted action. No-op if no prior action. */
        fun retry() {
            val rid = lastRegistryId ?: return
            when (lastAction) {
                Action.Accept -> accept(rid)
                Action.Decline -> decline(rid)
                null -> Unit
            }
        }

        /** Force state back to Idle (e.g., sheet dismissed mid-error). */
        fun reset() {
            _state.value = State.Idle
        }
    }
    ```

    Note: The Plan 16-01 test uses `State.Error(action: Action, ...)` — verify field order matches the test's `(err as InviteResponseViewModel.State.Error).action` access. The test does not check `messageKey`, only `action`, so the second field name doesn't matter for the test but the order Action-first is required.
  </action>
  <verify>
    <automated>./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.notifications.InviteResponseViewModelTest" 2>&1 | tail -30</automated>
  </verify>
  <acceptance_criteria>
    - File app/src/main/java/com/giftregistry/ui/notifications/InviteResponseViewModel.kt exists
    - InviteResponseViewModel.kt contains string "@HiltViewModel"
    - InviteResponseViewModel.kt contains string "sealed interface State"
    - InviteResponseViewModel.kt contains string "data object Idle"
    - InviteResponseViewModel.kt contains string "data class Submitting(val action: Action)"
    - InviteResponseViewModel.kt contains string "data class Error(val action: Action"
    - InviteResponseViewModel.kt contains string "data object AcceptedSuccess"
    - InviteResponseViewModel.kt contains string "data object DeclinedSuccess"
    - InviteResponseViewModel.kt contains string "enum class Action { Accept, Decline }"
    - InviteResponseViewModel.kt contains string "fun accept(registryId: String)"
    - InviteResponseViewModel.kt contains string "fun decline(registryId: String)"
    - InviteResponseViewModel.kt contains string "fun retry()"
    - InviteResponseViewModel.kt contains string "notificationRepository.acceptInvite"
    - InviteResponseViewModel.kt contains string "notificationRepository.declineInvite"
    - ./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.notifications.InviteResponseViewModelTest" exits 0 (4 tests pass)
  </acceptance_criteria>
  <done>InviteResponseViewModel exists; Plan 16-01 RED InviteResponseViewModelTest flips GREEN.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Create InviteResponseSheet ModalBottomSheet + DeclineConfirmDialog + shouldOpenInviteSheet predicate</name>
  <read_first>
    - app/src/main/java/com/giftregistry/ui/registry/invite/InviteBottomSheet.kt (reference ModalBottomSheet shape — DO NOT modify this file)
    - app/src/main/java/com/giftregistry/ui/registry/cover/HeroImageOrPlaceholder.kt (composable API)
    - app/src/main/java/com/giftregistry/ui/auth/AuthScreen.kt lines 274-286 (verbatim warn-banner pattern to copy)
    - app/src/main/java/com/giftregistry/ui/theme/GiftMaisonTheme.kt (color/typography/spacing/shape composition local accessors)
    - app/src/test/java/com/giftregistry/ui/notifications/NotificationCardBranchingTest.kt (Plan 16-01 RED test — defines predicate signature)
    - .planning/phases/16-android-notifications-inbox-invite-accept-decline/16-UI-SPEC.md (Spacing scale + Typography + Color + Interaction sections — this is the visual contract)
    - app/src/main/res/values/strings.xml (current string keys — Plan 16-05 will add the invite_sheet_* keys; for now stub via R.string.invite_sheet_*)
  </read_first>
  <behavior>
    1. shouldOpenInviteSheet predicate (pure-Kotlin, package-level): returns true iff `notification.type == NotificationType.INVITE && notification.payload["pendingEntryKey"] != null` per D-11. Extracted as top-level fun for testability (Plan 16-01 test imports it).

    2. InviteResponseSheet @Composable:
    - Args: `registryId: String`, `payload: Map<String, String?>`, `onAcceptSuccess: (registryId: String) -> Unit`, `onDismiss: () -> Unit`, `viewModel: InviteResponseViewModel = hiltViewModel()`.
    - rememberModalBottomSheetState with `confirmValueChange = { value -> state !is State.Submitting }` (D-07: dismissal blocked during in-flight).
    - LaunchedEffect on state:
      - On AcceptedSuccess: call onAcceptSuccess(registryId) (parent handles navigation + dismissal).
      - On DeclinedSuccess: call onDismiss().
    - ModalBottomSheet with onDismissRequest = { if (state !is Submitting) onDismiss() }, sheetState = sheetState.
    - Sheet content (per UI-SPEC Spacing section, vertical rhythm):
      - HeroImageOrPlaceholder fillMaxWidth().height(180.dp) — pass payload["coverUrl"] and payload["occasion"], glyphSize = 40.sp
      - Spacer 20.dp
      - Column with 20.dp horizontal padding:
        - Sheet title: Text(stringResource(R.string.invite_sheet_title_template, payload["actorName"] ?: "Someone"), typography.displayS, color = ink)
        - Spacer 4.dp
        - Registry name: Text(payload["registryName"] ?: "this registry", typography.displayL, color = accent) — italic comes from displayL FontFamily
        - if (payload["eventDateMs"] != null): Spacer 16.dp + Text(formatted date via DateUtils.formatDateTime, typography.monoCaps, color = inkSoft) — use try/catch on parse, omit if invalid
        - Spacer 20.dp
        - AnimatedVisibility on state is Error: show warn-banner (verbatim AuthScreen pattern) with banner message from stringResource(state.messageKey) + Retry button
        - Spacer 20.dp (if no banner, just 20.dp; if banner present, 20.dp below banner)
        - Button (Accept): containerColor = accent, contentColor = accentInk, fillMaxWidth, height 48.dp. Content: if state is Submitting && action == Accept → CircularProgressIndicator(size 16.dp, color = accentInk); else Text(stringResource(R.string.invite_sheet_accept_cta)). Enabled = state !is Submitting. onClick = { viewModel.accept(registryId) }
        - Spacer 8.dp
        - OutlinedButton (Decline): border BorderStroke(1.dp, ink.copy(alpha = 0.4f)), contentColor = ink. Content: if state is Submitting && action == Decline → CircularProgressIndicator(size 16.dp, color = ink); else Text(stringResource(R.string.invite_sheet_decline_cta)). Enabled = state !is Submitting. onClick = { showDeclineDialog = true }
        - Spacer 20.dp + system inset
    - Decline confirmation: rememberSaveable mutableStateOf(false) for showDeclineDialog. When true, render DeclineConfirmDialog (private composable below).
    - DisposableEffect cleanup: when sheet disposes (e.g., dismissed), if state is Error, call viewModel.reset() so reopening starts fresh.

    3. DeclineConfirmDialog (private @Composable):
    - AlertDialog with title = Text(stringResource(R.string.invite_sheet_decline_confirm_title, payload["registryName"] ?: "this registry"), typography.bodyMEmphasis, color = ink)
    - dismissButton = TextButton(onClick = onCancel) { Text(stringResource(R.string.invite_sheet_decline_confirm_cancel)) }
    - confirmButton = TextButton(onClick = onConfirm) { Text(stringResource(R.string.invite_sheet_decline_confirm_decline), color = accent) }
    - onDismissRequest = onCancel
  </behavior>
  <action>
    Create app/src/main/java/com/giftregistry/ui/notifications/InviteResponseSheet.kt:

    ```kotlin
    package com.giftregistry.ui.notifications

    import android.text.format.DateUtils
    import androidx.compose.foundation.BorderStroke
    import androidx.compose.foundation.background
    import androidx.compose.foundation.layout.*
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
    import androidx.compose.runtime.*
    import androidx.compose.runtime.saveable.rememberSaveable
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
        val sheetState = rememberModalBottomSheetState(
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
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                )

                Spacer(modifier = Modifier.height(spacing.gap20))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.edgeWide),
                ) {
                    val actorName = payload["actorName"] ?: stringResource(R.string.invite_sheet_default_actor)
                    val registryName = payload["registryName"] ?: stringResource(R.string.invite_sheet_default_registry)

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
                        modifier = Modifier.fillMaxWidth().height(48.dp),
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
                        modifier = Modifier.fillMaxWidth().height(48.dp),
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
                            Text(stringResource(R.string.invite_sheet_decline_cta), color = colors.ink)
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.gap20))
                }
            }

            // D-03 — Decline confirmation dialog
            if (showDeclineDialog) {
                DeclineConfirmDialog(
                    registryName = payload["registryName"] ?: stringResource(R.string.invite_sheet_default_registry),
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
    ```

    Note about strings: This task references R.string.invite_sheet_* keys that Plan 16-05 creates. Build will succeed only after Plan 16-05 adds the strings, OR — as a temporary measure — the executor can stub the strings in values/strings.xml with placeholder English copy AT THE END OF THIS TASK to keep the build green. Plan 16-05 will overwrite with the locked copy. Required stub keys (English placeholders OK; Romanian translations come in Plan 16-05):
    - invite_sheet_default_actor = "Someone"
    - invite_sheet_default_registry = "this registry"
    - invite_sheet_title_template = "%1$s invited you to"
    - invite_sheet_accept_cta = "Accept"
    - invite_sheet_decline_cta = "Decline"
    - invite_sheet_decline_confirm_title = "Decline invite to \"%1$s\"?"
    - invite_sheet_decline_confirm_cancel = "Cancel"
    - invite_sheet_decline_confirm_decline = "Decline"
    - invite_sheet_error_accept = "Couldn't accept invite. Please try again."
    - invite_sheet_error_decline = "Couldn't decline invite. Please try again."
    - invite_sheet_error_retry = "Retry"

    Add these 11 stub strings to BOTH app/src/main/res/values/strings.xml AND app/src/main/res/values-ro/strings.xml (with English placeholders in RO too — Plan 16-05 will translate). The LocalizationParityTest (Plan 16-01) enforces key parity, so RO must have the same keys even with placeholder copy.
  </action>
  <verify>
    <automated>./gradlew :app:compileDebugKotlin 2>&1 | tail -20 && ./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.notifications.NotificationCardBranchingTest" --tests "com.giftregistry.LocalizationParityTest" 2>&1 | tail -30</automated>
  </verify>
  <acceptance_criteria>
    - File app/src/main/java/com/giftregistry/ui/notifications/InviteResponseSheet.kt exists
    - InviteResponseSheet.kt contains string "fun shouldOpenInviteSheet(notification: Notification): Boolean"
    - InviteResponseSheet.kt contains string "notification.type == NotificationType.INVITE"
    - InviteResponseSheet.kt contains string "notification.payload[\"pendingEntryKey\"] != null"
    - InviteResponseSheet.kt contains string "fun InviteResponseSheet("
    - InviteResponseSheet.kt contains string "ModalBottomSheet("
    - InviteResponseSheet.kt contains string "rememberModalBottomSheetState"
    - InviteResponseSheet.kt contains string "confirmValueChange = { state !is InviteResponseViewModel.State.Submitting }"
    - InviteResponseSheet.kt contains string "HeroImageOrPlaceholder("
    - InviteResponseSheet.kt contains string "glyphSize = 40.sp"
    - InviteResponseSheet.kt contains string "AlertDialog("
    - InviteResponseSheet.kt contains string "viewModel.accept(registryId)"
    - InviteResponseSheet.kt contains string "viewModel.decline(registryId)"
    - InviteResponseSheet.kt contains string "viewModel.retry()"
    - InviteResponseSheet.kt contains string "colors.warn.copy(alpha = 0.15f)"
    - InviteResponseSheet.kt contains string "onAcceptSuccess(registryId)"
    - InviteResponseSheet.kt contains string "private fun DeclineConfirmDialog"
    - app/src/main/res/values/strings.xml contains string "invite_sheet_accept_cta"
    - app/src/main/res/values/strings.xml contains string "invite_sheet_decline_confirm_title"
    - app/src/main/res/values/strings.xml contains string "invite_sheet_error_retry"
    - app/src/main/res/values-ro/strings.xml contains string "invite_sheet_accept_cta" (key parity — copy can be placeholder)
    - ./gradlew :app:compileDebugKotlin exits 0
    - ./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.notifications.NotificationCardBranchingTest" exits 0 (6 tests pass — predicate is now defined)
    - ./gradlew :app:testDebugUnitTest --tests "com.giftregistry.LocalizationParityTest" exits 0 (key parity preserved with stub keys added to both locales)
  </acceptance_criteria>
  <done>InviteResponseSheet + DeclineConfirmDialog + shouldOpenInviteSheet predicate exist; build green; Plan 16-01 RED test NotificationCardBranchingTest flips GREEN; key parity preserved via stub RO strings.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 3: Wire NotificationsScreen tap-branching + sheet host + AppNavigation onAcceptSuccess</name>
  <read_first>
    - app/src/main/java/com/giftregistry/ui/notifications/NotificationsScreen.kt (current — lines 60-136 for the Scaffold and LazyColumn)
    - app/src/main/java/com/giftregistry/ui/notifications/NotificationsViewModel.kt (current — add sheet state)
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt (find NotificationsKey entry — search for `is NotificationsKey ->` or `NotificationsKey is`)
    - app/src/main/java/com/giftregistry/ui/notifications/InviteResponseSheet.kt (just created)
  </read_first>
  <behavior>
    NotificationsViewModel extension:
    - Add `private val _inviteSheetState = MutableStateFlow<Notification?>(null)`; expose as `val inviteSheetState: StateFlow<Notification?>`.
    - Add `fun openInviteSheet(notification: Notification) { _inviteSheetState.value = notification }`.
    - Add `fun dismissInviteSheet() { _inviteSheetState.value = null }`.

    NotificationsScreen modifications:
    - In the LazyColumn item onClick, branch:
      ```kotlin
      onClick = {
          if (shouldOpenInviteSheet(notification)) {
              viewModel.openInviteSheet(notification)
          } else {
              notification.payload["registryId"]?.let { onNavigateToRegistry(it) }
          }
      }
      ```
    - After the Scaffold, conditionally render InviteResponseSheet when inviteSheetState != null:
      ```kotlin
      val inviteNotif by viewModel.inviteSheetState.collectAsStateWithLifecycle()
      inviteNotif?.let { n ->
          val registryId = n.payload["registryId"]
          if (registryId != null) {
              InviteResponseSheet(
                  registryId = registryId,
                  payload = n.payload,
                  onAcceptSuccess = { rid ->
                      viewModel.dismissInviteSheet()
                      onNavigateToRegistry(rid)
                  },
                  onDismiss = { viewModel.dismissInviteSheet() },
              )
          } else {
              // Defensive: malformed payload — close sheet silently
              LaunchedEffect(Unit) { viewModel.dismissInviteSheet() }
          }
      }
      ```

    AppNavigation:
    - Find the NotificationsKey navigation entry. The existing call to NotificationsScreen already passes onNavigateToRegistry. No new param needed since onAcceptSuccess is wired inside NotificationsScreen itself (parent only needs onNavigateToRegistry, which already exists).
    - VERIFY (no edit if already correct): the existing onNavigateToRegistry callback navigates to RegistryDetailKey(registryId).
    - VERIFY (per RESEARCH.md last bullet): NotificationsKey is in the "hidden bottom nav" whitelist (quick task 260522-v0q set this). Search for `NotificationsKey` in showsBottomNav predicate file (app/src/main/java/com/giftregistry/ui/common/chrome/NavVisibility.kt) — if not present, add it.
  </behavior>
  <action>
    1. EDIT app/src/main/java/com/giftregistry/ui/notifications/NotificationsViewModel.kt:
    Add imports: `import kotlinx.coroutines.flow.MutableStateFlow`, `import kotlinx.coroutines.flow.asStateFlow`, `import com.giftregistry.domain.model.Notification` (verify already present).

    After the existing `markVisibleRead` method (after line 84, before the closing class brace), insert:
    ```kotlin

        // ----- D-01 invite-sheet state (host-in-screen pattern) -----

        private val _inviteSheetState = MutableStateFlow<Notification?>(null)
        val inviteSheetState: StateFlow<Notification?> = _inviteSheetState.asStateFlow()

        /** Open the InviteResponseSheet over [notification]. */
        fun openInviteSheet(notification: Notification) {
            _inviteSheetState.value = notification
        }

        /** Close the InviteResponseSheet. */
        fun dismissInviteSheet() {
            _inviteSheetState.value = null
        }
    ```

    2. EDIT app/src/main/java/com/giftregistry/ui/notifications/NotificationsScreen.kt:
    Replace the LazyColumn item onClick (line 126-128) with the branching block:
    ```kotlin
                            NotificationCard(
                                notification = notification,
                                onClick = {
                                    if (shouldOpenInviteSheet(notification)) {
                                        viewModel.openInviteSheet(notification)
                                    } else {
                                        notification.payload["registryId"]?.let { onNavigateToRegistry(it) }
                                    }
                                },
                            )
    ```

    BEFORE the closing `}` of the @Composable fun NotificationsScreen (after the Scaffold close brace), add the sheet host:
    ```kotlin
        // D-01 — InviteResponseSheet host (over the inbox scaffold)
        val inviteNotif by viewModel.inviteSheetState.collectAsStateWithLifecycle()
        inviteNotif?.let { n ->
            val registryId = n.payload["registryId"]
            if (registryId != null) {
                InviteResponseSheet(
                    registryId = registryId,
                    payload = n.payload,
                    onAcceptSuccess = { rid ->
                        viewModel.dismissInviteSheet()
                        onNavigateToRegistry(rid)
                    },
                    onDismiss = { viewModel.dismissInviteSheet() },
                )
            } else {
                LaunchedEffect(Unit) { viewModel.dismissInviteSheet() }
            }
        }
    ```

    3. EDIT app/src/main/java/com/giftregistry/ui/common/chrome/NavVisibility.kt (if it exists — verify path first):
    Search for the `showsBottomNav(key)` predicate or `isPrimary(key)`. If `NotificationsKey` is NOT in the hidden-list, add it.
    Use this command first to inspect:
    ```bash
    grep -n "NotificationsKey\|showsBottomNav\|isPrimary" app/src/main/java/com/giftregistry/ui/common/chrome/NavVisibility.kt
    ```
    If the file doesn't have NotificationsKey listed and the predicate is exclusion-based (e.g., `key !in setOf(AuthKey, ...)`), append `NotificationsKey` to that set. If the predicate is inclusion-based (`key in setOf(HomeKey, ...)`), DON'T add (notifications isn't a bottom-nav destination).
    If file doesn't exist or no edit is needed, skip — that's fine; this is a defensive verification step.

    4. EDIT app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt:
    Search for the NotificationsKey case (likely around line 343 per CONTEXT.md). VERIFY that the existing NotificationsScreen call passes:
    ```kotlin
    NotificationsScreen(
        onBack = { /* existing */ },
        onNavigateToRegistry = { rid -> /* existing — should navigate to RegistryDetailKey(rid) */ },
    )
    ```
    If the existing onNavigateToRegistry callback uses backStack.add(RegistryDetailKey(rid)) or similar Nav3 navigate operation, NO EDIT needed. The auto-nav on AcceptedSuccess is handled inside NotificationsScreen by reusing this same callback.
    If the callback is missing or different shape, ADD/FIX it to navigate to RegistryDetailKey.

    Document in commit message: "NotificationsScreen reuses onNavigateToRegistry for both legacy invite navigate and D-05 post-accept auto-nav."
  </action>
  <verify>
    <automated>./gradlew :app:compileDebugKotlin 2>&1 | tail -20 && ./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.notifications.*" 2>&1 | tail -20</automated>
  </verify>
  <acceptance_criteria>
    - app/src/main/java/com/giftregistry/ui/notifications/NotificationsViewModel.kt contains string "_inviteSheetState"
    - NotificationsViewModel.kt contains string "fun openInviteSheet(notification: Notification)"
    - NotificationsViewModel.kt contains string "fun dismissInviteSheet()"
    - NotificationsViewModel.kt contains string "val inviteSheetState: StateFlow<Notification?>"
    - app/src/main/java/com/giftregistry/ui/notifications/NotificationsScreen.kt contains string "shouldOpenInviteSheet(notification)"
    - NotificationsScreen.kt contains string "viewModel.openInviteSheet(notification)"
    - NotificationsScreen.kt contains string "InviteResponseSheet("
    - NotificationsScreen.kt contains string "viewModel.dismissInviteSheet()"
    - NotificationsScreen.kt contains string "onAcceptSuccess = { rid ->"
    - app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt contains string "NotificationsScreen("
    - ./gradlew :app:compileDebugKotlin exits 0
    - ./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.notifications.*" exits 0 (all notification tests pass)
    - ./gradlew :app:testDebugUnitTest (full suite) exits 0 (no regressions in other tests)
  </acceptance_criteria>
  <done>Tap-branching wired; sheet hosts inside NotificationsScreen; AppNavigation verified/updated; full Android test suite green.</done>
</task>

</tasks>

<verification>
- ./gradlew :app:compileDebugKotlin exits 0 (entire app compiles).
- ./gradlew :app:testDebugUnitTest exits 0 (all 4 Plan 16-01 Android RED tests flip GREEN, plus no regressions).
- shouldOpenInviteSheet predicate is a top-level function (not a method) per the test's import.
- InviteResponseSheet, DeclineConfirmDialog, and shouldOpenInviteSheet all live in the same file (InviteResponseSheet.kt) per the UI-SPEC component inventory.
- 11 invite_sheet_* string keys exist in both values/strings.xml AND values-ro/strings.xml (LocalizationParityTest green).
</verification>

<success_criteria>
- 5 files modified (2 created, 3 edited) — InviteResponseViewModel.kt, InviteResponseSheet.kt, NotificationsViewModel.kt, NotificationsScreen.kt, AppNavigation.kt + 2 strings.xml.
- All Plan 16-01 RED tests for Android UI symbols flip GREEN.
- No new Hilt module added (InviteResponseViewModel uses @HiltViewModel + injected NotificationRepository which is bound by Plan 16-03's repo extension).
- Legacy INVITE notifications (no pendingEntryKey) preserve their existing tap → navigate behavior (D-11).
</success_criteria>

<output>
After completion, create `.planning/phases/16-android-notifications-inbox-invite-accept-decline/16-04-SUMMARY.md` listing all 5 file edits, the stub-strings decision (overwritten in Plan 16-05), and the test results.
</output>
