import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";

interface InviteRequest {
  registryId: string;
  email: string;
}

interface InviteResponse {
  success: boolean;
  isExistingUser: boolean;
  message: string;
}

export const inviteToRegistry = onCall(
  { region: "europe-west3" },
  async (request): Promise<InviteResponse> => {
    // Require authentication
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Must be signed in to invite users");
    }

    const { registryId, email } = request.data as InviteRequest;

    if (!registryId || typeof registryId !== "string") {
      throw new HttpsError("invalid-argument", "registryId is required");
    }
    if (!email || typeof email !== "string") {
      throw new HttpsError("invalid-argument", "email is required");
    }

    const db = admin.firestore();
    const registryRef = db.collection("registries").doc(registryId);
    const registrySnap = await registryRef.get();

    if (!registrySnap.exists) {
      throw new HttpsError("not-found", "Registry not found");
    }

    const registryData = registrySnap.data()!;

    // Verify caller is the owner
    if (registryData.ownerId !== request.auth.uid) {
      throw new HttpsError("permission-denied", "Only the registry owner can invite users");
    }

    // Check if the email belongs to an existing user
    let invitedUid: string | null = null;
    let isExistingUser = false;
    try {
      const userRecord = await admin.auth().getUserByEmail(email);
      invitedUid = userRecord.uid;
      isExistingUser = true;
    } catch {
      // User does not exist — email-only invite (REG-07)
      invitedUid = null;
      isExistingUser = false;
    }

    // Update invitedUsers map on registry document
    // If the user has an account, use their UID as the key (for security rule matching)
    // If not, store email as key (prefixed to avoid collision with UIDs)
    const inviteKey = invitedUid ?? `email:${email}`;
    await registryRef.update({
      [`invitedUsers.${inviteKey}`]: true,
    });

    // STUB: Email sending — Phase 6 will implement actual email delivery
    // For REG-06 (existing user): would send in-app notification + email
    // For REG-07 (non-user): would send email-only with registry link
    console.log(`[STUB] Invite email would be sent to ${email} for registry ${registryId}. isExistingUser=${isExistingUser}`);

    return {
      success: true,
      isExistingUser,
      message: isExistingUser
        ? "Invitation sent to existing user"
        : "Invitation email will be sent",
    };
  }
);
