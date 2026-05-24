# Phase 16: Android Notifications Inbox + Invite Accept/Decline - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-05-24
**Phase:** 16-android-notifications-inbox-invite-accept-decline
**Areas discussed:** Accept/decline UI placement, Post-accept / post-decline UX, Inbox visual redesign scope, Existing invitedUsers migration
**Areas deferred to defaults (not discussed):** Web parity for Phase 15 acceptance UI; Re-invite policy after decline

---

## Accept/decline UI placement

### Q1: Where should the Accept / Decline buttons live for an INVITE notification in the Android inbox?

| Option | Description | Selected |
|--------|-------------|----------|
| Inline on the card (Recommended) | Two buttons rendered directly below the notification body on the inbox card. Tap card body → still opens registry (only if already-accepted; pending invite card has no body tap, just the two CTAs). Matches the existing one-screen feel. | |
| Dedicated invite-detail bottom sheet | Card tap opens a Material3 ModalBottomSheet with registry hero (cover/glyph + occasion + owner name + invited-on date) and large Accept / Decline CTAs. Mirrors the existing InviteBottomSheet on the owner side. | ✓ |
| Separate 'Pending invites' tab in inbox | Inbox grows a 2-tab segmented control (All notifications / Pending invites). Pending tab shows each invite as a rich card with Accept/Decline CTAs. Decouples actionable pending invites from passive notification history. | |

**User's choice:** Dedicated invite-detail bottom sheet
**Notes:** User opted for the richer treatment over the inline-card recommendation; sheet gets full GiftMaison hero rendering. Implication: invite notification payload must be enriched (see Area 3 / D-10) because the sheet can't read the registry doc pre-accept.

---

### Q2: How should the pending-invite card differ visually from other (already-accepted) notification cards?

| Option | Description | Selected |
|--------|-------------|----------|
| Show payload context only (Recommended) | Card shows owner name, registry name, optional occasion glyph next to the Mail icon, plus the two CTA buttons. No special highlighting beyond the unread-state styling that already exists. Keeps the card visually homogeneous with other notification types. | ✓ |
| Distinct 'pending action' treatment | Pending-invite cards get an accentSoft background pill + 'PENDING' MonoCaption label to signal 'this needs your action' more loudly than read/unread state. Visually separates from already-tapped notifications even after marked read. | |
| Cover-photo preview on the card | Render a 16:9 hero strip (cover photo or GiftMaison gradient + occasion glyph) above the CTAs so the invitee sees what they're being invited to. Heaviest visual treatment; closest to 'invitation card' metaphor. | |

**User's choice:** Show payload context only (Recommended)
**Notes:** Visual richness lives in the bottom sheet (chosen in Q1), not the card. Card stays homogeneous with other notification types.

---

### Q3: How should the Decline button confirm the action before doing the destructive thing?

| Option | Description | Selected |
|--------|-------------|----------|
| No confirmation — immediate decline + undo snackbar (Recommended) | Tap Decline → instantly removes the pending entry + dismisses the inbox card; a snackbar with 'Undo' (~5s) restores it if mis-tapped. Lowest friction. Matches Gmail/Material3 pattern. | |
| Confirmation dialog before decline | Tap Decline → AlertDialog: 'Decline invite to {registryName}?' [Cancel / Decline]. Slower but explicit. Use if you want to make 'decline' feel like a deliberate decision. | ✓ |
| No confirmation, no undo — just decline | Tap Decline → entry gone, no second chance. Simplest. Mistaken-tap recovery = owner needs to re-invite. | |

**User's choice:** Confirmation dialog before decline
**Notes:** User wants decline to feel deliberate. Removes the need to implement undo machinery on the snackbar.

---

### Q4: After accept/decline, what happens to the inbox INVITE notification itself?

| Option | Description | Selected |
|--------|-------------|----------|
| Remove from inbox + write a small confirmation notification (Recommended) | Accept → INVITE entry deleted, new 'You joined {registryName}' notification appended. Decline → INVITE entry deleted, no confirmation entry written. Server-side write via the same Admin SDK helper. | ✓ |
| Keep INVITE entry, mutate state inline | INVITE entry stays, but its CTAs are replaced with text 'Accepted on {date}' or 'Declined on {date}'. Preserves the full audit trail in the inbox. | |
| Remove from inbox entirely — no confirmation entry | Both Accept and Decline silently remove the entry. No trace remains in the inbox. Owner-side accept/decline notification would be the only record. | |

**User's choice:** Remove from inbox + write a small confirmation notification (Recommended)

---

## Post-accept / post-decline UX

### Q1: What happens immediately after the user taps Accept (success case)?

| Option | Description | Selected |
|--------|-------------|----------|
| Sheet dismisses → auto-navigate to RegistryDetail (Recommended) | Sheet closes; backStack.add(RegistryDetailKey(registryId)). User lands directly on the registry they just joined. Maximum 'I'm in' clarity. The 'You joined' confirmation notification appears in inbox the next time they open it. | ✓ |
| Sheet dismisses → stay in inbox + success snackbar | Sheet closes; SnackbarHost shows 'You joined {registryName}' with an 'Open' action button that navigates to the registry. User stays in inbox to triage other notifications. | |
| Sheet stays open → success state with 'Open registry' CTA | Sheet transitions to a confirmation state: '✓ You're in {registryName}' + 'Open registry' button + 'Done' button. Two-tap to navigate but explicit confirmation. | |

**User's choice:** Sheet dismisses → auto-navigate to RegistryDetail (Recommended)

---

### Q2: Does the registry OWNER get any notification when an invitee accepts or declines?

| Option | Description | Selected |
|--------|-------------|----------|
| No owner notifications in v1 (Recommended) | Out of scope. Owner sees the invitee appear in invitedUsers when they next open the registry. Keeps Phase 16 surface area tight; can layer in v1.2 with a 'invite_accepted' notification type if requested. | |
| Owner gets inbox + push on accept only (not decline) | Server writes an 'invite_accepted' inbox entry + FCM push to the owner when invitee accepts. Decline stays silent (don't make owner feel rejected; also encourages decline-without-friction). Adds a new NotificationType.INVITE_ACCEPTED + push template. | |
| Owner gets inbox entry on BOTH accept and decline | Full audit — owner sees who accepted and who declined. Inbox only (no push for decline to avoid pestering). Two new notification types. | ✓ |

**User's choice:** Owner gets inbox entry on BOTH accept and decline
**Notes:** User chose maximum visibility for the owner. Locked as inbox-only (no FCM push) per the option text; two new wire-level NotificationType values (`invite_accepted`, `invite_declined`) added.

---

### Q3: If the Accept / Decline callable fails (network error, expired pending entry, registry deleted), how should the bottom sheet handle it?

| Option | Description | Selected |
|--------|-------------|----------|
| Inline error banner inside the sheet + retry button (Recommended) | Sheet stays open; shows 'Couldn't accept invite. Try again.' inline above the CTAs with a Retry button. User can retry or dismiss. Matches the pattern in AuthScreen's inline warn-banner. | ✓ |
| Snackbar error + sheet dismisses | Sheet closes on failure too; snackbar shows error. User has to re-open the inbox card to retry. Looser feedback loop but matches Decline's snackbar pattern. | |
| AlertDialog blocking error | Modal AlertDialog 'Couldn't accept invite' [Retry / Cancel]. Heaviest. Use if you want errors to be unmissable. | |

**User's choice:** Inline error banner inside the sheet + retry button (Recommended)

---

### Q4: Should there be a 'pending invites count' badge anywhere distinct from the general unread bell count?

| Option | Description | Selected |
|--------|-------------|----------|
| No — just the existing unread bell count (Recommended) | Pending invites count as unread notifications and contribute to the bell badge like everything else. Keeps the chrome simple; the inbox itself surfaces 'this needs action' via the CTAs. | ✓ |
| Separate pending-invites badge | Add a second indicator (small accent dot on the bell or a separate Home banner 'You have {N} pending invites') that only counts pending invites. More attention to actionable items but more UI surface to maintain. | |

**User's choice:** No — just the existing unread bell count (Recommended)

---

## Inbox visual redesign scope

### Q1: How much of the existing Notifications inbox screen should this phase re-skin to GiftMaison?

| Option | Description | Selected |
|--------|-------------|----------|
| Strict scope — no re-skin (Recommended) | Add the new invite-detail bottom sheet (which gets full GiftMaison treatment as new code), add the new Accept/Decline backend, but leave NotificationsScreen + NotificationCard in their current Material3 default styling. PROJECT.md lists 'notifications inbox redesign' as v1.1 Out of Scope. Cleanest scope boundary. | |
| Minimum-viable polish | Re-skin only the parts that touch invite cards (Mail icon → accent stroke, accept/decline buttons → Btn atoms, MonoCaption for 'INVITE'/'JOINED'/'DECLINED' labels) but leave the screen scaffold (Scaffold + TopAppBar + Card defaults) as-is. Half-step — invite cards look right, others still stock M3. | |
| Full inbox re-skin in this phase | NotificationsScreen gets the full GiftMaison treatment: wordmark top bar, Display headline, all NotificationCards refactored to GiftMaison primitives (gm.paper background, gm.line dividers, MonoCaption metadata, accent for unread). Expands phase scope, but ships a coherent inbox surface. | ✓ |

**User's choice:** Full inbox re-skin in this phase
**Notes:** Explicitly pulls "notifications inbox redesign" from v1.1 Out of Scope back INTO this phase. Captured in CONTEXT.md D-09 with the explicit scope-expansion note.

---

### Q2: The new invite-detail bottom sheet pulls registry name + owner name from the notification payload, but cover photo / occasion / event date are NOT in the current payload. To render a meaningful 'this is what you're invited to' sheet, the invite notification needs more fields. Approach?

| Option | Description | Selected |
|--------|-------------|----------|
| Extend invite notification payload server-side (Recommended) | On invite send, server reads the registry doc (Admin SDK — bypasses rules) and embeds occasion + coverUrl + eventDate into the notification payload. Sheet renders the existing HeroImageOrPlaceholder composable from payload. Bonus: invite EMAIL can use the same enriched data. | ✓ |
| Defer rich sheet — text-only invite-detail | Bottom sheet shows only what's already in the payload: 'Invite from {actorName}' + 'You're invited to {registryName}' + Accept/Decline. No cover, no glyph, no date. Smallest server change; sheet is essentially a styled AlertDialog. | |
| Server-side preview endpoint (callable) the sheet calls on open | Add a `getInvitePreview(registryId)` callable that returns name/cover/occasion/date for any user with a pending entry in pendingInvitedUsers (rule check inside the function). Sheet calls it on open, shows skeleton then hero. Most flexible but extra round-trip. | |

**User's choice:** Extend invite notification payload server-side (Recommended)

---

### Q3: Existing INVITE notifications already in users' inboxes today (from invites sent before Phase 16) point to registries the user is already in (auto-accepted). What happens to those cards after Phase 16 ships?

| Option | Description | Selected |
|--------|-------------|----------|
| Untouched — they keep current behavior (Recommended) | Existing INVITE notifications have no `pendingEntryKey` field in their payload — the new bottom-sheet code branches on its presence. Missing → fall back to current behavior (tap card → navigate to registry, no accept/decline shown). Zero migration risk. | ✓ |
| Backfill script flags legacy INVITE cards as accepted | One-time script writes `accepted: true` flag onto legacy INVITE notification payloads so the new sheet code can render them as 'JOINED' state instead of inviting accept/decline. More work for minor visual consistency. | |

**User's choice:** Untouched — they keep current behavior (Recommended)

---

## Existing invitedUsers migration

### Q1: Currently, every existing invite has been auto-added to `invitedUsers` (the old behavior). When Phase 16 ships, how should the existing membership be treated?

| Option | Description | Selected |
|--------|-------------|----------|
| No migration — grandfather existing invites (Recommended) | Existing `invitedUsers` entries stay exactly where they are; those users keep their access. Only NEW invites sent after Phase 16 ships flow through the pending-then-accept model. Zero migration code, zero risk of breaking existing user access. | ✓ |
| Migration script moves existing email:* entries to pendingInvitedUsers | One-time admin script: any `invitedUsers['email:xxx']` (not-yet-registered invitees) gets moved to `pendingInvitedUsers`. Real UID entries stay accepted. Rationale: existing email-keyed entries can't grant access today anyway (UID lookup fails), so converting them to pending is harmless. | |
| Migration script moves ALL existing entries (UIDs included) to pending | Aggressive: every existing invitee must re-confirm via accept. Cleanest semantic boundary but FORCES every current registry member to take a manual action. Highest user-disruption. | |

**User's choice:** No migration — grandfather existing invites (Recommended)

---

### Q2: When the owner sends an invite (via the existing `inviteToRegistry` Cloud Function) and the invitee is an EXISTING signed-in user, where does their entry land?

| Option | Description | Selected |
|--------|-------------|----------|
| Always pendingInvitedUsers regardless of account status (Recommended) | `pendingInvitedUsers[uid] = true` for existing-user invites; `pendingInvitedUsers['email:xxx'] = true` for non-user invites. Symmetric. Existing user must still tap Accept on Android — explicit consent for joining any registry. Cleanest mental model. | ✓ |
| Existing users go directly to invitedUsers (auto-accept), non-users go to pending | If the invitee already has an account, skip the pending step — the owner-existing-user trust relationship is sufficient. Non-users go to pending and accept after signup. Asymmetric but lower-friction for the common case. | |

**User's choice:** Always pendingInvitedUsers regardless of account status (Recommended)

---

### Q3: When a non-user invitee later signs up (Phase 15's `linkInviteOnSignup` blocking function), what does it do with the `email:xxx` entry in `pendingInvitedUsers`?

| Option | Description | Selected |
|--------|-------------|----------|
| Swap email:xxx → uid in pendingInvitedUsers (Recommended) | The new user is now linkable by UID, but they still haven't accepted — they get the invite notification in their Android inbox after first login and decide there. Phase 16 just docs this contract for Phase 15 to honor on resume. | ✓ |
| Swap email:xxx → uid AND auto-promote to invitedUsers (skip accept) | Signup itself counts as acceptance. They're added straight to invitedUsers; no Android accept step. Lower friction but loses the 'explicit consent' property. | |

**User's choice:** Swap email:xxx → uid in pendingInvitedUsers (Recommended)
**Notes:** Captured as D-14 in CONTEXT.md and documented in the Phase 15 coupling addendum (deferred to Phase 15 resume).

---

## Defaults locked without discussion (user opted to skip the remaining 2 areas)

### Web parity (Phase 15 invite-landing flow)
**Default applied:** Web stays unchanged in this phase. Phase 15 resume coordinates the web invitee path. Web-only invitees (no Android install) effectively cannot accept in v1; acceptable limitation. Captured as CONTEXT.md D-17.

### Re-invite policy
**Default applied:** Re-invite is always allowed. Idempotent overwrite on pending entries; decline does NOT blacklist. No throttling or owner-side blocklist in v1. Captured as CONTEXT.md D-15 + D-16.

---

## Claude's Discretion (passed to planner)

- Exact composable / function names (e.g., `InviteResponseSheet`, `acceptInvite` vs `respondToInvite({action})`).
- Whether `acceptInvite` / `declineInvite` are two callables or one consolidated `respondToInvite`.
- Wave ordering within the plan (re-skin first vs accept/decline plumbing first).
- Whether the JOINED/confirmation notification uses a separate `NotificationType` or a `payload.eventKind` discriminator.
- Test layout extension over Phase 06's existing notification-rule tests.
- Whether to push the contract addendum (D-14) into Phase 15's CONTEXT.md or rely on STATE.md alone.
- FCM push data payload — whether the enriched fields (`pendingEntryKey`, `occasion`, `coverUrl`, `eventDateMs`) ride on `data` for system-notification-tap deep-link to the sheet.

## Deferred Ideas

(All deferred items captured in CONTEXT.md `<deferred>` section.)

- Web parity for accept/decline UI
- Owner FCM push on accept/decline
- Owner-side "manage pending invites" UI
- Pending-invite expiry timer
- Bulk accept/decline of multiple invites
- Migration of legacy `invitedUsers` entries to pending (grandfathered per D-12)
- Owner-side decline blacklist / re-invite throttling
- Registry preview endpoint for pending invitees
- Custom magic-link email template / invite email full redesign
- Cross-referenced pending todos not folded:
  - 2026-04-27-curate-real-preset-jpegs-for-phase-12 (unrelated)
  - 2026-05-21-invite-email-cta-link-stripped-in-gmail-mobile (deliverability — separate concern)
  - 2026-05-22-uat-pass-2-recruited-giver-web-fallback-items-2-3 (Phase 14 follow-up)
  - 2026-05-22-wire-android-app-check-and-flip-enforcement (Phase 14 follow-up; Phase 16 new callables already use enforceAppCheck:true)
  - 2026-04-20-group-registries-by-ownership-and-clarify-invitee-permissions (Home-screen UX concern, not Phase 16 inbox/invite scope)
