---
phase: 16-android-notifications-inbox-invite-accept-decline
plan: 02
type: execute
wave: 2
depends_on:
  - 16-01-wave-0-red-tests-and-index
files_modified:
  - functions/src/registry/acceptInvite.ts
  - functions/src/registry/declineInvite.ts
  - functions/src/registry/inviteToRegistry.ts
  - functions/src/registry/inviteNotificationHelpers.ts
  - functions/src/index.ts
autonomous: true
requirements:
  - D-10
  - D-13
  - D-15
  - D-16
  - D-18
  - D-19
  - D-20
  - D-21
  - D-22
  - D-23
  - D-24
user_setup: []

must_haves:
  truths:
    - "acceptInvite Cloud Function exists in functions/src/registry/acceptInvite.ts and is exported from functions/src/index.ts"
    - "declineInvite Cloud Function exists in functions/src/registry/declineInvite.ts and is exported from functions/src/index.ts"
    - "Both new callables use onCall with { region: 'europe-west3', enforceAppCheck: true }"
    - "acceptInvite runs a Firestore transaction that promotes uid from pendingInvitedUsers to invitedUsers atomically"
    - "acceptInvite post-tx: deletes INVITE inbox doc(s), writes invite_accepted_self for invitee, writes invite_accepted for owner"
    - "declineInvite runs a Firestore transaction that removes uid from pendingInvitedUsers"
    - "declineInvite post-tx: deletes INVITE inbox doc(s), writes invite_declined for owner"
    - "Both callables are idempotent — re-invoking after completion returns success without writes"
    - "inviteToRegistry writes to pendingInvitedUsers (NOT invitedUsers) for ALL new invites regardless of account status"
    - "inviteToRegistry includes enriched payload (pendingEntryKey, occasion, coverUrl, eventDateMs) in BOTH the inbox notification AND the FCM data payload remains minimal per Pitfall 6 recommendation"
    - "inviteToRegistry no-ops the membership write when invitee is already in invitedUsers (D-16) but still writes inbox+push"
    - "Wave 0 Jest tests for acceptInvite + declineInvite + inviteToRegistry all pass (RED → GREEN flip)"
  artifacts:
    - path: "functions/src/registry/acceptInvite.ts"
      provides: "2nd-gen onCall function for accept-gate; Admin SDK transaction"
      exports: ["acceptInvite"]
      contains: "enforceAppCheck: true"
    - path: "functions/src/registry/declineInvite.ts"
      provides: "2nd-gen onCall function for decline; Admin SDK transaction"
      exports: ["declineInvite"]
      contains: "enforceAppCheck: true"
    - path: "functions/src/registry/inviteNotificationHelpers.ts"
      provides: "Shared helpers: deleteInviteNotificationsForRegistry, lookupDisplayName, buildEnrichedInvitePayload"
      exports: ["deleteInviteNotificationsForRegistry", "lookupDisplayName", "buildEnrichedInvitePayload"]
    - path: "functions/src/registry/inviteToRegistry.ts"
      provides: "Modified — writes pendingInvitedUsers + enriched payload + D-16 already-member branch"
      contains: "pendingInvitedUsers"
    - path: "functions/src/index.ts"
      provides: "Registers acceptInvite + declineInvite exports"
      contains: "export { acceptInvite"
  key_links:
    - from: "functions/src/registry/acceptInvite.ts"
      to: "functions/src/notifications/writeNotification.ts"
      via: "Post-tx best-effort writes for invitee JOINED + owner accepted"
      pattern: "writeNotification\\("
    - from: "functions/src/registry/declineInvite.ts"
      to: "functions/src/notifications/writeNotification.ts"
      via: "Post-tx owner notification write"
      pattern: "writeNotification\\("
    - from: "functions/src/registry/inviteToRegistry.ts"
      to: "registries/{id}.pendingInvitedUsers"
      via: "FieldPath update with inviteKey (uid or email:xxx)"
      pattern: "new FieldPath\\(\"pendingInvitedUsers\""
    - from: "functions/src/index.ts"
      to: "acceptInvite + declineInvite"
      via: "Module exports for runtime registration"
      pattern: "export \\{ acceptInvite|export \\{ declineInvite"
---

<objective>
Ship the backend half of Phase 16's accept-gate model: two new 2nd-gen Cloud Function callables (`acceptInvite`, `declineInvite`) implementing the D-21/D-22 transaction semantics; modify `inviteToRegistry` to write to `pendingInvitedUsers` with the enriched payload (D-23) and to no-op the membership write for already-members while still writing inbox+push (D-16). Flip the Wave 0 RED Jest tests GREEN.

Purpose: All membership state transitions become explicit and atomic. Decouples the wire-level invite "delivery" from the user's consent act.
Output: 3 new TS files + modified inviteToRegistry.ts + modified index.ts.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/phases/16-android-notifications-inbox-invite-accept-decline/16-CONTEXT.md
@.planning/phases/16-android-notifications-inbox-invite-accept-decline/16-RESEARCH.md
@functions/src/reservation/confirmPurchase.ts
@functions/src/registry/inviteToRegistry.ts
@functions/src/notifications/writeNotification.ts
@functions/src/notifications/invitePush.ts
@functions/src/__tests__/acceptInvite.test.ts
@functions/src/__tests__/declineInvite.test.ts
@functions/src/__tests__/inviteToRegistry.test.ts
@functions/src/index.ts

<interfaces>
<!-- writeNotification signature (functions/src/notifications/writeNotification.ts) — REUSE: -->
```typescript
export interface WriteNotificationArgs {
  userId: string;
  type: string;
  titleKey: string;
  bodyKey: string;
  titleFallback: string;
  bodyFallback: string;
  payload: Record<string, string | number | boolean | null>;
}
export async function writeNotification(args: WriteNotificationArgs): Promise<void>;
```

<!-- sendInvitePush signature (functions/src/notifications/invitePush.ts) — REUSE: -->
```typescript
export interface SendInvitePushArgs {
  invitedUid: string;
  registryId: string;
  registryName: string;
  locale: "en" | "ro";
}
export async function sendInvitePush(args: SendInvitePushArgs): Promise<void>;
```

<!-- Current inviteToRegistry pattern (the bit being changed):
Line 92: `await registryRef.update(new FieldPath("invitedUsers", inviteKey), true);`
We change this to write to `pendingInvitedUsers` instead.
Line 128-141: writeNotification with payload { registryId, registryName, actorName, actorUid }.
We extend this payload with { pendingEntryKey, occasion, coverUrl, eventDateMs }. -->

<!-- Pattern from confirmPurchase.ts: onCall<Request>({ region, enforceAppCheck }, handler) -->
<!-- runTransaction with verify-first guard + FieldPath for the writes -->
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Create acceptInvite + declineInvite + shared helpers; register in index.ts</name>
  <read_first>
    - functions/src/reservation/confirmPurchase.ts (verbatim shape to mirror — onCall + transaction + post-tx writes)
    - functions/src/registry/inviteToRegistry.ts (FieldPath import pattern + dot-in-email comments — copy verbatim)
    - functions/src/notifications/writeNotification.ts (signature for the post-tx writes)
    - functions/src/__tests__/acceptInvite.test.ts (RED test from Plan 16-01 — these tests define the contract)
    - functions/src/__tests__/declineInvite.test.ts (RED test from Plan 16-01)
    - functions/src/__tests__/confirmPurchase.test.ts (test harness shape to ensure new tests match)
    - functions/src/index.ts (current exports)
    - .planning/phases/16-android-notifications-inbox-invite-accept-decline/16-RESEARCH.md (Patterns 3 + 4 + Example A — the complete acceptInvite skeleton)
  </read_first>
  <behavior>
    acceptInvite (D-21):
    - Reject if !request.auth → HttpsError("unauthenticated", "MUST_BE_SIGNED_IN")
    - Reject if !registryId || typeof registryId !== "string" → HttpsError("invalid-argument", "MISSING_REGISTRY_ID")
    - Run db.runTransaction:
      1. tx.get(registryRef); if !exists → HttpsError("not-found", "REGISTRY_NOT_FOUND")
      2. Read pendingInvitedUsers ?? {}; read invitedUsers ?? {}
      3. Idempotency (D-24): if invited[uid] === true AND pending[uid] !== true → return (no writes) — didPromote stays false
      4. If pending[uid] !== true → HttpsError("failed-precondition", "NO_PENDING_INVITE")
      5. Capture ownerId and registryName for post-tx use
      6. tx.update(registryRef, new FieldPath("pendingInvitedUsers", uid), FieldValue.delete())
      7. tx.update(registryRef, new FieldPath("invitedUsers", uid), true)
      8. didPromote = true
    - Post-tx (only if didPromote): best-effort, never throw:
      - deleteInviteNotificationsForRegistry(uid, registryId) — Pattern 4
      - writeNotification({ userId: uid, type: "invite_accepted_self", titleKey: "notification_invite_accepted_self_title", bodyKey: "notification_invite_accepted_self_body", titleFallback: `You joined "${registryName}"`, bodyFallback: `Tap to view ${registryName}`, payload: { registryId, registryName } })
      - if ownerId: writeNotification({ userId: ownerId, type: "invite_accepted", titleKey: "notification_invite_accepted_title", bodyKey: "notification_invite_accepted_body", titleFallback: `${actorName} accepted your invite to "${registryName}"`, bodyFallback: registryName, payload: { registryId, registryName, actorName, actorUid: uid } })
    - Return { success: true }

    declineInvite (D-22):
    - Same auth + arg validation as acceptInvite.
    - Run db.runTransaction:
      1. tx.get(registryRef); if !exists → HttpsError("not-found")
      2. Read pendingInvitedUsers ?? {}
      3. Idempotency (D-24): if pending[uid] !== true → HttpsError("failed-precondition", "NO_PENDING_INVITE") (declining a non-pending invite is an error since there is no "already declined" state)
      4. Capture ownerId and registryName
      5. tx.update(registryRef, new FieldPath("pendingInvitedUsers", uid), FieldValue.delete())
      6. didDecline = true
    - Post-tx (only if didDecline), best-effort:
      - deleteInviteNotificationsForRegistry(uid, registryId)
      - if ownerId: writeNotification({ userId: ownerId, type: "invite_declined", titleKey: "notification_invite_declined_title", bodyKey: "notification_invite_declined_body", titleFallback: `${actorName} declined your invite to "${registryName}"`, bodyFallback: registryName, payload: { registryId, registryName, actorName, actorUid: uid } })
    - Return { success: true }

    Shared helpers in inviteNotificationHelpers.ts (DRY):
    - `deleteInviteNotificationsForRegistry(uid, registryId)`: query users/{uid}/notifications where type=="invite" AND payload.registryId == registryId; batch delete. Try/catch — log + swallow.
    - `lookupDisplayName(uid)`: best-effort admin.auth().getUser; return displayName || email-prefix || "Someone".
    - `buildEnrichedInvitePayload(registryData, inviteKey)`: returns { pendingEntryKey: inviteKey, occasion: registryData.occasion ?? null, coverUrl: registryData.imageUrl ?? null, eventDateMs: registryData.eventAt?.toMillis()?.toString() ?? null } (eventDateMs as string for client Map<String,String?> compat; writeNotification accepts string|number|boolean|null).
  </behavior>
  <action>
    Create exactly these files.

    File 1 — functions/src/registry/inviteNotificationHelpers.ts:
    ```typescript
    import * as admin from "firebase-admin";

    /**
     * Best-effort batch-delete of INVITE inbox notification doc(s) for a (user, registry) pair.
     * Used by acceptInvite + declineInvite to clean up the actionable inbox card after the
     * user has responded. Never throws — clients tolerate orphan inbox docs (Phase 06 D-06).
     *
     * Requires composite index on users/*/notifications: (type asc, payload.registryId asc)
     * — added in firestore.indexes.json by Plan 16-01 (Pitfall 7).
     */
    export async function deleteInviteNotificationsForRegistry(
      uid: string,
      registryId: string,
    ): Promise<void> {
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
        console.error(
          `[inviteNotificationHelpers] inbox cleanup failed for ${uid}/${registryId}:`,
          err,
        );
        // Swallow — best-effort per Phase 06 D-06
      }
    }

    /**
     * Best-effort display-name lookup for a Firebase Auth uid.
     * Returns displayName, email-prefix, or "Someone" — never throws.
     */
    export async function lookupDisplayName(uid: string): Promise<string> {
      try {
        const record = await admin.auth().getUser(uid);
        return record.displayName || record.email?.split("@")[0] || "Someone";
      } catch {
        return "Someone";
      }
    }

    /**
     * D-10: Build the enriched payload embedded in invite inbox notifications.
     * The Android InviteResponseSheet renders the registry hero from these fields
     * with zero registry-doc read on the client (which would be denied pre-accept).
     *
     * All values coerced to string (or null) for Map<String, String?> client compat.
     */
    export function buildEnrichedInvitePayload(
      registryData: FirebaseFirestore.DocumentData,
      inviteKey: string,
    ): Record<string, string | null> {
      const eventAt = registryData.eventAt as FirebaseFirestore.Timestamp | undefined;
      return {
        pendingEntryKey: inviteKey,
        occasion: (registryData.occasion as string | undefined) ?? null,
        coverUrl: (registryData.imageUrl as string | undefined) ?? null,
        eventDateMs: eventAt ? String(eventAt.toMillis()) : null,
      };
    }
    ```

    File 2 — functions/src/registry/acceptInvite.ts:
    ```typescript
    import { onCall, HttpsError } from "firebase-functions/v2/https";
    import * as admin from "firebase-admin";
    import { FieldPath, FieldValue } from "firebase-admin/firestore";
    import { writeNotification } from "../notifications/writeNotification";
    import {
      deleteInviteNotificationsForRegistry,
      lookupDisplayName,
    } from "./inviteNotificationHelpers";

    interface AcceptInviteRequest { registryId: string }
    interface AcceptInviteResponse { success: boolean }

    const REGION = "europe-west3";

    /**
     * D-20/D-21 — Accept a pending registry invite.
     *
     * Atomically promotes the caller's uid from `pendingInvitedUsers` into
     * `invitedUsers` on the target registry doc. Post-tx (best-effort): deletes
     * the INVITE inbox notification(s) for this (uid, registry); writes a
     * JOINED confirmation to the invitee's inbox; writes an `invite_accepted`
     * notification to the owner's inbox.
     *
     * D-24 — Idempotent: if the uid is already in invitedUsers and not in
     * pendingInvitedUsers, returns success without writes.
     *
     * App Check enforced (Pitfall 2 — required for new callables since Phase 14
     * lesson; pre-existing callables are retrofitted via separate todo).
     */
    export const acceptInvite = onCall<AcceptInviteRequest>(
      { region: REGION, enforceAppCheck: true },
      async (request): Promise<AcceptInviteResponse> => {
        if (!request.auth) {
          throw new HttpsError("unauthenticated", "MUST_BE_SIGNED_IN");
        }
        const { registryId } = (request.data ?? {}) as AcceptInviteRequest;
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
          if (!snap.exists) {
            throw new HttpsError("not-found", "REGISTRY_NOT_FOUND");
          }
          const data = snap.data()!;
          const pending = (data.pendingInvitedUsers ?? {}) as Record<string, boolean>;
          const invited = (data.invitedUsers ?? {}) as Record<string, boolean>;

          // D-24 idempotency: already accepted, no-op success
          if (invited[uid] === true && pending[uid] !== true) {
            didPromote = false;
            return;
          }
          if (pending[uid] !== true) {
            throw new HttpsError("failed-precondition", "NO_PENDING_INVITE");
          }

          ownerId = (data.ownerId as string) ?? "";
          registryName = (data.title as string) ?? "a registry";

          // Pitfall 1 — FieldPath required for keys that may contain dots (email:xxx).
          // For uid keys it's belt-and-suspenders; for email keys it's mandatory.
          tx.update(registryRef, new FieldPath("pendingInvitedUsers", uid), FieldValue.delete());
          tx.update(registryRef, new FieldPath("invitedUsers", uid), true);
          didPromote = true;
        });

        if (!didPromote) {
          return { success: true };
        }

        // Post-tx side effects — best-effort per Phase 06 D-06
        await deleteInviteNotificationsForRegistry(uid, registryId);

        try {
          await writeNotification({
            userId: uid,
            type: "invite_accepted_self",
            titleKey: "notification_invite_accepted_self_title",
            bodyKey: "notification_invite_accepted_self_body",
            titleFallback: `You joined "${registryName}"`,
            bodyFallback: `Tap to view ${registryName}`,
            payload: { registryId, registryName },
          });
        } catch (err) {
          console.error("[acceptInvite] invitee notification failed:", err);
        }

        if (ownerId) {
          try {
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
          } catch (err) {
            console.error("[acceptInvite] owner notification failed:", err);
          }
        }

        return { success: true };
      },
    );
    ```

    File 3 — functions/src/registry/declineInvite.ts:
    ```typescript
    import { onCall, HttpsError } from "firebase-functions/v2/https";
    import * as admin from "firebase-admin";
    import { FieldPath, FieldValue } from "firebase-admin/firestore";
    import { writeNotification } from "../notifications/writeNotification";
    import {
      deleteInviteNotificationsForRegistry,
      lookupDisplayName,
    } from "./inviteNotificationHelpers";

    interface DeclineInviteRequest { registryId: string }
    interface DeclineInviteResponse { success: boolean }

    const REGION = "europe-west3";

    /**
     * D-20/D-22 — Decline a pending registry invite.
     *
     * Atomically removes the caller's uid from `pendingInvitedUsers`. Post-tx
     * (best-effort): deletes the INVITE inbox notification(s); writes an
     * `invite_declined` notification to the owner's inbox.
     *
     * No JOINED notification to the invitee — decline is silent on their side
     * (the inbox card simply disappears via snapshot listener).
     *
     * D-24 — Idempotent on a no-pending-entry case: returns failed-precondition
     * (declining a non-pending invite is a meaningful client error, distinct
     * from accepting an already-accepted invite which is success).
     */
    export const declineInvite = onCall<DeclineInviteRequest>(
      { region: REGION, enforceAppCheck: true },
      async (request): Promise<DeclineInviteResponse> => {
        if (!request.auth) {
          throw new HttpsError("unauthenticated", "MUST_BE_SIGNED_IN");
        }
        const { registryId } = (request.data ?? {}) as DeclineInviteRequest;
        if (!registryId || typeof registryId !== "string") {
          throw new HttpsError("invalid-argument", "MISSING_REGISTRY_ID");
        }

        const db = admin.firestore();
        const uid = request.auth.uid;
        const registryRef = db.collection("registries").doc(registryId);

        let ownerId = "";
        let registryName = "a registry";
        let didDecline = false;

        await db.runTransaction(async (tx) => {
          const snap = await tx.get(registryRef);
          if (!snap.exists) {
            throw new HttpsError("not-found", "REGISTRY_NOT_FOUND");
          }
          const data = snap.data()!;
          const pending = (data.pendingInvitedUsers ?? {}) as Record<string, boolean>;

          if (pending[uid] !== true) {
            throw new HttpsError("failed-precondition", "NO_PENDING_INVITE");
          }

          ownerId = (data.ownerId as string) ?? "";
          registryName = (data.title as string) ?? "a registry";

          tx.update(
            registryRef,
            new FieldPath("pendingInvitedUsers", uid),
            FieldValue.delete(),
          );
          didDecline = true;
        });

        if (!didDecline) {
          return { success: true };
        }

        await deleteInviteNotificationsForRegistry(uid, registryId);

        if (ownerId) {
          try {
            const actorName = await lookupDisplayName(uid);
            await writeNotification({
              userId: ownerId,
              type: "invite_declined",
              titleKey: "notification_invite_declined_title",
              bodyKey: "notification_invite_declined_body",
              titleFallback: `${actorName} declined your invite to "${registryName}"`,
              bodyFallback: registryName,
              payload: { registryId, registryName, actorName, actorUid: uid },
            });
          } catch (err) {
            console.error("[declineInvite] owner notification failed:", err);
          }
        }

        return { success: true };
      },
    );
    ```

    File 4 — MODIFY functions/src/index.ts. After the existing `confirmPurchase` export line, add exactly:
    ```typescript
    export { acceptInvite } from "./registry/acceptInvite";
    export { declineInvite } from "./registry/declineInvite";
    ```
    Preserve all existing exports verbatim.
  </action>
  <verify>
    <automated>cd functions && npm test -- acceptInvite declineInvite 2>&1 | tail -30</automated>
  </verify>
  <acceptance_criteria>
    - File functions/src/registry/acceptInvite.ts exists
    - acceptInvite.ts contains string "enforceAppCheck: true"
    - acceptInvite.ts contains string "europe-west3"
    - acceptInvite.ts contains string "new FieldPath(\"pendingInvitedUsers\""
    - acceptInvite.ts contains string "new FieldPath(\"invitedUsers\""
    - acceptInvite.ts contains string "FieldValue.delete()"
    - acceptInvite.ts contains string "NO_PENDING_INVITE"
    - acceptInvite.ts contains string "invite_accepted_self"
    - acceptInvite.ts contains string "invite_accepted"
    - acceptInvite.ts contains string "didPromote"
    - File functions/src/registry/declineInvite.ts exists
    - declineInvite.ts contains string "enforceAppCheck: true"
    - declineInvite.ts contains string "invite_declined"
    - declineInvite.ts contains string "FieldValue.delete()"
    - declineInvite.ts does NOT contain string "invite_accepted" (decline doesn't write accept notifications)
    - File functions/src/registry/inviteNotificationHelpers.ts exists
    - inviteNotificationHelpers.ts contains string "deleteInviteNotificationsForRegistry"
    - inviteNotificationHelpers.ts contains string "lookupDisplayName"
    - inviteNotificationHelpers.ts contains string "buildEnrichedInvitePayload"
    - inviteNotificationHelpers.ts contains string "payload.registryId"
    - functions/src/index.ts contains string "export { acceptInvite }"
    - functions/src/index.ts contains string "export { declineInvite }"
    - cd functions && npx tsc --noEmit exits 0
    - cd functions && npm test -- acceptInvite exits 0 (Jest tests flip RED → GREEN)
    - cd functions && npm test -- declineInvite exits 0
  </acceptance_criteria>
  <done>acceptInvite + declineInvite + shared helpers exist and exported; their Jest tests pass.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Modify inviteToRegistry to write pendingInvitedUsers + enriched payload + D-16 already-member branch</name>
  <read_first>
    - functions/src/registry/inviteToRegistry.ts (current implementation — note lines 75-92 for the write, lines 116-142 for the push + inbox write)
    - functions/src/__tests__/inviteToRegistry.test.ts (RED tests modified in Plan 16-01 — these define the new expected behavior)
    - functions/src/registry/inviteNotificationHelpers.ts (use buildEnrichedInvitePayload helper from Task 1)
    - functions/src/notifications/invitePush.ts (FCM push — Pitfall 6 says do NOT extend its data payload; only inbox notification payload gets the enriched fields)
    - .planning/phases/16-android-notifications-inbox-invite-accept-decline/16-CONTEXT.md (D-23 + D-16)
    - .planning/phases/16-android-notifications-inbox-invite-accept-decline/16-RESEARCH.md (Pitfall 6 recommendation)
  </read_first>
  <behavior>
    1. ALL new invites write to `pendingInvitedUsers` instead of `invitedUsers` (D-23). Use the same inviteKey logic (uid if existing user, else `email:${email}`) and the same FieldPath pattern.
    2. D-16 already-member branch: BEFORE the FieldPath update, check if the inviteKey is already in invitedUsers. If yes:
       - SKIP the membership write (no pendingInvitedUsers write either).
       - STILL send the email (REG-07 contract — owner can re-invite).
       - STILL send the FCM push (D-16 "soft re-invitation/reminder").
       - STILL write the inbox notification with enriched payload (so the existing member sees a re-invite card; their tap opens the sheet — but the sheet will receive NO_PENDING_INVITE from acceptInvite and gracefully handle via the warn-banner path).
       - Actually — better: if already a member, the inbox notification payload should NOT carry `pendingEntryKey` (so the inbox card falls back to legacy "tap → navigate to registry" per D-11). This is the cleaner UX: existing members see "you've been re-invited" and tap to view the registry (which they already have access to).
       - Document this branch with a code comment.
    3. Enriched payload (D-10): when NOT in the already-member branch, the inbox notification payload includes pendingEntryKey, occasion, coverUrl, eventDateMs (via buildEnrichedInvitePayload helper). The existing fields (registryId, registryName, actorName, actorUid) are preserved.
    4. Non-existing-user invite (REG-07): MUST still write an inbox notification — but to where? The user has no uid yet. Per D-13 + D-14 contract: the inbox notification is written ONLY for the existing-user branch (isExistingUser=true). For non-users, only the email is sent; the inbox notification will arrive after Phase 15's signup blocking function swaps email:xxx → uid. **Phase 16 preserves the existing behavior: inbox+push only for existing users; email always.** The non-user branch still writes `pendingInvitedUsers["email:xxx"] = true` (membership pending), but no inbox doc until the user signs up.
    5. FCM push data payload (Pitfall 6 recommendation): KEEP MINIMAL — do NOT extend with enriched fields. The push remains `{ type: "invite", registryId }`. The push tap deep-links to the inbox; the sheet opens from the inbox tap (which has the full enriched payload).
  </behavior>
  <action>
    Edit functions/src/registry/inviteToRegistry.ts as follows (preserve all imports, error handling wrapper, email logic, and the comment block at lines 79-91 verbatim — they are still relevant for the FieldPath pattern):

    1. Add import at the top, after the existing imports:
    ```typescript
    import { buildEnrichedInvitePayload } from "./inviteNotificationHelpers";
    ```

    2. Replace lines 75-92 (the FieldPath update block) with:
    ```typescript
      // Determine inviteKey (uid for existing users, email:xxx for non-users).
      const inviteKey = invitedUid ?? `email:${email}`;

      // D-16: If the invitee is ALREADY a member (in invitedUsers), no-op the
      // membership write (don't touch invitedUsers, don't write pendingInvitedUsers)
      // but still send email + FCM push + inbox notification (without
      // pendingEntryKey, so the inbox card falls back to legacy "tap → navigate"
      // behaviour per D-11). This treats the re-invite as a soft reminder.
      const existingInvitedUsers = (registryData.invitedUsers ?? {}) as Record<string, boolean>;
      const isAlreadyMember = existingInvitedUsers[inviteKey] === true;

      if (!isAlreadyMember) {
        // D-23: NEW invites write to pendingInvitedUsers (accept-gate model).
        // The invitee must explicitly tap Accept in the Android inbox before
        // becoming a real member (handled by the acceptInvite callable).
        //
        // IMPORTANT: pass the two path components via FieldPath instead of a single
        // dotted string key. String keys to update() are parsed by the admin SDK,
        // which splits on '.' — and real email addresses almost always contain dots
        // (e.g. "jane.doe@example.com"), which would cause the dots inside the key
        // to be interpreted as nested-field separators. That would create a tree of
        // nested maps under pendingInvitedUsers instead of a single boolean, breaking
        // both the security rules and client deserialization.
        //
        // Import FieldPath from "firebase-admin/firestore" subpath rather than
        // reaching through `admin.firestore.FieldPath`: the namespace-style access
        // is not reliably populated when using `import * as admin from "firebase-admin"`
        // with the v13 package exports, causing a runtime
        // "admin.firestore.FieldPath is not a constructor" error.
        await registryRef.update(
          new FieldPath("pendingInvitedUsers", inviteKey),
          true,
        );
      }
    ```

    3. Replace the existing-user branch (lines 116-142, the `if (isExistingUser && invitedUid) { ... }` block) with:
    ```typescript
      // D-17: existing-user invite ALSO delivers an FCM push to every token on
      // the invited user's account. D-18: non-user invite stays email-only
      // (no inbox doc until Phase 15's signup blocking function creates the uid).
      //
      // Pitfall 6: FCM data payload stays MINIMAL ({ type, registryId }) per
      // sendInvitePush's existing shape — extending with coverUrl could exceed
      // the 4096-byte cap for long emails + long Storage URLs. The enriched
      // payload lives on the inbox doc only (no size limit beyond Firestore's
      // 1MB doc cap).
      if (isExistingUser && invitedUid) {
        await sendInvitePush({
          invitedUid,
          registryId,
          registryName,
          locale,
        });

        // D-10 + D-23: inbox notification payload includes enriched fields
        // (pendingEntryKey, occasion, coverUrl, eventDateMs) so the Android
        // InviteResponseSheet can render the registry hero without a
        // registry-doc read (which would be denied pre-accept).
        //
        // D-16 already-member branch: when re-inviting an existing member,
        // OMIT pendingEntryKey so the inbox card falls back to legacy
        // "tap → navigate to registry" behaviour (D-11).
        const enriched = isAlreadyMember
          ? {} // no pendingEntryKey → legacy behaviour
          : buildEnrichedInvitePayload(registryData, inviteKey);

        await writeNotification({
          userId: invitedUid,
          type: "invite",
          titleKey: "notification_invite_title",
          bodyKey: "notification_invite_body",
          titleFallback: `${ownerName} invited you to "${registryName}"`,
          bodyFallback: `Tap to view ${registryName}`,
          payload: {
            registryId,
            registryName,
            actorName: ownerName,
            actorUid: request.auth.uid,
            ...enriched,
          },
        });
      }
    ```

    Preserve the return statement, the catch block, and all surrounding code verbatim.
  </action>
  <verify>
    <automated>cd functions && npm test -- inviteToRegistry 2>&1 | tail -30</automated>
  </verify>
  <acceptance_criteria>
    - functions/src/registry/inviteToRegistry.ts contains string "new FieldPath(\"pendingInvitedUsers\""
    - inviteToRegistry.ts no longer contains string "new FieldPath(\"invitedUsers\"" (the only FieldPath write is now to pendingInvitedUsers)
    - inviteToRegistry.ts contains string "isAlreadyMember"
    - inviteToRegistry.ts contains string "buildEnrichedInvitePayload"
    - inviteToRegistry.ts contains string "import { buildEnrichedInvitePayload }"
    - inviteToRegistry.ts contains string "...enriched"
    - inviteToRegistry.ts contains comment substring "D-16"
    - inviteToRegistry.ts contains comment substring "D-23"
    - inviteToRegistry.ts contains comment substring "Pitfall 6"
    - inviteToRegistry.ts contains string "sendInvitePush" (existing call preserved)
    - cd functions && npx tsc --noEmit exits 0
    - cd functions && npm test -- inviteToRegistry exits 0 (flips Plan 16-01 RED tests GREEN)
    - cd functions && npm test exits 0 (no regressions in other suites)
  </acceptance_criteria>
  <done>inviteToRegistry writes pending entries + enriched payload + handles already-member re-invites; full Jest suite green; no regressions.</done>
</task>

</tasks>

<verification>
- cd functions && npm test passes ALL suites (acceptInvite, declineInvite, inviteToRegistry, confirmPurchase, all others).
- cd functions && npx tsc --noEmit exits 0.
- functions/src/index.ts exports both new callables.
- No new dependencies installed.
- Existing inviteToRegistry behaviour for email + FCM push preserved (regression: email still sent, push still fires for existing users).
</verification>

<success_criteria>
- All Plan 16-01 Wave 0 RED tests for backend code flip GREEN.
- No new TypeScript errors anywhere in functions/.
- D-16 + D-23 + D-24 contractually upheld per the assertion suite.
</success_criteria>

<output>
After completion, create `.planning/phases/16-android-notifications-inbox-invite-accept-decline/16-02-SUMMARY.md` listing all 5 file changes, the test results, and confirming the rules tests + composite index remain in place from Plan 16-01.
</output>
