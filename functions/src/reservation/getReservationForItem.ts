/**
 * getReservationForItem callable — returns the caller's active reservation for a specific item.
 *
 * Extends hydrateActiveReservation by adding itemId scoping so the per-item
 * reserve-detail page (/registry/:id/item/:itemId) can resolve the correct
 * reservation even when the caller has multiple concurrent active reservations.
 *
 * Ownership / query strategy:
 *   - Signed-in: query by giverId == auth.uid AND itemId. payload.giverEmail is IGNORED
 *     even if present (defence in depth: prevents a signed-in attacker from impersonating
 *     a guest by email).
 *   - Guest (auth == null): require payload.giverEmail; query by giverEmail == email AND
 *     giverId == null AND itemId (guest-only reservations).
 *
 * Returns { active: ActiveReservation } or { active: null } when no active reservation found.
 * affiliateUrl === '' (legacy data) also returns { active: null }.
 *
 * Validation order: registryId → itemId → (guest-only) giverEmail.
 */

import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";

const REGION = "europe-west3";

interface GetItemPayload {
  registryId: string;
  itemId: string;
  giverEmail?: string;
}

interface ActiveReservation {
  reservationId: string;
  itemId: string;
  itemName: string;
  affiliateUrl: string;
  merchantDomain: string | null;
  expiresAtMs: number;
}

interface GetItemResponse {
  active: ActiveReservation | null;
}

export const getReservationForItem = onCall<GetItemPayload>(
  { region: REGION },
  async (request): Promise<GetItemResponse> => {
    const registryId = request.data?.registryId;
    if (!registryId || typeof registryId !== "string") {
      throw new HttpsError("invalid-argument", "MISSING_REGISTRY_ID");
    }

    const itemId = request.data?.itemId;
    if (!itemId || typeof itemId !== "string") {
      throw new HttpsError("invalid-argument", "MISSING_ITEM_ID");
    }

    const db = admin.firestore();
    let querySnap: admin.firestore.QuerySnapshot;

    if (request.auth) {
      // Signed-in path: query by giverId == auth.uid AND itemId == itemId.
      // Never use payload.giverEmail for signed-in callers (defence in depth).
      const uid = request.auth.uid;
      querySnap = await db
        .collection("reservations")
        .where("registryId", "==", registryId)
        .where("giverId", "==", uid)
        .where("itemId", "==", itemId)
        .where("status", "==", "active")
        .orderBy("createdAt", "desc")
        .limit(1)
        .get();
    } else {
      // Guest path: require giverEmail from payload.
      const giverEmail = request.data?.giverEmail;
      if (!giverEmail || typeof giverEmail !== "string") {
        throw new HttpsError("invalid-argument", "MISSING_GIVER_EMAIL");
      }

      querySnap = await db
        .collection("reservations")
        .where("registryId", "==", registryId)
        .where("giverEmail", "==", giverEmail)
        .where("giverId", "==", null)
        .where("itemId", "==", itemId)
        .where("status", "==", "active")
        .orderBy("createdAt", "desc")
        .limit(1)
        .get();
    }

    if (querySnap.empty) {
      return { active: null };
    }

    const reservationDoc = querySnap.docs[0];
    const reservation = reservationDoc.data();

    // Silently skip legacy reservations with no affiliate URL.
    const affiliateUrl = (reservation.affiliateUrl as string) ?? "";
    if (!affiliateUrl) {
      return { active: null };
    }

    const resolvedItemId = reservation.itemId as string;
    const expiresAt = reservation.expiresAt as { toMillis: () => number };
    const expiresAtMs = expiresAt.toMillis();

    // Read item document for title and merchantDomain.
    const itemSnap = await db
      .collection("registries")
      .doc(registryId)
      .collection("items")
      .doc(resolvedItemId)
      .get();

    const itemName = (itemSnap.data()?.title as string) ?? "your gift";
    const merchantDomain = (itemSnap.data()?.merchantDomain as string) ?? null;

    return {
      active: {
        reservationId: reservationDoc.id,
        itemId: resolvedItemId,
        itemName,
        affiliateUrl,
        merchantDomain,
        expiresAtMs,
      },
    };
  }
);
