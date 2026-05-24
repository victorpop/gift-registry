# Phase 16: Android Notifications Inbox + Invite Accept/Decline - Research

**Researched:** 2026-05-24
**Domain:** Android Compose UI (ModalBottomSheet + AlertDialog) + Firestore-transactional Cloud Functions (2nd gen, europe-west3) + FCM payload extension + Firestore security rule additions + GiftMaison design-system re-skin of an existing inbox
**Confidence:** HIGH

## Summary

Phase 16 is **fully scope-locked by CONTEXT.md** — 28 decisions cover every meaningful question. This research therefore focuses on (a) verifying the existing-code patterns the plan will extend, (b) catching the runtime / API pitfalls that grep-only planning would miss, and (c) prescribing the test surfaces required for Nyquist validation.

The work decomposes into five concrete deliverables: (1) new `pendingInvitedUsers` Firestore field + rule + matching Jest rules tests; (2) two new 2nd-gen `onCall` callables (`acceptInvite` / `declineInvite`) implemented as Admin-SDK Firestore transactions following the **exact** `confirmPurchase.ts` shape; (3) a modification to the existing `inviteToRegistry.ts` to write into `pendingInvitedUsers` and embed an enriched payload; (4) Android domain/data extensions (3 new `NotificationType` enum values, two new `NotificationRepository` methods mirroring `ReservationRepositoryImpl.confirmPurchase`); (5) a new `InviteResponseSheet` ModalBottomSheet + AlertDialog confirmation + full GiftMaison re-skin of `NotificationsScreen` and `NotificationCard`.

**Primary recommendation:** Use `FieldPath("pendingInvitedUsers", inviteKey)` for every Admin-SDK write/delete of the new map (mirrors the documented dot-in-email pitfall already solved in `inviteToRegistry.ts` line 92), wrap accept/decline callables in a single `db.runTransaction` that includes the notification-doc cleanup writes, and pass `confirmValueChange = { !isLoading }` to `rememberModalBottomSheetState` so swipe-dismiss during the in-flight callable is impossible while still leaving the swipe-dismiss free in idle and error states (per D-07).

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Accept/Decline UI placement**
- **D-01:** Tap on an actionable (pending) INVITE notification card opens a Material3 `ModalBottomSheet` (`InviteResponseSheet` or planner-chosen name) with registry-hero header (`HeroImageOrPlaceholder` + occasion glyph + registry name + owner name + optional event date) and two CTAs: primary Accept (gm.accent Btn), secondary Decline (gm.line outlined Btn).
- **D-02:** Pending-invite inbox card shows existing payload-driven layout (Mail icon + title + body), no extra "PENDING" badge, no cover preview. Visual richness lives in the sheet.
- **D-03:** Decline requires confirmation via `AlertDialog` titled "Decline invite to {registryName}?" with `[Cancel / Decline]`.
- **D-04:** On Accept success, server-side deletes the original INVITE inbox entry and writes a "You joined {registryName}" confirmation notification (new `NotificationType.INVITE_ACCEPTED_SELF` or `JOINED` — planner picks name). On Decline success, INVITE entry is deleted, no replacement.

**Post-action UX**
- **D-05:** Accept success → sheet dismisses → auto-navigate to `RegistryDetailKey(registryId)`. Confirmation surfaces next inbox open.
- **D-06:** Owner gets inbox-only entry on BOTH accept and decline (no FCM push). New wire types `invite_accepted` + `invite_declined`.
- **D-07:** Accept/Decline failures → inline warn-banner inside sheet (gm.warn at 0.15 alpha; mirrors AuthScreen Phase 10 pattern) with Retry button. Sheet stays open. Swipe/Esc/overlay-tap dismiss always allowed (even mid-error).
- **D-08:** No separate pending-invites badge — they contribute to existing unread bell count via `observeUnreadCount`.

**Inbox visual redesign**
- **D-09:** Full GiftMaison re-skin of `NotificationsScreen` + `NotificationCard` in this phase: wordmark `TopAppBar`, `gm.paper` background, `gm.line` dividers (drop Card elevation), `MonoCaption` time/date metadata, accent dot for unread, `Btn` atoms for inline CTAs. **Expands v1.1 scope** vs PROJECT.md — user explicitly opted in.
- **D-10:** Invite notification payload extended server-side. `inviteToRegistry` reads registry doc (Admin SDK bypasses rules) and embeds: `occasion`, `coverUrl` (nullable), `eventDateMs` (nullable), `pendingEntryKey` (uid or `email:xxx`). Android sheet renders `HeroImageOrPlaceholder` from payload — zero registry-doc read on client.
- **D-11:** Legacy INVITE notifications have NO `pendingEntryKey` field. New sheet code branches on its presence — missing → fall back to current "tap card → navigate to registry" behavior. Zero migration; branching lives in `NotificationCard`.

**Migration & data model**
- **D-12:** No migration of existing `invitedUsers` entries. Grandfathered. Both `invitedUsers` and `pendingInvitedUsers` coexist on every registry doc going forward.
- **D-13:** All NEW invites land in `pendingInvitedUsers` regardless of account status. Symmetric: existing signed-in user → `pendingInvitedUsers[uid] = true`; not-yet-registered email → `pendingInvitedUsers['email:{email}'] = true`. Both flows tap Accept on Android.
- **D-14:** Phase 15 coupling contract: `linkInviteOnSignup` swaps `pendingInvitedUsers['email:xxx']` → `pendingInvitedUsers[newUid]`. Does NOT auto-promote into `invitedUsers`. Newly-signed-up user receives INVITE inbox notification on first authenticated session.

**Re-invite & idempotency**
- **D-15:** Re-invite is always allowed; idempotent on pending entries. After Decline, pending entry is gone — re-invite re-creates it. No blacklist.
- **D-16:** When `inviteToRegistry` is called for someone already in `invitedUsers`: no-op on membership writes (don't touch `invitedUsers`, don't write to `pendingInvitedUsers`) but DO write inbox notification + FCM push so existing member sees re-invite.

**Web side (out of scope)**
- **D-17:** Web fallback gets NO UI changes. Web-only invitees (no Android install) CANNOT accept v1 — acceptable.

**Firestore security rules**
- **D-18:** Add `pendingInvitedUsers` map field to registry doc; readable ONLY by `isOwner(registryData)`. Invitee's pending status surfaced only via their server-written inbox notification. Client NEVER writes to `pendingInvitedUsers`; all writes via callables.
- **D-19:** Existing `isInvited()` rule UNCHANGED — still checks `invitedUsers[uid] == true`. Acceptance promotes uid into `invitedUsers`, after which existing path applies.

**Backend — Cloud Functions**
- **D-20:** Two new 2nd-gen `onCall` Cloud Functions on `europe-west3` with `enforceAppCheck: true` — `acceptInvite(registryId)` and `declineInvite(registryId)`. Planner MAY consolidate to single `respondToInvite(registryId, action)`.
- **D-21:** `acceptInvite` runs single Firestore transaction: (1) verify `pendingInvitedUsers[request.auth.uid] == true`, (2) remove that key, (3) set `invitedUsers[request.auth.uid] = true`, (4) delete INVITE inbox notification doc(s) for this user+registryId, (5) write "You joined" confirmation, (6) write "invite_accepted" notification for owner. All-or-nothing.
- **D-22:** `declineInvite`: (1) verify, (2) remove pending, (3) delete INVITE doc(s), (4) write "invite_declined" owner notification. All-or-nothing.
- **D-23:** `inviteToRegistry` modified: writes to `pendingInvitedUsers`; INVITE inbox + FCM push include enriched payload (`pendingEntryKey`, `occasion`, `coverUrl`, `eventDateMs`). Owner-existing-user push continues to fire; tap deep-links to inbox.
- **D-24:** All three functions idempotent — re-invoking accept/decline after transition completed is no-op.

**Domain & data layer (Android)**
- **D-25:** `NotificationType` enum grows three new wire-mappable values: `INVITE_ACCEPTED_SELF` (or `JOINED`), `INVITE_ACCEPTED`, `INVITE_DECLINED`. Extend `fromWire`.
- **D-26:** `Notification` adds optional payload keys readable in existing `payload: Map<String, String?>`: `pendingEntryKey`, `occasion`, `coverUrl`, `eventDateMs` (String). No domain breaking change.
- **D-27:** `NotificationRepository` grows `acceptInvite(registryId): Result<Unit>` and `declineInvite(registryId): Result<Unit>` — thin `httpsCallable` wrappers via `runCatching`, mirroring `ReservationRepositoryImpl`.

**Localization**
- **D-28:** New strings under `notification_*` (extend) + `invite_sheet_*` (new) in both `app/src/main/res/values/strings.xml` and `app/src/main/res/values-ro/strings.xml`. ~15-20 new keys × 2 locales. `functions/src/email/templates/invite.ts` may be lightly extended.

### Claude's Discretion

- Exact composable names (`InviteResponseSheet` vs `InviteAcceptDeclineSheet`).
- Final callable shape — two callables vs single `respondToInvite({ action })`.
- Whether inbox re-skin (D-09) ships before or after accept/decline plumbing.
- Whether JOINED uses separate `NotificationType` or rides `payload.eventKind = "joined"` discriminator.
- Test layout: extend Phase 06 rule tests + new `pendingInvitedUsers` tests; Kotlin unit tests for new VM state machine; Jest tests for new callables mirroring `confirmPurchase` patterns.
- Whether to add Phase 15 CONTEXT.md addendum (D-14) as part of this phase or rely on STATE.md note.
- FCM push data payload — extend with enriched fields (`pendingEntryKey`, `occasion`, `coverUrl`, `eventDateMs`) so system-tray tap can deep-link to sheet, or keep minimal `{ type, registryId }`.

### Deferred Ideas (OUT OF SCOPE)

- Web parity for accept/decline UI (Phase 15 resume + possible Phase 17).
- Owner FCM push on accept/decline.
- Owner-side "manage pending invites" UI.
- Pending-invite expiry timer.
- Bulk accept/decline.
- Migration of legacy `invitedUsers` → `pendingInvitedUsers`.
- Decline blacklist / re-invite throttling.
- Registry preview endpoint for pending invitees.
- Custom magic-link email template.

</user_constraints>

<phase_requirements>
## Phase Requirements

Phase 16 has **no formal REQ-IDs** in REQUIREMENTS.md (the goal/req IDs in ROADMAP.md are "TBD"). Per CONTEXT.md `<domain>`, the **28 decisions (D-01..D-28) constitute the requirement set**. The table below maps each substantive decision to the research finding(s) that enable safe implementation. Planner SHOULD use each `D-NN` ID as a `requirement_id` in PLAN.md frontmatter.

| ID | Behavior | Research Support |
|----|----------|------------------|
| D-01 | InviteResponseSheet — ModalBottomSheet with hero + Accept/Decline CTAs | Pattern 1 (existing `InviteBottomSheet.kt` shape) + Pattern 2 (`HeroImageOrPlaceholder` reuse) + Pattern 6 (`confirmValueChange` for D-07) |
| D-03 | Decline confirmation dialog | Material3 `AlertDialog` — no new component needed |
| D-04 | Server-side INVITE doc deletion + JOINED write | Pitfall 3 (inbox rule allows delete: false — only Admin SDK works) |
| D-07 | Inline warn-banner mirroring AuthScreen | Code Example "Warn Banner" (verbatim from `AuthScreen.kt:274-286`) |
| D-08 | Pending invites count via existing unread flow | Pattern 5 (`InboxBellViewModel` is auth-state-aware; no change needed) |
| D-09 | GiftMaison re-skin of NotificationsScreen | Don't-Hand-Roll item 4 (use existing GiftMaison primitives) |
| D-10 | Enriched payload embedded server-side | Pattern 3 (Admin SDK bypasses rules to read registry doc) |
| D-11 | Legacy notification branching on `pendingEntryKey` presence | Pattern 7 (forward-compat — `NotificationType.UNKNOWN` fallback already exists) |
| D-13 | Symmetric pending entry key (uid OR `email:xxx`) | Pitfall 1 (FieldPath required — emails contain dots) |
| D-18 | `pendingInvitedUsers` map field — owner-only read | Pattern 8 (extend `firestore.rules` + `tests/rules/firestore.rules.test.ts`) |
| D-20 | 2nd-gen onCall + enforceAppCheck | Pitfall 2 (CRITICAL — existing callables do NOT have `enforceAppCheck`; new ones MUST) |
| D-21/22 | Single all-or-nothing Firestore transaction | Pattern 4 (verbatim `confirmPurchase.ts` shape) |
| D-23 | `inviteToRegistry` writes to `pendingInvitedUsers` instead of `invitedUsers` | Pitfall 1 (FieldPath); Pitfall 4 (must update Jest test) |
| D-24 | Idempotent — verify-first guard | Pattern 4 (same verify-then-write reservation pattern) |
| D-25 | NotificationType enum extension + fromWire | Pattern 7 (forward-compat already established) |
| D-27 | Repository methods via httpsCallable + runCatching | Pattern 9 (verbatim `ReservationRepositoryImpl` shape) |
| D-28 | strings.xml × 2 locales + invite.ts email copy | Code Example "i18n keys" (see Pattern 11) |

</phase_requirements>

## Project Constraints (from CLAUDE.md)

CLAUDE.md project rules that directly bind this phase:

- **Kotlin/Compose only.** No Java. Compose BoM 2026.03.00.
- **Firebase BoM 34.11.0 — NO KTX modules.** New callable wrappers in `NotificationRepositoryImpl` MUST use `firebase-functions` (not `firebase-functions-ktx`). The existing `ReservationRepositoryImpl` is the verbatim pattern.
- **Cloud Functions 2nd gen, Node.js 22, TypeScript 5.x, europe-west3.** `firebase-functions/v2/https` import path. Verified against current `package.json`: `firebase-functions@^7.2.3`, `firebase-admin@^13.7.0` — both current as of npm registry 2026-05-24.
- **Firestore for all persistence — no SQLite/Room.** `pendingInvitedUsers` is a Firestore map field on existing registry docs. No new collection.
- **Localization — strings.xml only, no hardcoded strings in Kotlin/Compose.** Every new label belongs in both `values/strings.xml` and `values-ro/strings.xml`.
- **Hilt for DI throughout.** `NotificationRepositoryImpl` already `@Singleton` `@Inject` — no module changes; existing binding covers the new methods.
- **`enforceAppCheck: true` on every NEW callable (Phase 14 lesson — see Pitfall 2).**
- **GSD workflow enforcement.** All file changes via planned tasks.

## Standard Stack

### Core (already in repo — versions verified against `gradle/libs.versions.toml` + `functions/package.json` + npm registry on 2026-05-24)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `androidx.compose.material3` (via Compose BoM 2026.03.00) | bundled | `ModalBottomSheet`, `AlertDialog`, `rememberModalBottomSheetState` | The current GiftMaison foundation; matches `InviteBottomSheet.kt` already in repo |
| Firebase Android BoM | 34.11.0 | `firebase-functions` (callable) + `firebase-firestore` (no direct write needed) | Already wired; main modules (no KTX). Pinned in `libs.versions.toml`. |
| Coil 3 | 3.4.0 | `AsyncImage` inside `HeroImageOrPlaceholder` for sheet cover | Already wired; `HeroImageOrPlaceholder` is the reuse point |
| `firebase-functions` (Node) | 7.2.3 (current npm: 7.2.5 — non-breaking patch) | Cloud Function entry points | Already pinned. 2nd-gen import path. |
| `firebase-admin` (Node) | 13.7.0 (current npm: 13.10.0 — non-breaking minor) | Admin SDK in callables | Already pinned. Use `FieldPath` from `firebase-admin/firestore` subpath (not namespace style — see Pitfall 5). |
| `@firebase/rules-unit-testing` (in `tests/rules/`) | already wired | Firestore rules tests | Phase 01 + Phase 06 baseline; extend existing test file. |

**No new top-level dependencies required.** Phase 16 is purely additive code using already-installed libraries.

### Internal primitives (REUSE — do not rebuild)

| Primitive | Path | Use |
|-----------|------|-----|
| `HeroImageOrPlaceholder` | `app/src/main/java/com/giftregistry/ui/registry/cover/HeroImageOrPlaceholder.kt` | Sheet hero (D-01); pass `imageUrl` from payload `coverUrl`, `occasion` from payload `occasion`, `glyphSize = 40.sp` to match Registry Detail hero contract |
| `Btn` atoms (gm.accent, gm.line) | `app/src/main/java/com/giftregistry/ui/common/` (verify exact path during planning) | Accept (gm.accent) + Decline (gm.line outlined) CTAs |
| `MonoCaption` | `app/src/main/java/com/giftregistry/ui/common/` (verify) | Inbox card metadata (timestamp, "Pending" affordance if desired) |
| `GiftMaisonWordmark` | `app/src/main/java/com/giftregistry/ui/common/` (verify) | Inbox `TopAppBar` (D-09) |
| `AuthScreen` warn-banner shape | `app/src/main/java/com/giftregistry/ui/auth/AuthScreen.kt:274-286` | Verbatim copy of `Box` + `clip` + `background(colors.warn.copy(alpha = 0.15f))` for D-07 |
| `InviteBottomSheet` (owner-side) | `app/src/main/java/com/giftregistry/ui/registry/invite/InviteBottomSheet.kt` | Reference for ModalBottomSheet shape — do NOT modify this file |
| `ReservationRepositoryImpl.confirmPurchase` | `app/src/main/java/com/giftregistry/data/reservation/ReservationRepositoryImpl.kt:59-65` | Verbatim shape for `acceptInvite` / `declineInvite` wrappers |
| `writeNotification` | `functions/src/notifications/writeNotification.ts` | All four new inbox writes (joined, invite_accepted, invite_declined, re-invite refresh) |
| `confirmPurchase.ts` | `functions/src/reservation/confirmPurchase.ts` | Verbatim shape for new callables — onCall, region, runTransaction, HttpsError codes |
| `sendInvitePush` | `functions/src/notifications/invitePush.ts` | Reused for FCM push; payload data extension per D-10 (still under 4KB FCM cap — see Pitfall 6) |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Two callables (`acceptInvite` + `declineInvite`) | Single `respondToInvite({ action: "accept" \| "decline" })` | Single is DRYer (one auth gate, one verify path) but two are more granular for App Check / Cloud Logging filtering. Either acceptable per D-20. |
| New `NotificationType.JOINED` enum value | `payload.eventKind = "joined"` discriminator on a generic INVITE_RESPONSE_SELF type | Enum is cleaner for the `localizedTitle/Body` `when` blocks. Discriminator avoids enum churn if future "invite accepted by group" cases arrive. Discretion item per CONTEXT. |
| Owner-side inbox notifications written via direct doc-write in the transaction | Best-effort write OUTSIDE the transaction via `writeNotification` helper | CONTEXT D-21/22 say "all-or-nothing" — include in transaction. But `writeNotification.ts` swallows errors and logs to `notifications_failures` (best-effort by design). **Recommendation:** put membership writes (`pendingInvitedUsers` delete + `invitedUsers` set) INSIDE the transaction and call `writeNotification` AFTER `runTransaction` resolves. This matches the existing `inviteToRegistry.ts` ordering and preserves the best-effort semantics. The "all-or-nothing" framing in D-21 should be read as "membership state must be atomic" — the inbox-write tolerance is the established pattern (Phase 06 D-06). |

**Installation:** No new dependencies. Skip `npm install` / Gradle dep edits unless planner adds a Compose Material3 newer-than-BoM version.

**Version verification (npm registry, 2026-05-24):**
- `firebase-functions`: 7.2.5 (current) — repo pinned at 7.2.3 (compatible, non-breaking patch behind)
- `firebase-admin`: 13.10.0 (current) — repo pinned at 13.7.0 (compatible, minor behind)
- `@google-cloud/tasks`: 6.2.2 (current) — repo pinned at 6.2.1 (compatible, not used in Phase 16 callables)

No upgrade required for Phase 16 to ship safely. Leave version bumps for a separate housekeeping task.

## Architecture Patterns

### Recommended Project Structure

```
functions/src/registry/
├── acceptInvite.ts                  # NEW (D-20) — or combined respondToInvite.ts
├── declineInvite.ts                 # NEW (D-20) — or combined respondToInvite.ts
└── inviteToRegistry.ts              # MODIFIED (D-23) — writes pendingInvitedUsers + enriched payload

functions/src/notifications/
├── invitePush.ts                    # MODIFIED (D-10 optional) — extended data payload
└── writeNotification.ts             # REUSED as-is

functions/src/__tests__/
├── inviteToRegistry.test.ts         # MODIFIED — assert pendingInvitedUsers writes
├── acceptInvite.test.ts             # NEW — mirror confirmPurchase.test.ts harness
└── declineInvite.test.ts            # NEW — mirror confirmPurchase.test.ts harness

functions/src/index.ts               # MODIFIED — export new callables

tests/rules/
└── firestore.rules.test.ts          # MODIFIED — new "pendingInvitedUsers read scope" + "isInvited unchanged after promote" suites

app/src/main/java/com/giftregistry/ui/notifications/
├── InviteResponseSheet.kt           # NEW (D-01) — ModalBottomSheet host
├── DeclineConfirmDialog.kt          # NEW (D-03) — AlertDialog, or inline private composable inside InviteResponseSheet.kt
├── NotificationsScreen.kt           # MODIFIED — D-09 re-skin + D-11 branch on pendingEntryKey
├── NotificationsViewModel.kt        # MODIFIED — add acceptInvite/declineInvite action dispatchers
└── InviteResponseViewModel.kt       # NEW (D-07) — sheet-scoped state machine: Idle → Submitting → Error(msg, retry) → Success

app/src/main/java/com/giftregistry/domain/
├── model/Notification.kt            # MODIFIED — 3 new NotificationType + fromWire entries (D-25)
└── notifications/NotificationRepository.kt  # MODIFIED — add acceptInvite/declineInvite (D-27)

app/src/main/java/com/giftregistry/data/notifications/
└── NotificationRepositoryImpl.kt    # MODIFIED — two httpsCallable wrappers (D-27)

app/src/main/res/
├── values/strings.xml               # MODIFIED — new notification_* + invite_sheet_* keys (D-28)
└── values-ro/strings.xml            # MODIFIED — Romanian translations

app/src/test/java/com/giftregistry/
├── domain/model/NotificationTypeFromWireTest.kt        # NEW — assert 3 new wire mappings + UNKNOWN fallback (D-25)
├── ui/notifications/NotificationCardBranchingTest.kt    # NEW — pure-Kotlin predicate test for "pendingEntryKey present → sheet, missing → navigate" (D-11)
└── ui/notifications/InviteResponseViewModelTest.kt      # NEW — state machine: Idle → Submitting → Success / Error w/ Retry (D-07)

firestore.rules                      # MODIFIED — new pendingInvitedUsers read clause (D-18)
```

### Pattern 1: ModalBottomSheet hosting a state-driven content tree

**What:** Single composable owns sheet state, ViewModel state, and a child-composable tree that switches on the VM state. Existing pattern in repo: `InviteBottomSheet.kt`.
**When to use:** D-01 InviteResponseSheet.
**Example:**
```kotlin
// Source: app/src/main/java/com/giftregistry/ui/registry/invite/InviteBottomSheet.kt
// Adapted for D-01/D-07 — sheet stays open during loading; warn-banner on error.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteResponseSheet(
    registryId: String,
    payload: Map<String, String?>,    // contains coverUrl, occasion, eventDateMs, registryName, actorName
    onAcceptSuccess: () -> Unit,      // parent navigates to RegistryDetailKey (D-05)
    onDismiss: () -> Unit,
    viewModel: InviteResponseViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isLoading = state is InviteResponseViewModel.State.Submitting
    // Pattern 6: prevent swipe-to-dismiss DURING in-flight callable.
    // Idle and Error states leave dismissal free (D-07 — swipe always allowed when not loading).
    val sheetState = rememberModalBottomSheetState(
        confirmValueChange = { !isLoading }
    )

    LaunchedEffect(state) {
        if (state is InviteResponseViewModel.State.AcceptedSuccess) onAcceptSuccess()
        if (state is InviteResponseViewModel.State.DeclinedSuccess) onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = { if (!isLoading) onDismiss() },
        sheetState = sheetState,
    ) {
        InviteResponseSheetContent(
            payload = payload,
            state = state,
            onAccept = { viewModel.accept(registryId) },
            onDeclineConfirmed = { viewModel.decline(registryId) },
        )
    }
}
```

### Pattern 2: Reuse `HeroImageOrPlaceholder` for the sheet cover

**What:** Pass `imageUrl` from the notification payload `coverUrl` (nullable) + `occasion` string; component handles gradient-fallback and Coil rendering. `glyphSize = 40.sp` to match Registry Detail hero contract.
**When to use:** D-01, D-10 — render registry hero inside the sheet from payload only (no client registry-doc read, which would be denied pre-accept).
**Example:**
```kotlin
// Source: app/src/main/java/com/giftregistry/ui/registry/cover/HeroImageOrPlaceholder.kt
HeroImageOrPlaceholder(
    imageUrl = payload["coverUrl"],           // String? — null triggers placeholder
    occasion = payload["occasion"],            // drives glyph
    glyphSize = 40.sp,                         // Registry Detail hero contract
    modifier = Modifier.fillMaxWidth().height(180.dp),
)
```

### Pattern 3: Cloud Function callable with Admin-SDK transaction + post-tx inbox writes

**What:** Verbatim shape of `confirmPurchase.ts` — onCall with region pin, runTransaction for atomic state change, post-transaction best-effort side effects (Cloud Task cancel in confirmPurchase; inbox writes in Phase 16).
**When to use:** D-20/21/22 accept + decline callables.
**Example:**
```typescript
// Source: functions/src/reservation/confirmPurchase.ts (verbatim shape)
// Adapted for D-21 acceptInvite.
import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { FieldPath, FieldValue } from "firebase-admin/firestore";
import { writeNotification } from "../notifications/writeNotification";

const REGION = "europe-west3";

interface AcceptInviteRequest { registryId: string }
interface AcceptInviteResponse { success: boolean }

export const acceptInvite = onCall<AcceptInviteRequest>(
  { region: REGION, enforceAppCheck: true },   // Pitfall 2 — MUST enforce on new callables
  async (request): Promise<AcceptInviteResponse> => {
    if (!request.auth) throw new HttpsError("unauthenticated", "MUST_BE_SIGNED_IN");
    const { registryId } = request.data ?? ({} as AcceptInviteRequest);
    if (!registryId || typeof registryId !== "string") {
      throw new HttpsError("invalid-argument", "MISSING_REGISTRY_ID");
    }

    const uid = request.auth.uid;
    const db = admin.firestore();
    const registryRef = db.collection("registries").doc(registryId);

    // Pre-tx: capture data needed for post-tx inbox writes
    let ownerId = "";
    let registryName = "a registry";

    await db.runTransaction(async (tx) => {
      const snap = await tx.get(registryRef);
      if (!snap.exists) throw new HttpsError("not-found", "REGISTRY_NOT_FOUND");
      const data = snap.data()!;
      const pending = (data.pendingInvitedUsers ?? {}) as Record<string, boolean>;

      // D-24 idempotency: if already promoted, no-op success
      const invitedUsers = (data.invitedUsers ?? {}) as Record<string, boolean>;
      if (invitedUsers[uid] === true && pending[uid] !== true) {
        return; // already accepted — no writes
      }

      if (pending[uid] !== true) {
        throw new HttpsError("failed-precondition", "NO_PENDING_INVITE");
      }

      ownerId = (data.ownerId as string) ?? "";
      registryName = (data.title as string) ?? "a registry";

      // Pitfall 1 — FieldPath to avoid dot-in-key parsing. For uid keys this is
      // belt-and-suspenders; for email:xxx keys it is mandatory.
      tx.update(registryRef, new FieldPath("pendingInvitedUsers", uid), FieldValue.delete());
      tx.update(registryRef, new FieldPath("invitedUsers", uid), true);
    });

    // Post-tx: best-effort inbox cleanup + writes (Phase 06 D-06 pattern)
    // 1. Delete the INVITE inbox notification doc(s) for this user+registry (D-21 step 4)
    // 2. Write JOINED confirmation (D-21 step 5)
    // 3. Write invite_accepted owner notification (D-21 step 6)
    await deleteInviteNotificationsForRegistry(uid, registryId);  // helper — see below
    await writeNotification({
      userId: uid,
      type: "invite_accepted_self",  // D-25 — name locked during execution
      titleKey: "notification_invite_accepted_self_title",
      bodyKey: "notification_invite_accepted_self_body",
      titleFallback: `You joined "${registryName}"`,
      bodyFallback: registryName,
      payload: { registryId, registryName },
    });
    if (ownerId) {
      const actorName = await lookupDisplayName(uid);  // mirror inviteToRegistry owner lookup
      await writeNotification({
        userId: ownerId,
        type: "invite_accepted",
        titleKey: "notification_invite_accepted_title",
        bodyKey: "notification_invite_accepted_body",
        titleFallback: `${actorName} accepted your invite to "${registryName}"`,
        bodyFallback: registryName,
        payload: { registryId, registryName, actorName, actorUid: uid },
      });
    }

    return { success: true };
  }
);
```

### Pattern 4: Inbox-notification cleanup via query + batched delete (Admin SDK)

**What:** D-21 step 4 / D-22 step 3 require deleting the INVITE inbox doc(s) for `(uid, registryId)`. The inbox is `users/{uid}/notifications/{notifId}` with rule `allow delete: if false` — only Admin SDK can delete. Query for `type == "invite"` AND `payload.registryId == registryId`, then batch-delete.
**When to use:** Inside both `acceptInvite` and `declineInvite`, AFTER the runTransaction (the inbox docs are independent of the registry doc; including them in the tx would force multi-document tx complexity for no atomicity benefit since they're best-effort by Phase 06 design).
**Example:**
```typescript
async function deleteInviteNotificationsForRegistry(uid: string, registryId: string): Promise<void> {
  const db = admin.firestore();
  const inboxRef = db.collection("users").doc(uid).collection("notifications");
  try {
    const snap = await inboxRef
      .where("type", "==", "invite")
      .where("payload.registryId", "==", registryId)
      .get();
    if (snap.empty) return;
    const batch = db.batch();
    snap.docs.forEach((d) => batch.delete(d.ref));
    await batch.commit();
  } catch (err) {
    console.error(`[acceptInvite] inbox cleanup failed for ${uid}/${registryId}:`, err);
    // Best-effort — never throw on inbox cleanup (Phase 06 D-06)
  }
}
```
**Note:** The `where("payload.registryId", "==", ...)` query requires a composite index — Firestore will surface a console URL on first query failure if the index is missing. Add to `firestore.indexes.json` proactively (planner task).

### Pattern 5: Existing unread bell count flow — reuse as-is

**What:** `InboxBellViewModel` flat-maps auth state → `observeUnreadCount` which counts `readAt == null` docs. Pending INVITE docs (with their new enriched payload) inherit `readAt = null` from `writeNotification.ts` (line 54: `readAt: null`).
**When to use:** D-08 — no new code needed. Pending invites contribute to the count automatically.

### Pattern 6: Prevent ModalBottomSheet dismissal during loading

**What:** Pass `confirmValueChange = { newValue -> !isLoading }` to `rememberModalBottomSheetState`. Also guard `onDismissRequest`. From verified Android Developers docs.
**When to use:** D-07 — sheet stays open during in-flight callable; swipe-dismiss is denied. Idle and Error states leave dismissal free.
**Example:** See Pattern 1 above.

### Pattern 7: NotificationType forward-compat — UNKNOWN as default

**What:** Existing `NotificationType.fromWire(raw)` returns `UNKNOWN` for any unrecognized wire string. New server-only types ship transparently to old clients. Already established in `domain/model/Notification.kt:14-22`.
**When to use:** D-11 — older app builds receiving the new `invite_accepted_self` / `invite_accepted` / `invite_declined` types render as UNKNOWN (generic bell icon, titleFallback string). This is the desired graceful degradation.

### Pattern 8: Firestore rules — append field-scoped read clause

**What:** D-18 says `pendingInvitedUsers` is readable only by `isOwner(registryData)`. But Firestore rules cannot field-scope `allow read` — the read rule applies to the entire document. The actual scope: owners read the whole registry doc (already allowed via `canReadRegistry` → `isOwner`); invitees do NOT need to read the registry doc to learn their pending status (they get it from their inbox notification, which is in `users/{uid}/notifications` and already owner-isolated). So in practice **NO new explicit field rule is required** — the existing `canReadRegistry` already prevents non-owners from reading the registry doc, and `pendingInvitedUsers` is just another field on that doc.
**When to use:** D-18. Planner should verify this by: (a) writing a Jest rules test that confirms a non-owner cannot read a registry doc with `pendingInvitedUsers` populated, and (b) writing a test that confirms an invitee who has `pendingInvitedUsers[uid] = true` but is NOT in `invitedUsers[uid]` still cannot read the registry. Both should pass against the **unchanged** existing rules.

If those tests pass, no rule edit is needed beyond the new test suite. If they fail (edge case: legacy `isInvited` somehow reads through), only then add an explicit deny clause. **Recommend test-first verification rather than premature rule edits.**

### Pattern 9: Repository method — httpsCallable + runCatching

**What:** Verbatim `ReservationRepositoryImpl.confirmPurchase` shape.
**Example:**
```kotlin
// Source: app/src/main/java/com/giftregistry/data/reservation/ReservationRepositoryImpl.kt:59-65
override suspend fun acceptInvite(registryId: String): Result<Unit> = runCatching {
    functions
        .getHttpsCallable("acceptInvite")
        .call(mapOf("registryId" to registryId))
        .await()
    Unit
}

override suspend fun declineInvite(registryId: String): Result<Unit> = runCatching {
    functions
        .getHttpsCallable("declineInvite")
        .call(mapOf("registryId" to registryId))
        .await()
    Unit
}
```

### Pattern 10: Branching the inbox card — sheet open vs registry navigate

**What:** D-11 — `NotificationCard` clickable lambda branches on `payload["pendingEntryKey"] != null`.
**Example:**
```kotlin
// In NotificationsScreen.kt (modified)
NotificationCard(
    notification = notification,
    onClick = {
        val isPendingInvite = notification.type == NotificationType.INVITE
            && notification.payload["pendingEntryKey"] != null
        if (isPendingInvite) {
            // D-01 — open InviteResponseSheet
            viewModel.openInviteSheet(notification)
        } else {
            // Legacy or non-INVITE — preserve current behavior
            notification.payload["registryId"]?.let { onNavigateToRegistry(it) }
        }
    },
)
```

### Pattern 11: i18n key extension

**What:** Append new keys to both `values/strings.xml` and `values-ro/strings.xml`. Existing namespace `notification_*` extended; new namespace `invite_sheet_*`. Format args via positional `%1$s` / `%2$s` style consistent with current `notification_invite_title` etc.
**Example keys (final copy decided during execution per D-28):**
```xml
<!-- values/strings.xml additions -->
<string name="notification_invite_accepted_self_title">You joined "%1$s"</string>
<string name="notification_invite_accepted_self_body">Tap to view %1$s</string>
<string name="notification_invite_accepted_title">%1$s accepted your invite to "%2$s"</string>
<string name="notification_invite_accepted_body">%1$s</string>
<string name="notification_invite_declined_title">%1$s declined your invite to "%2$s"</string>
<string name="notification_invite_declined_body">%1$s</string>

<string name="invite_sheet_title_template">%1$s invited you to</string>
<string name="invite_sheet_accept_cta">Accept</string>
<string name="invite_sheet_decline_cta">Decline</string>
<string name="invite_sheet_decline_confirm_title">Decline invite to "%1$s"?</string>
<string name="invite_sheet_decline_confirm_cancel">Cancel</string>
<string name="invite_sheet_decline_confirm_decline">Decline</string>
<string name="invite_sheet_error_accept">Couldn\'t accept invite. Please try again.</string>
<string name="invite_sheet_error_decline">Couldn\'t decline invite. Please try again.</string>
<string name="invite_sheet_error_retry">Retry</string>
```

### Anti-Patterns to Avoid

- **Don't read the registry doc from the client to render the sheet hero.** Pre-accept, the invitee has no read permission on private registries. Render exclusively from the notification payload (D-10).
- **Don't write to `pendingInvitedUsers` from the Android client.** All writes via the new callables (D-18). The Android client never touches this field.
- **Don't put the inbox-doc deletion inside the runTransaction.** Multi-doc transactions across collection boundaries (`registries/...` AND `users/{uid}/notifications/...`) inflate tx contention with no atomicity benefit — inbox docs are best-effort by Phase 06 design.
- **Don't add a separate "PENDING" badge or richer card layout for pending invites** (D-02). Visual richness lives in the sheet only.
- **Don't migrate existing `invitedUsers` entries** (D-12). Grandfathered.
- **Don't store the notification icon/title hardcoded in Kotlin** — extend `NotificationsScreen.kt` `localizedTitle/Body` `when` blocks with the new types.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Registry hero in sheet | Custom Box + AsyncImage + glyph fallback | `HeroImageOrPlaceholder` from `ui/registry/cover/` | Already handles preset sentinels, gradient, glyph sizing, contentFilter; identical to Detail hero |
| Sheet dismissal-while-loading guard | Custom flag + manual back-press handling | `confirmValueChange = { !isLoading }` on sheet state | Built-in to Material3; verified pattern |
| Inbox warn-banner | Custom Snackbar / Toast / SnackbarHost | Verbatim Box-clip-background pattern from `AuthScreen.kt:274-286` | Established Phase 10 D-02 pattern; consistent visual; in-sheet (D-07) |
| FCM data payload structure | Custom JSON envelope | Plain key-value object passed to `getMessaging().sendEachForMulticast({ data: {...} })` | Already used in `invitePush.ts`; stay under 4KB cap (Pitfall 6) |
| Wire-string → enum mapping | `Map<String, NotificationType>` | Extend `NotificationType.fromWire(raw)` `when` block | Established Phase 06 pattern with UNKNOWN fallback |
| Decline confirmation | Custom Compose dialog with backdrop + buttons | Material3 `AlertDialog` | D-03 explicitly specifies AlertDialog; zero new code |
| Server-side inbox writes | Direct `firestore.collection().add()` | `writeNotification` helper | Already best-effort + logs to `notifications_failures`; consistent shape |
| Cloud Task scheduling for pending-invite expiry | Cloud Tasks queue + onTaskDispatched handler | Don't build it at all | Pending-invite expiry timer is OUT OF SCOPE (deferred per CONTEXT `<deferred>`) |

**Key insight:** Every meaningful primitive needed by Phase 16 already exists in the codebase. The phase is largely composition + extension, not new architecture.

## Common Pitfalls

### Pitfall 1: Dot-in-key Admin SDK parsing — emails break dotted-string update keys

**What goes wrong:** `await registryRef.update("pendingInvitedUsers.email:jane.doe@example.com", true)` interprets every `.` as a nested-field separator. Instead of `pendingInvitedUsers["email:jane.doe@example.com"] = true`, Firestore creates `{ pendingInvitedUsers: { "email:jane": { "doe@example": { com: true } } } }`. Security rules break, client deserialization breaks, silently.
**Why it happens:** Admin SDK's `update(string, value)` parses the path on `.`. Real email addresses almost always contain dots.
**How to avoid:** Use `FieldPath` from `firebase-admin/firestore`:
```typescript
import { FieldPath, FieldValue } from "firebase-admin/firestore";
await registryRef.update(new FieldPath("pendingInvitedUsers", inviteKey), true);
// To delete in a transaction:
tx.update(registryRef, new FieldPath("pendingInvitedUsers", uid), FieldValue.delete());
```
**Warning signs:** A pending-invite-not-found error from `acceptInvite` even though the owner-side invite "succeeded"; a registry doc snapshot that shows `pendingInvitedUsers` as a nested map of name fragments instead of a flat boolean map.
**Source:** Already solved in `inviteToRegistry.ts:86-92` with detailed comments — copy that pattern verbatim.

### Pitfall 2: `enforceAppCheck: true` missing on existing callables — Phase 16 MUST add it for new callables

**What goes wrong:** Without `enforceAppCheck: true` on `onCall`, anyone with the project's web API key (which is public) can invoke the callable directly via curl with any UID claim they fabricate. App Check enforcement gates this.
**Why it happens:** Easy to forget the option. The existing `fetchOgMetadata.ts` and `inviteToRegistry.ts` do NOT have `enforceAppCheck: true` — they were written before Phase 14's lesson. The pending todo `2026-05-22-wire-android-app-check-and-flip-enforcement.md` covers retrofitting them.
**How to avoid:** Every new `onCall` in Phase 16 must include `{ region: "europe-west3", enforceAppCheck: true }`. CONTEXT D-20 makes this explicit. Reference: `firebase-functions/v2/https` docs.
**Warning signs:** Android UAT works fine (App Check provider attached) but functional callable tests from non-App-Check contexts fail with `unauthenticated` — that's the success indicator. If they succeed, the enforcement isn't wired.

### Pitfall 3: Inbox-doc deletion requires Admin SDK — clients are denied

**What goes wrong:** D-21 step 4 / D-22 step 3 require deleting the original INVITE notification doc. The Firestore rule for `users/{uid}/notifications/{notifId}` is `allow delete: if false` — clients cannot self-delete. Only Admin SDK (in the callable) can delete.
**Why it happens:** Phase 06 D-06 locked down clients to read + update-readAt-only as a tampering guard.
**How to avoid:** Deletion lives in the callable, not in `NotificationRepositoryImpl`. The Android client never calls a delete API — when the callable completes successfully, the snapshot listener on `users/{uid}/notifications` removes the INVITE row from the inbox automatically.
**Warning signs:** A `permission-denied` error if anyone tries to add `markInviteResolved(notifId): Result<Unit>` to the client repository — don't do that.

### Pitfall 4: `inviteToRegistry.test.ts` will break when the function switches to `pendingInvitedUsers`

**What goes wrong:** Existing test asserts `mockStore.registries.regX.invitedUsers["email:..."]` after the function runs. D-23 changes this to `pendingInvitedUsers`. The test will turn red — that's correct, but the planner must include the test update in the same plan as the code change, or CI breaks.
**Why it happens:** The Phase 03 test was written when `inviteToRegistry` wrote directly to `invitedUsers`.
**How to avoid:** Same plan / wave that modifies `inviteToRegistry.ts` MUST modify `inviteToRegistry.test.ts`. Also add new assertions: enriched payload fields present in the notification doc, no write to `invitedUsers`.
**Warning signs:** CI green on local emulator but red on the test runner — invariably means the test wasn't updated.

### Pitfall 5: `admin.firestore.FieldPath` is undefined in `import * as admin` style — use subpath import

**What goes wrong:** `new admin.firestore.FieldPath(...)` throws `TypeError: admin.firestore.FieldPath is not a constructor` at runtime with `firebase-admin@13.x` and `import * as admin from "firebase-admin"`.
**Why it happens:** Package-exports map in v13 doesn't reliably populate the namespace-style accessor.
**How to avoid:** Always `import { FieldPath, FieldValue } from "firebase-admin/firestore"` at the top of the file. Already documented in `inviteToRegistry.ts:87-91` — copy the comment too.
**Warning signs:** Function deploys clean, throws at first invocation with the constructor error. Won't be caught by `tsc --noEmit`.

### Pitfall 6: FCM payload 4096-byte limit — enriched data fields can push close

**What goes wrong:** D-10 / planner-discretion item adds `pendingEntryKey`, `occasion`, `coverUrl`, `eventDateMs` to FCM data payload. `coverUrl` for a Firebase Storage path can be 200+ chars; `pendingEntryKey` for `email:long.name+tag@example.org` can be 60+ chars. Combined with `notification.title/body` (which counts toward the 4096 cap), the payload can exceed it for long values.
**Why it happens:** FCM's `MessageTooBig` (code = 4) is returned per-token, swallowed in `sendInvitePush` via `notifications_failures` log — push silently fails to deliver, but the inbox write still works.
**How to avoid:** Either (a) skip extending the FCM data payload — keep `{ type, registryId }` and rely on the inbox doc to carry the enriched fields (the sheet opens after a tap → inbox → tap pending card, not directly from system tray; this matches D-23: "push tap deep-links to the inbox"); or (b) extend with only the small fields (`pendingEntryKey`, `occasion`, `eventDateMs`) and OMIT `coverUrl` (cover preview happens after sheet open, sourced from the inbox doc's payload — which has no size limit, only Firestore's 1MB doc cap).
**Recommendation:** Planner picks (a) — keep FCM data payload minimal. The enriched fields live on the inbox doc; the FCM push is just a "wake up, check your inbox" signal.
**Warning signs:** `notifications_failures` entries with `error: "messaging/payload-too-large"` or similar. Test by deploying with a worst-case `email:long@example.org` invitee and a Storage URL cover.
**Source:** [Firebase Cloud Messaging Message Types](https://firebase.google.com/docs/cloud-messaging/customize-messages/set-message-type) confirms 4096-byte cap.

### Pitfall 7: Composite index missing for `where("type", "==", "invite").where("payload.registryId", "==", X)`

**What goes wrong:** Pattern 4's inbox cleanup query needs a composite index `(type asc, payload.registryId asc)` on `users/{uid}/notifications`. First invocation in production fails with a console URL to create the index; until created, the query throws.
**Why it happens:** Firestore requires composite indexes for any multi-field where-equality query.
**How to avoid:** Add the index to `firestore.indexes.json` BEFORE deploying the new callables. Deploy indexes via `firebase deploy --only firestore:indexes`. Confirm index build status in console before invoking the callable.
**Warning signs:** First production accept/decline call returns success on the membership write (transaction OK) but logs `notifications_failures` for "inbox cleanup failed" with a Firestore index-required error. The orphan INVITE doc remains in the inbox until the index builds and the next invocation runs (which won't happen automatically — the doc is orphan).
**Mitigation:** If indexes aren't deployed in time, fall back to fetching the user's inbox INVITE docs by `type == "invite"` only (single-field, no composite needed) and filter `payload.registryId == X` in code. Slightly less efficient but no index dependency.

### Pitfall 8: Sheet auto-navigation on Accept can race with snapshot listener latency

**What goes wrong:** D-05 says "Accept success → sheet dismisses → auto-navigate to RegistryDetailKey". But Registry detail relies on `canReadRegistry` rules — the user becomes a member when the transaction commits, but the Android Firestore SDK's local snapshot cache may not have refreshed yet. Navigating immediately may show a brief "registry not found" / 404 flash.
**Why it happens:** Local Firestore cache doesn't auto-invalidate; the next snapshot from the listener will refresh, but there's a window of milliseconds.
**How to avoid:** Either (a) rely on Firestore's offline persistence eventual-consistency (typical sub-100ms; usually invisible) and accept the rare flash; or (b) Force a `registryRef.get(Source.SERVER)` after the callable returns to prime the cache before navigating. Recommendation: ship (a) first; promote to (b) only if UAT surfaces a visible flash.
**Warning signs:** UAT testers report "tapped Accept, registry briefly showed 'not available', then loaded". If reported, switch to (b).

## Runtime State Inventory

> **Phase 16 is additive — no renames or refactors that risk runtime-state drift.** Inventory completed as a discipline check rather than because the work touches existing keys.

| Category | Items Found | Action Required |
|----------|-------------|------------------|
| Stored data | **New field added**: `registries/*.pendingInvitedUsers` map. Existing `registries/*.invitedUsers` map UNCHANGED — D-12 grandfathers existing entries. Existing `users/*/notifications` docs UNCHANGED in schema, but Phase 16 adds new payload keys (`pendingEntryKey`, `occasion`, `coverUrl`, `eventDateMs`) — additive, legacy docs lack them (D-11 handles missing field). | Code edit only (server writes the new field). No data migration of existing `invitedUsers`. Planner adds composite index `(type, payload.registryId)` on `users/*/notifications` per Pitfall 7. |
| Live service config | None — no n8n / Datadog / external service config. Firebase Console has reCAPTCHA + Identity Platform from Phase 14/15; no Phase 16 changes there. | None. |
| OS-registered state | None — no Windows Task Scheduler / launchd / systemd / pm2 registrations. | None. |
| Secrets/env vars | No new secrets. New callables use the existing project's Admin SDK credentials (auto-injected by Functions runtime). No `functions/.env` changes. | None. |
| Build artifacts | None — no compiled binaries, pip egg-info, or Docker image tags that embed the old field name (there is no "old field name" — `pendingInvitedUsers` is new). | None. |

**The canonical question:** *After every file in the repo is updated, what runtime systems still have the old string cached, stored, or registered?*

**Answer:** Nothing. Phase 16 introduces new state (`pendingInvitedUsers` field, 3 new notification types, new callable functions); legacy `invitedUsers` is left untouched per D-12. The only forward-deployment concern is the **composite index** for the inbox cleanup query (Pitfall 7) — add to `firestore.indexes.json` and deploy before the callables are invoked in production.

## Environment Availability

> Phase 16 is code-only — no new external tool requirements. All dependencies already wired and verified by Phases 01-14.

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Firebase Emulator Suite (Functions + Firestore + Auth) | Local development of new callables | ✓ | Phase 01 wiring confirmed | — |
| Node.js 22 LTS | Cloud Functions runtime | ✓ | `functions/package.json` engines | — |
| Android Studio Meerkat 2024.3+ | Compose preview for new sheet/dialog | ✓ | repo standard | — |
| Firebase CLI 13+ | Deploy callables + rules + indexes | ✓ | Phase 14 confirmed | — |
| App Check provider (Android) | Required for new callables in production (`enforceAppCheck: true`) | ⚠ partial | Web has reCAPTCHA v3 wired (Phase 14); Android has NO App Check provider | **Fallback:** Phase 16 enforces App Check on the NEW callables only. Until the pending todo `2026-05-22-wire-android-app-check-and-flip-enforcement.md` is done, Android **must** wire an App Check provider in `App.kt` initialization BEFORE Phase 16's callables are invoked from Android — or every accept/decline returns `unauthenticated`. **Block:** This is a hard dependency. Planner MUST add an Android App Check wiring task as a wave-0 prerequisite, or explicitly defer Phase 16 production rollout until the pending todo completes. |

**Missing dependencies with no fallback:** None purely-blocking.

**Missing dependencies with fallback:** Android App Check provider (see above — wire as Phase 16 wave-0 task, or coordinate with the pending todo).

## Validation Architecture

> Workflow `nyquist_validation: true` (verified in `.planning/config.json`). This section is required.

### Test Framework

| Property | Value |
|----------|-------|
| Framework (Android) | JUnit 4 + MockK 1.13.17 + Turbine 1.2.0 + kotlinx-coroutines-test 1.9.0 (all in `libs.versions.toml`) |
| Framework (Cloud Functions) | Jest 29.7 + ts-jest 29.2 + firebase-functions-test 3.3 (`functions/package.json`) |
| Framework (Firestore rules) | Jest + `@firebase/rules-unit-testing` (`tests/rules/jest.config.js`) |
| Android config | `app/build.gradle.kts` `testOptions` (existing) |
| Functions config | `functions/jest.config.*` (existing) |
| Rules config | `tests/rules/jest.config.js` (existing) |
| Quick run command (Android unit only) | `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.notifications.*" --tests "com.giftregistry.domain.model.NotificationTypeFromWireTest"` |
| Quick run command (Functions only) | `cd functions && npm test -- acceptInvite declineInvite inviteToRegistry` |
| Quick run command (Rules only) | `cd tests/rules && npm test` |
| Full suite command | `./gradlew test && (cd functions && npm test) && (cd tests/rules && npm test)` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| D-11 | NotificationCard branches sheet vs navigate on `pendingEntryKey` presence | unit (pure-Kotlin predicate) | `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.notifications.NotificationCardBranchingTest"` | ❌ Wave 0 |
| D-25 | NotificationType.fromWire maps 3 new wire strings + UNKNOWN fallback for unrecognized | unit | `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.domain.model.NotificationTypeFromWireTest"` | ❌ Wave 0 |
| D-07 | InviteResponseViewModel state machine: Idle → Submitting → Error(retry) / Success | unit (Turbine) | `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.ui.notifications.InviteResponseViewModelTest"` | ❌ Wave 0 |
| D-27 | acceptInvite / declineInvite repository methods call correct callable name + wrap in runCatching | unit (MockK FirebaseFunctions fake) | `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.data.notifications.NotificationRepositoryImplAcceptDeclineTest"` | ❌ Wave 0 |
| D-21 | acceptInvite callable transaction promotes uid + writes 3 notifications | integration (Jest, mock Firestore via existing harness) | `cd functions && npm test -- acceptInvite` | ❌ Wave 0 |
| D-22 | declineInvite callable transaction removes pending + writes owner notification | integration | `cd functions && npm test -- declineInvite` | ❌ Wave 0 |
| D-24 | acceptInvite/declineInvite are idempotent (no-op on already-completed) | integration | `cd functions && npm test -- acceptInvite declineInvite` | ❌ Wave 0 |
| D-23 | inviteToRegistry writes to pendingInvitedUsers (not invitedUsers) + enriched payload | integration | `cd functions && npm test -- inviteToRegistry` | ✓ exists (modify) |
| D-18 | Registry doc with pendingInvitedUsers — non-owner cannot read; invitee with only pending entry cannot read | integration (rules) | `cd tests/rules && npm test -- firestore.rules` | ✓ exists (extend) |
| D-19 | isInvited rule unchanged — invitee promoted to invitedUsers can read | integration (rules) | `cd tests/rules && npm test -- firestore.rules` | ✓ exists (extend) |
| D-05 / D-01 | Sheet auto-nav on accept; sheet stays open on error | manual / on-device | UAT checklist in final plan | ❌ Manual-only — Compose UI test for sheet visibility is fragile under emulator + ModalBottomSheet animations; defer to human UAT |
| D-09 | Inbox re-skin pixel contract (paper bg, line dividers, MonoCaption metadata, accent dot for unread) | manual + StyleGuidePreview | StyleGuidePreview section + UAT | ❌ Manual — visual; StyleGuidePreview file Wave 3 |
| D-28 | Strings present in both locales | unit (string-resource enumeration) | `./gradlew :app:testDebugUnitTest --tests "com.giftregistry.LocalizationParityTest"` (existing pattern from prior phases — verify if available; if not, manual diff of strings.xml files) | ⚠ Verify existing test exists; else manual diff |

### Sampling Rate

- **Per task commit:** Run the relevant quick-run command (Android unit OR Functions OR Rules) — under 30s each.
- **Per wave merge:** Full suite for the touched module (e.g., wave that ships acceptInvite runs `cd functions && npm test`).
- **Phase gate:** `./gradlew test && (cd functions && npm test) && (cd tests/rules && npm test)` green before `/gsd:verify-work`. Plus on-device UAT for D-01/05/09 manual items.

### Wave 0 Gaps

- [ ] `app/src/test/java/com/giftregistry/domain/model/NotificationTypeFromWireTest.kt` — RED test for 3 new wire-string mappings + UNKNOWN fallback (D-25)
- [ ] `app/src/test/java/com/giftregistry/ui/notifications/NotificationCardBranchingTest.kt` — RED test for `pendingEntryKey`-presence predicate (D-11). Extract predicate to pure-Kotlin helper for testability.
- [ ] `app/src/test/java/com/giftregistry/ui/notifications/InviteResponseViewModelTest.kt` — RED test for state machine using Turbine (D-07)
- [ ] `app/src/test/java/com/giftregistry/data/notifications/NotificationRepositoryImplAcceptDeclineTest.kt` — RED test for the two new callable wrappers using MockK fake `FirebaseFunctions` (D-27)
- [ ] `functions/src/__tests__/acceptInvite.test.ts` — RED test harness mirroring `confirmPurchase.test.ts` (D-21, D-24)
- [ ] `functions/src/__tests__/declineInvite.test.ts` — RED test harness (D-22, D-24)
- [ ] `functions/src/__tests__/inviteToRegistry.test.ts` — MODIFY existing tests: expect writes to `pendingInvitedUsers`, expect enriched payload fields in notification doc, expect re-invite of already-member to skip pending write but still write inbox doc (D-16, D-23)
- [ ] `tests/rules/firestore.rules.test.ts` — MODIFY: add `pendingInvitedUsers` read-scope tests (D-18) + verify `isInvited` unchanged after promote (D-19). New describe blocks; existing tests untouched.
- [ ] `firestore.indexes.json` — Add composite index `users/*/notifications` on `(type asc, payload.registryId asc)` per Pitfall 7
- [ ] No new framework install needed — all tooling already in repo

## Code Examples

Verified patterns from the repo (HIGH confidence — copied from existing tested code) and verified Material3 docs.

### Example A: Cloud Function callable — acceptInvite skeleton (verbatim shape adapted from confirmPurchase.ts)

```typescript
// Source: functions/src/reservation/confirmPurchase.ts (verbatim shape)
import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { FieldPath, FieldValue } from "firebase-admin/firestore";
import { writeNotification } from "../notifications/writeNotification";

interface AcceptInviteRequest { registryId: string }
interface AcceptInviteResponse { success: boolean }

const REGION = "europe-west3";

export const acceptInvite = onCall<AcceptInviteRequest>(
  { region: REGION, enforceAppCheck: true },
  async (request): Promise<AcceptInviteResponse> => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "MUST_BE_SIGNED_IN");
    }
    const { registryId } = request.data ?? ({} as AcceptInviteRequest);
    if (!registryId || typeof registryId !== "string") {
      throw new HttpsError("invalid-argument", "MISSING_REGISTRY_ID");
    }

    const db = admin.firestore();
    const uid = request.auth.uid;
    const registryRef = db.collection("registries").doc(registryId);

    let ownerId = "";
    let registryName = "a registry";
    let didPromote = false;

    await db.runTransaction(async (tx) => {
      const snap = await tx.get(registryRef);
      if (!snap.exists) throw new HttpsError("not-found", "REGISTRY_NOT_FOUND");
      const data = snap.data()!;
      const pending = (data.pendingInvitedUsers ?? {}) as Record<string, boolean>;
      const invited = (data.invitedUsers ?? {}) as Record<string, boolean>;

      // D-24 idempotency
      if (invited[uid] === true) { didPromote = false; return; }
      if (pending[uid] !== true) {
        throw new HttpsError("failed-precondition", "NO_PENDING_INVITE");
      }

      ownerId = (data.ownerId as string) ?? "";
      registryName = (data.title as string) ?? "a registry";

      // Pitfall 1 — FieldPath always (uids don't have dots, but emails do via D-14's email:xxx -> uid swap path)
      tx.update(registryRef, new FieldPath("pendingInvitedUsers", uid), FieldValue.delete());
      tx.update(registryRef, new FieldPath("invitedUsers", uid), true);
      didPromote = true;
    });

    if (didPromote) {
      // Post-tx side effects (best-effort, never throw)
      await deleteInviteNotificationsForRegistry(uid, registryId);
      await writeNotification({
        userId: uid,
        type: "invite_accepted_self",
        titleKey: "notification_invite_accepted_self_title",
        bodyKey: "notification_invite_accepted_self_body",
        titleFallback: `You joined "${registryName}"`,
        bodyFallback: registryName,
        payload: { registryId, registryName },
      });
      if (ownerId) {
        const actorName = await lookupDisplayName(uid);
        await writeNotification({
          userId: ownerId,
          type: "invite_accepted",
          titleKey: "notification_invite_accepted_title",
          bodyKey: "notification_invite_accepted_body",
          titleFallback: `${actorName} accepted your invite to "${registryName}"`,
          bodyFallback: registryName,
          payload: { registryId, registryName, actorName, actorUid: uid },
        });
      }
    }

    return { success: true };
  }
);

async function deleteInviteNotificationsForRegistry(uid: string, registryId: string): Promise<void> {
  const db = admin.firestore();
  try {
    const snap = await db.collection("users").doc(uid).collection("notifications")
      .where("type", "==", "invite")
      .where("payload.registryId", "==", registryId)
      .get();
    if (snap.empty) return;
    const batch = db.batch();
    snap.docs.forEach((d) => batch.delete(d.ref));
    await batch.commit();
  } catch (err) {
    console.error(`[acceptInvite] inbox cleanup failed for ${uid}/${registryId}:`, err);
  }
}

async function lookupDisplayName(uid: string): Promise<string> {
  try {
    const record = await admin.auth().getUser(uid);
    return record.displayName || record.email?.split("@")[0] || "Someone";
  } catch {
    return "Someone";
  }
}
```

### Example B: NotificationType extension (Android domain)

```kotlin
// Source: app/src/main/java/com/giftregistry/domain/model/Notification.kt (extended)
enum class NotificationType {
    INVITE,
    RESERVATION_CREATED,
    ITEM_PURCHASED,
    RESERVATION_EXPIRED,
    RE_RESERVE_WINDOW,
    INVITE_ACCEPTED_SELF,   // NEW — D-25 (or JOINED; planner chooses)
    INVITE_ACCEPTED,         // NEW — D-25 (owner view)
    INVITE_DECLINED,         // NEW — D-25 (owner view)
    UNKNOWN;

    companion object {
        fun fromWire(raw: String?): NotificationType = when (raw) {
            "invite" -> INVITE
            "reservation_created" -> RESERVATION_CREATED
            "item_purchased" -> ITEM_PURCHASED
            "reservation_expired" -> RESERVATION_EXPIRED
            "re_reserve_window" -> RE_RESERVE_WINDOW
            "invite_accepted_self" -> INVITE_ACCEPTED_SELF
            "invite_accepted" -> INVITE_ACCEPTED
            "invite_declined" -> INVITE_DECLINED
            else -> UNKNOWN
        }
    }
}
```

### Example C: NotificationRepository extension (Android)

```kotlin
// Source: app/src/main/java/com/giftregistry/domain/notifications/NotificationRepository.kt (extended)
interface NotificationRepository {
    fun observe(uid: String, limit: Int = 50): Flow<List<Notification>>
    fun observeUnreadCount(uid: String): Flow<Int>
    suspend fun markRead(uid: String, notificationIds: List<String>): Result<Unit>
    // NEW — D-27
    suspend fun acceptInvite(registryId: String): Result<Unit>
    suspend fun declineInvite(registryId: String): Result<Unit>
}

// Source: app/src/main/java/com/giftregistry/data/notifications/NotificationRepositoryImpl.kt (extended)
@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,  // NEW dep — already exposed by FirebaseModule (verify; if not, add binding)
) : NotificationRepository {
    // ...existing methods unchanged...

    override suspend fun acceptInvite(registryId: String): Result<Unit> = runCatching {
        functions.getHttpsCallable("acceptInvite")
            .call(mapOf("registryId" to registryId))
            .await()
        Unit
    }

    override suspend fun declineInvite(registryId: String): Result<Unit> = runCatching {
        functions.getHttpsCallable("declineInvite")
            .call(mapOf("registryId" to registryId))
            .await()
        Unit
    }
}
```

### Example D: Sheet warn-banner (verbatim from AuthScreen)

```kotlin
// Source: app/src/main/java/com/giftregistry/ui/auth/AuthScreen.kt:274-286 (verbatim)
state.errorMessage?.let { msg ->
    Spacer(modifier = Modifier.height(spacing.gap12))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.radius12)
            .background(colors.warn.copy(alpha = 0.15f))
            .padding(spacing.gap12),
    ) {
        Column {
            Text(text = msg, style = typography.bodyM, color = colors.inkSoft)
            Spacer(modifier = Modifier.height(spacing.gap8))
            // Retry CTA — pill-style Btn
            Btn(
                text = stringResource(R.string.invite_sheet_error_retry),
                onClick = onRetry,
                style = BtnStyle.Line,  // or whatever the line/secondary atom is named
            )
        }
    }
}
```

### Example E: Firestore rules test for D-18 (new suite to add)

```typescript
// Source: tests/rules/firestore.rules.test.ts (add new describe block)
describe("pendingInvitedUsers read scope (D-18)", () => {
  it("non-owner cannot read a registry doc with pendingInvitedUsers populated", async () => {
    await setDoc(adminDb, "registries/reg-pending", {
      ownerId: "owner-1",
      visibility: "private",
      invitedUsers: {},
      pendingInvitedUsers: { "stranger-uid": true },
    });
    const strangerDb = testEnv.authenticatedContext("stranger-uid").firestore();
    await assertFails(getDoc(doc(strangerDb, "registries/reg-pending")));
  });

  it("invitee with ONLY pending entry (no invitedUsers entry) cannot read registry", async () => {
    await setDoc(adminDb, "registries/reg-pending-only", {
      ownerId: "owner-1",
      visibility: "private",
      invitedUsers: {},
      pendingInvitedUsers: { "invitee-uid": true },
    });
    const inviteeDb = testEnv.authenticatedContext("invitee-uid").firestore();
    await assertFails(getDoc(doc(inviteeDb, "registries/reg-pending-only")));
  });

  it("owner CAN read registry with pendingInvitedUsers populated", async () => {
    await setDoc(adminDb, "registries/reg-owner-pending", {
      ownerId: "owner-1",
      visibility: "private",
      invitedUsers: {},
      pendingInvitedUsers: { "someone": true },
    });
    const ownerDb = testEnv.authenticatedContext("owner-1").firestore();
    await assertSucceeds(getDoc(doc(ownerDb, "registries/reg-owner-pending")));
  });

  it("D-19: invitee promoted to invitedUsers (post-accept) CAN read", async () => {
    await setDoc(adminDb, "registries/reg-accepted", {
      ownerId: "owner-1",
      visibility: "private",
      invitedUsers: { "accepted-uid": true },
      pendingInvitedUsers: {},
    });
    const acceptedDb = testEnv.authenticatedContext("accepted-uid").firestore();
    await assertSucceeds(getDoc(doc(acceptedDb, "registries/reg-accepted")));
  });
});
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Cloud Functions 1st gen | 2nd gen (`firebase-functions/v2/*`) | repo standard since Phase 04 | Phase 16 follows — all new callables use `firebase-functions/v2/https` |
| `firebase.firestore.FieldPath` (admin) | `import { FieldPath, FieldValue } from "firebase-admin/firestore"` | firebase-admin@13 | Phase 16 MUST use subpath import (Pitfall 5) |
| Firebase Realtime Database for any persistence | Firestore (already locked by CLAUDE.md) | project inception | N/A — Firestore only |
| Anonymous auth UIDs added to invitedUsers | Email-keyed pending + magic-link sign-in UID swap | Phase 15 (parked) + Phase 16 (this phase) | Together establish the new invite contract |
| Inbox card auto-marks-read instantly | 500ms-debounced batched mark-as-read | Phase 06 quick task | Preserved in Phase 16 re-skin (D-09 doesn't change behavior) |

**Deprecated/outdated:**
- Direct writes to `invitedUsers` from `inviteToRegistry`: deprecated by D-23 (writes go to `pendingInvitedUsers` from Phase 16 forward; existing entries grandfathered).
- Hardcoded "tap card → navigate to registry" for INVITE notifications: deprecated by D-11 (now branches on `pendingEntryKey` presence).

## Open Questions

1. **Does `FirebaseModule` (Hilt) already expose `FirebaseFunctions` to the notification data layer?**
   - What we know: `ReservationRepositoryImpl` `@Inject`s `FirebaseFunctions` so the binding exists somewhere.
   - What's unclear: Whether `NotificationRepositoryImpl` constructor can add the dep without a new Hilt module entry. The current `NotificationRepositoryImpl` only takes `FirebaseFirestore`.
   - Recommendation: Plan a small Wave 1 prerequisite to verify the existing `FirebaseFunctions` Hilt provider is accessible from the notifications package; if not, add a `@Provides` entry. Trivial change.

2. **Is there an existing `LocalizationParityTest` that enforces strings.xml ↔ values-ro/strings.xml key parity?**
   - What we know: D-28 requires both locales to have new keys; manual diff is error-prone.
   - What's unclear: Whether prior phases established an automated parity test.
   - Recommendation: Grep `app/src/test/java` for "LocalizationParity" / "strings" before Wave 0; if not present, add a simple parity unit test once and bank it for future phases.

3. **Should the FCM data payload extension (Claude's discretion) ship in Phase 16 or be deferred?**
   - What we know: Pitfall 6 — keeping payload minimal is safer.
   - What's unclear: Whether system-tray-tap-to-sheet UX (deep-link straight to sheet rather than inbox→tap→sheet) is a UAT requirement.
   - Recommendation: Ship Phase 16 with minimal FCM data payload (`{ type: "invite", registryId }` unchanged). Inbox-doc carries the enriched fields. If UAT demands faster path, add as v1.2 polish item with proper FCM size-budget testing.

4. **Composite index deployment timing.**
   - What we know: Pitfall 7 — query without index throws.
   - What's unclear: Whether `firebase deploy --only firestore:indexes` ahead of function deploy is workflow-supported (deploy ordering).
   - Recommendation: Wave 0 task — add to `firestore.indexes.json`, deploy indexes first, then deploy functions. Document in plan's commit notes.

## Sources

### Primary (HIGH confidence)
- Repo file: `/Users/victorpop/ai-projects/gift-registry/functions/src/reservation/confirmPurchase.ts` — verbatim callable + transaction shape
- Repo file: `/Users/victorpop/ai-projects/gift-registry/functions/src/registry/inviteToRegistry.ts` — FieldPath pattern + dot-in-email pitfall comment
- Repo file: `/Users/victorpop/ai-projects/gift-registry/functions/src/notifications/writeNotification.ts` — inbox-write helper
- Repo file: `/Users/victorpop/ai-projects/gift-registry/functions/src/notifications/invitePush.ts` — FCM push pattern + stale token cleanup
- Repo file: `/Users/victorpop/ai-projects/gift-registry/app/src/main/java/com/giftregistry/data/reservation/ReservationRepositoryImpl.kt` — verbatim httpsCallable wrapper pattern
- Repo file: `/Users/victorpop/ai-projects/gift-registry/app/src/main/java/com/giftregistry/data/notifications/NotificationRepositoryImpl.kt` — repository to extend
- Repo file: `/Users/victorpop/ai-projects/gift-registry/app/src/main/java/com/giftregistry/domain/model/Notification.kt` — enum + payload shape
- Repo file: `/Users/victorpop/ai-projects/gift-registry/app/src/main/java/com/giftregistry/ui/registry/invite/InviteBottomSheet.kt` — reference ModalBottomSheet shape
- Repo file: `/Users/victorpop/ai-projects/gift-registry/app/src/main/java/com/giftregistry/ui/registry/cover/HeroImageOrPlaceholder.kt` — reusable hero composable
- Repo file: `/Users/victorpop/ai-projects/gift-registry/app/src/main/java/com/giftregistry/ui/auth/AuthScreen.kt:274-286` — verbatim warn-banner pattern
- Repo file: `/Users/victorpop/ai-projects/gift-registry/firestore.rules` — existing rule set; `isInvited` definition
- Repo file: `/Users/victorpop/ai-projects/gift-registry/tests/rules/firestore.rules.test.ts` — extend with new suites
- Repo file: `/Users/victorpop/ai-projects/gift-registry/gradle/libs.versions.toml` — verified versions
- Repo file: `/Users/victorpop/ai-projects/gift-registry/functions/package.json` — verified Node deps
- npm registry queries (2026-05-24): `firebase-functions@7.2.5`, `firebase-admin@13.10.0`, `@google-cloud/tasks@6.2.2` confirmed current

### Secondary (MEDIUM confidence — WebFetch verified)
- [Android Developers — Bottom sheets in Compose](https://developer.android.com/develop/ui/compose/components/bottom-sheets) — `confirmValueChange` for dismissal guard
- [Firebase Cloud Messaging — Message types](https://firebase.google.com/docs/cloud-messaging/customize-messages/set-message-type) — 4096-byte payload cap
- [Compose Material 3 release notes](https://developer.android.com/jetpack/androidx/releases/compose-material3) — ModalBottomSheet API surface

### Tertiary (LOW confidence — single-source / not directly verified)
- WebSearch result for Firebase Admin `FieldValue.delete()` on nested map keys — confirmed dot-notation pattern but not directly verified for FieldPath + transaction combination. Mitigation: pattern is established in `inviteToRegistry.ts` for the write path; the FieldValue.delete in a tx is symmetric and tested against the local emulator by the Wave 0 RED suite.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — every primitive already in repo, versions verified against current files
- Architecture: HIGH — verbatim patterns from working, tested code (`confirmPurchase.ts`, `ReservationRepositoryImpl`, `InviteBottomSheet`, `AuthScreen` warn-banner)
- Pitfalls: HIGH — Pitfalls 1, 2, 4, 5 grounded in existing repo evidence; Pitfalls 3, 6, 7, 8 grounded in documented Firebase semantics
- Backend transaction shape: HIGH — `confirmPurchase.ts` test harness directly portable to acceptInvite/declineInvite
- Material3 ModalBottomSheet dismissal guard: MEDIUM-HIGH — verified against Android Developers docs
- FCM payload size: HIGH — verified against official FCM docs

**Research date:** 2026-05-24
**Valid until:** 2026-06-23 (30 days — repo stack is stable; Firebase BoM and Compose BoM ship quarterly; phase work should start within this window)
