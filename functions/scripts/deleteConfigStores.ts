/**
 * Phase 17 D-07 one-shot cleanup: deletes the live config/stores Firestore
 * document left behind by the decommissioned Phase 7 Stores capability.
 *
 * Idempotent — re-running on a missing doc is a no-op (Firestore delete()
 * succeeds on non-existent docs).
 *
 * Run via: `cd functions && npx ts-node scripts/deleteConfigStores.ts`
 * Requires application-default credentials (`gcloud auth application-default
 * login`) when targeting prod; emulator users can run inside
 * `firebase emulators:exec`.
 */
import * as admin from "firebase-admin";

if (admin.apps.length === 0) {
  admin.initializeApp();
}

async function main(): Promise<void> {
  const ref = admin.firestore().collection("config").doc("stores");
  const snap = await ref.get();
  if (!snap.exists) {
    console.log("config/stores not present — nothing to delete (idempotent no-op).");
    return;
  }
  await ref.delete();
  console.log("Deleted config/stores Firestore document.");
}

main()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error("deleteConfigStores failed:", err);
    process.exit(1);
  });
