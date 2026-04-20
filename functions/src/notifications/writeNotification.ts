/**
 * writeNotification — shared Admin-SDK helper for writing persistent in-app
 * notification documents to `users/{userId}/notifications`.
 *
 * Design principles:
 * - Best-effort: failures are logged to `notifications_failures` and never rethrown.
 *   An inbox-write error must never break the critical-path operation (invite, reservation,
 *   purchase, expiry) that triggered it.
 * - i18n-first: server writes titleKey + bodyKey (Android strings.xml keys) + positional
 *   payload. The Android client resolves the final localized string. titleFallback /
 *   bodyFallback are pre-rendered English for diagnostics and fallback rendering.
 * - Admin SDK only: clients are blocked from creating notification documents via
 *   Firestore security rules (allow create: if false). Only this helper — running
 *   in Cloud Functions with Admin SDK privileges — can write.
 */

import * as admin from "firebase-admin";
import { FieldValue } from "firebase-admin/firestore";

export type NotificationType =
  | "invite"
  | "reservation_created"
  | "item_purchased"
  | "reservation_expired"
  | "re_reserve_window";

export interface WriteNotificationParams {
  userId: string;
  type: NotificationType;
  titleKey: string;
  bodyKey: string;
  titleFallback: string;
  bodyFallback: string;
  payload: Record<string, unknown>;
}

/**
 * Writes a notification document to `users/{userId}/notifications`.
 * Never throws — logs failures to `notifications_failures` and returns.
 */
export async function writeNotification(params: WriteNotificationParams): Promise<void> {
  const { userId, type, titleKey, bodyKey, titleFallback, bodyFallback, payload } = params;
  const db = admin.firestore();

  try {
    await db.collection("users").doc(userId).collection("notifications").add({
      type,
      titleKey,
      bodyKey,
      title: titleFallback,
      body: bodyFallback,
      payload,
      createdAt: FieldValue.serverTimestamp(),
      readAt: null,
    });
  } catch (err) {
    // Best-effort: log to notifications_failures and swallow — never rethrow.
    console.error(`[writeNotification] Failed to write inbox entry for user ${userId}:`, err);
    try {
      await db.collection("notifications_failures").add({
        type: "inbox_write",
        userId,
        notificationType: type,
        error: err instanceof Error ? err.message : String(err),
        timestamp: FieldValue.serverTimestamp(),
      });
    } catch (loggingErr) {
      console.error("[writeNotification] Failed to log inbox_write failure:", loggingErr);
    }
  }
}
