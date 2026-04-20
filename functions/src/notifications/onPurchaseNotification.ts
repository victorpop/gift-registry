/**
 * onPurchaseNotification — Firestore trigger on items doc update (D-04).
 *
 * Fires ONLY when item status transitions from "reserved" → "purchased".
 *
 * Guards:
 *  1. Transition check: before.status === "reserved" && after.status === "purchased"
 *  2. Early-exit if after.purchaseNotificationSentAt already set (Pitfall 3: own sentinel write)
 *  3. Idempotency: claim sentinel in a transaction; __ALREADY_CLAIMED__ if lost race
 *
 * After transaction:
 *  - Read registry for ownerId + notifyOnPurchase (D-13)
 *  - If opted out: log + return
 *  - Read owner preferredLocale (D-14); default "en"
 *  - Fan out FCM via sendEachForMulticast (D-10); delete stale tokens (D-11)
 *  - Write purchase email via sendEmail + purchaseTemplate (NOTF-02)
 *  - Log any errors to notifications_failures (D-15); never rethrow
 */

import { onDocumentUpdated } from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";
import { FieldValue } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { sendEmail } from "../email/send";
import { purchaseTemplate } from "../email/templates/purchase";
import { buildRegistryUrl } from "../config/publicUrls";
import { writeNotification } from "./writeNotification";

const REGION = "europe-west3";

type Locale = "en" | "ro";

function normalizeLocale(value: unknown): Locale {
  return value === "ro" ? "ro" : "en";
}

async function logFailure(
  db: FirebaseFirestore.Firestore,
  params: {
    type: "fcm_batch" | "email_send" | "fcm_cleanup" | "read_error";
    userId: string;
    itemId: string;
    registryId: string;
    error: unknown;
  }
): Promise<void> {
  try {
    await db.collection("notifications_failures").add({
      type: params.type,
      userId: params.userId,
      itemId: params.itemId,
      registryId: params.registryId,
      error:
        params.error instanceof Error ? params.error.message : String(params.error),
      timestamp: FieldValue.serverTimestamp(),
    });
  } catch (loggingErr) {
    console.error("[onPurchaseNotification] failed to log failure:", loggingErr);
  }
}

export const onPurchaseNotification = onDocumentUpdated(
  { document: "registries/{registryId}/items/{itemId}", region: REGION },
  async (event) => {
    const before = event.data?.before.data();
    const after = event.data?.after.data();
    const { registryId, itemId } = event.params;

    if (!before || !after) return;

    // Guard 1: only fire on reserved → purchased transition (D-04)
    if (before.status !== "reserved" || after.status !== "purchased") return;

    // Guard 2: bail early if our own sentinel write re-triggered us (Pitfall 3)
    if (after.purchaseNotificationSentAt) {
      console.info(`[NOTF] Already sent for item ${itemId}; skipping`);
      return;
    }

    const db = admin.firestore();
    const itemRef = event.data!.after.ref;

    // Guard 3: claim notification slot atomically — idempotency sentinel write
    try {
      await db.runTransaction(async (tx) => {
        const snap = await tx.get(itemRef);
        if (snap.data()?.purchaseNotificationSentAt) {
          // Lost the race to another concurrent invocation; signal skip
          throw new Error("__ALREADY_CLAIMED__");
        }
        tx.update(itemRef, { purchaseNotificationSentAt: FieldValue.serverTimestamp() });
      });
    } catch (err: unknown) {
      if ((err as Error).message === "__ALREADY_CLAIMED__") return;
      console.error("[NOTF] Failed to claim sentinel:", err);
      return;
    }

    // Read registry: ownerId + opt-in flag (D-13)
    const registrySnap = await db.collection("registries").doc(registryId).get();
    if (!registrySnap.exists) {
      console.warn(`[NOTF] Registry ${registryId} missing; skipping`);
      return;
    }
    const registry = registrySnap.data()!;
    const ownerUid = registry.ownerId as string;
    const notifyOnPurchase = registry.notifyOnPurchase !== false; // default true when field absent

    if (!notifyOnPurchase) {
      console.info(`[NOTF] Skipped for owner ${ownerUid} — opted out`);
      return;
    }

    const registryName = (registry.title as string) ?? "your registry";
    const itemName = (after.title as string) ?? "a gift";
    const registryUrl = buildRegistryUrl(registryId);

    // Read owner preferredLocale + email (D-14)
    let locale: Locale = "en";
    let ownerEmail: string | null = null;
    try {
      const ownerSnap = await db.collection("users").doc(ownerUid).get();
      locale = normalizeLocale(ownerSnap.data()?.preferredLocale);
      ownerEmail = (ownerSnap.data()?.email as string) ?? null;
    } catch (err) {
      await logFailure(db, {
        type: "read_error",
        userId: ownerUid,
        itemId,
        registryId,
        error: err,
      });
    }

    // Fallback: look up email from admin.auth if user doc missing email
    if (!ownerEmail) {
      try {
        const authRecord = await admin.auth().getUser(ownerUid);
        ownerEmail = authRecord.email ?? null;
      } catch (err) {
        await logFailure(db, {
          type: "read_error",
          userId: ownerUid,
          itemId,
          registryId,
          error: err,
        });
      }
    }

    // Resolve giver name from the most recent purchased reservation for this item
    let giverFirstName: string | null = null;
    let giverLastName: string | null = null;
    try {
      const resSnap = await db
        .collection("reservations")
        .where("itemId", "==", itemId)
        .where("status", "==", "purchased")
        .orderBy("createdAt", "desc")
        .limit(1)
        .get();
      if (!resSnap.empty) {
        const res = resSnap.docs[0].data();
        const fullName = (res.giverName as string) ?? "";
        const parts = fullName.trim().split(/\s+/);
        giverFirstName = parts[0] || null;
        giverLastName = parts.slice(1).join(" ") || null;
      }
    } catch (err) {
      // Non-fatal: purchaseTemplate handles null names with graceful fallback
      console.warn(`[NOTF] Giver lookup failed for item ${itemId}:`, err);
    }

    // FCM fanout (D-10, D-11)
    const tokenDocs = await db.collection(`users/${ownerUid}/fcmTokens`).get();
    const tokens: string[] = tokenDocs.docs
      .map((d) => (d.data().token as string) ?? "")
      .filter(Boolean);

    if (tokens.length > 0) {
      const title =
        locale === "ro"
          ? "Cadoul tău a fost cumpărat! 🎁"
          : "Your gift was purchased! 🎁";
      const bodyText =
        locale === "ro"
          ? `Cineva a cumpărat un cadou din ${registryName}`
          : `Someone bought an item from ${registryName}`;

      try {
        const response = await getMessaging().sendEachForMulticast({
          tokens,
          notification: { title, body: bodyText },
          data: { registryId, itemId, type: "purchase" },
          android: { priority: "normal" },
        });

        // Stale token cleanup (D-11, Pattern 2)
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
            batch.delete(db.doc(`users/${ownerUid}/fcmTokens/${t}`))
          );
          try {
            await batch.commit();
          } catch (err) {
            await logFailure(db, {
              type: "fcm_cleanup",
              userId: ownerUid,
              itemId,
              registryId,
              error: err,
            });
          }
        }
      } catch (err) {
        await logFailure(db, {
          type: "fcm_batch",
          userId: ownerUid,
          itemId,
          registryId,
          error: err,
        });
      }
    }

    // Email fanout (D-14, NOTF-02)
    if (ownerEmail) {
      try {
        const { subject, html, text } = purchaseTemplate(
          { giverFirstName, giverLastName, itemName, registryName, registryUrl },
          locale
        );
        await sendEmail({ to: ownerEmail, subject, html, text });
      } catch (err) {
        await logFailure(db, {
          type: "email_send",
          userId: ownerUid,
          itemId,
          registryId,
          error: err,
        });
      }
    }

    // Persistent inbox notification — owner-side item_purchased (best-effort, after email).
    const giverDisplayName =
      [giverFirstName, giverLastName].filter(Boolean).join(" ") || null;
    await writeNotification({
      userId: ownerUid,
      type: "item_purchased",
      titleKey: "notification_item_purchased_title",
      bodyKey: "notification_item_purchased_body",
      titleFallback: `"${itemName}" was purchased`,
      bodyFallback: `Someone bought "${itemName}" from "${registryName}"`,
      payload: {
        registryId,
        itemId,
        registryName,
        itemName,
        actorName: giverDisplayName,
      },
    });
  }
);
