/**
 * Phase 17 D-18/D-19: unit tests for popularItems trigger handlers.
 * RED-phase: committed before implementation; tests MUST fail.
 *
 * Tests target the testable pure handlers (handleItemCreate / handleItemDelete /
 * handleItemUpdate) using a fake Firestore that records every transaction op.
 */

import { handleItemCreate, handleItemDelete, handleItemUpdate } from "../../discover/triggers";

// ────────────────────────────────────────────────────────────────────
// Fake Firestore mirroring the surface area the handlers use:
//   db.collection(name).doc(id) → DocRef
//   db.runTransaction((tx) => Promise<R>) → Promise<R>
//   tx.get(ref) → { exists, data() }
//   tx.set(ref, data)
//   tx.update(ref, data)
//   tx.delete(ref)
//
// We capture all ops on the docs so tests can assert behavior.
// ────────────────────────────────────────────────────────────────────

type StoreShape = Record<string, Record<string, Record<string, unknown>>>;

interface OpRecord {
  type: "set" | "update" | "delete";
  collection: string;
  id: string;
  data?: Record<string, unknown>;
}

function makeFakeDb(initial: StoreShape = {}) {
  const store: StoreShape = JSON.parse(JSON.stringify(initial));
  const ops: OpRecord[] = [];

  function makeDocRef(collection: string, id: string) {
    return {
      _collection: collection,
      _id: id,
      path: `${collection}/${id}`,
    };
  }

  const txApi = {
    get: async (ref: { _collection: string; _id: string }) => {
      const col = store[ref._collection];
      const data = col ? col[ref._id] : undefined;
      return {
        exists: data !== undefined,
        data: () => (data ? { ...data } : undefined),
      };
    },
    set: (
      ref: { _collection: string; _id: string },
      data: Record<string, unknown>
    ) => {
      if (!store[ref._collection]) store[ref._collection] = {};
      store[ref._collection][ref._id] = { ...data };
      ops.push({ type: "set", collection: ref._collection, id: ref._id, data: { ...data } });
    },
    update: (
      ref: { _collection: string; _id: string },
      data: Record<string, unknown>
    ) => {
      if (!store[ref._collection]) store[ref._collection] = {};
      const existing = store[ref._collection][ref._id] || {};
      // Note: this is a simplified update — FieldValue sentinels are NOT applied;
      // tests inspect the raw recorded ops to verify FieldValue usage.
      store[ref._collection][ref._id] = { ...existing, ...data };
      ops.push({ type: "update", collection: ref._collection, id: ref._id, data: { ...data } });
    },
    delete: (ref: { _collection: string; _id: string }) => {
      if (store[ref._collection]) delete store[ref._collection][ref._id];
      ops.push({ type: "delete", collection: ref._collection, id: ref._id });
    },
  };

  const db = {
    collection: (collection: string) => ({
      doc: (id: string) => makeDocRef(collection, id),
    }),
    runTransaction: async <R,>(fn: (tx: typeof txApi) => Promise<R>) => {
      return fn(txApi);
    },
  };

  return { db: db as unknown as import("firebase-admin/firestore").Firestore, store, ops };
}

// Stable productId from normalizeUrl for the test fixtures
import { normalizeUrl } from "../../discover/urlNormalization";

const URL_A = "https://emag.ro/products/coffee-machine";
const URL_A_WITH_UTM = "https://emag.ro/products/coffee-machine?utm_source=fb";
const URL_B = "https://altex.ro/products/headphones";

describe("popularItems triggers", () => {
  describe("handleItemCreate", () => {
    it("creates new popularItems doc when missing", async () => {
      const { db, store, ops } = makeFakeDb();
      const { productId, canonicalUrl } = normalizeUrl(URL_A);

      await handleItemCreate(db, "reg1", {
        originalUrl: URL_A,
        title: "Coffee Machine",
        imageUrl: "https://img/coffee.jpg",
        price: "459,00 RON",
      });

      const doc = store.popularItems[productId];
      expect(doc).toBeDefined();
      expect(doc.canonicalUrl).toBe(canonicalUrl);
      expect(doc.title).toBe("Coffee Machine");
      expect(doc.imageUrl).toBe("https://img/coffee.jpg");
      expect(doc.price).toBe("459,00 RON");
      expect(doc.registryIds).toEqual(["reg1"]);
      expect(doc.registryCount).toBe(1);
      expect(doc.updatedAt).toBeDefined();

      // Recorded op should be a 'set'
      const sets = ops.filter((o) => o.type === "set");
      expect(sets).toHaveLength(1);
      expect(sets[0].collection).toBe("popularItems");
      expect(sets[0].id).toBe(productId);
    });

    it("adds registryId via arrayUnion + increment when doc exists and registryId is new", async () => {
      const { productId } = normalizeUrl(URL_A);
      const { db, ops } = makeFakeDb({
        popularItems: {
          [productId]: {
            canonicalUrl: normalizeUrl(URL_A).canonicalUrl,
            title: "Coffee Machine",
            imageUrl: "",
            price: "",
            registryIds: ["regOther"],
            registryCount: 1,
            updatedAt: new Date(),
          },
        },
      });

      await handleItemCreate(db, "reg1", {
        originalUrl: URL_A,
        title: "Coffee Machine V2",
      });

      // Recorded op should be a single 'update' (NOT 'set')
      const updates = ops.filter((o) => o.type === "update");
      expect(updates).toHaveLength(1);
      const updateData = updates[0].data!;

      // arrayUnion + increment(1) are FieldValue sentinels — verify by structure
      expect(updateData.registryIds).toBeDefined();
      expect(updateData.registryCount).toBeDefined();
      expect(updateData.updatedAt).toBeDefined();
      // FieldValue sentinels expose isEqual / _methodName depending on impl;
      // we assert they are NOT plain arrays/numbers (proving FieldValue was used).
      expect(Array.isArray(updateData.registryIds)).toBe(false);
      expect(typeof updateData.registryCount).not.toBe("number");
    });

    it("is idempotent — re-firing for same (productId, registryId) is a no-op", async () => {
      const { productId } = normalizeUrl(URL_A);
      const { db, ops } = makeFakeDb({
        popularItems: {
          [productId]: {
            canonicalUrl: normalizeUrl(URL_A).canonicalUrl,
            title: "Coffee Machine",
            imageUrl: "",
            price: "",
            registryIds: ["reg1"], // Already counted
            registryCount: 1,
            updatedAt: new Date(),
          },
        },
      });

      await handleItemCreate(db, "reg1", {
        originalUrl: URL_A,
        title: "Coffee Machine",
      });

      // No update/set/delete ops should have been recorded
      expect(ops).toEqual([]);
    });

    it("normalizes URL — two items with different utm params hit the same productId", async () => {
      const { db, ops } = makeFakeDb();
      await handleItemCreate(db, "reg1", { originalUrl: URL_A });
      await handleItemCreate(db, "reg2", { originalUrl: URL_A_WITH_UTM });

      const { productId } = normalizeUrl(URL_A);
      // First call: set; second call: update on the same productId
      expect(ops[0].type).toBe("set");
      expect(ops[0].id).toBe(productId);
      expect(ops[1].type).toBe("update");
      expect(ops[1].id).toBe(productId);
    });

    it("no-op when originalUrl is undefined", async () => {
      const { db, ops } = makeFakeDb();
      await handleItemCreate(db, "reg1", { title: "No URL Item" });
      expect(ops).toEqual([]);
    });

    it("no-op when originalUrl is empty string", async () => {
      const { db, ops } = makeFakeDb();
      await handleItemCreate(db, "reg1", { originalUrl: "" });
      expect(ops).toEqual([]);
    });
  });

  describe("handleItemDelete", () => {
    it("no-op when popularItems doc is missing", async () => {
      const { db, ops } = makeFakeDb();
      await handleItemDelete(db, "reg1", { originalUrl: URL_A });
      expect(ops).toEqual([]);
    });

    it("decrements counter and removes registryId when registryCount > 1", async () => {
      const { productId } = normalizeUrl(URL_A);
      const { db, ops } = makeFakeDb({
        popularItems: {
          [productId]: {
            canonicalUrl: normalizeUrl(URL_A).canonicalUrl,
            title: "Coffee Machine",
            imageUrl: "",
            price: "",
            registryIds: ["reg1", "reg2"],
            registryCount: 2,
            updatedAt: new Date(),
          },
        },
      });

      await handleItemDelete(db, "reg1", { originalUrl: URL_A });

      const updates = ops.filter((o) => o.type === "update");
      const deletes = ops.filter((o) => o.type === "delete");
      expect(updates).toHaveLength(1);
      expect(deletes).toHaveLength(0);
      const updateData = updates[0].data!;
      expect(updateData.registryIds).toBeDefined();
      expect(updateData.registryCount).toBeDefined();
      expect(updateData.updatedAt).toBeDefined();
    });

    it("deletes popularItems doc entirely when registryCount falls to 0", async () => {
      const { productId } = normalizeUrl(URL_A);
      const { db, ops, store } = makeFakeDb({
        popularItems: {
          [productId]: {
            canonicalUrl: normalizeUrl(URL_A).canonicalUrl,
            title: "Coffee Machine",
            imageUrl: "",
            price: "",
            registryIds: ["reg1"],
            registryCount: 1,
            updatedAt: new Date(),
          },
        },
      });

      await handleItemDelete(db, "reg1", { originalUrl: URL_A });

      const deletes = ops.filter((o) => o.type === "delete");
      const updates = ops.filter((o) => o.type === "update");
      expect(deletes).toHaveLength(1);
      expect(updates).toHaveLength(0);
      expect(deletes[0].collection).toBe("popularItems");
      expect(deletes[0].id).toBe(productId);
      // Doc actually removed from store
      expect(store.popularItems[productId]).toBeUndefined();
    });

    it("idempotent — re-firing delete when registryId not in list is a no-op", async () => {
      const { productId } = normalizeUrl(URL_A);
      const { db, ops } = makeFakeDb({
        popularItems: {
          [productId]: {
            canonicalUrl: normalizeUrl(URL_A).canonicalUrl,
            title: "Coffee Machine",
            imageUrl: "",
            price: "",
            registryIds: ["regOther"],
            registryCount: 1,
            updatedAt: new Date(),
          },
        },
      });

      await handleItemDelete(db, "reg1", { originalUrl: URL_A });
      expect(ops).toEqual([]);
    });

    it("no-op when originalUrl missing on the deleted item", async () => {
      const { db, ops } = makeFakeDb();
      await handleItemDelete(db, "reg1", { title: "no-url" });
      expect(ops).toEqual([]);
    });
  });

  describe("handleItemUpdate", () => {
    it("no-op when originalUrl is unchanged", async () => {
      const { db, ops } = makeFakeDb();
      await handleItemUpdate(
        db,
        "reg1",
        { originalUrl: URL_A, title: "Old Title", price: "100" },
        { originalUrl: URL_A, title: "New Title", price: "150" }
      );
      expect(ops).toEqual([]);
    });

    it("invokes delete(old) + create(new) when originalUrl changed", async () => {
      const { productId: oldProductId } = normalizeUrl(URL_A);
      const { productId: newProductId } = normalizeUrl(URL_B);
      // Seed the old popularItems doc so delete actually runs
      const { db, ops, store } = makeFakeDb({
        popularItems: {
          [oldProductId]: {
            canonicalUrl: normalizeUrl(URL_A).canonicalUrl,
            title: "Coffee Machine",
            imageUrl: "",
            price: "",
            registryIds: ["reg1"],
            registryCount: 1,
            updatedAt: new Date(),
          },
        },
      });

      await handleItemUpdate(
        db,
        "reg1",
        { originalUrl: URL_A, title: "Coffee Machine" },
        { originalUrl: URL_B, title: "Headphones" }
      );

      // delete the old one (count went from 1 → 0)
      const deletes = ops.filter((o) => o.type === "delete");
      expect(deletes.some((o) => o.id === oldProductId)).toBe(true);
      expect(store.popularItems[oldProductId]).toBeUndefined();

      // create the new one
      const sets = ops.filter((o) => o.type === "set");
      expect(sets.some((o) => o.id === newProductId)).toBe(true);
      expect(store.popularItems[newProductId]).toBeDefined();
      expect(store.popularItems[newProductId].registryIds).toEqual(["reg1"]);
    });

    it("creates new doc when before.originalUrl was empty and after.originalUrl is set", async () => {
      const { db, store } = makeFakeDb();
      const { productId } = normalizeUrl(URL_A);

      await handleItemUpdate(
        db,
        "reg1",
        { originalUrl: "", title: "no url before" },
        { originalUrl: URL_A, title: "Now has URL" }
      );

      expect(store.popularItems[productId]).toBeDefined();
      expect(store.popularItems[productId].registryIds).toEqual(["reg1"]);
    });

    it("deletes from old doc when after.originalUrl is empty", async () => {
      const { productId } = normalizeUrl(URL_A);
      const { db, store } = makeFakeDb({
        popularItems: {
          [productId]: {
            canonicalUrl: normalizeUrl(URL_A).canonicalUrl,
            title: "Coffee Machine",
            imageUrl: "",
            price: "",
            registryIds: ["reg1"],
            registryCount: 1,
            updatedAt: new Date(),
          },
        },
      });

      await handleItemUpdate(
        db,
        "reg1",
        { originalUrl: URL_A, title: "Coffee Machine" },
        { originalUrl: "", title: "Cleared URL" }
      );

      expect(store.popularItems[productId]).toBeUndefined();
    });
  });
});
