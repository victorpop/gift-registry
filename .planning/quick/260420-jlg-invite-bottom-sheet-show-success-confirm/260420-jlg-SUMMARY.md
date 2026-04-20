---
phase: quick-260420-jlg
plan: 01
subsystem: android-ui
tags: [invite, bottom-sheet, ux, compose, coroutines]
dependency_graph:
  requires:
    - InviteViewModel (unchanged contract: email, isSending, inviteSent, resetInviteSent)
    - R.string.registry_invite_success (existing, unchanged)
  provides:
    - Success confirmation UX loop for registry invite flow
  affects:
    - RegistryDetailScreen invite entry point (behavior change: sheet now auto-closes)
tech_stack:
  added: []
  patterns:
    - "delay → reset VM → onDismiss() inside LaunchedEffect for timed auto-dismiss"
    - "enabled = !isSending && !inviteSent pattern to lock inputs during success window"
    - "Spinner guard isSending && !inviteSent to eliminate VM flag-overlap flash"
key_files:
  created: []
  modified:
    - app/src/main/java/com/giftregistry/ui/registry/invite/InviteBottomSheet.kt
decisions:
  - "Skipped optional CheckCircle icon polish — bug fix is the single responsibility; icon/typography change would bloat the diff beyond what the issue required"
  - "1500ms literal (no constant) — per plan constraint: single-use timing, intent captured in inline comment"
metrics:
  duration_seconds: 70
  tasks_completed: 1
  files_modified: 1
  completed_date: "2026-04-20"
requirements:
  - QUICK-JLG-01
---

# Quick Task 260420-jlg: Invite Bottom Sheet — Show Success Confirmation Summary

Fixed the InviteBottomSheet success confirmation UX so the "Invitation sent" banner stays visible for 1500ms before the sheet auto-dismisses, and inputs are locked during that confirmation window.

## Objective Recap

Before: On successful invite, `LaunchedEffect(inviteSent)` immediately called `resetInviteSent()`, collapsing the success banner in a single frame. The sheet never auto-closed, leaving the user with no feedback that the invite succeeded (even though the backend + email + FCM worked correctly).

After: On successful invite, the banner is held on screen for 1500ms, then the VM state is reset and `onDismiss()` is invoked so the parent closes the sheet. While the banner is on screen, the email field and Send button are disabled, and the spinner is suppressed to prevent any visual stutter during the brief VM flag overlap window.

## Diff Applied

File: `app/src/main/java/com/giftregistry/ui/registry/invite/InviteBottomSheet.kt` (+9 / −3)

1. Added import: `import kotlinx.coroutines.delay`
2. Replaced the `LaunchedEffect(inviteSent)` body:
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
3. Added `enabled = !isSending && !inviteSent` to `OutlinedTextField`.
4. Changed Button `enabled` from `!isSending && email.isNotBlank()` to `!isSending && !inviteSent && email.isNotBlank()`.
5. Tightened spinner guard from `if (isSending)` to `if (isSending && !inviteSent)` to cover the brief VM window where `_inviteSent.value = true` is set inside `onSuccess` before `_isSending.value = false` runs at the end of `onSendInvite`.

## Optional Polish — Skipped

The plan offered an optional CheckCircle icon + bodyLarge typography polish for the success banner. **Skipped.** Rationale: the reported bug was a presentation/timing bug; adding an icon + restyling the confirmation widens the diff beyond the single responsibility of fixing the flash-and-no-close behavior. Keeping the visual unchanged limits blast radius and keeps the fix reviewable at a glance. If product wants the icon polish it can be a follow-up quick task.

## Verification

### Build

```
./gradlew :app:assembleDebug
BUILD SUCCESSFUL in 16s
exit 0
```

### Grep checks (all passed)

| Check | Expected | Actual |
| ----- | -------- | ------ |
| `import kotlinx.coroutines.delay` | 1 hit | 1 hit (line 28) |
| `delay(1500L)` | 1 hit | 1 hit (line 48) |
| `onDismiss()` inside LaunchedEffect | 1 hit | 1 hit (line 50) |
| `enabled = !isSending && !inviteSent` | 2 hits (TextField + Button) | 2 hits (lines 77, 100) |
| `isSending && !inviteSent` substring | 3 hits (TextField + Button + spinner) | 3 hits (lines 77, 100, 103) |

### No collateral changes

`git diff --stat` on task commit:
```
 .../com/giftregistry/ui/registry/invite/InviteBottomSheet.kt | 12 +++++++++---
 1 file changed, 9 insertions(+), 3 deletions(-)
```

Files explicitly NOT touched (as required by the plan):
- `app/src/main/java/com/giftregistry/ui/registry/invite/InviteViewModel.kt` — untouched
- `app/src/main/res/values/strings.xml` — untouched (no new strings added; `registry_invite_success` reused)
- `app/src/main/res/values-ro/strings.xml` — untouched
- `RegistryDetailScreen.kt` callsite — untouched (existing `onDismiss = { showInviteSheet = false }` binding continues to work)

## Must-Haves (frontmatter) — all satisfied

- [x] Success confirmation text remains visible after a successful invite (no single-frame flash): `LaunchedEffect` now waits 1500ms before resetting.
- [x] After ~1500ms of showing the success confirmation, the bottom sheet auto-dismisses via `onDismiss()`.
- [x] While the success confirmation is visible, the email `OutlinedTextField` and Send Button are disabled (`enabled = !isSending && !inviteSent` / `!isSending && !inviteSent && email.isNotBlank()`).
- [x] Loading spinner does NOT appear simultaneously with the success confirmation (`if (isSending && !inviteSent)` guard).
- [x] `InviteViewModel.inviteSent` state is reset before dismissal (`viewModel.resetInviteSent()` fires before `onDismiss()` inside the LaunchedEffect) — reopening the sheet starts clean.

## Key Links (frontmatter) — all present

- `InviteBottomSheet.kt LaunchedEffect(inviteSent)` → `onDismiss()` via `kotlinx.coroutines.delay(1500L)` and `viewModel.resetInviteSent()` — pattern match confirmed (lines 43-52).
- `OutlinedTextField + Button enabled` wired to `inviteSent` state (lines 77, 100).

## Deviations from Plan

None — plan executed exactly as written. The optional CheckCircle polish was explicitly marked optional ("skip if it bloats the diff") and was skipped per that guidance.

## Commits

- `b2eb13b` — fix(quick-260420-jlg-01): InviteBottomSheet success confirmation timing + disabled inputs

## Self-Check: PASSED

- [x] File modified: `app/src/main/java/com/giftregistry/ui/registry/invite/InviteBottomSheet.kt` (verified via git show)
- [x] Commit exists: `b2eb13b` (verified via git log)
- [x] Build passes: `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL, exit 0
- [x] No `InviteViewModel.kt` / `strings.xml` / `RegistryDetailScreen.kt` changes
