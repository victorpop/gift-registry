---
phase: quick-260420-jlg
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/java/com/giftregistry/ui/registry/invite/InviteBottomSheet.kt
autonomous: true
requirements:
  - QUICK-JLG-01
must_haves:
  truths:
    - "After a successful invite, the success confirmation text remains visible to the user (no single-frame flash)"
    - "After ~1500ms of showing the success confirmation, the bottom sheet auto-dismisses by invoking the parent's onDismiss callback"
    - "While the success confirmation is visible, the email OutlinedTextField and the Send button are disabled — user cannot type or tap during the auto-dismiss window"
    - "The loading spinner does NOT appear simultaneously with the success confirmation (success state mutually exclusive with sending state)"
    - "The InviteViewModel.inviteSent state is reset (resetInviteSent()) before/at dismissal so reopening the sheet starts clean"
  artifacts:
    - path: "app/src/main/java/com/giftregistry/ui/registry/invite/InviteBottomSheet.kt"
      provides: "Fixed LaunchedEffect with delay + dismiss + reset; disabled-during-confirmation states on TextField and Button"
      contains: "kotlinx.coroutines.delay"
  key_links:
    - from: "InviteBottomSheet.kt LaunchedEffect(inviteSent)"
      to: "onDismiss() callback prop"
      via: "after kotlinx.coroutines.delay(1500L) and viewModel.resetInviteSent()"
      pattern: "delay\\(1500L\\)[\\s\\S]*resetInviteSent\\(\\)[\\s\\S]*onDismiss\\(\\)"
    - from: "OutlinedTextField + Button enabled prop"
      to: "inviteSent state"
      via: "enabled = !isSending && !inviteSent (Button also keeps email.isNotBlank())"
      pattern: "enabled\\s*=\\s*!isSending\\s*&&\\s*!inviteSent"
---

<objective>
Fix the InviteBottomSheet success confirmation UX. Currently, on a successful invite the success banner appears for ~1 frame then disappears (LaunchedEffect immediately calls resetInviteSent), and the sheet never auto-closes — leaving the user with no feedback that anything happened. The backend invite IS succeeding (verified in functions/src/registry/inviteToRegistry.ts: invitedUsers updated, mail doc written, FCM push sent) — this is a pure presentation bug.

Purpose: Restore the confirmation feedback loop. Gift registry owners must see clear acknowledgement that an invite was sent, then have the sheet politely close itself so they can return to the registry view without an extra tap.

Output: Modified InviteBottomSheet.kt with: (1) delayed reset + auto-dismiss in LaunchedEffect, (2) disabled input/button while confirmation is on screen, (3) spinner suppressed once success arrives.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@./CLAUDE.md

@app/src/main/java/com/giftregistry/ui/registry/invite/InviteBottomSheet.kt
@app/src/main/java/com/giftregistry/ui/registry/invite/InviteViewModel.kt

<interfaces>
<!-- Confirmed contracts the executor needs. No codebase exploration required. -->

From `InviteViewModel.kt` (DO NOT MODIFY — VM stays as-is):
```kotlin
val email: MutableStateFlow<String>
val isSending: StateFlow<Boolean>
val error: StateFlow<String?>
val inviteSent: StateFlow<Boolean>

fun onSendInvite(registryId: String)
fun clearError()
fun resetInviteSent()  // sets _inviteSent.value = false
```

From `InviteBottomSheet.kt` signature (KEEP UNCHANGED):
```kotlin
@Composable
fun InviteBottomSheet(
    registryId: String,
    onDismiss: () -> Unit,
    viewModel: InviteViewModel = hiltViewModel()
)
```

Existing localized strings (already translated, REUSE — do NOT add new strings):
- `R.string.registry_invite_success` → EN: "Invitation sent" / RO: "Invitatia a fost trimisa"

No existing tests for `InviteBottomSheet` or `InviteViewModel` (grep confirmed in `**/test/**` and `**/androidTest/**`).
</interfaces>

State management notes (from STATE.md, Phase 03 decision):
- "InviteBottomSheet resets inviteSent via resetInviteSent() — allows sending multiple invites per bottom sheet session" — this contract is preserved: reset still happens, just delayed by 1500ms and immediately followed by dismiss.
</context>

<tasks>

<task type="auto">
  <name>Task 1: Fix InviteBottomSheet success confirmation timing + disable inputs during confirm</name>
  <files>app/src/main/java/com/giftregistry/ui/registry/invite/InviteBottomSheet.kt</files>
  <action>
Make exactly these surgical edits to `app/src/main/java/com/giftregistry/ui/registry/invite/InviteBottomSheet.kt`:

1. **Add import** for `kotlinx.coroutines.delay`:
   ```kotlin
   import kotlinx.coroutines.delay
   ```
   Place alphabetically among existing kotlinx/androidx imports.

2. **Replace the broken LaunchedEffect (lines 42-47)** so the success banner stays visible for 1500ms, then resets VM state and dismisses the sheet via the parent callback:
   ```kotlin
   LaunchedEffect(inviteSent) {
       if (inviteSent) {
           // Hold the success confirmation on screen long enough for the user to register it,
           // then auto-dismiss so they don't have to tap away. 1500ms is the established
           // "show & go" window — long enough to read "Invitation sent", short enough to feel snappy.
           delay(1500L)
           viewModel.resetInviteSent()
           onDismiss()
       }
   }
   ```

3. **Disable the OutlinedTextField while sending OR while showing success** so the user can't type a new email during the 1500ms close animation. Add `enabled = !isSending && !inviteSent` to the OutlinedTextField (currently has no `enabled` param):
   ```kotlin
   OutlinedTextField(
       value = email,
       onValueChange = { viewModel.email.value = it },
       label = { Text(stringResource(R.string.registry_invite_email_label)) },
       modifier = Modifier.fillMaxWidth(),
       enabled = !isSending && !inviteSent,
       singleLine = true,
       keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
   )
   ```

4. **Disable the Send Button while showing success** by adding `&& !inviteSent` to its existing `enabled` clause:
   ```kotlin
   Button(
       onClick = { viewModel.onSendInvite(registryId) },
       enabled = !isSending && !inviteSent && email.isNotBlank(),
       modifier = Modifier.fillMaxWidth()
   ) { ... }
   ```

5. **Suppress the spinner once success arrives.** Today the spinner only renders when `isSending` is true, and `_isSending` is set back to false before `_inviteSent` is set to true in the VM (`onSuccess { _inviteSent.value = true; ... }; _isSending.value = false`). Re-read `InviteViewModel.onSendInvite` — actually `_inviteSent.value = true` happens inside `onSuccess` which executes BEFORE `_isSending.value = false` at the end. There is a brief overlap where both are true. To eliminate the visual stutter, change the spinner guard inside the Button content from `if (isSending)` to `if (isSending && !inviteSent)`:
   ```kotlin
   if (isSending && !inviteSent) {
       CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
   }
   ```

6. **Optional polish (only if it stays clean — skip if it bloats the diff):** Wrap the success Text in a Row with a leading `Icons.Default.CheckCircle` icon tinted `MaterialTheme.colorScheme.primary`, and bump the Text style to `MaterialTheme.typography.bodyLarge`. If you do this, add imports:
   ```kotlin
   import androidx.compose.foundation.layout.Row
   import androidx.compose.material.icons.Icons
   import androidx.compose.material.icons.filled.CheckCircle
   import androidx.compose.material3.Icon
   import androidx.compose.ui.Alignment
   ```
   And replace the existing `if (inviteSent) { Text(...) }` block with:
   ```kotlin
   if (inviteSent) {
       Row(
           verticalAlignment = Alignment.CenterVertically,
           horizontalArrangement = Arrangement.spacedBy(8.dp)
       ) {
           Icon(
               imageVector = Icons.Default.CheckCircle,
               contentDescription = null,
               tint = MaterialTheme.colorScheme.primary
           )
           Text(
               text = stringResource(R.string.registry_invite_success),
               color = MaterialTheme.colorScheme.primary,
               style = MaterialTheme.typography.bodyLarge
           )
       }
   }
   ```
   If skipping, leave the existing Text block untouched.

Do NOT modify `InviteViewModel.kt`. Do NOT add new strings. Do NOT change the function signature. Do NOT alter the `onDismissRequest = onDismiss` wiring on `ModalBottomSheet` (the user can still swipe/tap-outside to dismiss manually before the 1500ms elapses — that's correct behavior).

**Why these constraints:**
- 1500ms literal (no constant): single-use timing per planning constraints; explicit comment captures the UX intent.
- `enabled = !isSending && !inviteSent` on TextField: prevents the user typing into a field that's about to disappear.
- Spinner gate `isSending && !inviteSent`: covers the brief VM window where both flags overlap (`_inviteSent.value = true` is set inside `onSuccess` before `_isSending.value = false` runs at the end of `onSendInvite`).
- `onDismiss()` is the existing prop — `RegistryDetailScreen` already toggles `showInviteSheet = false` in response, so we just invoke it.
  </action>
  <verify>
    <automated>./gradlew :app:assembleDebug</automated>
  </verify>
  <done>
    - `app/src/main/java/com/giftregistry/ui/registry/invite/InviteBottomSheet.kt` imports `kotlinx.coroutines.delay`.
    - The `LaunchedEffect(inviteSent)` block contains `delay(1500L)`, then `viewModel.resetInviteSent()`, then `onDismiss()` (in that order, all inside `if (inviteSent) { ... }`).
    - `OutlinedTextField` has `enabled = !isSending && !inviteSent`.
    - `Button` has `enabled = !isSending && !inviteSent && email.isNotBlank()`.
    - Spinner inside Button is guarded by `if (isSending && !inviteSent)`.
    - `./gradlew :app:assembleDebug` exits 0.
    - No changes to `InviteViewModel.kt`, no changes to `strings.xml` files, no changes to `RegistryDetailScreen` callsite.
  </done>
</task>

</tasks>

<verification>
After the task completes:

1. **Build passes:** `./gradlew :app:assembleDebug` returns exit 0.
2. **Code grep checks** (all must match):
   - `grep -n "import kotlinx.coroutines.delay" app/src/main/java/com/giftregistry/ui/registry/invite/InviteBottomSheet.kt` → 1 hit
   - `grep -n "delay(1500L)" app/src/main/java/com/giftregistry/ui/registry/invite/InviteBottomSheet.kt` → 1 hit
   - `grep -n "onDismiss()" app/src/main/java/com/giftregistry/ui/registry/invite/InviteBottomSheet.kt` → 1 hit (inside the LaunchedEffect)
   - `grep -nE "enabled\s*=\s*!isSending\s*&&\s*!inviteSent" app/src/main/java/com/giftregistry/ui/registry/invite/InviteBottomSheet.kt` → 2 hits (TextField + Button)
   - `grep -nE "isSending\s*&&\s*!inviteSent" app/src/main/java/com/giftregistry/ui/registry/invite/InviteBottomSheet.kt` → 1 hit (spinner guard) — note: this is a substring of the Button `enabled =` line too; expect 3 hits total across TextField/Button/spinner. Validate by visual scan of the spinner block.
3. **No collateral changes:** `git diff --stat` should show exactly 1 file changed: `app/src/main/java/com/giftregistry/ui/registry/invite/InviteBottomSheet.kt`.

Manual smoke (NOT required for completion, recommended after build):
- Run app with `-Puse_emulator`, open a registry, tap Invite, type a valid email, hit Send. Expect: spinner → spinner replaced by "Invitation sent" success banner → sheet closes itself ~1.5s later. Reopening the sheet should show a clean empty state.
</verification>

<success_criteria>
- Build succeeds: `./gradlew :app:assembleDebug` exit 0.
- All goal-backward truths satisfied (see frontmatter `must_haves.truths`).
- All key_links present (see frontmatter `must_haves.key_links`).
- Single file changed; no new strings; no VM changes.
</success_criteria>

<output>
After completion, create `.planning/quick/260420-jlg-invite-bottom-sheet-show-success-confirm/260420-jlg-SUMMARY.md` capturing:
- The exact diff applied (delay + dismiss + disabled-states + spinner guard)
- Whether the optional CheckCircle icon polish was included or skipped (and why if skipped)
- Build verification command output (exit code)
- Confirmation that `InviteViewModel.kt`, `strings.xml`, and `RegistryDetailScreen.kt` were NOT touched
</output>
