// Cloud Functions entry point
// Phase 3+ will add: affiliate URL transformer, reservation expiry handler
// Phase 6 will add: notification triggers, email flows

import * as functions from "firebase-functions/v2";
import * as admin from "firebase-admin";

admin.initializeApp();

// Placeholder — functions will be added in subsequent phases
export const healthCheck = functions.https.onRequest((req, res) => {
  res.json({ status: "ok", timestamp: new Date().toISOString() });
});
