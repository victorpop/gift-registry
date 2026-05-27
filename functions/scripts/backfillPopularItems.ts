/**
 * Phase 17 D-22 one-shot script: backfill popularItems from existing
 * registries/*\/items.
 *
 * Idempotent — every write uses set(..., { merge: true }) so re-running
 * the script after triggers are live is safe (D-22 explicit).
 *
 * MUST run BEFORE deploying the onItemCreate/Delete/Update triggers,
 * otherwise live triggers race with the batched writes (D-22 verbatim).
 * Plan 17-06 orchestrates this ordering: backfill first, then deploy.
 *
 * Aggregation strategy:
 *   - Single collectionGroup("items") scan; in-memory Map<productId, agg>.
 *   - Per item: normalizeUrl → productId; merge registryIds set + take
 *     the first observed title/imageUrl/price for denorm fields.
 *   - Items without an originalUrl are skipped (Discover popular only
 *     surfaces URL-bearing items).
 *
 * Write strategy:
 *   - Firestore batch capped at 500 operations (hard limit per batch).
 *   - On reaching 500, commit, log progress, reset batch, continue.
 *   - Final partial batch committed after the loop.
 *   - All writes use set + merge so the script is safe to re-run.
 *
 * Run via:
 *   cd functions
 *   npm run backfill:popular
 *
 * Requires application-default credentials when targeting prod:
 *   gcloud auth application-default login
 */
import * as admin from "firebase-admin";
import { FieldValue } from "firebase-admin/firestore";
import { normalizeUrl } from "../src/discover/urlNormalization";

if (admin.apps.length === 0) {
  admin.initializeApp();
}

const BATCH_LIMIT = 500;

interface Aggregate {
  productId: string;
  canonicalUrl: string;
  title: string;
  imageUrl: string;
  price: string;
  registryIds: Set<string>;
}

async function main(): Promise<void> {
  const db = admin.firestore();
  console.log("Scanning collectionGroup('items')…");
  const snap = await db.collectionGroup("items").get();
  console.log(`Found ${snap.size} items across all registries.`);

  const byProduct = new Map<string, Aggregate>();
  let skippedNoUrl = 0;
  for (const docSnap of snap.docs) {
    const data = docSnap.data();
    const originalUrl = data.originalUrl as string | undefined;
    if (!originalUrl || typeof originalUrl !== "string") {
      skippedNoUrl += 1;
      continue;
    }
    // Path is registries/{registryId}/items/{itemId}
    const parts = docSnap.ref.path.split("/");
    const registryId = parts[1];
    if (!registryId) continue;

    let agg: Aggregate;
    try {
      const { productId, canonicalUrl } = normalizeUrl(originalUrl);
      const existing = byProduct.get(productId);
      if (existing) {
        existing.registryIds.add(registryId);
        continue;
      }
      agg = {
        productId,
        canonicalUrl,
        title: typeof data.title === "string" ? data.title : "",
        imageUrl: typeof data.imageUrl === "string" ? data.imageUrl : "",
        price: typeof data.price === "string" ? data.price : "",
        registryIds: new Set([registryId]),
      };
    } catch (err) {
      // Malformed URL — skip rather than abort the entire backfill.
      console.warn(
        `Skipping item ${docSnap.ref.path}: failed to normalize originalUrl (${
          (err as Error).message
        })`
      );
      skippedNoUrl += 1;
      continue;
    }
    byProduct.set(agg.productId, agg);
  }
  console.log(
    `Aggregated to ${byProduct.size} unique products (${skippedNoUrl} items skipped — no/invalid originalUrl).`
  );

  // Batched writes — 500 docs/batch (Firestore hard limit).
  let batch = db.batch();
  let inBatch = 0;
  let totalWritten = 0;
  for (const agg of byProduct.values()) {
    const ref = db.collection("popularItems").doc(agg.productId);
    const registryIds = Array.from(agg.registryIds);
    batch.set(
      ref,
      {
        canonicalUrl: agg.canonicalUrl,
        title: agg.title,
        imageUrl: agg.imageUrl,
        price: agg.price,
        registryIds,
        registryCount: registryIds.length,
        updatedAt: FieldValue.serverTimestamp(),
      },
      { merge: true }
    );
    inBatch += 1;
    if (inBatch === BATCH_LIMIT) {
      await batch.commit();
      totalWritten += inBatch;
      console.log(`  Committed batch (${totalWritten}/${byProduct.size})`);
      batch = db.batch();
      inBatch = 0;
    }
  }
  if (inBatch > 0) {
    await batch.commit();
    totalWritten += inBatch;
    console.log(`  Committed final batch (${totalWritten}/${byProduct.size})`);
  }
  console.log(`Backfill complete: ${totalWritten} popularItems docs written.`);
}

main()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error("backfillPopularItems failed:", err);
    process.exit(1);
  });
