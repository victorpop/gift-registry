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
export { inviteToRegistry } from "./registry/inviteToRegistry";
export { createReservation } from "./reservation/createReservation";
export { releaseReservation } from "./reservation/releaseReservation";
export { resolveReservation } from "./reservation/resolveReservation";

// Dev-only listener: logs rendered mail docs to console in emulator (D-08).
// In production, the Trigger Email extension consumes mail/ docs instead.
if (process.env.FUNCTIONS_EMULATOR === "true") {
  // eslint-disable-next-line @typescript-eslint/no-var-requires
  module.exports.devMailLogger = require("./email/devMailLogger").devMailLogger;
}
