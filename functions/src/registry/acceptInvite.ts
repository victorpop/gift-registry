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
