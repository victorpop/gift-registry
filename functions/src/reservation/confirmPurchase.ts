/**
 * confirmPurchase callable — giver-facing purchase confirmation (D-01/D-02).
 *
 * Runs a Firestore transaction that atomically transitions:
 *   - reservation.status: "active" → "purchased"
 *   - item.status: "reserved" → "purchased"
 *
 * After the transaction commits, cancels the stored Cloud Task (D-02).
 * Cloud Task NOT_FOUND (code 5) is swallowed — task already fired or already cancelled.
 * Any other Cloud Task error is logged but NOT rethrown — reservation is already purchased.
 *
 * No auth required — guest givers are supported (CONTEXT.md: guest access without account creation).
 */

import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { CloudTasksClient } from "@google-cloud/tasks";

interface ConfirmPurchaseRequest {
  reservationId: string;
}

interface ConfirmPurchaseResponse {
  success: boolean;
}

const REGION = "europe-west3";
const tasksClient = new CloudTasksClient();

export const confirmPurchase = onCall<ConfirmPurchaseRequest>(
  { region: REGION },
  async (request): Promise<ConfirmPurchaseResponse> => {
    const { reservationId } = request.data ?? ({} as ConfirmPurchaseRequest);

    if (!reservationId || typeof reservationId !== "string") {
      throw new HttpsError("invalid-argument", "MISSING_RESERVATION_ID");
    }

    const db = admin.firestore();
    const reservationRef = db.collection("reservations").doc(reservationId);
    let cloudTaskName = "";

    await db.runTransaction(async (tx) => {
      const reservationSnap = await tx.get(reservationRef);

      if (!reservationSnap.exists) {
        throw new HttpsError("not-found", "RESERVATION_NOT_FOUND");
      }

      const reservation = reservationSnap.data()!;

      if (reservation.status !== "active") {
        // Pitfall 4: giver tap races with timer expiry — surface as failed-precondition
        throw new HttpsError("failed-precondition", "RESERVATION_EXPIRED");
      }

      const registryId = reservation.registryId as string;
      const itemId = reservation.itemId as string;
      const itemRef = db
        .collection("registries").doc(registryId)
        .collection("items").doc(itemId);

      tx.update(reservationRef, { status: "purchased" });
      tx.update(itemRef, { status: "purchased" });

      cloudTaskName = (reservation.cloudTaskName as string) ?? "";
    });

    // Cancel Cloud Task AFTER transaction commits (never inside — Pitfall 2)
    if (cloudTaskName) {
      try {
        await tasksClient.deleteTask({ name: cloudTaskName });
      } catch (err: unknown) {
        const code = (err as { code?: number }).code;
        if (code === 5) {
          // NOT_FOUND: task already fired or already cancelled — expected, swallow
          console.info(
            `[confirmPurchase] Cloud Task ${cloudTaskName} already gone; ignoring`
          );
        } else {
          console.error(
            `[confirmPurchase] deleteTask failed for ${cloudTaskName}:`,
            err
          );
          // Intentionally do not rethrow — reservation is already purchased
        }
      }
    }

    return { success: true };
  }
);
