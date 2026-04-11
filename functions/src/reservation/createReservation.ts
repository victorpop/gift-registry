import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { CloudTasksClient } from "@google-cloud/tasks";

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
    const expiresAt = admin.firestore.Timestamp.fromMillis(expiresAtMs);

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
        reservedAt: admin.firestore.FieldValue.serverTimestamp(),
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
        createdAt: admin.firestore.FieldValue.serverTimestamp(),
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

    return { reservationId, affiliateUrl, expiresAtMs };
  }
);
