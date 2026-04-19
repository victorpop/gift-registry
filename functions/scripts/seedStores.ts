import * as admin from "firebase-admin";
import * as fs from "fs";
import * as path from "path";

// Initialize Admin SDK. When run via `firebase emulators:exec` or with
// GOOGLE_APPLICATION_CREDENTIALS set, default credentials work. When run
// directly against prod, user must have ADC set up (`gcloud auth application-default login`).
if (admin.apps.length === 0) {
  admin.initializeApp();
}

async function main(): Promise<void> {
  const jsonPath = path.resolve(__dirname, "../data/stores.seed.json");
  const raw = fs.readFileSync(jsonPath, "utf8");
  const payload = JSON.parse(raw) as { stores: unknown[] };

  // Idempotent: `.set` overwrites the doc every run with the canonical seed
  // (D-22). This is intentional — the JSON file is the single source of truth
  // for the store list; running the script twice yields the same result.
  await admin
    .firestore()
    .collection("config")
    .doc("stores")
    .set(payload);

  console.log(`Seeded config/stores with ${payload.stores.length} entries`);
}

main()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error("Seed failed:", err);
    process.exit(1);
  });
