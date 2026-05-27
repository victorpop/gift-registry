/**
 * Phase 17 D-17/D-18/D-19/D-47: popularItems counter maintenance triggers.
 *
 * Three Firestore-triggered Functions plus three exported testable handlers:
 *   - onItemCreatePopular  → handleItemCreate
 *   - onItemDeletePopular  → handleItemDelete
 *   - onItemUpdatePopular  → handleItemUpdate (delegates to delete + create)
 *
 * The handler functions accept an injected Firestore instance so they can be
 * unit-tested against a fake (no firebase-functions runtime required).
 *
 * Invariants (D-18 verbatim):
 *   - registryIds is the source of truth; registryCount is a denormalized
 *     counter kept in sync via FieldValue.increment.
 *   - Creates are idempotent: a re-fire for the same (productId, registryId)
 *     pair short-circuits to a no-op (checked before incrementing).
 *   - Deletes are idempotent: removing a registryId that is not in the array
 *     is a no-op (checked before decrementing).
 *   - When registryCount reaches 0, the popularItems doc is deleted entirely
 *     so the collection stays compact.
 *
 * D-19: updates only react when originalUrl changed. Title/image/price
 * changes do NOT touch counters (the next create-elsewhere refresh will pick
 * up the denormalized fields — D-19 explicitly accepts this staleness).
 */

import {
  onDocumentCreated,
  onDocumentDeleted,
  onDocumentUpdated,
} from "firebase-functions/v2/firestore";
import * as admin from "firebase-admin";
import { FieldValue, Firestore } from "firebase-admin/firestore";
import { normalizeUrl } from "./urlNormalization";

const REGION = "europe-west3";
const ITEM_DOCUMENT_PATH = "registries/{registryId}/items/{itemId}";

interface ItemDoc {
  originalUrl?: string;
  title?: string;
  imageUrl?: string;
  price?: string;
}

// ────────────────────────────────────────────────────────────────────
// Testable handlers (D-18 / D-19)
// ────────────────────────────────────────────────────────────────────

/**
 * D-18 onCreate handler: a registry just added this product.
 *
 * Behavior:
 *   - If popularItems/{productId} missing → set with registryCount=1.
 *   - If present and registryId NOT in registryIds → arrayUnion + increment(1).
 *   - If present and registryId already in registryIds → no-op (idempotent
 *     guard, since increment(1) is NOT itself idempotent on re-fire).
 *
 * Items lacking originalUrl are not tracked (Discover popular only surfaces
 * URL-bearing items; manually-added items without a URL never appear).
 */
export async function handleItemCreate(
  db: Firestore,
  registryId: string,
  item: ItemDoc
): Promise<void> {
  if (!item.originalUrl) return;
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
    const ids = Array.isArray(existing.registryIds)
      ? (existing.registryIds as string[])
      : [];
    if (ids.includes(registryId)) {
      // Idempotency guard (D-18): already counted — bail out before increment.
      return;
    }
    tx.update(ref, {
      registryIds: FieldValue.arrayUnion(registryId),
      registryCount: FieldValue.increment(1),
      updatedAt: FieldValue.serverTimestamp(),
      // Refresh denorm fields opportunistically (most-recent-write wins).
      title: item.title ?? (existing.title as string | undefined) ?? "",
      imageUrl: item.imageUrl ?? (existing.imageUrl as string | undefined) ?? "",
      price: item.price ?? (existing.price as string | undefined) ?? "",
    });
  });
}

/**
 * D-18 onDelete handler: a registry just removed this product.
 *
 * Behavior:
 *   - If popularItems doc missing → no-op (already cleaned up).
 *   - If registryId NOT in registryIds → no-op (already removed; idempotent).
 *   - If decremented count would be 0 → delete the popularItems doc entirely.
 *   - Else → arrayRemove + increment(-1).
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
    const ids = Array.isArray(existing.registryIds)
      ? (existing.registryIds as string[])
      : [];
    if (!ids.includes(registryId)) return; // Idempotency
    const currentCount =
      typeof existing.registryCount === "number"
        ? (existing.registryCount as number)
        : ids.length;
    const newCount = Math.max(0, currentCount - 1);
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
 * D-19 onUpdate handler: only react when originalUrl changed.
 *
 * - If before.originalUrl === after.originalUrl → no-op (other field changes
 *   do not touch counters; denorm fields stay until the next create event).
 * - If URL changed → delete(old, registryId) + create(new, registryId).
 *   Either side may be a no-op if its URL is empty.
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
  { region: REGION, document: ITEM_DOCUMENT_PATH },
  async (event) => {
    const snap = event.data;
    if (!snap) return;
    const registryId = event.params.registryId;
    const item = snap.data() as ItemDoc;
    await handleItemCreate(admin.firestore(), registryId, item);
  }
);

export const onItemDeletePopular = onDocumentDeleted(
  { region: REGION, document: ITEM_DOCUMENT_PATH },
  async (event) => {
    const snap = event.data;
    if (!snap) return;
    const registryId = event.params.registryId;
    const item = snap.data() as ItemDoc;
    await handleItemDelete(admin.firestore(), registryId, item);
  }
);

export const onItemUpdatePopular = onDocumentUpdated(
  { region: REGION, document: ITEM_DOCUMENT_PATH },
  async (event) => {
    const change = event.data;
    if (!change) return;
    const registryId = event.params.registryId;
    const before = change.before.data() as ItemDoc;
    const after = change.after.data() as ItemDoc;
    await handleItemUpdate(admin.firestore(), registryId, before, after);
  }
);
