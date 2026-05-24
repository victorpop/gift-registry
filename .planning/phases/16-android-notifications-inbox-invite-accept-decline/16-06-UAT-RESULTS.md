---
status: in_progress
phase: 16-android-notifications-inbox-invite-accept-decline
source: [16-06-PLAN.md, 16-VERIFICATION.md]
started: 2026-05-24T20:00:00Z
updated: 2026-05-24T20:00:00Z
devices:
  account_a_owner: emulator (Pixel 36.1 AVD, USE_FIREBASE_EMULATOR=false)
  account_b_invitee: Huawei (USE_FIREBASE_EMULATOR=false)
backend: production (gift-registry-ro)
---

## Tests

### A. Accept-gate flow (D-01, D-05, D-21, D-23)

#### A1. Send invite as owner
expected: Account A (emulator) opens private registry, taps invite, enters Account B's email, confirms invite sent. App reports success.
result: PASS

#### A2. Verify pending state in Firestore Console
expected: `registries/{id}` shows `pendingInvitedUsers[B-uid] = true` AND `invitedUsers[B-uid]` is absent or false.
result: PASS

#### A3. Invitee receives FCM push
expected: Account B's Huawei shows system-tray notification "Account A invited you to {registry}". Tap → opens app.
result: PASS — push delivered when app is backgrounded. Foreground behavior is Android-standard (push routed to onMessageReceived, badge still increments via Firestore listener). Required surfacing pre-existing Phase 6 FCM-token-on-signin gap (fixed in commit cf79b51).

#### A4. Invitee sees actionable card in inbox
expected: Account B opens inbox — first card is INVITE with proper title, body, MonoCaps timestamp, accent unread dot.
result: PASS — bell + badge work. Required surfacing pre-existing Phase 10 inbox-entry-point gap (fixed in commit 8e901fc).

#### A5. Tap card opens InviteResponseSheet
expected: ModalBottomSheet rises with registry hero (gradient placeholder or cover image), "Account A invited you to" title, registry name in italic-accent Display L, optional event date in MonoCaps. Accent dot disappears (mark-as-read fires).
result: PASS — partial OK with minor visual flags noted for future polish (no blockers, details deferred).

#### A6. Cannot dismiss sheet during loading
expected: Tap Accept → CircularProgressIndicator appears inside Accept button → swipe down on sheet → sheet stays open (D-07 dismissal guard).
result: PASS

#### A7. Auto-navigation on Accept
expected: When Accept completes, sheet auto-dismisses + app navigates to RegistryDetailScreen for the registry. Items list renders correctly (Account B is now a member; rules permit read).
result: PASS

#### A8. Firestore state post-accept
expected: `registries/{id}` shows `pendingInvitedUsers[B-uid]` GONE AND `invitedUsers[B-uid] = true`.
result: PASS

#### A9. Owner inbox receives invite_accepted
expected: Account A's inbox shows new notification "Account B accepted your invite to {registry}". Icon = CheckCircle, MonoCaps timestamp present, accent dot if unread.
result: PASS

#### A10. Invitee inbox shows JOINED confirmation
expected: Account B's inbox — original INVITE card is gone; new card "You joined {registry}" is present. Tapping navigates to the registry.
result: PASS

### B. Decline flow (D-03, D-22)

#### B1. Send another invite as owner
expected: Same as A1 — re-invite the already-removed (declined) account, or invite for testing decline.
result: [pending]

#### B2. Tap Decline opens confirmation
expected: Account B opens inbox → tap INVITE → sheet opens → tap Decline → AlertDialog: "Decline invite to {registry}?" with [Cancel / Decline] buttons.
result: [pending]

#### B3. Cancel keeps sheet in Idle
expected: Tap Cancel → dialog dismisses; sheet stays open with Accept + Decline both enabled.
result: [pending]

#### B4. Confirm Decline dismisses sheet
expected: Tap Decline again → dialog → tap Decline (destructive) → spinner on Decline button → sheet dismisses; inbox INVITE card is gone.
result: [pending]

#### B5. Owner sees invite_declined
expected: Account A's inbox — new card "Account B declined your invite to {registry}". Icon = Block.
result: [pending]

#### B6. Firestore state post-decline
expected: `registries/{id}` shows `pendingInvitedUsers[B-uid]` GONE AND `invitedUsers[B-uid]` unchanged (no promote).
result: [pending]

### C. Legacy + edge cases (D-11, D-16)

#### C1. Legacy invite fallback
expected: Pre-Phase-16 inbox doc (any existing INVITE notification without `pendingEntryKey`, OR manually crafted in Firestore Console with `type="invite"` and no `pendingEntryKey`). Tap that card → sheet does NOT open; app navigates directly to RegistryDetail per D-11 fallback.
result: [pending]

#### C2. Re-invite of already-member
expected: Account A invites B who is now already in `invitedUsers` (from A8). Confirm Firestore `registries/{id}.pendingInvitedUsers` does NOT acquire a B-uid entry (D-16). Account B receives FCM push + inbox card; card has no `pendingEntryKey`. Tapping navigates to RegistryDetail (no sheet).
result: [pending]

### D. Visual + locale (D-09, D-28)

#### D1. Inbox visual contract
expected: GiftMaisonWordmark in TopAppBar, gm.paper background, 1dp gm.line dividers between cards (no Card elevation), MonoCaps timestamp like "5M AGO", 6dp accent dot for unread cards. Capture screenshot.
result: [pending]

#### D2. Romanian locale parity
expected: Switch device language to Romanian via Settings screen. Re-trigger an invite. Verify sheet title is "Account A te-a invitat la", Accept = "Acceptă", Decline = "Refuză", confirmation dialog = "Refuzi invitația la „{registry}"?", owner notifications display proper Romanian translations.
result: [pending]

## Summary

total: 20
passed: 0
issues: 0
pending: 20
skipped: 0
blocked: 0

## Gaps

(none yet)
