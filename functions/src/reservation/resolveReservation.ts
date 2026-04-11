import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";

interface ResolveReservationRequest {
  reservationId: string;
}

interface ResolveReservationResponse {
  registryId: string;
  itemId: string;
  status: string;
}

const REGION = "europe-west3";

export const resolveReservation = onCall<ResolveReservationRequest>(
  { region: REGION },
  async (request): Promise<ResolveReservationResponse> => {
    const { reservationId } = request.data;

    if (!reservationId) {
      throw new HttpsError("invalid-argument", "MISSING_RESERVATION_ID");
    }

    const db = admin.firestore();
    const snap = await db.collection("reservations").doc(reservationId).get();

    if (!snap.exists) {
      throw new HttpsError("not-found", "RESERVATION_NOT_FOUND");
    }

    const data = snap.data()!;
    const registryId = data.registryId as string | undefined;
    const itemId = data.itemId as string | undefined;
    const status = (data.status as string | undefined) ?? "unknown";

    if (!registryId || !itemId) {
      throw new HttpsError("failed-precondition", "RESERVATION_MALFORMED");
    }

    return { registryId, itemId, status };
  }
);
