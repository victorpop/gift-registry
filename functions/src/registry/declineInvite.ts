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
