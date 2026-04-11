import { onTaskDispatched } from "firebase-functions/v2/tasks";
import * as admin from "firebase-admin";

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

    let stubEmailData: { giverEmail: string; reservationId: string } | null = null;

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

      const nowSeconds = admin.firestore.Timestamp.now().seconds;
      const expiresAtSeconds = (data.expiresAt as admin.firestore.Timestamp).seconds;
      if (nowSeconds < expiresAtSeconds) {
        console.info(`[releaseReservation] reservation ${reservationId} not yet expired; no-op`);
        return;
      }

      const itemRef = db
        .collection("registries").doc(data.registryId as string)
        .collection("items").doc(data.itemId as string);

      tx.update(itemRef, {
        status: "available",
        reservedBy: admin.firestore.FieldValue.delete(),
        reservedAt: admin.firestore.FieldValue.delete(),
        expiresAt: admin.firestore.FieldValue.delete(),
      });
      tx.update(reservationRef, { status: "expired" });

      stubEmailData = {
        giverEmail: data.giverEmail as string,
        reservationId,
      };
    });

    if (stubEmailData) {
      const { giverEmail, reservationId: rid } = stubEmailData;
      const reReserveUrl = `https://giftregistry.app/reservation/${rid}/re-reserve`;
      // eslint-disable-next-line no-console
      console.log(
        `[STUB] Email would be sent to ${giverEmail}: Your reservation has expired. ` +
        `Re-reserve: ${reReserveUrl}`
      );
    }
  }
);
