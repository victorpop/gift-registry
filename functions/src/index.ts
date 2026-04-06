// Cloud Functions entry point
// Phase 3+: affiliate URL transformer, reservation expiry handler
// Phase 6: notification triggers, email flows

import * as functions from "firebase-functions/v2";
import * as admin from "firebase-admin";

admin.initializeApp();

export const healthCheck = functions.https.onRequest((req, res) => {
  res.json({ status: "ok", timestamp: new Date().toISOString() });
});

export { fetchOgMetadata } from "./registry/fetchOgMetadata";
