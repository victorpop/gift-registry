import * as admin from "firebase-admin";

/**
 * Best-effort batch-delete of INVITE inbox notification doc(s) for a (user, registry) pair.
 * Used by acceptInvite + declineInvite to clean up the actionable inbox card after the
 * user has responded. Never throws — clients tolerate orphan inbox docs (Phase 06 D-06).
 *
 * Requires composite index on users/*\/notifications: (type asc, payload.registryId asc)
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
 * eventDateMs is returned as a number (Firestore stores it natively); the
 * Android client coerces to String when reading from the payload Map. The
 * writeNotification signature accepts string|number|boolean|null payload
 * values, so this round-trips cleanly to Firestore.
 *
 * description is included so the InviteResponseSheet can render it below the
 * registry name without a registry-doc read (denied pre-accept).
 */
export function buildEnrichedInvitePayload(
  registryData: FirebaseFirestore.DocumentData,
  inviteKey: string,
): Record<string, string | number | null> {
  const eventAt = registryData.eventAt as FirebaseFirestore.Timestamp | undefined;
  return {
    pendingEntryKey: inviteKey,
    occasion: (registryData.occasion as string | undefined) ?? null,
    coverUrl: (registryData.imageUrl as string | undefined) ?? null,
    eventDateMs: eventAt ? eventAt.toMillis() : null,
    description: (registryData.description as string | undefined) ?? null,
  };
}
