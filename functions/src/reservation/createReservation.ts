import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";
import { FieldValue, Timestamp } from "firebase-admin/firestore";
import { getFunctions } from "firebase-admin/functions";
import { writeNotification } from "../notifications/writeNotification";
import { releaseReservationCore, ReleasePayload } from "./releaseReservation";

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
      });
    });

    // Enqueue Cloud Task AFTER transaction commits (Pitfall 2 — never inside runTransaction).
    // Use Firebase Admin's TaskQueue API: handles OIDC token generation for the function's
    // runtime service account so the dispatched HTTP request authenticates against Cloud Run.
    // Replaces the previous raw @google-cloud/tasks CloudTasksClient pattern, which created
    // tasks without an oidcToken and was silently dropped after 3 retries (Plan 14-04 fix).
    const queue = getFunctions().taskQueue<ReleasePayload>("releaseReservation");
    try {
      await queue.enqueue(
        { reservationId },
        { scheduleTime: new Date(expiresAtMs) }
      );
    } catch (err) {
      // In emulator, Cloud Tasks may not be available. Log and proceed — releaseReservation
      // can still be invoked via direct HTTP POST to emulator endpoint in tests (Pitfall 3).
      console.warn("[createReservation] taskQueue.enqueue failed (emulator?):", err);

      // Emulator-only fallback: setTimeout to invoke release directly.
      // Production never hits this path because taskQueue.enqueue succeeds when deployed.
      // FUNCTIONS_EMULATOR is set automatically by `firebase emulators:start`, never in prod.
      // KNOWN LIMITATION: a Functions emulator restart loses pending timers — the
      // reservation will stay "reserved" forever in that emulator session. Production
      // is unaffected because Cloud Tasks is durable.
      if (process.env.FUNCTIONS_EMULATOR === "true") {
        const delayMs = Math.max(0, expiresAtMs - Date.now());
        console.info(
          `[createReservation] Emulator fallback: scheduling release of ${reservationId} in ${delayMs}ms`
        );
        const timer = setTimeout(() => {
          releaseReservationCore({ reservationId, db: admin.firestore() })
            .catch((e) => console.error(
              `[createReservation] Emulator fallback release failed for ${reservationId}:`, e
            ));
        }, delayMs);
        timer.unref?.();
      }
    }
    // Note: getFunctions().taskQueue().enqueue() returns Promise<void> — no task name available.
    // releaseReservationCallable's deleteTask path becomes a no-op when cloudTaskName is absent;
    // releaseReservationCore is idempotent (no-ops when status !== "active"), so a late-firing
    // task after manual release is a harmless wasted invocation.

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
