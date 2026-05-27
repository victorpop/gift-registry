/**
 * Phase 17 D-13/D-14/D-45: per-uid rolling-window rate limit for discoverSearch.
 *
 * Stored at `discoverRateLimits/{uid}` as:
 *   { timestamps: number[], lastWriteAt: Timestamp }
 *
 * `timestamps` holds epoch-ms markers for calls within the last hour.
 * `lastWriteAt` is the Firestore TTL DEADLINE — written as `now + 7 days`
 * because Firestore TTL semantics delete a doc when its TTL-field value is
 * less than the current time. Writing `FieldValue.serverTimestamp()` would
 * mark the doc eligible for deletion on the next TTL sweep (~24h), which
 * would defeat the 7-day abandoned-counter retention window.
 *
 * Implementation note: read + filter + count + write all happen inside a
 * Firestore transaction so two concurrent calls cannot both pass the cap
 * check at length=19 and end up bringing the array to 21.
 */
import { HttpsError } from "firebase-functions/v2/https";
import type { Firestore } from "firebase-admin/firestore";
import { Timestamp } from "firebase-admin/firestore";

const WINDOW_MS = 60 * 60 * 1000; // 1 hour rolling window
const MAX_CALLS = 20; // D-13 verbatim limit
const RATE_LIMIT_TTL_MS = 7 * 24 * 60 * 60 * 1000; // D-14: 7-day TTL on abandoned counters

export async function checkAndIncrementRateLimit(
  db: Firestore,
  uid: string,
): Promise<void> {
  const ref = db.collection("discoverRateLimits").doc(uid);
  await db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    const now = Date.now();
    const cutoff = now - WINDOW_MS;

    const prior = snap.exists ? (snap.data()?.timestamps as unknown) : null;
    const priorList: number[] = Array.isArray(prior)
      ? (prior.filter((t) => typeof t === "number" && t >= cutoff) as number[])
      : [];

    if (priorList.length >= MAX_CALLS) {
      throw new HttpsError("resource-exhausted", "Rate limit exceeded");
    }

    const nextList = [...priorList, now];
    // D-14 + D-45 TTL semantics: lastWriteAt must be the DEADLINE, not now.
    // Firestore TTL deletes when field_value < current_time, so we store
    // `now + 7 days` to ensure the counter survives the full 7-day window.
    tx.set(ref, {
      timestamps: nextList,
      lastWriteAt: Timestamp.fromDate(new Date(Date.now() + RATE_LIMIT_TTL_MS)),
    });
  });
}
