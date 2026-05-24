---
phase: 16-android-notifications-inbox-invite-accept-decline
plan: 05
type: execute
wave: 4
depends_on:
  - 16-04-invite-response-sheet-and-viewmodel
files_modified:
  - app/src/main/java/com/giftregistry/ui/notifications/NotificationsScreen.kt
  - app/src/main/res/values/strings.xml
  - app/src/main/res/values-ro/strings.xml
  - app/src/main/java/com/giftregistry/ui/theme/preview/StyleGuidePreview.kt
autonomous: true
requirements:
  - D-04
  - D-09
  - D-25
  - D-28
user_setup: []

must_haves:
  truths:
    - "NotificationsScreen TopAppBar renders GiftMaisonWordmark via the title slot (D-09)"
    - "NotificationsScreen Scaffold background is GiftMaisonTheme.colors.paper (D-09)"
    - "NotificationCard drops M3 Card elevation; uses flat row separated by 1dp gm.line divider (D-09)"
    - "NotificationCard renders unread accent dot (6dp circle, color = accent) at trailing edge of title row when readAtMs == null (D-09)"
    - "NotificationCard timestamp renders as MonoCaps (e.g. '5M AGO') (D-09)"
    - "NotificationCard title uses typography.bodyL (Inter Medium), body uses typography.bodyM (Inter Normal) — replaces M3 bodyMedium/bodySmall (D-09)"
    - "localizedTitle/Body when blocks handle 3 new NotificationType values: INVITE_ACCEPTED_SELF, INVITE_ACCEPTED, INVITE_DECLINED (D-25)"
    - "Empty state uses typography.displayS heading + typography.bodyS body with new strings notifications_empty_heading and notifications_empty_body (D-09 + D-28)"
    - "20 new string keys added to BOTH values/strings.xml and values-ro/strings.xml with locked EN + RO copy per UI-SPEC Copywriting Contract"
    - "All previously-stubbed invite_sheet_* RO strings replaced with proper Romanian translations"
    - "StyleGuidePreview appends new NotificationsScreen + InviteResponseSheet sections per Phase 8-11 pattern"
    - "LocalizationParityTest still passes (key parity preserved)"
  artifacts:
    - path: "app/src/main/java/com/giftregistry/ui/notifications/NotificationsScreen.kt"
      provides: "Re-skinned screen + NotificationCard + localizedTitle/Body extended for 3 new types"
      contains: "GiftMaisonWordmark"
    - path: "app/src/main/res/values/strings.xml"
      provides: "EN strings: notification_invite_accepted_self_title/body, notification_invite_accepted_title/body, notification_invite_declined_title/body, notifications_empty_heading/body, invite_sheet_* (proper copy replacing stubs)"
      contains: "notification_invite_accepted_self_title"
    - path: "app/src/main/res/values-ro/strings.xml"
      provides: "Romanian translations of all keys in values/strings.xml"
      contains: "notification_invite_accepted_self_title"
    - path: "app/src/main/java/com/giftregistry/ui/theme/preview/StyleGuidePreview.kt"
      provides: "Appended NotificationsScreen + InviteResponseSheet preview sections"
      contains: "NotificationsScreen"
  key_links:
    - from: "NotificationsScreen TopAppBar"
      to: "GiftMaisonWordmark (Phase 8 component)"
      via: "title slot composable"
      pattern: "GiftMaisonWordmark\\("
    - from: "NotificationCard"
      to: "GiftMaisonTheme.colors.line"
      via: "1dp divider drawn below row (HorizontalDivider or Box(height(1.dp).background(line)))"
      pattern: "GiftMaisonTheme.colors.line"
    - from: "NotificationCard unread state"
      to: "GiftMaisonTheme.colors.accent"
      via: "6dp circle drawn in title row when readAtMs == null"
      pattern: "(CircleShape|Box.*size\\(6\\.dp\\))"
    - from: "localizedTitle/Body when blocks"
      to: "3 new NotificationType values + 6 new strings.xml keys"
      via: "stringResource with payload args"
      pattern: "notification_invite_accepted_self_title|notification_invite_accepted_title|notification_invite_declined_title"
---

<objective>
Complete the inbox visual story: re-skin NotificationsScreen + NotificationCard to GiftMaison design language (D-09), extend localizedTitle/Body to handle the 3 new NotificationType values from Plan 16-03 (D-25), add 20 new string keys × 2 locales with the locked copy from UI-SPEC, replace Plan 16-04's stub Romanian strings with proper translations, and append a StyleGuidePreview section so designers can verify the new visuals offline.

Purpose: Visual richness is the second half of Phase 16's user-facing change (the first half being the accept/decline flow). Plan 16-04 made the inbox functional; Plan 16-05 makes it beautiful.
Output: 1 modified screen + 2 modified strings files + 1 modified StyleGuidePreview.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/phases/16-android-notifications-inbox-invite-accept-decline/16-UI-SPEC.md
@.planning/phases/16-android-notifications-inbox-invite-accept-decline/16-CONTEXT.md
@app/src/main/java/com/giftregistry/ui/notifications/NotificationsScreen.kt
@app/src/main/res/values/strings.xml
@app/src/main/res/values-ro/strings.xml
@app/src/main/java/com/giftregistry/ui/theme/GiftMaisonTheme.kt
@app/src/main/java/com/giftregistry/ui/theme/preview/StyleGuidePreview.kt
@app/src/main/java/com/giftregistry/ui/common/GiftMaisonWordmark.kt

<interfaces>
<!-- GiftMaisonWordmark API (Phase 8 component — verify exact path during execution): -->
```kotlin
@Composable
fun GiftMaisonWordmark(modifier: Modifier = Modifier, fontSize: TextUnit = 22.sp)
```

<!-- GiftMaisonTheme tokens used in this plan: -->
GiftMaisonTheme.colors.{paper, ink, inkSoft, accent, line, accentInk}
GiftMaisonTheme.typography.{displayS, bodyL, bodyM, bodyS, monoCaps}
GiftMaisonTheme.spacing.{gap4, gap8, gap12, gap14, gap16, edge}
</interfaces>
</context>

<tasks>

<task type="auto">
  <name>Task 1: Add 20 new strings × 2 locales + replace Plan 16-04 stub RO strings with proper translations</name>
  <read_first>
    - app/src/main/res/values/strings.xml (current keys — find where to insert new ones)
    - app/src/main/res/values-ro/strings.xml (current keys — verify parity)
    - .planning/phases/16-android-notifications-inbox-invite-accept-decline/16-UI-SPEC.md (Copywriting Contract section — has all 20 EN + RO pairs locked)
  </read_first>
  <action>
    Add EXACTLY these 9 NEW string keys (6 notification_* + 2 notifications_empty_* + 1 invite_sheet_default_actor refinement, plus 1 invite_sheet_default_registry refinement = wait — those were stubs from Plan 16-04). Let me re-enumerate.

    **EN strings to ADD to values/strings.xml** (insert in alphabetical/grouped order near existing notification_* keys):

    Notification body/title keys (6 new keys for D-25 / D-09 type rendering):
    ```xml
    <string name="notification_invite_accepted_self_title">You joined "%1$s"</string>
    <string name="notification_invite_accepted_self_body">Tap to view %1$s</string>
    <string name="notification_invite_accepted_title">%1$s accepted your invite to "%2$s"</string>
    <string name="notification_invite_accepted_body">%1$s</string>
    <string name="notification_invite_declined_title">%1$s declined your invite to "%2$s"</string>
    <string name="notification_invite_declined_body">%1$s</string>
    ```

    Empty state copy (2 new keys for D-09 re-skin):
    ```xml
    <string name="notifications_empty_heading">No notifications yet</string>
    <string name="notifications_empty_body">When someone invites you to a registry or reserves a gift, you\'ll see it here.</string>
    ```

    Inbox screen title (already exists as `notifications_screen_title` per the existing code — verify; if a new one is needed for the wordmark sub-title, add `notifications_subtitle` = "NOTIFICATIONS"; if not, skip).

    **EN strings to REPLACE in values/strings.xml** (the 11 invite_sheet_* stub keys from Plan 16-04 — confirm they are the proper copy from UI-SPEC):
    ```xml
    <string name="invite_sheet_default_actor">Someone</string>
    <string name="invite_sheet_default_registry">this registry</string>
    <string name="invite_sheet_title_template">%1$s invited you to</string>
    <string name="invite_sheet_accept_cta">Accept</string>
    <string name="invite_sheet_decline_cta">Decline</string>
    <string name="invite_sheet_decline_confirm_title">Decline invite to \"%1$s\"?</string>
    <string name="invite_sheet_decline_confirm_cancel">Cancel</string>
    <string name="invite_sheet_decline_confirm_decline">Decline</string>
    <string name="invite_sheet_error_accept">Couldn\'t accept invite. Please try again.</string>
    <string name="invite_sheet_error_decline">Couldn\'t decline invite. Please try again.</string>
    <string name="invite_sheet_error_retry">Retry</string>
    ```
    (If Plan 16-04 already added these with this exact copy, leave EN unchanged. If different, normalize to these strings.)

    **RO strings to ADD/REPLACE in values-ro/strings.xml** (proper Romanian per UI-SPEC):
    ```xml
    <!-- New notification keys (RO) -->
    <string name="notification_invite_accepted_self_title">Te-ai alăturat la „%1$s"</string>
    <string name="notification_invite_accepted_self_body">Apasă pentru a vedea %1$s</string>
    <string name="notification_invite_accepted_title">%1$s a acceptat invitația la „%2$s"</string>
    <string name="notification_invite_accepted_body">%1$s</string>
    <string name="notification_invite_declined_title">%1$s a refuzat invitația la „%2$s"</string>
    <string name="notification_invite_declined_body">%1$s</string>

    <!-- Empty state (RO) -->
    <string name="notifications_empty_heading">Nicio notificare încă</string>
    <string name="notifications_empty_body">Când cineva te invită la o listă sau rezervă un cadou, vei vedea aici.</string>

    <!-- Invite sheet (RO — replace Plan 16-04 placeholder English copy with proper Romanian) -->
    <string name="invite_sheet_default_actor">Cineva</string>
    <string name="invite_sheet_default_registry">această listă</string>
    <string name="invite_sheet_title_template">%1$s te-a invitat la</string>
    <string name="invite_sheet_accept_cta">Acceptă</string>
    <string name="invite_sheet_decline_cta">Refuză</string>
    <string name="invite_sheet_decline_confirm_title">Refuzi invitația la „%1$s"?</string>
    <string name="invite_sheet_decline_confirm_cancel">Anulează</string>
    <string name="invite_sheet_decline_confirm_decline">Refuză</string>
    <string name="invite_sheet_error_accept">Nu am putut accepta invitația. Încearcă din nou.</string>
    <string name="invite_sheet_error_decline">Nu am putut refuza invitația. Încearcă din nou.</string>
    <string name="invite_sheet_error_retry">Reîncearcă</string>
    ```

    Note on quoting: Android strings.xml supports „..." (Romanian smart quotes) directly inside `<string>` values. The XML 1.0 spec accepts them as-is — no escaping needed. The apostrophe in "you'll" / "couldn't" MUST be escaped as `\'` (XML attribute character) — but inside the string body it's optional; the safer pattern is `\'` consistently. Use `<string>...you\'ll...</string>` style.

    Final total: 9 new keys + 11 replacement/normalized keys = 20 keys in each locale.

    Validation:
    - LocalizationParityTest (Plan 16-01) MUST pass after this edit. Key sets in EN and RO must be identical.
  </action>
  <verify>
    <automated>./gradlew :app:testDebugUnitTest --tests "com.giftregistry.LocalizationParityTest" 2>&1 | tail -10 && grep -c "notification_invite_accepted_self_title\|notification_invite_accepted_title\|notification_invite_declined_title\|notifications_empty_heading" app/src/main/res/values/strings.xml && grep -c "notification_invite_accepted_self_title\|notification_invite_accepted_title\|notification_invite_declined_title\|notifications_empty_heading" app/src/main/res/values-ro/strings.xml</automated>
  </verify>
  <acceptance_criteria>
    - app/src/main/res/values/strings.xml contains string "notification_invite_accepted_self_title"
    - values/strings.xml contains string "notification_invite_accepted_title"
    - values/strings.xml contains string "notification_invite_declined_title"
    - values/strings.xml contains string "notifications_empty_heading"
    - values/strings.xml contains string "notifications_empty_body"
    - values/strings.xml contains string "You joined" (EN copy)
    - values/strings.xml contains string "accepted your invite"
    - values/strings.xml contains string "declined your invite"
    - app/src/main/res/values-ro/strings.xml contains string "notification_invite_accepted_self_title"
    - values-ro/strings.xml contains string "Te-ai alăturat"
    - values-ro/strings.xml contains string "a acceptat invitația"
    - values-ro/strings.xml contains string "a refuzat invitația"
    - values-ro/strings.xml contains string "Acceptă"
    - values-ro/strings.xml contains string "Refuză"
    - values-ro/strings.xml contains string "Reîncearcă"
    - values-ro/strings.xml contains string "Nicio notificare încă"
    - ./gradlew :app:testDebugUnitTest --tests "com.giftregistry.LocalizationParityTest" exits 0 (key parity preserved)
    - ./gradlew :app:compileDebugKotlin exits 0 (no R.string.* unresolved refs after Plan 16-04 stubs are now real)
  </acceptance_criteria>
  <done>9 new keys × 2 locales added, 11 stub RO keys replaced with proper Romanian, LocalizationParityTest green, build green.</done>
</task>

<task type="auto">
  <name>Task 2: Re-skin NotificationsScreen + NotificationCard + extend localizedTitle/Body for 3 new types</name>
  <read_first>
    - app/src/main/java/com/giftregistry/ui/notifications/NotificationsScreen.kt (current implementation — full file)
    - app/src/main/java/com/giftregistry/ui/theme/GiftMaisonTheme.kt (token access pattern via composition local)
    - app/src/main/java/com/giftregistry/ui/common/GiftMaisonWordmark.kt (verify path — adjust import if file lives elsewhere; if not found, grep `class GiftMaisonWordmark` or `fun GiftMaisonWordmark`)
    - .planning/phases/16-android-notifications-inbox-invite-accept-decline/16-UI-SPEC.md (full visual contract for re-skin)
    - app/src/main/res/values/strings.xml (verify new keys added by Task 1)
  </read_first>
  <behavior>
    Re-skin (D-09):
    1. TopAppBar title slot: GiftMaisonWordmark (instead of plain Text(notifications_screen_title)). Preserve the navigationIcon (back arrow). TopAppBar background = colors.paper (matches body).
    2. Scaffold containerColor = GiftMaisonTheme.colors.paper.
    3. Empty state: replace single Text with a Column centered: heading (displayS, ink, "No notifications yet") + 8dp Spacer + body (bodyS, inkSoft, "When someone invites you to..."). Max width 280dp.
    4. NotificationCard re-skin:
       - Drop the Card composable. Use Box(Modifier.fillMaxWidth().clickable(onClick)) with internal Column.
       - Internal Row: icon (24dp) + Spacer 12dp + Column { titleRow (Title + Spacer fill + MonoCaps timestamp + 4dp Spacer + unread accent dot 6dp when readAtMs==null) + Spacer 4dp + bodyText }.
       - Bottom border: HorizontalDivider(color = colors.line, thickness = 1.dp).
       - Padding: horizontal 16dp, vertical 14dp.
       - Title: typography.bodyL (Inter Medium 15sp), color = ink if unread else inkSoft.
       - Body: typography.bodyM (Inter Normal 13.5sp), color = inkSoft.
       - Timestamp: typography.monoCaps (JetBrains Mono 9.5sp), color = inkSoft. Format relative via DateUtils.getRelativeTimeSpanString(createdAtMs, now, MINUTE_IN_MILLIS).uppercase() — e.g. "5M AGO" / "ACUM 5M".
       - Unread accent dot: Box(Modifier.size(6.dp).clip(CircleShape).background(colors.accent)) only when readAtMs == null.
       - Accessibility: semantics { contentDescription = "Unread notification" if readAtMs == null }; on the icon, contentDescription = null (decorative).
       - Icon color: when unread = colors.accent; when read = colors.inkSoft.

    Extend localizedTitle/Body (D-25 — new types):
    - In `Notification.localizedTitle()` `when (type)` block, add 3 new cases:
      - INVITE_ACCEPTED_SELF → stringResource(R.string.notification_invite_accepted_self_title, p["registryName"] ?: "a registry")
      - INVITE_ACCEPTED → stringResource(R.string.notification_invite_accepted_title, p["actorName"] ?: "Someone", p["registryName"] ?: "a registry")
      - INVITE_DECLINED → stringResource(R.string.notification_invite_declined_title, p["actorName"] ?: "Someone", p["registryName"] ?: "a registry")
    - In `Notification.localizedBody()` `when (type)` block:
      - INVITE_ACCEPTED_SELF → stringResource(R.string.notification_invite_accepted_self_body, p["registryName"] ?: "a registry")
      - INVITE_ACCEPTED → stringResource(R.string.notification_invite_accepted_body, p["registryName"] ?: "a registry")
      - INVITE_DECLINED → stringResource(R.string.notification_invite_declined_body, p["registryName"] ?: "a registry")
    - In `NotificationType.toIcon()`:
      - INVITE_ACCEPTED_SELF → Icons.Filled.CheckCircle
      - INVITE_ACCEPTED → Icons.Filled.CheckCircle (could use Icons.Filled.PersonAdd if available — fallback to CheckCircle for simplicity)
      - INVITE_DECLINED → Icons.Filled.Block (or Icons.Filled.PersonOff if available — fallback to Block)
    - Preserve all other existing when branches verbatim.

    Preserve:
    - The batched mark-as-read LaunchedEffect (lines 67-79).
    - The InviteResponseSheet host added by Plan 16-04 (do not remove).
    - The branching onClick from Plan 16-04 (shouldOpenInviteSheet).
    - The ViewModel UiState handling (Loading/Unauthenticated/Empty/Loaded).
  </behavior>
  <action>
    Rewrite app/src/main/java/com/giftregistry/ui/notifications/NotificationsScreen.kt to apply the D-09 re-skin while preserving the Plan 16-04 sheet integration and the existing behaviour from the file's current state. The complete rewrite:

    ```kotlin
    package com.giftregistry.ui.notifications

    import android.text.format.DateUtils
    import androidx.compose.foundation.background
    import androidx.compose.foundation.clickable
    import androidx.compose.foundation.layout.*
    import androidx.compose.foundation.lazy.LazyColumn
    import androidx.compose.foundation.lazy.items
    import androidx.compose.foundation.shape.CircleShape
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.automirrored.filled.ArrowBack
    import androidx.compose.material.icons.filled.Block
    import androidx.compose.material.icons.filled.Bookmark
    import androidx.compose.material.icons.filled.CheckCircle
    import androidx.compose.material.icons.filled.Mail
    import androidx.compose.material.icons.filled.Notifications
    import androidx.compose.material.icons.filled.Refresh
    import androidx.compose.material.icons.filled.Schedule
    import androidx.compose.material3.CircularProgressIndicator
    import androidx.compose.material3.ExperimentalMaterial3Api
    import androidx.compose.material3.HorizontalDivider
    import androidx.compose.material3.Icon
    import androidx.compose.material3.IconButton
    import androidx.compose.material3.Scaffold
    import androidx.compose.material3.Text
    import androidx.compose.material3.TopAppBar
    import androidx.compose.material3.TopAppBarDefaults
    import androidx.compose.runtime.Composable
    import androidx.compose.runtime.LaunchedEffect
    import androidx.compose.runtime.getValue
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.draw.clip
    import androidx.compose.ui.graphics.vector.ImageVector
    import androidx.compose.ui.platform.LocalContext
    import androidx.compose.ui.res.stringResource
    import androidx.compose.ui.semantics.contentDescription
    import androidx.compose.ui.semantics.semantics
    import androidx.compose.ui.unit.dp
    import androidx.hilt.navigation.compose.hiltViewModel
    import androidx.lifecycle.compose.collectAsStateWithLifecycle
    import com.giftregistry.R
    import com.giftregistry.domain.model.Notification
    import com.giftregistry.domain.model.NotificationType
    import com.giftregistry.ui.common.GiftMaisonWordmark
    import com.giftregistry.ui.theme.GiftMaisonTheme
    import kotlinx.coroutines.delay

    /**
     * D-09 re-skinned notifications inbox.
     *
     * Visual: gm.paper background; wordmark TopAppBar; flat NotificationCards with
     * gm.line divider separators (no M3 Card elevation); MonoCaps timestamp;
     * accent dot for unread.
     *
     * Behaviour preserved from prior phase: 500ms batched mark-as-read; tap-branching
     * via shouldOpenInviteSheet (D-11) — pending invites open InviteResponseSheet,
     * everything else navigates to RegistryDetail.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun NotificationsScreen(
        onBack: () -> Unit,
        onNavigateToRegistry: (registryId: String) -> Unit,
        viewModel: NotificationsViewModel = hiltViewModel(),
    ) {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val colors = GiftMaisonTheme.colors
        val typography = GiftMaisonTheme.typography
        val spacing = GiftMaisonTheme.spacing

        // Batched mark-as-read: 500ms after the visible unread set changes.
        LaunchedEffect(uiState) {
            val state = uiState
            if (state is NotificationsViewModel.UiState.Loaded) {
                val unreadIds = state.notifications.filter { it.readAtMs == null }.map { it.id }
                if (unreadIds.isNotEmpty()) {
                    delay(500)
                    viewModel.markVisibleRead(unreadIds)
                }
            }
        }

        Scaffold(
            containerColor = colors.paper,
            topBar = {
                TopAppBar(
                    title = { GiftMaisonWordmark() },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.common_back),
                                tint = colors.ink,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.paper),
                )
            },
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                when (val state = uiState) {
                    is NotificationsViewModel.UiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = colors.accent,
                        )
                    }
                    is NotificationsViewModel.UiState.Unauthenticated,
                    is NotificationsViewModel.UiState.Empty -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = spacing.edge),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stringResource(R.string.notifications_empty_heading),
                                style = typography.displayS,
                                color = colors.ink,
                            )
                            Spacer(modifier = Modifier.height(spacing.gap8))
                            Text(
                                text = stringResource(R.string.notifications_empty_body),
                                style = typography.bodyS,
                                color = colors.inkSoft,
                                modifier = Modifier.widthIn(max = 280.dp),
                            )
                        }
                    }
                    is NotificationsViewModel.UiState.Loaded -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(state.notifications, key = { it.id }) { notification ->
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
                                HorizontalDivider(color = colors.line, thickness = 1.dp)
                            }
                        }
                    }
                }
            }
        }

        // D-01 — InviteResponseSheet host
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
    }

    @Composable
    private fun NotificationCard(
        notification: Notification,
        onClick: () -> Unit,
    ) {
        val isRead = notification.readAtMs != null
        val colors = GiftMaisonTheme.colors
        val typography = GiftMaisonTheme.typography
        val spacing = GiftMaisonTheme.spacing
        val context = LocalContext.current

        val titleColor = if (isRead) colors.inkSoft else colors.ink
        val iconTint = if (isRead) colors.inkSoft else colors.accent

        val timestamp = remember(notification.createdAtMs) {
            if (notification.createdAtMs <= 0L) ""
            else DateUtils.getRelativeTimeSpanString(
                notification.createdAtMs,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
            ).toString().uppercase()
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .semantics { if (!isRead) contentDescription = "Unread notification" }
                .padding(horizontal = spacing.gap16, vertical = spacing.gap14),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = notification.type.toIcon(),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(spacing.gap12))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = notification.localizedTitle(),
                        style = typography.bodyL,
                        color = titleColor,
                        modifier = Modifier.weight(1f),
                    )
                    if (timestamp.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(spacing.gap8))
                        Text(
                            text = timestamp,
                            style = typography.monoCaps,
                            color = colors.inkSoft,
                        )
                    }
                    if (!isRead) {
                        Spacer(modifier = Modifier.width(spacing.gap4))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(colors.accent),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(spacing.gap4))
                Text(
                    text = notification.localizedBody(),
                    style = typography.bodyM,
                    color = colors.inkSoft,
                )
            }
        }
    }

    /**
     * Per-type icon for the inbox card. UNKNOWN falls back to the generic bell.
     */
    private fun NotificationType.toIcon(): ImageVector = when (this) {
        NotificationType.INVITE -> Icons.Filled.Mail
        NotificationType.RESERVATION_CREATED -> Icons.Filled.Bookmark
        NotificationType.ITEM_PURCHASED -> Icons.Filled.CheckCircle
        NotificationType.RESERVATION_EXPIRED -> Icons.Filled.Schedule
        NotificationType.RE_RESERVE_WINDOW -> Icons.Filled.Refresh
        NotificationType.INVITE_ACCEPTED_SELF -> Icons.Filled.CheckCircle
        NotificationType.INVITE_ACCEPTED -> Icons.Filled.CheckCircle
        NotificationType.INVITE_DECLINED -> Icons.Filled.Block
        NotificationType.UNKNOWN -> Icons.Filled.Notifications
    }

    @Composable
    private fun Notification.localizedTitle(): String {
        val p = payload
        return when (type) {
            NotificationType.INVITE -> stringResource(
                R.string.notification_invite_title,
                p["actorName"] ?: "Someone",
                p["registryName"] ?: "a registry",
            )
            NotificationType.RESERVATION_CREATED -> stringResource(
                R.string.notification_reservation_created_title,
                p["itemName"] ?: "an item",
            )
            NotificationType.ITEM_PURCHASED -> stringResource(
                R.string.notification_item_purchased_title,
                p["itemName"] ?: "an item",
            )
            NotificationType.RESERVATION_EXPIRED -> stringResource(
                R.string.notification_reservation_expired_title,
                p["itemName"] ?: "an item",
            )
            NotificationType.RE_RESERVE_WINDOW -> stringResource(
                R.string.notification_re_reserve_window_title,
                p["itemName"] ?: "an item",
            )
            NotificationType.INVITE_ACCEPTED_SELF -> stringResource(
                R.string.notification_invite_accepted_self_title,
                p["registryName"] ?: "a registry",
            )
            NotificationType.INVITE_ACCEPTED -> stringResource(
                R.string.notification_invite_accepted_title,
                p["actorName"] ?: "Someone",
                p["registryName"] ?: "a registry",
            )
            NotificationType.INVITE_DECLINED -> stringResource(
                R.string.notification_invite_declined_title,
                p["actorName"] ?: "Someone",
                p["registryName"] ?: "a registry",
            )
            NotificationType.UNKNOWN -> titleFallback
        }
    }

    @Composable
    private fun Notification.localizedBody(): String {
        val p = payload
        return when (type) {
            NotificationType.INVITE -> stringResource(R.string.notification_invite_body)
            NotificationType.RESERVATION_CREATED -> stringResource(
                R.string.notification_reservation_created_body,
                p["actorName"] ?: "Someone",
                p["itemName"] ?: "an item",
                p["registryName"] ?: "a registry",
            )
            NotificationType.ITEM_PURCHASED -> stringResource(
                R.string.notification_item_purchased_body,
                p["itemName"] ?: "an item",
                p["registryName"] ?: "a registry",
            )
            NotificationType.RESERVATION_EXPIRED -> stringResource(
                R.string.notification_reservation_expired_body,
                p["itemName"] ?: "an item",
                p["registryName"] ?: "a registry",
            )
            NotificationType.RE_RESERVE_WINDOW -> stringResource(
                R.string.notification_re_reserve_window_body,
                p["itemName"] ?: "an item",
                p["registryName"] ?: "a registry",
            )
            NotificationType.INVITE_ACCEPTED_SELF -> stringResource(
                R.string.notification_invite_accepted_self_body,
                p["registryName"] ?: "a registry",
            )
            NotificationType.INVITE_ACCEPTED -> stringResource(
                R.string.notification_invite_accepted_body,
                p["registryName"] ?: "a registry",
            )
            NotificationType.INVITE_DECLINED -> stringResource(
                R.string.notification_invite_declined_body,
                p["registryName"] ?: "a registry",
            )
            NotificationType.UNKNOWN -> bodyFallback
        }
    }
    ```

    Verify GiftMaisonWordmark import path. If it's at a different path (e.g., `com.giftregistry.ui.theme.GiftMaisonWordmark`), adjust the import. Use `grep -rn "fun GiftMaisonWordmark\|class GiftMaisonWordmark" app/src/main/` to confirm.
  </action>
  <verify>
    <automated>./gradlew :app:compileDebugKotlin 2>&1 | tail -20 && ./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.notifications.*" --tests "com.giftregistry.domain.model.NotificationTypeFromWireTest" --tests "com.giftregistry.LocalizationParityTest" 2>&1 | tail -30</automated>
  </verify>
  <acceptance_criteria>
    - app/src/main/java/com/giftregistry/ui/notifications/NotificationsScreen.kt contains string "GiftMaisonWordmark()"
    - NotificationsScreen.kt contains string "containerColor = colors.paper"
    - NotificationsScreen.kt contains string "HorizontalDivider(color = colors.line"
    - NotificationsScreen.kt contains string "CircleShape"
    - NotificationsScreen.kt contains string ".size(6.dp)"
    - NotificationsScreen.kt contains string "typography.bodyL"
    - NotificationsScreen.kt contains string "typography.bodyM"
    - NotificationsScreen.kt contains string "typography.monoCaps"
    - NotificationsScreen.kt contains string "typography.displayS"
    - NotificationsScreen.kt contains string "typography.bodyS"
    - NotificationsScreen.kt contains string "DateUtils.getRelativeTimeSpanString"
    - NotificationsScreen.kt contains string "notifications_empty_heading"
    - NotificationsScreen.kt contains string "notifications_empty_body"
    - NotificationsScreen.kt contains string "NotificationType.INVITE_ACCEPTED_SELF"
    - NotificationsScreen.kt contains string "NotificationType.INVITE_ACCEPTED"
    - NotificationsScreen.kt contains string "NotificationType.INVITE_DECLINED"
    - NotificationsScreen.kt contains string "R.string.notification_invite_accepted_self_title"
    - NotificationsScreen.kt contains string "R.string.notification_invite_accepted_title"
    - NotificationsScreen.kt contains string "R.string.notification_invite_declined_title"
    - NotificationsScreen.kt does NOT contain string "MaterialTheme.colorScheme" (re-skin replaces all M3 theme refs with GiftMaisonTheme)
    - NotificationsScreen.kt does NOT contain string "androidx.compose.material3.Card" (Card elevation dropped per D-09)
    - NotificationsScreen.kt contains string "shouldOpenInviteSheet(notification)" (Plan 16-04 branching preserved)
    - NotificationsScreen.kt contains string "InviteResponseSheet(" (Plan 16-04 sheet host preserved)
    - ./gradlew :app:compileDebugKotlin exits 0
    - ./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.notifications.*" exits 0
    - ./gradlew :app:testDebugUnitTest --tests "com.giftregistry.LocalizationParityTest" exits 0
  </acceptance_criteria>
  <done>NotificationsScreen + NotificationCard re-skinned to GiftMaison; localizedTitle/Body extended for 3 new types; build green; all unit tests green.</done>
</task>

<task type="auto">
  <name>Task 3: Append StyleGuidePreview sections for NotificationsScreen + InviteResponseSheet</name>
  <read_first>
    - app/src/main/java/com/giftregistry/ui/theme/preview/StyleGuidePreview.kt (existing structure — append at end, follow Phase 8-11 pattern)
    - app/src/main/java/com/giftregistry/ui/notifications/NotificationsScreen.kt (the re-skinned screen from Task 2)
    - app/src/main/java/com/giftregistry/ui/notifications/InviteResponseSheet.kt (sheet to preview from Plan 16-04)
  </read_first>
  <behavior>
    Append two new @Preview sections to StyleGuidePreview.kt:

    1. NotificationsInboxPreview — renders a mock LazyColumn of mixed read/unread notifications including one pending INVITE (with pendingEntryKey), one legacy INVITE (without pendingEntryKey), one INVITE_ACCEPTED_SELF (JOINED), one RESERVATION_CREATED, one ITEM_PURCHASED. Wrap in GiftMaisonTheme. No ViewModel — render NotificationCard directly with hand-crafted Notification fixtures, or use a simple PreviewParameterProvider.

    2. InviteResponseSheetPreview — three preview variants showing the three sheet states: Idle, Submitting, Error. Since ModalBottomSheet doesn't render statically in Compose previews (animated component), render the InviteResponseSheetContent — a refactor target: the content tree (everything inside ModalBottomSheet) extracted as a private InviteResponseSheetContent composable for previewability. If extraction is too disruptive, skip the sheet itself and preview just the warn-banner + button row as a standalone Card.

    Pragmatic approach: rather than refactoring Plan 16-04's code, preview just the constituent atoms (NotificationCard variants) in this task. Mark the InviteResponseSheet preview as a follow-up in the SUMMARY.md if extraction proves too risky.
  </behavior>
  <action>
    Append to app/src/main/java/com/giftregistry/ui/theme/preview/StyleGuidePreview.kt (after the existing previews):

    ```kotlin
    // ============================================================
    // Phase 16 — Notifications Inbox + Invite Accept/Decline
    // ============================================================

    @Preview(showBackground = true, name = "Phase 16 — NotificationsInbox (mixed states)")
    @Composable
    private fun NotificationsInboxPreview() {
        GiftMaisonTheme {
            Box(modifier = Modifier.fillMaxSize().background(GiftMaisonTheme.colors.paper)) {
                Column {
                    PreviewNotificationRow(
                        type = NotificationType.INVITE,
                        title = "Maria invited you to \"Sara's birthday party\"",
                        body = "Tap to view Sara's birthday party",
                        timestampMs = System.currentTimeMillis() - 3 * 60_000L,
                        isUnread = true,
                    )
                    PreviewNotificationRow(
                        type = NotificationType.INVITE_ACCEPTED_SELF,
                        title = "You joined \"Sara's birthday party\"",
                        body = "Tap to view Sara's birthday party",
                        timestampMs = System.currentTimeMillis() - 30 * 60_000L,
                        isUnread = false,
                    )
                    PreviewNotificationRow(
                        type = NotificationType.RESERVATION_CREATED,
                        title = "Andrei reserved \"Coffee maker\"",
                        body = "Andrei reserved Coffee maker from Sara's list",
                        timestampMs = System.currentTimeMillis() - 60 * 60_000L,
                        isUnread = true,
                    )
                    PreviewNotificationRow(
                        type = NotificationType.ITEM_PURCHASED,
                        title = "Coffee maker was purchased",
                        body = "From Sara's list",
                        timestampMs = System.currentTimeMillis() - 2 * 60 * 60_000L,
                        isUnread = false,
                    )
                    PreviewNotificationRow(
                        type = NotificationType.INVITE_DECLINED,
                        title = "Alex declined your invite to \"Housewarming\"",
                        body = "Housewarming",
                        timestampMs = System.currentTimeMillis() - 24 * 60 * 60_000L,
                        isUnread = false,
                    )
                }
            }
        }
    }

    @Preview(showBackground = true, name = "Phase 16 — NotificationsInbox (empty state)")
    @Composable
    private fun NotificationsInboxEmptyPreview() {
        GiftMaisonTheme {
            Box(
                modifier = Modifier.fillMaxSize().background(GiftMaisonTheme.colors.paper),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No notifications yet",
                        style = GiftMaisonTheme.typography.displayS,
                        color = GiftMaisonTheme.colors.ink,
                    )
                    Spacer(Modifier.height(GiftMaisonTheme.spacing.gap8))
                    Text(
                        text = "When someone invites you to a registry or reserves a gift, you'll see it here.",
                        style = GiftMaisonTheme.typography.bodyS,
                        color = GiftMaisonTheme.colors.inkSoft,
                        modifier = Modifier.widthIn(max = 280.dp),
                    )
                }
            }
        }
    }

    @Composable
    private fun PreviewNotificationRow(
        type: NotificationType,
        title: String,
        body: String,
        timestampMs: Long,
        isUnread: Boolean,
    ) {
        val colors = GiftMaisonTheme.colors
        val typography = GiftMaisonTheme.typography
        val spacing = GiftMaisonTheme.spacing
        val titleColor = if (isUnread) colors.ink else colors.inkSoft
        val iconTint = if (isUnread) colors.accent else colors.inkSoft

        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.gap16, vertical = spacing.gap14),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = when (type) {
                        NotificationType.INVITE -> Icons.Filled.Mail
                        NotificationType.INVITE_ACCEPTED_SELF -> Icons.Filled.CheckCircle
                        NotificationType.RESERVATION_CREATED -> Icons.Filled.Bookmark
                        NotificationType.ITEM_PURCHASED -> Icons.Filled.CheckCircle
                        NotificationType.INVITE_DECLINED -> Icons.Filled.Block
                        else -> Icons.Filled.Notifications
                    },
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(spacing.gap12))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = typography.bodyL,
                            color = titleColor,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "5M AGO",
                            style = typography.monoCaps,
                            color = colors.inkSoft,
                        )
                        if (isUnread) {
                            Spacer(Modifier.width(spacing.gap4))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(colors.accent),
                            )
                        }
                    }
                    Spacer(Modifier.height(spacing.gap4))
                    Text(
                        text = body,
                        style = typography.bodyM,
                        color = colors.inkSoft,
                    )
                }
            }
            HorizontalDivider(color = colors.line, thickness = 1.dp)
        }
    }
    ```

    Required imports to add to the top of StyleGuidePreview.kt (skip if already present from earlier sections):
    - `import androidx.compose.foundation.background`
    - `import androidx.compose.foundation.layout.*`
    - `import androidx.compose.foundation.shape.CircleShape`
    - `import androidx.compose.material.icons.Icons`
    - `import androidx.compose.material.icons.filled.{Block, Bookmark, CheckCircle, Mail, Notifications}`
    - `import androidx.compose.material3.{HorizontalDivider, Icon, Text}`
    - `import androidx.compose.runtime.Composable`
    - `import androidx.compose.ui.Alignment`
    - `import androidx.compose.ui.Modifier`
    - `import androidx.compose.ui.draw.clip`
    - `import androidx.compose.ui.tooling.preview.Preview`
    - `import androidx.compose.ui.unit.dp`
    - `import com.giftregistry.domain.model.NotificationType`
    - `import com.giftregistry.ui.theme.GiftMaisonTheme`

    Skip the InviteResponseSheet preview (ModalBottomSheet doesn't render statically in @Preview without a complex test harness). Document this in the SUMMARY.md as a known-non-coverage area mitigated by on-device UAT in Plan 16-06.
  </action>
  <verify>
    <automated>./gradlew :app:compileDebugKotlin 2>&1 | tail -10</automated>
  </verify>
  <acceptance_criteria>
    - app/src/main/java/com/giftregistry/ui/theme/preview/StyleGuidePreview.kt contains string "Phase 16 — NotificationsInbox"
    - StyleGuidePreview.kt contains string "NotificationType.INVITE_ACCEPTED_SELF"
    - StyleGuidePreview.kt contains string "NotificationType.INVITE_DECLINED"
    - StyleGuidePreview.kt contains string "No notifications yet" (empty state preview)
    - StyleGuidePreview.kt contains string "HorizontalDivider(color = colors.line"
    - StyleGuidePreview.kt contains string ".size(6.dp)"
    - ./gradlew :app:compileDebugKotlin exits 0
  </acceptance_criteria>
  <done>StyleGuidePreview appended with 2 new @Preview sections (Loaded + Empty); InviteResponseSheet preview deferred to on-device UAT per documented decision.</done>
</task>

</tasks>

<verification>
- ./gradlew :app:compileDebugKotlin exits 0.
- ./gradlew :app:testDebugUnitTest exits 0 (entire suite; key tests: notifications.*, NotificationTypeFromWireTest, LocalizationParityTest).
- grep for "MaterialTheme.colorScheme" in NotificationsScreen.kt returns 0 hits (full re-skin verified).
- grep for "androidx.compose.material3.Card" import in NotificationsScreen.kt returns 0 hits (D-09 elevation dropped).
- Empty state, loaded state, and the 3 new NotificationType values all preview correctly when opening the @Preview in Android Studio.
</verification>

<success_criteria>
- 4 files modified.
- D-09 visual re-skin complete (wordmark, paper bg, line dividers, MonoCaps timestamp, accent unread dot, GiftMaison typography throughout).
- D-25 localizedTitle/Body extended for 3 new types — confirmation messages render with proper EN + RO copy.
- LocalizationParityTest preserves key parity across the new 20 keys × 2 locales.
- StyleGuidePreview previewable in Android Studio for offline visual review.
</success_criteria>

<output>
After completion, create `.planning/phases/16-android-notifications-inbox-invite-accept-decline/16-05-SUMMARY.md` listing the 4 file edits, the 20 strings × 2 locales added, the InviteResponseSheet preview deferral with rationale, and test results.
</output>
