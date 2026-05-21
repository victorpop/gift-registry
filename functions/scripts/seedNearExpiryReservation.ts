// Phase 14 Plan 04 — UAT item 6 helper (D-09 locked Cloud-Tasks path).
//
// Creates a reservation doc AND enqueues a Cloud Task scheduled 60s in the future
// targeting the DEPLOYED releaseReservation onTaskDispatched handler. The handler
// then runs releaseReservationCore (releases the item, sends expiry email,
// writes giver/owner notifications) — exact same pipeline as production, just
// compressed from 30min → 60s.
//
// Mirrors `functions/src/reservation/createReservation.ts` so the Cloud-Tasks
// payload shape, queue name, target URL, scheduleTime, and reservation/item
// document shapes match what the deployed handler expects.
//
// Usage:
//   cd functions
//   gcloud auth application-default login   # one-time, if not already authed
//   npx ts-node scripts/seedNearExpiryReservation.ts <registryId> <itemId> <giverEmail>
//
// Then wait ~60-120s, watch the inbox for the expiry email, click the
// re-reserve link, and verify a new reservation is created on the registry
// detail page.
//
// Cleanup: either let the new reservation expire naturally (30 min) or release
// it manually via the Android app so prod doesn't accumulate UAT cruft.

import * as admin from "firebase-admin";
import { FieldValue, Timestamp } from "firebase-admin/firestore";
import { CloudTasksClient } from "@google-cloud/tasks";

// Mirror createReservation.ts exactly — these MUST match the deployed function.
// Reference: functions/src/reservation/createReservation.ts:23-24, 86-87
const PROJECT_ID = "gift-registry-ro";
const REGION = "europe-west3";
const QUEUE_NAME = "release-reservation"; // hyphenated — matches deployed queue
const HANDLER_URL = `https://${REGION}-${PROJECT_ID}.cloudfunctions.net/releaseReservation`;

const NEAR_EXPIRY_MS = 60_000; // 60 seconds — compressed from production's 30 min

if (admin.apps.length === 0) {
  admin.initializeApp({ projectId: PROJECT_ID });
}
const db = admin.firestore();
const tasksClient = new CloudTasksClient();

async function main(): Promise<void> {
  const [registryId, itemId, giverEmail] = process.argv.slice(2);
  if (!registryId || !itemId || !giverEmail) {
    console.error(
      "Usage: ts-node scripts/seedNearExpiryReservation.ts <registryId> <itemId> <giverEmail>"
    );
    process.exit(1);
  }

  // Compute expiry first so the doc and the Cloud Task share an identical timestamp.
  const expiresAtMs = Date.now() + NEAR_EXPIRY_MS;
  const expiresAt = Timestamp.fromMillis(expiresAtMs);

  // Step 1 — flip item to reserved + create reservation doc in a transaction
  // (mirrors createReservation.ts:44-82). This makes the state-flip visible in
  // Firestore so UAT-6b can verify the release transaction (status active→expired,
  // item reserved→available) actually ran.
  let reservationId = "";
  let affiliateUrl = "";

  await db.runTransaction(async (tx) => {
    const itemRef = db
      .collection("registries").doc(registryId)
      .collection("items").doc(itemId);
    const itemSnap = await tx.get(itemRef);

    if (!itemSnap.exists) {
      throw new Error(`ITEM_NOT_FOUND: registries/${registryId}/items/${itemId}`);
    }
    const itemData = itemSnap.data()!;
    if (itemData.status !== "available") {
      throw new Error(
        `ITEM_UNAVAILABLE: registries/${registryId}/items/${itemId} has status="${itemData.status}" (expected "available")`
      );
    }

    affiliateUrl = (itemData.affiliateUrl as string) ?? "";

    const reservationRef = db.collection("reservations").doc();
    reservationId = reservationRef.id;

    tx.update(itemRef, {
      status: "reserved",
      reservedBy: giverEmail,
      reservedAt: FieldValue.serverTimestamp(),
      expiresAt,
    });

    tx.set(reservationRef, {
      itemId,
      registryId,
      giverId: null,            // guest reservation — matches the UAT-6 path
      giverName: "UAT Test",    // single field — matches createReservation.ts
      giverEmail,
      affiliateUrl,
      status: "active",
      createdAt: FieldValue.serverTimestamp(),
      expiresAt,
      cloudTaskName: "",
    });
  });

  // Step 2 — enqueue the Cloud Task AFTER transaction commits (Pitfall 2 from
  // createReservation.ts:84 — never inside runTransaction). Payload shape is
  // `{ data: { reservationId } }` because onTaskDispatched unwraps the `.data`
  // envelope to populate `req.data` (see createReservation.ts:97 +
  // releaseReservation.ts:163-164).
  const queuePath = tasksClient.queuePath(PROJECT_ID, REGION, QUEUE_NAME);
  const [taskResponse] = await tasksClient.createTask({
    parent: queuePath,
    task: {
      httpRequest: {
        httpMethod: "POST" as const,
        url: HANDLER_URL,
        body: Buffer.from(
          JSON.stringify({ data: { reservationId } })
        ).toString("base64"),
        headers: { "Content-Type": "application/json" },
      },
      scheduleTime: { seconds: Math.floor(expiresAtMs / 1000) },
    },
  });
  const cloudTaskName = taskResponse.name ?? "";

  // Step 3 — persist cloudTaskName so releaseReservationCallable could cancel
  // it on manual release (mirrors createReservation.ts:131-132).
  await db.collection("reservations").doc(reservationId)
    .update({ cloudTaskName });

  console.log("─".repeat(60));
  console.log("Seeded near-expiry reservation (D-09 UAT-6 helper)");
  console.log("─".repeat(60));
  console.log("Reservation ID :", reservationId);
  console.log("Registry ID    :", registryId);
  console.log("Item ID        :", itemId);
  console.log("Giver email    :", giverEmail);
  console.log("Expires at     :", expiresAt.toDate().toISOString(),
    `(in ~${Math.round((expiresAtMs - Date.now()) / 1000)}s)`);
  console.log("Cloud Task     :", cloudTaskName || "(empty — task enqueue may have failed)");
  console.log("Affiliate URL  :", affiliateUrl || "(none — item has no affiliateUrl)");
  console.log("─".repeat(60));
  console.log("Next: wait ~60-120s, then check inbox for the expiry email.");
  console.log("      Subject: see functions/src/email/templates/expiry.ts");
  console.log("      Click the re-reserve link → verify a NEW reservation is created.");
}

main()
  .then(() => process.exit(0))
  .catch((e) => {
    console.error(e);
    process.exit(1);
  });
