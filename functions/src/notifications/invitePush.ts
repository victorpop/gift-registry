/**
 * D-17: FCM push helper for existing-user registry invites.
 * Delivers a push notification to every token on the invited user's account.
 * Best-effort: failures are logged to notifications_failures and swallowed.
 * D-18: Caller MUST guard with isExistingUser === true; this helper does not
 * re-check — non-user invites should not call in.
 */

import * as admin from "firebase-admin";
import { FieldValue } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";

type Locale = "en" | "ro";

interface SendInvitePushParams {
  invitedUid: string;
  registryId: string;
  registryName: string;
  locale: Locale;
}

export async function sendInvitePush(params: SendInvitePushParams): Promise<void> {
  const { invitedUid, registryId, registryName, locale } = params;
  const db = admin.firestore();

  try {
    const tokensSnap = await db.collection(`users/${invitedUid}/fcmTokens`).get();
    const tokens: string[] = tokensSnap.docs
      .map((d) => (d.data().token as string) ?? "")
      .filter(Boolean);

    if (tokens.length === 0) {
      return; // nothing to send; not an error
    }

    const title =
      locale === "ro"
        ? "Ai fost invitat la o listă"
        : "You've been invited to a registry";
    const body = registryName;

    const response = await getMessaging().sendEachForMulticast({
      tokens,
      notification: { title, body },
      data: { type: "invite", registryId },
      android: { priority: "normal" },
    });

    // Stale-token cleanup (mirrors Plan 06-02 pattern)
    const staleTokens: string[] = [];
    response.responses.forEach((r, idx) => {
      if (!r.success) {
        const code = r.error?.code;
        if (
          code === "messaging/registration-token-not-registered" ||
          code === "messaging/invalid-registration-token"
        ) {
          staleTokens.push(tokens[idx]);
        }
      }
    });

    if (staleTokens.length > 0) {
      const batch = db.batch();
      staleTokens.forEach((t) =>
        batch.delete(db.doc(`users/${invitedUid}/fcmTokens/${t}`))
      );
      try {
        await batch.commit();
      } catch (cleanupErr) {
        await logInviteFailure(db, invitedUid, registryId, cleanupErr);
      }
    }
  } catch (err) {
    await logInviteFailure(db, invitedUid, registryId, err);
  }
}

async function logInviteFailure(
  db: FirebaseFirestore.Firestore,
  userId: string,
  registryId: string,
  error: unknown
): Promise<void> {
  try {
    await db.collection("notifications_failures").add({
      type: "invite_push",
      userId,
      registryId,
      error: error instanceof Error ? error.message : String(error),
      timestamp: FieldValue.serverTimestamp(),
    });
  } catch (loggingErr) {
    console.error("[sendInvitePush] failed to log failure:", loggingErr);
  }
}
