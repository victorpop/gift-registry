import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { FieldPath } from "firebase-admin/firestore";
import { inviteTemplate } from "../email/templates/invite";
import { sendEmail } from "../email/send";
import { sendInvitePush } from "../notifications/invitePush";

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
    try {
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
      } catch (lookupErr) {
        // User does not exist (or auth lookup blew up for another reason) —
        // fall back to email-only invite (REG-07). We log the underlying error
        // because auth-not-found is expected, but anything else indicates a
        // misconfiguration (e.g. Auth emulator not reachable from the Functions
        // emulator) that we want to surface during development.
        console.warn(
          `[inviteToRegistry] getUserByEmail(${email}) failed, falling back to email invite:`,
          lookupErr
        );
        invitedUid = null;
        isExistingUser = false;
      }

      // Update invitedUsers map on registry document.
      // If the user has an account, use their UID as the key (for security rule matching).
      // If not, store the email as key, prefixed to avoid collision with UIDs.
      //
      // IMPORTANT: pass the two path components via FieldPath instead of a single
      // dotted string key. String keys to update() are parsed by the admin SDK,
      // which splits on '.' — and real email addresses almost always contain dots
      // (e.g. "jane.doe@example.com"), which would cause the dots inside the key
      // to be interpreted as nested-field separators. That would create a tree of
      // nested maps under invitedUsers instead of a single boolean, breaking both
      // the security rules and client deserialization.
      const inviteKey = invitedUid ?? `email:${email}`;
      // Import FieldPath from the "firebase-admin/firestore" subpath rather than
      // reaching through `admin.firestore.FieldPath`: the namespace-style access
      // is not reliably populated when using `import * as admin from "firebase-admin"`
      // with the v13 package exports, causing a runtime
      // "admin.firestore.FieldPath is not a constructor" error.
      await registryRef.update(new FieldPath("invitedUsers", inviteKey), true);

      // D-16/D-18: Write invite email to mail collection (Trigger Email extension delivers)
      const registryUrl = `https://giftregistry.app/registry/${registryId}`;
      // ownerName: best-effort lookup from auth record; fall back to "Someone" if unknown
      let ownerName = "Someone";
      try {
        const ownerRecord = await admin.auth().getUser(request.auth.uid);
        ownerName = ownerRecord.displayName || ownerRecord.email?.split("@")[0] || "Someone";
      } catch (err) {
        console.warn(`[inviteToRegistry] owner lookup failed for ${request.auth.uid}:`, err);
      }
      const registryName = (registryData.title as string) ?? "a registry";
      const locale: "en" | "ro" = "en"; // D-14: invite recipient locale unknown; default en
      const { subject, html, text } = inviteTemplate(
        { ownerName, registryName, registryUrl },
        locale,
      );
      try {
        await sendEmail({ to: email, subject, html, text });
      } catch (err) {
        console.error(`[inviteToRegistry] sendEmail failed for invite to ${email}:`, err);
      }

      // D-17: existing-user invite ALSO delivers an FCM push to every token on
      // the invited user's account. D-18: non-user invite stays email-only.
      if (isExistingUser && invitedUid) {
        await sendInvitePush({
          invitedUid,
          registryId,
          registryName,
          locale,
        });
      }

      return {
        success: true,
        isExistingUser,
        message: isExistingUser
          ? "Invitation sent to existing user"
          : "Invitation email will be sent",
      };
    } catch (err) {
      // Rethrow HttpsError as-is (it already has a proper code + client message).
      if (err instanceof HttpsError) throw err;
      // Anything else becomes a logged INTERNAL with the real error details
      // propagated to the client so the Android side can surface a useful
      // message instead of a bare "INTERNAL".
      const message = err instanceof Error ? err.message : String(err);
      console.error("[inviteToRegistry] unhandled error", err);
      throw new HttpsError("internal", message, {
        stack: err instanceof Error ? err.stack : undefined,
      });
    }
  }
);
