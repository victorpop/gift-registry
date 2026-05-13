/**
 * hydrateActiveReservation callable — returns the caller's active reservation for a registry.
 *
 * Used by the web fallback to restore StickyReserveBanner + ReserveDetailSection across
 * page refreshes, new tabs, and (for signed-in users) other devices.
 *
 * Client-side Firestore query is blocked by security rules (reservations: read if false).
 * This callable runs under Admin SDK to bypass the rules, enforcing ownership server-side.
 *
 * Ownership / query strategy:
 *   - Signed-in: query by giverId == auth.uid. payload.giverEmail is IGNORED even if present
 *     (defence in depth: prevents a signed-in attacker from impersonating a guest by email).
 *   - Guest (auth == null): require payload.giverEmail; query by giverEmail == email AND
 *     giverId == null (guest-only reservations).
 *
 * Returns { active: ActiveReservation } or { active: null } when no active reservation found.
 * affiliateUrl resolves to reservation.affiliateUrl, falling back to item.originalUrl, and
 * finally '' if neither is set. Clients render the 'Continue to retailer' CTA only when
 * affiliateUrl is non-empty.
 */

import { onCall, HttpsError } from "firebase-functions/v2/https";
import * as admin from "firebase-admin";

const REGION = "europe-west3";

interface HydratePayload {
  registryId: string;
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

interface HydrateResponse {
  active: ActiveReservation | null;
}

export const hydrateActiveReservation = onCall<HydratePayload>(
  { region: REGION },
  async (request): Promise<HydrateResponse> => {
    const registryId = request.data?.registryId;
    if (!registryId || typeof registryId !== "string") {
      throw new HttpsError("invalid-argument", "MISSING_REGISTRY_ID");
    }

    const db = admin.firestore();
    let querySnap: admin.firestore.QuerySnapshot;

    if (request.auth) {
      // Signed-in path: query by giverId == auth.uid.
      // Never use payload.giverEmail for signed-in callers (defence in depth).
      const uid = request.auth.uid;
      querySnap = await db
        .collection("reservations")
        .where("registryId", "==", registryId)
        .where("giverId", "==", uid)
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

    const itemId = reservation.itemId as string;
    const expiresAt = reservation.expiresAt as { toMillis: () => number };
    const expiresAtMs = expiresAt.toMillis();

    // Read item document for title, merchantDomain, and originalUrl fallback.
    const itemSnap = await db
      .collection("registries")
      .doc(registryId)
      .collection("items")
      .doc(itemId)
      .get();

    const itemData = itemSnap.data();
    const itemName = (itemData?.title as string) ?? "your gift";
    const merchantDomain = (itemData?.merchantDomain as string) ?? null;
    const effectiveAffiliateUrl =
      ((reservation.affiliateUrl as string) || "") ||
      ((itemData?.originalUrl as string) || "") ||
      "";

    return {
      active: {
        reservationId: reservationDoc.id,
        itemId,
        itemName,
        affiliateUrl: effectiveAffiliateUrl,
        merchantDomain,
        expiresAtMs,
      },
    };
  }
);
