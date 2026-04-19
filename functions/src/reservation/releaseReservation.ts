import { onTaskDispatched } from "firebase-functions/v2/tasks";
import * as admin from "firebase-admin";
import { FieldValue, Timestamp } from "firebase-admin/firestore";
import { expiryTemplate } from "../email/templates/expiry";
import { sendEmail } from "../email/send";

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

      const itemRef = db
        .collection("registries").doc(data.registryId as string)
        .collection("items").doc(data.itemId as string);

      // Read item and registry inside transaction to capture names for email
      const itemSnap = await tx.get(itemRef);
      const itemName = (itemSnap.data()?.title as string) ?? "your gift";

      const registryRef = db.collection("registries").doc(data.registryId as string);
      const registrySnap = await tx.get(registryRef);
      const registryName = (registrySnap.data()?.title as string) ?? "a registry";

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
      };
    });

    if (emailData) {
      const { giverEmail, reservationId: rid, itemName, registryName } = emailData;
      const reReserveUrl = `https://giftregistry.app/reservation/${rid}/re-reserve`;
      const { subject, html, text } = expiryTemplate(
        { itemName, registryName, reReserveUrl },
        "en" // givers don't have preferredLocale stored on reservation; default en per D-14 fallback
      );
      try {
        await sendEmail({ to: giverEmail, subject, html, text });
      } catch (err) {
        console.error(`[releaseReservation] sendEmail failed for reservation ${rid}:`, err);
      }
    }
  }
);
