# Phase 16: Android Notifications Inbox + Invite Accept/Decline - Context

**Gathered:** 2026-05-24
**Status:** Ready for planning

<domain>
## Phase Boundary

Move the invite flow from auto-add-to-`invitedUsers` to a **strict accept-gate model**. New invites land in a parallel `registries.pendingInvitedUsers` field; the invited Android user sees an actionable INVITE entry in the notifications inbox; tapping it opens a GiftMaison-styled bottom sheet with the registry hero + Accept/Decline CTAs. Accept atomically promotes the uid into `invitedUsers` (granting read access via the existing `isInvited()` rule); Decline silently removes the pending entry. The owner sees both outcomes in their own inbox.

In addition, the existing notifications inbox screen (currently stock Material3 from a post-Phase-12 quick task) is re-skinned to the GiftMaison design language as part of this phase.

**Phase 15 coupling (committed in STATE.md 2026-05-24):** When the parked Phase 15 resumes, its `linkInviteOnSignup` blocking function MUST target `pendingInvitedUsers` instead of `invitedUsers` — newly-signed-up invitees still pass through accept-gate, signup does not imply acceptance.

**Out of scope (deferred / parked):**
- Web parity (web invite-landing modal from Phase 15 acceptance UI) — Phase 15 resume coordinates this; Android-only accept/decline in this phase.
- Owner-side FCM push on accept/decline — inbox-only entries are written; no push to the owner (push would pester).
- Owner-side "pending invites" list UI — no surface to view/cancel pending invites from the owner side in v1; owner relies on invitee acting or owner re-inviting.
- Bulk accept/decline of multiple pending invites at once.
- Pending-invite expiry timer — invites stay pending indefinitely until acted on or owner removes.
- Migration of existing `invitedUsers` entries to pending — grandfathered (D-12).
- Web fallback push notifications — still deferred per Phase 06.

</domain>

<decisions>
## Implementation Decisions

### Accept/Decline UI placement

- **D-01:** Tap on an actionable (pending) INVITE notification card opens a dedicated **Material3 `ModalBottomSheet`** — `InviteResponseSheet` (or planner-chosen name) — with a registry-hero header (cover photo via existing `HeroImageOrPlaceholder` composable + occasion glyph + registry name + owner name + optional event date) and two CTAs: primary Accept (gm.accent Btn), secondary Decline (gm.line outlined Btn).
- **D-02:** The pending-invite inbox card itself shows the existing payload-driven layout (Mail icon + title + body), no extra "PENDING" badge or cover preview. Visual richness lives in the sheet, not the card. Already-actioned and legacy (non-pending) INVITE cards remain visually homogeneous with other notification types.
- **D-03:** Decline requires confirmation. Tap Decline → `AlertDialog` with title "Decline invite to {registryName}?" and `[Cancel / Decline]` buttons. Cancel returns to the sheet; Decline fires the callable.
- **D-04:** On Accept success, the original INVITE inbox entry is deleted server-side and replaced by a "You joined {registryName}" confirmation notification (new `NotificationType.INVITE_ACCEPTED_SELF` or `JOINED` — planner picks final name; see Claude's Discretion). On Decline success, the INVITE entry is deleted and no replacement is written for the invitee.

### Post-action UX

- **D-05:** On Accept success the bottom sheet dismisses and the app auto-navigates to `RegistryDetailKey(registryId)`. The confirmation "You joined" notification surfaces the next time the user opens the inbox.
- **D-06:** Owner gets an inbox-only entry on BOTH accept and decline (no FCM push for either to avoid pestering). Two new wire-level `NotificationType` values: `invite_accepted` (owner) and `invite_declined` (owner). Reads "{actorName} accepted your invite to {registryName}" / "{actorName} declined your invite to {registryName}" — final copy locked during execution.
- **D-07:** Accept/Decline callable failures (network, expired pending entry, registry deleted) surface as an **inline warn-banner inside the sheet** (gm.warn at 0.15 alpha — mirrors AuthScreen pattern from Phase 10) with a Retry button. Sheet stays open. Dismissing the sheet via swipe/Esc/overlay-tap is always allowed even mid-error.
- **D-08:** No separate pending-invites badge. Pending INVITE notifications contribute to the existing unread bell count via the same `observeUnreadCount` flow.

### Inbox visual redesign

- **D-09:** Full re-skin of `NotificationsScreen` + `NotificationCard` to the GiftMaison design language in this phase: wordmark `TopAppBar`, `gm.paper` background, `gm.line` dividers between cards (drop Card elevation), MonoCaption for time/relative-date metadata, accent dot for unread (replaces M3 elevation-based unread distinction), `Btn` atoms for the inline CTAs on pending invite cards. NOTE: this expands scope vs PROJECT.md "Out of Scope" listing for v1.1; user explicitly opted in during discuss-phase.
- **D-10:** Invite notification payload extended server-side. `inviteToRegistry` reads the registry doc (Admin SDK bypasses rules) and embeds these extra fields into the INVITE notification payload + the FCM data payload: `occasion`, `coverUrl` (nullable), `eventDateMs` (nullable), `pendingEntryKey` (the key written into `pendingInvitedUsers` — uid or `email:xxx`, used by the client to call back). The Android sheet renders the existing `HeroImageOrPlaceholder` composable from these payload fields with zero registry-doc read on the client (which would be denied anyway pre-accept).
- **D-11:** Legacy INVITE notifications already in user inboxes (sent before Phase 16) have NO `pendingEntryKey` field in their payload. The new sheet code branches on its presence — missing → fall back to current "tap card → navigate to registry" behavior. Zero migration; legacy entries gracefully degrade. The branching logic lives in `NotificationCard` (sheet open vs navigate dispatch).

### Migration & data model

- **D-12:** No migration of existing `invitedUsers` entries. Existing invitees keep their access; the pending-then-accept model applies only to invites sent AFTER Phase 16 ships. Both `invitedUsers` (legacy + accepted) and `pendingInvitedUsers` (new) coexist on every registry doc going forward.
- **D-13:** All NEW invites land in `pendingInvitedUsers` regardless of invitee account status. Symmetric model:
  - Existing signed-in user → `pendingInvitedUsers[uid] = true`
  - Not-yet-registered email → `pendingInvitedUsers['email:{email}'] = true`
  Both types of invitee still tap Accept on Android — explicit consent for joining any registry, regardless of pre-existing trust relationship.
- **D-14:** Phase 15 coupling contract: when Phase 15's `linkInviteOnSignup` blocking function eventually runs at signup time, it swaps `pendingInvitedUsers['email:xxx']` → `pendingInvitedUsers[newUid]`. It does NOT auto-promote into `invitedUsers`. The newly-signed-up user receives the INVITE inbox notification on their first authenticated app session and accepts/declines normally. Phase 16 writes this contract into STATE.md (already done) and Phase 15's CONTEXT.md addendum; the actual Phase 15 code change happens when Phase 15 resumes.

### Re-invite & idempotency

- **D-15:** Re-invite is always allowed. Owner re-inviting the same email is idempotent on pending entries (overwrite no-op on `pendingInvitedUsers[key] = true`). After a Decline, the pending entry is gone — re-invite re-creates it. Decline does NOT blacklist; no "blocked invitees" list in v1.
- **D-16:** When `inviteToRegistry` is called for someone already in `invitedUsers` (already-accepted member), it should no-op the membership write (don't touch `invitedUsers`, don't write to `pendingInvitedUsers`) but still write an inbox notification and FCM push so the existing member sees the re-invite as a soft re-invitation/reminder. Planner can refine.

### Web side (out of scope confirmation)

- **D-17:** Web fallback gets no UI changes in this phase. The web invite-landing modal shipped (or to be shipped) by Phase 15 already handles the magic-link sign-in / create-account flow. After web sign-in, the user's UID lands in `pendingInvitedUsers` (per D-14); their Android device picks up the INVITE inbox notification and they accept there. If the user is web-only (no Android install), they CANNOT accept the invite — they'll see the private registry as 404 until they install Android or owner removes them from pending. This limitation is acceptable for v1 (giver primary device assumed mobile); web parity for accept/decline is a future-phase concern.

### Firestore security rules

- **D-18:** Add `pendingInvitedUsers` map field to the registry doc; allowed to be read only by `isOwner(registryData)` (owner-only visibility). The invitee's pending status is exposed to them ONLY via their server-written inbox notification (already owner-isolated via existing `users/{uid}/notifications` rule). Client never writes to `pendingInvitedUsers` directly; all writes go through the new callables (Admin SDK bypasses rules).
- **D-19:** Existing `isInvited()` rule is unchanged — it still checks `invitedUsers[uid] == true`. Acceptance via the new callable promotes uid into `invitedUsers`, after which the user can read the registry through the existing path. No rules-engine surgery beyond adding the new field's read scope.

### Backend — Cloud Functions

- **D-20:** Two new 2nd-gen `onCall` Cloud Functions on `europe-west3` with `enforceAppCheck: true` — `acceptInvite(registryId)` and `declineInvite(registryId)`. Planner may choose to consolidate into a single `respondToInvite(registryId, action)` callable; either pattern is acceptable provided the transaction semantics below hold.
- **D-21:** `acceptInvite` runs a single Firestore transaction: (1) verify `pendingInvitedUsers[request.auth.uid] == true`, (2) remove that key from `pendingInvitedUsers`, (3) set `invitedUsers[request.auth.uid] = true`, (4) delete the INVITE inbox notification doc(s) for this user and registryId, (5) write the "You joined" confirmation notification for the invitee, (6) write the "invite_accepted" notification for the owner. All-or-nothing.
- **D-22:** `declineInvite` runs a single transaction: (1) verify `pendingInvitedUsers[request.auth.uid] == true`, (2) remove that key from `pendingInvitedUsers`, (3) delete the INVITE inbox notification doc(s) for this user and registryId, (4) write the "invite_declined" notification for the owner. All-or-nothing.
- **D-23:** `inviteToRegistry` is modified: writes go to `pendingInvitedUsers` instead of `invitedUsers`; the INVITE inbox notification + FCM push include the enriched payload from D-10 (`pendingEntryKey`, `occasion`, `coverUrl`, `eventDateMs`). The owner-existing-user push notification continues to fire (so the invitee notices the invite arriving) — the push tap deep-links to the inbox; the sheet open from the inbox is the actionable surface.
- **D-24:** All three functions are idempotent — re-invoking accept/decline after the transition completed is a no-op (verify-first guard returns success without writes).

### Domain & data layer (Android)

- **D-25:** `NotificationType` enum grows three new wire-mappable values: `INVITE_ACCEPTED_SELF` (or `JOINED` — the invitee's confirmation), `INVITE_ACCEPTED` (the owner's view), `INVITE_DECLINED` (the owner's view). Final names locked during execution; `fromWire("...") = ...` mappings extended.
- **D-26:** `Notification` domain model adds optional payload keys readable in the existing `payload: Map<String, String?>` shape: `pendingEntryKey`, `occasion`, `coverUrl`, `eventDateMs` (as String). No domain model breaking change beyond enum entries.
- **D-27:** `NotificationRepository` grows two new methods: `acceptInvite(registryId): Result<Unit>` and `declineInvite(registryId): Result<Unit>` — thin `httpsCallable` wrappers mirroring the `ReservationRepositoryImpl` pattern. Implemented as `runCatching` around the callable.

### Localization

- **D-28:** New strings under namespaces `notification_*` (extend) + `invite_sheet_*` (new) in both `app/src/main/res/values/strings.xml` and `app/src/main/res/values-ro/strings.xml`. Estimated 15-20 new keys × 2 locales. Email template strings in `functions/src/email/templates/invite.ts` may be lightly extended to mention "tap Accept in the app" — final copy decided during execution.

### Claude's Discretion

- Exact composable names (e.g., `InviteResponseSheet` vs `InviteAcceptDeclineSheet`).
- Final callable shape — two callables (`acceptInvite` / `declineInvite`) vs one (`respondToInvite({ action })`).
- Whether the inbox re-skin (D-09) ships before or after the accept/decline plumbing within the phase (planner orders waves; a Wave 0 RED test suite for the new types + a Wave 1 backend + a Wave 2 Android UI + a Wave 3 re-skin is one viable ordering).
- Whether the JOINED/confirmation notification has a separate `NotificationType` or just rides on a generic `payload.eventKind = "joined"` discriminator — planner picks based on which keeps `NotificationsScreen.localizedTitle/Body` cleanest.
- Test layout: extend Phase 06's existing notification-rule tests + add new ones for `pendingInvitedUsers` read/write rules; Kotlin unit tests for the new ViewModel state machine; Jest tests for the two new callables mirroring `confirmPurchase` patterns.
- Whether to write the contract addendum to Phase 15's CONTEXT.md (D-14) as part of Phase 16 execution or just leave the STATE.md note (which is already committed) as the source of truth.
- FCM push payload changes — the existing `invitePush.ts` sends `notification + data` with only `{ type, registryId }`. Should the data payload also carry the enriched fields (`pendingEntryKey`, `occasion`, `coverUrl`, `eventDateMs`)? Doing so lets a deep-link from the system notification tap open the sheet directly without first hitting the inbox. Planner decides — both are acceptable.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase 16 design contracts & coupling
- `.planning/phases/15-web-invite-landing-magic-link-guest-flow/15-CONTEXT.md` — Phase 15's `linkInviteOnSignup` blocking function and email-link guest flow; Phase 16's D-14 commits Phase 15 to target `pendingInvitedUsers`.
- `.planning/phases/06-notifications-email-flows/06-CONTEXT.md` — Notification infrastructure baseline (D-04 trigger pattern, D-09 `users/{uid}/fcmTokens`, D-13 server-side opt-out, D-15 `notifications_failures`, D-20 namespaced i18n).
- `.planning/STATE.md` — "Phase 16 added 2026-05-24" entry locks the accept-gate commitment and the Phase 15 coupling note.

### Existing Android inbox (re-skin & extend targets)
- `app/src/main/java/com/giftregistry/ui/notifications/NotificationsScreen.kt` — Current stock-M3 inbox; full GiftMaison re-skin target (D-09). Existing batched-mark-as-read LaunchedEffect and per-type icon mapping are preserved.
- `app/src/main/java/com/giftregistry/ui/notifications/NotificationsViewModel.kt` — `UiState.Loaded(List<Notification>)` flow; gains `acceptInvite` / `declineInvite` action dispatchers.
- `app/src/main/java/com/giftregistry/ui/notifications/NotificationsInboxBell.kt` — Unread badge; no behavioral change, but the icon/badge presentation may be re-skinned as part of D-09.
- `app/src/main/java/com/giftregistry/ui/notifications/InboxBellViewModel.kt` — Unread count flow; reused as-is (D-08 — pending invites count via existing flow).
- `app/src/main/java/com/giftregistry/ui/notifications/NotificationBus.kt` — In-app push toast bridge; no change.

### Existing Android invite path (do not modify on owner side — Phase 16 is invitee-side)
- `app/src/main/java/com/giftregistry/ui/registry/invite/InviteBottomSheet.kt` — Owner-side compose-email-and-send sheet; reference for visual conventions only.
- `app/src/main/java/com/giftregistry/ui/registry/invite/InviteViewModel.kt` — Owner-side send VM; no change.
- `app/src/main/java/com/giftregistry/domain/usecase/InviteToRegistryUseCase.kt` — Wraps the callable; no signature change required (the callable handles the pending swap internally).

### Domain model + data layer
- `app/src/main/java/com/giftregistry/domain/model/Notification.kt` — Add new `NotificationType` enum values (D-25); extend payload reading conventions (D-26).
- `app/src/main/java/com/giftregistry/domain/notifications/NotificationRepository.kt` — Grow `acceptInvite` + `declineInvite` suspend methods (D-27).
- `app/src/main/java/com/giftregistry/data/notifications/NotificationRepositoryImpl.kt` — `httpsCallable` wrappers for the new callables; mirror `ReservationRepositoryImpl` patterns.
- `app/src/main/java/com/giftregistry/data/notifications/NotificationDto.kt` — Read the new payload fields; coerce to `Map<String, String?>` via existing flatten path.

### Cloud Functions backend
- `functions/src/registry/inviteToRegistry.ts` — Modify writes to `pendingInvitedUsers`; include enriched payload (D-23). The owner-name/registry-name/locale lookup pattern is unchanged.
- `functions/src/notifications/writeNotification.ts` — Reused as-is for all four new writes (joined-confirmation, invite_accepted owner, invite_declined owner, and any re-invite inbox refresh).
- `functions/src/notifications/invitePush.ts` — Reused for FCM push; payload extension per D-10 / discretion item.
- `functions/src/email/templates/invite.ts` — Email body copy may need light touch-up to mention "tap Accept in the app" (D-28 final copy phase).
- `functions/src/config/publicUrls.ts` — `buildRegistryUrl()` unchanged for Phase 16. Phase 15's `invite=1` query param work is independent.
- `functions/src/__tests__/inviteToRegistry.test.ts` — Update tests for `pendingInvitedUsers` write path.

### Firestore security model
- `firestore.rules` — Add explicit `pendingInvitedUsers` rule per D-18. Existing `isInvited(registryData)` function (lines 30-36) is unchanged. The new `acceptInvite` / `declineInvite` callables bypass rules via Admin SDK.
- `tests/rules/` (Phase 1 / Phase 6 harness) — Extend with `pendingInvitedUsers` read/write tests.

### Design system / GiftMaison primitives (for the re-skin)
- `design_handoff_android_owner_flow/` — Original handoff; Phase 16 follows the same primitives (Instrument Serif, accent terracotta, gm.paper bg, MonoCaption metadata).
- `app/src/main/java/com/giftregistry/ui/common/` — `GiftMaisonWordmark`, `Btn` equivalents, `MonoCaption`, `Pill`, etc. (Phase 08/09/10/11 primitives; re-use, don't re-build).
- `app/src/main/java/com/giftregistry/ui/registry/detail/HeroImageOrPlaceholder.kt` — Phase 12 component; reused inside the invite-response sheet for the registry hero (D-01).

### Navigation
- `app/src/main/java/com/giftregistry/ui/navigation/AppNavigation.kt` — `NotificationsKey` entry (line ~343); no new key needed (sheet is hosted inside `NotificationsScreen`). RegistryDetailKey auto-navigation on Accept reuses the existing entry.
- `app/src/main/java/com/giftregistry/ui/navigation/AppNavKeys.kt` — No new keys.
- `app/src/main/java/com/giftregistry/ui/common/chrome/NavVisibility.kt` — No change; `NotificationsKey` already in the hidden-whitelist? Verify (quick task `260522-v0q` set the hidden-whitelist; `NotificationsKey` may need to be added if not present).

### Project conventions
- `CLAUDE.md` (repo root) — Tech stack constraints; Kotlin + Compose, Firebase BoM, no KTX modules, Cloud Functions 2nd gen on `europe-west3`.
- `.planning/PROJECT.md` — v1.1 Out of Scope explicitly lists "notifications inbox redesign" — Phase 16 pulls it back in (D-09 user-confirmed expansion).

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `NotificationsScreen` + `NotificationCard` + `localizedTitle/Body` extension fns — extended in-place for the new types and re-skinned.
- `HeroImageOrPlaceholder` (Phase 12) — drop-in for the registry hero in the invite-response sheet.
- `writeNotification` (functions/src/notifications/) — server-side helper for ALL four new writes (joined, invite_accepted, invite_declined, re-invite refresh).
- `sendInvitePush` (functions/src/notifications/) — FCM push; payload extension only.
- `httpsCallable` pattern (e.g. `useConfirmPurchase` web / `ReservationRepositoryImpl` Android) — direct template for the two new callables.
- `AlertDialog` + decline-confirmation pattern — vanilla M3; no new component needed.
- Existing inline warn-banner from AuthScreen (Phase 10 D-02) — re-used for sheet error state.
- `notifications_failures` collection — re-used for best-effort logging when accept/decline transactions partially succeed (shouldn't happen given atomicity, but the path exists).

### Established Patterns
- Server writes notification docs; clients only update `readAt` (Phase 06 rule pattern) — extended: the accept/decline callables also DELETE notification docs (the rule allows delete: if false from clients; Admin SDK bypasses).
- Best-effort writes — never throw on inbox-write failure (Phase 06 D-06).
- Idempotency guards via verify-then-write transactions (Phase 04 reservation pattern) — re-applied to accept/decline transactions (D-24).
- `enforceAppCheck: true` on every callable (Phase 14 lesson) — new callables follow.
- Feature-namespaced i18n keys (Phase 01 D-01) — `notification_*` extended, `invite_sheet_*` new.
- Zero Firebase imports in domain layer (Phase 02 D-08) — `NotificationRepository` stays pure.
- `runCatching` wraps Firebase suspend calls (Phase 03 pattern) — `acceptInvite`/`declineInvite` impl follows.
- callbackFlow + awaitClose for Firebase observers — no new flow needed (existing `observe` covers).
- Nullable-callback pattern for owner-only affordances (Phase quick-260507) — not directly applicable (accept/decline is invitee-only, gated by payload field presence not by isOwner).

### Integration Points
- **Firestore schema additions:** `registries/{id}.pendingInvitedUsers: Map<String, Boolean>` (parallel to `invitedUsers`).
- **Firestore security rules:** Add field rule per D-18.
- **Cloud Functions new files:** `functions/src/registry/acceptInvite.ts`, `functions/src/registry/declineInvite.ts` (or combined `respondToInvite.ts` per discretion). Both exported from `functions/src/index.ts`.
- **Android new files:** `app/src/main/java/com/giftregistry/ui/notifications/InviteResponseSheet.kt` (or planner-chosen name); the new `acceptInvite`/`declineInvite` methods on `NotificationRepositoryImpl`.
- **Android modified files:** `NotificationsScreen.kt` (branch on `pendingEntryKey` for sheet open), `NotificationsViewModel.kt` (action dispatchers), `Notification.kt` enum, `NotificationDto.kt` (no change — already flattens payload), `strings.xml` × 2 locales.
- **Cloud Functions modified files:** `inviteToRegistry.ts` (writes pending + enriched payload), `invitePush.ts` (enriched data payload, optional).

</code_context>

<specifics>
## Specific Ideas

- **Acceptance copy (en):** "Accept" / "Decline" CTAs; sheet title "{ownerName} invited you to" with the registry name below in italic-accent Display style (mirrors the GiftMaison auth-screen italic-accent headline pattern).
- **Acceptance copy (ro):** "Acceptă" / "Refuză"; titlu "{ownerName} te-a invitat la".
- **Joined-confirmation:** "You joined {registryName}" / "Te-ai alăturat la {registryName}". Inbox entry only; no toast/snackbar (auto-nav to registry is the immediate feedback).
- **Owner accept-notification:** "{actorName} accepted your invite to {registryName}" / "{actorName} a acceptat invitația la {registryName}".
- **Owner decline-notification:** "{actorName} declined your invite to {registryName}" / "{actorName} a refuzat invitația la {registryName}".
- **Decline confirmation dialog:** "Decline invite to {registryName}?" / "Refuzi invitația la {registryName}?". Buttons "Cancel" / "Decline".
- **Sheet error banner:** "Couldn't accept invite. Please try again." / "Nu am putut accepta invitația. Încearcă din nou." + Retry CTA.
- **Notification type wire values:** `invite_accepted_self` (or `joined`) for invitee-confirmation; `invite_accepted` and `invite_declined` for owner-side. Reserve clear names so future v2 fanout (e.g. group invites) can extend without rename.
- **`NotificationType.fromWire` fallback:** Any new server-only types still map to `UNKNOWN` on older client builds — Phase 06 forward-compat property preserved.
- **Phase 15 coupling addendum:** Append a "Phase 16 update" stanza to `.planning/phases/15-web-invite-landing-magic-link-guest-flow/15-CONTEXT.md` `<decisions>` block (or `<deferred>`) noting that `linkInviteOnSignup` must target `pendingInvitedUsers`. The STATE.md note is authoritative; this is a convenience-pointer for the Phase 15 resume team.

</specifics>

<deferred>
## Deferred Ideas

- **Web parity for accept/decline UI.** Phase 15 resume + a possible Phase 17 cover this; web-only invitees who never install Android effectively can't accept in this phase (acceptable v1 limitation per D-17).
- **Owner FCM push on accept/decline.** Inbox-only suffices for v1 (D-06).
- **Owner-side "manage pending invites" UI.** No surface to view/cancel pending invites from owner side; owner relies on invitee acting or re-inviting.
- **Pending-invite expiry timer.** Invites stay pending indefinitely; no auto-cleanup. v2 polish.
- **Bulk accept/decline of multiple invites.** No multi-select UI in the inbox; one card, one action at a time.
- **Migration of legacy `invitedUsers` entries to pending.** Grandfathered per D-12.
- **Owner-side decline blacklist / re-invite throttling.** Decline does not blacklist (D-15); spam control is owner discretion.
- **Registry preview endpoint for pending invitees.** D-10's payload-embed approach removes the need for a separate callable in v1.
- **Custom magic-link email template / invite email redesign.** Not in scope; minor copy tweak only (D-28).
- **Reviewed-not-folded todos** (matched by `gsd-tools todo match-phase 16`):
  - `2026-04-27-curate-real-preset-jpegs-for-phase-12.md` — Phase 12 cleanup, unrelated.
  - `2026-05-21-invite-email-cta-link-stripped-in-gmail-mobile.md` — Email deliverability (SPF/DKIM) — separate concern; Phase 16's invite-email copy touch is in-scope but the deliverability fix is not.
  - `2026-05-22-uat-pass-2-recruited-giver-web-fallback-items-2-3.md` — Phase 14 UAT follow-up, unrelated.
  - `2026-05-22-wire-android-app-check-and-flip-enforcement.md` — Phase 14 follow-up; Phase 16 new callables already use `enforceAppCheck: true` so this todo is orthogonal.
  - `2026-04-20-group-registries-by-ownership-and-clarify-invitee-permissions.md` — UI grouping concern (owner-list vs invitee-list separation on Home); related to invitee permissions but a separate Home-screen UX problem, not Phase 16 inbox/invite scope.

</deferred>

---

*Phase: 16-android-notifications-inbox-invite-accept-decline*
*Context gathered: 2026-05-24 via /gsd:discuss-phase*
