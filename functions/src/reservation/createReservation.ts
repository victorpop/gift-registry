import { onCall, HttpsError } from "firebase-functions/v2/https";

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

export const createReservation = onCall<CreateReservationRequest, Promise<CreateReservationResponse>>(
  { region: "europe-west3", minInstances: 1 },
  async (_request) => {
    throw new HttpsError("unimplemented", "NOT_IMPLEMENTED: Wave 1 will implement this (Plan 02).");
  }
);
