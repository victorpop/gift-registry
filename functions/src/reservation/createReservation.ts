import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { FieldValue, Timestamp } from "firebase-admin/firestore";
import { CloudTasksClient } from "@google-cloud/tasks";
import { writeNotification } from "../notifications/writeNotification";

interface CreateReservationRequest {
  registryId: string;
  itemId: string;
  giverName: string;
  giverEmail: string;
  giverId: string | null;
}

interface CreateReservationResponse {
  reservationId: string;
  affiliateUrl: string;
  expiresAtMs: number;
}

const RESERVATION_DURATION_MS = 30 * 60 * 1000;
const REGION = "europe-west3";
const QUEUE_NAME = "release-reservation";

const tasksClient = new CloudTasksClient();

export const createReservation = onCall<CreateReservationRequest>(
  { region: REGION, minInstances: 1 },
  async (request): Promise<CreateReservationResponse> => {
    const { registryId, itemId, giverName, giverEmail, giverId } = request.data;

    if (!registryId || !itemId || !giverName || !giverEmail) {
      throw new HttpsError("invalid-argument", "MISSING_REQUIRED_FIELDS");
    }

    const db = admin.firestore();
    const expiresAtMs = Date.now() + RESERVATION_DURATION_MS;
    const expiresAt = Timestamp.fromMillis(expiresAtMs);

    let reservationId = "";
    let affiliateUrl = "";

    await db.runTransaction(async (tx) => {
      const itemRef = db
        .collection("registries").doc(registryId)
        .collection("items").doc(itemId);
      const itemSnap = await tx.get(itemRef);

      if (!itemSnap.exists) {
        throw new HttpsError("not-found", "ITEM_NOT_FOUND");
      }
      const itemData = itemSnap.data()!;
      if (itemData.status !== "available") {
        throw new HttpsError("failed-precondition", "ITEM_UNAVAILABLE");
      }

      affiliateUrl = (itemData.affiliateUrl as string) ?? "";

      const reservationRef = db.collection("reservations").doc();
      reservationId = reservationRef.id;

      tx.update(itemRef, {
        status: "reserved",
        reservedBy: giverEmail,
        reservedAt: FieldValue.serverTimestamp(),
        expiresAt,
      });

      tx.set(reservationRef, {
        itemId,
        registryId,
        giverId: giverId ?? null,
        giverName,
        giverEmail,
        affiliateUrl,
        status: "active",
        createdAt: FieldValue.serverTimestamp(),
        expiresAt,
        cloudTaskName: "",
      });
    });

    // CRITICAL: Enqueue Cloud Task AFTER transaction commits (Pitfall 2 — never inside runTransaction)
    const projectId = process.env.GCLOUD_PROJECT!;
    const queuePath = tasksClient.queuePath(projectId, REGION, QUEUE_NAME);
    const targetUrl = `https://${REGION}-${projectId}.cloudfunctions.net/releaseReservation`;

    let cloudTaskName = "";
    try {
      const [taskResponse] = await tasksClient.createTask({
        parent: queuePath,
        task: {
          httpRequest: {
            httpMethod: "POST" as const,
            url: targetUrl,
            body: Buffer.from(JSON.stringify({ data: { reservationId } })).toString("base64"),
            headers: { "Content-Type": "application/json" },
          },
          scheduleTime: { seconds: Math.floor(expiresAtMs / 1000) },
        },
      });
      cloudTaskName = taskResponse.name ?? "";
    } catch (err) {
      // In emulator, Cloud Tasks may not be available. Log and proceed — releaseReservation
      // can still be invoked via direct HTTP POST to emulator endpoint in tests (Pitfall 3).
      console.warn("[createReservation] Cloud Tasks enqueue failed (emulator?):", err);
    }

    await db.collection("reservations").doc(reservationId)
      .update({ cloudTaskName });

    // Write owner-side reservation_created notification.
    // Two extra reads (registry + item) happen AFTER transaction commit — best-effort;
    // a failure here must never prevent the caller from receiving the reservation response.
    try {
      const registrySnap = await db.collection("registries").doc(registryId).get();
      const itemSnap = await db
        .collection("registries").doc(registryId)
        .collection("items").doc(itemId).get();
      const ownerId = registrySnap.data()?.ownerId as string | undefined;
      const registryName = (registrySnap.data()?.title as string) ?? "your registry";
      const itemName = (itemSnap.data()?.title as string) ?? "a gift";
      if (ownerId) {
        await writeNotification({
          userId: ownerId,
          type: "reservation_created",
          titleKey: "notification_reservation_created_title",
          bodyKey: "notification_reservation_created_body",
          titleFallback: `Someone reserved "${itemName}"`,
          bodyFallback: `${giverName} reserved "${itemName}" on "${registryName}"`,
          payload: { registryId, itemId, reservationId, registryName, itemName, actorName: giverName },
        });
      }
    } catch (err) {
      // Best-effort: reservation already created, notification is supplementary.
      console.error("[createReservation] Failed to write reservation_created notification:", err);
      try {
        await db.collection("notifications_failures").add({
          type: "inbox_write",
          notificationType: "reservation_created",
          registryId,
          itemId,
          error: err instanceof Error ? err.message : String(err),
          timestamp: FieldValue.serverTimestamp(),
        });
      } catch (loggingErr) {
        console.error("[createReservation] Failed to log notification failure:", loggingErr);
      }
    }

    return { reservationId, affiliateUrl, expiresAtMs };
  }
);
