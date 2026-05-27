---
phase: 17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini
plan: 04
type: execute
wave: 2
depends_on:
  - "17-02"
files_modified:
  - functions/src/discover/triggers.ts
  - functions/src/__tests__/discover/triggers.test.ts
  - functions/scripts/backfillPopularItems.ts
  - functions/src/index.ts
autonomous: true
requirements:
  - D-17
  - D-18
  - D-19
  - D-22
  - D-47

must_haves:
  truths:
    - "onItemCreatePopular increments popularItems counter via URL-normalized productId"
    - "onItemDeletePopular decrements counter and deletes the popularItems doc when registryCount falls to 0"
    - "onItemUpdatePopular fires only when originalUrl changed — treats as delete(old) + create(new)"
    - "Triggers are idempotent — re-firing onCreate for the same (productId, registryId) pair is a no-op"
    - "backfillPopularItems.ts scans collectionGroup('items'), aggregates by productId, writes in 500-doc batches, uses set+merge for idempotency"
  artifacts:
    - path: "functions/src/discover/triggers.ts"
      provides: "onItemCreatePopular + onItemDeletePopular + onItemUpdatePopular Firestore triggers"
      contains: "onDocumentCreated"
    - path: "functions/scripts/backfillPopularItems.ts"
      provides: "One-shot Admin SDK script — scans items, populates popularItems"
      contains: "collectionGroup(\"items\")"
    - path: "functions/src/__tests__/discover/triggers.test.ts"
      provides: "Unit tests for trigger counter logic (idempotent create, decrement, delete on zero)"
      contains: "describe(\"popularItems triggers\""
    - path: "functions/src/index.ts"
      provides: "Exports onItemCreatePopular, onItemDeletePopular, onItemUpdatePopular"
      contains: "onItemCreatePopular"
  key_links:
    - from: "functions/src/discover/triggers.ts"
      to: "popularItems collection"
      via: "FieldValue.arrayUnion / arrayRemove / increment(1) / increment(-1)"
      pattern: "arrayUnion\\|arrayRemove\\|increment"
    - from: "functions/src/discover/triggers.ts"
      to: "urlNormalization.normalizeUrl"
      via: "Item.originalUrl → { productId, canonicalUrl }"
      pattern: "normalizeUrl"
    - from: "functions/scripts/backfillPopularItems.ts"
      to: "Admin SDK batched writes"
      via: "batch.set(ref, {...}, { merge: true }); commit every 500"
      pattern: "merge: true"
---

<objective>
Implement the three Firestore-triggered Cloud Functions that maintain the `popularItems` counters in real time, plus the one-shot backfill script that pre-populates `popularItems` from existing `registries/*/items` before the triggers go live. Per CONTEXT.md D-17, D-18, D-19, D-22, D-47.

Purpose: Without these, the `popularItems` collection is empty and `discoverPopular` (plan 17-03) returns an empty list forever. The backfill script must run BEFORE the triggers are deployed (D-22) — plan 17-06 orchestrates this ordering.

Output: One trigger source file with 3 exported triggers, one backfill script, one trigger test file, and 3 new named exports in `functions/src/index.ts`.
</objective>

<execution_context>
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/workflows/execute-plan.md
@/Users/victorpop/ai-projects/gift-registry/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md
@.planning/STATE.md
@.planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-CONTEXT.md
@.planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-02-backend-foundations-PLAN.md
@CLAUDE.md
</context>

<interfaces>
<!-- Modules created in plan 17-02 — consumed verbatim here. -->

From functions/src/discover/urlNormalization.ts:
```typescript
export interface NormalizedUrl { productId: string; canonicalUrl: string }
export function normalizeUrl(url: string): NormalizedUrl;
```

From functions/src/email/devMailLogger.ts (existing onDocumentCreated pattern):
```typescript
import { onDocumentCreated } from "firebase-functions/v2/firestore";
export const devMailLogger = onDocumentCreated(
  { document: "mail/{docId}", region: "europe-west3" },
  async (event) => { /* ... */ }
);
```

Item document shape (consumed by triggers — from app/src/main/java/com/giftregistry/domain/model/Item.kt + Firestore writes in RegistryRepositoryImpl):
```
registries/{registryId}/items/{itemId} document fields:
  - title: string
  - originalUrl: string
  - affiliateUrl: string
  - imageUrl: string?
  - price: string?  (display format, e.g. "459,00 RON")
  - status: "available" | "reserved" | "purchased"
  - addedAt: Timestamp
  - (no description field per CONTEXT.md `<canonical_refs>` note)
```
</interfaces>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: triggers.ts (onItemCreatePopular / onItemDeletePopular / onItemUpdatePopular) + unit tests + index.ts exports</name>

  <behavior>
    - onItemCreatePopular: reads new item's originalUrl → normalizeUrl → productId. If popularItems/{productId} missing → create with {canonicalUrl, title, imageUrl, price, registryIds: [registryId], registryCount: 1, updatedAt: serverTimestamp}. If present and registryIds excludes registryId → arrayUnion + increment(1). If present and registryIds already contains registryId → no-op (idempotent).
    - onItemDeletePopular: same productId computation. arrayRemove registryId + increment(-1). If registryCount falls to 0 → delete the popularItems doc.
    - onItemUpdatePopular: fires on document update. Compare before.originalUrl vs after.originalUrl. If unchanged → no-op. If changed → execute delete(oldProductId) + create(newProductId) logic.
  </behavior>

  <read_first>
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-CONTEXT.md (decisions D-17, D-18, D-19, D-47 verbatim)
    - functions/src/discover/urlNormalization.ts (consumed for productId computation)
    - functions/src/email/devMailLogger.ts (existing onDocumentCreated pattern with region + path template)
    - functions/src/__tests__/onPurchaseNotification.test.ts (existing trigger test pattern — handler split for testability)
    - functions/src/index.ts (current — list of named exports to extend)
  </read_first>

  <files>
    functions/src/discover/triggers.ts,
    functions/src/__tests__/discover/triggers.test.ts,
    functions/src/index.ts
  </files>

  <action>
    **1. `functions/src/discover/triggers.ts`** — implements D-17, D-18, D-19. Split each trigger into an exported `onCall*Handler` core function (testable without firebase-functions runtime) and the `onDocumentCreated/Deleted/Updated` wrapper:

    ```typescript
    import { onDocumentCreated, onDocumentDeleted, onDocumentUpdated, FirestoreEvent, QueryDocumentSnapshot, Change } from "firebase-functions/v2/firestore";
    import * as admin from "firebase-admin";
    import { FieldValue, Firestore } from "firebase-admin/firestore";
    import { normalizeUrl } from "./urlNormalization";

    const REGION = "europe-west3";

    interface ItemDoc {
      originalUrl?: string;
      title?: string;
      imageUrl?: string;
      price?: string;
    }

    /**
     * D-18 onCreate: registry adds this product; increment counter.
     * Idempotent — arrayUnion ensures duplicate registryId is a no-op.
     * But increment(1) is NOT idempotent on re-fire — guard by checking
     * registryIds first.
     */
    export async function handleItemCreate(
      db: Firestore,
      registryId: string,
      item: ItemDoc
    ): Promise<void> {
      if (!item.originalUrl) return;  // Items without URL aren't tracked
      const { productId, canonicalUrl } = normalizeUrl(item.originalUrl);
      const ref = db.collection("popularItems").doc(productId);

      await db.runTransaction(async (tx) => {
        const snap = await tx.get(ref);
        if (!snap.exists) {
          tx.set(ref, {
            canonicalUrl,
            title: item.title ?? "",
            imageUrl: item.imageUrl ?? "",
            price: item.price ?? "",
            registryIds: [registryId],
            registryCount: 1,
            updatedAt: FieldValue.serverTimestamp(),
          });
          return;
        }
        const existing = snap.data() as Record<string, unknown>;
        const ids = Array.isArray(existing.registryIds) ? (existing.registryIds as string[]) : [];
        if (ids.includes(registryId)) {
          // D-18 idempotency: already counted — no-op
          return;
        }
        tx.update(ref, {
          registryIds: FieldValue.arrayUnion(registryId),
          registryCount: FieldValue.increment(1),
          updatedAt: FieldValue.serverTimestamp(),
          // Refresh denorm fields on the way through (most-recent wins)
          title: item.title ?? existing.title ?? "",
          imageUrl: item.imageUrl ?? existing.imageUrl ?? "",
          price: item.price ?? existing.price ?? "",
        });
      });
    }

    /**
     * D-18 onDelete: registry removes this product; decrement counter.
     * If count reaches 0, delete the popularItems doc.
     */
    export async function handleItemDelete(
      db: Firestore,
      registryId: string,
      item: ItemDoc
    ): Promise<void> {
      if (!item.originalUrl) return;
      const { productId } = normalizeUrl(item.originalUrl);
      const ref = db.collection("popularItems").doc(productId);

      await db.runTransaction(async (tx) => {
        const snap = await tx.get(ref);
        if (!snap.exists) return;
        const existing = snap.data() as Record<string, unknown>;
        const ids = Array.isArray(existing.registryIds) ? (existing.registryIds as string[]) : [];
        if (!ids.includes(registryId)) return;  // Idempotency
        const newCount = Math.max(0, (typeof existing.registryCount === "number" ? existing.registryCount : ids.length) - 1);
        if (newCount === 0) {
          tx.delete(ref);
        } else {
          tx.update(ref, {
            registryIds: FieldValue.arrayRemove(registryId),
            registryCount: FieldValue.increment(-1),
            updatedAt: FieldValue.serverTimestamp(),
          });
        }
      });
    }

    /**
     * D-19 onUpdate: only react when originalUrl changed.
     * If URL changed → delete(oldUrl, registryId) + create(newUrl, registryId).
     * For other field changes (title, image, price), no counter update needed.
     * (Denorm refresh of title/image/price could be done here as a quality
     * improvement, but D-19 explicitly says no counter update — leave denorm
     * stale until the next create event refreshes it.)
     */
    export async function handleItemUpdate(
      db: Firestore,
      registryId: string,
      before: ItemDoc,
      after: ItemDoc
    ): Promise<void> {
      const beforeUrl = before.originalUrl ?? "";
      const afterUrl = after.originalUrl ?? "";
      if (beforeUrl === afterUrl) return;
      if (beforeUrl) {
        await handleItemDelete(db, registryId, before);
      }
      if (afterUrl) {
        await handleItemCreate(db, registryId, after);
      }
    }

    // ────────────────────────────────────────────────────────────────────
    // Trigger wrappers — D-47 named exports
    // ────────────────────────────────────────────────────────────────────

    export const onItemCreatePopular = onDocumentCreated(
      { region: REGION, document: "registries/{registryId}/items/{itemId}" },
      async (event) => {
        const snap = event.data;
        if (!snap) return;
        const registryId = event.params.registryId;
        const item = snap.data() as ItemDoc;
        await handleItemCreate(admin.firestore(), registryId, item);
      }
    );

    export const onItemDeletePopular = onDocumentDeleted(
      { region: REGION, document: "registries/{registryId}/items/{itemId}" },
      async (event) => {
        const snap = event.data;
        if (!snap) return;
        const registryId = event.params.registryId;
        const item = snap.data() as ItemDoc;
        await handleItemDelete(admin.firestore(), registryId, item);
      }
    );

    export const onItemUpdatePopular = onDocumentUpdated(
      { region: REGION, document: "registries/{registryId}/items/{itemId}" },
      async (event) => {
        const change = event.data;
        if (!change) return;
        const registryId = event.params.registryId;
        const before = change.before.data() as ItemDoc;
        const after = change.after.data() as ItemDoc;
        await handleItemUpdate(admin.firestore(), registryId, before, after);
      }
    );
    ```

    Confirm the actual signatures of `onDocumentCreated/Deleted/Updated` in firebase-functions v7.x against the existing `devMailLogger.ts` import — adjust if the API has changed.

    **2. `functions/src/__tests__/discover/triggers.test.ts`** — unit tests for the three handler functions, using a fake Firestore (same pattern as `rateLimit.test.ts` in plan 17-03):

    Test cases (minimum):
    - `handleItemCreate` on missing popularItems doc → creates doc with registryCount=1
    - `handleItemCreate` on existing doc + new registryId → arrayUnion + increment(1)
    - `handleItemCreate` on existing doc + already-counted registryId → no-op (registryCount unchanged)
    - `handleItemDelete` on missing popularItems doc → no-op
    - `handleItemDelete` on doc with registryCount > 1 → decrement
    - `handleItemDelete` on doc with registryCount == 1 → delete popularItems doc entirely
    - `handleItemUpdate` with before.originalUrl == after.originalUrl → no-op (no Firestore calls)
    - `handleItemUpdate` with URL change → invokes delete(old) + create(new)
    - `handleItemCreate` with item.originalUrl undefined → no-op
    - URL normalization integration: two items with different utm params hit the same productId (assert via spying the ref path)

    Use a fake Firestore object that supports `collection().doc()`, `runTransaction()`, with tx.get/set/update/delete; track all writes for assertion.

    **3. `functions/src/index.ts`** — add three named exports after the discoverSearch line (added in plan 17-03):
    ```typescript
    export { onItemCreatePopular, onItemDeletePopular, onItemUpdatePopular } from "./discover/triggers";
    ```
  </action>

  <verify>
    <automated>
      bash -c '
      set -e
      cd /Users/victorpop/ai-projects/gift-registry
      grep -q "export const onItemCreatePopular" functions/src/discover/triggers.ts
      grep -q "export const onItemDeletePopular" functions/src/discover/triggers.ts
      grep -q "export const onItemUpdatePopular" functions/src/discover/triggers.ts
      grep -q "export async function handleItemCreate" functions/src/discover/triggers.ts
      grep -q "export async function handleItemDelete" functions/src/discover/triggers.ts
      grep -q "export async function handleItemUpdate" functions/src/discover/triggers.ts
      grep -q "registries/{registryId}/items/{itemId}" functions/src/discover/triggers.ts
      grep -q "europe-west3" functions/src/discover/triggers.ts
      grep -q "arrayUnion\\|arrayRemove\\|increment" functions/src/discover/triggers.ts
      grep -q "normalizeUrl" functions/src/discover/triggers.ts
      grep -q "onItemCreatePopular" functions/src/index.ts
      grep -q "onItemDeletePopular" functions/src/index.ts
      grep -q "onItemUpdatePopular" functions/src/index.ts
      cd functions && npm test -- --testPathPattern=discover/triggers --silent 2>&1 | tail -20
      npm run build 2>&1 | tail -5
      echo OK
      '
    </automated>
  </verify>

  <done>
    `functions/src/discover/triggers.ts` exports 3 triggers (onCreate/Delete/Update) wired to `registries/{registryId}/items/{itemId}` in `europe-west3`, plus 3 testable handler functions. All trigger unit tests pass (10+ cases). `functions/src/index.ts` re-exports all three. `npm run build` succeeds.
  </done>
</task>

<task type="auto">
  <name>Task 2: backfillPopularItems.ts one-shot script (idempotent, 500-doc batched, set+merge)</name>

  <read_first>
    - .planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-CONTEXT.md (decision D-22 verbatim — scan collectionGroup("items"), aggregate, 500-doc batches, set+merge, run BEFORE deploying triggers)
    - functions/src/discover/urlNormalization.ts (consumed for productId aggregation)
    - functions/scripts/seedStores.ts — DELETED in plan 17-01; reference is now via the SUMMARY only. The plan 17-01 deleted the seedStores file but its pattern (Admin SDK init, main() with batched writes, ts-node entry, exit codes) is documented in plan 17-01's task action — mirror that structure.
    - functions/package.json (confirm npm scripts entry point; this plan adds a `backfill:popular` script)
  </read_first>

  <files>
    functions/scripts/backfillPopularItems.ts,
    functions/package.json
  </files>

  <action>
    **1. `functions/scripts/backfillPopularItems.ts`** — one-shot Admin SDK script that scans every item across all registries, aggregates by productId, and writes `popularItems` docs in 500-doc batches. Per D-22 verbatim:

    ```typescript
    /**
     * Phase 17 D-22 one-shot script: backfill popularItems from existing
     * registries/*\/items.
     *
     * Idempotent — every write uses set(..., { merge: true }) so re-running
     * the script after triggers are live is safe (D-22 explicit).
     *
     * MUST run BEFORE deploying the onItemCreate/Delete/Update triggers,
     * otherwise live triggers race with the batched writes (D-22 verbatim).
     *
     * Run via:
     *   cd functions
     *   npx ts-node scripts/backfillPopularItems.ts
     *
     * Requires application-default credentials (`gcloud auth
     * application-default login`) when targeting prod.
     */
    import * as admin from "firebase-admin";
    import { FieldValue } from "firebase-admin/firestore";
    import { normalizeUrl } from "../src/discover/urlNormalization";

    if (admin.apps.length === 0) {
      admin.initializeApp();
    }

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
        const { productId, canonicalUrl } = normalizeUrl(originalUrl);
        const existing = byProduct.get(productId);
        if (existing) {
          existing.registryIds.add(registryId);
        } else {
          byProduct.set(productId, {
            productId,
            canonicalUrl,
            title: typeof data.title === "string" ? data.title : "",
            imageUrl: typeof data.imageUrl === "string" ? data.imageUrl : "",
            price: typeof data.price === "string" ? data.price : "",
            registryIds: new Set([registryId]),
          });
        }
      }
      console.log(`Aggregated to ${byProduct.size} unique products (${skippedNoUrl} items skipped — no originalUrl).`);

      // Batched writes — 500 docs/batch (Firestore limit).
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
        if (inBatch === 500) {
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
        console.error("Backfill failed:", err);
        process.exit(1);
      });
    ```

    **2. `functions/package.json`** — add a new npm script entry. Insert after the existing `"test:watch": "jest --watch",` line (the previous `seed:stores` line was deleted in plan 17-01):
    ```json
        "backfill:popular": "ts-node scripts/backfillPopularItems.ts",
    ```
    Preserve JSON validity (trailing comma management).
  </action>

  <verify>
    <automated>
      bash -c '
      set -e
      cd /Users/victorpop/ai-projects/gift-registry
      grep -q "collectionGroup(\"items\")" functions/scripts/backfillPopularItems.ts
      grep -q "merge: true" functions/scripts/backfillPopularItems.ts
      grep -q "batch.set" functions/scripts/backfillPopularItems.ts
      grep -q "normalizeUrl" functions/scripts/backfillPopularItems.ts
      # 500-doc batches present
      grep -q "500\\|inBatch ===" functions/scripts/backfillPopularItems.ts
      # npm script registered
      grep -q "backfill:popular" functions/package.json
      # TypeScript still compiles
      cd functions && npm run build 2>&1 | tail -5
      # The script must compile by itself when invoked via ts-node
      npx ts-node --transpileOnly scripts/backfillPopularItems.ts --help 2>&1 | head -5 || true
      echo OK
      '
    </automated>
  </verify>

  <done>
    `functions/scripts/backfillPopularItems.ts` exists with: Admin SDK init, `collectionGroup("items").get()`, productId aggregation via normalizeUrl, batched writes capped at 500 docs/batch, `set(..., { merge: true })` for idempotency, console progress logging, exit codes. `functions/package.json` has a `backfill:popular` npm script. TypeScript compiles. The script is invokable from CLI (verified by ts-node compile pass; actual execution against live data is plan 17-06).
  </done>
</task>

</tasks>

<verification>
1. `cd functions && npm run build` succeeds — both triggers.ts and backfillPopularItems.ts compile.
2. `cd functions && npm test -- --testPathPattern=discover` runs all discover tests (urlNormalization, retailers, promptTemplate, parseGeminiResponse, cacheKeyNormalization, rateLimit, triggers) and all pass.
3. `grep -E "onItemCreatePopular|onItemDeletePopular|onItemUpdatePopular" functions/src/index.ts` shows three new exports.
4. `cd functions && npm run backfill:popular --help` (or `npx ts-node --transpileOnly scripts/backfillPopularItems.ts`) does not error at compile time. Actual execution is gated behind plan 17-06 (must run against live data with ADC).
</verification>

<success_criteria>
- `functions/src/discover/triggers.ts` exports onItemCreatePopular + onItemDeletePopular + onItemUpdatePopular (Firestore v2 wrappers) and 3 testable handlers (handleItemCreate / handleItemDelete / handleItemUpdate).
- All triggers use `region: "europe-west3"` and `document: "registries/{registryId}/items/{itemId}"`.
- handleItemCreate is idempotent (checks registryIds before incrementing).
- handleItemDelete deletes the popularItems doc when registryCount hits 0.
- handleItemUpdate fires the delete+create flow only when originalUrl changed.
- 10+ unit test cases pass for trigger handlers.
- `functions/scripts/backfillPopularItems.ts` uses collectionGroup, normalizeUrl, set+merge, 500-doc batched commits.
- `functions/package.json` has a `backfill:popular` npm script.
- Three new named exports added to `functions/src/index.ts`.
- `npm run build` succeeds.
</success_criteria>

<output>
After completion, create `.planning/phases/17-discover-feature-with-community-popular-products-and-ai-powered-web-search-via-gemini/17-04-SUMMARY.md` documenting:
- The exact Firestore document path used by the triggers (registries/{registryId}/items/{itemId}).
- The aggregation strategy used in backfill (Map<productId, {registryIds: Set<string>}> in-memory).
- The Firestore 500-doc batch limit ceremony (commit, reset, commit final).
- A reminder that plan 17-06 MUST run backfill BEFORE deploying triggers (D-22 deploy ordering).
- Any deviation from D-18/D-19 atomicity model (e.g., if `runTransaction` was replaced with `batch` due to triggers not supporting transactions).
</output>
