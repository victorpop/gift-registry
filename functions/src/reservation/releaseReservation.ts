import { onTaskDispatched } from "firebase-functions/v2/tasks";
import * as admin from "firebase-admin";
import { FieldValue, Timestamp } from "firebase-admin/firestore";
import { expiryTemplate } from "../email/templates/expiry";
import { sendEmail } from "../email/send";
import { buildReReserveUrl } from "../config/publicUrls";
import { writeNotification } from "../notifications/writeNotification";

interface ReleasePayload { reservationId: string; }

const REGION = "europe-west3";

export const releaseReservation = onTaskDispatched<ReleasePayload>(
  {
    region: REGION,
    retryConfig: { maxAttempts: 3, minBackoffSeconds: 10 },
    rateLimits: { maxConcurrentDispatches: 20 },
    minInstances: 0,
    timeoutSeconds: 60,
  },
  async (req) => {
    const { reservationId } = req.data;
    if (!reservationId) {
      console.warn("[releaseReservation] missing reservationId; no-op");
      return;
    }

    const db = admin.firestore();
    const reservationRef = db.collection("reservations").doc(reservationId);

    let emailData: {
      giverEmail: string;
      reservationId: string;
      itemName: string;
      registryName: string;
      registryId: string;
      itemId: string;
      giverId: string | null;
      ownerId: string | null;
    } | null = null;

    await db.runTransaction(async (tx) => {
      const snap = await tx.get(reservationRef);
      if (!snap.exists) {
        console.info(`[releaseReservation] reservation ${reservationId} not found; no-op`);
        return;
      }
      const data = snap.data()!;

      if (data.status !== "active") {
        console.info(`[releaseReservation] reservation ${reservationId} already ${data.status}; no-op`);
        return;
      }

      const nowSeconds = Timestamp.now().seconds;
      const expiresAtSeconds = (data.expiresAt as Timestamp).seconds;
      if (nowSeconds < expiresAtSeconds) {
        console.info(`[releaseReservation] reservation ${reservationId} not yet expired; no-op`);
        return;
      }

      const txRegistryId = data.registryId as string;
      const txItemId = data.itemId as string;

      const itemRef = db
        .collection("registries").doc(txRegistryId)
        .collection("items").doc(txItemId);

      // Read item and registry inside transaction to capture names for email
      const itemSnap = await tx.get(itemRef);
      const itemName = (itemSnap.data()?.title as string) ?? "your gift";

      const registryRef = db.collection("registries").doc(txRegistryId);
      const registrySnap = await tx.get(registryRef);
      const registryName = (registrySnap.data()?.title as string) ?? "a registry";
      const ownerId = (registrySnap.data()?.ownerId as string) ?? null;

      tx.update(itemRef, {
        status: "available",
        reservedBy: FieldValue.delete(),
        reservedAt: FieldValue.delete(),
        expiresAt: FieldValue.delete(),
      });
      tx.update(reservationRef, { status: "expired" });

      emailData = {
        giverEmail: data.giverEmail as string,
        reservationId,
        itemName,
        registryName,
        registryId: txRegistryId,
        itemId: txItemId,
        giverId: (data.giverId as string) ?? null,
        ownerId,
      };
    });

    if (emailData) {
      const {
        giverEmail, reservationId: rid, itemName, registryName,
        registryId, itemId, giverId, ownerId,
      } = emailData;
      const reReserveUrl = buildReReserveUrl(rid);
      const { subject, html, text } = expiryTemplate(
        { itemName, registryName, reReserveUrl },
        "en" // givers don't have preferredLocale stored on reservation; default en per D-14 fallback
      );
      try {
        await sendEmail({ to: giverEmail, subject, html, text });
      } catch (err) {
        console.error(`[releaseReservation] sendEmail failed for reservation ${rid}:`, err);
      }

      // Owner-side reservation_expired notification
      if (ownerId) {
        await writeNotification({
          userId: ownerId,
          type: "reservation_expired",
          titleKey: "notification_reservation_expired_title",
          bodyKey: "notification_reservation_expired_body",
          titleFallback: `Reservation on "${itemName}" expired`,
          bodyFallback: `The 30-minute reservation on "${itemName}" in "${registryName}" expired`,
          payload: { registryId, itemId, reservationId: rid, registryName, itemName },
        });
      }

      // Giver-side re_reserve_window notification — only for signed-in givers.
      // Guest givers (giverId == null) have no account so no inbox entry.
      if (giverId) {
        await writeNotification({
          userId: giverId,
          type: "re_reserve_window",
          titleKey: "notification_re_reserve_window_title",
          bodyKey: "notification_re_reserve_window_body",
          titleFallback: `"${itemName}" is available again`,
          bodyFallback: `Your reservation expired — "${itemName}" is available to re-reserve in "${registryName}"`,
          payload: { registryId, itemId, reservationId: rid, registryName, itemName },
        });
      }
    }
  }
);
